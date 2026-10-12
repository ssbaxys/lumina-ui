package io.github.ssbaxys.lumina.render;

/**
 * Ссылка на текстуру. Достаточно идентификатора ресурса: {@code TextureRef.of("minecraft:textures/block/tnt_side.png")}
 * — бэкенд загрузчика сам найдёт и загрузит файл. Модули загрузчиков могут давать и свои типы.
 */
public interface TextureRef {
    String id();

    /** Текстура по полному идентификатору ресурса "namespace:path/to/file.png". */
    static TextureRef of(String id) {
        return new Named(id);
    }

    /** Текстура ванильного ресурспака: {@code vanilla("block/tnt_side")} → minecraft:textures/block/tnt_side.png. */
    static TextureRef vanilla(String path) {
        return new Named("minecraft:textures/" + path + ".png");
    }

    record Named(String id) implements TextureRef {
    }
}
