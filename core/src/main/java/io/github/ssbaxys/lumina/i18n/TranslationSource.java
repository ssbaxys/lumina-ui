package io.github.ssbaxys.lumina.i18n;

/** Дополнительный источник переводов (свой формат файлов, сеть и т.п.). */
@FunctionalInterface
public interface TranslationSource {
    /** Перевод ключа на язык {@code language} или {@code null}, если его нет. */
    String translate(String language, String key);
}
