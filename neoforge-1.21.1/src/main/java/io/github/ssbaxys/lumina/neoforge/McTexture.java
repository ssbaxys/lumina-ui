package io.github.ssbaxys.lumina.neoforge;

import io.github.ssbaxys.lumina.render.TextureRef;
import net.minecraft.resources.ResourceLocation;

/** Текстура Minecraft для {@link io.github.ssbaxys.lumina.render.Canvas#drawTexture}. */
public record McTexture(ResourceLocation location) implements TextureRef {
    public static McTexture of(String namespace, String path) {
        return new McTexture(ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    @Override
    public String id() {
        return location.toString();
    }
}
