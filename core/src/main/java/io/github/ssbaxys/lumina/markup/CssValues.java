package io.github.ssbaxys.lumina.markup;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorSpace;
import io.github.ssbaxys.lumina.render.ColorStops;
import io.github.ssbaxys.lumina.render.Gradients;
import io.github.ssbaxys.lumina.render.Outline;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Разбор значений CSS: цвета, длины, время, кривые, градиенты, тени, отступы, скругления. */
public final class CssValues {
    private CssValues() {
    }

    /** Делит строку по разделителю верхнего уровня (не внутри скобок). */
    public static List<String> split(String s, char sep) {
        List<String> out = new ArrayList<>();
        int depth = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
            } else if (depth == 0 && (c == sep || (sep == ' ' && Character.isWhitespace(c)))) {
                if (i > start) {
                    String part = s.substring(start, i).trim();
                    if (!part.isEmpty()) {
                        out.add(part);
                    }
                }
                start = i + 1;
            }
        }
        String last = s.substring(start).trim();
        if (!last.isEmpty()) {
            out.add(last);
        }
        return out;
    }

    /** "12px", "12", "1.5em" (×9 — высота строки шрифта) → пиксели. */
    public static float length(String s) {
        String v = s.trim().toLowerCase(Locale.ROOT);
        if (v.endsWith("px")) {
            return Float.parseFloat(v.substring(0, v.length() - 2));
        }
        if (v.endsWith("em")) {
            return Float.parseFloat(v.substring(0, v.length() - 2)) * 9f;
        }
        return Float.parseFloat(v);
    }

    /** Число или процент ("50%" → 0.5). */
    public static float number(String s) {
        String v = s.trim();
        if (v.endsWith("%")) {
            return Float.parseFloat(v.substring(0, v.length() - 1)) / 100f;
        }
        return Float.parseFloat(v);
    }

    /** "0.3s", "300ms" → секунды. */
    public static float time(String s) {
        String v = s.trim().toLowerCase(Locale.ROOT);
        if (v.endsWith("ms")) {
            return Float.parseFloat(v.substring(0, v.length() - 2)) / 1000f;
        }
        if (v.endsWith("s")) {
            return Float.parseFloat(v.substring(0, v.length() - 1));
        }
        return Float.parseFloat(v);
    }

    public static boolean isTime(String s) {
        return s.matches("(?i)-?[0-9.]+m?s");
    }

    /** "45deg", "0.25turn", "1rad", "45" → градусы. */
    public static float angle(String s) {
        String v = s.trim().toLowerCase(Locale.ROOT);
        if (v.endsWith("deg")) {
            return Float.parseFloat(v.substring(0, v.length() - 3));
        }
        if (v.endsWith("turn")) {
            return Float.parseFloat(v.substring(0, v.length() - 4)) * 360f;
        }
        if (v.endsWith("rad")) {
            return (float) Math.toDegrees(Float.parseFloat(v.substring(0, v.length() - 3)));
        }
        return Float.parseFloat(v);
    }

    public static int color(String s) {
        return Color.parse(s.trim());
    }

    public static boolean isColor(String s) {
        try {
            Color.parse(s.trim());
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * Кривая: {@code linear, ease, ease-in, ease-out, ease-in-out, cubic-bezier(...), steps(n)}, а также
     * все встроенные по имени в кебаб-регистре ({@code out-back}, {@code in-out-elastic}, {@code emphasized}).
     */
    public static Easing easing(String s) {
        String v = s.trim().toLowerCase(Locale.ROOT);
        switch (v) {
            case "linear":
                return Easing.LINEAR;
            case "ease":
                return Easing.cubicBezier(0.25f, 0.1f, 0.25f, 1f);
            case "ease-in":
                return Easing.cubicBezier(0.42f, 0f, 1f, 1f);
            case "ease-out":
                return Easing.cubicBezier(0f, 0f, 0.58f, 1f);
            case "ease-in-out":
                return Easing.cubicBezier(0.42f, 0f, 0.58f, 1f);
            default:
                break;
        }
        if (v.startsWith("cubic-bezier(")) {
            List<String> a = args(v);
            return Easing.cubicBezier(Float.parseFloat(a.get(0)), Float.parseFloat(a.get(1)),
                    Float.parseFloat(a.get(2)), Float.parseFloat(a.get(3)));
        }
        if (v.startsWith("steps(")) {
            return Easing.steps(Integer.parseInt(args(v).get(0)));
        }
        if (v.startsWith("spring(")) {
            List<String> a = args(v);
            return Easing.spring(Float.parseFloat(a.get(0)), Float.parseFloat(a.get(1)));
        }
        Easing preset = Easing.presets().get(v.replace('-', '_').toUpperCase(Locale.ROOT));
        if (preset == null) {
            throw new IllegalArgumentException("Unknown easing: " + s);
        }
        return preset;
    }

    public static boolean isEasing(String s) {
        try {
            easing(s);
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Аргументы функции "name(a, b, c)". */
    public static List<String> args(String fn) {
        int open = fn.indexOf('(');
        int close = fn.lastIndexOf(')');
        return split(fn.substring(open + 1, close), ',');
    }

    /** 1–4 значения как в CSS: верх, право, низ, лево. */
    public static Insets insets(String s) {
        List<String> p = split(s, ' ');
        float[] v = new float[p.size()];
        for (int i = 0; i < v.length; i++) {
            v[i] = length(p.get(i));
        }
        return switch (v.length) {
            case 1 -> Insets.all(v[0]);
            case 2 -> Insets.of(v[0], v[1]);
            case 3 -> new Insets(v[0], v[1], v[2], v[1]);
            default -> new Insets(v[0], v[1], v[2], v[3]);
        };
    }

    /** border-radius: 1–4 значения, по часовой с левого верхнего угла. */
    public static Radii radii(String s) {
        List<String> p = split(s, ' ');
        float[] v = new float[p.size()];
        for (int i = 0; i < v.length; i++) {
            v[i] = length(p.get(i));
        }
        return switch (v.length) {
            case 1 -> Radii.all(v[0]);
            case 2 -> new Radii(v[0], v[1], v[0], v[1]);
            case 3 -> new Radii(v[0], v[1], v[2], v[1]);
            default -> new Radii(v[0], v[1], v[2], v[3]);
        };
    }

    /** box-shadow: {@code x y [blur [spread]] color} или {@code none}. */
    public static Shadow shadow(String s) {
        if (s.trim().equalsIgnoreCase("none")) {
            return Shadow.NONE;
        }
        List<Float> nums = new ArrayList<>();
        int color = 0x80000000;
        for (String p : split(s, ' ')) {
            if (isColor(p) && !Character.isDigit(p.charAt(0)) && p.charAt(0) != '-' && p.charAt(0) != '.') {
                color = color(p);
            } else {
                nums.add(length(p));
            }
        }
        float x = nums.size() > 0 ? nums.get(0) : 0f;
        float y = nums.size() > 1 ? nums.get(1) : 0f;
        float blur = nums.size() > 2 ? nums.get(2) : 0f;
        float spread = nums.size() > 3 ? nums.get(3) : 0f;
        return new Shadow(x, y, blur, spread, color);
    }

    /** Свечение: {@code blur [spread] color}. */
    public static Shadow glow(String s) {
        if (s.trim().equalsIgnoreCase("none")) {
            return Shadow.NONE;
        }
        return shadow("0 0 " + s);
    }

    /** outline: {@code width [solid] color [offset]}. */
    public static Outline outline(String s) {
        if (s.trim().equalsIgnoreCase("none")) {
            return Outline.NONE;
        }
        List<Float> nums = new ArrayList<>();
        int color = Color.WHITE;
        for (String p : split(s, ' ')) {
            if (p.equalsIgnoreCase("solid")) {
                continue;
            }
            if (Character.isDigit(p.charAt(0)) || p.charAt(0) == '.' || p.charAt(0) == '-') {
                nums.add(length(p));
            } else {
                color = color(p);
            }
        }
        return new Outline(nums.isEmpty() ? 1f : nums.get(0), nums.size() > 1 ? nums.get(1) : 0f, color);
    }

    public static BlendMode blend(String s) {
        return switch (s.trim().toLowerCase(Locale.ROOT)) {
            case "add", "plus-lighter", "lighter" -> BlendMode.ADD;
            case "multiply" -> BlendMode.MULTIPLY;
            case "screen" -> BlendMode.SCREEN;
            default -> BlendMode.NORMAL;
        };
    }

    /**
     * Кисть: цвет, {@code none}, {@code linear-gradient(135deg, a, b 40%, c)}, {@code radial-gradient(...)},
     * {@code conic-gradient(from 90deg, ...)}, {@code repeating-*}, имя готового градиента ({@code gradient(sunset)}).
     * Пространство смешивания — последним аргументом: {@code in oklch}.
     */
    public static Brush brush(String s) {
        String v = s.trim();
        String lower = v.toLowerCase(Locale.ROOT);
        if (lower.equals("none") || lower.equals("transparent")) {
            return Brush.solid(Color.TRANSPARENT);
        }
        boolean repeating = lower.startsWith("repeating-");
        String fn = repeating ? lower.substring("repeating-".length()) : lower;
        if (fn.startsWith("gradient(")) {
            Brush.Linear preset = Gradients.all().get(args(v).get(0).toUpperCase(Locale.ROOT));
            if (preset == null) {
                throw new IllegalArgumentException("Unknown gradient: " + s);
            }
            return preset;
        }
        if (fn.startsWith("linear-gradient(") || fn.startsWith("radial-gradient(") || fn.startsWith("conic-gradient(")) {
            List<String> a = new ArrayList<>(args(v));
            ColorSpace space = null;
            String first = a.get(0).toLowerCase(Locale.ROOT);
            if (fn.startsWith("linear")) {
                float angle = 180f;
                if (first.startsWith("to ")) {
                    angle = switch (first.substring(3).trim()) {
                        case "top" -> 0f;
                        case "right" -> 90f;
                        case "left" -> 270f;
                        case "top right", "right top" -> 45f;
                        case "bottom right", "right bottom" -> 135f;
                        case "bottom left", "left bottom" -> 225f;
                        case "top left", "left top" -> 315f;
                        default -> 180f;
                    };
                    a.remove(0);
                } else if (!isColor(first) && !first.startsWith("in ")) {
                    angle = angle(first);
                    a.remove(0);
                }
                space = takeSpace(a);
                Brush.Linear g = Brush.linear(angle, stops(a)).withRepeating(repeating);
                return space != null ? g.withSpace(space) : g;
            }
            if (fn.startsWith("radial")) {
                float cx = 0.5f;
                float cy = 0.5f;
                if (!isColor(first) && !first.startsWith("in ")) {
                    int at = first.indexOf("at ");
                    if (at >= 0) {
                        List<String> pos = split(first.substring(at + 3), ' ');
                        cx = number(pos.get(0));
                        cy = pos.size() > 1 ? number(pos.get(1)) : cx;
                    }
                    a.remove(0);
                }
                space = takeSpace(a);
                Brush.Radial g = Brush.radial(stops(a)).at(cx, cy).withRepeating(repeating);
                return space != null ? g.withSpace(space) : g;
            }
            float from = 0f;
            if (first.startsWith("from ")) {
                from = angle(first.substring(5).trim());
                a.remove(0);
            }
            space = takeSpace(a);
            Brush.Conic g = Brush.conic(from, stops(a)).withRepeating(repeating);
            return space != null ? g.withSpace(space) : g;
        }
        return Brush.solid(color(v));
    }

    private static ColorSpace takeSpace(List<String> args) {
        String last = args.get(args.size() - 1).trim().toLowerCase(Locale.ROOT);
        if (last.startsWith("in ")) {
            args.remove(args.size() - 1);
            return ColorSpace.valueOf(last.substring(3).trim().toUpperCase(Locale.ROOT).replace("SRGB-LINEAR", "LINEAR"));
        }
        return null;
    }

    /** Цвета с необязательными позициями: "red", "blue 40%", "green 40% 60%" (жёсткий участок). */
    private static ColorStops stops(List<String> args) {
        int n = args.size();
        int[] colors = new int[n];
        float[] pos = new float[n];
        boolean anyPos = false;
        List<float[]> extra = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<String> p = split(args.get(i), ' ');
            colors[i] = color(p.get(0));
            if (p.size() > 1) {
                pos[i] = number(p.get(1));
                anyPos = true;
                if (p.size() > 2) {
                    extra.add(new float[]{i, number(p.get(2))});
                }
            } else {
                pos[i] = Float.NaN;
            }
        }
        if (!anyPos) {
            return ColorStops.even(colors);
        }
        // Пропущенные позиции — равномерно между известными, как в CSS.
        if (Float.isNaN(pos[0])) {
            pos[0] = 0f;
        }
        if (Float.isNaN(pos[n - 1])) {
            pos[n - 1] = 1f;
        }
        for (int i = 1; i < n - 1; i++) {
            if (Float.isNaN(pos[i])) {
                int j = i;
                while (Float.isNaN(pos[j])) {
                    j++;
                }
                for (int k = i; k < j; k++) {
                    pos[k] = pos[i - 1] + (pos[j] - pos[i - 1]) * (k - i + 1) / (j - i + 1);
                }
            }
        }
        ColorStops.Builder b = ColorStops.builder();
        int e = 0;
        for (int i = 0; i < n; i++) {
            b.add(pos[i], colors[i]);
            if (e < extra.size() && extra.get(e)[0] == i) {
                b.add(extra.get(e)[1], colors[i]);
                e++;
            }
        }
        return b.build();
    }
}
