package io.github.ssbaxys.lumina.hud;

import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * Стопка тостов: показывает не больше {@link #maxVisible} сразу, остальные ждут в очереди и выходят,
 * когда освобождается место. Новые добавляются снизу (или сверху — {@link #newestOnTop}); ушедший тост
 * плавно схлопывается, и соседи подтягиваются. Общая стопка HUD — {@link Hud#toasts()}, но её можно
 * поставить и в обычный экран.
 */
public class ToastStack extends Widget<ToastStack> {
    private final List<Toast> queue = new ArrayList<>();
    private int maxVisible = 5;
    private float gap = 4f;
    private boolean newestOnTop;
    private int slideFrom;

    public ToastStack() {
        super(null);
        setLayout(FlexLayout.column().align(Align.END));
        setHitTestable(false);
    }

    /** Сколько тостов на экране одновременно (по умолчанию 5). */
    public ToastStack maxVisible(int count) {
        this.maxVisible = Math.max(1, count);
        pullQueue();
        return this;
    }

    public ToastStack gap(float gap) {
        this.gap = gap;
        return this;
    }

    /** Новые тосты сверху (для стопки у нижнего края экрана). */
    public ToastStack newestOnTop(boolean top) {
        this.newestOnTop = top;
        return this;
    }

    /** Выравнивание тостов разной ширины: {@link Align#END} — по правому краю (по умолчанию). */
    public ToastStack align(Align align) {
        ((FlexLayout) layout()).align(align);
        invalidateLayout();
        return this;
    }

    /** Откуда въезжают: {@code 1} — справа, {@code -1} — слева, {@code 0} — от ближнего края экрана (по умолчанию). */
    public ToastStack slideFrom(int side) {
        this.slideFrom = Integer.signum(side);
        return this;
    }

    int slideFrom() {
        return slideFrom;
    }

    // ================================================================ показ

    /** Тост с заголовком и текстом (обычный, 5 секунд). */
    public Toast show(String title, String body) {
        return show(Toast.info(title, body));
    }

    /** Показать тост (или поставить в очередь, если места нет). Уже показанный не дублируется. */
    public Toast show(Toast toast) {
        if (toast.parent() == this || queue.contains(toast)) {
            return toast;
        }
        if (toast.parent() != null) {
            toast.parent().removeChild(toast);
        }
        toast.prepare(this, gap);
        if (activeCount() < maxVisible) {
            addChild(newestOnTop ? 0 : children().size(), toast);
        } else {
            queue.add(toast);
        }
        return toast;
    }

    /** Тосты на экране (включая уходящие), сверху вниз. */
    public List<Toast> visible() {
        List<Toast> out = new ArrayList<>(children().size());
        for (UiElement c : children()) {
            if (c instanceof Toast t) {
                out.add(t);
            }
        }
        return out;
    }

    /** Сколько тостов ждёт места. */
    public int queued() {
        return queue.size();
    }

    /** Убрать все: показанные уезжают, очередь очищается. */
    public void dismissAll() {
        queue.clear();
        for (Toast t : visible()) {
            t.dismiss();
        }
    }

    /** Убрать все сразу, без анимации. */
    public void clear() {
        queue.clear();
        clearChildren();
    }

    /** На экране и не уходят — столько мест занято. */
    private int activeCount() {
        int n = 0;
        for (UiElement c : children()) {
            if (c instanceof Toast t && !t.isClosing()) {
                n++;
            }
        }
        return n;
    }

    void onToastGone(Toast toast) {
        if (toast.parent() == this) {
            removeChild(toast);
        }
        queue.remove(toast);
        pullQueue();
    }

    void dropQueued(Toast toast) {
        queue.remove(toast);
    }

    private void pullQueue() {
        while (!queue.isEmpty() && activeCount() < maxVisible) {
            Toast next = queue.remove(0);
            addChild(newestOnTop ? 0 : children().size(), next);
        }
    }
}
