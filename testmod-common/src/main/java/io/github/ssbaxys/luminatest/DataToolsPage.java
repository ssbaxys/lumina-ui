package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.data.DataView;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.markdown.Book;
import io.github.ssbaxys.lumina.markdown.Markdown;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Computed;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Checkbox;
import io.github.ssbaxys.lumina.widget.FieldBase;
import io.github.ssbaxys.lumina.widget.Form;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.NumberField;
import io.github.ssbaxys.lumina.widget.Paginator;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.RadioGroup;
import io.github.ssbaxys.lumina.widget.Table;
import io.github.ssbaxys.lumina.widget.TextField;
import io.github.ssbaxys.lumina.widget.Validator;
import io.github.ssbaxys.lumina.widget.VirtualList;
import io.github.ssbaxys.lumina.widget.Wizard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Инструменты для данных и форм: виртуальный список на 10 000 предметов с поиском, фильтрами и
 * страницами, форма регистрации с проверками, мастер настройки сервера, книга и подсветка кода.
 */
final class DataToolsPage {
    /** Предмет витрины: id для иконки, название, категория, редкость и количество. */
    record Item(int number, String id, String name, String category, int rarity, int count) {
        String rarityName() {
            return Tr.r(RARITY[rarity]);
        }
    }

    static final String[] RARITY = {"Обычный", "Необычный", "Редкий", "Эпический"};
    private static final int[] RARITY_COLOR = {0xFFB0B4BC, 0xFFFFE066, 0xFF55D6FF, 0xFFD27BFF};
    private static final String[][] BASE = {
            {"minecraft:diamond", "Алмаз", "Ресурсы"},
            {"minecraft:emerald", "Изумруд", "Ресурсы"},
            {"minecraft:gold_ingot", "Золотой слиток", "Ресурсы"},
            {"minecraft:iron_ingot", "Железный слиток", "Ресурсы"},
            {"minecraft:redstone", "Редстоун", "Ресурсы"},
            {"minecraft:lapis_lazuli", "Лазурит", "Ресурсы"},
            {"minecraft:netherite_ingot", "Незеритовый слиток", "Ресурсы"},
            {"minecraft:stone", "Камень", "Блоки"},
            {"minecraft:grass_block", "Блок травы", "Блоки"},
            {"minecraft:oak_planks", "Дубовые доски", "Блоки"},
            {"minecraft:glass", "Стекло", "Блоки"},
            {"minecraft:tnt", "Динамит", "Блоки"},
            {"minecraft:bookshelf", "Книжная полка", "Блоки"},
            {"minecraft:obsidian", "Обсидиан", "Блоки"},
            {"minecraft:diamond_sword", "Алмазный меч", "Инструменты"},
            {"minecraft:iron_pickaxe", "Железная кирка", "Инструменты"},
            {"minecraft:bow", "Лук", "Инструменты"},
            {"minecraft:fishing_rod", "Удочка", "Инструменты"},
            {"minecraft:shield", "Щит", "Инструменты"},
            {"minecraft:apple", "Яблоко", "Еда"},
            {"minecraft:bread", "Хлеб", "Еда"},
            {"minecraft:cooked_beef", "Стейк", "Еда"},
            {"minecraft:golden_carrot", "Золотая морковь", "Еда"},
            {"minecraft:cake", "Торт", "Еда"}};
    static final int ITEM_COUNT = 10_000;

    private DataToolsPage() {
    }

    static UiElement create() {
        return Ui.page(
                Ui.section(Tr.r("Список на 10 000 предметов"),
                        Tr.r("VirtualList рисует только видимые строки; поиск, фильтры и сортировка — DataView"), itemList()),
                Ui.section(Tr.r("Таблица со страницами"),
                        Tr.r("Тот же поиск, по 10 строк на страницу; клик по заголовку сортирует все страницы"), itemTable()),
                Ui.section(Tr.r("Регистрация"), Tr.r("Проверки полей, ошибки под полями, кнопка проверяет всю форму"),
                        registration()),
                Ui.section(Tr.r("Мастер настройки"), Tr.r("Шаги с проверкой: «Далее» не пустит дальше с ошибкой"), wizard()),
                Ui.section(Tr.r("Книга"), Tr.r("Страницы из Markdown, перелистывание стрелками, колесом и клавишами"), books()),
                Ui.section(Tr.r("Подсветка кода"), Tr.r("Блоки ```java, ```json, ```css и ```html раскрашиваются по теме"),
                        new Markdown(Tr.r(CODE))));
    }

    /** 10 000 предметов из базового набора: номер в названии, редкость и количество по номеру. */
    static List<Item> items() {
        List<Item> out = new ArrayList<>(ITEM_COUNT);
        for (int i = 0; i < ITEM_COUNT; i++) {
            String[] b = BASE[i % BASE.length];
            int rarity = (i * 7 + i / BASE.length) % 11 == 0 ? 3 : (i * 13) % 7 == 0 ? 2 : (i % 3 == 0 ? 1 : 0);
            out.add(new Item(i + 1, b[0], Tr.r(b[1]) + " №" + (i / BASE.length + 1), Tr.r(b[2]), rarity, 1 + (i * 37) % 64));
        }
        return out;
    }

    // ================================================================ список

    private static UiElement itemList() {
        DataView<Item> view = new DataView<>(items()).searchBy(i -> i.name() + " " + i.category());
        Signal<Boolean> onlyBlocks = Signal.of(false);
        Signal<Boolean> onlyRare = Signal.of(false);
        view.filterWhen(Tr.r("блоки"), onlyBlocks, i -> i.category().equals(Tr.r("Блоки")));
        view.filterWhen(Tr.r("редкие"), onlyRare, i -> i.rarity() >= 2);

        Signal<String> sort = Signal.of(Tr.r("По номеру"));
        sort.subscribe(s -> view.sort(switch (s) {
            case "По названию" -> Comparator.comparing(Item::name);
            case "По редкости" -> Comparator.comparingInt(Item::rarity).reversed().thenComparing(Item::number);
            default -> null;
        }));

        TextField search = new TextField(view.query()).placeholder(Tr.r("Поиск: «алмаз», «еда 12»…"))
                .icon(FieldBase.SEARCH).clearButton(true).width(Dim.px(190));
        Panel filters = Ui.row(10, search,
                new Checkbox(Tr.r("Только блоки"), onlyBlocks),
                new Checkbox(Tr.r("Редкие и выше"), onlyRare));
        RadioGroup<String> order = RadioGroup.of(sort, Tr.r("По номеру"), Tr.r("По названию"), Tr.r("По редкости")).horizontal();

        Label selected = new Label(Tr.r("Выберите предмет (двойной клик — «выдать»)"));
        VirtualList<Item> list = new VirtualList<Item>(18, DataToolsPage::paintRow)
                .bind(view.visible())
                .emptyText(Tr.r("Ничего не найдено"))
                .onActivate(i -> selected.text(Tr.r("Выдано: ") + i.name() + " ×" + i.count()))
                .width(Dim.FILL).height(Dim.px(180));
        list.selection().subscribe(i -> {
            if (i != null) {
                selected.text(Tr.r("Выбрано: ") + i.name() + " — " + i.category() + ", " + i.rarityName().toLowerCase());
            }
        });

        Label found = Label.bound(view.total().map(n -> Tr.r("Найдено: ") + n + Tr.r(" из ") + ITEM_COUNT));
        return Ui.column(6, filters, Ui.row(10, Ui.muted(Tr.r("Порядок:")), order, Ui.spacer(), found).width(Dim.FILL),
                list, selected).width(Dim.FILL);
    }

    /** Строка списка: иконка предмета, название, категория и цветная метка редкости. */
    private static void paintRow(io.github.ssbaxys.lumina.render.Canvas c, Item item, int index, float x, float y,
                                 float w, float h, VirtualList.RowState row) {
        TextMetrics m = c.textMetrics();
        float ty = y + (h - m.lineHeight()) / 2f + 0.5f;
        c.drawItem(item.id(), x + 3f, y + 1f, 16f);
        int text = row.textColor();
        c.drawText(item.name(), x + 24f, ty, text, false);
        String cat = item.category();
        float right = x + w - 6f;
        String count = "×" + item.count();
        c.drawText(count, right - m.width(count), ty, Color.mulAlpha(text, 0.6f), false);
        right -= m.width(count) + 10f;
        int rc = RARITY_COLOR[item.rarity()];
        c.fillCircle(right - 3f, y + h / 2f, 3f, Brush.solid(rc));
        right -= 10f;
        c.drawText(cat, right - m.width(cat), ty, Color.mulAlpha(text, 0.55f), false);
    }

    // ================================================================ таблица

    private static UiElement itemTable() {
        DataView<Item> view = new DataView<>(items()).searchBy(Item::name).pageSize(10);
        TextField search = new TextField(view.query()).placeholder(Tr.r("Поиск по названию")).icon(FieldBase.SEARCH)
                .clearButton(true).width(Dim.px(170));
        Table<Item> table = new Table<Item>()
                .column("№", 0.6f, i -> String.valueOf(i.number()), Comparator.comparingInt(Item::number))
                .column(Tr.r("Предмет"), 2.4f, Item::name)
                .column(Tr.r("Категория"), 1.4f, Item::category)
                .column(Tr.r("Редкость"), 1.2f, (c, i, x, y, w, h) -> {
                    c.fillCircle(x + 3f, y + h / 2f, 3f, Brush.solid(RARITY_COLOR[i.rarity()]));
                    c.drawText(i.rarityName(), x + 10f, y + 4f, Color.mulAlpha(RARITY_COLOR[i.rarity()], 0.95f), false);
                }, Comparator.comparingInt(Item::rarity))
                .column(Tr.r("Кол-во"), 0.8f, i -> String.valueOf(i.count()), Comparator.comparingInt(Item::count))
                .bind(view)
                .width(Dim.FILL);
        Label pages = Label.bound(new Computed<>(() -> Tr.r("Страница ") + (view.page().get() + 1) + Tr.r(" из ")
                + view.pageCount().get() + Tr.r(" · строк: ") + view.total().get()));
        return Ui.column(6, Ui.row(10, search, Ui.spacer(), pages).width(Dim.FILL), table,
                Ui.row(0, Ui.spacer(), new Paginator(view), Ui.spacer()).width(Dim.FILL)).width(Dim.FILL);
    }

    // ================================================================ форма

    private static UiElement registration() {
        Signal<String> nick = Signal.of("");
        Signal<String> mail = Signal.of("");
        Signal<String> password = Signal.of("");
        Signal<String> repeat = Signal.of("");
        Signal<Float> age = Signal.of(0f);
        Signal<Boolean> agree = Signal.of(false);
        Label result = Ui.muted(Tr.r("Заполните форму и нажмите «Создать аккаунт»"));

        Form form = new Form();
        TextField nickField = new TextField(nick).placeholder("Steve_2009").maxLength(16)
                .validate(Validator.required(Tr.r("Придумайте ник")), Validator.minLength(3),
                        Validator.pattern("[A-Za-z0-9_]+", Tr.r("Только латиница, цифры и _")));
        TextField mailField = new TextField(mail).placeholder("steve@mail.ru")
                .validate(Validator.required(), Validator.email());
        TextField passField = new TextField(password).password(true).placeholder(Tr.r("не меньше 6 символов"))
                .validate(Validator.required(), Validator.minLength(6),
                        Validator.of(s -> s.chars().anyMatch(Character::isDigit), Tr.r("Добавьте хотя бы одну цифру")));
        TextField repeatField = new TextField(repeat).password(true)
                .validate(Validator.required(Tr.r("Повторите пароль")), Validator.sameAs(password::peek, Tr.r("Пароли не совпадают")));
        NumberField ageField = new NumberField(age).decimals(0).range(0, 200)
                .validate(Validator.range(13, 120, Tr.r("Возраст от 13 до 120 лет"))).width(Dim.px(70));
        // Если пароль поменяли после повтора — повтор перепроверяется.
        password.subscribe(p -> {
            if (repeatField.error().get() != null) {
                repeatField.validate();
            }
        });

        Checkbox rules = new Checkbox(Tr.r("Согласен с правилами сервера"), agree);
        Label rulesError = new Label("").styleClass("form-error");
        form.check(() -> {
            boolean ok = Boolean.TRUE.equals(agree.peek());
            rulesError.text(ok ? "" : Tr.r("Нужно согласиться с правилами"));
            return ok;
        });
        agree.subscribe(v -> {
            if (v) {
                rulesError.text("");
            }
        });
        form.onSubmit(() -> result.text(Tr.r("Аккаунт «") + nick.peek() + Tr.r("» создан! Письмо отправлено на ") + mail.peek()))
                .onInvalid(() -> result.text(Tr.r("Исправьте ошибки: ") + form.errors().size()));

        Button submit = form.submitButton(new Button(Tr.r("Создать аккаунт")), false);
        Button reset = new Button(Tr.r("Сбросить"), () -> {
            nick.set("");
            mail.set("");
            password.set("");
            repeat.set("");
            age.set(0f);
            agree.set(false);
            form.reset();
            rulesError.text("");
            result.text(Tr.r("Форма очищена"));
        }).styleClass("button ghost");
        Label status = Label.bound(form.valid().map(ok -> ok ? Tr.r("✔ Все поля заполнены верно") : Tr.r("Есть незаполненные поля")));

        Panel left = Ui.column(8,
                form.field(Tr.r("Ник"), nickField).hint(Tr.r("3–16 символов")),
                form.field(Tr.r("Почта"), mailField),
                form.field(Tr.r("Возраст"), ageField));
        Panel right = Ui.column(8,
                form.field(Tr.r("Пароль"), passField),
                form.field(Tr.r("Повтор пароля"), repeatField));
        Panel columns = Ui.row(24, left, right);
        columns.align(Align.START);
        return Ui.column(8, columns, Ui.column(2, rules, rulesError), Ui.row(8, submit, reset, status), result);
    }

    // ================================================================ мастер

    private static UiElement wizard() {
        Signal<String> name = Signal.of("");
        Signal<String> mode = Signal.of(Tr.r("Выживание"));
        Signal<Float> players = Signal.of(10f);
        Label done = Ui.muted("");

        Form serverForm = new Form();
        Panel serverPage = Ui.column(8,
                Ui.muted(Tr.r("Как будет называться сервер?")),
                serverForm.field(Tr.r("Название"), new TextField(name).placeholder(Tr.r("Мой сервер"))
                        .validate(Validator.required(Tr.r("Введите название")), Validator.maxLength(24))));

        Form playersForm = new Form();
        Panel playersPage = Ui.column(8,
                Ui.muted(Tr.r("Сколько игроков смогут зайти одновременно?")),
                playersForm.field(Tr.r("Слотов"), new NumberField(players).decimals(0).range(0, 1000)
                        .validate(Validator.range(1, 100, Tr.r("От 1 до 100 игроков"))).width(Dim.px(70))));

        Wizard wizard = new Wizard()
                .step(Tr.r("Сервер"), serverPage, serverForm)
                .step(Tr.r("Режим"), () -> Ui.column(8, Ui.muted(Tr.r("Выберите режим игры")),
                        RadioGroup.of(mode, Tr.r("Выживание"), Tr.r("Творческий"), Tr.r("Хардкор"), Tr.r("Приключение"))))
                .step(Tr.r("Игроки"), playersPage, playersForm)
                .step(Tr.r("Готово"), () -> Ui.column(6,
                        Ui.heading(Tr.r("Проверьте настройки")),
                        Label.bound(name.map(n -> Tr.r("Название: ") + n)),
                        Label.bound(mode.map(m -> Tr.r("Режим: ") + m)),
                        Label.bound(players.map(p -> Tr.r("Игроков: ") + Math.round(p)))))
                .labels(Tr.r("Назад"), Tr.r("Далее"), Tr.r("Создать сервер"))
                .width(Dim.FILL).height(Dim.px(210));
        wizard.onFinish(() -> done.text(Tr.r("Сервер «") + name.peek() + Tr.r("» создан: ") + mode.peek().toLowerCase()
                + ", " + Math.round(players.peek()) + Tr.r(" слотов")));
        return Ui.column(6, wizard, done).width(Dim.FILL);
    }

    // ================================================================ книги

    static final String STORY = """
            # Дневник Стива
            День первый. Я очнулся на берегу и понял, что до заката нужно построить **укрытие**.

            Дерево рубится кулаком — медленно, но верно.
            <!-- page -->
            ## Первая ночь
            Зомби стучат в дверь, а я считаю *блоки угля*. Факелы — лучшие друзья шахтёра.

            > Не копай прямо вниз.
            <!-- page -->
            ## Шахта
            На глубине **Y = −58** нашлись алмазы! Ещё нужно:

            - железо для ведра;
            - обсидиан для портала;
            - ![](item:minecraft:ender_eye) око Края.
            <!-- page -->
            ## Незер
            Портал загудел, и мир окрасился в красный. Гасты плачут, а я ищу крепость.
            <!-- page -->
            ## Край
            Дракон повержен. Вернуться [к началу](#дневник-стива) или [в шахту](#шахта).
            """;

    private static UiElement books() {
        Book vanilla = new Book(Tr.r(STORY));
        Book modern = new Book(Tr.r(STORY)).look(Book.Look.MODERN).tableOfContents(true)
                .width(Dim.px(340)).height(Dim.px(210));
        Label counter = Label.bound(new Computed<>(() -> Tr.r("Современная книга: страница ")
                + (modern.page().get() + 1) + Tr.r(" из ") + modern.pageCount().get()));
        Panel controls = Ui.row(8,
                new Button(Tr.r("« Оглавление"), () -> modern.goTo(0)).styleClass("button ghost"),
                new Button("‹", modern::previous).styleClass("button ghost"),
                new Button("›", modern::next).styleClass("button ghost"),
                counter);
        return Ui.column(10,
                Ui.row(12, vanilla, Ui.column(4,
                        Ui.muted(Tr.r("Ванильный вид: текстура книги игры,")),
                        Ui.muted(Tr.r("одна страница и стрелки внизу.")),
                        Ui.muted(Tr.r("Разрыв страницы — <!-- page -->.")))),
                modern, controls);
    }

    // ================================================================ код

    private static final String CODE = """
            ```java
            // Список на десятки тысяч строк
            DataView<Item> view = new DataView<>(items).searchBy(Item::name).pageSize(50);
            new VirtualList<Item>(18, (c, item, i, x, y, w, h, row) -> c.drawText(item.name(), x + 4, y + 5, row.textColor(), false))
                    .bind(view.visible());
            ```

            ```json
            { "pack": { "pack_format": 15, "description": "Lumina" }, "replace": false }
            ```

            ```css
            .card:hover { background: #2a2f3a; transform: scale(1.04); transition: transform .2s out-back; }
            ```

            ```html
            <form on-submit="send"><field label="Ник"><input bind="nick" required minlength="3"/></field></form>
            ```
            """;
}
