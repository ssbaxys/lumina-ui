package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.WorldInfo;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.sound.SoundTheme;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.ThemeBlend;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.theme.ThemeSchedule;
import io.github.ssbaxys.lumina.theme.ThemeTransition;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Расписание тем по времени суток и биому, плавные переходы и источники значений. */
class ThemeScheduleTest {
    private static final int DAY = 0xFFFFC040;
    private static final int NIGHT = 0xFF4060FF;

    private CapturePlatform platform;
    private Theme day;
    private Theme night;

    @BeforeEach
    void init() {
        platform = CapturePlatform.install();
        day = Theme.light().set(Tokens.ACCENT, DAY);
        night = Theme.dark().set(Tokens.ACCENT, NIGHT);
    }

    @AfterEach
    void restoreClock() {
        WorldInfo.setClock(null);
    }

    private static void run(UiRoot root, float seconds) {
        for (float t = 0f; t < seconds; t += 0.05f) {
            root.tick(0.05f);
        }
    }

    @Test
    void timeOfDayPicksKeyThemesAndBlendsNearChange() {
        ThemeSchedule.TimeOfDay s = ThemeSchedule.byTime(() -> 0).dayNight(day, night).blend(1000f);
        assertSame(day, s.themeAt(6000f));
        assertSame(night, s.themeAt(18000f));
        assertSame(night, s.themeAt(-6000f), "wraps around");
        Theme dusk = s.themeAt(12000f);
        int accent = dusk.get(Tokens.ACCENT);
        assertNotEquals(DAY, accent);
        assertNotEquals(NIGHT, accent);
        assertEquals(Color.mix(DAY, NIGHT, 0.5f), accent, "half way: smoothstep(0.5) = 0.5");
        assertTrue(dusk.hasSameStyles(night));
        // Перед рассветом ночь смешивается с днём.
        assertNotEquals(NIGHT, (int) s.themeAt(23800f).get(Tokens.ACCENT));
        assertSame(night, s.themeAt(22000f));
    }

    @Test
    void biomeRulesInOrderWithFallback() {
        Theme desert = Theme.light();
        Theme snow = Theme.light();
        Theme other = Theme.dark();
        ThemeSchedule.ByValue<String> s = ThemeSchedule.byBiome(() -> "")
                .when("minecraft:desert", desert)
                .when(ThemeSchedule.contains("snow"), snow)
                .otherwise(other);
        assertSame(desert, s.themeFor("minecraft:desert"));
        assertSame(snow, s.themeFor("minecraft:snowy_taiga"));
        assertSame(other, s.themeFor("minecraft:plains"));
        assertSame(other, s.themeFor(""));
    }

    @Test
    void attachedScheduleTransitionsSmoothly() {
        Signal<Float> time = Signal.of(6000f);
        Button b = new Button("OK");
        UiRoot root = new UiRoot(Panel.column().add(b), Theme.dark());
        root.resize(200, 100);
        Subscription sub = ThemeSchedule.byTime(time::peek).dayNight(day, night).build()
                .transition(1f).poll(0f).attach(root);
        root.tick(0.05f);
        root.tick(0.05f);
        int mid = root.theme().get(Tokens.ACCENT);
        assertNotEquals(DAY, mid, "transition in progress");
        run(root, 1.5f);
        assertEquals(DAY, (int) root.theme().get(Tokens.ACCENT));
        assertEquals(day.get(Tokens.SURFACE), root.theme().get(Tokens.SURFACE));

        time.set(18000f);
        run(root, 0.3f);
        int half = root.theme().get(Tokens.ACCENT);
        assertNotEquals(DAY, half);
        assertNotEquals(NIGHT, half);
        run(root, 1.5f);
        assertEquals(NIGHT, (int) root.theme().get(Tokens.ACCENT));
        // Стили виджетов идут за токенами.
        assertEquals(root.theme().style("button").resolve(0)[Styles.BACKGROUND.id()],
                b.computedStyle().get(Styles.BACKGROUND));

        sub.close();
        time.set(6000f);
        run(root, 1.5f);
        assertEquals(NIGHT, (int) root.theme().get(Tokens.ACCENT), "closed schedule no longer changes the theme");
    }

    @Test
    void decoratorIsAppliedToScheduledThemes() {
        UiRoot root = new UiRoot(Panel.column(), Theme.dark());
        ThemeSchedule s = ThemeSchedule.of(() -> day).poll(0f).transition(0.2f)
                .decorate(t -> SoundTheme.RETRO.applyTo(t));
        s.attach(root);
        run(root, 0.5f);
        assertEquals(SoundTheme.RETRO.click(), root.theme().rawStyle("button").get(Styles.CLICK_SOUND));
        assertEquals(null, day.rawStyle("button").get(Styles.CLICK_SOUND), "source theme untouched");
        s.decorate(t -> SoundTheme.SOFT.applyTo(t));
        run(root, 0.5f);
        assertEquals(SoundTheme.SOFT.click(), root.theme().rawStyle("button").get(Styles.CLICK_SOUND));
    }

    @Test
    void themeTransitionIsInstantWithReducedMotion() {
        UiRoot root = new UiRoot(Panel.column(), Theme.dark());
        root.accessibility().reducedMotion(true);
        ThemeTransition t = ThemeTransition.to(root, day, 2f);
        assertTrue(t.isFinished());
        assertEquals(DAY, (int) root.theme().get(Tokens.ACCENT));

        root.accessibility().reducedMotion(false);
        ThemeTransition slow = ThemeTransition.to(root, night, 1f);
        assertEquals(DAY, (int) root.theme().get(Tokens.ACCENT));
        run(root, 1.2f);
        assertTrue(slow.isFinished());
        assertEquals(NIGHT, (int) root.theme().get(Tokens.ACCENT));
        assertEquals(DAY, (int) day.get(Tokens.ACCENT), "target is copied");
    }

    @Test
    void blendInterpolatesColorAndNumberTokens() {
        Theme a = Theme.dark().set(Tokens.CORNER, 0f);
        Theme b = Theme.dark().set(Tokens.CORNER, 10f);
        assertEquals(5f, ThemeBlend.mix(a, b, 0.5f).get(Tokens.CORNER), 1e-5f);
        assertEquals(b.get(Tokens.ACCENT), ThemeBlend.mix(a, b, 1f).get(Tokens.ACCENT));
    }

    @Test
    void worldInfoUsesPlatformThenSystemClock() {
        assertEquals(0f, WorldInfo.ticksOf(6, 0));
        assertEquals(6000f, WorldInfo.ticksOf(12, 0));
        assertEquals(18000f, WorldInfo.ticksOf(0, 0));
        assertEquals(17000f, WorldInfo.ticksOf(23, 0));
        assertEquals("12:00", WorldInfo.formatTime(6000f));
        assertEquals("06:30", WorldInfo.formatTime(500f));
        WorldInfo.setClock(() -> LocalTime.of(18, 0));
        assertEquals(12000f, WorldInfo.timeOfDay(), "no world: system clock");
        assertEquals("", WorldInfo.biome());

        Lumina.init(new CapturePlatform() {
            @Override
            public float dayTime() {
                return 30000f;
            }

            @Override
            public String biomeId() {
                return "minecraft:desert";
            }
        });
        assertEquals(6000f, WorldInfo.timeOfDay());
        assertEquals("minecraft:desert", WorldInfo.biome());
    }
}
