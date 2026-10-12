package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.Objects;

/** Однострочный текст. Может быть привязан к сигналу: обновляется сам, отписывается при удалении. */
public class Label extends Widget<Label> {
    private String text;
    private float textScale = 1f;
    private ReadableSignal<String> binding;
    private Subscription subscription;

    public Label(String text) {
        super("label");
        this.text = text == null ? "" : text;
        setHitTestable(false);
    }

    @Override
    protected String defaultAccessibleLabel() {
        return text.isEmpty() ? null : text;
    }

    public static Label bound(ReadableSignal<String> signal) {
        return new Label(signal.get()).bind(signal);
    }

    public String text() {
        return text;
    }

    public Label text(String text) {
        String t = text == null ? "" : text;
        if (!Objects.equals(this.text, t)) {
            this.text = t;
            invalidateLayout();
        }
        return this;
    }

    public Label scale(float scale) {
        this.textScale = scale;
        invalidateLayout();
        return this;
    }

    /** Свой масштаб × наследуемый {@link Styles#FONT_SCALE}. */
    private float effectiveScale() {
        return textScale * style.get(Styles.FONT_SCALE);
    }

    public Label bind(ReadableSignal<String> signal) {
        unbind();
        this.binding = signal;
        if (root() != null) {
            subscribe();
        }
        return this;
    }

    private void subscribe() {
        text(binding.get());
        subscription = binding.subscribe(this::text);
    }

    private void unbind() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    @Override
    protected void onAttach() {
        if (binding != null && subscription == null) {
            subscribe();
        }
    }

    @Override
    protected void onDetach() {
        unbind();
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float k = effectiveScale();
        setContentSize(m.width(text) * k, m.lineHeight() * k);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float k = effectiveScale();
        float lh = canvas.textMetrics().lineHeight() * k;
        float top = padding().top() + (height() - padding().vertical() - lh) / 2f;
        if (k != 1f) {
            canvas.save();
            canvas.translate(padding().left(), top);
            canvas.scale(k, k);
            canvas.drawText(text, 0, 0, style.get(Styles.TEXT_COLOR), style.get(Styles.TEXT_SHADOW));
            canvas.restore();
        } else {
            canvas.drawText(text, padding().left(), top, style.get(Styles.TEXT_COLOR), style.get(Styles.TEXT_SHADOW));
        }
    }
}
