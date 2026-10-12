package io.github.ssbaxys.lumina.demo;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.Justify;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Transition;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ScrollPanel;

/** Демонстрационный экран: открывается командой {@code /lumina demo} в любом из загрузчиков. */
public final class LuminaDemo {
    private LuminaDemo() {
    }

    public static UiRoot create(Runnable onClose) {
        Signal<Integer> clicks = Signal.of(0);

        Panel list = Panel.box(io.github.ssbaxys.lumina.layout.FlexLayout.column().gap(4));
        for (int i = 1; i <= 30; i++) {
            int n = i;
            list.add(new Panel(null, io.github.ssbaxys.lumina.layout.FlexLayout.row())
                    .style(b -> b
                            .set(Styles.BACKGROUND, Brush.solid(0x18FFFFFF))
                            .set(Styles.RADIUS, Radii.all(4))
                            .set(Styles.PADDING, Insets.of(4, 8))
                            .set(Styles.TRANSITION, Transition.of(0.12f))
                            .hovered(h -> h.set(Styles.BACKGROUND, Brush.solid(0x30FFFFFF))))
                    .add(new Label("Элемент списка #" + n)));
        }

        Panel window = Panel.column()
                .gap(10)
                .width(Dim.px(260))
                .add(
                        new Label("Lumina UI").scale(1.5f),
                        new Label("Скругления, тени, градиенты и анимации")
                                .style(b -> b.set(Styles.TEXT_COLOR, 0xFF9AA3B5)),
                        Label.bound(clicks.map(c -> "Нажатий: " + c)),
                        new ScrollPanel(list).height(Dim.px(120)),
                        Panel.box(io.github.ssbaxys.lumina.layout.FlexLayout.row().gap(8).justify(Justify.END))
                                .add(
                                        new Button("Нажми", () -> clicks.update(c -> c + 1)),
                                        new Button("Закрыть", onClose)));

        Panel screen = Panel.box(new StackLayout(Align.CENTER, Align.CENTER))
                .fill()
                .style(b -> b.set(Styles.BACKGROUND, Brush.vertical(0x60000000, 0xA0000000)))
                .add(window);
        return new UiRoot(screen);
    }
}
