package io.github.ssbaxys.luminatest.forge;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(LuminaTestForge.MOD_ID)
public final class LuminaTestForge {
    public static final String MOD_ID = "luminatest";

    public LuminaTestForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        // Меню «Сундука Lumina» и его команда нужны и серверу.
        ChestDemoForge.init(modBus);
        if (FMLEnvironment.dist.isClient()) {
            LuminaTestForgeClient.init(modBus);
        }
    }
}
