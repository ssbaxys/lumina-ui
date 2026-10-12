package io.github.ssbaxys.lumina.markup;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.a11y.AccessibleThemes;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorStops;
import io.github.ssbaxys.lumina.render.Outline;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.sound.UiSound;
import io.github.ssbaxys.lumina.style.ModernTheme;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.StyleKey;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.ThemeVar;
import io.github.ssbaxys.lumina.style.Transition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Supplier;

/**
 * Файлы тем: тема → текст CSS и обратно.
 *
 * <pre>{@code
 * String css = ThemeCss.export(theme, "dark");   // токены + стили классов, отличные от базовой темы "dark"
 * Theme t = ThemeCss.load(css);                    // базовая тема из --lumina-base, поверх — файл
 * ThemeCss.save("моя.css", theme, "dark");         // в config/luminaui/themes/ через Platform.writeFile
 * Theme saved = ThemeCss.loadFile("моя.css");
 * }</pre>
 *
 * Что попадает в файл:
 * <ul>
 *   <li>{@code :root} — все токены, связанные с переменными CSS ({@code --accent}, {@code --corner}…), и имя
 *       базовой темы в {@code --lumina-base};</li>
 *   <li>правила классов — значения, заданные в стилях как есть (числа, цвета, кисти, тени, переходы, звуки),
 *       в том числе в состояниях {@code :hover}, {@code :active}, {@code :focus}, {@code :disabled},
 *       {@code :checked}; модификаторы вида {@code "button.danger"} пишутся селектором {@code button.danger}.</li>
 * </ul>
 * Значения, вычисляемые из токенов ({@code Style.Builder#bind}), в файл не пишутся — их даёт базовая тема,
 * а токены в файле есть. Значение, которое нельзя записать и прочитать обратно без потерь (свой тип мода,
 * градиент со сдвигом, собственное состояние виджета), пропускается с комментарием.
 */
public final class ThemeCss {
    /** Переменная с именем базовой темы. */
    public static final String BASE_VAR = "--lumina-base";
    /** Подкаталог {@code Platform.dataDirectory()} для файлов тем. */
    public static final String THEMES_DIR = "themes";

    private static final Map<String, Supplier<Theme>> BASES = new LinkedHashMap<>();

    static {
        registerBase("dark", Theme::dark);
        registerBase("light", Theme::light);
        registerBase("modern-dark", ModernTheme::dark);
        registerBase("modern-light", ModernTheme::light);
        registerBase("high-contrast-dark", AccessibleThemes::highContrastDark);
        registerBase("high-contrast-light", AccessibleThemes::highContrastLight);
    }

    private ThemeCss() {
    }

    /** Своя базовая тема для файлов: {@code --lumina-base: имя}. */
    public static synchronized void registerBase(String name, Supplier<Theme> factory) {
        BASES.put(name.toLowerCase(Locale.ROOT), factory);
    }

    public static synchronized Set<String> bases() {
        return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(BASES.keySet()));
    }

    /** Новая базовая тема по имени или {@code null}. */
    public static synchronized Theme base(String name) {
        Supplier<Theme> f = name == null ? null : BASES.get(name.trim().toLowerCase(Locale.ROOT));
        return f == null ? null : f.get();
    }

    // ================================================================ экспорт

    /**
     * Экспорт без имени базовой темы: все токены и классы, отличные от тёмной темы — на неё такой файл
     * и загружается ({@link #load(String)}).
     */
    public static String export(Theme theme) {
        return export(theme, null);
    }

    /**
     * Экспорт относительно базовой темы {@code baseName}: все токены и только те классы, чьи заданные как есть
     * значения отличаются от базовой. Класса нет в базе — его значения из токенов пишутся снимком.
     * Ограничение: правила файла дописываются к стилю класса базы, поэтому если у изменённого класса
     * вариант-привязка базы перекрывал более ранний вариант с числом, в пересечении состояний порядок другой.
     */
    public static String export(Theme theme, String baseName) {
        Theme base = base(baseName);
        // Без базы файл загрузится поверх тёмной темы (load): по ней и решаем, чьи привязки к токенам живы.
        Theme implicitBase = base != null ? base : Theme.dark();
        StringBuilder sb = new StringBuilder();
        sb.append("/* Lumina UI theme */\n:root {\n");
        if (base != null) {
            sb.append("  ").append(BASE_VAR).append(": ").append(baseName.trim().toLowerCase(Locale.ROOT)).append(";\n");
        }
        for (Map.Entry<String, ThemeVar<?>> e : CssProperties.tokens().entrySet()) {
            Object v = theme.get(e.getValue());
            String text = v instanceof Integer c ? Color.toHex(c) : v instanceof Float f ? num(f) : null;
            if (text != null) {
                sb.append("  ").append(e.getKey()).append(": ").append(text).append(";\n");
            }
        }
        sb.append("}\n");
        for (String cls : new TreeSet<>(theme.styleClasses())) {
            Style style = theme.rawStyle(cls);
            if (style == null || cls.isBlank() || cls.indexOf(' ') >= 0) {
                continue;
            }
            Style baseStyle = (base != null ? base : implicitBase).rawStyle(cls);
            List<Object[]> entries;
            if (baseStyle != null) {
                // Привязки к токенам даёт базовая тема — пишутся только значения «как есть».
                entries = literals(style);
                if (baseStyle == style || sameLiterals(entries, literals(baseStyle))) {
                    continue;
                }
            } else {
                // Своего класса в базе нет: значения из токенов пишутся снимком на момент экспорта.
                entries = new ArrayList<>();
                List<Object[]> out = entries;
                style.forEachValue(theme, (mask, key, value, fromTheme) -> out.add(new Object[]{mask, key, value}));
            }
            appendClass(sb, cls, entries);
        }
        return sb.toString();
    }

    private static List<Object[]> literals(Style style) {
        List<Object[]> out = new ArrayList<>();
        style.forEachLiteral((mask, key, value) -> out.add(new Object[]{mask, key, value}));
        return out;
    }

    private static boolean sameLiterals(List<Object[]> a, List<Object[]> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!java.util.Arrays.equals(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** Правила одного класса: подряд идущие значения с одной маской состояний — одно правило. */
    private static void appendClass(StringBuilder sb, String cls, List<Object[]> entries) {
        String selector = selectorOf(cls);
        int i = 0;
        while (i < entries.size()) {
            int mask = (Integer) entries.get(i)[0];
            int j = i;
            while (j < entries.size() && (Integer) entries.get(j)[0] == mask) {
                j++;
            }
            String states = statesOf(mask);
            sb.append('\n');
            if (states == null) {
                sb.append("/* ").append(cls).append(": состояние 0x").append(Integer.toHexString(mask))
                        .append(" не выражается в CSS */\n");
                i = j;
                continue;
            }
            sb.append(selector).append(states).append(" {\n");
            for (int k = i; k < j; k++) {
                StyleKey<?> key = (StyleKey<?>) entries.get(k)[1];
                Object value = entries.get(k)[2];
                String text = format(key, value);
                if (text == null) {
                    sb.append("  /* ").append(key.name()).append(": не выражается в CSS */\n");
                } else {
                    sb.append("  ").append(key.name()).append(": ").append(text).append(";\n");
                }
            }
            sb.append("}\n");
            i = j;
        }
    }

    /** "button" → ".button"; модификатор "button.danger" → "button.danger" (тег и класс). */
    private static String selectorOf(String cls) {
        int dot = cls.indexOf('.');
        return dot > 0 ? cls : "." + cls;
    }

    private static String statesOf(int mask) {
        StringBuilder sb = new StringBuilder();
        int known = State.HOVERED | State.PRESSED | State.FOCUSED | State.DISABLED | State.CHECKED;
        if ((mask & ~known) != 0) {
            return null;
        }
        if ((mask & State.HOVERED) != 0) {
            sb.append(":hover");
        }
        if ((mask & State.PRESSED) != 0) {
            sb.append(":active");
        }
        if ((mask & State.FOCUSED) != 0) {
            sb.append(":focus");
        }
        if ((mask & State.DISABLED) != 0) {
            sb.append(":disabled");
        }
        if ((mask & State.CHECKED) != 0) {
            sb.append(":checked");
        }
        return sb.toString();
    }

    /**
     * Значение свойства стиля как текст CSS, который читается обратно в равное значение, или {@code null},
     * если так записать нельзя.
     */
    public static String format(StyleKey<?> key, Object value) {
        String text = formatRaw(key, value);
        if (text == null) {
            return null;
        }
        Object[] parsed = new Object[1];
        try {
            CssProperties.apply(key.name(), text, new CssProperties.Sink() {
                @Override
                public <T> void put(StyleKey<T> k, T v) {
                    if (k == key) {
                        parsed[0] = v;
                    }
                }
            });
        } catch (RuntimeException e) {
            return null;
        }
        return value.equals(parsed[0]) ? text : null;
    }

    private static String formatRaw(StyleKey<?> key, Object v) {
        if (key == Styles.GLOW && v instanceof Shadow s) {
            if (s.equals(Shadow.NONE)) {
                return "none";
            }
            return s.offsetX() == 0f && s.offsetY() == 0f ? num(s.blur()) + " " + num(s.spread()) + " " + Color.toHex(s.color()) : null;
        }
        if (v instanceof Integer c) {
            return Color.toHex(c);
        }
        if (v instanceof Float f) {
            return num(f);
        }
        if (v instanceof Boolean b) {
            return b ? "true" : "none";
        }
        if (v instanceof Brush b) {
            return brush(b);
        }
        if (v instanceof Radii r) {
            return num(r.topLeft()) + " " + num(r.topRight()) + " " + num(r.bottomRight()) + " " + num(r.bottomLeft());
        }
        if (v instanceof Insets i) {
            return num(i.top()) + " " + num(i.right()) + " " + num(i.bottom()) + " " + num(i.left());
        }
        if (v instanceof Shadow s) {
            return s.equals(Shadow.NONE) ? "none"
                    : num(s.offsetX()) + " " + num(s.offsetY()) + " " + num(s.blur()) + " " + num(s.spread()) + " " + Color.toHex(s.color());
        }
        if (v instanceof Outline o) {
            return o.equals(Outline.NONE) ? "none" : num(o.width()) + " " + num(o.offset()) + " " + Color.toHex(o.color());
        }
        if (v instanceof BlendMode m) {
            return m.name().toLowerCase(Locale.ROOT);
        }
        if (v instanceof Transition t) {
            return transition(t);
        }
        if (v instanceof UiSound s) {
            return s.toString();
        }
        return null;
    }

    private static String brush(Brush b) {
        if (b instanceof Brush.Solid s) {
            return Color.toHex(s.color());
        }
        if (b instanceof Brush.Linear l) {
            if (l.offset() != 0f) {
                return null;
            }
            return (l.repeating() ? "repeating-" : "") + "linear-gradient(" + num(l.angle()) + "deg, "
                    + stops(l.stops()) + ", in " + l.space().name().toLowerCase(Locale.ROOT) + ")";
        }
        if (b instanceof Brush.Radial r) {
            if (r.offset() != 0f) {
                return null;
            }
            return (r.repeating() ? "repeating-" : "") + "radial-gradient(at " + num(r.centerX()) + " " + num(r.centerY())
                    + ", " + stops(r.stops()) + ", in " + r.space().name().toLowerCase(Locale.ROOT) + ")";
        }
        if (b instanceof Brush.Conic c) {
            if (c.offset() != 0f) {
                return null;
            }
            return (c.repeating() ? "repeating-" : "") + "conic-gradient(from " + num(c.angle()) + "deg, "
                    + stops(c.stops()) + ", in " + c.space().name().toLowerCase(Locale.ROOT) + ")";
        }
        return null;
    }

    private static String stops(ColorStops stops) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stops.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(Color.toHex(stops.color(i))).append(' ').append(num(stops.position(i)));
        }
        return sb.toString();
    }

    private static String transition(Transition t) {
        if (t.equals(Transition.NONE)) {
            return "none";
        }
        String easing = easingName(t.easing());
        if (easing == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder("all ").append(num(t.duration())).append("s ").append(easing);
        if (t.delay() != 0f) {
            sb.append(' ').append(num(t.delay())).append('s');
        }
        for (int id : t.keyIds()) {
            String e = easingName(t.easingOf(id));
            if (e == null) {
                return null;
            }
            sb.append(", ").append(StyleKey.byId(id).name()).append(' ').append(num(t.durationOf(id))).append("s ")
                    .append(e).append(' ').append(num(t.delayOf(id))).append('s');
        }
        return sb.toString();
    }

    /** Имя встроенной кривой в кебаб-регистре ("out-cubic") или {@code null} для своей кривой. */
    private static String easingName(Easing easing) {
        for (Map.Entry<String, Easing> e : Easing.presets().entrySet()) {
            if (e.getValue() == easing) {
                return e.getKey().toLowerCase(Locale.ROOT).replace('_', '-');
            }
        }
        return null;
    }

    /** Число без лишних нулей, читающееся обратно в тот же float. */
    static String num(float f) {
        if (f == (int) f && Math.abs(f) < 1e7f) {
            return Integer.toString((int) f);
        }
        return Float.toString(f);
    }

    // ================================================================ импорт

    /**
     * Тема из текста: базовая тема из {@code --lumina-base} (или тёмная по умолчанию), поверх — токены
     * и правила файла.
     */
    public static Theme load(String css) {
        return load(css, null);
    }

    /** Как {@link #load(String)}, но без {@code --lumina-base} в файле берётся {@code fallback} (копия). */
    public static Theme load(String css, Theme fallback) {
        Stylesheet sheet = Stylesheet.parse(css);
        Theme base = base(sheet.variables().get(BASE_VAR));
        if (base == null) {
            base = fallback != null ? fallback.copy() : Theme.dark();
        }
        return sheet.applyTo(base);
    }

    /** Применить текст темы к существующей теме (на месте): токены и правила классов. */
    public static Theme applyTo(Theme theme, String css) {
        return Stylesheet.parse(css).applyTo(theme);
    }

    /** Имя базовой темы из текста ({@code --lumina-base}) или {@code null}. */
    public static String baseName(String css) {
        String b = Stylesheet.parse(css).variables().get(BASE_VAR);
        return b == null ? null : b.trim().toLowerCase(Locale.ROOT);
    }

    // ================================================================ файлы

    /** Сохранить тему в {@code themes/<fileName>} каталога данных платформы. */
    public static boolean save(String fileName, Theme theme, String baseName) {
        return Lumina.isInitialized() && Lumina.platform().writeFile(path(fileName), export(theme, baseName));
    }

    /** Загрузить тему из {@code themes/<fileName>}; {@code null}, если файла нет. */
    public static Theme loadFile(String fileName) {
        String css = readFile(fileName);
        return css == null ? null : load(css);
    }

    /** Текст файла темы или {@code null}. */
    public static String readFile(String fileName) {
        return Lumina.isInitialized() ? Lumina.platform().readFile(path(fileName)) : null;
    }

    /** Сохранённые темы ({@code *.css} в каталоге тем). */
    public static List<String> savedThemes() {
        if (!Lumina.isInitialized()) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (String f : Lumina.platform().listFiles(THEMES_DIR)) {
            if (f.toLowerCase(Locale.ROOT).endsWith(".css")) {
                out.add(f);
            }
        }
        return out;
    }

    private static String path(String fileName) {
        String f = fileName.toLowerCase(Locale.ROOT).endsWith(".css") ? fileName : fileName + ".css";
        return THEMES_DIR + "/" + f;
    }
}
