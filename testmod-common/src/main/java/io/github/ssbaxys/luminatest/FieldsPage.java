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
            {"minecraft:diamond_sword", "Алмазный меч"},
            {"minecraft:golden_apple", "Золотое яблоко"},
            {"minecraft:tnt", "Динамит"},
            {"minecraft:flint_and_steel", "Огниво"},
            {"minecraft:blaze_rod", "Стержень ифрита"},
            {"minecraft:emerald", "Изумруд"},
            {"minecraft:iron_ingot", "Железный слиток"},
            {"minecraft:netherite_ingot", "Незеритовый слиток"},
            {"minecraft:lapis_lazuli", "Лазурит"},
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
        TextField outlined = new TextField(nick).placeholder("Имя игрока").width(Dim.px(150)).defaultValue("Steve");
        TextField filled = new TextField().variant("filled").placeholder("С заливкой").width(Dim.px(150));
        TextField underline = new TextField().variant("underline").placeholder("Подчёркивание").width(Dim.px(150));
        TextField capsule = pill(new TextField().placeholder("Капсула").width(Dim.px(150)));
        return Ui.section("Варианты оформления",
                "Рамка плавно перетекает в акцентный цвет при фокусе; у подчёркивания линия растёт от центра.",
                Ui.row(12,
                        Ui.labeled(outlined, "field"),
                        Ui.labeled(filled, "field filled"),
                        Ui.labeled(underline, "field underline"),
                        Ui.labeled(capsule, "field pill")),
                Label.bound(nick.map(t -> t.isEmpty()
                        ? "Значение обновляется на лету. Правая кнопка по полю — меню с отменой и сбросом."
                        : "Привет, " + t + "!")).style(Ui.MUTED));
    }

    private static Panel search() {
        Signal<String> query = Signal.of("");
        TextField field = pill(new TextField(query)
                .icon(TextField.SEARCH)
                .clearButton(true)
                .placeholder("Поиск предметов...")
                .width(Dim.px(240)));
        Panel results = Ui.column(2);
        Runnable refresh = () -> {
            results.clearChildren();
            String q = query.peek().trim().toLowerCase(Locale.ROOT);
            for (String[] item : ITEMS) {
                if (item[1].toLowerCase(Locale.ROOT).contains(q)) {
                    results.add(Ui.row(6, Icon.item(item[0], 16), new Label(item[1])));
                }
            }
            if (results.children().isEmpty()) {
                results.add(Ui.muted("Ничего не найдено"));
            }
        };
        query.subscribe(q -> refresh.run());
        refresh.run();
        return Ui.section("Поиск", "Значок, крестик очистки и фильтрация списка при каждом нажатии.",
                field, results);
    }

    private static Panel extras() {
        Signal<String> sent = Signal.of("—");
        TextField hex = new TextField(Signal.of("5B8CFF")).prefix("#").maxLength(6)
                .charFilter(c -> Character.digit(c, 16) >= 0).width(Dim.px(110));
        TextField size = new TextField(Signal.of("128")).suffix("px")
                .charFilter(Character::isDigit).maxLength(4).width(Dim.px(90));
        TextField password = new TextField().password(true).placeholder("Пароль").width(Dim.px(120));
        TextField readOnly = new TextField(Signal.of("Только чтение")).readOnly(true).width(Dim.px(120));
        TextField disabled = new TextField(Signal.of("Отключено")).enabled(false).width(Dim.px(110));
        TextField message = new TextField(Signal.of("")).live(false).placeholder("Сообщение")
                .onSubmit(text -> sent.set(text.isEmpty() ? "—" : text)).width(Dim.px(200));
        return Ui.section("Префиксы, фильтры и состояния",
                "Префикс и суффикс, допустимые символы, длина, пароль, только чтение. Ctrl+Z / Ctrl+Y — история.",
                Ui.row(12,
                        Ui.labeled(hex, "# и hex, до 6"),
                        Ui.labeled(size, "только цифры"),
                        Ui.labeled(password, "password"),
                        Ui.labeled(readOnly, "readOnly"),
                        Ui.labeled(disabled, "disabled")),
                Ui.row(8, message, Ui.muted("Enter — отправить, Esc — отменить ввод. Отправлено:"), Label.bound(sent)));
    }

    private static Panel settings() {
        Signal<Float> border = Signal.of(1.33f);
        Signal<Float> radius = Signal.of(8f);
        Signal<Float> opacity = Signal.of(100f);
        Signal<Float> blur = Signal.of(12f);
        Panel panel = Ui.column(6,
                new SliderField("Толщина рамки", border, 0f, 4f).step(0.01f).decimals(2),
                new SliderField("Скругление", radius, 0f, 24f).step(1f).decimals(0).suffix("px"),
                new SliderField("Непрозрачность", opacity, 0f, 100f).step(1f).decimals(0).suffix("%"),
                new SliderField("Размытие тени", blur, 0f, 30f).step(0.5f).decimals(1))
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
                Ui.muted("Потяните за букву: значение меняется перетаскиванием"));
        return Ui.section("Числа и ползунки",
                "Ползунок и поле на одном сигнале. Колесо и стрелки вверх/вниз в фокусе меняют на шаг, Shift — на 10 шагов. "
                        + "Неверный ввод вспыхивает красным.",
                Ui.row(16, panel, preview).align(Align.CENTER),
                transform);
    }

    private static Panel colors() {
        Signal<Integer> color = Signal.of(0xFF5B8CFF);
        ColorPicker picker = new ColorPicker(color).size(150, 90);
        ColorField hex = new ColorField(color).defaultValue(0xFF5B8CFF).width(Dim.px(120));
        ColorField opaque = new ColorField(Signal.of(0xFF34C38F)).alpha(false).variant("filled").width(Dim.px(120));
        ColorField translucent = new ColorField(Signal.of(0x80F0506E)).variant("underline").width(Dim.px(120));
        return Ui.section("Цвета", "Hex \"#RRGGBB\" или \"#AARRGGBB\" с образцом на шахматке; верный ввод применяется сразу.",
                Ui.row(16, picker, Ui.column(6,
                        Ui.row(8, hex, Ui.muted("общий сигнал с палитрой")),
                        Ui.row(8, opaque, Ui.muted("без прозрачности")),
                        Ui.row(8, translucent, Ui.muted("полупрозрачный")))));
    }

    private static Panel contextMenu() {
        Signal<String> last = Signal.of("—");
        Panel area = Ui.centered(new Label("Щёлкните здесь правой кнопкой мыши"))
                .width(Dim.FILL).height(Dim.px(56)).style(Ui.WELL);
        area.on(PointerEvent.PRESS, e -> {
            if (e.button() == Keys.MOUSE_RIGHT && area.root() != null) {
                new ContextMenu()
                        .item("Новый мир", "Ctrl+N", () -> last.set("Новый мир"))
                        .item("Открыть...", "Ctrl+O", () -> last.set("Открыть"))
                        .separator()
                        .item("Сохранить", "Ctrl+S", () -> last.set("Сохранить"))
                        .item("Сохранить как...", "Ctrl+Shift+S", () -> last.set("Сохранить как"))
                        .separator()
                        .item("Печать", "Ctrl+P", false, () -> last.set("Печать"))
                        .item("Выйти", () -> last.set("Выйти"))
                        .show(area.root(), e.x(), e.y());
                e.consume();
            }
        });
        return Ui.section("Контекстное меню",
                "Всплывающий слой поверх экрана: стрелки и Enter, Escape или клик мимо закрывают меню.",
                area, Ui.row(6, Ui.muted("Последний пункт:"), Label.bound(last)));
    }

    private static Panel markup() {
        Markup.Built built = Markup.build(MARKUP, new Bindings());
        Markdown source = new Markdown("```html\n" + MARKUP + "\n```").width(Dim.FILL);
        return Ui.section("В разметке", "Теги <input>, <number> и <color-field> с привязкой к состоянию.",
                built.root(), source);
    }
}
