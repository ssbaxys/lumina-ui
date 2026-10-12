package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.Lumina;
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
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Checkbox;
import io.github.ssbaxys.lumina.widget.ImageButton;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Страница кнопок без игры: рисуется, реагирует на мышь, клики и клавиатуру без исключений. */
class ButtonsPageSmokeTest {
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
    void pageRendersAndReacts() {
        Panel screen = Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH)).fill().add(ButtonsPage.create());
        UiRoot root = new UiRoot(screen, ShowcaseTheme.dark());
        root.resize(640, 2000);
        Canvas canvas = new Canvas();
        frame(root, canvas);
        assertTrue(canvas.textures > 0 && canvas.items > 0, "icons and pictures are drawn");

        List<UiElement> clickable = new ArrayList<>();
        collect(root.content(), clickable);
        assertTrue(clickable.size() > 40, "found " + clickable.size());
        for (UiElement e : clickable) {
            float x = e.screenX() + e.width() / 2f;
            float y = e.screenY() + e.height() / 2f;
            root.pointerMove(x, y);
            root.pointerDown(x, y, Keys.MOUSE_LEFT);
            frame(root, canvas);
            root.pointerUp(x, y, Keys.MOUSE_LEFT);
            frame(root, canvas);
        }
        for (int i = 0; i < 20; i++) {
            root.keyDown(Keys.TAB, 0, 0);
            root.keyDown(Keys.SPACE, 0, 0);
            root.keyDown(Keys.DOWN, 0, 0);
            frame(root, canvas);
        }
        for (int i = 0; i < 300; i++) {
            frame(root, canvas);
        }
        root.setTheme(ShowcaseTheme.light());
        frame(root, canvas);
    }

    private static void collect(UiElement e, List<UiElement> out) {
        if (e instanceof Button || e instanceof Checkbox || e instanceof ImageButton) {
            out.add(e);
        }
        for (UiElement c : e.children()) {
            collect(c, out);
        }
    }

    private static void frame(UiRoot root, Canvas canvas) {
        canvas.begin(root.width(), root.height());
        root.tick(1f / 60f);
        root.render(canvas, 0f);
        canvas.end();
    }

    private static final class Canvas extends AbstractCanvas {
        int textures;
        int items;

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
            textures++;
        }

        @Override
        protected void drawItemImpl(String itemId, float x, float y, float size) {
            items++;
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
