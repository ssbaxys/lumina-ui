package io.github.ssbaxys.lumina.render;

/**
 * Градиент, подготовленный для шейдера: плоские числа, готовые для uniform-ов. Один экземпляр
 * переиспользуется канвасом на каждой фигуре, поэтому градиенты не создают мусора.
 *
 * <p>Геометрия — в экранных пикселях относительно центра фигуры (как {@code localPos} в шейдере):</p>
 * <ul>
 *   <li>{@link #LINEAR}: (gx, gy, 0.5, 0) — параметр = dot(p, g) + 0.5;</li>
 *   <li>{@link #RADIAL}: (cx, cy, 1/rx, 1/ry) — параметр = |(p − c) / r|;</li>
 *   <li>{@link #CONIC}: (cx, cy, начальный угол в радианах, 0).</li>
 * </ul>
 * Цвета — RGBA 0..1 без предумножения, опора i в {@code colors[4i..4i+3]}.
 */
public final class ShapePaint {
    /** Цвета вершин, без градиента в шейдере. */
    public static final int VERTEX = 0;
    public static final int LINEAR = 1;
    public static final int RADIAL = 2;
    public static final int CONIC = 3;

    int kind;
    int stopCount;
    int space;
    boolean repeating;
    float offset;
    final float[] geometry = new float[4];
    final float[] colors = new float[ColorStops.MAX * 4];
    final float[] positions = new float[ColorStops.MAX];

    public int kind() {
        return kind;
    }

    public int stopCount() {
        return stopCount;
    }

    /** {@link ColorSpace#shaderId()}. */
    public int space() {
        return space;
    }

    public boolean repeating() {
        return repeating;
    }

    public float offset() {
        return offset;
    }

    public float geometry(int i) {
        return geometry[i];
    }

    /** Канал {@code channel} (0 = R … 3 = A) опоры {@code stop}; за пределами stopCount — последняя опора. */
    public float color(int stop, int channel) {
        return colors[Math.min(stop, Math.max(stopCount - 1, 0)) * 4 + channel];
    }

    /** Позиция опоры; за пределами stopCount — 1 + индекс, чтобы шейдер их никогда не выбрал. */
    public float position(int stop) {
        return stop < stopCount ? positions[stop] : 2f + stop;
    }

    void setGradient(Brush.Gradient g, float x0, float y0, float x1, float y1, float opacity) {
        float w = x1 - x0;
        float h = y1 - y0;
        ColorStops stops = g.stops();
        stopCount = stops.size();
        for (int i = 0; i < stopCount; i++) {
            int c = Color.mulAlpha(stops.color(i), opacity);
            colors[4 * i] = Color.red(c) / 255f;
            colors[4 * i + 1] = Color.green(c) / 255f;
            colors[4 * i + 2] = Color.blue(c) / 255f;
            colors[4 * i + 3] = Color.alpha(c) / 255f;
            positions[i] = stops.position(i);
        }
        space = g.space().shaderId();
        repeating = g.repeating();
        offset = g.offset();
        if (g instanceof Brush.Linear l) {
            kind = LINEAR;
            double a = Math.toRadians(l.angle());
            float dx = (float) Math.sin(a);
            float dy = (float) -Math.cos(a);
            float len = Math.max(Math.abs(w * dx) + Math.abs(h * dy), 1e-4f);
            geometry[0] = dx / len;
            geometry[1] = dy / len;
            geometry[2] = 0.5f;
            geometry[3] = 0f;
        } else if (g instanceof Brush.Radial r) {
            kind = RADIAL;
            geometry[0] = (r.centerX() - 0.5f) * w;
            geometry[1] = (r.centerY() - 0.5f) * h;
            geometry[2] = 1f / Math.max(r.radiusX() * w, 1e-4f);
            geometry[3] = 1f / Math.max(r.radiusY() * h, 1e-4f);
        } else {
            Brush.Conic c = (Brush.Conic) g;
            kind = CONIC;
            geometry[0] = (c.centerX() - 0.5f) * w;
            geometry[1] = (c.centerY() - 0.5f) * h;
            geometry[2] = (float) Math.toRadians(c.angle());
            geometry[3] = 0f;
        }
    }
}
