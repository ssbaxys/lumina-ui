package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.LayoutParams;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Transition;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ScrollPanel;

final class InputPage {
    private static final int MAX_LOG = 40;

    private InputPage() {
    }

    static UiElement create() {
        Signal<Integer> clicks = Signal.of(0);
        Panel counter = Ui.row(8,
                Button.tr("luminatest.input.click").onPress(() -> clicks.update(c -> c + 1)),
                Button.tr("luminatest.input.reset").onPress(() -> clicks.set(0)),
                Ui.text("luminatest.input.pressed", clicks));

        Panel focus = Ui.row(8, Button.tr("luminatest.input.first"), Button.tr("luminatest.input.second"), Button.tr("luminatest.input.third"));

        return Ui.page(
                Ui.section("luminatest.input.clicks", null, counter),
                Ui.section("luminatest.input.drag", "luminatest.input.drag.hint", dragArea()),
                Ui.section("luminatest.input.focus", "luminatest.input.focus.hint",
                        focus, new KeyEcho()),
                Ui.section("luminatest.input.log", "luminatest.input.log.hint", eventLog()));
    }

    private static Panel dragArea() {
        Panel area = Panel.box(StackLayout.topLeft()).width(Dim.FILL).height(Dim.px(90)).style(Ui.WELL);
        area.setClipChildren(true);
        Panel handle = Ui.centered(Ui.text("luminatest.input.drag.handle")).absolute(10, 10).style(b -> b
                .set(Styles.BACKGROUND, Brush.vertical(0xFF34C38F, 0xFF22A06B))
                .set(Styles.RADIUS, Radii.all(6))
                .set(Styles.PADDING, Insets.of(5, 9))
                .set(Styles.SHADOW, Shadow.of(3, 6, 0x70000000))
                .set(Styles.TRANSITION, Transition.of(0.12f))
                .hovered(h -> h.set(Styles.SCALE, 1.05f))
                .pressed(p -> p.set(Styles.SCALE, 0.97f).set(Styles.SHADOW, Shadow.of(8, 14, 0x90000000))));
        float[] grab = new float[2];
        handle.on(PointerEvent.PRESS, e -> {
            grab[0] = e.localX();
            grab[1] = e.localY();
            e.consume();
        });
        handle.on(PointerEvent.DRAG, e -> {
            LayoutParams p = handle.layoutParams();
            float pad = area.padding().left();
            p.left = clamp(e.x() - area.screenX() - pad - grab[0], 0f, area.width() - area.padding().horizontal() - handle.width());
            p.top = clamp(e.y() - area.screenY() - area.padding().top() - grab[1], 0f, area.height() - area.padding().vertical() - handle.height());
            handle.invalidateLayout();
        });
        return area.add(handle);
    }

    private static Panel eventLog() {
        Panel lines = Panel.box(FlexLayout.column().gap(1));
        ScrollPanel logView = new ScrollPanel(lines).width(Dim.FILL).height(Dim.px(84));

        Panel zone = Ui.centered(Ui.text("luminatest.input.zone"))
                .width(Dim.FILL).height(Dim.px(40))
                .style(b -> b
                        .set(Styles.BACKGROUND, Brush.solid(0x18808080))
                        .set(Styles.RADIUS, Radii.all(6))
                        .set(Styles.BORDER_WIDTH, 1f)
                        .set(Styles.BORDER_COLOR, 0x30808080)
                        .set(Styles.TRANSITION, Transition.of(0.15f))
                        .hovered(h -> h.set(Styles.BACKGROUND, Brush.solid(0x30808080))));
        Panel wrapper = Panel.box(FlexLayout.column().align(Align.STRETCH)).width(Dim.FILL).add(zone);

        wrapper.onCapture(PointerEvent.CLICK, e -> log(lines, I18n.get("luminatest.input.log.capture")));
        zone.on(PointerEvent.ENTER, e -> log(lines, "enter"));
        zone.on(PointerEvent.LEAVE, e -> log(lines, "leave"));
        zone.on(PointerEvent.PRESS, e -> log(lines, I18n.get("luminatest.input.log.press", e.button())));
        zone.on(PointerEvent.RELEASE, e -> log(lines, I18n.get("luminatest.input.log.release", e.button())));
        zone.on(PointerEvent.CLICK, e -> log(lines, I18n.get("luminatest.input.log.bubble")));
        zone.on(ScrollEvent.SCROLL, e -> {
            log(lines, I18n.get(e.deltaY() > 0 ? "luminatest.input.log.scroll.up" : "luminatest.input.log.scroll.down"));
            e.consume();
        });

        return Ui.column(6, wrapper, logView).width(Dim.FILL);
    }

    private static void log(Panel lines, String text) {
        lines.addChild(0, new Label(text));
        while (lines.children().size() > MAX_LOG) {
            lines.removeChild(lines.children().get(lines.children().size() - 1));
        }
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(Math.max(min, max), v));
    }
}
