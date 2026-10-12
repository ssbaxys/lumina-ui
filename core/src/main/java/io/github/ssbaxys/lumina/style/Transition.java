package io.github.ssbaxys.lumina.style;

import io.github.ssbaxys.lumina.anim.Easing;

/** Как анимировать смену стиля при смене состояния (hover, pressed...). */
public record Transition(float duration, Easing easing) {
    public static final Transition NONE = new Transition(0f, Easing.LINEAR);

    public static Transition of(float durationSeconds) {
        return new Transition(durationSeconds, Easing.OUT_CUBIC);
    }
}
