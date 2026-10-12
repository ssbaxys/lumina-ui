package io.github.ssbaxys.lumina.hud;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Widget;

/**
 * Таймер: подпись и время «М:СС» обратного отсчёта или секундомера ({@link Countdown}), под ним — тонкая
 * полоска оставшегося времени. Последние {@link #warnBelow} секунд цифры краснеют ({@code hud-low-color})
 * и пульсируют, а с {@link #tenthsBelow} — показывают десятые.
 *
 * <pre>{@code
 * Countdown c = Countdown.seconds(60).start();
 * hud.add(new TimerDisplay(c).label("До начала").scale(1.5f), HudAnchor.TOP_CENTER).offset(0, 24);
 * }</pre>
 *
 * <p>Строка времени создаётся только когда меняется показываемое значение (раз в секунду или десятую).</p>
 */
public class TimerDisplay extends Widget<TimerDisplay> implements Animation {
    private Countdown countdown;
    private String label;
    private float textScale = 1f;
    private float warnBelow = 10f;
    private float tenthsBelow;
    private boolean bar = true;

    private String text = "0:00";
    private int textKey = Integer.MIN_VALUE;
    private float pulse;
    private boolean ticking;

    public TimerDisplay(Countdown countdown) {
        super("hud-timer");
        this.countdown = countdown;
        setHitTestable(false);
        refreshText();
    }

    public Countdown countdown() {
        return countdown;
    }

    /** Показать другой отсчёт. */
    public TimerDisplay countdown(Countdown countdown) {
        this.countdown = countdown;
        textKey = Integer.MIN_VALUE;
        refreshText();
        startTicking();
        return this;
    }

    /** Подпись перед временем ({@code null} — без подписи). */
    public TimerDisplay label(String label) {
        this.label = label == null || label.isEmpty() ? null : label;
        invalidateLayout();
        return this;
    }

    public TimerDisplay scale(float scale) {
        this.textScale = scale;
        invalidateLayout();
        return this;
    }

    /** Последние секунды выделяются цветом и пульсом (0 — никогда). */
    public TimerDisplay warnBelow(float seconds) {
        this.warnBelow = Math.max(0f, seconds);
        return this;
    }

    /** С какого остатка показывать десятые («0:04.7»); 0 — никогда. */
    public TimerDisplay tenthsBelow(float seconds) {
        this.tenthsBelow = Math.max(0f, seconds);
        textKey = Integer.MIN_VALUE;
        refreshText();
        return this;
    }

    /** Полоска оставшегося времени под цифрами (у отсчёта; по умолчанию есть). */
    public TimerDisplay bar(boolean show) {
        this.bar = show;
        invalidateLayout();
        return this;
    }

    /** Текущая строка времени. */
    public String text() {
        return text;
    }

    // ================================================================ время

    private boolean tenths(float value) {
        return !countdown.isStopwatch() && tenthsBelow > 0f && value < tenthsBelow && value > 0f;
    }

    /** Пересчитать строку, если изменилось показываемое значение; {@code true} — изменилась. */
    private boolean refreshText() {
        if (countdown == null) {
            return false;
        }
        float v = countdown.displayed();
        boolean t = tenths(v);
        boolean up = !countdown.isStopwatch();
        int key = HudText.timeKey(v, t, up) * 2 + (t ? 1 : 0);
        if (key == textKey) {
            return false;
        }
        textKey = key;
        String next = HudText.formatTime(v, t, up);
        boolean resize = next.length() != text.length();
        text = next;
        if (resize) {
            invalidateLayout();
        }
        return true;
    }

    private boolean warning() {
        return countdown != null && !countdown.isStopwatch() && warnBelow > 0f
                && countdown.remaining() <= warnBelow && countdown.remaining() > 0f && countdown.isRunning();
    }

    @Override
    protected void onAttach() {
        startTicking();
    }

    private void startTicking() {
        if (!ticking && root() != null) {
            ticking = true;
            root().animator().start(this);
        }
    }

    @Override
    public boolean tick(float dt) {
        if (root() == null || countdown == null) {
            ticking = false;
            return false;
        }
        // Отсчёт могут запустить или поставить на паузу откуда угодно, поэтому смотрим каждый кадр:
        // это одно чтение часов и сравнение чисел, строка создаётся только при смене значения.
        countdown.poll();
        refreshText();
        pulse = (pulse + dt) % 1f;
        return true;
    }

    // ================================================================ раскладка и отрисовка

    private float barHeight() {
        return bar && countdown != null && !countdown.isStopwatch() ? 4f : 0f;
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float w = m.width(text) * textScale;
        float h = m.lineHeight() * textScale;
        if (label != null) {
            w += m.width(label) + 6f;
            h = Math.max(h, m.lineHeight());
        }
        setContentSize(w, h + barHeight());
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        if (countdown == null) {
            return;
        }
        TextMetrics m = canvas.textMetrics();
        float x = padding().left();
        float y = padding().top();
        float bh = barHeight();
        float lineH = height() - padding().vertical() - bh;
        int color = style.get(Styles.TEXT_COLOR);
        boolean shadow = style.get(Styles.TEXT_SHADOW);
        if (label != null) {
            canvas.drawText(label, x, y + (lineH - m.lineHeight()) / 2f, Color.mulAlpha(color, 0.7f), shadow);
            x += m.width(label) + 6f;
        }
        int digits = color;
        if (warning()) {
            int low = style.get(HudStyles.LOW_COLOR);
            float wave = 0.5f + 0.5f * (float) Math.sin(pulse * Math.PI * 2.0);
            digits = Color.lerp(color, low, 0.55f + 0.45f * wave);
        } else if (countdown.isFinished()) {
            digits = style.get(HudStyles.LOW_COLOR);
        }
        float ty = y + (lineH - m.lineHeight() * textScale) / 2f;
        if (textScale != 1f) {
            canvas.save();
            canvas.translate(x, ty);
            canvas.scale(textScale, textScale);
            canvas.drawText(text, 0f, 0f, digits, shadow);
            canvas.restore();
        } else {
            canvas.drawText(text, x, ty, digits, shadow);
        }
        if (bh > 0f) {
            float bw = width() - padding().horizontal();
            float by = height() - padding().bottom() - 2f;
            canvas.fillRect(padding().left(), by, bw, 2f, style.get(HudStyles.TRACK_COLOR));
            float left = 1f - countdown.progress();
            int fill = warning() ? style.get(HudStyles.LOW_COLOR) : style.get(Styles.ACCENT_COLOR);
            canvas.fillRect(padding().left(), by, bw * left, 2f, fill);
        }
    }
}
