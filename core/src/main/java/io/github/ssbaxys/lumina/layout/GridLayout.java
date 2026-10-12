package io.github.ssbaxys.lumina.layout;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.node.UiElement;

import java.util.List;

/**
 * Сетка из {@code columns} колонок одинаковой ширины: дети идут по строкам слева направо, высота строки —
 * по самому высокому в ней. Ребёнок растягивается на ширину колонки и высоту строки, если его размер не задан.
 */
public final class GridLayout implements Layout {
    private int columns;
    private float columnGap;
    private float rowGap;

    public GridLayout(int columns, float gap) {
        this(columns, gap, gap);
    }

    public GridLayout(int columns, float columnGap, float rowGap) {
        this.columns = Math.max(1, columns);
        this.columnGap = columnGap;
        this.rowGap = rowGap;
    }

    public int columns() {
        return columns;
    }

    public GridLayout columns(int columns) {
        this.columns = Math.max(1, columns);
        return this;
    }

    public float columnGap() {
        return columnGap;
    }

    public float rowGap() {
        return rowGap;
    }

    @Override
    public void measure(UiElement container, float maxWidth, float maxHeight) {
        List<UiElement> children = container.children();
        float colW = Float.isFinite(maxWidth)
                ? Math.max(0f, (maxWidth - columnGap * (columns - 1)) / columns) : Float.POSITIVE_INFINITY;
        float widest = 0f;
        float total = 0f;
        float rowH = 0f;
        int col = 0;
        int rows = 0;
        for (int i = 0, n = children.size(); i < n; i++) {
            UiElement child = children.get(i);
            if (!child.isVisible()) {
                continue;
            }
            LayoutParams p = child.layoutParams();
            Insets m = p.margin;
            child.measure(Math.max(0f, colW - m.horizontal()), Float.POSITIVE_INFINITY);
            if (p.absolute) {
                continue;
            }
            widest = Math.max(widest, child.measuredWidth() + m.horizontal());
            rowH = Math.max(rowH, child.measuredHeight() + m.vertical());
            if (++col == columns) {
                total += rowH;
                rows++;
                rowH = 0f;
                col = 0;
            }
        }
        if (col > 0) {
            total += rowH;
            rows++;
        }
        if (rows > 1) {
            total += rowGap * (rows - 1);
        }
        int used = rows > 1 ? columns : col;
        float width = Float.isFinite(maxWidth) ? maxWidth : widest * used + columnGap * Math.max(0, used - 1);
        container.setContentSize(rows == 0 ? 0f : width, total);
    }

    @Override
    public void arrange(UiElement container, float cx, float cy, float cw, float ch) {
        List<UiElement> children = container.children();
        float colW = Math.max(0f, (cw - columnGap * (columns - 1)) / columns);
        int n = children.size();
        float y = cy;
        int i = 0;
        while (i < n) {
            // Строка: до columns видимых детей в потоке; высота — по самому высокому.
            int count = 0;
            float rowH = 0f;
            int j = i;
            while (j < n && count < columns) {
                UiElement child = children.get(j);
                LayoutParams p = child.layoutParams();
                if (child.isVisible() && !p.absolute) {
                    rowH = Math.max(rowH, child.measuredHeight() + p.margin.vertical());
                    count++;
                }
                j++;
            }
            int col = 0;
            for (int k = i; k < j; k++) {
                UiElement child = children.get(k);
                LayoutParams p = child.layoutParams();
                if (!child.isVisible()) {
                    continue;
                }
                if (p.absolute) {
                    Layout.arrangeAbsolute(child, cx, cy, cw, ch);
                    continue;
                }
                Insets m = p.margin;
                float slotW = Math.max(0f, colW - m.horizontal());
                float w = stretches(p.width) ? p.clampWidth(slotW) : Math.min(child.measuredWidth(), slotW);
                float h = stretches(p.height) ? p.clampHeight(rowH - m.vertical()) : child.measuredHeight();
                child.setBounds(cx + col * (colW + columnGap) + m.left(), y + m.top(), w, Math.max(0f, h));
                col++;
            }
            if (count > 0) {
                y += rowH + rowGap;
            }
            i = j;
        }
    }

    private static boolean stretches(Dim d) {
        return d.isAuto() || d.kind() == Dim.Kind.FILL;
    }
}
