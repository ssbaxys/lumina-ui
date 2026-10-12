package io.github.ssbaxys.lumina.event;

public final class ScrollEvent extends UiEvent {
    public static final EventType<ScrollEvent> SCROLL = EventType.create("scroll", true);

    private final float x;
    private final float y;
    private final float deltaX;
    private final float deltaY;

    public ScrollEvent(float x, float y, float deltaX, float deltaY) {
        super(SCROLL);
        this.x = x;
        this.y = y;
        this.deltaX = deltaX;
        this.deltaY = deltaY;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float deltaX() {
        return deltaX;
    }

    /** Положительное значение — колесо от себя (прокрутка вверх). */
    public float deltaY() {
        return deltaY;
    }
}
