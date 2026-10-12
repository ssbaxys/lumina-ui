package io.github.ssbaxys.lumina.markup;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.style.StyleKey;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.ThemeVar;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.style.Transition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Свойства CSS → свойства стиля Lumina. Набор открыт: {@link #register} добавляет своё свойство
 * (например, для собственного {@link StyleKey} мода), а любое зарегистрированное свойство стиля
 * доступно и под своим именем ({@code text-color}, {@code font-scale}...).
 */
public final class CssProperties {
    /** Куда свойство записывает разобранные значения. */
    public interface Sink {
        <T> void put(StyleKey<T> key, T value);
    }

    @FunctionalInterface
    public interface Handler {
        void apply(String value, Sink out);
    }

    private static final Map<String, Handler> HANDLERS = new HashMap<>();
    private static final Map<String, ThemeVar<?>> TOKENS = new HashMap<>();
    private static final Map<String, StyleKey<?>[]> TRANSITION_GROUPS = new HashMap<>();
    private static final Pattern VAR = Pattern.compile("var\\(\\s*(--[\\w-]+)\\s*(?:,\\s*([^)]*))?\\)");

    static {
        register("background", (v, o) -> o.put(Styles.BACKGROUND, CssValues.brush(v)));
        register("background-color", (v, o) -> o.put(Styles.BACKGROUND, CssValues.brush(v)));
        register("color", (v, o) -> o.put(Styles.TEXT_COLOR, CssValues.color(v)));
        register("accent-color", (v, o) -> o.put(Styles.ACCENT_COLOR, CssValues.color(v)));
        register("opacity", (v, o) -> o.put(Styles.OPACITY, CssValues.number(v)));
        register("border-radius", (v, o) -> o.put(Styles.RADIUS, CssValues.radii(v)));
        register("border-width", (v, o) -> o.put(Styles.BORDER_WIDTH, CssValues.length(v)));
        register("border-color", (v, o) -> o.put(Styles.BORDER_COLOR, CssValues.color(v)));
        register("border-image", (v, o) -> o.put(Styles.BORDER_BRUSH, CssValues.brush(v)));
        register("border", CssProperties::border);
        register("box-shadow", (v, o) -> o.put(Styles.SHADOW, CssValues.shadow(v)));
        register("glow", (v, o) -> o.put(Styles.GLOW, CssValues.glow(v)));
        register("outline", (v, o) -> o.put(Styles.OUTLINE, CssValues.outline(v)));
        register("foreground", (v, o) -> o.put(Styles.FOREGROUND, CssValues.brush(v)));
        register("nine-slice", (v, o) -> o.put(Styles.NINE_SLICE, CssValues.nineSlice(v)));
        register("padding", (v, o) -> o.put(Styles.PADDING, CssValues.insets(v)));
        register("transform", CssProperties::transform);
        register("scale", (v, o) -> transform("scale(" + v.trim().replace(' ', ',') + ")", o));
        register("translate", (v, o) -> transform("translate(" + v.trim().replace(' ', ',') + ")", o));
        register("font-size", (v, o) -> o.put(Styles.FONT_SCALE, fontScale(v)));
        register("text-shadow", (v, o) -> o.put(Styles.TEXT_SHADOW, !v.trim().equalsIgnoreCase("none")));
        register("mix-blend-mode", (v, o) -> o.put(Styles.BLEND, CssValues.blend(v)));
        register("transition", (v, o) -> o.put(Styles.TRANSITION, transition(v)));

        token("--accent", Tokens.ACCENT);
        token("--text", Tokens.TEXT);
        token("--surface", Tokens.SURFACE);
        token("--surface-alt", Tokens.SURFACE_ALT);
        token("--border", Tokens.BORDER);
        token("--shadow", Tokens.SHADOW);
        token("--well", Tokens.WELL);
        token("--corner", Tokens.CORNER);
        token("--motion", Tokens.MOTION);

        transitionGroup("all");
        transitionGroup("transform", Styles.SCALE, Styles.SCALE_X, Styles.SCALE_Y, Styles.TRANSLATE_X, Styles.TRANSLATE_Y);
        transitionGroup("background", Styles.BACKGROUND);
        transitionGroup("background-color", Styles.BACKGROUND);
        transitionGroup("color", Styles.TEXT_COLOR);
        transitionGroup("border", Styles.BORDER_WIDTH, Styles.BORDER_COLOR, Styles.BORDER_BRUSH);
        transitionGroup("border-color", Styles.BORDER_COLOR, Styles.BORDER_BRUSH);
        transitionGroup("border-radius", Styles.RADIUS);
        transitionGroup("box-shadow", Styles.SHADOW);
        transitionGroup("glow", Styles.GLOW);
        transitionGroup("outline", Styles.OUTLINE);
        transitionGroup("opacity", Styles.OPACITY);
        transitionGroup("padding", Styles.PADDING);
        transitionGroup("font-size", Styles.FONT_SCALE);
    }

    private CssProperties() {
    }

    /** Своё свойство CSS. */
    public static synchronized void register(String name, Handler handler) {
        HANDLERS.put(name.toLowerCase(Locale.ROOT), handler);
    }

    /** Связать переменную CSS с токеном темы: {@code var(--brand)} будет меняться вместе с темой. */
    public static synchronized void token(String cssVar, ThemeVar<?> var) {
        TOKENS.put(cssVar, var);
    }

    /** Имя для {@code transition: <имя> 0.3s}: какие свойства стиля оно охватывает. */
    public static synchronized void transitionGroup(String name, StyleKey<?>... keys) {
        TRANSITION_GROUPS.put(name, keys);
    }

    static ThemeVar<?> tokenOf(String cssVar) {
        return TOKENS.get(cssVar);
    }

    static boolean isKnown(String name) {
        return HANDLERS.containsKey(name) || StyleKey.byName(name) != null;
    }

    /** Разобрать одно объявление в {@code out}. Неизвестные свойства молча пропускаются (как в браузере). */
    static void apply(String name, String value, Sink out) {
        Handler h = HANDLERS.get(name);
        if (h != null) {
            h.apply(value, out);
            return;
        }
        StyleKey<?> key = StyleKey.byName(name);
        if (key != null) {
            putGeneric(key, value, out);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void putGeneric(StyleKey key, String v, Sink out) {
        Class<?> t = key.type();
        Object value;
        if (t == Integer.class) {
            value = CssValues.color(v);
        } else if (t == Float.class) {
            value = CssValues.isTime(v) ? CssValues.time(v) : CssValues.number(v.replace("px", ""));
        } else if (t == Boolean.class) {
            value = !v.trim().equalsIgnoreCase("false") && !v.trim().equalsIgnoreCase("none");
        } else if (t == Brush.class) {
            value = CssValues.brush(v);
        } else if (t == io.github.ssbaxys.lumina.render.Radii.class) {
            value = CssValues.radii(v);
        } else if (t == io.github.ssbaxys.lumina.geom.Insets.class) {
            value = CssValues.insets(v);
        } else if (t == io.github.ssbaxys.lumina.render.Shadow.class) {
            value = CssValues.shadow(v);
        } else if (t == io.github.ssbaxys.lumina.render.Outline.class) {
            value = CssValues.outline(v);
        } else if (t == io.github.ssbaxys.lumina.render.BlendMode.class) {
            value = CssValues.blend(v);
        } else if (t == Transition.class) {
            value = transition(v);
        } else {
            return;
        }
        out.put(key, value);
    }

    // ================================================================ var()

    /** Есть ли в значении ссылки на токены темы (тогда значение пересчитывается при смене темы). */
    static boolean referencesTokens(String value) {
        Matcher m = VAR.matcher(value);
        while (m.find()) {
            if (TOKENS.containsKey(m.group(1))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Подставить переменные: свои ({@code vars}) — текстом, токены — значениями из {@code theme}
     * (или их значениями по умолчанию, если темы нет).
     */
    static String substitute(String value, Map<String, String> vars, Theme theme) {
        if (value.indexOf("var(") < 0) {
            return value;
        }
        StringBuilder sb = new StringBuilder();
        Matcher m = VAR.matcher(value);
        int last = 0;
        while (m.find()) {
            sb.append(value, last, m.start());
            String name = m.group(1);
            String replacement;
            ThemeVar<?> token = TOKENS.get(name);
            if (token != null) {
                Object v = theme != null ? theme.get(token) : token.defaultValue();
                replacement = v instanceof Integer c ? Color.toHex(c) : String.valueOf(v);
            } else if (vars.containsKey(name)) {
                replacement = substitute(vars.get(name), vars, theme);
            } else {
                replacement = m.group(2) != null ? m.group(2) : "";
            }
            sb.append(replacement);
            last = m.end();
        }
        sb.append(value.substring(last));
        return sb.toString();
    }

    // ================================================================ составные свойства

    private static void border(String v, Sink o) {
        if (v.trim().equalsIgnoreCase("none")) {
            o.put(Styles.BORDER_WIDTH, 0f);
            return;
        }
        for (String p : CssValues.split(v, ' ')) {
            char c = p.charAt(0);
            if (Character.isDigit(c) || c == '.') {
                o.put(Styles.BORDER_WIDTH, CssValues.length(p));
            } else if (p.equalsIgnoreCase("solid")) {
                continue;
            } else if (p.contains("gradient(")) {
                o.put(Styles.BORDER_BRUSH, CssValues.brush(p));
            } else {
                o.put(Styles.BORDER_COLOR, CssValues.color(p));
            }
        }
    }

    private static void transform(String v, Sink o) {
        if (v.trim().equalsIgnoreCase("none")) {
            o.put(Styles.SCALE, 1f);
            o.put(Styles.SCALE_X, 1f);
            o.put(Styles.SCALE_Y, 1f);
            o.put(Styles.TRANSLATE_X, 0f);
            o.put(Styles.TRANSLATE_Y, 0f);
            return;
        }
        for (String fn : CssValues.split(v, ' ')) {
            String name = fn.substring(0, fn.indexOf('(')).trim().toLowerCase(Locale.ROOT);
            List<String> a = CssValues.args(fn);
            switch (name) {
                case "scale" -> {
                    if (a.size() == 1) {
                        o.put(Styles.SCALE, CssValues.number(a.get(0)));
                    } else {
                        o.put(Styles.SCALE_X, CssValues.number(a.get(0)));
                        o.put(Styles.SCALE_Y, CssValues.number(a.get(1)));
                    }
                }
                case "scalex" -> o.put(Styles.SCALE_X, CssValues.number(a.get(0)));
                case "scaley" -> o.put(Styles.SCALE_Y, CssValues.number(a.get(0)));
                case "translate" -> {
                    o.put(Styles.TRANSLATE_X, CssValues.length(a.get(0)));
                    o.put(Styles.TRANSLATE_Y, a.size() > 1 ? CssValues.length(a.get(1)) : 0f);
                }
                case "translatex" -> o.put(Styles.TRANSLATE_X, CssValues.length(a.get(0)));
                case "translatey" -> o.put(Styles.TRANSLATE_Y, CssValues.length(a.get(0)));
                default -> {
                }
            }
        }
    }

    private static float fontScale(String v) {
        String t = v.trim();
        if (t.endsWith("%")) {
            return CssValues.number(t);
        }
        if (t.endsWith("em")) {
            return Float.parseFloat(t.substring(0, t.length() - 2));
        }
        if (t.endsWith("px")) {
            return CssValues.length(t) / 9f;
        }
        return Float.parseFloat(t);
    }

    /** {@code all 0.3s ease-out} или список {@code transform .5s out-elastic, background 1s 0.2s}. */
    static Transition transition(String v) {
        if (v.trim().equalsIgnoreCase("none")) {
            return Transition.NONE;
        }
        List<String> items = CssValues.split(v, ',');
        Transition result = null;
        List<Object[]> keyed = new ArrayList<>();
        for (String item : items) {
            String prop = "all";
            float dur = 0f;
            float delay = 0f;
            int times = 0;
            Easing easing = Easing.OUT_CUBIC;
            for (String p : CssValues.split(item, ' ')) {
                if (CssValues.isTime(p)) {
                    if (times++ == 0) {
                        dur = CssValues.time(p);
                    } else {
                        delay = CssValues.time(p);
                    }
                } else if (TRANSITION_GROUPS.containsKey(p.toLowerCase(Locale.ROOT)) || StyleKey.byName(p) != null) {
                    prop = p.toLowerCase(Locale.ROOT);
                } else {
                    easing = CssValues.easing(p);
                }
            }
            if (prop.equals("all")) {
                result = Transition.of(dur, easing).delay(delay);
            } else {
                keyed.add(new Object[]{prop, dur, easing, delay});
            }
        }
        if (result == null) {
            result = Transition.NONE;
        }
        for (Object[] k : keyed) {
            StyleKey<?>[] keys = TRANSITION_GROUPS.get((String) k[0]);
            if (keys == null) {
                keys = new StyleKey<?>[]{StyleKey.byName((String) k[0])};
            }
            for (StyleKey<?> key : keys) {
                result = result.key(key, (Float) k[1], (Easing) k[2], (Float) k[3]);
            }
        }
        return result;
    }
}
