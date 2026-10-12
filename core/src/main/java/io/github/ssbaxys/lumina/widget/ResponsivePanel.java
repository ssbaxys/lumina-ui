package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.layout.Breakpoint;
import io.github.ssbaxys.lumina.layout.Layout;
import io.github.ssbaxys.lumina.layout.ResponsiveLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Subscription;

/**
 * Контейнер, раскладка которого зависит от ширины экрана ({@link Breakpoint}): строка на широком экране
 * и колонка на узком, или сетка с разным числом колонок.
 *
 * <pre>{@code
 * ResponsivePanel.rowFrom(Breakpoint.MEDIUM, 8).add(sidebar, content);   // узкий экран — колонка
 * ResponsivePanel.columns(1, 2, 3, 6).add(cards);                         // 1/2/3 колонки
 * new ResponsivePanel().compact(FlexLayout.column()).wide(new GridLayout(4, 6)).byContainerWidth(true);
 * }</pre>
 *
 * Класс берётся у корня ({@code UiRoot.breakpoint()}); при его смене панель перекладывается сама.
 * {@link #byContainerWidth(boolean)} — по собственной доступной ширине.
 */
public class ResponsivePanel extends Widget<ResponsivePanel> {
    private final ResponsiveLayout layout;
    private Subscription subscription;

    /** Прозрачная панель (без класса стиля); раскладки задаются {@link #compact}, {@link #medium}, {@link #wide}. */
    public ResponsivePanel() {
        this(null, new ResponsiveLayout());
    }

    public ResponsivePanel(String styleClass, ResponsiveLayout layout) {
        super(styleClass);
        this.layout = layout;
        setLayout(layout);
    }

    /** Колонка на узких экранах, строка начиная с {@code rowFrom}. */
    public static ResponsivePanel rowFrom(Breakpoint rowFrom, float gap) {
        return new ResponsivePanel(null, ResponsiveLayout.rowFrom(rowFrom, gap));
    }

    /** Сетка: число колонок на узком, среднем и широком экране. */
    public static ResponsivePanel columns(int compact, int medium, int wide, float gap) {
        return new ResponsivePanel(null, ResponsiveLayout.columns(compact, medium, wide, gap));
    }

    public ResponsiveLayout responsiveLayout() {
        return layout;
    }

    public ResponsivePanel compact(Layout l) {
        layout.compact(l);
        invalidateLayout();
        return this;
    }

    public ResponsivePanel medium(Layout l) {
        layout.medium(l);
        invalidateLayout();
        return this;
    }

    public ResponsivePanel wide(Layout l) {
        layout.wide(l);
        invalidateLayout();
        return this;
    }

    /** Выбирать раскладку по собственной ширине панели, а не всего экрана. */
    public ResponsivePanel byContainerWidth(boolean on) {
        layout.byContainerWidth(on);
        invalidateLayout();
        return this;
    }

    /** Класс ширины при последней раскладке. */
    public Breakpoint current() {
        return layout.current();
    }

    public ResponsivePanel add(UiElement... elements) {
        for (UiElement e : elements) {
            addChild(e);
        }
        return this;
    }

    @Override
    protected void onAttach() {
        if (subscription == null) {
            subscription = root().breakpoint().subscribe(bp -> invalidateLayout());
        }
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }
}
