package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Spring;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.Layout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;

/**
 * Сворачиваемая карточка-секция: заголовок со стрелкой и содержимое, которое плавно
 * раскрывается по высоте. Клик по заголовку (или Enter/Space в фокусе) сворачивает.
 */
public class Collapsible extends Widget<Collapsible> {
    private static final float HEADER = 26f;

    private final String title;
    private final Signal<Boolean> open;
    private final Panel body;
    private AnimatedFloat progress;
    private Subscription subscription;

    public Collapsible(String title, UiElement... content) {
        this(title, Signal.of(true), content);
    }

    public Collapsible(String title, Signal<Boolean> open, UiElement... content) {
        super("section");
        this.title = title;
        this.open = open;
        this.body = Panel.box(FlexLayout.column().gap(6)).add(content);
        body.width(Dim.FILL);
        setFocusable(true);
        setClipChildren(true);
        layoutParams().width = Dim.FILL;
        setLayout(new Layout() {
            @Override
            public void measure(UiElement container, float maxWidth, float maxHeight) {
                body.measure(maxWidth, Float.POSITIVE_INFINITY);
                float t = expanded();
                float w = Float.isInfinite(maxWidth) ? body.measuredWidth() : maxWidth;
                container.setContentSize(w, HEADER + body.measuredHeight() * t);
            }

            @Override
            public void arrange(UiElement container, float x, float y, float w, float h) {
                body.measure(w, Float.POSITIVE_INFINITY);
                body.setBounds(x, y + HEADER, w, body.measuredHeight());
            }
        });
        addChild(body);
    }

    public Signal<Boolean> open() {
        return open;
    }

    /** Содержимое секции: сюда можно добавлять строки. */
    public Panel body() {
        return body;
    }

    public Collapsible toggle() {
        open.set(!Boolean.TRUE.equals(open.peek()));
        return this;
    }

    private float expanded() {
        if (progress != null) {
            return Math.max(0f, Math.min(1f, progress.get()));
        }
        return Boolean.TRUE.equals(open.peek()) ? 1f : 0f;
    }

    @Override
    protected void onAttach() {
        progress = new AnimatedFloat(root().animator(), expanded()).spring(Spring.SNAPPY)
                .listener(v -> invalidateLayout());
        subscription = open.subscribe(o -> progress.animateTo(o ? 1f : 0f));
        body.setVisible(expanded() > 0f || Boolean.TRUE.equals(open.peek()));
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    @Override
    protected void onLayout() {
        // Свёрнутое содержимое не участвует во вводе и фокусе.
        body.setVisible(expanded() > 0.001f || Boolean.TRUE.equals(open.peek()));
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.CLICK && event.target() == this
                && ((PointerEvent) event).button() == Keys.MOUSE_LEFT
                && ((PointerEvent) event).localY() < padding().top() + HEADER) {
            toggle();
            event.consume();
        } else if (event.type() == KeyEvent.PRESS && event.target() == this) {
            int key = ((KeyEvent) event).key();
            if (key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.SPACE) {
                toggle();
                event.consume();
            }
        }
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        float top = padding().top();
        float ty = top + (HEADER - m.lineHeight()) / 2f - 3f;
        canvas.drawText(title, padding().left(), ty, style.get(Styles.ACCENT_COLOR), false);
        // Стрелка: «^» открыто, «v» закрыто, поворачивается вместе с раскрытием.
        float t = expanded();
        float cx = width() - padding().right() - 6f;
        float cy = ty + m.lineHeight() / 2f - 0.5f;
        float dir = 1f - 2f * t;
        int c = style.get(Styles.TEXT_COLOR);
        canvas.drawLine(cx - 4f, cy - 2f * dir, cx, cy + 2f * dir, 1.5f, c);
        canvas.drawLine(cx, cy + 2f * dir, cx + 4f, cy - 2f * dir, 1.5f, c);
    }
}
