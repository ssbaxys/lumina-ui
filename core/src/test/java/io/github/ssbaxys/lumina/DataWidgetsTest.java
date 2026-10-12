package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.style.ModernTheme;
import io.github.ssbaxys.lumina.widget.FancyText;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ScrollPanel;
import io.github.ssbaxys.lumina.widget.Skeleton;
import io.github.ssbaxys.lumina.widget.Spinner;
import io.github.ssbaxys.lumina.widget.Table;
import io.github.ssbaxys.lumina.widget.TreeView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataWidgetsTest {
    private record Player(String name, int score) {
    }

    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    private static UiRoot mount(UiElement... e) {
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).fill().add(e), ModernTheme.dark());
        root.resize(400, 300);
        root.tick(0f);
        return root;
    }

    private static void click(UiRoot root, float x, float y) {
        root.pointerMove(x, y);
        root.pointerDown(x, y, Keys.MOUSE_LEFT);
        root.pointerUp(x, y, Keys.MOUSE_LEFT);
    }

    private static List<String> paint(UiRoot root) {
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(400, 300);
        root.render(canvas, 0f);
        canvas.end();
        return canvas.ops;
    }

    @Test
    void tableSortsByHeaderAndSelectsRow() {
        Table<Player> table = new Table<Player>()
                .column("Name", 2, Player::name)
                .column("Score", 1, p -> String.valueOf(p.score()), Comparator.comparingInt(Player::score))
                .rows(List.of(new Player("Steve", 30), new Player("Alex", 50), new Player("Herobrine", 10)))
                .width(Dim.px(300));
        UiRoot root = mount(table);
        float x = table.screenX();
        float y = table.screenY() + table.padding().top();
        float inner = table.width() - table.padding().horizontal();
        // заголовок «Score» — последняя треть
        click(root, x + table.padding().left() + inner * 0.8f, y + 5);
        assertEquals(List.of(10, 30, 50), table.rows().stream().map(Player::score).toList());
        click(root, x + table.padding().left() + inner * 0.8f, y + 5);
        assertEquals(50, table.rows().get(0).score(), "second click reverses");
        // вторая строка: под заголовком 18px, строки по 16px
        click(root, x + 20, y + 18 + 16 + 8);
        assertEquals("Steve", table.selection().peek().name());
        assertTrue(paint(root).contains("text Score ▼ @" + (x + table.padding().left() + inner * 2f / 3f + 4f)
                + "," + (table.screenY() + table.padding().top() + 4.5f)), "sort arrow in header");
    }

    @Test
    void tableTruncatesLongText() {
        Table<Player> table = new Table<Player>().column("Name", 1, Player::name)
                .rows(List.of(new Player("A very long player name indeed", 1))).width(Dim.px(60));
        UiRoot root = mount(table);
        assertTrue(paint(root).stream().anyMatch(op -> op.startsWith("text ") && op.contains("…")));
    }

    @Test
    void treeExpandsAndNavigatesWithKeys() {
        TreeView.Node world = TreeView.node("world").add(
                TreeView.node("region").add(TreeView.node("r.0.0.mca")),
                TreeView.node("level.dat"));
        TreeView tree = new TreeView(world, true);
        UiRoot root = mount(tree);
        float rowH = 16f;
        float top = tree.screenY() + tree.padding().top();
        float x = tree.screenX() + tree.padding().left();
        // корень раскрыт сразу; клик по стрелке сворачивает, второй раскрывает
        assertTrue(world.isExpanded());
        click(root, x + 4, top + rowH / 2f);
        assertFalse(world.isExpanded());
        click(root, x + 4, top + rowH / 2f);
        assertTrue(world.isExpanded());
        root.tick(0f);
        assertEquals(world, tree.selection().peek());
        root.setFocus(tree);
        root.keyDown(Keys.DOWN, 0, 0);
        TreeView.Node region = world.children().get(0);
        assertEquals(region, tree.selection().peek());
        root.keyDown(Keys.RIGHT, 0, 0);
        assertTrue(region.isExpanded());
        root.keyDown(Keys.DOWN, 0, 0);
        assertEquals("r.0.0.mca", tree.selection().peek().label());
        root.keyDown(Keys.UP, 0, 0);
        root.keyDown(Keys.LEFT, 0, 0);
        assertFalse(region.isExpanded());
    }

    @Test
    void typewriterRevealsTextOverTime() {
        int[] done = {0};
        FancyText text = new FancyText("Hello").rainbow().wave(1f, 4f).typewriter(10f).onTyped(() -> done[0]++);
        UiRoot root = mount(text);
        assertTrue(text.isTyping());
        root.tick(0.25f);
        List<String> ops = paint(root);
        long letters = ops.stream().filter(op -> op.startsWith("text ")).count();
        assertTrue(letters >= 2 && letters < 5, "partially typed: " + letters);
        root.tick(1f);
        assertFalse(text.isTyping());
        assertEquals(1, done[0]);
        text.retype();
        assertTrue(text.isTyping());
        text.skip();
        assertFalse(text.isTyping());
    }

    @Test
    void loadersAnimateAndPaint() {
        UiRoot root = mount(new Spinner(), new Spinner(Spinner.Kind.DOTS, 16), new Spinner(Spinner.Kind.BARS, 16),
                new Spinner(Spinner.Kind.PULSE, 16), Skeleton.line(80), Skeleton.circle(20));
        root.tick(0.3f);
        assertFalse(paint(root).isEmpty());
    }

    @Test
    void scrollBarDoesNotCoverContent() {
        Panel tall = Panel.box(FlexLayout.column()).add(Panel.box(FlexLayout.column()).width(Dim.FILL).height(Dim.px(900)));
        Panel shortContent = Panel.box(FlexLayout.column()).add(Panel.box(FlexLayout.column()).width(Dim.FILL).height(Dim.px(50)));
        ScrollPanel scrolling = new ScrollPanel(tall).width(Dim.px(120)).height(Dim.px(200));
        ScrollPanel fitting = new ScrollPanel(shortContent).width(Dim.px(120)).height(Dim.px(200));
        scrolling.setStyleClass(null);
        fitting.setStyleClass(null);
        mount(Panel.box(FlexLayout.row()).add(scrolling, fitting));
        assertTrue(tall.width() <= 120 - 8, "gutter for the bar: " + tall.width());
        assertEquals(120, shortContent.width(), 0.01f, "no gutter without a bar");
    }
}
