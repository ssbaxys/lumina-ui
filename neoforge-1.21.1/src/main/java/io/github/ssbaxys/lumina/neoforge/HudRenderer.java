package io.github.ssbaxys.lumina.neoforge;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.hud.Hud;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Рисует общий {@link Hud} поверх игрового интерфейса (слой над всеми ванильными) и тикает его
 * на каждом тике клиента — там же обновляется {@code GameData}. Мышь в HUD не передаётся.
 */
final class HudRenderer {
    private static McCanvas canvas;

    private HudRenderer() {
    }

    static void init(IEventBus modBus) {
        modBus.addListener(HudRenderer::registerLayers);
        NeoForge.EVENT_BUS.addListener(HudRenderer::onClientTick);
    }

    private static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(ResourceLocation.fromNamespaceAndPath(Lumina.MOD_ID, "hud"), HudRenderer::render);
    }

    private static void render(GuiGraphics graphics, DeltaTracker delta) {
        Hud hud = Hud.get();
        if (hud.isEmpty()) {
            return;
        }
        if (canvas == null) {
            canvas = new McCanvas();
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        canvas.begin(graphics, width, height);
        try {
            hud.render(canvas, width, height, delta.getGameTimeDeltaPartialTick(false));
        } finally {
            canvas.end();
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Hud.get().clientTick();
    }
}
