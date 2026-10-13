package io.github.ssbaxys.lumina.i18n;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.reactive.Derived;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Signal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Языки интерфейса со сменой на лету. Текущий язык — сигнал: всё, что получено через
 * {@link #tr(String, Object...)}, обновляется само, без пересоздания экрана.
 *
 * <p>По умолчанию язык совпадает с языком игры; {@link #setLanguage(String)} выбирает свой
 * (выбор сохраняется между запусками), {@link #followGameLanguage()} возвращает как было.
 *
 * <p>Переводы ищутся по порядку: таблицы из кода ({@link #translations(String)}), свои источники
 * ({@link #addSource(TranslationSource)}), lang-файлы игры ({@code assets/<мод>/lang/<код>.json}).
 * Если на текущем языке ключа нет, берётся {@link #DEFAULT_LANGUAGE}, затем сам ключ.
 */
public final class I18n {
    public static final String DEFAULT_LANGUAGE = "en_us";

    private static final String SETTING = "language";
    private static final String SETTING_AUTO = "auto";

    private static final Signal<String> language = Signal.of(DEFAULT_LANGUAGE);
    /** Растёт при любом изменении переводов (новые таблицы, перезагрузка ресурсов). */
    private static final Signal<Integer> revision = Signal.of(0);
    private static final Map<String, Translations> tables = new HashMap<>();
    private static final List<TranslationSource> sources = new ArrayList<>();
    /** Код -> явно заданное название или null (тогда название спрашивается у игры). */
    private static final Map<String, String> declared = new LinkedHashMap<>();
    private static boolean followGame = true;
    private static boolean loaded;

    private I18n() {
    }

    // ---------------------------------------------------------------- текущий язык

    /** Сигнал с кодом текущего языка. */
    public static ReadableSignal<String> language() {
        ensureLoaded();
        return language;
    }

    /** Код текущего языка. Внутри Computed/Derived регистрирует зависимость. */
    public static String current() {
        ensureLoaded();
        return language.get();
    }

    /** Выбрать язык интерфейса независимо от языка игры. Выбор сохраняется. */
    public static void setLanguage(String code) {
        ensureLoaded();
        followGame = false;
        String normalized = normalize(code);
        language.set(normalized);
        save(normalized);
    }

    /** Снова использовать язык игры. */
    public static void followGameLanguage() {
        ensureLoaded();
        followGame = true;
        if (Lumina.isInitialized()) {
            language.set(normalize(Lumina.platform().gameLanguage()));
        }
        save(SETTING_AUTO);
    }

    public static boolean isFollowingGame() {
        ensureLoaded();
        return followGame;
    }

    /** Переключить на следующий язык из {@link #languages()} (по кругу). */
    public static void nextLanguage() {
        ensureLoaded();
        List<Language> all = languages();
        String now = language.peek();
        int index = -1;
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).code().equals(now)) {
                index = i;
                break;
            }
        }
        setLanguage(all.get((index + 1) % all.size()).code());
    }

    // ---------------------------------------------------------------- список языков

    /** Объявить язык доступным для выбора; название возьмётся у игры. */
    public static void declareLanguage(String code) {
        declared.putIfAbsent(normalize(code), null);
    }

    /** Объявить язык с собственным названием (например, «Русский»). */
    public static void declareLanguage(String code, String name) {
        declared.put(normalize(code), name);
    }

    /**
     * Языки, доступные для выбора, в порядке объявления. Никогда не пуст: если ничего не объявлено,
     * это язык по умолчанию и язык игры.
     */
    public static List<Language> languages() {
        List<Language> out = new ArrayList<>(Math.max(1, declared.size()));
        for (Map.Entry<String, String> e : declared.entrySet()) {
            out.add(new Language(e.getKey(), nameOf(e.getKey(), e.getValue())));
        }
        if (out.isEmpty()) {
            // Ничего не объявлено: язык по умолчанию и язык игры.
            out.add(new Language(DEFAULT_LANGUAGE, nameOf(DEFAULT_LANGUAGE, null)));
            String game = Lumina.isInitialized() ? normalize(Lumina.platform().gameLanguage()) : DEFAULT_LANGUAGE;
            if (!game.equals(DEFAULT_LANGUAGE)) {
                out.add(new Language(game, nameOf(game, null)));
            }
        }
        return out;
    }

    /** Название языка для показа (объявленное, от игры или сам код). */
    public static String languageName(String code) {
        String c = normalize(code);
        return nameOf(c, declared.get(c));
    }

    private static String nameOf(String code, String explicit) {
        if (explicit != null) {
            return explicit;
        }
        String name = Lumina.isInitialized() ? Lumina.platform().languageName(code) : null;
        return name != null ? name : code;
    }

    // ---------------------------------------------------------------- переводы

    /** Таблица переводов языка, заполняемая из кода. Язык автоматически становится доступным для выбора. */
    public static Translations translations(String code) {
        String c = normalize(code);
        declared.putIfAbsent(c, null);
        return tables.computeIfAbsent(c, k -> new Translations());
    }

    public static void addSource(TranslationSource source) {
        sources.add(source);
        changed();
    }

    /** Сообщить, что переводы изменились (перезагрузка ресурсов, смена языка игры). Вызывает загрузчик. */
    public static void refresh() {
        ensureLoaded();
        if (followGame && Lumina.isInitialized()) {
            language.set(normalize(Lumina.platform().gameLanguage()));
        }
        changed();
    }

    static void changed() {
        revision.set(revision.peek() + 1);
    }

    /** Есть ли перевод ключа на текущем языке или языке по умолчанию. */
    public static boolean has(String key) {
        String lang = current();
        revision.get();
        return find(lang, key) != null || (!lang.equals(DEFAULT_LANGUAGE) && find(DEFAULT_LANGUAGE, key) != null);
    }

    /**
     * Перевод ключа на текущий язык прямо сейчас. Аргументы подставляются вместо {@code %s} и
     * {@code %1$s}; аргумент-сигнал подставляется своим значением. Внутри Computed/Derived
     * регистрирует зависимость от языка, поэтому результат пересчитается при его смене.
     */
    public static String get(String key, Object... args) {
        String lang = current();
        revision.get();
        String pattern = find(lang, key);
        if (pattern == null && !lang.equals(DEFAULT_LANGUAGE)) {
            pattern = find(DEFAULT_LANGUAGE, key);
        }
        if (pattern == null) {
            pattern = key;
        }
        return args.length == 0 ? pattern : format(pattern, args);
    }

    /**
     * «Живой» перевод: сигнал, который меняется при смене языка, перезагрузке переводов и
     * изменении аргументов-сигналов. Для {@code Label.bound(...)}, {@code Button.bind(...)}.
     */
    public static ReadableSignal<String> tr(String key, Object... args) {
        return new Derived<>(() -> get(key, args));
    }

    /** «Живой» текст произвольного вида: внутри можно вызывать {@link #get} и читать любые сигналы. */
    public static ReadableSignal<String> text(Supplier<String> supplier) {
        return new Derived<>(supplier);
    }

    private static String find(String lang, String key) {
        Translations table = tables.get(lang);
        if (table != null) {
            String v = table.get(key);
            if (v != null) {
                return v;
            }
        }
        for (int i = 0; i < sources.size(); i++) {
            String v = sources.get(i).translate(lang, key);
            if (v != null) {
                return v;
            }
        }
        return Lumina.isInitialized() ? Lumina.platform().translate(lang, key) : null;
    }

    // ---------------------------------------------------------------- формат

    /** Подстановка {@code %s}, {@code %d}, {@code %1$s} и {@code %%} без String.format и без исключений. */
    static String format(String pattern, Object[] args) {
        int n = pattern.length();
        StringBuilder sb = new StringBuilder(n + 16);
        int next = 0;
        int i = 0;
        while (i < n) {
            char c = pattern.charAt(i);
            if (c != '%' || i + 1 >= n) {
                sb.append(c);
                i++;
                continue;
            }
            char d = pattern.charAt(i + 1);
            if (d == '%') {
                sb.append('%');
                i += 2;
            } else if (d == 's' || d == 'd') {
                appendArg(sb, args, next++);
                i += 2;
            } else {
                int j = i + 1;
                int index = 0;
                while (j < n && Character.isDigit(pattern.charAt(j))) {
                    index = index * 10 + (pattern.charAt(j) - '0');
                    j++;
                }
                if (j > i + 1 && j + 1 < n && pattern.charAt(j) == '$'
                        && (pattern.charAt(j + 1) == 's' || pattern.charAt(j + 1) == 'd')) {
                    appendArg(sb, args, index - 1);
                    i = j + 2;
                } else {
                    sb.append(c);
                    i++;
                }
            }
        }
        return sb.toString();
    }

    private static void appendArg(StringBuilder sb, Object[] args, int index) {
        if (index < 0 || index >= args.length) {
            return;
        }
        Object arg = args[index];
        if (arg instanceof ReadableSignal<?> signal) {
            arg = signal.get();
        }
        sb.append(arg);
    }

    // ---------------------------------------------------------------- служебное

    private static String normalize(String code) {
        return code == null || code.isEmpty() ? DEFAULT_LANGUAGE : code.toLowerCase(Locale.ROOT);
    }

    private static void ensureLoaded() {
        if (loaded || !Lumina.isInitialized()) {
            return;
        }
        loaded = true;
        Platform platform = Lumina.platform();
        String saved = platform.readSetting(SETTING);
        if (saved == null || saved.isEmpty() || saved.equals(SETTING_AUTO)) {
            followGame = true;
            language.set(normalize(platform.gameLanguage()));
        } else {
            followGame = false;
            language.set(normalize(saved));
        }
    }

    private static void save(String value) {
        if (Lumina.isInitialized()) {
            Lumina.platform().writeSetting(SETTING, value);
        }
    }

    /**
     * Встроенный текст библиотеки, написанный в коде по-русски: для русского языка возвращается как есть,
     * для остальных — английский вариант (если его нет — исходная строка). Язык берётся в момент вызова.
     */
    public static String ru(String russian) {
        if (russian == null || !Lumina.isInitialized()) {
            return russian;
        }
        String code = current();
        if (code != null && code.startsWith("ru")) {
            return russian;
        }
        String english = BuiltinEnglish.TEXT.get(russian);
        return english != null ? english : russian;
    }

    /** Сбросить выбор языка, таблицы и источники к начальному состоянию (для тестов). */
    public static void reset() {
        tables.clear();
        sources.clear();
        declared.clear();
        followGame = true;
        loaded = false;
        language.set(DEFAULT_LANGUAGE);
        changed();
    }
}
