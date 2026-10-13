package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.markdown.Markdown;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.CalendarView;
import io.github.ssbaxys.lumina.widget.DatePicker;
import io.github.ssbaxys.lumina.widget.Icon;
import io.github.ssbaxys.lumina.widget.KeyBindField;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.RadialMenu;
import io.github.ssbaxys.lumina.widget.Select;
import io.github.ssbaxys.lumina.widget.Tooltip;
import io.github.ssbaxys.lumina.widget.Tooltips;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Всплывающие виджеты: выпадающие списки с поиском и иконками, подсказки в стиле игры и
 * современные, радиальное меню (правая кнопка или зажатая клавиша), назначение клавиш и выбор даты.
 */
final class PopupsPage {
    private static final Map<String, String> ITEMS = new LinkedHashMap<>();

    static {
        ITEMS.put("minecraft:diamond", "Алмаз");
        ITEMS.put("minecraft:emerald", "Изумруд");
        ITEMS.put("minecraft:gold_ingot", "Золотой слиток");
        ITEMS.put("minecraft:iron_ingot", "Железный слиток");
        ITEMS.put("minecraft:netherite_ingot", "Незеритовый слиток");
        ITEMS.put("minecraft:copper_ingot", "Медный слиток");
        ITEMS.put("minecraft:lapis_lazuli", "Лазурит");
        ITEMS.put("minecraft:redstone", "Редстоун");
        ITEMS.put("minecraft:coal", "Уголь");
        ITEMS.put("minecraft:quartz", "Кварц");
        ITEMS.put("minecraft:amethyst_shard", "Осколок аметиста");
        ITEMS.put("minecraft:blaze_rod", "Стержень ифрита");
        ITEMS.put("minecraft:ender_pearl", "Жемчуг Края");
        ITEMS.put("minecraft:slime_ball", "Сгусток слизи");
        ITEMS.put("minecraft:golden_apple", "Золотое яблоко");
        ITEMS.put("minecraft:apple", "Яблоко");
        ITEMS.put("minecraft:bread", "Хлеб");
        ITEMS.put("minecraft:cookie", "Печенье");
        ITEMS.put("minecraft:tnt", "Динамит");
        ITEMS.put("minecraft:compass", "Компас");
    }

    private static final String MARKUP = """
            <state name="mode" value="normal"/>
            <state name="drop" value="Q"/>
            <state name="day" value="2026-10-12"/>
            <column gap="6">
              <row gap="8">
                <select bind="mode" options="peaceful,easy,normal,hard"
                        labels="Мирный,Лёгкий,Нормальный,Сложный" width="130"
                        tooltip="Сложность мира\\nМеняет урон мобов"/>
                <label>Режим: {{mode}}</label>
              </row>
              <row gap="8">
                <keybind bind="drop" default="Q" width="110"/>
                <date-picker bind="day" min="2026-01-01" width="110"/>
                <label tooltip="Дата: {{day}}" tooltip-kind="modern">Наведите сюда</label>
              </row>
            </column>""";

    private PopupsPage() {
    }

    static UiElement create() {
        return Ui.page(
                selects(),
                tooltips(),
                radial(),
                keybinds(),
                dates(),
                markup());
    }

    // ================================================================ выпадающие списки

    private static Panel selects() {
        Signal<String> difficulty = Signal.of(Tr.r("Нормальный"));
        Select<String> simple = Select.of(difficulty, Tr.r("Мирный"), Tr.r("Лёгкий"), Tr.r("Нормальный"), Tr.r("Сложный"), Tr.r("Хардкор"))
                .width(Dim.px(130));

        Signal<String> item = Signal.of("minecraft:diamond");
        Select<String> items = new Select<>(item, new ArrayList<>(ITEMS.keySet()))
                .labels(id -> Tr.r(ITEMS.get(id)))
                .icons(id -> id)
                .searchable(true)
                .maxRows(7)
                .width(Dim.px(190));

        Signal<String> biome = Signal.of(null);
        Select<String> empty = Select.of(biome, Tr.r("Равнины"), Tr.r("Лес"), Tr.r("Пустыня"), Tr.r("Тайга"), Tr.r("Джунгли"), Tr.r("Саванна"), Tr.r("Болото"))
                .placeholder(Tr.r("Выберите биом"))
                .width(Dim.px(140));

        Label summary = Label.bound(difficulty.map(d -> Tr.r("Сложность: ") + d));
        Label chosen = Label.bound(item.map(id -> Tr.r("Предмет: ") + (id == null ? "—" : Tr.r(ITEMS.get(id)))));
        Label picked = Label.bound(biome.map(b -> Tr.r("Биом: ") + (b == null ? Tr.r("не выбран") : b)));
        return Ui.section(Tr.r("Выпадающие списки"),
                Tr.r("Список открывается под полем (у нижнего края — над ним), стрелки и Enter, буква прыгает к варианту, ")
                        + Tr.r("колесо прокручивает длинный список, поиск отбирает варианты по подстроке."),
                Ui.row(12,
                        Ui.labeled(simple, Tr.r("простой")),
                        Ui.labeled(items, Tr.r("иконки и поиск")),
                        Ui.labeled(empty, Tr.r("с подсказкой"))),
                Ui.row(16, summary.style(Ui.MUTED), chosen.style(Ui.MUTED), picked.style(Ui.MUTED)));
    }

    // ================================================================ подсказки

    private static Panel tooltips() {
        Button vanilla = new Button(Tr.r("Алмазный меч")).item("minecraft:diamond_sword")
                .tooltip(Tr.r("Алмазный меч\nПри ударе:\n 7 ед. урона\n 1,6 скорости атаки"));
        Button modern = new Button(Tr.r("Современная")).styleClass("button outline")
                .tooltip(Tooltip.Kind.MODERN, Tr.r("Карточка темы со скруглением,\nрамкой и мягким появлением"));
        Button markdown = new Button("Markdown").styleClass("button ghost")
                .tooltipMarkdown(Tr.r("**Жемчуг Края** ![](item:minecraft:ender_pearl)\n\n")
                        + Tr.r("- телепортирует при броске\n- стакается до *16*"));
        Tooltips.of(markdown).kind(Tooltip.Kind.MODERN).maxWidth(170);

        Icon slot = Icon.item("minecraft:golden_apple", 24);
        Tooltips.attach(slot, PopupsPage::appleCard).kind(Tooltip.Kind.MODERN).follow(false).delay(0.2f);

        Signal<Integer> clicks = Signal.of(0);
        Button counter = new Button(Tr.r("Счётчик"), () -> clicks.update(c -> c + 1));
        Tooltip live = Tooltips.attach(counter, Tr.r("Нажатий: 0"));
        clicks.subscribe(c -> live.text(Tr.r("Нажатий: ") + c));

        return Ui.section(Tr.r("Подсказки"),
                Tr.r("Появляются после задержки наведения, следуют за мышью и не выходят за экран. Нажатие прячет подсказку."),
                Ui.row(10,
                        Ui.labeled(vanilla, Tr.r("как в игре")),
                        Ui.labeled(modern, Tr.r("современная")),
                        Ui.labeled(markdown, "Markdown"),
                        Ui.labeled(slot, Tr.r("свой элемент")),
                        Ui.labeled(counter, Tr.r("живой текст"))));
    }

    /** Карточка предмета для подсказки: создаётся заново при каждом показе. */
    private static UiElement appleCard() {
        return Ui.column(4,
                Ui.row(6, Icon.item("minecraft:golden_apple", 16), new Label(Tr.r("Золотое яблоко")).scale(1.1f)),
                Ui.muted(Tr.r("Поглощение II (2:00)")),
                Ui.muted(Tr.r("Регенерация II (0:05)")),
                new Label(Tr.r("Редкость: необычная")).style(b -> b.set(Styles.TEXT_COLOR, 0xFFFFFF55)));
    }

    // ================================================================ радиальное меню

    private static Panel radial() {
        Signal<String> last = Signal.of("—");
        Panel pad = Ui.centered(Ui.column(4,
                        new Label(Tr.r("Щёлкните сюда, затем зажмите R")).alignSelf(Align.CENTER),
                        Ui.muted(Tr.r("или нажмите правую кнопку мыши")).alignSelf(Align.CENTER)))
                .width(Dim.FILL).height(Dim.px(70)).style(Ui.WELL)
                .with(p -> p.setFocusable(true))
                .style(b -> b.set(Styles.BORDER_WIDTH, 1f).set(Styles.BORDER_COLOR, 0x30808080));
        RadialMenu.bindKey(pad, Keys.R, () -> wheel(last));
        RadialMenu.bindRightClick(pad, () -> wheel(last));
        return Ui.section(Tr.r("Радиальное меню"),
                Tr.r("Сектор выбирается направлением мыши; отпустите R — пункт сработает. Стрелки и цифры тоже работают, ")
                        + Tr.r("правая кнопка или клик в центре — назад из вложенного меню."),
                pad, Ui.row(6, Ui.muted(Tr.r("Последнее действие:")), Label.bound(last)));
    }

    private static RadialMenu wheel(Signal<String> last) {
        return new RadialMenu()
                .title(Tr.r("Действия"))
                .item(Tr.r("Меч"), "minecraft:diamond_sword", () -> last.set(Tr.r("Меч")))
                .item(Tr.r("Кирка"), "minecraft:diamond_pickaxe", () -> last.set(Tr.r("Кирка")))
                .item(Tr.r("Лук"), "minecraft:bow", () -> last.set(Tr.r("Лук")))
                .submenu(Tr.r("Еда"), "minecraft:cooked_beef",
                        RadialMenu.Item.of(Tr.r("Стейк"), () -> last.set(Tr.r("Стейк"))).icon("minecraft:cooked_beef"),
                        RadialMenu.Item.of(Tr.r("Хлеб"), () -> last.set(Tr.r("Хлеб"))).icon("minecraft:bread"),
                        RadialMenu.Item.of(Tr.r("Яблоко"), () -> last.set(Tr.r("Яблоко"))).icon("minecraft:apple"),
                        RadialMenu.Item.of(Tr.r("Торт"), () -> last.set(Tr.r("Торт"))).icon("minecraft:cake").enabled(false))
                .item(Tr.r("Факел"), "minecraft:torch", () -> last.set(Tr.r("Факел")))
                .item(Tr.r("Щит"), "minecraft:shield", () -> last.set(Tr.r("Щит")))
                .submenu(Tr.r("Блоки"), "minecraft:grass_block",
                        RadialMenu.Item.of(Tr.r("Камень"), () -> last.set(Tr.r("Камень"))).icon("minecraft:stone"),
                        RadialMenu.Item.of(Tr.r("Доски"), () -> last.set(Tr.r("Доски"))).icon("minecraft:oak_planks"),
                        RadialMenu.Item.of(Tr.r("Стекло"), () -> last.set(Tr.r("Стекло"))).icon("minecraft:glass"))
                .item(Tr.r("Компас"), "minecraft:compass", () -> last.set(Tr.r("Компас")));
    }

    // ================================================================ клавиши

    private static Panel keybinds() {
        String[][] actions = {
                {Tr.r("Прыжок"), "32"}, {Tr.r("Инвентарь"), String.valueOf(Keys.E)}, {Tr.r("Выбросить"), String.valueOf(Keys.Q)},
                {Tr.r("Красться"), "340"}, {Tr.r("Чат"), String.valueOf(Keys.T)}, {Tr.r("Скриншот"), "291"},
        };
        List<UiElement> rows = new ArrayList<>();
        for (String[] a : actions) {
            int def = Integer.parseInt(a[1]);
            Signal<Integer> key = Signal.of(def);
            KeyBindField field = new KeyBindField(key).defaultKey(def).width(Dim.px(120));
            rows.add(Ui.row(8, new Label(a[0]).width(Dim.px(80)), field));
        }
        Panel grid = Ui.grid(2, 8, rows.toArray(new UiElement[0]));
        return Ui.section(Tr.r("Назначение клавиш"),
                Tr.r("Щелчок — поле ждёт клавишу; Escape отменяет, правая кнопка или Delete — вернуть по умолчанию."),
                grid);
    }

    // ================================================================ даты

    private static Panel dates() {
        LocalDate today = LocalDate.now();
        Signal<LocalDate> date = Signal.of(today);
        DatePicker picker = new DatePicker(date).width(Dim.px(120));
        Signal<LocalDate> trip = Signal.of(today.plusDays(7));
        DatePicker future = new DatePicker(trip).range(today, today.plusMonths(3)).width(Dim.px(120))
                .tooltip(Tr.r("Только ближайшие три месяца"));
        CalendarView inline = new CalendarView(date).style(Ui.WELL).style(b -> b.set(Styles.PADDING, Insets.all(6)));
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMMM yyyy", java.util.Locale.forLanguageTag(Tr.russian() ? "ru" : "en"));
        Label info = Label.bound(date.map(d -> d == null ? "—"
                : fmt.format(d) + Tr.r(" — через ") + ChronoUnit.DAYS.between(today, d) + Tr.r(" дн.")));
        return Ui.section(Tr.r("Дата"),
                Tr.r("Календарь во всплывающем окне: стрелки — дни, PageUp/PageDown и колесо — месяцы, Enter — выбрать."),
                Ui.row(16,
                        Ui.column(8,
                                Ui.labeled(picker, Tr.r("общий сигнал с календарём")),
                                Ui.labeled(future, Tr.r("с ограничением")),
                                info.style(Ui.MUTED)),
                        inline).align(Align.START));
    }

    private static Panel markup() {
        Markup.Built built = Markup.build(Tr.r(MARKUP), new Bindings());
        Markdown source = new Markdown("```html\n" + Tr.r(MARKUP) + "\n```").width(Dim.FILL);
        return Ui.section(Tr.r("В разметке"), Tr.r("Теги <select>, <keybind>, <date-picker> и атрибут tooltip."),
                built.root(), source);
    }
}
