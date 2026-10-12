package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.style.Styles;

/**
 * Круговой индикатор 0..1: дуга с градиентом от {@code from} к {@code to} (по умолчанию от акцента к
 * его светлому оттенку) и подпись в центре. Режим {@link #spinner()} — бесконечное вращение.
 */
public class RingProgress extends Widget<RingProgress> {
    private float progress;
    private float thickness = 4f;
    private Integer from;
    private Integer to;
    private boolean spinner;
    private boolean showPercent = true;
    private AnimatedFloat shown;
    private ReadableSignal<Float> binding;
    private Subscription subscription;

    public RingProgress() {
        super("progress");
        layoutParams().width = Dim.px(40);
        layoutParams().height = Dim.px(40);
    }

    public float progress() {
        return progress;
    }

    public RingProgress progress(float p) {
        progress = Math.max(0f, Math.min(1f, p));
        if (shown != null) {
            shown.animateTo(progress);
        }
        return this;
    }

    public RingProgress thickness(float px) {
        this.thickness = px;
        return this;
    }

    /** Свои цвета начала и конца дуги вместо акцента темы. */
    public RingProgress colors(int from, int to) {
        this.from = from;
        this.to = to;
        return this;
    }

    public RingProgress showPercent(boolean show) {
        this.showPercent = show;
        return this;
    }

    /** Бесконечный спиннер вместо значения. */
    public RingProgress spinner() {
        this.spinner = true;
        this.showPercent = false;
        return this;
    }

    public RingProgress bind(ReadableSignal<Float> signal) {
        this.binding = signal;
        if (root() != null) {
            subscribe();
        }
        return this;
    }

    private void subscribe() {
        if (subscription != null) {
            subscription.close();
        }
        progress(binding.get());
        subscription = binding.subscribe(this::progress);
    }

    @Override
    protected void onAttach() {
        shown = new AnimatedFloat(root().animator(), progress).tween(0.6f, Easing.OUT_CUBIC);
        if (binding != null) {
            subscribe();
        }
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float size = Math.min(width(), height());
        float cx = width() / 2f;
        float cy = height() / 2f;
        float radius = size / 2f - thickness / 2f;
        int accent = style.get(Styles.ACCENT_COLOR);
        int a = from != null ? from : accent;
        int b = to != null ? to : Color.lighten(Color.rotateHue(accent, 40f), 0.1f);
        canvas.strokeCircle(cx, cy, radius + thickness / 2f, thickness, Brush.solid(0x30808080));
        if (spinner) {
            float spin = (Lumina.platform().nanoTime() / 1_000_000L % 1000L) * 0.36f;
            canvas.drawArc(cx, cy, radius, thickness, spin, 270f, Color.withAlpha(a, 0), b);
            return;
        }
        float t = shown != null ? shown.get() : progress;
        if (t > 0.001f) {
            canvas.drawArc(cx, cy, radius, thickness, 0f, 360f * t, a, b);
        }
        if (showPercent) {
            String label = Math.round(t * 100f) + "%";
            float tw = canvas.textMetrics().width(label);
            float lh = canvas.textMetrics().lineHeight();
            canvas.drawText(label, cx - tw / 2f, cy - lh / 2f + 1f, style.get(Styles.TEXT_COLOR), false);
        }
    }
}
