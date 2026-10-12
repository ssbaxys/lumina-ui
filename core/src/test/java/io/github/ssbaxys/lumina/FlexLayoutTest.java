package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.Justify;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FlexLayoutTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    private static Panel box(float w, float h) {
        return Panel.box(FlexLayout.column()).size(w, h);
    }

    @Test
    void rowDistributesGrow() {
        Panel a = box(20, 10);
        Panel b = box(20, 10).grow(1);
        Panel c = box(20, 10).grow(3);
        Panel d = Panel.box(FlexLayout.column()).width(io.github.ssbaxys.lumina.layout.Dim.px(0));
        Panel row = Panel.box(FlexLayout.row().gap(10)).size(200, 50).add(a, b, c, d);
        UiRoot root = new UiRoot(row, new Theme());
        root.resize(300, 300);
        root.layoutIfNeeded();
        // свободно: 200 - 60 - 3*10 = 110 → b +27.5, c +82.5
        assertEquals(0, a.x());
        assertEquals(20, a.width());
        assertEquals(30, b.x());
        assertEquals(47.5f, b.width());
        assertEquals(87.5f, c.x());
        assertEquals(102.5f, c.width());
        assertEquals(10, a.height(), "фиксированная высота не растягивается");
        assertEquals(50, d.height(), "STRETCH по умолчанию растягивает auto-высоту");
    }

    @Test
    void columnCentersWithJustifyAndAlign() {
        Panel a = box(40, 10);
        Panel col = Panel.box(FlexLayout.column().justify(Justify.CENTER).align(Align.CENTER))
                .size(100, 100).add(a);
        UiRoot root = new UiRoot(col, new Theme());
        root.resize(100, 100);
        root.layoutIfNeeded();
        assertEquals(30, a.x());
        assertEquals(45, a.y());
    }

    @Test
    void autoSizeIncludesPaddingMarginsAndGap() {
        Panel a = box(10, 10).margin(Insets.all(2));
        Panel b = box(10, 20);
        Panel col = Panel.box(FlexLayout.column().gap(5).align(Align.START)).add(a, b);
        col.style(s -> s.set(io.github.ssbaxys.lumina.style.Styles.PADDING, Insets.all(4)));
        UiRoot root = new UiRoot(col, new Theme());
        root.resize(500, 500);
        root.layoutIfNeeded();
        assertEquals(4 + 14 + 4, col.width());
        assertEquals(4 + 14 + 5 + 20 + 4, col.height());
        assertEquals(6, a.x());
        assertEquals(6, a.y());
        assertEquals(4 + 14 + 5, b.y());
    }

    @Test
    void fillTakesRemainingSpace() {
        Panel a = box(30, 10);
        Panel b = Panel.box(FlexLayout.column()).width(Dim.FILL).height(Dim.px(10));
        Panel row = Panel.box(FlexLayout.row()).size(100, 10).add(a, b);
        UiRoot root = new UiRoot(row, new Theme());
        root.resize(100, 100);
        root.layoutIfNeeded();
        assertEquals(30, b.x());
        assertEquals(70, b.width());
    }

    @Test
    void relayoutOnlyAfterInvalidate() {
        Panel a = box(10, 10);
        Panel row = Panel.box(FlexLayout.row()).add(a);
        UiRoot root = new UiRoot(row, new Theme());
        root.resize(100, 100);
        root.layoutIfNeeded();
        assertEquals(10, row.width());
        a.size(40, 10);
        root.layoutIfNeeded();
        assertEquals(40, row.width());
    }
}
