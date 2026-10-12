package io.github.ssbaxys.lumina.forge;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ssbaxys.lumina.render.TextureRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL11;

/**
 * Копии кадра для бэкенда: неразмытые снимки (точная маска скруглённого клипа) и размытая копия для
 * {@code Styles.BACKDROP_BLUR}. Основной буфер копируется в свой {@link TextureTarget} через
 * glBlitFramebuffer, размывается ванильной цепочкой {@code minecraft:shaders/post/blur.json}
 * (та же, что у ванильного эффекта наблюдателя) — сам кадр не меняется. Любая ошибка выключает
 * размытие до конца сессии: ядро рисует матовую подложку.
 */
final class McFrameCapture {
    private static final int GL_READ_FRAMEBUFFER = 36008;
    private static final int GL_DRAW_FRAMEBUFFER = 36009;
    private static final int GL_FRAMEBUFFER = 36160;
    private static final ResourceLocation BLUR = new ResourceLocation("shaders/post/blur.json");
    static final int SLOTS = 8;

    /** Текстура-копия кадра; id нужен ядру только как ключ. V перевёрнут (у OpenGL ноль внизу). */
    record FrameTexture(String id, RenderTarget target) implements TextureRef {
    }

    private final TextureTarget[] slots = new TextureTarget[SLOTS];
    private final FrameTexture[] slotRefs = new FrameTexture[SLOTS];
    private TextureTarget blurTarget;
    private FrameTexture blurRef;
    private PostChain blur;
    private boolean blurBroken;
    private boolean copyBroken;

    boolean blurAvailable() {
        return !blurBroken && !copyBroken;
    }

    boolean snapshotsAvailable() {
        return !copyBroken;
    }

    /** Неразмытая копия кадра в слот; {@code null} — не вышло. */
    TextureRef snapshot(int slot) {
        if (copyBroken || slot < 0 || slot >= SLOTS) {
            return null;
        }
        RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
        try {
            TextureTarget t = slots[slot] = ensure(slots[slot], main);
            copy(main, t);
            if (slotRefs[slot] == null) {
                slotRefs[slot] = new FrameTexture("luminaui:frame/snapshot" + slot, t);
            }
            return slotRefs[slot];
        } catch (RuntimeException e) {
            copyBroken = true;
            return null;
        } finally {
            main.bindWrite(true);
        }
    }

    /**
     * Размытая копия кадра. В 1.20.1 у {@link PostChain} нет {@code setUniform}, поэтому радиус берётся из
     * {@code blur.json} (20); {@code radius} не используется.
     */
    TextureRef blurred(float radius) {
        if (!blurAvailable()) {
            return null;
        }
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        try {
            TextureTarget t = blurTarget = ensure(blurTarget, main);
            copy(main, t);
            if (blur == null) {
                blur = new PostChain(mc.getTextureManager(), mc.getResourceManager(), t, BLUR);
                blur.resize(t.width, t.height);
            }
            blur.process(0f);
            opaqueAlpha(t);
            if (blurRef == null) {
                blurRef = new FrameTexture("luminaui:frame/blur", t);
            }
            return blurRef;
        } catch (Exception e) {
            blurBroken = true;
            closeBlur();
            return null;
        } finally {
            main.bindWrite(true);
        }
    }

    /** Цель размера основного буфера; при смене размера окна пересоздаётся вместе с цепочкой размытия. */
    private TextureTarget ensure(TextureTarget t, RenderTarget main) {
        if (t == null) {
            t = new TextureTarget(main.width, main.height, false, Minecraft.ON_OSX);
            t.setFilterMode(GL11.GL_LINEAR);
        } else if (t.width != main.width || t.height != main.height) {
            t.resize(main.width, main.height, Minecraft.ON_OSX);
            if (t == blurTarget && blur != null) {
                blur.resize(main.width, main.height);
            }
        }
        return t;
    }

    private static void copy(RenderTarget from, RenderTarget to) {
        GlStateManager._glBindFramebuffer(GL_READ_FRAMEBUFFER, from.frameBufferId);
        GlStateManager._glBindFramebuffer(GL_DRAW_FRAMEBUFFER, to.frameBufferId);
        GlStateManager._glBlitFrameBuffer(0, 0, from.width, from.height, 0, 0, to.width, to.height,
                GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL_FRAMEBUFFER, 0);
        opaqueAlpha(to);
    }

    /** Альфа кадра бывает любой (её пишет смешивание GUI): делаем копию непрозрачной. */
    private static void opaqueAlpha(RenderTarget t) {
        t.bindWrite(false);
        RenderSystem.colorMask(false, false, false, true);
        RenderSystem.clearColor(0f, 0f, 0f, 1f);
        RenderSystem.clear(GL11.GL_COLOR_BUFFER_BIT, Minecraft.ON_OSX);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.clearColor(0f, 0f, 0f, 0f);
    }

    private void closeBlur() {
        if (blur != null) {
            blur.close();
            blur = null;
        }
    }

    /** Освободить буферы (экран закрыт). */
    void close() {
        closeBlur();
        for (int i = 0; i < SLOTS; i++) {
            if (slots[i] != null) {
                slots[i].destroyBuffers();
                slots[i] = null;
                slotRefs[i] = null;
            }
        }
        if (blurTarget != null) {
            blurTarget.destroyBuffers();
            blurTarget = null;
            blurRef = null;
        }
    }
}
