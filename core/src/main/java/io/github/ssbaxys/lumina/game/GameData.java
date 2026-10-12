package io.github.ssbaxys.lumina.game;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.platform.WorldInfo;
import io.github.ssbaxys.lumina.reactive.Computed;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Signal;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Живые данные игры в виде сигналов: здоровье, голод, броня, воздух, опыт, координаты, взгляд, биом,
 * измерение, время суток, FPS, выбранный предмет и инвентарь. Загрузчик вызывает {@link #update()} на каждом
 * тике клиента (20 раз в секунду) — значения берутся у платформы ({@code Platform.pollGameData}),
 * а сигналы уведомляют подписчиков только когда значение действительно изменилось.
 *
 * <pre>{@code
 * new SegmentedBar().bind(GameData.health()).bindMax(GameData.maxHealth());
 * Label.bound(GameData.position());
 * GameData.inventory().subscribe(inv -> count.set(inv.total("minecraft:diamond")));
 * }</pre>
 *
 * <p>Без мира (главное меню, тесты) сигналы держат значения по умолчанию из {@link GameState#reset()},
 * а {@link #inWorld()} — {@code false}. Для тестов и своих источников — {@link #setSource}.</p>
 *
 * <p>Однопоточный, как и все сигналы: вызывайте только из потока клиента.</p>
 */
public final class GameData {
    /** Свой источник данных вместо платформы (тесты, повтор записи, сервер-сайд данные). */
    @FunctionalInterface
    public interface Source {
        /**
         * Заполнить {@code out} (он уже сброшен в значения по умолчанию).
         *
         * @return {@code true}, если игрок в мире и данные настоящие
         */
        boolean poll(GameState out);
    }

    private static final GameState STATE = new GameState();
    private static Source source;
    private static int clockMinute = -1;

    private static final Signal<Boolean> IN_WORLD = Signal.of(false);
    private static final Signal<Float> HEALTH = Signal.of(20f);
    private static final Signal<Float> MAX_HEALTH = Signal.of(20f);
    private static final Signal<Float> ABSORPTION = Signal.of(0f);
    private static final Signal<Integer> FOOD = Signal.of(20);
    private static final Signal<Float> SATURATION = Signal.of(5f);
    private static final Signal<Integer> ARMOR = Signal.of(0);
    private static final Signal<Integer> AIR = Signal.of(300);
    private static final Signal<Integer> MAX_AIR = Signal.of(300);
    private static final Signal<Integer> XP_LEVEL = Signal.of(0);
    private static final Signal<Float> XP_PROGRESS = Signal.of(0f);
    private static final Signal<Double> X = Signal.of(0.0);
    private static final Signal<Double> Y = Signal.of(0.0);
    private static final Signal<Double> Z = Signal.of(0.0);
    private static final Signal<Integer> BLOCK_X = Signal.of(0);
    private static final Signal<Integer> BLOCK_Y = Signal.of(0);
    private static final Signal<Integer> BLOCK_Z = Signal.of(0);
    private static final Signal<String> POSITION = Signal.of("0 0 0");
    private static final Signal<Float> YAW = Signal.of(0f);
    private static final Signal<Float> PITCH = Signal.of(0f);
    private static final Signal<String> FACING = Signal.of("south");
    private static final Signal<String> BIOME = Signal.of("");
    private static final Signal<String> DIMENSION = Signal.of("");
    private static final Signal<Integer> DAY_TIME = Signal.of(0);
    private static final Signal<String> CLOCK = Signal.of(WorldInfo.formatTime(0f));
    private static final Signal<Integer> FPS = Signal.of(0);
    private static final Signal<Integer> SELECTED_SLOT = Signal.of(0);
    private static final Signal<String> SELECTED_ITEM = Signal.of("");
    private static final Signal<Integer> SELECTED_COUNT = Signal.of(0);
    private static final Signal<Inventory> INVENTORY = Signal.of(Inventory.EMPTY);
    private static final Computed<Float> HEALTH_FRACTION = new Computed<>(
            () -> MAX_HEALTH.get() <= 0f ? 0f : Math.max(0f, Math.min(1f, HEALTH.get() / MAX_HEALTH.get())));
    private static final Computed<Float> FOOD_FRACTION = new Computed<>(() -> Math.max(0f, Math.min(1f, FOOD.get() / 20f)));
    private static final Computed<Float> AIR_FRACTION = new Computed<>(
            () -> MAX_AIR.get() <= 0 ? 0f : Math.max(0f, Math.min(1f, AIR.get() / (float) MAX_AIR.get())));

    private static final Map<String, ReadableSignal<?>> NAMED;

    static {
        Map<String, ReadableSignal<?>> m = new LinkedHashMap<>();
        m.put("inWorld", IN_WORLD);
        m.put("health", HEALTH);
        m.put("maxHealth", MAX_HEALTH);
        m.put("healthFraction", HEALTH_FRACTION);
        m.put("absorption", ABSORPTION);
        m.put("food", FOOD);
        m.put("foodFraction", FOOD_FRACTION);
        m.put("saturation", SATURATION);
        m.put("armor", ARMOR);
        m.put("air", AIR);
        m.put("maxAir", MAX_AIR);
        m.put("airFraction", AIR_FRACTION);
        m.put("xpLevel", XP_LEVEL);
        m.put("xpProgress", XP_PROGRESS);
        m.put("x", BLOCK_X);
        m.put("y", BLOCK_Y);
        m.put("z", BLOCK_Z);
        m.put("pos", POSITION);
        m.put("yaw", YAW);
        m.put("pitch", PITCH);
        m.put("facing", FACING);
        m.put("biome", BIOME);
        m.put("dimension", DIMENSION);
        m.put("dayTime", DAY_TIME);
        m.put("time", CLOCK);
        m.put("fps", FPS);
        m.put("slot", SELECTED_SLOT);
        m.put("item", SELECTED_ITEM);
        m.put("itemCount", SELECTED_COUNT);
        m.put("inventory", INVENTORY);
        NAMED = Collections.unmodifiableMap(m);
    }

    private GameData() {
    }

    // ================================================================ обновление

    /**
     * Опросить источник и обновить сигналы. Загрузчик вызывает это на каждом тике клиента
     * (через {@code Hud.get().clientTick()}); вручную — только в тестах и своих циклах.
     */
    public static void update() {
        GameState s = STATE;
        s.reset();
        boolean inWorld;
        if (source != null) {
            inWorld = source.poll(s);
        } else {
            inWorld = Lumina.isInitialized() && Lumina.platform().pollGameData(s);
        }
        if (!inWorld) {
            s.reset();
        }
        apply(s, inWorld);
    }

    /** Свой источник ({@code null} — снова платформа). */
    public static void setSource(Source newSource) {
        source = newSource;
    }

    /** Вернуть всё к значениям по умолчанию и источник — к платформе (для тестов). */
    public static void reset() {
        source = null;
        STATE.reset();
        apply(STATE, false);
    }

    /** Текущий снимок (только чтение; перезаписывается при каждом {@link #update()}). */
    public static GameState state() {
        return STATE;
    }

    private static void apply(GameState s, boolean inWorld) {
        if (IN_WORLD.peek() != inWorld) {
            IN_WORLD.set(inWorld);
        }
        setF(HEALTH, s.health);
        setF(MAX_HEALTH, s.maxHealth);
        setF(ABSORPTION, s.absorption);
        setI(FOOD, s.food);
        setF(SATURATION, s.saturation);
        setI(ARMOR, s.armor);
        setI(AIR, s.air);
        setI(MAX_AIR, s.maxAir);
        setI(XP_LEVEL, s.xpLevel);
        setF(XP_PROGRESS, s.xpProgress);
        setD(X, s.x);
        setD(Y, s.y);
        setD(Z, s.z);
        int bx = (int) Math.floor(s.x);
        int by = (int) Math.floor(s.y);
        int bz = (int) Math.floor(s.z);
        boolean blockMoved = BLOCK_X.peek() != bx || BLOCK_Y.peek() != by || BLOCK_Z.peek() != bz;
        setI(BLOCK_X, bx);
        setI(BLOCK_Y, by);
        setI(BLOCK_Z, bz);
        if (blockMoved) {
            POSITION.set(bx + " " + by + " " + bz);
        }
        setF(YAW, s.yaw);
        setF(PITCH, s.pitch);
        FACING.set(s.facing == null ? "" : s.facing);
        BIOME.set(s.biome == null ? "" : s.biome);
        DIMENSION.set(s.dimension == null ? "" : s.dimension);
        int day = Math.floorMod(s.dayTime, 24000);
        setI(DAY_TIME, day);
        // Часы меняются раз в игровую минуту (≈16.7 тика): строка создаётся только тогда.
        int minute = (int) (WorldInfo.hoursOf(day) * 60f);
        if (minute != clockMinute) {
            clockMinute = minute;
            CLOCK.set(WorldInfo.formatTime(day));
        }
        setI(FPS, s.fps);
        int sel = Math.max(0, Math.min(GameState.HOTBAR - 1, s.selectedSlot));
        setI(SELECTED_SLOT, sel);
        String held = sel < s.slotCount ? s.itemIds[sel] : null;
        boolean emptyHand = held == null || held.isEmpty() || s.counts[sel] <= 0;
        SELECTED_ITEM.set(emptyHand ? "" : held);
        setI(SELECTED_COUNT, emptyHand ? 0 : s.counts[sel]);
        int size = Math.max(0, Math.min(GameState.SLOTS, s.slotCount));
        if (!inWorld) {
            size = 0;
        }
        if (!INVENTORY.peek().matches(s.itemIds, s.counts, size)) {
            INVENTORY.set(Inventory.of(s.itemIds, s.counts, size));
        }
    }

    // Сравнение до упаковки: неизменившееся значение не создаёт объект-обёртку на каждом тике.
    private static void setF(Signal<Float> signal, float v) {
        if (signal.peek() != v) {
            signal.set(v);
        }
    }

    private static void setI(Signal<Integer> signal, int v) {
        if (signal.peek() != v) {
            signal.set(v);
        }
    }

    private static void setD(Signal<Double> signal, double v) {
        if (signal.peek() != v) {
            signal.set(v);
        }
    }

    // ================================================================ сигналы

    /** Игрок в мире (иначе все значения — по умолчанию). */
    public static ReadableSignal<Boolean> inWorld() {
        return IN_WORLD;
    }

    public static ReadableSignal<Float> health() {
        return HEALTH;
    }

    public static ReadableSignal<Float> maxHealth() {
        return MAX_HEALTH;
    }

    /** Здоровье / максимум, 0..1. */
    public static ReadableSignal<Float> healthFraction() {
        return HEALTH_FRACTION;
    }

    /** Дополнительное здоровье (золотые сердца). */
    public static ReadableSignal<Float> absorption() {
        return ABSORPTION;
    }

    /** Сытость 0..20. */
    public static ReadableSignal<Integer> food() {
        return FOOD;
    }

    public static ReadableSignal<Float> foodFraction() {
        return FOOD_FRACTION;
    }

    /** Насыщение (скрытый запас сытости) 0..20. */
    public static ReadableSignal<Float> saturation() {
        return SATURATION;
    }

    /** Очки брони 0..20 (и больше с модами). */
    public static ReadableSignal<Integer> armor() {
        return ARMOR;
    }

    /** Запас воздуха в тиках (300 — полный, под водой убывает). */
    public static ReadableSignal<Integer> air() {
        return AIR;
    }

    public static ReadableSignal<Integer> maxAir() {
        return MAX_AIR;
    }

    public static ReadableSignal<Float> airFraction() {
        return AIR_FRACTION;
    }

    public static ReadableSignal<Integer> xpLevel() {
        return XP_LEVEL;
    }

    /** Заполнение полосы опыта, 0..1. */
    public static ReadableSignal<Float> xpProgress() {
        return XP_PROGRESS;
    }

    public static ReadableSignal<Double> x() {
        return X;
    }

    public static ReadableSignal<Double> y() {
        return Y;
    }

    public static ReadableSignal<Double> z() {
        return Z;
    }

    /** Координата блока под игроком (округление вниз). */
    public static ReadableSignal<Integer> blockX() {
        return BLOCK_X;
    }

    public static ReadableSignal<Integer> blockY() {
        return BLOCK_Y;
    }

    public static ReadableSignal<Integer> blockZ() {
        return BLOCK_Z;
    }

    /** Координаты блока строкой: {@code "12 64 -30"}; меняется только при переходе в другой блок. */
    public static ReadableSignal<String> position() {
        return POSITION;
    }

    public static ReadableSignal<Float> yaw() {
        return YAW;
    }

    public static ReadableSignal<Float> pitch() {
        return PITCH;
    }

    /** Сторона света: {@code "north"}, {@code "south"}, {@code "west"}, {@code "east"}. */
    public static ReadableSignal<String> facing() {
        return FACING;
    }

    /** Биом под игроком ({@code "minecraft:plains"}) или {@code ""}. */
    public static ReadableSignal<String> biome() {
        return BIOME;
    }

    /** Измерение ({@code "minecraft:the_nether"}) или {@code ""}. */
    public static ReadableSignal<String> dimension() {
        return DIMENSION;
    }

    /** Время суток мира в тиках, {@code 0..23999}. */
    public static ReadableSignal<Integer> dayTime() {
        return DAY_TIME;
    }

    /** Время суток как на часах: {@code "06:00"} на восходе. */
    public static ReadableSignal<String> clock() {
        return CLOCK;
    }

    public static ReadableSignal<Integer> fps() {
        return FPS;
    }

    /** Выбранный слот панели, {@code 0..8}. */
    public static ReadableSignal<Integer> selectedSlot() {
        return SELECTED_SLOT;
    }

    /** Предмет в руке ({@code "minecraft:diamond_sword"}) или {@code ""}. */
    public static ReadableSignal<String> selectedItem() {
        return SELECTED_ITEM;
    }

    public static ReadableSignal<Integer> selectedCount() {
        return SELECTED_COUNT;
    }

    /** Снимок инвентаря; новый объект — только когда что-то изменилось. */
    public static ReadableSignal<Inventory> inventory() {
        return INVENTORY;
    }

    /**
     * Все сигналы под короткими именами для разметки ({@code {{health}}}, {@code {{pos}}}, {@code {{time}}}):
     * inWorld, health, maxHealth, healthFraction, absorption, food, foodFraction, saturation, armor, air, maxAir,
     * airFraction, xpLevel, xpProgress, x, y, z (блок), pos, yaw, pitch, facing, biome, dimension, dayTime,
     * time, fps, slot, item, itemCount, inventory.
     */
    public static Map<String, ReadableSignal<?>> named() {
        return NAMED;
    }
}
