package io.github.ssbaxys.lumina.markup;

import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.StyleKey;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.ThemeVar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Таблица стилей в синтаксисе CSS, превращённая в обычные {@link Style} — без рефлексии и без
 * разбора во время кадра (всё считается при загрузке).
 *
 * <ul>
 *   <li>Селекторы: тег ({@code button}), класс ({@code .card}), id ({@code #title}), их сочетания,
 *       потомки ({@code .menu button}) и дети ({@code .menu > button}), списки через запятую.</li>
 *   <li>Состояния: {@code :hover}, {@code :active} (или {@code :pressed}), {@code :focus}, {@code :disabled},
 *       {@code :checked}.</li>
 *   <li>Переменные: {@code :root { --gap: 8px }} и {@code var(--gap)}; переменные-токены
 *       ({@code --accent}, {@code --corner}…) берутся из темы и меняются вместе с ней.</li>
 *   <li>Раскладка в CSS: {@code width, height, min-/max-, margin, gap, flex-direction, align-items,
 *       justify-content, flex-grow, flex-shrink, position: absolute, left, top}.</li>
 *   <li>Анимации: {@code @keyframes} и {@code animation: имя 2s ease-in-out infinite alternate}.</li>
 * </ul>
 *
 * Применение: к дереву из разметки ({@link Markup}) или к теме целиком ({@link #applyTo(Theme)}).
 */
public final class Stylesheet {
    /** Свойства раскладки и анимации: применяются к элементу при построении, а не через стиль. */
    static final List<String> LAYOUT_PROPS = List.of("width", "height", "min-width", "min-height", "max-width",
            "max-height", "margin", "gap", "flex-direction", "align-items", "justify-content", "flex-grow", "grow",
            "flex-shrink", "position", "left", "top", "animation", "overflow", "display");

    final List<Rule> rules = new ArrayList<>();
    final Map<String, String> vars = new LinkedHashMap<>();
    final Map<String, List<Keyframe>> keyframes = new HashMap<>();
    private int order;

    record Declaration(String name, String value) {
    }

    record Keyframe(float offset, List<Declaration> declarations) {
    }

    /** Простой селектор: tag#id.class:state; {@code combinator} — как он связан с предыдущим (' ' или '>'). */
    record Compound(String tag, String id, List<String> classes, int states, char combinator) {
        boolean matches(Markup.Node n) {
            if (tag != null && !tag.equals(n.tag())) {
                return false;
            }
            if (id != null && !id.equals(n.attr("id"))) {
                return false;
            }
            for (String c : classes) {
                if (!n.classes().contains(c)) {
                    return false;
                }
            }
            return true;
        }
    }

    record Selector(List<Compound> parts, int specificity) {
        Compound subject() {
            return parts.get(parts.size() - 1);
        }

        /** Подходит ли узел (без учёта состояний — они становятся вариантами стиля). */
        boolean matches(Markup.Node node) {
            int i = parts.size() - 1;
            if (!parts.get(i).matches(node)) {
                return false;
            }
            Markup.Node cur = node;
            while (--i >= 0) {
                char comb = parts.get(i + 1).combinator();
                Compound want = parts.get(i);
                Markup.Node p = cur.parent();
                if (comb == '>') {
                    if (p == null || !want.matches(p)) {
                        return false;
                    }
                    cur = p;
                } else {
                    while (p != null && !want.matches(p)) {
                        p = p.parent();
                    }
                    if (p == null) {
                        return false;
                    }
                    cur = p;
                }
            }
            return true;
        }
    }

    record Rule(Selector selector, List<Declaration> declarations, int order) {
    }

    // ================================================================ разбор

    public static Stylesheet parse(String css) {
        Stylesheet s = new Stylesheet();
        s.add(css);
        return s;
    }

    /** Дописать ещё CSS (правила идут после уже имеющихся). */
    public Stylesheet add(String css) {
        String src = stripComments(css);
        int i = 0;
        while (i < src.length()) {
            int open = src.indexOf('{', i);
            if (open < 0) {
                break;
            }
            String prelude = src.substring(i, open).trim();
            int close = matchingBrace(src, open);
            String body = src.substring(open + 1, close);
            if (prelude.startsWith("@keyframes")) {
                keyframes.put(prelude.substring("@keyframes".length()).trim(), parseKeyframes(body));
            } else if (prelude.equals(":root")) {
                for (Declaration d : declarations(body)) {
                    if (d.name().startsWith("--")) {
                        vars.put(d.name(), d.value());
                    }
                }
            } else if (!prelude.startsWith("@")) {
                List<Declaration> decls = declarations(body);
                for (String sel : CssValues.split(prelude, ',')) {
                    rules.add(new Rule(selector(sel), decls, order++));
                }
            }
            i = close + 1;
        }
        return this;
    }

    public Stylesheet merge(Stylesheet other) {
        vars.putAll(other.vars);
        keyframes.putAll(other.keyframes);
        for (Rule r : other.rules) {
            rules.add(new Rule(r.selector(), r.declarations(), order++));
        }
        return this;
    }

    private static String stripComments(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        int i = 0;
        while (i < s.length()) {
            if (s.startsWith("/*", i)) {
                int end = s.indexOf("*/", i + 2);
                i = end < 0 ? s.length() : end + 2;
            } else {
                sb.append(s.charAt(i++));
            }
        }
        return sb.toString();
    }

    private static int matchingBrace(String s, int open) {
        int depth = 0;
        for (int i = open; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i;
            }
        }
        return s.length() - 1;
    }

    static List<Declaration> declarations(String body) {
        List<Declaration> out = new ArrayList<>();
        for (String part : CssValues.split(body, ';')) {
            int colon = part.indexOf(':');
            if (colon <= 0) {
                continue;
            }
            String name = part.substring(0, colon).trim();
            if (!name.startsWith("--")) {
                name = name.toLowerCase(Locale.ROOT);
            }
            String value = part.substring(colon + 1).trim();
            if (value.endsWith("!important")) {
                value = value.substring(0, value.length() - "!important".length()).trim();
            }
            out.add(new Declaration(name, value));
        }
        return out;
    }

    private static List<Keyframe> parseKeyframes(String body) {
        List<Keyframe> out = new ArrayList<>();
        int i = 0;
        while (i < body.length()) {
            int open = body.indexOf('{', i);
            if (open < 0) {
                break;
            }
            int close = body.indexOf('}', open);
            List<Declaration> decls = declarations(body.substring(open + 1, close));
            for (String sel : CssValues.split(body.substring(i, open).trim(), ',')) {
                String s = sel.trim().toLowerCase(Locale.ROOT);
                float offset = s.equals("from") ? 0f : s.equals("to") ? 1f : CssValues.number(s);
                out.add(new Keyframe(offset, decls));
            }
            i = close + 1;
        }
        out.sort((a, b) -> Float.compare(a.offset(), b.offset()));
        return out;
    }

    static Selector selector(String text) {
        List<Compound> parts = new ArrayList<>();
        int spec = 0;
        char comb = ' ';
        for (String token : text.trim().replace(">", " > ").split("\\s+")) {
            if (token.isEmpty()) {
                continue;
            }
            if (token.equals(">")) {
                comb = '>';
                continue;
            }
            String tag = null;
            String id = null;
            List<String> classes = new ArrayList<>();
            int states = 0;
            int i = 0;
            while (i < token.length()) {
                char c = token.charAt(i);
                int j = i + 1;
                while (j < token.length() && ".#:".indexOf(token.charAt(j)) < 0) {
                    j++;
                }
                String name = token.substring(c == '.' || c == '#' || c == ':' ? i + 1 : i, j);
                if (c == '.') {
                    classes.add(name);
                    spec += 10;
                } else if (c == '#') {
                    id = name;
                    spec += 100;
                } else if (c == ':') {
                    states |= state(name);
                    spec += 10;
                } else if (!name.equals("*")) {
                    tag = name.toLowerCase(Locale.ROOT);
                    spec += 1;
                }
                i = j;
            }
            parts.add(new Compound(tag, id, classes, states, comb));
            comb = ' ';
        }
        return new Selector(parts, spec);
    }

    private static int state(String name) {
        return switch (name.toLowerCase(Locale.ROOT)) {
            case "hover" -> State.HOVERED;
            case "active", "pressed" -> State.PRESSED;
            case "focus", "focus-visible" -> State.FOCUSED;
            case "disabled" -> State.DISABLED;
            case "checked" -> State.CHECKED;
            default -> 0;
        };
    }

    // ================================================================ применение

    /** Правила, подходящие узлу, по возрастанию специфичности и порядка (последнее сильнее). */
    List<Rule> matching(Markup.Node node) {
        List<Rule> out = new ArrayList<>();
        for (Rule r : rules) {
            if (r.selector().matches(node)) {
                out.add(r);
            }
        }
        out.sort((a, b) -> a.selector().specificity() != b.selector().specificity()
                ? Integer.compare(a.selector().specificity(), b.selector().specificity())
                : Integer.compare(a.order(), b.order()));
        return out;
    }

    /** Добавить объявления в стиль (с учётом состояний). Раскладка и анимации сюда не входят. */
    void applyDeclarations(Style.Builder b, List<Declaration> decls, int states) {
        if (states == 0) {
            for (Declaration d : decls) {
                applyOne(b, d);
            }
        } else {
            b.when(states, v -> {
                for (Declaration d : decls) {
                    applyOne(v, d);
                }
            });
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void applyOne(Style.Builder b, Declaration d) {
        if (d.name().startsWith("--") || LAYOUT_PROPS.contains(d.name()) || !CssProperties.isKnown(d.name())) {
            return;
        }
        String raw = CssProperties.substitute(d.value(), vars, null);
        if (!CssProperties.referencesTokens(d.value()) && !referencesTokensViaVars(d.value())) {
            try {
                CssProperties.apply(d.name(), raw, new CssProperties.Sink() {
                    @Override
                    public <T> void put(StyleKey<T> key, T value) {
                        b.set(key, value);
                    }
                });
            } catch (RuntimeException e) {
                warn(d, e);
            }
            return;
        }
        // Значение зависит от токенов темы: пересчитывается при смене темы.
        List<StyleKey<?>> keys = new ArrayList<>();
        try {
            CssProperties.apply(d.name(), raw, new CssProperties.Sink() {
                @Override
                public <T> void put(StyleKey<T> key, T value) {
                    keys.add(key);
                }
            });
        } catch (RuntimeException e) {
            warn(d, e);
            return;
        }
        for (StyleKey key : keys) {
            b.fromTheme(key, theme -> {
                Object[] found = new Object[1];
                CssProperties.apply(d.name(), CssProperties.substitute(d.value(), vars, theme), new CssProperties.Sink() {
                    @Override
                    public <T> void put(StyleKey<T> k, T value) {
                        if (k == key) {
                            found[0] = value;
                        }
                    }
                });
                return found[0];
            });
        }
    }

    private boolean referencesTokensViaVars(String value) {
        for (Map.Entry<String, String> v : vars.entrySet()) {
            if (value.contains(v.getKey()) && CssProperties.referencesTokens(v.getValue())) {
                return true;
            }
        }
        return false;
    }

    /** Значение свойства раскладки/анимации для узла (последнее подходящее правило без состояний). */
    String layoutValue(List<Rule> matched, String name) {
        String value = null;
        for (Rule r : matched) {
            if (r.selector().subject().states() != 0) {
                continue;
            }
            for (Declaration d : r.declarations()) {
                if (d.name().equals(name)) {
                    value = CssProperties.substitute(d.value(), vars, null);
                }
            }
        }
        return value;
    }

    /** Значения свойств по ключевому кадру: offset → (ключ стиля → значение). */
    Map<StyleKey<?>, List<Object[]>> keyframeTracks(String name) {
        List<Keyframe> frames = keyframes.get(name);
        if (frames == null) {
            return Collections.emptyMap();
        }
        Map<StyleKey<?>, List<Object[]>> tracks = new LinkedHashMap<>();
        for (Keyframe f : frames) {
            for (Declaration d : f.declarations()) {
                try {
                    CssProperties.apply(d.name(), CssProperties.substitute(d.value(), vars, null), new CssProperties.Sink() {
                        @Override
                        public <T> void put(StyleKey<T> key, T value) {
                            tracks.computeIfAbsent(key, k -> new ArrayList<>()).add(new Object[]{f.offset(), value});
                        }
                    });
                } catch (RuntimeException e) {
                    warn(d, e);
                }
            }
        }
        return tracks;
    }

    /**
     * Применить к теме: правила с простыми селекторами ({@code button}, {@code .card:hover}) дополняют
     * стили одноимённых классов, переменные-токены из {@code :root} меняют токены темы. Так из CSS-файла
     * можно оформить и обычные виджеты, созданные кодом. Селектор «тег.класс» ({@code button.danger})
     * задаёт модификатор темы {@code "button.danger"} — он действует в {@code styleClass("button danger")}.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Theme applyTo(Theme theme) {
        for (Map.Entry<String, String> v : vars.entrySet()) {
            ThemeVar token = CssProperties.tokenOf(v.getKey());
            if (token != null) {
                String value = CssProperties.substitute(v.getValue(), vars, null);
                // Без тернарного оператора: int и float в нём продвигаются к float.
                Object parsed;
                if (token.type() == Integer.class) {
                    parsed = CssValues.color(value);
                } else {
                    parsed = CssValues.length(value);
                }
                theme.set(token, parsed);
            }
        }
        List<Rule> sorted = new ArrayList<>(rules);
        sorted.sort((a, b) -> Integer.compare(a.selector().specificity(), b.selector().specificity()));
        for (Rule r : sorted) {
            if (r.selector().parts().size() != 1) {
                continue;
            }
            Compound c = r.selector().subject();
            String cls = c.classes().size() == 1 && c.tag() == null ? c.classes().get(0)
                    : c.classes().isEmpty() && c.tag() != null ? c.tag()
                    : c.classes().size() == 1 ? c.tag() + "." + c.classes().get(0) : null;
            if (cls == null || c.id() != null) {
                continue;
            }
            theme.edit(cls, b -> applyDeclarations(b, r.declarations(), c.states()));
        }
        return theme;
    }

    public Map<String, String> variables() {
        return Collections.unmodifiableMap(vars);
    }

    private static void warn(Declaration d, RuntimeException e) {
        System.err.println("[Lumina CSS] " + d.name() + ": " + d.value() + " — " + e.getMessage());
    }
}
