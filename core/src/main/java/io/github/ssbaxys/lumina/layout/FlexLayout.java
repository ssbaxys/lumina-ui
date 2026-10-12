package io.github.ssbaxys.lumina.layout;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.node.UiElement;

import java.util.List;

/**
 * Упрощённый flexbox: строка или колонка, gap, grow/shrink, justify и align.
 * Не выделяет память при раскладке: промежуточный размер хранится в самом ребёнке.
 */
public final class FlexLayout implements Layout {
    private Direction direction;
    private float gap;
    private Justify justify = Justify.START;
    private Align align = Align.STRETCH;

    public FlexLayout(Direction direction) {
        this.direction = direction;
    }

    public static FlexLayout row() {
        return new FlexLayout(Direction.ROW);
    }

    public static FlexLayout column() {
        return new FlexLayout(Direction.COLUMN);
    }

    public FlexLayout direction(Direction d) {
        this.direction = d;
        return this;
    }

    public FlexLayout gap(float gap) {
        this.gap = gap;
        return this;
    }

    public FlexLayout justify(Justify justify) {
        this.justify = justify;
        return this;
    }

    public FlexLayout align(Align align) {
        this.align = align;
        return this;
    }

    public Direction direction() {
        return direction;
    }

    public float gap() {
        return gap;
    }

    public Justify justify() {
        return justify;
    }

    public Align align() {
        return align;
    }

    @Override
    public void measure(UiElement container, float maxWidth, float maxHeight) {
        boolean row = direction == Direction.ROW;
        float main = 0f;
        float cross = 0f;
        int count = 0;
        List<UiElement> children = container.children();
        for (int i = 0, n = children.size(); i < n; i++) {
            UiElement child = children.get(i);
            if (!child.isVisible()) {
                continue;
            }
            LayoutParams p = child.layoutParams();
            Insets m = p.margin;
            child.measure(Math.max(0f, maxWidth - m.horizontal()), Math.max(0f, maxHeight - m.vertical()));
            if (p.absolute) {
                continue;
            }
            float w = child.measuredWidth() + m.horizontal();
            float h = child.measuredHeight() + m.vertical();
            if (row) {
                main += isFill(p.width) ? m.horizontal() : w;
                cross = Math.max(cross, isFill(p.height) ? m.vertical() : h);
            } else {
                main += isFill(p.height) ? m.vertical() : h;
                cross = Math.max(cross, isFill(p.width) ? m.horizontal() : w);
            }
            count++;
        }
        if (count > 1) {
            main += gap * (count - 1);
        }
        if (row) {
            container.setContentSize(main, cross);
        } else {
            container.setContentSize(cross, main);
        }
    }

    @Override
    public void arrange(UiElement container, float cx, float cy, float cw, float ch) {
        boolean row = direction == Direction.ROW;
        float mainSize = row ? cw : ch;
        float crossSize = row ? ch : cw;
        List<UiElement> children = container.children();
        int n = children.size();

        // 1. Базовые размеры и сумма коэффициентов.
        float used = 0f;
        float totalGrow = 0f;
        float totalShrink = 0f;
        int count = 0;
        for (int i = 0; i < n; i++) {
            UiElement child = children.get(i);
            if (!child.isVisible()) {
                continue;
            }
            LayoutParams p = child.layoutParams();
            if (p.absolute) {
                Layout.arrangeAbsolute(child, cx, cy, cw, ch);
                continue;
            }
            Dim mainDim = row ? p.width : p.height;
            float grow = p.grow;
            float basis;
            if (isFill(mainDim)) {
                basis = 0f;
                grow = Math.max(grow, 1f);
            } else {
                basis = row ? child.measuredWidth() : child.measuredHeight();
            }
            child.setLayoutSlot(basis);
            used += basis + (row ? p.margin.horizontal() : p.margin.vertical());
            totalGrow += grow;
            totalShrink += p.shrink * basis;
            count++;
        }
        if (count > 1) {
            used += gap * (count - 1);
        }

        // 2. Раздать свободное место (grow) или отнять недостающее (shrink).
        float free = mainSize - used;
        if (Float.isFinite(free) && (free > 0f && totalGrow > 0f || free < 0f && totalShrink > 0f)) {
            float actualUsed = 0f;
            for (int i = 0; i < n; i++) {
                UiElement child = children.get(i);
                LayoutParams p = child.layoutParams();
                if (!child.isVisible() || p.absolute) {
                    continue;
                }
                float basis = child.layoutSlot();
                float size;
                if (free > 0f) {
                    float grow = isFill(row ? p.width : p.height) ? Math.max(p.grow, 1f) : p.grow;
                    size = basis + free * grow / totalGrow;
                } else {
                    size = Math.max(0f, basis + free * (p.shrink * basis) / totalShrink);
                }
                size = row ? p.clampWidth(size) : p.clampHeight(size);
                child.setLayoutSlot(size);
                actualUsed += size - basis;
            }
            free -= actualUsed;
        }

        // 3. Justify.
        float pos = row ? cx : cy;
        float between = gap;
        float leftover = Float.isFinite(free) ? Math.max(0f, free) : 0f;
        switch (justify) {
            case CENTER -> pos += leftover / 2f;
            case END -> pos += leftover;
            case SPACE_BETWEEN -> between += count > 1 ? leftover / (count - 1) : 0f;
            case SPACE_AROUND -> {
                float s = count > 0 ? leftover / count : 0f;
                pos += s / 2f;
                between += s;
            }
            case SPACE_EVENLY -> {
                float s = leftover / (count + 1);
                pos += s;
                between += s;
            }
            default -> {
            }
        }

        // 4. Позиции и поперечная ось.
        for (int i = 0; i < n; i++) {
            UiElement child = children.get(i);
            LayoutParams p = child.layoutParams();
            if (!child.isVisible() || p.absolute) {
                continue;
            }
            Insets m = p.margin;
            float mainLen = child.layoutSlot();
            Dim crossDim = row ? p.height : p.width;
            float crossMarginStart = row ? m.top() : m.left();
            float crossMargins = row ? m.vertical() : m.horizontal();
            float availCross = Math.max(0f, crossSize - crossMargins);
            Align a = p.alignSelf != null ? p.alignSelf : align;
            float crossLen;
            if (isFill(crossDim) || (a == Align.STRETCH && crossDim.isAuto() && Float.isFinite(availCross))) {
                crossLen = row ? p.clampHeight(availCross) : p.clampWidth(availCross);
            } else {
                crossLen = row ? child.measuredHeight() : child.measuredWidth();
            }
            float crossPos = (row ? cy : cx) + crossMarginStart;
            if (Float.isFinite(availCross)) {
                if (a == Align.CENTER) {
                    crossPos += (availCross - crossLen) / 2f;
                } else if (a == Align.END) {
                    crossPos += availCross - crossLen;
                }
            }
            pos += row ? m.left() : m.top();
            if (row) {
                child.setBounds(pos, crossPos, mainLen, crossLen);
            } else {
                child.setBounds(crossPos, pos, crossLen, mainLen);
            }
            pos += mainLen + (row ? m.right() : m.bottom()) + between;
        }
    }

    private static boolean isFill(Dim d) {
        return d.kind() == Dim.Kind.FILL;
    }
}
