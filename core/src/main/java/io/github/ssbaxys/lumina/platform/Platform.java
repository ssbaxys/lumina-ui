package io.github.ssbaxys.lumina.platform;

import io.github.ssbaxys.lumina.render.TextMetrics;

/** Всё, что ядру нужно от игры и загрузчика. Реализуется в каждом модуле загрузчика. */
public interface Platform {
    TextMetrics textMetrics();

    String getClipboard();

    void setClipboard(String text);

    /** Стандартный звук нажатия кнопки интерфейса. */
    void playClickSound();

    default long nanoTime() {
        return System.nanoTime();
    }
}
