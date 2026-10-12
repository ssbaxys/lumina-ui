package io.github.ssbaxys.lumina.layout;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.node.UiElement;

import java.util.List;

/** Дети накладываются друг на друга (слои), каждый выравнивается внутри области содержимого. */
public final class StackLayout implements Layout {
    private final Align horizontal;
    private final Align vertical;

    public StackLayout(Align horizontal, Align vertical) {
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public static StackLayout centered() {
        return new StackLayout(Align.CENTER, Align.CENTER);
    }

    public static StackLayout topLeft() {
        return new StackLayout(Align.START, Align.START);
    }

    @Override
    public void measure(UiElement container, float maxWidth, float maxHeight) {
        float w = 0f;
        float h = 0f;
        List<UiElement> children = container.children();
        for (int i = 0, n = children.size(); i < n; i++) {
            UiElement child = children.get(i);
            if (!child.isVisible()) {
                continue;
            }
            Insets m = child.layoutParams().margin;
            child.measure(Math.max(0f, maxWidth - m.horizontal()), Math.max(0f, maxHeight - m.vertical()));
            if (child.layoutParams().absolute) {
                continue;
            }
            w = Math.max(w, child.layoutParams().width.kind() == Dim.Kind.FILL ? m.horizontal() : child.measuredWidth() + m.horizontal());
            h = Math.max(h, child.layoutParams().height.kind() == Dim.Kind.FILL ? m.vertical() : child.measuredHeight() + m.vertical());
        }
        container.setContentSize(w, h);
    }

    @Override
    public void arrange(UiElement container, float cx, float cy, float cw, float ch) {
        List<UiElement> children = container.children();
        for (int i = 0, n = children.size(); i < n; i++) {
            UiElement child = children.get(i);
            if (!child.isVisible()) {
                continue;
            }
            LayoutParams p = child.layoutParams();
            if (p.absolute) {
                Layout.arrangeAbsolute(child, cx, cy, cw, ch);
                continue;
            }
            Insets m = p.margin;
            float aw = Math.max(0f, cw - m.horizontal());
            float ah = Math.max(0f, ch - m.vertical());
            float w = fits(p.width, horizontal) ? p.clampWidth(aw) : child.measuredWidth();
            float h = fits(p.height, vertical) ? p.clampHeight(ah) : child.measuredHeight();
            child.setBounds(cx + m.left() + offset(horizontal, aw, w), cy + m.top() + offset(vertical, ah, h), w, h);
        }
    }

    private static boolean fits(Dim d, Align a) {
        return d.kind() == Dim.Kind.FILL || (a == Align.STRETCH && d.isAuto());
    }

    private static float offset(Align a, float avail, float size) {
        if (!Float.isFinite(avail)) {
            return 0f;
        }
        return switch (a) {
            case CENTER -> (avail - size) / 2f;
            case END -> avail - size;
            default -> 0f;
        };
    }
}
