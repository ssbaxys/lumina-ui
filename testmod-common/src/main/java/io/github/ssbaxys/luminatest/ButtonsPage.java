package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.anim.Motion;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Computed;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Images;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Checkbox;
import io.github.ssbaxys.lumina.widget.Image;
import io.github.ssbaxys.lumina.widget.ImageButton;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.RadioGroup;
import io.github.ssbaxys.lumina.widget.Toggle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Stream;

/** Флажки и радио-группы, все варианты кнопок, иконки, загрузка, кнопки-картинки и картинки из файлов. */
final class ButtonsPage {
    private static final String PANORAMA = "minecraft:textures/gui/title/background/panorama_";

    private ButtonsPage() {
    }

    static UiElement create() {
        return Ui.page(
                Ui.section(Tr.r("Флажки"), Tr.r("Checkbox: свой вид анимации у каждого Kind; подпись тоже кликабельна, Space/Enter в фокусе"),
                        kinds()),
                Ui.section(Tr.r("Неопределённое состояние"), Tr.r("Kind.CHECK: черта, когда выбрана только часть"), selectAll()),
                Ui.section(Tr.r("Радио-группы"), Tr.r("RadioGroup<T>: ровно один вариант в Signal<T>, стрелки двигают выбор"), radios()),
                Ui.section(Tr.r("Варианты кнопок"), "styleClass(\"button outline\"): primary, outline, ghost, pill, gradient, danger, success, big",
                        variants()),
                Ui.section(Tr.r("Иконки"), Tr.r(".item(\"minecraft:diamond\"), .icon(TextureRef), Button.round(...) — круглые кнопки-иконки"),
                        icons()),
                Ui.section(Tr.r("Загрузка"), Tr.r(".loading(true): спиннер вместо иконки, кнопка недоступна"), loading()),
                Ui.section(Tr.r("Кнопки-картинки"), Tr.r("ImageButton: картинка на всю кнопку, подпись снизу, при наведении — приближение"),
                        imageButtons()),
                Ui.section(Tr.r("Картинки"), Tr.r("Image: CONTAIN, COVER, STRETCH и скругление; Images.fromFile читает PNG/JPG/GIF/BMP с диска"),
                        images()));
    }

    // ---------------------------------------------------------------- флажки

    private static Panel kinds() {
        Checkbox.Kind[] kinds = Checkbox.Kind.values();
        UiElement[] boxes = new UiElement[kinds.length];
        for (int i = 0; i < kinds.length; i++) {
            Checkbox.Kind kind = kinds[i];
            Checkbox cb = new Checkbox(kind.name(), Signal.of(i % 2 == 0)).kind(kind);
            if (kind == Checkbox.Kind.HEART) {
                cb.color(0xFFF0506E);
            } else if (kind == Checkbox.Kind.STAR) {
                cb.color(0xFFFFC83D);
            }
            boxes[i] = cb;
        }
        Checkbox big = new Checkbox(Tr.r("Крупный"), Signal.of(true)).boxSize(18);
        Checkbox off = new Checkbox(Tr.r("Недоступный"), Signal.of(true)).enabled(false);
        return Ui.column(8, Ui.grid(4, 14, boxes), Ui.row(14, big, off));
    }

    private static Panel selectAll() {
        String[] names = {Tr.r("Мечи"), Tr.r("Броня"), Tr.r("Зелья")};
        List<Signal<Boolean>> items = new ArrayList<>();
        Signal<Boolean> all = Signal.of(false);
        Checkbox parent = new Checkbox(Tr.r("Выбрать всё"), all);
        Panel children = Ui.column(4);
        Runnable refresh = () -> {
            long count = items.stream().filter(s -> Boolean.TRUE.equals(s.peek())).count();
            all.set(count == items.size());
            parent.indeterminate(count > 0 && count < items.size());
        };
        for (int i = 0; i < names.length; i++) {
            Signal<Boolean> item = Signal.of(i == 0);
            item.subscribe(v -> refresh.run());
            items.add(item);
            children.add(new Checkbox(names[i], item));
        }
        parent.action(() -> {
            boolean target = !Boolean.TRUE.equals(all.peek());
            items.forEach(s -> s.set(target));
            refresh.run();
        });
        refresh.run();
        children.style(b -> b.set(Styles.PADDING, new Insets(0, 0, 0, 16)));
        return Ui.column(4, parent, children);
    }

    private static Panel radios() {
        Signal<String> difficulty = Signal.of(Tr.r("Нормальный"));
        RadioGroup<String> vertical = RadioGroup.of(difficulty, Tr.r("Мирный"), Tr.r("Лёгкий"), Tr.r("Нормальный"), Tr.r("Сложный"));
        Label chosen = Label.bound(new Computed<>(() -> Tr.r("Сложность: ") + difficulty.get()));

        Signal<Integer> size = Signal.of(16);
        RadioGroup<Integer> horizontal = RadioGroup.of(size, 8, 16, 32, 64).labels(v -> v + "px").horizontal().gap(12);
        RadioGroup<Integer> asFill = RadioGroup.of(size, 8, 16, 32, 64).labels(v -> v + "px")
                .kind(Checkbox.Kind.FILL).horizontal().gap(12);
        Label sizeLabel = Label.bound(new Computed<>(() -> Tr.r("Размер: ") + size.get() + Tr.r(" (одна переменная на две группы)")));
        return Ui.row(24, Ui.column(6, vertical, chosen), Ui.column(8, horizontal, asFill, sizeLabel));
    }

    // ---------------------------------------------------------------- кнопки

    private static Panel variants() {
        String[][] all = {
                {Tr.r("Обычная"), "button"}, {"Primary", "button primary"}, {"Outline", "button outline"},
                {"Ghost", "button ghost"}, {"Pill", "button pill"}, {"Gradient", "button gradient"},
                {"Danger", "button danger"}, {"Success", "button success"}, {"Gradient pill", "button gradient pill"},
                {"Outline pill", "button outline pill"}
        };
        UiElement[] buttons = new UiElement[all.length];
        for (int i = 0; i < all.length; i++) {
            buttons[i] = new Button(all[i][0]).styleClass(all[i][1]);
        }
        Button big = new Button(Tr.r("Большая кнопка")).styleClass("button big gradient");
        Button disabled = new Button(Tr.r("Недоступная")).styleClass("button outline").enabled(false);
        return Ui.column(8, Ui.grid(5, 6, buttons), Ui.row(8, big, disabled));
    }

    private static Panel icons() {
        Panel items = Ui.row(6,
                new Button(Tr.r("Алмаз")).item("minecraft:diamond"),
                new Button(Tr.r("Изумруд")).item("minecraft:emerald").styleClass("button outline"),
                new Button(Tr.r("Яблоко")).icon(TextureRef.vanilla("item/apple")).styleClass("button pill"),
                new Button(Tr.r("Жемчуг")).icon(TextureRef.vanilla("item/ender_pearl")).styleClass("button ghost"),
                new Button(Tr.r("Взорвать")).item("minecraft:tnt").styleClass("button danger"));
        Panel round = Ui.row(6,
                Button.roundItem("minecraft:diamond_sword", null),
                Button.roundItem("minecraft:golden_apple", null).styleClass("button icon success"),
                Button.round(TextureRef.vanilla("item/compass_00"), null).styleClass("button icon outline"),
                Button.round(TextureRef.vanilla("item/clock_00"), null).styleClass("button icon ghost"),
                Button.roundItem("minecraft:tnt", null).styleClass("button icon danger"),
                Button.roundItem("minecraft:nether_star", null).styleClass("button icon gradient").iconSize(22),
                Ui.muted(Tr.r("круглые: styleClass(\"button icon ...\")")));
        Button play = new Button(Tr.r("Играть")).item("minecraft:grass_block").iconSize(18).styleClass("button big gradient");
        Button world = new Button(Tr.r("Новый мир")).icon(TextureRef.vanilla("item/map")).iconSize(16).styleClass("button big outline");
        return Ui.column(8, items, round, Ui.row(8, play, world));
    }

    private static Panel loading() {
        Button save = new Button(Tr.r("Сохранить")).item("minecraft:writable_book");
        save.onPress(() -> fakeWork(save, 2f));
        Button send = new Button(Tr.r("Отправить")).styleClass("button gradient");
        send.onPress(() -> fakeWork(send, 3f));
        Button round = Button.roundItem("minecraft:ender_eye", null).styleClass("button icon outline");
        round.onPress(() -> fakeWork(round, 1.5f));

        Signal<Boolean> busy = Signal.of(true);
        Button bound = new Button(Tr.r("Ждём сервер")).styleClass("button outline").loading(busy);
        return Ui.column(8,
                Ui.row(8, save, send, round, Ui.muted(Tr.r("нажмите — пара секунд «работы»"))),
                Ui.row(8, new Toggle(busy), new Label("loading(Signal)"), bound));
    }

    /** Показать загрузку {@code seconds} секунд: таймер привязан к кнопке и остановится вместе с ней. */
    private static void fakeWork(Button button, float seconds) {
        button.loading(true);
        button.play(Motion.delay(seconds).then(Motion.call(() -> button.loading(false))));
    }

    // ---------------------------------------------------------------- картинки

    private static Panel imageButtons() {
        Signal<String> picked = Signal.of("—");
        String[] sides = {Tr.r("Север"), Tr.r("Восток"), Tr.r("Юг"), Tr.r("Запад"), Tr.r("Небо"), Tr.r("Земля")};
        UiElement[] cards = new UiElement[sides.length];
        for (int i = 0; i < sides.length; i++) {
            String side = sides[i];
            cards[i] = new ImageButton(TextureRef.of(PANORAMA + i + ".png"), Tr.r("Панорама: ") + side,
                    () -> picked.set(side)).size(112, 70);
        }
        String[][] blocks = {
                {"block/oak_planks", Tr.r("Дуб")}, {"block/bricks", Tr.r("Кирпичи")}, {"block/diamond_ore", Tr.r("Алмазная руда")},
                {"block/cherry_planks", Tr.r("Вишня")}, {"block/amethyst_block", Tr.r("Аметист")}
        };
        UiElement[] tiles = new UiElement[blocks.length];
        for (int i = 0; i < blocks.length; i++) {
            String name = blocks[i][1];
            tiles[i] = new ImageButton(TextureRef.vanilla(blocks[i][0]), name, () -> picked.set(name)).size(64, 64);
        }
        return Ui.column(8, Ui.grid(3, 8, cards), Ui.row(8, tiles),
                Label.bound(new Computed<>(() -> Tr.r("Выбрано: ") + picked.get())));
    }

    private static Panel images() {
        TextureRef panorama = TextureRef.of(PANORAMA + "1.png");
        Panel fits = Ui.row(10,
                Ui.labeled(framed(Image.of(panorama).fit(Image.Fit.CONTAIN)), "CONTAIN"),
                Ui.labeled(framed(Image.of(panorama).fit(Image.Fit.COVER)), "COVER"),
                Ui.labeled(framed(Image.of(panorama).fit(Image.Fit.STRETCH)), "STRETCH"),
                Ui.labeled(Image.of(TextureRef.vanilla("block/diamond_block")).size(50, 50).radius(25), "radius 25"));
        return Ui.column(8, fits, screenshot());
    }

    /** Картинка 110×50 в рамке-подложке, чтобы были видны поля CONTAIN. */
    private static Panel framed(Image image) {
        image.size(110, 50).radius(8);
        Panel frame = Ui.column(0, image);
        frame.style(Ui.WELL);
        return frame;
    }

    /** Последний скриншот игры из папки screenshots — картинка из обычного файла. */
    private static UiElement screenshot() {
        Optional<Path> last = Optional.empty();
        Path dir = Path.of("screenshots");
        if (Files.isDirectory(dir)) {
            try (Stream<Path> files = Files.list(dir)) {
                last = files.filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                        .max(Comparator.comparingLong(ButtonsPage::modified));
            } catch (IOException | RuntimeException e) {
                last = Optional.empty();
            }
        }
        if (last.isEmpty()) {
            return Ui.muted(Tr.r("Сделайте скриншот (F2) — здесь появится последний из папки screenshots"));
        }
        TextureRef shot = Images.fromFile(last.get());
        if (shot == null) {
            return Ui.muted(Tr.r("Не удалось загрузить ") + last.get().getFileName());
        }
        Image image = Image.of(shot).fit(Image.Fit.COVER).size(192, 108).radius(10);
        return Ui.row(10, image, Ui.column(4, new Label(Tr.r("Последний скриншот")), Ui.muted(last.get().getFileName().toString()),
                Ui.muted("Images.fromFile(path) → Image.fit(COVER)")));
    }

    private static long modified(Path p) {
        try {
            return Files.getLastModifiedTime(p).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}
