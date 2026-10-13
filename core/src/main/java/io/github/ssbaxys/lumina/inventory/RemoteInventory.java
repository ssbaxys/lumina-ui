package io.github.ssbaxys.lumina.inventory;

/**
 * Хозяин ячеек на другой стороне (обычно на сервере). Если он задан у {@link Inventory} ({@link Inventory#remote}),
 * слоты над этим инвентарём не меняют модель сами: перетаскивание, Shift+клик и выброс становятся запросами, а
 * содержимое ячеек обновляет тот, кто синхронизирует их с сервером. Пока стопку несут, всё выглядит как обычно —
 * «призрак» под курсором, остаток в источнике, подсветка; правила {@link SlotGroup} (валидатор, фильтры, обмен
 * только всей стопкой) проверяются по видимому содержимому, а окончательно решает сервер.
 *
 * <p>Перетаскивать можно только между ячейками одного хозяина: смешивать удалённые и локальные инвентари нельзя.
 * Готовая реализация — {@code menu.RemoteMenu}.</p>
 *
 * <p>Все методы возвращают {@code true}, если запрос отправлен.</p>
 */
public interface RemoteInventory {
    /**
     * Перенести стопку из ячейки в ячейку: всю или половину ({@code half}, ПКМ). Не влезшее возвращается в
     * источник, другой предмет при переносе всей стопки меняется местами.
     */
    boolean requestMove(Inventory from, int fromIndex, Inventory to, int toIndex, boolean half);

    /** Shift+клик: куда уйдёт стопка, решает сервер. */
    boolean requestQuickMove(Inventory inventory, int index);

    /** Выбросить стопку (всю или половину) — сброс мимо слотов, если {@link SlotGroup#onDropOutside} согласен. */
    boolean requestDrop(Inventory inventory, int index, boolean half);

    /**
     * Нажатие на слот кнопкой мыши. Нужно, только если сервер оставил стопку на курсоре: тогда щелчок кладёт её
     * в эту ячейку и возвращает {@code true}. По умолчанию — {@code false} (слот работает как обычно).
     */
    default boolean press(Inventory inventory, int index, int button) {
        return false;
    }
}
