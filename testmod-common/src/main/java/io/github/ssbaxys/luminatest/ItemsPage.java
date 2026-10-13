package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.inventory.Inventory;
import io.github.ssbaxys.lumina.inventory.InventoryPanel;
import io.github.ssbaxys.lumina.inventory.Slot;
import io.github.ssbaxys.lumina.inventory.SlotGrid;
import io.github.ssbaxys.lumina.inventory.SlotGroup;
import io.github.ssbaxys.lumina.inventory.Stack;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.preview.EntityView;
import io.github.ssbaxys.lumina.preview.ItemView;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Toggle;

import java.util.ArrayList;
import java.util.List;

/**
 * Предметы и сущности: сундук и инвентарь игрока с перетаскиванием (тот же сундук в двух видах), броня и
 * мусорка, галерея мобов в 3D, игрок в своей броне, крупные вращающиеся предметы и те же виджеты из разметки.
 */
final class ItemsPage {
    private static final String[][] MOBS = {
            {"minecraft:zombie", "Зомби"},
            {"minecraft:creeper", Tr.r("Крипер")},
            {"minecraft:villager", Tr.r("Житель")},
            {"minecraft:skeleton", Tr.r("Скелет")},
            {"minecraft:enderman", Tr.r("Эндермен")},
            {"minecraft:iron_golem", Tr.r("Железный голем")},
            {"minecraft:pig", Tr.r("Свинья")},
            {"minecraft:sheep", Tr.r("Овца")},
            {"minecraft:wolf", Tr.r("Волк")},
            {"minecraft:chicken", Tr.r("Курица")},
            {"minecraft:spider", Tr.r("Паук")},
            {"minecraft:fox", Tr.r("Лиса")},
    };

    private ItemsPage() {
    }

    static UiElement create() {
        SlotGroup group = new SlotGroup();
        Label status = Ui.muted(Tr.r("Перенесите что-нибудь: здесь появится, что и куда ушло."));
        group.onTransfer((from, to, moved) -> status.text(Tr.r("Перенесено: ") + moved.count() + " × "
                + moved.displayName() + (to == null ? Tr.r(" (Shift+клик или выброшено)") : Tr.r(" → ячейка ") + to.index())));
        // Мимо слотов — «выбросить»: предмет пропадает.
        group.onDropOutside((from, stack) -> true);

        Inventory chest = chest();
        Inventory player = player();
        return Ui.page(
                Ui.section(Tr.r("Сундук и инвентарь"),
                        Tr.r("ЛКМ-перетаскивание — вся стопка, ПКМ — половина, Shift+клик — быстрый перенос, мимо окна — ")
                                + Tr.r("выбросить. Справа тот же сундук в современном виде: оба окна показывают одни данные."),
                        Ui.row(12, vanilla(chest, player, group), modern(chest, player, group)).align(Align.START),
                        status),
                Ui.section(Tr.r("Броня и мусорка"),
                        Tr.r("В ячейки брони ложится только своя броня (фильтр инвентаря), мусорка уничтожает всё, что в неё ")
                                + Tr.r("положили. Пустые ячейки брони показывают бледный силуэт."),
                        armor(player, group)),
                Ui.section(Tr.r("Мобы"),
                        Tr.r("Каждого можно крутить мышью; переключатель включает автовращение. Мобы рисуются игрой, как ")
                                + Tr.r("игрок в окне инвентаря, поэтому видны только в мире (не в главном меню)."),
                        mobs()),
                Ui.section(Tr.r("Игрок"),
                        Tr.r("Вы сами в своей броне и с предметами в руках. Слева — смотрит на курсор, справа — вращается."),
                        Ui.row(16,
                                Ui.labeled(EntityView.player().followMouse(true).zoomable(false).size(110, 150),
                                        Tr.r("Следит за мышью")),
                                Ui.labeled(EntityView.player().autoRotate(true).zoomable(false).size(110, 150),
                                        Tr.r("Автовращение")))),
                Ui.section(Tr.r("Крупные предметы"),
                        Tr.r("Предметы в 3D: блоки объёмные, остальное — как выброшенное на землю. Крутятся мышью и сами."),
                        Ui.row(16,
                                Ui.labeled(new ItemView("minecraft:grass_block").autoRotate(true).zoomable(false)
                                        .size(72, 72), Tr.r("Блок")),
                                Ui.labeled(new ItemView(Stack.of("minecraft:diamond_sword").withGlint(true))
                                        .autoRotate(40f).zoomable(false).size(72, 72), Tr.r("Зачарованный меч")),
                                Ui.labeled(new ItemView("minecraft:totem_of_undying").camera(20f, 10f).zoomable(false)
                                        .size(72, 72), Tr.r("Тотем (мышью)")))),
                Ui.section(Tr.r("Из разметки"),
                        Tr.r("Теги <slot>, <entity>, <player> и <item-view>."),
                        Markup.build(Tr.r("""
                                <row gap="10">
                                  <slot item="minecraft:golden_apple" count="3" glint name="Чарованное яблоко"
                                        lore="Даёт поглощение|И регенерацию"/>
                                  <slot item="minecraft:iron_pickaxe" durability="80/250" look="modern"/>
                                  <entity type="minecraft:zombie" mainhand="minecraft:iron_sword" baby auto-rotate
                                          zoomable="false" width="60" height="70"/>
                                  <player follow-mouse zoomable="false" width="60" height="80"/>
                                  <item-view item="minecraft:beacon" auto-rotate="60" zoomable="false" size="48"/>
                                </row>
                                """), new Bindings()).root()));
    }

    // ---------------------------------------------------------------- инвентари

    private static Inventory chest() {
        Inventory chest = new Inventory(27);
        chest.set(0, Stack.of("minecraft:diamond", 12));
        chest.set(1, Stack.of("minecraft:iron_ingot", 32));
        chest.set(2, Stack.of("minecraft:gold_ingot", 7));
        chest.set(3, Stack.of("minecraft:emerald", 5));
        chest.set(4, Stack.of("minecraft:diamond_sword").withName(Tr.r("Экскалибур"))
                .withLore(Tr.r("Выкован в недрах Незера"), Tr.r("Острый, как в первый день")).withGlint(true).withDurability(400, 1561));
        chest.set(5, Stack.of("minecraft:bow").withDurability(300, 384));
        chest.set(6, Stack.of("minecraft:ender_pearl", 16).withMaxStackSize(16));
        chest.set(9, Stack.of("minecraft:oak_log", 20));
        chest.set(10, Stack.of("minecraft:cobblestone", 64));
        chest.set(11, Stack.of("minecraft:torch", 48));
        chest.set(12, Stack.of("minecraft:redstone", 45));
        chest.set(13, Stack.of("minecraft:tnt", 3));
        chest.set(18, Stack.of("minecraft:apple", 16));
        chest.set(19, Stack.of("minecraft:bread", 9));
        chest.set(20, Stack.of("minecraft:enchanted_golden_apple", 2).withGlint(true));
        chest.set(26, Stack.of("minecraft:diamond_helmet"));
        return chest;
    }

    private static Inventory player() {
        Inventory player = new Inventory(36);
        player.set(0, Stack.of("minecraft:iron_pickaxe").withDurability(170, 250));
        player.set(1, Stack.of("minecraft:stone_sword").withDurability(20, 131));
        player.set(2, Stack.of("minecraft:bread", 5));
        player.set(3, Stack.of("minecraft:torch", 30));
        player.set(8, Stack.of("minecraft:water_bucket"));
        player.set(9, Stack.of("minecraft:cobblestone", 40));
        player.set(17, Stack.of("minecraft:iron_chestplate").withDurability(30, 240));
        player.set(27, Stack.of("minecraft:coal", 23));
        return player;
    }

    private static UiElement vanilla(Inventory chest, Inventory player, SlotGroup group) {
        InventoryPanel panel = InventoryPanel.chest(Tr.r("Сундук"), chest, player, Slot.Look.VANILLA);
        setGroup(panel, group);
        return panel;
    }

    private static UiElement modern(Inventory chest, Inventory player, SlotGroup group) {
        InventoryPanel panel = InventoryPanel.chest(Tr.r("Сундук"), chest, player, Slot.Look.MODERN);
        setGroup(panel, group);
        return panel;
    }

    private static void setGroup(UiElement e, SlotGroup group) {
        if (e instanceof SlotGrid g) {
            g.group(group);
        }
        for (UiElement c : e.children()) {
            setGroup(c, group);
        }
    }

    private static UiElement armor(Inventory player, SlotGroup group) {
        String[] kinds = {"_helmet", "_chestplate", "_leggings", "_boots"};
        String[] silhouettes = {"minecraft:iron_helmet", "minecraft:iron_chestplate", "minecraft:iron_leggings",
                "minecraft:iron_boots"};
        Inventory armor = new Inventory(4).limit(1)
                .filter((index, stack) -> stack.itemId().endsWith(kinds[index]));
        armor.set(3, Stack.of("minecraft:diamond_boots").withDurability(100, 429));
        Panel column = Ui.column(0);
        for (int i = 0; i < 4; i++) {
            column.add(new Slot(armor, i).placeholder(silhouettes[i]).group(group).quickMoveTo(player));
        }

        Inventory trash = new Inventory(1);
        trash.onChange((inv, index, before, after) -> {
            if (!after.isEmpty()) {
                inv.set(index, Stack.EMPTY);
            }
        });
        Slot bin = new Slot(trash, 0).look(Slot.Look.MODERN).placeholder("minecraft:lava_bucket").group(group)
                .size(26, 26);

        Inventory loot = Inventory.of(Stack.of("minecraft:golden_helmet"), Stack.of("minecraft:chainmail_leggings"),
                Stack.of("minecraft:leather_chestplate"), Stack.of("minecraft:rotten_flesh", 30));
        SlotGrid lootGrid = new SlotGrid(loot, 4).look(Slot.Look.MODERN).slotSize(22).group(group).quickMoveTo(armor);
        return Ui.row(16,
                Ui.labeled(column, Tr.r("Броня")),
                Ui.labeled(lootGrid, Tr.r("Добыча (Shift+клик — надеть)")),
                Ui.labeled(bin, Tr.r("Мусорка"))).align(Align.START);
    }

    // ---------------------------------------------------------------- мобы

    private static UiElement mobs() {
        Signal<Boolean> spin = Signal.of(false);
        List<EntityView> views = new ArrayList<>();
        List<UiElement> cells = new ArrayList<>();
        for (String[] mob : MOBS) {
            EntityView v = new EntityView(mob[0]).zoomable(false).size(64, 80);
            if (mob[0].equals("minecraft:zombie")) {
                v.equip("mainhand", "minecraft:iron_sword").equip("head", "minecraft:iron_helmet");
            } else if (mob[0].equals("minecraft:skeleton")) {
                v.equip("mainhand", "minecraft:bow");
            }
            views.add(v);
            cells.add(Ui.labeled(v, Tr.r(mob[1])));
        }
        EntityView babyZombie = new EntityView("minecraft:zombie").baby(true).zoomable(false).size(64, 80);
        views.add(babyZombie);
        cells.add(Ui.labeled(babyZombie, Tr.r("Зомби-ребёнок")));
        spin.subscribe(on -> {
            for (EntityView v : views) {
                v.autoRotate(on);
            }
        });
        Panel gallery = Ui.grid(7, 10, cells.toArray(new UiElement[0]));
        return Ui.column(8, Ui.row(8, new Toggle(spin), new Label(Tr.r("Автовращение"))), gallery);
    }
}
