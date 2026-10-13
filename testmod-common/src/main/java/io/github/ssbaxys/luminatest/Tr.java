package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.i18n.I18n;

/**
 * Тексты страниц витрины написаны по-русски прямо в коде; для остальных языков берётся английский
 * перевод из {@link ShowcaseEnglish}. Перевод выбирается в момент сборки страницы.
 */
final class Tr {
    private Tr() {
    }

    static boolean russian() {
        String code = I18n.current();
        return code != null && code.startsWith("ru");
    }

    /** Русская строка как есть или её английский перевод (если его нет — исходная строка). */
    static String r(String russian) {
        if (russian()) {
            return russian;
        }
        String english = ShowcaseEnglish.TEXT.get(russian);
        return english != null ? english : russian;
    }
}
