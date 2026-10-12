package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.render.AnimatedTexture;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Images;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Styles;

import java.nio.file.Path;
import java.util.function.IntConsumer;

/**
 * Анимированная картинка (GIF) с управлением проигрыванием: пауза, скорость, повтор, переход к кадру.
 * Кадры сменяются с задержками из файла. Вписывание и скругление — как у {@link Image}.
 *
 * <pre>{@code
 * AnimatedImage.file(Path.of("emotes/party.gif")).size(48, 48).speed(1.5f).loop(AnimatedImage.Loop.FOREVER);
 * }</pre>
 *
 * Обычная (не анимированная) текстура показывается как один кадр.
 */
public class AnimatedImage extends Widget<AnimatedImage> implements Animation {
    /** Сколько раз повторять. */
    public enum Loop {
        /** Как записано в файле (NETSCAPE2.0); без записи — один раз. */
        AS_FILE,
        /** Бесконечно. */
        FOREVER,
        /** Один раз и остановиться на последнем кадре. */
        ONCE
    }

    private TextureRef texture;
    private Image.Fit fit = Image.Fit.CONTAIN;
    private int tint = Color.WHITE;
    private float speed = 1f;
    private boolean playing = true;
    private Loop loop = Loop.FOREVER;
    private float elapsed;
    private boolean ticking;
    private int lastFrame = -1;
    private IntConsumer onFrame;

    public AnimatedImage(TextureRef texture) {
        super("image");
        this.texture = texture;
        setHitTestable(false);
    }

    public static AnimatedImage of(TextureRef texture) {
        return new AnimatedImage(texture);
    }

    /** GIF с диска. */
    public static AnimatedImage file(Path path) {
        return new AnimatedImage(Images.fromFile(path));
    }

    /** GIF из ресурсов "мод:путь". */
    public static AnimatedImage resource(String id) {
        return new AnimatedImage(Images.fromResource(id));
    }

    public TextureRef texture() {
        return texture;
    }

    public AnimatedImage texture(TextureRef texture) {
        this.texture = texture;
        elapsed = 0f;
        lastFrame = -1;
        invalidateLayout();
        start();
        return this;
    }

    /** Анимация или {@code null}, если текстура не анимированная. */
    public AnimatedTexture animation() {
        return texture instanceof AnimatedTexture a ? a : null;
    }

    public AnimatedImage fit(Image.Fit fit) {
        this.fit = fit == null ? Image.Fit.CONTAIN : fit;
        return this;
    }

    public AnimatedImage tint(int argb) {
        this.tint = argb;
        return this;
    }

    public AnimatedImage radius(float r) {
        return style(b -> b.set(Styles.RADIUS, Radii.all(r)));
    }

    /** Скорость проигрывания (1 — как в файле, 2 — вдвое быстрее). */
    public AnimatedImage speed(float speed) {
        this.speed = Math.max(0f, speed);
        return this;
    }

    public float speed() {
        return speed;
    }

    public AnimatedImage loop(Loop loop) {
        this.loop = loop == null ? Loop.FOREVER : loop;
        return this;
    }

    /** Вызывается при смене кадра (номер нового кадра). */
    public AnimatedImage onFrame(IntConsumer onFrame) {
        this.onFrame = onFrame;
        return this;
    }

    public AnimatedImage play() {
        playing = true;
        start();
        return this;
    }

    public AnimatedImage pause() {
        playing = false;
        return this;
    }

    /** Остановить и вернуться к первому кадру. */
    public AnimatedImage stop() {
        playing = false;
        elapsed = 0f;
        return this;
    }

    public boolean isPlaying() {
        return playing;
    }

    /** Перейти к началу кадра {@code index}. */
    public AnimatedImage seek(int index) {
        AnimatedTexture a = animation();
        if (a == null) {
            return this;
        }
        int target = Math.floorMod(index, a.frameCount());
        long ms = 0L;
        for (int i = 0; i < target; i++) {
            ms += a.delay(i);
        }
        elapsed = ms / 1000f;
        return this;
    }

    /** Прошедшее время проигрывания в секундах. */
    public float elapsed() {
        return elapsed;
    }

    /** Номер текущего кадра. */
    public int frame() {
        AnimatedTexture a = animation();
        if (a == null) {
            return 0;
        }
        long ms = (long) (elapsed * 1000f);
        return switch (loop) {
            case AS_FILE -> a.frameAt(ms, true);
            case FOREVER -> a.frameAt(ms % Math.max(1L, a.duration()), true);
            case ONCE -> a.frameAt(ms, false);
        };
    }

    /** Текстура текущего кадра. */
    public TextureRef currentTexture() {
        AnimatedTexture a = animation();
        return a == null ? texture : a.frame(frame());
    }

    @Override
    protected void onAttach() {
        start();
    }

    @Override
    protected void onDetach() {
        ticking = false;
    }

    private void start() {
        if (!ticking && root() != null) {
            ticking = true;
            root().animator().start(this);
        }
    }

    @Override
    public boolean tick(float dt) {
        if (!ticking) {
            return false;
        }
        if (playing && animation() != null) {
            elapsed += dt * speed;
            int f = frame();
            if (f != lastFrame) {
                lastFrame = f;
                if (onFrame != null) {
                    onFrame.accept(f);
                }
            }
        }
        return true;
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        float w = 32f;
        float h = 32f;
        AnimatedTexture a = animation();
        if (a != null && a.width() > 0) {
            w = a.width();
            h = a.height();
        }
        if (Float.isFinite(maxWidth) && maxWidth > 0f && w > maxWidth) {
            h *= maxWidth / w;
            w = maxWidth;
        }
        setContentSize(w, h);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float x = padding().left();
        float y = padding().top();
        float w = width() - padding().horizontal();
        float h = height() - padding().vertical();
        Image.paint(canvas, currentTexture(), x, y, w, h, fit, Image.aspectOf(texture, 0f, 0f),
                style.get(Styles.RADIUS), tint, 1f, style.get(Styles.CLIP_ROUNDED));
    }
}
