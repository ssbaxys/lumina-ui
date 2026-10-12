package io.github.ssbaxys.lumina.fx;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.TextureRef;

/**
 * Покадровая анимация: либо вертикальная полоса кадров в одной текстуре (как ванильный огонь
 * fire_0.png 16×512), либо отдельные текстуры на кадр (как explosion_0..15 у частиц).
 */
public final class SpriteSheet {
    private final TextureRef[] textures;
    private int frames;
    private boolean autoFrames;
    private final float fps;

    private SpriteSheet(TextureRef[] textures, int frames, float fps) {
        this.textures = textures;
        this.frames = frames;
        this.fps = fps;
    }

    /**
     * Полоса кадров, число которых берётся из размера картинки (высота / ширина), как делает игра
     * для анимированных текстур. Работает и с ресурспаками, где кадров другое число.
     * {@code fallbackFrames} — если размер узнать нельзя (например, в тестах без игры).
     */
    public static SpriteSheet animated(TextureRef texture, float fps, int fallbackFrames) {
        SpriteSheet sheet = new SpriteSheet(new TextureRef[]{texture}, Math.max(1, fallbackFrames), fps);
        sheet.autoFrames = true;
        return sheet;
    }

    private int frames() {
        if (autoFrames && Lumina.isInitialized()) {
            int[] size = pngSize(Lumina.platform().readResourceBytes(textures[0].id()));
            if (size != null && size[0] > 0 && size[1] >= size[0]) {
                frames = size[1] / size[0];
            }
            autoFrames = false;
        }
        return frames;
    }

    /** Ширина и высота из заголовка PNG (IHDR) или {@code null}. */
    public static int[] pngSize(byte[] png) {
        if (png == null || png.length < 24 || (png[1] & 0xFF) != 'P' || (png[2] & 0xFF) != 'N' || (png[3] & 0xFF) != 'G') {
            return null;
        }
        return new int[]{readInt(png, 16), readInt(png, 20)};
    }

    private static int readInt(byte[] b, int at) {
        return (b[at] & 0xFF) << 24 | (b[at + 1] & 0xFF) << 16 | (b[at + 2] & 0xFF) << 8 | (b[at + 3] & 0xFF);
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
        return frames();
    }

    /** Кадров в секунду при проигрывании по времени ({@link #frameAt}). */
    public float fps() {
        return fps;
    }

    /** Номер кадра в момент {@code seconds} при зацикленном проигрывании. */
    public int frameAt(float seconds) {
        return Math.floorMod((int) (seconds * fps), frames());
    }

    /** Номер кадра по доле жизни 0..1 (для частиц: от первого до последнего кадра). */
    public int frameOf(float progress) {
        return Math.max(0, Math.min(frames() - 1, (int) (progress * frames())));
    }

    public void draw(Canvas canvas, int frame, float x, float y, float w, float h, int tint) {
        drawCropped(canvas, frame, x, y, w, h, 0f, 1f, tint);
    }

    /** Кадр с обрезкой по высоте: показывается доля [vFrom, vTo] кадра (огонь «поднимается» снизу). */
    public void drawCropped(Canvas canvas, int frame, float x, float y, float w, float h, float vFrom, float vTo, int tint) {
        int n = frames();
        int f = Math.floorMod(frame, n);
        if (textures.length == 1) {
            float v0 = (f + vFrom) / n;
            float v1 = (f + vTo) / n;
            canvas.drawTexture(textures[0], x, y, w, h, 0f, v0, 1f, v1, tint);
        } else {
            canvas.drawTexture(textures[f], x, y, w, h, 0f, vFrom, 1f, vTo, tint);
        }
    }

    /** То же, но отражённое по горизонтали (задняя плоскость модели, видимая с обратной стороны). */
    public void drawCroppedMirrored(Canvas canvas, int frame, float x, float y, float w, float h,
                                    float vFrom, float vTo, int tint) {
        int n = frames();
        int f = Math.floorMod(frame, n);
        if (textures.length == 1) {
            canvas.drawTexture(textures[0], x, y, w, h, 1f, (f + vFrom) / n, 0f, (f + vTo) / n, tint);
        } else {
            canvas.drawTexture(textures[f], x, y, w, h, 1f, vFrom, 0f, vTo, tint);
        }
    }
}
