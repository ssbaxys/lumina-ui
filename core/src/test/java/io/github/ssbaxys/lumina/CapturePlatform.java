package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.AbstractCanvas;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.ShapePaint;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Тестовая платформа, которая запоминает звуки и озвучку, со своими часами и каталогом данных. */
class CapturePlatform implements Platform {
    final List<String> sounds = new ArrayList<>();
    final List<String> narrated = new ArrayList<>();
    int clicks;
    long nanos = 1_000_000_000L;
    Path dataDir;
    String clipboard = "";

    static CapturePlatform install() {
        CapturePlatform p = new CapturePlatform();
        Lumina.init(p);
        return p;
    }

    void advance(float seconds) {
        nanos += (long) (seconds * 1_000_000_000L);
    }

    @Override
    public TextMetrics textMetrics() {
        return TestSupport.METRICS;
    }

    @Override
    public String getClipboard() {
        return clipboard;
    }

    @Override
    public void setClipboard(String text) {
        clipboard = text;
    }

    @Override
    public void playClickSound() {
        clicks++;
    }

    @Override
    public void playSound(String soundId, float volume, float pitch) {
        sounds.add(soundId);
    }

    @Override
    public void narrate(String text) {
        narrated.add(text);
    }

    @Override
    public long nanoTime() {
        return nanos;
    }

    @Override
    public Path dataDirectory() {
        return dataDir != null ? dataDir : Platform.super.dataDirectory();
    }

    /** Canvas, запоминающий цвета фигур и текста (после фильтра и прозрачности). */
    static final class ColorCanvas extends AbstractCanvas {
        final List<Integer> colors = new ArrayList<>();
        final List<String> ops = new ArrayList<>();

        void begin(float w, float h) {
            colors.clear();
            ops.clear();
            beginFrame(w, h);
        }

        void end() {
            endFrame();
        }

        @Override
        protected void drawQuad(float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl) {
            colors.add(tl);
            ops.add("quad " + x0 + "," + y0 + "," + x1 + "," + y1);
        }

        @Override
        protected void drawFreeQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                    int c0, int c1, int c2, int c3) {
            colors.add(c0);
            ops.add("freequad");
        }

        @Override
        protected void drawShape(float x0, float y0, float x1, float y1, float rTL, float rTR, float rBR, float rBL,
                                 int tl, int tr, int br, int bl, float stroke, float softness, ShapePaint paint) {
            colors.add(tl);
            ops.add("shape " + x0 + "," + y0 + "," + x1 + "," + y1 + " s=" + stroke);
        }

        @Override
        protected void applyBlendMode(BlendMode mode) {
        }

        @Override
        protected void drawTextImpl(String text, float x, float y, float scale, int argb, boolean shadow) {
            colors.add(argb);
            ops.add("text " + text + " @" + x + "," + y + " x" + scale);
        }

        @Override
        protected void drawTextureQuadImpl(TextureRef texture, float x0, float y0, float x1, float y1,
                                           float x2, float y2, float x3, float y3,
                                           float u0, float v0, float u1, float v1,
                                           float u2, float v2, float u3, float v3, int tint) {
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
            return TestSupport.METRICS;
        }
    }
}
