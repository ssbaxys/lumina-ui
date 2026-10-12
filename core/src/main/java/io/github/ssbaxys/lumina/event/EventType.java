package io.github.ssbaxys.lumina.event;

/** Тип события с плотным id: слушатели хранятся в массиве по этому id. */
public final class EventType<E extends UiEvent> {
    private static int counter;

    private final int id;
    private final String name;
    private final boolean bubbles;

    private EventType(int id, String name, boolean bubbles) {
        this.id = id;
        this.name = name;
        this.bubbles = bubbles;
    }

    public static synchronized <E extends UiEvent> EventType<E> create(String name, boolean bubbles) {
        return new EventType<>(counter++, name, bubbles);
    }

    public static synchronized int count() {
        return counter;
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    /** Всплывает ли событие от цели к корню. */
    public boolean bubbles() {
        return bubbles;
    }

    @Override
    public String toString() {
        return name;
    }
}
