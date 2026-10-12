package io.github.ssbaxys.lumina.style;

import java.util.ArrayList;
import java.util.List;

/**
 * Токен темы (дизайн-переменная): акцентный цвет, цвет текста, базовое скругление… Стили ссылаются на
 * токены через {@link Style.Builder#bind}, а значения живут в {@link Theme}. Изменение токена в теме
 * на лету плавно перекрашивает весь интерфейс — без пересоздания стилей и виджетов.
 *
 * <p>Как и {@link StyleKey}, каждый токен получает плотный id: значения хранятся в массиве, без Map.
 * Моды объявляют свои токены наравне со встроенными ({@link Tokens}).</p>
 */
public final class ThemeVar<T> {
    private static final List<ThemeVar<?>> BY_ID = new ArrayList<>();

    private final int id;
    private final String name;
    private final Class<T> type;
    private final T defaultValue;

    private ThemeVar(int id, String name, Class<T> type, T defaultValue) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.defaultValue = defaultValue;
    }

    public static <T> ThemeVar<T> of(String name, Class<T> type, T defaultValue) {
        synchronized (ThemeVar.class) {
            ThemeVar<T> v = new ThemeVar<>(BY_ID.size(), name, type, defaultValue);
            BY_ID.add(v);
            return v;
        }
    }

    /** Все объявленные токены (встроенные и модов) в порядке регистрации. */
    public static synchronized List<ThemeVar<?>> all() {
        return List.copyOf(BY_ID);
    }

    static synchronized int count() {
        return BY_ID.size();
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

    @SuppressWarnings("unchecked")
    T cast(Object value) {
        return (T) value;
    }

    @Override
    public String toString() {
        return "ThemeVar[" + name + "]";
    }
}
