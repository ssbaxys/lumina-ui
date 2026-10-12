package io.github.ssbaxys.lumina.hud;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.List;

/**
 * Всплывающее уведомление: иконка-предмет, заголовок, текст (до трёх строк), полоса прогресса и
 * время жизни. Въезжает сбоку, живёт {@link #timeout} секунд (полоска внизу показывает, сколько осталось),
 * уезжает и схлопывается, освобождая место соседям. Показывается через {@link ToastStack#show}.
 *
 * <pre>{@code
 * Hud.get().toasts().show(Toast.success("Достижение", "Добыть алмазы").icon("minecraft:diamond"));
 * Toast t = Toast.info("Загрузка", "Чанки 0/100").timeout(0).progress(0f);   // висит до dismiss()
 * }</pre>
 *
 * <p>Пока тост под мышью (в обычном экране), время не идёт. Время считается по кадрам: пока HUD скрыт
 * (F1), тост ждёт и не пропадёт незамеченным.</p>
 */
public class Toast extends Widget<Toast> implements Animation {
    /** Этап жизни тоста. */
    public enum Phase { QUEUED, ENTERING, SHOWN, LEAVING, COLLAPSING, GONE }

    public static final int INFO = 0xFF5B8CFF;
    public static final int SUCCESS = 0xFF34C38F;
    public static final int WARNING = 0xFFF5A524;
    public static final int ERROR = 0xFFF0506E;

    private static final float ENTER = 0.32f;
    private static final float LEAVE = 0.25f;
    private static final float COLLAPSE = 0.16f;
    private static final float ICON = 16f;
    private static final Brush FALLBACK_BG = Brush.solid(0xF0202430);
    private static final Radii FALLBACK_RADII = Radii.all(5f);

    private String title;
    private String body = "";
    private String icon;
    private Integer accent;
    private float timeout = 5f;
    private float progress = -1f;
    private float shownProgress = -1f;
    private boolean timeBar = true;
    private ReadableSignal<? extends Number> progressBinding;
    private Subscription progressSub;
    private Runnable onClose;

    private Phase phase = Phase.QUEUED;
    private float phaseTime;
    private float age;
    private float enter;
    private float collapse = 1f;
    private float fullHeight;
    private float gapBelow;
    private boolean ticking;
    private ToastStack stack;

    private String titleLine = "";
    private List<String> bodyLines = List.of();

    public Toast(String title) {
        super("toast");
        this.title = title == null ? "" : title;
        layoutParams().width = Dim.px(170);
        layoutParams().shrink = 0f;
    }

    public static Toast info(String title, String body) {
        return new Toast(title).body(body);
    }

    public static Toast success(String title, String body) {
        return new Toast(title).body(body).accent(SUCCESS);
    }

    public static Toast warning(String title, String body) {
        return new Toast(title).body(body).accent(WARNING);
    }

    public static Toast error(String title, String body) {
        return new Toast(title).body(body).accent(ERROR).timeout(8f);
    }

    // ================================================================ содержимое

    public String title() {
        return title;
    }

    public Toast title(String title) {
        this.title = title == null ? "" : title;
        invalidateLayout();
        return this;
    }

    public String body() {
        return body;
    }

    public Toast body(String body) {
        this.body = body == null ? "" : body;
        invalidateLayout();
        return this;
    }

    /** Иконка — предмет игры ({@code "minecraft:diamond"}); {@code null} — без иконки. */
    public Toast icon(String itemId) {
        this.icon = itemId == null || itemId.isEmpty() ? null : itemId;
        invalidateLayout();
        return this;
    }

    public String icon() {
        return icon;
    }

    /** Цвет полоски слева и прогресса; без него — акцент темы. */
    public Toast accent(int argb) {
        this.accent = argb;
        return this;
    }

    /** Время жизни в секундах после появления; {@code 0} — висит, пока не вызван {@link #dismiss()}. */
    public Toast timeout(float seconds) {
        this.timeout = Math.max(0f, seconds);
        invalidateLayout();
        return this;
    }

    public float timeout() {
        return timeout;
    }

    /** Показывать ли полоску оставшегося времени (по умолчанию да, если есть {@link #timeout}). */
    public Toast timeBar(boolean show) {
        this.timeBar = show;
        invalidateLayout();
        return this;
    }

    /** Полоса прогресса 0..1 (загрузка, задание); отрицательное — без полосы. */
    public Toast progress(float p) {
        boolean had = progress >= 0f;
        progress = p < 0f ? -1f : Math.min(1f, p);
        if (shownProgress < 0f) {
            shownProgress = progress;
        }
        if (had != progress >= 0f) {
            invalidateLayout();
        }
        return this;
    }

    public float progress() {
        return progress;
    }

    /** Прогресс из сигнала (0..1), пока тост на экране. */
    public Toast bindProgress(ReadableSignal<? extends Number> signal) {
        unbind();
        progressBinding = signal;
        if (root() != null) {
            subscribe();
        }
        return this;
    }

    /** Вызывается один раз, когда тост ушёл с экрана (по времени или {@link #dismiss()}). */
    public Toast onClose(Runnable callback) {
        this.onClose = callback;
        return this;
    }

    // ================================================================ жизненный цикл

    public Phase phase() {
        return phase;
    }

    /** Секунд на экране (после появления). */
    public float age() {
        return age;
    }

    /** Уходит ли тост (или уже ушёл). */
    public boolean isClosing() {
        return phase == Phase.LEAVING || phase == Phase.COLLAPSING || phase == Phase.GONE;
    }

    /** Убрать тост: уезжает и схлопывается; из очереди — сразу. */
    public void dismiss() {
        switch (phase) {
            case QUEUED -> {
                if (parent() == null && stack != null) {
                    stack.dropQueued(this);
                }
                finish();
            }
            case ENTERING, SHOWN -> {
                // Уезжает с того места, куда успел въехать.
                phaseTime = (1f - enter) * LEAVE;
                phase = Phase.LEAVING;
                startTicking();
            }
            default -> {
            }
        }
    }

    /** Начать жизнь заново (стопка вызывает перед показом). */
    void prepare(ToastStack owner, float gap) {
        stack = owner;
        gapBelow = gap;
        phase = Phase.QUEUED;
        phaseTime = 0f;
        age = 0f;
        enter = 0f;
        collapse = 1f;
        layoutParams().maxHeight = Float.POSITIVE_INFINITY;
        layoutParams().margin = new Insets(0, 0, gap, 0);
        clearAnimatedStyle(Styles.TRANSLATE_X);
        clearAnimatedStyle(Styles.OPACITY);
        invalidateLayout();
    }

    @Override
    protected void onAttach() {
        if (phase == Phase.QUEUED || phase == Phase.GONE) {
            phase = Phase.ENTERING;
            phaseTime = 0f;
            enter = 0f;
            applySlide();
        }
        if (progressBinding != null && progressSub == null) {
            subscribe();
        }
        startTicking();
    }

    @Override
    protected void onDetach() {
        unbind();
    }

    private void subscribe() {
        progress(progressBinding.get().floatValue());
        progressSub = progressBinding.subscribe(v -> progress(v.floatValue()));
    }

    private void unbind() {
        if (progressSub != null) {
            progressSub.close();
            progressSub = null;
        }
    }

    private void startTicking() {
        if (!ticking && root() != null && phase != Phase.GONE) {
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
        phaseTime += dt;
        switch (phase) {
            case ENTERING -> {
                enter = Math.min(1f, phaseTime / ENTER);
                applySlide();
                if (enter >= 1f) {
                    phase = Phase.SHOWN;
                    phaseTime = 0f;
                    clearAnimatedStyle(Styles.TRANSLATE_X);
                    clearAnimatedStyle(Styles.OPACITY);
                }
            }
            case SHOWN -> {
                if (timeout > 0f && !hasState(State.HOVERED)) {
                    age += dt;
                    if (age >= timeout) {
                        dismiss();
                    }
                }
            }
            case LEAVING -> {
                float t = Math.min(1f, phaseTime / LEAVE);
                enter = 1f - t;
                applySlide();
                if (t >= 1f) {
                    phase = Phase.COLLAPSING;
                    phaseTime = 0f;
                    fullHeight = height();
                }
            }
            case COLLAPSING -> {
                float c = 1f - Math.min(1f, phaseTime / COLLAPSE);
                setCollapse(c);
                if (c <= 0f) {
                    finish();
                    ticking = false;
                    return false;
                }
            }
            default -> {
                ticking = false;
                return false;
            }
        }
        if (progress >= 0f && shownProgress != progress) {
            float k = Math.min(1f, dt * 10f);
            shownProgress += (progress - shownProgress) * k;
            if (Math.abs(progress - shownProgress) < 0.002f) {
                shownProgress = progress;
            }
        }
        return true;
    }

    /** Въезд и уход: сдвиг вбок и прозрачность (без сдвига при «меньше анимаций»). */
    private void applySlide() {
        float e = phase == Phase.LEAVING ? Easing.IN_CUBIC.apply(enter) : Easing.OUT_CUBIC.apply(enter);
        boolean reduced = root() != null && root().accessibility().reducedMotion();
        float distance = (width() > 0f ? width() : 170f) + 16f;
        setAnimatedStyle(Styles.TRANSLATE_X, reduced ? 0f : (1f - e) * distance * slideDirection());
        setAnimatedStyle(Styles.OPACITY, Math.max(0f, Math.min(1f, e)));
    }

    /** +1 — въезжает справа (стопка в правой половине экрана), -1 — слева. */
    private float slideDirection() {
        if (stack != null && stack.slideFrom() != 0) {
            return stack.slideFrom();
        }
        if (root() == null || parent() == null) {
            return 1f;
        }
        float cx = parent().screenX() + parent().width() / 2f;
        return cx >= root().width() / 2f ? 1f : -1f;
    }

    private void setCollapse(float c) {
        collapse = c;
        layoutParams().maxHeight = fullHeight * c;
        layoutParams().margin = new Insets(0, 0, gapBelow * c, 0);
        invalidateLayout();
    }

    private void finish() {
        if (phase == Phase.GONE) {
            return;
        }
        phase = Phase.GONE;
        ToastStack s = stack;
        if (s != null) {
            s.onToastGone(this);
        } else if (parent() != null) {
            parent().removeChild(this);
        }
        if (onClose != null) {
            onClose.run();
        }
    }

    // ================================================================ раскладка и отрисовка

    private boolean hasBar() {
        return progress >= 0f;
    }

    private boolean hasTimeBar() {
        return timeBar && timeout > 0f && progress < 0f;
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float iconRoom = icon != null ? ICON + 6f : 0f;
        float textW = Float.isFinite(maxWidth) ? Math.max(10f, maxWidth - iconRoom) : 160f;
        titleLine = HudText.ellipsize(title, textW, m);
        bodyLines = HudText.wrap(body, textW, m, 3);
        float lh = m.lineHeight();
        float textH = lh + (bodyLines.isEmpty() ? 0f : 3f + bodyLines.size() * (lh + 1f) - 1f);
        float h = Math.max(icon != null ? ICON : 0f, textH) + (hasBar() ? 6f : 0f);
        float w = Float.isFinite(maxWidth) ? maxWidth : iconRoom + textW;
        setContentSize(w, h);
    }

    @Override
    protected void paintBackground(Canvas canvas) {
        if (style.get(Styles.BACKGROUND) == null) {
            canvas.fillRoundRect(0, 0, width(), height(), FALLBACK_RADII, FALLBACK_BG);
        }
        super.paintBackground(canvas);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        if (collapse < 1f) {
            return;
        }
        Insets pad = padding();
        TextMetrics m = canvas.textMetrics();
        float lh = m.lineHeight();
        int color = accent != null ? accent : style.get(Styles.ACCENT_COLOR);
        int text = style.get(Styles.TEXT_COLOR);
        boolean shadow = style.get(Styles.TEXT_SHADOW);
        float innerH = height() - pad.vertical();
        // Полоска цвета слева.
        canvas.fillRect(3f, pad.top() + 1f, 2f, Math.max(0f, innerH - 2f), color);
        float x = pad.left();
        float y = pad.top();
        if (icon != null) {
            float textH = innerH - (hasBar() ? 6f : 0f);
            canvas.drawItem(icon, x, y + Math.max(0f, (textH - ICON) / 2f), ICON);
            x += ICON + 6f;
        }
        canvas.drawText(titleLine, x, y, text, shadow);
        if (!bodyLines.isEmpty()) {
            int sub = Color.mulAlpha(text, 0.72f);
            float by = y + lh + 3f;
            for (int i = 0, n = bodyLines.size(); i < n; i++) {
                canvas.drawText(bodyLines.get(i), x, by, sub, shadow);
                by += lh + 1f;
            }
        }
        float barW = width() - pad.horizontal();
        if (hasBar()) {
            float by = height() - pad.bottom() - 3f;
            canvas.fillRect(pad.left(), by, barW, 3f, style.get(HudStyles.TRACK_COLOR));
            canvas.fillRect(pad.left(), by, barW * Math.max(0f, shownProgress), 3f, color);
        } else if (hasTimeBar()) {
            float left = phase == Phase.SHOWN ? Math.max(0f, 1f - age / timeout) : (phase == Phase.ENTERING ? 1f : 0f);
            canvas.fillRect(pad.left(), height() - 3f, barW * left, 1.5f, Color.mulAlpha(color, 0.7f));
        }
    }

    @Override
    public String toString() {
        return "Toast[" + title + ", " + phase + "]";
    }
}
