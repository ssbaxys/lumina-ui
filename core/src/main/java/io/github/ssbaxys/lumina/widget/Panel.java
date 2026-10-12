package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.Justify;
import io.github.ssbaxys.lumina.layout.Layout;
import io.github.ssbaxys.lumina.node.UiElement;

/** Контейнер. По умолчанию — колонка flex; раскладку можно заменить любой {@link Layout}. */
public class Panel extends Widget<Panel> {

    public Panel() {
        this("panel", FlexLayout.column());
    }

    public Panel(String styleClass, Layout layout) {
        super(styleClass);
        setLayout(layout);
    }

    public static Panel column() {
        return new Panel("panel", FlexLayout.column());
    }

    public static Panel row() {
        return new Panel("panel", FlexLayout.row());
    }

    /** Прозрачный контейнер без стиля темы (для группировки). */
    public static Panel box(Layout layout) {
        return new Panel(null, layout);
    }

    public Panel add(UiElement... elements) {
        for (UiElement e : elements) {
            addChild(e);
        }
        return this;
    }

    public Panel gap(float gap) {
        flex().gap(gap);
        invalidateLayout();
        return this;
    }

    public Panel justify(Justify justify) {
        flex().justify(justify);
        invalidateLayout();
        return this;
    }

    public Panel align(Align align) {
        flex().align(align);
        invalidateLayout();
        return this;
    }

    public Panel clip(boolean clip) {
        setClipChildren(clip);
        return this;
    }

    private FlexLayout flex() {
        if (layout() instanceof FlexLayout f) {
            return f;
        }
        throw new IllegalStateException("Panel layout is not a FlexLayout: " + layout());
    }
}
