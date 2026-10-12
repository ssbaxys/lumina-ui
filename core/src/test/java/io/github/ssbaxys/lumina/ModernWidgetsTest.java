package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.style.ModernTheme;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.widget.Collapsible;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.SideNav;
import io.github.ssbaxys.lumina.widget.Tabs;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ModernWidgetsTest {
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

    @Test
    void tabsSelectByClickAndKeys() {
        Signal<Integer> tab = Signal.of(0);
        Tabs tabs = new Tabs(tab, "Screen", "View", "Font").stretch(true).width(Dim.px(300));
        UiRoot root = mount(tabs);
        // третья треть ширины — вкладка "Font"
        click(root, tabs.screenX() + 250, tabs.screenY() + tabs.height() / 2f);
        assertEquals(2, tab.peek());
        root.setFocus(tabs);
        root.keyDown(Keys.RIGHT, 0, 0);
        assertEquals(0, tab.peek(), "wraps around");
        root.tick(1f);
    }

    @Test
    void sideNavSelectsItem() {
        Signal<Integer> page = Signal.of(0);
        SideNav nav = new SideNav(page).item("Home", "minecraft:oak_door").item("Music", "minecraft:jukebox");
        UiRoot root = mount(nav);
        click(root, nav.screenX() + nav.width() / 2f, nav.screenY() + nav.padding().top() + 44f + 10f);
        assertEquals(1, page.peek());
    }

    @Test
    void collapsibleAnimatesHeight() {
        Panel row = Panel.box(FlexLayout.column()).height(Dim.px(60));
        Collapsible section = new Collapsible("Border Style", row);
        UiRoot root = mount(section);
        float open = section.height();
        assertTrue(open > 80f, "header + body: " + open);
        click(root, section.screenX() + 20, section.screenY() + 10);
        assertFalse(section.open().peek());
        root.tick(0.05f);
        float mid = section.height();
        root.tick(1f);
        root.tick(1f);
        float closed = section.height();
        assertTrue(mid < open && mid > closed, open + " > " + mid + " > " + closed);
        assertFalse(section.body().isVisible(), "collapsed body leaves focus order");
        section.toggle();
        root.tick(1f);
        root.tick(1f);
        assertEquals(open, section.height(), 0.5f);
    }

    @Test
    void modernThemeSetsTokens() {
        assertEquals(ModernTheme.DEFAULT_ACCENT, ModernTheme.dark().get(Tokens.ACCENT));
        assertEquals(0xFF8AB4F8, ModernTheme.dark(0xFF8AB4F8).get(Tokens.ACCENT));
        assertEquals(9f, ModernTheme.light().get(Tokens.CORNER), 1e-6);
    }
}
