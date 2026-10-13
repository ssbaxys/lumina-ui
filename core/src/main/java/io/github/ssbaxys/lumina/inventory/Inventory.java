package io.github.ssbaxys.lumina.inventory;

import io.github.ssbaxys.lumina.reactive.Subscription;

import java.util.Arrays;

/**
 * Модель инвентаря: ряд ячеек со стопками ({@link Stack}). Слоты ({@link Slot}, {@link SlotGrid}) только
 * показывают ячейки и меняют их через эту модель, поэтому мод синхронизирует данные одной подпиской
 * {@link #onChange}, а логику можно проверять без интерфейса.
 *
 * <pre>{@code
 * Inventory chest = new Inventory(27);
 * chest.set(0, Stack.of("minecraft:diamond", 5));
 * Stack left = chest.insert(Stack.of("minecraft:stone", 100));   // разложит по стопкам, вернёт остаток
 * chest.filter((index, stack) -> index != 26 || stack.itemId().endsWith("_ingot"));
 * chest.onChange((inv, index, before, after) -> sendToServer(index, after));
 * }</pre>
 */
public final class Inventory {
    /** Слушатель изменения ячейки. */
    @FunctionalInterface
    public interface Listener {
        void changed(Inventory inventory, int index, Stack before, Stack after);
    }

    /** Какие предметы можно положить в ячейку (броня — только в свои места и т. п.). */
    @FunctionalInterface
    public interface Filter {
        boolean accepts(int index, Stack stack);
    }

    private final Stack[] stacks;
    private Listener[] listeners = new Listener[0];
    private Filter filter;
    private int limit = 99;
    private RemoteInventory remote;

    public Inventory(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("size < 0: " + size);
        }
        stacks = new Stack[size];
        Arrays.fill(stacks, Stack.EMPTY);
    }

    /** Инвентарь с готовыми стопками по порядку. */
    public static Inventory of(Stack... stacks) {
        Inventory inv = new Inventory(stacks.length);
        for (int i = 0; i < stacks.length; i++) {
            inv.stacks[i] = stacks[i] == null ? Stack.EMPTY : stacks[i];
        }
        return inv;
    }

    public int size() {
        return stacks.length;
    }

    /** Стопка в ячейке (никогда не {@code null}: пустая — {@link Stack#EMPTY}). */
    public Stack get(int index) {
        return stacks[index];
    }

    public boolean isEmpty(int index) {
        return stacks[index].isEmpty();
    }

    /** Положить стопку в ячейку как есть (без проверок фильтра); {@code null} — очистить. */
    public void set(int index, Stack stack) {
        Stack next = stack == null || stack.isEmpty() ? Stack.EMPTY : stack;
        Stack before = stacks[index];
        if (before == next || before.equals(next)) {
            stacks[index] = next;
            return;
        }
        stacks[index] = next;
        Listener[] ls = listeners;
        for (Listener l : ls) {
            l.changed(this, index, before, next);
        }
    }

    /** Очистить все ячейки. */
    public void clear() {
        for (int i = 0; i < stacks.length; i++) {
            set(i, Stack.EMPTY);
        }
    }

    // ================================================================ правила

    /** Фильтр ячеек ({@code null} — принимают всё). */
    public Inventory filter(Filter filter) {
        this.filter = filter;
        return this;
    }

    /** Общий предел стопки для ячеек этого инвентаря (по умолчанию 99 — решает сама стопка). */
    public Inventory limit(int maxPerSlot) {
        this.limit = Math.max(1, maxPerSlot);
        return this;
    }

    /**
     * Ячейки живут на другой стороне (сервере): слоты над этим инвентарём не меняют его сами, а просят
     * {@code remote} (перетаскивание, Shift+клик, выброс), а содержимое сюда кладёт тот, кто синхронизирует.
     * {@code null} — обычный локальный инвентарь (по умолчанию).
     */
    public Inventory remote(RemoteInventory remote) {
        this.remote = remote;
        return this;
    }

    /** Хозяин ячеек на сервере или {@code null}, если инвентарь локальный. */
    public RemoteInventory remote() {
        return remote;
    }

    /** Можно ли положить стопку в ячейку (фильтр). Пустую можно всегда. */
    public boolean accepts(int index, Stack stack) {
        return stack == null || stack.isEmpty() || filter == null || filter.accepts(index, stack);
    }

    /** Сколько таких предметов помещается в ячейку: предел стопки и предел инвентаря. */
    public int maxStack(int index, Stack stack) {
        return Math.min(limit, stack == null || stack.isEmpty() ? 64 : stack.maxStackSize());
    }

    // ================================================================ операции

    /**
     * Добавить стопку: сначала докладывает в такие же стопки, затем в пустые ячейки (с учётом фильтра).
     *
     * @return остаток, который не поместился ({@link Stack#EMPTY}, если поместилось всё)
     */
    public Stack insert(Stack stack) {
        return insert(stack, 0, stacks.length);
    }

    /** {@link #insert(Stack)} только в ячейки {@code from} (включительно) .. {@code to} (исключительно). */
    public Stack insert(Stack stack, int from, int to) {
        if (stack == null || stack.isEmpty()) {
            return Stack.EMPTY;
        }
        int lo = Math.max(0, from);
        int hi = Math.min(stacks.length, to);
        Stack rest = stack;
        for (int i = lo; i < hi && !rest.isEmpty(); i++) {
            Stack here = stacks[i];
            if (here.canStackWith(rest) && accepts(i, rest)) {
                int room = maxStack(i, here) - here.count();
                if (room > 0) {
                    int moved = Math.min(room, rest.count());
                    set(i, here.grow(moved));
                    rest = rest.grow(-moved);
                }
            }
        }
        for (int i = lo; i < hi && !rest.isEmpty(); i++) {
            if (stacks[i].isEmpty() && accepts(i, rest)) {
                int moved = Math.min(maxStack(i, rest), rest.count());
                set(i, rest.withCount(moved));
                rest = rest.grow(-moved);
            }
        }
        return rest;
    }

    /** Сколько всего предметов с таким id. */
    public int count(String itemId) {
        int n = 0;
        for (Stack s : stacks) {
            if (s.itemId().equals(itemId)) {
                n += s.count();
            }
        }
        return n;
    }

    /** Подписаться на изменения ячеек; закрыть подписку — отписаться. */
    public Subscription onChange(Listener listener) {
        Listener[] next = Arrays.copyOf(listeners, listeners.length + 1);
        next[listeners.length] = listener;
        listeners = next;
        return () -> removeListener(listener);
    }

    private void removeListener(Listener listener) {
        Listener[] old = listeners;
        for (int i = 0; i < old.length; i++) {
            if (old[i] == listener) {
                Listener[] next = new Listener[old.length - 1];
                System.arraycopy(old, 0, next, 0, i);
                System.arraycopy(old, i + 1, next, i, old.length - i - 1);
                listeners = next;
                return;
            }
        }
    }

    @Override
    public String toString() {
        return "Inventory" + Arrays.toString(stacks);
    }
}
