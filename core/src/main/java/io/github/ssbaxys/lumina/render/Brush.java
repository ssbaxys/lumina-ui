package io.github.ssbaxys.lumina.render;

/**
 * Заливка фигуры. Сейчас: сплошной цвет и линейный градиент по оси.
 * Бэкенд получает цвета четырёх углов, поэтому новые виды заливки не требуют изменений в загрузчиках.
 */
public sealed interface Brush permits Brush.Solid, Brush.Gradient {

    record Solid(int color) implements Brush {
    }

    /** Линейный градиент: vertical = сверху вниз, иначе слева направо. */
    record Gradient(int from, int to, boolean vertical) implements Brush {
    }

    static Brush solid(int argb) {
        return new Solid(argb);
    }

    static Brush vertical(int top, int bottom) {
        return new Gradient(top, bottom, true);
    }

    static Brush horizontal(int left, int right) {
        return new Gradient(left, right, false);
    }

    default int topLeft() {
        return this instanceof Gradient g ? g.from : ((Solid) this).color;
    }

    default int topRight() {
        return this instanceof Gradient g ? (g.vertical ? g.from : g.to) : ((Solid) this).color;
    }

    default int bottomRight() {
        return this instanceof Gradient g ? g.to : ((Solid) this).color;
    }

    default int bottomLeft() {
        return this instanceof Gradient g ? (g.vertical ? g.to : g.from) : ((Solid) this).color;
    }

    default boolean isInvisible() {
        return Color.alpha(topLeft()) == 0 && Color.alpha(topRight()) == 0
                && Color.alpha(bottomRight()) == 0 && Color.alpha(bottomLeft()) == 0;
    }

    /** Интерполяция для переходов стилей. {@code null} трактуется как прозрачная версия другой заливки. */
    static Brush lerp(Brush a, Brush b, float t) {
        if (a == null && b == null) {
            return null;
        }
        if (a == null) {
            a = transparentOf(b);
        }
        if (b == null) {
            b = transparentOf(a);
        }
        if (a instanceof Solid sa && b instanceof Solid sb) {
            return new Solid(Color.lerp(sa.color, sb.color, t));
        }
        boolean vertical = b instanceof Gradient gb ? gb.vertical : ((Gradient) a).vertical;
        int from = Color.lerp(a.topLeft(), b.topLeft(), t);
        int to = Color.lerp(a.bottomRight(), b.bottomRight(), t);
        return new Gradient(from, to, vertical);
    }

    private static Brush transparentOf(Brush b) {
        return b instanceof Gradient g
                ? new Gradient(Color.withAlpha(g.from, 0), Color.withAlpha(g.to, 0), g.vertical)
                : new Solid(Color.withAlpha(((Solid) b).color, 0));
    }
}
