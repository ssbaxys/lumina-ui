package io.github.ssbaxys.lumina.i18n;

import java.util.HashMap;
import java.util.Map;

/** Таблица переводов одного языка, заполняемая из кода: {@code I18n.translations("ru_ru").add(...)}. */
public final class Translations {
    private final Map<String, String> values = new HashMap<>();

    Translations() {
    }

    public Translations add(String key, String value) {
        values.put(key, value);
        I18n.changed();
        return this;
    }

    public Translations addAll(Map<String, String> entries) {
        values.putAll(entries);
        I18n.changed();
        return this;
    }

    String get(String key) {
        return values.get(key);
    }
}
