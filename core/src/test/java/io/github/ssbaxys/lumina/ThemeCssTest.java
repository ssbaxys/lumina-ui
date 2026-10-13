package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.markup.Stylesheet;
import io.github.ssbaxys.lumina.markup.ThemeCss;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.ColorStops;
import io.github.ssbaxys.lumina.render.ColorSpace;
import io.github.ssbaxys.lumina.render.Outline;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.sound.SoundTheme;
import io.github.ssbaxys.lumina.sound.UiSound;
import io.github.ssbaxys.lumina.style.ModernTheme;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.ThemeVar;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.style.Transition;
import io.github.ssbaxys.lumina.a11y.AccessibleThemes;
import io.github.ssbaxys.lumina.platform.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Тема → CSS → тема: токены и стили классов возвращаются без потерь; файлы через платформу. */
class ThemeCssTest {
    @org.junit.jupiter.api.BeforeEach
    void russianTexts() {
        io.github.ssbaxys.lumina.i18n.I18n.setLanguage("ru_ru");
    }

    private CapturePlatform platform;

    @BeforeEach
    void init() {
        platform = CapturePlatform.install();
    }

    /** Все токены и все разрешённые стили всех классов во всех состояниях совпадают. */
    private static void assertSameTheme(Theme expected, Theme actual) {
        for (ThemeVar<?> v : List.of(Tokens.ACCENT, Tokens.TEXT, Tokens.SURFACE, Tokens.SURFACE_ALT, Tokens.BORDER,
                Tokens.SHADOW, Tokens.WELL, Tokens.CORNER, Tokens.MOTION)) {
            assertEquals(expected.get(v), actual.get(v), "token " + v.name());
        }
        Set<String> classes = new TreeSet<>(expected.styleClasses());
        classes.addAll(actual.styleClasses());
        for (String cls : classes) {
            for (int state = 0; state < 32; state++) {
                Object[] a = expected.style(cls).resolve(state);
                Object[] b = actual.style(cls).resolve(state);
                int len = Math.max(a.length, b.length);
                assertTrue(Arrays.equals(Arrays.copyOf(a, len), Arrays.copyOf(b, len)),
                        "class " + cls + " state " + state);
            }
        }
    }

    private static Theme edited() {
        Theme t = Theme.dark()
                .set(Tokens.ACCENT, 0xFFF0506E)
                .set(Tokens.CORNER, 4.5f)
                .set(Tokens.MOTION, 0.5f)
                .edit("button", b -> b.set(Styles.PADDING, Insets.of(3, 9)).hovered(h -> h.set(Styles.SCALE, 1.05f)))
                .put("card", Style.builder()
                        .set(Styles.BACKGROUND, Brush.linear(135, 0xFF22D3EE, 0xFF34C38F, 0xFFA855F7))
                        .set(Styles.RADIUS, new Radii(1, 2, 3, 4))
                        .set(Styles.SHADOW, new Shadow(1, 4, 12, -1, 0x80000000))
                        .set(Styles.GLOW, new Shadow(0, 0, 8, 1, 0x605B8CFF))
                        .set(Styles.OUTLINE, Outline.of(1.5f, 2f, 0xFFFFFFFF))
                        .set(Styles.TEXT_SHADOW, true)
                        .set(Styles.TRANSITION, Transition.of(0.25f, Easing.OUT_BACK).key(Styles.SCALE, 0.4f, Easing.OUT_ELASTIC, 0.1f))
                        .bind(Styles.BORDER_COLOR, Tokens.ACCENT)
                        .pressed(p -> p.set(Styles.SCALE, 0.95f).set(Styles.BACKGROUND, Brush.radial(0xFFFFFFFF, 0x00FFFFFF)))
                        .focused(f -> f.set(Styles.BORDER_WIDTH, 2f))
                        .disabled(d -> d.set(Styles.OPACITY, 0.3f))
                        .checked(c -> c.set(Styles.BACKGROUND, Brush.conic(90, 0xFFFF0000, 0xFF0000FF)))
                        .build())
                .put("button.wide", Style.builder().set(Styles.PADDING, Insets.of(4, 30)).build());
        SoundTheme.RETRO.applyTo(t, "card");
        return t;
    }

    @Test
    void exportImportRoundTripAgainstBase() {
        Theme theme = edited();
        String css = ThemeCss.export(theme, "dark");
        assertTrue(css.contains("--lumina-base: dark;"), css);
        assertTrue(css.contains("--accent: #F0506E;"), css);
        assertTrue(css.contains("button.wide {"), css);
        assertTrue(css.contains(".card:active {"), css);
        assertTrue(css.contains("hover-sound: minecraft:block.note_block.bit"), css);
        // Неизменённые классы базовой темы в файл не попадают.
        assertFalse(css.contains(".scroll {"), css);

        Theme loaded = ThemeCss.load(css);
        assertSameTheme(theme, loaded);
        // Привязки базовой темы живы: смена акцента после загрузки перекрашивает кнопку.
        loaded.set(Tokens.ACCENT, 0xFF00FF00);
        assertEquals(Brush.vertical(0xFF00FF00, io.github.ssbaxys.lumina.render.Color.darken(0xFF00FF00, 0.07f)),
                loaded.style("button").resolve(0)[Styles.BACKGROUND.id()]);
    }

    @Test
    void builtInThemesRoundTripWithTheirBase() {
        assertSameTheme(ModernTheme.dark(), ThemeCss.load(ThemeCss.export(ModernTheme.dark(), "modern-dark")));
        assertSameTheme(Theme.light(), ThemeCss.load(ThemeCss.export(Theme.light(), "light")));
        Theme hc = AccessibleThemes.highContrastDark();
        assertSameTheme(hc, ThemeCss.load(ThemeCss.export(hc, "high-contrast-dark")));
        // Полный экспорт на ту же базу тоже совпадает.
        Theme t = edited();
        assertSameTheme(t, ThemeCss.load(ThemeCss.export(t), Theme.dark()));
    }

    @Test
    void valuesThatCannotBeWrittenAreSkipped() {
        assertNull(ThemeCss.format(Styles.TRANSITION, Transition.of(0.2f, Easing.cubicBezier(0.1f, 0.2f, 0.3f, 0.4f))));
        assertNull(ThemeCss.format(Styles.BACKGROUND, new Brush.Linear(90, ColorStops.even(0xFF000000, 0xFFFFFFFF),
                ColorSpace.OKLAB, false, 0.3f)));
        assertEquals("#80FF0000", ThemeCss.format(Styles.TEXT_COLOR, 0x80FF0000));
        assertEquals("none", ThemeCss.format(Styles.CLICK_SOUND, UiSound.NONE));
        assertEquals("all 0.2s out-cubic", ThemeCss.format(Styles.TRANSITION, Transition.of(0.2f)));
        Theme t = Theme.dark().put("x", Style.builder()
                .set(Styles.TRANSITION, Transition.of(0.2f, Easing.cubicBezier(0.1f, 0.2f, 0.3f, 0.4f)))
                .set(Styles.OPACITY, 0.5f)
                .when(State.FIRST_CUSTOM, v -> v.set(Styles.SCALE, 2f))
                .build());
        String css = ThemeCss.export(t, "dark");
        assertTrue(css.contains("/* transition: не выражается в CSS */"), css);
        assertTrue(css.contains("opacity: 0.5;"), css);
        assertTrue(css.contains("не выражается в CSS */"), css);
        Theme back = ThemeCss.load(css);
        assertEquals(0.5f, back.style("x").get(Styles.OPACITY));
    }

    @Test
    void stylesheetSelectorTagDotClassEditsModifier() {
        Theme t = Stylesheet.parse("button.huge { padding: 20px; } .chip:checked { opacity: 0.5 }").applyTo(Theme.dark());
        assertEquals(Insets.all(20), t.rawStyle("button.huge").get(Styles.PADDING));
        assertEquals(Insets.all(20), t.style("button huge").resolve(0)[Styles.PADDING.id()]);
        assertEquals(0.5f, t.style("chip").resolve(State.CHECKED)[Styles.OPACITY.id()]);
    }

    @Test
    void saveAndLoadThroughPlatformFiles(@TempDir Path dir) throws Exception {
        platform.dataDir = dir;
        Theme theme = edited();
        assertTrue(ThemeCss.save("my-theme", theme, "dark"));
        Path file = dir.resolve("themes").resolve("my-theme.css");
        assertTrue(Files.isRegularFile(file));
        assertEquals(List.of("my-theme.css"), ThemeCss.savedThemes());
        Theme loaded = ThemeCss.loadFile("my-theme.css");
        assertNotNull(loaded);
        assertSameTheme(theme, loaded);
        assertNull(ThemeCss.loadFile("missing"));
        assertEquals("dark", ThemeCss.baseName(Files.readString(file)));
    }

    @Test
    void themeEditorEditsLiveAndExportsImports(@TempDir Path dir) {
        platform.dataDir = dir;
        io.github.ssbaxys.lumina.widget.ThemeEditor editor = new io.github.ssbaxys.lumina.widget.ThemeEditor()
                .fileName("editor.css");
        io.github.ssbaxys.lumina.node.UiRoot root = new io.github.ssbaxys.lumina.node.UiRoot(
                io.github.ssbaxys.lumina.widget.Panel.column().add(editor), Theme.dark());
        root.resize(400, 400);
        root.tick(0f);
        assertEquals(Theme.dark().get(Tokens.ACCENT), editor.accent().peek(), "fields read the theme");

        editor.accent().set(0xFF00C080);
        editor.corner().set(3f);
        assertEquals(0xFF00C080, (int) root.theme().get(Tokens.ACCENT), "live edit");
        assertEquals(3f, root.theme().get(Tokens.CORNER));

        editor.exportTheme();
        assertTrue(Files.isRegularFile(dir.resolve("themes/editor.css")), editor.status());
        assertEquals(editor.lastExport(), platform.clipboard);

        editor.accent().set(0xFFFF0000);
        editor.importTheme();
        for (int i = 0; i < 40; i++) {
            root.tick(0.05f);
        }
        assertEquals(0xFF00C080, (int) root.theme().get(Tokens.ACCENT), "imported back");
        assertEquals(0xFF00C080, editor.accent().peek());

        editor.resetTheme();
        for (int i = 0; i < 40; i++) {
            root.tick(0.05f);
        }
        assertEquals(Theme.dark().get(Tokens.ACCENT), root.theme().get(Tokens.ACCENT));
        platform.clipboard = "не тема";
        editor.importClipboard();
        assertEquals("В буфере нет темы", editor.status());
    }

    @Test
    void platformFilesStayInsideDataDirectory(@TempDir Path dir) {
        platform.dataDir = dir.resolve("data");
        Platform p = Lumina.platform();
        assertTrue(p.writeFile("a/b.txt", "привет"));
        assertEquals("привет", p.readFile("a/b.txt"));
        assertFalse(p.writeFile("../outside.txt", "x"));
        assertFalse(p.writeFile(dir.resolve("abs.txt").toAbsolutePath().toString(), "x"));
        assertNull(p.readFile("../data/../../etc/passwd"));
        assertFalse(Files.exists(dir.resolve("outside.txt")));
        assertEquals(List.of("b.txt"), p.listFiles("a"));
    }
}
