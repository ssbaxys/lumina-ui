package io.github.ssbaxys.lumina.inventory;

import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.widget.Tooltips;

import java.util.ArrayList;
import java.util.List;

/**
 * Перетаскивание предметов между слотами: кто сейчас «в руке», правила сброса и быстрый перенос.
 * Слоты одной группы обмениваются стопками; по умолчанию все слоты в {@link #shared() общей} группе,
 * свою группу дают, когда нужны свои правила ({@code grid.group(new SlotGroup().validator(...))}).
 *
 * <p>Пока стопка в руке, модель ({@link Inventory}) не меняется: слот-источник просто рисует остаток,
 * а под курсором летит полупрозрачный «призрак». Всё меняется разом при сбросе, поэтому отмена ничего
 * не откатывает и мод видит одно изменение на каждую ячейку.</p>
 *
 * <ul>
 *   <li>ЛКМ-перетаскивание — вся стопка, ПКМ-перетаскивание — половина (с округлением вверх).</li>
 *   <li>Сброс на пустой слот или такую же стопку — докладывает, сколько влезет, остаток остаётся в источнике.</li>
 *   <li>Сброс всей стопки на другой предмет — обмен местами (если обе ячейки принимают чужой предмет).</li>
 *   <li>Сброс мимо слотов — {@link #onDropOutside} (например, выбросить), иначе отмена.</li>
 *   <li>Shift+клик — {@link #quickMove(Slot)}: свой обработчик {@link #onQuickMove} или перенос в инвентари
 *       из {@link Slot#quickMoveTo}.</li>
 * </ul>
 *
 * <pre>{@code
 * SlotGroup group = new SlotGroup()
 *         .validator((from, to, stack) -> to.inventory() != shop)       // в магазин не класть
 *         .onTransfer((from, to, moved) -> playSound())
 *         .onDropOutside((from, stack) -> { throwItem(stack); return true; });
 * }</pre>
 */
public final class SlotGroup {
    /** Можно ли положить стопку из {@code from} в {@code to} (свои правила поверх фильтров инвентаря). */
    @FunctionalInterface
    public interface DropValidator {
        boolean canDrop(Slot from, Slot to, Stack stack);
    }

    /** Свой Shift+клик; {@code true} — обработано, иначе работает перенос по умолчанию. */
    @FunctionalInterface
    public interface QuickMoveHandler {
        boolean quickMove(Slot slot, Stack stack);
    }

    /** Сброс мимо слотов; {@code true} — стопку забрали (выбросили), иначе она вернётся на место. */
    @FunctionalInterface
    public interface OutsideDropHandler {
        boolean dropOutside(Slot from, Stack stack);
    }

    /** После удачного переноса: сколько и куда ушло ({@code to == null} — быстрый перенос или сброс мимо). */
    @FunctionalInterface
    public interface TransferListener {
        void transferred(Slot from, Slot to, Stack moved);
    }

    private static final SlotGroup SHARED = new SlotGroup();

    private final List<Slot> slots = new ArrayList<>();
    private DropValidator validator;
    private QuickMoveHandler quickMoveHandler;
    private OutsideDropHandler outsideHandler;
    private TransferListener transferListener;

    private Slot source;
    private Stack picked = Stack.EMPTY;
    private Stack carried = Stack.EMPTY;
    private Stack remainder = Stack.EMPTY;
    private Ghost ghost;

    /** Группа слотов без своей. */
    public static SlotGroup shared() {
        return SHARED;
    }

    public SlotGroup validator(DropValidator validator) {
        this.validator = validator;
        return this;
    }

    public SlotGroup onQuickMove(QuickMoveHandler handler) {
        this.quickMoveHandler = handler;
        return this;
    }

    public SlotGroup onDropOutside(OutsideDropHandler handler) {
        this.outsideHandler = handler;
        return this;
    }

    public SlotGroup onTransfer(TransferListener listener) {
        this.transferListener = listener;
        return this;
    }

    // ================================================================ состояние

    /** Несётся ли сейчас стопка. */
    public boolean isDragging() {
        return source != null;
    }

    /** Слот, из которого несут, или {@code null}. */
    public Slot source() {
        return source;
    }

    /** Что в руке ({@link Stack#EMPTY}, если ничего). */
    public Stack carried() {
        return carried;
    }

    /** Что видно в слоте-источнике, пока стопка в руке. */
    Stack remainder() {
        return remainder;
    }

    /** Открытый «призрак» под курсором (для тестов) или {@code null}. */
    public UiElement ghost() {
        return ghost != null && ghost.root() != null ? ghost : null;
    }

    void register(Slot slot) {
        if (!slots.contains(slot)) {
            slots.add(slot);
        }
    }

    void unregister(Slot slot) {
        slots.remove(slot);
        if (source == slot) {
            cancel();
        }
    }

    // ================================================================ перетаскивание

    /**
     * Взять стопку из слота в руку: всю или половину ({@code half}). {@code false} — нечего брать, слот
     * не отдаёт предметы или уже что-то несут.
     */
    public boolean beginDrag(Slot from, boolean half) {
        if (source != null || from == null || !from.isTakeable()) {
            return false;
        }
        Stack stack = from.stack();
        if (stack.isEmpty()) {
            return false;
        }
        int amount = half ? (stack.count() + 1) / 2 : stack.count();
        source = from;
        picked = stack;
        carried = stack.withCount(amount);
        remainder = stack.withCount(stack.count() - amount);
        Tooltips.hideAll();
        refreshTooltips();
        UiRoot root = from.root();
        if (root != null) {
            if (ghost == null) {
                ghost = new Ghost();
            }
            float size = from.itemSize();
            if (ghost.size != size) {
                ghost.size = size;
                ghost.invalidateLayout();
            }
            root.showOverlay(ghost, root.mouseX() - size / 2f, root.mouseY() - size / 2f);
        }
        return true;
    }

    /** Курсор с ношей сдвинулся (координаты корня): «призрак» летит следом. */
    public void dragTo(float x, float y) {
        if (ghost != null && ghost.root() != null) {
            ghost.root().movePopup(ghost, x - ghost.size / 2f, y - ghost.size / 2f);
        }
    }

    /**
     * Положить ношу в слот (или мимо слотов, если {@code target == null}).
     *
     * @return {@code true}, если хоть что-то перенесено; иначе ноша просто возвращается на место
     */
    public boolean dropOn(Slot target) {
        if (source == null) {
            return false;
        }
        Slot from = source;
        Stack stack = carried;
        Stack original = picked;
        if (!sourceIntact()) {
            cancel();
            return false;
        }
        if (target == null) {
            if (outsideHandler != null && outsideHandler.dropOutside(from, stack)) {
                end();
                from.inventory().set(from.index(), original.grow(-stack.count()));
                fire(from, null, stack);
                return true;
            }
            cancel();
            return false;
        }
        int moved = placeable(from, target, stack);
        if (moved > 0) {
            end();
            Inventory ti = target.inventory();
            Stack there = ti.get(target.index());
            ti.set(target.index(), stack.withCount(there.count() + moved));
            from.inventory().set(from.index(), original.grow(-moved));
            fire(from, target, stack.withCount(moved));
            return true;
        }
        if (moved == SWAP) {
            end();
            Stack there = target.stack();
            target.inventory().set(target.index(), stack);
            from.inventory().set(from.index(), there);
            fire(from, target, stack);
            return true;
        }
        cancel();
        return false;
    }

    /** Положить ношу в слот под элементом {@code hovered} (или мимо, если там не слот). */
    public boolean dropAt(UiElement hovered) {
        Slot target = null;
        for (UiElement e = hovered; e != null; e = e.parent()) {
            if (e instanceof Slot s) {
                target = s;
                break;
            }
        }
        return dropOn(target);
    }

    /** Вернуть ношу на место (модель не менялась, так что просто закончить перенос). */
    public void cancel() {
        end();
    }

    /** Примет ли слот текущую ношу (для подсветки под курсором). */
    public boolean wouldAccept(Slot target) {
        return source != null && target != source && placeable(source, target, carried) != 0;
    }

    private static final int SWAP = -1;

    /**
     * Сколько предметов ляжет в {@code target}: {@code > 0} — доложить столько, {@link #SWAP} — обмен,
     * 0 — нельзя.
     */
    private int placeable(Slot from, Slot target, Stack stack) {
        if (target == from || !target.isDropTarget()
                || (target.inventory() == from.inventory() && target.index() == from.index())) {
            return 0;
        }
        Inventory ti = target.inventory();
        int tj = target.index();
        if (!ti.accepts(tj, stack) || (validator != null && !validator.canDrop(from, target, stack))) {
            return 0;
        }
        Stack there = ti.get(tj);
        if (there.isEmpty() || there.canStackWith(stack)) {
            int room = ti.maxStack(tj, stack) - there.count();
            return Math.max(0, Math.min(room, stack.count()));
        }
        // Обмен: только всей стопкой и если обе ячейки примут чужое.
        Inventory si = from.inventory();
        int sj = from.index();
        if (!remainder.isEmpty() || !from.isDropTarget() || stack.count() > ti.maxStack(tj, stack)
                || !si.accepts(sj, there) || there.count() > si.maxStack(sj, there)
                || (validator != null && !validator.canDrop(target, from, there))) {
            return 0;
        }
        return SWAP;
    }

    /** Не поменяли ли источник со стороны, пока стопку несли. */
    private boolean sourceIntact() {
        Stack now = source.inventory().get(source.index());
        return now.canStackWith(picked) && now.count() == picked.count();
    }

    private void end() {
        boolean was = source != null;
        source = null;
        picked = Stack.EMPTY;
        carried = Stack.EMPTY;
        remainder = Stack.EMPTY;
        if (ghost != null && ghost.root() != null) {
            ghost.root().closePopup(ghost);
        }
        if (was) {
            refreshTooltips();
        }
    }

    private void fire(Slot from, Slot to, Stack moved) {
        if (transferListener != null) {
            transferListener.transferred(from, to, moved);
        }
    }

    private void refreshTooltips() {
        for (int i = 0; i < slots.size(); i++) {
            slots.get(i).refreshTooltip();
        }
    }

    // ================================================================ быстрый перенос

    /**
     * Shift+клик: свой {@link #onQuickMove} или перенос стопки в инвентари {@link Slot#quickMoveTo}
     * (сначала докладывает в такие же стопки, потом в пустые ячейки).
     *
     * @return {@code true}, если что-то перенесено или обработчик взял работу на себя
     */
    public boolean quickMove(Slot slot) {
        if (slot == null || source != null || !slot.isTakeable()) {
            return false;
        }
        Stack stack = slot.stack();
        if (stack.isEmpty()) {
            return false;
        }
        if (quickMoveHandler != null && quickMoveHandler.quickMove(slot, stack)) {
            return true;
        }
        Stack rest = stack;
        for (Slot.Target t : slot.quickMoveTargets()) {
            if (rest.isEmpty()) {
                break;
            }
            if (t.inventory() == slot.inventory() && slot.index() >= t.from() && slot.index() < t.to()) {
                continue;
            }
            rest = t.inventory().insert(rest, t.from(), t.to());
        }
        if (rest.count() == stack.count()) {
            return false;
        }
        slot.inventory().set(slot.index(), rest);
        fire(slot, null, stack.withCount(stack.count() - rest.count()));
        return true;
    }

    // ================================================================ призрак

    /** Предмет под курсором: пассивный оверлей, мышь проходит сквозь него. */
    private final class Ghost extends UiElement {
        float size = 16f;

        Ghost() {
            super(null);
            setHitTestable(false);
            setOverflowsBounds(true);
        }

        @Override
        protected void measureContent(float maxWidth, float maxHeight) {
            setContentSize(size, size);
        }

        @Override
        protected void paintContent(Canvas canvas, float partialTick) {
            Stack s = carried;
            if (s.isEmpty()) {
                return;
            }
            Slot.paintStack(canvas, s, 0f, 0f, size, Slot.labelFor(s.count()));
        }
    }
}
