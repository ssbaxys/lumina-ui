package io.github.ssbaxys.lumina.markdown;

import io.github.ssbaxys.lumina.markdown.Md.Block;
import io.github.ssbaxys.lumina.markdown.Md.Inline;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Разбор Markdown (CommonMark + расширения GitHub): заголовки (# и подчёркивание), абзацы, жёсткие
 * переносы, **жирный**, *курсив*, ~~зачёркнутый~~, ==выделение==, ++подчёркнутый++, `код`, блоки кода ``` и с отступом,
 * цитаты, вложенные списки (маркированные, нумерованные, задачи [ ] / [x]), таблицы с выравниванием,
 * горизонтальные линии, ссылки [текст](url), автоссылки &lt;url&gt;, картинки ![alt](url), экранирование \*.
 */
public final class MarkdownParser {
    private static final Pattern HEADING = Pattern.compile("^ {0,3}(#{1,6})(?:[ \\t]+(.*?))?(?:[ \\t]+#+)?[ \\t]*$");
    private static final Pattern FENCE = Pattern.compile("^( {0,3})(`{3,}|~{3,})\\s*([^`\\s]*).*$");
    private static final Pattern RULE = Pattern.compile("^ {0,3}((\\*[ \\t]*){3,}|(-[ \\t]*){3,}|(_[ \\t]*){3,})$");
    private static final Pattern BULLET = Pattern.compile("^( *)([-+*])([ \\t]+|$)(.*)$");
    private static final Pattern ORDERED = Pattern.compile("^( *)(\\d{1,9})([.)])([ \\t]+|$)(.*)$");
    private static final Pattern TASK = Pattern.compile("^\\[([ xX])][ \\t]+(.*)$");
    private static final Pattern TABLE_DELIM = Pattern.compile("^ {0,3}\\|?\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)*\\|?\\s*$");
    private static final Pattern SETEXT = Pattern.compile("^ {0,3}(=+|-+)[ \\t]*$");

    private MarkdownParser() {
    }

    public static List<Block> parse(String source) {
        String[] lines = source.replace("\r\n", "\n").replace('\r', '\n').replace("\t", "    ").split("\n", -1);
        return new MarkdownParser().blocks(lines, 0, lines.length);
    }

    // ================================================================ блоки

    private List<Block> blocks(String[] lines, int from, int to) {
        List<Block> out = new ArrayList<>();
        int i = from;
        while (i < to) {
            String line = lines[i];
            if (line.isBlank()) {
                i++;
                continue;
            }
            Matcher m;
            if ((m = FENCE.matcher(line)).matches()) {
                String fence = m.group(2);
                int indent = m.group(1).length();
                StringBuilder code = new StringBuilder();
                int j = i + 1;
                while (j < to && !lines[j].stripLeading().startsWith(fence)) {
                    if (code.length() > 0) {
                        code.append('\n');
                    }
                    code.append(stripIndent(lines[j], indent));
                    j++;
                }
                out.add(new Md.CodeBlock(m.group(3), code.toString()));
                i = Math.min(j + 1, to);
                continue;
            }
            if ((m = HEADING.matcher(line)).matches()) {
                String text = m.group(2) == null ? "" : m.group(2);
                out.add(new Md.Heading(m.group(1).length(), inline(text)));
                i++;
                continue;
            }
            if (RULE.matcher(line).matches()) {
                out.add(new Md.Rule());
                i++;
                continue;
            }
            if (line.stripLeading().startsWith(">")) {
                List<String> inner = new ArrayList<>();
                int j = i;
                while (j < to && !lines[j].isBlank()) {
                    String l = lines[j].stripLeading();
                    if (l.startsWith(">")) {
                        l = l.substring(1);
                        if (l.startsWith(" ")) {
                            l = l.substring(1);
                        }
                    } else if (inner.isEmpty()) {
                        break;
                    }
                    inner.add(l);
                    j++;
                }
                String[] a = inner.toArray(new String[0]);
                out.add(new Md.Quote(blocks(a, 0, a.length)));
                i = j;
                continue;
            }
            if (BULLET.matcher(line).matches() || ORDERED.matcher(line).matches()) {
                i = list(lines, i, to, out);
                continue;
            }
            if (line.startsWith("    ")) {
                StringBuilder code = new StringBuilder();
                int j = i;
                while (j < to && (lines[j].startsWith("    ") || lines[j].isBlank())) {
                    if (code.length() > 0) {
                        code.append('\n');
                    }
                    code.append(lines[j].length() >= 4 ? lines[j].substring(4) : "");
                    j++;
                }
                out.add(new Md.CodeBlock("", code.toString().stripTrailing()));
                i = j;
                continue;
            }
            if (line.contains("|") && i + 1 < to && TABLE_DELIM.matcher(lines[i + 1]).matches()) {
                i = table(lines, i, to, out);
                continue;
            }
            // Абзац: до пустой строки или начала другого блока.
            StringBuilder para = new StringBuilder(paragraphLine(line));
            boolean hardBreak = endsWithHardBreak(line);
            int j = i + 1;
            while (j < to) {
                String l = lines[j];
                if (l.isBlank()) {
                    break;
                }
                if ((m = SETEXT.matcher(l)).matches()) {
                    int level = m.group(1).charAt(0) == '=' ? 1 : 2;
                    out.add(new Md.Heading(level, inline(para.toString())));
                    para = null;
                    j++;
                    break;
                }
                if (startsBlock(l)) {
                    break;
                }
                para.append(hardBreak ? "\n" : " ").append(paragraphLine(l));
                hardBreak = endsWithHardBreak(l);
                j++;
            }
            if (para != null) {
                out.add(new Md.Paragraph(inline(para.toString())));
            }
            i = j;
        }
        return out;
    }

    private static String paragraphLine(String line) {
        String s = line.strip();
        return s.endsWith("\\") ? s.substring(0, s.length() - 1) : s;
    }

    private static boolean endsWithHardBreak(String line) {
        return line.endsWith("  ") || line.endsWith("\\");
    }

    private static boolean startsBlock(String l) {
        return FENCE.matcher(l).matches() || HEADING.matcher(l).matches() || RULE.matcher(l).matches()
                || l.stripLeading().startsWith(">") || BULLET.matcher(l).matches() || ORDERED.matcher(l).matches();
    }

    private static String stripIndent(String s, int n) {
        int i = 0;
        while (i < n && i < s.length() && s.charAt(i) == ' ') {
            i++;
        }
        return s.substring(i);
    }

    private int list(String[] lines, int from, int to, List<Block> out) {
        Matcher first = BULLET.matcher(lines[from]);
        boolean ordered = !first.matches();
        if (ordered) {
            first = ORDERED.matcher(lines[from]);
            first.matches();
        }
        int baseIndent = first.group(1).length();
        int start = ordered ? Integer.parseInt(first.group(2)) : 1;
        String marker = ordered ? first.group(3) : first.group(2);
        List<Md.ListItem> items = new ArrayList<>();
        int i = from;
        while (i < to) {
            Matcher m = ordered ? ORDERED.matcher(lines[i]) : BULLET.matcher(lines[i]);
            if (!m.matches() || m.group(1).length() != baseIndent
                    || !(ordered ? m.group(3) : m.group(2)).equals(marker)) {
                break;
            }
            int contentIndent = m.group(1).length() + (ordered ? m.group(2).length() + 1 : 1)
                    + Math.max(1, Math.min(m.group(ordered ? 4 : 3).length(), 4));
            List<String> body = new ArrayList<>();
            body.add(m.group(ordered ? 5 : 4));
            int j = i + 1;
            boolean blank = false;
            while (j < to) {
                String l = lines[j];
                if (l.isBlank()) {
                    blank = true;
                    body.add("");
                    j++;
                    continue;
                }
                int indent = l.length() - l.stripLeading().length();
                if (indent >= contentIndent || (indent > baseIndent && (BULLET.matcher(l).matches() || ORDERED.matcher(l).matches()))) {
                    body.add(l.substring(Math.min(indent, contentIndent)));
                    blank = false;
                    j++;
                } else if (!blank && indent > baseIndent - 1 && !startsBlock(l)) {
                    body.add(l.strip()); // ленивое продолжение абзаца
                    j++;
                } else {
                    break;
                }
            }
            while (!body.isEmpty() && body.get(body.size() - 1).isBlank()) {
                body.remove(body.size() - 1);
            }
            Boolean checked = null;
            Matcher task = TASK.matcher(body.get(0));
            if (task.matches()) {
                checked = !task.group(1).equals(" ");
                body.set(0, task.group(2));
            }
            String[] a = body.toArray(new String[0]);
            items.add(new Md.ListItem(blocks(a, 0, a.length), checked));
            i = j;
            if (i < to && lines[i - 1].isBlank()) {
                Matcher next = ordered ? ORDERED.matcher(lines[i]) : BULLET.matcher(lines[i]);
                if (!next.matches()) {
                    break;
                }
            }
        }
        out.add(new Md.ListBlock(ordered, start, items));
        return i;
    }

    private int table(String[] lines, int from, int to, List<Block> out) {
        List<String> head = cells(lines[from]);
        List<String> delim = cells(lines[from + 1]);
        List<Md.Align> aligns = new ArrayList<>();
        for (int c = 0; c < head.size(); c++) {
            String d = c < delim.size() ? delim.get(c).strip() : "";
            boolean left = d.startsWith(":");
            boolean right = d.endsWith(":");
            aligns.add(left && right ? Md.Align.CENTER : right ? Md.Align.RIGHT : Md.Align.LEFT);
        }
        List<List<Inline>> header = new ArrayList<>();
        for (String h : head) {
            header.add(inline(h));
        }
        List<List<List<Inline>>> rows = new ArrayList<>();
        int i = from + 2;
        while (i < to && !lines[i].isBlank() && lines[i].contains("|")) {
            List<String> cs = cells(lines[i]);
            List<List<Inline>> row = new ArrayList<>();
            for (int c = 0; c < head.size(); c++) {
                row.add(inline(c < cs.size() ? cs.get(c) : ""));
            }
            rows.add(row);
            i++;
        }
        out.add(new Md.Table(header, aligns, rows));
        return i;
    }

    private static List<String> cells(String line) {
        String s = line.strip();
        if (s.startsWith("|")) {
            s = s.substring(1);
        }
        if (s.endsWith("|") && !s.endsWith("\\|")) {
            s = s.substring(0, s.length() - 1);
        }
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length() && s.charAt(i + 1) == '|') {
                cur.append('|');
                i++;
            } else if (c == '|') {
                out.add(cur.toString().strip());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString().strip());
        return out;
    }

    // ================================================================ строчные элементы

    public static List<Inline> inline(String text) {
        List<Inline> out = new ArrayList<>();
        inline(text, 0, out);
        return out;
    }

    private static void inline(String s, int flags, List<Inline> out) {
        StringBuilder buf = new StringBuilder();
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < n && isPunct(s.charAt(i + 1))) {
                buf.append(s.charAt(i + 1));
                i += 2;
                continue;
            }
            if (c == '\n') {
                flush(buf, flags, out);
                out.add(new Md.LineBreak());
                i++;
                continue;
            }
            if (c == '`') {
                int ticks = run(s, i, '`');
                int close = s.indexOf("`".repeat(ticks), i + ticks);
                if (close > 0) {
                    flush(buf, flags, out);
                    out.add(new Md.Code(s.substring(i + ticks, close).strip()));
                    i = close + ticks;
                    continue;
                }
            }
            if (c == '!' && i + 1 < n && s.charAt(i + 1) == '[') {
                int[] link = link(s, i + 1);
                if (link != null) {
                    flush(buf, flags, out);
                    out.add(new Md.Image(s.substring(i + 2, link[0]), s.substring(link[1], link[2]).strip()));
                    i = link[2] + 1;
                    continue;
                }
            }
            if (c == '[') {
                int[] link = link(s, i);
                if (link != null) {
                    flush(buf, flags, out);
                    List<Inline> content = new ArrayList<>();
                    inline(s.substring(i + 1, link[0]), flags, content);
                    out.add(new Md.Link(s.substring(link[1], link[2]).strip(), content));
                    i = link[2] + 1;
                    continue;
                }
            }
            if (c == '<') {
                int close = s.indexOf('>', i);
                if (close > 0) {
                    String url = s.substring(i + 1, close);
                    if (url.matches("(?i)(https?|ftp|mailto):[^\\s<>]+")) {
                        flush(buf, flags, out);
                        out.add(new Md.Link(url, List.of(new Md.Text(url, flags))));
                        i = close + 1;
                        continue;
                    }
                }
            }
            int span = emphasis(s, i, flags, buf, out);
            if (span > 0) {
                i = span;
                continue;
            }
            buf.append(c);
            i++;
        }
        flush(buf, flags, out);
    }

    /** Пробует разобрать выделение в позиции i; возвращает позицию после него или -1. */
    private static int emphasis(String s, int i, int flags, StringBuilder buf, List<Inline> out) {
        char c = s.charAt(i);
        String delim;
        int add;
        if (s.startsWith("***", i) || s.startsWith("___", i)) {
            delim = s.substring(i, i + 3);
            add = Md.BOLD | Md.ITALIC;
        } else if (s.startsWith("**", i) || s.startsWith("__", i)) {
            delim = s.substring(i, i + 2);
            add = Md.BOLD;
        } else if (s.startsWith("~~", i)) {
            delim = "~~";
            add = Md.STRIKE;
        } else if (s.startsWith("==", i)) {
            delim = "==";
            add = Md.HIGHLIGHT;
        } else if (s.startsWith("++", i)) {
            delim = "++";
            add = Md.UNDERLINE;
        } else if (c == '*' || c == '_') {
            delim = String.valueOf(c);
            add = Md.ITALIC;
        } else {
            return -1;
        }
        int open = i + delim.length();
        if (open >= s.length() || Character.isWhitespace(s.charAt(open))) {
            return -1;
        }
        // "_" внутри слова (snake_case) — не выделение.
        if (c == '_' && i > 0 && Character.isLetterOrDigit(s.charAt(i - 1))) {
            return -1;
        }
        int close = findCloser(s, open, delim);
        if (close < 0) {
            return -1;
        }
        flush(buf, flags, out);
        inline(s.substring(open, close), flags | add, out);
        return close + delim.length();
    }

    private static int findCloser(String s, int from, String delim) {
        int i = from + 1;
        while (i <= s.length() - delim.length()) {
            char ch = s.charAt(i);
            if (ch == '\\') {
                i += 2;
                continue;
            }
            if (ch == '`') {
                int ticks = run(s, i, '`');
                int close = s.indexOf("`".repeat(ticks), i + ticks);
                if (close > 0) {
                    i = close + ticks;
                    continue;
                }
            }
            if (s.startsWith(delim, i) && !Character.isWhitespace(s.charAt(i - 1))) {
                boolean single = delim.length() == 1;
                // Одиночный * не должен быть частью ** (иначе "*a **b** c*" закроется рано).
                boolean partOfDouble = single && i + 1 < s.length() && s.charAt(i + 1) == delim.charAt(0);
                boolean wordUnderscore = delim.charAt(0) == '_' && i + delim.length() < s.length()
                        && Character.isLetterOrDigit(s.charAt(i + delim.length()));
                if (!partOfDouble && !wordUnderscore) {
                    return i;
                }
                if (partOfDouble) {
                    i += 2;
                    continue;
                }
            }
            i++;
        }
        return -1;
    }

    /** [text](url): {конец текста, начало url, конец url} или null. */
    private static int[] link(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++;
            } else if (c == '[') {
                depth++;
            } else if (c == ']') {
                depth--;
                if (depth == 0) {
                    if (i + 1 < s.length() && s.charAt(i + 1) == '(') {
                        int close = s.indexOf(')', i + 2);
                        if (close > 0) {
                            return new int[]{i, i + 2, close};
                        }
                    }
                    return null;
                }
            }
        }
        return null;
    }

    private static int run(String s, int i, char c) {
        int n = 0;
        while (i + n < s.length() && s.charAt(i + n) == c) {
            n++;
        }
        return n;
    }

    private static boolean isPunct(char c) {
        return "\\`*_{}[]()#+-.!|~=<>\"'".indexOf(c) >= 0;
    }

    private static void flush(StringBuilder buf, int flags, List<Inline> out) {
        if (buf.length() > 0) {
            out.add(new Md.Text(buf.toString(), flags));
            buf.setLength(0);
        }
    }
}
