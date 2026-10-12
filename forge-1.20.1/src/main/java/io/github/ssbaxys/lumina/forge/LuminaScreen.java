package io.github.ssbaxys.lumina.forge;

import io.github.ssbaxys.lumina.node.UiRoot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

/**
 * Экран Minecraft, в котором живёт {@link UiRoot}. Весь ввод пересылается в ядро;
 * то, что UI не обработал, уходит в стандартную логику Screen (например, Esc).
 */
public class LuminaScreen extends Screen {
    private final UiRoot ui;
    private final McCanvas canvas = new McCanvas();
    private boolean pauseGame;
    private Screen parent;

    public LuminaScreen(Component title, Function<? super LuminaScreen, UiRoot> factory) {
        super(title);
        this.ui = factory.apply(this);
        if (ui.gamepad().onBack() == null) {
            // кнопка B геймпада, которую UI не обработал, — как Esc
            ui.gamepad().onBack(() -> {
                if (shouldCloseOnEsc()) {
                    onClose();
                }
            });
        }
    }

    public UiRoot ui() {
        return ui;
    }

    /** Экран, на который вернуться при закрытии (например, меню паузы или титульный экран). */
    public LuminaScreen parent(Screen parent) {
        this.parent = parent;
        return this;
    }

    public LuminaScreen pauseGame(boolean pause) {
        this.pauseGame = pause;
        return this;
    }

    @Override
    protected void init() {
        ui.resize(width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        canvas.begin(graphics, width, height);
        try {
            ui.render(canvas, partialTick);
        } finally {
            canvas.end();
        }
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        ui.pointerMove((float) mouseX, (float) mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return ui.pointerDown((float) mouseX, (float) mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return ui.pointerUp((float) mouseX, (float) mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        ui.pointerMove((float) mouseX, (float) mouseY);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return ui.scroll((float) mouseX, (float) mouseY, 0f, (float) delta) || super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return ui.keyDown(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return ui.keyUp(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return ui.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return pauseGame;
    }

    @Override
    public void onClose() {
        if (parent != null && minecraft != null) {
            minecraft.setScreen(parent);
        } else {
            super.onClose();
        }
    }

    @Override
    public void removed() {
        ui.dispose();
        canvas.dispose();
        super.removed();
    }
}
