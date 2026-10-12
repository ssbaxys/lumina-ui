package io.github.ssbaxys.lumina.forge;

import io.github.ssbaxys.lumina.Lumina;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Lumina.MOD_ID)
public final class LuminaForge {
    public LuminaForge() {
        // Клиентские классы загружаются только на клиенте; на сервере мод ничего не делает.
        if (FMLEnvironment.dist.isClient()) {
            LuminaForgeClient.init(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
}
