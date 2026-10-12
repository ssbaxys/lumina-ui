package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.hud.BossBar;
import io.github.ssbaxys.lumina.hud.Countdown;
import io.github.ssbaxys.lumina.hud.Hud;
import io.github.ssbaxys.lumina.hud.HudAnchor;
import io.github.ssbaxys.lumina.hud.HudEntry;
import io.github.ssbaxys.lumina.hud.HudVisibility;
import io.github.ssbaxys.lumina.hud.NotificationFeed;
import io.github.ssbaxys.lumina.hud.RadialGauge;
import io.github.ssbaxys.lumina.hud.SegmentedBar;
import io.github.ssbaxys.lumina.hud.TimerDisplay;
import io.github.ssbaxys.lumina.hud.Toast;
import io.github.ssbaxys.lumina.hud.ToastStack;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** HUD: привязки к краям, правила видимости, слои, тосты, лента, таймеры и полосы. */
class HudTest {
    private CapturePlatform platform;

    @BeforeEach
    void init() {
        platform = CapturePlatform.install();
    }

    private static Panel box(float w, float h) {
        return Panel.box(FlexLayout.column()).size(w, h);
    }

    @Test
    void anchorsPlaceElementsWithInwardOffsets() {
        Hud hud = new Hud();
        HudEntry tl = hud.add(box(20, 10), HudAnchor.TOP_LEFT, 4, 6);
        HudEntry tr = hud.add(box(20, 10), HudAnchor.TOP_RIGHT, 4, 6);
        HudEntry c = hud.add(box(20, 10), HudAnchor.CENTER);
        HudEntry bc = hud.add(box(30, 10), HudAnchor.BOTTOM_CENTER, 0, 40);
        HudEntry br = hud.add(box(20, 10), HudAnchor.BOTTOM_RIGHT, 2, 2);
        HudEntry cl = hud.add(box(20, 10), HudAnchor.CENTER_LEFT, 3, 5);
        assertTrue(hud.tick(320, 240, 0f, 0));

        assertEquals(4f, tl.x());
        assertEquals(6f, tl.y());
        assertEquals(320f - 20f - 4f, tr.x());
        assertEquals(6f, tr.y());
        assertEquals(150f, c.x());
        assertEquals(115f, c.y());
        assertEquals(145f, bc.x());
        assertEquals(240f - 10f - 40f, bc.y());
        assertEquals(298f, br.x());
        assertEquals(228f, br.y());
        assertEquals(3f, cl.x());
        assertEquals(120f, cl.y());
        assertEquals(20f, tl.width());

        // Смена привязки и смещения, затем новый размер экрана.
        tl.anchor(HudAnchor.BOTTOM_LEFT).offset(0, 0);
        hud.tick(400, 300, 0f, 0);
        assertEquals(0f, tl.x());
        assertEquals(290f, tl.y());
        assertEquals(400f - 24f, tr.x());
    }

    @Test
    void elementGrowingKeepsItsAnchor() {
        Hud hud = new Hud();
        Panel grow = box(20, 10);
        HudEntry e = hud.add(grow, HudAnchor.BOTTOM_RIGHT);
        hud.tick(100, 100, 0f, 0);
        assertEquals(80f, e.x());
        grow.size(50, 30);
        hud.tick(100, 100, 0f, 0);
        assertEquals(50f, e.x());
        assertEquals(70f, e.y());
    }

    @Test
    void visibilityRulesFollowGameState() {
        Hud hud = new Hud();
        HudEntry def = hud.add(box(10, 10), HudAnchor.TOP_LEFT);
        HudEntry always = hud.add(box(10, 10), HudAnchor.TOP_LEFT).visibility(HudVisibility.ALWAYS);
        HudEntry gameplay = hud.add(box(10, 10), HudAnchor.TOP_LEFT).visibility(HudVisibility.GAMEPLAY);
        AtomicBoolean flag = new AtomicBoolean(true);
        HudEntry custom = hud.add(box(10, 10), HudAnchor.TOP_LEFT)
                .visibility(HudVisibility.ALWAYS.hideInChat(true).when(flag::get));

        hud.tick(100, 100, 0f, 0);
        assertTrue(def.isShown() && always.isShown() && gameplay.isShown() && custom.isShown());

        hud.tick(100, 100, 0f, HudVisibility.GUI_HIDDEN);
        assertFalse(def.isShown());
        assertTrue(always.isShown());
        assertFalse(gameplay.isShown());
        assertTrue(custom.isShown());

        hud.tick(100, 100, 0f, HudVisibility.CHAT_OPEN | HudVisibility.SCREEN_OPEN);
        assertTrue(def.isShown());
        assertFalse(gameplay.isShown());
        assertFalse(custom.isShown());

        hud.tick(100, 100, 0f, HudVisibility.DEBUG_SCREEN);
        assertFalse(def.isShown());
        assertTrue(custom.isShown());
        flag.set(false);
        hud.tick(100, 100, 0f, 0);
        assertFalse(custom.isShown());

        // Своя видимость элемента не трогается правилами HUD.
        assertTrue(def.element().isVisible());

        // Если всё скрыто, отрисовки нет вовсе.
        Hud only = new Hud();
        only.add(box(10, 10), HudAnchor.CENTER);
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(100, 100);
        only.render(canvas, 100, 100, 0f, HudVisibility.GUI_HIDDEN);
        canvas.end();
        assertTrue(canvas.ops.isEmpty());

        // Общее правило HUD для записей без своего.
        hud.setDefaultVisibility(HudVisibility.ALWAYS);
        hud.tick(100, 100, 0f, HudVisibility.GUI_HIDDEN);
        assertTrue(def.isShown());
    }

    @Test
    void layersOrderDrawingAndRemovalWorks() {
        Hud hud = new Hud();
        Panel a = box(5, 5);
        Panel b = box(5, 5);
        Panel c = box(5, 5);
        hud.add(a, HudAnchor.TOP_LEFT).layer(5);
        hud.add(b, HudAnchor.TOP_LEFT);
        hud.add(c, HudAnchor.TOP_LEFT).layer(5);
        assertEquals(List.of(b, a, c), elements(hud));

        hud.entry(b).layer(10);
        assertEquals(List.of(a, c, b), elements(hud));
        hud.entry(a).layer(-1);
        assertEquals(List.of(a, c, b), elements(hud));
        assertSame(a.root(), hud.root());

        assertTrue(hud.remove(c));
        assertFalse(hud.remove(c));
        assertNull(c.root());
        assertNull(c.parent());
        assertEquals(List.of(a, b), elements(hud));

        // Повторное добавление — та же запись с новой привязкой.
        HudEntry again = hud.add(a, HudAnchor.CENTER, 1, 1);
        assertSame(hud.entry(a), again);
        assertEquals(HudAnchor.CENTER, again.anchor());
        assertEquals(2, hud.entries().size());

        hud.clear();
        assertTrue(hud.isEmpty());
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(100, 100);
        hud.render(canvas, 100, 100, 0f, 0);
        canvas.end();
        assertTrue(canvas.ops.isEmpty(), "empty HUD draws nothing");
    }

    private static List<UiElement> elements(Hud hud) {
        List<UiElement> out = new ArrayList<>();
        for (HudEntry e : hud.entries()) {
            out.add(e.element());
        }
        return out;
    }

    @Test
    void toastsEnterLiveLeaveAndQueue() {
        Hud hud = new Hud();
        ToastStack stack = hud.toasts().maxVisible(2);
        assertSame(stack, hud.toasts());
        HudEntry entry = hud.entry(stack);
        assertEquals(HudAnchor.TOP_RIGHT, entry.anchor());

        AtomicInteger closed = new AtomicInteger();
        Toast a = stack.show(Toast.success("Готово", "Мир сохранён").icon("minecraft:book").timeout(1f).onClose(closed::incrementAndGet));
        Toast b = stack.show("Второй", "Длинный текст уведомления, который не помещается в одну строку и переносится");
        Toast c = stack.show(Toast.info("Третий", "").timeout(0f));
        assertEquals(1, stack.queued());
        assertEquals(List.of(a, b), stack.visible());
        assertEquals(Toast.Phase.ENTERING, a.phase());
        assertEquals(Toast.Phase.QUEUED, c.phase());

        tick(hud, 0.05f, 2);
        assertEquals(Toast.Phase.ENTERING, a.phase());
        assertTrue(a.computedStyle().get(io.github.ssbaxys.lumina.style.Styles.TRANSLATE_X) > 0f, "slides in from the right");
        tick(hud, 0.05f, 6);
        assertEquals(Toast.Phase.SHOWN, a.phase());
        assertEquals(0f, a.computedStyle().get(io.github.ssbaxys.lumina.style.Styles.TRANSLATE_X));
        assertTrue(b.height() > a.height() - 1f, "wrapped body makes the toast taller");

        // Время жизни: 1 секунда после появления, затем уход, схлопывание и следующий из очереди.
        tick(hud, 0.05f, 21);
        assertTrue(a.isClosing());
        tick(hud, 0.05f, 10);
        assertEquals(Toast.Phase.GONE, a.phase());
        assertNull(a.parent());
        assertEquals(1, closed.get());
        assertEquals(0, stack.queued());
        assertTrue(stack.visible().contains(c));

        // Бессрочный тост живёт до dismiss().
        tick(hud, 0.1f, 100);
        assertEquals(Toast.Phase.SHOWN, c.phase());
        assertEquals(Toast.Phase.GONE, b.phase(), "default 5s timeout");
        c.dismiss();
        tick(hud, 0.05f, 12);
        assertEquals(Toast.Phase.GONE, c.phase());
        assertTrue(stack.visible().isEmpty());
    }

    @Test
    void toastProgressAndDismissFromQueue() {
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()), Theme.dark());
        ToastStack stack = new ToastStack().maxVisible(1).newestOnTop(true);
        root.content().addChild(stack);
        root.resize(300, 200);
        Signal<Float> p = Signal.of(0.2f);
        Toast loading = stack.show(Toast.info("Загрузка", "Чанки").timeout(0).bindProgress(p));
        Toast waiting = stack.show(new Toast("В очереди"));
        root.tick(0.1f);
        assertEquals(0.2f, loading.progress());
        p.set(0.9f);
        assertEquals(0.9f, loading.progress());
        AtomicBoolean gone = new AtomicBoolean();
        waiting.onClose(() -> gone.set(true));
        waiting.dismiss();
        assertTrue(gone.get());
        assertEquals(0, stack.queued());

        // Отрисовка тоста с иконкой и прогрессом.
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        for (int i = 0; i < 10; i++) {
            root.tick(0.05f);
        }
        canvas.begin(300, 200);
        root.render(canvas, 0f);
        canvas.end();
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("text Загрузка")), canvas.ops.toString());

        stack.dismissAll();
        for (int i = 0; i < 20; i++) {
            root.tick(0.05f);
        }
        assertTrue(stack.visible().isEmpty());
    }

    @Test
    void feedShowsRecentLinesAndForgetsOldOnes() {
        Hud hud = new Hud();
        NotificationFeed feed = hud.feed().capacity(3).lifetime(2f);
        feed.post("Первая");
        feed.post("minecraft:diamond", "Вторая", Toast.SUCCESS);
        feed.post("Третья", 0xFFFF0000);
        feed.post("Четвёртая");
        assertEquals(List.of("Вторая", "Третья", "Четвёртая"), feed.lines());
        hud.tick(320, 240, 0f, 0);
        HudEntry e = hud.entry(feed);
        assertEquals(HudAnchor.CENTER_LEFT, e.anchor());
        assertEquals(3 * 13f + 2 * 2f, e.height());

        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        tick(hud, 0.5f, 1);
        canvas.begin(320, 240);
        hud.render(canvas, 320, 240, 0f, 0);
        canvas.end();
        assertTrue(canvas.ops.contains("item minecraft:diamond"), canvas.ops.toString());

        tick(hud, 0.1f, 20);
        assertEquals(0, feed.size());
        hud.tick(320, 240, 0f, 0);
        assertEquals(0f, e.height());

        // Длинная строка обрезается по ширине.
        feed.lineWidth(60).post("Очень длинное сообщение в ленте");
        assertTrue(feed.lines().get(0).endsWith("…"), feed.lines().toString());
    }

    @Test
    void countdownFollowsRealClockAndFiresOnce() {
        AtomicInteger finished = new AtomicInteger();
        Countdown c = Countdown.seconds(10).onFinish(finished::incrementAndGet);
        assertFalse(c.isRunning());
        assertEquals(10f, c.remaining());
        c.start();
        platform.advance(3f);
        assertEquals(7f, c.remaining(), 1e-3f);
        assertEquals(0.3f, c.progress(), 1e-3f);
        c.pause();
        platform.advance(5f);
        assertEquals(7f, c.remaining(), 1e-3f);
        c.start();
        platform.advance(8f);
        assertTrue(c.isFinished());
        assertTrue(c.poll());
        assertTrue(c.poll());
        assertEquals(1, finished.get());
        assertEquals(0f, c.remaining());
        c.restart(2f);
        assertTrue(c.isRunning());
        assertFalse(c.isFinished());

        Countdown sw = Countdown.stopwatch().start();
        platform.advance(75f);
        assertEquals(75f, sw.elapsed(), 1e-3f);
        assertFalse(sw.isFinished());
    }

    @Test
    void timerDisplayFormatsAndWarns() {
        Countdown c = Countdown.seconds(65).start();
        TimerDisplay timer = new TimerDisplay(c).label("Раунд").tenthsBelow(5f);
        Hud hud = new Hud();
        hud.add(timer, HudAnchor.TOP_CENTER, 0, 4);
        hud.tick(320, 240, 0.016f, 0);
        assertEquals("1:05", timer.text());
        platform.advance(0.4f);
        hud.tick(320, 240, 0.016f, 0);
        assertEquals("1:05", timer.text(), "countdown rounds up");
        platform.advance(1f);
        hud.tick(320, 240, 0.016f, 0);
        assertEquals("1:04", timer.text());
        platform.advance(61.3f);
        hud.tick(320, 240, 0.016f, 0);
        assertEquals("0:02.3", timer.text());
        AtomicBoolean done = new AtomicBoolean();
        c.onFinish(() -> done.set(true));
        platform.advance(3f);
        hud.tick(320, 240, 0.016f, 0);
        assertEquals("0:00", timer.text());
        assertTrue(done.get());

        Countdown sw = Countdown.stopwatch().start();
        timer.countdown(sw);
        platform.advance(3725f);
        hud.tick(320, 240, 0.016f, 0);
        assertEquals("1:02:05", timer.text());

        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(320, 240);
        hud.render(canvas, 320, 240, 0f, 0);
        canvas.end();
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("text 1:02:05")), canvas.ops.toString());
    }

    @Test
    void barsFollowSignals() {
        Signal<Float> health = Signal.of(20f);
        Signal<Float> max = Signal.of(20f);
        Signal<Integer> air = Signal.of(300);
        SegmentedBar hearts = new SegmentedBar().bind(health).bindMax(max);
        BossBar boss = new BossBar("Иссушитель").notches(10).bind(Signal.of(0.5f));
        RadialGauge gauge = new RadialGauge("воздух").max(300).percent(true).bind(air);
        Hud hud = new Hud();
        hud.add(hearts, HudAnchor.BOTTOM_LEFT, 4, 4);
        hud.add(boss, HudAnchor.TOP_CENTER, 0, 2);
        hud.add(gauge, HudAnchor.BOTTOM_RIGHT, 4, 4);
        hud.tick(320, 240, 0f, 0);
        assertEquals(10 * 8f + 9f, hud.entry(hearts).width());
        assertEquals(182f, hud.entry(boss).width());
        assertEquals("100%", gauge.number());

        health.set(4f);
        assertEquals(4f, hearts.value());
        air.set(150);
        assertEquals("50%", gauge.number());
        max.set(40f);
        assertEquals(40f, hearts.max());
        assertEquals(0.5f, boss.progress());

        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        for (int i = 0; i < 30; i++) {
            canvas.begin(320, 240);
            hud.render(canvas, 320, 240, 0f, 0);
            canvas.end();
            hud.tick(320, 240, 0.05f, 0);
        }
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("text Иссушитель")));
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("text 50%")));

        // Удалённый с HUD виджет отписывается от сигнала.
        hud.remove(gauge);
        air.set(0);
        assertEquals("50%", gauge.number());
    }

    @Test
    void globalHudAndClientTick() {
        Hud hud = Hud.get();
        assertSame(hud, Hud.get());
        AtomicInteger ticks = new AtomicInteger();
        var sub = hud.onClientTick(ticks::incrementAndGet);
        hud.clientTick();
        hud.clientTick();
        sub.close();
        hud.clientTick();
        assertEquals(2, ticks.get());
        assertNotNull(hud.theme().rawStyle("toast"), "built-in themes include HUD styles");
    }

    private static void tick(Hud hud, float dt, int n) {
        for (int i = 0; i < n; i++) {
            hud.tick(320, 240, dt, 0);
        }
    }
}
