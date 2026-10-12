package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Transition;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ScrollPanel;

/** Небольшие строительные блоки витрины. */
final class Ui {
    static final Style MUTED = Style.builder().set(Styles.OPACITY, 0.62f).build();
    static final Style WELL = Style.builder()
            .set(Styles.BACKGROUND, Brush.solid(0x18808080))
            .set(Styles.RADIUS, Radii.all(6))
            .set(Styles.PADDING, Insets.all(4))
            .build();

    private Ui() {
    }

    static Label heading(String text) {
        return new Label(text).scale(1.25f);
    }

    static Label muted(String text) {
        return new Label(text).style(MUTED);
    }

    static Panel row(float gap, UiElement... children) {
        return Panel.box(FlexLayout.row().gap(gap).align(Align.CENTER)).add(children);
    }

    static Panel column(float gap, UiElement... children) {
        return Panel.box(FlexLayout.column().gap(gap)).add(children);
    }

    static Panel spacer() {
        return Panel.box(FlexLayout.column()).grow(1);
    }

    /** Заголовок раздела, пояснение и содержимое. */
    static Panel section(String title, String hint, UiElement... content) {
        Panel p = column(5, heading(title));
        if (hint != null) {
            p.add(muted(hint));
        }
        return p.add(content).width(Dim.FILL);
    }

    /** Цветной прямоугольник для демонстрации раскладки. */
    static Panel box(int color, float w, float h) {
        return Panel.box(FlexLayout.column()).size(w, h).style(b -> b
                .set(Styles.BACKGROUND, Brush.solid(color))
                .set(Styles.RADIUS, Radii.all(3)));
    }

    /** Элемент с подписью под ним. */
    static Panel labeled(UiElement element, String caption) {
        return Panel.box(FlexLayout.column().gap(4).align(Align.CENTER)).add(element, muted(caption));
    }

    static Panel centered(UiElement... children) {
        return Panel.box(new StackLayout(Align.CENTER, Align.CENTER)).add(children);
    }

    /** Страница витрины: колонка разделов в прокручиваемой области. */
    static UiElement page(UiElement... sections) {
        Panel column = Panel.box(FlexLayout.column().gap(16).align(Align.STRETCH)).add(sections);
        column.style(b -> b.set(Styles.PADDING, new Insets(4, 12, 8, 4)));
        ScrollPanel scroll = new ScrollPanel(column).fill();
        scroll.setStyleClass(null);
        return scroll;
    }

    /** Подготовить появление: прозрачность 0 и лёгкое уменьшение (до добавления в дерево). */
    static void prepareFadeIn(UiElement e) {
        e.setStyle(e.inlineStyle().toBuilder()
                .set(Styles.OPACITY, 0f)
                .set(Styles.SCALE, 0.97f)
                .set(Styles.TRANSITION, new Transition(0.28f, Easing.EMPHASIZED))
                .build());
    }

    /** Запустить появление (после добавления в дерево): переход стиля сделает анимацию сам. */
    static void playFadeIn(UiElement e) {
        e.setStyle(e.inlineStyle().toBuilder()
                .set(Styles.OPACITY, 1f)
                .set(Styles.SCALE, 1f)
                .build());
    }
}
