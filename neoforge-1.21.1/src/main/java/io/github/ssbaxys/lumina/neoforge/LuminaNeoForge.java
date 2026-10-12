package io.github.ssbaxys.lumina.neoforge;

import io.github.ssbaxys.lumina.Lumina;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Lumina.MOD_ID)
public final class LuminaNeoForge {
    public LuminaNeoForge(IEventBus modBus) {
        // Клиентские классы загружаются только на клиенте; на сервере мод ничего не делает.
        if (FMLEnvironment.dist.isClient()) {
            LuminaNeoForgeClient.init(modBus);
        }
    }
}
