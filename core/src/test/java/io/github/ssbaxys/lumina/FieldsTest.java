package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.ColorField;
import io.github.ssbaxys.lumina.widget.ContextMenu;
import io.github.ssbaxys.lumina.widget.NumberField;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.SliderField;
import io.github.ssbaxys.lumina.widget.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldsTest {
    @org.junit.jupiter.api.BeforeEach
    void russianTexts() {
        io.github.ssbaxys.lumina.i18n.I18n.setLanguage("ru_ru");
    }

    private static final int CTRL = Keys.MOD_CONTROL;
    private static final int SHIFT = Keys.MOD_SHIFT;

    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    @BeforeEach
    void clearClipboard() {
        TestSupport.clipboard = "";
    }

    private static UiRoot mount(UiElement... elements) {
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column().gap(10)).fill().add(elements), Theme.dark());
        root.resize(400, 300);
        root.tick(0f);
        return root;
    }

    private static void type(UiRoot root, String s) {
        for (char c : s.toCharArray()) {
            root.charTyped(c, 0);
        }
    }

    private static void key(UiRoot root, int key, int mods) {
        root.keyDown(key, 0, mods);
    }

    private static void key(UiRoot root, int key) {
        key(root, key, 0);
    }

    private static void click(UiRoot root, float x, float y, int button) {
        root.pointerMove(x, y);
        root.pointerDown(x, y, button);
        root.pointerUp(x, y, button);
    }

    private static void click(UiRoot root, UiElement e, int button) {
        click(root, e.screenX() + e.width() / 2f, e.screenY() + e.height() / 2f, button);
    }

    /** Экранная X середины символа {@code index} (6px на символ, отступ поля слева 7px). */
    private static float charX(TextField f, int index) {
        return f.screenX() + 7f + index * 6f + 3f;
    }

    private static float midY(UiElement e) {
        return e.screenY() + e.height() / 2f;
    }

    private static ContextMenu openMenu(UiRoot root) {
        assertEquals(1, root.popups().size(), "context menu is open");
        return assertInstanceOf(ContextMenu.class, root.popups().get(0));
    }

    @Test
    void typingAndEditingKeys() {
        Signal<String> s = Signal.of("");
        TextField f = new TextField(s).width(Dim.px(200));
        UiRoot root = mount(f);
        root.setFocus(f);

        type(root, "hello world");
        assertEquals("hello world", s.peek(), "live: the signal follows typing");
        assertEquals(11, f.caret());

        key(root, Keys.HOME);
        key(root, Keys.RIGHT, CTRL);
        assertEquals(6, f.caret(), "Ctrl+Right jumps over the word and the space");
        key(root, Keys.BACKSPACE, CTRL);
        assertEquals("world", f.text(), "Ctrl+Backspace removes the previous word");

        key(root, Keys.END);
        key(root, Keys.BACKSPACE);
        assertEquals("worl", f.text());
        key(root, Keys.DELETE);
        assertEquals("worl", f.text(), "Delete at the end does nothing");
        key(root, Keys.HOME);
        key(root, Keys.DELETE);
        assertEquals("orl", f.text());

        key(root, Keys.RIGHT, SHIFT);
        key(root, Keys.RIGHT, SHIFT);
        assertEquals("or", f.selectedText());
        type(root, "X");
        assertEquals("Xl", s.peek(), "typing replaces the selection");

        f.setText("one two three");
        key(root, Keys.END);
        key(root, Keys.LEFT, CTRL);
        assertEquals(8, f.caret());
        key(root, Keys.DELETE, CTRL);
        assertEquals("one two ", f.text(), "Ctrl+Delete removes the next word");
        assertTrue(root.keyDown(Keys.E, 0, 0), "printable keys stay in the field");
    }

    @Test
    void selectionClipboardAndHistory() {
        Signal<String> s = Signal.of("alpha beta");
        TextField f = new TextField(s).width(Dim.px(200));
        UiRoot root = mount(f);
        root.setFocus(f);

        key(root, Keys.A, CTRL);
        key(root, Keys.C, CTRL);
        assertEquals("alpha beta", TestSupport.clipboard);
        key(root, Keys.END);
        type(root, " ");
        key(root, Keys.V, CTRL);
        assertEquals("alpha beta alpha beta", s.peek());

        key(root, Keys.Z, CTRL);
        assertEquals("alpha beta ", f.text(), "undo the paste");
        key(root, Keys.Z, CTRL);
        assertEquals("alpha beta", f.text(), "undo the typing");
        assertFalse(f.canUndo());
        key(root, Keys.Y, CTRL);
        assertEquals("alpha beta ", f.text(), "redo");
        key(root, Keys.Z, CTRL | SHIFT);
        assertEquals("alpha beta alpha beta", s.peek(), "Ctrl+Shift+Z also redoes");

        f.setText("alpha beta");
        key(root, Keys.HOME);
        key(root, Keys.RIGHT, CTRL | SHIFT);
        assertEquals("alpha ", f.selectedText());
        key(root, Keys.X, CTRL);
        assertEquals("alpha ", TestSupport.clipboard);
        assertEquals("beta", s.peek());

        // мышь: двойной клик выделяет слово, перетаскивание — диапазон
        f.setText("foo bar baz");
        root.tick(0f);
        float y = midY(f);
        click(root, charX(f, 5), y, Keys.MOUSE_LEFT);
        click(root, charX(f, 5), y, Keys.MOUSE_LEFT);
        assertEquals("bar", f.selectedText(), "double click selects a word");

        root.pointerMove(charX(f, 8) - 3f, y);
        root.pointerDown(charX(f, 8) - 3f, y, Keys.MOUSE_LEFT);
        root.pointerMove(charX(f, 11) - 3f, y);
        root.pointerUp(charX(f, 11) - 3f, y, Keys.MOUSE_LEFT);
        assertEquals("baz", f.selectedText(), "drag selects");
    }

    @Test
    void limitsFiltersAndScrolling() {
        TextField limited = new TextField().maxLength(5).width(Dim.px(200));
        TextField digits = new TextField().charFilter(Character::isDigit).width(Dim.px(200));
        TextField narrow = new TextField().width(Dim.px(60)).placeholder("Поиск");
        UiRoot root = mount(limited, digits, narrow);

        root.setFocus(limited);
        type(root, "abcdefg");
        assertEquals("abcde", limited.text());
        root.setFocus(digits);
        type(root, "a1b2");
        assertEquals("12", digits.text());
        TestSupport.clipboard = "x3\ny4";
        key(root, Keys.V, CTRL);
        assertEquals("1234", digits.text(), "paste goes through the filter too");

        assertEquals("Поиск", narrow.placeholderText());
        root.setFocus(narrow);
        type(root, "abcdefghijklmnopqrstuvwxyz");
        float view = 60f - 14f;
        assertTrue(narrow.scrollOffset() > 0f, "long text scrolls");
        float caretX = narrow.caret() * 6f - narrow.scrollOffset();
        assertTrue(caretX >= 0f && caretX <= view, "caret stays visible: " + caretX);
        key(root, Keys.HOME);
        assertEquals(0f, narrow.scrollOffset(), 1e-4);
    }

    @Test
    void submitCommitOnBlurAndEscape() {
        Signal<String> s = Signal.of("");
        List<String> submitted = new ArrayList<>();
        TextField f = new TextField(s).live(false).onSubmit(submitted::add);
        Button other = new Button("other");
        UiRoot root = mount(f, other);
        root.setFocus(f);

        type(root, "abc");
        assertEquals("", s.peek(), "not live: nothing written yet");
        key(root, Keys.ENTER);
        assertEquals("abc", s.peek());
        assertEquals(List.of("abc"), submitted);

        type(root, "d");
        root.setFocus(other);
        assertEquals("abcd", s.peek(), "focus loss commits");

        root.setFocus(f);
        key(root, Keys.END);
        type(root, "zzz");
        assertTrue(root.keyDown(Keys.ESCAPE, 0, 0));
        assertEquals("abcd", f.text(), "Escape reverts uncommitted input");
        assertFalse(root.keyDown(Keys.ESCAPE, 0, 0), "nothing to revert: Escape goes to the screen");

        s.set("external");
        assertEquals("external", f.text(), "clean field follows the signal");
    }

    @Test
    void focusAnimatesBorderToAccent() {
        TextField f = new TextField();
        UiRoot root = mount(f);
        int accent = root.theme().get(Tokens.ACCENT);
        assertTrue(f.computedStyle().get(Styles.BORDER_COLOR) != accent);
        root.setFocus(f);
        root.tick(0.05f);
        int mid = f.computedStyle().get(Styles.BORDER_COLOR);
        assertTrue(mid != accent, "border colour transitions, not jumps");
        root.tick(1f);
        assertEquals(accent, f.computedStyle().get(Styles.BORDER_COLOR));

        TextField u = new TextField().variant("underline");
        mount(u);
        assertTrue(u.computedStyle().get(Styles.UNDERLINE));
        assertEquals(0f, u.computedStyle().get(Styles.BORDER_WIDTH), 1e-6);
    }

    @Test
    void numberFieldClampsStepsAndScrolls() {
        Signal<Float> v = Signal.of(5f);
        NumberField n = new NumberField(v, 0f, 10f).step(0.5f).decimals(1);
        UiRoot root = mount(n);
        assertEquals("5.0", n.text());
        root.setFocus(n);

        key(root, Keys.UP);
        assertEquals(5.5f, v.peek(), 1e-6);
        key(root, Keys.UP, SHIFT);
        assertEquals(10f, v.peek(), 1e-6, "Shift = x10, clamped to max");
        key(root, Keys.DOWN);
        assertEquals(9.5f, v.peek(), 1e-6);
        assertEquals("9.5", n.text());

        key(root, Keys.A, CTRL);
        type(root, "4abc2");
        assertEquals("42", n.text(), "letters are rejected");
        key(root, Keys.ENTER);
        assertEquals(10f, v.peek(), 1e-6, "typed value clamps on Enter");
        assertEquals("10.0", n.text());

        key(root, Keys.A, CTRL);
        type(root, "-");
        key(root, Keys.ENTER);
        assertEquals(10f, v.peek(), 1e-6, "invalid input keeps the value");
        assertEquals("10.0", n.text(), "and the text reverts");

        key(root, Keys.A, CTRL);
        type(root, "2,25");
        root.setFocus(null);
        assertEquals(2.3f, v.peek(), 1e-5, "comma decimal, rounded to 1 decimal on blur");

        root.setFocus(n);
        float x = n.screenX() + n.width() / 2f;
        float y = midY(n);
        root.scroll(x, y, 0, 1);
        assertEquals(2.8f, v.peek(), 1e-5, "wheel up adds a step");
        root.scroll(x, y, 0, -1);
        assertEquals(2.3f, v.peek(), 1e-5);

        v.set(7f);
        assertEquals("7.0", n.text(), "external change updates the text");
    }

    @Test
    void numberFieldScrubsOnPrefix() {
        Signal<Float> v = Signal.of(1f);
        NumberField n = new NumberField(v, 0f, 100f).step(1f).decimals(0).prefix("W").width(Dim.px(80));
        UiRoot root = mount(n);
        float x = n.screenX() + 7f + 3f;
        float y = midY(n);
        root.pointerMove(x, y);
        root.pointerDown(x, y, Keys.MOUSE_LEFT);
        root.pointerMove(x + 20f, y);
        root.pointerUp(x + 20f, y, Keys.MOUSE_LEFT);
        assertEquals(11f, v.peek(), 1e-6, "2px per step");
        assertEquals("11", n.text());
    }

    @Test
    void sliderFieldSharesOneSignal() {
        Signal<Float> v = Signal.of(1f);
        SliderField row = new SliderField("Border width", v, 0f, 4f).step(0.01f).decimals(2);
        UiRoot root = mount(row);
        assertSame(v, row.slider().value());
        assertSame(v, row.field().value());
        v.set(1.33f);
        assertEquals("1.33", row.field().text());
        root.setFocus(row.field());
        key(root, Keys.A, CTRL);
        type(root, "3.5");
        key(root, Keys.ENTER);
        assertEquals(3.5f, row.slider().value().peek(), 1e-6);
    }

    @Test
    void colorFieldParsesHex() {
        assertEquals(0xFFFF0000, ColorField.parse("#FF0000"));
        assertEquals(0x80FF0000, ColorField.parse("80ff0000"));
        assertEquals(0xFFAABBCC, ColorField.parse("#abc"));
        assertNull(ColorField.parse("#12345"));
        assertNull(ColorField.parse("#GG0000"));

        Signal<Integer> c = Signal.of(0xFF5B8CFF);
        ColorField f = new ColorField(c);
        UiRoot root = mount(f);
        assertEquals("#5B8CFF", f.text());
        root.setFocus(f);
        key(root, Keys.A, CTRL);
        type(root, "#0f0");
        assertEquals(0xFF00FF00, c.peek(), "valid input applies while typing");
        assertEquals("#0f0", f.text(), "and does not disturb the text");
        key(root, Keys.ENTER);
        assertEquals("#00FF00", f.text(), "Enter expands to full form");

        c.set(0x8000FF00);
        root.setFocus(null);
        assertEquals("#8000FF00", f.text());

        root.setFocus(f);
        key(root, Keys.A, CTRL);
        type(root, "#12");
        key(root, Keys.ENTER);
        assertEquals(0x8000FF00, c.peek(), "invalid hex keeps the colour");
        assertEquals("#8000FF00", f.text());

        f.alpha(false);
        assertEquals("#00FF00", f.text(), "without alpha only #RRGGBB is shown");
    }

    @Test
    void contextMenuOpensOnRightClickAndRunsItems() {
        Signal<String> s = Signal.of("hello");
        int[] custom = {0};
        TextField f = new TextField(s).width(Dim.px(200)).defaultValue("def").menuItem("Своё", () -> custom[0]++);
        UiRoot root = mount(f);

        click(root, f, Keys.MOUSE_RIGHT);
        assertSame(f, root.focused());
        ContextMenu menu = openMenu(root);
        root.tick(1f);
        assertEquals(1f, menu.computedStyle().get(Styles.OPACITY), 1e-6, "menu fades in");
        assertFalse(menu.row("Повторить").isEnabled(), "nothing to redo");
        assertNotNull(menu.row("Выделить всё"));
        assertEquals("Ctrl+A", menu.row("Выделить всё").item().shortcut());

        click(root, menu.row("Повторить"), Keys.MOUSE_LEFT);
        assertTrue(menu.isOpen(), "disabled item does nothing");

        click(root, menu.row("Выделить всё"), Keys.MOUSE_LEFT);
        assertFalse(menu.isOpen(), "running an item closes the menu");
        assertSame(f, root.focused(), "the field keeps focus");
        assertEquals("hello", f.selectedText());

        click(root, f, Keys.MOUSE_RIGHT);
        menu = openMenu(root);
        root.tick(1f);
        click(root, menu.row("Копировать"), Keys.MOUSE_LEFT);
        assertEquals("hello", TestSupport.clipboard);

        click(root, f, Keys.MOUSE_RIGHT);
        menu = openMenu(root);
        root.tick(1f);
        click(root, menu.row("Сбросить по умолчанию"), Keys.MOUSE_LEFT);
        assertEquals("def", s.peek());

        click(root, f, Keys.MOUSE_RIGHT);
        menu = openMenu(root);
        root.tick(1f);
        assertFalse(menu.row("Сбросить по умолчанию").isEnabled(), "already at default");
        click(root, menu.row("Своё"), Keys.MOUSE_LEFT);
        assertEquals(1, custom[0]);

        // клавиатура: стрелка вниз подсвечивает первый доступный пункт, Enter выполняет
        click(root, f, Keys.MOUSE_RIGHT);
        menu = openMenu(root);
        key(root, Keys.DOWN);
        assertTrue(menu.row("Отменить").hasState(ContextMenu.ACTIVE));
        key(root, Keys.ENTER);
        assertEquals("hello", s.peek(), "undo of the reset");
        assertTrue(root.popups().isEmpty());

        click(root, f, Keys.MOUSE_RIGHT);
        openMenu(root);
        key(root, Keys.ESCAPE);
        assertTrue(root.popups().isEmpty(), "Escape closes the menu");
        assertSame(f, root.focused());
    }

    @Test
    void popupsLayerAboveContentAndCloseOnOutsideClick() {
        int[] presses = {0};
        Button under = new Button("under", () -> presses[0]++).size(100, 40);
        UiRoot root = mount(under);
        Panel popup = Panel.column().size(50, 30);

        root.showPopup(popup, 20, 5);
        root.tick(0f);
        assertEquals(20f, popup.x(), 1e-6);
        root.pointerMove(30, 15);
        assertSame(popup, root.hovered(), "popup is hit before content");
        click(root, 30, 15, Keys.MOUSE_LEFT);
        assertEquals(0, presses[0], "content under the popup is not pressed");
        assertTrue(root.isPopupOpen(popup), "clicking inside keeps it open");

        click(root, 90, 30, Keys.MOUSE_LEFT);
        assertFalse(root.isPopupOpen(popup), "outside click closes it");
        assertEquals(0, presses[0], "the closing click is swallowed");
        click(root, 90, 30, Keys.MOUSE_LEFT);
        assertEquals(1, presses[0]);

        Panel sticky = Panel.column().size(50, 30);
        root.showPopup(sticky, 390, 290, false);
        assertEquals(340f, sticky.x(), 1e-6, "kept inside the screen");
        assertEquals(260f, sticky.y(), 1e-6);
        click(root, 10, 10, Keys.MOUSE_LEFT);
        assertTrue(root.isPopupOpen(sticky), "non-dismissable popup ignores outside clicks");
        root.closePopups();
        assertTrue(root.popups().isEmpty());
        assertNull(sticky.root());
    }

    @Test
    void rendersVariantsCaretSelectionAndMenu() {
        TextField outlined = new TextField(Signal.of("text")).prefix("#").suffix("px");
        TextField filled = new TextField().variant("filled").placeholder("Пусто");
        TextField underline = new TextField(Signal.of("u")).variant("underline");
        TextField search = new TextField(Signal.of("query")).variant("pill").icon(TextField.SEARCH).clearButton(true);
        TextField password = new TextField(Signal.of("secret")).password(true);
        ColorField color = new ColorField(Signal.of(0x80FF8800));
        UiRoot root = mount(outlined, filled, underline, search, password, color);
        root.setFocus(underline);
        key(root, Keys.A, CTRL);
        underline.flashError();
        click(root, search, Keys.MOUSE_RIGHT);
        root.tick(0.05f);

        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(400, 300);
        root.render(canvas, 0f);
        canvas.end();
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("text Пусто ")), "placeholder: " + canvas.ops);
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("text ****** ")), "password is masked");
        assertFalse(canvas.ops.stream().anyMatch(o -> o.startsWith("text secret")));
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("text Копировать")), "menu is drawn above content");
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("text Ctrl+C")), "shortcut hints are drawn");
    }

    @Test
    void markupTags() {
        Signal<String> name = Signal.of("Steve");
        Signal<Float> size = Signal.of(4f);
        Signal<Integer> color = Signal.of(0xFFFF0000);
        Bindings b = new Bindings().signal("name", name).signal("size", size).signal("color", color);
        Markup.Built built = Markup.build("""
                <column>
                  <input id="name" bind="name" placeholder="Имя" variant="underline" maxlength="8"/>
                  <number id="size" bind="size" min="0" max="10" step="2" decimals="0" suffix="px"/>
                  <color-field id="color" bind="color"/>
                  <input id="free" value="x" class="wide"/>
                </column>
                """, b);
        UiRoot root = mount(built.root());
        TextField input = assertInstanceOf(TextField.class, built.root().findById("name"));
        assertEquals("Steve", input.text());
        assertEquals("Имя", input.placeholderText());
        assertTrue(input.styleClass().contains("underline"));
        NumberField number = assertInstanceOf(NumberField.class, built.root().findById("size"));
        assertEquals("4", number.text());
        root.setFocus(number);
        key(root, Keys.UP);
        assertEquals(6f, size.peek(), 1e-6);
        ColorField cf = assertInstanceOf(ColorField.class, built.root().findById("color"));
        assertEquals("#FF0000", cf.text());
        TextField free = assertInstanceOf(TextField.class, built.root().findById("free"));
        assertEquals("x", free.text());
        assertTrue(free.styleClass().contains("field") && free.styleClass().contains("wide"));
    }
}
