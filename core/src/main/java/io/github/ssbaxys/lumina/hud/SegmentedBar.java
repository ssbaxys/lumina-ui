package io.github.ssbaxys.lumina.hud;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Widget;

/**
 * Полоса из отдельных делений, как сердца здоровья: значение {@code value} из {@code max} распределяется
 * по {@link #segments} делениям, неполное деление заполнено частично. Потерянное значение ещё полсекунды
 * видно светлым «следом» и тает; при малом значении ({@link #lowBelow}) полоса краснеет и мигает.
 *
 * <pre>{@code
 * new SegmentedBar().segments(10).bind(GameData.health()).bindMax(GameData.maxHealth());
 * new SegmentedBar().color(0xFFD9A441).segments(10).max(20).bind(GameData.food());
 * }</pre>
 */
public class SegmentedBar extends Widget<SegmentedBar> implements Animation {
    private static final float TRAIL_DELAY = 0.45f;
    private static final float TRAIL_SPEED = 1.6f;

    private float value = 20f;
    private float max = 20f;
    private int segments = 10;
    private float segmentWidth = 8f;
    private float segmentHeight = 7f;
    private float gap = 1f;
    private Integer color;
    private float lowBelow = 0.25f;
    private float shown = 20f;
    private float trail = 20f;
    private float trailWait;
    private float time;
    private boolean ticking;
    private ReadableSignal<? extends Number> valueBinding;
    private ReadableSignal<? extends Number> maxBinding;
    private Subscription valueSub;
    private Subscription maxSub;

    public SegmentedBar() {
        super("hud-bar");
        setHitTestable(false);
    }

    // ================================================================ значения

    public float value() {
        return value;
    }

    public SegmentedBar value(float v) {
        float next = Math.max(0f, v);
        if (next == value) {
            return this;
        }
        if (next < value) {
            // Урон: след остаётся на прежнем значении и тает после паузы.
            trail = Math.max(trail, shown);
            trailWait = TRAIL_DELAY;
        }
        value = next;
        startTicking();
        return this;
    }

    public float max() {
        return max;
    }

    public SegmentedBar max(float m) {
        this.max = Math.max(0.0001f, m);
        return this;
    }

    /** Число делений (по умолчанию 10: 20 здоровья = 10 сердец). */
    public SegmentedBar segments(int count) {
        this.segments = Math.max(1, count);
        invalidateLayout();
        return this;
    }

    public SegmentedBar segmentSize(float width, float height) {
        this.segmentWidth = width;
        this.segmentHeight = height;
        invalidateLayout();
        return this;
    }

    public SegmentedBar gap(float gap) {
        this.gap = gap;
        invalidateLayout();
        return this;
    }

    /** Цвет заполнения; без него — акцент темы. */
    public SegmentedBar color(int argb) {
        this.color = argb;
        return this;
    }

    /** Доля, ниже которой полоса краснеет и мигает (0 — никогда). */
    public SegmentedBar lowBelow(float fraction) {
        this.lowBelow = fraction;
        return this;
    }

    /** Значение из сигнала (любое число). */
    public SegmentedBar bind(ReadableSignal<? extends Number> signal) {
        closeValue();
        valueBinding = signal;
        if (root() != null) {
            subscribeValue();
        }
        return this;
    }

    /** Максимум из сигнала. */
    public SegmentedBar bindMax(ReadableSignal<? extends Number> signal) {
        closeMax();
        maxBinding = signal;
        if (root() != null) {
            subscribeMax();
        }
        return this;
    }

    private void subscribeValue() {
        snap(valueBinding.get().floatValue());
        valueSub = valueBinding.subscribe(v -> value(v.floatValue()));
    }

    private void subscribeMax() {
        max(maxBinding.get().floatValue());
        maxSub = maxBinding.subscribe(v -> max(v.floatValue()));
    }

    /** Сразу показать значение, без следа и анимации. */
    public SegmentedBar snap(float v) {
        value = Math.max(0f, v);
        shown = value;
        trail = value;
        return this;
    }

    private void closeValue() {
        if (valueSub != null) {
            valueSub.close();
            valueSub = null;
        }
    }

    private void closeMax() {
        if (maxSub != null) {
            maxSub.close();
            maxSub = null;
        }
    }

    @Override
    protected void onAttach() {
        if (valueBinding != null && valueSub == null) {
            subscribeValue();
        }
        if (maxBinding != null && maxSub == null) {
            subscribeMax();
        }
        startTicking();
    }

    @Override
    protected void onDetach() {
        closeValue();
        closeMax();
    }

    // ================================================================ анимация

    private boolean isLow() {
        return lowBelow > 0f && value / max <= lowBelow && value > 0f;
    }

    private void startTicking() {
        if (!ticking && root() != null) {
            ticking = true;
            root().animator().start(this);
        }
    }

    @Override
    public boolean tick(float dt) {
        if (root() == null) {
            ticking = false;
            return false;
        }
        time = (time + dt) % 2f;
        // Заполнение догоняет значение быстро, след — после паузы и медленнее.
        float k = Math.min(1f, dt * 14f);
        shown += (value - shown) * k;
        if (Math.abs(value - shown) < 0.01f) {
            shown = value;
        }
        if (trailWait > 0f) {
            trailWait -= dt;
        } else if (trail > shown) {
            trail = Math.max(shown, trail - dt * TRAIL_SPEED * Math.max(1f, max / segments));
        }
        if (trail < shown) {
            trail = shown;
        }
        boolean keep = shown != value || trail != shown || isLow();
        ticking = keep;
        return keep;
    }

    // ================================================================ отрисовка

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        setContentSize(segments * segmentWidth + (segments - 1) * gap, segmentHeight);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        int fill = color != null ? color : style.get(Styles.ACCENT_COLOR);
        int track = style.get(HudStyles.TRACK_COLOR);
        boolean low = isLow();
        if (low) {
            int lowColor = style.get(HudStyles.LOW_COLOR);
            // Мигание: два коротких вспышки в секунду.
            float blink = (time % 0.5f) < 0.25f ? 1f : 0.55f;
            fill = Color.mulAlpha(Color.lerp(fill, lowColor, 0.6f), blink);
        }
        int trailColor = Color.mulAlpha(Color.lerp(fill, Color.WHITE, 0.55f), 0.85f);
        int edge = Color.withAlpha(Color.darken(fill, 0.35f), Color.alpha(fill));
        float per = max / segments;
        float x = padding().left();
        float y = padding().top();
        float h = segmentHeight;
        for (int i = 0; i < segments; i++) {
            float lo = i * per;
            float f = clamp01((shown - lo) / per);
            float t = clamp01((trail - lo) / per);
            canvas.fillRect(x, y, segmentWidth, h, track);
            if (t > f) {
                canvas.fillRect(x + segmentWidth * f, y, segmentWidth * (t - f), h, trailColor);
            }
            if (f > 0f) {
                float w = segmentWidth * f;
                canvas.fillRect(x, y, w, h, fill);
                // Блик сверху и тень снизу — деление выглядит выпуклым.
                canvas.fillRect(x, y, w, 1f, Color.mulAlpha(Color.WHITE, 0.35f * Color.alphaF(fill)));
                canvas.fillRect(x, y + h - 1f, w, 1f, edge);
            }
            x += segmentWidth + gap;
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
