package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Transition;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ProgressBar;
import io.github.ssbaxys.lumina.widget.Slider;
import io.github.ssbaxys.lumina.widget.Toggle;

import java.util.function.Consumer;

final class StylePage {
    private static final int BASE = 0xFF5B8CFF;
    private static final int[] ACCENTS = {0xFF5B8CFF, 0xFF34C38F, 0xFFF5A524, 0xFFF0506E, 0xFFA855F7, 0xFF22D3EE};

    private StylePage() {
    }

    static UiElement create(Signal<Boolean> light) {
        Panel radii = Ui.row(14,
                swatch("0", b -> b.set(Styles.RADIUS, Radii.ZERO)),
                swatch("4", b -> b.set(Styles.RADIUS, Radii.all(4))),
                swatch("10", b -> b.set(Styles.RADIUS, Radii.all(10))),
                swatch("пилюля", b -> b.set(Styles.RADIUS, Radii.all(15))),
                swatch("разные", b -> b.set(Styles.RADIUS, new Radii(14, 2, 14, 2))));

        Panel shadows = Ui.row(14,
                swatch("нет", b -> b.set(Styles.RADIUS, Radii.all(6))),
                swatch("малая", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.SHADOW, Shadow.of(2, 4, 0x80000000))),
                swatch("большая", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.SHADOW, Shadow.of(8, 18, 0xA0000000))),
                swatch("свечение", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.SHADOW, new Shadow(0, 0, 10, 2, 0xC05B8CFF))),
                swatch("жёсткая", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.SHADOW, new Shadow(5, 5, 0, 0, 0x90000000))));

        Panel fills = Ui.row(14,
                swatch("сплошная", b -> b.set(Styles.RADIUS, Radii.all(6))),
                swatch("вертикальный", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.BACKGROUND, Brush.vertical(0xFFF0506E, 0xFFA855F7))),
                swatch("горизонтальный", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.BACKGROUND, Brush.horizontal(0xFF22D3EE, 0xFF34C38F))),
                swatch("в прозрачность", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.BACKGROUND, Brush.vertical(0xFFF5A524, 0x00F5A524))),
                swatch("обводка", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.BACKGROUND, null)
                        .set(Styles.BORDER_WIDTH, 2f).set(Styles.BORDER_COLOR, BASE)));

        Panel states = Ui.row(14,
                swatch("наведи", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.TRANSITION, Transition.of(0.2f))
                        .hovered(h -> h.set(Styles.SCALE, 1.15f).set(Styles.SHADOW, Shadow.of(6, 12, 0x90000000)))),
                swatch("нажми", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.TRANSITION, Transition.of(0.12f))
                        .pressed(p -> p.set(Styles.SCALE, 0.85f).set(Styles.BACKGROUND, Brush.solid(0xFFF0506E)))),
                swatch("цвет", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.TRANSITION, Transition.of(0.35f))
                        .hovered(h -> h.set(Styles.BACKGROUND, Brush.horizontal(0xFFF5A524, 0xFFF0506E)).set(Styles.RADIUS, Radii.all(15)))),
                swatch("50%", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.OPACITY, 0.5f)),
                swatch("25%", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.OPACITY, 0.25f)));

        Panel accents = Ui.row(8, new Label("Акцент:"));
        for (int color : ACCENTS) {
            Panel chip = Ui.box(color, 18, 18);
            chip.style(b -> b.set(Styles.RADIUS, Radii.all(9)).set(Styles.TRANSITION, Transition.of(0.15f))
                    .hovered(h -> h.set(Styles.SCALE, 1.2f).set(Styles.SHADOW, new Shadow(0, 0, 6, 1, (color & 0x00FFFFFF) | 0xA0000000))));
            chip.onClick(e -> setAccent(chip, color));
            accents.add(chip);
        }
        Signal<Float> demo = Signal.of(60f);
        Panel accentDemo = Ui.row(12, new Toggle(Signal.of(true)), new Slider(demo, 0, 100),
                new ProgressBar().bind(demo.map(v -> v / 100f)).width(Dim.px(100)));

        return Ui.page(
                Ui.section("Скругления", "Каждый угол может иметь свой радиус", radii),
                Ui.section("Тени", "Мягкие тени рисуются шейдером, без текстур", shadows),
                Ui.section("Заливки и обводки", null, fills),
                Ui.section("Состояния и переходы", "Варианты стиля для hover/pressed с плавным переходом", states),
                Ui.section("Тема", "Переключение темы на лету: стили перетекают плавно",
                        Ui.row(8, new Toggle(light), new Label("Светлая тема"))),
                Ui.section("Наследуемый акцент", "ACCENT_COLOR задаётся на корне и наследуется всеми виджетами",
                        accents, accentDemo));
    }

    private static Panel swatch(String caption, Consumer<Style.Builder> edit) {
        Panel p = Panel.box(FlexLayout.column()).size(44, 30);
        p.style(b -> {
            b.set(Styles.BACKGROUND, Brush.solid(BASE));
            edit.accept(b);
        });
        return Ui.labeled(p, caption);
    }

    /** Задать акцент на окне (самом внешнем элементе класса "panel"): его унаследуют все виджеты внутри. */
    private static void setAccent(UiElement from, int color) {
        UiElement window = null;
        for (UiElement e = from; e != null; e = e.parent()) {
            if ("panel".equals(e.styleClass())) {
                window = e;
            }
        }
        if (window != null) {
            window.setStyle(window.inlineStyle().toBuilder().set(Styles.ACCENT_COLOR, color).build());
        }
    }
}
