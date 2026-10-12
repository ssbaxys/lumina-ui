package io.github.ssbaxys.lumina.render;

import java.util.Arrays;

/**
 * Анимированная картинка (GIF): по текстуре на кадр и длительность каждого кадра. Сама тоже {@link TextureRef}
 * (идентификатор — первого кадра), поэтому её можно отдать любому месту, где ждут текстуру: там будет первый
 * кадр, а {@code Image} и {@code AnimatedImage} проигрывают её целиком.
 *
 * <pre>{@code
 * TextureRef gif = Images.fromFile(Path.of("loading.gif"));   // AnimatedTexture, если кадров больше одного
 * new Image(gif).size(64, 64);                                 // играет сама
 * }</pre>
 */
public final class AnimatedTexture implements TextureRef {
    private final int width;
    private final int height;
    private final TextureRef[] frames;
    private final int[] delays;
    private final int loopCount;
    private final long duration;

    /**
     * @param delaysMs  длительность каждого кадра в миллисекундах
     * @param loopCount сколько раз проиграть; 0 — бесконечно
     */
    public AnimatedTexture(int width, int height, TextureRef[] frames, int[] delaysMs, int loopCount) {
        if (frames.length == 0 || frames.length != delaysMs.length) {
            throw new IllegalArgumentException("frames and delays must be non-empty and of equal length");
        }
        this.width = width;
        this.height = height;
        this.frames = frames.clone();
        this.delays = delaysMs.clone();
        this.loopCount = Math.max(0, loopCount);
        long total = 0L;
        for (int i = 0; i < delays.length; i++) {
            delays[i] = Math.max(1, delays[i]);
            total += delays[i];
        }
        this.duration = total;
    }

    @Override
    public String id() {
        return frames[0].id();
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** Отношение ширины к высоте (1, если размер неизвестен). */
    public float aspect() {
        return width > 0 && height > 0 ? width / (float) height : 1f;
    }

    public int frameCount() {
        return frames.length;
    }

    public TextureRef frame(int index) {
        return frames[Math.floorMod(index, frames.length)];
    }

    /** Длительность кадра в миллисекундах. */
    public int delay(int index) {
        return delays[Math.floorMod(index, delays.length)];
    }

    /** Длительность одного проигрывания в миллисекундах. */
    public long duration() {
        return duration;
    }

    /** Сколько раз проигрывать (0 — бесконечно), как записано в файле. */
    public int loopCount() {
        return loopCount;
    }

    /**
     * Номер кадра через {@code ms} от начала. Если повторов конечное число и они закончились — последний кадр.
     *
     * @param loop {@code false} — играть один раз, независимо от {@link #loopCount()}
     */
    public int frameAt(long ms, boolean loop) {
        if (ms <= 0L || frames.length == 1) {
            return 0;
        }
        int loops = loop ? loopCount : 1;
        if (loops > 0 && ms >= duration * loops) {
            return frames.length - 1;
        }
        long t = ms % duration;
        for (int i = 0; i < delays.length; i++) {
            if (t < delays[i]) {
                return i;
            }
            t -= delays[i];
        }
        return frames.length - 1;
    }

    /** Номер кадра через {@code ms} при проигрывании, как записано в файле ({@link #loopCount()}). */
    public int frameAt(long ms) {
        return frameAt(ms, true);
    }

    @Override
    public String toString() {
        return "AnimatedTexture[" + width + "x" + height + ", " + frames.length + " frames, " + duration + " ms, delays="
                + Arrays.toString(delays) + "]";
    }
}
