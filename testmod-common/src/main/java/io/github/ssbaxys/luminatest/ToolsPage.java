package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.config.ConfigScreen;
import io.github.ssbaxys.lumina.config.ConfigSpec;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.map.DemoMap;
import io.github.ssbaxys.lumina.map.MapSource;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Gif;
import io.github.ssbaxys.lumina.render.Images;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.AnimatedImage;
import io.github.ssbaxys.lumina.widget.Backdrop;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Image;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.MiniMap;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ScrollPanel;
import io.github.ssbaxys.lumina.widget.Slider;
import io.github.ssbaxys.lumina.widget.Toggle;
import io.github.ssbaxys.lumina.widget.WorldMap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Инструменты: размытие фона, точная скруглённая обрезка, анимированный GIF, мини-карта и карта мира,
 * частицы за меню, инспектор интерфейса, геймпад и экран настроек, собранный из описания.
 */
final class ToolsPage {
    /** Пример перечисления для настроек. */
    enum Quality { LOW, MEDIUM, HIGH }

    private ToolsPage() {
    }

    static UiElement create() {
        return Ui.page(blurSection(), clipSection(), gifSection(), mapSection(), particlesSection(),
                inspectorSection(), configSection());
    }

    // ---------------------------------------------------------------- размытие

    private static Panel blurSection() {
        Signal<Float> blur = Signal.of(8f);
        Label value = Ui.muted("");
        Panel card = Panel.box(FlexLayout.column().gap(4).align(Align.CENTER)).size(200, 80).id("glass");
        card.style(b -> b
                .set(Styles.BACKDROP_BLUR, blur.peek())
                .set(Styles.BACKGROUND, Brush.solid(0x30FFFFFF))
                .set(Styles.BORDER_COLOR, 0x60FFFFFF)
                .set(Styles.BORDER_WIDTH, 1f)
                .set(Styles.RADIUS, Radii.all(12))
                .set(Styles.PADDING, Insets.all(8)));
        card.add(new Label("Матовое стекло"), Ui.muted("за ним — живой фон"));
        blur.subscribe(v -> {
            card.style(b -> b.set(Styles.BACKDROP_BLUR, v));
            value.text("backdrop-blur: " + Math.round(v));
        });
        value.text("backdrop-blur: " + Math.round(blur.peek()));

        Panel stage = Panel.box(new StackLayout(Align.CENTER, Align.CENTER)).width(Dim.FILL).height(Dim.px(130));
        stage.style(b -> b.set(Styles.RADIUS, Radii.all(8)).set(Styles.CLIP_ROUNDED, true));
        stage.setClipChildren(true);
        stage.add(new Backdrop(Backdrop.Kind.AURORA), card);

        Slider slider = new Slider(blur, 0f, 24f).step(1f);
        slider.id("blur-slider").width(Dim.px(160));
        return Ui.section("Размытие фона",
                "Стиль backdrop-blur размывает то, что уже нарисовано под элементом. В игре — ванильный шейдер "
                        + "размытия; где его нет, рисуется матовая подложка",
                stage, Ui.row(8, slider, value));
    }

    // ---------------------------------------------------------------- обрезка

    private static Panel clipSection() {
        Panel list = Panel.box(FlexLayout.column().gap(3));
        int[] colors = {0xFF5B8CFF, 0xFF2BD9A8, 0xFFFFB347, 0xFFE05A9C, 0xFFB46CFF};
        for (int i = 0; i < 14; i++) {
            Panel row = Panel.box(FlexLayout.row().gap(6).align(Align.CENTER)).width(Dim.FILL);
            row.style(b -> b.set(Styles.PADDING, Insets.of(3, 6)));
            row.add(Ui.box(colors[i % colors.length], 10, 10), new Label("Строка " + (i + 1)));
            row.style(b -> b.set(Styles.BACKGROUND, Brush.solid(0x22FFFFFF)));
            list.add(row);
        }
        ScrollPanel scroll = new ScrollPanel(list).size(170, 110).id("rounded-scroll");
        scroll.style(b -> b
                .set(Styles.RADIUS, Radii.all(14))
                .set(Styles.CLIP_ROUNDED, true)
                .set(Styles.BACKGROUND, Brush.solid(0x30000000)));

        UiElement picture;
        TextureRef checker = Images.upload("luminatest/tools/checker", checker(64, 64));
        if (checker == null) {
            picture = Ui.muted("Текстуры недоступны");
        } else {
            picture = new Image(checker).size(110, 110).id("rounded-image").style(b -> b
                    .set(Styles.RADIUS, Radii.all(28))
                    .set(Styles.CLIP_ROUNDED, true));
        }
        return Ui.section("Скруглённая обрезка",
                "clip-rounded: true обрезает детей по radius. Бэкенд закрашивает углы снимком фона; без снимков "
                        + "обрезка прямоугольная",
                Ui.row(16, Ui.labeled(scroll, "ScrollPanel"), Ui.labeled(picture, "Image")));
    }

    /** Шахматка с полосой: на ней хорошо видно, ровно ли срезаны углы. */
    static Images.Pixels checker(int w, int h) {
        int[] px = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean dark = ((x >> 3) + (y >> 3)) % 2 == 0;
                int c = dark ? 0xFF2F6FD8 : 0xFF6FD6F0;
                if (Math.abs(x - y) < 3) {
                    c = 0xFFFFE08A;
                }
                px[y * w + x] = c;
            }
        }
        return new Images.Pixels(w, h, px);
    }

    // ---------------------------------------------------------------- GIF

    private static Panel gifSection() {
        TextureRef spinner = null;
        try {
            spinner = Images.fromBytes("luminatest/tools/spinner", Gif.encode(spinnerFrames(32, 12), 70, 0));
        } catch (IOException e) {
            // кодировщик GIF недоступен — покажем подпись
        }
        if (spinner == null) {
            return Ui.section("Анимированный GIF", null, Ui.muted("Текстуры недоступны"));
        }
        Image plain = new Image(spinner).size(48, 48).id("gif-image");
        AnimatedImage controlled = AnimatedImage.of(spinner).size(48, 48).id("gif-animated");
        Label frame = Ui.muted("кадр 1");
        controlled.onFrame(i -> frame.text("кадр " + (i + 1)));
        Button pause = new Button("Пауза").id("gif-pause");
        pause.onPress(() -> {
            if (controlled.isPlaying()) {
                controlled.pause();
                pause.text("Играть");
            } else {
                controlled.play();
                pause.text("Пауза");
            }
        });
        Button fast = new Button("×2", () -> controlled.speed(controlled.speed() == 1f ? 2f : 1f)).id("gif-speed");
        return Ui.section("Анимированный GIF",
                "Кадры читает javax.imageio, каждый кадр — своя текстура; задержки и число повторов берутся из файла. "
                        + "Этот GIF собран в коде через Gif.encode",
                Ui.row(16, Ui.labeled(plain, "Image"), Ui.labeled(controlled, "AnimatedImage"),
                        Ui.column(4, Ui.row(6, pause, fast), frame)));
    }

    /** Кольцо с бегущей яркой точкой и хвостом. */
    static List<Images.Pixels> spinnerFrames(int size, int count) {
        List<Images.Pixels> frames = new ArrayList<>();
        float c = (size - 1) / 2f;
        float ring = size * 0.36f;
        for (int f = 0; f < count; f++) {
            int[] px = new int[size * size];
            double head = Math.PI * 2 * f / count;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    float dx = x - c;
                    float dy = y - c;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    int color = 0xFF141826;
                    if (Math.abs(d - ring) < 2.2f) {
                        double a = Math.atan2(dy, dx);
                        double behind = (head - a + Math.PI * 4) % (Math.PI * 2);
                        float k = (float) Math.max(0, 1 - behind / Math.PI);
                        int g = 60 + (int) (k * 195);
                        color = 0xFF000000 | (g / 3) << 16 | (g * 2 / 3) << 8 | g;
                    }
                    px[y * size + x] = color;
                }
            }
            frames.add(new Images.Pixels(size, size, px));
        }
        return frames;
    }

    // ---------------------------------------------------------------- карты

    private static Panel mapSection() {
        DemoMap demo = new DemoMap(42).walk(48f, 0.03f);
        MapSource source = MapSource.platform().or(demo);
        MiniMap round = new MiniMap().source(source).size(120, 120).zoom(1.5f).id("minimap-round")
                .marker("home", 0, 0, 0xFFFFE08A, "Дом")
                .marker("portal", 60, -40, 0xFFB46CFF, "Портал");
        MiniMap square = new MiniMap().source(source).shape(MiniMap.Shape.SQUARE).rotate(false)
                .size(120, 120).zoom(1f).showCoordinates(true).id("minimap-square")
                .marker("home", 0, 0, 0xFFFFE08A, "Дом");
        WorldMap world = new WorldMap(demo, 0, 0, 256).size(200, 120).zoom(1f).id("world-map")
                .marker(0, 0, 0xFFFFE08A, "Дом")
                .marker(60, -40, 0xFFB46CFF, "Портал");
        return Ui.section("Мини-карта",
                "В мире цвета верхних блоков читает загрузчик (по кусочку за кадр); без мира — демо-ландшафт. "
                        + "Колесо — масштаб, у карты мира — перетаскивание",
                Ui.row(12, Ui.labeled(round, "круглая, по взгляду"), Ui.labeled(square, "квадратная, север вверху"),
                        Ui.labeled(world, "карта мира")));
    }

    // ---------------------------------------------------------------- частицы

    private static Panel particlesSection() {
        Panel row = Ui.row(10);
        String[] names = {"Искры", "Снег", "Пыль"};
        Backdrop.ParticleStyle[] styles = Backdrop.ParticleStyle.values();
        for (int i = 0; i < styles.length; i++) {
            Panel box = Panel.box(new StackLayout(Align.CENTER, Align.CENTER)).size(150, 90);
            box.style(b -> b.set(Styles.RADIUS, Radii.all(8)).set(Styles.CLIP_ROUNDED, true));
            box.setClipChildren(true);
            Backdrop particles = new Backdrop(Backdrop.Kind.PARTICLES).particles(styles[i]).particleCount(70);
            particles.id("particles-" + styles[i].name().toLowerCase());
            box.add(particles, new Label(names[i]));
            row.add(box);
        }
        return Ui.section("Частицы за меню",
                "Backdrop.Kind.PARTICLES: искры, снег или пыль. Частицы разлетаются от курсора", row);
    }

    // ---------------------------------------------------------------- инспектор и геймпад

    private static Panel inspectorSection() {
        // Переключатель разрешает инспектор (и клавишу F8); кнопка включает его и сразу открывает.
        Signal<Boolean> f8 = Signal.of(false);
        Toggle key = new Toggle(f8);
        key.id("inspector-key");
        Button open = new Button("Открыть инспектор", () -> {
            f8.set(true);
            UiRoot r = key.root();
            if (r != null) {
                r.inspector().enabled(true).open();
            }
        }).id("open-inspector");
        f8.subscribe(v -> {
            UiRoot r = key.root();
            if (r != null) {
                r.inspector().toggleKey(Keys.F8).enabled(v);
            }
        });
        Signal<Boolean> pad = Signal.of(true);
        Toggle padToggle = new Toggle(pad);
        padToggle.id("gamepad-toggle");
        pad.subscribe(v -> {
            UiRoot r = padToggle.root();
            if (r != null) {
                r.gamepad().enabled(v);
            }
        });
        return Ui.section("Инспектор и геймпад",
                "Инспектор показывает границы, отступы, классы и вычисленный стиль; клик выбирает элемент, стрелки "
                        + "ходят по дереву, Esc закрывает",
                Ui.row(10, open, key, new Label("Клавиша F8")),
                Ui.row(10, padToggle, new Label("Геймпад: A — нажать, B — назад, крестовина и стик — фокус, "
                        + "бамперы — вкладки")));
    }

    // ---------------------------------------------------------------- настройки

    static ConfigSpec spec() {
        return ConfigSpec.builder("luminatest/tools.toml")
                .header("Настройки витрины Lumina")
                .category("hud", "Интерфейс").icon("minecraft:compass")
                .bool("showMinimap", "Мини-карта", true).comment("Показывать в углу экрана")
                .integer("minimapSize", "Размер карты", 96, 48, 192).step(8)
                .number("opacity", "Прозрачность", 0.85f, 0.2f, 1f).step(0.05f)
                .key("zoomKey", "Приблизить карту", Keys.Z)
                .category("look", "Внешний вид").icon("minecraft:painting")
                .choice("quality", "Качество частиц", Quality.MEDIUM, Quality.values())
                .labels(q -> switch ((Quality) q) {
                    case LOW -> "Низкое";
                    case MEDIUM -> "Среднее";
                    case HIGH -> "Высокое";
                })
                .color("accent", "Акцент", 0xFF5B8CFF).alpha(false)
                .string("greeting", "Приветствие", "Привет!").maxLength(24)
                .build();
    }

    private static Panel configSection() {
        ConfigSpec spec = spec();
        spec.load();
        ConfigScreen screen = new ConfigScreen(spec).title("Настройки витрины").id("config");
        screen.width(Dim.FILL).height(Dim.px(250));
        return Ui.section("Экран настроек из описания",
                "ConfigSpec описывает настройки без рефлексии; ConfigScreen сам строит строки, поиск, сброс и "
                        + "сохранение в TOML",
                screen);
    }
}
