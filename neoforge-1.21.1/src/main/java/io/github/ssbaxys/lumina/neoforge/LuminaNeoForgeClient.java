package io.github.ssbaxys.lumina.neoforge;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.demo.LuminaDemo;
import io.github.ssbaxys.lumina.i18n.I18n;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;

final class LuminaNeoForgeClient {
    private LuminaNeoForgeClient() {
    }

    static void init(IEventBus modBus) {
        McPlatform platform = new McPlatform();
        Lumina.init(platform);
        modBus.addListener(LuminaShaders::register);
        HudRenderer.init(modBus);
        modBus.addListener((RegisterClientReloadListenersEvent event) -> registerReload(event, platform));
        NeoForge.EVENT_BUS.addListener(LuminaNeoForgeClient::registerCommands);
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
