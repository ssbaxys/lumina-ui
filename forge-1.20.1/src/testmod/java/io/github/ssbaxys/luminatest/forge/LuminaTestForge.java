package io.github.ssbaxys.luminatest.forge;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(LuminaTestForge.MOD_ID)
public final class LuminaTestForge {
    public static final String MOD_ID = "luminatest";

    public LuminaTestForge() {
        if (FMLEnvironment.dist.isClient()) {
            LuminaTestForgeClient.init(FMLJavaModLoadingContext.get().getModEventBus());
        }
    }
}
