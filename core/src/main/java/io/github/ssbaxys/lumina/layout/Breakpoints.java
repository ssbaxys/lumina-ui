package io.github.ssbaxys.lumina.layout;

/**
 * Пороги ширины (в единицах интерфейса, то есть уже с учётом масштаба корня): от {@code medium} —
 * {@link Breakpoint#MEDIUM}, от {@code wide} — {@link Breakpoint#WIDE}, уже — {@link Breakpoint#COMPACT}.
 * По умолчанию 400 и 640: при масштабе GUI 2 на экране шириной 1920 точек ширина интерфейса 960 — широкий.
 */
public record Breakpoints(float medium, float wide) {
    public static final Breakpoints DEFAULT = new Breakpoints(400f, 640f);

    public Breakpoints {
        if (!(medium <= wide)) {
            throw new IllegalArgumentException("medium must be <= wide: " + medium + ", " + wide);
        }
    }

    public Breakpoint of(float width) {
        if (width >= wide) {
            return Breakpoint.WIDE;
        }
        return width >= medium ? Breakpoint.MEDIUM : Breakpoint.COMPACT;
    }
}
