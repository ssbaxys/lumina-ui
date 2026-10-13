package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.inventory.Slot;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.AbstractCanvas;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.EntityOptions;
import io.github.ssbaxys.lumina.render.ShapePaint;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.widget.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Страница предметов: рисуется, стопки перетаскиваются между сундуком и инвентарём, мобы и игрок рисуются. */
class ItemsPageTest {
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
        });
    }

    @Test
    void pageWorksEndToEnd() {
        UiRoot root = new UiRoot(ItemsPage.create(), ShowcaseTheme.dark());
        root.resize(900, 2400);
        Canvas canvas = new Canvas();
        frames(root, canvas, 3);
        assertTrue(canvas.items.contains("minecraft:diamond"), "chest items are drawn");
        assertTrue(canvas.entities.containsAll(Set.of("minecraft:zombie", "minecraft:creeper", "minecraft:villager",
                io.github.ssbaxys.lumina.render.Canvas.PLAYER)),
                "mobs and the player: " + canvas.entities);

        List<Slot> slots = new ArrayList<>();
        collect(root.content(), slots);
        assertTrue(slots.size() >= 2 * 63, "two chest windows: " + slots.size());

        // Первый слот сундука (алмазы) → первая пустая ячейка рюкзака того же окна.
        Slot from = slots.get(0);
        Slot to = slots.get(27 + 1);
        assertEquals(12, from.stack().count());
        assertTrue(to.stack().isEmpty());
        drag(root, from, to);
        assertTrue(from.stack().isEmpty());
        assertEquals(12, to.stack().count());
        assertEquals(12, slots.get(63 + 27 + 1).stack().count(), "the modern window shows the same inventory");
        assertTrue(findLabel(root.content(), "Перенесено: 12 × Diamond → ячейка 10"), "status updated");

        frames(root, canvas, 2);
        assertTrue(canvas.texts.contains("12"), "count is drawn");
    }

    private static void drag(UiRoot root, UiElement from, UiElement to) {
        root.tick(0f);
        float x0 = from.screenX() + from.width() / 2f;
        float y0 = from.screenY() + from.height() / 2f;
        float x1 = to.screenX() + to.width() / 2f;
        float y1 = to.screenY() + to.height() / 2f;
        root.pointerMove(x0, y0);
        root.pointerDown(x0, y0, Keys.MOUSE_LEFT);
        root.pointerMove(x0 + 6f, y0);
        root.pointerMove(x1, y1);
        root.pointerUp(x1, y1, Keys.MOUSE_LEFT);
    }

    private static void collect(UiElement e, List<Slot> out) {
        if (e instanceof Slot s) {
            out.add(s);
        }
        for (UiElement c : e.children()) {
            collect(c, out);
        }
    }

    private static boolean findLabel(UiElement e, String text) {
        if (e instanceof Label l && text.equals(l.text())) {
            return true;
        }
        for (UiElement c : e.children()) {
            if (findLabel(c, text)) {
                return true;
            }
        }
        return false;
    }

    private static void frames(UiRoot root, Canvas canvas, int n) {
        for (int i = 0; i < n; i++) {
            canvas.begin(root.width(), root.height());
            root.tick(1f / 60f);
            root.render(canvas, 0f);
            canvas.end();
        }
    }

    private static final class Canvas extends AbstractCanvas {
        final Set<String> items = new HashSet<>();
        final Set<String> entities = new HashSet<>();
        final Set<String> texts = new HashSet<>();

        void begin(float w, float h) {
            items.clear();
            entities.clear();
            texts.clear();
            beginFrame(w, h);
        }

        void end() {
            endFrame();
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
            texts.add(text);
        }

        @Override
        protected void drawTextureQuadImpl(TextureRef texture, float x0, float y0, float x1, float y1,
                                           float x2, float y2, float x3, float y3,
                                           float u0, float v0, float u1, float v1,
                                           float u2, float v2, float u3, float v3, int tint) {
        }

        @Override
        protected void drawItemImpl(String itemId, float x, float y, float size) {
            items.add(itemId);
        }

        @Override
        protected void drawEntityImpl(String type, float x, float y, float size, float yaw, float pitch,
                                      EntityOptions options) {
            entities.add(type);
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
