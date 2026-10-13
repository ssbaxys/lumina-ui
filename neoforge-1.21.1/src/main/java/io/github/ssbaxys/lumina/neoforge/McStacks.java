package io.github.ssbaxys.lumina.neoforge;

import io.github.ssbaxys.lumina.inventory.Stack;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link ItemStack} игры → {@link Stack} ядра для слотов меню: id, количество, название, строки подсказки игры
 * (с цветами — кодами {@code §}, их понимает шрифт), перелив, прочность и предел стопки. Сама стопка
 * прикладывается ({@link Stack#withPlatformStack}), чтобы {@link McCanvas} рисовал предмет точно. Только на
 * клиенте и только при изменении слота — не каждый кадр.
 */
final class McStacks {
    private static final Map<Item, String> IDS = new IdentityHashMap<>();

    private McStacks() {
    }

    static Stack toStack(ItemStack stack) {
        if (stack.isEmpty()) {
            return Stack.EMPTY;
        }
        Minecraft mc = Minecraft.getInstance();
        Stack out = Stack.of(id(stack.getItem()), stack.getCount())
                .withName(stack.getHoverName().getString());
        Item.TooltipContext context = mc.level != null ? Item.TooltipContext.of(mc.level) : Item.TooltipContext.EMPTY;
        List<Component> lines = stack.getTooltipLines(context, mc.player,
                mc.options.advancedItemTooltips ? TooltipFlag.ADVANCED : TooltipFlag.NORMAL);
        if (lines.size() > 1) {
            List<String> lore = new ArrayList<>(lines.size() - 1);
            for (int i = 1; i < lines.size(); i++) {
                lore.add(legacy(lines.get(i)));
            }
            out = out.withLore(lore);
        }
        out = out.withGlint(stack.hasFoil()).withMaxStackSize(stack.getMaxStackSize());
        if (stack.isDamageableItem()) {
            out = out.withDurability(stack.getDamageValue(), stack.getMaxDamage());
        }
        return out.withPlatformStack(stack);
    }

    private static String id(Item item) {
        String id = IDS.get(item);
        if (id == null) {
            id = BuiltInRegistries.ITEM.getKey(item).toString();
            IDS.put(item, id);
        }
        return id;
    }

    /** Текст со стилями → строка с кодами {@code §} (цвета без своего имени теряются). */
    static String legacy(Component component) {
        StringBuilder sb = new StringBuilder();
        component.<Void>visit((style, text) -> {
            if (!text.isEmpty()) {
                appendStyle(sb, style);
                sb.append(text);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

    private static void appendStyle(StringBuilder sb, Style style) {
        if (sb.length() > 0) {
            sb.append(ChatFormatting.RESET);
        }
        TextColor color = style.getColor();
        if (color != null) {
            ChatFormatting named = ChatFormatting.getByName(color.serialize());
            if (named != null && named.isColor()) {
                sb.append(named);
            }
        }
        if (style.isObfuscated()) {
            sb.append(ChatFormatting.OBFUSCATED);
        }
        if (style.isBold()) {
            sb.append(ChatFormatting.BOLD);
        }
        if (style.isStrikethrough()) {
            sb.append(ChatFormatting.STRIKETHROUGH);
        }
        if (style.isUnderlined()) {
            sb.append(ChatFormatting.UNDERLINE);
        }
        if (style.isItalic()) {
            sb.append(ChatFormatting.ITALIC);
        }
    }
}
