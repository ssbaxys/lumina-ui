package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.blocks.BlockStructure;
import io.github.ssbaxys.lumina.blocks.DemoWorld;
import io.github.ssbaxys.lumina.blocks.WorldBlocks;
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
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.StructurePreview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Страница 3D-построек: превью мира без игры (демо-мир) и с поддельным миром, срез слоёв и копирование текстом. */
class StructuresPageTest {
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

    static String clipboard = "";
    static WorldBlocks world;

    @BeforeEach
    void init() {
        clipboard = "";
        world = null;
        Lumina.init(new Platform() {
            @Override
            public TextMetrics textMetrics() {
                return METRICS;
            }

            @Override
            public String getClipboard() {
                return clipboard;
            }

            @Override
            public void setClipboard(String text) {
                clipboard = text;
            }

            @Override
            public void playClickSound() {
            }

            @Override
            public WorldBlocks worldBlocks() {
                return world;
            }
        });
    }

    @Test
    void worldPreviewWithoutAndWithAWorld() {
        Panel host = Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH)).fill();
        UiRoot root = new UiRoot(host, ShowcaseTheme.dark());
        root.resize(900, 2400);
        host.add(StructuresPage.create());
        Canvas canvas = new Canvas();
        frames(root, canvas, 10);
        UiElement content = root.content();
        StructurePreview preview = (StructurePreview) content.findById("world-preview");

        // главное меню: демо-мир
        assertFalse(preview.hasWorld());
        assertTrue(preview.world() instanceof DemoWorld);
        assertTrue(preview.capture().passes() > 0, "demo captured");
        assertTrue(preview.structure().size() > 0);
        assertTrue(preview.visibleFaceCount() > 0);

        // снять и вернуть слой
        ((Button) content.findById("world-peel")).press();
        ((Button) content.findById("world-peel")).press();
        assertEquals(2, preview.peel());
        assertEquals(preview.capture().localBox().maxY() - 2, preview.cutAboveLayer());
        ((Button) content.findById("world-unpeel")).press();
        assertEquals(1, preview.peel());

        // поделиться текстом: строка разбирается обратно в ту же постройку
        ((Button) content.findById("world-copy")).press();
        assertTrue(clipboard.startsWith(BlockStructure.FORMAT + ";"), clipboard);
        assertEquals(preview.structure().size(), BlockStructure.decode(clipboard).size());
        ((Button) content.findById("world-to-gallery")).press();

        // появился мир — снимок пересобирается из него
        world = new WorldBlocks() {
            @Override
            public String blockAt(int x, int y, int z) {
                return y <= 64 ? "minecraft:stone" : null;
            }

            @Override
            public double playerX() {
                return 100.5;
            }

            @Override
            public double playerY() {
                return 65;
            }

            @Override
            public double playerZ() {
                return -40.5;
            }

            @Override
            public float playerYaw() {
                return 90f;
            }
        };
        frames(root, canvas, 5);
        assertTrue(preview.hasWorld());
        BlockStructure.Bounds box = preview.capture().worldBox();
        assertEquals(90, box.minX());
        assertEquals(21 * 21 * 5, preview.structure().size(), "y 60..64 under the player, 21×21 each");
        ((Button) content.findById("world-refresh")).press();
        frames(root, canvas, 2);
        assertTrue(preview.hasWorld());
    }

    private static void frames(UiRoot root, Canvas canvas, int n) {
        for (int i = 0; i < n; i++) {
            canvas.begin(root.width(), root.height());
            root.tick(1f / 30f);
            root.render(canvas, 0f);
            canvas.end();
        }
    }

    /** Холст без вывода. */
    private static final class Canvas extends AbstractCanvas {
        void begin(float w, float h) {
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
        }

        @Override
        protected void drawTextureQuadImpl(TextureRef texture, float x0, float y0, float x1, float y1,
                                           float x2, float y2, float x3, float y3,
                                           float u0, float v0, float u1, float v1,
                                           float u2, float v2, float u3, float v3, int tint) {
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
