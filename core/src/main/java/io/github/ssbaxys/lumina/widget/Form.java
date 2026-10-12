package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Форма: набор полей с проверками, общий признак «всё верно» и отправка.
 *
 * <pre>{@code
 * Form form = new Form().onSubmit(() -> register(login.peek(), mail.peek()));
 * Panel column = Panel.column().add(
 *         form.field("Логин", new TextField(login).required().validate(Validator.minLength(3))),
 *         form.field("Почта", new TextField(mail).validate(Validator.required(), Validator.email())),
 *         form.submitButton(new Button("Создать"), false));
 * form.valid().subscribe(ok -> ...);   // меняется на каждой правке
 * }</pre>
 *
 * <p>{@link #submit()} записывает незаписанный ввод, проверяет все поля (ошибки показываются сразу у всех),
 * переводит фокус на первое неверное поле и только при успехе вызывает обработчик.</p>
 */
public final class Form {
    private final List<FieldBase<?>> fields = new ArrayList<>();
    private final List<BooleanSupplier> checks = new ArrayList<>();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private final Signal<Boolean> valid = Signal.of(true);
    private Runnable onSubmit;
    private Runnable onInvalid;

    /** Добавить поле в форму и обернуть его в строку с подписью и местом под ошибку. */
    public FormField field(String label, FieldBase<?> field) {
        add(field);
        return new FormField(label, field);
    }

    /** Добавить поля (без обёртки: их ошибки видны только рамкой, если не положить в {@link FormField}). */
    public Form add(FieldBase<?>... more) {
        for (FieldBase<?> f : more) {
            if (!fields.contains(f)) {
                fields.add(f);
                subscriptions.add(f.valid().subscribe(v -> recompute()));
            }
        }
        recompute();
        return this;
    }

    /** Все поля с проверками внутри элемента (например, собранного из разметки). */
    public Form collect(UiElement root) {
        if (root instanceof FieldBase<?> f && !f.validators().isEmpty()) {
            add(f);
        }
        for (UiElement c : root.children()) {
            collect(c);
        }
        return this;
    }

    /** Своя проверка всей формы (например, «согласен с правилами»); вызывается в {@link #validate()}. */
    public Form check(BooleanSupplier check) {
        checks.add(check);
        return this;
    }

    public List<FieldBase<?>> fields() {
        return List.copyOf(fields);
    }

    public Form onSubmit(Runnable onSubmit) {
        this.onSubmit = onSubmit;
        return this;
    }

    /** Попытка отправить неверную форму. */
    public Form onInvalid(Runnable onInvalid) {
        this.onInvalid = onInvalid;
        return this;
    }

    /**
     * Кнопка отправки. {@code disableWhenInvalid} — кнопка недоступна, пока форма неверна
     * (иначе нажатие на неверную форму показывает все ошибки).
     */
    public Button submitButton(Button button, boolean disableWhenInvalid) {
        button.onPress(this::submit);
        if (disableWhenInvalid) {
            button.setEnabled(Boolean.TRUE.equals(valid.peek()));
            subscriptions.add(valid.subscribe(button::setEnabled));
        }
        return button;
    }

    /** Все поля проходят проверки — живой сигнал, ошибки при этом не показываются. */
    public ReadableSignal<Boolean> valid() {
        return valid;
    }

    public boolean isValid() {
        for (FieldBase<?> f : fields) {
            if (!f.isValid()) {
                return false;
            }
        }
        for (BooleanSupplier c : checks) {
            if (!c.getAsBoolean()) {
                return false;
            }
        }
        return true;
    }

    /** Проверить всё и показать ошибки; фокус — на первое неверное поле. */
    public boolean validate() {
        FieldBase<?> firstBad = null;
        for (FieldBase<?> f : fields) {
            if (f.isDirty()) {
                f.commit();
            }
            if (!f.validate() && firstBad == null) {
                firstBad = f;
            }
        }
        boolean ok = firstBad == null;
        for (BooleanSupplier c : checks) {
            ok &= c.getAsBoolean();
        }
        if (firstBad != null && firstBad.root() != null) {
            firstBad.requestFocus();
        }
        return ok;
    }

    /** Отправить: при верной форме вызывается {@link #onSubmit}, иначе показываются ошибки. */
    public boolean submit() {
        if (validate()) {
            if (onSubmit != null) {
                onSubmit.run();
            }
            return true;
        }
        if (onInvalid != null) {
            onInvalid.run();
        }
        return false;
    }

    /** Убрать показанные ошибки (значения полей не трогаются). */
    public void reset() {
        for (FieldBase<?> f : fields) {
            f.resetValidation();
        }
    }

    /** Сообщения всех неверных полей по порядку. */
    public List<String> errors() {
        List<String> out = new ArrayList<>();
        for (FieldBase<?> f : fields) {
            String e = f.check();
            if (e != null) {
                out.add(e);
            }
        }
        return out;
    }

    /** Отписаться от полей (если форма живёт дольше них). */
    public void dispose() {
        subscriptions.forEach(Subscription::close);
        subscriptions.clear();
    }

    private void recompute() {
        boolean ok = true;
        for (FieldBase<?> f : fields) {
            ok &= Boolean.TRUE.equals(f.valid().get());
        }
        valid.set(ok);
    }
}
