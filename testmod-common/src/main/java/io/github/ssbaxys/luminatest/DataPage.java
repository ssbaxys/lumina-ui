package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Computed;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ProgressBar;
import io.github.ssbaxys.lumina.widget.Slider;
import io.github.ssbaxys.lumina.widget.Toggle;

final class DataPage {
    private DataPage() {
    }

    static UiElement create() {
        // Один сигнал — три представления.
        Signal<Float> volume = Signal.of(40f);
        Panel binding = Ui.column(6,
                Ui.row(10, new Slider(volume, 0, 100).step(1).width(Dim.px(160)),
                        Label.bound(I18n.text(() -> I18n.get("luminatest.data.volume", Math.round(volume.get()))))),
                new ProgressBar().bind(volume.map(v -> v / 100f)).width(Dim.px(260)))
                .width(Dim.FILL);

        // Вычисляемое значение с автоматическими зависимостями.
        Signal<Float> a = Signal.of(3f);
        Signal<Float> b = Signal.of(4f);
        Computed<String> sum = new Computed<>(() -> {
            float x = a.get();
            float y = b.get();
            return String.format("a = %d,  b = %d,  a + b = %d,  √(a² + b²) = %.2f",
                    Math.round(x), Math.round(y), Math.round(x + y), Math.sqrt(x * x + y * y));
        });
        Panel computed = Ui.column(6,
                Ui.row(8, Ui.plain("a").width(Dim.px(12)), new Slider(a, 0, 10).step(1)),
                Ui.row(8, Ui.plain("b").width(Dim.px(12)), new Slider(b, 0, 10).step(1)),
                Label.bound(sum));

        // Сигнал управляет доступностью другого виджета.
        Signal<Boolean> enabled = Signal.of(true);
        Button action = Button.tr("luminatest.data.action");
        enabled.subscribe(action::setEnabled);
        Panel gating = Ui.row(10, new Toggle(enabled), Label.bound(I18n.text(() -> I18n.get(enabled.get() ? "luminatest.data.on" : "luminatest.data.off"))), action);

        // Динамический список.
        Signal<Integer> count = Signal.of(0);
        int[] serial = {0};
        Panel list = Ui.column(3).width(Dim.FILL);
        Panel todo = Ui.column(6,
                Ui.row(10,
                        Button.tr("luminatest.data.add").onPress(() -> addTask(list, count, ++serial[0])),
                        Ui.text("luminatest.data.tasks", count)),
                list).width(Dim.FILL);
        for (int i = 0; i < 3; i++) {
            addTask(list, count, ++serial[0]);
        }

        return Ui.page(
                Ui.section("luminatest.data.binding", "luminatest.data.binding.hint", binding),
                Ui.section("luminatest.data.computed", "luminatest.data.computed.hint", computed),
                Ui.section("luminatest.data.state", null, gating),
                Ui.section("luminatest.data.list", "luminatest.data.list.hint", todo));
    }

    private static void addTask(Panel list, Signal<Integer> count, int n) {
        Panel row = Panel.box(FlexLayout.row().gap(8).align(io.github.ssbaxys.lumina.layout.Align.CENTER))
                .width(Dim.FILL)
                .style(s -> s
                        .set(Styles.BACKGROUND, Brush.solid(0x18808080))
                        .set(Styles.RADIUS, Radii.all(5))
                        .set(Styles.PADDING, Insets.of(3, 8)));
        Button remove = new Button("×").style(s -> s.set(Styles.PADDING, Insets.of(1, 6)));
        remove.onPress(() -> {
            list.removeChild(row);
            count.update(c -> c - 1);
        });
        row.add(new Toggle(), Ui.text("luminatest.data.task", n), Ui.spacer(), remove);
        Ui.prepareFadeIn(row);
        list.add(row);
        Ui.playFadeIn(row);
        count.update(c -> c + 1);
    }
}
