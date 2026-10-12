package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.sound.SoundTheme;
import io.github.ssbaxys.lumina.sound.UiSound;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Toggle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Звуки наведения и нажатия из стиля, наборы звуков, защита от «треска» при проведении мышью. */
class SoundThemeTest {
    private static final String HOVER = SoundTheme.SOFT.hover().id();
    private static final String CLICK = SoundTheme.SOFT.click().id();

    private CapturePlatform platform;

    @BeforeEach
    void init() {
        platform = CapturePlatform.install();
    }

    private record Scene(UiRoot root, Button a, Button b, Button off, Toggle toggle) {
    }

    /** Кнопки 60×20 в ряд с шагом 70: a (0..60), b (70..130), off (140..200), переключатель дальше. */
    private static Scene scene(Theme theme) {
        Button a = new Button("Один").size(60, 20);
        Button b = new Button("Два").size(60, 20);
        Button off = new Button("Нет").size(60, 20).enabled(false);
        Toggle toggle = new Toggle(Signal.of(false));
        Panel row = Panel.box(FlexLayout.row().gap(10).align(Align.START)).add(a, b, off, toggle);
        UiRoot root = new UiRoot(row, theme);
        root.resize(400, 100);
        root.tick(0f);
        return new Scene(root, a, b, off, toggle);
    }

    private long count(String id) {
        return platform.sounds.stream().filter(id::equals).count();
    }

    @Test
    void hoverSoundPlaysOnEnterOnlyAndIsRateLimited() {
        Scene s = scene(SoundTheme.SOFT.applyTo(Theme.dark()));
        s.root().pointerMove(10, 10);
        assertEquals(1, count(HOVER), "enter a");
        s.root().pointerMove(30, 12);
        s.root().pointerMove(50, 8);
        assertEquals(1, count(HOVER), "moving inside a is silent");

        s.root().pointerMove(80, 10);
        assertEquals(1, count(HOVER), "b entered too soon after a");
        platform.advance(0.2f);
        s.root().pointerMove(65, 10);
        s.root().pointerMove(90, 10);
        assertEquals(2, count(HOVER), "b after the interval");

        platform.advance(0.2f);
        s.root().pointerMove(150, 10);
        assertEquals(2, count(HOVER), "disabled button is silent");

        s.root().setHoverSoundInterval(0f);
        s.root().pointerMove(10, 10);
        s.root().pointerMove(90, 10);
        assertEquals(4, count(HOVER), "no limit with zero interval");
    }

    @Test
    void clickSoundComesFromStyleOrVanillaFallback() {
        Scene plain = scene(Theme.dark());
        plain.a().press();
        assertEquals(1, platform.clicks, "vanilla click without a sound theme");
        assertTrue(platform.sounds.isEmpty());
        plain.root().pointerDown(plain.toggle().screenX() + 2, 5, Keys.MOUSE_LEFT);
        plain.root().pointerUp(plain.toggle().screenX() + 2, 5, Keys.MOUSE_LEFT);
        assertTrue(plain.toggle().isOn());
        assertTrue(platform.sounds.isEmpty(), "toggles are silent by default");

        Scene soft = scene(SoundTheme.SOFT.applyTo(Theme.dark()));
        soft.a().press();
        assertEquals(1, platform.clicks);
        assertEquals(1, count(CLICK));
        soft.root().pointerDown(soft.toggle().screenX() + 2, 5, Keys.MOUSE_LEFT);
        soft.root().pointerUp(soft.toggle().screenX() + 2, 5, Keys.MOUSE_LEFT);
        assertEquals(2, count(CLICK), "toggle uses the theme click");

        Scene none = scene(SoundTheme.NONE.applyTo(Theme.dark()));
        int before = platform.sounds.size();
        none.a().press();
        none.root().pointerMove(10, 10);
        assertEquals(1, platform.clicks, "NONE silences the vanilla click too");
        assertEquals(before, platform.sounds.size());
    }

    @Test
    void soundThemesAndUiSound() {
        assertEquals(List.of("vanilla", "soft", "retro", "none"),
                SoundTheme.builtIn().stream().map(SoundTheme::name).toList());
        assertEquals(SoundTheme.RETRO, SoundTheme.byName("RETRO"));
        assertNull(SoundTheme.byName("нет"));
        UiSound s = UiSound.of("minecraft:block.note_block.bit", 0.15f, 2f);
        assertEquals(s, UiSound.parse(s.toString()));
        assertEquals(UiSound.NONE, UiSound.parse("none"));
        assertTrue(UiSound.of("x", 0f, 1f).isNone());
        SoundTheme quiet = SoundTheme.SOFT.scaled(0.5f);
        assertEquals(SoundTheme.SOFT.hover().volume() * 0.5f, quiet.hover().volume(), 1e-6f);

        Theme t = SoundTheme.RETRO.applyTo(Theme.dark(), "card");
        assertEquals(SoundTheme.RETRO.hover(), t.rawStyle("card").get(Styles.HOVER_SOUND));
        assertEquals(SoundTheme.RETRO.click(), t.rawStyle("button").get(Styles.CLICK_SOUND));
        // Звук не наследуется: метка внутри кнопки-панели его не получает.
        Label inner = new Label("x");
        Panel card = Panel.box(FlexLayout.column()).add(inner).styleClass("card");
        UiRoot root = new UiRoot(Panel.column().add(card), t);
        root.tick(0f);
        assertEquals(SoundTheme.RETRO.hover(), card.computedStyle().get(Styles.HOVER_SOUND));
        assertNull(inner.computedStyle().get(Styles.HOVER_SOUND));
    }
}
