package io.github.ssbaxys.luminatest.forge;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.ssbaxys.lumina.forge.LuminaMenuType;
import io.github.ssbaxys.lumina.menu.MenuData;
import io.github.ssbaxys.luminatest.ChestDemo;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Серверная часть «Сундука Lumina» ({@link ChestDemo}): тип меню, команда {@code /luminatest chest} и сундук на
 * 27 ячеек у каждого игрока (живёт, пока работает сервер). Значения считаются на лету при каждой рассылке, а
 * действия окна сортируют и наполняют сундук — всё на сервере.
 */
final class ChestDemoForge {
    static final LuminaMenuType CHEST = new LuminaMenuType(ChestDemo.SPEC);

    private static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, LuminaTestForge.MOD_ID);
    private static final Map<UUID, SimpleContainer> CHESTS = new HashMap<>();
    private static final int SIZE = ChestDemo.ROWS * 9;

    static {
        MENUS.register("chest", CHEST::create);
    }

    private ChestDemoForge() {
    }

    static void init(IEventBus modBus) {
        MENUS.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(ChestDemoForge::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(ChestDemoForge::onServerStopping);
    }

    private static void onServerStopping(ServerStoppingEvent event) {
        CHESTS.clear();
    }

    /**
     * {@code /luminatest chest} — серверная команда рядом с клиентской {@code /luminatest}: клиент свою
     * ветку без аргументов выполняет сам, а «chest» не узнаёт и отдаёт серверу.
     */
    private static void registerCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("luminatest")
                .then(Commands.literal("chest").executes(ctx -> open(ctx.getSource()))));
    }

    private static int open(CommandSourceStack source) throws CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        SimpleContainer chest = CHESTS.computeIfAbsent(player.getUUID(), id -> new SimpleContainer(SIZE));
        MenuData data = index -> value(player, chest, index);
        CHEST.open(player, Component.literal("Сундук Lumina"), chest, data, (p, action, value) -> {
            if (action == ChestDemo.SORT) {
                sort(chest, value == ChestDemo.BY_COUNT);
            } else if (action == ChestDemo.FILL) {
                fill(chest);
            }
        });
        return 1;
    }

    private static int value(ServerPlayer player, SimpleContainer chest, int index) {
        long time = player.level().getGameTime();
        if (index == ChestDemo.PROGRESS.index()) {
            return (int) (time % (ChestDemo.CYCLE + 1));
        }
        if (index == ChestDemo.ITEMS.index()) {
            int n = 0;
            for (int i = 0; i < SIZE; i++) {
                n += chest.getItem(i).getCount();
            }
            return n;
        }
        if (index == ChestDemo.SORTED.index()) {
            return sorted(chest) ? 1 : 0;
        }
        if (index == ChestDemo.TIME.index()) {
            return (int) time;
        }
        return 0;
    }

    private static String id(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    /** Разложен: предметы подряд с начала, по id. */
    private static boolean sorted(SimpleContainer chest) {
        String last = null;
        boolean gap = false;
        for (int i = 0; i < SIZE; i++) {
            ItemStack s = chest.getItem(i);
            if (s.isEmpty()) {
                gap = true;
                continue;
            }
            String id = id(s);
            if (gap || (last != null && last.compareTo(id) > 0)) {
                return false;
            }
            last = id;
        }
        return true;
    }

    /** Собрать одинаковые стопки и разложить по id (или по количеству) с начала сундука. */
    private static void sort(SimpleContainer chest, boolean byCount) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 0; i < SIZE; i++) {
            ItemStack s = chest.getItem(i);
            if (!s.isEmpty()) {
                merge(stacks, s.copy());
            }
        }
        Comparator<ItemStack> byId = Comparator.comparing(ChestDemoForge::id);
        Comparator<ItemStack> bySize = Comparator.comparingInt(ItemStack::getCount).reversed();
        stacks.sort(byCount ? bySize.thenComparing(byId) : byId.thenComparing(bySize));
        for (int i = 0; i < SIZE; i++) {
            chest.setItem(i, i < stacks.size() ? stacks.get(i) : ItemStack.EMPTY);
        }
    }

    private static void merge(List<ItemStack> into, ItemStack stack) {
        for (ItemStack s : into) {
            if (ItemStack.isSameItemSameTags(s, stack) && s.getCount() < s.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), s.getMaxStackSize() - s.getCount());
                s.grow(move);
                stack.shrink(move);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
        into.add(stack);
    }

    /** Пробные предметы: зелье и повреждённый меч проверяют, что слоты рисуют настоящие стопки. */
    private static void fill(SimpleContainer chest) {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(600);
        ItemStack[] loot = {
                new ItemStack(Items.DIAMOND, 12), new ItemStack(Items.IRON_INGOT, 40), new ItemStack(Items.GOLD_INGOT, 9),
                new ItemStack(Items.EMERALD, 5), new ItemStack(Items.COBBLESTONE, 64), new ItemStack(Items.OAK_LOG, 23),
                new ItemStack(Items.TORCH, 32), new ItemStack(Items.BREAD, 7), new ItemStack(Items.ENDER_PEARL, 6),
                new ItemStack(Items.ENCHANTED_GOLDEN_APPLE), sword,
                PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.HEALING),
        };
        for (ItemStack s : loot) {
            chest.addItem(s);
        }
    }
}
