package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.render.AnimatedTexture;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ImageTexture;
import io.github.ssbaxys.lumina.render.Images;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Styles;

import java.nio.file.Path;
import java.util.Locale;

/**
 * Картинка: {@link TextureRef} вписывается в размер виджета по правилу {@link Fit}. Скругление углов —
 * из {@link Styles#RADIUS} (картинка обрезается по скруглённой фигуре). Без заданного размера виджет
 * берёт размер самой картинки (если он известен, см. {@link ImageTexture}). Анимированный GIF
 * ({@link AnimatedTexture}) проигрывается сам с задержками кадров из файла; управлять проигрыванием —
 * {@link AnimatedImage}. Со стилем {@link Styles#CLIP_ROUNDED} углы режутся точной маской бэкенда
 * ({@code Canvas.clipRoundRect}), если он её умеет.
 *
 * <pre>{@code
 * Image.file(Path.of("screenshots/last.png")).fit(Image.Fit.COVER).size(160, 90).radius(8)
 * }</pre>
 */
public class Image extends Widget<Image> implements Animation {
    /** Как вписать картинку в прямоугольник. */
    public enum Fit {
        /** Целиком, с сохранением пропорций (могут остаться поля). */
        CONTAIN,
        /** Заполнить целиком с сохранением пропорций (лишнее обрезается). */
        COVER,
        /** Растянуть без сохранения пропорций. */
        STRETCH;

        /** "contain", "cover", "fill"/"stretch" → значение; неизвестное — CONTAIN. */
        public static Fit parse(String s) {
            if (s == null) {
                return CONTAIN;
            }
            return switch (s.trim().toLowerCase(Locale.ROOT)) {
                case "cover" -> COVER;
                case "fill", "stretch" -> STRETCH;
                default -> CONTAIN;
            };
        }
    }

    private static final float DEFAULT_SIZE = 32f;
    private static final int PLACEHOLDER = 0x30808080;

    private TextureRef texture;
    private Fit fit = Fit.CONTAIN;
    private int tint = Color.WHITE;
    private float sourceWidth;
    private float sourceHeight;
    private float elapsed;
    private boolean playing;

    public Image(TextureRef texture) {
        super("image");
        this.texture = texture;
        setHitTestable(false);
    }

    public static Image of(TextureRef texture) {
        return new Image(texture);
    }

    /** Картинка с диска (PNG, JPG, GIF, BMP). */
    public static Image file(Path path) {
        return new Image(Images.fromFile(path));
    }

    /** Картинка из ресурсов "мод:путь". */
    public static Image resource(String id) {
        return new Image(Images.fromResource(id));
    }

    public TextureRef texture() {
        return texture;
    }

    public Image texture(TextureRef texture) {
        this.texture = texture;
        elapsed = 0f;
        invalidateLayout();
        startPlayback();
        return this;
    }

    /** Текстура текущего кадра (для GIF — кадр по прошедшему времени). */
    public TextureRef currentTexture() {
        if (texture instanceof AnimatedTexture a) {
            return a.frame(a.frameAt((long) (elapsed * 1000f)));
        }
        return texture;
    }

    @Override
    protected void onAttach() {
        startPlayback();
    }

    @Override
    protected void onDetach() {
        playing = false;
    }

    private void startPlayback() {
        if (!playing && root() != null && texture instanceof AnimatedTexture a && a.frameCount() > 1) {
            playing = true;
            root().animator().start(this);
        }
    }

    @Override
    public boolean tick(float dt) {
        if (!playing || !(texture instanceof AnimatedTexture)) {
            playing = false;
            return false;
        }
        elapsed += dt;
        return true;
    }

    public Fit fit() {
        return fit;
    }

    public Image fit(Fit fit) {
        this.fit = fit == null ? Fit.CONTAIN : fit;
        return this;
    }

    /** Цвет-множитель (белый — без изменений). */
    public Image tint(int argb) {
        this.tint = argb;
        return this;
    }

    /** Размер исходной картинки в пикселях, если текстура его не знает (нужен для пропорций). */
    public Image sourceSize(float width, float height) {
        this.sourceWidth = width;
        this.sourceHeight = height;
        invalidateLayout();
        return this;
    }

    /** Скругление углов (то же, что {@code style(b -> b.set(Styles.RADIUS, Radii.all(r)))}). */
    public Image radius(float r) {
        return style(b -> b.set(Styles.RADIUS, Radii.all(r)));
    }

    /** Отношение ширины к высоте исходной картинки (1, если неизвестно). */
    public float aspect() {
        return aspectOf(texture, sourceWidth, sourceHeight);
    }

    static float aspectOf(TextureRef texture, float sourceWidth, float sourceHeight) {
        if (sourceWidth > 0f && sourceHeight > 0f) {
            return sourceWidth / sourceHeight;
        }
        if (texture instanceof AnimatedTexture at) {
            return at.aspect();
        }
        return texture instanceof ImageTexture it ? it.aspect() : 1f;
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        float w = sourceWidth;
        float h = sourceHeight;
        if ((w <= 0f || h <= 0f) && texture instanceof ImageTexture it && it.width() > 0) {
            w = it.width();
            h = it.height();
        } else if ((w <= 0f || h <= 0f) && texture instanceof AnimatedTexture at && at.width() > 0) {
            w = at.width();
            h = at.height();
        }
        if (w <= 0f || h <= 0f) {
            w = DEFAULT_SIZE;
            h = DEFAULT_SIZE;
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
        paint(canvas, currentTexture(), x, y, w, h, fit, aspect(), style.get(Styles.RADIUS), tint, 1f,
                style.get(Styles.CLIP_ROUNDED));
    }

    /**
     * Нарисовать картинку в прямоугольнике по правилу {@code fit}, со скруглением и увеличением {@code zoom}
     * (≥ 1, от центра, без выхода за границы). Без текстуры — серая заглушка.
     */
    public static void paint(Canvas canvas, TextureRef texture, float x, float y, float w, float h,
                             Fit fit, float aspect, Radii radii, int tint, float zoom) {
        paint(canvas, texture, x, y, w, h, fit, aspect, radii, tint, zoom, false);
    }

    /**
     * То же; {@code exactMask} — скругление точной маской {@code Canvas.clipRoundRect} (если бэкенд её умеет),
     * а не нарезкой на полосы.
     */
    public static void paint(Canvas canvas, TextureRef texture, float x, float y, float w, float h,
                             Fit fit, float aspect, Radii radii, int tint, float zoom, boolean exactMask) {
        if (w <= 0f || h <= 0f) {
            return;
        }
        if (texture == null) {
            canvas.fillRoundRect(x, y, w, h, radii, Brush.solid(PLACEHOLDER));
            return;
        }
        float u0 = 0f;
        float v0 = 0f;
        float u1 = 1f;
        float v1 = 1f;
        float box = w / h;
        float a = aspect > 0f ? aspect : 1f;
        if (fit == Fit.CONTAIN) {
            if (a > box) {
                float dh = w / a;
                y += (h - dh) / 2f;
                h = dh;
            } else {
                float dw = h * a;
                x += (w - dw) / 2f;
                w = dw;
            }
        } else if (fit == Fit.COVER) {
            if (a > box) {
                float visible = box / a;
                u0 = (1f - visible) / 2f;
                u1 = 1f - u0;
            } else {
                float visible = a / box;
                v0 = (1f - visible) / 2f;
                v1 = 1f - v0;
            }
        }
        if (zoom > 1f) {
            float cu = (u0 + u1) / 2f;
            float cv = (v0 + v1) / 2f;
            float hu = (u1 - u0) / (2f * zoom);
            float hv = (v1 - v0) / (2f * zoom);
            u0 = cu - hu;
            u1 = cu + hu;
            v0 = cv - hv;
            v1 = cv + hv;
        }
        if (exactMask && radii != null && !radii.isZero() && canvas.supportsRoundClip()) {
            canvas.save();
            canvas.clipRoundRect(x, y, w, h, radii);
            canvas.drawTexture(texture, x, y, w, h, u0, v0, u1, v1, tint);
            canvas.restore();
        } else {
            canvas.drawTextureRounded(texture, x, y, w, h, u0, v0, u1, v1, radii, tint);
        }
    }
}
