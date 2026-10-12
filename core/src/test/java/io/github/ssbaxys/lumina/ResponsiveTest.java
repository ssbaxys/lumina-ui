package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Breakpoint;
import io.github.ssbaxys.lumina.layout.Breakpoints;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.GridLayout;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ResponsivePanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Классы ширины, адаптивные контейнеры, сетка и масштаб интерфейса. */
class ResponsiveTest {
    @BeforeEach
    void init() {
        TestSupport.init();
    }

    private static Panel box(float w, float h) {
        return Panel.box(FlexLayout.column()).size(w, h);
    }

    private static UiRoot root(UiElement content, float w, float h) {
        Panel host = Panel.box(new StackLayout(Align.STRETCH, Align.START)).fill().add(content);
        UiRoot root = new UiRoot(host);
        root.resize(w, h);
        root.tick(0f);
        return root;
    }

    @Test
    void breakpointsFollowRootWidthAndScale() {
        UiRoot root = root(box(10, 10), 300, 200);
        List<Breakpoint> seen = new ArrayList<>();
        root.breakpoint().subscribe(seen::add);
        assertEquals(Breakpoint.COMPACT, root.currentBreakpoint());
        root.resize(500, 200);
        assertEquals(Breakpoint.MEDIUM, root.currentBreakpoint());
        root.resize(900, 200);
        assertEquals(Breakpoint.WIDE, root.currentBreakpoint());
        root.setScale(2f);
        assertEquals(450f, root.width());
        assertEquals(Breakpoint.MEDIUM, root.currentBreakpoint());
        root.setBreakpoints(new Breakpoints(200f, 400f));
        assertEquals(Breakpoint.WIDE, root.currentBreakpoint());
        assertEquals(List.of(Breakpoint.MEDIUM, Breakpoint.WIDE, Breakpoint.MEDIUM, Breakpoint.WIDE), seen);
        assertEquals(2, Breakpoint.MEDIUM.pick(1, 2, 3));
        assertThrows(IllegalArgumentException.class, () -> new Breakpoints(500f, 100f));
    }

    @Test
    void rowOnWideColumnOnCompact() {
        Panel a = box(50, 20);
        Panel b = box(50, 20);
        ResponsivePanel p = ResponsivePanel.rowFrom(Breakpoint.MEDIUM, 4).add(a, b);
        UiRoot root = root(p, 800, 300);
        assertEquals(Breakpoint.WIDE, p.current());
        assertEquals(a.y(), b.y());
        assertEquals(54f, b.x());

        root.resize(300, 300);
        root.tick(0f);
        assertEquals(Breakpoint.COMPACT, p.current());
        assertEquals(a.x(), b.x());
        assertEquals(24f, b.y());
    }

    @Test
    void gridColumnsPerBreakpoint() {
        ResponsivePanel grid = ResponsivePanel.columns(1, 2, 3, 10);
        List<Panel> cells = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            Panel c = Panel.box(FlexLayout.column()).height(Dim.px(20));
            cells.add(c);
            grid.add(c);
        }
        grid.width(Dim.FILL);
        UiRoot root = root(grid, 700, 400);
        // 3 колонки по (700 - 20) / 3.
        assertEquals(680f / 3f, cells.get(0).width(), 0.01f);
        assertEquals(cells.get(0).y(), cells.get(2).y());
        assertEquals(30f, cells.get(3).y(), 0.01f);
        assertEquals(cells.get(0).x(), cells.get(3).x());
        assertEquals(50f, grid.height(), 0.01f);

        root.resize(300, 400);
        root.tick(0f);
        assertEquals(300f, cells.get(0).width(), 0.01f);
        assertEquals(150f, cells.get(5).y(), 0.01f);
        assertEquals(170f, grid.height(), 0.01f);
    }

    @Test
    void containerWidthModeIgnoresRoot() {
        ResponsivePanel own = new ResponsivePanel()
                .compact(FlexLayout.column())
                .wide(new GridLayout(4, 0))
                .byContainerWidth(true);
        Panel a = box(10, 10);
        Panel b = box(10, 10);
        own.add(a, b).width(Dim.FILL);
        Panel narrow = Panel.box(FlexLayout.column().align(Align.STRETCH)).width(Dim.px(200)).add(own);
        UiRoot root = root(narrow, 1000, 400);
        assertEquals(Breakpoint.WIDE, root.currentBreakpoint());
        assertSame(Breakpoint.COMPACT, own.current());
        assertEquals(10f, b.y());
    }

    @Test
    void uiScaleScalesRenderingAndInput() {
        int[] pressed = {0};
        Button button = new Button("OK", () -> pressed[0]++).size(40, 20).margin(new io.github.ssbaxys.lumina.geom.Insets(10, 0, 0, 10));
        Panel host = Panel.box(new StackLayout(Align.START, Align.START)).fill().add(button);
        UiRoot root = new UiRoot(host);
        root.resize(400, 200);
        root.setScale(2f);
        root.tick(0f);
        assertEquals(200f, root.width());
        assertEquals(10f, button.x());

        // Точка (25, 15) интерфейса — это (50, 30) на экране.
        root.pointerDown(50, 30, Keys.MOUSE_LEFT);
        root.pointerUp(50, 30, Keys.MOUSE_LEFT);
        assertEquals(1, pressed[0]);
        assertEquals(25f, root.mouseX());
        // Снаружи кнопки в единицах интерфейса (60, 15), хотя на экране x=30 «попал» бы без масштаба.
        root.pointerDown(120, 30, Keys.MOUSE_LEFT);
        root.pointerUp(120, 30, Keys.MOUSE_LEFT);
        assertEquals(1, pressed[0]);

        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(400, 200);
        root.render(canvas, 0f);
        canvas.end();
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("shape 20.0,20.0,100.0,60.0")), canvas.ops.toString());
    }
}
