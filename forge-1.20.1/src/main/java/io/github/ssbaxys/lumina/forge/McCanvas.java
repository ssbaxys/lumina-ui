package io.github.ssbaxys.lumina.forge;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.render.AbstractCanvas;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;

/**
 * Бэкенд Canvas для 1.20.1. Простые прямоугольники копятся в один буфер и рисуются одним
 * вызовом до смены состояния; скруглённые фигуры идут через SDF-шейдер.
 */
final class McCanvas extends AbstractCanvas {
    private GuiGraphics graphics;
    private BufferBuilder quadBatch;
    private boolean scissor;

    void begin(GuiGraphics graphics, int width, int height) {
        this.graphics = graphics;
        beginFrame(width, height);
    }

    void end() {
        try {
            flushQuads();
            endFrame();
        } finally {
            if (scissor) {
                graphics.disableScissor();
                scissor = false;
            }
            graphics = null;
        }
    }

    @Override
    public TextMetrics textMetrics() {
        return Lumina.platform().textMetrics();
    }

    private Matrix4f pose() {
        return graphics.pose().last().pose();
    }

    // ---------------------------------------------------------------- прямоугольники (батч)

    @Override
    protected void drawQuad(float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl) {
        if (quadBatch == null) {
            quadBatch = Tesselator.getInstance().getBuilder();
            quadBatch.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        }
        Matrix4f m = pose();
        color(quadBatch.vertex(m, x0, y0, 0f), tl).endVertex();
        color(quadBatch.vertex(m, x0, y1, 0f), bl).endVertex();
        color(quadBatch.vertex(m, x1, y1, 0f), br).endVertex();
        color(quadBatch.vertex(m, x1, y0, 0f), tr).endVertex();
    }

    private void flushQuads() {
        if (quadBatch == null) {
            return;
        }
        BufferBuilder bb = quadBatch;
        quadBatch = null;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferUploader.drawWithShader(bb.end());
    }

    // ---------------------------------------------------------------- SDF-фигуры

    @Override
    protected void drawShape(float x0, float y0, float x1, float y1, float rTL, float rTR, float rBR, float rBL,
                             int tl, int tr, int br, int bl, float stroke, float softness) {
        ShaderInstance shader = LuminaShaders.roundedRect();
        if (shader == null) {
            if (stroke <= 0f && softness <= 0f) {
                drawQuad(x0, y0, x1, y1, tl, tr, br, bl);
            }
            return;
        }
        flushQuads();
        float hw = (x1 - x0) * 0.5f;
        float hh = (y1 - y0) * 0.5f;
        float pad = softness + 1f;
        shader.safeGetUniform("HalfSize").set(hw, hh);
        shader.safeGetUniform("Radii").set(rBR, rTR, rBL, rTL);
        shader.safeGetUniform("Params").set(stroke, softness);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(() -> shader);
        Matrix4f m = pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        color(bb.vertex(m, x0 - pad, y0 - pad, 0f).uv(-hw - pad, -hh - pad), tl).endVertex();
        color(bb.vertex(m, x0 - pad, y1 + pad, 0f).uv(-hw - pad, hh + pad), bl).endVertex();
        color(bb.vertex(m, x1 + pad, y1 + pad, 0f).uv(hw + pad, hh + pad), br).endVertex();
        color(bb.vertex(m, x1 + pad, y0 - pad, 0f).uv(hw + pad, -hh - pad), tr).endVertex();
        BufferUploader.drawWithShader(bb.end());
    }

    // ---------------------------------------------------------------- текст и текстуры

    @Override
    protected void drawTextImpl(String text, float x, float y, float scale, int argb, boolean shadow) {
        flushQuads();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0f);
        if (scale != 1f) {
            pose.scale(scale, scale, 1f);
        }
        graphics.drawString(Minecraft.getInstance().font, text, 0, 0, argb, shadow);
        pose.popPose();
        graphics.flush();
    }

    @Override
    protected void drawTextureImpl(TextureRef texture, float x0, float y0, float x1, float y1,
                                   float u0, float v0, float u1, float v1, int tint) {
        if (!(texture instanceof McTexture tex)) {
            return;
        }
        flushQuads();
        RenderSystem.setShaderTexture(0, tex.location());
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        Matrix4f m = pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        color(bb.vertex(m, x0, y0, 0f).uv(u0, v0), tint).endVertex();
        color(bb.vertex(m, x0, y1, 0f).uv(u0, v1), tint).endVertex();
        color(bb.vertex(m, x1, y1, 0f).uv(u1, v1), tint).endVertex();
        color(bb.vertex(m, x1, y0, 0f).uv(u1, v0), tint).endVertex();
        BufferUploader.drawWithShader(bb.end());
    }

    // ---------------------------------------------------------------- клип

    @Override
    protected void applyClip(float x0, float y0, float x1, float y1) {
        flushQuads();
        graphics.flush();
        if (scissor) {
            graphics.disableScissor();
        }
        graphics.enableScissor((int) Math.floor(x0), (int) Math.floor(y0), (int) Math.ceil(x1), (int) Math.ceil(y1));
        scissor = true;
    }

    @Override
    protected void clearClip() {
        flushQuads();
        graphics.flush();
        if (scissor) {
            graphics.disableScissor();
            scissor = false;
        }
    }

    private static com.mojang.blaze3d.vertex.VertexConsumer color(com.mojang.blaze3d.vertex.VertexConsumer v, int argb) {
        return v.color((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, argb >>> 24);
    }
}
