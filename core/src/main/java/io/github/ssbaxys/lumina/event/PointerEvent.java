package io.github.ssbaxys.lumina.event;

/** Событие мыши. Координаты в системе корня; {@link #localX()} — относительно текущего элемента. */
public final class PointerEvent extends UiEvent {
    public static final EventType<PointerEvent> PRESS = EventType.create("pointer-press", true);
    public static final EventType<PointerEvent> RELEASE = EventType.create("pointer-release", true);
    public static final EventType<PointerEvent> MOVE = EventType.create("pointer-move", true);
    /** Перемещение с зажатой кнопкой; приходит элементу, на котором была нажата кнопка. */
    public static final EventType<PointerEvent> DRAG = EventType.create("pointer-drag", true);
    public static final EventType<PointerEvent> CLICK = EventType.create("click", true);
    public static final EventType<PointerEvent> ENTER = EventType.create("pointer-enter", false);
    public static final EventType<PointerEvent> LEAVE = EventType.create("pointer-leave", false);

    private final float x;
    private final float y;
    private final int button;

    public PointerEvent(EventType<PointerEvent> type, float x, float y, int button) {
        super(type);
        this.x = x;
        this.y = y;
        this.button = button;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float localX() {
        return currentTarget() == null ? x : x - currentTarget().screenX();
    }

    public float localY() {
        return currentTarget() == null ? y : y - currentTarget().screenY();
    }

    /** Кнопка мыши (0 — левая) или -1 для движения без нажатия. */
    public int button() {
        return button;
    }
}
