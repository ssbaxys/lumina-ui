package io.github.ssbaxys.lumina.neoforge;

import io.github.ssbaxys.lumina.menu.MenuActions;
import io.github.ssbaxys.lumina.menu.MenuData;
import io.github.ssbaxys.lumina.menu.MenuSpec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Меню-контейнер по {@link MenuSpec} — обычное {@link AbstractContainerMenu}, поэтому всё, что умеет сундук,
 * работает само: сервер проверяет щелчки, {@code broadcastChanges} рассылает содержимое ванильных слотов и
 * курсор, а значения уходят 16-битными {@link DataSlot} (целое — двумя). На клиенте меню создаёт тип
 * ({@link LuminaMenuType}) с пустым контейнером, а окно — {@link LuminaMenuScreen}.
 *
 * <p>Своё поведение — наследованием: переопределите {@link #quickMoveStack} или {@link #stillValid}; тогда
 * создавайте меню своим {@code MenuProvider}.</p>
 */
public class LuminaMenu extends AbstractContainerMenu {
    /** Свои действия окна на сервере ({@code RemoteMenu.sendAction} на клиенте). */
    @FunctionalInterface
    public interface Actions {
        void onAction(ServerPlayer player, int action, int value);
    }

    /** Ванильные слоты не рисуются и не ловят мышь: их место — за краем экрана. */
    private static final int HIDDEN = -10000;

    private final LuminaMenuType type;
    private final MenuSpec spec;
    private final Container container;
    private final MenuData data;
    private final Actions actions;
    private final int[] raw;
    private final MenuActions.Decoder decoder = new MenuActions.Decoder();

    /** Клиент: пустой контейнер нужного размера, содержимое пришлёт сервер. */
    protected LuminaMenu(LuminaMenuType type, int containerId, Inventory playerInventory) {
        this(type, containerId, playerInventory, new SimpleContainer(type.spec().containerSize()), null, null);
    }

    /**
     * Сервер: {@code container} — ячейки всех разделов контейнера подряд ({@link MenuSpec#containerSize()}),
     * {@code data} — значения ({@code null} — нули), {@code actions} — свои действия ({@code null} — нет).
     */
    public LuminaMenu(LuminaMenuType type, int containerId, Inventory playerInventory, Container container,
                      MenuData data, Actions actions) {
        super(type.get(), containerId);
        this.type = type;
        this.spec = type.spec();
        checkContainerSize(container, spec.containerSize());
        this.container = container;
        this.data = data;
        this.actions = actions;
        container.startOpen(playerInventory.player);
        for (MenuSpec.Section s : spec.sections()) {
            int end = s.firstSlot() + s.size();
            for (int slot = s.firstSlot(); slot < end; slot++) {
                int i = s.index(slot);
                if (s.isPlayer()) {
                    addSlot(new Slot(playerInventory, i, HIDDEN, HIDDEN));
                } else if (s.isTakeOnly()) {
                    addSlot(new TakeOnlySlot(container, s.containerOffset() + i));
                } else {
                    addSlot(new Slot(container, s.containerOffset() + i, HIDDEN, HIDDEN));
                }
            }
        }
        this.raw = new int[spec.dataSlotCount()];
        for (int d = 0; d < raw.length; d++) {
            addDataSlot(new Part(d));
        }
    }

    public LuminaMenuType luminaType() {
        return type;
    }

    public MenuSpec spec() {
        return spec;
    }

    public Container container() {
        return container;
    }

    /** Принятая часть значения {@code dataSlot} (на клиенте; знак — как пришёл). */
    public int rawData(int dataSlot) {
        return raw[dataSlot];
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    /** Shift+клик по маршруту {@link MenuSpec#quickMoveRoute}: как у сундука, без своего кода. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved = false;
        List<MenuSpec.Range> route = spec.quickMoveRoute(index);
        for (int r = 0; r < route.size() && !stack.isEmpty(); r++) {
            MenuSpec.Range range = route.get(r);
            moved |= moveItemStackTo(stack, range.from(), range.to(), range.reverse());
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    /** Нажатия кнопок меню собираются в действия ({@link MenuActions}). */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (actions == null || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        if (decoder.accept(id)) {
            actions.onAction(serverPlayer, decoder.action(), decoder.value());
        }
        return true;
    }

    /** Одна 16-битная часть значения: на сервере читается из {@link MenuData}, на клиенте хранится. */
    private final class Part extends DataSlot {
        private final int index;

        Part(int index) {
            this.index = index;
        }

        @Override
        public int get() {
            return data != null ? spec.dataPart(data, index) : raw[index];
        }

        @Override
        public void set(int value) {
            raw[index] = value;
        }
    }

    /** Слот «только брать» (результат, выход). */
    private static final class TakeOnlySlot extends Slot {
        TakeOnlySlot(Container container, int index) {
            super(container, index, HIDDEN, HIDDEN);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }
}
