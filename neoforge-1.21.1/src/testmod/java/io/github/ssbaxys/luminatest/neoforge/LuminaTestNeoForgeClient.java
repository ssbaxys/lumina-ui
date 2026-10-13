package io.github.ssbaxys.luminatest.neoforge;

import io.github.ssbaxys.lumina.neoforge.LuminaMenuScreen;
import io.github.ssbaxys.lumina.neoforge.LuminaScreen;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Все точки входа в витрину: клавиша, команда и кнопки в главном меню и меню паузы; окно «Сундука Lumina»
 * ({@code /luminatest chest}).
 */
final class LuminaTestNeoForgeClient {
    private static final KeyMapping OPEN = new KeyMapping("key.luminatest.open", GLFW.GLFW_KEY_K, "key.categories.luminatest");

    private LuminaTestNeoForgeClient() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(LuminaTestNeoForgeClient::registerKeys);
        modBus.addListener(LuminaTestNeoForgeClient::registerScreens);
        NeoForge.EVENT_BUS.addListener(LuminaTestNeoForgeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(LuminaTestNeoForgeClient::registerCommands);
        NeoForge.EVENT_BUS.addListener(LuminaTestNeoForgeClient::onScreenInit);
    }

    static void open(Screen parent) {
        Minecraft.getInstance().setScreen(new LuminaScreen(Component.literal("Lumina UI Showcase"),
                screen -> Showcase.create(screen::onClose)).parent(parent).pauseGame(parent instanceof PauseScreen));
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        LuminaMenuScreen.register(event, ChestDemoNeoForge.CHEST, ChestDemo::create);
    }

    private static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
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
