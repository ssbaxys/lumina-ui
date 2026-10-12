package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.game.GameData;
import io.github.ssbaxys.lumina.hud.Hud;
import io.github.ssbaxys.lumina.hud.HudEntry;
import io.github.ssbaxys.lumina.hud.NotificationFeed;
import io.github.ssbaxys.lumina.hud.TimerDisplay;
import io.github.ssbaxys.lumina.hud.Toast;
import io.github.ssbaxys.lumina.hud.ToastStack;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.AbstractCanvas;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.ShapePaint;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Toggle;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Страница HUD: живые данные, тосты, таймер, лента и демо-HUD работают из интерфейса. */
class HudPageTest {
    private static final TextMetrics METRICS = new TextMetrics() {
        @Override
        public float width(String text) {
            return text.length() * 6f;
        }

        @Override
        public float lineHeight() {
            return 9f;
        }
    };

    private long nanos = 5_000_000_000L;
    private final List<String> items = new ArrayList<>();

    @BeforeEach
    void init() {
        Lumina.init(new Platform() {
            @Override
            public TextMetrics textMetrics() {
                return METRICS;
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
            public long nanoTime() {
                return nanos;
            }
        });
        GameData.reset();
    }

    @AfterEach
    void cleanup() {
        HudDemo.setActive(false);
        Hud.get().clear();
        GameData.reset();
        HudDemo.TIMER.reset();
    }

    @Test
    void pageShowsLiveDataAndDrivesTheHud() {
        float[] health = {18f};
        GameData.setSource(out -> {
            out.health = health[0];
            out.food = 9;
            out.x = 100.4;
            out.y = 70;
            out.z = -12.6;
            out.biome = "minecraft:desert";
            out.dimension = "minecraft:overworld";
            out.dayTime = 18000;
            out.fps = 60;
            out.air = 150;
            out.setSlot(0, "minecraft:diamond_pickaxe", 1);
            out.setSlot(2, "minecraft:torch", 48);
            out.selectedSlot = 2;
            return true;
        });
        GameData.update();

        Panel host = Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH)).fill();
        UiRoot root = new UiRoot(host, ShowcaseTheme.dark());
        root.resize(640, 2400);
        UiElement page = HudPage.create();
        host.add(page);
        RecordingCanvas canvas = new RecordingCanvas();
        frames(root, canvas, 20);
        assertTrue(canvas.draws > 0);
        assertTrue(items.contains("minecraft:torch"), "hotbar draws items");

        // Живые значения.
        assertEquals("да", label(root, "in-world").text());
        assertEquals("100 70 -13", label(root, "position").text());
        assertEquals("desert", label(root, "biome").text());
        assertEquals("00:00", label(root, "clock").text());
        assertEquals("torch", label(root, "held").text());
        assertEquals("18 / 20", label(root, "health-text").text());
        assertEquals("minecraft:diamond_pickaxe", ((HudPage.HotbarView) root.content().findById("hotbar")).item(0));
        health[0] = 6.5f;
        GameData.update();
        assertEquals("6.5 / 20", label(root, "health-text").text());

        // Тосты: три на экране, остальные в очереди.
        ToastStack toasts = (ToastStack) root.content().findById("preview-toasts");
        press(root, "toast-info");
        press(root, "toast-success");
        press(root, "toast-error");
        press(root, "toast-progress");
        assertEquals(3, toasts.visible().size());
        assertEquals(1, toasts.queued());
        frames(root, canvas, 30);
        assertTrue(canvas.texts.contains("Подсказка"), canvas.texts.toString());
        press(root, "toast-global");
        assertEquals(1, Hud.get().toasts().visible().size());

        // Таймер на 10 секунд по часам платформы.
        TimerDisplay timer = (TimerDisplay) root.content().findById("timer");
        press(root, "timer-10");
        frames(root, canvas, 1);
        assertEquals("0:10", timer.text());
        nanos += 4_000_000_000L;
        frames(root, canvas, 1);
        assertEquals("0:06", timer.text());
        nanos += 2_500_000_000L;
        frames(root, canvas, 1);
        assertEquals("0:03.5", timer.text());
        press(root, "timer-pause");
        nanos += 60_000_000_000L;
        frames(root, canvas, 1);
        assertEquals("0:03.5", timer.text());
        press(root, "timer-pause");
        nanos += 4_000_000_000L;
        frames(root, canvas, 400);
        assertEquals("0:00", timer.text());
        assertTrue(toasts.visible().stream().anyMatch(t -> t.title().equals("Время вышло"))
                || toastTitles(toasts).contains("Время вышло"), toastTitles(toasts).toString());

        // Лента.
        NotificationFeed feed = (NotificationFeed) root.content().findById("preview-feed");
        press(root, "feed-event");
        press(root, "feed-loot");
        press(root, "feed-danger");
        assertEquals(3, feed.size());
        assertTrue(feed.lines().get(2).contains("крипер"));

        // Демо-HUD поверх игры.
        Toggle demo = (Toggle) root.content().findById("demo-toggle");
        assertFalse(demo.isOn());
        demo.toggle();
        assertTrue(HudDemo.isActive());
        Hud hud = Hud.get();
        int withDemo = hud.entries().size();
        assertTrue(withDemo >= 6, hud.entries().toString());
        RecordingCanvas hudCanvas = new RecordingCanvas();
        for (int i = 0; i < 10; i++) {
            hudCanvas.begin(480, 270);
            hud.render(hudCanvas, 480, 270, 0f, 0);
            hudCanvas.end();
        }
        assertTrue(hudCanvas.texts.contains("XYZ: 100 70 -13"), hudCanvas.texts.toString());
        assertTrue(hudCanvas.texts.contains("Опыт"));
        HudEntry air = null;
        for (HudEntry e : hud.entries()) {
            if (e.element() instanceof io.github.ssbaxys.lumina.hud.RadialGauge) {
                air = e;
            }
        }
        assertNotNull(air);
        assertTrue(air.isShown(), "air gauge visible while air is running out");
        demo.toggle();
        assertFalse(HudDemo.isActive());
        assertEquals(withDemo - 5, hud.entries().size());

        // Ушедшая страница больше не слушает GameData.
        Label position = label(root, "position");
        host.removeChild(page);
        GameData.setSource(out -> true);
        GameData.update();
        assertEquals("100 70 -13", position.text());
    }

    private static List<String> toastTitles(ToastStack stack) {
        List<String> out = new ArrayList<>();
        for (Toast t : stack.visible()) {
            out.add(t.title());
        }
        return out;
    }

    private static Label label(UiRoot root, String id) {
        UiElement e = root.content().findById(id);
        assertNotNull(e, id);
        return (Label) e;
    }

    private static void press(UiRoot root, String id) {
        Button b = (Button) root.content().findById(id);
        assertNotNull(b, id);
        b.press();
    }

    private static void frames(UiRoot root, RecordingCanvas canvas, int n) {
        for (int i = 0; i < n; i++) {
            canvas.begin(root.width(), root.height());
            root.tick(1f / 60f);
            root.render(canvas, 0f);
            canvas.end();
        }
    }

    private final class RecordingCanvas extends AbstractCanvas {
        int draws;
        final List<String> texts = new ArrayList<>();

        void begin(float w, float h) {
            draws = 0;
            texts.clear();
            beginFrame(w, h);
        }

        void end() {
            endFrame();
        }

        @Override
        protected void drawQuad(float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl) {
            draws++;
        }

        @Override
        protected void drawFreeQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                    int c0, int c1, int c2, int c3) {
            draws++;
        }

        @Override
        protected void drawShape(float x0, float y0, float x1, float y1, float rTL, float rTR, float rBR, float rBL,
                                 int tl, int tr, int br, int bl, float stroke, float softness, ShapePaint paint) {
            draws++;
        }

        @Override
        protected void applyBlendMode(BlendMode mode) {
        }

        @Override
        protected void drawTextImpl(String text, float x, float y, float scale, int argb, boolean shadow) {
            draws++;
            texts.add(text);
        }

        @Override
        protected void drawTextureQuadImpl(TextureRef texture, float x0, float y0, float x1, float y1,
                                           float x2, float y2, float x3, float y3,
                                           float u0, float v0, float u1, float v1,
                                           float u2, float v2, float u3, float v3, int tint) {
            draws++;
        }

        @Override
        protected void drawItemImpl(String itemId, float x, float y, float size) {
            draws++;
            items.add(itemId);
        }

        @Override
        protected void applyClip(float x0, float y0, float x1, float y1) {
        }

        @Override
        protected void clearClip() {
        }

        @Override
        public TextMetrics textMetrics() {
            return METRICS;
        }
    }
}
