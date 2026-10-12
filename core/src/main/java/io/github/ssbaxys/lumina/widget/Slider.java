package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Spring;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;

/** Ползунок значения в диапазоне. Мышь (нажатие и перетаскивание), колесо и стрелки клавиатуры. */
public class Slider extends Widget<Slider> {
    private static final float KNOB = 10f;
    private static final Shadow KNOB_SHADOW = new Shadow(0, 1, 3, 0, 0x70000000);

    private final Signal<Float> value;
    private final float min;
    private final float max;
    private float step;
    private boolean dragging;
    private AnimatedFloat shown;
    private AnimatedFloat knobScale;
    private Subscription subscription;

    public Slider(Signal<Float> value, float min, float max) {
        super("slider");
        this.value = value;
        this.min = min;
        this.max = max;
        setFocusable(true);
        layoutParams().width = Dim.px(120);
        layoutParams().height = Dim.px(14);
    }

    public Slider step(float step) {
        this.step = step;
        return this;
    }

    public Signal<Float> value() {
        return value;
    }

    private float fraction() {
        float v = value.peek();
        return max > min ? Math.max(0f, Math.min(1f, (v - min) / (max - min))) : 0f;
    }

    private void set(float v) {
        if (!isEnabled()) {
            return;
        }
        v = Math.max(min, Math.min(max, v));
        if (step > 0f) {
            v = min + Math.round((v - min) / step) * step;
        }
        value.set(v);
    }

    private void setFromX(float localX) {
        float track = width() - KNOB;
        set(min + Math.max(0f, Math.min(1f, (localX - KNOB / 2f) / Math.max(track, 1f))) * (max - min));
    }

    private float keyStep() {
        return step > 0f ? step : (max - min) / 20f;
    }

    @Override
    protected void onAttach() {
        shown = new AnimatedFloat(root().animator(), fraction()).spring(Spring.SNAPPY);
        knobScale = new AnimatedFloat(root().animator(), 1f).spring(Spring.SNAPPY);
        subscription = value.subscribe(v -> shown.animateTo(fraction()));
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    @Override
    protected void onStateChanged() {
        if (knobScale != null) {
            knobScale.animateTo(dragging ? 1.3f : hasState(State.HOVERED) ? 1.15f : 1f);
        }
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.PRESS && ((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
            dragging = true;
            onStateChanged();
            setFromX(((PointerEvent) event).localX());
            event.consume();
        } else if (event.type() == PointerEvent.DRAG && dragging) {
            setFromX(((PointerEvent) event).localX());
            event.consume();
        } else if (event.type() == PointerEvent.RELEASE) {
            dragging = false;
            onStateChanged();
        } else if (event.type() == ScrollEvent.SCROLL) {
            set(value.peek() + Math.signum(((ScrollEvent) event).deltaY()) * keyStep());
            event.consume();
            event.stopPropagation();
        } else if (event.type() == KeyEvent.PRESS) {
            int key = ((KeyEvent) event).key();
            if (key == Keys.LEFT || key == Keys.DOWN) {
                set(value.peek() - keyStep());
                event.consume();
            } else if (key == Keys.RIGHT || key == Keys.UP) {
                set(value.peek() + keyStep());
                event.consume();
            }
        }
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float t = shown != null ? shown.get() : fraction();
        float w = width();
        float cy = height() / 2f;
        int accent = style.get(Styles.ACCENT_COLOR);
        Radii track = Radii.all(2f);
        canvas.fillRoundRect(KNOB / 2f, cy - 2f, w - KNOB, 4f, track, Brush.solid(0x50808080));
        canvas.fillRoundRect(KNOB / 2f, cy - 2f, (w - KNOB) * t, 4f, track,
                Brush.horizontal(Color.withAlpha(accent, 0xB0), accent));
        float s = knobScale != null ? knobScale.get() : 1f;
        float d = KNOB * s;
        float kx = KNOB / 2f + (w - KNOB) * t - d / 2f;
        Radii r = Radii.all(d / 2f);
        canvas.drawShadow(kx, cy - d / 2f, d, d, r, KNOB_SHADOW);
        canvas.fillRoundRect(kx, cy - d / 2f, d, d, r, Brush.solid(Color.WHITE));
        if (isFocused()) {
            canvas.strokeRoundRect(kx - 1.5f, cy - d / 2f - 1.5f, d + 3f, d + 3f, Radii.all(d / 2f + 1.5f), 1f, accent);
        }
    }
}
