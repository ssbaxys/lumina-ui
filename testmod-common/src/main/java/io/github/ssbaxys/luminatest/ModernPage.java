package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markdown.Markdown;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.ModernTheme;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.widget.Backdrop;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Checkbox;
import io.github.ssbaxys.lumina.widget.Collapsible;
import io.github.ssbaxys.lumina.widget.ColorField;
import io.github.ssbaxys.lumina.widget.ContextMenu;
import io.github.ssbaxys.lumina.widget.FancyText;
import io.github.ssbaxys.lumina.widget.FieldBase;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.NumberField;
import io.github.ssbaxys.lumina.widget.Pages;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.RadioGroup;
import io.github.ssbaxys.lumina.widget.ScrollPanel;
import io.github.ssbaxys.lumina.widget.SideNav;
import io.github.ssbaxys.lumina.widget.SliderField;
import io.github.ssbaxys.lumina.widget.Tabs;
import io.github.ssbaxys.lumina.widget.TextField;
import io.github.ssbaxys.lumina.widget.Toggle;

/**
 * Экран настроек в современном стиле (как в ModernUI): боковое меню с иконками, вкладки сверху,
 * сворачиваемые карточки, переключатели, ползунки с полем числа, пресеты-«пилюли», hex-цвета
 * с градиентом и контекстное меню. На время показа страницы вся витрина переходит на {@link ModernTheme}.
 */
final class ModernPage {
    private static final float RADIUS = 9f;
    private static final float WIDTH = 1f;
    private static final int TOP = 0xFFE9C1B7;
    private static final int BOTTOM = 0xFF8A6CFF;

    private ModernPage() {
    }

    static UiElement create() {
        Signal<Integer> section = Signal.of(1);
        SideNav nav = new SideNav(section)
                .item(Tr.r("Главная"), "minecraft:oak_door")
                .item(Tr.r("Настройки"), "minecraft:comparator")
                .item(Tr.r("Дополнительно"), "minecraft:command_block")
                .item(Tr.r("Музыка"), "minecraft:jukebox")
                .item(Tr.r("Разработка"), "minecraft:redstone")
                .item("Markdown", "minecraft:writable_book");
        nav.width(Dim.px(132));
        Pages pages = new Pages(section).effect(Pages.Effect.SLIDE_UP)
                .page(ModernPage::home)
                .page(ModernPage::settings)
                .page(ModernPage::advanced)
                .page(ModernPage::music)
                .page(ModernPage::dev)
                .page(ModernPage::markdown);
        pages.grow(1).height(Dim.FILL);

        // Пока страница открыта, вся витрина в современной теме; при уходе тема возвращается.
        Panel screen = new Panel(null, FlexLayout.row().gap(10).align(Align.STRETCH)) {
            private UiRoot attachedTo;
            private Theme previous;

            @Override
            protected void onAttach() {
                attachedTo = root();
                previous = attachedTo.theme();
                attachedTo.setTheme(ShowcaseTheme.modern(false));
            }

            @Override
            protected void onDetach() {
                if (attachedTo != null && previous != null) {
                    attachedTo.setTheme(previous);
                }
                attachedTo = null;
            }
        };
        screen.add(nav, pages).fill();
        return screen;
    }

    // ---------------------------------------------------------------- страницы бокового меню

    private static UiElement home() {
        return scroll(
                new FancyText(Tr.r("Добро пожаловать")).scale(2f).gradient(TOP, BOTTOM).flow(0.3f),
                Ui.muted(Tr.r("Это экран настроек, собранный из виджетов Lumina в современной теме.")),
                Ui.row(8,
                        card("minecraft:comparator", Tr.r("Настройки"), Tr.r("Рамки, цвета, анимации")),
                        card("minecraft:jukebox", Tr.r("Музыка"), Tr.r("Громкость по категориям")),
                        card("minecraft:redstone", Tr.r("Разработка"), Tr.r("Инспектор и отладка"))),
                Ui.row(6,
                        new Button(Tr.r("Начать")).styleClass("button primary"),
                        new Button(Tr.r("Документация")).item("minecraft:book").styleClass("button outline"),
                        new Button("GitHub").styleClass("button ghost")));
    }

    private static UiElement settings() {
        Signal<Integer> tab = Signal.of(1);
        Tabs tabs = new Tabs(tab, Tr.r("Экран"), Tr.r("Вид"), Tr.r("Шрифт"), Tr.r("Расширения"), Tr.r("Текст")).stretch(true);
        tabs.width(Dim.FILL);
        Pages inner = new Pages(tab).effect(Pages.Effect.SLIDE)
                .page(ModernPage::screenTab)
                .page(ModernPage::viewTab)
                .page(ModernPage::fontTab)
                .page(ModernPage::extensionsTab)
                .page(ModernPage::textTab);
        inner.width(Dim.FILL).grow(1);
        Panel column = Panel.box(FlexLayout.column().gap(6)).add(tabs, inner);
        column.fill();
        return column;
    }

    private static UiElement advanced() {
        Signal<String> quality = Signal.of(Tr.r("Высокое"));
        return scroll(
                new Collapsible(Tr.r("Графика"),
                        row(Tr.r("Качество"), RadioGroup.of(quality, Tr.r("Низкое"), Tr.r("Среднее"), Tr.r("Высокое"), Tr.r("Ультра"))
                                .horizontal().gap(10)),
                        toggleRow(Tr.r("Плавное освещение"), true),
                        toggleRow(Tr.r("Облака"), false),
                        row(Tr.r("Дальность прорисовки"), new SliderField(Signal.of(12f), 2, 32).step(1).decimals(0)
                                .width(Dim.px(190)))),
                new Collapsible(Tr.r("Флажки"),
                        Ui.row(10,
                                new Checkbox(Tr.r("Галочка"), Signal.of(true)).kind(Checkbox.Kind.CHECK),
                                new Checkbox(Tr.r("Крестик"), Signal.of(true)).kind(Checkbox.Kind.CROSS),
                                new Checkbox(Tr.r("Заливка"), Signal.of(false)).kind(Checkbox.Kind.FILL)),
                        Ui.row(10,
                                new Checkbox(Tr.r("Сердце"), Signal.of(true)).kind(Checkbox.Kind.HEART),
                                new Checkbox(Tr.r("Звезда"), Signal.of(false)).kind(Checkbox.Kind.STAR),
                                new Checkbox(Tr.r("Точка"), Signal.of(true)).kind(Checkbox.Kind.SWITCH_DOT))));
    }

    private static UiElement music() {
        Panel visual = Panel.box(new io.github.ssbaxys.lumina.layout.StackLayout(Align.START, Align.START))
                .add(new Backdrop(Backdrop.Kind.WAVES).colors(0xFFB46CFF, 0xFFE9C1B7, 0xFF8A6CFF).base(0xFF191716));
        visual.width(Dim.FILL).height(Dim.px(70));
        visual.setClipChildren(true);
        visual.style(b -> b.set(Styles.RADIUS, Radii.all(RADIUS)));
        return scroll(visual,
                new Collapsible(Tr.r("Громкость"),
                        slider(Tr.r("Общая"), 100), slider(Tr.r("Музыка"), 60), slider(Tr.r("Погода"), 80),
                        slider(Tr.r("Блоки"), 100), slider(Tr.r("Враждебные"), 90), slider(Tr.r("Голос"), 100)));
    }

    private static UiElement dev() {
        TextField command = new TextField(Signal.of("")).placeholder(Tr.r("/команда")).icon(FieldBase.SEARCH).clearButton(true);
        command.width(Dim.FILL);
        return scroll(
                new Collapsible(Tr.r("Отладка"),
                        toggleRow(Tr.r("Показывать границы элементов"), false),
                        toggleRow(Tr.r("Счётчик кадров"), true),
                        toggleRow(Tr.r("Логировать события"), false),
                        row(Tr.r("Масштаб интерфейса"), new NumberField(Signal.of(1f), 0.5f, 3f).step(0.25f).decimals(2)
                                .defaultValue(1f).width(Dim.px(70)))),
                new Collapsible(Tr.r("Консоль"), command));
    }

    private static UiElement markdown() {
        return scroll(new Markdown(Tr.r("""
                # Markdown
                Текст **жирный**, *курсив*, `код` и ![](item:minecraft:diamond) иконки.

                - [x] Современная тема
                - [x] Вкладки и боковое меню
                - [ ] Ваш экран настроек
                """)));
    }

    // ---------------------------------------------------------------- вкладки «Настроек»

    private static UiElement screenTab() {
        return scroll(new Collapsible(Tr.r("Окно"),
                toggleRow(Tr.r("Полноэкранный режим"), false),
                toggleRow(Tr.r("Вертикальная синхронизация"), true),
                row(Tr.r("Ограничение FPS"), new NumberField(Signal.of(120f), 10, 260).step(10).decimals(0)
                        .defaultValue(120f).width(Dim.px(70)))));
    }

    /** Главная вкладка — повтор скриншота: «Стиль рамки» с переключателями, ползунками, пресетами и цветами. */
    private static UiElement viewTab() {
        Signal<Boolean> rounded = Signal.of(true);
        Signal<Boolean> shadow = Signal.of(true);
        Signal<Boolean> gradient = Signal.of(true);
        Signal<Float> radius = Signal.of(RADIUS);
        Signal<Float> width = Signal.of(WIDTH);
        Signal<Integer> top = Signal.of(TOP);
        Signal<Integer> bottom = Signal.of(BOTTOM);
        Signal<Integer> accent = Signal.of(ModernTheme.DEFAULT_ACCENT);
        Signal<Float> motion = Signal.of(1f);

        Panel preview = Ui.centered(Panel.box(FlexLayout.column().gap(3).align(Align.CENTER))
                .add(new Label(Tr.r("Предпросмотр")), Ui.muted(Tr.r("ПКМ — меню"))));
        preview.size(150, 70);
        Runnable restyle = () -> preview.style(b -> {
            b.set(Styles.BACKGROUND, Brush.solid(0x22FFFFFF))
                    .set(Styles.RADIUS, Radii.all(rounded.peek() ? radius.peek() : 0f))
                    .set(Styles.BORDER_WIDTH, width.peek())
                    .set(Styles.SHADOW, shadow.peek() ? new Shadow(0, 6, 18, 0, 0x80000000) : Shadow.NONE);
            if (gradient.peek()) {
                b.set(Styles.BORDER_BRUSH, Brush.vertical(top.peek(), bottom.peek()));
            } else {
                b.set(Styles.BORDER_COLOR, top.peek());
            }
        });
        rounded.subscribe(v -> restyle.run());
        shadow.subscribe(v -> restyle.run());
        gradient.subscribe(v -> restyle.run());
        radius.subscribe(v -> restyle.run());
        width.subscribe(v -> restyle.run());
        top.subscribe(v -> restyle.run());
        bottom.subscribe(v -> restyle.run());

        Runnable reset = () -> {
            rounded.set(true);
            shadow.set(true);
            gradient.set(true);
            radius.set(RADIUS);
            width.set(WIDTH);
            top.set(TOP);
            bottom.set(BOTTOM);
        };
        preview.on(PointerEvent.PRESS, e -> {
            if (e.button() == Keys.MOUSE_RIGHT && preview.root() != null) {
                new ContextMenu()
                        .item(Tr.r("Острые углы"), () -> rounded.set(false))
                        .item(Tr.r("Без тени"), () -> shadow.set(false))
                        .item(Tr.r("Поменять цвета местами"), () -> {
                            int t = top.peek();
                            top.set(bottom.peek());
                            bottom.set(t);
                        })
                        .separator()
                        .item(Tr.r("Сбросить по умолчанию"), reset)
                        .show(preview.root(), e.x(), e.y());
                e.consume();
            }
        });

        Panel gradientBar = Panel.box(FlexLayout.row());
        gradientBar.width(Dim.FILL).height(Dim.px(10));
        Runnable paintBar = () -> gradientBar.style(b -> b
                .set(Styles.BACKGROUND, Brush.horizontal(top.peek(), bottom.peek()))
                .set(Styles.RADIUS, Radii.all(5)));
        top.subscribe(v -> paintBar.run());
        bottom.subscribe(v -> paintBar.run());

        Panel presets = Ui.row(6,
                preset(Tr.r("Мягкий"), () -> {
                    rounded.set(true);
                    radius.set(14f);
                    width.set(1f);
                }),
                preset(Tr.r("Острый"), () -> {
                    rounded.set(false);
                    width.set(1.5f);
                }),
                preset(Tr.r("Неон"), () -> {
                    radius.set(8f);
                    width.set(2f);
                    top.set(0xFFFF4FD8);
                    bottom.set(0xFF4FE3FF);
                }),
                preset(Tr.r("Стекло"), () -> {
                    radius.set(16f);
                    width.set(0.5f);
                    top.set(0xC0FFFFFF);
                    bottom.set(0x40FFFFFF);
                }));

        ColorField accentField = new ColorField(accent).defaultValue(ModernTheme.DEFAULT_ACCENT);
        accentField.width(Dim.px(110));
        accent.subscribe(c -> {
            if (preview.root() != null) {
                preview.root().theme().set(Tokens.ACCENT, c);
            }
        });
        motion.subscribe(m -> {
            if (preview.root() != null) {
                preview.root().theme().set(Tokens.MOTION, m);
            }
        });

        Collapsible border = new Collapsible(Tr.r("Стиль рамки"),
                Ui.row(14,
                        Ui.column(6,
                                toggleRow(Tr.r("Скруглённые углы"), rounded),
                                toggleRow(Tr.r("Тень"), shadow),
                                toggleRow(Tr.r("Градиентная рамка"), gradient)).grow(1),
                        preview),
                row(Tr.r("Радиус"), new SliderField(radius, 0, 24).step(1).decimals(0).suffix(" px").width(Dim.px(210))),
                row(Tr.r("Толщина"), new SliderField(width, 0, 4).step(0.5f).decimals(1).suffix(" px").width(Dim.px(210))),
                row(Tr.r("Пресеты"), presets),
                row(Tr.r("Цвет сверху"), color(top, TOP)),
                row(Tr.r("Цвет снизу"), color(bottom, BOTTOM)),
                gradientBar);
        Collapsible theme = new Collapsible(Tr.r("Тема"),
                row(Tr.r("Акцент"), accentField),
                row(Tr.r("Скорость анимаций"), new SliderField(motion, 0.25f, 2f).step(0.05f).decimals(2).suffix("×")
                        .width(Dim.px(210))));
        restyle.run();
        paintBar.run();
        return scroll(border, theme);
    }

    private static UiElement fontTab() {
        Signal<Float> size = Signal.of(1f);
        Signal<String> family = Signal.of(Tr.r("Обычный"));
        FancyText sample = new FancyText(Tr.r("Съешь же ещё этих мягких булок")).gradient(TOP, BOTTOM);
        size.subscribe(sample::scale);
        return scroll(new Collapsible(Tr.r("Шрифт"),
                row(Tr.r("Размер"), new SliderField(size, 0.5f, 2.5f).step(0.1f).decimals(1).suffix("×").width(Dim.px(210))),
                row(Tr.r("Начертание"), RadioGroup.of(family, Tr.r("Обычный"), Tr.r("Жирный"), Tr.r("Моно")).horizontal().gap(10)),
                sample));
    }

    private static UiElement extensionsTab() {
        Panel list = Ui.column(4);
        String[][] mods = {
                {"minecraft:map", Tr.r("Мини-карта")}, {"minecraft:clock", Tr.r("Часы на экране")},
                {"minecraft:compass", Tr.r("Координаты")}, {"minecraft:spyglass", Tr.r("Приближение")},
                {"minecraft:chest", Tr.r("Сортировка сундуков")}};
        for (int i = 0; i < mods.length; i++) {
            Panel r = Panel.box(FlexLayout.row().gap(8).align(Align.CENTER))
                    .add(io.github.ssbaxys.lumina.widget.Icon.item(mods[i][0], 16),
                            new Label(mods[i][1]).grow(1),
                            new Toggle(Signal.of(i % 2 == 0)));
            r.width(Dim.FILL);
            list.add(r);
        }
        list.width(Dim.FILL);
        return scroll(new Collapsible(Tr.r("Расширения"), list));
    }

    private static UiElement textTab() {
        Signal<String> name = Signal.of("Steve");
        TextField outlined = new TextField(name).placeholder(Tr.r("Имя")).defaultValue("Steve");
        TextField filled = new TextField(Signal.of("")).placeholder(Tr.r("Заполненное")).variant("filled");
        TextField underline = new TextField(Signal.of("")).placeholder(Tr.r("С подчёркиванием")).variant("underline");
        TextField pill = new TextField(Signal.of("")).placeholder(Tr.r("Поиск")).variant("pill").icon(FieldBase.SEARCH)
                .clearButton(true);
        for (TextField f : new TextField[]{outlined, filled, underline, pill}) {
            f.width(Dim.px(200));
        }
        return scroll(new Collapsible(Tr.r("Поля ввода"),
                row(Tr.r("Обычное"), outlined), row(Tr.r("Заполненное"), filled),
                row(Tr.r("Линия"), underline), row(Tr.r("Пилюля"), pill),
                Label.bound(name.map(n -> Tr.r("Привет, ") + (n.isBlank() ? Tr.r("незнакомец") : n) + "!")).style(Ui.MUTED)));
    }

    // ---------------------------------------------------------------- строительные блоки

    private static ScrollPanel scroll(UiElement... content) {
        Panel column = Panel.box(FlexLayout.column().gap(8).align(Align.STRETCH)).add(content);
        column.style(b -> b.set(Styles.PADDING, new Insets(2, 8, 6, 2)));
        ScrollPanel scroll = new ScrollPanel(column).fill();
        scroll.setStyleClass(null);
        return scroll;
    }

    /** Строка настройки: подпись слева, элемент справа. */
    private static Panel row(String label, UiElement control) {
        Panel r = Panel.box(FlexLayout.row().gap(8).align(Align.CENTER)).add(new Label(label).grow(1), control);
        r.width(Dim.FILL);
        return r;
    }

    private static Panel toggleRow(String label, boolean on) {
        return toggleRow(label, Signal.of(on));
    }

    private static Panel toggleRow(String label, Signal<Boolean> value) {
        return row(label, new Toggle(value));
    }

    private static Panel slider(String label, float value) {
        return row(label, new SliderField(Signal.of(value), 0, 100).step(1).decimals(0).suffix("%").width(Dim.px(210)));
    }

    private static ColorField color(Signal<Integer> value, int def) {
        ColorField f = new ColorField(value).defaultValue(def);
        f.width(Dim.px(110));
        return f;
    }

    private static Button preset(String text, Runnable action) {
        return new Button(text, action).styleClass("button pill outline");
    }

    private static Panel card(String item, String title, String text) {
        Panel c = Ui.column(4, io.github.ssbaxys.lumina.widget.Icon.item(item, 20), new Label(title), Ui.muted(text));
        c.setStyleClass("card");
        c.width(Dim.px(130));
        return c;
    }
}
