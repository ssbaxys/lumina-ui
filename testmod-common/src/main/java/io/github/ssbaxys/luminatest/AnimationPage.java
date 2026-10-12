package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.anim.Spring;
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
        easings.add(new Button("Запустить", () -> eased.forEach(Runner::run)));

        List<Runner> sprung = new ArrayList<>();
        Panel springs = Ui.column(5).width(Dim.FILL);
        addRunner(springs, sprung, "GENTLE", null, Spring.GENTLE);
        addRunner(springs, sprung, "SNAPPY", null, Spring.SNAPPY);
        addRunner(springs, sprung, "BOUNCY", null, Spring.BOUNCY);
        springs.add(Ui.row(8,
                new Button("Запустить", () -> sprung.forEach(Runner::run)),
                Ui.muted("Пружину можно перенаправить на полпути — нажмите дважды")));

        Signal<Float> progress = Signal.of(0.3f);
        Panel progressDemo = Ui.column(6,
                new ProgressBar().bind(progress).height(Dim.px(8)),
                Ui.row(6,
                        new Button("−25%", () -> progress.update(p -> Math.max(0f, p - 0.25f))),
                        new Button("+25%", () -> progress.update(p -> Math.min(1f, p + 0.25f))),
                        Label.bound(progress.map(p -> Math.round(p * 100) + "%"))))
                .width(Dim.FILL);

        Panel morph = Ui.centered(new Label("Стиль с переходом")).style(Style.builder()
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
                new Button("Изменить", () -> morph.setState(State.CHECKED, !morph.hasState(State.CHECKED))),
                morph);

        return Ui.page(
                Ui.section("Функции плавности", "Одинаковая длительность — разный характер движения", easings),
                Ui.section("Пружины", "Физика: жёсткость и затухание вместо длительности", springs),
                Ui.section("Плавный прогресс", null, progressDemo),
                Ui.section("Переход стиля, влияющий на раскладку", "Отступы анимируются, соседи плавно сдвигаются", morphDemo),
                Ui.section("Бесконечная анимация", "Своя Animation в аниматоре корня; останавливается сама при закрытии",
                        Ui.row(14, new Pulse(), Ui.muted("Аниматор пуст, когда ничего не движется"))));
    }

    private static void addRunner(Panel into, List<Runner> list, String name, Easing easing, Spring spring) {
        Runner r = new Runner(easing, spring);
        list.add(r);
        into.add(Ui.row(8, Ui.muted(name).width(Dim.px(84)), r).width(Dim.FILL));
    }
}
