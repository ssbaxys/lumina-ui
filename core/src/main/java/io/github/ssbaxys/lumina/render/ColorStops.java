package io.github.ssbaxys.lumina.render;

import java.util.Arrays;

/**
 * Неизменяемый список опорных цветов градиента: позиции 0..1 по возрастанию и цвета.
 * Одинаковые позиции подряд дают резкую границу (полосы, сектора). До {@link #MAX} опор —
 * столько помещается в uniform-ы шейдера.
 */
public final class ColorStops {
    public static final int MAX = 8;

    private final float[] positions;
    private final int[] colors;

    private ColorStops(float[] positions, int[] colors) {
        this.positions = positions;
        this.colors = colors;
    }

    /** Цвета, равномерно распределённые от 0 до 1. */
    public static ColorStops even(int... colors) {
        if (colors.length == 0) {
            throw new IllegalArgumentException("Gradient needs at least one color");
        }
        if (colors.length == 1) {
            return new ColorStops(new float[]{0f, 1f}, new int[]{colors[0], colors[0]});
        }
        float[] pos = new float[colors.length];
        for (int i = 0; i < pos.length; i++) {
            pos[i] = i / (float) (pos.length - 1);
        }
        return of(pos, colors);
    }

    /** Опоры по одной: {@code ColorStops.builder().add(0f, RED).add(0.3f, ORANGE).add(1f, YELLOW).build()}. */
    public static Builder builder() {
        return new Builder();
    }

    public static ColorStops of(float[] positions, int[] colors) {
        if (positions.length != colors.length || positions.length == 0) {
            throw new IllegalArgumentException("positions and colors must have the same non-zero length");
        }
        if (positions.length > MAX) {
            throw new IllegalArgumentException("At most " + MAX + " color stops are supported, got " + positions.length);
        }
        float[] p = positions.clone();
        float prev = 0f;
        for (int i = 0; i < p.length; i++) {
            // Как в CSS: позиция не может быть меньше предыдущей.
            p[i] = Math.max(prev, Math.max(0f, Math.min(1f, p[i])));
            prev = p[i];
        }
        if (p.length == 1) {
            return new ColorStops(new float[]{0f, 1f}, new int[]{colors[0], colors[0]});
        }
        return new ColorStops(p, colors.clone());
    }

    /** Те же позиции, все цвета одного значения (для плавного перехода из сплошной заливки). */
    public static ColorStops uniform(ColorStops shape, int color) {
        int[] c = new int[shape.size()];
        Arrays.fill(c, color);
        return new ColorStops(shape.positions, c);
    }

    public int size() {
        return colors.length;
    }

    public float position(int i) {
        return positions[i];
    }

    public int color(int i) {
        return colors[i];
    }

    public boolean isInvisible() {
        for (int c : colors) {
            if (Color.alpha(c) != 0) {
                return false;
            }
        }
        return true;
    }

    /** Цвет в точке {@code t} (0..1) при смешивании в {@code space}. */
    public int sample(float t, ColorSpace space) {
        int n = colors.length;
        if (t <= positions[0]) {
            return colors[0];
        }
        for (int i = 1; i < n; i++) {
            if (t <= positions[i]) {
                float span = positions[i] - positions[i - 1];
                float f = span <= 1e-5f ? 1f : (t - positions[i - 1]) / span;
                return space.lerp(colors[i - 1], colors[i], f);
            }
        }
        return colors[n - 1];
    }

    public ColorStops map(java.util.function.IntUnaryOperator colorOp) {
        int[] c = new int[colors.length];
        for (int i = 0; i < c.length; i++) {
            c[i] = colorOp.applyAsInt(colors[i]);
        }
        return new ColorStops(positions, c);
    }

    public ColorStops withAlpha(float factor) {
        return map(c -> Color.mulAlpha(c, factor));
    }

    /** Те же цвета в обратном порядке. */
    public ColorStops reversed() {
        int n = colors.length;
        float[] p = new float[n];
        int[] c = new int[n];
        for (int i = 0; i < n; i++) {
            p[i] = 1f - positions[n - 1 - i];
            c[i] = colors[n - 1 - i];
        }
        return new ColorStops(p, c);
    }

    /**
     * Интерполяция двух наборов. Если позиции различаются, оба набора пересэмплируются на общие
     * позиции (когда их не больше {@link #MAX}), иначе возвращается ближайший конец.
     */
    public static ColorStops lerp(ColorStops a, ColorStops b, float t, ColorSpace space) {
        if (a.size() == b.size()) {
            float[] p = new float[a.size()];
            int[] c = new int[a.size()];
            for (int i = 0; i < p.length; i++) {
                p[i] = a.positions[i] + (b.positions[i] - a.positions[i]) * t;
                c[i] = Color.lerp(a.colors[i], b.colors[i], t);
            }
            return new ColorStops(p, c);
        }
        float[] union = union(a.positions, b.positions);
        if (union == null) {
            return t < 0.5f ? a : b;
        }
        int[] c = new int[union.length];
        for (int i = 0; i < union.length; i++) {
            c[i] = Color.lerp(a.sample(union[i], space), b.sample(union[i], space), t);
        }
        return new ColorStops(union, c);
    }

    private static float[] union(float[] a, float[] b) {
        float[] all = new float[a.length + b.length];
        System.arraycopy(a, 0, all, 0, a.length);
        System.arraycopy(b, 0, all, a.length, b.length);
        Arrays.sort(all);
        int n = 0;
        for (int i = 0; i < all.length; i++) {
            if (n == 0 || all[i] - all[n - 1] > 1e-4f) {
                all[n++] = all[i];
            }
        }
        return n <= MAX ? Arrays.copyOf(all, n) : null;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof ColorStops s && Arrays.equals(positions, s.positions) && Arrays.equals(colors, s.colors));
    }

    @Override
    public int hashCode() {
        return 31 * Arrays.hashCode(positions) + Arrays.hashCode(colors);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ColorStops[");
        for (int i = 0; i < colors.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(Color.toHex(colors[i])).append(' ').append(positions[i]);
        }
        return sb.append(']').toString();
    }

    public static final class Builder {
        private final float[] positions = new float[MAX];
        private final int[] colors = new int[MAX];
        private int size;

        private Builder() {
        }

        public Builder add(float position, int color) {
            if (size == MAX) {
                throw new IllegalArgumentException("At most " + MAX + " color stops are supported");
            }
            positions[size] = position;
            colors[size] = color;
            size++;
            return this;
        }

        /** Резкая граница: два цвета в одной позиции. */
        public Builder hard(float position, int before, int after) {
            return add(position, before).add(position, after);
        }

        public ColorStops build() {
            return of(Arrays.copyOf(positions, size), Arrays.copyOf(colors, size));
        }
    }
}
