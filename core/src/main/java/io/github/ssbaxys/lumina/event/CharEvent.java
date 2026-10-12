package io.github.ssbaxys.lumina.event;

public final class CharEvent extends UiEvent {
    public static final EventType<CharEvent> TYPED = EventType.create("char-typed", true);

    private final char character;
    private final int modifiers;

    public CharEvent(char character, int modifiers) {
        super(TYPED);
        this.character = character;
        this.modifiers = modifiers;
    }

    public char character() {
        return character;
    }

    public int modifiers() {
        return modifiers;
    }
}
