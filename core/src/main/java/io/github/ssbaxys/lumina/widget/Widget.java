package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.event.EventListener;
import io.github.ssbaxys.lumina.event.EventType;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.style.Style;

import java.util.function.Consumer;

/**
 * Базовый класс виджетов с типобезопасным fluent-API ({@code S} — сам виджет),
 * чтобы цепочки вида {@code new Button("OK").width(Dim.px(80)).onClick(...)} не теряли тип.
 */
public abstract class Widget<S extends Widget<S>> extends UiElement {

    protected Widget(String styleClass) {
        super(styleClass);
    }

    @SuppressWarnings("unchecked")
    protected final S self() {
        return (S) this;
    }

    public S id(String id) {
        setId(id);
        return self();
    }

    public S width(Dim width) {
        layoutParams().width = width;
        invalidateLayout();
        return self();
    }

    public S height(Dim height) {
        layoutParams().height = height;
        invalidateLayout();
        return self();
    }

    public S size(float width, float height) {
        layoutParams().width = Dim.px(width);
        layoutParams().height = Dim.px(height);
        invalidateLayout();
        return self();
    }

    public S fill() {
        layoutParams().width = Dim.FILL;
        layoutParams().height = Dim.FILL;
        invalidateLayout();
        return self();
    }

    public S minSize(float minWidth, float minHeight) {
        layoutParams().minWidth = minWidth;
        layoutParams().minHeight = minHeight;
        invalidateLayout();
        return self();
    }

    public S maxSize(float maxWidth, float maxHeight) {
        layoutParams().maxWidth = maxWidth;
        layoutParams().maxHeight = maxHeight;
        invalidateLayout();
        return self();
    }

    public S grow(float grow) {
        layoutParams().grow = grow;
        invalidateLayout();
        return self();
    }

    public S shrink(float shrink) {
        layoutParams().shrink = shrink;
        invalidateLayout();
        return self();
    }

    public S margin(Insets margin) {
        layoutParams().margin = margin;
        invalidateLayout();
        return self();
    }

    public S alignSelf(Align align) {
        layoutParams().alignSelf = align;
        invalidateLayout();
        return self();
    }

    /** Абсолютная позиция внутри родителя, вне потока раскладки. */
    public S absolute(float left, float top) {
        layoutParams().absolute = true;
        layoutParams().left = left;
        layoutParams().top = top;
        invalidateLayout();
        return self();
    }

    public S style(Style style) {
        setStyle(style);
        return self();
    }

    /** Дополнить inline-стиль: {@code .style(b -> b.set(Styles.RADIUS, Radii.all(4)))}. */
    public S style(Consumer<Style.Builder> edit) {
        Style.Builder b = inlineStyle().toBuilder();
        edit.accept(b);
        setStyle(b.build());
        return self();
    }

    public S styleClass(String styleClass) {
        setStyleClass(styleClass);
        return self();
    }

    public S visible(boolean visible) {
        setVisible(visible);
        return self();
    }

    public S enabled(boolean enabled) {
        setEnabled(enabled);
        return self();
    }

    @Override
    public <E extends UiEvent> S on(EventType<E> type, EventListener<? super E> listener) {
        super.on(type, listener);
        return self();
    }

    public S onClick(EventListener<? super PointerEvent> listener) {
        return on(PointerEvent.CLICK, listener);
    }

    /** Всплывающая подсказка при наведении (строки через {@code \n}); настройка — {@link Tooltips#of}. */
    public S tooltip(String text) {
        Tooltips.attach(this, text);
        return self();
    }

    /** Подсказка своего вида: {@code .tooltip(Tooltip.Kind.MODERN, "Текст")}. */
    public S tooltip(Tooltip.Kind kind, String text) {
        Tooltips.attach(this, text).kind(kind);
        return self();
    }

    /** Подсказка с Markdown: {@code .tooltipMarkdown("**Алмаз**\n\nРедкая руда")}. */
    public S tooltipMarkdown(String markdown) {
        Tooltips.attachMarkdown(this, markdown);
        return self();
    }

    /** Произвольная настройка без разрыва цепочки. */
    public S with(Consumer<? super S> configurer) {
        configurer.accept(self());
        return self();
    }
}
