package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Transition;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ScrollPanel;
import io.github.ssbaxys.lumina.widget.Toggle;

final class PerformancePage {
    private static final int ROWS = 1000;
    /** Один объект стиля на все строки: варианты состояний кэшируются один раз. */
    private static final Style ROW = Style.builder()
            .set(Styles.BACKGROUND, Brush.solid(0x10808080))
            .set(Styles.RADIUS, Radii.all(4))
            .set(Styles.PADDING, Insets.of(2, 8))
            .set(Styles.TRANSITION, Transition.of(0.12f))
            .hovered(h -> h.set(Styles.BACKGROUND, Brush.solid(0x38808080)))
            .build();

    private PerformancePage() {
    }

    static UiElement create() {
        Signal<Boolean> rounded = Signal.of(true);

        Panel rows = Panel.box(FlexLayout.column().gap(2));
        for (int i = 1; i <= ROWS; i++) {
            rows.add(Panel.box(FlexLayout.row().gap(10).align(Align.CENTER)).width(Dim.FILL).style(ROW).add(
                    Ui.plain("#" + i).width(Dim.px(36)),
                    Ui.text("luminatest.perf.row")));
        }
        ScrollPanel list = new ScrollPanel(rows).width(Dim.FILL).height(Dim.px(150));

        return Ui.page(
                Ui.section("luminatest.perf.frame", "luminatest.perf.frame.hint", new FrameMeter()),
                Ui.sectionWith("luminatest.perf.grid", Ui.muted("luminatest.perf.grid.hint", WaveGrid.COLS * WaveGrid.ROWS),
                        Ui.row(8, new Toggle(rounded), Ui.text("luminatest.perf.rounded")),
                        new WaveGrid(rounded)),
                Ui.sectionWith("luminatest.perf.list", Ui.muted("luminatest.perf.list.hint", ROWS), list));
    }
}
