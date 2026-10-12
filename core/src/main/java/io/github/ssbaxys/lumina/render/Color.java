package io.github.ssbaxys.lumina.render;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Утилиты для цветов в формате ARGB, упакованных в int (без аллокаций).
 *
 * <p>Цвет можно задать как угодно: {@link #rgb}, {@link #hsv}, {@link #hsl}, перцептивные
 * {@link #oklch}/{@link #oklab}, строкой {@link #parse} ("#FF8800", "rgb(255 136 0)", "hsl(30 100% 50%)",
 * "oklch(0.7 0.18 50)", "orange"). Операции вроде {@link #lighten} и {@link #rotateHue} работают в OKLCH,
 * поэтому «светлее на 10%» выглядит одинаково для любого оттенка.</p>
 */
public final class Color {
    public static final int TRANSPARENT = 0x00000000;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;

    private static final Map<String, Integer> NAMED = new HashMap<>();

    static {
        named("transparent", TRANSPARENT);
        named("white", WHITE);
        named("black", BLACK);
        named("red", 0xFFFF0000);
        named("green", 0xFF008000);
        named("lime", 0xFF00FF00);
        named("blue", 0xFF0000FF);
        named("yellow", 0xFFFFFF00);
        named("cyan", 0xFF00FFFF);
        named("aqua", 0xFF00FFFF);
        named("magenta", 0xFFFF00FF);
        named("fuchsia", 0xFFFF00FF);
        named("orange", 0xFFFFA500);
        named("purple", 0xFF800080);
        named("violet", 0xFFEE82EE);
        named("pink", 0xFFFFC0CB);
        named("hotpink", 0xFFFF69B4);
        named("gold", 0xFFFFD700);
        named("coral", 0xFFFF7F50);
        named("tomato", 0xFFFF6347);
        named("crimson", 0xFFDC143C);
        named("teal", 0xFF008080);
        named("navy", 0xFF000080);
        named("indigo", 0xFF4B0082);
        named("turquoise", 0xFF40E0D0);
        named("skyblue", 0xFF87CEEB);
        named("deepskyblue", 0xFF00BFFF);
        named("royalblue", 0xFF4169E1);
        named("springgreen", 0xFF00FF7F);
        named("chartreuse", 0xFF7FFF00);
        named("salmon", 0xFFFA8072);
        named("orchid", 0xFFDA70D6);
        named("silver", 0xFFC0C0C0);
        named("gray", 0xFF808080);
        named("grey", 0xFF808080);
        named("darkgray", 0xFFA9A9A9);
        named("lightgray", 0xFFD3D3D3);
        named("brown", 0xFFA52A2A);
        named("maroon", 0xFF800000);
        named("olive", 0xFF808000);
    }

    private Color() {
    }

    private static void named(String name, int argb) {
        NAMED.put(name, argb);
    }

    // ================================================================ создание

    public static int argb(int a, int r, int g, int b) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }

    /** Непрозрачный цвет из каналов 0..255. */
    public static int rgb(int r, int g, int b) {
        return argb(255, clamp255(r), clamp255(g), clamp255(b));
    }

    /** Каналы 0..255, альфа 0..1. */
    public static int rgba(int r, int g, int b, float alpha) {
        return argb(toByte(alpha), clamp255(r), clamp255(g), clamp255(b));
    }

    /** Каналы и альфа 0..1. */
    public static int rgbaF(float r, float g, float b, float a) {
        return argb(toByte(a), toByte(r), toByte(g), toByte(b));
    }

    /** Оттенок в градусах, насыщенность и яркость 0..1. */
    public static int hsv(float hueDeg, float saturation, float value) {
        return hsva(hueDeg, saturation, value, 1f);
    }

    public static int hsva(float hueDeg, float saturation, float value, float alpha) {
        float h = wrapHue(hueDeg) / 60f;
        float s = clamp01(saturation);
        float v = clamp01(value);
        float c = v * s;
        float x = c * (1f - Math.abs(h % 2f - 1f));
        float m = v - c;
        float r;
        float g;
        float b;
        switch ((int) h) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return rgbaF(r + m, g + m, b + m, alpha);
    }

    /** Оттенок в градусах, насыщенность и светлота 0..1. */
    public static int hsl(float hueDeg, float saturation, float lightness) {
        return hsla(hueDeg, saturation, lightness, 1f);
    }

    public static int hsla(float hueDeg, float saturation, float lightness, float alpha) {
        float l = clamp01(lightness);
        float s = clamp01(saturation);
        float v = l + s * Math.min(l, 1f - l);
        float sv = v == 0f ? 0f : 2f * (1f - l / v);
        return hsva(hueDeg, sv, v, alpha);
    }

    /** OKLab: L 0..1, a и b примерно −0.4..0.4. */
    public static int oklab(float l, float a, float b) {
        return oklaba(l, a, b, 1f);
    }

    public static int oklaba(float l, float a, float b, float alpha) {
        float[] rgb = new float[3];
        oklabToLinear(l, a, b, rgb);
        return rgbaF(linearToSrgb(rgb[0]), linearToSrgb(rgb[1]), linearToSrgb(rgb[2]), alpha);
    }

    /** OKLCH: светлота 0..1, насыщенность (chroma) 0..~0.37, оттенок в градусах. Перцептивно ровный HSL. */
    public static int oklch(float l, float chroma, float hueDeg) {
        return oklcha(l, chroma, hueDeg, 1f);
    }

    public static int oklcha(float l, float chroma, float hueDeg, float alpha) {
        double h = Math.toRadians(hueDeg);
        return oklaba(l, (float) (chroma * Math.cos(h)), (float) (chroma * Math.sin(h)), alpha);
    }

    /** Цвет радуги по доле 0..1 (полный круг оттенков, яркий и ровный по светлоте). */
    public static int rainbow(float t) {
        return oklch(0.75f, 0.17f, t * 360f + 25f);
    }

    // ================================================================ каналы

    public static int alpha(int c) {
        return c >>> 24;
    }

    public static int red(int c) {
        return (c >> 16) & 255;
    }

    public static int green(int c) {
        return (c >> 8) & 255;
    }

    public static int blue(int c) {
        return c & 255;
    }

    public static float alphaF(int c) {
        return alpha(c) / 255f;
    }

    public static int withAlpha(int c, int a) {
        return (c & 0x00FFFFFF) | (a & 255) << 24;
    }

    public static int withAlpha(int c, float a) {
        return withAlpha(c, toByte(a));
    }

    public static int withRed(int c, int r) {
        return (c & 0xFF00FFFF) | clamp255(r) << 16;
    }

    public static int withGreen(int c, int g) {
        return (c & 0xFFFF00FF) | clamp255(g) << 8;
    }

    public static int withBlue(int c, int b) {
        return (c & 0xFFFFFF00) | clamp255(b);
    }

    /** Умножает альфу цвета на коэффициент (так применяется прозрачность контейнеров). */
    public static int mulAlpha(int c, float factor) {
        if (factor >= 1f) {
            return c;
        }
        return withAlpha(c, Math.round(alpha(c) * clamp01(factor)));
    }

    // ================================================================ разбор в модели

    /** HSV: out = {оттенок°, насыщенность, яркость}. */
    public static float[] toHsv(int c, float[] out) {
        float r = red(c) / 255f;
        float g = green(c) / 255f;
        float b = blue(c) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        float h;
        if (d == 0f) {
            h = 0f;
        } else if (max == r) {
            h = 60f * (((g - b) / d) % 6f);
        } else if (max == g) {
            h = 60f * ((b - r) / d + 2f);
        } else {
            h = 60f * ((r - g) / d + 4f);
        }
        out[0] = wrapHue(h);
        out[1] = max == 0f ? 0f : d / max;
        out[2] = max;
        return out;
    }

    /** HSL: out = {оттенок°, насыщенность, светлота}. */
    public static float[] toHsl(int c, float[] out) {
        toHsv(c, out);
        float v = out[2];
        float l = v * (1f - out[1] / 2f);
        out[1] = (l == 0f || l == 1f) ? 0f : (v - l) / Math.min(l, 1f - l);
        out[2] = l;
        return out;
    }

    /** OKLab: out = {L, a, b}. */
    public static float[] toOklab(int c, float[] out) {
        linearToOklab(srgbToLinear(red(c) / 255f), srgbToLinear(green(c) / 255f), srgbToLinear(blue(c) / 255f), out);
        return out;
    }

    /** OKLCH: out = {L, chroma, оттенок°}. */
    public static float[] toOklch(int c, float[] out) {
        toOklab(c, out);
        float a = out[1];
        float b = out[2];
        out[1] = (float) Math.sqrt(a * a + b * b);
        out[2] = wrapHue((float) Math.toDegrees(Math.atan2(b, a)));
        return out;
    }

    public static float hue(int c) {
        return toHsv(c, new float[3])[0];
    }

    // ================================================================ операции (перцептивные, OKLCH)

    /** Светлее на {@code amount} (0..1 по шкале светлоты OKLCH). */
    public static int lighten(int c, float amount) {
        float[] lch = toOklch(c, new float[3]);
        return oklcha(clamp01(lch[0] + amount), lch[1], lch[2], alphaF(c));
    }

    public static int darken(int c, float amount) {
        return lighten(c, -amount);
    }

    /** Насыщеннее: chroma × (1 + amount). Отрицательное значение — бледнее. */
    public static int saturate(int c, float amount) {
        float[] lch = toOklch(c, new float[3]);
        return oklcha(lch[0], Math.max(0f, lch[1] * (1f + amount)), lch[2], alphaF(c));
    }

    public static int desaturate(int c, float amount) {
        return saturate(c, -amount);
    }

    /** Повернуть оттенок по кругу OKLCH, сохраняя светлоту и насыщенность. */
    public static int rotateHue(int c, float degrees) {
        float[] lch = toOklch(c, new float[3]);
        return oklcha(lch[0], lch[1], lch[2] + degrees, alphaF(c));
    }

    /** Заменить оттенок (в градусах HSV), сохранив насыщенность и яркость. */
    public static int withHue(int c, float hueDeg) {
        float[] hsv = toHsv(c, new float[3]);
        return hsva(hueDeg, hsv[1], hsv[2], alphaF(c));
    }

    public static int complement(int c) {
        return rotateHue(c, 180f);
    }

    /** Оттенки серого с той же воспринимаемой светлотой. */
    public static int grayscale(int c) {
        float[] lab = toOklab(c, new float[3]);
        return oklaba(lab[0], 0f, 0f, alphaF(c));
    }

    public static int invert(int c) {
        return (c & 0xFF000000) | (~c & 0x00FFFFFF);
    }

    /** Относительная яркость по WCAG (0 — чёрный, 1 — белый). */
    public static float luminance(int c) {
        return 0.2126f * srgbToLinear(red(c) / 255f)
                + 0.7152f * srgbToLinear(green(c) / 255f)
                + 0.0722f * srgbToLinear(blue(c) / 255f);
    }

    /** Контраст по WCAG: от 1 до 21. */
    public static float contrast(int a, int b) {
        float la = luminance(a);
        float lb = luminance(b);
        return (Math.max(la, lb) + 0.05f) / (Math.min(la, lb) + 0.05f);
    }

    /** Чёрный или белый — что читается лучше на фоне {@code background}. */
    public static int readableOn(int background) {
        return contrast(background, WHITE) >= contrast(background, BLACK) ? WHITE : BLACK;
    }

    /** Наложить {@code top} поверх {@code bottom} с учётом альфы (как краска поверх краски). */
    public static int over(int top, int bottom) {
        float ta = alphaF(top);
        if (ta >= 1f) {
            return top;
        }
        float ba = alphaF(bottom);
        float oa = ta + ba * (1f - ta);
        if (oa <= 0f) {
            return TRANSPARENT;
        }
        float k = ba * (1f - ta);
        return rgbaF(
                (red(top) / 255f * ta + red(bottom) / 255f * k) / oa,
                (green(top) / 255f * ta + green(bottom) / 255f * k) / oa,
                (blue(top) / 255f * ta + blue(bottom) / 255f * k) / oa,
                oa);
    }

    // ================================================================ интерполяция

    /** Быстрое смешивание в sRGB (используется в анимациях стиля каждый кадр). */
    public static int lerp(int a, int b, float t) {
        if (t <= 0f) {
            return a;
        }
        if (t >= 1f) {
            return b;
        }
        return argb(
                lerpChannel(alpha(a), alpha(b), t),
                lerpChannel(red(a), red(b), t),
                lerpChannel(green(a), green(b), t),
                lerpChannel(blue(a), blue(b), t));
    }

    /** Смешивание в выбранном цветовом пространстве (OKLAB — самые чистые переходы без «грязи»). */
    public static int lerp(int a, int b, float t, ColorSpace space) {
        return space.lerp(a, b, t);
    }

    public static int mix(int a, int b, float t) {
        return lerp(a, b, t, ColorSpace.OKLAB);
    }

    // ================================================================ строки

    /** Разбирает "#RGB", "#RGBA", "#RRGGBB" или "#AARRGGBB". */
    public static int hex(String s) {
        String h = s.startsWith("#") ? s.substring(1) : s;
        return switch (h.length()) {
            case 3 -> argb(255, nibble(h, 0) * 17, nibble(h, 1) * 17, nibble(h, 2) * 17);
            case 4 -> argb(nibble(h, 3) * 17, nibble(h, 0) * 17, nibble(h, 1) * 17, nibble(h, 2) * 17);
            case 6 -> 0xFF000000 | Integer.parseUnsignedInt(h, 16);
            case 8 -> Integer.parseUnsignedInt(h, 16);
            default -> throw new IllegalArgumentException("Bad color: " + s);
        };
    }

    /** "#AARRGGBB" (или "#RRGGBB" для непрозрачных). */
    public static String toHex(int c) {
        return alpha(c) == 255
                ? String.format(Locale.ROOT, "#%06X", c & 0xFFFFFF)
                : String.format(Locale.ROOT, "#%08X", c);
    }

    /**
     * Цвет в любом привычном формате: hex, имя, {@code rgb()/rgba()}, {@code hsl()/hsla()},
     * {@code hsv()}, {@code oklch()}, {@code oklab()}. Разделители — пробелы, запятые или "/" перед альфой;
     * проценты поддерживаются. Например: {@code "rgb(255 128 0 / 50%)"}, {@code "hsl(200, 80%, 60%)"}.
     */
    public static int parse(String s) {
        String str = s.trim().toLowerCase(Locale.ROOT);
        if (str.startsWith("#")) {
            return hex(str);
        }
        Integer named = NAMED.get(str);
        if (named != null) {
            return named;
        }
        int open = str.indexOf('(');
        if (open < 0 || !str.endsWith(")")) {
            throw new IllegalArgumentException("Bad color: " + s);
        }
        String fn = str.substring(0, open).trim();
        String[] parts = str.substring(open + 1, str.length() - 1).trim().split("[\\s,/]+");
        if (parts.length < 3 || parts.length > 4) {
            throw new IllegalArgumentException("Bad color: " + s);
        }
        float alpha = parts.length == 4 ? number(parts[3], 1f) : 1f;
        return switch (fn) {
            case "rgb", "rgba" -> rgbaF(number(parts[0], 255f), number(parts[1], 255f), number(parts[2], 255f), alpha);
            case "hsl", "hsla" -> hsla(angle(parts[0]), number(parts[1], 1f), number(parts[2], 1f), alpha);
            case "hsv", "hsva", "hsb" -> hsva(angle(parts[0]), number(parts[1], 1f), number(parts[2], 1f), alpha);
            case "oklch" -> oklcha(number(parts[0], 1f), percentOf(parts[1], 0.4f), angle(parts[2]), alpha);
            case "oklab" -> oklaba(number(parts[0], 1f), percentOf(parts[1], 0.4f), percentOf(parts[2], 0.4f), alpha);
            default -> throw new IllegalArgumentException("Unknown color function: " + s);
        };
    }

    /** Число из строки: "50%" → 0.5; иначе value / scale (так "255" для канала даёт 1.0). */
    private static float number(String p, float scale) {
        if (p.endsWith("%")) {
            return Float.parseFloat(p.substring(0, p.length() - 1)) / 100f;
        }
        return Float.parseFloat(p) / scale;
    }

    /** Как есть; "100%" соответствует {@code full}. */
    private static float percentOf(String p, float full) {
        return p.endsWith("%") ? Float.parseFloat(p.substring(0, p.length() - 1)) / 100f * full : Float.parseFloat(p);
    }

    private static float angle(String p) {
        if (p.endsWith("deg")) {
            return Float.parseFloat(p.substring(0, p.length() - 3));
        }
        if (p.endsWith("turn")) {
            return Float.parseFloat(p.substring(0, p.length() - 4)) * 360f;
        }
        if (p.endsWith("rad")) {
            return (float) Math.toDegrees(Float.parseFloat(p.substring(0, p.length() - 3)));
        }
        return Float.parseFloat(p);
    }

    private static int nibble(String h, int i) {
        int d = Character.digit(h.charAt(i), 16);
        if (d < 0) {
            throw new IllegalArgumentException("Bad color: #" + h);
        }
        return d;
    }

    // ================================================================ математика пространств

    public static float srgbToLinear(float c) {
        return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
    }

    public static float linearToSrgb(float c) {
        return c <= 0.0031308f ? c * 12.92f : 1.055f * (float) Math.pow(c, 1f / 2.4f) - 0.055f;
    }

    static void linearToOklab(float r, float g, float b, float[] out) {
        float l = (float) Math.cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b);
        float m = (float) Math.cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b);
        float s = (float) Math.cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b);
        out[0] = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s;
        out[1] = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s;
        out[2] = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s;
    }

    static void oklabToLinear(float lightness, float a, float b, float[] out) {
        float l = lightness + 0.3963377774f * a + 0.2158037573f * b;
        float m = lightness - 0.1055613458f * a - 0.0638541728f * b;
        float s = lightness - 0.0894841775f * a - 1.2914855480f * b;
        l = l * l * l;
        m = m * m * m;
        s = s * s * s;
        out[0] = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
        out[1] = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s;
        out[2] = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s;
    }

    static float wrapHue(float h) {
        float r = h % 360f;
        return r < 0f ? r + 360f : r;
    }

    private static int lerpChannel(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    static int toByte(float v) {
        return Math.round(clamp01(v) * 255f);
    }

    private static int clamp255(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }
}
