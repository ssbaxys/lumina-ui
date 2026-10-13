package io.github.ssbaxys.lumina.menu;

import io.github.ssbaxys.lumina.inventory.Inventory;
import io.github.ssbaxys.lumina.inventory.RemoteInventory;
import io.github.ssbaxys.lumina.inventory.Slot;
import io.github.ssbaxys.lumina.inventory.Stack;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Signal;

import java.util.List;

/**
 * Клиентская сторона меню-контейнера, которым владеет сервер (как ванильный сундук). По {@link MenuSpec} даёт
 * на каждый раздел обычный {@link Inventory} — его показывают {@code Slot}, {@code SlotGrid} и
 * {@code InventoryPanel}, — но такой инвентарь удалённый ({@link Inventory#remote}): перетаскивание, Shift+клик,
 * выброс и клавиши хотбара превращаются в щелчки по слотам меню игры ({@link MenuChannel}), сервер их проверяет,
 * а содержимое приходит обратно синхронизацией. Синхронизируемые числа — сигналы ({@link #intValue},
 * {@link #boolValue}), свои кнопки — {@link #sendAction}.
 *
 * <pre>{@code
 * UiRoot create(RemoteMenu menu) {
 *     InventoryPanel panel = InventoryPanel.chest(menu.title(), menu.inventory("chest"), menu.playerInventory(),
 *             Slot.Look.MODERN);
 *     ProgressBar bar = new ProgressBar().bind(menu.intValue("progress").map(p -> p / 100f));
 *     Button sort = new Button("Сортировать", () -> menu.sendAction(SORT, 0));
 *     return new UiRoot(Panel.column().add(panel, bar, sort), ModernTheme.dark());
 * }
 * }</pre>
 *
 * <p>Жесты переводятся в ванильные щелчки: перенос A→B — «взять A» (вся стопка или половина), «положить в B» и,
 * если что-то осталось на курсоре, «положить обратно в A»; выброс — «взять A» и щелчок мимо окна; Shift+клик —
 * быстрый перенос; клавиши 1–9 и F — обмен с хотбаром и второй рукой; Q — выбросить один предмет или стопку.</p>
 *
 * <p>Методы с пометкой «для бэкенда» вызывает загрузчик: он кладёт в модель содержимое слотов, курсор и части
 * значений, а {@link #publish()} раз в кадр обновляет сигналы. Всё — на потоке клиента.</p>
 */
public final class RemoteMenu implements RemoteInventory {
    private final MenuSpec spec;
    private final String title;
    private final MenuChannel channel;
    private final Inventory[] inventories;
    private final List<MenuSpec.Section> sections;
    private final int[] raw;
    private final Signal<?>[] signals;
    private final Signal<Stack> carried = Signal.of(Stack.EMPTY);
    private boolean dirty;

    public RemoteMenu(MenuSpec spec, String title, MenuChannel channel) {
        this.spec = spec;
        this.title = title == null ? "" : title;
        this.channel = channel;
        this.sections = spec.sections();
        this.inventories = new Inventory[sections.size()];
        for (int i = 0; i < inventories.length; i++) {
            MenuSpec.Section s = sections.get(i);
            Inventory inv = new Inventory(s.size()).remote(this);
            if (s.isTakeOnly()) {
                inv.filter((index, stack) -> false);
            }
            inventories[i] = inv;
        }
        this.raw = new int[spec.dataSlotCount()];
        List<MenuSpec.Value> values = spec.values();
        this.signals = new Signal<?>[values.size()];
        for (int i = 0; i < signals.length; i++) {
            signals[i] = values.get(i).kind() == MenuSpec.ValueKind.BOOLEAN ? Signal.of(false) : Signal.of(0);
        }
    }

    public MenuSpec spec() {
        return spec;
    }

    /** Заголовок окна, присланный сервером. */
    public String title() {
        return title;
    }

    // ================================================================ ячейки

    /** Инвентарь раздела (номера ячеек — как в {@link MenuSpec.Section#index}). */
    public Inventory inventory(String section) {
        return inventories[spec.section(section).ordinal()];
    }

    public Inventory inventory(MenuSpec.Section section) {
        return inventories[section.ordinal()];
    }

    /** Инвентарь игрока (36 ячеек: 0..8 — хотбар, 9..35 — рюкзак). */
    public Inventory playerInventory() {
        MenuSpec.Section p = spec.playerSection();
        if (p == null) {
            throw new IllegalStateException("the menu has no player inventory: " + spec);
        }
        return inventories[p.ordinal()];
    }

    /** Что сервер держит на курсоре (обычно пусто: жесты не оставляют там ничего). */
    public ReadableSignal<Stack> carried() {
        return carried;
    }

    /** Слот меню для ячейки {@code index} инвентаря {@code inventory} или {@code -1}, если инвентарь не отсюда. */
    public int menuSlot(Inventory inventory, int index) {
        for (int i = 0; i < inventories.length; i++) {
            if (inventories[i] == inventory) {
                MenuSpec.Section s = sections.get(i);
                return index >= 0 && index < s.size() ? s.slot(index) : -1;
            }
        }
        return -1;
    }

    // ================================================================ значения

    /** Целое значение как сигнал (меняется, когда придёт новое с сервера). */
    @SuppressWarnings("unchecked")
    public ReadableSignal<Integer> intValue(MenuSpec.Value value) {
        if (value.kind() != MenuSpec.ValueKind.INT) {
            throw new IllegalArgumentException(value + " is not an int value");
        }
        return (ReadableSignal<Integer>) signals[value.index()];
    }

    public ReadableSignal<Integer> intValue(String name) {
        return intValue(spec.value(name));
    }

    /** Флаг как сигнал. */
    @SuppressWarnings("unchecked")
    public ReadableSignal<Boolean> boolValue(MenuSpec.Value value) {
        if (value.kind() != MenuSpec.ValueKind.BOOLEAN) {
            throw new IllegalArgumentException(value + " is not a boolean value");
        }
        return (ReadableSignal<Boolean>) signals[value.index()];
    }

    public ReadableSignal<Boolean> boolValue(String name) {
        return boolValue(spec.value(name));
    }

    // ================================================================ действия

    /** Своё действие на сервер: номер {@code action >= 0} и число (обработчик действий меню на сервере). */
    public void sendAction(int action, int value) {
        MenuActions.encode(action, value, channel::clickButton);
    }

    public void sendAction(int action) {
        sendAction(action, 0);
    }

    /** Закрыть окно (как Esc). */
    public void close() {
        channel.close();
    }

    // ================================================================ запросы (RemoteInventory)

    @Override
    public boolean requestMove(Inventory from, int fromIndex, Inventory to, int toIndex, boolean half) {
        int a = menuSlot(from, fromIndex);
        int b = menuSlot(to, toIndex);
        if (a < 0 || b < 0 || a == b || channel.isCarrying()) {
            return false;
        }
        channel.click(a, half ? 1 : 0, SlotClick.PICKUP);
        channel.click(b, 0, SlotClick.PICKUP);
        if (channel.isCarrying()) {
            // Не влезло (или обмен не случился): остаток — обратно в источник.
            channel.click(a, 0, SlotClick.PICKUP);
        }
        channel.flush();
        return true;
    }

    @Override
    public boolean requestQuickMove(Inventory inventory, int index) {
        int a = menuSlot(inventory, index);
        if (a < 0) {
            return false;
        }
        channel.click(a, 0, SlotClick.QUICK_MOVE);
        channel.flush();
        return true;
    }

    @Override
    public boolean requestDrop(Inventory inventory, int index, boolean half) {
        int a = menuSlot(inventory, index);
        if (a < 0 || channel.isCarrying()) {
            return false;
        }
        channel.click(a, half ? 1 : 0, SlotClick.PICKUP);
        channel.click(SlotClick.OUTSIDE, 0, SlotClick.PICKUP);
        channel.flush();
        return true;
    }

    @Override
    public boolean press(Inventory inventory, int index, int button) {
        if (!channel.isCarrying()) {
            return false;
        }
        int a = menuSlot(inventory, index);
        if (a < 0) {
            return false;
        }
        channel.click(a, button == 1 ? 1 : 0, SlotClick.PICKUP);
        channel.flush();
        return true;
    }

    /** Обмен ячейки с хотбаром ({@code 0..8}) или второй рукой ({@link SlotClick#OFFHAND}). */
    public boolean requestSwap(Inventory inventory, int index, int hotbarSlot) {
        int a = menuSlot(inventory, index);
        if (a < 0 || (hotbarSlot < 0 || hotbarSlot >= MenuSpec.HOTBAR) && hotbarSlot != SlotClick.OFFHAND) {
            return false;
        }
        channel.click(a, hotbarSlot, SlotClick.SWAP);
        channel.flush();
        return true;
    }

    /** Выбросить из ячейки один предмет или всю стопку. */
    public boolean requestThrow(Inventory inventory, int index, boolean wholeStack) {
        int a = menuSlot(inventory, index);
        if (a < 0 || channel.isCarrying()) {
            return false;
        }
        channel.click(a, wholeStack ? 1 : 0, SlotClick.THROW);
        channel.flush();
        return true;
    }

    /**
     * Клавиши 1–9 / F над слотом этого меню: обмен с хотбаром. {@code hovered} — элемент под курсором
     * ({@code UiRoot.hovered()}); {@code false} — под курсором не наш слот или стопку сейчас несут.
     */
    public boolean swapHovered(UiElement hovered, int hotbarSlot) {
        Slot slot = slotAt(hovered);
        return slot != null && requestSwap(slot.inventory(), slot.index(), hotbarSlot);
    }

    /** Клавиша Q над слотом этого меню: выбросить один предмет или (с Ctrl) всю стопку. */
    public boolean throwHovered(UiElement hovered, boolean wholeStack) {
        Slot slot = slotAt(hovered);
        return slot != null && slot.isTakeable() && !slot.stack().isEmpty()
                && requestThrow(slot.inventory(), slot.index(), wholeStack);
    }

    private Slot slotAt(UiElement hovered) {
        for (UiElement e = hovered; e != null; e = e.parent()) {
            if (e instanceof Slot s) {
                if (s.inventory().remote() != this || !s.isEnabled() || s.group().isDragging()) {
                    return null;
                }
                return s;
            }
        }
        return null;
    }

    // ================================================================ для бэкенда

    /** Содержимое слота меню пришло с сервера (или из предсказания игры). */
    public void setSlot(int menuSlot, Stack stack) {
        MenuSpec.Section s = spec.sectionOf(menuSlot);
        if (s != null) {
            inventories[s.ordinal()].set(s.index(menuSlot), stack);
        }
    }

    /** Что на курсоре. */
    public void setCarried(Stack stack) {
        carried.set(stack == null ? Stack.EMPTY : stack);
    }

    /** Пришла часть значения номер {@code dataSlot} ({@code DataSlot} меню); сигналы обновит {@link #publish()}. */
    public void setData(int dataSlot, int part) {
        if (dataSlot >= 0 && dataSlot < raw.length && raw[dataSlot] != part) {
            raw[dataSlot] = part;
            dirty = true;
        }
    }

    /**
     * Обновить сигналы значений из принятых частей. Части одного числа приходят отдельными пакетами, поэтому
     * сигналы обновляются разом (раз в кадр или тик), а не на каждую часть.
     */
    @SuppressWarnings("unchecked")
    public void publish() {
        if (!dirty) {
            return;
        }
        dirty = false;
        List<MenuSpec.Value> values = spec.values();
        for (int i = 0; i < signals.length; i++) {
            MenuSpec.Value v = values.get(i);
            int decoded = spec.decode(v, raw);
            if (v.kind() == MenuSpec.ValueKind.BOOLEAN) {
                ((Signal<Boolean>) signals[i]).set(decoded != 0);
            } else {
                ((Signal<Integer>) signals[i]).set(decoded);
            }
        }
    }
}
