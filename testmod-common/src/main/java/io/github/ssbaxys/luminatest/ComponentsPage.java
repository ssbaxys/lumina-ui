package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Chart;
import io.github.ssbaxys.lumina.widget.FancyText;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Skeleton;
import io.github.ssbaxys.lumina.widget.Spinner;
import io.github.ssbaxys.lumina.widget.Table;
import io.github.ssbaxys.lumina.widget.TreeView;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/** Таблицы, деревья, графики, текст с эффектами и индикаторы загрузки. */
final class ComponentsPage {
    private record Ore(String name, String item, int level, int count, float chance) {
    }

    private static final List<Ore> ORES = List.of(
            new Ore("Уголь", "minecraft:coal_ore", 0, 1840, 0.42f),
            new Ore("Железо", "minecraft:iron_ore", 1, 912, 0.21f),
            new Ore("Медь", "minecraft:copper_ore", 1, 1203, 0.27f),
            new Ore("Золото", "minecraft:gold_ore", 2, 214, 0.05f),
            new Ore("Редстоун", "minecraft:redstone_ore", 2, 530, 0.12f),
            new Ore("Лазурит", "minecraft:lapis_ore", 1, 187, 0.04f),
            new Ore("Алмаз", "minecraft:diamond_ore", 2, 61, 0.012f),
            new Ore("Изумруд", "minecraft:emerald_ore", 2, 9, 0.002f));

    private ComponentsPage() {
    }

    static UiElement create() {
        return Ui.page(
                Ui.section("Текст с эффектами",
                        "Градиент, радуга, обводка, волна, дрожание и печатная машинка — посимвольно обычным шрифтом.",
                        fancyText()),
                Ui.section("Таблица",
                        "Клик по заголовку сортирует (повторный — в обратную сторону), клик по строке выбирает.",
                        table()),
                Ui.section("Дерево", "Стрелка или двойной клик раскрывают узел; стрелки клавиатуры тоже работают.",
                        tree()),
                Ui.section("Графики", "Значения вырастают при появлении и плавно перетекают к новым; наведите на точку.",
                        charts()),
                Ui.section("Загрузка", "Спиннеры и скелетоны для экранов, которые ждут данных.", loaders()));
    }

    private static UiElement fancyText() {
        FancyText typer = new FancyText("Добро пожаловать в Lumina UI! Этот текст печатается сам.")
                .typewriter(24f).typeSound("minecraft:ui.button.click");
        return Ui.column(8,
                new FancyText("Lumina UI").scale(2.4f).gradient(0xFFFF8A00, 0xFFE52E71, 0xFF9B5CFF).flow(0.4f)
                        .outline(0xFF1A0F24),
                Ui.row(16,
                        new FancyText("Радуга").scale(1.5f).rainbow().flow(0.6f),
                        new FancyText("Волна").scale(1.5f).gradient(0xFF4FE3FF, 0xFF2F6FD8).wave(2f, 6f),
                        new FancyText("Тряска!").scale(1.5f).gradient(0xFFFF5050, 0xFFFFC14F).shake(0.8f),
                        new FancyText("Обводка").scale(1.5f).gradient(0xFFFFFFFF, 0xFFFFFFFF).outline(0xFF3A6BFF)),
                Ui.row(8, typer, new Button("Ещё раз", typer::retype).styleClass("button ghost")));
    }

    private static UiElement table() {
        Table<Ore> table = new Table<Ore>()
                .column("", 0.5f, (c, row, x, y, w, h) -> c.drawItem(row.item(), x, y + 1f, 14f), null)
                .column("Руда", 2f, Ore::name)
                .column("Кирка", 1.2f, o -> switch (o.level()) {
                    case 0 -> "деревянная";
                    case 1 -> "каменная";
                    default -> "железная";
                }, Comparator.comparingInt(Ore::level))
                .column("Добыто", 1f, o -> String.valueOf(o.count()), Comparator.comparingInt(Ore::count))
                .column("Шанс", 1.6f, (c, row, x, y, w, h) -> {
                    float share = Math.min(1f, row.chance() / 0.42f);
                    c.fillRoundRect(x, y + 5f, w, 6f, Radii.all(3f), Brush.solid(0x30808080));
                    c.fillRoundRect(x, y + 5f, Math.max(6f, w * share), 6f, Radii.all(3f),
                            Brush.horizontal(0xFF5B8CFF, 0xFF9B5CFF));
                }, Comparator.comparingDouble(Ore::chance))
                .rows(ORES)
                .width(Dim.FILL);
        Label picked = Label.bound(table.selection().map(o -> o == null ? "Ничего не выбрано"
                : "Выбрано: " + o.name() + ", добыто " + o.count()));
        return Ui.column(6, table, picked.style(Ui.MUTED));
    }

    private static UiElement tree() {
        TreeView.Node world = TreeView.node("Мир", "minecraft:grass_block").add(
                TreeView.node("Верхний мир", "minecraft:oak_sapling").add(
                        TreeView.node("Деревня", "minecraft:bell").add(
                                TreeView.node("Кузница", "minecraft:anvil"),
                                TreeView.node("Библиотека", "minecraft:bookshelf")),
                        TreeView.node("Шахта", "minecraft:rail")).expanded(true),
                TreeView.node("Незер", "minecraft:netherrack").add(
                        TreeView.node("Крепость", "minecraft:nether_bricks"),
                        TreeView.node("Бастион", "minecraft:gilded_blackstone")),
                TreeView.node("Энд", "minecraft:end_stone").add(
                        TreeView.node("Город Энда", "minecraft:purpur_block")));
        TreeView view = new TreeView(world);
        Label picked = Label.bound(view.selection().map(n -> n == null ? "Выберите узел" : "Узел: " + n.label()));
        return Ui.row(16, view.width(Dim.px(200)), picked.style(Ui.MUTED));
    }

    private static UiElement charts() {
        Chart bars = new Chart(Chart.Kind.BAR)
                .labels("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
                .series("Камень", 0xFF8AB4F8, 120, 180, 90, 210, 160, 260, 140)
                .series("Руда", 0xFFF6C177, 30, 45, 20, 60, 55, 90, 35)
                .unit(" шт.");
        bars.size(300, 130);
        Chart line = new Chart(Chart.Kind.AREA)
                .labels("0", "4", "8", "12", "16", "20", "24")
                .series("Игроки онлайн", 0xFF9CCFD8, 3, 1, 5, 12, 18, 24, 9)
                .legend(false);
        line.size(240, 130);
        Chart donut = new Chart(Chart.Kind.DONUT)
                .labels("Камень", "Земля", "Дерево", "Руда")
                .series("Блоки", 0, 52, 24, 16, 8)
                .unit("%");
        donut.size(200, 110);
        Random random = new Random();
        Button shuffle = new Button("Новые данные", () -> {
            float[] a = new float[7];
            float[] b = new float[7];
            float[] c = new float[7];
            for (int i = 0; i < 7; i++) {
                a[i] = 60 + random.nextInt(220);
                b[i] = 10 + random.nextInt(90);
                c[i] = random.nextInt(30);
            }
            bars.values(0, a).values(1, b);
            line.values(0, c);
            donut.values(0, 10 + random.nextInt(50), 10 + random.nextInt(30), 5 + random.nextInt(20), 2 + random.nextInt(10));
        });
        return Ui.column(8, Ui.row(16, bars, line), Ui.row(16, donut, shuffle));
    }

    private static UiElement loaders() {
        Signal<Boolean> loading = Signal.of(true);
        Panel card = Ui.column(6,
                Ui.row(8, Skeleton.circle(28), Ui.column(5, Skeleton.line(140), Skeleton.line(90))),
                Skeleton.fullLine(), Skeleton.fullLine(), Skeleton.line(120)).width(Dim.px(240));
        card.style(b -> b
                .set(Styles.BACKGROUND, Brush.solid(0x14808080))
                .set(Styles.RADIUS, Radii.all(8))
                .set(Styles.PADDING, io.github.ssbaxys.lumina.geom.Insets.all(10)));
        Panel loaded = Ui.column(6,
                Ui.row(8, io.github.ssbaxys.lumina.widget.Icon.item("minecraft:player_head", 28),
                        Ui.column(2, new Label("Steve"), Ui.muted("в сети · 12 ч"))),
                Ui.muted("Любит строить башни из булыжника и терять кирки в лаве."));
        loaded.width(Dim.px(240));
        loaded.setVisible(false);
        loading.subscribe(l -> {
            card.setVisible(l);
            loaded.setVisible(!l);
        });
        return Ui.column(10,
                Ui.row(18,
                        Ui.labeled(new Spinner(), "кольцо"),
                        Ui.labeled(new Spinner(Spinner.Kind.DOTS, 16), "точки"),
                        Ui.labeled(new Spinner(Spinner.Kind.BARS, 16), "полоски"),
                        Ui.labeled(new Spinner(Spinner.Kind.PULSE, 20), "пульс")),
                Ui.row(10, card, loaded,
                        new Button("Загрузить / сбросить", () -> loading.set(!loading.peek())).styleClass("button ghost")));
    }
}
