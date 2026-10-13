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
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.style.ModernTheme;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.SideNav;
import io.github.ssbaxys.lumina.widget.Tabs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Современная страница: тема включается и возвращается, все разделы и вкладки открываются. */
class ModernPageTest {
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
    void modernPageSwitchesThemeAndEveryPageWorks() {
        Panel host = Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH)).fill();
        Theme showcase = ShowcaseTheme.dark();
        UiRoot root = new UiRoot(host, showcase);
        root.resize(640, 400);
        UiElement page = ModernPage.create();
        host.add(page);
        Canvas canvas = new Canvas();
        frame(root, canvas);
        assertEquals(ModernTheme.DEFAULT_ACCENT, root.theme().get(Tokens.ACCENT), "modern theme while shown");

        SideNav nav = find(root.content(), SideNav.class);
        // пункты по центру меню по высоте
        float itemsTop = nav.screenY() + nav.padding().top()
                + Math.max(0f, (nav.height() - nav.padding().vertical() - 6 * 44f) / 2f);
        for (int i = 0; i < 6; i++) {
            float x = nav.screenX() + nav.width() / 2f;
            float y = itemsTop + 44f * i + 22f;
            click(root, x, y, Keys.MOUSE_LEFT);
            for (int f = 0; f < 30; f++) {
                frame(root, canvas);
            }
            assertTrue(canvas.draws > 0, "section " + i);
        }
        click(root, nav.screenX() + nav.width() / 2f, itemsTop + 44f + 22f, Keys.MOUSE_LEFT);
        for (int f = 0; f < 30; f++) {
            frame(root, canvas);
        }
        Tabs tabs = find(root.content(), Tabs.class);
        for (int t = 0; t < 5; t++) {
            click(root, tabs.screenX() + tabs.width() * (t + 0.5f) / 5f, tabs.screenY() + tabs.height() / 2f, Keys.MOUSE_LEFT);
            for (int f = 0; f < 30; f++) {
                frame(root, canvas);
            }
            for (int y = 60; y < 390; y += 23) {
                root.pointerMove(400, y);
            }
            frame(root, canvas);
        }
        // вкладка «Вид»: правый клик по предпросмотру открывает меню
        click(root, tabs.screenX() + tabs.width() * 1.5f / 5f, tabs.screenY() + tabs.height() / 2f, Keys.MOUSE_LEFT);
        for (int f = 0; f < 30; f++) {
            frame(root, canvas);
        }
        Label preview = findLabel(root.content(), "Предпросмотр");
        UiElement card = preview.parent();
        click(root, card.screenX() + 4, card.screenY() + 4, Keys.MOUSE_RIGHT);
        assertEquals(1, root.popups().size(), "preview context menu");
        frame(root, canvas);
        root.keyDown(Keys.ESCAPE, 0, 0);
        frame(root, canvas);

        host.removeChild(page);
        assertSame(showcase, root.theme(), "theme restored after leaving the page");
    }

    private static void click(UiRoot root, float x, float y, int button) {
        root.pointerMove(x, y);
        root.pointerDown(x, y, button);
        root.pointerUp(x, y, button);
    }

    private static <T> T find(UiElement e, Class<T> type) {
        if (type.isInstance(e) && e.isVisible()) {
            return type.cast(e);
        }
        for (UiElement c : e.children()) {
            T found = find(c, type);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Label findLabel(UiElement e, String text) {
        if (e instanceof Label l && text.equals(l.text()) && e.isVisible()) {
            return l;
        }
        for (UiElement c : e.children()) {
            if (!c.isVisible()) {
                continue;
            }
            Label found = findLabel(c, text);
            if (found != null) {
                return found;
            }
        }
        return null;
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
