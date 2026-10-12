package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.CssValues;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.markup.MarkupView;
import io.github.ssbaxys.lumina.markup.Stylesheet;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Slider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkupTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    private static UiRoot mount(UiElement e) {
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).fill().add(e), Theme.dark());
        root.resize(400, 300);
        root.tick(0f);
        return root;
    }

    private static void click(UiRoot root, UiElement e) {
        float x = e.screenX() + e.width() / 2f;
        float y = e.screenY() + e.height() / 2f;
        root.pointerMove(x, y);
        root.pointerDown(x, y, Keys.MOUSE_LEFT);
        root.pointerUp(x, y, Keys.MOUSE_LEFT);
    }

    @Test
    void parsesCssValues() {
        Brush.Gradient g = (Brush.Gradient) CssValues.brush("linear-gradient(to right, #000, #fff)");
        assertEquals(Color.BLACK, g.sample(0f, 0.5f, 10, 10));
        assertEquals(Color.WHITE, g.sample(1f, 0.5f, 10, 10));
        assertInstanceOf(Brush.Radial.class, CssValues.brush("radial-gradient(circle at 30% 30%, white, blue)"));
        assertInstanceOf(Brush.Conic.class, CssValues.brush("conic-gradient(from 90deg, red, yellow, red) "));
        assertEquals(new Radii(1, 2, 3, 4), CssValues.radii("1px 2px 3px 4px"));
        assertEquals(0.3f, CssValues.time("300ms"), 1e-6);
        assertEquals(0.5f, CssValues.easing("ease-in-out").apply(0.5f), 0.01f);
        assertEquals(1f, CssValues.easing("out-back").apply(1f), 1e-4);
        assertEquals(8f, CssValues.shadow("0 2px 8px rgba(0,0,0,0.5)").blur(), 1e-6);
    }

    @Test
    void stylesheetMatchesSelectorsStatesAndSpecificity() {
        String html = """
                <style>
                  :root { --pad: 6px; }
                  button { opacity: 0.9; }
                  .menu button { border-radius: 4px; }
                  .menu > .danger { background: #f00; }
                  #ok { background: #0f0; }
                  button:hover { opacity: 0.5; transition: opacity 0.2s, transform .3s out-back; }
                  .padded { padding: var(--pad); }
                </style>
                <column class="menu">
                  <button id="ok" class="danger">OK</button>
                  <button class="danger padded">Cancel</button>
                </column>
                """;
        Markup.Built built = Markup.build(html, new Bindings());
        UiRoot root = mount(built.root());
        Button ok = (Button) built.root().findById("ok");
        Button cancel = (Button) built.root().children().get(1);
        assertEquals(Brush.solid(0xFF00FF00), ok.computedStyle().get(Styles.BACKGROUND), "#id beats .class");
        assertEquals(Brush.solid(0xFFFF0000), cancel.computedStyle().get(Styles.BACKGROUND));
        assertEquals(Radii.all(4), cancel.computedStyle().get(Styles.RADIUS));
        assertEquals(6f, cancel.padding().top(), 1e-6);
        assertEquals(0.9f, cancel.computedStyle().get(Styles.OPACITY), 1e-6);
        cancel.setState(State.HOVERED, true);
        root.tick(1f);
        assertEquals(0.5f, cancel.computedStyle().get(Styles.OPACITY), 1e-6);
        assertTrue(cancel.styleClass().contains("button") && cancel.styleClass().contains("danger"));
    }

    @Test
    void tokenVariablesFollowTheTheme() {
        Markup.Built built = Markup.build("""
                <style>.chip { background: var(--accent); border: 1px solid var(--accent); }</style>
                <div class="chip" width="20" height="20"></div>
                """, new Bindings());
        UiRoot root = mount(built.root());
        root.theme().set(Tokens.ACCENT, 0xFF123456);
        root.tick(0f);
        root.tick(2f);
        assertEquals(Brush.solid(0xFF123456), built.root().computedStyle().get(Styles.BACKGROUND));
        assertEquals(0xFF123456, built.root().computedStyle().get(Styles.BORDER_COLOR));
        assertEquals(Dim.px(20).toString(), built.root().layoutParams().width.toString());
    }

    @Test
    void stateTemplatesActionsAndConditions() {
        Markup.Built built = Markup.build("""
                <state name="count" value="0"/>
                <state name="tab" value="1"/>
                <column gap="4">
                  <label id="text">Clicks: {{count}}</label>
                  <button id="inc" on-click="inc:count">+1</button>
                  <button id="tab2" on-click="set:tab=2">Tab 2</button>
                  <label id="first" if="tab==1">first</label>
                  <label id="second" if="tab==2">second</label>
                </column>
                """, new Bindings());
        UiRoot root = mount(built.root());
        Label text = (Label) built.root().findById("text");
        assertEquals("Clicks: 0", text.text());
        click(root, built.root().findById("inc"));
        click(root, built.root().findById("inc"));
        assertEquals("Clicks: 2", text.text());
        assertTrue(built.root().findById("first").isVisible());
        assertFalse(built.root().findById("second").isVisible());
        root.tick(0f);
        click(root, built.root().findById("tab2"));
        assertFalse(built.root().findById("first").isVisible());
        assertTrue(built.root().findById("second").isVisible());
    }

    @Test
    void javaHandlersAndTwoWayBinding() {
        Signal<Float> volume = Signal.of(25f);
        int[] saved = {0};
        Bindings b = new Bindings().signal("volume", volume).handler("save", () -> saved[0]++);
        Markup.Built built = Markup.build("""
                <row><slider id="s" bind="volume" min="0" max="100" step="1"/>
                <label id="v">{{volume|int}}%</label><button id="save" on-click="save">Save</button></row>
                """, b);
        UiRoot root = mount(built.root());
        Slider s = (Slider) built.root().findById("s");
        assertEquals(volume, s.value());
        volume.set(70f);
        assertEquals("70%", ((Label) built.root().findById("v")).text());
        click(root, built.root().findById("save"));
        assertEquals(1, saved[0]);
    }

    @Test
    void keyframeAnimationsRun() {
        Markup.Built built = Markup.build("""
                <style>
                  @keyframes pulse { from { opacity: 0.2; } to { opacity: 1; } }
                  .p { animation: pulse 1s linear infinite alternate; }
                </style>
                <div class="p" width="10" height="10"></div>
                """, new Bindings());
        UiRoot root = mount(built.root());
        root.tick(0.5f);
        assertEquals(0.6f, built.root().computedStyle().get(Styles.OPACITY), 0.02f);
        root.tick(1f);
        assertEquals(0.6f, built.root().computedStyle().get(Styles.OPACITY), 0.02f, "alternates back");
    }

    @Test
    void paragraphHtmlBecomesMarkdownAndViewHotReloads() {
        Map<String, String> files = new HashMap<>();
        files.put("t:ui/page.html", "<link rel=\"stylesheet\" href=\"t:ui/page.css\"><p id=\"p\">Hello <b>bold</b> <a href=\"x\">link</a></p>");
        files.put("t:ui/page.css", "p { opacity: 0.5; }");
        MarkupView view = MarkupView.resource("t:ui/page.html", new Bindings()).resources(files::get).watch(0.5f);
        UiRoot root = mount(view);
        assertEquals(1, view.generation());
        assertEquals("Hello **bold** [link](x)",
                ((io.github.ssbaxys.lumina.markdown.Markdown) view.content()).text());
        assertEquals(0.5f, view.content().computedStyle().get(Styles.OPACITY), 1e-6);
        files.put("t:ui/page.css", "p { opacity: 0.25; }");
        root.tick(0.6f);
        assertEquals(2, view.generation(), "linked stylesheet change triggers rebuild");
        root.tick(0f);
        assertEquals(0.25f, view.content().computedStyle().get(Styles.OPACITY), 1e-6);
    }

    @Test
    void stylesheetCanStyleTheWholeTheme() {
        Theme theme = Theme.dark();
        Stylesheet.parse("""
                :root { --accent: #ff8800; --corner: 2px; }
                .button:hover { transform: scale(1.2); }
                """).applyTo(theme);
        assertEquals(0xFFFF8800, theme.get(Tokens.ACCENT));
        assertEquals(2f, theme.get(Tokens.CORNER), 1e-6);
        Button b = new Button("x");
        UiRoot root = mount(b);
        root.setTheme(theme);
        b.setState(State.HOVERED, true);
        root.tick(0f);
        root.tick(2f);
        assertEquals(1.2f, b.computedStyle().get(Styles.SCALE), 1e-6);
    }
}
