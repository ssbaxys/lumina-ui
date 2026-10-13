package io.github.ssbaxys.lumina.blocks;

/**
 * Чтение блоков мира по одному — то, что загрузчик даёт ядру для снимков ({@link WorldCapture},
 * {@code widget.StructurePreview}). Реализация — {@code Platform.worldBlocks()} в загрузчике (клиентский мир)
 * или выдуманный мир без игры ({@link DemoWorld}).
 *
 * <p>Методы вызываются из потока отрисовки много раз за кадр, поэтому должны быть дешёвыми: без выделения
 * памяти, id блоков — закэшированные строки.</p>
 */
public interface WorldBlocks {
    /**
     * Особое значение {@link #blockAt}: чанк не загружен, блок неизвестен (снимок оставит прежний и перечитает
     * его позже). Сравнивается по ссылке — возвращайте именно эту константу.
     */
    String UNLOADED = new String("lumina:unloaded");

    /** Id блока ({@code "minecraft:stone"}), {@code null} — воздух (и всё вне высоты мира), {@link #UNLOADED} — неизвестно. */
    String blockAt(int x, int y, int z);

    /** Есть ли игрок (иначе положение игрока не определено). */
    default boolean hasPlayer() {
        return true;
    }

    /** Положение ног игрока, в блоках. */
    double playerX();

    double playerY();

    double playerZ();

    /** Поворот игрока в градусах, как в Minecraft: 0 — на юг (+Z), 90 — на запад, 180 — на север. */
    float playerYaw();
}
