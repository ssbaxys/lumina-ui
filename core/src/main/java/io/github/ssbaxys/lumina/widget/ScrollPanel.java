package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Spring;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.layout.Layout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.State;

/**
 * Вертикальная прокрутка одного дочернего элемента с плавной (пружинной) инерцией
 * и перетаскиваемым ползунком.
 */
public class ScrollPanel extends Widget<ScrollPanel> {
    private static final float SCROLL_STEP = 24f;
    private static final float BAR_WIDTH = 4f;

    private final UiElement content;
    private AnimatedFloat offset;
    private float scrollTarget;
    private boolean draggingBar;
    private float dragStartY;
    private float dragStartOffset;

    public ScrollPanel(UiElement content) {
        super("scroll");
        this.content = content;
        setClipChildren(true);
        setLayout(new ScrollLayout());
        addChild(content);
    }

    public UiElement content() {
        return content;
    }

    public float maxScroll() {
        return Math.max(0f, content.measuredHeight() - (height() - padding().vertical()));
    }

    public float scrollOffset() {
        return offset == null ? 0f : offset.get();
    }

    public void scrollTo(float y, boolean animate) {
        scrollTarget = Math.max(0f, Math.min(maxScroll(), y));
        if (offset == null) {
            return;
        }
        if (animate) {
            offset.animateTo(scrollTarget);
        } else {
            offset.snapTo(scrollTarget);
        }
    }

    @Override
    protected void onAttach() {
        offset = new AnimatedFloat(root().animator(), scrollTarget)
                .spring(Spring.SNAPPY)
                .listener(v -> content.setBounds(content.x(), padding().top() - v, content.width(), content.height()));
    }

    @Override
    protected void onDetach() {
        if (offset != null) {
            offset.snapTo(offset.target());
        }
    }

    @Override
    protected void onLayout() {
        if (scrollTarget > maxScroll()) {
            scrollTo(maxScroll(), false);
        }
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == ScrollEvent.SCROLL) {
            float max = maxScroll();
            if (max <= 0f) {
                return;
            }
            scrollTo(scrollTarget - ((ScrollEvent) event).deltaY() * SCROLL_STEP, true);
            event.consume();
            event.stopPropagation();
        } else if (event.type() == PointerEvent.PRESS) {
            PointerEvent p = (PointerEvent) event;
            if (p.localX() >= width() - BAR_WIDTH * 3f && maxScroll() > 0f) {
                draggingBar = true;
                dragStartY = p.y();
                dragStartOffset = scrollTarget;
                event.consume();
            }
        } else if (event.type() == PointerEvent.DRAG && draggingBar) {
            PointerEvent p = (PointerEvent) event;
            float track = height() - thumbHeight();
            if (track > 0f) {
                scrollTo(dragStartOffset + (p.y() - dragStartY) / track * maxScroll(), false);
            }
            event.consume();
        } else if (event.type() == PointerEvent.RELEASE) {
            draggingBar = false;
        }
    }

    private float thumbHeight() {
        float viewport = height();
        float total = content.measuredHeight() + padding().vertical();
        return Math.max(16f, viewport * viewport / Math.max(total, 1f));
    }

    @Override
    protected void paintOverlay(Canvas canvas, float partialTick) {
        float max = maxScroll();
        if (max <= 0f) {
            return;
        }
        float th = thumbHeight();
        float ty = (height() - th) * (scrollOffset() / max);
        boolean active = hasState(State.HOVERED) || draggingBar;
        int color = active ? 0xB0FFFFFF : 0x50FFFFFF;
        canvas.fillRoundRect(width() - BAR_WIDTH - 2f, ty + 2f, BAR_WIDTH, th - 4f, Radii.all(BAR_WIDTH / 2f),
                Brush.solid(color));
    }

    /** Ребёнок измеряется с бесконечной высотой и сдвигается на текущий offset. */
    private final class ScrollLayout implements Layout {
        @Override
        public void measure(UiElement container, float maxWidth, float maxHeight) {
            content.measure(maxWidth, Float.POSITIVE_INFINITY);
            container.setContentSize(content.measuredWidth(), Math.min(content.measuredHeight(), maxHeight));
        }

        @Override
        public void arrange(UiElement container, float cx, float cy, float cw, float ch) {
            content.measure(cw, Float.POSITIVE_INFINITY);
            content.setBounds(cx, cy - scrollOffset(), cw, content.measuredHeight());
        }
    }
}
