package io.github.ssbaxys.lumina.config;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.widget.KeyBindField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Описание настроек мода без рефлексии: категории и типизированные параметры (флаг, целое, дробное, строка,
 * перечисление, цвет, клавиша) с подписями, комментариями, значениями по умолчанию и диапазонами.
 * Значение каждого параметра — {@link Signal}, поэтому интерфейс и код мода видят изменения сразу.
 * Файл — простой TOML ({@code [категория]}, {@code ключ = значение}, комментарии {@code #}) в каталоге данных
 * платформы ({@code Platform.readFile/writeFile}). Готовый экран настроек — {@code ConfigScreen}.
 *
 * <pre>{@code
 * enum Mode { FAST, FANCY }
 * ConfigSpec spec = ConfigSpec.builder("mymod.toml")
 *         .category("general", "Общие").icon("minecraft:compass")
 *             .bool("showHud", "Показывать HUD", true).comment("Панель с координатами")
 *             .integer("hudScale", "Масштаб HUD", 2, 1, 4)
 *             .number("opacity", "Прозрачность", 0.8f, 0f, 1f).step(0.05f)
 *         .category("look", "Вид")
 *             .choice("mode", "Качество", Mode.FANCY, Mode.values())
 *             .color("accent", "Акцент", 0xFF5B8CFF)
 *             .key("zoom", "Приближение", Keys.C)
 *             .string("nick", "Ник", "Steve").maxLength(16)
 *         .build();
 * spec.load();
 * boolean hud = spec.bool("showHud").get();
 * }</pre>
 */
public final class ConfigSpec {
    /** Тип параметра. */
    public enum Type {
        BOOL, INT, FLOAT, STRING, ENUM, COLOR, KEY
    }

    /** Категория: раздел экрана настроек и секция файла. */
    public static final class Category {
        private final String id;
        private final String title;
        private String icon;
        private final List<Option<?>> options = new ArrayList<>();

        Category(String id, String title) {
            this.id = id;
            this.title = title;
        }

        public String id() {
            return id;
        }

        public String title() {
            return title;
        }

        /** Предмет-иконка в боковой навигации ({@code "minecraft:compass"}) или {@code null}. */
        public String icon() {
            return icon;
        }

        public List<Option<?>> options() {
            return Collections.unmodifiableList(options);
        }
    }

    /** Параметр настроек. */
    public static final class Option<T> {
        private final Category category;
        private final String key;
        private final String label;
        private final Type type;
        private final T defaultValue;
        private final Signal<T> value;
        private String comment = "";
        private float min = Float.NEGATIVE_INFINITY;
        private float max = Float.POSITIVE_INFINITY;
        private float step;
        private int maxLength = 256;
        private boolean alpha = true;
        private List<T> choices = List.of();
        private Function<? super T, String> labeler;

        Option(Category category, String key, String label, Type type, T defaultValue) {
            this.category = category;
            this.key = key;
            this.label = label;
            this.type = type;
            this.defaultValue = defaultValue;
            this.value = Signal.of(defaultValue);
        }

        public Category category() {
            return category;
        }

        public String key() {
            return key;
        }

        /** Полный путь {@code категория.ключ}. */
        public String path() {
            return category.id() + "." + key;
        }

        public String label() {
            return label;
        }

        public String comment() {
            return comment;
        }

        public Type type() {
            return type;
        }

        public T defaultValue() {
            return defaultValue;
        }

        /** Значение-сигнал: на него подписываются виджеты и код мода. */
        public Signal<T> value() {
            return value;
        }

        public T get() {
            return value.peek();
        }

        /** Записать значение (числа зажимаются в диапазон, строки обрезаются, чужие варианты отбрасываются). */
        public void set(T v) {
            value.set(sanitize(v));
        }

        public void reset() {
            value.set(defaultValue);
        }

        public boolean isDefault() {
            return Objects.equals(value.peek(), defaultValue);
        }

        public float min() {
            return min;
        }

        public float max() {
            return max;
        }

        /** Шаг ползунка (0 — по умолчанию: 1 для целых, сотая диапазона для дробных). */
        public float step() {
            if (step > 0f) {
                return step;
            }
            if (type == Type.INT) {
                return 1f;
            }
            return Float.isFinite(max - min) ? (max - min) / 100f : 0.1f;
        }

        public int maxLength() {
            return maxLength;
        }

        /** Разрешена ли прозрачность (для цвета). */
        public boolean alpha() {
            return alpha;
        }

        /** Варианты перечисления. */
        public List<T> choices() {
            return choices;
        }

        /** Подпись варианта перечисления. */
        public String labelOf(T choice) {
            return labeler != null ? labeler.apply(choice) : String.valueOf(choice);
        }

        /** Подходит ли под строку поиска (подпись, ключ, комментарий, категория). */
        public boolean matches(String query) {
            if (query == null || query.isBlank()) {
                return true;
            }
            String q = query.trim().toLowerCase(Locale.ROOT);
            return label.toLowerCase(Locale.ROOT).contains(q) || key.toLowerCase(Locale.ROOT).contains(q)
                    || comment.toLowerCase(Locale.ROOT).contains(q) || category.title().toLowerCase(Locale.ROOT).contains(q);
        }

        @SuppressWarnings("unchecked")
        private T sanitize(T v) {
            if (v == null) {
                return defaultValue;
            }
            return switch (type) {
                case INT -> (T) (Integer) (int) Math.max(min, Math.min(max, ((Integer) v).floatValue()));
                case FLOAT -> {
                    float f = (Float) v;
                    yield Float.isNaN(f) ? defaultValue : (T) (Float) Math.max(min, Math.min(max, f));
                }
                case STRING -> {
                    String s = (String) v;
                    yield (T) (s.length() > maxLength ? s.substring(0, maxLength) : s);
                }
                case ENUM -> choices.contains(v) ? v : defaultValue;
                case COLOR -> alpha ? v : (T) (Integer) ((Integer) v | 0xFF000000);
                default -> v;
            };
        }

        /** Значение в виде текста для файла. */
        String format() {
            T v = value.peek();
            return switch (type) {
                case BOOL, INT -> String.valueOf(v);
                case FLOAT -> formatFloat((Float) v);
                case STRING -> quote((String) v);
                case ENUM -> quote(((Enum<?>) v).name());
                case COLOR -> quote(Color.toHex((Integer) v));
                case KEY -> quote(KeyBindField.keyName((Integer) v));
            };
        }

        /** Прочитать текст из файла; {@code false} — не подходит (значение не меняется). */
        @SuppressWarnings("unchecked")
        boolean parse(String text) {
            String t = text.trim();
            try {
                switch (type) {
                    case BOOL -> {
                        if (!t.equals("true") && !t.equals("false")) {
                            return false;
                        }
                        set((T) Boolean.valueOf(t));
                    }
                    case INT -> set((T) (Integer) (int) Math.round(Double.parseDouble(t)));
                    case FLOAT -> set((T) (Float) Float.parseFloat(t));
                    case STRING -> set((T) unquote(t));
                    case ENUM -> {
                        String name = unquote(t);
                        for (T c : choices) {
                            if (((Enum<?>) c).name().equalsIgnoreCase(name)) {
                                set(c);
                                return true;
                            }
                        }
                        return false;
                    }
                    case COLOR -> set((T) (Integer) Color.parse(unquote(t)));
                    case KEY -> set((T) (Integer) KeyBindField.parseKey(unquote(t)));
                    default -> {
                        return false;
                    }
                }
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }
    }

    private final String fileName;
    private final String header;
    private final List<Category> categories;
    private final Map<String, Option<?>> byPath = new LinkedHashMap<>();

    private ConfigSpec(String fileName, String header, List<Category> categories) {
        this.fileName = fileName;
        this.header = header;
        this.categories = List.copyOf(categories);
        for (Category c : categories) {
            for (Option<?> o : c.options) {
                byPath.put(o.path(), o);
            }
        }
    }

    public static Builder builder(String fileName) {
        return new Builder(fileName);
    }

    /** Имя файла внутри каталога данных платформы. */
    public String fileName() {
        return fileName;
    }

    public List<Category> categories() {
        return categories;
    }

    /** Все параметры по порядку. */
    public List<Option<?>> options() {
        return List.copyOf(byPath.values());
    }

    /**
     * Параметр по пути {@code "категория.ключ"} или по одному ключу, если он уникален.
     *
     * @throws IllegalArgumentException если такого нет
     */
    public Option<?> option(String path) {
        Option<?> o = byPath.get(path);
        if (o != null) {
            return o;
        }
        Option<?> found = null;
        for (Option<?> c : byPath.values()) {
            if (c.key().equals(path)) {
                if (found != null) {
                    throw new IllegalArgumentException("Ambiguous config key: " + path);
                }
                found = c;
            }
        }
        if (found == null) {
            throw new IllegalArgumentException("Unknown config option: " + path);
        }
        return found;
    }

    @SuppressWarnings("unchecked")
    private <T> Option<T> typed(String path, Type type) {
        Option<?> o = option(path);
        if (o.type() != type) {
            throw new IllegalArgumentException(path + " is " + o.type() + ", not " + type);
        }
        return (Option<T>) o;
    }

    public Signal<Boolean> bool(String path) {
        return this.<Boolean>typed(path, Type.BOOL).value();
    }

    public Signal<Integer> integer(String path) {
        return this.<Integer>typed(path, Type.INT).value();
    }

    public Signal<Float> number(String path) {
        return this.<Float>typed(path, Type.FLOAT).value();
    }

    public Signal<String> string(String path) {
        return this.<String>typed(path, Type.STRING).value();
    }

    public Signal<Integer> color(String path) {
        return this.<Integer>typed(path, Type.COLOR).value();
    }

    public Signal<Integer> key(String path) {
        return this.<Integer>typed(path, Type.KEY).value();
    }

    public <E extends Enum<E>> Signal<E> choice(String path, Class<E> type) {
        Option<E> o = typed(path, Type.ENUM);
        if (!o.choices().isEmpty() && !type.isInstance(o.choices().get(0))) {
            throw new IllegalArgumentException(path + " is not " + type.getSimpleName());
        }
        return o.value();
    }

    /** Параметры, подходящие под строку поиска, по порядку. */
    public List<Option<?>> search(String query) {
        List<Option<?>> out = new ArrayList<>();
        for (Option<?> o : byPath.values()) {
            if (o.matches(query)) {
                out.add(o);
            }
        }
        return out;
    }

    public void resetAll() {
        for (Option<?> o : byPath.values()) {
            o.reset();
        }
    }

    // ================================================================ файл

    /** Текст файла: заголовок, секции категорий, комментарии и значения. */
    public String write() {
        StringBuilder sb = new StringBuilder();
        if (header != null && !header.isBlank()) {
            for (String line : header.split("\n")) {
                sb.append("# ").append(line).append('\n');
            }
            sb.append('\n');
        }
        for (Category c : categories) {
            sb.append("# ").append(c.title()).append('\n');
            sb.append('[').append(c.id()).append("]\n");
            for (Option<?> o : c.options) {
                sb.append("# ").append(o.label());
                if (!o.comment().isBlank()) {
                    sb.append(" — ").append(o.comment().replace('\n', ' '));
                }
                String range = rangeHint(o);
                if (!range.isEmpty()) {
                    sb.append(" (").append(range).append(')');
                }
                sb.append('\n');
                sb.append(o.key()).append(" = ").append(o.format()).append('\n');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    private static String rangeHint(Option<?> o) {
        return switch (o.type()) {
            case INT, FLOAT -> Float.isFinite(o.min()) && Float.isFinite(o.max())
                    ? formatRangeValue(o, o.min()) + "…" + formatRangeValue(o, o.max()) : "";
            case ENUM -> {
                StringBuilder sb = new StringBuilder();
                for (Object c : o.choices()) {
                    if (sb.length() > 0) {
                        sb.append(", ");
                    }
                    sb.append(((Enum<?>) c).name());
                }
                yield sb.toString();
            }
            default -> "";
        };
    }

    private static String formatRangeValue(Option<?> o, float v) {
        return o.type() == Type.INT ? Integer.toString((int) v) : formatFloat(v);
    }

    /**
     * Прочитать текст файла: известные параметры получают значения (с проверкой), неизвестные и неверные
     * строки пропускаются.
     *
     * @return сколько параметров прочитано
     */
    public int read(String text) {
        if (text == null) {
            return 0;
        }
        String section = "";
        int count = 0;
        for (String raw : text.split("\r?\n")) {
            String line = stripComment(raw).trim();
            if (line.isEmpty()) {
                continue;
            }
            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            Option<?> o = byPath.get(section.isEmpty() ? key : section + "." + key);
            if (o != null && o.parse(value)) {
                count++;
            }
        }
        return count;
    }

    /** Прочитать файл {@link #fileName()} (если он есть). {@code false} — файла нет. */
    public boolean load() {
        if (!Lumina.isInitialized()) {
            return false;
        }
        String text = Lumina.platform().readFile(fileName);
        if (text == null) {
            return false;
        }
        read(text);
        return true;
    }

    /** Записать файл {@link #fileName()}. */
    public boolean save() {
        return Lumina.isInitialized() && Lumina.platform().writeFile(fileName, write());
    }

    // ================================================================ строки

    static String formatFloat(float v) {
        if (v == Math.rint(v) && Math.abs(v) < 1e7) {
            return String.format(Locale.ROOT, "%.1f", v);
        }
        String s = String.format(Locale.ROOT, "%.4f", v);
        s = s.replaceAll("0+$", "");
        return s.endsWith(".") ? s + "0" : s;
    }

    static String quote(String s) {
        StringBuilder sb = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\t' -> sb.append("\\t");
                case '\r' -> sb.append("\\r");
                default -> sb.append(c);
            }
        }
        return sb.append('"').toString();
    }

    static String unquote(String s) {
        String t = s.trim();
        if (t.length() < 2 || !(t.startsWith("\"") && t.endsWith("\""))) {
            return t;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < t.length() - 1; i++) {
            char c = t.charAt(i);
            if (c == '\\' && i + 1 < t.length() - 1) {
                char n = t.charAt(++i);
                switch (n) {
                    case 'n' -> sb.append('\n');
                    case 't' -> sb.append('\t');
                    case 'r' -> sb.append('\r');
                    default -> sb.append(n);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /** Убрать комментарий {@code #}, не трогая решётки внутри кавычек ("#FF0000"). */
    static String stripComment(String line) {
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && quoted) {
                i++;
            } else if (c == '"') {
                quoted = !quoted;
            } else if (c == '#' && !quoted) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    // ================================================================ построитель

    /** Построитель: {@code category(...)} открывает раздел, методы параметров добавляют в него. */
    public static final class Builder {
        private final String fileName;
        private String header = "Настройки (файл создан автоматически; # — комментарии)";
        private final List<Category> categories = new ArrayList<>();
        private Category current;
        private Option<?> last;

        private Builder(String fileName) {
            this.fileName = Objects.requireNonNull(fileName, "fileName");
        }

        /** Заголовок файла (строки станут комментариями). */
        public Builder header(String header) {
            this.header = header;
            return this;
        }

        public Builder category(String id, String title) {
            for (Category c : categories) {
                if (c.id().equals(id)) {
                    current = c;
                    return this;
                }
            }
            current = new Category(id, title == null ? id : title);
            categories.add(current);
            return this;
        }

        /** Предмет-иконка текущей категории для боковой навигации. */
        public Builder icon(String itemId) {
            requireCategory().icon = itemId;
            return this;
        }

        private Category requireCategory() {
            if (current == null) {
                category("general", "Общие");
            }
            return current;
        }

        private <T> Builder add(String key, String label, Type type, T def) {
            Category c = requireCategory();
            for (Option<?> o : c.options) {
                if (o.key().equals(key)) {
                    throw new IllegalArgumentException("Duplicate config key: " + c.id() + "." + key);
                }
            }
            Option<T> o = new Option<>(c, key, label == null ? key : label, type, def);
            c.options.add(o);
            last = o;
            return this;
        }

        public Builder bool(String key, String label, boolean def) {
            return add(key, label, Type.BOOL, def);
        }

        public Builder integer(String key, String label, int def, int min, int max) {
            add(key, label, Type.INT, Math.max(min, Math.min(max, def)));
            last.min = min;
            last.max = max;
            return this;
        }

        public Builder number(String key, String label, float def, float min, float max) {
            add(key, label, Type.FLOAT, Math.max(min, Math.min(max, def)));
            last.min = min;
            last.max = max;
            return this;
        }

        public Builder string(String key, String label, String def) {
            return add(key, label, Type.STRING, def == null ? "" : def);
        }

        @SafeVarargs
        public final <E extends Enum<E>> Builder choice(String key, String label, E def, E... values) {
            add(key, label, Type.ENUM, def);
            @SuppressWarnings("unchecked")
            Option<E> o = (Option<E>) last;
            o.choices = values.length == 0 ? List.of(def) : List.of(values);
            return this;
        }

        /** Подписи вариантов последнего перечисления. */
        @SuppressWarnings({"unchecked", "rawtypes"})
        public Builder labels(Function<? super Enum<?>, String> labeler) {
            if (last == null || last.type() != Type.ENUM) {
                throw new IllegalStateException("labels() must follow choice()");
            }
            ((Option) last).labeler = labeler;
            return this;
        }

        public Builder color(String key, String label, int def) {
            return add(key, label, Type.COLOR, def);
        }

        /** Клавиша (код GLFW, {@code KeyBindField.NONE} — не задана). */
        public Builder key(String key, String label, int def) {
            return add(key, label, Type.KEY, def);
        }

        // ---- уточнения последнего параметра

        public Builder comment(String comment) {
            requireLast().comment = comment == null ? "" : comment;
            return this;
        }

        public Builder step(float step) {
            requireLast().step = Math.max(0f, step);
            return this;
        }

        public Builder maxLength(int maxLength) {
            requireLast().maxLength = Math.max(1, maxLength);
            return this;
        }

        /** Разрешить прозрачность цвета (по умолчанию да). */
        public Builder alpha(boolean alpha) {
            Option<?> o = requireLast();
            o.alpha = alpha;
            return this;
        }

        private Option<?> requireLast() {
            if (last == null) {
                throw new IllegalStateException("No option to configure yet");
            }
            return last;
        }

        public ConfigSpec build() {
            return new ConfigSpec(fileName, header, categories);
        }
    }
}
