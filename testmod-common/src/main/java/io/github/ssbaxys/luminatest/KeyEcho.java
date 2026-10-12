package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.event.EventListener;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.widget.Label;

/** Показывает последнюю нажатую клавишу. Слушает корень и отписывается при удалении из дерева. */
final class KeyEcho extends Label {
    private final EventListener<KeyEvent> listener = e -> text(describe(e));
    private UiElement subscribed;

    KeyEcho() {
        super("Нажмите любую клавишу");
    }

    @Override
    protected void onAttach() {
        super.onAttach();
        subscribed = root().content();
        subscribed.on(KeyEvent.PRESS, listener);
    }

    @Override
    protected void onDetach() {
        super.onDetach();
        if (subscribed != null) {
            subscribed.off(KeyEvent.PRESS, listener);
            subscribed = null;
        }
    }

    private static String describe(KeyEvent e) {
        StringBuilder sb = new StringBuilder("Клавиша: код ").append(e.key());
        if (e.hasModifier(Keys.MOD_CONTROL)) {
            sb.append(" + Ctrl");
        }
        if (e.hasModifier(Keys.MOD_SHIFT)) {
            sb.append(" + Shift");
        }
        if (e.hasModifier(Keys.MOD_ALT)) {
            sb.append(" + Alt");
        }
        return sb.toString();
    }
}
