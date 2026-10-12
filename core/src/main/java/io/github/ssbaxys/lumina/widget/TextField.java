package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.reactive.Signal;

import java.util.Objects;

/**
 * Однострочное текстовое поле, привязанное к {@link Signal} со строкой. По умолчанию значение
 * обновляется при каждой правке ({@link #live}); Enter вызывает {@link #onSubmit}.
 *
 * <pre>{@code
 * new TextField(name).placeholder("Имя игрока").maxLength(16).variant("filled")
 *         .defaultValue("Steve")
 *         .onSubmit(this::save);
 * }</pre>
 */
public class TextField extends FieldBase<TextField> {
    private final Signal<String> value;

    public TextField() {
        this(Signal.of(""));
    }

    public TextField(Signal<String> value) {
        super(value);
        this.value = value;
        live(true);
        reload();
    }

    public Signal<String> value() {
        return value;
    }

    /** Пункт меню "Сбросить по умолчанию" с этим значением. */
    public TextField defaultValue(String defaultValue) {
        return resetItem(() -> defaultValue);
    }

    @Override
    protected String externalText() {
        return Objects.requireNonNullElse(value.peek(), "");
    }

    @Override
    protected boolean commitText(String text) {
        value.set(text);
        return true;
    }
}
