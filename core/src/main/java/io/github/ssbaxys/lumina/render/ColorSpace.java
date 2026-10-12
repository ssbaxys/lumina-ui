package io.github.ssbaxys.lumina.render;

/**
 * Пространство, в котором смешиваются цвета градиентов и переходов.
 *
 * <ul>
 *   <li>{@link #SRGB} — как в старых браузерах: быстро, но середина красного и зелёного «грязная».</li>
 *   <li>{@link #LINEAR} — физически корректное смешивание света, светлее в середине.</li>
 *   <li>{@link #OKLAB} — перцептивно ровное, без серой середины (по умолчанию для градиентов, как в CSS).</li>
 *   <li>{@link #OKLCH}, {@link #HSV} — идут по кругу оттенков (самым коротким путём): радужные переходы.</li>
 * </ul>
 *
 * <p>Смешивание идёт с предумноженной альфой, поэтому переход в прозрачность не темнеет.
 * Шейдер повторяет те же формулы, так что CPU-запасной путь и GPU рисуют одинаково.</p>
 */
public enum ColorSpace {
    SRGB,
    LINEAR,
    OKLAB,
    OKLCH,
    HSV;

    /** Номер для шейдера (uniform GradMode.z). */
    public int shaderId() {
        return ordinal();
    }

    public int lerp(int a, int b, float t) {
        if (t <= 0f) {
            return a;
        }
        if (t >= 1f) {
            return b;
        }
        float aa = Color.alphaF(a);
        float ba = Color.alphaF(b);
        float alpha = aa + (ba - aa) * t;
        if (alpha <= 0f) {
            return Color.TRANSPARENT;
        }
        float[] x = new float[3];
        float[] y = new float[3];
        toSpace(a, x);
        toSpace(b, y);
        float[] o = new float[3];
        if (this == OKLCH || this == HSV) {
            int hue = this == OKLCH ? 2 : 0;
            int other1 = this == OKLCH ? 0 : 1;
            int other2 = this == OKLCH ? 1 : 2;
            // У бесцветного (или невидимого) конца оттенок не определён — берём оттенок другого конца.
            float chromaA = this == OKLCH ? x[1] : x[1] * x[2];
            float chromaB = this == OKLCH ? y[1] : y[1] * y[2];
            float ha = chromaA < 1e-4f || aa == 0f ? y[hue] : x[hue];
            float hb = chromaB < 1e-4f || ba == 0f ? x[hue] : y[hue];
            float dh = hb - ha;
            if (dh > 180f) {
                dh -= 360f;
            } else if (dh < -180f) {
                dh += 360f;
            }
            o[hue] = ha + dh * t;
            o[other1] = (x[other1] * aa + (y[other1] * ba - x[other1] * aa) * t) / alpha;
            o[other2] = (x[other2] * aa + (y[other2] * ba - x[other2] * aa) * t) / alpha;
        } else {
            for (int i = 0; i < 3; i++) {
                o[i] = (x[i] * aa + (y[i] * ba - x[i] * aa) * t) / alpha;
            }
        }
        return fromSpace(o, alpha);
    }

    private void toSpace(int c, float[] out) {
        switch (this) {
            case SRGB -> {
                out[0] = Color.red(c) / 255f;
                out[1] = Color.green(c) / 255f;
                out[2] = Color.blue(c) / 255f;
            }
            case LINEAR -> {
                out[0] = Color.srgbToLinear(Color.red(c) / 255f);
                out[1] = Color.srgbToLinear(Color.green(c) / 255f);
                out[2] = Color.srgbToLinear(Color.blue(c) / 255f);
            }
            case OKLAB -> Color.toOklab(c, out);
            case OKLCH -> Color.toOklch(c, out);
            case HSV -> Color.toHsv(c, out);
        }
    }

    private int fromSpace(float[] v, float alpha) {
        return switch (this) {
            case SRGB -> Color.rgbaF(v[0], v[1], v[2], alpha);
            case LINEAR -> Color.rgbaF(Color.linearToSrgb(v[0]), Color.linearToSrgb(v[1]), Color.linearToSrgb(v[2]), alpha);
            case OKLAB -> Color.oklaba(v[0], v[1], v[2], alpha);
            case OKLCH -> Color.oklcha(v[0], v[1], v[2], alpha);
            case HSV -> Color.hsva(v[0], v[1], v[2], alpha);
        };
    }
}
