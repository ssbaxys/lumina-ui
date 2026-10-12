package io.github.ssbaxys.lumina.widget;

import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Проверка текста поля ввода: {@code null} — всё в порядке, иначе текст ошибки для пользователя.
 * Все готовые проверки, кроме {@link #required}, пропускают пустую строку: необязательное поле
 * можно оставить пустым, а «обязательно» добавляется отдельно.
 *
 * <pre>{@code
 * new TextField(login)
 *         .validate(Validator.required(), Validator.minLength(3), Validator.pattern("[a-zA-Z0-9_]+", "Только латиница, цифры и _"));
 * new NumberField(age).validate(Validator.range(13, 120));
 * new TextField(repeat).validate(Validator.sameAs(password::peek, "Пароли не совпадают"));
 * }</pre>
 */
@FunctionalInterface
public interface Validator {
    /** @return {@code null}, если текст подходит, иначе сообщение об ошибке */
    String validate(String text);

    /** Сначала эта проверка, при успехе — следующая. */
    default Validator and(Validator next) {
        return text -> {
            String e = validate(text);
            return e != null ? e : next.validate(text);
        };
    }

    /** Та же проверка со своим сообщением. */
    default Validator message(String message) {
        return text -> validate(text) == null ? null : message;
    }

    static Validator required() {
        return required("Обязательное поле");
    }

    /** Не пусто (пробелы не в счёт). */
    static Validator required(String message) {
        return text -> text == null || text.isBlank() ? message : null;
    }

    static Validator minLength(int min) {
        return minLength(min, "Не короче " + min + " " + plural(min, "символа", "символов", "символов"));
    }

    static Validator minLength(int min, String message) {
        return text -> text == null || text.isEmpty() || text.length() >= min ? null : message;
    }

    static Validator maxLength(int max) {
        return maxLength(max, "Не длиннее " + max + " " + plural(max, "символа", "символов", "символов"));
    }

    static Validator maxLength(int max, String message) {
        return text -> text == null || text.length() <= max ? null : message;
    }

    /** Весь текст соответствует регулярному выражению. */
    static Validator pattern(String regex, String message) {
        Pattern p = Pattern.compile(regex);
        return text -> text == null || text.isEmpty() || p.matcher(text).matches() ? null : message;
    }

    static Validator email() {
        return pattern("[^@\\s]+@[^@\\s]+\\.[^@\\s]+", "Неверный адрес почты");
    }

    /** Целое число. */
    static Validator integer() {
        return pattern("[-+]?\\d+", "Нужно целое число");
    }

    /** Число в диапазоне (запятая тоже понимается как точка). */
    static Validator range(float min, float max) {
        return range(min, max, "От " + format(min) + " до " + format(max));
    }

    static Validator range(float min, float max, String message) {
        return text -> {
            if (text == null || text.isBlank()) {
                return null;
            }
            Float v = NumberField.parse(text);
            if (v == null) {
                return "Нужно число";
            }
            return v < min || v > max ? message : null;
        };
    }

    /** Совпадает с другим значением (повтор пароля). */
    static Validator sameAs(Supplier<String> other, String message) {
        return text -> text == null || text.isEmpty() || text.equals(other.get()) ? null : message;
    }

    /** Своя проверка: условие и сообщение. */
    static Validator of(Predicate<String> test, String message) {
        return text -> text == null || text.isEmpty() || test.test(text) ? null : message;
    }

    private static String format(float v) {
        return v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v);
    }

    /** Русское склонение: 1 символа, 2 символов… (для «не короче N …» нужен родительный падеж). */
    private static String plural(int n, String one, String few, String many) {
        int m10 = n % 10;
        int m100 = n % 100;
        if (m10 == 1 && m100 != 11) {
            return one;
        }
        if (m10 >= 2 && m10 <= 4 && (m100 < 12 || m100 > 14)) {
            return few;
        }
        return many;
    }
}
