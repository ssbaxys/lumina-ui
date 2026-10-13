package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.input.Gamepad;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.input.NavDirection;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.Inspector;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ScrollPanel;
import io.github.ssbaxys.lumina.widget.Tabs;
import io.github.ssbaxys.lumina.widget.Toggle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Инспектор интерфейса и управление с геймпада (пространственная навигация, A/B, бамперы). */
class InspectorGamepadTest {
    @org.junit.jupiter.api.BeforeEach
    void russianTexts() {
        io.github.ssbaxys.lumina.i18n.I18n.setLanguage("ru_ru");
    }

    Gamepad.State pad;

    @BeforeEach
    void init() {
        pad = null;
        Lumina.init(new CapturePlatform() {
            @Override
            public Gamepad.State pollGamepad() {
                return pad;
            }
        });
    }

    private static UiRoot mount(UiElement content) {
        UiRoot root = new UiRoot(content, Theme.dark());
        root.resize(400, 300);
        root.tick(0f);
        return root;
    }

    // ================================================================ инспектор

    @Test
    void inspectorTogglesOnlyWhenEnabled() {
        List<String> pressed = new ArrayList<>();
        Button ok = new Button("OK", () -> pressed.add("ok")).id("ok");
        ok.margin(Insets.all(5));
        Panel content = Panel.box(new StackLayout(Align.START, Align.START)).add(
                Panel.box(FlexLayout.column().gap(4)).add(new Label("Привет").id("hello"), ok));
        UiRoot root = mount(content);
        Inspector inspector = root.inspector();
        assertFalse(root.keyDown(Keys.F8, 0, 0), "disabled by default");
        assertFalse(inspector.isOpen());

        inspector.enabled(true);
        assertTrue(root.keyDown(Keys.F8, 0, 0));
        assertTrue(inspector.isOpen());

        // наведение подсвечивает даже надписи, которые не ловят мышь
        UiElement hello = content.findById("hello");
        root.pointerMove(hello.screenX() + 2, hello.screenY() + 2);
        assertSame(hello, inspector.hovered());

        // клик выбирает и не нажимает кнопку
        float bx = ok.screenX() + 3;
        float by = ok.screenY() + 3;
        root.pointerMove(bx, by);
        root.pointerDown(bx, by, Keys.MOUSE_LEFT);
        root.pointerUp(bx, by, Keys.MOUSE_LEFT);
        assertTrue(pressed.isEmpty(), "inspector swallows the click");
        assertTrue(inspector.selected() == ok || ok.isAncestorOrSelfOf(inspector.selected()));
        inspector.select(ok);

        List<String> lines = inspector.describe(ok);
        assertTrue(lines.get(0).startsWith("Button.button#ok"), lines.toString());
        assertTrue(lines.contains("margin: 5 5 5 5"), lines.toString());
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("radius: ")), lines.toString());
        assertTrue(lines.stream().anyMatch(l -> l.startsWith("background: ")), lines.toString());

        // стрелка вверх — к родителю
        root.keyDown(Keys.UP, 0, 0);
        assertSame(ok.parent(), inspector.selected());
        assertTrue(inspector.treeRows().stream().anyMatch(r -> r.element() == ok));

        TestSupport.RecordingCanvas c = new TestSupport.RecordingCanvas();
        c.begin(400, 300);
        root.render(c, 0f);
        c.end();
        assertTrue(c.ops.stream().anyMatch(o -> o.startsWith("text Инспектор (F8)")), c.ops.toString());
        assertTrue(c.ops.stream().anyMatch(o -> o.startsWith("text Button.button#ok")));

        assertTrue(root.keyDown(Keys.ESCAPE, 0, 0));
        assertFalse(inspector.isOpen());
        root.pointerDown(bx, by, Keys.MOUSE_LEFT);
        root.pointerUp(bx, by, Keys.MOUSE_LEFT);
        assertEquals(List.of("ok"), pressed, "closed inspector lets clicks through");

        inspector.toggleKey(Keys.F12);
        assertFalse(root.keyDown(Keys.F8, 0, 0));
        assertTrue(root.keyDown(Keys.F12, 0, 0));
        assertTrue(inspector.isOpen());
        inspector.enabled(false);
        assertFalse(inspector.isOpen());
    }

    @Test
    void inspectorPanelRowsSelectAndScroll() {
        Panel column = Panel.box(FlexLayout.column());
        for (int i = 0; i < 80; i++) {
            column.add(new Label("Строка " + i).id("row" + i));
        }
        UiRoot root = mount(Panel.box(new StackLayout(Align.START, Align.START)).add(column));
        Inspector inspector = root.inspector().enabled(true).open().panelLeft(true);
        // первая строка дерева — содержимое корня
        root.pointerDown(20, 4 + 16 + 4 + 2, Keys.MOUSE_LEFT);
        assertSame(root.content(), inspector.selected());
        assertTrue(root.scroll(20, 100, 0f, -3f), "wheel scrolls the tree");
        inspector.select(column.findById("row79"));
        assertEquals("row79", inspector.selected().id());
        root.pointerDown(300, 290, Keys.MOUSE_RIGHT);
        assertNull(inspector.selected(), "right click clears selection");
    }

    // ================================================================ геймпад

    private static Button button(String id, List<String> log) {
        return new Button(id, () -> log.add(id)).id(id).size(60, 20);
    }

    @Test
    void spatialNavigationPicksNearestInDirection() {
        List<String> log = new ArrayList<>();
        Panel grid = Panel.box(FlexLayout.column().gap(10)).add(
                Panel.box(FlexLayout.row().gap(10)).add(button("a", log), button("b", log), button("c", log)),
                Panel.box(FlexLayout.row().gap(10)).add(button("d", log), button("e", log)));
        UiRoot root = mount(grid);
        assertTrue(root.focusDirection(NavDirection.RIGHT));
        assertEquals("a", root.focused().id(), "no focus → first element");
        root.focusDirection(NavDirection.RIGHT);
        assertEquals("b", root.focused().id());
        root.focusDirection(NavDirection.DOWN);
        assertEquals("e", root.focused().id(), "below b is e");
        root.focusDirection(NavDirection.LEFT);
        assertEquals("d", root.focused().id());
        assertFalse(root.focusDirection(NavDirection.LEFT), "nothing to the left");
        root.focusDirection(NavDirection.UP);
        assertEquals("a", root.focused().id());
    }

    @Test
    void gamepadMovesActivatesAndGoesBack() {
        List<String> log = new ArrayList<>();
        Signal<Boolean> on = Signal.of(false);
        Toggle toggle = new Toggle(on);
        toggle.id("toggle");
        Panel row = Panel.box(FlexLayout.row().gap(10)).add(button("a", log), toggle);
        UiRoot root = mount(row);
        int[] back = {0};
        root.gamepad().onBack(() -> back[0]++);

        pad = Gamepad.State.pressed(Gamepad.DPAD_RIGHT);
        root.tick(0.016f);
        assertEquals("a", root.focused().id());
        assertTrue(root.gamepad().isConnected());
        pad = Gamepad.State.idle();
        root.tick(0.016f);
        pad = Gamepad.State.pressed(Gamepad.A);
        root.tick(0.016f);
        assertEquals(List.of("a"), log);
        root.tick(0.016f);
        assertEquals(List.of("a"), log, "held button fires once");
        pad = Gamepad.State.stick(0.9f, 0.1f);
        root.tick(0.016f);
        assertEquals("toggle", root.focused().id());
        pad = Gamepad.State.pressed(Gamepad.A);
        root.tick(0.016f);
        assertTrue(on.peek(), "A toggles via Enter");
        pad = Gamepad.State.pressed(Gamepad.B);
        root.tick(0.016f);
        assertEquals(1, back[0]);
        pad = null;
        root.tick(0.016f);
        assertFalse(root.gamepad().isConnected());
    }

    @Test
    void holdingDirectionRepeats() {
        List<String> log = new ArrayList<>();
        Panel col = Panel.box(FlexLayout.column().gap(4));
        for (int i = 0; i < 10; i++) {
            col.add(button("b" + i, log));
        }
        UiRoot root = mount(col);
        root.gamepad().repeat(0.3f, 0.1f);
        root.gamepad().handle(Gamepad.State.pressed(Gamepad.DPAD_DOWN), 0f);
        assertEquals("b0", root.focused().id());
        root.gamepad().handle(Gamepad.State.pressed(Gamepad.DPAD_DOWN), 0.2f);
        assertEquals("b0", root.focused().id(), "before delay");
        root.gamepad().handle(Gamepad.State.pressed(Gamepad.DPAD_DOWN), 0.15f);
        assertEquals("b1", root.focused().id());
        root.gamepad().handle(Gamepad.State.pressed(Gamepad.DPAD_DOWN), 0.22f);
        assertEquals("b3", root.focused().id());
    }

    @Test
    void bumpersSwitchTabsAndFocusScrollsIntoView() {
        Signal<Integer> tab = Signal.of(0);
        Tabs tabs = new Tabs(tab, "Один", "Два", "Три");
        List<String> log = new ArrayList<>();
        Panel col = Panel.box(FlexLayout.column().gap(4));
        for (int i = 0; i < 30; i++) {
            col.add(button("s" + i, log));
        }
        ScrollPanel scroll = new ScrollPanel(col).size(100, 100);
        scroll.style(b -> b.set(Styles.RADIUS, Radii.all(4)));
        UiRoot root = mount(Panel.box(FlexLayout.column()).add(tabs, scroll).width(Dim.FILL));
        root.gamepad().handle(Gamepad.State.pressed(Gamepad.RIGHT_BUMPER), 0f);
        assertEquals(1, tab.peek());
        root.gamepad().handle(Gamepad.State.idle(), 0f);
        root.gamepad().handle(Gamepad.State.pressed(Gamepad.LEFT_BUMPER), 0f);
        root.gamepad().handle(Gamepad.State.idle(), 0f);
        root.gamepad().handle(Gamepad.State.pressed(Gamepad.LEFT_BUMPER), 0f);
        assertEquals(2, tab.peek(), "wraps around");

        root.setFocus(col.findById("s0"));
        for (int i = 0; i < 12; i++) {
            root.focusDirection(NavDirection.DOWN);
        }
        assertEquals("s12", root.focused().id());
        for (int i = 0; i < 60; i++) {
            root.tick(1f / 60f);
        }
        assertTrue(scroll.scrollOffset() > 100f, "scrolled to focused: " + scroll.scrollOffset());
    }
}
