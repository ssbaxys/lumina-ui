package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.Justify;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;

final class LayoutPage {
    private static final int A = 0xFF5B8CFF;
    private static final int B = 0xFF34C38F;
    private static final int C = 0xFFF5A524;

    private LayoutPage() {
    }

    static UiElement create() {
        Panel justify = Ui.column(4).width(Dim.FILL);
        for (Justify j : Justify.values()) {
            justify.add(Ui.row(8,
                    Ui.muted(j.name()).width(Dim.px(96)),
                    Panel.box(FlexLayout.row().gap(4).justify(j).align(Align.CENTER))
                            .grow(1).height(Dim.px(18)).style(Ui.WELL)
                            .add(Ui.box(A, 18, 10), Ui.box(B, 18, 10), Ui.box(C, 18, 10)))
                    .width(Dim.FILL));
        }

        Panel grow = Panel.box(FlexLayout.row().gap(4)).width(Dim.FILL).add(
                growBox(A, 1), growBox(B, 2), growBox(C, 3));

        Panel align = Panel.box(FlexLayout.row().gap(8)).width(Dim.FILL);
        for (Align a : Align.values()) {
            Panel lane = Panel.box(FlexLayout.row().gap(3).align(a)).width(Dim.FILL).height(Dim.px(40)).style(Ui.WELL);
            if (a == Align.STRETCH) {
                lane.add(Ui.box(A, 10, 0).height(Dim.AUTO), Ui.box(B, 10, 0).height(Dim.AUTO), Ui.box(C, 10, 0).height(Dim.AUTO));
            } else {
                lane.add(Ui.box(A, 10, 10), Ui.box(B, 10, 20), Ui.box(C, 10, 30));
            }
            align.add(Ui.labeled(lane, a.name()).grow(1));
        }

        Panel badge = Ui.centered(new Label("absolute"))
                .absolute(6, 6)
                .style(b -> b
                        .set(Styles.BACKGROUND, Brush.solid(C))
                        .set(Styles.RADIUS, Radii.all(8))
                        .set(Styles.PADDING, Insets.of(2, 6))
                        .set(Styles.TEXT_COLOR, 0xFF1A1A1A));
        Panel stack = Panel.box(new StackLayout(Align.CENTER, Align.CENTER))
                .width(Dim.FILL).height(Dim.px(56)).style(Ui.WELL)
                .add(new Label("По центру (StackLayout)"), badge);

        Panel holyGrail = Panel.box(FlexLayout.column().gap(3)).width(Dim.FILL).height(Dim.px(96)).add(
                part(A, "шапка").height(Dim.px(14)),
                Panel.box(FlexLayout.row().gap(3)).grow(1).add(
                        part(B, "меню").width(Dim.px(56)),
                        part(0xFF6B7280, "содержимое (grow)").grow(1),
                        part(C, "панель").width(Dim.px(52))),
                part(A, "подвал").height(Dim.px(12)));

        return Ui.page(
                Ui.section("Justify", "Распределение по главной оси", justify),
                Ui.section("Grow", "Свободное место делится в пропорции 1 : 2 : 3", grow),
                Ui.section("Align", "Выравнивание по поперечной оси", align),
                Ui.section("Слои и абсолютные позиции", null, stack),
                Ui.section("Вложенные раскладки", "Классическая схема страницы из строк и колонок", holyGrail));
    }

    private static Panel growBox(int color, int grow) {
        return Ui.centered(new Label("grow " + grow)).grow(grow).height(Dim.px(18))
                .style(b -> b.set(Styles.BACKGROUND, Brush.solid(color)).set(Styles.RADIUS, Radii.all(3)));
    }

    private static Panel part(int color, String text) {
        return Ui.centered(Ui.muted(text)).style(b -> b
                .set(Styles.BACKGROUND, Brush.solid((color & 0x00FFFFFF) | 0x60000000))
                .set(Styles.RADIUS, Radii.all(3)));
    }
}
