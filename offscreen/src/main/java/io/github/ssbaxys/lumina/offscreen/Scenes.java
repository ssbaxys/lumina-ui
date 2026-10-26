package io.github.ssbaxys.lumina.offscreen;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;

/** Помощники для постановки кадра: прогнать анимации, нажать, навести. */
public final class Scenes {
    private Scenes() {
    }

    /** Прогнать {@code seconds} секунд кадров по 60 в секунду (без рисования). */
    public static void settle(UiRoot root, OffscreenPlatform platform, float seconds) {
        int frames = Math.max(1, Math.round(seconds * 60f));
        NullCanvas nul = new NullCanvas(platform.font());
        for (int i = 0; i < frames; i++) {
            platform.advance(1f / 60f);
            nul.begin(root.width(), root.height());
            root.render(nul, 0f);
            nul.end();
        }
    }

    public static void click(UiRoot root, UiElement e) {
        float x = e.screenX() + e.width() / 2f;
        float y = e.screenY() + e.height() / 2f;
        root.pointerMove(x, y);
        root.pointerDown(x, y, Keys.MOUSE_LEFT);
        root.pointerUp(x, y, Keys.MOUSE_LEFT);
    }

    public static void hover(UiRoot root, UiElement e) {
        root.pointerMove(e.screenX() + e.width() / 2f, e.screenY() + e.height() / 2f);
    }
}
