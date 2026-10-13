package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.ColorSpace;
import io.github.ssbaxys.lumina.render.ColorStops;
import io.github.ssbaxys.lumina.render.Gradients;
import io.github.ssbaxys.lumina.render.Outline;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Transition;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;

import java.util.Map;
import java.util.function.Consumer;

/** Градиенты всех видов, пресеты, анимированные рамки, свечение и режимы смешивания. */
final class GradientsPage {
    private GradientsPage() {
    }

    static UiElement create() {
        Panel kinds = Ui.row(12,
                tile(Tr.r("линейный"), Brush.linear(135, 0xFFF0506E, 0xFFF5A524, 0xFF34C38F)),
                tile(Tr.r("радиальный"), Brush.radial(0xFFFFFFFF, 0xFF22D3EE, 0xFF2E3192)),
                tile(Tr.r("эллипс"), Brush.radial(0xFFFFE070, 0xFFF0506E, 0x00F0506E).withRadius(0.7f, 0.35f)),
                tile(Tr.r("конический"), Brush.rainbowWheel()),
                tile(Tr.r("полосы"), Brush.linear(45, ColorStops.builder()
                        .add(0f, 0xFFF5A524).hard(0.1f, 0xFFF5A524, 0xFF1D2330).add(0.2f, 0xFF1D2330).build())
                        .withRepeating(true)),
                tile(Tr.r("кольца"), Brush.radial(ColorStops.of(new float[]{0f, 0.15f, 0.3f},
                        new int[]{0xFF5B8CFF, 0xFFA855F7, 0xFF5B8CFF})).withRepeating(true)));

        Panel angles = Ui.row(12);
        for (int angle = 0; angle < 360; angle += 45) {
            angles.add(tile(angle + "°", Brush.linear(angle, 0xFF22D3EE, 0xFFA855F7), 34, 26));
        }

        // Во flex нет переноса строк — раскладываем пресеты по шесть в ряд.
        Panel presets = Ui.column(8);
        Panel presetRow = null;
        int n = 0;
        for (Map.Entry<String, Brush.Linear> e : Gradients.all().entrySet()) {
            if (n++ % 6 == 0) {
                presetRow = Ui.row(10);
                presets.add(presetRow);
            }
            presetRow.add(tile(e.getKey().toLowerCase(java.util.Locale.ROOT), e.getValue(), 52, 30));
        }

        Panel animated = Ui.row(16,
                Ui.labeled(rotatingBorder(), Tr.r("бегущая рамка")),
                Ui.labeled(flowing(), Tr.r("течёт")),
                Ui.labeled(spinner(), Tr.r("конус-спиннер")),
                Ui.labeled(morphing(), Tr.r("смена кистей")),
                Ui.labeled(hoverGlow(), Tr.r("наведи")));

        Panel blends = Ui.row(16,
                Ui.labeled(blendDemo(BlendMode.NORMAL), "NORMAL"),
                Ui.labeled(blendDemo(BlendMode.ADD), "ADD"),
                Ui.labeled(blendDemo(BlendMode.SCREEN), "SCREEN"),
                Ui.labeled(blendDemo(BlendMode.MULTIPLY), "MULTIPLY"));

        Panel outlines = Ui.row(16,
                Ui.labeled(styled(b -> b.set(Styles.BORDER_WIDTH, 2f)
                        .set(Styles.BORDER_BRUSH, Gradients.NEON)), Tr.r("рамка-градиент")),
                Ui.labeled(styled(b -> b.set(Styles.OUTLINE, new Outline(1.5f, 3f, 0xFF22D3EE))), "outline"),
                Ui.labeled(styled(b -> b.set(Styles.GLOW, new Shadow(0, 0, 14, 2, 0xC0A855F7))), Tr.r("свечение")),
                Ui.labeled(styled(b -> b.set(Styles.FOREGROUND, Brush.vertical(0x00000000, 0xA0000000))),
                        Tr.r("передний слой")));

        return Ui.page(
                Ui.section(Tr.r("Виды градиентов"), Tr.r("Считаются в шейдере попиксельно: до 8 цветов, жёсткие переходы, повтор"),
                        kinds),
                Ui.section(Tr.r("Любой угол"), Tr.r("Угол как в CSS: 0° — вверх, 90° — вправо"), angles),
                Ui.section(Tr.r("Пространства цвета"), Tr.r("Пространство смешивания задаётся у каждой кисти"), spaces()),
                Ui.section(Tr.r("Готовые градиенты"), Tr.r("Класс Gradients"), presets),
                Ui.section(Tr.r("Анимированные"), Tr.r("Кисти тоже анимируются: поворот, сдвиг, плавная смена"), animated),
                Ui.section(Tr.r("Рамки, контур, свечение"), Tr.r("Новые свойства стиля: BORDER_BRUSH, OUTLINE, GLOW, FOREGROUND"),
                        outlines),
                Ui.section(Tr.r("Режимы смешивания"), Tr.r("ADD — для света и огня, MULTIPLY — для теней и тонировки"), blends));
    }

    private static Panel tile(String caption, Brush brush) {
        return tile(caption, brush, 48, 40);
    }

    private static Panel tile(String caption, Brush brush, float w, float h) {
        Panel p = Panel.box(FlexLayout.column()).size(w, h);
        p.style(b -> b.set(Styles.BACKGROUND, brush).set(Styles.RADIUS, Radii.all(6)));
        return Ui.labeled(p, caption);
    }

    private static Panel styled(Consumer<Style.Builder> edit) {
        Panel p = Panel.box(FlexLayout.column()).size(48, 34);
        p.style(b -> {
            b.set(Styles.BACKGROUND, Brush.solid(0xFF2A2F3D)).set(Styles.RADIUS, Radii.all(7));
            edit.accept(b);
        });
        return p;
    }

    private static Panel spaces() {
        Panel col = Ui.column(4).width(Dim.FILL);
        for (ColorSpace space : ColorSpace.values()) {
            col.add(Ui.row(8, new Label(space.name()).width(Dim.px(52)),
                    new Sketch(300, 12, (canvas, w, h, t) -> canvas.fillRoundRect(0, 0, w, h, Radii.all(3),
                            Brush.linear(90, 0xFFFF0000, 0xFF00FF00, 0xFF0000FF).inSpace(space)))));
        }
        return col;
    }

    private static Sketch rotatingBorder() {
        return new Sketch(70, 44, (canvas, w, h, t) -> {
            Radii r = Radii.all(8);
            Brush.Conic ring = Brush.conic(t * 120f, 0xFF22D3EE, 0xFFA855F7, 0xFFF0506E, 0xFFF5A524, 0xFF22D3EE);
            canvas.drawGlow(0, 0, w, h, r, new Shadow(0, 0, 8, 0, 0x6022D3EE));
            canvas.fillRoundRect(0, 0, w, h, r, Brush.solid(0xFF161A22));
            canvas.strokeRoundRect(0, 0, w, h, r, 2f, ring);
        });
    }

    private static Sketch flowing() {
        Brush.Linear stripes = Brush.linear(60, ColorStops.of(new float[]{0f, 0.25f, 0.5f},
                new int[]{0xFF5B8CFF, 0xFF22D3EE, 0xFF5B8CFF})).withRepeating(true);
        return new Sketch(70, 44, (canvas, w, h, t) ->
                canvas.fillRoundRect(0, 0, w, h, Radii.all(8), stripes.withOffset(-t * 0.6f)));
    }

    private static Sketch spinner() {
        return new Sketch(44, 44, (canvas, w, h, t) -> {
            float cx = w / 2f;
            float cy = h / 2f;
            canvas.drawArc(cx, cy, 16f, 5f, t * 360f, 270f, 0x0022D3EE, 0xFF22D3EE);
            canvas.drawArc(cx, cy, 9f, 3f, -t * 540f, 200f, 0x00F0506E, 0xFFF0506E);
        });
    }

    /** Кисть меняется ключевыми кадрами: сплошная → линейная → радиальная, туда и обратно бесконечно. */
    private static Panel morphing() {
        Panel p = Panel.box(FlexLayout.column()).size(70, 44);
        p.style(b -> b.set(Styles.RADIUS, Radii.all(8)).set(Styles.BACKGROUND, Brush.solid(0xFF5B8CFF)));
        p.animate(Styles.BACKGROUND)
                .keyframe(0f, Brush.solid(0xFF5B8CFF))
                .keyframe(0.4f, Gradients.SUNSET)
                .keyframe(0.7f, Gradients.AURORA)
                .keyframe(1f, Gradients.NEON)
                .duration(3f).ease(Easing.IN_OUT_SINE).loop().yoyo().play();
        return p;
    }

    private static Panel hoverGlow() {
        Panel p = Panel.box(new StackLayout(Align.CENTER, Align.CENTER)).size(70, 44).add(new Label("★"));
        p.style(b -> b
                .set(Styles.BACKGROUND, Gradients.BERRY)
                .set(Styles.RADIUS, Radii.all(8))
                .set(Styles.TRANSITION, Transition.of(0.3f, Easing.OUT_BACK))
                .hovered(h -> h
                        .set(Styles.BACKGROUND, Gradients.NEON)
                        .set(Styles.GLOW, new Shadow(0, 0, 16, 3, 0xD0A855F7))
                        .set(Styles.SCALE_X, 1.12f)
                        .set(Styles.TRANSLATE_Y, -3f)));
        return p;
    }

    private static Sketch blendDemo(BlendMode mode) {
        return new Sketch(64, 50, (canvas, w, h, t) -> {
            canvas.fillRoundRect(0, 0, w, h, Radii.all(6), Brush.vertical(0xFF3A4060, 0xFF8090B0));
            canvas.save();
            canvas.setBlendMode(mode);
            float wob = (float) Math.sin(t * 2f) * 3f;
            canvas.fillCircle(24 + wob, 20, 13, Brush.solid(0xC0FF4060));
            canvas.fillCircle(40 - wob, 20, 13, Brush.solid(0xC040FF80));
            canvas.fillCircle(32, 33, 13, Brush.solid(0xC04080FF));
            canvas.restore();
        });
    }
}
