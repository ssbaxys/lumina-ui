package io.github.ssbaxys.luminatest.neoforge;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(LuminaTestNeoForge.MOD_ID)
public final class LuminaTestNeoForge {
    public static final String MOD_ID = "luminatest";

    public LuminaTestNeoForge(IEventBus modBus) {
        if (FMLEnvironment.dist.isClient()) {
            LuminaTestNeoForgeClient.init(modBus);
        }
    }
}
