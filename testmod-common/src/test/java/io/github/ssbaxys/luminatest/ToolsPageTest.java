package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.config.ConfigScreen;
import io.github.ssbaxys.lumina.input.Keys;
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
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.AnimatedImage;
import io.github.ssbaxys.lumina.widget.Backdrop;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.MiniMap;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Slider;
import io.github.ssbaxys.lumina.widget.Toggle;
import io.github.ssbaxys.lumina.widget.WorldMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Страница инструментов без игры: размытие, обрезка, GIF, карты, частицы, инспектор и настройки. */
class ToolsPageTest {
    @org.junit.jupiter.api.BeforeEach
    void russianTexts() {
        io.github.ssbaxys.lumina.i18n.I18n.setLanguage("ru_ru");
    }

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

    @TempDir
    static Path dataDir;

    @BeforeAll
    static void init() {
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
            public TextureRef createTexture(String key, int width, int height, int[] argb) {
                return TextureRef.of("luminaui:dynamic/" + key);
            }

            @Override
            public Path dataDirectory() {
                return dataDir;
            }
        });
    }

    @Test
    void everyToolWorks() throws Exception {
        Panel host = Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH)).fill();
        UiRoot root = new UiRoot(host, ShowcaseTheme.dark());
        root.resize(900, 2400);
        host.add(ToolsPage.create());
        Canvas canvas = new Canvas();
        frames(root, canvas, 20);
        UiElement content = root.content();

        // Размытие: бэкенд отдал размытую копию, ползунок меняет радиус.
        assertTrue(canvas.count("blur") > 0, "blurred copy drawn: " + canvas.textures);
        UiElement glass = content.findById("glass");
        assertEquals(8f, glass.computedStyle().get(Styles.BACKDROP_BLUR));
        ((Slider) content.findById("blur-slider")).value().set(16f);
        frames(root, canvas, 2);
        assertEquals(16f, glass.computedStyle().get(Styles.BACKDROP_BLUR));
        assertEquals(16f, canvas.lastBlurRadius, 0.01f);

        // Скруглённая обрезка: углы закрашены снимками.
        assertTrue(canvas.count("snap") > 0, "corners repainted: " + canvas.textures);
        assertTrue(content.findById("rounded-scroll").computedStyle().get(Styles.CLIP_ROUNDED));

        // GIF: кадры идут, пауза останавливает.
        assertTrue(canvas.textures.keySet().stream().anyMatch(t -> t.startsWith("luminaui:dynamic/luminatest/tools/spinner/")),
                canvas.textures.toString());
        AnimatedImage gif = (AnimatedImage) content.findById("gif-animated");
        assertTrue(gif.animation().frameCount() == 12);
        int before = gif.frame();
        frames(root, canvas, 5);
        assertNotEquals(before, gif.frame(), "animates");
        ((Button) content.findById("gif-pause")).press();
        assertFalse(gif.isPlaying());
        int paused = gif.frame();
        frames(root, canvas, 10);
        assertEquals(paused, gif.frame());

        // Карты: демо-источник без мира.
        assertNotNull(((MiniMap) content.findById("minimap-round")).lastSample());
        assertNotNull(((MiniMap) content.findById("minimap-square")).lastSample());
        assertEquals(256 * 256, ((WorldMap) content.findById("world-map")).colors().length);
        assertTrue(canvas.textures.keySet().stream().anyMatch(t -> t.contains("minimap")), canvas.textures.toString());

        // Частицы двигаются.
        for (String id : new String[]{"particles-sparks", "particles-snow", "particles-dust"}) {
            Backdrop b = (Backdrop) content.findById(id);
            float[] p = b.particlePosition(0);
            assertNotNull(p, id);
            frames(root, canvas, 10);
            float[] q = b.particlePosition(0);
            assertTrue(p[0] != q[0] || p[1] != q[1], id);
        }

        // Инспектор: F8 выключена, кнопка включает и открывает, Esc закрывает, переключатель выключает.
        Toggle key = (Toggle) content.findById("inspector-key");
        assertFalse(root.keyDown(Keys.F8, 0, 0), "F8 is off by default");
        ((Button) content.findById("open-inspector")).press();
        assertTrue(root.inspector().isOpen());
        assertTrue(key.value().peek(), "toggle follows the button");
        frames(root, canvas, 2);
        assertTrue(root.keyDown(Keys.ESCAPE, 0, 0));
        assertFalse(root.inspector().isOpen());
        assertTrue(root.keyDown(Keys.F8, 0, 0));
        assertTrue(root.inspector().isOpen());
        key.toggle();
        assertFalse(root.inspector().isOpen());
        assertFalse(root.keyDown(Keys.F8, 0, 0));
        ((Toggle) content.findById("gamepad-toggle")).toggle();
        assertFalse(root.gamepad().isEnabled());

        // Настройки: строки построены, сохранение пишет файл.
        ConfigScreen config = (ConfigScreen) content.findById("config");
        assertTrue(config.rows().size() >= 4, "rows: " + config.rows().size());
        config.spec().integer("minimapSize").set(128);
        ((Button) config.findById("config-save")).press();
        Path file = dataDir.resolve("luminatest/tools.toml");
        assertTrue(Files.readString(file).contains("minimapSize = 128"));
        assertEquals(128, reload());
    }

    /** Свежий экземпляр описания читает сохранённый файл. */
    private static int reload() {
        var spec = ToolsPage.spec();
        spec.load();
        return spec.integer("minimapSize").peek();
    }

    private static void frames(UiRoot root, Canvas canvas, int n) {
        for (int i = 0; i < n; i++) {
            canvas.begin(root.width(), root.height());
            root.tick(1f / 30f);
            root.render(canvas, 0f);
            canvas.end();
        }
    }

    /** Холст, который умеет «снимки» и «размытие» и считает, какие текстуры рисовались. */
    private static final class Canvas extends AbstractCanvas {
        final Map<String, Integer> textures = new HashMap<>();
        float lastBlurRadius;

        void begin(float w, float h) {
            textures.clear();
            beginFrame(w, h);
        }

        void end() {
            endFrame();
        }

        int count(String prefix) {
            return textures.entrySet().stream().filter(e -> e.getKey().startsWith(prefix))
                    .mapToInt(Map.Entry::getValue).sum();
        }

        @Override
        protected TextureRef captureBackdrop(float radius) {
            lastBlurRadius = radius;
            return TextureRef.of("blur");
        }

        @Override
        protected boolean backdropBlurAvailable() {
            return true;
        }

        @Override
        protected TextureRef captureSnapshot(int slot) {
            return TextureRef.of("snap" + slot);
        }

        @Override
        protected int snapshotSlots() {
            return 8;
        }

        @Override
        protected void drawQuad(float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl) {
        }

        @Override
        protected void drawFreeQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                    int c0, int c1, int c2, int c3) {
        }

        @Override
        protected void drawShape(float x0, float y0, float x1, float y1, float rTL, float rTR, float rBR, float rBL,
                                 int tl, int tr, int br, int bl, float stroke, float softness, ShapePaint paint) {
        }

        @Override
        protected void applyBlendMode(BlendMode mode) {
        }

        @Override
        protected void drawTextImpl(String text, float x, float y, float scale, int argb, boolean shadow) {
        }

        @Override
        protected void drawTextureQuadImpl(TextureRef texture, float x0, float y0, float x1, float y1,
                                           float x2, float y2, float x3, float y3,
                                           float u0, float v0, float u1, float v1,
                                           float u2, float v2, float u3, float v3, int tint) {
            textures.merge(texture.id(), 1, Integer::sum);
        }

        @Override
        protected void drawItemImpl(String itemId, float x, float y, float size) {
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
