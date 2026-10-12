package io.github.ssbaxys.lumina.input;

/** Направление пространственной навигации фокусом (геймпад, стрелки). */
public enum NavDirection {
    UP(0, -1),
    DOWN(0, 1),
    LEFT(-1, 0),
    RIGHT(1, 0);

    private final int dx;
    private final int dy;

    NavDirection(int dx, int dy) {
        this.dx = dx;
        this.dy = dy;
    }

    public int dx() {
        return dx;
    }

    public int dy() {
        return dy;
    }

    /** По горизонтали ли движение. */
    public boolean horizontal() {
        return dx != 0;
    }
}
