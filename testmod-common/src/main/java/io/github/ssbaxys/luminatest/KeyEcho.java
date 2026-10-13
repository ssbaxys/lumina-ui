package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.event.EventListener;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.widget.Label;

/** Показывает последнюю нажатую клавишу. Слушает корень и отписывается при удалении из дерева. */
final class KeyEcho extends Label {
    /** Последнее нажатие: {код, Ctrl, Shift, Alt} или null. Текст — производное от него и от языка. */
    private final Signal<int[]> last = Signal.of(null);
    private final EventListener<KeyEvent> listener = e -> last.set(new int[] {
            e.key(), flag(e, Keys.MOD_CONTROL), flag(e, Keys.MOD_SHIFT), flag(e, Keys.MOD_ALT)});
    private UiElement subscribed;

    KeyEcho() {
        super("");
        bind(I18n.text(() -> describe(last.get())));
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

    private static int flag(KeyEvent e, int modifier) {
        return e.hasModifier(modifier) ? 1 : 0;
    }

    private static String describe(int[] press) {
        if (press == null) {
            return I18n.get("luminatest.input.key.prompt");
        }
        StringBuilder sb = new StringBuilder(I18n.get("luminatest.input.key.code", press[0]));
        if (press[1] != 0) {
            sb.append(" + Ctrl");
        }
        if (press[2] != 0) {
            sb.append(" + Shift");
        }
        if (press[3] != 0) {
            sb.append(" + Alt");
        }
        return sb.toString();
    }
}
