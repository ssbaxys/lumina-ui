package io.github.ssbaxys.lumina.neoforge;

import io.github.ssbaxys.lumina.node.UiRoot;
import net.minecraft.client.gui.GuiGraphics;

/**
 * {@link UiRoot} и его {@link McCanvas} внутри экрана игры — общее у {@link LuminaScreen} и
 * {@link LuminaMenuScreen}: кадр, пересылка ввода в ядро и освобождение ресурсов при закрытии.
 */
final class UiHost {
    private final UiRoot ui;
    private final McCanvas canvas = new McCanvas();

    /** {@code back} — что делать с кнопкой B геймпада, если UI её не обработал (обычно — как Esc). */
    UiHost(UiRoot ui, Runnable back) {
        this.ui = ui;
        if (ui.gamepad().onBack() == null) {
            ui.gamepad().onBack(back);
        }
    }

    UiRoot ui() {
        return ui;
    }

    void resize(int width, int height) {
        ui.resize(width, height);
    }

    void render(GuiGraphics graphics, int width, int height, float partialTick) {
        canvas.begin(graphics, width, height);
        try {
            ui.render(canvas, partialTick);
        } finally {
            canvas.end();
        }
    }

    void mouseMoved(double mouseX, double mouseY) {
        ui.pointerMove((float) mouseX, (float) mouseY);
    }

    boolean mouseClicked(double mouseX, double mouseY, int button) {
        return ui.pointerDown((float) mouseX, (float) mouseY, button);
    }

    boolean mouseReleased(double mouseX, double mouseY, int button) {
        return ui.pointerUp((float) mouseX, (float) mouseY, button);
    }

    boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        return ui.scroll((float) mouseX, (float) mouseY, (float) deltaX, (float) deltaY);
    }

    boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return ui.keyDown(keyCode, scanCode, modifiers);
    }

    boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return ui.keyUp(keyCode, scanCode, modifiers);
    }

    boolean charTyped(char codePoint, int modifiers) {
        return ui.charTyped(codePoint, modifiers);
    }

    void dispose() {
        ui.dispose();
        canvas.dispose();
    }
}
