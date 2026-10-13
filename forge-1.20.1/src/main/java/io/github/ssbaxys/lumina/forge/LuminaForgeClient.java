package io.github.ssbaxys.lumina.forge;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.demo.LuminaDemo;
import io.github.ssbaxys.lumina.i18n.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;

final class LuminaForgeClient {
    private LuminaForgeClient() {
    }

    static void init(IEventBus modBus) {
        McPlatform platform = new McPlatform();
        Lumina.init(platform);
        modBus.addListener(LuminaShaders::register);
        HudRenderer.init(modBus);
        modBus.addListener((RegisterClientReloadListenersEvent event) -> registerReload(event, platform));
        MinecraftForge.EVENT_BUS.addListener(LuminaForgeClient::registerCommands);
    }

    /** После перезагрузки ресурсов (в том числе смены языка игры) переводы читаются заново. */
    private static void registerReload(RegisterClientReloadListenersEvent event, McPlatform platform) {
        event.registerReloadListener((ResourceManagerReloadListener) manager -> {
            platform.clearLanguages();
            // Следующей задачей: к этому моменту игра уже загрузила свои таблицы языка.
            Minecraft.getInstance().tell(I18n::refresh);
        });
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
