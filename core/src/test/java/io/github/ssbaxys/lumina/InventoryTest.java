package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.inventory.Inventory;
import io.github.ssbaxys.lumina.inventory.InventoryPanel;
import io.github.ssbaxys.lumina.inventory.ItemTooltip;
import io.github.ssbaxys.lumina.inventory.Slot;
import io.github.ssbaxys.lumina.inventory.SlotGrid;
import io.github.ssbaxys.lumina.inventory.SlotGroup;
import io.github.ssbaxys.lumina.inventory.Stack;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Tooltips;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Стопки, модель инвентаря, слоты и перетаскивание мышью. */
class InventoryTest {
    @org.junit.jupiter.api.BeforeEach
    void russianTexts() {
        io.github.ssbaxys.lumina.i18n.I18n.setLanguage("ru_ru");
    }

    static boolean shift;

    @BeforeEach
    void init() {
        shift = false;
        Lumina.init(new Platform() {
            @Override
            public TextMetrics textMetrics() {
                return TestSupport.METRICS;
            }

            @Override
            public String getClipboard() {
                return "";
            }

            @Override
            public void setClipboard(String text) {
            }

            @Override
            public void playClickSound() {
            }

            @Override
            public boolean isShiftDown() {
                return shift;
            }
        });
    }

    @AfterEach
    void cleanup() {
        SlotGroup.shared().cancel();
    }

    // ================================================================ стопки и инвентарь

    @Test
    void stacksParseAndStack() {
        Stack d = Stack.of("diamond", 12);
        assertEquals("minecraft:diamond", d.itemId());
        assertEquals(12, d.count());
        assertEquals("Diamond", d.displayName());
        assertEquals(64, Stack.parse("minecraft:stone*64").count());
        assertEquals(12, Stack.parse("minecraft:coal x12").count());
        assertEquals("minecraft:oak_log", Stack.parse("oak_log").itemId());
        assertTrue(Stack.parse("  ").isEmpty());
        assertTrue(Stack.of("minecraft:dirt", 0).isEmpty());

        assertTrue(d.canStackWith(Stack.of("minecraft:diamond", 3)));
        assertFalse(d.canStackWith(d.withGlint(true)));
        assertFalse(d.canStackWith(d.withName("Камень")));
        assertFalse(d.canStackWith(Stack.EMPTY));
        assertEquals(d, Stack.of("minecraft:diamond", 12));

        Stack sword = Stack.of("minecraft:diamond_sword").withDurability(390, 1561);
        assertEquals("Diamond Sword", sword.displayName());
        assertEquals(1, sword.maxStackSize());
        assertTrue(sword.isDamaged());
        assertEquals(0.75f, sword.durability(), 0.01f);
        assertEquals(16, Stack.of("minecraft:ender_pearl").withMaxStackSize(16).maxStackSize());
        assertTrue(d.withCount(0).isEmpty());
        assertEquals(7, d.grow(-5).count());
    }

    @Test
    void inventoryInsertsMergesThenFillsAndNotifies() {
        Inventory inv = new Inventory(4);
        inv.set(1, Stack.of("minecraft:stone", 60));
        inv.set(2, Stack.of("minecraft:dirt", 5));
        List<String> changes = new ArrayList<>();
        var sub = inv.onChange((i, index, before, after) -> changes.add(index + ":" + after.count()));
        Stack left = inv.insert(Stack.of("minecraft:stone", 100));
        assertEquals(64, inv.get(1).count(), "merged into the matching stack first");
        assertEquals(64, inv.get(0).count());
        assertEquals(32, inv.get(3).count());
        assertTrue(left.isEmpty());
        assertEquals(List.of("1:64", "0:64", "3:32"), changes);
        assertEquals(160, inv.count("minecraft:stone"));

        Stack rest = inv.insert(Stack.of("minecraft:stone", 40));
        assertEquals(8, rest.count(), "only 32 fit into slot 3");
        sub.close();
        inv.set(3, Stack.EMPTY);
        assertEquals(4, changes.size(), "unsubscribed");

        Inventory armor = new Inventory(2).filter((index, s) -> s.itemId().endsWith("_helmet"));
        assertFalse(armor.accepts(0, Stack.of("minecraft:stone")));
        assertEquals(1, armor.insert(Stack.of("minecraft:stone")).count());
        assertTrue(armor.insert(Stack.of("minecraft:iron_helmet")).isEmpty());
        assertEquals(16, new Inventory(1).limit(16).maxStack(0, Stack.of("minecraft:stone")));
    }

    // ================================================================ перетаскивание

    /** Две сетки 9×1 в корне. */
    private static final class Scene {
        final Inventory a = new Inventory(9);
        final Inventory b = new Inventory(9);
        final SlotGroup group = new SlotGroup();
        final SlotGrid gridA;
        final SlotGrid gridB;
        final UiRoot root;

        Scene() {
            gridA = new SlotGrid(a, 9).group(group).quickMoveTo(b);
            gridB = new SlotGrid(b, 9).group(group).quickMoveTo(a);
            root = new UiRoot(Panel.box(FlexLayout.column().gap(10)).fill().add(gridA, gridB), Theme.dark());
            root.resize(400, 300);
            root.tick(0f);
        }

        Slot a(int i) {
            return gridA.slot(i);
        }

        Slot b(int i) {
            return gridB.slot(i);
        }
    }

    private static float cx(UiElement e) {
        return e.screenX() + e.width() / 2f;
    }

    private static float cy(UiElement e) {
        return e.screenY() + e.height() / 2f;
    }

    private static void press(UiRoot root, UiElement from, int button) {
        root.pointerMove(cx(from), cy(from));
        root.pointerDown(cx(from), cy(from), button);
    }

    private static void drag(UiRoot root, UiElement from, UiElement to, int button) {
        press(root, from, button);
        root.pointerMove(cx(from) + 5f, cy(from));
        root.pointerMove(cx(to), cy(to));
        root.pointerUp(cx(to), cy(to), button);
    }

    @Test
    void dragMovesWholeStackWithGhost() {
        Scene s = new Scene();
        s.a.set(0, Stack.of("minecraft:diamond", 12));
        assertEquals(18f, s.a(0).width());
        assertEquals(9 * 18f, s.gridA.width(), "vanilla grid has no gaps");

        press(s.root, s.a(0), Keys.MOUSE_LEFT);
        s.root.pointerMove(cx(s.a(0)) + 1f, cy(s.a(0)));
        assertFalse(s.group.isDragging(), "below the drag threshold");
        s.root.pointerMove(cx(s.a(0)) + 6f, cy(s.a(0)));
        assertTrue(s.group.isDragging());
        assertSame(s.a(0), s.group.source());
        assertEquals(12, s.group.carried().count());
        assertNotNull(s.group.ghost());
        assertTrue(s.root.overlays().contains(s.group.ghost()));
        assertTrue(s.a(0).displayedStack().isEmpty(), "source shows the remainder");
        assertEquals(12, s.a.get(0).count(), "model untouched until the drop");

        s.root.pointerMove(cx(s.b(3)), cy(s.b(3)));
        UiElement ghost = s.group.ghost();
        assertEquals(cx(s.b(3)) - 8f, ghost.x(), 0.01f, "ghost follows the cursor");
        assertTrue(s.group.wouldAccept(s.b(3)));
        s.root.pointerUp(cx(s.b(3)), cy(s.b(3)), Keys.MOUSE_LEFT);

        assertFalse(s.group.isDragging());
        assertNull(s.group.ghost());
        assertTrue(s.a.get(0).isEmpty());
        assertEquals(Stack.of("minecraft:diamond", 12), s.b.get(3));
    }

    @Test
    void rightDragTakesHalf() {
        Scene s = new Scene();
        s.a.set(0, Stack.of("minecraft:coal", 13));
        drag(s.root, s.a(0), s.b(0), Keys.MOUSE_RIGHT);
        assertEquals(7, s.b.get(0).count());
        assertEquals(6, s.a.get(0).count());
    }

    @Test
    void mergeKeepsOverflowInSource() {
        Scene s = new Scene();
        s.a.set(0, Stack.of("minecraft:stone", 40));
        s.b.set(0, Stack.of("minecraft:stone", 50));
        List<String> moved = new ArrayList<>();
        s.group.onTransfer((from, to, stack) -> moved.add(stack.count() + (to == null ? " away" : " to " + to.index())));
        drag(s.root, s.a(0), s.b(0), Keys.MOUSE_LEFT);
        assertEquals(64, s.b.get(0).count());
        assertEquals(26, s.a.get(0).count());
        assertEquals(List.of("14 to 0"), moved);

        // Полная стопка ничего не принимает: перенос отменяется.
        drag(s.root, s.a(0), s.b(0), Keys.MOUSE_LEFT);
        assertEquals(26, s.a.get(0).count());
        assertEquals(1, moved.size());
    }

    @Test
    void differentItemsSwapOnlyWithWholeStack() {
        Scene s = new Scene();
        s.a.set(0, Stack.of("minecraft:diamond", 3));
        s.b.set(0, Stack.of("minecraft:emerald", 5));
        drag(s.root, s.a(0), s.b(0), Keys.MOUSE_RIGHT);
        assertEquals("minecraft:diamond", s.a.get(0).itemId(), "half a stack does not swap");
        assertEquals(3, s.a.get(0).count());
        drag(s.root, s.a(0), s.b(0), Keys.MOUSE_LEFT);
        assertEquals(Stack.of("minecraft:emerald", 5), s.a.get(0));
        assertEquals(Stack.of("minecraft:diamond", 3), s.b.get(0));
    }

    @Test
    void validatorsAndFiltersRejectDrops() {
        Scene s = new Scene();
        s.a.set(0, Stack.of("minecraft:diamond", 3));
        s.group.validator((from, to, stack) -> to.inventory() != s.b || to.index() != 1);
        s.b.filter((index, stack) -> index != 2);

        press(s.root, s.a(0), Keys.MOUSE_LEFT);
        s.root.pointerMove(cx(s.a(0)) + 6f, cy(s.a(0)));
        assertFalse(s.group.wouldAccept(s.b(1)), "validator");
        assertFalse(s.group.wouldAccept(s.b(2)), "inventory filter");
        assertTrue(s.group.wouldAccept(s.b(3)));
        s.root.pointerMove(cx(s.b(1)), cy(s.b(1)));
        s.root.pointerUp(cx(s.b(1)), cy(s.b(1)), Keys.MOUSE_LEFT);
        assertEquals(3, s.a.get(0).count(), "rejected drop returns the stack");
        assertTrue(s.b.get(1).isEmpty());

        drag(s.root, s.a(0), s.b(2), Keys.MOUSE_LEFT);
        assertTrue(s.b.get(2).isEmpty());
        s.a(0).takeable(false);
        drag(s.root, s.a(0), s.b(3), Keys.MOUSE_LEFT);
        assertTrue(s.b.get(3).isEmpty(), "not takeable");
        s.a(0).takeable(true);
        s.b(3).dropTarget(false);
        drag(s.root, s.a(0), s.b(3), Keys.MOUSE_LEFT);
        assertTrue(s.b.get(3).isEmpty(), "not a drop target");
        assertEquals(3, s.a.get(0).count());
    }

    @Test
    void dropOutsideCancelsOrCallsHandler() {
        Scene s = new Scene();
        s.a.set(0, Stack.of("minecraft:apple", 10));
        press(s.root, s.a(0), Keys.MOUSE_LEFT);
        s.root.pointerMove(300f, 250f);
        s.root.pointerUp(300f, 250f, Keys.MOUSE_LEFT);
        assertEquals(10, s.a.get(0).count(), "no handler: back to the source");

        List<Stack> thrown = new ArrayList<>();
        s.group.onDropOutside((from, stack) -> thrown.add(stack));
        press(s.root, s.a(0), Keys.MOUSE_RIGHT);
        s.root.pointerMove(300f, 250f);
        s.root.pointerUp(300f, 250f, Keys.MOUSE_RIGHT);
        assertEquals(List.of(Stack.of("minecraft:apple", 5)), thrown);
        assertEquals(5, s.a.get(0).count());
    }

    @Test
    void sourceChangedDuringDragCancels() {
        Scene s = new Scene();
        s.a.set(0, Stack.of("minecraft:apple", 10));
        press(s.root, s.a(0), Keys.MOUSE_LEFT);
        s.root.pointerMove(cx(s.a(0)) + 6f, cy(s.a(0)));
        s.a.set(0, Stack.of("minecraft:apple", 2));
        s.root.pointerMove(cx(s.b(0)), cy(s.b(0)));
        s.root.pointerUp(cx(s.b(0)), cy(s.b(0)), Keys.MOUSE_LEFT);
        assertTrue(s.b.get(0).isEmpty());
        assertEquals(2, s.a.get(0).count());
    }

    @Test
    void shiftClickQuickMoves() {
        Scene s = new Scene();
        s.a.set(4, Stack.of("minecraft:stone", 30));
        s.b.set(5, Stack.of("minecraft:stone", 60));
        shift = true;
        press(s.root, s.a(4), Keys.MOUSE_LEFT);
        s.root.pointerUp(cx(s.a(4)), cy(s.a(4)), Keys.MOUSE_LEFT);
        assertTrue(s.a.get(4).isEmpty());
        assertEquals(64, s.b.get(5).count(), "merged first");
        assertEquals(26, s.b.get(0).count(), "then the first empty slot");
        assertFalse(s.group.isDragging());

        List<Slot> handled = new ArrayList<>();
        s.group.onQuickMove((slot, stack) -> handled.add(slot));
        press(s.root, s.b(0), Keys.MOUSE_LEFT);
        s.root.pointerUp(cx(s.b(0)), cy(s.b(0)), Keys.MOUSE_LEFT);
        assertEquals(List.of(s.b(0)), handled);
        assertEquals(26, s.b.get(0).count(), "custom handler took over");
    }

    @Test
    void tooltipsFollowContents() {
        Scene s = new Scene();
        s.a.set(0, Stack.of("minecraft:diamond_sword").withName("Экскалибур").withLore("Древний").withGlint(true)
                .withDurability(100, 1561));
        assertNotNull(Tooltips.of(s.a(0)));
        assertNull(Tooltips.of(s.a(1)), "empty slot has no tooltip");

        ItemTooltip card = new ItemTooltip(s.a.get(0));
        assertEquals(List.of("Экскалибур", "Древний", "Прочность: 1461 / 1561"), card.lines());

        press(s.root, s.a(0), Keys.MOUSE_LEFT);
        s.root.pointerMove(cx(s.a(0)) + 6f, cy(s.a(0)));
        assertNull(Tooltips.of(s.a(0)), "no tooltips while carrying");
        s.root.pointerMove(cx(s.b(0)), cy(s.b(0)));
        s.root.pointerUp(cx(s.b(0)), cy(s.b(0)), Keys.MOUSE_LEFT);
        assertNull(Tooltips.of(s.a(0)));
        assertNotNull(Tooltips.of(s.b(0)));
    }

    @Test
    void slotsPaintItemsCountsAndDurability() {
        Scene s = new Scene();
        s.a.set(0, Stack.of("minecraft:diamond", 12));
        s.a.set(1, Stack.of("minecraft:iron_pickaxe").withDurability(125, 250));
        s.a.set(2, Stack.of("minecraft:stone", 1500).withMaxStackSize(99));
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(400, 300);
        s.root.render(canvas, 0f);
        canvas.end();
        assertTrue(canvas.ops.contains("item minecraft:diamond"));
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("text 12 ")), canvas.ops.toString());
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("text 1.5k ")), "large counts are shortened");
        assertTrue(canvas.ops.contains("item minecraft:iron_pickaxe"));
        int itemAt = canvas.ops.indexOf("item minecraft:diamond");
        int textAt = canvas.ops.indexOf(canvas.ops.stream().filter(o -> o.startsWith("text 12 ")).findFirst().orElseThrow());
        assertTrue(textAt > itemAt, "count is drawn over the item");
    }

    @Test
    void inventoryPanelBuildsChestLayout() {
        Inventory chest = new Inventory(27);
        Inventory player = new Inventory(36);
        InventoryPanel panel = InventoryPanel.chest("Сундук", chest, player, Slot.Look.VANILLA);
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).fill().add(panel), Theme.dark());
        root.resize(400, 300);
        root.tick(0f);
        List<Slot> slots = new ArrayList<>();
        collect(panel, slots);
        assertEquals(27 + 36, slots.size());
        assertEquals(9 * 18f + 14f, panel.measuredWidth(), "vanilla padding 7 on both sides");

        chest.set(0, Stack.of("minecraft:gold_ingot", 20));
        shift = true;
        Slot first = slots.get(0);
        press(root, first, Keys.MOUSE_LEFT);
        root.pointerUp(cx(first), cy(first), Keys.MOUSE_LEFT);
        assertEquals(20, player.get(9).count(), "chest → backpack first");
        assertThrows(IllegalArgumentException.class, () -> InventoryPanel.player(new Inventory(9), Slot.Look.MODERN));
    }

    private static void collect(UiElement e, List<Slot> out) {
        if (e instanceof Slot s) {
            out.add(s);
        }
        for (UiElement c : e.children()) {
            collect(c, out);
        }
    }
}
