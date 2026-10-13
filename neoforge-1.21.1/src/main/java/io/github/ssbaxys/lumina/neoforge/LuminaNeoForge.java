package io.github.ssbaxys.lumina.neoforge;

import io.github.ssbaxys.lumina.Lumina;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Lumina.MOD_ID)
public final class LuminaNeoForge {
    public LuminaNeoForge(IEventBus modBus) {
        // Клиентские классы загружаются только на клиенте. Серверу нужны лишь классы меню (LuminaMenu,
        // LuminaMenuType), которые регистрирует сам мод-пользователь, — инициализировать нечего.
        if (FMLEnvironment.dist.isClient()) {
            LuminaNeoForgeClient.init(modBus);
        }
    }
}
