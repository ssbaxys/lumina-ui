package io.github.ssbaxys.lumina.forge;

import io.github.ssbaxys.lumina.menu.MenuChannel;
import io.github.ssbaxys.lumina.menu.RemoteMenu;
import io.github.ssbaxys.lumina.menu.SlotClick;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * Окно меню-контейнера с интерфейсом Lumina: ванильные слоты не рисуются, вместо них — {@link UiRoot} из фабрики,
 * которая получает {@link RemoteMenu}. Каждый кадр окно сверяет слоты меню игры с моделью ядра (сравнение
 * ссылок и количества; полное — раз в тик) и перестраивает {@code Stack} только у изменившихся слотов.
 * Жесты ядра уходят в {@code MultiPlayerGameMode.handleInventoryMouseClick}, как щелчки ванильного окна:
 * игра предсказывает результат, сервер проверяет. Клавиши 1–9, F и Q над слотом — обмен и выброс.
 *
 * <pre>{@code
 * // FMLClientSetupEvent
 * event.enqueueWork(() -> LuminaMenuScreen.register(CHEST, menu -> ChestUi.create(menu)));
 * }</pre>
 */
public class LuminaMenuScreen extends AbstractContainerScreen<LuminaMenu> {
    private final RemoteMenu remote;
    private final UiHost host;
    /** Копии стопок, уже переданных в ядро. */
    private final ItemStack[] seen;
    /** Какой объект стопки лежал в слоте и сколько в нём было при прошлой сверке. */
    private final ItemStack[] seenRef;
    private final int[] seenCount;
    private ItemStack seenCarried = ItemStack.EMPTY;
    private ItemStack seenCarriedRef;
    private int seenCarriedCount;

    public LuminaMenuScreen(LuminaMenu menu, Inventory playerInventory, Component title,
                            Function<? super RemoteMenu, UiRoot> factory) {
        super(menu, playerInventory, title);
        int n = menu.slots.size();
        this.seen = new ItemStack[n];
        Arrays.fill(seen, ItemStack.EMPTY);
        this.seenRef = new ItemStack[n];
        this.seenCount = new int[n];
        this.remote = new RemoteMenu(menu.spec(), title.getString(), new Channel());
        pull(true);
        this.host = new UiHost(factory.apply(remote), this::onClose);
    }

    /** Окно для типа меню (вызывать на главном потоке: {@code FMLClientSetupEvent.enqueueWork}). */
    public static void register(LuminaMenuType type, Function<? super RemoteMenu, UiRoot> factory) {
        MenuScreens.<LuminaMenu, LuminaMenuScreen>register(type.get(),
                (menu, inventory, title) -> new LuminaMenuScreen(menu, inventory, title, factory));
    }

    public RemoteMenu remote() {
        return remote;
    }

    public UiRoot ui() {
        return host.ui();
    }

    // ================================================================ синхронизация

    /** Сверить слоты, курсор и значения меню игры с моделью ядра. */
    private void pull(boolean deep) {
        List<Slot> slots = menu.slots;
        for (int i = 0; i < seen.length; i++) {
            ItemStack now = slots.get(i).getItem();
            if (!deep && now == seenRef[i] && now.getCount() == seenCount[i]) {
                continue;
            }
            seenRef[i] = now;
            seenCount[i] = now.getCount();
            if (!ItemStack.matches(seen[i], now)) {
                ItemStack copy = now.copy();
                seen[i] = copy;
                remote.setSlot(i, McStacks.toStack(copy));
            }
        }
        ItemStack carried = menu.getCarried();
        if (deep || carried != seenCarriedRef || carried.getCount() != seenCarriedCount) {
            seenCarriedRef = carried;
            seenCarriedCount = carried.getCount();
            if (!ItemStack.matches(seenCarried, carried)) {
                seenCarried = carried.copy();
                remote.setCarried(McStacks.toStack(seenCarried));
            }
        }
        int parts = menu.spec().dataSlotCount();
        for (int d = 0; d < parts; d++) {
            remote.setData(d, menu.rawData(d));
        }
        remote.publish();
    }

    @Override
    protected void containerTick() {
        pull(true);
    }

    // ================================================================ кадр

    @Override
    protected void init() {
        super.init();
        host.resize(width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        pull(false);
        renderBackground(graphics);
        host.render(graphics, width, height, partialTick);
        ItemStack carried = menu.getCarried();
        if (!carried.isEmpty()) {
            // Обычно пусто: жесты ничего не оставляют на курсоре. Если сервер оставил — видно, что в руке.
            graphics.pose().pushPose();
            graphics.pose().translate(0f, 0f, 232f);
            graphics.renderItem(carried, mouseX - 8, mouseY - 8);
            graphics.renderItemDecorations(font, carried, mouseX - 8, mouseY - 8);
            graphics.pose().popPose();
        }
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    // ================================================================ ввод

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        host.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return host.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return host.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        host.mouseMoved(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return host.mouseScrolled(mouseX, mouseY, 0d, delta);
    }

    /** UI первым (поля ввода), затем клавиши слотов, затем ванильные: Esc и клавиша инвентаря закрывают окно. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (host.keyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        if (minecraft != null && slotKey(minecraft.options, keyCode, scanCode)) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private boolean slotKey(Options options, int keyCode, int scanCode) {
        UiElement hovered = host.ui().hovered();
        if (hovered == null) {
            return false;
        }
        for (int i = 0; i < options.keyHotbarSlots.length; i++) {
            if (options.keyHotbarSlots[i].matches(keyCode, scanCode)) {
                return remote.swapHovered(hovered, i);
            }
        }
        if (options.keySwapOffhand.matches(keyCode, scanCode)) {
            return remote.swapHovered(hovered, SlotClick.OFFHAND);
        }
        if (options.keyDrop.matches(keyCode, scanCode)) {
            return remote.throwHovered(hovered, hasControlDown());
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return host.keyReleased(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return host.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        super.removed();
        host.dispose();
    }

    // ================================================================ связь с игрой

    private static ClickType vanilla(SlotClick click) {
        return switch (click) {
            case PICKUP -> ClickType.PICKUP;
            case QUICK_MOVE -> ClickType.QUICK_MOVE;
            case SWAP -> ClickType.SWAP;
            case CLONE -> ClickType.CLONE;
            case THROW -> ClickType.THROW;
            case QUICK_CRAFT -> ClickType.QUICK_CRAFT;
            case PICKUP_ALL -> ClickType.PICKUP_ALL;
        };
    }

    /** Щелчки и кнопки — тем же путём, что у ванильных окон. */
    private final class Channel implements MenuChannel {
        @Override
        public void click(int slot, int button, SlotClick type) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null && mc.player != null) {
                mc.gameMode.handleInventoryMouseClick(menu.containerId, slot, button, vanilla(type), mc.player);
            }
        }

        @Override
        public boolean isCarrying() {
            return !menu.getCarried().isEmpty();
        }

        @Override
        public void clickButton(int buttonId) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                mc.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
            }
        }

        @Override
        public void flush() {
            pull(false);
        }

        @Override
        public void close() {
            onClose();
        }
    }
}
