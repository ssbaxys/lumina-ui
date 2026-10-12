package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.markup.CssValues;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.render.NineSlice;
import io.github.ssbaxys.lumina.style.ModernTheme;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Chart;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WindowChartTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    private static Panel desk;

    private static UiRoot mount(Window... windows) {
        desk = Panel.box(new StackLayout(Align.START, Align.START)).fill();
        desk.add(windows);
        UiRoot root = new UiRoot(desk, ModernTheme.dark());
        root.resize(400, 300);
        root.tick(0f);
        root.tick(1f);
        return root;
    }

    private static List<String> paint(UiRoot root) {
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(400, 300);
        root.render(canvas, 0f);
        canvas.end();
        return canvas.ops;
    }

    @Test
    void windowDragsResizesAndRaises() {
        Window a = new Window("A", new Label("hello")).at(10, 10).size(120, 80);
        Window b = new Window("B", new Label("world")).at(60, 40).size(120, 80);
        UiRoot root = mount(a, b);
        assertSame(b, desk.children().get(1));
        // тащим A за заголовок: она поднимается наверх и едет
        root.pointerMove(30, 15);
        root.pointerDown(30, 15, Keys.MOUSE_LEFT);
        assertSame(a, desk.children().get(1), "pressed window comes to front");
        root.pointerMove(130, 115);
        root.pointerUp(130, 115, Keys.MOUSE_LEFT);
        root.tick(0f);
        assertEquals(110, a.x(), 0.01f);
        assertEquals(110, a.y(), 0.01f);
        // за край не уезжает
        root.pointerDown(130, 115, Keys.MOUSE_LEFT);
        root.pointerMove(1000, 1000);
        root.pointerUp(1000, 1000, Keys.MOUSE_LEFT);
        root.tick(0f);
        assertEquals(400 - 120, a.x(), 0.01f);
        assertEquals(300 - 80, a.y(), 0.01f);
        // уголок B меняет размер, не меньше минимума
        float gx = b.screenX() + b.width() - 3;
        float gy = b.screenY() + b.height() - 3;
        root.pointerMove(gx, gy);
        root.pointerDown(gx, gy, Keys.MOUSE_LEFT);
        root.pointerMove(gx + 40, gy + 20);
        root.pointerUp(gx + 40, gy + 20, Keys.MOUSE_LEFT);
        root.tick(0f);
        assertEquals(160, b.width(), 0.5f);
        assertEquals(100, b.height(), 0.5f);
        assertEquals(100 - Window.HEADER, b.body().height(), 0.5f);
    }

    @Test
    void closeButtonRemovesWindow() {
        boolean[] closed = {false};
        Window w = new Window("Settings").at(20, 20).size(150, 90).onClose(() -> closed[0] = true);
        UiRoot root = mount(w);
        float cx = w.screenX() + w.width() - Window.HEADER / 2f;
        float cy = w.screenY() + Window.HEADER / 2f;
        root.pointerMove(cx, cy);
        root.pointerDown(cx, cy, Keys.MOUSE_LEFT);
        root.pointerUp(cx, cy, Keys.MOUSE_LEFT);
        assertTrue(w.isClosing());
        root.tick(0.5f);
        assertTrue(closed[0]);
        assertTrue(desk.children().isEmpty());
    }

    @Test
    void modalWindowBlocksClicksBehind() {
        int[] clicks = {0};
        Window under = new Window("Under", new Button("Hit", () -> clicks[0]++)).at(0, 0).size(200, 100);
        Window modal = new Window("Confirm").at(150, 150).size(120, 60).modal(true);
        UiRoot root = mount(under, modal);
        float bx = under.screenX() + 20;
        float by = under.screenY() + Window.HEADER + 8;
        root.pointerMove(bx, by);
        root.pointerDown(bx, by, Keys.MOUSE_LEFT);
        root.pointerUp(bx, by, Keys.MOUSE_LEFT);
        assertEquals(0, clicks[0]);
        assertTrue(paint(root).stream().anyMatch(op -> op.startsWith("quad 0.0,0.0,400.0,300.0")), "dims the screen");
    }

    @Test
    void chartAnimatesTowardValues() {
        Chart bars = new Chart(Chart.Kind.BAR).labels("Mon", "Tue", "Wed").series("Mine", 0xFF8AB4F8, 10, 40, 20);
        Chart pie = new Chart(Chart.Kind.DONUT).labels("Stone", "Dirt").series("Blocks", 0, 3, 1);
        Panel box = Panel.column().add(bars, pie);
        UiRoot root = new UiRoot(box, ModernTheme.dark());
        root.resize(400, 300);
        root.tick(0f);
        root.tick(0.05f);
        assertFalse(paint(root).isEmpty());
        root.tick(2f);
        root.tick(2f);
        bars.values(0, 5, 5, 5);
        assertEquals(5f, bars.values(0)[1], 1e-6);
        root.tick(2f);
        root.tick(2f);
        List<String> ops = paint(root);
        assertTrue(ops.stream().anyMatch(op -> op.startsWith("text Stone  75%")), "pie legend shows share: " + ops);
    }

    @Test
    void nineSliceDrawsNinePatchesAndParsesCss() {
        NineSlice frame = NineSlice.of("minecraft:textures/gui/demo_background.png", 248, 166, 4).scaled(2f);
        Panel panel = Panel.box(new StackLayout(Align.START, Align.START)).width(Dim.px(100)).height(Dim.px(60));
        panel.style(b -> b.set(Styles.NINE_SLICE, frame));
        UiRoot root = new UiRoot(Panel.column().add(panel), ModernTheme.dark());
        root.resize(400, 300);
        root.tick(0f);
        long patches = paint(root).stream().filter(op -> op.equals("tex minecraft:textures/gui/demo_background.png")).count();
        assertEquals(9, patches);

        NineSlice css = CssValues.nineSlice("url(minecraft:textures/gui/demo_background.png) 248 166 4 5 6 7 / 2");
        assertEquals(4, css.top());
        assertEquals(5, css.right());
        assertEquals(6, css.bottom());
        assertEquals(7, css.left());
        assertEquals(2f, css.scale(), 1e-6);
    }
}
