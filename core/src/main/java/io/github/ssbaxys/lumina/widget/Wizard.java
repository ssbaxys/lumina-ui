package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Мастер настройки из нескольких шагов: сверху {@link Stepper}, в середине страница шага
 * (переход сдвигом через {@link Pages}), снизу кнопки «Назад» и «Далее» / «Готово».
 * «Далее» сначала выполняет проверку шага: не прошла — страница вздрагивает и остаётся на месте.
 *
 * <pre>{@code
 * Form account = new Form();
 * new Wizard()
 *         .step("Аккаунт", accountPage, account)                    // проверка — форма шага
 *         .step("Сервер", this::serverPage, () -> server.peek() != null)
 *         .step("Готово", this::summaryPage)
 *         .onFinish(this::save);
 * }</pre>
 *
 * <p>Стили: "wizard" и "wizard-nav" (строка кнопок).</p>
 */
public class Wizard extends Widget<Wizard> {
    private record Step(String title, BooleanSupplier check) {
    }

    private final Signal<Integer> step = Signal.of(0);
    private final List<Step> steps = new ArrayList<>();
    private final List<String> titles = new ArrayList<>();
    private final Stepper stepper;
    private final Pages pages;
    private final Button back;
    private final Button next;
    private String nextText = I18n.ru("Далее");
    private String finishText = I18n.ru("Готово");
    private Runnable onFinish;
    private Subscription subscription;
    private boolean finished;

    public Wizard() {
        super("wizard");
        setLayout(FlexLayout.column().gap(10).align(Align.STRETCH));
        stepper = new Stepper(step, titles).clickable(i -> i < current());
        pages = new Pages(step).effect(Pages.Effect.SLIDE).duration(0.32f);
        pages.layoutParams().grow = 1f;
        pages.layoutParams().width = Dim.FILL;
        back = new Button(I18n.ru("Назад"), this::back).styleClass("button ghost");
        next = new Button(nextText, this::next);
        Panel nav = new Panel("wizard-nav", FlexLayout.row().gap(8).align(Align.CENTER))
                .add(back, Panel.box(FlexLayout.row()).grow(1f), next);
        addChild(stepper);
        addChild(pages);
        addChild(nav);
        updateButtons();
    }

    /** Шаг без проверки. */
    public Wizard step(String title, Supplier<UiElement> page) {
        return step(title, page, () -> true);
    }

    /** Шаг с проверкой перед переходом дальше. */
    public Wizard step(String title, Supplier<UiElement> page, BooleanSupplier check) {
        steps.add(new Step(title, check == null ? () -> true : check));
        titles.add(title);
        pages.page(page);
        stepper.invalidateLayout();
        updateButtons();
        return this;
    }

    /** Шаг, который проверяет форма: «Далее» показывает её ошибки. */
    public Wizard step(String title, UiElement page, Form form) {
        return step(title, () -> page, form::validate);
    }

    /** Подписи кнопок. */
    public Wizard labels(String back, String next, String finish) {
        this.back.text(back);
        this.nextText = next;
        this.finishText = finish;
        updateButtons();
        return this;
    }

    public Wizard onFinish(Runnable onFinish) {
        this.onFinish = onFinish;
        return this;
    }

    /** Номер текущего шага с нуля (можно менять напрямую — без проверок). */
    public Signal<Integer> step() {
        return step;
    }

    public int current() {
        Integer i = step.peek();
        return i == null ? 0 : i;
    }

    public int stepCount() {
        return steps.size();
    }

    public Stepper stepper() {
        return stepper;
    }

    public Pages pages() {
        return pages;
    }

    public Button backButton() {
        return back;
    }

    public Button nextButton() {
        return next;
    }

    /** Завершён ли мастер кнопкой «Готово». */
    public boolean isFinished() {
        return finished;
    }

    /** Проверить шаг и перейти дальше (на последнем — завершить). {@code false} — проверка не прошла. */
    public boolean next() {
        int i = current();
        if (i < 0 || i >= steps.size()) {
            return false;
        }
        if (!steps.get(i).check().getAsBoolean()) {
            shake();
            return false;
        }
        if (i == steps.size() - 1) {
            finished = true;
            if (onFinish != null) {
                onFinish.run();
            }
        } else {
            step.set(i + 1);
        }
        return true;
    }

    public void back() {
        int i = current();
        if (i > 0) {
            step.set(i - 1);
        }
    }

    private void shake() {
        UiElement page = pages.current();
        if (page != null && page.root() != null) {
            page.animate(Styles.TRANSLATE_X)
                    .keyframe(0f, 0f).keyframe(0.2f, -5f).keyframe(0.45f, 4f).keyframe(0.7f, -2f).keyframe(1f, 0f)
                    .duration(0.35f).ease(Easing.LINEAR).keep(false).play();
        }
    }

    private void updateButtons() {
        int i = current();
        back.setEnabled(i > 0);
        back.setVisible(i > 0);
        next.text(i >= steps.size() - 1 ? finishText : nextText);
    }

    @Override
    protected void onAttach() {
        subscription = step.subscribe(i -> {
            finished = false;
            updateButtons();
        });
        updateButtons();
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }
}
