package io.github.ssbaxys.lumina.neoforge;

import com.mojang.blaze3d.platform.GlStateManager;
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
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.EntityOptions;
import io.github.ssbaxys.lumina.render.ShapePaint;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;

import java.util.HashMap;
import java.util.Map;

/**
 * Бэкенд Canvas для 1.21.1. Простые прямоугольники и произвольные четырёхугольники копятся в один
 * буфер и рисуются одним вызовом до смены состояния; скруглённые фигуры и градиенты идут через SDF-шейдер.
 */
final class McCanvas extends AbstractCanvas {
    private GuiGraphics graphics;
    private BufferBuilder quadBatch;
    private boolean scissor;
    private BlendMode blendMode = BlendMode.NORMAL;
    private final Matrix4f gradColorsA = new Matrix4f();
    private final Matrix4f gradColorsB = new Matrix4f();
    private final Map<String, ResourceLocation> locations = new HashMap<>();
    private final Map<String, ItemStack> items = new HashMap<>();
    /** После 3D-предмета или сущности в буфере глубины остались их пиксели (см. {@link #clearDepth()}). */
    private boolean depthDirty;
    private final McFrameCapture frames = new McFrameCapture();
    private float clipX0;
    private float clipY0;
    private float clipX1;
    private float clipY1;

    void begin(GuiGraphics graphics, int width, int height) {
        this.graphics = graphics;
        this.depthDirty = false;
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
            blendMode = BlendMode.NORMAL;
        }
    }

    /** Освободить копии кадра (экран закрыт). */
    void dispose() {
        frames.close();
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
        drawFreeQuad(x0, y0, x0, y1, x1, y1, x1, y0, tl, bl, br, tr);
    }

    @Override
    protected void drawFreeQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                int c0, int c1, int c2, int c3) {
        if (quadBatch == null) {
            clearDepth();
            quadBatch = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        }
        Matrix4f m = pose();
        quadBatch.addVertex(m, x0, y0, 0f).setColor(c0);
        quadBatch.addVertex(m, x1, y1, 0f).setColor(c1);
        quadBatch.addVertex(m, x2, y2, 0f).setColor(c2);
        quadBatch.addVertex(m, x3, y3, 0f).setColor(c3);
    }

    private void flushQuads() {
        if (quadBatch == null) {
            return;
        }
        BufferBuilder bb = quadBatch;
        quadBatch = null;
        setupBlend();
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
                             int tl, int tr, int br, int bl, float stroke, float softness, ShapePaint paint) {
        ShaderInstance shader = LuminaShaders.roundedRect();
        if (shader == null) {
            if (stroke <= 0f && softness <= 0f) {
                drawQuad(x0, y0, x1, y1, tl, tr, br, bl);
            }
            return;
        }
        flushQuads();
        clearDepth();
        float hw = (x1 - x0) * 0.5f;
        float hh = (y1 - y0) * 0.5f;
        float pad = softness + 1f;
        shader.safeGetUniform("HalfSize").set(hw, hh);
        shader.safeGetUniform("Radii").set(rBR, rTR, rBL, rTL);
        shader.safeGetUniform("Params").set(stroke, softness);
        applyPaint(shader, paint);
        setupBlend();
        RenderSystem.setShader(() -> shader);
        Matrix4f m = pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.addVertex(m, x0 - pad, y0 - pad, 0f).setUv(-hw - pad, -hh - pad).setColor(tl);
        bb.addVertex(m, x0 - pad, y1 + pad, 0f).setUv(-hw - pad, hh + pad).setColor(bl);
        bb.addVertex(m, x1 + pad, y1 + pad, 0f).setUv(hw + pad, hh + pad).setColor(br);
        bb.addVertex(m, x1 + pad, y0 - pad, 0f).setUv(hw + pad, -hh - pad).setColor(tr);
        draw(bb);
    }

    @Override
    protected boolean supportsShapes() {
        return LuminaShaders.roundedRect() != null;
    }

    private void applyPaint(ShaderInstance shader, ShapePaint p) {
        if (p == null || p.kind() == ShapePaint.VERTEX) {
            shader.safeGetUniform("GradMode").set(0f, 0f, 0f, 0f);
            return;
        }
        shader.safeGetUniform("GradMode").set((float) p.kind(), (float) p.stopCount(), (float) p.space(),
                p.repeating() ? 1f : 0f);
        shader.safeGetUniform("GradGeom").set(p.geometry(0), p.geometry(1), p.geometry(2), p.geometry(3));
        shader.safeGetUniform("GradExtra").set(p.offset(), 0f, 0f, 0f);
        shader.safeGetUniform("GradColorsA").set(stopColors(p, 0, gradColorsA));
        shader.safeGetUniform("GradColorsB").set(stopColors(p, 4, gradColorsB));
        shader.safeGetUniform("GradStopsA").set(p.position(0), p.position(1), p.position(2), p.position(3));
        shader.safeGetUniform("GradStopsB").set(p.position(4), p.position(5), p.position(6), p.position(7));
    }

    /** Четыре опоры в матрицу: столбец i — RGBA опоры {@code first + i} (JOML хранит по столбцам). */
    private static Matrix4f stopColors(ShapePaint p, int first, Matrix4f out) {
        return out.set(
                p.color(first, 0), p.color(first, 1), p.color(first, 2), p.color(first, 3),
                p.color(first + 1, 0), p.color(first + 1, 1), p.color(first + 1, 2), p.color(first + 1, 3),
                p.color(first + 2, 0), p.color(first + 2, 1), p.color(first + 2, 2), p.color(first + 2, 3),
                p.color(first + 3, 0), p.color(first + 3, 1), p.color(first + 3, 2), p.color(first + 3, 3));
    }

    // ---------------------------------------------------------------- смешивание

    @Override
    protected void applyBlendMode(BlendMode mode) {
        flushQuads();
        blendMode = mode;
        if (mode == BlendMode.NORMAL) {
            RenderSystem.defaultBlendFunc();
        }
    }

    private void setupBlend() {
        RenderSystem.enableBlend();
        switch (blendMode) {
            case ADD -> RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            case MULTIPLY -> RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            case SCREEN -> RenderSystem.blendFuncSeparate(
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR,
                    GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            default -> RenderSystem.defaultBlendFunc();
        }
    }

    // ---------------------------------------------------------------- текст и текстуры

    @Override
    protected void drawTextImpl(String text, float x, float y, float scale, int argb, boolean shadow) {
        flushQuads();
        clearDepth();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0f);
        if (scale != 1f) {
            pose.scale(scale, scale, 1f);
        }
        graphics.drawString(Minecraft.getInstance().font, text, 0, 0, argb, shadow);
        pose.popPose();
        graphics.flush();
        if (text.indexOf('§') >= 0) {
            // Подчёркивание и зачёркивание шрифт кладёт чуть выше z=0: следующий слой иначе под ними.
            depthDirty = true;
        }
    }

    @Override
    protected void drawTextureQuadImpl(TextureRef texture, float x0, float y0, float x1, float y1,
                                       float x2, float y2, float x3, float y3,
                                       float u0, float v0, float u1, float v1,
                                       float u2, float v2, float u3, float v3, int tint) {
        if (texture instanceof McFrameCapture.FrameTexture frame) {
            // копия кадра: у OpenGL V растёт снизу вверх
            v0 = 1f - v0;
            v1 = 1f - v1;
            v2 = 1f - v2;
            v3 = 1f - v3;
            flushQuads();
            RenderSystem.setShaderTexture(0, frame.target().getColorTextureId());
        } else {
            ResourceLocation location = location(texture);
            if (location == null) {
                return;
            }
            flushQuads();
            RenderSystem.setShaderTexture(0, location);
        }
        clearDepth();
        setupBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        Matrix4f m = pose();
        BufferBuilder bb = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        bb.addVertex(m, x0, y0, 0f).setUv(u0, v0).setColor(tint);
        bb.addVertex(m, x1, y1, 0f).setUv(u1, v1).setColor(tint);
        bb.addVertex(m, x2, y2, 0f).setUv(u2, v2).setColor(tint);
        bb.addVertex(m, x3, y3, 0f).setUv(u3, v3).setColor(tint);
        draw(bb);
    }

    private ResourceLocation location(TextureRef texture) {
        if (texture instanceof McTexture tex) {
            return tex.location();
        }
        return locations.computeIfAbsent(texture.id(), ResourceLocation::tryParse);
    }

    @Override
    protected void drawItemImpl(String itemId, float x, float y, float size) {
        ItemStack stack = items.computeIfAbsent(itemId, id -> {
            ResourceLocation rl = ResourceLocation.tryParse(id);
            return rl == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(rl));
        });
        renderStack(stack, x, y, size);
    }

    @Override
    protected void drawItemImpl(String itemId, float x, float y, float size, boolean glint) {
        if (!glint) {
            drawItemImpl(itemId, x, y, size);
            return;
        }
        renderStack(McModels.stack(itemId, true), x, y, size);
    }

    private void renderStack(ItemStack stack, float x, float y, float size) {
        if (stack.isEmpty()) {
            return;
        }
        flushQuads();
        clearDepth();
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x, y, 0f);
        pose.scale(size / 16f, size / 16f, 1f);
        graphics.renderItem(stack, 0, 0);
        pose.popPose();
        graphics.flush();
        depthDirty = true;
    }

    @Override
    protected void drawItem3dImpl(String itemId, float x, float y, float size, float yaw, float pitch, boolean glint) {
        flushQuads();
        // Крупная модель уходит по глубине и за z=0: чужая глубина под ней (фон экрана) её бы обрезала.
        depthDirty = true;
        clearDepth();
        McModels.drawItem3d(graphics, itemId, glint, x, y, size, yaw, pitch);
        depthDirty = true;
    }

    @Override
    protected void drawEntityImpl(String entityTypeId, float x, float y, float size, float yaw, float pitch,
                                  EntityOptions options) {
        flushQuads();
        // Крупная модель уходит по глубине и за z=0: чужая глубина под ней (фон экрана) её бы обрезала.
        depthDirty = true;
        clearDepth();
        McModels.drawEntity(graphics, entityTypeId, x, y, size, yaw, pitch, options);
        depthDirty = true;
    }

    /**
     * Предметы рисуются на z=150, сущности около z=50, а текст — на z=0 с проверкой глубины, поэтому без
     * очистки число поверх предмета (количество в слоте) пропало бы. Глубина нужна только внутри одной модели,
     * так что перед следующим текстом или моделью буфер глубины очищается (в пределах текущего клипа).
     */
    private void clearDepth() {
        if (depthDirty) {
            depthDirty = false;
            graphics.flush();
            RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        }
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
        clipX0 = x0;
        clipY0 = y0;
        clipX1 = x1;
        clipY1 = y1;
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

    // ---------------------------------------------------------------- копии кадра (размытие, маски)

    @Override
    protected TextureRef captureBackdrop(float radius) {
        if (!frames.blurAvailable()) {
            return null;
        }
        float scale = (float) Minecraft.getInstance().getWindow().getGuiScale();
        return capture(-1, radius * scale);
    }

    @Override
    protected boolean backdropBlurAvailable() {
        return frames.blurAvailable();
    }

    @Override
    protected TextureRef captureSnapshot(int slot) {
        return frames.snapshotsAvailable() ? capture(slot, 0f) : null;
    }

    @Override
    protected int snapshotSlots() {
        return frames.snapshotsAvailable() ? McFrameCapture.SLOTS : 0;
    }

    /**
     * Дорисовать накопленное, снять ножницы (они режут и копирование), скопировать кадр и вернуть клип.
     * {@code slot} &lt; 0 — размытая копия.
     */
    private TextureRef capture(int slot, float radius) {
        flushQuads();
        graphics.flush();
        boolean hadScissor = scissor;
        if (scissor) {
            graphics.disableScissor();
            scissor = false;
        }
        try {
            return slot < 0 ? frames.blurred(radius) : frames.snapshot(slot);
        } finally {
            RenderSystem.defaultBlendFunc();
            if (hadScissor) {
                applyClip(clipX0, clipY0, clipX1, clipY1);
            }
        }
    }
}
