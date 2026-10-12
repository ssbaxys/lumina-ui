package io.github.ssbaxys.lumina.render;

/** Утилиты для цветов в формате ARGB, упакованных в int (без аллокаций). */
public final class Color {
    public static final int TRANSPARENT = 0x00000000;
    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0xFF000000;

    private Color() {
    }

    public static int argb(int a, int r, int g, int b) {
        return (a & 255) << 24 | (r & 255) << 16 | (g & 255) << 8 | (b & 255);
    }

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

    public static int withAlpha(int c, int a) {
        return (c & 0x00FFFFFF) | (a & 255) << 24;
    }

    public static int withAlpha(int c, float a) {
        return withAlpha(c, Math.round(clamp01(a) * 255f));
    }

    /** Умножает альфу цвета на коэффициент (так применяется прозрачность контейнеров). */
    public static int mulAlpha(int c, float factor) {
        if (factor >= 1f) {
            return c;
        }
        return withAlpha(c, Math.round(alpha(c) * clamp01(factor)));
    }

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

    /** Разбирает "#RGB", "#RRGGBB" или "#AARRGGBB". */
    public static int hex(String s) {
        String h = s.startsWith("#") ? s.substring(1) : s;
        return switch (h.length()) {
            case 3 -> {
                int r = Character.digit(h.charAt(0), 16);
                int g = Character.digit(h.charAt(1), 16);
                int b = Character.digit(h.charAt(2), 16);
                yield argb(255, r * 17, g * 17, b * 17);
            }
            case 6 -> 0xFF000000 | Integer.parseUnsignedInt(h, 16);
            case 8 -> Integer.parseUnsignedInt(h, 16);
            default -> throw new IllegalArgumentException("Bad color: " + s);
        };
    }

    private static int lerpChannel(int a, int b, float t) {
        return Math.round(a + (b - a) * t);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }
}
