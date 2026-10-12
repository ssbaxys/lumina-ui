package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;

/** Индикатор прогресса 0..1 с плавным переходом к новому значению. Можно привязать к сигналу. */
public class ProgressBar extends Widget<ProgressBar> {
    private float progress;
    private AnimatedFloat shown;
    private ReadableSignal<Float> binding;
    private Subscription subscription;

    public ProgressBar() {
        super("progress");
        layoutParams().width = Dim.FILL;
        layoutParams().height = Dim.px(6);
    }

    public float progress() {
        return progress;
    }

    public ProgressBar progress(float p) {
        progress = Math.max(0f, Math.min(1f, p));
        if (shown != null) {
            shown.animateTo(progress);
        }
        return this;
    }

    public ProgressBar bind(ReadableSignal<Float> signal) {
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
        shown = new AnimatedFloat(root().animator(), progress).tween(0.45f, Easing.OUT_CUBIC);
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
        float t = shown != null ? shown.get() : progress;
        if (t <= 0f) {
            return;
        }
        int accent = style.get(Styles.ACCENT_COLOR);
        Radii r = style.get(Styles.RADIUS);
        canvas.fillRoundRect(0, 0, width() * t, height(), r,
                Brush.horizontal(Color.lerp(accent, Color.WHITE, 0.25f), accent));
    }
}
