package io.github.ssbaxys.lumina.forge;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.Images;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

final class McPlatform implements Platform {
    private final TextMetrics metrics = new TextMetrics() {
        @Override
        public float width(String text) {
            return Minecraft.getInstance().font.width(text);
        }

        @Override
        public float lineHeight() {
            return Minecraft.getInstance().font.lineHeight;
        }
    };

    @Override
    public TextMetrics textMetrics() {
        return metrics;
    }

    @Override
    public String getClipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    @Override
    public void setClipboard(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }

    @Override
    public void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void playSound(String soundId, float volume, float pitch) {
        ResourceLocation id = ResourceLocation.tryParse(soundId);
        SoundEvent sound = id == null ? null : BuiltInRegistries.SOUND_EVENT.get(id);
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }

    /** Через менеджер ресурсов игры: учитываются ресурспаки и их перезагрузка (F3+T). */
    @Override
    public byte[] readResourceBytes(String resourceId) {
        ResourceLocation id = ResourceLocation.tryParse(resourceId);
        if (id == null) {
            return null;
        }
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            return Platform.super.readResourceBytes(resourceId);
        }
        try (InputStream in = resource.get().open()) {
            return in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Пиксели → {@link NativeImage} → {@link DynamicTexture} под {@code luminaui:dynamic/<ключ>}. Регистрация
     * в менеджере текстур — на потоке отрисовки; рисуется по идентификатору, как любой ресурс.
     */
    @Override
    public TextureRef createTexture(String key, int width, int height, int[] argb) {
        if (width <= 0 || height <= 0 || argb == null || argb.length < width * height) {
            return null;
        }
        ResourceLocation id = new ResourceLocation(Lumina.MOD_ID, "dynamic/" + Images.safeKey(key));
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int c = argb[y * width + x];
                // NativeImage хранит ABGR: меняем местами красный и синий канал.
                image.setPixelRGBA(x, y, (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16));
            }
        }
        if (RenderSystem.isOnRenderThread()) {
            registerTexture(id, image);
        } else {
            RenderSystem.recordRenderCall(() -> registerTexture(id, image));
        }
        return TextureRef.of(id.toString());
    }

    /** Старая текстура с тем же ключом закрывается менеджером текстур. */
    private static void registerTexture(ResourceLocation id, NativeImage image) {
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
    }
}
