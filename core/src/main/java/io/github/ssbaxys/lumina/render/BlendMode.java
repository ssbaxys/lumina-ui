package io.github.ssbaxys.lumina.render;

/**
 * Как новые пиксели ложатся на уже нарисованные. Задаётся в {@link Canvas#setBlendMode} или стилем
 * {@code Styles.BLEND}; бэкенд переключает функцию смешивания OpenGL.
 */
public enum BlendMode {
    /** Обычная прозрачность. */
    NORMAL,
    /** Свет складывается: неон, блики, свечение. */
    ADD,
    /** Затемнение: тени, тонировка. */
    MULTIPLY,
    /** Осветление без пересвета: мягкие блики. */
    SCREEN
}
