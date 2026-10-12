package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ScrollPanel;
import io.github.ssbaxys.lumina.widget.Slider;
import io.github.ssbaxys.lumina.widget.Toggle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WidgetTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    @Test
    void toggleFlipsSignalAndCheckedState() {
        Signal<Boolean> on = Signal.of(false);
        Toggle t = new Toggle(on);
        UiRoot root = new UiRoot(Panel.box(FlexLayout.row()).add(t), Theme.dark());
        root.resize(100, 100);
        root.tick(0f);
        root.pointerDown(5, 5, Keys.MOUSE_LEFT);
        root.pointerUp(5, 5, Keys.MOUSE_LEFT);
        assertTrue(on.peek());
        assertTrue(t.hasState(State.CHECKED));
        root.keyDown(Keys.SPACE, 0, 0);
        assertFalse(on.peek());
    }

    @Test
    void sliderFollowsDragAndKeys() {
        Signal<Float> v = Signal.of(0f);
        Slider s = new Slider(v, 0f, 100f).step(1f);
        UiRoot root = new UiRoot(Panel.box(FlexLayout.row()).add(s), Theme.dark());
        root.resize(200, 100);
        root.tick(0f);
        // ширина 120, ход ползунка 110 начиная с x=5
        root.pointerDown(60, 7, Keys.MOUSE_LEFT);
        assertEquals(50f, v.peek());
        root.pointerMove(115, 7);
        assertEquals(100f, v.peek());
        root.pointerUp(115, 7, Keys.MOUSE_LEFT);
        root.keyDown(Keys.LEFT, 0, 0);
        assertEquals(99f, v.peek());
    }

    @Test
    void offscreenRowsAreNotRendered() {
        Panel list = Panel.box(FlexLayout.column());
        for (int i = 0; i < 100; i++) {
            list.add(Panel.box(FlexLayout.row()).size(50, 10)
                    .style(b -> b.set(Styles.BACKGROUND, Brush.solid(0xFFFFFFFF))));
        }
        ScrollPanel scroll = new ScrollPanel(list).size(50, 30).style(io.github.ssbaxys.lumina.style.Style.EMPTY);
        scroll.setStyleClass(null);
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).add(scroll), new Theme());
        root.resize(200, 200);
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(200, 200);
        root.render(canvas, 0f);
        canvas.end();
        long quads = canvas.ops.stream().filter(o -> o.startsWith("quad")).count();
        assertEquals(3, quads, canvas.ops.toString());
    }
}
