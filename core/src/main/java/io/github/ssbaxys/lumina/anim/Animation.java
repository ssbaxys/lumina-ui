package io.github.ssbaxys.lumina.anim;

/** Что-то, что меняется со временем. Регистрируется в {@link Animator}. */
@FunctionalInterface
public interface Animation {
    /**
     * @param dt прошедшее время в секундах
     * @return {@code true}, пока анимация продолжается; {@code false} снимает её с аниматора
     */
    boolean tick(float dt);
}
