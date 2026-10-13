package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.i18n.I18n;

/**
 * Готовый переключатель языка: показывает название текущего языка, нажатие выбирает следующий
 * из {@link I18n#languages()}. Весь интерфейс, собранный через {@code tr(...)}, обновляется сразу.
 */
public class LanguageButton extends Button {
    public LanguageButton() {
        super("");
        bind(I18n.text(() -> I18n.languageName(I18n.current())));
        onPress(I18n::nextLanguage);
    }
}
