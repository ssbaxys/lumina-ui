package io.github.ssbaxys.lumina.markdown;

import io.github.ssbaxys.lumina.markdown.Md.Block;
import io.github.ssbaxys.lumina.markdown.Md.Inline;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;

import java.util.ArrayList;
import java.util.List;

/**
 * Раскладка Markdown в плоский список операций рисования для заданной ширины. Считается только при
 * изменении текста или ширины; кадр просто проходит по готовым спискам.
 */
final class MarkdownLayout {
    // Роли цвета: настоящие цвета подставляются при рисовании из стиля (тема может меняться).
    static final int TEXT = 0;
    static final int MUTED = 1;
    static final int LINK = 2;
    static final int CODE = 3;
    static final int CODE_BG = 4;
    static final int QUOTE_BAR = 5;
    static final int LINE = 6;
    static final int HEAD_BG = 7;
    static final int HIGHLIGHT = 8;
    static final int ACCENT = 9;
    /** Роли подсветки кода: {@code SYNTAX + вид лексемы} ({@link SyntaxHighlighter#KEYWORD} и т. д.). */
    static final int SYNTAX = 10;
    /** Размер палитры: базовые роли и все виды лексем. */
    static final int ROLES = SYNTAX + SyntaxHighlighter.KINDS;

    record Run(float x, float y, String text, int role, float scale, int link) {
    }

    record Box(float x, float y, float w, float h, int role, float radius) {
    }

    /** Маркер списка: 0 — круг, 1 — кольцо, 2 — квадрат. */
    record Bullet(float cx, float cy, float r, int kind) {
    }

    record Check(float x, float y, float size, Md.ListItem item) {
    }

    record Icon(float x, float y, float size, String url) {
    }

    record LinkArea(float x, float y, float w, float h, int link) {
    }

    private static final float BLOCK_GAP = 6f;
    private static final float LINE_GAP = 2f;
    private static final float[] HEADING_SCALE = {2f, 1.5f, 1.25f, 1.1f, 1f, 1f};

    final List<Run> runs = new ArrayList<>();
    final List<Box> boxes = new ArrayList<>();
    final List<Bullet> bullets = new ArrayList<>();
    final List<Check> checks = new ArrayList<>();
    final List<Icon> icons = new ArrayList<>();
    final List<LinkArea> linkAreas = new ArrayList<>();
    final List<String> links = new ArrayList<>();
    float width;
    float height;

    private final TextMetrics metrics;
    private final float lh;

    MarkdownLayout(TextMetrics metrics) {
        this.metrics = metrics;
        this.lh = metrics.lineHeight();
    }

    static MarkdownLayout build(List<Block> blocks, float maxWidth, TextMetrics metrics) {
        MarkdownLayout l = new MarkdownLayout(metrics);
        l.height = l.blocks(blocks, 0f, 0f, maxWidth, TEXT, 0);
        return l;
    }

    // ================================================================ блоки

    private float blocks(List<Block> blocks, float x, float y, float w, int textRole, int depth) {
        boolean first = true;
        for (Block b : blocks) {
            if (!first) {
                y += BLOCK_GAP;
            }
            first = false;
            y = block(b, x, y, w, textRole, depth);
        }
        return y;
    }

    private float block(Block b, float x, float y, float w, int textRole, int depth) {
        if (b instanceof Md.Heading h) {
            float scale = HEADING_SCALE[Math.min(h.level(), 6) - 1];
            if (h.level() > 1 || y > 0f) {
                y += 2f;
            }
            y = paragraph(h.content(), x, y, w, scale, Md.BOLD, textRole, Md.Align.LEFT);
            if (h.level() <= 2) {
                y += 2f;
                boxes.add(new Box(x, y, w, 1f, LINE, 0f));
                y += 1f;
            }
            return y;
        }
        if (b instanceof Md.Paragraph p) {
            return paragraph(p.content(), x, y, w, 1f, 0, textRole, Md.Align.LEFT);
        }
        if (b instanceof Md.CodeBlock c) {
            return codeBlock(c, x, y, w);
        }
        if (b instanceof Md.Quote q) {
            float top = y;
            float end = blocks(q.blocks(), x + 9f, y + 2f, w - 9f, MUTED, depth);
            boxes.add(new Box(x + 1f, top, 2f, end - top + 2f, QUOTE_BAR, 1f));
            return end + 2f;
        }
        if (b instanceof Md.ListBlock list) {
            return list(list, x, y, w, textRole, depth);
        }
        if (b instanceof Md.Table t) {
            return table(t, x, y, w, textRole);
        }
        if (b instanceof Md.Rule) {
            boxes.add(new Box(x, y + 3f, w, 1f, LINE, 0f));
            return y + 7f;
        }
        return y;
    }

    private float codeBlock(Md.CodeBlock c, float x, float y, float w) {
        float pad = 5f;
        float top = y;
        float cy = y + pad;
        if (!c.language().isEmpty()) {
            float s = 0.75f;
            String lang = c.language();
            float lw = metrics.width(lang) * s;
            runs.add(new Run(x + w - pad - lw, y + 3f, lang, MUTED, s, -1));
            cy += lh * s;
        }
        float inner = w - 2f * pad;
        String[] lines = c.code().split("\n", -1);
        // Подсветка считается один раз здесь, при раскладке; кадр рисует готовые куски.
        List<List<SyntaxHighlighter.Span>> spans = SyntaxHighlighter.supports(c.language())
                ? SyntaxHighlighter.highlight(c.language(), c.code()) : null;
        for (int li = 0; li < lines.length; li++) {
            String line = lines[li];
            if (line.isEmpty()) {
                cy += lh + LINE_GAP;
                continue;
            }
            // Код не переносится по словам, только по символам, если не влезает.
            int from = 0;
            while (from < line.length()) {
                int to = from + fitChars(line.substring(from), "", inner, 1f);
                if (spans == null) {
                    runs.add(new Run(x + pad, cy, line.substring(from, to), CODE, 1f, -1));
                } else {
                    for (SyntaxHighlighter.Span sp : spans.get(li)) {
                        int a = Math.max(sp.start(), from);
                        int b = Math.min(sp.end(), to);
                        if (b <= a || line.substring(a, b).isBlank()) {
                            continue;
                        }
                        float dx = metrics.width(line.substring(from, a));
                        runs.add(new Run(x + pad + dx, cy, line.substring(a, b), SYNTAX + sp.kind(), 1f, -1));
                    }
                }
                from = to;
                cy += lh + LINE_GAP;
            }
        }
        float bottom = cy - LINE_GAP + pad;
        boxes.add(new Box(x, top, w, bottom - top, CODE_BG, 4f));
        width = Math.max(width, x + w);
        return bottom;
    }

    private float list(Md.ListBlock list, float x, float y, float w, int textRole, int depth) {
        float indent = 12f;
        if (list.ordered()) {
            int last = list.start() + list.items().size() - 1;
            indent = Math.max(indent, metrics.width(last + ".") + 5f);
        }
        boolean first = true;
        int number = list.start();
        for (Md.ListItem item : list.items()) {
            if (!first) {
                y += 2f;
            }
            first = false;
            float markerY = y;
            float contentX = x + indent;
            if (item.checked() != null) {
                float s = lh - 1f;
                checks.add(new Check(contentX, markerY, s, item));
                contentX += s + 4f;
            }
            float end = blocks(item.blocks(), contentX, y, x + w - contentX, textRole, depth + 1);
            if (item.blocks().isEmpty()) {
                end = y + lh;
            }
            if (list.ordered()) {
                String n = number + ".";
                runs.add(new Run(x + indent - 4f - metrics.width(n), markerY, n, textRole, 1f, -1));
            } else if (item.checked() == null) {
                bullets.add(new Bullet(x + 5f, markerY + lh / 2f - 0.5f, 2f, Math.min(depth, 2)));
            }
            number++;
            y = end;
        }
        return y;
    }

    private float table(Md.Table t, float x, float y, float w, int textRole) {
        int cols = t.header().size();
        float pad = 4f;
        float[] natural = new float[cols];
        for (int c = 0; c < cols; c++) {
            natural[c] = naturalWidth(t.header().get(c), Md.BOLD) + 2f * pad;
            for (List<List<Inline>> row : t.rows()) {
                natural[c] = Math.max(natural[c], naturalWidth(row.get(c), 0) + 2f * pad);
            }
        }
        float total = 0f;
        for (float n : natural) {
            total += n;
        }
        float[] widths = natural.clone();
        if (total > w) {
            // Не влезает: ужимаем пропорционально, ячейки переносятся по словам.
            float k = w / total;
            for (int c = 0; c < cols; c++) {
                widths[c] = Math.max(natural[c] * k, 20f);
            }
        }
        float tableW = 0f;
        for (float cw : widths) {
            tableW += cw;
        }
        float top = y;
        y = tableRow(t.header(), t.aligns(), widths, x, y, pad, Md.BOLD, textRole, true);
        for (List<List<Inline>> row : t.rows()) {
            boxes.add(new Box(x, y, tableW, 1f, LINE, 0f));
            y = tableRow(row, t.aligns(), widths, x, y, pad, 0, textRole, false);
        }
        // Рамка и вертикальные линии.
        boxes.add(new Box(x, top, tableW, 1f, LINE, 0f));
        boxes.add(new Box(x, y, tableW, 1f, LINE, 0f));
        float cx = x;
        for (int c = 0; c <= cols; c++) {
            boxes.add(new Box(cx, top, 1f, y - top + 1f, LINE, 0f));
            if (c < cols) {
                cx += widths[c];
            }
        }
        width = Math.max(width, x + tableW + 1f);
        return y + 1f;
    }

    private float tableRow(List<List<Inline>> cells, List<Md.Align> aligns, float[] widths, float x, float y,
                           float pad, int flags, int textRole, boolean header) {
        float rowBottom = y;
        float cx = x;
        int headBoxIndex = boxes.size();
        for (int c = 0; c < widths.length; c++) {
            float end = paragraph(cells.get(c), cx + pad, y + pad, widths[c] - 2f * pad, 1f, flags, textRole,
                    aligns.get(c));
            rowBottom = Math.max(rowBottom, end + pad);
            cx += widths[c];
        }
        if (header) {
            boxes.add(headBoxIndex, new Box(x, y, cx - x, rowBottom - y, HEAD_BG, 0f));
        }
        return rowBottom;
    }

    private float naturalWidth(List<Inline> content, int flags) {
        float wsum = 0f;
        for (Inline in : content) {
            if (in instanceof Md.Text t) {
                wsum += metrics.width(prefix(flags | t.flags()) + t.text());
            } else if (in instanceof Md.Code c) {
                wsum += metrics.width(c.code()) + 4f;
            } else if (in instanceof Md.Link l) {
                wsum += naturalWidth(l.content(), flags | Md.UNDERLINE);
            } else if (in instanceof Md.Image img) {
                wsum += isIcon(img.url()) ? lh + 1f : metrics.width("[" + img.alt() + "]");
            }
        }
        return wsum;
    }

    // ================================================================ строки

    /** Кусок строки перед размещением. */
    private record Piece(String text, int flags, int role, int link, boolean code, boolean space, String icon) {
    }

    private float paragraph(List<Inline> content, float x, float y, float w, float scale, int baseFlags, int role,
                            Md.Align align) {
        List<Piece> pieces = new ArrayList<>();
        collect(content, baseFlags, role, -1, pieces);
        float lineH = lh * scale;
        float cx = x;
        int lineStartRun = runs.size();
        int lineStartBox = boxes.size();
        int lineStartIcon = icons.size();
        int lineStartArea = linkAreas.size();
        boolean lineEmpty = true;
        for (int i = 0; i < pieces.size(); i++) {
            Piece p = pieces.get(i);
            if (p.text == null && p.icon == null) { // перенос строки
                alignLine(align, x, w, cx, lineStartRun, lineStartBox, lineStartIcon, lineStartArea);
                y += lineH + LINE_GAP;
                cx = x;
                lineStartRun = runs.size();
                lineStartBox = boxes.size();
                lineStartIcon = icons.size();
                lineStartArea = linkAreas.size();
                lineEmpty = true;
                continue;
            }
            if (p.space && lineEmpty) {
                continue;
            }
            float pw = pieceWidth(p, scale);
            if (!lineEmpty && cx + pw > x + w + 0.01f && !p.space) {
                alignLine(align, x, w, cx, lineStartRun, lineStartBox, lineStartIcon, lineStartArea);
                y += lineH + LINE_GAP;
                cx = x;
                lineStartRun = runs.size();
                lineStartBox = boxes.size();
                lineStartIcon = icons.size();
                lineStartArea = linkAreas.size();
                lineEmpty = true;
            }
            if (p.space) {
                if (!lineEmpty) {
                    place(p, cx, y, pw, lineH, scale);
                    cx += pw;
                }
                continue;
            }
            // Слово длиннее строки — режем по символам.
            Piece cur = p;
            while (cur.icon == null && cx + pieceWidth(cur, scale) > x + w + 0.01f && cur.text.length() > 1) {
                int fit = Math.max(1, fitChars(cur.text, prefix(cur.flags), x + w - cx, scale));
                Piece head = new Piece(cur.text.substring(0, fit), cur.flags, cur.role, cur.link, cur.code, false, null);
                place(head, cx, y, pieceWidth(head, scale), lineH, scale);
                alignLine(align, x, w, cx + pieceWidth(head, scale), lineStartRun, lineStartBox, lineStartIcon, lineStartArea);
                y += lineH + LINE_GAP;
                cx = x;
                lineStartRun = runs.size();
                lineStartBox = boxes.size();
                lineStartIcon = icons.size();
                lineStartArea = linkAreas.size();
                cur = new Piece(cur.text.substring(fit), cur.flags, cur.role, cur.link, cur.code, false, null);
            }
            float cw = pieceWidth(cur, scale);
            place(cur, cx, y, cw, lineH, scale);
            cx += cw;
            lineEmpty = false;
        }
        alignLine(align, x, w, cx, lineStartRun, lineStartBox, lineStartIcon, lineStartArea);
        width = Math.max(width, Math.min(x + w, cx));
        return y + lineH;
    }

    private void alignLine(Md.Align align, float x, float w, float lineEnd, int run, int box, int icon, int area) {
        if (align == Md.Align.LEFT) {
            return;
        }
        float free = x + w - lineEnd;
        float dx = align == Md.Align.CENTER ? free / 2f : free;
        if (dx <= 0f) {
            return;
        }
        for (int i = run; i < runs.size(); i++) {
            Run r = runs.get(i);
            runs.set(i, new Run(r.x + dx, r.y, r.text, r.role, r.scale, r.link));
        }
        for (int i = box; i < boxes.size(); i++) {
            Box b = boxes.get(i);
            boxes.set(i, new Box(b.x + dx, b.y, b.w, b.h, b.role, b.radius));
        }
        for (int i = icon; i < icons.size(); i++) {
            Icon c = icons.get(i);
            icons.set(i, new Icon(c.x + dx, c.y, c.size, c.url));
        }
        for (int i = area; i < linkAreas.size(); i++) {
            LinkArea a = linkAreas.get(i);
            linkAreas.set(i, new LinkArea(a.x + dx, a.y, a.w, a.h, a.link));
        }
    }

    private void place(Piece p, float x, float y, float w, float lineH, float scale) {
        if (p.icon != null) {
            icons.add(new Icon(x + 0.5f, y + (lineH - lh * scale) / 2f, lh * scale, p.icon));
        } else {
            if (p.code && !p.space) {
                boxes.add(new Box(x, y - 1f, w, lineH + 1f, CODE_BG, 2f));
            }
            if ((p.flags & Md.HIGHLIGHT) != 0) {
                boxes.add(new Box(x, y - 1f, w, lineH + 1f, HIGHLIGHT, 0f));
            }
            float tx = p.code && !p.space ? x + 2f : x;
            String text = prefix(p.flags) + p.text;
            int role = p.code ? CODE : p.role;
            // Склеиваем с предыдущим куском того же стиля на той же строке — меньше вызовов отрисовки.
            Run last = runs.isEmpty() ? null : runs.get(runs.size() - 1);
            if (!p.code && last != null && last.y == y && last.role == role && last.link == p.link
                    && last.scale == scale && sameFlags(last.text, p.flags)
                    && Math.abs(last.x + metrics.width(last.text) * scale - tx) < 0.01f) {
                runs.set(runs.size() - 1, new Run(last.x, y, last.text + p.text, role, scale, p.link));
            } else {
                runs.add(new Run(tx, y, text, role, scale, p.link));
            }
        }
        if (p.link >= 0) {
            linkAreas.add(new LinkArea(x, y - 1f, w, lineH + 2f, p.link));
        }
    }

    private static boolean sameFlags(String runText, int flags) {
        String pre = prefix(flags);
        if (!runText.startsWith(pre)) {
            return false;
        }
        return runText.length() == pre.length() || runText.charAt(pre.length()) != '§';
    }

    private float pieceWidth(Piece p, float scale) {
        if (p.icon != null) {
            return lh * scale + 1f;
        }
        float w = metrics.width(prefix(p.flags) + p.text) * scale;
        return p.code && !p.space ? w + 4f : w;
    }

    private int fitChars(String text, String prefix, float maxW, float scale) {
        int n = 1;
        while (n < text.length() && metrics.width(prefix + text.substring(0, n + 1)) * scale <= maxW) {
            n++;
        }
        return n;
    }

    private void collect(List<Inline> content, int flags, int role, int link, List<Piece> out) {
        for (Inline in : content) {
            if (in instanceof Md.Text t) {
                words(t.text(), flags | t.flags(), role, link, false, out);
            } else if (in instanceof Md.Code c) {
                words(c.code(), flags, role, link, true, out);
            } else if (in instanceof Md.Link l) {
                int id = links.size();
                links.add(l.url());
                collect(l.content(), flags | Md.UNDERLINE, LINK, id, out);
            } else if (in instanceof Md.Image img) {
                if (isIcon(img.url())) {
                    out.add(new Piece(null, flags, role, link, false, false, img.url()));
                } else {
                    words("[" + img.alt() + "]", flags | Md.ITALIC, MUTED, link, false, out);
                }
            } else if (in instanceof Md.LineBreak) {
                out.add(new Piece(null, 0, role, -1, false, false, null));
            }
        }
    }

    private static void words(String text, int flags, int role, int link, boolean code, List<Piece> out) {
        int i = 0;
        int n = text.length();
        while (i < n) {
            if (text.charAt(i) == ' ') {
                out.add(new Piece(" ", flags & ~Md.HIGHLIGHT, role, link, false, true, null));
                while (i < n && text.charAt(i) == ' ') {
                    i++;
                }
            } else {
                int j = i;
                while (j < n && text.charAt(j) != ' ') {
                    j++;
                }
                out.add(new Piece(text.substring(i, j), flags, role, link, code, false, null));
                i = j;
            }
        }
    }

    static boolean isIcon(String url) {
        return url.startsWith("item:") || url.startsWith("texture:");
    }

    /** Коды форматирования шрифта Minecraft: §l жирный, §o курсив, §m зачёркнутый, §n подчёркнутый. */
    static String prefix(int flags) {
        if (flags == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder(8);
        if ((flags & Md.BOLD) != 0) {
            sb.append("§l");
        }
        if ((flags & Md.ITALIC) != 0) {
            sb.append("§o");
        }
        if ((flags & Md.STRIKE) != 0) {
            sb.append("§m");
        }
        if ((flags & Md.UNDERLINE) != 0) {
            sb.append("§n");
        }
        return sb.toString();
    }

    // ================================================================ рисование

    /**
     * Нарисовать готовую раскладку в точке (0, 0). {@code palette} — цвета по ролям (размер {@link #ROLES}),
     * {@code hoveredLink} — подсвеченная ссылка или -1.
     */
    void paint(Canvas canvas, int[] palette, boolean shadow, int hoveredLink) {
        int[] c = palette;
        for (Box b : boxes) {
            if (b.radius() > 0f) {
                canvas.fillRoundRect(b.x(), b.y(), b.w(), b.h(), Radii.all(b.radius()), Brush.solid(c[b.role()]));
            } else {
                canvas.fillRect(b.x(), b.y(), b.w(), b.h(), c[b.role()]);
            }
        }
        if (hoveredLink >= 0) {
            for (LinkArea a : linkAreas) {
                if (a.link() == hoveredLink) {
                    canvas.fillRoundRect(a.x() - 1f, a.y(), a.w() + 2f, a.h(), Radii.all(2f),
                            Brush.solid(Color.withAlpha(c[ACCENT], 0x30)));
                }
            }
        }
        for (Bullet b : bullets) {
            int color = c[MUTED];
            switch (b.kind()) {
                case 0 -> canvas.fillCircle(b.cx(), b.cy(), b.r(), Brush.solid(color));
                case 1 -> canvas.strokeCircle(b.cx(), b.cy(), b.r(), 1f, Brush.solid(color));
                default -> canvas.fillRect(b.cx() - b.r(), b.cy() - b.r(), 2f * b.r(), 2f * b.r(), color);
            }
        }
        for (Check ch : checks) {
            paintCheck(canvas, ch, c);
        }
        for (Run r : runs) {
            int color = r.link() >= 0 && r.link() == hoveredLink ? Color.lighten(c[r.role()], 0.12f) : c[r.role()];
            if (r.scale() != 1f) {
                canvas.save();
                canvas.translate(r.x(), r.y());
                canvas.scale(r.scale(), r.scale());
                canvas.drawText(r.text(), 0f, 0f, color, shadow);
                canvas.restore();
            } else {
                canvas.drawText(r.text(), r.x(), r.y(), color, shadow);
            }
        }
        for (Icon icon : icons) {
            String url = icon.url();
            if (url.startsWith("item:")) {
                canvas.drawItem(url.substring(5), icon.x(), icon.y(), icon.size());
            } else {
                canvas.drawTexture(TextureRef.of(url.substring(8)), icon.x(), icon.y(), icon.size(), icon.size(),
                        Color.WHITE);
            }
        }
    }

    private static void paintCheck(Canvas canvas, Check ch, int[] c) {
        float x = ch.x();
        float y = ch.y();
        float s = ch.size();
        Radii r = Radii.all(2f);
        if (Boolean.TRUE.equals(ch.item().checked())) {
            canvas.fillRoundRect(x, y, s, s, r, Brush.solid(c[ACCENT]));
            int mark = Color.luminance(c[ACCENT]) > 0.5f ? 0xFF1D2330 : Color.WHITE;
            canvas.drawLine(x + s * 0.22f, y + s * 0.52f, x + s * 0.42f, y + s * 0.72f, 1.4f, mark);
            canvas.drawLine(x + s * 0.42f, y + s * 0.72f, x + s * 0.8f, y + s * 0.28f, 1.4f, mark);
        } else {
            canvas.strokeRoundRect(x, y, s, s, r, 1f, c[MUTED]);
        }
    }

    /** Ссылка под точкой или -1. */
    int linkAt(float x, float y) {
        for (LinkArea a : linkAreas) {
            if (x >= a.x() && x < a.x() + a.w() && y >= a.y() && y < a.y() + a.h()) {
                return a.link();
            }
        }
        return -1;
    }
}
