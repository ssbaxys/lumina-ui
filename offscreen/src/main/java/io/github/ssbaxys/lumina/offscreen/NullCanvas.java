package io.github.ssbaxys.lumina.offscreen;

import io.github.ssbaxys.lumina.render.AbstractCanvas;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.ShapePaint;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;

/** Канвас, который ничего не рисует: для «прокрутки» анимаций между снимками. */
final class NullCanvas extends AbstractCanvas {
    private final TextMetrics metrics;

    NullCanvas(TextMetrics metrics) {
        this.metrics = metrics;
    }

    void begin(float w, float h) {
        beginFrame(w, h);
    }

    void end() {
        endFrame();
    }

    @Override
    protected void drawQuad(float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl) {
    }

    @Override
    protected void drawFreeQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                int c0, int c1, int c2, int c3) {
    }

    @Override
    protected void drawShape(float x0, float y0, float x1, float y1, float rTL, float rTR, float rBR, float rBL,
                             int tl, int tr, int br, int bl, float stroke, float softness, ShapePaint paint) {
    }

    @Override
    protected void applyBlendMode(BlendMode mode) {
    }

    @Override
    protected void drawTextImpl(String text, float x, float y, float scale, int argb, boolean shadow) {
    }

    @Override
    protected void drawTextureQuadImpl(TextureRef texture, float x0, float y0, float x1, float y1, float x2, float y2,
                                       float x3, float y3, float u0, float v0, float u1, float v1, float u2, float v2,
                                       float u3, float v3, int tint) {
    }

    @Override
    protected void drawItemImpl(String itemId, float x, float y, float size) {
    }

    @Override
    protected void applyClip(float x0, float y0, float x1, float y1) {
    }

    @Override
    protected void clearClip() {
    }

    @Override
    public TextMetrics textMetrics() {
        return metrics;
    }
}
