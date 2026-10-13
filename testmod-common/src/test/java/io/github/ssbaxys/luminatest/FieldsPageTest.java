package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.AbstractCanvas;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.ShapePaint;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.widget.FieldBase;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Страница полей ввода: рисуется, печатает, открывает контекстные меню — без исключений. */
class FieldsPageTest {
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
    void pageRendersAndEveryFieldTakesInput() {
        UiRoot root = new UiRoot(FieldsPage.create(), ShowcaseTheme.dark());
        root.resize(640, 480);
        Canvas canvas = new Canvas();
        frame(root, canvas);

        List<FieldBase<?>> fields = new ArrayList<>();
        collect(root.content(), fields);
        assertTrue(fields.size() >= 15, "fields on the page: " + fields.size());

        int menus = 0;
        for (FieldBase<?> f : fields) {
            if (f.screenY() < 0f || f.screenY() + f.height() > root.height()) {
                continue;
            }
            float x = f.screenX() + f.width() / 2f;
            float y = f.screenY() + f.height() / 2f;
            root.pointerMove(x, y);
            root.pointerDown(x, y, Keys.MOUSE_LEFT);
            root.pointerUp(x, y, Keys.MOUSE_LEFT);
            root.charTyped('7', 0);
            root.keyDown(Keys.UP, 0, 0);
            root.scroll(x, y, 0, 1);
            root.pointerDown(x, y, Keys.MOUSE_RIGHT);
            root.pointerUp(x, y, Keys.MOUSE_RIGHT);
            if (f.isEnabled()) {
                assertEquals(1, root.popups().size(), "context menu for " + f);
                menus++;
            }
            frame(root, canvas);
            root.keyDown(Keys.ESCAPE, 0, 0);
            frame(root, canvas);
        }
        assertTrue(menus > 0);
        assertTrue(canvas.draws > 0);
    }

    private static void collect(UiElement e, List<FieldBase<?>> out) {
        if (e instanceof FieldBase<?> f) {
            out.add(f);
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
        int draws;

        void begin(float w, float h) {
            draws = 0;
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
