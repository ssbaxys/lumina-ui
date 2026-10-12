package io.github.ssbaxys.lumina.markdown;

import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.ThemeVar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Подсветка синтаксиса для блоков кода Markdown ({@code ```java}). Встроены java, kotlin, javascript (js, ts),
 * json, css и html (xml, svg); свои языки — {@link #register}. Разбор построчный с переносом состояния
 * между строками (многострочные комментарии, текстовые блоки), выполняется один раз при раскладке.
 *
 * <p>Цвета зависят от темы: по умолчанию палитра подбирается под светлый или тёмный текст и подмешивает
 * акцент темы в ключевые слова, а токены ({@link #KEYWORD_COLOR} и др.) задают цвета явно:
 * {@code theme.set(SyntaxHighlighter.STRING_COLOR, 0xFF98C379)}.</p>
 */
public final class SyntaxHighlighter {
    // Виды лексем.
    public static final int PLAIN = 0;
    public static final int KEYWORD = 1;
    public static final int TYPE = 2;
    public static final int STRING = 3;
    public static final int NUMBER = 4;
    public static final int COMMENT = 5;
    public static final int CONSTANT = 6;
    public static final int FUNCTION = 7;
    public static final int TAG = 8;
    public static final int ATTRIBUTE = 9;
    public static final int PUNCTUATION = 10;
    public static final int ANNOTATION = 11;
    /** Сколько видов лексем. */
    public static final int KINDS = 12;

    // Токены темы: null — цвет подбирается автоматически.
    public static final ThemeVar<Integer> KEYWORD_COLOR = ThemeVar.of("syntax-keyword", Integer.class, null);
    public static final ThemeVar<Integer> TYPE_COLOR = ThemeVar.of("syntax-type", Integer.class, null);
    public static final ThemeVar<Integer> STRING_COLOR = ThemeVar.of("syntax-string", Integer.class, null);
    public static final ThemeVar<Integer> NUMBER_COLOR = ThemeVar.of("syntax-number", Integer.class, null);
    public static final ThemeVar<Integer> COMMENT_COLOR = ThemeVar.of("syntax-comment", Integer.class, null);
    public static final ThemeVar<Integer> CONSTANT_COLOR = ThemeVar.of("syntax-constant", Integer.class, null);
    public static final ThemeVar<Integer> FUNCTION_COLOR = ThemeVar.of("syntax-function", Integer.class, null);
    public static final ThemeVar<Integer> TAG_COLOR = ThemeVar.of("syntax-tag", Integer.class, null);
    public static final ThemeVar<Integer> ATTRIBUTE_COLOR = ThemeVar.of("syntax-attribute", Integer.class, null);
    public static final ThemeVar<Integer> PUNCTUATION_COLOR = ThemeVar.of("syntax-punctuation", Integer.class, null);
    public static final ThemeVar<Integer> ANNOTATION_COLOR = ThemeVar.of("syntax-annotation", Integer.class, null);
    private static final List<ThemeVar<Integer>> VARS = List.of(KEYWORD_COLOR, TYPE_COLOR, STRING_COLOR, NUMBER_COLOR,
            COMMENT_COLOR, CONSTANT_COLOR, FUNCTION_COLOR, TAG_COLOR, ATTRIBUTE_COLOR, PUNCTUATION_COLOR, ANNOTATION_COLOR);

    /** Кусок строки {@code [start, end)} одного вида. */
    public record Span(int start, int end, int kind) {
    }

    /** Куда язык сообщает найденные лексемы. */
    @FunctionalInterface
    public interface Sink {
        void span(int start, int end, int kind);
    }

    /**
     * Язык: размечает одну строку. {@code state} — состояние после предыдущей строки (0 в начале),
     * результат — состояние после этой (например, «внутри комментария /* … *\/»).
     */
    @FunctionalInterface
    public interface Language {
        int line(String line, int state, Sink out);
    }

    private static final Map<String, Language> LANGUAGES = new HashMap<>();

    static {
        Language java = new CLike(
                Set.of("abstract", "assert", "break", "case", "catch", "class", "continue", "default", "do", "else",
                        "enum", "extends", "final", "finally", "for", "if", "implements", "import", "instanceof",
                        "interface", "native", "new", "package", "private", "protected", "public", "return", "static",
                        "strictfp", "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try",
                        "volatile", "while", "var", "record", "sealed", "permits", "yield", "non-sealed",
                        "boolean", "byte", "char", "short", "int", "long", "float", "double", "void"),
                Set.of("true", "false", "null"), true, false);
        register(java, "java");
        register(new CLike(
                Set.of("as", "break", "class", "continue", "do", "else", "for", "fun", "if", "in", "interface", "is",
                        "object", "package", "return", "super", "this", "throw", "try", "typealias", "val", "var",
                        "when", "while", "by", "catch", "constructor", "data", "enum", "finally", "import", "init",
                        "internal", "open", "override", "private", "protected", "public", "sealed", "companion",
                        "lateinit", "suspend", "inline", "reified", "vararg", "abstract", "annotation", "const"),
                Set.of("true", "false", "null"), true, false), "kotlin", "kt");
        register(new CLike(
                Set.of("break", "case", "catch", "class", "const", "continue", "debugger", "default", "delete", "do",
                        "else", "export", "extends", "finally", "for", "function", "if", "import", "in", "instanceof",
                        "let", "new", "return", "super", "switch", "this", "throw", "try", "typeof", "var", "void",
                        "while", "with", "yield", "async", "await", "of", "from", "static", "get", "set",
                        "interface", "type", "enum", "implements", "private", "public", "protected", "readonly",
                        "declare", "namespace", "abstract", "as"),
                Set.of("true", "false", "null", "undefined", "NaN", "Infinity"), false, true),
                "javascript", "js", "typescript", "ts", "jsx", "tsx");
        register(SyntaxHighlighter::json, "json", "json5", "mcmeta");
        register(SyntaxHighlighter::css, "css", "scss", "less");
        register(SyntaxHighlighter::html, "html", "xml", "svg", "htm", "xhtml");
    }

    private SyntaxHighlighter() {
    }

    /** Свой язык под одним или несколькими именами (регистр не важен). */
    public static synchronized void register(Language language, String... names) {
        for (String n : names) {
            LANGUAGES.put(n.toLowerCase(Locale.ROOT), language);
        }
    }

    /** Язык по имени из ```имя; {@code null} — неизвестен (код показывается без подсветки). */
    public static synchronized Language language(String name) {
        return name == null || name.isEmpty() ? null : LANGUAGES.get(name.toLowerCase(Locale.ROOT));
    }

    public static boolean supports(String name) {
        return language(name) != null;
    }

    /**
     * Разметить код: для каждой строки — куски, покрывающие её целиком (промежутки — {@link #PLAIN}).
     * Для неизвестного языка — по одному куску {@code PLAIN} на строку.
     */
    public static List<List<Span>> highlight(String languageName, String code) {
        Language lang = language(languageName);
        String[] lines = code.split("\n", -1);
        List<List<Span>> out = new ArrayList<>(lines.length);
        int state = 0;
        for (String line : lines) {
            List<Span> spans = new ArrayList<>();
            if (lang != null) {
                int[] pos = {0};
                state = lang.line(line, state, (s, e, k) -> {
                    int start = Math.max(s, pos[0]);
                    int end = Math.min(e, line.length());
                    if (end <= start) {
                        return;
                    }
                    if (start > pos[0]) {
                        add(spans, pos[0], start, PLAIN);
                    }
                    add(spans, start, end, k);
                    pos[0] = end;
                });
                if (pos[0] < line.length()) {
                    add(spans, pos[0], line.length(), PLAIN);
                }
            } else if (!line.isEmpty()) {
                spans.add(new Span(0, line.length(), PLAIN));
            }
            out.add(spans);
        }
        return out;
    }

    /** Склеивает соседние куски одного вида — меньше вызовов отрисовки. */
    private static void add(List<Span> spans, int start, int end, int kind) {
        if (!spans.isEmpty()) {
            Span last = spans.get(spans.size() - 1);
            if (last.kind() == kind && last.end() == start) {
                spans.set(spans.size() - 1, new Span(last.start(), end, kind));
                return;
            }
        }
        spans.add(new Span(start, end, kind));
    }

    // ================================================================ цвета

    /**
     * Цвета видов лексем (индекс — вид) для текста цвета {@code text} и акцента {@code accent}:
     * под тёмный фон — светлые пастельные, под светлый — насыщенные тёмные. Токены темы перекрывают.
     */
    public static int[] palette(int text, int accent, Theme theme) {
        boolean darkBg = Color.luminance(text) > 0.5f;
        int[] c = new int[KINDS];
        c[PLAIN] = text;
        if (darkBg) {
            c[KEYWORD] = Color.mix(0xFFC792EA, accent, 0.3f);
            c[TYPE] = 0xFFFFCB6B;
            c[STRING] = 0xFFC3E88D;
            c[NUMBER] = 0xFFF78C6C;
            c[COMMENT] = Color.mulAlpha(Color.mix(text, 0xFF7F8C98, 0.6f), 0.85f);
            c[CONSTANT] = 0xFFFF9CAC;
            c[FUNCTION] = 0xFF82AAFF;
            c[TAG] = 0xFFF07178;
            c[ATTRIBUTE] = 0xFFFFCB6B;
            c[PUNCTUATION] = Color.mulAlpha(text, 0.7f);
            c[ANNOTATION] = 0xFF89DDFF;
        } else {
            c[KEYWORD] = Color.mix(0xFF8E24AA, accent, 0.3f);
            c[TYPE] = 0xFF9C6500;
            c[STRING] = 0xFF2E7D32;
            c[NUMBER] = 0xFFD84315;
            c[COMMENT] = Color.mix(text, 0xFF8A949E, 0.7f);
            c[CONSTANT] = 0xFFC2185B;
            c[FUNCTION] = 0xFF1565C0;
            c[TAG] = 0xFFC62828;
            c[ATTRIBUTE] = 0xFF8D5A00;
            c[PUNCTUATION] = Color.mulAlpha(text, 0.75f);
            c[ANNOTATION] = 0xFF00838F;
        }
        if (theme != null) {
            for (int k = 1; k < KINDS; k++) {
                Integer v = theme.get(VARS.get(k - 1));
                if (v != null) {
                    c[k] = v;
                }
            }
        }
        return c;
    }

    // ================================================================ языки

    private static boolean identStart(char c) {
        return Character.isLetter(c) || c == '_' || c == '$';
    }

    private static boolean identPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    /** Конец строкового литерала с кавычкой {@code q} от {@code i} (за открывающей кавычкой); -1 — не закрыт. */
    private static int stringEnd(String s, int i, char q) {
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') {
                i += 2;
                continue;
            }
            if (c == q) {
                return i + 1;
            }
            i++;
        }
        return -1;
    }

    private static int number(String s, int i) {
        int n = s.length();
        if (i + 1 < n && s.charAt(i) == '0' && (s.charAt(i + 1) == 'x' || s.charAt(i + 1) == 'X')) {
            i += 2;
            while (i < n && (Character.digit(s.charAt(i), 16) >= 0 || s.charAt(i) == '_')) {
                i++;
            }
            return i;
        }
        while (i < n) {
            char c = s.charAt(i);
            if (Character.isDigit(c) || c == '_' || c == '.') {
                i++;
            } else if ((c == 'e' || c == 'E') && i + 1 < n
                    && (Character.isDigit(s.charAt(i + 1)) || s.charAt(i + 1) == '-' || s.charAt(i + 1) == '+')) {
                i += 2;
            } else {
                break;
            }
        }
        // Суффиксы: 10L, 1.5f, 2d.
        if (i < n && "lLfFdD".indexOf(s.charAt(i)) >= 0) {
            i++;
        }
        return i;
    }

    private static int skipSpaces(String s, int i) {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) {
            i++;
        }
        return i;
    }

    /** Языки со скобками C: java, kotlin, javascript. Состояние: 1 — в комментарии, 2 — в текстовом блоке """. */
    private record CLike(Set<String> keywords, Set<String> constants, boolean annotations, boolean templates)
            implements Language {
        private static final int IN_COMMENT = 1;
        private static final int IN_TEXT_BLOCK = 2;
        private static final int IN_TEMPLATE = 3;

        @Override
        public int line(String s, int state, Sink out) {
            int n = s.length();
            int i = 0;
            if (state == IN_COMMENT) {
                int end = s.indexOf("*/");
                if (end < 0) {
                    out.span(0, n, COMMENT);
                    return IN_COMMENT;
                }
                out.span(0, end + 2, COMMENT);
                i = end + 2;
            } else if (state == IN_TEXT_BLOCK || state == IN_TEMPLATE) {
                String close = state == IN_TEXT_BLOCK ? "\"\"\"" : "`";
                int end = s.indexOf(close);
                if (end < 0) {
                    out.span(0, n, STRING);
                    return state;
                }
                out.span(0, end + close.length(), STRING);
                i = end + close.length();
            }
            while (i < n) {
                char c = s.charAt(i);
                if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                    out.span(i, n, COMMENT);
                    return 0;
                }
                if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                    int end = s.indexOf("*/", i + 2);
                    if (end < 0) {
                        out.span(i, n, COMMENT);
                        return IN_COMMENT;
                    }
                    out.span(i, end + 2, COMMENT);
                    i = end + 2;
                } else if (c == '"' && s.startsWith("\"\"\"", i)) {
                    int end = s.indexOf("\"\"\"", i + 3);
                    if (end < 0) {
                        out.span(i, n, STRING);
                        return IN_TEXT_BLOCK;
                    }
                    out.span(i, end + 3, STRING);
                    i = end + 3;
                } else if (c == '`' && templates) {
                    int end = stringEnd(s, i + 1, '`');
                    if (end < 0) {
                        out.span(i, n, STRING);
                        return IN_TEMPLATE;
                    }
                    out.span(i, end, STRING);
                    i = end;
                } else if (c == '"' || c == '\'') {
                    int end = stringEnd(s, i + 1, c);
                    end = end < 0 ? n : end;
                    out.span(i, end, STRING);
                    i = end;
                } else if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(s.charAt(i + 1)))) {
                    int end = number(s, i);
                    out.span(i, end, NUMBER);
                    i = end;
                } else if (c == '@' && annotations && i + 1 < n && identStart(s.charAt(i + 1))) {
                    int end = i + 1;
                    while (end < n && (identPart(s.charAt(end)) || s.charAt(end) == '.')) {
                        end++;
                    }
                    out.span(i, end, ANNOTATION);
                    i = end;
                } else if (identStart(c)) {
                    int end = i + 1;
                    while (end < n && identPart(s.charAt(end))) {
                        end++;
                    }
                    String word = s.substring(i, end);
                    int kind;
                    if (keywords.contains(word)) {
                        kind = KEYWORD;
                    } else if (constants.contains(word)) {
                        kind = CONSTANT;
                    } else if (Character.isUpperCase(word.charAt(0))) {
                        // ВСЕ_ЗАГЛАВНЫЕ — константы, С заглавной — типы.
                        kind = word.length() > 1 && word.equals(word.toUpperCase(Locale.ROOT)) ? CONSTANT : TYPE;
                    } else {
                        int after = skipSpaces(s, end);
                        kind = after < n && s.charAt(after) == '(' ? FUNCTION : PLAIN;
                    }
                    out.span(i, end, kind);
                    i = end;
                } else if ("{}()[];,.:<>=+-*/%!&|^~?".indexOf(c) >= 0) {
                    out.span(i, i + 1, PUNCTUATION);
                    i++;
                } else {
                    i++;
                }
            }
            return 0;
        }
    }

    /** JSON: ключи — атрибуты, строки, числа, true/false/null; комментарии // и /* *\/ тоже (JSON5). */
    private static int json(String s, int state, Sink out) {
        int n = s.length();
        int i = 0;
        if (state == 1) {
            int end = s.indexOf("*/");
            if (end < 0) {
                out.span(0, n, COMMENT);
                return 1;
            }
            out.span(0, end + 2, COMMENT);
            i = end + 2;
        }
        while (i < n) {
            char c = s.charAt(i);
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '/') {
                out.span(i, n, COMMENT);
                return 0;
            }
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                int end = s.indexOf("*/", i + 2);
                if (end < 0) {
                    out.span(i, n, COMMENT);
                    return 1;
                }
                out.span(i, end + 2, COMMENT);
                i = end + 2;
            } else if (c == '"' || c == '\'') {
                int end = stringEnd(s, i + 1, c);
                end = end < 0 ? n : end;
                int after = skipSpaces(s, end);
                out.span(i, end, after < n && s.charAt(after) == ':' ? ATTRIBUTE : STRING);
                i = end;
            } else if (Character.isDigit(c) || c == '-' && i + 1 < n && Character.isDigit(s.charAt(i + 1))) {
                int end = number(s, c == '-' ? i + 1 : i);
                out.span(i, end, NUMBER);
                i = end;
            } else if (identStart(c)) {
                int end = i + 1;
                while (end < n && identPart(s.charAt(end))) {
                    end++;
                }
                String word = s.substring(i, end);
                int after = skipSpaces(s, end);
                boolean key = after < n && s.charAt(after) == ':';
                out.span(i, end, key ? ATTRIBUTE : word.equals("true") || word.equals("false") || word.equals("null")
                        ? CONSTANT : PLAIN);
                i = end;
            } else if ("{}[]:,".indexOf(c) >= 0) {
                out.span(i, i + 1, PUNCTUATION);
                i++;
            } else {
                i++;
            }
        }
        return 0;
    }

    /**
     * CSS: селекторы (теги, .классы, #id, :псевдоклассы), свойства, значения с единицами, цвета #rgb,
     * @-правила, функции. Состояние: бит 0 — внутри комментария, остальные биты — глубина фигурных скобок.
     */
    private static int css(String s, int state, Sink out) {
        int n = s.length();
        int i = 0;
        int depth = state >> 1;
        if ((state & 1) != 0) {
            int end = s.indexOf("*/");
            if (end < 0) {
                out.span(0, n, COMMENT);
                return state;
            }
            out.span(0, end + 2, COMMENT);
            i = end + 2;
        }
        // Внутри блока свойство отличаем от вложенного селектора по двоеточию до ';' или '{'.
        boolean value = false;
        while (i < n) {
            char c = s.charAt(i);
            if (c == '/' && i + 1 < n && s.charAt(i + 1) == '*') {
                int end = s.indexOf("*/", i + 2);
                if (end < 0) {
                    out.span(i, n, COMMENT);
                    return (depth << 1) | 1;
                }
                out.span(i, end + 2, COMMENT);
                i = end + 2;
            } else if (c == '{') {
                depth++;
                value = false;
                out.span(i, i + 1, PUNCTUATION);
                i++;
            } else if (c == '}') {
                depth = Math.max(0, depth - 1);
                value = false;
                out.span(i, i + 1, PUNCTUATION);
                i++;
            } else if (c == ';') {
                value = false;
                out.span(i, i + 1, PUNCTUATION);
                i++;
            } else if (c == '"' || c == '\'') {
                int end = stringEnd(s, i + 1, c);
                end = end < 0 ? n : end;
                out.span(i, end, STRING);
                i = end;
            } else if (c == '@') {
                int end = i + 1;
                while (end < n && (identPart(s.charAt(end)) || s.charAt(end) == '-')) {
                    end++;
                }
                out.span(i, end, KEYWORD);
                i = end;
            } else if (value && c == '#') {
                int end = i + 1;
                while (end < n && Character.digit(s.charAt(end), 16) >= 0) {
                    end++;
                }
                out.span(i, end, NUMBER);
                i = end;
            } else if (value && (Character.isDigit(c) || (c == '.' || c == '-') && i + 1 < n
                    && Character.isDigit(s.charAt(i + 1)))) {
                int end = number(s, c == '-' ? i + 1 : i);
                // Единицы: px, em, %, s, deg…
                while (end < n && (Character.isLetter(s.charAt(end)) || s.charAt(end) == '%')) {
                    end++;
                }
                out.span(i, end, NUMBER);
                i = end;
            } else if (value && c == '!') {
                int end = i + 1;
                while (end < n && Character.isLetter(s.charAt(end))) {
                    end++;
                }
                out.span(i, end, KEYWORD);
                i = end;
            } else if (identStart(c) || c == '-' || (!value && (c == '.' || c == '#' || c == ':') && i + 1 < n
                    && (identStart(s.charAt(i + 1)) || s.charAt(i + 1) == '-' || s.charAt(i + 1) == ':'))) {
                int start = i;
                int end = i + 1;
                while (end < n && (identPart(s.charAt(end)) || s.charAt(end) == '-' || (c == ':' && s.charAt(end) == ':'))) {
                    end++;
                }
                int kind;
                if (value) {
                    int after = skipSpaces(s, end);
                    kind = after < n && s.charAt(after) == '(' ? FUNCTION : CONSTANT;
                } else if (depth > 0 && isProperty(s, end)) {
                    kind = ATTRIBUTE;
                } else if (c == '.' || c == '#') {
                    kind = TYPE;
                } else if (c == ':') {
                    kind = KEYWORD;
                } else {
                    kind = TAG;
                }
                out.span(start, end, kind);
                i = end;
            } else if (c == ':' && depth > 0) {
                value = true;
                out.span(i, i + 1, PUNCTUATION);
                i++;
            } else if (",>+~()".indexOf(c) >= 0) {
                out.span(i, i + 1, PUNCTUATION);
                i++;
            } else {
                i++;
            }
        }
        return depth << 1;
    }

    /** После имени идёт «: значение;», а не «:hover {» — значит, это свойство. */
    private static boolean isProperty(String s, int from) {
        int after = skipSpaces(s, from);
        if (after >= s.length() || s.charAt(after) != ':') {
            return false;
        }
        int brace = s.indexOf('{', after);
        int semi = s.indexOf(';', after);
        return brace < 0 || (semi >= 0 && semi < brace);
    }

    /**
     * HTML и XML: теги, атрибуты, значения, комментарии, сущности &amp;amp;. Состояние: 1 — внутри
     * комментария, 2 — внутри открывающего тега (атрибуты на нескольких строках).
     */
    private static int html(String s, int state, Sink out) {
        int n = s.length();
        int i = 0;
        boolean inTag = state == 2;
        if (state == 1) {
            int end = s.indexOf("-->");
            if (end < 0) {
                out.span(0, n, COMMENT);
                return 1;
            }
            out.span(0, end + 3, COMMENT);
            i = end + 3;
        }
        while (i < n) {
            char c = s.charAt(i);
            if (!inTag) {
                if (s.startsWith("<!--", i)) {
                    int end = s.indexOf("-->", i + 4);
                    if (end < 0) {
                        out.span(i, n, COMMENT);
                        return 1;
                    }
                    out.span(i, end + 3, COMMENT);
                    i = end + 3;
                } else if (c == '<' && i + 1 < n && (identStart(s.charAt(i + 1)) || s.charAt(i + 1) == '/'
                        || s.charAt(i + 1) == '!' || s.charAt(i + 1) == '?')) {
                    int nameStart = i + 1;
                    if (s.charAt(nameStart) == '/' || s.charAt(nameStart) == '?') {
                        nameStart++;
                    }
                    out.span(i, nameStart, PUNCTUATION);
                    int end = nameStart;
                    boolean decl = end < n && s.charAt(end) == '!';
                    if (decl) {
                        end++;
                    }
                    while (end < n && (identPart(s.charAt(end)) || s.charAt(end) == '-' || s.charAt(end) == ':'
                            || s.charAt(end) == '.')) {
                        end++;
                    }
                    out.span(nameStart, end, decl ? KEYWORD : TAG);
                    i = end;
                    inTag = true;
                } else if (c == '&') {
                    int end = s.indexOf(';', i);
                    if (end > i && end - i < 10) {
                        out.span(i, end + 1, CONSTANT);
                        i = end + 1;
                    } else {
                        i++;
                    }
                } else {
                    i++;
                }
            } else {
                if (c == '>' || (c == '/' || c == '?') && i + 1 < n && s.charAt(i + 1) == '>') {
                    int len = c == '>' ? 1 : 2;
                    out.span(i, i + len, PUNCTUATION);
                    i += len;
                    inTag = false;
                } else if (c == '"' || c == '\'') {
                    int end = stringEnd(s, i + 1, c);
                    end = end < 0 ? n : end;
                    out.span(i, end, STRING);
                    i = end;
                } else if (c == '=') {
                    out.span(i, i + 1, PUNCTUATION);
                    i++;
                } else if (identStart(c) || c == ':' || c == '@') {
                    int end = i + 1;
                    while (end < n && (identPart(s.charAt(end)) || s.charAt(end) == '-' || s.charAt(end) == ':'
                            || s.charAt(end) == '.')) {
                        end++;
                    }
                    out.span(i, end, ATTRIBUTE);
                    i = end;
                } else {
                    i++;
                }
            }
        }
        return inTag ? 2 : 0;
    }
}
