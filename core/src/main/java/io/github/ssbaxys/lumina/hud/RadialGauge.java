package io.github.ssbaxys.lumina.hud;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Widget;

/**
 * Круговая шкала-«спидометр»: дуга на {@link #sweep} градусов (по умолчанию 270, разрыв снизу), в центре —
 * значение числом, под ним — короткая подпись. Ниже {@link #lowBelow} дуга окрашивается в {@code hud-low-color}.
 *
 * <pre>{@code
 * new RadialGauge("воздух").max(300).bind(GameData.air()).percent(true);
 * new RadialGauge("FPS").max(144).bind(GameData.fps()).lowBelow(0.25f);
 * }</pre>
 *
 * <p>Число в центре превращается в строку только при смене целого значения.</p>
 */
public class RadialGauge extends Widget<RadialGauge> implements Animation {
    private String label;
    private float value;
    private float max = 1f;
    private float shown;
    private float sweep = 270f;
    private float thickness = 4f;
    private float lowBelow;
    private boolean percent;
    private Integer from;
    private Integer to;
    private String number = "0";
    private int numberKey = Integer.MIN_VALUE;
    private boolean ticking;
    private ReadableSignal<? extends Number> binding;
    private Subscription sub;

    public RadialGauge(String label) {
        super("hud-bar");
        this.label = label == null || label.isEmpty() ? null : label;
        layoutParams().width = Dim.px(40);
        layoutParams().height = Dim.px(40);
        setHitTestable(false);
        refreshNumber();
    }

    public RadialGauge label(String label) {
        this.label = label == null || label.isEmpty() ? null : label;
        return this;
    }

    public float value() {
        return value;
    }

    public RadialGauge value(float v) {
        value = Math.max(0f, v);
        refreshNumber();
        startTicking();
        return this;
    }

    public RadialGauge max(float max) {
        this.max = Math.max(0.0001f, max);
        refreshNumber();
        return this;
    }

    /** Угол дуги в градусах (360 — полное кольцо). */
    public RadialGauge sweep(float degrees) {
        this.sweep = Math.max(10f, Math.min(360f, degrees));
        return this;
    }

    public RadialGauge thickness(float px) {
        this.thickness = px;
        return this;
    }

    /** Доля, ниже которой дуга — цвета {@code hud-low-color} (0 — никогда). */
    public RadialGauge lowBelow(float fraction) {
        this.lowBelow = fraction;
        return this;
    }

    /** Показывать в центре проценты от максимума вместо самого значения. */
    public RadialGauge percent(boolean percent) {
        this.percent = percent;
        numberKey = Integer.MIN_VALUE;
        refreshNumber();
        return this;
    }

    public RadialGauge colors(int from, int to) {
        this.from = from;
        this.to = to;
        return this;
    }

    public RadialGauge bind(ReadableSignal<? extends Number> signal) {
        if (sub != null) {
            sub.close();
            sub = null;
        }
        binding = signal;
        if (root() != null) {
            subscribe();
        }
        return this;
    }

    /** Текст в центре. */
    public String number() {
        return number;
    }

    private void subscribe() {
        value(binding.get().floatValue());
        shown = value;
        sub = binding.subscribe(v -> value(v.floatValue()));
    }

    private void refreshNumber() {
        int key = percent ? Math.round(value / max * 100f) : Math.round(value);
        if (key != numberKey) {
            numberKey = key;
            number = percent ? key + "%" : Integer.toString(key);
        }
    }

    @Override
    protected void onAttach() {
        if (binding != null && sub == null) {
            subscribe();
        }
        startTicking();
    }

    @Override
    protected void onDetach() {
        if (sub != null) {
            sub.close();
            sub = null;
        }
    }

    private void startTicking() {
        if (!ticking && root() != null && shown != value) {
            ticking = true;
            root().animator().start(this);
        }
    }

    @Override
    public boolean tick(float dt) {
        float k = Math.min(1f, dt * 9f);
        shown += (value - shown) * k;
        if (Math.abs(value - shown) < max * 0.001f) {
            shown = value;
        }
        ticking = root() != null && shown != value;
        return ticking;
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float size = Math.min(width(), height());
        float cx = width() / 2f;
        float cy = height() / 2f;
        float radius = size / 2f - thickness / 2f;
        float start = -sweep / 2f;
        float t = Math.max(0f, Math.min(1f, shown / max));
        int accent = style.get(Styles.ACCENT_COLOR);
        int a = from != null ? from : accent;
        int b = to != null ? to : Color.lighten(Color.rotateHue(accent, 40f), 0.1f);
        if (lowBelow > 0f && value / max <= lowBelow) {
            int low = style.get(HudStyles.LOW_COLOR);
            a = low;
            b = Color.lighten(low, 0.15f);
        }
        int track = style.get(HudStyles.TRACK_COLOR);
        canvas.drawArc(cx, cy, radius, thickness, start, sweep, track, track);
        if (t > 0.001f) {
            canvas.drawArc(cx, cy, radius, thickness, start, sweep * t, a, Color.lerp(a, b, t));
        }
        TextMetrics m = canvas.textMetrics();
        int text = style.get(Styles.TEXT_COLOR);
        boolean shadow = style.get(Styles.TEXT_SHADOW);
        float lh = m.lineHeight();
        float numberY = label != null ? cy - lh + 1f : cy - lh / 2f + 1f;
        canvas.drawText(number, cx - m.width(number) / 2f, numberY, text, shadow);
        if (label != null) {
            canvas.save();
            canvas.translate(cx, numberY + lh);
            canvas.scale(0.75f, 0.75f);
            canvas.drawText(label, -m.width(label) / 2f, 0f, Color.mulAlpha(text, 0.75f), shadow);
            canvas.restore();
        }
    }
}
