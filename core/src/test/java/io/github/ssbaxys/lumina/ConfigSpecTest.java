package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.config.ConfigScreen;
import io.github.ssbaxys.lumina.config.ConfigSpec;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.ColorField;
import io.github.ssbaxys.lumina.widget.KeyBindField;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Select;
import io.github.ssbaxys.lumina.widget.SliderField;
import io.github.ssbaxys.lumina.widget.TextField;
import io.github.ssbaxys.lumina.widget.Toggle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Описание настроек, файл TOML и автоматический экран настроек. */
class ConfigSpecTest {
    enum Mode { FAST, FANCY, FABULOUS }

    @TempDir
    Path dir;

    @BeforeEach
    void init() {
        CapturePlatform p = CapturePlatform.install();
        p.dataDir = dir;
    }

    static ConfigSpec spec() {
        return ConfigSpec.builder("test/mod.toml")
                .category("general", "Общие").icon("minecraft:compass")
                .bool("showHud", "Показывать HUD", true).comment("Панель с координатами")
                .integer("hudScale", "Масштаб HUD", 2, 1, 4)
                .number("opacity", "Прозрачность", 0.8f, 0f, 1f).step(0.05f)
                .category("look", "Вид")
                .choice("mode", "Качество", Mode.FANCY, Mode.values())
                .labels(m -> m == Mode.FAST ? "Быстро" : m.name().toLowerCase())
                .color("accent", "Акцент", 0xFF5B8CFF).alpha(false)
                .key("zoom", "Приближение", Keys.C)
                .string("nick", "Ник", "Steve").maxLength(8)
                .build();
    }

    @Test
    void typedAccessAndSanitizing() {
        ConfigSpec s = spec();
        assertEquals(2, s.categories().size());
        assertEquals(7, s.options().size());
        assertTrue(s.bool("showHud").peek());
        assertEquals(2, s.integer("general.hudScale").peek());
        assertEquals(Mode.FANCY, s.choice("mode", Mode.class).peek());
        ConfigSpec.Option<?> scale = s.option("hudScale");
        @SuppressWarnings("unchecked")
        ConfigSpec.Option<Integer> si = (ConfigSpec.Option<Integer>) scale;
        si.set(99);
        assertEquals(4, si.get(), "clamped");
        @SuppressWarnings("unchecked")
        ConfigSpec.Option<String> nick = (ConfigSpec.Option<String>) s.option("nick");
        nick.set("Very long nickname");
        assertEquals("Very lon", nick.get());
        @SuppressWarnings("unchecked")
        ConfigSpec.Option<Integer> accent = (ConfigSpec.Option<Integer>) s.option("accent");
        accent.set(0x80112233);
        assertEquals(0xFF112233, accent.get(), "no alpha");
        assertFalse(accent.isDefault());
        accent.reset();
        assertTrue(accent.isDefault());
        assertThrows(IllegalArgumentException.class, () -> s.option("missing"));
        assertThrows(IllegalArgumentException.class, () -> s.bool("nick"));
        assertEquals(2, s.search("hud").size());
        assertEquals(1, s.search("координат").size(), "comment matches");
        assertEquals(4, s.search("вид").size(), "category title matches");
    }

    @Test
    void writeAndReadRoundTrip() {
        ConfigSpec a = spec();
        a.bool("showHud").set(false);
        a.number("opacity").set(0.35f);
        a.choice("mode", Mode.class).set(Mode.FABULOUS);
        a.color("accent").set(0xFFABCDEF);
        a.key("zoom").set(Keys.SPACE);
        a.string("nick").set("A \"q\" #1");
        String text = a.write();
        assertTrue(text.contains("[general]"), text);
        assertTrue(text.contains("# Показывать HUD — Панель с координатами"), text);
        assertTrue(text.contains("hudScale = 2"), text);
        assertTrue(text.contains("# Масштаб HUD (1…4)"), text);
        assertTrue(text.contains("mode = \"FABULOUS\""), text);
        assertTrue(text.contains("accent = \"#ABCDEF\""), text);
        assertTrue(text.contains("nick = \"A \\\"q\\\" #1\""), text);

        ConfigSpec b = spec();
        int read = b.read(text + "\nunknown = 5\n[general]\nhudScale = nope\n");
        assertEquals(7, read);
        assertFalse(b.bool("showHud").peek());
        assertEquals(0.35f, b.number("opacity").peek(), 1e-6);
        assertEquals(Mode.FABULOUS, b.choice("mode", Mode.class).peek());
        assertEquals(0xFFABCDEF, b.color("accent").peek());
        assertEquals(Keys.SPACE, b.key("zoom").peek());
        assertEquals("A \"q\" #1", b.string("nick").peek());
        assertEquals(2, b.integer("hudScale").peek(), "bad value ignored");
    }

    @Test
    void saveAndLoadThroughPlatform() throws Exception {
        ConfigSpec a = spec();
        assertFalse(a.load(), "no file yet");
        a.integer("hudScale").set(3);
        assertTrue(a.save());
        assertTrue(Files.readString(dir.resolve("test/mod.toml")).contains("hudScale = 3"));
        ConfigSpec b = spec();
        assertTrue(b.load());
        assertEquals(3, b.integer("hudScale").peek());
    }

    private static <T> T find(UiElement e, Class<T> type) {
        if (type.isInstance(e) && e.isVisible()) {
            return type.cast(e);
        }
        for (UiElement c : e.children()) {
            T found = find(c, type);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Test
    void screenBuildsRowsSearchesResetsAndSaves() throws Exception {
        ConfigSpec s = spec();
        ConfigScreen screen = new ConfigScreen(s).title("Мой мод").fill();
        UiRoot root = new UiRoot(Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH)).fill().add(screen), Theme.dark());
        root.resize(640, 400);
        root.tick(0f);
        assertEquals(3, screen.rows().size());
        assertInstanceOf(Toggle.class, find(screen.rows().get(0), Toggle.class));
        assertInstanceOf(SliderField.class, find(screen.rows().get(1), SliderField.class));

        screen.category().set(1);
        root.tick(0f);
        assertEquals(4, screen.rows().size());
        assertInstanceOf(Select.class, find(screen.rows().get(0), Select.class));
        assertInstanceOf(ColorField.class, find(screen.rows().get(1), ColorField.class));
        assertInstanceOf(KeyBindField.class, find(screen.rows().get(2), KeyBindField.class));
        assertInstanceOf(TextField.class, find(screen.rows().get(3), TextField.class));

        // поиск по всем разделам: заголовок категории + строки
        screen.query().set("hud");
        root.tick(0f);
        assertEquals(3, screen.rows().size());
        assertInstanceOf(Label.class, screen.rows().get(0));
        screen.query().set("zzz");
        root.tick(0f);
        assertEquals(1, screen.rows().size());
        screen.query().set("");
        screen.category().set(0);
        root.tick(0f);

        // целое через ползунок-мост
        SliderField slider = find(screen.rows().get(1), SliderField.class);
        slider.slider().value().set(3.4f);
        assertEquals(3, s.integer("hudScale").peek());
        s.integer("hudScale").set(1);
        assertEquals(1f, slider.slider().value().peek());

        // переключатель меняет значение, кнопка сброса строки включается
        Toggle toggle = find(screen.rows().get(0), Toggle.class);
        toggle.toggle();
        assertFalse(s.bool("showHud").peek());
        assertTrue(screen.isDirty());
        Button reset = (Button) screen.rows().get(0).children().get(2);
        assertTrue(reset.isEnabled());
        reset.press();
        assertTrue(s.bool("showHud").peek());
        assertFalse(reset.isEnabled());

        ((Button) screen.findById("config-save")).press();
        assertFalse(screen.isDirty());
        assertTrue(Files.isRegularFile(dir.resolve("test/mod.toml")));
        assertTrue(((Label) screen.findById("config-status")).text().startsWith("Сохранено"));

        screen.autoSave(true);
        s.number("opacity").set(0.5f);
        assertTrue(Files.readString(dir.resolve("test/mod.toml")).contains("opacity = 0.5"));

        ((Button) screen.findById("config-reset")).press();
        assertEquals(0.8f, s.number("opacity").peek(), 1e-6);

        TestSupport.RecordingCanvas c = new TestSupport.RecordingCanvas();
        c.begin(640, 400);
        root.render(c, 0f);
        c.end();
        assertTrue(c.ops.stream().anyMatch(o -> o.startsWith("text Мой мод")));
        assertTrue(c.ops.stream().anyMatch(o -> o.startsWith("item minecraft:compass")));
    }
}
