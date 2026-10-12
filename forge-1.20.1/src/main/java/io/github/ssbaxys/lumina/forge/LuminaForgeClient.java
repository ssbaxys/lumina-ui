package io.github.ssbaxys.lumina.forge;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.demo.LuminaDemo;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

final class LuminaForgeClient {
    private LuminaForgeClient() {
    }

    static void init(IEventBus modBus) {
        Lumina.init(new McPlatform());
        modBus.addListener(LuminaShaders::register);
        HudRenderer.init(modBus);
        MinecraftForge.EVENT_BUS.addListener(LuminaForgeClient::registerCommands);
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("lumina")
                .then(Commands.literal("demo").executes(ctx -> {
                    Minecraft mc = Minecraft.getInstance();
                    // Чат закрывается после команды, поэтому открываем экран следующей задачей.
                    mc.tell(() -> mc.setScreen(new LuminaScreen(Component.literal("Lumina UI"),
                            screen -> LuminaDemo.create(screen::onClose))));
                    return 1;
                })));
    }
}
