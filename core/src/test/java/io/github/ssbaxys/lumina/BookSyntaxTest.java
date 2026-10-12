package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markdown.Book;
import io.github.ssbaxys.lumina.markdown.Markdown;
import io.github.ssbaxys.lumina.markdown.SyntaxHighlighter;
import io.github.ssbaxys.lumina.markdown.SyntaxHighlighter.Span;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.style.ModernTheme;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Книга (страницы, перелистывание, оглавление) и подсветка кода в Markdown. */
class BookSyntaxTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    private static UiRoot mount(Theme theme, UiElement... e) {
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).fill().add(e), theme);
        root.resize(480, 320);
        root.tick(0f);
        return root;
    }

    private static List<String> paint(UiRoot root) {
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(480, 320);
        root.render(canvas, 0f);
        canvas.end();
        return canvas.ops;
    }

    // ================================================================ книга

    private static final String STORY = """
            # Начало
            Стив проснулся в деревне.

            <!-- page -->
            # Шахта
            Глубоко под землёй.

            ## Алмазы
            Наконец-то!
            \\newpage
            # Конец
            Дракон повержен.
            """;

    @Test
    void bookSplitsByBreaksAndFlips() {
        Book book = new Book(STORY);
        UiRoot root = mount(Theme.dark(), book);
        assertEquals(3, book.pageCount().get());
        assertTrue(book.pageText(0).contains("Начало"));
        assertTrue(book.pageText(1).contains("Алмазы"));
        assertTrue(book.pageText(2).contains("Дракон"));

        List<String> ops = paint(root);
        assertTrue(ops.contains("tex minecraft:textures/gui/book.png"), "vanilla look uses the game texture");
        assertTrue(ops.stream().anyMatch(op -> op.startsWith("text Страница 1 из 3 ")));

        root.setFocus(book);
        root.keyDown(Keys.RIGHT, 0, 0);
        assertEquals(1, book.page().get());
        root.tick(0.1f);
        assertTrue(book.isFlipping());
        // Во время перелистывания видны обе страницы: старая (лист) и новая (под ним).
        List<String> mid = paint(root);
        assertTrue(mid.stream().anyMatch(op -> op.contains("Начало")) && mid.stream().anyMatch(op -> op.contains("Шахта")));
        root.tick(1f);
        assertFalse(book.isFlipping());
        assertFalse(paint(root).stream().anyMatch(op -> op.contains("Начало")));

        root.keyDown(Keys.END, 0, 0);
        assertEquals(2, book.page().get());
        assertFalse(book.canNext());
        book.next();
        assertEquals(2, book.page().get());
        root.keyDown(Keys.HOME, 0, 0);
        assertEquals(0, book.page().get());
        // Колесо вниз — вперёд.
        root.scroll(book.screenX() + 50, book.screenY() + 50, 0, -1);
        assertEquals(1, book.page().get());
    }

    @Test
    void longTextOverflowsOntoNextPages() {
        StringBuilder sb = new StringBuilder("# Длинная глава\n\n");
        for (int i = 0; i < 120; i++) {
            sb.append("слово").append(i).append(' ');
        }
        Book book = new Book(sb.toString());
        mount(Theme.dark(), book);
        assertTrue(book.pageCount().get() >= 3, "pages: " + book.pageCount().get());
        // Слова не теряются и не повторяются при разрезании абзаца.
        String all = "";
        for (int p = 0; p < book.pageCount().get(); p++) {
            all += " " + book.pageText(p);
        }
        for (int i = 0; i < 120; i++) {
            assertTrue(all.contains("слово" + i + " ") || all.endsWith("слово" + i), "word " + i);
        }
        assertEquals(1, all.split("слово57\\b", -1).length - 1);
    }

    @Test
    void modernSpreadWithTableOfContentsAndLinks() {
        Book book = new Book(STORY + "\n<!-- page -->\nСм. [шахту](#шахта) или [стр. 1](#page:1).")
                .look(Book.Look.MODERN).tableOfContents(true).width(Dim.px(360)).height(Dim.px(240));
        UiRoot root = mount(ModernTheme.dark(), book);
        assertTrue(book.isSpread());
        assertEquals(2, book.tocPageCount(), "toc page + blank page so chapters start on the left");
        List<Book.Entry> toc = book.toc();
        assertEquals(List.of("Начало", "Шахта", "Алмазы", "Конец"), toc.stream().map(Book.Entry::title).toList());
        assertEquals(2, toc.get(0).page());
        assertEquals(3, toc.get(1).page());
        assertTrue(book.pageText(0).contains("Содержание"));
        assertTrue(book.pageText(0).replaceAll("\\s+", " ").contains("Шахта · 4"), book.pageText(0));

        // В развороте страница всегда чётная.
        book.goTo(3);
        assertEquals(2, book.page().get());
        root.tick(1f);
        List<String> ops = paint(root);
        assertTrue(ops.stream().anyMatch(op -> op.startsWith("text Начало") || op.contains("Начало")));
        assertTrue(ops.stream().anyMatch(op -> op.contains("Шахта")), "right page shows the next chapter");

        // Клик по пункту оглавления.
        book.goTo(0);
        root.tick(1f);
        clickText(root, "Конец");
        root.tick(1f);
        assertEquals(4, book.page().get());

        // Ссылка-якорь на заголовок.
        book.goTo(book.pageCount().get() - 1);
        root.tick(1f);
        clickText(root, "шахту");
        assertEquals(2, book.page().get());
    }

    /** Кликнуть в начало нарисованного текста. */
    private static void clickText(UiRoot root, String text) {
        for (String op : paint(root)) {
            if (op.startsWith("text ") && op.contains(text)) {
                String[] at = op.substring(op.lastIndexOf('@') + 1).split(",");
                float x = Float.parseFloat(at[0]) + 3;
                float y = Float.parseFloat(at[1]) + 3;
                root.pointerMove(x, y);
                root.pointerDown(x, y, Keys.MOUSE_LEFT);
                root.pointerUp(x, y, Keys.MOUSE_LEFT);
                return;
            }
        }
        throw new AssertionError("no text " + text);
    }

    @Test
    void bookTagInMarkup() {
        Markup.Built built = Markup.build("""
                <book look="modern" toc="Оглавление" spread="false">
                  # Глава
                  Текст
                  <!-- page -->
                  # Вторая
                </book>
                """, new Bindings());
        Book book = (Book) built.root();
        mount(Theme.dark(), book);
        assertEquals(Book.Look.MODERN, book.look());
        assertFalse(book.isSpread());
        assertEquals(3, book.pageCount().get());
        assertTrue(book.pageText(0).contains("Оглавление"));
    }

    // ================================================================ подсветка

    private static String kinds(String lang, String code) {
        List<List<Span>> lines = SyntaxHighlighter.highlight(lang, code);
        List<String> out = new ArrayList<>();
        String[] src = code.split("\n", -1);
        for (int i = 0; i < lines.size(); i++) {
            for (Span s : lines.get(i)) {
                String t = src[i].substring(s.start(), s.end());
                if (!t.isBlank() && s.kind() != SyntaxHighlighter.PLAIN && s.kind() != SyntaxHighlighter.PUNCTUATION) {
                    out.add(t.strip() + ":" + s.kind());
                }
            }
        }
        return String.join(" ", out);
    }

    @Test
    void highlightsJava() {
        String k = kinds("java", """
                @Override
                public static int count(String s) { // счёт
                    /* много
                       строк */ return s.length() + 0x1F + 2.5f; "x\\"y"
                }""");
        int kw = SyntaxHighlighter.KEYWORD;
        assertTrue(k.contains("@Override:" + SyntaxHighlighter.ANNOTATION), k);
        assertTrue(k.contains("public:" + kw) && k.contains("static:" + kw) && k.contains("int:" + kw), k);
        assertTrue(k.contains("count:" + SyntaxHighlighter.FUNCTION), k);
        assertTrue(k.contains("String:" + SyntaxHighlighter.TYPE), k);
        assertTrue(k.contains("// счёт:" + SyntaxHighlighter.COMMENT), k);
        assertTrue(k.contains("/* много:" + SyntaxHighlighter.COMMENT), k);
        assertTrue(k.contains("строк */:" + SyntaxHighlighter.COMMENT), "multi-line comment continues: " + k);
        assertTrue(k.contains("return:" + kw), k);
        assertTrue(k.contains("0x1F:" + SyntaxHighlighter.NUMBER) && k.contains("2.5f:" + SyntaxHighlighter.NUMBER), k);
        assertTrue(k.contains("\"x\\\"y\":" + SyntaxHighlighter.STRING), k);
    }

    @Test
    void highlightsJsonCssAndHtml() {
        String json = kinds("json", "{\"name\": \"Стив\", \"lvl\": 30, \"ok\": true, \"x\": null}");
        assertTrue(json.contains("\"name\":" + SyntaxHighlighter.ATTRIBUTE), json);
        assertTrue(json.contains("\"Стив\":" + SyntaxHighlighter.STRING), json);
        assertTrue(json.contains("30:" + SyntaxHighlighter.NUMBER), json);
        assertTrue(json.contains("true:" + SyntaxHighlighter.CONSTANT) && json.contains("null:" + SyntaxHighlighter.CONSTANT), json);

        String css = kinds("css", """
                .card:hover, #main > button {
                  color: #ff8800; margin: 4px 2em !important;
                  background: linear-gradient(red, blue);
                }
                @media screen { a { opacity: .5 } }""");
        assertTrue(css.contains(".card:" + SyntaxHighlighter.TYPE) && css.contains("#main:" + SyntaxHighlighter.TYPE), css);
        assertTrue(css.contains(":hover:" + SyntaxHighlighter.KEYWORD), css);
        assertTrue(css.contains("button:" + SyntaxHighlighter.TAG), css);
        assertTrue(css.contains("color:" + SyntaxHighlighter.ATTRIBUTE) && css.contains("margin:" + SyntaxHighlighter.ATTRIBUTE), css);
        assertTrue(css.contains("#ff8800:" + SyntaxHighlighter.NUMBER) && css.contains("4px:" + SyntaxHighlighter.NUMBER)
                && css.contains("2em:" + SyntaxHighlighter.NUMBER), css);
        assertTrue(css.contains("!important:" + SyntaxHighlighter.KEYWORD), css);
        assertTrue(css.contains("linear-gradient:" + SyntaxHighlighter.FUNCTION), css);
        assertTrue(css.contains("@media:" + SyntaxHighlighter.KEYWORD), css);
        assertTrue(css.contains("opacity:" + SyntaxHighlighter.ATTRIBUTE) && css.contains(".5:" + SyntaxHighlighter.NUMBER), css);

        String html = kinds("html", """
                <!-- комментарий -->
                <button class="big" on-click="inc:count"
                        disabled>Жми &amp; смотри</button>""");
        assertTrue(html.contains("<!-- комментарий -->:" + SyntaxHighlighter.COMMENT), html);
        assertTrue(html.contains("button:" + SyntaxHighlighter.TAG), html);
        assertTrue(html.contains("class:" + SyntaxHighlighter.ATTRIBUTE) && html.contains("on-click:" + SyntaxHighlighter.ATTRIBUTE), html);
        assertTrue(html.contains("\"big\":" + SyntaxHighlighter.STRING), html);
        assertTrue(html.contains("disabled:" + SyntaxHighlighter.ATTRIBUTE), "attributes continue on the next line: " + html);
        assertTrue(html.contains("&amp;:" + SyntaxHighlighter.CONSTANT), html);

        assertFalse(SyntaxHighlighter.supports("brainfuck"));
        assertEquals(List.of(List.of(new Span(0, 3, SyntaxHighlighter.PLAIN))), SyntaxHighlighter.highlight("?", "abc"));
    }

    @Test
    void markdownCodeBlockIsColoredByTheme() {
        Markdown md = new Markdown("```java\npublic int x = 42; // ответ\n```").width(Dim.px(300));
        UiRoot root = mount(Theme.dark(), md);
        List<String> ops = paint(root);
        // Лексемы рисуются отдельными кусками.
        assertTrue(ops.stream().anyMatch(op -> op.startsWith("text public @")), ops.toString());
        assertTrue(ops.stream().anyMatch(op -> op.startsWith("text 42 @")), ops.toString());
        assertTrue(ops.stream().anyMatch(op -> op.startsWith("text // ответ @")), ops.toString());

        int[] dark = SyntaxHighlighter.palette(0xFFE6E9F2, 0xFF5B8CFF, Theme.dark());
        int[] light = SyntaxHighlighter.palette(0xFF1D2330, 0xFF3D6DF2, Theme.light());
        assertNotEquals(dark[SyntaxHighlighter.STRING], light[SyntaxHighlighter.STRING], "palette follows the theme");
        Theme custom = Theme.dark().set(SyntaxHighlighter.STRING_COLOR, 0xFF00FF00);
        assertEquals(0xFF00FF00, SyntaxHighlighter.palette(0xFFE6E9F2, 0xFF5B8CFF, custom)[SyntaxHighlighter.STRING]);
        String distinct = java.util.Arrays.stream(dark).mapToObj(Integer::toHexString).distinct()
                .collect(Collectors.joining(","));
        assertTrue(distinct.split(",").length >= 9, "kinds have distinct colors: " + distinct);
    }
}
