package io.github.ssbaxys.lumina.render;

import java.util.function.IntUnaryOperator;

/**
 * Заливка фигуры: сплошной цвет или градиент (линейный, радиальный, конический).
 *
 * <p>Градиенты считаются в фрагментном шейдере: до {@link ColorStops#MAX} опор, любое пространство
 * смешивания ({@link ColorSpace}), повторение и сдвиг для бегущих анимаций. Все кисти — неизменяемые
 * значения с {@code equals}, поэтому их можно хранить в стилях и плавно переводить одну в другую.</p>
 *
 * <pre>{@code
 * Brush.linear(135, 0xFFF0506E, 0xFFA855F7)            // по диагонали, смешивание в OKLAB
 * Brush.radial(Color.WHITE, Color.TRANSPARENT)         // мягкое пятно света
 * Brush.conic(0, Color.rainbow(0), ..., Color.rainbow(1))
 * Gradients.SUNSET.rotate(90).repeating()
 * }</pre>
 */
public sealed interface Brush permits Brush.Solid, Brush.Gradient {

    record Solid(int color) implements Brush {
        @Override
        public boolean isInvisible() {
            return Color.alpha(color) == 0;
        }

        @Override
        public int sample(float u, float v, float width, float height) {
            return color;
        }

        @Override
        public Brush map(IntUnaryOperator colorOp) {
            return new Solid(colorOp.applyAsInt(color));
        }
    }

    /** Общее у всех градиентов. */
    sealed interface Gradient extends Brush permits Linear, Radial, Conic {
        ColorStops stops();

        ColorSpace space();

        /** Повторять рисунок опор за пределами [первая, последняя] вместо растягивания крайних цветов. */
        boolean repeating();

        /** Сдвиг параметра градиента: анимируйте его, чтобы полосы «бежали». */
        float offset();

        Gradient withStops(ColorStops stops);

        Gradient withSpace(ColorSpace space);

        Gradient withRepeating(boolean repeating);

        Gradient withOffset(float offset);

        /** Параметр 0..1 в точке (u, v) прямоугольника width × height, без сдвига и повторения. */
        float rawParameter(float u, float v, float width, float height);

        default Gradient inSpace(ColorSpace space) {
            return withSpace(space);
        }

        default Gradient repeating(boolean on) {
            return withRepeating(on);
        }

        default Gradient shift(float delta) {
            return withOffset(offset() + delta);
        }

        @Override
        default boolean isInvisible() {
            return stops().isInvisible();
        }

        @Override
        default int sample(float u, float v, float width, float height) {
            return stops().sample(Brush.wrap(rawParameter(u, v, width, height) + offset(), stops(), repeating()), space());
        }

        @Override
        default Brush map(IntUnaryOperator colorOp) {
            return withStops(stops().map(colorOp));
        }
    }

    /**
     * Линейный градиент. {@code angle} в градусах как в CSS: 0 — снизу вверх, 90 — слева направо,
     * 180 — сверху вниз. Длина линии подобрана так, что крайние цвета попадают точно в углы.
     */
    record Linear(float angle, ColorStops stops, ColorSpace space, boolean repeating, float offset) implements Gradient {
        @Override
        public float rawParameter(float u, float v, float width, float height) {
            double a = Math.toRadians(angle);
            float dx = (float) Math.sin(a);
            float dy = (float) -Math.cos(a);
            float len = Math.abs(width * dx) + Math.abs(height * dy);
            if (len <= 0f) {
                return 0.5f;
            }
            return ((u - 0.5f) * width * dx + (v - 0.5f) * height * dy) / len + 0.5f;
        }

        public Linear withAngle(float angle) {
            return new Linear(angle, stops, space, repeating, offset);
        }

        public Linear rotate(float degrees) {
            return withAngle(angle + degrees);
        }

        @Override
        public Linear withStops(ColorStops stops) {
            return new Linear(angle, stops, space, repeating, offset);
        }

        @Override
        public Linear withSpace(ColorSpace space) {
            return new Linear(angle, stops, space, repeating, offset);
        }

        @Override
        public Linear withRepeating(boolean repeating) {
            return new Linear(angle, stops, space, repeating, offset);
        }

        @Override
        public Linear withOffset(float offset) {
            return new Linear(angle, stops, space, repeating, offset);
        }

        /**
         * Можно ли нарисовать цветами вершин без шейдера: два цвета на концах в sRGB. Тогда цвет линеен
         * по координате, и интерполяция по треугольникам даёт ровно тот же результат при любом угле.
         */
        public boolean isVertexExact() {
            return space == ColorSpace.SRGB && !repeating && offset == 0f && stops.size() == 2
                    && stops.position(0) == 0f && stops.position(1) == 1f;
        }
    }

    /**
     * Радиальный градиент: эллипс с центром (centerX, centerY) и полуосями (radiusX, radiusY),
     * всё в долях ширины/высоты фигуры. Параметр 1 — на краю эллипса.
     */
    record Radial(float centerX, float centerY, float radiusX, float radiusY,
                  ColorStops stops, ColorSpace space, boolean repeating, float offset) implements Gradient {
        @Override
        public float rawParameter(float u, float v, float width, float height) {
            float x = (u - centerX) / Math.max(radiusX, 1e-5f);
            float y = (v - centerY) / Math.max(radiusY, 1e-5f);
            return (float) Math.sqrt(x * x + y * y);
        }

        public Radial at(float centerX, float centerY) {
            return new Radial(centerX, centerY, radiusX, radiusY, stops, space, repeating, offset);
        }

        public Radial withRadius(float radiusX, float radiusY) {
            return new Radial(centerX, centerY, radiusX, radiusY, stops, space, repeating, offset);
        }

        @Override
        public Radial withStops(ColorStops stops) {
            return new Radial(centerX, centerY, radiusX, radiusY, stops, space, repeating, offset);
        }

        @Override
        public Radial withSpace(ColorSpace space) {
            return new Radial(centerX, centerY, radiusX, radiusY, stops, space, repeating, offset);
        }

        @Override
        public Radial withRepeating(boolean repeating) {
            return new Radial(centerX, centerY, radiusX, radiusY, stops, space, repeating, offset);
        }

        @Override
        public Radial withOffset(float offset) {
            return new Radial(centerX, centerY, radiusX, radiusY, stops, space, repeating, offset);
        }
    }

    /**
     * Конический градиент (по кругу) вокруг (centerX, centerY) в долях фигуры. {@code angle} — начало
     * в градусах от «12 часов» по часовой стрелке. Цветовой круг, сектора, кольцевые индикаторы.
     */
    record Conic(float centerX, float centerY, float angle,
                 ColorStops stops, ColorSpace space, boolean repeating, float offset) implements Gradient {
        @Override
        public float rawParameter(float u, float v, float width, float height) {
            float x = (u - centerX) * width;
            float y = (v - centerY) * height;
            double turn = (Math.atan2(x, -y) - Math.toRadians(angle)) / (2.0 * Math.PI);
            return (float) (turn - Math.floor(turn));
        }

        public Conic withAngle(float angle) {
            return new Conic(centerX, centerY, angle, stops, space, repeating, offset);
        }

        public Conic rotate(float degrees) {
            return withAngle(angle + degrees);
        }

        public Conic at(float centerX, float centerY) {
            return new Conic(centerX, centerY, angle, stops, space, repeating, offset);
        }

        @Override
        public Conic withStops(ColorStops stops) {
            return new Conic(centerX, centerY, angle, stops, space, repeating, offset);
        }

        @Override
        public Conic withSpace(ColorSpace space) {
            return new Conic(centerX, centerY, angle, stops, space, repeating, offset);
        }

        @Override
        public Conic withRepeating(boolean repeating) {
            return new Conic(centerX, centerY, angle, stops, space, repeating, offset);
        }

        @Override
        public Conic withOffset(float offset) {
            return new Conic(centerX, centerY, angle, stops, space, repeating, offset);
        }
    }

    // ================================================================ фабрики

    static Brush solid(int argb) {
        return new Solid(argb);
    }

    /** Двухцветный вертикальный градиент в sRGB: самый дешёвый, рисуется цветами вершин. */
    static Linear vertical(int top, int bottom) {
        return new Linear(180f, ColorStops.even(top, bottom), ColorSpace.SRGB, false, 0f);
    }

    /** Двухцветный горизонтальный градиент в sRGB: самый дешёвый, рисуется цветами вершин. */
    static Linear horizontal(int left, int right) {
        return new Linear(90f, ColorStops.even(left, right), ColorSpace.SRGB, false, 0f);
    }

    /** Линейный градиент под углом (градусы CSS), цвета равномерно, смешивание в OKLAB. */
    static Linear linear(float angle, int... colors) {
        return linear(angle, ColorStops.even(colors));
    }

    static Linear linear(float angle, ColorStops stops) {
        return new Linear(angle, stops, ColorSpace.OKLAB, false, 0f);
    }

    /** Радиальный градиент из центра до углов фигуры. */
    static Radial radial(int... colors) {
        return radial(ColorStops.even(colors));
    }

    static Radial radial(ColorStops stops) {
        return new Radial(0.5f, 0.5f, 0.7072f, 0.7072f, stops, ColorSpace.OKLAB, false, 0f);
    }

    /** Радиальный градиент с центром и радиусом в долях фигуры. */
    static Radial radial(float centerX, float centerY, float radius, int... colors) {
        return new Radial(centerX, centerY, radius, radius, ColorStops.even(colors), ColorSpace.OKLAB, false, 0f);
    }

    /** Конический градиент вокруг центра, начиная с угла {@code angle}. */
    static Conic conic(float angle, int... colors) {
        return conic(angle, ColorStops.even(colors));
    }

    static Conic conic(float angle, ColorStops stops) {
        return new Conic(0.5f, 0.5f, angle, stops, ColorSpace.OKLAB, false, 0f);
    }

    /** Радуга по прямой под углом {@code angle}. */
    static Linear rainbow(float angle) {
        return new Linear(angle, rainbowStops(), ColorSpace.OKLCH, false, 0f);
    }

    /** Цветовой круг: замкнутая радуга по окружности. */
    static Conic rainbowWheel() {
        return new Conic(0.5f, 0.5f, 0f, rainbowStops(), ColorSpace.OKLCH, false, 0f);
    }

    private static ColorStops rainbowStops() {
        int[] c = new int[7];
        for (int i = 0; i < c.length; i++) {
            c[i] = Color.rainbow(i / 6f);
        }
        return ColorStops.even(c);
    }

    // ================================================================ общее

    boolean isInvisible();

    /** Цвет в точке (u, v) ∈ [0, 1]² прямоугольника размером width × height. */
    int sample(float u, float v, float width, float height);

    /** Та же кисть с преобразованными цветами. */
    Brush map(IntUnaryOperator colorOp);

    default Brush withOpacity(float factor) {
        return map(c -> Color.mulAlpha(c, factor));
    }

    /** Параметр после сдвига и повторения. */
    static float wrap(float t, ColorStops stops, boolean repeating) {
        if (!repeating) {
            return t;
        }
        float first = stops.position(0);
        float period = stops.position(stops.size() - 1) - first;
        if (period <= 1e-5f) {
            return t;
        }
        float r = (t - first) % period;
        return first + (r < 0f ? r + period : r);
    }

    /**
     * Интерполяция для переходов стилей. {@code null} — прозрачная версия другой заливки. Сплошной цвет
     * плавно перетекает в градиент любого вида; градиенты одного вида — по всем параметрам; разных видов —
     * переключаются на середине.
     */
    static Brush lerp(Brush a, Brush b, float t) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            a = b.withOpacity(0f);
        }
        if (b == null) {
            b = a.withOpacity(0f);
        }
        if (t <= 0f) {
            return a;
        }
        if (t >= 1f) {
            return b;
        }
        if (a instanceof Solid sa && b instanceof Solid sb) {
            return new Solid(Color.lerp(sa.color, sb.color, t));
        }
        if (a instanceof Solid sa) {
            a = ((Gradient) b).withStops(ColorStops.uniform(((Gradient) b).stops(), sa.color));
        } else if (b instanceof Solid sb) {
            b = ((Gradient) a).withStops(ColorStops.uniform(((Gradient) a).stops(), sb.color));
        }
        Gradient ga = (Gradient) a;
        Gradient gb = (Gradient) b;
        ColorStops stops = ColorStops.lerp(ga.stops(), gb.stops(), t, gb.space());
        float offset = ga.offset() + (gb.offset() - ga.offset()) * t;
        if (ga instanceof Linear la && gb instanceof Linear lb) {
            return new Linear(la.angle + (lb.angle - la.angle) * t, stops, lb.space, lb.repeating, offset);
        }
        if (ga instanceof Radial ra && gb instanceof Radial rb) {
            return new Radial(lerpF(ra.centerX, rb.centerX, t), lerpF(ra.centerY, rb.centerY, t),
                    lerpF(ra.radiusX, rb.radiusX, t), lerpF(ra.radiusY, rb.radiusY, t),
                    stops, rb.space, rb.repeating, offset);
        }
        if (ga instanceof Conic ca && gb instanceof Conic cb) {
            return new Conic(lerpF(ca.centerX, cb.centerX, t), lerpF(ca.centerY, cb.centerY, t),
                    lerpF(ca.angle, cb.angle, t), stops, cb.space, cb.repeating, offset);
        }
        return t < 0.5f ? a : b;
    }

    private static float lerpF(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
