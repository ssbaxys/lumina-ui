package io.github.ssbaxys.lumina.neoforge;

import io.github.ssbaxys.lumina.menu.MenuData;
import io.github.ssbaxys.lumina.menu.MenuSpec;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;

/**
 * Тип меню Lumina для реестра и открытие окна с сервера одной строкой. Описание ({@link MenuSpec}) общее у
 * сервера и клиента, поэтому по сети идёт только то, что и у ванильного сундука: id окна, тип и заголовок.
 *
 * <pre>{@code
 * // общий код мода
 * static final LuminaMenuType CHEST = new LuminaMenuType(SPEC);
 * static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MOD_ID);
 * static { MENUS.register("chest", CHEST::create); }      // и MENUS.register(modBus)
 *
 * // сервер: команда, блок, предмет
 * CHEST.open(player, Component.literal("Сундук"), container, values, (p, action, value) -> sort(container));
 *
 * // клиент, шина мода: RegisterMenuScreensEvent → LuminaMenuScreen.register(event, CHEST, ChestUi::create)
 * }</pre>
 */
public final class LuminaMenuType {
    private final MenuSpec spec;
    private MenuType<LuminaMenu> type;

    public LuminaMenuType(MenuSpec spec) {
        this.spec = spec;
    }

    public MenuSpec spec() {
        return spec;
    }

    /** Создать {@link MenuType} для реестра (поставщик для {@code DeferredRegister.register}). */
    public MenuType<LuminaMenu> create() {
        MenuType<LuminaMenu> created = IMenuTypeExtension.create((id, inventory, buf) -> new LuminaMenu(this, id, inventory));
        this.type = created;
        return created;
    }

    /** Зарегистрированный тип ({@link IllegalStateException}, если {@link #create()} ещё не вызывали). */
    public MenuType<LuminaMenu> get() {
        if (type == null) {
            throw new IllegalStateException("menu type is not registered yet: register LuminaMenuType::create");
        }
        return type;
    }

    /** Открыть окно игроку: серверное меню над {@code container} со значениями {@code data} и действиями. */
    public void open(ServerPlayer player, Component title, Container container, MenuData data,
                     LuminaMenu.Actions actions) {
        player.openMenu(provider(title, container, data, actions));
    }

    /** То же как {@link MenuProvider} (для блоков: {@code getMenuProvider}). */
    public MenuProvider provider(Component title, Container container, MenuData data, LuminaMenu.Actions actions) {
        return new SimpleMenuProvider((id, inventory, player) ->
                new LuminaMenu(this, id, inventory, container, data, actions), title);
    }
}
