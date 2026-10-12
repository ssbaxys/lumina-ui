package io.github.ssbaxys.lumina.fx;

import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.TextureRef;

/**
 * Покадровая анимация: либо вертикальная полоса кадров в одной текстуре (как ванильный огонь
 * fire_0.png 16×512), либо отдельные текстуры на кадр (как explosion_0..15 у частиц).
 */
public final class SpriteSheet {
    private final TextureRef[] textures;
    private final int frames;
    private final float fps;

    private SpriteSheet(TextureRef[] textures, int frames, float fps) {
        this.textures = textures;
        this.frames = frames;
        this.fps = fps;
    }

    /** Полоса из {@code frames} кадров сверху вниз в одной текстуре. */
    public static SpriteSheet strip(TextureRef texture, int frames, float fps) {
        return new SpriteSheet(new TextureRef[]{texture}, Math.max(1, frames), fps);
    }

    /** По текстуре на кадр. */
    public static SpriteSheet frames(float fps, TextureRef... textures) {
        return new SpriteSheet(textures.clone(), textures.length, fps);
    }

    /** Один неподвижный кадр. */
    public static SpriteSheet single(TextureRef texture) {
        return new SpriteSheet(new TextureRef[]{texture}, 1, 1f);
    }

    public int frameCount() {
        return frames;
    }

    /** Кадров в секунду при проигрывании по времени ({@link #frameAt}). */
    public float fps() {
        return fps;
    }

    /** Номер кадра в момент {@code seconds} при зацикленном проигрывании. */
    public int frameAt(float seconds) {
        return Math.floorMod((int) (seconds * fps), frames);
    }

    /** Номер кадра по доле жизни 0..1 (для частиц: от первого до последнего кадра). */
    public int frameOf(float progress) {
        return Math.max(0, Math.min(frames - 1, (int) (progress * frames)));
    }

    public void draw(Canvas canvas, int frame, float x, float y, float w, float h, int tint) {
        drawCropped(canvas, frame, x, y, w, h, 0f, 1f, tint);
    }

    /** Кадр с обрезкой по высоте: показывается доля [vFrom, vTo] кадра (огонь «поднимается» снизу). */
    public void drawCropped(Canvas canvas, int frame, float x, float y, float w, float h, float vFrom, float vTo, int tint) {
        int f = Math.floorMod(frame, frames);
        if (textures.length == 1) {
            float v0 = (f + vFrom) / frames;
            float v1 = (f + vTo) / frames;
            canvas.drawTexture(textures[0], x, y, w, h, 0f, v0, 1f, v1, tint);
        } else {
            canvas.drawTexture(textures[f], x, y, w, h, 0f, vFrom, 1f, vTo, tint);
        }
    }
}
