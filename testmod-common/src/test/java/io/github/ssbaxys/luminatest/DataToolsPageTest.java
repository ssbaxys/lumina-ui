package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.markdown.Book;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.AbstractCanvas;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.ShapePaint;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.FormField;
import io.github.ssbaxys.lumina.widget.Paginator;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ScrollPanel;
import io.github.ssbaxys.lumina.widget.Table;
import io.github.ssbaxys.lumina.widget.TextField;
import io.github.ssbaxys.lumina.widget.VirtualList;
import io.github.ssbaxys.lumina.widget.Wizard;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Страница инструментов данных: всё строится, рисуется и откликается — без исключений. */
class DataToolsPageTest {
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
    void pageMountsAndEveryToolWorks() {
        Panel host = Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH)).fill();
        UiRoot root = new UiRoot(host, ShowcaseTheme.dark());
        root.resize(640, 400);
        host.add(DataToolsPage.create());
        Canvas canvas = new Canvas();
        frame(root, canvas);
        assertTrue(canvas.draws > 0);

        // Список: 10 000 предметов, но рисуются только видимые строки.
        VirtualList<?> list = find(root.content(), VirtualList.class);
        assertNotNull(list);
        assertEquals(DataToolsPage.ITEM_COUNT, list.items().size());
        assertTrue(list.lastVisible() - list.firstVisible() < 15);
        int itemsBefore = canvas.items;
        assertTrue(itemsBefore < 40, "item icons drawn: " + itemsBefore);

        // Поиск сужает список; фильтр «только блоки».
        TextField search = find(root.content(), TextField.class);
        root.setFocus(search);
        for (char c : "алмаз №1".toCharArray()) {
            root.charTyped(c, 0);
        }
        frame(root, canvas);
        assertTrue(list.items().size() > 0 && list.items().size() < 1000, "found: " + list.items().size());
        root.setFocus(list);
        root.keyDown(Keys.END, 0, 0);
        root.keyDown(Keys.ENTER, 0, 0);
        frame(root, canvas);

        // Таблица со страницами.
        Table<?> table = find(root.content(), Table.class);
        Paginator paginator = find(root.content(), Paginator.class);
        assertEquals(10, table.rows().size());
        root.setFocus(paginator);
        root.keyDown(Keys.END, 0, 0);
        frame(root, canvas);
        assertEquals(paginator.count() - 1, paginator.current());

        // Форма регистрации: «Создать аккаунт» без данных показывает ошибки под полями.
        Button create = findButton(root.content(), "Создать аккаунт");
        create.press();
        List<FormField> rows = new ArrayList<>();
        collect(root.content(), FormField.class, rows);
        assertTrue(rows.size() >= 5);
        for (int i = 0; i < 30; i++) {
            frame(root, canvas);
        }
        assertTrue(rows.stream().anyMatch(r -> r.errorReveal() > 0.99f && !r.errorText().isEmpty()));

        // Мастер: первый шаг не пускает дальше без названия.
        Wizard wizard = find(root.content(), Wizard.class);
        wizard.nextButton().press();
        assertEquals(0, wizard.current());

        // Книги перелистываются.
        List<Book> books = new ArrayList<>();
        collect(root.content(), Book.class, books);
        assertEquals(2, books.size());
        for (Book b : books) {
            assertTrue(b.pageCount().get() >= 5, "pages: " + b.pageCount().get());
            b.next();
        }
        ScrollPanel scroll = find(root.content(), ScrollPanel.class);
        scroll.scrollTo(scroll.maxScroll(), false);
        for (int i = 0; i < 40; i++) {
            frame(root, canvas);
        }
        assertFalse(books.get(0).isFlipping());
        assertTrue(books.get(1).toc().size() >= 5);
    }

    private static Button findButton(UiElement e, String text) {
        if (e instanceof Button b && b.text().equals(text)) {
            return b;
        }
        for (UiElement c : e.children()) {
            Button found = findButton(c, text);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static <T> T find(UiElement e, Class<T> type) {
        if (type.isInstance(e)) {
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

    private static <T> void collect(UiElement e, Class<T> type, List<T> out) {
        if (type.isInstance(e)) {
            out.add(type.cast(e));
        }
        for (UiElement c : e.children()) {
            collect(c, type, out);
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
        int items;

        void begin(float w, float h) {
            draws = 0;
            items = 0;
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
