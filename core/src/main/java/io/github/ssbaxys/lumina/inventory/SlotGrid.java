package io.github.ssbaxys.lumina.inventory;

import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.GridLayout;
import io.github.ssbaxys.lumina.widget.Tooltip;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Сетка слотов над ячейками инвентаря: сундук 9×3, рюкзак, хотбар. Слоты идут по строкам слева направо,
 * без зазора у ванильного вида (как в игре) и с зазором 2 у современного. Всё, что задаётся сетке (вид,
 * группа, Shift+клик), передаётся её слотам.
 *
 * <pre>{@code
 * Inventory player = new Inventory(36);
 * SlotGrid main = new SlotGrid(player, 9, 27, 9);      // ячейки 9..35 — рюкзак
 * SlotGrid hotbar = new SlotGrid(player, 0, 9, 9);     // ячейки 0..8 — хотбар
 * SlotGrid chestGrid = new SlotGrid(chest, 9).look(Slot.Look.MODERN).quickMoveTo(player);
 * }</pre>
 */
public class SlotGrid extends Widget<SlotGrid> {
    private final Inventory inventory;
    private final int from;
    private final int columns;
    private final List<Slot> slots = new ArrayList<>();
    private float slotSize = Slot.SIZE;
    private float gap = -1f;
    private Slot.Look look = Slot.Look.VANILLA;

    /** Все ячейки инвентаря по {@code columns} в ряд. */
    public SlotGrid(Inventory inventory, int columns) {
        this(inventory, 0, inventory.size(), columns);
    }

    /** Ячейки {@code from .. from+count-1} по {@code columns} в ряд. */
    public SlotGrid(Inventory inventory, int from, int count, int columns) {
        super("slot-grid");
        if (from < 0 || count < 0 || from + count > inventory.size()) {
            throw new IndexOutOfBoundsException("slots " + from + "+" + count + " of " + inventory.size());
        }
        this.inventory = inventory;
        this.from = from;
        this.columns = Math.max(1, columns);
        for (int i = 0; i < count; i++) {
            Slot s = new Slot(inventory, from + i);
            slots.add(s);
            addChild(s);
        }
        resize();
    }

    public Inventory inventory() {
        return inventory;
    }

    /** Первая показанная ячейка инвентаря. */
    public int firstIndex() {
        return from;
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return (slots.size() + columns - 1) / columns;
    }

    /** Слоты по порядку. */
    public List<Slot> slots() {
        return Collections.unmodifiableList(slots);
    }

    /** {@code i}-й слот сетки (не ячейка инвентаря: ячейка — {@code firstIndex() + i}). */
    public Slot slot(int i) {
        return slots.get(i);
    }

    public SlotGrid look(Slot.Look look) {
        this.look = look == null ? Slot.Look.VANILLA : look;
        for (Slot s : slots) {
            s.look(this.look);
        }
        resize();
        return this;
    }

    /** Размер слота (18 по умолчанию; иконка — 16/18 от него). */
    public SlotGrid slotSize(float size) {
        this.slotSize = Math.max(8f, size);
        resize();
        return this;
    }

    /** Зазор между слотами ({@code < 0} — по виду: 0 у ванильного, 2 у современного). */
    public SlotGrid gap(float gap) {
        this.gap = gap;
        resize();
        return this;
    }

    public SlotGrid group(SlotGroup group) {
        for (Slot s : slots) {
            s.group(group);
        }
        return this;
    }

    /** Куда уходит Shift+клик из слотов этой сетки (можно несколько: по порядку). */
    public SlotGrid quickMoveTo(Inventory target) {
        for (Slot s : slots) {
            s.quickMoveTo(target);
        }
        return this;
    }

    public SlotGrid quickMoveTo(Inventory target, int fromIndex, int toIndex) {
        for (Slot s : slots) {
            s.quickMoveTo(target, fromIndex, toIndex);
        }
        return this;
    }

    public SlotGrid takeable(boolean takeable) {
        for (Slot s : slots) {
            s.takeable(takeable);
        }
        return this;
    }

    public SlotGrid dropTarget(boolean dropTarget) {
        for (Slot s : slots) {
            s.dropTarget(dropTarget);
        }
        return this;
    }

    public SlotGrid tooltipKind(Tooltip.Kind kind) {
        for (Slot s : slots) {
            s.tooltipKind(kind);
        }
        return this;
    }

    private float effectiveGap() {
        return gap >= 0f ? gap : look == Slot.Look.MODERN ? 2f : 0f;
    }

    /** Размер сетки — ровно по слотам, чтобы растягивающая раскладка родителя не разносила их. */
    private void resize() {
        float g = effectiveGap();
        for (Slot s : slots) {
            s.size(slotSize, slotSize);
        }
        setLayout(new GridLayout(columns, g, g));
        int cols = Math.min(columns, Math.max(1, slots.size()));
        float w = cols * slotSize + (cols - 1) * g;
        float h = rows() * slotSize + Math.max(0, rows() - 1) * g;
        layoutParams().width = Dim.px(w);
        layoutParams().height = Dim.px(h);
        invalidateLayout();
    }
}
