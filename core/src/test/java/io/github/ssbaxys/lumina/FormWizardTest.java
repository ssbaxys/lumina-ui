package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.style.ModernTheme;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.FieldBase;
import io.github.ssbaxys.lumina.widget.Form;
import io.github.ssbaxys.lumina.widget.FormField;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.NumberField;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Stepper;
import io.github.ssbaxys.lumina.widget.TextField;
import io.github.ssbaxys.lumina.widget.Validator;
import io.github.ssbaxys.lumina.widget.Wizard;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Проверка ввода, формы, индикатор шагов и мастер настройки. */
class FormWizardTest {
    @org.junit.jupiter.api.BeforeEach
    void russianTexts() {
        io.github.ssbaxys.lumina.i18n.I18n.setLanguage("ru_ru");
    }

    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    private static UiRoot mount(UiElement... e) {
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column().gap(4)).fill().add(e), ModernTheme.dark());
        root.resize(400, 300);
        root.tick(0f);
        return root;
    }

    private static void type(UiRoot root, String s) {
        for (char c : s.toCharArray()) {
            root.charTyped(c, 0);
        }
    }

    private static void click(UiRoot root, float x, float y) {
        root.pointerMove(x, y);
        root.pointerDown(x, y, Keys.MOUSE_LEFT);
        root.pointerUp(x, y, Keys.MOUSE_LEFT);
    }

    @Test
    void validatorsReturnRussianMessagesAndSkipEmptyText() {
        assertEquals("Обязательное поле", Validator.required().validate("  "));
        assertNull(Validator.required().validate("x"));
        assertEquals("Не короче 3 символов", Validator.minLength(3).validate("ab"));
        assertEquals("Не короче 21 символа", Validator.minLength(21).validate("a"));
        assertNull(Validator.minLength(3).validate(""), "empty text is left to required()");
        assertEquals("Не длиннее 5 символов", Validator.maxLength(5).validate("abcdef"));
        assertEquals("Неверный адрес почты", Validator.email().validate("steve@"));
        assertNull(Validator.email().validate("steve@mine.craft"));
        assertEquals("От 1 до 10", Validator.range(1, 10).validate("11"));
        assertEquals("Нужно число", Validator.range(1, 10).validate("abc"));
        assertNull(Validator.range(1, 10).validate("2,5"));
        assertEquals("Нужно целое число", Validator.integer().validate("2.5"));
        Signal<String> pass = Signal.of("secret");
        assertEquals("Пароли не совпадают", Validator.sameAs(pass::peek, "Пароли не совпадают").validate("secrex"));
        Validator both = Validator.required().and(Validator.pattern("[a-z]+", "Только строчные"));
        assertEquals("Только строчные", both.validate("ABC"));
        assertEquals("Своё", Validator.minLength(5).message("Своё").validate("ab"));
        assertEquals("Чётное", Validator.of(s -> s.length() % 2 == 0, "Чётное").validate("abc"));
    }

    @Test
    void fieldShowsErrorAfterBlurAndUpdatesLive() {
        Signal<String> login = Signal.of("");
        TextField field = new TextField(login).required().validate(Validator.minLength(3));
        FormField row = new FormField("Логин", field).hint("От 3 символов");
        TextField other = new TextField();
        UiRoot root = mount(row, other);
        assertFalse(field.valid().get());
        assertNull(field.error().get(), "no error before the user touched the field");

        root.setFocus(field);
        type(root, "ab");
        assertNull(field.error().get(), "still typing");
        root.setFocus(other);
        assertEquals("Не короче 3 символов", field.error().get());
        assertTrue(field.hasState(FieldBase.INVALID));
        root.tick(1f);
        assertEquals("Не короче 3 символов", row.errorText());
        assertEquals(1f, row.errorReveal(), 0.001f);

        // После показа ошибки поле перепроверяется на каждой правке.
        root.setFocus(field);
        root.keyDown(Keys.END, 0, 0);
        type(root, "c");
        assertNull(field.error().get());
        assertFalse(field.hasState(FieldBase.INVALID));
        root.tick(0.05f);
        assertTrue(row.errorReveal() < 1f && row.errorReveal() > 0f, "collapsing: " + row.errorReveal());
        root.tick(1f);
        assertEquals(0f, row.errorReveal(), 0.001f);
        assertTrue(field.valid().get());

        field.setError("Имя занято");
        assertEquals("Имя занято", field.error().get());
        field.resetValidation();
        assertNull(field.error().get());
    }

    @Test
    void formValidatesSubmitsAndFocusesFirstInvalidField() {
        Signal<String> login = Signal.of("");
        Signal<String> mail = Signal.of("");
        Signal<Float> age = Signal.of(5f);
        int[] submitted = {0};
        int[] rejected = {0};
        Form form = new Form().onSubmit(() -> submitted[0]++).onInvalid(() -> rejected[0]++);
        TextField loginField = new TextField(login).required();
        TextField mailField = new TextField(mail).validate(Validator.required(), Validator.email());
        NumberField ageField = new NumberField(age).decimals(0).validate(Validator.range(13, 120));
        Button submit = form.submitButton(new Button("Создать"), false);
        UiRoot root = mount(form.field("Логин", loginField), form.field("Почта", mailField),
                form.field("Возраст", ageField), submit);
        assertEquals(3, form.fields().size());
        assertFalse(form.valid().get());
        assertEquals(List.of("Обязательное поле", "Обязательное поле", "От 13 до 120"), form.errors());

        submit.press();
        assertEquals(0, submitted[0]);
        assertEquals(1, rejected[0]);
        assertSame(loginField, root.focused(), "focus goes to the first invalid field");
        assertNotNull(mailField.error().get(), "all errors are shown at once");

        login.set("Steve");
        mail.set("steve@mine.craft");
        age.set(30f);
        assertTrue(form.valid().get(), "valid() follows every change");
        assertTrue(form.submit());
        assertEquals(1, submitted[0]);
        form.reset();
        assertNull(loginField.error().get());

        // Кнопка, недоступная при неверной форме.
        Button strict = new Form().add(new TextField().required()).submitButton(new Button("OK"), true);
        assertFalse(strict.isEnabled());
    }

    @Test
    void markupFormWiresValidationAndSubmitButton() {
        Bindings b = new Bindings();
        int[] sent = {0};
        b.handler("send", () -> sent[0]++);
        Markup.Built built = Markup.build("""
                <state name="nick" value=""/>
                <form on-submit="send" valid="ok">
                  <field label="Ник" hint="Латиница">
                    <input bind="nick" required minlength="3" pattern="[a-z]+" title="Только строчные латинские"/>
                  </field>
                  <field label="Почта"><input type="email" required="Укажите почту"/></field>
                  <button type="submit">Отправить</button>
                </form>
                """, b);
        UiRoot root = mount(built.root());
        List<FieldBase<?>> fields = new ArrayList<>();
        collect(root.content(), fields);
        assertEquals(2, fields.size());
        Button button = find(root.content(), Button.class);
        assertNotNull(button);
        button.press();
        assertEquals(0, sent[0]);
        assertEquals("Обязательное поле", fields.get(0).error().get());
        assertEquals("Укажите почту", fields.get(1).error().get());
        assertEquals(Boolean.FALSE, b.get("ok").get());

        fields.get(0).setText("STEVE");
        assertEquals("Только строчные латинские", fields.get(0).error().get());
        fields.get(0).setText("steve");
        fields.get(1).setText("a@b.c");
        assertEquals(Boolean.TRUE, b.get("ok").get());
        button.press();
        assertEquals(1, sent[0]);
        assertEquals("steve", b.get("nick").get());
        assertTrue(find(root.content(), FormField.class).label().text().equals("Ник"));
    }

    private static void collect(UiElement e, List<FieldBase<?>> out) {
        if (e instanceof FieldBase<?> f) {
            out.add(f);
        }
        for (UiElement c : e.children()) {
            collect(c, out);
        }
    }

    private static <T> T find(UiElement e, Class<T> type) {
        if (type.isInstance(e)) {
            return type.cast(e);
        }
        for (UiElement c : e.children()) {
            T found = find(c, type);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Test
    void stepperAnimatesProgressAndAllowsGoingBack() {
        Signal<Integer> step = Signal.of(0);
        Stepper stepper = new Stepper(step, "Аккаунт", "Профиль", "Готово").clickable(i -> i < step.peek())
                .width(Dim.px(300));
        UiRoot root = mount(stepper);
        step.set(2);
        root.tick(0.05f);
        assertTrue(stepper.progress() > 0f && stepper.progress() < 2f, "moving: " + stepper.progress());
        for (int i = 0; i < 60; i++) {
            root.tick(1f / 30f);
        }
        assertEquals(2f, stepper.progress(), 0.01f);
        // Клик по первому (пройденному) кружку возвращает к нему.
        float cx = stepper.screenX() + stepper.padding().left() + 21f;
        click(root, cx, stepper.screenY() + stepper.padding().top() + 9f);
        assertEquals(0, step.get());
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(400, 300);
        root.render(canvas, 0f);
        canvas.end();
        assertTrue(canvas.ops.stream().anyMatch(op -> op.startsWith("text Профиль ")));
        assertTrue(canvas.ops.stream().anyMatch(op -> op.startsWith("text 3 ")), "numbers in circles");
    }

    @Test
    void wizardChecksEachStepBeforeMovingOn() {
        Signal<String> name = Signal.of("");
        Form account = new Form();
        Panel accountPage = Panel.column().add(account.field("Имя", new TextField(name).required()));
        boolean[] agree = {false};
        int[] finished = {0};
        Wizard wizard = new Wizard()
                .step("Аккаунт", accountPage, account)
                .step("Правила", () -> new Label("Согласны?"), () -> agree[0])
                .step("Готово", () -> new Label("Всё!"))
                .onFinish(() -> finished[0]++)
                .width(Dim.px(320)).height(Dim.px(220));
        UiRoot root = mount(wizard);
        assertEquals(3, wizard.stepCount());
        assertFalse(wizard.backButton().isVisible());
        assertEquals("Далее", wizard.nextButton().text());

        wizard.nextButton().press();
        assertEquals(0, wizard.current(), "empty name blocks the step");
        assertEquals("Обязательное поле", account.fields().get(0).error().get());

        name.set("Alex");
        wizard.nextButton().press();
        assertEquals(1, wizard.current());
        root.tick(0.1f);
        assertTrue(wizard.backButton().isVisible());
        assertFalse(wizard.next(), "custom check fails");
        agree[0] = true;
        assertTrue(wizard.next());
        assertEquals(2, wizard.current());
        assertEquals("Готово", wizard.nextButton().text());
        for (int i = 0; i < 30; i++) {
            root.tick(1f / 30f);
        }
        assertEquals("Всё!", ((Label) wizard.pages().current()).text());
        wizard.nextButton().press();
        assertEquals(1, finished[0]);
        assertTrue(wizard.isFinished());
        wizard.backButton().press();
        assertEquals(1, wizard.current());
        assertEquals("Далее", wizard.nextButton().text());
    }
}
