package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.anim.Motion;
import io.github.ssbaxys.lumina.anim.Spring;
import io.github.ssbaxys.lumina.fx.VanillaFx;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Transition;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.List;

final class AnimationPage {
    private AnimationPage() {
    }

    static UiElement create() {
        List<Runner> eased = new ArrayList<>();
        Panel easings = Ui.column(5).width(Dim.FILL);
        addRunner(easings, eased, "LINEAR", Easing.LINEAR, null);
        addRunner(easings, eased, "OUT_QUAD", Easing.OUT_QUAD, null);
        addRunner(easings, eased, "IN_OUT_CUBIC", Easing.IN_OUT_CUBIC, null);
        addRunner(easings, eased, "OUT_EXPO", Easing.OUT_EXPO, null);
        addRunner(easings, eased, "OUT_BACK", Easing.OUT_BACK, null);
        addRunner(easings, eased, "EMPHASIZED", Easing.EMPHASIZED, null);
        easings.add(Button.tr("luminatest.anim.run").onPress(() -> eased.forEach(Runner::run)));

        List<Runner> sprung = new ArrayList<>();
        Panel springs = Ui.column(5).width(Dim.FILL);
        addRunner(springs, sprung, "GENTLE", null, Spring.GENTLE);
        addRunner(springs, sprung, "SNAPPY", null, Spring.SNAPPY);
        addRunner(springs, sprung, "BOUNCY", null, Spring.BOUNCY);
        springs.add(Ui.row(8,
                Button.tr("luminatest.anim.run").onPress(() -> sprung.forEach(Runner::run)),
                Ui.muted("luminatest.anim.springs.tip")));

        Signal<Float> progress = Signal.of(0.3f);
        Panel progressDemo = Ui.column(6,
                new ProgressBar().bind(progress).height(Dim.px(8)),
                Ui.row(6,
                        new Button("−25%", () -> progress.update(p -> Math.max(0f, p - 0.25f))),
                        new Button("+25%", () -> progress.update(p -> Math.min(1f, p + 0.25f))),
                        Label.bound(progress.map(p -> Math.round(p * 100) + "%"))))
                .width(Dim.FILL);

        Panel morph = Ui.centered(Ui.text("luminatest.anim.morph.label")).style(Style.builder()
                .set(Styles.BACKGROUND, Brush.solid(0xFF5B8CFF))
                .set(Styles.RADIUS, Radii.all(4))
                .set(Styles.PADDING, Insets.of(6, 10))
                .set(Styles.TRANSITION, new Transition(0.45f, Easing.EMPHASIZED))
                .checked(c -> c
                        .set(Styles.BACKGROUND, Brush.horizontal(0xFFF0506E, 0xFFA855F7))
                        .set(Styles.RADIUS, Radii.all(16))
                        .set(Styles.PADDING, Insets.of(14, 28))
                        .set(Styles.SHADOW, new Shadow(0, 6, 16, 0, 0x90A855F7)))
                .build());
        Panel morphDemo = Ui.row(12,
                Button.tr("luminatest.anim.morph.button").onPress(() -> morph.setState(State.CHECKED, !morph.hasState(State.CHECKED))),
                morph);

        return Ui.page(
                Ui.section(Tr.r("Галерея кривых"), Tr.r("Все 23 встроенные кривые; есть ещё steps(n), spring(), cubicBezier()"),
                        gallery()),
                Ui.section(Tr.r("Ключевые кадры"), Tr.r("animate(key).keyframe(...).loop().yoyo() — любое свойство стиля"),
                        keyframes()),
                Ui.section(Tr.r("Последовательности"), Tr.r("Motion.sequence / parallel / delay / call — как таймлайн"),
                        sequence()),
                Ui.section("luminatest.anim.easing", "luminatest.anim.easing.hint", easings),
                Ui.section("luminatest.anim.springs", "luminatest.anim.springs.hint", springs),
                Ui.section("luminatest.anim.progress", null, progressDemo),
                Ui.section("luminatest.anim.morph", "luminatest.anim.morph.hint", morphDemo),
                Ui.section("luminatest.anim.endless", "luminatest.anim.endless.hint",
                        Ui.row(14, new Pulse(), Ui.muted("luminatest.anim.idle"))));
    }

    private static Panel gallery() {
        Panel col = Ui.column(8);
        Panel row = null;
        int i = 0;
        for (Map.Entry<String, Easing> e : Easing.presets().entrySet()) {
            if (i++ % 6 == 0) {
                row = Ui.row(8);
                col.add(row);
            }
            Easing easing = e.getValue();
            Sketch plot = new Sketch(70, 44, (canvas, w, h, t) -> {
                float pad = 6f;
                float gw = w - 2f * pad;
                float gh = h - 2f * pad;
                canvas.fillRoundRect(0, 0, w, h, Radii.all(5), Brush.solid(0x18808080));
                canvas.fillRect(pad, h - pad, gw, 1f, 0x30808080);
                canvas.fillRect(pad, pad, gw, 1f, 0x30808080);
                float px = pad;
                float py = h - pad;
                for (int s = 1; s <= 32; s++) {
                    float x = s / 32f;
                    float nx = pad + x * gw;
                    float ny = h - pad - easing.apply(x) * gh;
                    canvas.drawLine(px, py, nx, ny, 1.2f, 0xFF7AA2FF);
                    px = nx;
                    py = ny;
                }
                float p = Math.min(1f, (t % 2.2f) / 1.6f);
                canvas.fillCircle(pad + p * gw, h - pad - easing.apply(p) * gh, 2.5f, Brush.solid(0xFFF5A524));
            });
            row.add(Ui.labeled(plot, e.getKey().toLowerCase(Locale.ROOT)));
        }
        return col;
    }

    private static Panel keyframes() {
        Panel bouncer = Ui.box(0xFF5B8CFF, 22, 22);
        bouncer.animate(Styles.TRANSLATE_X)
                .keyframe(0f, 0f)
                .keyframe(0.5f, 160f, Easing.OUT_BOUNCE)
                .keyframe(1f, 0f, Easing.IN_OUT_CUBIC)
                .duration(2.4f).loop().play();
        bouncer.animate(Styles.BACKGROUND)
                .keyframe(0f, Brush.solid(0xFF5B8CFF))
                .keyframe(0.5f, Brush.solid(0xFFF0506E))
                .keyframe(1f, Brush.solid(0xFF5B8CFF))
                .duration(2.4f).loop().play();
        bouncer.animate(Styles.RADIUS)
                .from(Radii.all(2)).to(Radii.all(11))
                .duration(0.6f).ease(Easing.IN_OUT_SINE).loop().yoyo().play();

        Panel breathing = Ui.box(0xFF34C38F, 22, 22);
        breathing.style(b -> b.set(Styles.RADIUS, Radii.all(11)));
        breathing.animate(Styles.SCALE).from(0.7f).to(1.15f).duration(0.9f).ease(Easing.IN_OUT_SINE).loop().yoyo().play();
        breathing.animate(Styles.GLOW).from(new Shadow(0, 0, 2, 0, 0x0034C38F)).to(new Shadow(0, 0, 14, 2, 0xC034C38F))
                .duration(0.9f).ease(Easing.IN_OUT_SINE).loop().yoyo().play();

        Panel squash = Ui.box(0xFFF5A524, 22, 22);
        squash.style(b -> b.set(Styles.RADIUS, Radii.all(5)));
        squash.animate(Styles.SCALE_X).keyframe(0f, 1f).keyframe(0.2f, 1.35f).keyframe(0.45f, 0.8f)
                .keyframe(0.7f, 1.08f).keyframe(1f, 1f).duration(1.2f).loop().play();
        squash.animate(Styles.SCALE_Y).keyframe(0f, 1f).keyframe(0.2f, 0.7f).keyframe(0.45f, 1.2f)
                .keyframe(0.7f, 0.95f).keyframe(1f, 1f).duration(1.2f).loop().play();

        Panel track = Panel.box(new StackLayout(Align.START, Align.CENTER)).width(Dim.px(190)).height(Dim.px(26))
                .add(bouncer);
        return Ui.row(20, track, Ui.labeled(breathing, Tr.r("дыхание")), Ui.labeled(squash, Tr.r("сжатие")));
    }

    private static Panel sequence() {
        Panel[] boxes = new Panel[4];
        int[] colors = {0xFF5B8CFF, 0xFF34C38F, 0xFFF5A524, 0xFFF0506E};
        Panel row = Ui.row(10);
        for (int i = 0; i < boxes.length; i++) {
            boxes[i] = Ui.box(colors[i], 26, 26);
            boxes[i].style(b -> b.set(Styles.RADIUS, Radii.all(6)));
            row.add(boxes[i]);
        }
        Label status = Ui.muted(Tr.r("готово"));
        Runnable run = () -> {
            Motion[] drops = new Motion[boxes.length];
            for (int i = 0; i < boxes.length; i++) {
                drops[i] = boxes[i].animate(Styles.TRANSLATE_Y).from(-30f).to(0f).duration(0.5f).ease(Easing.OUT_BOUNCE)
                        .motion().with(boxes[i].animate(Styles.OPACITY).from(0f).to(1f).duration(0.25f).motion());
            }
            Motion pulse = Motion.parallel(
                    boxes[0].animate(Styles.SCALE).keyframe(0f, 1f).keyframe(0.5f, 1.3f).keyframe(1f, 1f)
                            .duration(0.4f).motion(),
                    boxes[3].animate(Styles.SCALE).keyframe(0f, 1f).keyframe(0.5f, 1.3f).keyframe(1f, 1f)
                            .duration(0.4f).motion());
            row.play(Motion.sequence(
                    Motion.call(() -> {
                        status.text(Tr.r("падают по очереди…"));
                        for (Panel b : boxes) {
                            b.setAnimatedStyle(Styles.OPACITY, 0f);
                        }
                    }),
                    Motion.parallel(drops[0], drops[1].delayed(0.15f), drops[2].delayed(0.3f), drops[3].delayed(0.45f)),
                    Motion.call(() -> status.text(Tr.r("крайние вместе"))),
                    pulse,
                    Motion.delay(0.2f),
                    VanillaFx.shake(row, 3f, 0.35f),
                    Motion.call(() -> status.text(Tr.r("готово")))));
        };
        return Ui.row(14, new Button(Tr.r("Запустить"), run), row, status);
    }

    private static void addRunner(Panel into, List<Runner> list, String name, Easing easing, Spring spring) {
        Runner r = new Runner(easing, spring);
        list.add(r);
        into.add(Ui.row(8, Ui.plain(name).width(Dim.px(84)), r).width(Dim.FILL));
    }
}
