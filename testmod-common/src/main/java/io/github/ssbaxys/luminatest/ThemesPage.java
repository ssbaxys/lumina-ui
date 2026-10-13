package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.a11y.AccessibleThemes;
import io.github.ssbaxys.lumina.a11y.Accessibility;
import io.github.ssbaxys.lumina.a11y.ColorVision;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Breakpoint;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markdown.Markdown;
import io.github.ssbaxys.lumina.markup.ThemeCss;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.WorldInfo;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.sound.SoundTheme;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.theme.ThemeSchedule;
import io.github.ssbaxys.lumina.theme.ThemeTransition;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Checkbox;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.RadioGroup;
import io.github.ssbaxys.lumina.widget.ResponsivePanel;
import io.github.ssbaxys.lumina.widget.Slider;
import io.github.ssbaxys.lumina.widget.ThemeEditor;
import io.github.ssbaxys.lumina.widget.Toggle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Темы, доступность, звуки и адаптивность: редактор темы с экспортом и импортом CSS, автосмена темы
 * по времени суток и биому, режимы для дальтоников и крупный текст, наборы звуков, раскладка по ширине
 * экрана и масштаб интерфейса. Пока страница открыта, она управляет темой корня; при уходе тема и масштаб
 * возвращаются, а настройки доступности остаются (их удобно проверить и на других страницах).
 */
final class ThemesPage {
    private static final Map<ColorVision, String> VISION_NAMES = Map.of(
            ColorVision.NORMAL, "Обычное",
            ColorVision.PROTANOPIA, "Протанопия",
            ColorVision.DEUTERANOPIA, "Дейтеранопия",
            ColorVision.TRITANOPIA, "Тританопия");
    private static final Map<String, String> SOUND_NAMES = Map.of(
            "vanilla", "Ванильные", "soft", "Мягкие", "retro", "Ретро", "none", "Без звука");
    private static final Map<String, String> BIOME_NAMES = Map.of(
            "minecraft:plains", "Равнины", "minecraft:desert", "Пустыня",
            "minecraft:snowy_plains", "Снежные равнины", "minecraft:nether_wastes", "Незер");

    private ThemesPage() {
    }

    /** Состояние страницы: всё, что меняет корень, собрано здесь и откатывается при уходе. */
    private static final class Controller {
        final Signal<String> mode = Signal.of("off");
        final Signal<Float> time = Signal.of(6000f);
        final Signal<String> biome = Signal.of("minecraft:plains");
        final Signal<String> sound = Signal.of("vanilla");
        final Signal<Boolean> highContrast = Signal.of(false);
        final Signal<Boolean> largeText = Signal.of(false);
        final Signal<Boolean> reducedMotion = Signal.of(false);
        final Signal<Boolean> focusRing = Signal.of(false);
        final Signal<Boolean> narration = Signal.of(false);
        final Signal<Boolean> simulate = Signal.of(false);
        final Signal<ColorVision> vision = Signal.of(ColorVision.NORMAL);
        final Signal<Float> scale = Signal.of(1f);
        final Label timeLabel = new Label("");
        final Label scheduleLabel = Ui.muted("");
        final Label breakpointLabel = new Label("");

        final ThemeSchedule byTime = ThemeSchedule.byTime(() -> time.peek())
                .at(0f, ShowcaseTheme.light())
                .at(11500f, ShowcaseTheme.dark().set(Tokens.ACCENT, 0xFFFF8A3D))
                .at(13500f, ShowcaseTheme.dark())
                .at(22500f, ShowcaseTheme.dark().set(Tokens.ACCENT, 0xFFF0A0C8))
                .blend(1200f)
                .build()
                .transition(0.8f)
                .poll(0.1f);
        final ThemeSchedule byBiome = ThemeSchedule.byBiome(() -> biome.peek())
                .when("minecraft:desert", ShowcaseTheme.light().set(Tokens.ACCENT, 0xFFC98B2B))
                .when(ThemeSchedule.contains("snow"), ShowcaseTheme.light().set(Tokens.ACCENT, 0xFF3D9BE0))
                .when(ThemeSchedule.contains("nether"), ShowcaseTheme.dark().set(Tokens.ACCENT, 0xFFE5484D))
                .otherwise(ShowcaseTheme.dark().set(Tokens.ACCENT, 0xFF34C38F))
                .build()
                .transition(1.2f)
                .poll(0.1f);

        UiRoot root;
        Theme previous;
        Theme beforeContrast;
        float previousScale = 1f;
        Subscription schedule;
        final List<Subscription> subscriptions = new ArrayList<>();

        Controller() {
            byTime.decorate(this::decorate);
            byBiome.decorate(this::decorate);
        }

        /** Звуки и высокий контраст поверх темы из расписания. */
        Theme decorate(Theme t) {
            Theme out = highContrast.peek() ? AccessibleThemes.highContrast(t) : t;
            return soundTheme().applyTo(out, "nav", "card");
        }

        SoundTheme soundTheme() {
            SoundTheme s = SoundTheme.byName(sound.peek());
            return s != null ? s : SoundTheme.VANILLA;
        }

        void attach(UiRoot r) {
            root = r;
            previous = r.theme();
            previousScale = r.scale();
            Accessibility a = r.accessibility();
            largeText.set(a.textScale() > 1f);
            reducedMotion.set(a.reducedMotion());
            focusRing.set(a.focusAlwaysVisible());
            narration.set(a.narration());
            vision.set(a.colorVision());
            simulate.set(a.colorMode() == Accessibility.ColorMode.SIMULATE);
            subscriptions.add(r.breakpoint().subscribe(this::showBreakpoint));
            showBreakpoint(r.currentBreakpoint());
            applyMode();
        }

        void detach() {
            for (Subscription s : subscriptions) {
                s.close();
            }
            subscriptions.clear();
            stopSchedule();
            if (root != null) {
                root.setScale(previousScale);
                root.setTheme(previous);
            }
            root = null;
        }

        void showBreakpoint(Breakpoint bp) {
            String name = switch (bp) {
                case COMPACT -> Tr.r("узкий");
                case MEDIUM -> Tr.r("средний");
                case WIDE -> Tr.r("широкий");
            };
            breakpointLabel.text(Tr.r("Ширина интерфейса: ") + (root == null ? 0 : Math.round(root.width()))
                    + Tr.r(" — класс «") + name + "»");
        }

        void stopSchedule() {
            if (schedule != null) {
                schedule.close();
                schedule = null;
            }
        }

        void applyMode() {
            if (root == null) {
                return;
            }
            stopSchedule();
            switch (mode.peek()) {
                case "time" -> {
                    schedule = byTime.attach(root);
                    scheduleLabel.text(Tr.r("Тема выбирается по ползунку времени; на закате и рассвете цвета смешиваются"));
                }
                case "biome" -> {
                    schedule = byBiome.attach(root);
                    scheduleLabel.text(Tr.r("Тема выбирается по биому"));
                }
                default -> {
                    scheduleLabel.text(Tr.r("Автосмена выключена"));
                    beforeContrast = null;
                    ThemeTransition.to(root, decorate(previous.copy()), 0.5f);
                }
            }
        }

        /** Звуки или контраст поменялись: перестроить тему. */
        void redecorate() {
            if (root == null) {
                return;
            }
            if (schedule != null) {
                byTime.decorate(this::decorate);
                byBiome.decorate(this::decorate);
                return;
            }
            if (highContrast.peek()) {
                if (beforeContrast == null) {
                    beforeContrast = root.theme();
                }
                ThemeTransition.to(root, decorate(beforeContrast.copy()), 0.4f);
            } else if (beforeContrast != null) {
                Theme back = soundTheme().applyTo(beforeContrast.copy(), "nav", "card");
                beforeContrast = null;
                ThemeTransition.to(root, back, 0.4f);
            } else {
                // Только звуки: на месте, правки редактора сохраняются.
                soundTheme().applyTo(root.theme(), "nav", "card");
            }
        }

        void applyAccessibility() {
            if (root == null) {
                return;
            }
            root.accessibility()
                    .textScale(largeText.peek() ? 1.25f : 1f)
                    .reducedMotion(reducedMotion.peek())
                    .focusAlwaysVisible(focusRing.peek())
                    .narration(narration.peek())
                    .colorVision(vision.peek(), simulate.peek() ? Accessibility.ColorMode.SIMULATE : Accessibility.ColorMode.CORRECT);
        }
    }

    static UiElement create() {
        Controller c = new Controller();

        Panel content = new Panel(null, FlexLayout.column().gap(16).align(Align.STRETCH)) {
            @Override
            protected void onAttach() {
                c.attach(root());
            }

            @Override
            protected void onDetach() {
                c.detach();
            }
        };
        content.width(Dim.FILL);
        content.add(editorSection(), exportSection(c), scheduleSection(c), accessibilitySection(c),
                soundSection(c), responsiveSection(c));

        c.mode.subscribe(v -> c.applyMode());
        c.time.subscribe(v -> c.timeLabel.text(Tr.r("Время: ") + WorldInfo.formatTime(v) + " (" + Math.round(v) + Tr.r(" тиков)")));
        c.timeLabel.text(Tr.r("Время: ") + WorldInfo.formatTime(c.time.peek()) + " (" + Math.round(c.time.peek()) + Tr.r(" тиков)"));
        c.sound.subscribe(v -> c.redecorate());
        c.highContrast.subscribe(v -> c.redecorate());
        c.largeText.subscribe(v -> c.applyAccessibility());
        c.reducedMotion.subscribe(v -> c.applyAccessibility());
        c.focusRing.subscribe(v -> c.applyAccessibility());
        c.narration.subscribe(v -> c.applyAccessibility());
        c.vision.subscribe(v -> c.applyAccessibility());
        c.simulate.subscribe(v -> c.applyAccessibility());
        c.scale.subscribe(v -> {
            if (c.root != null) {
                c.root.setScale(v);
            }
        });
        return Ui.page(content);
    }

    // ---------------------------------------------------------------- разделы

    private static Panel editorSection() {
        return Ui.section(Tr.r("Редактор темы"),
                Tr.r("Цвета, скругление и скорость меняют тему всей витрины на лету"),
                new ThemeEditor().fileName("showcase.css").baseName("dark").id("editor").width(Dim.FILL));
    }

    private static Panel exportSection(Controller c) {
        Markdown code = new Markdown("").width(Dim.FILL);
        code.setVisible(false);
        Label check = Ui.muted("");
        Button show = new Button(Tr.r("Показать CSS"), () -> {
            if (c.root == null) {
                return;
            }
            String css = ThemeCss.export(c.root.theme(), "dark");
            String[] lines = css.split("\n");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < Math.min(lines.length, 18); i++) {
                sb.append(lines[i]).append('\n');
            }
            if (lines.length > 18) {
                sb.append(Tr.r("/* … ещё ")).append(lines.length - 18).append(Tr.r(" строк */\n"));
            }
            code.text("```css\n" + sb + "```");
            code.setVisible(true);
        }).id("show-css");
        Button roundTrip = new Button(Tr.r("Проверить: экспорт → импорт"), () -> {
            if (c.root == null) {
                return;
            }
            Theme theme = c.root.theme();
            Theme back = ThemeCss.load(ThemeCss.export(theme, "dark"));
            boolean same = back.get(Tokens.ACCENT).equals(theme.get(Tokens.ACCENT))
                    && back.get(Tokens.CORNER).equals(theme.get(Tokens.CORNER));
            check.text(same ? Tr.r("Тема прочитана обратно без потерь") : Tr.r("Токены отличаются"));
        }).styleClass("button outline").id("round-trip");
        return Ui.section(Tr.r("Экспорт и импорт"),
                Tr.r("Тема сохраняется в CSS: токены в :root, стили классов — обычными правилами"),
                Ui.row(8, show, roundTrip), check, code);
    }

    private static Panel scheduleSection(Controller c) {
        RadioGroup<String> mode = RadioGroup.of(c.mode, "off", "time", "biome")
                .labels(v -> switch (v) {
                    case "time" -> Tr.r("По времени суток");
                    case "biome" -> Tr.r("По биому");
                    default -> Tr.r("Выключено");
                })
                .horizontal();
        Slider time = new Slider(c.time, 0f, 24000f).step(250f).width(Dim.px(220)).id("time-slider");
        RadioGroup<String> biome = RadioGroup.of(c.biome, "minecraft:plains", "minecraft:desert",
                        "minecraft:snowy_plains", "minecraft:nether_wastes")
                .labels(k -> Tr.r(BIOME_NAMES.get(k)))
                .horizontal();
        return Ui.section(Tr.r("Автосмена темы"),
                Tr.r("В игре время и биом берутся из мира (WorldInfo); здесь их заменяют ползунок и переключатель"),
                mode, Ui.row(8, time, c.timeLabel), biome, c.scheduleLabel);
    }

    private static Panel accessibilitySection(Controller c) {
        RadioGroup<ColorVision> vision = RadioGroup.of(c.vision, ColorVision.values())
                .labels(k -> Tr.r(VISION_NAMES.get(k)))
                .horizontal();
        Panel swatches = Ui.row(6,
                swatch(0xFFE5484D), swatch(0xFF34C38F), swatch(0xFF3D6DF2), swatch(0xFFF5A524), swatch(0xFFA855F7));
        return Ui.section(Tr.r("Доступность"),
                Tr.r("Действует на весь интерфейс поверх любой темы"),
                Ui.grid(2, 8,
                        toggle(Tr.r("Высокий контраст"), c.highContrast),
                        toggle(Tr.r("Крупный текст"), c.largeText),
                        toggle(Tr.r("Без анимаций"), c.reducedMotion),
                        toggle(Tr.r("Рамка фокуса всегда"), c.focusRing),
                        toggle(Tr.r("Озвучивать фокус"), c.narration),
                        toggle(Tr.r("Симуляция вместо коррекции"), c.simulate)),
                Ui.muted(Tr.r("Цветовое зрение:")), vision, swatches,
                Ui.muted(Tr.r("Озвучка идёт через Platform.narrate: загрузчик передаёт текст диктору игры")));
    }

    private static Panel soundSection(Controller c) {
        RadioGroup<String> sounds = RadioGroup.of(c.sound, "vanilla", "soft", "retro", "none")
                .labels(k -> Tr.r(SOUND_NAMES.get(k)))
                .horizontal();
        return Ui.section(Tr.r("Звуки интерфейса"),
                Tr.r("Звук наведения играет при входе мыши в кнопку и не чаще раза в 70 мс"),
                sounds,
                Ui.row(8, new Button(Tr.r("Наведи")), new Button(Tr.r("И сюда")).styleClass("button outline"),
                        new Toggle(Signal.of(false)), new Checkbox(Tr.r("Флажок"), Signal.of(true))));
    }

    private static Panel responsiveSection(Controller c) {
        ResponsivePanel rowOrColumn = ResponsivePanel.rowFrom(Breakpoint.MEDIUM, 8)
                .add(card(Tr.r("Меню"), 0xFF5B8CFF).grow(1), card(Tr.r("Содержимое"), 0xFF34C38F).grow(2), card(Tr.r("Детали"), 0xFFF5A524).grow(1))
                .id("row-or-column");
        rowOrColumn.width(Dim.FILL);
        ResponsivePanel grid = ResponsivePanel.columns(1, 2, 3, 6).id("grid");
        for (int i = 1; i <= 6; i++) {
            grid.add(card(Tr.r("Карточка ") + i, i % 2 == 0 ? 0xFFA855F7 : 0xFF22D3EE));
        }
        grid.width(Dim.FILL);
        ResponsivePanel own = ResponsivePanel.columns(1, 2, 3, 4).byContainerWidth(true).id("own-width");
        for (int i = 1; i <= 3; i++) {
            own.add(card(Tr.r("По своей ширине ") + i, 0xFFF0506E));
        }
        own.width(Dim.FILL);
        Panel narrow = Panel.box(FlexLayout.column()).width(Dim.px(220)).add(own);
        RadioGroup<Float> scale = RadioGroup.of(c.scale, 0.75f, 1f, 1.25f, 1.5f)
                .labels(v -> v + "×")
                .horizontal();
        return Ui.section(Tr.r("Адаптивная раскладка"),
                Tr.r("Строка на широком экране и колонка на узком; сетка 1/2/3 колонки; масштаб всего интерфейса"),
                c.breakpointLabel, rowOrColumn, grid, Ui.muted(Tr.r("Узкий блок (220 пикселей) — раскладка по своей ширине:")),
                narrow, Ui.row(8, Ui.muted(Tr.r("Масштаб интерфейса:")), scale));
    }

    // ---------------------------------------------------------------- мелочи

    private static Panel toggle(String title, Signal<Boolean> value) {
        return Ui.row(6, new Toggle(value), new Label(title));
    }

    private static Panel swatch(int color) {
        return Ui.box(color, 28, 18);
    }

    private static Panel card(String title, int color) {
        Panel p = Panel.box(FlexLayout.column().align(Align.START)).add(new Label(title));
        p.style(b -> b
                .set(Styles.BACKGROUND, Brush.vertical(color, 0xC0000000 | (color & 0xFFFFFF)))
                .set(Styles.RADIUS, Radii.all(6))
                .set(Styles.PADDING, Insets.of(8, 8))
                .set(Styles.TEXT_COLOR, 0xFFFFFFFF));
        return p;
    }
}
