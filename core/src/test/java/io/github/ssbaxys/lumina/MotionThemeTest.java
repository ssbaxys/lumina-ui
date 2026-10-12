package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.anim.Animator;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.anim.Interpolator;
import io.github.ssbaxys.lumina.anim.Keyframes;
import io.github.ssbaxys.lumina.anim.Motion;
import io.github.ssbaxys.lumina.anim.Playback;
import io.github.ssbaxys.lumina.anim.Tween;
import io.github.ssbaxys.lumina.fx.Effect;
import io.github.ssbaxys.lumina.fx.FxLayer;
import io.github.ssbaxys.lumina.fx.FxRegistry;
import io.github.ssbaxys.lumina.fx.Particles;
import io.github.ssbaxys.lumina.fx.VanillaFx;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.style.Transition;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotionThemeTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    @Test
    void easingsStartAndEndInPlace() {
        for (var e : Easing.presets().entrySet()) {
            assertEquals(0f, e.getValue().apply(0f), 1e-3, e.getKey());
            assertEquals(1f, e.getValue().apply(1f), 1e-3, e.getKey());
        }
        assertEquals(0.5f, Easing.steps(2).apply(0.6f));
        assertEquals(1f, Easing.spring(0.5f, 3f).apply(1f));
        assertEquals(0f, Easing.LINEAR.mirrored().apply(1f), 1e-6);
    }

    @Test
    void keyframesInterpolateBetweenPoints() {
        Keyframes<Float> k = Keyframes.of(Interpolator.FLOAT).at(0f, 0f).at(0.5f, 10f).at(1f, 0f);
        assertEquals(5f, k.sample(0.25f), 1e-4);
        assertEquals(10f, k.sample(0.5f), 1e-4);
        assertEquals(5f, k.sample(0.75f), 1e-4);
        assertEquals(0f, k.sample(2f), 1e-4);
    }

    @Test
    void sequenceParallelAndCallsSeekCorrectly() {
        float[] a = {0f};
        float[] b = {0f};
        AtomicInteger calls = new AtomicInteger();
        Motion m = Motion.sequence(
                Tween.of(Interpolator.FLOAT, 0f, 1f, 1f, Easing.LINEAR, v -> a[0] = v),
                Motion.call(calls::incrementAndGet),
                Tween.of(Interpolator.FLOAT, 0f, 2f, 1f, Easing.LINEAR, v -> b[0] = v));
        assertEquals(2f, m.duration(), 1e-6);
        m.apply(0.5f);
        assertEquals(0.5f, a[0], 1e-4);
        assertEquals(0f, b[0], 1e-4);
        m.apply(1.5f);
        assertEquals(1f, a[0], 1e-4, "first part snaps to its end when skipped over");
        assertEquals(1f, b[0], 1e-4);
        assertEquals(1, calls.get());
        Motion par = Motion.parallel(Motion.delay(1f), Motion.delay(3f));
        assertEquals(3f, par.duration(), 1e-6);
        assertEquals(4f, Motion.delay(1f).then(Motion.delay(1f)).repeat(2, false).duration(), 1e-6);
    }

    @Test
    void playbackLoopsYoyoAndFinishes() {
        float[] v = {0f};
        Animator animator = new Animator();
        AtomicBoolean done = new AtomicBoolean();
        Playback p = new Playback(Tween.of(Interpolator.FLOAT, 0f, 1f, 1f, Easing.LINEAR, x -> v[0] = x))
                .loops(2).yoyo(true).onFinish(() -> done.set(true)).start(animator);
        animator.tick(0.5f);
        assertEquals(0.5f, v[0], 1e-4);
        animator.tick(0.75f);
        assertEquals(0.75f, v[0], 1e-4, "second loop runs backwards");
        assertEquals(1, p.iteration());
        animator.tick(1f);
        assertEquals(0f, v[0], 1e-4);
        assertTrue(done.get());
        assertFalse(p.isRunning());
    }

    @Test
    void styleAnimationOverridesAndReleases() {
        Panel panel = Panel.box(FlexLayout.column());
        UiRoot root = new UiRoot(panel, Theme.dark());
        root.resize(100, 100);
        panel.animate(Styles.OPACITY).from(0f).to(1f).duration(1f).ease(Easing.LINEAR).keep(false).play();
        root.tick(0f);
        root.tick(0.5f);
        assertEquals(0.5f, panel.computedStyle().get(Styles.OPACITY), 1e-3);
        root.tick(0.6f);
        assertFalse(panel.computedStyle().hasOverride(Styles.OPACITY));
        assertEquals(1f, panel.computedStyle().get(Styles.OPACITY), 1e-4);
    }

    @Test
    void themeTokenChangeRestylesEverythingBoundToIt() {
        Theme theme = Theme.dark();
        Button button = new Button("ok");
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).add(button), theme);
        root.resize(200, 100);
        root.tick(0f);
        Brush before = button.computedStyle().get(Styles.BACKGROUND);
        theme.set(Tokens.ACCENT, 0xFFFF4060);
        root.tick(0f);
        root.tick(1f);
        Brush after = button.computedStyle().get(Styles.BACKGROUND);
        assertNotEquals(before, after);
        assertEquals(0xFFFF4060, ((Brush.Linear) after).stops().color(0));
        // светлый акцент → тёмный текст на кнопке
        theme.set(Tokens.ACCENT, 0xFFFFE070);
        root.tick(0f);
        root.tick(1f);
        assertEquals(0xFF1D2330, button.computedStyle().get(Styles.TEXT_COLOR));
    }

    @Test
    void multipleClassesMergeInOrderAndMotionScalesTransitions() {
        Theme theme = new Theme()
                .put("a", Style.builder().set(Styles.OPACITY, 0.2f)
                        .set(Styles.TRANSITION, Transition.of(1f)).build())
                .put("b", Style.builder().set(Styles.OPACITY, 0.7f).build());
        assertEquals(0.7f, theme.style("a b").get(Styles.OPACITY));
        assertEquals(0.2f, theme.style("b a").get(Styles.OPACITY));
        theme.set(Tokens.MOTION, 0.5f);
        assertEquals(0.5f, theme.style("a").get(Styles.TRANSITION).duration(), 1e-6);
        Transition t = Transition.of(1f).key(Styles.OPACITY, 0.2f, Easing.LINEAR, 0.1f).instant(Styles.SCALE);
        assertEquals(0.2f, t.durationOf(Styles.OPACITY.id()), 1e-6);
        assertEquals(0.1f, t.delayOf(Styles.OPACITY.id()), 1e-6);
        assertEquals(0f, t.durationOf(Styles.SCALE.id()), 1e-6);
        assertEquals(1f, t.durationOf(Styles.RADIUS.id()), 1e-6);
    }

    @Test
    void effectsLiveAndDie() {
        FxLayer fx = new FxLayer();
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).fill().add(fx), Theme.dark());
        root.resize(300, 200);
        root.tick(0f);
        List<String> events = new ArrayList<>();
        fx.spawn(VanillaFx.explosion(100, 100, 1f));
        fx.spawn(VanillaFx.primedTnt(50, 10, 150, 16, 0.5f).onExplode(() -> events.add("boom")));
        assertEquals(2, fx.effectCount());
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        for (int i = 0; i < 300; i++) {
            root.tick(1f / 60f);
            if (i % 30 == 0) {
                canvas.begin(300, 200);
                root.render(canvas, 0f);
                canvas.end();
            }
        }
        assertEquals(List.of("boom"), events);
        assertEquals(0, fx.effectCount(), "everything burns out");
    }

    @Test
    void everyRegisteredEffectRunsAndStops() {
        FxLayer fx = new FxLayer();
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).fill().add(fx), Theme.dark());
        root.resize(300, 200);
        root.tick(0f);
        assertTrue(FxRegistry.ids().contains("minecraft:snow"));
        assertTrue(FxRegistry.ids().size() >= 20);
        List<Effect> spawned = new ArrayList<>();
        for (String id : FxRegistry.ids()) {
            spawned.add(fx.spawn(id, 20, 20, 200, 150));
        }
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        for (int i = 0; i < 120; i++) {
            root.tick(1f / 60f);
            if (i % 20 == 0) {
                canvas.begin(300, 200);
                root.render(canvas, 0f);
                canvas.end();
            }
        }
        assertTrue(canvas.ops.stream().anyMatch(o -> o.contains("environment/snow")), "weather draws vanilla texture");
        spawned.forEach(Effect::stop);
        for (int i = 0; i < 60 * 12; i++) {
            root.tick(1f / 60f);
        }
        assertEquals(0, fx.effectCount(), "stopped weather and emitters fade out");
    }

    @Test
    void particlesLandOnFloorAndAttract() {
        List<Float> landed = new ArrayList<>();
        Particles p = new Particles(null).gravity(100f).floor(50f, (x, y, c) -> landed.add(y));
        p.emit(0, 0, 0, 0, 10f, 1f);
        Particles q = new Particles(null).attract(100f, 0f, 400f);
        q.emit(0, 0, 0, 0, 10f, 1f);
        FxLayer layer = new FxLayer();
        for (int i = 0; i < 120; i++) {
            p.tick(1f / 60f, layer);
            q.tick(1f / 60f, layer);
        }
        assertEquals(List.of(50f), landed);
        assertEquals(0, p.count());
        assertEquals(0, q.count(), "attracted particle reaches the target and disappears");
    }
}
