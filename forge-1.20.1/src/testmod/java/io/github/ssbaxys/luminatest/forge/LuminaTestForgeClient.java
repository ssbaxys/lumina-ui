package io.github.ssbaxys.luminatest.forge;

import io.github.ssbaxys.lumina.forge.LuminaMenuScreen;
import io.github.ssbaxys.lumina.forge.LuminaScreen;
import io.github.ssbaxys.luminatest.ChestDemo;
import io.github.ssbaxys.luminatest.Showcase;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

/**
 * Все точки входа в витрину: клавиша, команда и кнопки в главном меню и меню паузы; окно «Сундука Lumina»
 * ({@code /luminatest chest}).
 */
final class LuminaTestForgeClient {
    private static final KeyMapping OPEN = new KeyMapping("key.luminatest.open", GLFW.GLFW_KEY_K, "key.categories.luminatest");

    private LuminaTestForgeClient() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(LuminaTestForgeClient::registerKeys);
        modBus.addListener(LuminaTestForgeClient::clientSetup);
        MinecraftForge.EVENT_BUS.addListener(LuminaTestForgeClient::onClientTick);
        MinecraftForge.EVENT_BUS.addListener(LuminaTestForgeClient::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(LuminaTestForgeClient::onScreenInit);
    }

    static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new LuminaScreen(Component.literal("Lumina UI Showcase"),
                screen -> Showcase.create(screen::onClose)).parent(parent).pauseGame(parent instanceof PauseScreen));
    }

    private static void clientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> LuminaMenuScreen.register(ChestDemoForge.CHEST, ChestDemo::create));
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        while (OPEN.consumeClick()) {
            if (Minecraft.getInstance().screen == null) {
                open(null);
            }
        }
    }

    private static void registerCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("luminatest").executes(ctx -> {
            // Чат закрывается после команды, поэтому открываем экран следующей задачей.
            Minecraft.getInstance().tell(() -> open(null));
            return 1;
        }));
    }

    private static void onScreenInit(ScreenEvent.Init.Post event) {
        Screen screen = event.getScreen();
        if (screen instanceof TitleScreen || screen instanceof PauseScreen) {
            event.addListener(Button.builder(Component.literal("Lumina"), b -> open(screen))
                    .bounds(4, 4, 56, 20)
                    .build());
        }
    }
}
