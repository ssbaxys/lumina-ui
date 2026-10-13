package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.AbstractCanvas;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.ShapePaint;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Открывает каждую страницу витрины, рисует кадры и водит мышью: ловит исключения без запуска игры. */
class ShowcaseSmokeTest {
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
    void everyPageRendersAndReactsToInput() {
        boolean[] closed = {false};
        UiRoot root = Showcase.create(() -> closed[0] = true);
        root.resize(854, 480);
        CountingCanvas canvas = new CountingCanvas();
        frame(root, canvas);

        List<Button> nav = new ArrayList<>();
        collectNav(root.content(), nav);
        assertEquals(25, nav.size());

        for (Button b : nav) {
            for (UiElement p = b.parent(); p != null; p = p.parent()) {
                if (p instanceof io.github.ssbaxys.lumina.widget.ScrollPanel sp) {
                    sp.scrollIntoView(b, false);
                    break;
                }
            }
            frame(root, canvas);
            click(root, b);
            for (int i = 0; i < 30; i++) {
                frame(root, canvas);
            }
            assertTrue(b.hasState(State.CHECKED), b.text());
            assertTrue(canvas.draws > 0, "page " + b.text() + " drew nothing");
            // поводить мышью и покрутить колесо по всей странице
            for (int y = 40; y < 440; y += 37) {
                for (int x = 160; x < 820; x += 61) {
                    root.pointerMove(x, y);
                }
                root.scroll(500, y, 0, -1);
                frame(root, canvas);
            }
            root.keyDown(Keys.TAB, 0, 0);
            root.keyDown(Keys.SPACE, 0, 0);
            frame(root, canvas);
        }
    }

    @Test
    void markupDemoLoadsFromResources() {
        String html = Lumina.platform().readResource("luminatest:ui/markup_demo.html");
        assertTrue(html != null && html.contains("<state"), "demo html on classpath");
        io.github.ssbaxys.lumina.markup.Markup.Built built = io.github.ssbaxys.lumina.markup.Markup.build(html,
                new io.github.ssbaxys.lumina.markup.Bindings());
        assertEquals(1, built.linkedResources().size());
        assertTrue(built.stylesheet().variables().containsKey("--gold"), "linked css parsed");
        List<String> texts = new ArrayList<>();
        collectText(built.root(), texts);
        assertTrue(texts.contains("120"), "template {{coins}} resolved: " + texts);
        built.dispose();
    }

    private static void collectText(UiElement e, List<String> out) {
        if (e instanceof io.github.ssbaxys.lumina.widget.Label l) {
            out.add(l.text());
        }
        for (UiElement c : e.children()) {
            collectText(c, out);
        }
    }

    @Test
    void languageSwitchRetranslatesOpenScreen() {
        UiRoot root = Showcase.create(() -> { });
        root.resize(854, 480);
        CountingCanvas canvas = new CountingCanvas();
        List<Button> nav = new ArrayList<>();
        collectNav(root.content(), nav);
        try {
            I18n.setLanguage("en_us");
            frame(root, canvas);
            assertEquals("Overview", nav.get(0).text());
            float englishWidth = nav.get(24).width();

            I18n.setLanguage("ru_ru");
            frame(root, canvas);
            assertEquals("Обзор", nav.get(0).text());
            assertEquals("Производительность", nav.get(24).text());
            assertEquals(englishWidth, nav.get(24).width());

            // Каждая страница открывается и рисуется на обоих языках, переключение — прямо на открытой.
            for (Button b : nav) {
                click(root, b);
                frame(root, canvas);
                I18n.nextLanguage();
                frame(root, canvas);
                assertTrue(canvas.draws > 0, "page " + b.text() + " drew nothing");
            }
        } finally {
            I18n.followGameLanguage();
        }
    }

    private static void click(UiRoot root, UiElement e) {
        float x = e.screenX() + e.width() / 2f;
        float y = e.screenY() + e.height() / 2f;
        root.pointerMove(x, y);
        root.pointerDown(x, y, Keys.MOUSE_LEFT);
        root.pointerUp(x, y, Keys.MOUSE_LEFT);
    }

    private static void frame(UiRoot root, CountingCanvas canvas) {
        canvas.begin(root.width(), root.height());
        root.tick(1f / 60f);
        root.content().render(canvas, 0f);
        canvas.end();
    }

    private static void collectNav(UiElement e, List<Button> out) {
        if (e instanceof Button b && "nav".equals(b.styleClass())) {
            out.add(b);
        }
        for (UiElement c : e.children()) {
            collectNav(c, out);
        }
    }

    @Test
    void englishHasNoRussianText() {
        I18n.setLanguage("en_us");
        try {
            UiRoot root = Showcase.create(() -> { });
            root.resize(854, 480);
            CountingCanvas canvas = new CountingCanvas();
            List<Button> nav = new ArrayList<>();
            collectNav(root.content(), nav);
            java.util.regex.Pattern cyrillic = java.util.regex.Pattern.compile("[А-Яа-яЁё]");
            for (Button b : nav) {
                b.press();
                for (int i = 0; i < 20; i++) {
                    frame(root, canvas);
                }
                List<String> texts = new ArrayList<>();
                collectTexts(root.content(), texts);
                for (String t : texts) {
                    assertTrue(t.equals("Русский") || !cyrillic.matcher(t).find(), "page " + b.text() + " is not translated: " + t);
                }
            }
        } finally {
            I18n.setLanguage("ru_ru");
        }
    }

    private static void collectTexts(UiElement e, List<String> out) {
        if (e instanceof Label l) {
            out.add(l.text());
        } else if (e instanceof Button b) {
            out.add(b.text());
        }
        for (UiElement c : e.children()) {
            collectTexts(c, out);
        }
    }

    private static final class CountingCanvas extends AbstractCanvas {
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
