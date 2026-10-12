package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.anim.Spring;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;

/**
 * Флажок, привязанный к {@link Signal}, с подписью справа (клик по подписи тоже переключает).
 * Space/Enter в фокусе — переключить. Вид задаёт {@link Kind}: галочка рисуется штрихом, квадрат
 * вырастает с отскоком, точка радио «выпрыгивает», сердце и звезда подпрыгивают. Цвета — от акцента
 * ({@link Styles#ACCENT_COLOR}) или свои ({@link #color}). Выставляет состояние CHECKED.
 *
 * <pre>{@code
 * new Checkbox("Показывать подсказки", hints).kind(Checkbox.Kind.CHECK)
 * }</pre>
 */
public class Checkbox extends Widget<Checkbox> {
    /** Вид флажка. */
    public enum Kind {
        /** Квадрат с пружинной заливкой и галочкой, которая рисуется штрихом; есть неопределённое состояние. */
        CHECK,
        /** Квадрат с крестиком из двух штрихов. */
        CROSS,
        /** Внутренний квадрат вырастает с перелётом. */
        FILL,
        /** Круг с выпрыгивающей точкой. */
        RADIO,
        /** Сердце: наполняется цветом и подпрыгивает. */
        HEART,
        /** Звезда: наполняется цветом и подпрыгивает. */
        STAR,
        /** Маленький переключатель с бегущей точкой. */
        SWITCH_DOT
    }

    private static final float LABEL_GAP = 5f;
    private static final Spring FILL_SPRING = new Spring(380f, 22f, 1f);

    private final Signal<Boolean> value;
    private Kind kind = Kind.CHECK;
    private String label;
    private float boxSize = 12f;
    private boolean indeterminate;
    private Integer color;
    private Runnable action;

    private AnimatedFloat fill;
    private AnimatedFloat mark;
    private AnimatedFloat dash;
    private AnimatedFloat feedback;
    private AnimatedFloat bump;
    private Subscription subscription;
    private boolean shownOn;

    public Checkbox() {
        this(Signal.of(false));
    }

    public Checkbox(Signal<Boolean> value) {
        this("", value);
    }

    public Checkbox(String label, Signal<Boolean> value) {
        super("checkbox");
        this.value = value;
        this.label = label == null ? "" : label;
        setFocusable(true);
    }

    public Signal<Boolean> value() {
        return value;
    }

    public boolean isChecked() {
        return Boolean.TRUE.equals(value.peek());
    }

    public Kind kind() {
        return kind;
    }

    public Checkbox kind(Kind kind) {
        this.kind = kind == null ? Kind.CHECK : kind;
        invalidateLayout();
        if (root() != null) {
            createAnimations();
        }
        return this;
    }

    public String label() {
        return label;
    }

    public Checkbox label(String label) {
        this.label = label == null ? "" : label;
        invalidateLayout();
        return this;
    }

    /** Размер значка (высота; у SWITCH_DOT ширина больше). */
    public Checkbox boxSize(float size) {
        this.boxSize = size;
        invalidateLayout();
        return this;
    }

    /** Свой цвет вместо акцента темы ({@code null} — снова акцент). */
    public Checkbox color(Integer argb) {
        this.color = argb;
        return this;
    }

    /** Неопределённое состояние (черта вместо галочки, только у {@link Kind#CHECK}); клик его снимает. */
    public Checkbox indeterminate(boolean on) {
        if (indeterminate != on) {
            indeterminate = on;
            sync(true);
        }
        return this;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    /** Своё действие вместо переключения (так работают радио-кнопки в {@link RadioGroup}). */
    public Checkbox action(Runnable action) {
        this.action = action;
        return this;
    }

    /** Переключить, как будто нажали: снимает неопределённость, иначе меняет значение. */
    public void toggle() {
        if (!isEnabled()) {
            return;
        }
        if (indeterminate) {
            indeterminate = false;
            if (isChecked()) {
                sync(true);
            } else {
                value.set(true);
            }
            return;
        }
        value.set(!isChecked());
    }

    private void activate() {
        if (!isEnabled()) {
            return;
        }
        if (action != null) {
            action.run();
        } else {
            toggle();
        }
    }

    private boolean mixed() {
        return indeterminate && kind == Kind.CHECK;
    }

    // ---------------------------------------------------------------- анимации

    @Override
    protected void onAttach() {
        createAnimations();
        subscription = value.subscribe(v -> sync(true));
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    private void createAnimations() {
        var animator = root().animator();
        fill = new AnimatedFloat(animator, 0f);
        mark = new AnimatedFloat(animator, 0f);
        dash = new AnimatedFloat(animator, 0f).tween(0.22f, Easing.OUT_CUBIC);
        feedback = new AnimatedFloat(animator, 1f).spring(Spring.SNAPPY);
        bump = new AnimatedFloat(animator, 1f).tween(0.45f, Easing.LINEAR);
        switch (kind) {
            case CHECK -> {
                fill.spring(FILL_SPRING);
                mark.tween(0.3f, Easing.OUT_CUBIC);
            }
            case CROSS -> {
                fill.tween(0.2f, Easing.OUT_CUBIC);
                mark.tween(0.3f, Easing.OUT_CUBIC);
            }
            case FILL, RADIO, HEART, STAR -> {
                fill.tween(0.2f, Easing.OUT_CUBIC);
                mark.tween(0.32f, Easing.OUT_BACK);
            }
            case SWITCH_DOT -> {
                fill.tween(0.2f, Easing.OUT_CUBIC);
                mark.spring(Spring.BOUNCY);
            }
        }
        sync(false);
        updateFeedback();
    }

    /** Привести значки к значению; {@code animate=false} — мгновенно. */
    private void sync(boolean animate) {
        boolean on = isChecked();
        boolean mix = mixed();
        setState(State.CHECKED, on);
        if (fill == null) {
            shownOn = on;
            return;
        }
        float fillT = on || mix ? 1f : 0f;
        float markT = on && !mix ? 1f : 0f;
        float dashT = mix ? 1f : 0f;
        if (!animate) {
            fill.snapTo(fillT);
            mark.snapTo(markT);
            dash.snapTo(dashT);
        } else {
            fill.animateTo(fillT);
            mark.animateTo(markT);
            dash.animateTo(dashT);
            if (on && !shownOn && (kind == Kind.HEART || kind == Kind.STAR)) {
                bump.snapTo(0f);
                bump.animateTo(1f);
            }
        }
        shownOn = on;
    }

    @Override
    protected void onStateChanged() {
        updateFeedback();
    }

    private void updateFeedback() {
        if (feedback == null) {
            return;
        }
        float target = !isEnabled() ? 1f : hasState(State.PRESSED) ? 0.86f : hasState(State.HOVERED) ? 1.08f : 1f;
        feedback.animateTo(target);
    }

    // ---------------------------------------------------------------- события

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.target() != this) {
            return;
        }
        if (event.type() == PointerEvent.CLICK && ((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
            activate();
            event.consume();
        } else if (event.type() == KeyEvent.PRESS) {
            int key = ((KeyEvent) event).key();
            if (key == Keys.SPACE || key == Keys.ENTER || key == Keys.KP_ENTER) {
                activate();
                event.consume();
            }
        }
    }

    // ---------------------------------------------------------------- раскладка и отрисовка

    private float boxWidth() {
        return kind == Kind.SWITCH_DOT ? boxSize * 1.8f : boxSize;
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float k = style.get(Styles.FONT_SCALE);
        float tw = label.isEmpty() ? 0f : m.width(label) * k;
        float th = label.isEmpty() ? 0f : m.lineHeight() * k;
        setContentSize(boxWidth() + (tw > 0f ? LABEL_GAP + tw : 0f), Math.max(boxSize, th));
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float s = boxSize;
        float bw = boxWidth();
        float bx = padding().left();
        float innerH = height() - padding().vertical();
        float by = padding().top() + (innerH - s) / 2f;
        int accent = color != null ? color : style.get(Styles.ACCENT_COLOR);
        int text = style.get(Styles.TEXT_COLOR);

        float scale = feedback != null ? feedback.get() : 1f;
        if (kind == Kind.HEART || kind == Kind.STAR) {
            float b = bump != null ? Math.max(0f, Math.min(1f, bump.get())) : 1f;
            scale *= 1f + 0.28f * (float) Math.sin(Math.PI * b);
        }
        canvas.save();
        canvas.translate(bx + bw / 2f, by + s / 2f);
        if (scale != 1f) {
            canvas.scale(scale, scale);
        }
        canvas.translate(-bw / 2f, -s / 2f);
        paintMark(canvas, s, bw, accent, text);
        canvas.restore();

        if (isFocused()) {
            boolean round = kind == Kind.RADIO || kind == Kind.SWITCH_DOT || kind == Kind.HEART || kind == Kind.STAR;
            float r = round ? s / 2f + 2.5f : s * 0.28f + 2.5f;
            canvas.strokeRoundRect(bx - 2.5f, by - 2.5f, bw + 5f, s + 5f, Radii.all(r), 1f, Color.withAlpha(accent, 0xB0));
        }

        if (!label.isEmpty()) {
            float k = style.get(Styles.FONT_SCALE);
            float lh = canvas.textMetrics().lineHeight() * k;
            float tx = bx + bw + LABEL_GAP;
            float ty = padding().top() + (innerH - lh) / 2f + 0.5f;
            boolean shadow = style.get(Styles.TEXT_SHADOW);
            if (k != 1f) {
                canvas.save();
                canvas.translate(tx, ty);
                canvas.scale(k, k);
                canvas.drawText(label, 0f, 0f, text, shadow);
                canvas.restore();
            } else {
                canvas.drawText(label, tx, ty, text, shadow);
            }
        }
    }

    private void paintMark(Canvas canvas, float s, float bw, int accent, int text) {
        boolean on = isChecked() || mixed();
        float f = fill != null ? fill.get() : (on ? 1f : 0f);
        float t = mark != null ? mark.get() : (isChecked() && !mixed() ? 1f : 0f);
        float d = dash != null ? dash.get() : (mixed() ? 1f : 0f);
        float f01 = clamp01(f);
        int idleFill = Color.withAlpha(text, 0x14);
        int idleBorder = Color.withAlpha(text, 0x70);
        int border = Color.lerp(idleBorder, accent, f01);
        int onAccent = Color.luminance(accent) > 0.6f ? 0xFF1D2330 : Color.WHITE;
        float stroke = Math.max(1.5f, s * 0.15f);
        switch (kind) {
            case CHECK -> {
                Radii r = Radii.all(s * 0.28f);
                canvas.fillRoundRect(0f, 0f, s, s, r, Brush.solid(idleFill));
                float fc = Math.max(0f, f);
                if (fc > 0.001f) {
                    float fs = s * fc;
                    canvas.fillRoundRect((s - fs) / 2f, (s - fs) / 2f, fs, fs, Radii.all(s * 0.28f * fc), Brush.solid(accent));
                }
                canvas.strokeRoundRect(0f, 0f, s, s, r, 1f, border);
                int tick = Color.mulAlpha(onAccent, f01);
                strokePath(canvas, s, t, stroke, tick, 0.24f, 0.53f, 0.43f, 0.71f, 0.77f, 0.32f);
                if (d > 0.001f) {
                    float x0 = s * 0.27f;
                    float x1 = x0 + s * 0.46f * clamp01(d);
                    canvas.drawLine(x0, s / 2f, x1, s / 2f, stroke, tick);
                    canvas.fillCircle(x0, s / 2f, stroke / 2f, Brush.solid(tick));
                    canvas.fillCircle(x1, s / 2f, stroke / 2f, Brush.solid(tick));
                }
            }
            case CROSS -> {
                Radii r = Radii.all(s * 0.28f);
                canvas.fillRoundRect(0f, 0f, s, s, r, Brush.solid(Color.lerp(idleFill, Color.withAlpha(accent, 0x38), f01)));
                canvas.strokeRoundRect(0f, 0f, s, s, r, 1f, border);
                float a = clamp01(t * 2f);
                float b = clamp01(t * 2f - 1f);
                if (a > 0f) {
                    strokePath(canvas, s, a, stroke, accent, 0.28f, 0.28f, 0.72f, 0.72f);
                }
                if (b > 0f) {
                    strokePath(canvas, s, b, stroke, accent, 0.72f, 0.28f, 0.28f, 0.72f);
                }
            }
            case FILL -> {
                Radii r = Radii.all(s * 0.24f);
                canvas.fillRoundRect(0f, 0f, s, s, r, Brush.solid(idleFill));
                canvas.strokeRoundRect(0f, 0f, s, s, r, 1f, border);
                float in = (s - 5f) * Math.max(0f, t);
                if (in > 0.05f) {
                    canvas.fillRoundRect((s - in) / 2f, (s - in) / 2f, in, in, Radii.all(in * 0.25f), Brush.solid(accent));
                }
            }
            case RADIO -> {
                float c = s / 2f;
                canvas.fillCircle(c, c, c, Brush.solid(idleFill));
                canvas.strokeCircle(c, c, c, 1f, Brush.solid(border));
                float dot = s * 0.26f * Math.max(0f, t);
                if (dot > 0.05f) {
                    canvas.fillCircle(c, c, dot, Brush.solid(accent));
                }
            }
            case HEART -> {
                heart(canvas, s, 1f, idleShape(text));
                if (t > 0.01f) {
                    heart(canvas, s, Math.max(0f, t), accent);
                }
            }
            case STAR -> {
                star(canvas, s, 1f, idleShape(text));
                if (t > 0.01f) {
                    star(canvas, s, Math.max(0f, t), accent);
                }
            }
            case SWITCH_DOT -> {
                canvas.fillRoundRect(0f, 0f, bw, s, Radii.all(s / 2f), Brush.solid(Color.lerp(0x50808080, accent, f01)));
                float dd = s - 4f;
                float x = 2f + t * (bw - 4f - dd);
                canvas.fillCircle(x + dd / 2f, s / 2f, dd / 2f, Brush.solid(Color.WHITE));
            }
        }
    }

    /** Непрозрачный приглушённый цвет для пустых сердца и звезды (части фигуры перекрываются). */
    private static int idleShape(int text) {
        int toward = Color.luminance(text) > 0.5f ? Color.BLACK : Color.WHITE;
        return Color.withAlpha(Color.lerp(text, toward, 0.62f), 0xFF);
    }

    /**
     * Ломаная из точек (в долях {@code s}), нарисованная на долю {@code progress} своей длины,
     * со скруглёнными стыками.
     */
    private static void strokePath(Canvas canvas, float s, float progress, float width, int color, float... pts) {
        if (progress <= 0.001f) {
            return;
        }
        float total = 0f;
        for (int i = 2; i < pts.length; i += 2) {
            total += (float) Math.hypot((pts[i] - pts[i - 2]) * s, (pts[i + 1] - pts[i - 1]) * s);
        }
        float left = total * Math.min(progress, 1f);
        Brush cap = Brush.solid(color);
        canvas.fillCircle(pts[0] * s, pts[1] * s, width / 2f, cap);
        for (int i = 2; i < pts.length && left > 0f; i += 2) {
            float x0 = pts[i - 2] * s;
            float y0 = pts[i - 1] * s;
            float x1 = pts[i] * s;
            float y1 = pts[i + 1] * s;
            float len = (float) Math.hypot(x1 - x0, y1 - y0);
            float k = Math.min(1f, left / len);
            float ex = x0 + (x1 - x0) * k;
            float ey = y0 + (y1 - y0) * k;
            canvas.drawLine(x0, y0, ex, ey, width, color);
            canvas.fillCircle(ex, ey, width / 2f, cap);
            left -= len;
        }
    }

    /** Сердце из двух кругов и «воздушного змея» под ними, масштаб {@code k} от центра. */
    private static void heart(Canvas canvas, float s, float k, int color) {
        float c = s / 2f;
        float u = s * k;
        Brush b = Brush.solid(color);
        float r = 0.22f * u;
        canvas.fillCircle(c + (0.30f - 0.5f) * u, c + (0.38f - 0.5f) * u, r, b);
        canvas.fillCircle(c + (0.70f - 0.5f) * u, c + (0.38f - 0.5f) * u, r, b);
        canvas.fillQuad(
                c + (0.147f - 0.5f) * u, c + (0.538f - 0.5f) * u,
                c, c + (0.40f - 0.5f) * u,
                c + (0.853f - 0.5f) * u, c + (0.538f - 0.5f) * u,
                c, c + (0.88f - 0.5f) * u,
                color, color, color, color);
    }

    /** Пятиконечная звезда из десяти треугольников вокруг центра. */
    private static void star(Canvas canvas, float s, float k, int color) {
        float cx = s / 2f;
        float cy = s * 0.53f;
        float outer = s * 0.52f * k;
        float inner = outer * 0.45f;
        float px = 0f;
        float py = 0f;
        for (int i = 0; i <= 10; i++) {
            double a = -Math.PI / 2 + i * Math.PI / 5;
            float r = i % 2 == 0 ? outer : inner;
            float x = cx + (float) Math.cos(a) * r;
            float y = cy + (float) Math.sin(a) * r;
            if (i > 0) {
                canvas.fillTriangle(cx, cy, px, py, x, y, color);
            }
            px = x;
            py = y;
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }
}
