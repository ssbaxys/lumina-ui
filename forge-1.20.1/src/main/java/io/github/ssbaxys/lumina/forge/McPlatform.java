package io.github.ssbaxys.lumina.forge;

import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.TextMetrics;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

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
}
