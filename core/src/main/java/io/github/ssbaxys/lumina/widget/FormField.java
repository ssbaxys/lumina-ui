package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.style.Styles;

/**
 * Строка формы: подпись, поле и сообщение об ошибке под ним. Ошибка выезжает сверху и проявляется,
 * а место под неё плавно раздвигается (и так же схлопывается, когда ошибка исправлена).
 *
 * <pre>{@code
 * new FormField("Логин", new TextField(login).required()).hint("От 3 до 16 символов");
 * form.field("Почта", new TextField(mail).validate(Validator.email()));   // то же через Form
 * }</pre>
 *
 * <p>Стили: "form-field" (сама строка), "form-label", "form-hint", "form-error".</p>
 */
public class FormField extends Widget<FormField> {
    private final UiElement field;
    private final Label label;
    private Label hint;
    private final Panel errorBox;
    private final Label errorLabel;
    private AnimatedFloat reveal;
    private Subscription subscription;

    public FormField(String label, UiElement field) {
        super("form-field");
        this.field = field;
        setLayout(FlexLayout.column().gap(3));
        if (label != null && !label.isEmpty()) {
            this.label = new Label(label).styleClass("form-label");
            addChild(this.label);
        } else {
            this.label = null;
        }
        addChild(field);
        errorLabel = new Label("").styleClass("form-error");
        // Обёртка с отсечением: её высота анимируется, текст ошибки выезжает изнутри.
        errorBox = Panel.box(FlexLayout.column()).clip(true).height(Dim.px(0)).add(errorLabel);
        errorBox.setVisible(false);
        addChild(errorBox);
    }

    /** Поле внутри строки. */
    public UiElement field() {
        return field;
    }

    public Label label() {
        return label;
    }

    /** Пояснение под полем (над ошибкой), приглушённым цветом. */
    public FormField hint(String text) {
        if (hint == null) {
            hint = new Label(text).styleClass("form-hint");
            addChild(children().indexOf(errorBox), hint);
        } else {
            hint.text(text);
        }
        return this;
    }

    /** Текст ошибки, видимый сейчас (пустой — ошибки нет или она уже схлопывается). */
    public String errorText() {
        return reveal != null && reveal.target() > 0f ? errorLabel.text() : "";
    }

    /** Насколько раскрыта ошибка: 0 — скрыта, 1 — видна целиком. */
    public float errorReveal() {
        return reveal == null ? 0f : reveal.get();
    }

    /** Показать ошибку (или убрать, {@code null}). Для полей ввода это делается само. */
    public void showError(String message) {
        if (message != null && !message.isEmpty()) {
            errorLabel.text(message);
            if (reveal != null) {
                reveal.animateTo(1f);
            }
        } else if (reveal != null) {
            // Текст остаётся, пока строка схлопывается.
            reveal.animateTo(0f);
        }
    }

    private void applyReveal(float t) {
        float lh = Lumina.isInitialized() ? Lumina.platform().textMetrics().lineHeight() + 2f : 11f;
        errorBox.layoutParams().height = Dim.px(lh * t);
        errorBox.setVisible(t > 0.001f);
        errorBox.invalidateLayout();
        errorLabel.setAnimatedStyle(Styles.OPACITY, t >= 0.999f ? null : t);
        errorLabel.setAnimatedStyle(Styles.TRANSLATE_Y, t >= 0.999f ? null : (t - 1f) * 5f);
    }

    @Override
    protected void onAttach() {
        reveal = new AnimatedFloat(root().animator(), 0f).tween(0.22f, Easing.OUT_CUBIC).listener(this::applyReveal);
        if (field instanceof FieldBase<?> f) {
            String now = f.error().get();
            if (now != null) {
                errorLabel.text(now);
                reveal.snapTo(1f);
            }
            subscription = f.error().subscribe(this::showError);
        }
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
        if (reveal != null) {
            reveal.snapTo(reveal.target());
        }
    }
}
