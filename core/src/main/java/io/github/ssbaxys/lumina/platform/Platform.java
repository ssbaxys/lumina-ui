package io.github.ssbaxys.lumina.platform;

import io.github.ssbaxys.lumina.render.TextMetrics;

/** Всё, что ядру нужно от игры и загрузчика. Реализуется в каждом модуле загрузчика. */
public interface Platform {
    TextMetrics textMetrics();

    String getClipboard();

    void setClipboard(String text);

    /** Стандартный звук нажатия кнопки интерфейса. */
    void playClickSound();

    /** Звук игры по идентификатору ("minecraft:entity.generic.explode"). По умолчанию — тишина. */
    default void playSound(String soundId, float volume, float pitch) {
    }

    /**
     * Текст ресурса по идентификатору "мод:путь" ({@code luminatest:ui/page.html} →
     * {@code assets/luminatest/ui/page.html}) или {@code null}. Загрузчики читают через менеджер ресурсов
     * игры, поэтому подхватываются ресурспаки (и их перезагрузка по F3+T).
     */
    default String readResource(String id) {
        byte[] bytes = readResourceBytes(id);
        return bytes == null ? null : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Байты ресурса "мод:путь" или {@code null} (текстуры, звуки, данные). */
    default byte[] readResourceBytes(String id) {
        int colon = id.indexOf(':');
        String path = colon < 0 ? "assets/minecraft/" + id : "assets/" + id.substring(0, colon) + "/" + id.substring(colon + 1);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = Platform.class.getClassLoader();
        }
        try (java.io.InputStream in = cl.getResourceAsStream(path)) {
            return in == null ? null : in.readAllBytes();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    default long nanoTime() {
        return System.nanoTime();
    }
}
