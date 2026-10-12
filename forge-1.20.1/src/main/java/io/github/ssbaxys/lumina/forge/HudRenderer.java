package io.github.ssbaxys.lumina.forge;

import io.github.ssbaxys.lumina.hud.Hud;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Рисует общий {@link Hud} поверх игрового интерфейса (оверлей над всеми ванильными) и тикает его
 * на каждом тике клиента — там же обновляется {@code GameData}. Мышь в HUD не передаётся.
 */
final class HudRenderer {
    private static McCanvas canvas;

    private HudRenderer() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(HudRenderer::registerOverlays);
        MinecraftForge.EVENT_BUS.addListener(HudRenderer::onClientTick);
    }

    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        // id станет "luminaui:hud" (пространство имён — мода, который сейчас загружается).
        event.registerAboveAll("hud", HudRenderer::render);
    }

    private static void render(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height) {
        Hud hud = Hud.get();
        if (hud.isEmpty()) {
            return;
        }
        if (canvas == null) {
            canvas = new McCanvas();
        }
        canvas.begin(graphics, width, height);
        try {
            hud.render(canvas, width, height, partialTick);
        } finally {
            canvas.end();
        }
    }

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Hud.get().clientTick();
        }
    }
}
