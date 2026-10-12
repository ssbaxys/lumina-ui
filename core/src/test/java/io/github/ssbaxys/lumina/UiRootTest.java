package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UiRootTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    @Test
    void clickHoverFocusAndKeyboard() {
        AtomicInteger presses = new AtomicInteger();
        Button a = new Button("A", presses::incrementAndGet).size(50, 20);
        Button b = new Button("B").size(50, 20);
        Panel row = Panel.box(FlexLayout.row().gap(10)).add(a, b);
        UiRoot root = new UiRoot(row, Theme.dark());
        root.resize(200, 100);
        root.tick(0f);

        root.pointerMove(10, 10);
        assertTrue(a.hasState(State.HOVERED));
        assertTrue(row.hasState(State.HOVERED), "hover распространяется на предков");

        assertTrue(root.pointerDown(10, 10, Keys.MOUSE_LEFT));
        assertTrue(a.hasState(State.PRESSED));
        assertSame(a, root.focused());
        root.pointerUp(10, 10, Keys.MOUSE_LEFT);
        assertEquals(1, presses.get());

        // отпускание вне кнопки — не клик
        root.pointerDown(10, 10, Keys.MOUSE_LEFT);
        root.pointerUp(190, 90, Keys.MOUSE_LEFT);
        assertEquals(1, presses.get());

        root.keyDown(Keys.ENTER, 0, 0);
        assertEquals(2, presses.get());

        root.keyDown(Keys.TAB, 0, 0);
        assertSame(b, root.focused());
        assertFalse(a.isFocused());
    }

    @Test
    void canvasBalancesStateAndCullsOffscreen() {
        Panel content = Panel.box(FlexLayout.column()).size(10, 10);
        content.style(s -> s.set(io.github.ssbaxys.lumina.style.Styles.BACKGROUND,
                io.github.ssbaxys.lumina.render.Brush.solid(0xFFFFFFFF)));
        Panel outer = Panel.box(FlexLayout.column()).size(50, 50).add(content).clip(true);
        UiRoot root = new UiRoot(outer, new Theme());
        root.resize(100, 100);
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(100, 100);
        root.render(canvas, 0f);
        canvas.end();
        assertTrue(canvas.ops.contains("quad 0.0,0.0,10.0,10.0"), canvas.ops.toString());
    }
}
