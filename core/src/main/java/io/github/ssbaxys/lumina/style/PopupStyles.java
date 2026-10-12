package io.github.ssbaxys.lumina.style;

import io.github.ssbaxys.lumina.anim.Interpolator;

/** Свойства стиля всплывающих виджетов, которым не хватает общих {@link Styles}. */
public final class PopupStyles {
    /** Цвет секторов и центрального круга радиального меню ("radial-menu"). */
    public static final StyleKey<Integer> WEDGE_COLOR = StyleKey.builder("radial-wedge-color", Integer.class)
            .defaultValue(0xE0202430).animated(Interpolator.COLOR).register();

    private PopupStyles() {
    }
}
