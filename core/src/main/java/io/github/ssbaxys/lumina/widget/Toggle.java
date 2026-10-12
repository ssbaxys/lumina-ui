package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Spring;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
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

/** Переключатель вкл/выкл, привязанный к {@link Signal}. Ползунок двигается пружиной; выставляет состояние CHECKED. */
public class Toggle extends Widget<Toggle> {
    private static final Shadow KNOB_SHADOW = new Shadow(0, 1, 2, 0, 0x60000000);

    private final Signal<Boolean> value;
    private AnimatedFloat knob;
    private Subscription subscription;

    public Toggle() {
        this(Signal.of(false));
    }

    public Toggle(Signal<Boolean> value) {
        super("toggle");
        this.value = value;
        setFocusable(true);
        layoutParams().width = Dim.px(26);
        layoutParams().height = Dim.px(14);
    }

    public Signal<Boolean> value() {
        return value;
    }

    public boolean isOn() {
        return Boolean.TRUE.equals(value.peek());
    }

    public void toggle() {
        if (isEnabled()) {
            value.set(!isOn());
        }
    }

    @Override
    protected void onAttach() {
        knob = new AnimatedFloat(root().animator(), isOn() ? 1f : 0f).spring(Spring.BOUNCY);
        setState(State.CHECKED, isOn());
        subscription = value.subscribe(on -> {
            setState(State.CHECKED, on);
            knob.animateTo(on ? 1f : 0f);
        });
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.target() != this) {
            return;
        }
        if (event.type() == PointerEvent.CLICK && ((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
            toggle();
            event.consume();
        } else if (event.type() == KeyEvent.PRESS) {
            int key = ((KeyEvent) event).key();
            if (key == Keys.SPACE || key == Keys.ENTER || key == Keys.KP_ENTER) {
                toggle();
                event.consume();
            }
        }
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float t = knob != null ? knob.get() : (isOn() ? 1f : 0f);
        float w = width();
        float h = height();
        int accent = style.get(Styles.ACCENT_COLOR);
        int track = Color.lerp(0x50808080, accent, Math.max(0f, Math.min(1f, t)));
        canvas.fillRoundRect(0, 0, w, h, Radii.all(h / 2f), Brush.solid(track));
        float d = h - 4f;
        float x = 2f + t * (w - 4f - d);
        Radii r = Radii.all(d / 2f);
        canvas.drawShadow(x, 2f, d, d, r, KNOB_SHADOW);
        canvas.fillRoundRect(x, 2f, d, d, r, Brush.solid(Color.WHITE));
    }
}
