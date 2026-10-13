package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.inventory.InventoryPanel;
import io.github.ssbaxys.lumina.inventory.Slot;
import io.github.ssbaxys.lumina.inventory.SlotGrid;
import io.github.ssbaxys.lumina.inventory.SlotGroup;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.menu.MenuSpec;
import io.github.ssbaxys.lumina.menu.RemoteMenu;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ProgressBar;

/**
 * «Сундук Lumina» ({@code /luminatest chest}): настоящий контейнер на сервере на 27 ячеек в современном окне
 * Lumina. Сервер ведёт цикл прогресса, считает предметы, знает, разложен ли сундук, и присылает время мира (оно
 * больше 16 бит — проверка полных int); кнопки просят сервер отсортировать или наполнить сундук.
 *
 * <p>Здесь только общая часть: описание меню (одно для сервера и клиента) и интерфейс. Регистрация меню,
 * команда и сортировка настоящих стопок — в source set {@code testmod} каждого загрузчика.</p>
 */
public final class ChestDemo {
    /** Ряды сундука. */
    public static final int ROWS = 3;
    /** Действие «отсортировать»: число — {@link #BY_NAME} или {@link #BY_COUNT}. */
    public static final int SORT = 0;
    /** Действие «наполнить пробными предметами». */
    public static final int FILL = 1;
    public static final int BY_NAME = 0;
    public static final int BY_COUNT = 1;
    /** Длина цикла прогресса в тиках сервера. */
    public static final int CYCLE = 100;
    /** Размер слота окна. */
    private static final float SLOT = 22f;

    public static final MenuSpec SPEC = MenuSpec.builder()
            .container("chest", ROWS * 9)
            .playerInventory()
            .intValue("progress")
            .intValue("items")
            .boolValue("sorted")
            .intValue("time")
            .build();
    /** Тик цикла 0..{@link #CYCLE}. */
    public static final MenuSpec.Value PROGRESS = SPEC.value("progress");
    /** Предметов в сундуке. */
    public static final MenuSpec.Value ITEMS = SPEC.value("items");
    /** Разложен ли сундук (по id, без дыр). */
    public static final MenuSpec.Value SORTED = SPEC.value("sorted");
    /** Время мира в тиках. */
    public static final MenuSpec.Value TIME = SPEC.value("time");

    private ChestDemo() {
    }

    /** Интерфейс окна; {@code menu} — клиентская сторона меню. */
    public static UiRoot create(RemoteMenu menu) {
        SlotGroup group = new SlotGroup();
        InventoryPanel window = new InventoryPanel(Slot.Look.MODERN);

        Label sorted = Label.bound(menu.boolValue(SORTED).map(s -> s ? "Разложен" : "Не разложен")).style(Ui.MUTED);
        Panel header = Ui.row(8, Ui.heading(menu.title().isEmpty() ? "Сундук Lumina" : menu.title()), Ui.spacer(),
                sorted);
        header.width(Dim.FILL);

        SlotGrid chest = new SlotGrid(menu.inventory("chest"), 9).look(Slot.Look.MODERN).slotSize(SLOT).group(group);

        ProgressBar progress = new ProgressBar().bind(menu.intValue(PROGRESS).map(p -> p / (float) CYCLE));
        Label cycle = Label.bound(menu.intValue(PROGRESS).map(p -> "Цикл сервера: " + (p * 100 / CYCLE) + "%"));
        Label stats = Label.bound(menu.intValue(ITEMS).map(n -> "Предметов: " + n)).style(Ui.MUTED);
        Label time = Label.bound(menu.intValue(TIME).map(t -> "Тик мира: " + t)).style(Ui.MUTED);

        Panel sortRow = Ui.row(6,
                new Button("По имени", () -> menu.sendAction(SORT, BY_NAME)).styleClass("button primary"),
                new Button("По количеству", () -> menu.sendAction(SORT, BY_COUNT)));
        Panel moreRow = Ui.row(6,
                new Button("Наполнить", () -> menu.sendAction(FILL)).styleClass("button ghost"),
                Ui.spacer(),
                new Button("Закрыть", menu::close).styleClass("button ghost"));
        moreRow.width(Dim.FILL);

        SlotGrid main = new SlotGrid(menu.playerInventory(), 9, 27, 9).look(Slot.Look.MODERN).slotSize(SLOT)
                .group(group);
        SlotGrid hotbar = new SlotGrid(menu.playerInventory(), 0, 9, 9).look(Slot.Look.MODERN).slotSize(SLOT)
                .group(group);

        window.add(header, chest)
                .add(Ui.column(3, cycle, progress, Ui.row(10, stats, time)).width(Dim.FILL), 4f)
                .add(Ui.column(4, sortRow, moreRow).width(Dim.FILL), 4f)
                .add(Ui.column(1, Ui.muted("Shift — быстро, 1–9 и F — в хотбар,"),
                        Ui.muted("Q — выбросить. Всё проверяет сервер.")), 2f)
                .title("Инвентарь")
                .add(main)
                .add(hotbar, 3f);
        // Ширина по сетке: 9 слотов, 8 зазоров по 2 и поля панели по 12.
        window.layoutParams().width = Dim.px(9 * SLOT + 8 * 2 + 24);

        // Мимо окна — выбросить, как в игре; на фон самого окна — просто отмена.
        Panel screen = Ui.centered(window);
        screen.fill();
        UiRoot root = new UiRoot(screen, ShowcaseTheme.modern(false));
        group.onDropOutside((from, stack) -> !inside(window, root.mouseX(), root.mouseY()));
        return root;
    }

    private static boolean inside(UiElement e, float x, float y) {
        float ex = e.screenX();
        float ey = e.screenY();
        return x >= ex && y >= ey && x < ex + e.width() && y < ey + e.height();
    }
}
