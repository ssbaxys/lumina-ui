package io.github.ssbaxys.lumina.layout;

import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;

/**
 * Раскладка, которая меняется по классу ширины ({@link Breakpoint}): например, строка на широком экране
 * и колонка на узком, или 3/2/1 колонки сетки.
 *
 * <ul>
 *   <li>По умолчанию класс берётся у корня ({@code UiRoot.breakpoint()}, ширина всего интерфейса);</li>
 *   <li>{@link #byContainerWidth(boolean)} — по ширине, доступной самому контейнеру (как container queries
 *       в CSS): полезно для боковых панелей и карточек.</li>
 * </ul>
 *
 * Не заданная для класса раскладка берётся у ближайшего меньшего класса, затем у большего.
 * Виджет-обёртка, который сам перекладывается при смене класса корня, — {@code ResponsivePanel}.
 */
public final class ResponsiveLayout implements Layout {
    private final Layout[] layouts = new Layout[Breakpoint.values().length];
    private Breakpoints breakpoints;
    private boolean byContainer;
    private Breakpoint current;

    public ResponsiveLayout() {
    }

    /** Колонка на узком экране, строка начиная с {@code rowFrom}. */
    public static ResponsiveLayout rowFrom(Breakpoint rowFrom, float gap) {
        ResponsiveLayout l = new ResponsiveLayout();
        for (Breakpoint bp : Breakpoint.values()) {
            l.set(bp, bp.atLeast(rowFrom) ? FlexLayout.row().gap(gap) : FlexLayout.column().gap(gap).align(Align.STRETCH));
        }
        return l;
    }

    /** Сетка: число колонок для каждого класса. */
    public static ResponsiveLayout columns(int compact, int medium, int wide, float gap) {
        return new ResponsiveLayout()
                .set(Breakpoint.COMPACT, new GridLayout(compact, gap))
                .set(Breakpoint.MEDIUM, new GridLayout(medium, gap))
                .set(Breakpoint.WIDE, new GridLayout(wide, gap));
    }

    public ResponsiveLayout set(Breakpoint bp, Layout layout) {
        layouts[bp.ordinal()] = layout;
        return this;
    }

    public ResponsiveLayout compact(Layout layout) {
        return set(Breakpoint.COMPACT, layout);
    }

    public ResponsiveLayout medium(Layout layout) {
        return set(Breakpoint.MEDIUM, layout);
    }

    public ResponsiveLayout wide(Layout layout) {
        return set(Breakpoint.WIDE, layout);
    }

    /** Класс ширины по ширине самого контейнера, а не корня. */
    public ResponsiveLayout byContainerWidth(boolean on) {
        this.byContainer = on;
        return this;
    }

    public boolean isByContainerWidth() {
        return byContainer;
    }

    /** Свои пороги (только для режима по ширине контейнера; у корня — {@code UiRoot.setBreakpoints}). */
    public ResponsiveLayout breakpoints(Breakpoints breakpoints) {
        this.breakpoints = breakpoints;
        return this;
    }

    /** Класс ширины при последней раскладке ({@code null} — ещё не раскладывали). */
    public Breakpoint current() {
        return current;
    }

    /** Раскладка для класса (с откатом к соседним). */
    public Layout layoutFor(Breakpoint bp) {
        for (int i = bp.ordinal(); i >= 0; i--) {
            if (layouts[i] != null) {
                return layouts[i];
            }
        }
        for (int i = bp.ordinal() + 1; i < layouts.length; i++) {
            if (layouts[i] != null) {
                return layouts[i];
            }
        }
        throw new IllegalStateException("ResponsiveLayout has no layouts");
    }

    /** Класс ширины для контейнера с доступной шириной содержимого {@code available}. */
    public Breakpoint breakpointOf(UiElement container, float available) {
        UiRoot root = container.root();
        if (byContainer && Float.isFinite(available)) {
            Breakpoints b = breakpoints != null ? breakpoints : root != null ? root.breakpoints() : Breakpoints.DEFAULT;
            return b.of(available + container.padding().horizontal());
        }
        return root != null ? root.currentBreakpoint() : Breakpoint.WIDE;
    }

    @Override
    public void measure(UiElement container, float maxWidth, float maxHeight) {
        current = breakpointOf(container, maxWidth);
        layoutFor(current).measure(container, maxWidth, maxHeight);
    }

    @Override
    public void arrange(UiElement container, float contentX, float contentY, float contentWidth, float contentHeight) {
        Breakpoint bp = current != null ? current : breakpointOf(container, contentWidth);
        layoutFor(bp).arrange(container, contentX, contentY, contentWidth, contentHeight);
    }
}
