package io.github.ssbaxys.lumina.sound;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.style.Styles;

/** Проигрывание звуков интерфейса по стилю элемента. */
public final class UiSounds {
    private UiSounds() {
    }

    /**
     * Звук нажатия элемента: {@link Styles#CLICK_SOUND} из его стиля. Если в стиле звука нет, то при
     * {@code vanillaFallback} играет стандартный щелчок игры (так звучат кнопки), иначе — тишина
     * (переключатели и флажки по умолчанию молчат).
     */
    public static void click(UiElement element, boolean vanillaFallback) {
        if (!Lumina.isInitialized()) {
            return;
        }
        UiSound sound = element.computedStyle().get(Styles.CLICK_SOUND);
        if (sound != null) {
            sound.play();
        } else if (vanillaFallback) {
            Lumina.platform().playClickSound();
        }
    }
}
