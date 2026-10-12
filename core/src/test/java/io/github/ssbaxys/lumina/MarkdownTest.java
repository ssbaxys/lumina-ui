package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markdown.Markdown;
import io.github.ssbaxys.lumina.markdown.MarkdownParser;
import io.github.ssbaxys.lumina.markdown.Md;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkdownTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    @Test
    void parsesBlocks() {
        List<Md.Block> doc = MarkdownParser.parse("""
                # Title
                Sub
                ---

                Para *one*
                continues

                > quote

                ```java
                int x = 1;
                ```

                - a
                  - nested
                - [x] done
                3. three

                | L | C | R |
                |:--|:-:|--:|
                | 1 | 2 | 3 |

                ***
                """);
        assertInstanceOf(Md.Heading.class, doc.get(0));
        assertEquals(1, ((Md.Heading) doc.get(0)).level());
        assertEquals(2, ((Md.Heading) doc.get(1)).level(), "setext heading");
        assertInstanceOf(Md.Paragraph.class, doc.get(2));
        assertInstanceOf(Md.Quote.class, doc.get(3));
        Md.CodeBlock code = (Md.CodeBlock) doc.get(4);
        assertEquals("java", code.language());
        assertEquals("int x = 1;", code.code());
        Md.ListBlock list = (Md.ListBlock) doc.get(5);
        assertFalse(list.ordered());
        assertEquals(2, list.items().size());
        assertNull(list.items().get(0).checked());
        assertInstanceOf(Md.ListBlock.class, list.items().get(0).blocks().get(1), "nested list");
        assertEquals(Boolean.TRUE, list.items().get(1).checked());
        Md.ListBlock ordered = (Md.ListBlock) doc.get(6);
        assertTrue(ordered.ordered());
        assertEquals(3, ordered.start());
        Md.Table table = (Md.Table) doc.get(7);
        assertEquals(List.of(Md.Align.LEFT, Md.Align.CENTER, Md.Align.RIGHT), table.aligns());
        assertEquals(1, table.rows().size());
        assertInstanceOf(Md.Rule.class, doc.get(8));
        assertEquals(9, doc.size());
    }

    @Test
    void parsesInlineFormatting() {
        List<Md.Inline> in = MarkdownParser.inline(
                "**b** *i* ~~s~~ ==h== ++u++ ***bi*** `c` [l](http://x) ![d](item:minecraft:diamond) \\*lit\\* snake_case_name");
        List<String> texts = new ArrayList<>();
        int flagsSeen = 0;
        for (Md.Inline i : in) {
            if (i instanceof Md.Text t) {
                texts.add(t.text());
                flagsSeen |= t.flags();
                if (t.text().equals("bi")) {
                    assertEquals(Md.BOLD | Md.ITALIC, t.flags());
                }
            }
        }
        assertEquals(Md.BOLD | Md.ITALIC | Md.STRIKE | Md.HIGHLIGHT | Md.UNDERLINE, flagsSeen);
        assertTrue(in.stream().anyMatch(i -> i instanceof Md.Code c && c.code().equals("c")));
        assertTrue(in.stream().anyMatch(i -> i instanceof Md.Link l && l.url().equals("http://x")));
        assertTrue(in.stream().anyMatch(i -> i instanceof Md.Image img && img.url().equals("item:minecraft:diamond")));
        String joined = String.join("", texts);
        assertTrue(joined.contains("*lit*"), joined);
        assertTrue(joined.contains("snake_case_name"), joined);
    }

    @Test
    void wrapsToWidthAndGrowsTaller() {
        Markdown md = new Markdown("word ".repeat(40).strip());
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).fill().add(md), Theme.dark());
        root.resize(400, 400);
        root.tick(0f);
        float wide = md.height();
        root.resize(120, 400);
        root.tick(0f);
        assertTrue(md.height() > wide * 2f, "narrow layout wraps into more lines: " + md.height() + " vs " + wide);
        assertTrue(md.width() <= 120f);
    }

    @Test
    void linksAndTasksReactToClicks() {
        List<String> clicked = new ArrayList<>();
        Markdown md = new Markdown("[go](lumina:home)\n\n- [ ] task").onLink(clicked::add);
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).fill().add(md), Theme.dark());
        root.resize(300, 200);
        root.tick(0f);
        float x0 = md.screenX() + md.padding().left();
        float y0 = md.screenY() + md.padding().top();
        root.pointerMove(x0 + 3, y0 + 3);
        root.pointerDown(x0 + 3, y0 + 3, Keys.MOUSE_LEFT);
        root.pointerUp(x0 + 3, y0 + 3, Keys.MOUSE_LEFT);
        assertEquals(List.of("lumina:home"), clicked);

        Md.ListItem item = ((Md.ListBlock) md.document().get(1)).items().get(0);
        assertEquals(Boolean.FALSE, item.checked());
        // пункт идёт после абзаца (9) и промежутка между блоками; флажок — после отступа списка (12)
        float cy = y0 + 9f + 6f + 3f;
        root.pointerDown(x0 + 15, cy, Keys.MOUSE_LEFT);
        root.pointerUp(x0 + 15, cy, Keys.MOUSE_LEFT);
        assertEquals(Boolean.TRUE, item.checked());
    }

    @Test
    void rendersWithoutErrors() {
        Markdown md = new Markdown("""
                # Заголовок
                Текст с **жирным**, `кодом` и ссылкой [тут](x).

                | a | b |
                |---|---|
                | очень длинная ячейка таблицы | 2 |

                - круг
                  - кольцо
                    - квадрат
                > цитата ![](texture:minecraft:textures/block/stone.png)
                """);
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).fill().add(md), Theme.dark());
        root.resize(200, 400);
        root.tick(0f);
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(200, 400);
        root.render(canvas, 0f);
        canvas.end();
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("text")), String.valueOf(canvas.ops));
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("tex ")), String.valueOf(canvas.ops));
    }
}
