package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Spring;
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
import io.github.ssbaxys.lumina.style.Styles;

import java.util.List;
import java.util.function.IntPredicate;

/**
 * Индикатор шагов: пронумерованные кружки, соединённые линией прогресса, и подписи под ними.
 * Пройденные шаги отмечены галочкой, текущий — обведён и мягко пульсирует, линия плавно (пружиной)
 * дотягивается до текущего шага. Номер шага — {@link Signal} с нуля.
 *
 * <pre>{@code
 * new Stepper(step, "Аккаунт", "Профиль", "Готово").clickable(i -> i < step.peek());
 * }</pre>
 */
public class Stepper extends Widget<Stepper> {
    private static final float DOT = 18f;
    private static final float LABEL_GAP = 4f;

    private final Signal<Integer> step;
    private final List<String> titles;
    private IntPredicate clickable = i -> false;
    private AnimatedFloat progress;
    private Subscription subscription;
    private int hover = -1;

    public Stepper(Signal<Integer> step, String... titles) {
        this(step, List.of(titles));
    }

    /** Подписи из живого списка: {@link Wizard} дописывает в него шаги. */
    Stepper(Signal<Integer> step, List<String> titles) {
        super("stepper");
        this.step = step;
        this.titles = titles;
    }

    /** Можно ли перейти к шагу кликом по кружку (по умолчанию нельзя). */
    public Stepper clickable(IntPredicate clickable) {
        this.clickable = clickable == null ? i -> false : clickable;
        return this;
    }

    public int stepCount() {
        return titles.size();
    }

    private int current() {
        Integer i = step.peek();
        return i == null ? 0 : Math.max(0, Math.min(titles.size() - 1, i));
    }

    /** Положение линии прогресса в шагах (дробное во время анимации). */
    public float progress() {
        return progress == null ? current() : progress.get();
    }

    @Override
    protected void onAttach() {
        progress = new AnimatedFloat(root().animator(), current()).spring(Spring.GENTLE);
        subscription = step.subscribe(i -> progress.animateTo(current()));
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float w = 0f;
        for (String t : titles) {
            w += Math.max(DOT, m.width(t)) + 16f;
        }
        setContentSize(Float.isInfinite(maxWidth) ? w : Math.max(w, maxWidth), DOT + LABEL_GAP + m.lineHeight());
    }

    /** Центр кружка {@code i} по X: шаги равномерно, крайние подписи не выходят за края. */
    private float centerX(int i, float left, float inner, TextMetrics m) {
        int n = titles.size();
        if (n == 1) {
            return left + inner / 2f;
        }
        float first = Math.max(DOT, m.width(titles.get(0))) / 2f;
        float last = Math.max(DOT, m.width(titles.get(n - 1))) / 2f;
        return left + first + (inner - first - last) * i / (n - 1);
    }

    private int stepAt(float lx, float ly) {
        if (!Lumina.isInitialized()) {
            return -1;
        }
        TextMetrics m = Lumina.platform().textMetrics();
        float left = padding().left();
        float inner = width() - padding().horizontal();
        for (int i = 0; i < titles.size(); i++) {
            float cx = centerX(i, left, inner, m);
            float half = Math.max(DOT, m.width(titles.get(i))) / 2f;
            if (Math.abs(lx - cx) <= half && ly >= padding().top() && ly <= height()) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.MOVE) {
            PointerEvent p = (PointerEvent) event;
            int i = stepAt(p.localX(), p.localY());
            hover = i >= 0 && clickable.test(i) ? i : -1;
        } else if (event.type() == PointerEvent.LEAVE) {
            hover = -1;
        } else if (event.type() == PointerEvent.CLICK && ((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
            PointerEvent p = (PointerEvent) event;
            int i = stepAt(p.localX(), p.localY());
            if (i >= 0 && i != current() && clickable.test(i) && isEnabled()) {
                step.set(i);
                event.consume();
            }
        }
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        int n = titles.size();
        if (n == 0) {
            return;
        }
        TextMetrics m = canvas.textMetrics();
        int text = style.get(Styles.TEXT_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        int onAccent = Color.luminance(accent) > 0.5f ? 0xFF1D2330 : Color.WHITE;
        float left = padding().left();
        float inner = width() - padding().horizontal();
        float top = padding().top();
        float cy = top + DOT / 2f;
        float r = DOT / 2f;
        float p = progress();
        int cur = current();

        // Линия: серая подложка и акцентная часть до текущего положения.
        float x0 = centerX(0, left, inner, m);
        float x1 = centerX(n - 1, left, inner, m);
        canvas.fillRoundRect(x0, cy - 1f, x1 - x0, 2f, Radii.all(1f), Brush.solid(Color.withAlpha(text, 0x30)));
        if (n > 1 && p > 0f) {
            int seg = Math.min(n - 2, (int) Math.floor(p));
            float a = centerX(seg, left, inner, m);
            float b = centerX(seg + 1, left, inner, m);
            float px = a + (b - a) * Math.min(1f, p - seg);
            canvas.fillRoundRect(x0, cy - 1f, px - x0, 2f, Radii.all(1f), Brush.solid(accent));
        }

        long ms = Lumina.isInitialized() ? Lumina.platform().nanoTime() / 1_000_000L : 0L;
        float pulse = 0.5f + 0.5f * (float) Math.sin(ms / 1000f * Math.PI * 1.6f);
        for (int i = 0; i < n; i++) {
            float cx = centerX(i, left, inner, m);
            // Насколько кружок «залит»: плавно, по мере подхода линии.
            float fill = Math.max(0f, Math.min(1f, p - i + 1f));
            boolean done = i < cur;
            boolean active = i == cur;
            if (active) {
                canvas.fillCircle(cx, cy, r + 2f + pulse * 1.5f, Brush.solid(Color.withAlpha(accent, (int) (0x30 + 0x30 * pulse))));
            }
            int base = Color.withAlpha(text, 0x18);
            canvas.fillCircle(cx, cy, r, Brush.solid(Color.lerp(base, accent, fill)));
            if (fill < 0.999f) {
                canvas.strokeCircle(cx, cy, r - 0.5f, 1f, Brush.solid(Color.withAlpha(text, i == hover ? 0xB0 : 0x60)));
            } else if (i == hover) {
                canvas.strokeCircle(cx, cy, r + 1f, 1f, Brush.solid(Color.withAlpha(onAccent, 0xA0)));
            }
            int mark = fill > 0.5f ? onAccent : text;
            if (done) {
                canvas.drawLine(cx - 4f, cy + 0.5f, cx - 1f, cy + 3.5f, 1.6f, mark);
                canvas.drawLine(cx - 1f, cy + 3.5f, cx + 4.5f, cy - 3f, 1.6f, mark);
            } else {
                String num = String.valueOf(i + 1);
                canvas.drawText(num, cx - m.width(num) / 2f + 0.5f, cy - m.lineHeight() / 2f + 1f, mark, false);
            }
            String title = titles.get(i);
            int tc = active ? text : Color.withAlpha(text, done ? 0xC0 : 0x80);
            canvas.drawText(title, cx - m.width(title) / 2f, top + DOT + LABEL_GAP, tc, false);
        }
    }
}
