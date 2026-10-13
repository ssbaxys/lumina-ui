package io.github.ssbaxys.lumina.menu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Описание меню-контейнера, общее для сервера и клиента: какие ячейки в нём есть (разделы) и какие числа
 * сервер синхронизирует с клиентом. Одно и то же описание (обычно — константа в общем коде мода) строит
 * серверное меню загрузчика и клиентский {@link RemoteMenu}, поэтому номера слотов и значений совпадают.
 *
 * <pre>{@code
 * static final MenuSpec SPEC = MenuSpec.builder()
 *         .container("chest", 27)            // слоты 0..26 меню — ячейки 0..26 контейнера
 *         .container("result", 1).takeOnly() // слот 27 — только брать
 *         .playerInventory()                 // слоты 28..63: рюкзак, затем хотбар (как в игре)
 *         .intValue("progress")              // полное int (две 16-битные части на сервере)
 *         .boolValue("burning")
 *         .build();
 * }</pre>
 *
 * Слоты меню идут подряд по разделам в порядке объявления. Ячейки контейнерных разделов лежат подряд в одном
 * контейнере сервера ({@link Section#containerOffset()}). У раздела игрока номера ячеек — как в инвентаре игры:
 * 0..8 — хотбар, 9..35 — рюкзак, а слоты меню — сначала рюкзак, потом хотбар (как у ванильного сундука).
 */
public final class MenuSpec {
    /** Чьи ячейки в разделе. */
    public enum SectionKind {
        /** Ячейки контейнера меню (сундук, печь, свой блок). */
        CONTAINER,
        /** Инвентарь игрока: 36 ячеек, 0..8 — хотбар, 9..35 — рюкзак. */
        PLAYER
    }

    /** Вид синхронизируемого значения. */
    public enum ValueKind {
        /** Целое число любого размера (на сервере — две 16-битные части). */
        INT,
        /** Флаг (одна часть). */
        BOOLEAN
    }

    /** Размер инвентаря игрока в разделе {@link SectionKind#PLAYER}. */
    public static final int PLAYER_SLOTS = 36;
    /** Ячеек в хотбаре. */
    public static final int HOTBAR = 9;

    /** Раздел ячеек меню. */
    public static final class Section {
        private final String name;
        private final SectionKind kind;
        private final int size;
        private final int firstSlot;
        private final int containerOffset;
        private final boolean takeOnly;
        private final int ordinal;

        Section(String name, SectionKind kind, int size, int firstSlot, int containerOffset, boolean takeOnly,
                int ordinal) {
            this.name = name;
            this.kind = kind;
            this.size = size;
            this.firstSlot = firstSlot;
            this.containerOffset = containerOffset;
            this.takeOnly = takeOnly;
            this.ordinal = ordinal;
        }

        public String name() {
            return name;
        }

        public SectionKind kind() {
            return kind;
        }

        public boolean isPlayer() {
            return kind == SectionKind.PLAYER;
        }

        /** Сколько ячеек. */
        public int size() {
            return size;
        }

        /** Номер первого слота раздела в меню. */
        public int firstSlot() {
            return firstSlot;
        }

        /** Где ячейки раздела начинаются в контейнере сервера; у раздела игрока — -1. */
        public int containerOffset() {
            return containerOffset;
        }

        /** Только брать: класть в ячейки раздела нельзя (результат крафта, выход печи). */
        public boolean isTakeOnly() {
            return takeOnly;
        }

        /** Порядковый номер раздела в описании. */
        public int ordinal() {
            return ordinal;
        }

        /** Есть ли слот меню в этом разделе. */
        public boolean contains(int slot) {
            return slot >= firstSlot && slot < firstSlot + size;
        }

        /** Слот меню для ячейки раздела {@code index} (у игрока — номер ячейки инвентаря игры). */
        public int slot(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("cell " + index + " of section " + name + " (" + size + ")");
            }
            if (kind == SectionKind.PLAYER) {
                return index < HOTBAR ? firstSlot + PLAYER_SLOTS - HOTBAR + index : firstSlot + index - HOTBAR;
            }
            return firstSlot + index;
        }

        /** Ячейка раздела для слота меню ({@code -1}, если слот не из этого раздела). */
        public int index(int slot) {
            if (!contains(slot)) {
                return -1;
            }
            int rel = slot - firstSlot;
            if (kind == SectionKind.PLAYER) {
                return rel < PLAYER_SLOTS - HOTBAR ? rel + HOTBAR : rel - (PLAYER_SLOTS - HOTBAR);
            }
            return rel;
        }

        @Override
        public String toString() {
            return "Section[" + name + " " + kind + " slots " + firstSlot + ".." + (firstSlot + size - 1) + "]";
        }
    }

    /** Синхронизируемое значение. */
    public static final class Value {
        private final String name;
        private final ValueKind kind;
        private final int index;
        private final int firstDataSlot;

        Value(String name, ValueKind kind, int index, int firstDataSlot) {
            this.name = name;
            this.kind = kind;
            this.index = index;
            this.firstDataSlot = firstDataSlot;
        }

        public String name() {
            return name;
        }

        public ValueKind kind() {
            return kind;
        }

        /** Номер значения (для {@link MenuData#get}). */
        public int index() {
            return index;
        }

        /** Первая 16-битная часть ({@code DataSlot} меню на сервере). */
        public int firstDataSlot() {
            return firstDataSlot;
        }

        /** Сколько частей занимает: 2 у целого, 1 у флага. */
        public int dataSlots() {
            return kind == ValueKind.INT ? 2 : 1;
        }

        @Override
        public String toString() {
            return "Value[" + name + " " + kind + " #" + index + "]";
        }
    }

    /**
     * Куда Shift+клик переносит стопку: слоты меню {@code from} (включительно) .. {@code to} (исключительно),
     * с конца, если {@code reverse} (как у ванильного {@code moveItemStackTo}).
     */
    public record Range(int from, int to, boolean reverse) {
    }

    private final List<Section> sections;
    private final List<Value> values;
    private final Map<String, Section> sectionsByName;
    private final Map<String, Value> valuesByName;
    private final Section player;
    private final int slotCount;
    private final int containerSize;
    private final int dataSlotCount;
    /** Для каждой части: чьё она значение и какая по счёту. */
    private final int[] dataValue;
    private final int[] dataPart;
    /** Для каждого слота меню — его раздел. */
    private final Section[] slotSections;
    /** Маршруты Shift+клика по разделам; у игрока два — из рюкзака и из хотбара. */
    private final List<List<Range>> routes;
    private final List<Range> fromHotbar;

    private MenuSpec(List<Section> sections, List<Value> values) {
        this.sections = Collections.unmodifiableList(sections);
        this.values = Collections.unmodifiableList(values);
        Map<String, Section> sByName = new HashMap<>();
        Section p = null;
        int slots = 0;
        int container = 0;
        for (Section s : sections) {
            sByName.put(s.name, s);
            slots += s.size;
            if (s.kind == SectionKind.PLAYER) {
                p = s;
            } else {
                container += s.size;
            }
        }
        this.sectionsByName = sByName;
        this.player = p;
        this.slotCount = slots;
        this.containerSize = container;

        Map<String, Value> vByName = new HashMap<>();
        int parts = 0;
        for (Value v : values) {
            vByName.put(v.name, v);
            parts += v.dataSlots();
        }
        this.valuesByName = vByName;
        this.dataSlotCount = parts;
        this.dataValue = new int[parts];
        this.dataPart = new int[parts];
        for (Value v : values) {
            for (int k = 0; k < v.dataSlots(); k++) {
                dataValue[v.firstDataSlot + k] = v.index;
                dataPart[v.firstDataSlot + k] = k;
            }
        }

        this.slotSections = new Section[slots];
        for (Section s : sections) {
            for (int i = 0; i < s.size; i++) {
                slotSections[s.firstSlot + i] = s;
            }
        }

        List<List<Range>> r = new ArrayList<>(sections.size());
        List<Range> hotbarRoute = List.of();
        for (Section s : sections) {
            if (s.kind == SectionKind.CONTAINER) {
                r.add(containerRoute(s));
            } else {
                List<Range> toContainers = new ArrayList<>();
                for (Section c : sections) {
                    if (c.kind == SectionKind.CONTAINER && !c.takeOnly) {
                        toContainers.add(new Range(c.firstSlot, c.firstSlot + c.size, false));
                    }
                }
                if (toContainers.isEmpty()) {
                    // Только игрок: рюкзак ↔ хотбар, как в окне инвентаря.
                    int hotbar = s.firstSlot + PLAYER_SLOTS - HOTBAR;
                    r.add(List.of(new Range(hotbar, hotbar + HOTBAR, false)));
                    hotbarRoute = List.of(new Range(s.firstSlot, hotbar, false));
                } else {
                    r.add(List.copyOf(toContainers));
                    hotbarRoute = List.copyOf(toContainers);
                }
            }
        }
        this.routes = r;
        this.fromHotbar = hotbarRoute;
    }

    private List<Range> containerRoute(Section from) {
        if (player != null) {
            return List.of(new Range(player.firstSlot, player.firstSlot + player.size, true));
        }
        List<Range> out = new ArrayList<>();
        for (Section c : sections) {
            if (c != from && c.kind == SectionKind.CONTAINER && !c.takeOnly) {
                out.add(new Range(c.firstSlot, c.firstSlot + c.size, false));
            }
        }
        return List.copyOf(out);
    }

    public static Builder builder() {
        return new Builder();
    }

    // ================================================================ разделы

    public List<Section> sections() {
        return sections;
    }

    /** Раздел по имени ({@link IllegalArgumentException}, если такого нет). */
    public Section section(String name) {
        Section s = sectionsByName.get(name);
        if (s == null) {
            throw new IllegalArgumentException("no section '" + name + "' in " + sectionsByName.keySet());
        }
        return s;
    }

    /** Раздел инвентаря игрока или {@code null}. */
    public Section playerSection() {
        return player;
    }

    /** Раздел слота меню или {@code null}, если такого слота нет. */
    public Section sectionOf(int slot) {
        return slot >= 0 && slot < slotSections.length ? slotSections[slot] : null;
    }

    /** Сколько всего слотов в меню. */
    public int slotCount() {
        return slotCount;
    }

    /** Сколько ячеек нужно контейнеру сервера (все разделы, кроме игрока). */
    public int containerSize() {
        return containerSize;
    }

    /**
     * Shift+клик по умолчанию: из контейнера — в инвентарь игрока (с хотбара назад, как у сундука; без игрока — в
     * другие разделы), от игрока — в разделы контейнера, куда можно класть, по порядку; если их нет — между
     * рюкзаком и хотбаром. Списки готовые, без выделения памяти.
     */
    public List<Range> quickMoveRoute(int slot) {
        Section s = sectionOf(slot);
        if (s == null) {
            return List.of();
        }
        if (s.kind == SectionKind.PLAYER && s.index(slot) < HOTBAR) {
            return fromHotbar;
        }
        return routes.get(s.ordinal);
    }

    // ================================================================ значения

    public List<Value> values() {
        return values;
    }

    /** Значение по имени ({@link IllegalArgumentException}, если такого нет). */
    public Value value(String name) {
        Value v = valuesByName.get(name);
        if (v == null) {
            throw new IllegalArgumentException("no value '" + name + "' in " + valuesByName.keySet());
        }
        return v;
    }

    /** Сколько 16-битных частей ({@code DataSlot}) нужно меню на сервере. */
    public int dataSlotCount() {
        return dataSlotCount;
    }

    /**
     * Часть {@code dataSlot} для отправки клиенту, когда значения сервера даёт {@code data}. Части 16-битные:
     * игра передаёт {@code DataSlot} как {@code short}, поэтому целое делится на младшую и старшую половины.
     */
    public int dataPart(MenuData data, int dataSlot) {
        int valueIndex = dataValue[dataSlot];
        int v = data == null ? 0 : data.get(valueIndex);
        if (values.get(valueIndex).kind == ValueKind.BOOLEAN) {
            return v != 0 ? 1 : 0;
        }
        return dataPart[dataSlot] == 0 ? v & 0xFFFF : v >> 16;
    }

    /** Значение из принятых частей {@code raw} (по номерам {@code DataSlot}; знак части не важен). */
    public int decode(Value value, int[] raw) {
        int lo = raw[value.firstDataSlot];
        if (value.kind == ValueKind.BOOLEAN) {
            return lo != 0 ? 1 : 0;
        }
        int hi = raw[value.firstDataSlot + 1];
        return (hi << 16) | (lo & 0xFFFF);
    }

    @Override
    public String toString() {
        return "MenuSpec" + sections + values;
    }

    // ================================================================ построение

    /** Строитель описания. */
    public static final class Builder {
        private final List<Section> sections = new ArrayList<>();
        private final List<Value> values = new ArrayList<>();
        private int slots;
        private int container;
        private int parts;

        private Builder() {
        }

        /** Раздел контейнера на {@code size} ячеек. */
        public Builder container(String name, int size) {
            if (size <= 0) {
                throw new IllegalArgumentException("section '" + name + "' needs at least one cell");
            }
            checkName(name);
            sections.add(new Section(name, SectionKind.CONTAINER, size, slots, container, false, sections.size()));
            slots += size;
            container += size;
            return this;
        }

        /** Инвентарь игрока (36 ячеек) под именем {@code "player"}. */
        public Builder playerInventory() {
            return playerInventory("player");
        }

        public Builder playerInventory(String name) {
            for (Section s : sections) {
                if (s.kind == SectionKind.PLAYER) {
                    throw new IllegalStateException("player inventory is already in the menu");
                }
            }
            checkName(name);
            sections.add(new Section(name, SectionKind.PLAYER, PLAYER_SLOTS, slots, -1, false, sections.size()));
            slots += PLAYER_SLOTS;
            return this;
        }

        /** Последний добавленный раздел контейнера — только брать (результат, выход). */
        public Builder takeOnly() {
            if (sections.isEmpty() || sections.get(sections.size() - 1).kind != SectionKind.CONTAINER) {
                throw new IllegalStateException("takeOnly() follows a container(...) section");
            }
            Section s = sections.remove(sections.size() - 1);
            sections.add(new Section(s.name, s.kind, s.size, s.firstSlot, s.containerOffset, true, s.ordinal));
            return this;
        }

        /** Целое значение (любое int). */
        public Builder intValue(String name) {
            return value(name, ValueKind.INT);
        }

        /** Флаг. */
        public Builder boolValue(String name) {
            return value(name, ValueKind.BOOLEAN);
        }

        private Builder value(String name, ValueKind kind) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("value needs a name");
            }
            for (Value v : values) {
                if (v.name.equals(name)) {
                    throw new IllegalArgumentException("duplicate value '" + name + "'");
                }
            }
            Value v = new Value(name, kind, values.size(), parts);
            values.add(v);
            parts += v.dataSlots();
            return this;
        }

        private void checkName(String name) {
            if (name == null || name.isEmpty()) {
                throw new IllegalArgumentException("section needs a name");
            }
            for (Section s : sections) {
                if (s.name.equals(name)) {
                    throw new IllegalArgumentException("duplicate section '" + name + "'");
                }
            }
        }

        public MenuSpec build() {
            return new MenuSpec(new ArrayList<>(sections), new ArrayList<>(values));
        }
    }
}
