package io.github.ssbaxys.lumina.neoforge;

import io.github.ssbaxys.lumina.node.UiRoot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.Function;

/**
 * Экран Minecraft, в котором живёт {@link UiRoot}. Весь ввод пересылается в ядро;
 * то, что UI не обработал, уходит в стандартную логику Screen (например, Esc).
 * Окна контейнеров с сервером — {@link LuminaMenuScreen}.
 */
public class LuminaScreen extends Screen {
    private final UiHost host;
    private boolean pauseGame;
    private Screen parent;

    public LuminaScreen(Component title, Function<? super LuminaScreen, UiRoot> factory) {
        super(title);
        // кнопка B геймпада, которую UI не обработал, — как Esc
        this.host = new UiHost(factory.apply(this), () -> {
            if (shouldCloseOnEsc()) {
                onClose();
            }
        });
    }

    public UiRoot ui() {
        return host.ui();
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
        host.resize(width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        host.render(graphics, width, height, partialTick);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        host.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return host.mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return host.mouseReleased(mouseX, mouseY, button) || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        host.mouseMoved(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return host.mouseScrolled(mouseX, mouseY, scrollX, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return host.keyPressed(keyCode, scanCode, modifiers) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        return host.keyReleased(keyCode, scanCode, modifiers) || super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return host.charTyped(codePoint, modifiers) || super.charTyped(codePoint, modifiers);
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
        host.dispose();
        super.removed();
    }
}
