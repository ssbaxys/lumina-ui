package io.github.ssbaxys.lumina.style;

import io.github.ssbaxys.lumina.anim.Interpolator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Типизированное свойство стиля. Каждый ключ получает плотный целочисленный id,
 * поэтому значения хранятся в массивах и читаются по индексу, без Map и рефлексии.
 * Моды могут регистрировать свои ключи наравне со встроенными.
 */
public final class StyleKey<T> {
    private static final List<StyleKey<?>> BY_ID = new ArrayList<>();
    private static final Map<String, StyleKey<?>> BY_NAME = new HashMap<>();

    private final int id;
    private final String name;
    private final Class<T> type;
    private final T defaultValue;
    private final boolean inherited;
    private final boolean affectsLayout;
    private final Interpolator<T> interpolator;

    private StyleKey(int id, String name, Class<T> type, T defaultValue, boolean inherited,
                     boolean affectsLayout, Interpolator<T> interpolator) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
        this.inherited = inherited;
        this.affectsLayout = affectsLayout;
        this.interpolator = interpolator;
    }

    public static <T> Builder<T> builder(String name, Class<T> type) {
        return new Builder<>(name, type);
    }

    public static synchronized int count() {
        return BY_ID.size();
    }

    public static synchronized StyleKey<?> byId(int id) {
        return BY_ID.get(id);
    }

    public static synchronized StyleKey<?> byName(String name) {
        return BY_NAME.get(name);
    }

    public int id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Class<T> type() {
        return type;
    }

    public T defaultValue() {
        return defaultValue;
    }

    /** Наследуется ли значение от родителя, если не задано (как color в CSS). */
    public boolean inherited() {
        return inherited;
    }

    public boolean affectsLayout() {
        return affectsLayout;
    }

    /** {@code null} — свойство меняется скачком, без перехода. */
    public Interpolator<T> interpolator() {
        return interpolator;
    }

    @SuppressWarnings("unchecked")
    T cast(Object value) {
        return (T) value;
    }

    @Override
    public String toString() {
        return "StyleKey[" + name + "]";
    }

    public static final class Builder<T> {
        private final String name;
        private final Class<T> type;
        private T defaultValue;
        private boolean inherited;
        private boolean affectsLayout;
        private Interpolator<T> interpolator;

        private Builder(String name, Class<T> type) {
            this.name = name;
            this.type = type;
        }

        public Builder<T> defaultValue(T value) {
            this.defaultValue = value;
            return this;
        }

        public Builder<T> inherited() {
            this.inherited = true;
            return this;
        }

        public Builder<T> affectsLayout() {
            this.affectsLayout = true;
            return this;
        }

        public Builder<T> animated(Interpolator<T> interpolator) {
            this.interpolator = interpolator;
            return this;
        }

        public StyleKey<T> register() {
            synchronized (StyleKey.class) {
                if (BY_NAME.containsKey(name)) {
                    throw new IllegalStateException("Duplicate style key: " + name);
                }
                StyleKey<T> key = new StyleKey<>(BY_ID.size(), name, type, defaultValue, inherited, affectsLayout, interpolator);
                BY_ID.add(key);
                BY_NAME.put(name, key);
                return key;
            }
        }
    }
}
