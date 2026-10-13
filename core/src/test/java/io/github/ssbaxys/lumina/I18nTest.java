package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.i18n.Language;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.LanguageButton;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class I18nTest {
    /** Платформа с «игрой»: свой язык, lang-файлы и сохранённые настройки. */
    private static final class GamePlatform implements Platform {
        String gameLanguage = "en_us";
        final Map<String, String> files = new HashMap<>();
        final Map<String, String> settings = new HashMap<>();

        @Override
        public TextMetrics textMetrics() {
            return TestSupport.METRICS;
        }

        @Override
        public String getClipboard() {
            return "";
        }

        @Override
        public void setClipboard(String text) {
        }

        @Override
        public void playClickSound() {
        }

        @Override
        public String gameLanguage() {
            return gameLanguage;
        }

        @Override
        public String languageName(String language) {
            return language.equals("ru_ru") ? "Русский" : language.equals("en_us") ? "English" : null;
        }

        @Override
        public String translate(String language, String key) {
            return files.get(language + ":" + key);
        }

        @Override
        public String readSetting(String key) {
            return settings.get(key);
        }

        @Override
        public void writeSetting(String key, String value) {
            settings.put(key, value);
        }
    }

    private GamePlatform platform;

    @BeforeEach
    void setUp() {
        platform = new GamePlatform();
        Lumina.init(platform);
        I18n.reset();
        I18n.translations("en_us").add("hello", "Hello").add("count", "Count: %s").add("only.en", "English only");
        I18n.translations("ru_ru").add("hello", "Привет").add("count", "Счёт: %s");
    }

    @AfterEach
    void tearDown() {
        TestSupport.init();
        I18n.reset();
    }

    @Test
    void fallsBackToDefaultLanguageAndThenToKey() {
        I18n.setLanguage("ru_ru");
        assertEquals("Привет", I18n.get("hello"));
        assertEquals("English only", I18n.get("only.en"));
        assertEquals("no.such.key", I18n.get("no.such.key"));
        assertTrue(I18n.has("only.en"));
        assertFalse(I18n.has("no.such.key"));
    }

    @Test
    void formatsArguments() {
        I18n.translations("en_us").add("pair", "%2$s before %1$s, 100%%").add("broken", "50% off %s");
        assertEquals("Count: 3", I18n.get("count", 3));
        assertEquals("b before a, 100%", I18n.get("pair", "a", "b"));
        assertEquals("50% off now", I18n.get("broken", "now"));
        assertEquals("Count: %s", I18n.get("count"));
    }

    @Test
    void liveTextFollowsLanguageAndSignalArguments() {
        Signal<Integer> n = Signal.of(1);
        ReadableSignal<String> text = I18n.tr("count", n);
        List<String> seen = new ArrayList<>();
        Subscription sub = text.subscribe(seen::add);
        n.set(2);
        I18n.setLanguage("ru_ru");
        assertEquals(List.of("Count: 2", "Счёт: 2"), seen);
        sub.close();
        I18n.setLanguage("en_us");
        n.set(3);
        assertEquals(2, seen.size());
        assertEquals("Count: 3", text.get());
    }

    @Test
    void widgetsRetranslateWhileAttached() {
        Label label = Label.tr("hello");
        Button button = Button.tr("count", 7);
        Panel panel = Panel.column().add(label, button);
        UiRoot root = new UiRoot(panel);
        root.resize(200, 100);
        assertEquals("Hello", label.text());

        I18n.setLanguage("ru_ru");
        assertEquals("Привет", label.text());
        assertEquals("Счёт: 7", button.text());

        // Снятый с дерева виджет не слушает язык, а при возврате подхватывает текущий.
        panel.removeChild(label);
        I18n.setLanguage("en_us");
        assertEquals("Привет", label.text());
        panel.add(label);
        assertEquals("Hello", label.text());
    }

    @Test
    void followsGameLanguageUntilUserChooses() {
        assertTrue(I18n.isFollowingGame());
        platform.gameLanguage = "ru_ru";
        I18n.refresh();
        assertEquals("ru_ru", I18n.current());

        I18n.setLanguage("en_us");
        platform.gameLanguage = "de_de";
        I18n.refresh();
        assertEquals("en_us", I18n.current());
        assertEquals("en_us", platform.settings.get("language"));

        I18n.followGameLanguage();
        assertEquals("de_de", I18n.current());
        assertEquals("auto", platform.settings.get("language"));
    }

    @Test
    void restoresSavedChoice() {
        platform.settings.put("language", "ru_ru");
        I18n.reset();
        assertEquals("ru_ru", I18n.current());
        assertFalse(I18n.isFollowingGame());
    }

    @Test
    void readsGameLangFilesAndReloads() {
        platform.files.put("ru_ru:from.file", "Из файла");
        ReadableSignal<String> text = I18n.tr("from.file");
        List<String> seen = new ArrayList<>();
        text.subscribe(seen::add);
        I18n.setLanguage("ru_ru");
        platform.files.put("ru_ru:from.file", "Обновлено");
        I18n.refresh();
        assertEquals(List.of("Из файла", "Обновлено"), seen);
    }

    @Test
    void languageButtonCyclesDeclaredLanguages() {
        assertEquals(List.of(new Language("en_us", "English"), new Language("ru_ru", "Русский")), I18n.languages());
        I18n.declareLanguage("ru_ru", "По-русски");
        LanguageButton button = new LanguageButton();
        UiRoot root = new UiRoot(Panel.column().add(button));
        root.resize(200, 100);
        assertEquals("English", button.text());
        button.press();
        assertEquals("ru_ru", I18n.current());
        assertEquals("По-русски", button.text());
        button.press();
        assertEquals("en_us", I18n.current());
    }
}
