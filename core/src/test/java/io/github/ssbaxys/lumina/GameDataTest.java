package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.game.GameData;
import io.github.ssbaxys.lumina.game.GameState;
import io.github.ssbaxys.lumina.game.Inventory;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Label;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Живые данные игры: опрос источника, сигналы только на изменения, инвентарь, разметка. */
class GameDataTest {
    /** Поддельный игрок: поля меняет тест, источник копирует их в снимок. */
    static final class FakePlayer implements GameData.Source {
        boolean inWorld = true;
        float health = 20f;
        int food = 20;
        double x = 0.5;
        double y = 64;
        double z = -3.2;
        int dayTime = 6000;
        String biome = "minecraft:plains";
        int selected;
        final String[] items = new String[GameState.SLOTS];
        final int[] counts = new int[GameState.SLOTS];
        int polls;

        @Override
        public boolean poll(GameState out) {
            polls++;
            out.health = health;
            out.maxHealth = 20f;
            out.food = food;
            out.saturation = 3.5f;
            out.armor = 7;
            out.air = 120;
            out.xpLevel = 5;
            out.xpProgress = 0.25f;
            out.x = x;
            out.y = y;
            out.z = z;
            out.yaw = 90f;
            out.facing = "west";
            out.biome = biome;
            out.dimension = "minecraft:overworld";
            out.dayTime = dayTime;
            out.fps = 144;
            out.selectedSlot = selected;
            for (int i = 0; i < GameState.SLOTS; i++) {
                out.setSlot(i, items[i], counts[i]);
            }
            return inWorld;
        }
    }

    @BeforeEach
    void init() {
        TestSupport.init();
        GameData.reset();
    }

    @AfterEach
    void cleanup() {
        GameData.reset();
    }

    @Test
    void defaultsWithoutWorld() {
        GameData.update();
        assertFalse(GameData.inWorld().get());
        assertEquals(20f, GameData.health().get());
        assertEquals(20, GameData.food().get());
        assertEquals(1f, GameData.healthFraction().get());
        assertEquals(300, GameData.air().get());
        assertEquals("", GameData.biome().get());
        assertEquals(0, GameData.inventory().get().size());
    }

    @Test
    void sourceFillsSignalsAndOnlyChangesNotify() {
        FakePlayer p = new FakePlayer();
        p.items[0] = "minecraft:diamond_sword";
        p.counts[0] = 1;
        p.items[3] = "minecraft:torch";
        p.counts[3] = 32;
        p.items[12] = "minecraft:torch";
        p.counts[12] = 10;
        GameData.setSource(p);

        List<Float> healthChanges = new ArrayList<>();
        List<String> positions = new ArrayList<>();
        List<Inventory> inventories = new ArrayList<>();
        GameData.health().subscribe(healthChanges::add);
        GameData.position().subscribe(positions::add);
        GameData.inventory().subscribe(inventories::add);

        GameData.update();
        assertTrue(GameData.inWorld().get());
        assertEquals(20f, GameData.health().get());
        assertEquals(7, GameData.armor().get());
        assertEquals(120, GameData.air().get());
        assertEquals(0.4f, GameData.airFraction().get(), 1e-6f);
        assertEquals(5, GameData.xpLevel().get());
        assertEquals("west", GameData.facing().get());
        assertEquals("minecraft:plains", GameData.biome().get());
        assertEquals("minecraft:overworld", GameData.dimension().get());
        assertEquals(144, GameData.fps().get());
        assertEquals("0 64 -4", GameData.position().get());
        assertEquals(-3.2, GameData.z().get(), 1e-9);
        assertEquals(-4, GameData.blockZ().get());
        assertEquals("12:00", GameData.clock().get());
        assertEquals("minecraft:diamond_sword", GameData.selectedItem().get());
        assertEquals(1, GameData.selectedCount().get());
        Inventory inv = GameData.inventory().get();
        assertEquals(GameState.SLOTS, inv.size());
        assertEquals(42, inv.total("minecraft:torch"));
        assertEquals(3, inv.usedSlots());
        assertTrue(inv.isEmpty(1));
        assertEquals(new Inventory.Slot("minecraft:torch", 32), inv.slot(3));
        assertEquals(1, inventories.size());

        // Ничего не изменилось — никаких уведомлений, тот же снимок инвентаря.
        GameData.update();
        assertEquals(List.of(), healthChanges);
        assertEquals(1, positions.size());
        assertSame(inv, GameData.inventory().get());
        assertEquals(1, inventories.size());

        // Урон, шаг внутри блока и смена слота.
        p.health = 13.5f;
        p.x = 0.9;
        p.selected = 3;
        GameData.update();
        assertEquals(List.of(13.5f), healthChanges);
        assertEquals(1, positions.size(), "same block — no new position string");
        assertEquals(0.675f, GameData.healthFraction().get(), 1e-6f);
        assertEquals("minecraft:torch", GameData.selectedItem().get());
        assertEquals(32, GameData.selectedCount().get());

        // Изменился инвентарь — новый снимок.
        p.counts[3] = 31;
        p.x = 1.2;
        GameData.update();
        assertNotSame(inv, GameData.inventory().get());
        assertEquals(41, GameData.inventory().get().total("minecraft:torch"));
        assertEquals(2, inventories.size());
        assertEquals("1 64 -4", GameData.position().get());

        // Вышли из мира — значения по умолчанию.
        p.inWorld = false;
        GameData.update();
        assertFalse(GameData.inWorld().get());
        assertEquals(20f, GameData.health().get());
        assertEquals(0, GameData.inventory().get().size());
        assertEquals(5, p.polls);
    }

    @Test
    void platformHookIsTheDefaultSource() {
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
            public boolean pollGameData(GameState out) {
                out.food = 6;
                out.dimension = "minecraft:the_nether";
                return true;
            }
        });
        GameData.update();
        assertTrue(GameData.inWorld().get());
        assertEquals(6, GameData.food().get());
        assertEquals(0.3f, GameData.foodFraction().get(), 1e-6f);
        assertEquals("minecraft:the_nether", GameData.dimension().get());
    }

    @Test
    void markupBindsGameData() {
        FakePlayer p = new FakePlayer();
        GameData.setSource(p);
        GameData.update();
        Markup.Built built = Markup.build("<label id='l'>{{health}} ❤ {{food}} · {{pos}} · {{time}} · {{biome}}</label>",
                Bindings.gameData());
        UiElement ui = built.root();
        UiRoot root = new UiRoot(ui, Theme.dark());
        root.resize(400, 100);
        Label label = (Label) ui.findById("l");
        assertEquals("20 ❤ 20 · 0 64 -4 · 12:00 · minecraft:plains", label.text());
        p.health = 7f;
        p.biome = "minecraft:desert";
        GameData.update();
        assertEquals("7 ❤ 20 · 0 64 -4 · 12:00 · minecraft:desert", label.text());
        assertTrue(GameData.named().containsKey("inventory"));
    }
}
