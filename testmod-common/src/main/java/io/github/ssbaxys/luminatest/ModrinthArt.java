package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Chart;
import io.github.ssbaxys.lumina.widget.Checkbox;
import io.github.ssbaxys.lumina.widget.FancyText;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ProgressBar;
import io.github.ssbaxys.lumina.widget.RingProgress;
import io.github.ssbaxys.lumina.widget.Slider;
import io.github.ssbaxys.lumina.widget.TextField;
import io.github.ssbaxys.lumina.widget.Toggle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Картинки для страницы мода (баннер, галерея, иконка), собранные из виджетов самой библиотеки.
 * Рисуются без игры модулем {@code offscreen}: {@code gradlew :offscreen:modrinthArt}.
 */
public final class ModrinthArt {
    /** Одна картинка: размер в GUI-пикселях, множитель до пикселей PNG, сцена и подготовка кадра. */
    public record Art(String name, int width, int height, float scale, Supplier<UiRoot> scene,
                      float settle, Consumer<UiRoot> prepare) {
    }

    // Фирменная палитра: «свет» от голубого через фиолетовый к розовому.
    static final int BLUE = 0xFF5B8CFF;
    static final int VIOLET = 0xFF8B5CF6;
    static final int PINK = 0xFFEC4899;
    static final int CYAN = 0xFF38D6E8;
    static final int TEXT = 0xFFE8ECF6;
    static final int MUTED = 0xFF9AA3B8;

    private static boolean ru;

    private ModrinthArt() {
    }

    private static String t(String en, String russian) {
        return ru ? russian : en;
    }

    public static List<Art> all(boolean russian) {
        ru = russian;
        I18n.setLanguage(russian ? "ru_ru" : "en_us");
        List<Art> list = new ArrayList<>();
        list.add(new Art("icon", 128, 128, 4, ModrinthArt::icon, 0.1f, null));
        list.add(new Art("banner", 640, 160, 3, ModrinthArt::banner, 1.5f, null));
        list.add(new Art("gallery-01-hero", 960, 540, 2, ModrinthArt::hero, 1.5f, null));
        list.add(gallery("gallery-02-modern", t("Modern, themeable widgets", "Современные виджеты с темами"),
                t("Settings screens, tabs, toggles, sliders and color fields that restyle from a few theme tokens.",
                        "Экраны настроек, вкладки, переключатели, ползунки и поля цвета — всё перекрашивается токенами темы."),
                ModernPage::create, null));
        list.add(gallery("gallery-03-gradients", t("Gradients, colors and soft shadows", "Градиенты, цвета и мягкие тени"),
                t("Linear, radial and conic gradients with up to 8 stops, blended in OKLab, OKLCH or HSV right in the shader.",
                        "Линейные, радиальные и конические градиенты до 8 цветов, смешивание в OKLab, OKLCH и HSV прямо в шейдере."),
                GradientsPage::create, null));
        list.add(gallery("gallery-04-items", t("Made for Minecraft", "Сделано для Minecraft"),
                t("Real items, chests and inventories with drag and drop, armor slots, server-synced menus.",
                        "Настоящие предметы, сундуки и инвентари с перетаскиванием, слоты брони и меню, синхронные с сервером."),
                ItemsPage::create, null));
        list.add(gallery("gallery-05-structures", t("Blocks and 3D builds", "Блоки и 3D-постройки"),
                t("Block maps, a rotating 3D structure preview with perspective and a layer-by-layer editor.",
                        "Карты из блоков, вращаемый 3D-предпросмотр построек с перспективой и редактор по слоям."),
                StructuresPage::create, null));
        list.add(gallery("gallery-06-markup", t("Write screens in HTML and CSS", "Экраны на HTML и CSS"),
                t("Load layouts from resource packs, bind them to your logic, reload with F3+T.",
                        "Разметка грузится из ресурспака, связывается с логикой мода и обновляется по F3+T."),
                MarkupPage::create, null));
        list.add(gallery("gallery-07-effects", t("Vanilla effects and animations", "Ванильные эффекты и анимации"),
                t("Fire, explosions, falling TNT, weather and particles, plus tweens, springs and keyframes for any style.",
                        "Огонь, взрывы, падающий TNT, погода и частицы, а ещё твины, пружины и ключевые кадры для любого стиля."),
                EffectsPage::create, root -> {
                    press(root, "Fire", "Огонь");
                    press(root, "Soul fire", "Огонь душ");
                    press(root, "Snow", "Снег");
                    press(root, "Flames", "Пламя");
                    press(root, "TNT rain", "Ливень TNT");
                }));
        list.add(gallery("gallery-08-components", t("Rich components", "Богатые компоненты"),
                t("Animated text, sortable tables, trees, charts, dropdowns, tooltips and Markdown.",
                        "Анимированный текст, сортируемые таблицы, деревья, графики, списки, подсказки и Markdown."),
                ComponentsPage::create, null));
        return list;
    }

    // ================================================================ общие детали

    static Theme theme() {
        return ShowcaseTheme.dark().set(Tokens.ACCENT, BLUE);
    }

    /** Знак Lumina: скруглённый квадрат с градиентом, буква L из двух полос и искра света. */
    static void paintMark(Canvas c, float x, float y, float s, boolean shadow) {
        Radii r = Radii.all(s * 0.24f);
        if (shadow) {
            c.drawShadow(x, y + s * 0.03f, s, s, r, new Shadow(0, s * 0.04f, s * 0.12f, 0, 0x70000000));
            c.drawGlow(x, y, s, s, r, new Shadow(0, 0, s * 0.18f, 0, 0x408B5CF6));
        }
        c.fillRoundRect(x, y, s, s, r, Brush.linear(135f, BLUE, VIOLET, PINK));
        c.fillRoundRect(x, y, s, s, r, Brush.radial(0.25f, 0.15f, 0.9f, 0x55FFFFFF, 0x00FFFFFF));
        c.strokeRoundRect(x, y, s, s, r, Math.max(1f, s * 0.015f), 0x40FFFFFF);
        // L: вертикальная и горизонтальная полосы с общей тенью
        float bw = s * 0.15f;
        float lx = x + s * 0.27f;
        float ly = y + s * 0.20f;
        float lh = s * 0.58f;
        float lw = s * 0.44f;
        Radii br = Radii.all(bw * 0.5f);
        Shadow bs = new Shadow(0, s * 0.02f, s * 0.05f, 0, 0x50200838);
        c.drawShadow(lx, ly, bw, lh, br, bs);
        c.drawShadow(lx, ly + lh - bw, lw, bw, br, bs);
        Brush white = Brush.vertical(0xFFFFFFFF, 0xFFEDE7FF);
        c.fillRoundRect(lx, ly, bw, lh, br, white);
        c.fillRoundRect(lx, ly + lh - bw, lw, bw, br, white);
        // искра: четырёхлучевая звезда со свечением
        paintSpark(c, x + s * 0.71f, y + s * 0.31f, s * 0.13f, 0xFFFFFFFF);
    }

    static void paintSpark(Canvas c, float cx, float cy, float r, int color) {
        c.drawGlow(cx - r, cy - r, 2 * r, 2 * r, Radii.all(r), new Shadow(0, 0, r * 1.2f, 0, Color.withAlpha(color, 0x90)));
        float w = r * 0.26f;
        c.fillQuad(cx, cy - r, cx + w, cy, cx, cy + r, cx - w, cy, color, color, color, color);
        c.fillQuad(cx - r, cy, cx, cy - w, cx + r, cy, cx, cy + w, color, color, color, color);
    }

    /** Тёмная стеклянная карточка с тенью. */
    static Panel card(float w, float h) {
        Panel p = new Panel(null, FlexLayout.column().gap(5));
        if (w > 0) {
            p.width(Dim.px(w));
        }
        if (h > 0) {
            p.height(Dim.px(h));
        }
        return p.style(b -> b
                .set(Styles.BACKGROUND, Brush.vertical(0xF6222839, 0xF61A1F2E))
                .set(Styles.RADIUS, Radii.all(10))
                .set(Styles.BORDER_WIDTH, 1f)
                .set(Styles.BORDER_COLOR, 0x24FFFFFF)
                .set(Styles.PADDING, Insets.all(8))
                .set(Styles.SHADOW, new Shadow(0, 8, 22, 0, 0x90000000)));
    }

    static Label label(String text, int color) {
        return new Label(text).style(b -> b.set(Styles.TEXT_COLOR, color));
    }

    static Label title(String text) {
        return label(text, TEXT);
    }

    static Label muted(String text) {
        return label(text, MUTED);
    }

    static Panel row(float gap, UiElement... children) {
        return Panel.box(FlexLayout.row().gap(gap).align(Align.CENTER)).add(children);
    }

    static Panel column(float gap, UiElement... children) {
        return Panel.box(FlexLayout.column().gap(gap)).add(children);
    }

    static Panel spacer() {
        return Panel.box(FlexLayout.column()).grow(1);
    }

    /** Пилюля-метка: цветная точка и текст. */
    static Panel chip(String text, int color) {
        Panel dot = Panel.box(FlexLayout.column()).size(5, 5)
                .style(b -> b.set(Styles.BACKGROUND, Brush.solid(color)).set(Styles.RADIUS, Radii.all(2.5f))
                        .set(Styles.GLOW, new Shadow(0, 0, 4, 0, Color.withAlpha(color, 0xA0))));
        return row(5, dot, label(text, TEXT)).style(b -> b
                .set(Styles.BACKGROUND, Brush.solid(0x26FFFFFF))
                .set(Styles.BORDER_WIDTH, 1f)
                .set(Styles.BORDER_COLOR, Color.withAlpha(color, 0x70))
                .set(Styles.RADIUS, Radii.all(9))
                .set(Styles.PADDING, new Insets(4, 8, 4, 7)));
    }

    /** Ряд ячеек инвентаря с предметами. */
    static Sketch hotbar(String... items) {
        float cell = 20f;
        return new Sketch(items.length * cell + 2, cell + 2, (c, w, h, time) -> {
            for (int i = 0; i < items.length; i++) {
                float x = 1 + i * cell;
                c.fillRoundRect(x + 1, 1, cell - 2, cell - 2, Radii.all(4), Brush.vertical(0xFF2C3346, 0xFF232838));
                c.strokeRoundRect(x + 1, 1, cell - 2, cell - 2, Radii.all(4), 1f, i == 1 ? BLUE : 0x30FFFFFF);
                if (i == 1) {
                    c.drawGlow(x + 1, 1, cell - 2, cell - 2, Radii.all(4), new Shadow(0, 0, 6, 0, 0x705B8CFF));
                }
                String id = items[i];
                boolean glint = id.startsWith("!");
                c.drawItem(glint ? id.substring(1) : id, x + 3, 3, 16, glint);
            }
        });
    }

    /** Вся картинка: прозрачный фон и элементы по абсолютным координатам. */
    static Panel canvas(UiElement... children) {
        Panel p = Panel.box(new StackLayout(Align.START, Align.START)).fill();
        return p.add(children);
    }

    static UiRoot root(UiElement content) {
        return new UiRoot(content, theme());
    }

    private static void press(UiRoot root, String en, String russian) {
        Button b = findButton(root.content(), en, russian);
        if (b != null) {
            b.press();
        }
    }

    private static Button findButton(UiElement e, String en, String russian) {
        if (e instanceof Button b && (en.equals(b.text()) || russian.equals(b.text()))) {
            return b;
        }
        for (UiElement c : e.children()) {
            Button found = findButton(c, en, russian);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    // ================================================================ мини-карточки

    static Panel settingsCard(float w) {
        Signal<Boolean> a = Signal.of(true);
        Signal<Boolean> b = Signal.of(false);
        Signal<Float> volume = Signal.of(0.68f);
        Panel card = card(w, 0);
        card.add(row(6, title(t("Settings", "Настройки")), spacer(), muted("v1.0")).width(Dim.FILL),
                row(6, label(t("Shaders", "Шейдеры"), TEXT), spacer(), new Toggle(a)).width(Dim.FILL),
                row(6, label(t("Particles", "Частицы"), TEXT), spacer(), new Toggle(b)).width(Dim.FILL),
                row(10, label(t("Volume", "Звук"), TEXT), new Slider(volume, 0, 1).grow(1)).width(Dim.FILL),
                row(10, new Checkbox(t("Vsync", "V-Sync"), Signal.of(true)), new Checkbox(t("HUD", "HUD"), Signal.of(false))));
        return card;
    }

    static Panel chartCard(float w, float h) {
        Chart chart = new Chart(Chart.Kind.AREA)
                .series("fps", CYAN, 38, 52, 47, 61, 58, 72, 66, 80, 77, 92)
                .grid(2).legend(false).max(100).settle();
        chart.width(Dim.FILL).grow(1);
        Panel card = card(w, h);
        card.add(row(6, title("FPS"), spacer(), label("92", CYAN)).width(Dim.FILL), chart);
        return card;
    }

    static Panel ringCard() {
        RingProgress ring = new RingProgress().progress(0.72f).thickness(5).colors(VIOLET, PINK).showPercent(true);
        ring.size(46, 46);
        ring.style(b -> b.set(Styles.BACKGROUND, Brush.solid(0)));
        Panel card = card(0, 0);
        card.add(column(4, ring, muted(t("Loading", "Загрузка"))).align(Align.CENTER));
        return card;
    }

    static Panel buttonsCard() {
        Panel card = card(0, 0);
        card.add(row(5,
                        new Button(t("Play", "Играть")).styleClass("button pill"),
                        new Button(t("Outline", "Контур")).styleClass("button ghost")),
                row(5,
                        new Button(t("Delete", "Удалить")).styleClass("button danger"),
                        new Button(t("Save", "Сохранить")).styleClass("button success")));
        return card;
    }

    static Panel inventoryCard() {
        Panel card = card(0, 0);
        card.add(row(6, title(t("Hotbar", "Хотбар")), spacer(), muted("6 / 9")).width(Dim.FILL),
                hotbar("minecraft:diamond_sword", "minecraft:grass_block", "minecraft:golden_apple",
                        "minecraft:tnt", "!minecraft:enchanted_book", "minecraft:ender_pearl"));
        return card;
    }

    static Panel searchCard(float w) {
        TextField field = new TextField(Signal.of(t("diamond", "алмаз")));
        field.width(Dim.FILL);
        ProgressBar bar = new ProgressBar().progress(0.62f);
        bar.width(Dim.FILL);
        Panel card = card(w, 0);
        card.add(title(t("Search", "Поиск")), field,
                row(6, muted(t("Indexing", "Индексация")), spacer(), muted("62%")).width(Dim.FILL), bar);
        return card;
    }

    static Panel gradientCard() {
        Sketch swatches = new Sketch(118, 22, (c, w, h, time) -> {
            c.fillRoundRect(0, 0, 22, 22, Radii.all(6), Brush.linear(135f, 0xFFFF8A5B, 0xFFFFD25B));
            c.fillRoundRect(24, 0, 22, 22, Radii.all(11), Brush.radial(0xFFFFFFFF, CYAN, 0xFF2B59C3));
            c.fillRoundRect(48, 0, 22, 22, Radii.all(6), Brush.conic(0f, BLUE, VIOLET, PINK, 0xFFFFD25B, BLUE));
            c.fillRoundRect(72, 0, 22, 22, Radii.all(6), Brush.rainbow(90f));
            c.fillRoundRect(96, 0, 22, 22, Radii.all(6), Brush.linear(160f, 0xFF34C38F, 0xFF38D6E8));
        });
        Panel card = card(0, 0);
        card.add(title(t("Gradients", "Градиенты")), swatches);
        return card;
    }

    static FancyText brandTitle(float scale) {
        return new FancyText("Lumina UI").scale(scale).gradient(0xFF8FB0FF, 0xFFC4A5FF, 0xFFFF9AD1);
    }

    // ================================================================ иконка

    static UiRoot icon() {
        Sketch mark = new Sketch(128, 128, (c, w, h, time) -> paintMark(c, 0, 0, 128, false));
        return root(canvas(mark));
    }

    // ================================================================ баннер 1920×480

    static UiRoot banner() {
        // Подложка: тёмное стекло с полосой света по краю.
        Sketch back = new Sketch(640, 160, (c, w, h, time) -> {
            float x = 6, y = 8, bw = 628, bh = 144;
            Radii r = Radii.all(18);
            c.drawShadow(x, y, bw, bh, r, new Shadow(0, 6, 18, 0, 0x60000000));
            c.fillRoundRect(x, y, bw, bh, r, Brush.linear(100f, 0xFA141826, 0xFA1B2034, 0xFA161A29));
            c.save();
            c.clipRoundRect(x, y, bw, bh, r);
            c.fillRect(x - 60, y - 80, 300, 260, Brush.radial(0.5f, 0.5f, 0.5f, 0x405B8CFF, 0x005B8CFF));
            c.fillRect(x + 380, y - 70, 320, 280, Brush.radial(0.5f, 0.5f, 0.5f, 0x38EC4899, 0x00EC4899));
            c.fillRect(x + 220, y + 60, 260, 200, Brush.radial(0.5f, 0.5f, 0.5f, 0x308B5CF6, 0x008B5CF6));
            // сетка точек
            for (float gy = y + 10; gy < y + bh; gy += 12) {
                for (float gx = x + 10; gx < x + bw; gx += 12) {
                    c.fillRect(gx, gy, 1, 1, 0x14FFFFFF);
                }
            }
            c.restore();
            c.strokeRoundRect(x, y, bw, bh, r, 1f, Brush.linear(90f, 0x805B8CFF, 0x408B5CF6, 0x80EC4899));
            paintMark(c, 26, 46, 68, true);
        });

        Panel chips = row(5,
                chip("Forge 1.20.1", 0xFFE0A34A),
                chip("NeoForge 1.21.1", 0xFFFF7A45),
                chip("HTML / CSS", CYAN));
        Panel text = column(6,
                brandTitle(4f),
                label(t("Beautiful, fast and flexible GUI library", "Красивая и быстрая GUI-библиотека"), TEXT).scale(1.25f),
                muted(t("for Minecraft mods: one UI code for every loader", "для модов Minecraft на Forge и NeoForge")),
                Panel.box(FlexLayout.column()).size(1, 2),
                chips).absolute(108, 38);

        Panel settings = settingsCard(120);
        settings.absolute(384, 20);
        Panel chart = chartCard(110, 70);
        chart.absolute(514, 26);
        Panel inv = card(0, 0);
        inv.style(b -> b.set(Styles.PADDING, Insets.all(6)));
        inv.add(hotbar("minecraft:diamond_sword", "minecraft:grass_block", "minecraft:golden_apple",
                "minecraft:tnt", "!minecraft:enchanted_book", "minecraft:ender_pearl"));
        inv.absolute(494, 100);
        return root(canvas(back, text, settings, chart, inv));
    }

    // ================================================================ обложка галереи 1920×1080

    static Sketch backdrop(float w, float h) {
        return new Sketch(w, h, (c, ww, hh, time) -> {
            c.fillRect(0, 0, w, h, Brush.linear(160f, 0xFF0F1220, 0xFF151A2C, 0xFF120F22));
            c.fillRect(-200, -260, 760, 760, Brush.radial(0.5f, 0.5f, 0.5f, 0x445B8CFF, 0x005B8CFF));
            c.fillRect(w - 520, -180, 760, 760, Brush.radial(0.5f, 0.5f, 0.5f, 0x3AEC4899, 0x00EC4899));
            c.fillRect(w * 0.35f, h - 300, 700, 600, Brush.radial(0.5f, 0.5f, 0.5f, 0x308B5CF6, 0x008B5CF6));
            for (float gy = 12; gy < h; gy += 16) {
                for (float gx = 12; gx < w; gx += 16) {
                    c.fillRect(gx, gy, 1, 1, 0x12FFFFFF);
                }
            }
        });
    }

    static UiRoot hero() {
        Sketch back = backdrop(960, 540);
        Sketch mark = new Sketch(120, 120, (c, w, h, time) -> paintMark(c, 6, 6, 108, true));
        mark.absolute(64, 92);

        Panel features = column(7,
                feature(BLUE, t("Flexbox layout, themes and CSS-like styles", "Flex-раскладка, темы и стили как в CSS")),
                feature(VIOLET, t("SDF shapes, gradients, shadows and blur", "SDF-фигуры, градиенты, тени и размытие")),
                feature(PINK, t("Tweens, springs, keyframes and vanilla FX", "Твины, пружины, ключевые кадры и ванильные эффекты")),
                feature(CYAN, t("Screens in HTML and CSS, Markdown, i18n", "Экраны на HTML и CSS, Markdown, переводы")),
                feature(0xFFE0A34A, t("Items, inventories, blocks and 3D builds", "Предметы, инвентари, блоки и 3D-постройки")));
        Panel text = column(8,
                brandTitle(6f),
                label(t("A GUI library for Minecraft mods", "GUI-библиотека для модов Minecraft"), TEXT).scale(1.6f),
                Panel.box(FlexLayout.column()).size(1, 8),
                features,
                Panel.box(FlexLayout.column()).size(1, 10),
                row(6, chip("Forge 1.20.1", 0xFFE0A34A), chip("NeoForge 1.21.1", 0xFFFF7A45))).absolute(64, 226);

        Panel settings = settingsCard(170);
        settings.absolute(560, 70);
        Panel chart = chartCard(170, 104);
        chart.absolute(746, 92);
        Panel ring = ringCard();
        ring.absolute(760, 212);
        Panel buttons = buttonsCard();
        buttons.absolute(530, 222);
        Panel inv = inventoryCard();
        inv.absolute(580, 300);
        Panel grad = gradientCard();
        grad.absolute(752, 300);
        Panel search = searchCard(200);
        search.absolute(610, 380);

        return root(canvas(back, mark, text, settings, chart, buttons, ring, grad, inv, search));
    }

    static Panel feature(int color, String text) {
        Panel dot = Panel.box(FlexLayout.column()).size(6, 6)
                .style(b -> b.set(Styles.BACKGROUND, Brush.solid(color)).set(Styles.RADIUS, Radii.all(3))
                        .set(Styles.GLOW, new Shadow(0, 0, 6, 0, Color.withAlpha(color, 0xB0))));
        return row(8, dot, label(text, TEXT).scale(1.25f));
    }

    // ================================================================ кадры галереи

    static Art gallery(String name, String heading, String hint, Supplier<UiElement> page, Consumer<UiRoot> prepare) {
        return new Art(name, 768, 432, 2.5f, () -> galleryScene(heading, hint, page.get()), 1.6f, prepare);
    }

    static UiRoot galleryScene(String heading, String hint, UiElement page) {
        Sketch back = backdrop(768, 432);
        Sketch mark = new Sketch(36, 36, (c, w, h, time) -> paintMark(c, 2, 2, 32, true));
        Panel head = row(10, mark,
                column(3,
                        new FancyText(heading).scale(2f).gradient(0xFFFFFFFF, 0xFFD7DEFF, 0xFFFFC9E6),
                        label(hint, MUTED))).absolute(24, 16);
        Panel brand = row(6, label("Lumina UI", MUTED)).absolute(694, 22);

        float wx = 24, wy = 70, ww = 720, wh = 346;
        Sketch frame = new Sketch(ww, wh, (c, w, h, time) -> {
            Radii r = Radii.all(14);
            c.drawShadow(0, 0, w, h, r, new Shadow(0, 14, 36, 0, 0xA0000000));
            c.fillRoundRect(0, 0, w, h, r, Brush.vertical(0xFA1E2333, 0xFA171B28));
            c.fillRoundRect(0, 0, w, 22, Radii.top(14), Brush.solid(0x0CFFFFFF));
            c.fillRect(0, 22, w, 1, 0x18FFFFFF);
            c.strokeRoundRect(0, 0, w, h, r, 1f, Brush.linear(90f, 0x705B8CFF, 0x308B5CF6, 0x70EC4899));
            int[] dots = {0xFFFF5F57, 0xFFFEBC2E, 0xFF28C840};
            for (int i = 0; i < 3; i++) {
                c.fillCircle(14 + i * 12, 11, 3.5f, Brush.solid(dots[i]));
            }
        });
        frame.absolute(wx, wy);
        Panel content = Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH)).size(ww - 24, wh - 36);
        content.setClipChildren(true);
        content.add(page);
        content.absolute(wx + 12, wy + 30);
        return root(canvas(back, head, brand, frame, content));
    }
}
