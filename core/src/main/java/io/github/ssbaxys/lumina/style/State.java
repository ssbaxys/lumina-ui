package io.github.ssbaxys.lumina.style;

/** Битовые флаги состояния элемента (аналог псевдоклассов CSS). */
public final class State {
    public static final int HOVERED = 1;
    public static final int PRESSED = 1 << 1;
    public static final int FOCUSED = 1 << 2;
    public static final int DISABLED = 1 << 3;
    public static final int CHECKED = 1 << 4;
    /** Первый свободный бит для собственных состояний виджетов. */
    public static final int FIRST_CUSTOM = 1 << 5;

    private State() {
    }
}
