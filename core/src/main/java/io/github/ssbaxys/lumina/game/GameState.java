package io.github.ssbaxys.lumina.game;

import java.util.Arrays;

/**
 * Изменяемый снимок состояния игрока, который платформа заполняет на каждом тике клиента
 * ({@link io.github.ssbaxys.lumina.platform.Platform#pollGameData}). Один и тот же объект переиспользуется,
 * поэтому опрос ничего не выделяет; {@link GameData} переносит изменившиеся поля в сигналы.
 *
 * <p>Значения по умолчанию ({@link #reset()}) — как у нового игрока: 20 здоровья, полная сытость,
 * полный запас воздуха. Слоты инвентаря нумеруются как в игре: 0–8 — панель быстрого доступа,
 * 9–35 — рюкзак, 36–39 — броня (ботинки, поножи, нагрудник, шлем), 40 — вторая рука.</p>
 */
public final class GameState {
    /** Слотов в инвентаре игрока Minecraft. */
    public static final int SLOTS = 41;
    /** Слотов панели быстрого доступа. */
    public static final int HOTBAR = 9;

    public float health;
    public float maxHealth;
    public float absorption;
    public int food;
    public float saturation;
    public int armor;
    public int air;
    public int maxAir;
    public int xpLevel;
    /** Заполнение полосы опыта до следующего уровня, 0..1. */
    public float xpProgress;
    public double x;
    public double y;
    public double z;
    /** Поворот головы по горизонтали в градусах, как в игре (0 — юг, 90 — запад). */
    public float yaw;
    public float pitch;
    /** Сторона света, куда смотрит игрок: {@code "north"}, {@code "south"}, {@code "west"}, {@code "east"}. */
    public String facing;
    /** Идентификатор биома ({@code "minecraft:plains"}) или пустая строка. */
    public String biome;
    /** Идентификатор измерения ({@code "minecraft:overworld"}) или пустая строка. */
    public String dimension;
    /** Время суток мира в тиках, {@code 0..23999}. */
    public int dayTime;
    public int fps;
    /** Выбранный слот панели быстрого доступа, {@code 0..8}. */
    public int selectedSlot;
    /** Сколько слотов заполнено в {@link #itemIds}/{@link #counts}, не больше {@link #SLOTS}. */
    public int slotCount;
    /** Идентификатор предмета в слоте ({@code "minecraft:stone"}) или {@code null}/пустая строка, если пусто. */
    public final String[] itemIds = new String[SLOTS];
    public final int[] counts = new int[SLOTS];

    public GameState() {
        reset();
    }

    /** Значения по умолчанию: мира нет, игрок «как новый». */
    public void reset() {
        health = 20f;
        maxHealth = 20f;
        absorption = 0f;
        food = 20;
        saturation = 5f;
        armor = 0;
        air = 300;
        maxAir = 300;
        xpLevel = 0;
        xpProgress = 0f;
        x = 0.0;
        y = 0.0;
        z = 0.0;
        yaw = 0f;
        pitch = 0f;
        facing = "south";
        biome = "";
        dimension = "";
        dayTime = 0;
        fps = 0;
        selectedSlot = 0;
        slotCount = SLOTS;
        Arrays.fill(itemIds, null);
        Arrays.fill(counts, 0);
    }

    /** Положить предмет в слот (для платформ и тестов); {@code count <= 0} или пустой id — пустой слот. */
    public void setSlot(int slot, String itemId, int count) {
        boolean empty = itemId == null || itemId.isEmpty() || count <= 0;
        itemIds[slot] = empty ? null : itemId;
        counts[slot] = empty ? 0 : count;
    }
}
