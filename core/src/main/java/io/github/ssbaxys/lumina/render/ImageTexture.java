package io.github.ssbaxys.lumina.render;

/**
 * Текстура с известным размером в пикселях: её возвращает {@code Images}. Размер нужен виджетам,
 * чтобы вписывать картинку с сохранением пропорций (CONTAIN, COVER).
 */
public record ImageTexture(String id, int width, int height) implements TextureRef {
    /** Отношение ширины к высоте (1, если размер неизвестен). */
    public float aspect() {
        return width > 0 && height > 0 ? width / (float) height : 1f;
    }
}
