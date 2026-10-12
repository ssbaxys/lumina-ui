package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Spring;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.node.TabSwitchable;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.List;

/**
 * Строка вкладок с подчёркиванием, которое пружиной переезжает к выбранной вкладке.
 * Выбор — индекс в {@link Signal}; {@link #stretch} раскладывает вкладки равномерно на всю ширину.
 */
public class Tabs extends Widget<Tabs> implements TabSwitchable {
    private final Signal<Integer> selected;
    private final List<String> titles;
    private float gap = 20f;
    private boolean stretch;
    private float[] xs = new float[0];
    private float[] ws = new float[0];
    private int hover = -1;
    private AnimatedFloat indicatorX;
    private AnimatedFloat indicatorW;
    private Subscription subscription;

    public Tabs(Signal<Integer> selected, String... titles) {
        super("tabs");
        this.selected = selected;
        this.titles = List.of(titles);
        setFocusable(true);
    }

    public Signal<Integer> selected() {
        return selected;
    }

    /** Равномерно по всей ширине (как в окнах настроек). */
    public Tabs stretch(boolean stretch) {
        this.stretch = stretch;
        invalidateLayout();
        return this;
    }

    public Tabs gap(float gap) {
        this.gap = gap;
        invalidateLayout();
        return this;
    }

    /** Соседняя вкладка по кругу (бамперы геймпада). */
    @Override
    public boolean switchTab(int delta) {
        int n = titles.size();
        if (n <= 1 || !isEnabled()) {
            return false;
        }
        selected.set(Math.floorMod(index() + delta, n));
        return true;
    }

    private int index() {
        Integer i = selected.peek();
        return i == null ? 0 : Math.max(0, Math.min(titles.size() - 1, i));
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float total = 0f;
        for (String t : titles) {
            total += m.width(t);
        }
        total += gap * Math.max(0, titles.size() - 1);
        setContentSize(total, m.lineHeight() + 8f);
    }

    @Override
    protected void onLayout() {
        TextMetrics m = Lumina.platform().textMetrics();
        int n = titles.size();
        xs = new float[n];
        ws = new float[n];
        float left = padding().left();
        float inner = width() - padding().horizontal();
        float x = left;
        for (int i = 0; i < n; i++) {
            ws[i] = m.width(titles.get(i));
            if (stretch) {
                float slot = inner / n;
                xs[i] = left + slot * i + (slot - ws[i]) / 2f;
            } else {
                xs[i] = x;
                x += ws[i] + gap;
            }
        }
        if (indicatorX != null && n > 0) {
            indicatorX.animateTo(xs[index()]);
            indicatorW.animateTo(ws[index()]);
        }
    }

    @Override
    protected void onAttach() {
        indicatorX = new AnimatedFloat(root().animator(), 0f).spring(Spring.SNAPPY);
        indicatorW = new AnimatedFloat(root().animator(), 0f).spring(Spring.SNAPPY);
        subscription = selected.subscribe(i -> {
            if (index() < xs.length) {
                indicatorX.animateTo(xs[index()]);
                indicatorW.animateTo(ws[index()]);
            }
        });
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    private int tabAt(float lx) {
        for (int i = 0; i < xs.length; i++) {
            float half = stretch ? (width() - padding().horizontal()) / xs.length / 2f : gap / 2f + ws[i] / 2f;
            float center = xs[i] + ws[i] / 2f;
            if (lx >= center - half && lx < center + half) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.MOVE) {
            hover = tabAt(((PointerEvent) event).localX());
        } else if (event.type() == PointerEvent.LEAVE) {
            hover = -1;
        } else if (event.type() == PointerEvent.CLICK && ((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
            int i = tabAt(((PointerEvent) event).localX());
            if (i >= 0 && isEnabled()) {
                selected.set(i);
                event.consume();
            }
        } else if (event.type() == KeyEvent.PRESS && event.target() == this) {
            int key = ((KeyEvent) event).key();
            if (key == Keys.LEFT || key == Keys.RIGHT) {
                int next = Math.floorMod(index() + (key == Keys.LEFT ? -1 : 1), titles.size());
                selected.set(next);
                event.consume();
            }
        }
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        int text = style.get(Styles.TEXT_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        float ty = padding().top() + 1f;
        int current = index();
        for (int i = 0; i < xs.length; i++) {
            int c = i == current ? text : Color.withAlpha(text, i == hover ? 0xD8 : 0x90);
            canvas.drawText(titles.get(i), xs[i], ty, c, false);
        }
        if (indicatorX != null && xs.length > 0) {
            float w = indicatorW.get() + 8f;
            float x = indicatorX.get() - 4f;
            float y = ty + m.lineHeight() + 4f;
            canvas.fillRoundRect(x, y, w, 2f, Radii.all(1f), Brush.solid(accent));
        }
    }
}
