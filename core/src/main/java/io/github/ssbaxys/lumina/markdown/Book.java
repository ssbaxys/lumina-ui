package io.github.ssbaxys.lumina.markdown;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Книга: Markdown-текст, разбитый на страницы, с перелистыванием, счётчиком страниц и оглавлением.
 *
 * <ul>
 *   <li>Разрыв страницы — строка {@code <!-- page -->} или {@code \newpage}. Текст, который не влез
 *       на страницу, сам переносится на следующую (длинные абзацы режутся по словам).</li>
 *   <li>{@link Look#VANILLA} — как книга Minecraft: текстура {@code textures/gui/book.png}, чёрный текст,
 *       «Страница 1 из 3» и стрелки. {@link Look#MODERN} — разворот из двух страниц на карточке темы.</li>
 *   <li>Оглавление ({@link #tableOfContents}) собирается из заголовков и кладётся первой страницей;
 *       пункты — ссылки. Ссылки {@code [текст](#page:5)} и {@code [текст](#заголовок)} тоже листают книгу.</li>
 *   <li>Листать: стрелки, колесо, клавиши ←/→, PageUp/PageDown, Home/End.</li>
 * </ul>
 *
 * <pre>{@code
 * new Book("# Глава 1\nТекст…\n<!-- page -->\n# Глава 2\n…").look(Book.Look.MODERN).tableOfContents(true);
 * }</pre>
 *
 * <p>Стили: "book" (современный вид) и модификатор "book.vanilla".</p>
 */
public class Book extends Widget<Book> {
    public enum Look {
        /** Книга Minecraft: одна страница, текстура игры. */
        VANILLA,
        /** Разворот на карточке темы. */
        MODERN
    }

    /** Пункт оглавления: заголовок, его уровень и страница (с нуля). */
    public record Entry(String title, int level, int page) {
    }

    private record Page(List<Md.Block> blocks, MarkdownLayout layout) {
    }

    private static final Pattern BREAK = Pattern.compile("(?m)^[ \\t]*(?:<!--\\s*page\\s*-->|\\\\newpage|\\f)[ \\t]*$");
    private static final TextureRef VANILLA_TEXTURE = TextureRef.of("minecraft:textures/gui/book.png");
    private static final float VANILLA_SIZE = 192f;
    private static final float BLOCK_GAP = 6f;
    private static final float FOOTER = 14f;
    private static final float GUTTER = 18f;
    private static final int PAPER = 0xFFF5ECD4;
    private static final int VANILLA_TEXT = 0xFF000000;
    private static final int VANILLA_LINK = 0xFF2340A8;

    private String source = "";
    private List<List<Md.Block>> chunks = List.of();
    private final List<Page> pages = new ArrayList<>();
    private final List<Entry> toc = new ArrayList<>();
    private int tocPages;
    private Look look = Look.VANILLA;
    private Boolean spread;
    private boolean tableOfContents;
    private String tocTitle = "Содержание";
    private int tocLevel = 2;
    private Consumer<String> onLink;

    private final Signal<Integer> page = Signal.of(0);
    private final Signal<Integer> pageCount = Signal.of(1);
    private Subscription pageSub;
    private float layoutW = Float.NaN;
    private float layoutH = Float.NaN;
    private boolean dirty = true;

    private int shown;
    private int flipFrom;
    private int flipDir;
    private AnimatedFloat flip;
    private float flipDuration = 0.45f;
    private boolean sound = true;

    private int hoverArrow;
    private int hoverPageSlot = -1;
    private int hoverLink = -1;
    private int[] cachedPalette;
    private int paletteKey;

    public Book(String markdown) {
        super("book vanilla");
        setFocusable(true);
        text(markdown);
    }

    // ================================================================ настройка

    /** Текст книги; разрывы страниц — {@code <!-- page -->} или {@code \newpage}. */
    public Book text(String markdown) {
        String s = markdown == null ? "" : markdown;
        if (!s.equals(source)) {
            source = s;
            List<List<Md.Block>> parsed = new ArrayList<>();
            for (String part : BREAK.split(s, -1)) {
                parsed.add(MarkdownParser.parse(part));
            }
            chunks = parsed;
            dirty = true;
            invalidateLayout();
        }
        return this;
    }

    public String text() {
        return source;
    }

    public Book look(Look look) {
        this.look = look;
        setStyleClass(look == Look.VANILLA ? "book vanilla" : "book");
        dirty = true;
        invalidateLayout();
        return this;
    }

    public Look look() {
        return look;
    }

    /** Разворот из двух страниц (по умолчанию да) или одна страница. Только для современного вида. */
    public Book spread(boolean spread) {
        this.spread = spread;
        dirty = true;
        invalidateLayout();
        return this;
    }

    public boolean isSpread() {
        return look == Look.MODERN && (spread == null || spread);
    }

    /** Оглавление первой страницей (из заголовков до уровня {@link #tocLevel}). */
    public Book tableOfContents(boolean on) {
        this.tableOfContents = on;
        dirty = true;
        invalidateLayout();
        return this;
    }

    public Book tableOfContents(String title) {
        this.tocTitle = title;
        return tableOfContents(true);
    }

    /** До какого уровня заголовки попадают в оглавление (по умолчанию 2). */
    public Book tocLevel(int level) {
        this.tocLevel = Math.max(1, Math.min(6, level));
        dirty = true;
        invalidateLayout();
        return this;
    }

    /** Клик по внешней ссылке (ссылки {@code #…} книга обрабатывает сама). */
    public Book onLink(Consumer<String> handler) {
        this.onLink = handler;
        return this;
    }

    /** Длительность перелистывания в секундах. */
    public Book flipDuration(float seconds) {
        this.flipDuration = Math.max(0f, seconds);
        return this;
    }

    /** Звук перелистывания ({@code item.book.page_turn}). */
    public Book sound(boolean on) {
        this.sound = on;
        return this;
    }

    // ================================================================ страницы

    /** Текущая страница с нуля (в развороте — левая, всегда чётная). Можно задавать: {@code page().set(4)}. */
    public Signal<Integer> page() {
        return page;
    }

    public ReadableSignal<Integer> pageCount() {
        return pageCount;
    }

    /** Оглавление (страницы с учётом самого оглавления). Готово после первой раскладки. */
    public List<Entry> toc() {
        return List.copyOf(toc);
    }

    /** Сколько страниц занимает оглавление. */
    public int tocPageCount() {
        return tocPages;
    }

    /** Текст страницы без разметки (для проверок и озвучки). */
    public String pageText(int index) {
        if (index < 0 || index >= pages.size()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (MarkdownLayout.Run r : pages.get(index).layout().runs) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(r.text().replaceAll("§.", ""));
        }
        return sb.toString();
    }

    private int step() {
        return isSpread() ? 2 : 1;
    }

    private int normalize(int p) {
        int max = Math.max(0, pages.size() - 1);
        p = Math.max(0, Math.min(max, p));
        return isSpread() ? p - p % 2 : p;
    }

    public void next() {
        goTo(current() + step());
    }

    public void previous() {
        goTo(current() - step());
    }

    public void goTo(int index) {
        page.set(normalize(index));
    }

    public int current() {
        Integer p = page.peek();
        return p == null ? 0 : p;
    }

    public boolean canNext() {
        return current() + step() < pages.size();
    }

    public boolean canPrevious() {
        return current() > 0;
    }

    /** Идёт ли сейчас перелистывание. */
    public boolean isFlipping() {
        return flip != null && flip.isRunning();
    }

    // ================================================================ разбиение на страницы

    /** Прямоугольник страницы {@code slot} (0 — левая или единственная, 1 — правая): x, y, w, h. */
    private float[] pageRect(int slot) {
        float left = padding().left();
        float top = padding().top();
        float innerW = Math.max(1f, width() - padding().horizontal());
        float innerH = Math.max(1f, height() - padding().vertical());
        if (look == Look.VANILLA) {
            float bx = left + (innerW - VANILLA_SIZE) / 2f;
            float by = top + (innerH - VANILLA_SIZE) / 2f;
            return new float[]{bx + 36f, by + 30f, 114f, 128f};
        }
        float h = innerH - 20f - FOOTER;
        if (isSpread()) {
            float w = (innerW - 20f - GUTTER) / 2f;
            return new float[]{left + 10f + slot * (w + GUTTER), top + 10f, w, h};
        }
        return new float[]{left + 12f, top + 10f, innerW - 24f, h};
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        if (look == Look.VANILLA) {
            setContentSize(VANILLA_SIZE, VANILLA_SIZE);
        } else {
            setContentSize(isSpread() ? 340f : 200f, 220f);
        }
    }

    @Override
    protected void onLayout() {
        float[] r = pageRect(0);
        if (dirty || Float.compare(r[2], layoutW) != 0 || Float.compare(r[3], layoutH) != 0) {
            paginate(r[2], r[3]);
        }
    }

    private void paginate(float w, float h) {
        dirty = false;
        layoutW = w;
        layoutH = h;
        TextMetrics m = Lumina.platform().textMetrics();
        List<List<Md.Block>> content = new ArrayList<>();
        List<Entry> found = new ArrayList<>();
        for (List<Md.Block> chunk : chunks) {
            paginateBlocks(chunk, w, h, m, content, found);
        }
        // Оглавление занимает несколько страниц, а номера в нём от этого зависят — подгоняем.
        List<List<Md.Block>> tocContent = new ArrayList<>();
        int offset = 0;
        if (tableOfContents && !found.isEmpty()) {
            for (int attempt = 0; attempt < 3; attempt++) {
                tocContent.clear();
                paginateBlocks(MarkdownParser.parse(tocMarkdown(found, offset)), w, h, m, tocContent, null);
                if (isSpread() && tocContent.size() % 2 == 1) {
                    tocContent.add(List.of());
                }
                if (tocContent.size() == offset) {
                    break;
                }
                offset = tocContent.size();
            }
        }
        tocPages = tocContent.size();
        toc.clear();
        for (Entry e : found) {
            toc.add(new Entry(e.title(), e.level(), e.page() + tocPages));
        }
        pages.clear();
        for (List<Md.Block> blocks : tocContent) {
            pages.add(new Page(blocks, MarkdownLayout.build(blocks, w, m)));
        }
        for (List<Md.Block> blocks : content) {
            pages.add(new Page(blocks, MarkdownLayout.build(blocks, w, m)));
        }
        if (pages.isEmpty()) {
            pages.add(new Page(List.of(), MarkdownLayout.build(List.of(), w, m)));
        }
        pageCount.set(pages.size());
        int p = normalize(current());
        shown = p;
        if (p != current()) {
            page.set(p);
        }
    }

    private String tocMarkdown(List<Entry> entries, int offset) {
        StringBuilder sb = new StringBuilder("## ").append(tocTitle).append("\n\n");
        for (Entry e : entries) {
            sb.append("  ".repeat(Math.max(0, e.level() - 1)));
            sb.append("- [").append(escape(e.title())).append("](#page:").append(e.page() + offset + 1).append(")")
                    .append(" · ").append(e.page() + offset + 1).append('\n');
        }
        return sb.toString();
    }

    private static String escape(String s) {
        return s.replace("[", "\\[").replace("]", "\\]");
    }

    private static float heightOf(Md.Block b, float w, TextMetrics m) {
        return MarkdownLayout.build(List.of(b), w, m).height;
    }

    /** Раскладывает блоки по страницам высоты {@code h}; заголовки попадают в {@code entries}. */
    private void paginateBlocks(List<Md.Block> blocks, float w, float h, TextMetrics m,
                                List<List<Md.Block>> out, List<Entry> entries) {
        List<Md.Block> queue = new ArrayList<>(blocks);
        List<Md.Block> cur = new ArrayList<>();
        float used = 0f;
        int i = 0;
        while (i < queue.size()) {
            Md.Block b = queue.get(i);
            float gap = cur.isEmpty() ? 0f : BLOCK_GAP;
            float bh = heightOf(b, w, m);
            if (used + gap + bh <= h + 0.01f) {
                cur.add(b);
                used += gap + bh;
                if (entries != null && b instanceof Md.Heading hd && hd.level() <= tocLevel) {
                    entries.add(new Entry(plain(hd.content()), hd.level(), out.size()));
                }
                i++;
                continue;
            }
            if (b instanceof Md.Paragraph p) {
                Md.Paragraph[] parts = split(p, w, h - used - gap, m);
                if (parts != null) {
                    cur.add(parts[0]);
                    out.add(cur);
                    cur = new ArrayList<>();
                    used = 0f;
                    queue.set(i, parts[1]);
                    continue;
                }
            }
            if (!cur.isEmpty()) {
                out.add(cur);
                cur = new ArrayList<>();
                used = 0f;
                continue;
            }
            // Блок выше целой страницы и не режется: отдельная страница (низ обрежется).
            cur.add(b);
            out.add(cur);
            cur = new ArrayList<>();
            i++;
        }
        if (!cur.isEmpty() || out.isEmpty()) {
            out.add(cur);
        }
    }

    /**
     * Разрезать абзац так, чтобы начало влезло в {@code avail} по высоте: двоичный поиск по числу слов.
     * {@code null} — не влезает ни одно слово.
     */
    private static Md.Paragraph[] split(Md.Paragraph p, float w, float avail, TextMetrics m) {
        List<Object[]> units = new ArrayList<>();
        List<Md.Inline> content = p.content();
        for (int k = 0; k < content.size(); k++) {
            Md.Inline in = content.get(k);
            if (in instanceof Md.Text t) {
                String s = t.text();
                int start = 0;
                for (int j = 0; j < s.length(); j++) {
                    if (s.charAt(j) == ' ' && (j + 1 == s.length() || s.charAt(j + 1) != ' ')) {
                        units.add(new Object[]{k, s.substring(start, j + 1)});
                        start = j + 1;
                    }
                }
                if (start < s.length()) {
                    units.add(new Object[]{k, s.substring(start)});
                }
            } else {
                units.add(new Object[]{k, null});
            }
        }
        if (units.size() < 2 || avail <= 0f) {
            return null;
        }
        int lo = 0;
        int hi = units.size() - 1;
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (heightOf(new Md.Paragraph(join(content, units, 0, mid)), w, m) <= avail + 0.01f) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        if (lo == 0) {
            return null;
        }
        List<Md.Inline> tail = join(content, units, lo, units.size());
        if (!tail.isEmpty() && tail.get(0) instanceof Md.Text t) {
            tail.set(0, new Md.Text(t.text().stripLeading(), t.flags()));
        }
        return new Md.Paragraph[]{new Md.Paragraph(join(content, units, 0, lo)), new Md.Paragraph(tail)};
    }

    /** Собрать строчные элементы из кусков {@code [from, to)}: слова одного Text склеиваются обратно. */
    private static List<Md.Inline> join(List<Md.Inline> content, List<Object[]> units, int from, int to) {
        List<Md.Inline> out = new ArrayList<>();
        int lastIndex = -1;
        StringBuilder text = null;
        for (int u = from; u < to; u++) {
            int k = (Integer) units.get(u)[0];
            String piece = (String) units.get(u)[1];
            if (piece != null && k == lastIndex && text != null) {
                text.append(piece);
                continue;
            }
            flush(content, out, lastIndex, text);
            text = null;
            lastIndex = k;
            if (piece != null) {
                text = new StringBuilder(piece);
            } else {
                out.add(content.get(k));
            }
        }
        flush(content, out, lastIndex, text);
        return out;
    }

    private static void flush(List<Md.Inline> content, List<Md.Inline> out, int index, StringBuilder text) {
        if (text != null && index >= 0) {
            out.add(new Md.Text(text.toString(), ((Md.Text) content.get(index)).flags()));
        }
    }

    private static String plain(List<Md.Inline> content) {
        StringBuilder sb = new StringBuilder();
        for (Md.Inline in : content) {
            if (in instanceof Md.Text t) {
                sb.append(t.text());
            } else if (in instanceof Md.Code c) {
                sb.append(c.code());
            } else if (in instanceof Md.Link l) {
                sb.append(plain(l.content()));
            }
        }
        return sb.toString().strip();
    }

    /** Якорь заголовка: нижний регистр, пробелы — дефисы ({@code #глава-1}). */
    static String slug(String title) {
        return title.toLowerCase(Locale.ROOT).strip().replaceAll("[^\\p{L}\\p{N}]+", "-").replaceAll("(^-|-$)", "");
    }

    /** Ссылка внутри книги: {@code #page:N} (с единицы) или {@code #якорь-заголовка}. */
    private boolean followInternal(String url) {
        if (!url.startsWith("#")) {
            return false;
        }
        String target = url.substring(1);
        if (target.startsWith("page:")) {
            try {
                goTo(Integer.parseInt(target.substring(5).strip()) - 1);
            } catch (NumberFormatException ignored) {
                // неверный номер — ссылка ничего не делает
            }
            return true;
        }
        String want = slug(target);
        for (Entry e : toc) {
            if (slug(e.title()).equals(want)) {
                goTo(e.page());
                return true;
            }
        }
        return true;
    }

    // ================================================================ жизненный цикл

    @Override
    protected void onAttach() {
        flip = new AnimatedFloat(root().animator(), 1f).tween(flipDuration, Easing.IN_OUT_CUBIC);
        shown = current();
        pageSub = page.subscribe(p -> startFlip());
    }

    @Override
    protected void onDetach() {
        if (pageSub != null) {
            pageSub.close();
            pageSub = null;
        }
        if (flip != null) {
            flip.snapTo(1f);
        }
    }

    private void startFlip() {
        int target = normalize(current());
        if (target != current()) {
            page.set(target);
            return;
        }
        if (target == shown) {
            return;
        }
        flipFrom = shown;
        flipDir = target > shown ? 1 : -1;
        shown = target;
        hoverLink = -1;
        if (sound && Lumina.isInitialized()) {
            Lumina.platform().playSound("minecraft:item.book.page_turn", 1f, 1f);
        }
        if (flip != null && flipDuration > 0f) {
            flip.tween(flipDuration, Easing.IN_OUT_CUBIC);
            flip.snapTo(0f);
            flip.animateTo(1f);
        }
    }

    // ================================================================ ввод

    /** Стрелки: -1 — назад, 1 — вперёд; прямоугольники x, y, w, h. */
    private float[] arrowRect(int dir) {
        if (look == Look.VANILLA) {
            float innerW = width() - padding().horizontal();
            float innerH = height() - padding().vertical();
            float bx = padding().left() + (innerW - VANILLA_SIZE) / 2f;
            float by = padding().top() + (innerH - VANILLA_SIZE) / 2f;
            return dir > 0 ? new float[]{bx + 116f, by + 156f, 23f, 13f} : new float[]{bx + 43f, by + 156f, 23f, 13f};
        }
        float y = height() - padding().bottom() - FOOTER - 4f;
        return dir > 0 ? new float[]{width() - padding().right() - 24f, y, 16f, 16f}
                : new float[]{padding().left() + 8f, y, 16f, 16f};
    }

    private static boolean inside(float[] r, float x, float y) {
        return x >= r[0] && y >= r[1] && x < r[0] + r[2] && y < r[1] + r[3];
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.MOVE) {
            PointerEvent p = (PointerEvent) event;
            updateHover(p.localX(), p.localY());
        } else if (event.type() == PointerEvent.LEAVE) {
            hoverArrow = 0;
            hoverLink = -1;
            hoverPageSlot = -1;
        } else if (event.type() == PointerEvent.CLICK && ((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
            PointerEvent p = (PointerEvent) event;
            float lx = p.localX();
            float ly = p.localY();
            if (canNext() && inside(arrowRect(1), lx, ly)) {
                next();
                event.consume();
                return;
            }
            if (canPrevious() && inside(arrowRect(-1), lx, ly)) {
                previous();
                event.consume();
                return;
            }
            updateHover(lx, ly);
            if (hoverLink >= 0 && hoverPageSlot >= 0 && !isFlipping()) {
                MarkdownLayout l = pages.get(current() + hoverPageSlot).layout();
                String url = l.links.get(hoverLink);
                if (!followInternal(url) && onLink != null) {
                    onLink.accept(url);
                }
                event.consume();
            }
        } else if (event.type() == ScrollEvent.SCROLL) {
            float dy = ((ScrollEvent) event).deltaY();
            if (dy < 0f && canNext()) {
                next();
            } else if (dy > 0f && canPrevious()) {
                previous();
            }
            event.consume();
            event.stopPropagation();
        } else if (event.type() == KeyEvent.PRESS && event.target() == this) {
            switch (((KeyEvent) event).key()) {
                case Keys.RIGHT, Keys.PAGE_DOWN -> next();
                case Keys.LEFT, Keys.PAGE_UP -> previous();
                case Keys.HOME -> goTo(0);
                case Keys.END -> goTo(pages.size() - 1);
                default -> {
                    return;
                }
            }
            event.consume();
        }
    }

    private void updateHover(float lx, float ly) {
        hoverArrow = canNext() && inside(arrowRect(1), lx, ly) ? 1 : canPrevious() && inside(arrowRect(-1), lx, ly) ? -1 : 0;
        hoverLink = -1;
        hoverPageSlot = -1;
        int slots = isSpread() ? 2 : 1;
        for (int s = 0; s < slots; s++) {
            int index = current() + s;
            float[] r = pageRect(s);
            if (index < pages.size() && inside(r, lx, ly)) {
                int link = pages.get(index).layout().linkAt(lx - r[0], ly - r[1]);
                if (link >= 0) {
                    hoverLink = link;
                    hoverPageSlot = s;
                }
            }
        }
    }

    // ================================================================ отрисовка

    private int[] palette() {
        int text;
        int accent;
        Theme theme = null;
        if (look == Look.VANILLA) {
            text = VANILLA_TEXT;
            accent = VANILLA_LINK;
        } else {
            text = style.get(Styles.TEXT_COLOR);
            accent = style.get(Styles.ACCENT_COLOR);
            theme = root() != null ? root().theme() : null;
        }
        int key = text * 31 + accent + (theme != null ? theme.revision() * 961 : 0);
        if (cachedPalette == null || key != paletteKey) {
            cachedPalette = Markdown.palette(text, accent, theme);
            if (look == Look.VANILLA) {
                cachedPalette[MarkdownLayout.LINK] = VANILLA_LINK;
                cachedPalette[MarkdownLayout.CODE_BG] = 0x18000000;
                cachedPalette[MarkdownLayout.CODE] = 0xFF7A3E00;
            }
            paletteKey = key;
        }
        return cachedPalette;
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        if (pages.isEmpty()) {
            return;
        }
        int[] pal = palette();
        boolean shadow = look != Look.VANILLA && style.get(Styles.TEXT_SHADOW);
        if (look == Look.VANILLA) {
            paintVanillaFrame(canvas);
        } else {
            paintModernFrame(canvas, pal);
        }
        float t = flip != null ? flip.get() : 1f;
        boolean flipping = t < 1f && flipFrom != shown;
        int cur = shown;
        if (!isSpread()) {
            float[] r = pageRect(0);
            if (!flipping) {
                paintPage(canvas, cur, r, pal, shadow, 0);
            } else if (flipDir > 0) {
                // Вперёд: под листом уже новая страница, старый лист складывается к корешку слева.
                paintPage(canvas, cur, r, pal, shadow, -1);
                paintSheet(canvas, flipFrom, r, pal, shadow, r[0] - 2f, 1f - t, false);
            } else {
                paintPage(canvas, flipFrom, r, pal, shadow, -1);
                paintSheet(canvas, cur, r, pal, shadow, r[0] - 2f, t, false);
            }
        } else {
            float[] l = pageRect(0);
            float[] r = pageRect(1);
            float spine = (l[0] + l[2] + r[0]) / 2f;
            if (!flipping) {
                paintPage(canvas, cur, l, pal, shadow, 0);
                paintPage(canvas, cur + 1, r, pal, shadow, 1);
            } else if (flipDir > 0) {
                paintPage(canvas, flipFrom, l, pal, shadow, -1);
                paintPage(canvas, cur + 1, r, pal, shadow, -1);
                if (t < 0.5f) {
                    paintSheet(canvas, flipFrom + 1, r, pal, shadow, spine, 1f - 2f * t, false);
                } else {
                    paintSheet(canvas, cur, l, pal, shadow, spine, 2f * t - 1f, true);
                }
            } else {
                paintPage(canvas, cur, l, pal, shadow, -1);
                paintPage(canvas, flipFrom + 1, r, pal, shadow, -1);
                if (t < 0.5f) {
                    paintSheet(canvas, flipFrom, l, pal, shadow, spine, 1f - 2f * t, true);
                } else {
                    paintSheet(canvas, cur + 1, r, pal, shadow, spine, 2f * t - 1f, false);
                }
            }
        }
        paintChrome(canvas, pal);
    }

    /** Содержимое страницы в её прямоугольнике (с отсечением). {@code slot} — для подсветки ссылки, -1 — без. */
    private void paintPage(Canvas canvas, int index, float[] r, int[] pal, boolean shadow, int slot) {
        if (index < 0 || index >= pages.size()) {
            return;
        }
        canvas.save();
        canvas.clipRect(r[0], r[1], r[2], r[3]);
        canvas.translate(r[0], r[1]);
        pages.get(index).layout().paint(canvas, pal, shadow, slot >= 0 && slot == hoverPageSlot ? hoverLink : -1);
        canvas.restore();
    }

    /**
     * Перелистываемый лист: страница {@code index}, сжатая по горизонтали до {@code scale} и прижатая
     * к корешку {@code anchor} ({@code leftOfAnchor} — лист лежит слева от корешка). Чем уже лист,
     * тем темнее тень на нём.
     */
    private void paintSheet(Canvas canvas, int index, float[] r, int[] pal, boolean shadow, float anchor,
                            float scale, boolean leftOfAnchor) {
        if (scale <= 0.01f) {
            return;
        }
        float pad = 6f;
        float w = r[2] + 2f * pad;
        float h = r[3] + 2f * pad;
        float x = leftOfAnchor ? anchor - w * scale : anchor;
        canvas.save();
        canvas.translate(x, r[1] - pad);
        canvas.scale(scale, 1f);
        int paper = look == Look.VANILLA ? PAPER : paperColor();
        canvas.fillRoundRect(0f, 0f, w, h, Radii.all(3f), Brush.solid(paper));
        canvas.save();
        canvas.clipRect(pad, pad, r[2], r[3]);
        canvas.translate(pad, pad);
        if (index >= 0 && index < pages.size()) {
            pages.get(index).layout().paint(canvas, pal, shadow, -1);
        }
        canvas.restore();
        float dark = (1f - scale) * 0.45f;
        int shade = Color.withAlpha(Color.BLACK, dark);
        int clear = Color.withAlpha(Color.BLACK, 0f);
        canvas.fillRect(0f, 0f, w, h, leftOfAnchor ? Brush.horizontal(clear, shade) : Brush.horizontal(shade, clear));
        canvas.restore();
    }

    private int paperColor() {
        Brush bg = style.get(Styles.BACKGROUND);
        int text = style.get(Styles.TEXT_COLOR);
        // Лист чуть светлее (или темнее) фона книги, чтобы отличаться от него.
        int base = bg instanceof Brush.Solid s ? s.color() : Color.luminance(text) > 0.5f ? 0xFF262A36 : 0xFFF7F8FB;
        return Color.alpha(base) < 0xF0 ? Color.withAlpha(base, 0xFF) : base;
    }

    private void paintVanillaFrame(Canvas canvas) {
        float innerW = width() - padding().horizontal();
        float innerH = height() - padding().vertical();
        float bx = padding().left() + (innerW - VANILLA_SIZE) / 2f;
        float by = padding().top() + (innerH - VANILLA_SIZE) / 2f;
        // Книга занимает левый верхний квадрат 192×192 текстуры 256×256.
        float k = VANILLA_SIZE / 256f;
        canvas.drawTexture(VANILLA_TEXTURE, bx, by, VANILLA_SIZE, VANILLA_SIZE, 0f, 0f, k, k, Color.WHITE);
    }

    private void paintModernFrame(Canvas canvas, int[] pal) {
        float[] l = pageRect(0);
        int text = pal[MarkdownLayout.TEXT];
        if (isSpread()) {
            float[] r = pageRect(1);
            float spine = (l[0] + l[2] + r[0]) / 2f;
            float top = l[1] - 4f;
            float h = l[3] + 8f;
            canvas.fillRect(spine - 10f, top, 10f, h,
                    Brush.horizontal(Color.withAlpha(Color.BLACK, 0f), Color.withAlpha(Color.BLACK, 0.18f)));
            canvas.fillRect(spine, top, 10f, h,
                    Brush.horizontal(Color.withAlpha(Color.BLACK, 0.18f), Color.withAlpha(Color.BLACK, 0f)));
            canvas.fillRect(spine - 0.5f, top, 1f, h, Color.withAlpha(text, 0x30));
        }
    }

    /** Счётчик страниц и стрелки. */
    private void paintChrome(Canvas canvas, int[] pal) {
        TextMetrics m = canvas.textMetrics();
        int total = pages.size();
        int cur = shown;
        if (look == Look.VANILLA) {
            float innerW = width() - padding().horizontal();
            float innerH = height() - padding().vertical();
            float bx = padding().left() + (innerW - VANILLA_SIZE) / 2f;
            float by = padding().top() + (innerH - VANILLA_SIZE) / 2f;
            String counter = "Страница " + (cur + 1) + " из " + total;
            float right = bx + VANILLA_SIZE - 44f;
            canvas.drawText(counter, right - m.width(counter), by + 16f, VANILLA_TEXT, false);
            if (canNext()) {
                paintVanillaArrow(canvas, arrowRect(1), 1, hoverArrow == 1);
            }
            if (canPrevious()) {
                paintVanillaArrow(canvas, arrowRect(-1), -1, hoverArrow == -1);
            }
            return;
        }
        int text = pal[MarkdownLayout.TEXT];
        int muted = pal[MarkdownLayout.MUTED];
        int accent = pal[MarkdownLayout.ACCENT];
        float fy = height() - padding().bottom() - FOOTER;
        if (isSpread()) {
            float[] l = pageRect(0);
            float[] r = pageRect(1);
            String a = String.valueOf(cur + 1);
            canvas.drawText(a, l[0] + (l[2] - m.width(a)) / 2f, fy, muted, false);
            if (cur + 1 < total) {
                String b = String.valueOf(cur + 2);
                canvas.drawText(b, r[0] + (r[2] - m.width(b)) / 2f, fy, muted, false);
            }
        } else {
            String counter = (cur + 1) + " / " + total;
            canvas.drawText(counter, (width() - m.width(counter)) / 2f, fy, muted, false);
        }
        for (int dir = -1; dir <= 1; dir += 2) {
            boolean enabled = dir > 0 ? canNext() : canPrevious();
            float[] ar = arrowRect(dir);
            float cx = ar[0] + ar[2] / 2f;
            float cy = ar[1] + ar[3] / 2f;
            if (enabled && hoverArrow == dir) {
                canvas.fillCircle(cx, cy, ar[2] / 2f + 1f, Brush.solid(Color.withAlpha(accent, 0x40)));
            }
            int c = enabled ? (hoverArrow == dir ? text : Color.mulAlpha(text, 0.75f)) : Color.mulAlpha(text, 0.2f);
            float d = 3f * dir;
            canvas.drawLine(cx - d, cy - 4f, cx + d, cy, 1.5f, c);
            canvas.drawLine(cx + d, cy, cx - d, cy + 4f, 1.5f, c);
        }
    }

    /** Стрелка в духе ванильной книги: древко и наконечник, при наведении светлее. */
    private static void paintVanillaArrow(Canvas canvas, float[] r, int dir, boolean hover) {
        int body = hover ? 0xFFC8A464 : 0xFF8C6239;
        int edge = hover ? 0xFF6B4A1E : 0xFF4A2F16;
        float cy = r[1] + r[3] / 2f;
        float tipX = dir > 0 ? r[0] + r[2] - 2f : r[0] + 2f;
        float baseX = dir > 0 ? r[0] + r[2] - 10f : r[0] + 10f;
        float tailX = dir > 0 ? r[0] + 3f : r[0] + r[2] - 3f;
        canvas.fillRect(Math.min(tailX, baseX), cy - 2f, Math.abs(baseX - tailX), 4f, edge);
        canvas.fillRect(Math.min(tailX, baseX), cy - 1f, Math.abs(baseX - tailX), 2f, body);
        canvas.fillTriangle(tipX, cy, baseX, cy - 6f, baseX, cy + 6f, edge);
        float inset = dir > 0 ? -2f : 2f;
        canvas.fillTriangle(tipX + inset, cy, baseX - inset * 0.5f, cy - 4f, baseX - inset * 0.5f, cy + 4f, body);
    }
}
