package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.inventory.Inventory;
import io.github.ssbaxys.lumina.inventory.InventoryPanel;
import io.github.ssbaxys.lumina.inventory.Slot;
import io.github.ssbaxys.lumina.inventory.SlotGrid;
import io.github.ssbaxys.lumina.inventory.SlotGroup;
import io.github.ssbaxys.lumina.inventory.Stack;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.menu.MenuActions;
import io.github.ssbaxys.lumina.menu.MenuChannel;
import io.github.ssbaxys.lumina.menu.MenuSpec;
import io.github.ssbaxys.lumina.menu.MenuValues;
import io.github.ssbaxys.lumina.menu.RemoteMenu;
import io.github.ssbaxys.lumina.menu.SlotClick;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Меню-контейнеры с сервером: описание, кодирование значений и действий, перевод жестов в щелчки. */
class MenuTest {
    static boolean shift;

    static final MenuSpec SPEC = MenuSpec.builder()
            .container("chest", 27)
            .container("result", 1).takeOnly()
            .playerInventory()
            .intValue("progress")
            .boolValue("burning")
            .intValue("big")
            .build();

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

    // ================================================================ описание

    @Test
    void specLaysOutSlotsLikeVanilla() {
        assertEquals(27 + 1 + 36, SPEC.slotCount());
        assertEquals(28, SPEC.containerSize());
        MenuSpec.Section chest = SPEC.section("chest");
        MenuSpec.Section result = SPEC.section("result");
        MenuSpec.Section player = SPEC.playerSection();
        assertSame(player, SPEC.section("player"));
        assertEquals(0, chest.firstSlot());
        assertEquals(27, result.firstSlot());
        assertEquals(27, result.containerOffset());
        assertTrue(result.isTakeOnly());
        assertEquals(28, player.firstSlot());
        assertEquals(-1, player.containerOffset());

        // Рюкзак (ячейки игрока 9..35) идёт первым, хотбар (0..8) — последним, как у ванильного сундука.
        assertEquals(28, player.slot(9));
        assertEquals(28 + 26, player.slot(35));
        assertEquals(28 + 27, player.slot(0));
        assertEquals(63, player.slot(8));
        for (int i = 0; i < 36; i++) {
            assertEquals(i, player.index(player.slot(i)));
        }
        assertEquals(-1, player.index(3));
        assertSame(result, SPEC.sectionOf(27));
        assertSame(null, SPEC.sectionOf(64));
        assertThrows(IllegalArgumentException.class, () -> SPEC.section("nope"));
        assertThrows(IllegalArgumentException.class, () -> MenuSpec.builder().container("a", 1).container("a", 2));
        assertThrows(IllegalStateException.class, () -> MenuSpec.builder().playerInventory().playerInventory());
        assertThrows(IllegalStateException.class, () -> MenuSpec.builder().playerInventory().takeOnly());
    }

    @Test
    void quickMoveRoutesFollowVanilla() {
        assertEquals(List.of(new MenuSpec.Range(28, 64, true)), SPEC.quickMoveRoute(5), "chest → player, from the end");
        assertEquals(List.of(new MenuSpec.Range(28, 64, true)), SPEC.quickMoveRoute(27), "result → player");
        assertEquals(List.of(new MenuSpec.Range(0, 27, false)), SPEC.quickMoveRoute(30),
                "player → chest only: the result section takes nothing");
        assertEquals(List.of(new MenuSpec.Range(0, 27, false)), SPEC.quickMoveRoute(60), "hotbar too");

        MenuSpec playerOnly = MenuSpec.builder().playerInventory().build();
        assertEquals(List.of(new MenuSpec.Range(27, 36, false)), playerOnly.quickMoveRoute(0), "backpack → hotbar");
        assertEquals(List.of(new MenuSpec.Range(0, 27, false)), playerOnly.quickMoveRoute(30), "hotbar → backpack");
        assertEquals(List.of(), playerOnly.quickMoveRoute(99));
    }

    @Test
    void valuesSurviveSixteenBitParts() {
        assertEquals(5, SPEC.dataSlotCount(), "two parts per int, one per flag");
        MenuValues values = new MenuValues(SPEC);
        int[] samples = {0, 1, 42, 65535, 65536, -1, -70000, Integer.MAX_VALUE, Integer.MIN_VALUE, 123456789};
        MenuSpec.Value big = SPEC.value("big");
        for (int sample : samples) {
            values.set(big, sample).set("burning", true);
            int[] raw = new int[SPEC.dataSlotCount()];
            for (int part = 0; part < raw.length; part++) {
                // Игра передаёт части как short: знак расширяется на клиенте.
                raw[part] = (short) SPEC.dataPart(values, part);
            }
            assertEquals(sample, SPEC.decode(big, raw), "value " + sample);
            assertEquals(1, SPEC.decode(SPEC.value("burning"), raw));
        }
        values.set("burning", 7);
        assertEquals(1, values.get("burning"), "flags are stored as 0/1");
    }

    // ================================================================ действия

    @Test
    void actionsTravelAsMenuButtons() {
        int[] small = MenuActions.encode(3, 1);
        assertEquals(2, small.length, "small action and value: two button clicks");
        int[][] cases = {{0, 0}, {3, -64}, {127, 63}, {128, 64}, {5, -1_000_000}, {1_000_000, Integer.MAX_VALUE},
                {Integer.MAX_VALUE, Integer.MIN_VALUE}};
        MenuActions.Decoder decoder = new MenuActions.Decoder();
        for (int[] c : cases) {
            int[] ids = MenuActions.encode(c[0], c[1]);
            int done = 0;
            for (int i = 0; i < ids.length; i++) {
                assertTrue(ids[i] >= -128 && ids[i] <= 127, "fits a signed byte: " + Arrays.toString(ids));
                // Так id проходит через writeByte/readByte пакета 1.20.1.
                if (decoder.accept((byte) ids[i])) {
                    done++;
                    assertEquals(ids.length - 1, i, "completes on the last click");
                }
            }
            assertEquals(1, done);
            assertEquals(c[0], decoder.action());
            assertEquals(c[1], decoder.value());
        }
        assertThrows(IllegalArgumentException.class, () -> MenuActions.encode(-1, 0));

        // Мусор: слишком длинное число и id вне байта сбрасывают сборку, следующее действие собирается.
        for (int i = 0; i < 7; i++) {
            assertFalse(decoder.accept(-1));
        }
        assertFalse(decoder.accept(1000));
        int[] ids = MenuActions.encode(9, 2);
        assertFalse(decoder.accept(ids[0]));
        assertTrue(decoder.accept(ids[1]));
        assertEquals(9, decoder.action());
        assertEquals(2, decoder.value());
    }

    // ================================================================ клиент

    /** Сервер в миниатюре: ванильные правила PICKUP / QUICK_MOVE / SWAP / THROW на массиве стопок. */
    static final class FakeServer implements MenuChannel {
        final Stack[] slots = new Stack[SPEC.slotCount()];
        final List<String> clicks = new ArrayList<>();
        final List<Integer> buttons = new ArrayList<>();
        final List<Stack> dropped = new ArrayList<>();
        Stack cursor = Stack.EMPTY;
        RemoteMenu menu;
        int flushes;
        boolean closed;

        FakeServer() {
            Arrays.fill(slots, Stack.EMPTY);
        }

        @Override
        public void click(int slot, int button, SlotClick type) {
            clicks.add(type + " " + slot + " " + button);
            switch (type) {
                case PICKUP -> pickup(slot, button);
                case QUICK_MOVE -> {
                    Stack s = slots[slot];
                    for (MenuSpec.Range r : SPEC.quickMoveRoute(slot)) {
                        for (int i = r.from(); i < r.to() && !s.isEmpty(); i++) {
                            if (slots[i].isEmpty()) {
                                slots[i] = s;
                                s = Stack.EMPTY;
                            }
                        }
                    }
                    slots[slot] = s;
                }
                case SWAP -> {
                    int hotbar = SPEC.playerSection().slot(button);
                    Stack t = slots[hotbar];
                    slots[hotbar] = slots[slot];
                    slots[slot] = t;
                }
                case THROW -> {
                    Stack s = slots[slot];
                    int n = button == 1 ? s.count() : 1;
                    dropped.add(s.withCount(n));
                    slots[slot] = s.grow(-n);
                }
                default -> {
                }
            }
        }

        private void pickup(int slot, int button) {
            if (slot == SlotClick.OUTSIDE) {
                dropped.add(cursor);
                cursor = Stack.EMPTY;
                return;
            }
            Stack here = slots[slot];
            boolean takeOnly = SPEC.sectionOf(slot).isTakeOnly();
            if (cursor.isEmpty()) {
                int n = button == 0 ? here.count() : (here.count() + 1) / 2;
                cursor = here.withCount(n);
                slots[slot] = here.grow(-n);
            } else if (takeOnly) {
                return;
            } else if (here.isEmpty() || here.canStackWith(cursor)) {
                int room = cursor.maxStackSize() - here.count();
                int n = Math.min(room, button == 0 ? cursor.count() : 1);
                slots[slot] = cursor.withCount(here.count() + n);
                cursor = cursor.grow(-n);
            } else {
                slots[slot] = cursor;
                cursor = here;
            }
        }

        @Override
        public boolean isCarrying() {
            return !cursor.isEmpty();
        }

        @Override
        public void clickButton(int buttonId) {
            buttons.add(buttonId);
        }

        @Override
        public void flush() {
            flushes++;
            sync();
        }

        @Override
        public void close() {
            closed = true;
        }

        /** То, что сделала бы синхронизация меню игры. */
        void sync() {
            for (int i = 0; i < slots.length; i++) {
                menu.setSlot(i, slots[i]);
            }
            menu.setCarried(cursor);
        }

        void put(int slot, Stack stack) {
            slots[slot] = stack;
            sync();
        }
    }

    static final class Scene {
        final FakeServer server = new FakeServer();
        final RemoteMenu menu = new RemoteMenu(SPEC, "Сундук", server);
        final InventoryPanel panel;
        final SlotGrid result;
        final UiRoot root;
        final List<Slot> slots = new ArrayList<>();

        Scene() {
            server.menu = menu;
            panel = InventoryPanel.chest(menu.title(), menu.inventory("chest"), menu.playerInventory(),
                    Slot.Look.MODERN);
            result = new SlotGrid(menu.inventory("result"), 1).look(Slot.Look.MODERN);
            root = new UiRoot(Panel.box(FlexLayout.row().gap(10)).fill().add(panel, result), Theme.dark());
            root.resize(500, 400);
            root.tick(0f);
            collect(panel, slots);
        }

        /** Слот окна: сначала 27 ячеек сундука, затем рюкзак (9..35) и хотбар (0..8). */
        Slot slot(int i) {
            return slots.get(i);
        }
    }

    private static void collect(UiElement e, List<Slot> out) {
        if (e instanceof Slot s) {
            out.add(s);
        }
        for (UiElement c : e.children()) {
            collect(c, out);
        }
    }

    private static float cx(UiElement e) {
        return e.screenX() + e.width() / 2f;
    }

    private static float cy(UiElement e) {
        return e.screenY() + e.height() / 2f;
    }

    private static void click(UiRoot root, UiElement at, int button) {
        root.pointerMove(cx(at), cy(at));
        root.pointerDown(cx(at), cy(at), button);
        root.pointerUp(cx(at), cy(at), button);
    }

    private static void drag(UiRoot root, UiElement from, float x, float y, int button) {
        root.pointerMove(cx(from), cy(from));
        root.pointerDown(cx(from), cy(from), button);
        root.pointerMove(cx(from) + 5f, cy(from));
        root.pointerMove(x, y);
        root.pointerUp(x, y, button);
    }

    private static void drag(UiRoot root, UiElement from, UiElement to, int button) {
        drag(root, from, cx(to), cy(to), button);
    }

    @Test
    void sectionsAreRemoteInventories() {
        Scene s = new Scene();
        Inventory chest = s.menu.inventory("chest");
        assertSame(s.menu, chest.remote());
        assertEquals(27, chest.size());
        assertEquals(36, s.menu.playerInventory().size());
        assertEquals(5, s.menu.menuSlot(chest, 5));
        assertEquals(55, s.menu.menuSlot(s.menu.playerInventory(), 0), "hotbar 0 is slot 28 + 27");
        assertEquals(-1, s.menu.menuSlot(new Inventory(3), 0));

        s.server.put(2, Stack.of("minecraft:diamond", 5));
        assertEquals(Stack.of("minecraft:diamond", 5), chest.get(2), "contents come from the server");
        assertEquals(5, s.slot(2).stack().count());
    }

    @Test
    void dragBecomesPickupClicks() {
        Scene s = new Scene();
        s.server.put(0, Stack.of("minecraft:diamond", 12));
        Slot from = s.slot(0);
        Slot to = s.slot(27 + 3);   // ячейка игрока 12 → слот меню 28 + 3

        drag(s.root, from, to, Keys.MOUSE_LEFT);
        assertEquals(List.of("PICKUP 0 0", "PICKUP 31 0"), s.server.clicks);
        assertTrue(from.stack().isEmpty(), "the view follows the server");
        assertEquals(12, s.menu.playerInventory().get(12).count());
        assertEquals(1, s.server.flushes);

        // ПКМ — половина; остаток не вмещается — третий щелчок возвращает его в источник.
        s.server.clicks.clear();
        s.server.put(31, Stack.of("minecraft:stone", 40));
        s.server.put(1, Stack.of("minecraft:stone", 60));
        drag(s.root, s.slot(27 + 3), s.slot(1), Keys.MOUSE_RIGHT);
        assertEquals(List.of("PICKUP 31 1", "PICKUP 1 0", "PICKUP 31 0"), s.server.clicks);
        assertEquals(64, s.menu.inventory("chest").get(1).count());
        assertEquals(36, s.menu.playerInventory().get(12).count());
        assertFalse(s.server.isCarrying(), "nothing is left on the cursor");
    }

    @Test
    void swapAndRulesUseTheVisibleState() {
        Scene s = new Scene();
        s.server.put(0, Stack.of("minecraft:diamond", 3));
        s.server.put(1, Stack.of("minecraft:emerald", 5));
        drag(s.root, s.slot(0), s.slot(1), Keys.MOUSE_RIGHT);
        assertEquals(List.of(), s.server.clicks, "half a stack does not swap: no request at all");
        drag(s.root, s.slot(0), s.slot(1), Keys.MOUSE_LEFT);
        assertEquals(List.of("PICKUP 0 0", "PICKUP 1 0", "PICKUP 0 0"), s.server.clicks, "swap = three clicks");
        assertEquals("minecraft:emerald", s.menu.inventory("chest").get(0).itemId());
        assertEquals("minecraft:diamond", s.menu.inventory("chest").get(1).itemId());

        // В раздел «только брать» положить нельзя — даже подсветка это знает.
        s.server.clicks.clear();
        Slot result = s.result.slot(0);
        s.root.pointerMove(cx(s.slot(0)), cy(s.slot(0)));
        s.root.pointerDown(cx(s.slot(0)), cy(s.slot(0)), Keys.MOUSE_LEFT);
        s.root.pointerMove(cx(s.slot(0)) + 6f, cy(s.slot(0)));
        assertFalse(SlotGroup.shared().wouldAccept(result));
        s.root.pointerMove(cx(result), cy(result));
        s.root.pointerUp(cx(result), cy(result), Keys.MOUSE_LEFT);
        assertEquals(List.of(), s.server.clicks);

        // С локальным инвентарём удалённый не смешивается.
        Inventory local = new Inventory(1);
        Slot localSlot = new Slot(local, 0);
        s.panel.add(localSlot);
        s.root.tick(0f);
        drag(s.root, s.slot(0), localSlot, Keys.MOUSE_LEFT);
        assertTrue(local.get(0).isEmpty());
        assertEquals(List.of(), s.server.clicks);
    }

    @Test
    void shiftClickDropAndKeysAreServerRequests() {
        Scene s = new Scene();
        s.server.put(4, Stack.of("minecraft:apple", 10));
        shift = true;
        click(s.root, s.slot(4), Keys.MOUSE_LEFT);
        shift = false;
        assertEquals(List.of("QUICK_MOVE 4 0"), s.server.clicks, "the server decides where it goes");
        assertTrue(s.menu.inventory("chest").get(4).isEmpty());

        // Мимо слотов без обработчика — отмена, с обработчиком — выброс через сервер.
        s.server.clicks.clear();
        s.server.put(4, Stack.of("minecraft:apple", 10));
        drag(s.root, s.slot(4), 490f, 390f, Keys.MOUSE_LEFT);
        assertEquals(List.of(), s.server.clicks);
        SlotGroup.shared().onDropOutside((from, stack) -> true);
        try {
            drag(s.root, s.slot(4), 490f, 390f, Keys.MOUSE_RIGHT);
        } finally {
            SlotGroup.shared().onDropOutside(null);
        }
        assertEquals(List.of("PICKUP 4 1", "PICKUP -999 0"), s.server.clicks);
        assertEquals(List.of(Stack.of("minecraft:apple", 5)), s.server.dropped);
        assertEquals(5, s.menu.inventory("chest").get(4).count());

        // Клавиши над слотом: 1–9 — обмен с хотбаром, Q — выбросить.
        s.server.clicks.clear();
        s.root.pointerMove(cx(s.slot(4)), cy(s.slot(4)));
        assertTrue(s.menu.swapHovered(s.root.hovered(), 2));
        assertEquals("minecraft:apple", s.menu.playerInventory().get(2).itemId());
        assertTrue(s.menu.inventory("chest").get(4).isEmpty());
        assertFalse(s.menu.throwHovered(s.root.hovered(), false), "empty slot: nothing to throw");
        assertFalse(s.menu.swapHovered(s.root.hovered(), 12), "not a hotbar slot");
        Slot hotbar2 = s.slot(27 + 27 + 2);
        s.root.pointerMove(cx(hotbar2), cy(hotbar2));
        assertTrue(s.menu.throwHovered(s.root.hovered(), true));
        assertEquals(List.of("SWAP 4 2", "THROW 57 1"), s.server.clicks);
        assertTrue(s.menu.playerInventory().get(2).isEmpty());
    }

    @Test
    void cursorLeftByTheServerIsPlacedByAClick() {
        Scene s = new Scene();
        s.server.cursor = Stack.of("minecraft:coal", 8);
        s.server.sync();
        assertEquals(8, s.menu.carried().get().count());
        click(s.root, s.slot(6), Keys.MOUSE_LEFT);
        assertEquals(List.of("PICKUP 6 0"), s.server.clicks);
        assertEquals(8, s.menu.inventory("chest").get(6).count());
        assertTrue(s.menu.carried().get().isEmpty());
    }

    @Test
    void syncedValuesAreSignals() {
        Scene s = new Scene();
        MenuValues server = new MenuValues(SPEC).set("progress", 70_000).set("burning", true).set("big", -5);
        List<Integer> seen = new ArrayList<>();
        s.menu.intValue("progress").subscribe(seen::add);
        assertEquals(0, s.menu.intValue("progress").get());

        for (int part = 0; part < SPEC.dataSlotCount(); part++) {
            s.menu.setData(part, (short) SPEC.dataPart(server, part));
        }
        assertEquals(0, s.menu.intValue("progress").get(), "nothing changes until publish");
        s.menu.publish();
        assertEquals(70_000, s.menu.intValue("progress").get());
        assertTrue(s.menu.boolValue("burning").get());
        assertEquals(-5, s.menu.intValue("big").get());
        assertEquals(List.of(70_000), seen, "one notification for both halves");
        s.menu.publish();
        assertEquals(1, seen.size());
        assertThrows(IllegalArgumentException.class, () -> s.menu.intValue("burning"));
        assertThrows(IllegalArgumentException.class, () -> s.menu.boolValue("progress"));

        s.menu.sendAction(2, 1);
        assertEquals(Arrays.stream(MenuActions.encode(2, 1)).boxed().toList(), s.server.buttons);
        s.menu.close();
        assertTrue(s.server.closed);
    }

    @Test
    void platformStacksAreDrawnByTheBackend() {
        Object handle = new Object();
        Stack potion = Stack.of("minecraft:potion").withPlatformStack(handle);
        assertSame(handle, potion.platformStack());
        assertEquals(Stack.of("minecraft:potion"), potion, "the handle is not part of equality");
        assertSame(handle, potion.withCount(3).platformStack(), "count does not change the looks");
        assertSame(null, potion.withName("Зелье").platformStack(), "other changes drop it");

        List<Object> drawn = new ArrayList<>();
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas() {
            @Override
            protected void drawPlatformItemImpl(Object platformStack, String itemId, float x, float y, float size,
                                                boolean glint) {
                drawn.add(platformStack);
            }
        };
        Scene s = new Scene();
        s.server.put(0, potion);
        s.server.put(1, Stack.of("minecraft:stone"));
        canvas.begin(500, 400);
        s.root.render(canvas, 0f);
        canvas.end();
        assertEquals(List.of(handle), drawn);
        assertTrue(canvas.ops.contains("item minecraft:stone"), "plain stacks still go by id");
    }

    @Test
    void localInventoriesKeepTheirOwnRules() {
        Inventory a = new Inventory(9);
        Inventory b = new Inventory(9);
        assertSame(null, a.remote());
        SlotGrid ga = new SlotGrid(a, 9).quickMoveTo(b);
        SlotGrid gb = new SlotGrid(b, 9);
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column().gap(10)).fill().add(ga, gb), Theme.dark());
        root.resize(300, 200);
        root.tick(0f);
        a.set(0, Stack.of("minecraft:diamond", 4));
        drag(root, ga.slot(0), gb.slot(2), Keys.MOUSE_LEFT);
        assertEquals(4, b.get(2).count(), "no remote: the model changes right away");
        shift = true;
        click(root, gb.slot(2), Keys.MOUSE_LEFT);
        shift = false;
        assertEquals(4, b.get(2).count(), "grid b has no quick-move targets");
        b.set(2, Stack.EMPTY);
        a.set(1, Stack.of("minecraft:coal", 3));
        shift = true;
        click(root, ga.slot(1), Keys.MOUSE_LEFT);
        shift = false;
        assertEquals(3, b.get(0).count());
    }
}
