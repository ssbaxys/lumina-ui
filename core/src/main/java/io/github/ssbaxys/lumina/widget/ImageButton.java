package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;

/**
 * Кнопка-карточка: картинка на всю кнопку (по умолчанию {@link Image.Fit#COVER}) со скруглёнными углами
 * и полосой подписи снизу. При наведении картинка плавно приближается и темнеет. Стиль "image-button".
 *
 * <pre>{@code
 * new ImageButton(TextureRef.of("minecraft:textures/gui/title/background/panorama_0.png"), "Мир", this::play)
 *         .size(140, 90)
 * }</pre>
 */
public class ImageButton extends Widget<ImageButton> {
    private static final float ZOOM = 0.1f;
    private static final int DARKEN = 0x50;

    private TextureRef image;
    private String caption;
    private Runnable onPress;
    private boolean clickSound = true;
    private Image.Fit fit = Image.Fit.COVER;
    private float sourceWidth;
    private float sourceHeight;
    private AnimatedFloat hover;

    public ImageButton(TextureRef image, String caption) {
        this(image, caption, null);
    }

    public ImageButton(TextureRef image, String caption, Runnable onPress) {
        super("image-button");
        this.image = image;
        this.caption = caption == null ? "" : caption;
        this.onPress = onPress;
        setFocusable(true);
        layoutParams().width = Dim.px(120);
        layoutParams().height = Dim.px(80);
    }

    public TextureRef image() {
        return image;
    }

    public ImageButton image(TextureRef image) {
        this.image = image;
        return this;
    }

    public String caption() {
        return caption;
    }

    public ImageButton caption(String caption) {
        this.caption = caption == null ? "" : caption;
        return this;
    }

    public ImageButton onPress(Runnable onPress) {
        this.onPress = onPress;
        return this;
    }

    public ImageButton clickSound(boolean enabled) {
        this.clickSound = enabled;
        return this;
    }

    public ImageButton fit(Image.Fit fit) {
        this.fit = fit == null ? Image.Fit.COVER : fit;
        return this;
    }

    /** Размер исходной картинки (для пропорций, если текстура его не знает). */
    public ImageButton sourceSize(float width, float height) {
        this.sourceWidth = width;
        this.sourceHeight = height;
        return this;
    }

    public void press() {
        if (!isEnabled()) {
            return;
        }
        if (clickSound && Lumina.isInitialized()) {
            Lumina.platform().playClickSound();
        }
        if (onPress != null) {
            onPress.run();
        }
    }

    @Override
    protected void onAttach() {
        hover = new AnimatedFloat(root().animator(), hovered() ? 1f : 0f).tween(0.3f, Easing.OUT_CUBIC);
    }

    @Override
    protected void onStateChanged() {
        if (hover != null) {
            hover.animateTo(hovered() ? 1f : 0f);
        }
    }

    private boolean hovered() {
        return isEnabled() && (hasState(State.HOVERED) || hasState(State.FOCUSED));
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.target() != this) {
            return;
        }
        if (event.type() == PointerEvent.CLICK && ((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
            press();
            event.consume();
        } else if (event.type() == KeyEvent.PRESS) {
            int key = ((KeyEvent) event).key();
            if (key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.SPACE) {
                press();
                event.consume();
            }
        }
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float w = width();
        float h = height();
        float t = hover != null ? hover.get() : 0f;
        Radii r = style.get(Styles.RADIUS);
        Image.paint(canvas, image, 0f, 0f, w, h, fit, Image.aspectOf(image, sourceWidth, sourceHeight), r,
                Color.WHITE, 1f + ZOOM * t);
        if (t > 0.002f) {
            canvas.fillRoundRect(0f, 0f, w, h, r, Brush.solid(Color.withAlpha(Color.BLACK, Math.round(DARKEN * t))));
        }
        if (caption.isEmpty()) {
            return;
        }
        TextMetrics m = canvas.textMetrics();
        float barH = Math.min(h, m.lineHeight() + 9f);
        Radii bottom = new Radii(0f, 0f, r.bottomRight(), r.bottomLeft());
        canvas.fillRoundRect(0f, h - barH, w, barH, bottom, Brush.vertical(0x30000000, 0xC0000000));
        String text = ellipsize(caption, w - 12f, m);
        canvas.drawText(text, 6f, h - barH + (barH - m.lineHeight()) / 2f + 0.5f, style.get(Styles.TEXT_COLOR), true);
    }

    /** Обрезать текст с многоточием, чтобы он поместился в {@code max}. */
    static String ellipsize(String text, float max, TextMetrics m) {
        if (m.width(text) <= max) {
            return text;
        }
        String dots = "...";
        int end = text.length();
        while (end > 0 && m.width(text.substring(0, end) + dots) > max) {
            end--;
        }
        return end == 0 ? "" : text.substring(0, end) + dots;
    }
}
