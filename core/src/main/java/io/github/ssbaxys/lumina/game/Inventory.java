package io.github.ssbaxys.lumina.game;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Неизменяемый снимок инвентаря игрока. {@link GameData#inventory()} выдаёт новый снимок только
 * когда что-то поменялось, поэтому подписка на сигнал срабатывает ровно на изменения.
 *
 * <p>Нумерация слотов — как в {@link GameState}: 0–8 панель, 9–35 рюкзак, 36–39 броня, 40 вторая рука.</p>
 */
public final class Inventory {
    /** Слот: предмет и количество; пустой слот — {@link #EMPTY}. */
    public record Slot(String itemId, int count) {
        public static final Slot EMPTY = new Slot("", 0);

        public boolean isEmpty() {
            return count <= 0 || itemId.isEmpty();
        }
    }

    public static final Inventory EMPTY = new Inventory(new String[0], new int[0]);

    private final String[] itemIds;
    private final int[] counts;

    private Inventory(String[] itemIds, int[] counts) {
        this.itemIds = itemIds;
        this.counts = counts;
    }

    /** Снимок из массивов (копируются): {@code null} и пустые строки — пустые слоты. */
    public static Inventory of(String[] itemIds, int[] counts, int size) {
        String[] ids = new String[size];
        int[] cs = new int[size];
        for (int i = 0; i < size; i++) {
            String id = itemIds[i];
            boolean empty = id == null || id.isEmpty() || counts[i] <= 0;
            ids[i] = empty ? "" : id;
            cs[i] = empty ? 0 : counts[i];
        }
        return new Inventory(ids, cs);
    }

    public int size() {
        return itemIds.length;
    }

    /** Предмет в слоте ({@code ""} — пусто или слота нет). */
    public String itemId(int slot) {
        return slot >= 0 && slot < itemIds.length ? itemIds[slot] : "";
    }

    public int count(int slot) {
        return slot >= 0 && slot < counts.length ? counts[slot] : 0;
    }

    public Slot slot(int slot) {
        String id = itemId(slot);
        return id.isEmpty() ? Slot.EMPTY : new Slot(id, counts[slot]);
    }

    public boolean isEmpty(int slot) {
        return itemId(slot).isEmpty();
    }

    /** Сколько всего таких предметов во всех слотах. */
    public int total(String itemId) {
        int sum = 0;
        for (int i = 0; i < itemIds.length; i++) {
            if (itemIds[i].equals(itemId)) {
                sum += counts[i];
            }
        }
        return sum;
    }

    /** Есть ли предмет хотя бы в одном слоте. */
    public boolean contains(String itemId) {
        return total(itemId) > 0;
    }

    /** Занятых слотов. */
    public int usedSlots() {
        int n = 0;
        for (String id : itemIds) {
            if (!id.isEmpty()) {
                n++;
            }
        }
        return n;
    }

    /** Слоты по порядку (создаёт список; для отрисовки удобнее {@link #itemId}/{@link #count}). */
    public List<Slot> slots() {
        Slot[] out = new Slot[itemIds.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = slot(i);
        }
        return List.of(out);
    }

    /** Совпадает ли снимок с массивами платформы (без выделения памяти). */
    boolean matches(String[] ids, int[] cs, int size) {
        if (size != itemIds.length) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            String id = ids[i];
            boolean empty = id == null || id.isEmpty() || cs[i] <= 0;
            if (empty) {
                if (!itemIds[i].isEmpty()) {
                    return false;
                }
            } else if (cs[i] != counts[i] || !id.equals(itemIds[i])) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Inventory other && Arrays.equals(itemIds, other.itemIds) && Arrays.equals(counts, other.counts);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(itemIds), Arrays.hashCode(counts));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Inventory[");
        for (int i = 0; i < itemIds.length; i++) {
            if (!itemIds[i].isEmpty()) {
                if (sb.length() > 10) {
                    sb.append(", ");
                }
                sb.append(i).append('=').append(itemIds[i]).append('×').append(counts[i]);
            }
        }
        return sb.append(']').toString();
    }
}
