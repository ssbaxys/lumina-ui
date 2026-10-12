package io.github.ssbaxys.lumina.forge;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.github.ssbaxys.lumina.Lumina;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RegisterShadersEvent;

import java.io.IOException;
import java.io.UncheckedIOException;

/** SDF-шейдер скруглённых фигур, теней и обводок (общий GLSL лежит в ресурсах ядра). */
public final class LuminaShaders {
    private static ShaderInstance roundedRect;

    private LuminaShaders() {
    }

    /** {@code null}, пока ресурсы не загружены: тогда Canvas рисует обычные прямоугольники. */
    public static ShaderInstance roundedRect() {
        return roundedRect;
    }

    static void register(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(event.getResourceProvider(),
                    new ResourceLocation(Lumina.MOD_ID, "rounded_rect"), DefaultVertexFormat.POSITION_TEX_COLOR),
                    shader -> roundedRect = shader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to load Lumina UI shaders", e);
        }
    }
}
