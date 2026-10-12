package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.AbstractCanvas;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.ShapePaint;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;

import java.util.ArrayList;
import java.util.List;

/** Платформа и Canvas без Minecraft: 6px на символ, высота строки 9. */
public final class TestSupport {
    public static final TextMetrics METRICS = new TextMetrics() {
        @Override
        public float width(String text) {
            return text.length() * 6f;
        }

        @Override
        public float lineHeight() {
            return 9f;
        }
    };

    private TestSupport() {
    }

    public static void init() {
        Lumina.init(new Platform() {
            @Override
            public TextMetrics textMetrics() {
                return METRICS;
            }

            @Override
            public String getClipboard() {
                return "";
            }

            @Override
            public void setClipboard(String text) {
            }

            @Override
            public void playClickSound() {
            }
        });
    }

    public static final class RecordingCanvas extends AbstractCanvas {
        public final List<String> ops = new ArrayList<>();

        public void begin(float w, float h) {
            ops.clear();
            beginFrame(w, h);
        }

        public void end() {
            endFrame();
        }

        @Override
        protected void drawQuad(float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl) {
            ops.add("quad " + x0 + "," + y0 + "," + x1 + "," + y1);
        }

        @Override
        protected void drawFreeQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                    int c0, int c1, int c2, int c3) {
            ops.add("freequad " + x0 + "," + y0 + " " + x1 + "," + y1 + " " + x2 + "," + y2 + " " + x3 + "," + y3);
        }

        @Override
        protected void drawShape(float x0, float y0, float x1, float y1, float rTL, float rTR, float rBR, float rBL,
                                 int tl, int tr, int br, int bl, float stroke, float softness, ShapePaint paint) {
            ops.add("shape " + x0 + "," + y0 + "," + x1 + "," + y1 + " s=" + stroke + " soft=" + softness
                    + (paint != null ? " grad=" + paint.kind() + "x" + paint.stopCount() : ""));
        }

        @Override
        protected void applyBlendMode(BlendMode mode) {
            ops.add("blend " + mode);
        }

        @Override
        protected void drawTextImpl(String text, float x, float y, float scale, int argb, boolean shadow) {
            ops.add("text " + text + " @" + x + "," + y);
        }

        @Override
        protected void drawTextureImpl(TextureRef texture, float x0, float y0, float x1, float y1,
                                       float u0, float v0, float u1, float v1, int tint) {
            ops.add("tex " + texture.id());
        }

        @Override
        protected void applyClip(float x0, float y0, float x1, float y1) {
            ops.add("clip " + x0 + "," + y0 + "," + x1 + "," + y1);
        }

        @Override
        protected void clearClip() {
            ops.add("noclip");
        }

        @Override
        public TextMetrics textMetrics() {
            return METRICS;
        }
    }
}
