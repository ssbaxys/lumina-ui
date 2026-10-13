package io.github.ssbaxys.lumina.forge;

import io.github.ssbaxys.lumina.Lumina;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Lumina.MOD_ID)
public final class LuminaForge {
    public LuminaForge() {
        // Клиентские классы загружаются только на клиенте. Серверу нужны лишь классы меню (LuminaMenu,
        // LuminaMenuType), которые регистрирует сам мод-пользователь, — инициализировать нечего.
        if (FMLEnvironment.dist.isClient()) {
            LuminaForgeClient.init(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
}
