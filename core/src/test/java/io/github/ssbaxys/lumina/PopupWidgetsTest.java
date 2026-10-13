package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.CalendarView;
import io.github.ssbaxys.lumina.widget.DatePicker;
import io.github.ssbaxys.lumina.widget.KeyBindField;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.RadialMenu;
import io.github.ssbaxys.lumina.widget.Select;
import io.github.ssbaxys.lumina.widget.TextField;
import io.github.ssbaxys.lumina.widget.Tooltip;
import io.github.ssbaxys.lumina.widget.Tooltips;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PopupWidgetsTest {
    @org.junit.jupiter.api.BeforeEach
    void russianTexts() {
        io.github.ssbaxys.lumina.i18n.I18n.setLanguage("ru_ru");
    }

    private static final List<String> FRUITS = List.of("Яблоко", "Банан", "Вишня", "Груша", "Дыня",
            "Ежевика", "Земляника", "Инжир", "Киви", "Лайм", "Манго", "Нектарин");

    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    @AfterEach
    void resetTooltips() {
        Tooltips.hideAll();
        Tooltips.setDefaultDelay(0.5f);
        Tooltips.setDefaultKind(Tooltip.Kind.VANILLA);
    }

    private static UiRoot mount(UiElement... elements) {
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column().gap(10)).fill().add(elements), Theme.dark());
        root.resize(400, 300);
        root.tick(0f);
        return root;
    }

    private static void click(UiRoot root, float x, float y, int button) {
        root.pointerMove(x, y);
        root.pointerDown(x, y, button);
        root.pointerUp(x, y, button);
    }

    private static void click(UiRoot root, UiElement e) {
        click(root, e.screenX() + e.width() / 2f, e.screenY() + e.height() / 2f, Keys.MOUSE_LEFT);
    }

    private static void key(UiRoot root, int key) {
        root.keyDown(key, 0, 0);
    }

    private static void frame(UiRoot root) {
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(root.width(), root.height());
        root.render(canvas, 0f);
        canvas.end();
    }

    private static boolean drew(UiRoot root, String op) {
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(root.width(), root.height());
        root.tick(0f);
        root.render(canvas, 0f);
        canvas.end();
        return canvas.ops.stream().anyMatch(o -> o.startsWith(op));
    }

    // ================================================================ слой оверлеев

    @Test
    void overlaysAreDrawnOnTopButTakeNoInput() {
        int[] presses = {0};
        Panel content = Panel.column().size(100, 100);
        content.on(io.github.ssbaxys.lumina.event.PointerEvent.PRESS, e -> presses[0]++);
        UiRoot root = mount(content);
        Panel overlay = Panel.column().size(60, 20);
        root.showOverlay(overlay, 10, 10);
        assertTrue(root.popups().isEmpty(), "an overlay is not a popup");
        assertEquals(List.of(overlay), root.overlays());
        click(root, 20, 15, Keys.MOUSE_LEFT);
        assertEquals(1, presses[0], "the click goes through the overlay");
        assertTrue(root.isPopupOpen(overlay), "outside clicks do not close overlays");
        key(root, Keys.ESCAPE);
        assertTrue(root.isPopupOpen(overlay), "Escape does not close overlays");

        root.movePopup(overlay, 380, 5);
        assertEquals(320f, overlay.x(), 1e-6, "moved; flipped left of the point to stay on screen");
        root.closePopup(overlay);
        assertTrue(root.overlays().isEmpty());
    }

    @Test
    void keyReleaseGoesToTopPopupFirst() {
        UiRoot root = mount(Panel.column().size(10, 10));
        int[] releases = {0};
        Panel popup = Panel.column().size(40, 40);
        popup.on(io.github.ssbaxys.lumina.event.KeyEvent.RELEASE, e -> {
            releases[0]++;
            e.consume();
        });
        root.showPopup(popup, 0, 0);
        assertTrue(root.keyUp(Keys.R, 0, 0));
        assertEquals(1, releases[0]);
    }

    // ================================================================ выпадающий список

    @Test
    void selectOpensWithMouseAndPicksRow() {
        Signal<String> fruit = Signal.of("Банан");
        List<String> changes = new ArrayList<>();
        Select<String> select = new Select<>(fruit, FRUITS).maxRows(5).onChange(changes::add);
        UiRoot root = mount(select);
        click(root, select);
        assertTrue(select.isOpen());
        assertTrue(select.hasState(Select.OPEN));
        root.tick(1f);
        UiElement dropdown = select.dropdown();
        assertEquals(1, root.popups().size());
        assertTrue(dropdown.screenY() >= select.screenY() + select.height(), "opens below the field");
        assertTrue(dropdown.width() >= select.width(), "at least as wide as the field");
        assertEquals("Банан", select.activeOption(), "the current value is highlighted");

        UiElement list = dropdown.children().get(dropdown.children().size() - 1);
        assertEquals(5 * 18f, list.height(), 1e-3, "five rows visible");
        // третья строка — "Вишня"
        click(root, list.screenX() + 30, list.screenY() + 2 * 18 + 9, Keys.MOUSE_LEFT);
        assertEquals("Вишня", fruit.peek());
        assertEquals(List.of("Вишня"), changes);
        assertFalse(select.isOpen());
        assertFalse(select.hasState(Select.OPEN));
        assertTrue(drew(root, "text Вишня"));
    }

    @Test
    void selectKeyboardNavigation() {
        Signal<String> fruit = Signal.of("Яблоко");
        Select<String> select = new Select<>(fruit, FRUITS).maxRows(4);
        UiRoot root = mount(select);
        root.setFocus(select);

        key(root, Keys.DOWN);
        assertEquals("Банан", fruit.peek(), "closed: arrows change the value in place");
        root.charTyped('г', 0);
        assertEquals("Груша", fruit.peek(), "a letter jumps to the option");

        key(root, Keys.ENTER);
        assertTrue(select.isOpen());
        root.tick(1f);
        assertEquals("Груша", select.activeOption());
        key(root, Keys.DOWN);
        key(root, Keys.DOWN);
        assertEquals("Ежевика", select.activeOption());
        key(root, Keys.END);
        assertEquals("Нектарин", select.activeOption());
        root.tick(1f);
        frame(root);
        key(root, Keys.HOME);
        key(root, Keys.PAGE_DOWN);
        assertEquals("Дыня", select.activeOption(), "PageDown moves by the visible rows");
        key(root, Keys.ESCAPE);
        assertFalse(select.isOpen(), "Escape closes the list");
        assertEquals("Груша", fruit.peek(), "without a change");
        assertSame(select, root.focused());

        key(root, Keys.SPACE);
        key(root, Keys.UP);
        key(root, Keys.ENTER);
        assertEquals("Вишня", fruit.peek());
        assertFalse(select.isOpen());
    }

    @Test
    void selectSearchFiltersOptions() {
        Signal<String> fruit = Signal.of(null);
        Select<String> select = new Select<>(fruit, FRUITS).searchable(true).placeholder("Фрукт")
                .icons(f -> f.equals("Яблоко") ? "minecraft:apple" : null);
        UiRoot root = mount(select);
        assertTrue(drew(root, "text Фрукт"), "placeholder while empty");
        click(root, select);
        root.tick(1f);
        TextField search = select.searchField();
        assertNotNull(search);
        assertSame(search, root.focused(), "typing goes to the search box");
        assertEquals(FRUITS.size(), select.visibleOptions().size());
        assertTrue(drew(root, "item minecraft:apple"), "item icons are drawn");

        for (char c : "ИК".toCharArray()) {
            root.charTyped(c, 0);
        }
        assertEquals(List.of("Ежевика", "Земляника"), select.visibleOptions(), "case-insensitive substring");
        assertEquals("Ежевика", select.activeOption());
        key(root, Keys.DOWN);
        key(root, Keys.ENTER);
        assertEquals("Земляника", fruit.peek());
        assertFalse(select.isOpen());
        assertSame(select, root.focused(), "focus comes back to the field");

        click(root, select);
        root.tick(1f);
        root.charTyped('я', 0);
        root.charTyped('я', 0);
        assertTrue(select.visibleOptions().isEmpty());
        assertTrue(drew(root, "text Ничего не найдено"));
        key(root, Keys.ESCAPE);
        assertFalse(select.isOpen());
    }

    @Test
    void selectOpensUpwardNearTheBottomAndScrolls() {
        Signal<String> fruit = Signal.of("Яблоко");
        Select<String> select = new Select<>(fruit, FRUITS).maxRows(4);
        Panel spacer = Panel.column().size(10, 250);
        UiRoot root = mount(spacer, select);
        click(root, select);
        root.tick(1f);
        UiElement dropdown = select.dropdown();
        assertTrue(dropdown.screenY() + dropdown.height() <= select.screenY(), "no room below — opens above");
        UiElement list = dropdown.children().get(0);
        root.scroll(list.screenX() + 5, list.screenY() + 5, 0, -3);
        root.tick(1f);
        // после прокрутки верхняя видимая строка — уже не первая
        click(root, list.screenX() + 30, list.screenY() + 9, Keys.MOUSE_LEFT);
        assertFalse("Яблоко".equals(fruit.peek()), "scrolled list picks a later row: " + fruit.peek());
    }

    @Test
    @SuppressWarnings("unchecked")
    void selectFromMarkup() {
        Bindings b = new Bindings();
        b.state("mode", "normal");
        UiElement ui = Markup.build("""
                <select bind="mode" options="easy,normal,hard" labels="Легко,Нормально,Сложно"
                        icons="minecraft:apple,,minecraft:tnt" tooltip="Сложность"/>""", b).root();
        Select<?> select = assertInstanceOf(Select.class, ui);
        UiRoot root = mount(ui);
        assertTrue(drew(root, "text Нормально"));
        root.setFocus(select);
        key(root, Keys.DOWN);
        assertEquals("hard", b.get("mode").get());
        assertNotNull(Tooltips.of(select));

        UiElement withOptions = Markup.build("""
                <select bind="ore" search>
                  <option value="d" item="minecraft:diamond">Алмаз</option>
                  <option value="e" item="minecraft:emerald">Изумруд</option>
                </select>""", b).root();
        Select<?> s2 = assertInstanceOf(Select.class, withOptions);
        assertEquals(List.of("d", "e"), s2.options());
        assertEquals("Изумруд", ((Select<Object>) s2).labelOf("e"));
    }

    // ================================================================ подсказки

    @Test
    void tooltipAppearsAfterDelayFollowsAndHides() {
        Button button = new Button("Сохранить").tooltip("Сохранить мир\nCtrl+S");
        Panel other = Panel.column().size(50, 30);
        UiRoot root = mount(button, other);
        Tooltip tip = Tooltips.of(button);
        assertNotNull(tip);
        assertEquals(Tooltip.Kind.VANILLA, tip.kind());

        float x = button.screenX() + 5;
        float y = button.screenY() + 5;
        root.pointerMove(x, y);
        root.tick(0.2f);
        assertTrue(root.overlays().isEmpty(), "not before the delay");
        root.tick(0.4f);
        assertEquals(1, root.overlays().size(), "shown after the hover delay");
        assertTrue(tip.isShowing());
        assertSame(tip, Tooltips.current());
        UiElement box = tip.box();
        assertEquals("tooltip-vanilla", box.styleClass());
        assertEquals(x + 12f, box.x(), 1e-3, "right of the cursor");
        // две строки: 9 + 1 шаг, 2px после заголовка; высота текста 8 + 10 + 2 и отступы 4+4
        assertEquals(28f, box.height(), 1e-3);
        assertTrue(drew(root, "text Ctrl+S"));
        assertTrue(root.popups().isEmpty(), "tooltips do not block input");

        root.pointerMove(x + 10, y + 2);
        root.tick(0.016f);
        assertEquals(x + 22f, box.x(), 1e-3, "follows the mouse");

        root.pointerDown(x + 10, y + 2, Keys.MOUSE_LEFT);
        assertFalse(tip.isShowing(), "a press hides it");
        root.pointerUp(x + 10, y + 2, Keys.MOUSE_LEFT);
        root.tick(1f);
        assertFalse(tip.isShowing(), "stays hidden until the mouse moves");
        root.pointerMove(x + 20, y + 2);
        root.tick(1f);
        assertTrue(tip.isShowing());

        root.pointerMove(other.screenX() + 5, other.screenY() + 5);
        assertFalse(tip.isShowing(), "leaving hides it");
        assertTrue(root.overlays().isEmpty());
    }

    @Test
    void tooltipStaysOnScreenAndSupportsContentKinds() {
        Tooltips.setDefaultKind(Tooltip.Kind.MODERN);
        Tooltips.setDefaultDelay(0f);
        Label corner = new Label("угол");
        Panel row = Panel.box(FlexLayout.row()).width(Dim.FILL).add(Panel.box(FlexLayout.column()).grow(1), corner);
        UiRoot root = mount(row);
        Tooltip tip = Tooltips.attach(corner, "Очень длинная подсказка, которая переносится по ширине").maxWidth(80);
        assertEquals(Tooltip.Kind.MODERN, tip.kind());
        root.pointerMove(398, corner.screenY() + 2);
        root.tick(0.016f);
        assertTrue(tip.isShowing(), "labels become hoverable for tooltips");
        UiElement box = tip.box();
        root.tick(1f);
        assertTrue(box.x() + box.width() <= 400f, "flipped left of the cursor");
        assertTrue(box.x() >= 0f);
        assertTrue(box.height() > 30f, "long text wraps into several lines: " + box.height());
        assertEquals("tooltip", box.styleClass());

        tip.markdown("**Жирный** текст");
        assertEquals(1, box.children().size(), "markdown content");
        tip.content(() -> new Label("Своё содержимое"));
        assertInstanceOf(Label.class, tip.box().children().get(0));
        assertTrue(drew(root, "text Своё содержимое"));

        Tooltips.detach(corner);
        assertFalse(tip.isShowing());
        assertNull(Tooltips.of(corner));
    }

    @Test
    void innermostTooltipWins() {
        Tooltips.setDefaultDelay(0f);
        Panel inner = Panel.column().size(40, 20).tooltip("внутренняя");
        Panel outer = Panel.column().size(120, 80).add(inner).tooltip("внешняя");
        UiRoot root = mount(outer);
        root.pointerMove(inner.screenX() + 3, inner.screenY() + 3);
        root.tick(0.016f);
        root.tick(0.016f);
        assertTrue(Tooltips.of(inner).isShowing());
        assertFalse(Tooltips.of(outer).isShowing());
        assertEquals(1, root.overlays().size());
        root.pointerMove(outer.screenX() + 100, outer.screenY() + 60);
        root.tick(0.016f);
        root.tick(0.016f);
        assertTrue(Tooltips.of(outer).isShowing());
        assertFalse(Tooltips.of(inner).isShowing());
    }

    @Test
    void tooltipHidesWhenItsElementIsRemoved() {
        Tooltips.setDefaultDelay(0f);
        Button b = new Button("X").tooltip("подсказка");
        UiRoot root = mount(b);
        root.pointerMove(b.screenX() + 2, b.screenY() + 2);
        root.tick(0.016f);
        assertTrue(Tooltips.of(b).isShowing());
        ((Panel) root.content()).clearChildren();
        root.tick(0.016f);
        assertTrue(root.overlays().isEmpty(), "no orphaned tooltip");
    }

    @Test
    void tooltipAttributeInMarkup() {
        Bindings b = new Bindings();
        b.state("n", 3);
        UiElement ui = Markup.build("""
                <button tooltip="Нажато: {{n}}\\nвторая" tooltip-kind="modern" tooltip-delay="100ms">Кнопка</button>""", b).root();
        Tooltip tip = Tooltips.of(ui);
        assertNotNull(tip);
        assertEquals("Нажато: 3\nвторая", tip.text());
        assertEquals(Tooltip.Kind.MODERN, tip.kind());
        b.writable("n").set(4);
        assertEquals("Нажато: 4\nвторая", tip.text());
    }

    // ================================================================ радиальное меню

    private static RadialMenu fourItems(List<String> log) {
        return new RadialMenu()
                .item("Север", "minecraft:compass", () -> log.add("N"))
                .item("Восток", () -> log.add("E"))
                .item("Юг", () -> log.add("S"))
                .submenu("Запад", "minecraft:apple",
                        RadialMenu.Item.of("Яблоко", () -> log.add("apple")),
                        RadialMenu.Item.of("Хлеб", () -> log.add("bread")),
                        RadialMenu.Item.of("Нельзя", () -> log.add("no")).enabled(false));
    }

    @Test
    void radialMenuPicksByDirectionAndOpensSubmenus() {
        List<String> log = new ArrayList<>();
        UiRoot root = mount(Panel.column().size(10, 10));
        RadialMenu menu = fourItems(log).show(root, 200, 150);
        assertTrue(menu.isOpen());
        assertEquals(200f, menu.centerX(), 1e-3);
        root.tick(1f);

        root.pointerMove(200, 60);
        assertEquals("Север", menu.hoveredItem().label(), "up = first wedge");
        root.pointerMove(340, 150);
        assertEquals("Восток", menu.hoveredItem().label(), "outside the ring still counts");
        root.pointerMove(399, 150);
        assertNull(menu.hoveredItem(), "far away — nothing");
        root.pointerMove(202, 151);
        assertNull(menu.hoveredItem(), "dead zone in the centre");
        assertTrue(drew(root, "freequad"), "wedges are drawn");
        assertTrue(drew(root, "item minecraft:compass"));

        click(root, 200, 240, Keys.MOUSE_LEFT);
        assertEquals(List.of("S"), log);
        assertFalse(menu.isOpen(), "picking an action closes the menu");

        menu.show(root, 200, 150);
        click(root, 120, 150, Keys.MOUSE_LEFT);
        assertTrue(menu.isOpen(), "a submenu keeps the menu open");
        assertEquals(1, menu.depth());
        assertEquals(3, menu.currentItems().size());
        click(root, 200, 150, Keys.MOUSE_LEFT);
        assertEquals(0, menu.depth(), "a click in the centre goes back");
        click(root, 120, 150, Keys.MOUSE_LEFT);
        key(root, Keys.ESCAPE);
        assertEquals(0, menu.depth(), "Escape goes back first");
        assertTrue(menu.isOpen());
        key(root, Keys.ESCAPE);
        assertFalse(menu.isOpen(), "then closes");
    }

    @Test
    void radialMenuWhileKeyHeld() {
        List<String> log = new ArrayList<>();
        UiRoot root = mount(Panel.column().size(300, 200));
        // Клавиши без фокуса получает корневой элемент: к нему и привязываем.
        RadialMenu.bindKey(root.content(), Keys.R, () -> fourItems(log));
        root.pointerMove(200, 100);
        key(root, Keys.R);
        assertEquals(1, root.popups().size());
        RadialMenu menu = assertInstanceOf(RadialMenu.class, root.popups().get(0));
        key(root, Keys.R);
        assertEquals(1, root.popups().size(), "key repeat does not open another menu");
        root.tick(1f);
        root.pointerMove(menu.centerX() + 80, menu.centerY());
        root.keyUp(Keys.R, 0, 0);
        assertEquals(List.of("E"), log, "release picks the hovered wedge");
        assertFalse(menu.isOpen());

        key(root, Keys.R);
        menu = (RadialMenu) root.popups().get(0);
        root.pointerMove(menu.centerX(), menu.centerY());
        root.keyUp(Keys.R, 0, 0);
        assertFalse(menu.isOpen(), "release in the centre just closes");
        assertEquals(1, log.size());
    }

    @Test
    void radialMenuKeyboard() {
        List<String> log = new ArrayList<>();
        UiRoot root = mount(Panel.column().size(10, 10));
        RadialMenu menu = fourItems(log).show(root, 200, 150);
        key(root, Keys.RIGHT);
        assertEquals("Север", menu.hoveredItem().label());
        key(root, Keys.LEFT);
        assertEquals("Запад", menu.hoveredItem().label());
        key(root, Keys.ENTER);
        assertEquals(1, menu.depth());
        key(root, '3');
        assertTrue(menu.isOpen(), "a disabled item does nothing");
        key(root, '2');
        assertEquals(List.of("bread"), log);
        assertFalse(menu.isOpen());
    }

    // ================================================================ клавиша и дата

    @Test
    void keyBindFieldListensAndBinds() {
        Signal<Integer> jump = Signal.of(Keys.SPACE);
        KeyBindField field = new KeyBindField(jump).defaultKey(Keys.SPACE);
        TextField next = new TextField();
        UiRoot root = mount(field, next);
        assertTrue(drew(root, "text Пробел"));

        click(root, field);
        assertTrue(field.isListening());
        assertTrue(drew(root, "text > Нажмите клавишу <"));
        key(root, Keys.ESCAPE);
        assertFalse(field.isListening(), "Escape cancels");
        assertEquals(Keys.SPACE, jump.peek());

        click(root, field);
        key(root, Keys.G);
        root.charTyped('g', 0);
        assertEquals(Keys.G, jump.peek());
        assertFalse(field.isListening());
        assertEquals("", next.text(), "the typed character does not leak");
        assertTrue(drew(root, "text G"));

        key(root, Keys.ENTER);
        assertTrue(field.isListening(), "Enter starts listening when focused");
        key(root, Keys.TAB);
        assertEquals(Keys.TAB, jump.peek(), "any key, even Tab, can be bound");

        click(root, field.screenX() + 3, field.screenY() + 3, Keys.MOUSE_RIGHT);
        assertEquals(Keys.SPACE, jump.peek(), "right click resets to the default");

        click(root, field);
        click(root, next);
        assertFalse(field.isListening(), "losing focus cancels");

        assertEquals("Левый Shift", KeyBindField.keyName(340));
        assertEquals("F5", KeyBindField.keyName(294));
        assertEquals("Num 7", KeyBindField.keyName(327));
        assertEquals("Не задано", KeyBindField.keyName(KeyBindField.NONE));
        assertEquals(Keys.R, KeyBindField.parseKey("r"));
        assertEquals(Keys.SPACE, KeyBindField.parseKey("space"));
        assertEquals(Keys.SPACE, KeyBindField.parseKey("Пробел"));
        assertEquals(294, KeyBindField.parseKey("F5"));
        assertEquals(340, KeyBindField.parseKey("left-shift"));
    }

    @Test
    void datePickerOpensCalendarAndPicks() {
        Signal<LocalDate> date = Signal.of(LocalDate.of(2026, 10, 12));
        DatePicker picker = new DatePicker(date).today(LocalDate.of(2026, 10, 12));
        UiRoot root = mount(picker);
        assertEquals("12.10.2026", picker.text());
        assertTrue(drew(root, "text 12.10.2026"));

        click(root, picker);
        assertTrue(picker.isOpen());
        root.tick(1f);
        CalendarView cal = picker.calendar();
        assertEquals(YearMonth.of(2026, 10), cal.month());
        assertEquals(LocalDate.of(2026, 9, 28), cal.dateAt(0), "the grid starts on Monday");
        assertTrue(drew(root, "text Октябрь 2026"));
        // 20 октября: 22-я ячейка от 28 сентября — ряд 3, колонка 1; отступ 6, заголовок 20, дни недели 14
        float x = cal.screenX() + 6 + 22 + 11;
        float y = cal.screenY() + 40 + 3 * 18 + 9;
        click(root, x, y, Keys.MOUSE_LEFT);
        assertEquals(LocalDate.of(2026, 10, 20), date.peek());
        assertFalse(picker.isOpen(), "picking a day closes the calendar");
        assertFalse(picker.hasState(Select.OPEN));

        root.setFocus(picker);
        key(root, Keys.ENTER);
        assertTrue(picker.isOpen());
        root.tick(1f);
        cal = picker.calendar();
        key(root, Keys.RIGHT);
        key(root, Keys.DOWN);
        assertEquals(LocalDate.of(2026, 10, 28), cal.cursor());
        key(root, Keys.PAGE_DOWN);
        assertEquals(YearMonth.of(2026, 11), cal.month(), "PageDown flips the month");
        root.tick(1f);
        click(root, cal.screenX() + 6 + 4, cal.screenY() + 6 + 10, Keys.MOUSE_LEFT);
        assertEquals(YearMonth.of(2026, 10), cal.month(), "the ‹ arrow goes back a month");
        assertTrue(picker.isOpen(), "month navigation keeps it open");
        assertEquals(LocalDate.of(2026, 10, 28), cal.cursor(), "the cursor follows the shown month");
        key(root, Keys.ENTER);
        assertEquals(LocalDate.of(2026, 10, 28), date.peek());
        assertFalse(picker.isOpen());

        click(root, picker);
        key(root, Keys.ESCAPE);
        assertFalse(picker.isOpen());
        assertFalse(picker.hasState(Select.OPEN));
        assertEquals(LocalDate.of(2026, 10, 28), date.peek());
    }

    @Test
    void calendarRespectsRange() {
        Signal<LocalDate> date = Signal.of(LocalDate.of(2026, 10, 12));
        CalendarView cal = new CalendarView(date).range(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 20));
        UiRoot root = mount(cal);
        cal.pick(LocalDate.of(2026, 10, 25));
        assertEquals(LocalDate.of(2026, 10, 12), date.peek(), "outside the range — ignored");
        root.setFocus(cal);
        key(root, Keys.LEFT);
        key(root, Keys.ENTER);
        assertEquals(LocalDate.of(2026, 10, 11), date.peek());
        frame(root);
    }

    @Test
    void formHelpersFromMarkup() {
        Bindings b = new Bindings();
        b.state("key", "R");
        b.state("day", "2026-03-08");
        UiElement ui = Markup.build("""
                <column>
                  <keybind id="k" bind="key" default="space">
                  <date-picker id="d" bind="day" min="2026-01-01">
                  <calendar id="c" bind="day">
                </column>""", b).root();
        KeyBindField k = assertInstanceOf(KeyBindField.class, ui.findById("k"));
        assertEquals(Keys.R, k.key());
        DatePicker d = assertInstanceOf(DatePicker.class, ui.findById("d"));
        assertEquals("08.03.2026", d.text());
        assertInstanceOf(CalendarView.class, ui.findById("c"));
        assertEquals(LocalDate.of(2026, 3, 8), b.get("day").get());
    }

    @Test
    void themesStyleTheNewWidgets() {
        for (Theme t : new Theme[]{Theme.dark(), Theme.light(), io.github.ssbaxys.lumina.style.ModernTheme.dark()}) {
            for (String cls : new String[]{"select", "dropdown", "calendar", "keybind", "tooltip", "tooltip-vanilla", "radial-menu"}) {
                assertNotNull(t.rawStyle(cls), cls);
            }
        }
    }
}
