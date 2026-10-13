package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Gradients;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.widget.Button;
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
                swatch("luminatest.style.pill", b -> b.set(Styles.RADIUS, Radii.all(15))),
                swatch("luminatest.style.mixed", b -> b.set(Styles.RADIUS, new Radii(14, 2, 14, 2))));

        Panel shadows = Ui.row(14,
                swatch("luminatest.style.none", b -> b.set(Styles.RADIUS, Radii.all(6))),
                swatch("luminatest.style.small", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.SHADOW, Shadow.of(2, 4, 0x80000000))),
                swatch("luminatest.style.large", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.SHADOW, Shadow.of(8, 18, 0xA0000000))),
                swatch("luminatest.style.glow", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.SHADOW, new Shadow(0, 0, 10, 2, 0xC05B8CFF))),
                swatch("luminatest.style.hard", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.SHADOW, new Shadow(5, 5, 0, 0, 0x90000000))));

        Panel fills = Ui.row(14,
                swatch("luminatest.style.solid", b -> b.set(Styles.RADIUS, Radii.all(6))),
                swatch("luminatest.style.vertical", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.BACKGROUND, Brush.vertical(0xFFF0506E, 0xFFA855F7))),
                swatch("luminatest.style.horizontal", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.BACKGROUND, Brush.horizontal(0xFF22D3EE, 0xFF34C38F))),
                swatch("luminatest.style.fade", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.BACKGROUND, Brush.vertical(0xFFF5A524, 0x00F5A524))),
                swatch("luminatest.style.border", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.BACKGROUND, null)
                        .set(Styles.BORDER_WIDTH, 2f).set(Styles.BORDER_COLOR, BASE)));

        Panel states = Ui.row(14,
                swatch("luminatest.style.hover", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.TRANSITION, Transition.of(0.2f))
                        .hovered(h -> h.set(Styles.SCALE, 1.15f).set(Styles.SHADOW, Shadow.of(6, 12, 0x90000000)))),
                swatch("luminatest.style.press", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.TRANSITION, Transition.of(0.12f))
                        .pressed(p -> p.set(Styles.SCALE, 0.85f).set(Styles.BACKGROUND, Brush.solid(0xFFF0506E)))),
                swatch("luminatest.style.color", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.TRANSITION, Transition.of(0.35f))
                        .hovered(h -> h.set(Styles.BACKGROUND, Brush.horizontal(0xFFF5A524, 0xFFF0506E)).set(Styles.RADIUS, Radii.all(15)))),
                swatch("50%", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.OPACITY, 0.5f)),
                swatch("25%", b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.OPACITY, 0.25f)));

        Panel accents = Ui.row(8, Ui.text("luminatest.style.accent"));
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
                Ui.section("luminatest.style.radii", "luminatest.style.radii.hint", radii),
                Ui.section("luminatest.style.shadows", "luminatest.style.shadows.hint", shadows),
                Ui.section("luminatest.style.fills", null, fills),
                Ui.section("luminatest.style.states", "luminatest.style.states.hint", states),
                Ui.section("luminatest.style.theme", "luminatest.style.theme.hint",
                        Ui.row(8, new Toggle(light), Ui.text("luminatest.light_theme"))),
                Ui.section("Токен акцента", "theme.set(Tokens.ACCENT, цвет) — всё, что на нём построено, перекрашивается",
                        accents, accentDemo),
                Ui.section("Токены темы", "Стили ссылаются на токены, а не на числа", tokens()),
                Ui.section("Несколько классов", "styleClass(\"button danger pill\") — классы накладываются по порядку",
                        classes()),
                Ui.section("Переходы по свойствам", "Transition.key(свойство, длительность, кривая, задержка)",
                        perProperty()),
                Ui.section("Трансформации и шрифт", "TRANSLATE, SCALE_X/Y, FONT_SCALE, BLEND", transforms()));
    }

    private static Panel swatch(String caption, Consumer<Style.Builder> edit) {
        Panel p = Panel.box(FlexLayout.column()).size(44, 30);
        p.style(b -> {
            b.set(Styles.BACKGROUND, Brush.solid(BASE));
            edit.accept(b);
        });
        return Ui.labeled(p, caption);
    }

    /** Акцент — токен темы: меняется одно значение, и все стили, ссылающиеся на него, плавно перекрашиваются. */
    private static void setAccent(UiElement from, int color) {
        if (from.root() != null) {
            from.root().theme().set(Tokens.ACCENT, color);
        }
    }

    private static Panel tokens() {
        Signal<Float> corner = Signal.of(10f);
        Signal<Float> motion = Signal.of(1f);
        Panel col = Ui.column(6).width(Dim.FILL);
        Slider cornerSlider = new Slider(corner, 0f, 16f).step(1f);
        Slider motionSlider = new Slider(motion, 0f, 4f).step(0.25f);
        corner.subscribe(v -> {
            if (cornerSlider.root() != null) {
                cornerSlider.root().theme().set(Tokens.CORNER, v);
            }
        });
        motion.subscribe(v -> {
            if (motionSlider.root() != null) {
                motionSlider.root().theme().set(Tokens.MOTION, v);
            }
        });
        col.add(Ui.row(8, new Label("CORNER").width(Dim.px(60)), cornerSlider,
                Label.bound(corner.map(v -> Math.round(v) + " px")), Ui.muted("скругление всех панелей и кнопок")));
        col.add(Ui.row(8, new Label("MOTION").width(Dim.px(60)), motionSlider,
                Label.bound(motion.map(v -> "×" + v)), Ui.muted("множитель всех переходов, 0 — без анимаций")));
        col.add(Ui.row(6, new Button("Кнопка"), new Button("Ещё одна"), new Toggle(Signal.of(true)),
                new ProgressBar().progress(0.6f).width(Dim.px(80))));
        return col;
    }

    private static Panel classes() {
        return Ui.row(6,
                new Button("button"),
                new Button("button danger").styleClass("button danger"),
                new Button("button success").styleClass("button success"),
                new Button("button ghost").styleClass("button ghost"),
                new Button("button pill").styleClass("button pill"),
                new Button("danger pill").styleClass("button danger pill"));
    }

    /** Каждое свойство со своей длительностью, кривой и задержкой. */
    private static Panel perProperty() {
        Panel p = Ui.centered(new Label("наведи")).size(90, 34);
        p.style(b -> b
                .set(Styles.BACKGROUND, Brush.solid(BASE))
                .set(Styles.RADIUS, Radii.all(6))
                .set(Styles.TRANSITION, Transition.of(0.2f)
                        .key(Styles.SCALE, 0.5f, Easing.OUT_ELASTIC)
                        .key(Styles.BACKGROUND, 1.2f, Easing.IN_OUT_SINE)
                        .key(Styles.RADIUS, 0.3f, Easing.OUT_CUBIC, 0.4f)
                        .instant(Styles.BORDER_WIDTH))
                .hovered(h -> h
                        .set(Styles.SCALE, 1.15f)
                        .set(Styles.BACKGROUND, Gradients.SUNSET)
                        .set(Styles.RADIUS, Radii.all(17))
                        .set(Styles.BORDER_WIDTH, 2f)
                        .set(Styles.BORDER_COLOR, Color.WHITE)));
        return Ui.row(14, p, Ui.column(2,
                Ui.muted("масштаб: 0.5 с, OUT_ELASTIC"),
                Ui.muted("заливка: 1.2 с, плавно"),
                Ui.muted("скругление: после задержки 0.4 с"),
                Ui.muted("рамка: мгновенно")));
    }

    private static Panel transforms() {
        Panel shifted = Ui.box(0xFF22D3EE, 30, 30);
        shifted.style(b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.TRANSITION, Transition.of(0.25f, Easing.OUT_BACK))
                .hovered(h -> h.set(Styles.TRANSLATE_Y, -6f).set(Styles.GLOW, new Shadow(0, 0, 10, 1, 0xC022D3EE))));
        Panel stretched = Ui.box(0xFFA855F7, 30, 30);
        stretched.style(b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.TRANSITION, Transition.of(0.3f, Easing.OUT_BACK))
                .hovered(h -> h.set(Styles.SCALE_X, 1.4f).set(Styles.SCALE_Y, 0.8f)));
        Panel big = Ui.column(0, new Label("FONT_SCALE"));
        big.style(b -> b.set(Styles.FONT_SCALE, 1f).set(Styles.TRANSITION, Transition.of(0.3f))
                .hovered(h -> h.set(Styles.FONT_SCALE, 1.6f)));
        Panel multiplied = Ui.box(0xFFF5A524, 30, 30);
        multiplied.style(b -> b.set(Styles.RADIUS, Radii.all(6)).set(Styles.BLEND, BlendMode.ADD)
                .set(Styles.BACKGROUND, Brush.radial(0xFFFFE070, 0x00F5A524)));
        return Ui.row(16, Ui.labeled(shifted, "сдвиг"), Ui.labeled(stretched, "растяжение"),
                Ui.labeled(multiplied, "ADD"), big);
    }
}
