package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorSpace;
import io.github.ssbaxys.lumina.render.ColorStops;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;

/**
 * Текст с эффектами: градиент (в том числе бегущий и радужный), обводка, волна, дрожание
 * и печатная машинка. Всё рисуется посимвольно обычным шрифтом игры.
 *
 * <pre>{@code
 * new FancyText("Lumina UI").scale(2).gradient(0xFFFF8A00, 0xFFE52E71).flow(0.5f).outline(0xFF000000);
 * new FancyText("Привет, мир!").typewriter(20f).onTyped(() -> next());
 * }</pre>
 */
public class FancyText extends Widget<FancyText> implements Animation {
    private String text;
    private float textScale = 1f;
    private ColorStops gradient;
    private float flow;
    private Integer outline;
    private float waveAmp;
    private float waveSpeed = 6f;
    private float shake;
    private float typeSpeed;
    private float typed = Float.MAX_VALUE;
    private String typeSound;
    private Runnable onTyped;
    private float time;
    private boolean ticking;

    public FancyText(String text) {
        super("label");
        this.text = text == null ? "" : text;
        setHitTestable(false);
    }

    public String text() {
        return text;
    }

    public FancyText text(String text) {
        this.text = text == null ? "" : text;
        if (typeSpeed > 0f) {
            typed = 0f;
        }
        invalidateLayout();
        startTicking();
        return this;
    }

    public FancyText scale(float scale) {
        this.textScale = scale;
        invalidateLayout();
        return this;
    }

    /** Градиент по ширине текста (2–8 цветов). */
    public FancyText gradient(int... colors) {
        this.gradient = ColorStops.even(colors);
        return this;
    }

    /** Радужный текст. */
    public FancyText rainbow() {
        return gradient(0xFFFF4D4D, 0xFFFFB84D, 0xFFFFF24D, 0xFF4DFF88, 0xFF4DC3FF, 0xFFA64DFF, 0xFFFF4D4D);
    }

    /** Градиент бежит по тексту: оборотов в секунду. */
    public FancyText flow(float speed) {
        this.flow = speed;
        startTicking();
        return this;
    }

    /** Обводка в 1 пиксель шрифта. */
    public FancyText outline(int argb) {
        this.outline = argb;
        return this;
    }

    /** Буквы покачиваются волной: амплитуда в пикселях шрифта. */
    public FancyText wave(float amplitude, float speed) {
        this.waveAmp = amplitude;
        this.waveSpeed = speed;
        startTicking();
        return this;
    }

    /** Буквы дрожат (злой или испуганный текст). */
    public FancyText shake(float amplitude) {
        this.shake = amplitude;
        startTicking();
        return this;
    }

    /** Печатная машинка: символов в секунду. Запускается заново при {@link #text(String)} и {@link #retype()}. */
    public FancyText typewriter(float charsPerSecond) {
        this.typeSpeed = charsPerSecond;
        this.typed = 0f;
        startTicking();
        return this;
    }

    /** Звук на каждый напечатанный символ, например {@code "minecraft:block.note_block.hat"}. */
    public FancyText typeSound(String soundId) {
        this.typeSound = soundId;
        return this;
    }

    public FancyText onTyped(Runnable callback) {
        this.onTyped = callback;
        return this;
    }

    public FancyText retype() {
        typed = 0f;
        startTicking();
        return this;
    }

    /** Допечатать сразу. */
    public FancyText skip() {
        typed = text.length();
        return this;
    }

    public boolean isTyping() {
        return typeSpeed > 0f && typed < text.length();
    }

    private float effectiveScale() {
        return textScale * style.get(Styles.FONT_SCALE);
    }

    private boolean animated() {
        return flow != 0f || waveAmp != 0f || shake != 0f || isTyping();
    }

    private void startTicking() {
        if (!ticking && root() != null && animated()) {
            ticking = true;
            root().animator().start(this);
        }
    }

    @Override
    protected void onAttach() {
        startTicking();
    }

    @Override
    public boolean tick(float dt) {
        time += dt;
        if (isTyping()) {
            int before = (int) typed;
            typed = Math.min(text.length(), typed + dt * typeSpeed);
            int after = (int) typed;
            if (after > before && typeSound != null && Lumina.isInitialized() && !Character.isWhitespace(text.charAt(after - 1))) {
                Lumina.platform().playSound(typeSound, 0.4f, 0.9f + (after % 5) * 0.05f);
            }
            if (after >= text.length() && onTyped != null) {
                onTyped.run();
            }
        }
        boolean keep = root() != null && animated();
        ticking = keep;
        return keep;
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float k = effectiveScale();
        float extra = outline != null ? 2f : 0f;
        setContentSize((m.width(text) + extra) * k, (m.lineHeight() + extra + Math.abs(waveAmp) * 2f) * k);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        float k = effectiveScale();
        float lh = m.lineHeight() * k;
        float top = padding().top() + (height() - padding().vertical() - lh) / 2f;
        canvas.save();
        canvas.translate(padding().left() + (outline != null ? k : 0f), top);
        canvas.scale(k, k);
        int base = style.get(Styles.TEXT_COLOR);
        boolean shadow = style.get(Styles.TEXT_SHADOW) && outline == null;
        float total = Math.max(1f, m.width(text));
        int visible = typeSpeed > 0f ? (int) Math.min(text.length(), typed) : text.length();
        float x = 0f;
        for (int i = 0; i < visible; i++) {
            String ch = String.valueOf(text.charAt(i));
            float w = m.width(ch);
            float dy = waveAmp != 0f ? (float) Math.sin(time * waveSpeed - i * 0.6f) * waveAmp : 0f;
            float dx = 0f;
            if (shake != 0f) {
                dx = (float) Math.sin(time * 53f + i * 12.9898f) * shake;
                dy += (float) Math.cos(time * 47f + i * 78.233f) * shake;
            }
            int color = base;
            if (gradient != null) {
                float t = (x + w / 2f) / total - time * flow;
                t = t - (float) Math.floor(t);
                color = Color.mulAlpha(gradient.sample(t, ColorSpace.OKLAB), Color.alpha(base) / 255f);
            }
            if (outline != null) {
                for (int oy = -1; oy <= 1; oy++) {
                    for (int ox = -1; ox <= 1; ox++) {
                        if (ox != 0 || oy != 0) {
                            canvas.drawText(ch, x + dx + ox, dy + oy, outline, false);
                        }
                    }
                }
            }
            canvas.drawText(ch, x + dx, dy, color, shadow);
            x += w;
        }
        if (isTyping() && ((int) (time * 3f)) % 2 == 0) {
            canvas.drawText("_", x, 0f, base, shadow);
        }
        canvas.restore();
    }
}
