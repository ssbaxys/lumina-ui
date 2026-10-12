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
import io.github.ssbaxys.lumina.widget.DatePicker;
import io.github.ssbaxys.lumina.widget.KeyBindField;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.RadialMenu;
import io.github.ssbaxys.lumina.widget.Select;
import io.github.ssbaxys.lumina.widget.Tooltip;
import io.github.ssbaxys.lumina.widget.Tooltips;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Страница всплывающих виджетов: рисуется, списки открываются, подсказки появляются, меню выбирает. */
class PopupsPageTest {
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
        UiRoot root = new UiRoot(PopupsPage.create(), ShowcaseTheme.dark());
        root.resize(900, 1800);
        Canvas canvas = new Canvas();
        frame(root, canvas);
        assertTrue(canvas.draws > 0);

        // Выпадающие списки: открыть, сдвинуться, выбрать.
        List<Select<?>> selects = collect(root.content(), Select.class);
        assertTrue(selects.size() >= 4, "selects on the page: " + selects.size());
        for (Select<?> s : selects) {
            Object before = s.value().peek();
            click(root, s, Keys.MOUSE_LEFT);
            assertTrue(s.isOpen(), "opens: " + s);
            frame(root, canvas);
            root.tick(1f);
            frame(root, canvas);
            key(root, Keys.DOWN);
            key(root, Keys.ENTER);
            assertFalse(s.isOpen(), "Enter picks and closes");
            assertFalse(before != null && before.equals(s.value().peek()), "value changed for " + s);
            frame(root, canvas);
        }
        Select<?> searchable = selects.get(1);
        click(root, searchable, Keys.MOUSE_LEFT);
        assertNotNull(searchable.searchField());
        root.charTyped('с', 0);
        root.charTyped('л', 0);
        assertTrue(!searchable.visibleOptions().isEmpty() && searchable.visibleOptions().size() < searchable.options().size(),
                "search narrows the list: " + searchable.visibleOptions());
        frame(root, canvas);
        key(root, Keys.ESCAPE);
        assertFalse(searchable.isOpen());

        // Подсказки.
        List<UiElement> withTips = new ArrayList<>();
        collect(root.content(), e -> Tooltips.of(e) != null, withTips);
        assertTrue(withTips.size() >= 6, "elements with tooltips: " + withTips.size());
        int shown = 0;
        for (UiElement e : withTips) {
            hover(root, e);
            root.tick(1f);
            root.tick(0.016f);
            Tooltip tip = Tooltips.of(e);
            if (tip.isShowing()) {
                shown++;
                UiElement box = tip.box();
                assertTrue(box.x() >= 0 && box.x() + box.width() <= root.width(), "on screen");
            }
            frame(root, canvas);
        }
        assertTrue(shown >= 6, "tooltips shown: " + shown);
        root.pointerMove(1, 1);
        root.tick(0.016f);
        assertTrue(root.overlays().isEmpty());

        // Радиальное меню: клик по площадке даёт ей фокус, зажатая R открывает меню.
        Label hint = find(root.content(), Label.class, l -> l.text().startsWith("Щёлкните сюда"));
        UiElement pad = hint;
        while (pad != null && !pad.isFocusable()) {
            pad = pad.parent();
        }
        assertNotNull(pad, "the radial pad is focusable");
        click(root, pad, Keys.MOUSE_LEFT);
        key(root, Keys.R);
        RadialMenu menu = assertInstanceOf(RadialMenu.class, root.popups().get(0));
        root.tick(1f);
        root.pointerMove(menu.centerX() + 60, menu.centerY() - 60);
        assertEquals("Кирка", menu.hoveredItem().label());
        frame(root, canvas);
        root.keyUp(Keys.R, 0, 0);
        assertTrue(root.popups().isEmpty());
        assertNotNull(find(root.content(), Label.class, l -> l.text().equals("Кирка")), "the action ran");

        float px = pad.screenX() + 20;
        float py = pad.screenY() + 20;
        root.pointerMove(px, py);
        root.pointerDown(px, py, Keys.MOUSE_RIGHT);
        root.pointerUp(px, py, Keys.MOUSE_RIGHT);
        assertEquals(1, root.popups().size(), "right click opens the wheel");
        frame(root, canvas);
        key(root, Keys.ESCAPE);
        assertTrue(root.popups().isEmpty());

        // Клавиши.
        List<KeyBindField> binds = collect(root.content(), KeyBindField.class);
        assertTrue(binds.size() >= 6);
        KeyBindField jump = binds.get(0);
        click(root, jump, Keys.MOUSE_LEFT);
        assertTrue(jump.isListening());
        key(root, Keys.G);
        assertEquals(Keys.G, jump.key());
        frame(root, canvas);

        // Даты.
        List<DatePicker> dates = collect(root.content(), DatePicker.class);
        assertTrue(dates.size() >= 3);
        for (DatePicker d : dates) {
            click(root, d, Keys.MOUSE_LEFT);
            assertTrue(d.isOpen());
            root.tick(1f);
            frame(root, canvas);
            key(root, Keys.RIGHT);
            key(root, Keys.ENTER);
            assertFalse(d.isOpen());
        }
        frame(root, canvas);
    }

    private static void key(UiRoot root, int key) {
        root.keyDown(key, 0, 0);
    }

    private static void hover(UiRoot root, UiElement e) {
        root.pointerMove(e.screenX() + Math.min(4f, e.width() / 2f), e.screenY() + e.height() / 2f);
    }

    private static void click(UiRoot root, UiElement e, int button) {
        float x = e.screenX() + e.width() / 2f;
        float y = e.screenY() + e.height() / 2f;
        root.pointerMove(x, y);
        root.pointerDown(x, y, button);
        root.pointerUp(x, y, button);
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> collect(UiElement root, Class<?> type) {
        List<UiElement> out = new ArrayList<>();
        collect(root, type::isInstance, out);
        return (List<T>) (List<?>) out;
    }

    private static void collect(UiElement e, Predicate<UiElement> test, List<UiElement> out) {
        if (test.test(e)) {
            out.add(e);
        }
        for (UiElement c : e.children()) {
            collect(c, test, out);
        }
    }

    private static <T extends UiElement> T find(UiElement root, Class<T> type, Predicate<T> test) {
        List<UiElement> out = new ArrayList<>();
        collect(root, e -> type.isInstance(e) && test.test(type.cast(e)), out);
        return out.isEmpty() ? null : type.cast(out.get(0));
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
