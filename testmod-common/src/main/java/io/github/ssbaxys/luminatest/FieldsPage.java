package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.markdown.Markdown;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.ColorField;
import io.github.ssbaxys.lumina.widget.ColorPicker;
import io.github.ssbaxys.lumina.widget.ContextMenu;
import io.github.ssbaxys.lumina.widget.Icon;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.NumberField;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.SliderField;
import io.github.ssbaxys.lumina.widget.TextField;

import java.util.Locale;

/**
 * Поля ввода: варианты оформления, поиск, префиксы и фильтры, числа с ползунками как в панели
 * настроек, цвета в hex и контекстное меню (правая кнопка по любому полю).
 */
final class FieldsPage {
    private static final String[][] ITEMS = {
            {"minecraft:diamond", "Алмаз"},
            {"minecraft:diamond_sword", Tr.r("Алмазный меч")},
            {"minecraft:golden_apple", Tr.r("Золотое яблоко")},
            {"minecraft:tnt", Tr.r("Динамит")},
            {"minecraft:flint_and_steel", Tr.r("Огниво")},
            {"minecraft:blaze_rod", Tr.r("Стержень ифрита")},
            {"minecraft:emerald", Tr.r("Изумруд")},
            {"minecraft:iron_ingot", Tr.r("Железный слиток")},
            {"minecraft:netherite_ingot", Tr.r("Незеритовый слиток")},
            {"minecraft:lapis_lazuli", Tr.r("Лазурит")},
    };

    private static final String MARKUP = """
            <state name="nick" value="Alex"/>
            <column gap="6">
              <row gap="8">
                <input bind="nick" placeholder="Ник" variant="filled" width="140"/>
                <label>Привет, {{nick}}!</label>
              </row>
              <row gap="8">
                <number bind="fov" value="70" min="30" max="110" decimals="0" prefix="FOV" width="90"/>
                <color-field bind="tint" value="#F0506E" width="110"/>
              </row>
            </column>""";

    private FieldsPage() {
    }

    static UiElement create() {
        return Ui.page(
                variants(),
                search(),
                extras(),
                settings(),
                colors(),
                contextMenu(),
                markup());
    }

    /** Капсулу в витрине переопределяет ShowcaseTheme (для кнопок): возвращаем полю отступы. */
    private static TextField pill(TextField f) {
        return f.variant("pill").style(b -> b.set(Styles.PADDING, Insets.of(5, 10)));
    }

    private static Panel variants() {
        Signal<String> nick = Signal.of("");
        TextField outlined = new TextField(nick).placeholder(Tr.r("Имя игрока")).width(Dim.px(150)).defaultValue("Steve");
        TextField filled = new TextField().variant("filled").placeholder(Tr.r("С заливкой")).width(Dim.px(150));
        TextField underline = new TextField().variant("underline").placeholder(Tr.r("Подчёркивание")).width(Dim.px(150));
        TextField capsule = pill(new TextField().placeholder(Tr.r("Капсула")).width(Dim.px(150)));
        return Ui.section(Tr.r("Варианты оформления"),
                Tr.r("Рамка плавно перетекает в акцентный цвет при фокусе; у подчёркивания линия растёт от центра."),
                Ui.row(12,
                        Ui.labeled(outlined, "field"),
                        Ui.labeled(filled, "field filled"),
                        Ui.labeled(underline, "field underline"),
                        Ui.labeled(capsule, "field pill")),
                Label.bound(nick.map(t -> t.isEmpty()
                        ? Tr.r("Значение обновляется на лету. Правая кнопка по полю — меню с отменой и сбросом.")
                        : Tr.r("Привет, ") + t + "!")).style(Ui.MUTED));
    }

    private static Panel search() {
        Signal<String> query = Signal.of("");
        TextField field = pill(new TextField(query)
                .icon(TextField.SEARCH)
                .clearButton(true)
                .placeholder(Tr.r("Поиск предметов..."))
                .width(Dim.px(240)));
        Panel results = Ui.column(2);
        Runnable refresh = () -> {
            results.clearChildren();
            String q = query.peek().trim().toLowerCase(Locale.ROOT);
            for (String[] item : ITEMS) {
                if (Tr.r(item[1]).toLowerCase(Locale.ROOT).contains(q)) {
                    results.add(Ui.row(6, Icon.item(item[0], 16), new Label(Tr.r(item[1]))));
                }
            }
            if (results.children().isEmpty()) {
                results.add(Ui.muted(Tr.r("Ничего не найдено")));
            }
        };
        query.subscribe(q -> refresh.run());
        refresh.run();
        return Ui.section(Tr.r("Поиск"), Tr.r("Значок, крестик очистки и фильтрация списка при каждом нажатии."),
                field, results);
    }

    private static Panel extras() {
        Signal<String> sent = Signal.of("—");
        TextField hex = new TextField(Signal.of("5B8CFF")).prefix("#").maxLength(6)
                .charFilter(c -> Character.digit(c, 16) >= 0).width(Dim.px(110));
        TextField size = new TextField(Signal.of("128")).suffix("px")
                .charFilter(Character::isDigit).maxLength(4).width(Dim.px(90));
        TextField password = new TextField().password(true).placeholder(Tr.r("Пароль")).width(Dim.px(120));
        TextField readOnly = new TextField(Signal.of(Tr.r("Только чтение"))).readOnly(true).width(Dim.px(120));
        TextField disabled = new TextField(Signal.of(Tr.r("Отключено"))).enabled(false).width(Dim.px(110));
        TextField message = new TextField(Signal.of("")).live(false).placeholder(Tr.r("Сообщение"))
                .onSubmit(text -> sent.set(text.isEmpty() ? "—" : text)).width(Dim.px(200));
        return Ui.section(Tr.r("Префиксы, фильтры и состояния"),
                Tr.r("Префикс и суффикс, допустимые символы, длина, пароль, только чтение. Ctrl+Z / Ctrl+Y — история."),
                Ui.row(12,
                        Ui.labeled(hex, Tr.r("# и hex, до 6")),
                        Ui.labeled(size, Tr.r("только цифры")),
                        Ui.labeled(password, "password"),
                        Ui.labeled(readOnly, "readOnly"),
                        Ui.labeled(disabled, "disabled")),
                Ui.row(8, message, Ui.muted(Tr.r("Enter — отправить, Esc — отменить ввод. Отправлено:")), Label.bound(sent)));
    }

    private static Panel settings() {
        Signal<Float> border = Signal.of(1.33f);
        Signal<Float> radius = Signal.of(8f);
        Signal<Float> opacity = Signal.of(100f);
        Signal<Float> blur = Signal.of(12f);
        Panel panel = Ui.column(6,
                new SliderField(Tr.r("Толщина рамки"), border, 0f, 4f).step(0.01f).decimals(2),
                new SliderField(Tr.r("Скругление"), radius, 0f, 24f).step(1f).decimals(0).suffix("px"),
                new SliderField(Tr.r("Непрозрачность"), opacity, 0f, 100f).step(1f).decimals(0).suffix("%"),
                new SliderField(Tr.r("Размытие тени"), blur, 0f, 30f).step(0.5f).decimals(1))
                .width(Dim.px(380)).style(Ui.WELL).style(b -> b.set(Styles.PADDING, Insets.all(10)));
        for (UiElement row : panel.children()) {
            ((SliderField) row).field().width(Dim.px(70));
        }

        Sketch preview = new Sketch(130, 110, (canvas, w, h, t) -> {
            float r = radius.peek();
            float a = opacity.peek() / 100f;
            Radii radii = Radii.all(r);
            canvas.drawShadow(25, 20, 80, 60, radii, new Shadow(0, 4, blur.peek(), 0, Color.withAlpha(0xFF000000, 0.6f * a)));
            canvas.fillRoundRect(25, 20, 80, 60, radii, Brush.vertical(Color.withAlpha(0xFF5B8CFF, a), Color.withAlpha(0xFF3D6DF2, a)));
            float bw = border.peek();
            if (bw > 0f) {
                canvas.strokeRoundRect(25, 20, 80, 60, radii, bw, Color.withAlpha(0xFFFFFFFF, 0.85f * a));
            }
        });

        Signal<Float> x = Signal.of(64f);
        Signal<Float> y = Signal.of(32f);
        Signal<Float> scale = Signal.of(1f);
        Panel transform = Ui.row(8,
                new NumberField(x, -999f, 999f).prefix("X").decimals(0).width(Dim.px(70)),
                new NumberField(y, -999f, 999f).prefix("Y").decimals(0).width(Dim.px(70)),
                new NumberField(scale, 0.1f, 10f).prefix("S").step(0.05f).decimals(2).defaultValue(1f).width(Dim.px(76)),
                Ui.muted(Tr.r("Потяните за букву: значение меняется перетаскиванием")));
        return Ui.section(Tr.r("Числа и ползунки"),
                Tr.r("Ползунок и поле на одном сигнале. Колесо и стрелки вверх/вниз в фокусе меняют на шаг, Shift — на 10 шагов. ")
                        + Tr.r("Неверный ввод вспыхивает красным."),
                Ui.row(16, panel, preview).align(Align.CENTER),
                transform);
    }

    private static Panel colors() {
        Signal<Integer> color = Signal.of(0xFF5B8CFF);
        ColorPicker picker = new ColorPicker(color).size(150, 90);
        ColorField hex = new ColorField(color).defaultValue(0xFF5B8CFF).width(Dim.px(120));
        ColorField opaque = new ColorField(Signal.of(0xFF34C38F)).alpha(false).variant("filled").width(Dim.px(120));
        ColorField translucent = new ColorField(Signal.of(0x80F0506E)).variant("underline").width(Dim.px(120));
        return Ui.section(Tr.r("Цвета"), Tr.r("Hex \"#RRGGBB\" или \"#AARRGGBB\" с образцом на шахматке; верный ввод применяется сразу."),
                Ui.row(16, picker, Ui.column(6,
                        Ui.row(8, hex, Ui.muted(Tr.r("общий сигнал с палитрой"))),
                        Ui.row(8, opaque, Ui.muted(Tr.r("без прозрачности"))),
                        Ui.row(8, translucent, Ui.muted(Tr.r("полупрозрачный"))))));
    }

    private static Panel contextMenu() {
        Signal<String> last = Signal.of("—");
        Panel area = Ui.centered(new Label(Tr.r("Щёлкните здесь правой кнопкой мыши")))
                .width(Dim.FILL).height(Dim.px(56)).style(Ui.WELL);
        area.on(PointerEvent.PRESS, e -> {
            if (e.button() == Keys.MOUSE_RIGHT && area.root() != null) {
                new ContextMenu()
                        .item(Tr.r("Новый мир"), "Ctrl+N", () -> last.set(Tr.r("Новый мир")))
                        .item(Tr.r("Открыть..."), "Ctrl+O", () -> last.set(Tr.r("Открыть")))
                        .separator()
                        .item(Tr.r("Сохранить"), "Ctrl+S", () -> last.set(Tr.r("Сохранить")))
                        .item(Tr.r("Сохранить как..."), "Ctrl+Shift+S", () -> last.set(Tr.r("Сохранить как")))
                        .separator()
                        .item(Tr.r("Печать"), "Ctrl+P", false, () -> last.set(Tr.r("Печать")))
                        .item(Tr.r("Выйти"), () -> last.set(Tr.r("Выйти")))
                        .show(area.root(), e.x(), e.y());
                e.consume();
            }
        });
        return Ui.section(Tr.r("Контекстное меню"),
                Tr.r("Всплывающий слой поверх экрана: стрелки и Enter, Escape или клик мимо закрывают меню."),
                area, Ui.row(6, Ui.muted(Tr.r("Последний пункт:")), Label.bound(last)));
    }

    private static Panel markup() {
        Markup.Built built = Markup.build(Tr.r(MARKUP), new Bindings());
        Markdown source = new Markdown("```html\n" + Tr.r(MARKUP) + "\n```").width(Dim.FILL);
        return Ui.section(Tr.r("В разметке"), Tr.r("Теги <input>, <number> и <color-field> с привязкой к состоянию."),
                built.root(), source);
    }
}
