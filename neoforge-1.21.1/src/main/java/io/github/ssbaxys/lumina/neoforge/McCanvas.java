package io.github.ssbaxys.lumina.neoforge;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
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
 * Бэкенд Canvas для 1.21.1. Простые прямоугольники копятся в один буфер и рисуются одним
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
            quadBatch = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        }
        Matrix4f m = pose();
        quadBatch.addVertex(m, x0, y0, 0f).setColor(tl);
        quadBatch.addVertex(m, x0, y1, 0f).setColor(bl);
        quadBatch.addVertex(m, x1, y1, 0f).setColor(br);
        quadBatch.addVertex(m, x1, y0, 0f).setColor(tr);
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
        draw(bb);
    }

    private static void draw(BufferBuilder bb) {
        MeshData mesh = bb.build();
        if (mesh != null) {
            BufferUploader.drawWithShader(mesh);
        }
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
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.addVertex(m, x0 - pad, y0 - pad, 0f).setUv(-hw - pad, -hh - pad).setColor(tl);
        bb.addVertex(m, x0 - pad, y1 + pad, 0f).setUv(-hw - pad, hh + pad).setColor(bl);
        bb.addVertex(m, x1 + pad, y1 + pad, 0f).setUv(hw + pad, hh + pad).setColor(br);
        bb.addVertex(m, x1 + pad, y0 - pad, 0f).setUv(hw + pad, -hh - pad).setColor(tr);
        draw(bb);
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
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.addVertex(m, x0, y0, 0f).setUv(u0, v0).setColor(tint);
        bb.addVertex(m, x0, y1, 0f).setUv(u0, v1).setColor(tint);
        bb.addVertex(m, x1, y1, 0f).setUv(u1, v1).setColor(tint);
        bb.addVertex(m, x1, y0, 0f).setUv(u1, v0).setColor(tint);
        draw(bb);
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
}
