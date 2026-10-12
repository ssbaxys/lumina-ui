package io.github.ssbaxys.lumina.style;

import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Неизменяемый набор значений свойств + варианты для состояний (hover, pressed...).
 *
 * <p>Значения хранятся разреженным массивом по id ключа. Итоговые значения для каждой
 * комбинации состояний считаются один раз и кэшируются, поэтому смена hover стоит O(1).</p>
 *
 * <p>Значение может ссылаться на тему ({@link Builder#bind}, {@link Builder#fromTheme}): такие ссылки
 * заменяются настоящими значениями, когда стиль применяется к элементу ({@link Theme#resolve}).</p>
 */
public final class Style {
    static {
        Styles.bootstrap();
        Tokens.bootstrap();
    }

    /** Значение, вычисляемое из темы. */
    record ThemeRef(Function<Theme, ?> compute) {
    }

    public static final Style EMPTY = new Style(new Object[0], new int[0], new Object[0][]);

    private static final int CACHE_STATES = 64;

    private final Object[] values;
    private final int[] variantMasks;
    private final Object[][] variantValues;
    private final Object[][] resolvedCache = new Object[CACHE_STATES][];
    private final boolean themed;

    private Style(Object[] values, int[] variantMasks, Object[][] variantValues) {
        this.values = values;
        this.variantMasks = variantMasks;
        this.variantValues = variantValues;
        boolean refs = hasRefs(values);
        for (Object[] v : variantValues) {
            refs |= hasRefs(v);
        }
        this.themed = refs;
    }

    private static boolean hasRefs(Object[] a) {
        for (Object o : a) {
            if (o instanceof ThemeRef) {
                return true;
            }
        }
        return false;
    }

    /** Есть ли значения, ссылающиеся на тему. */
    public boolean isThemed() {
        return themed;
    }

    /** Копия, где каждое непустое значение пропущено через {@code f} (в базе и во всех вариантах). */
    Style mapValues(UnaryOperator<Object> f) {
        Object[] v = mapArray(values, f);
        Object[][] vv = new Object[variantValues.length][];
        for (int i = 0; i < vv.length; i++) {
            vv[i] = mapArray(variantValues[i], f);
        }
        return new Style(v, variantMasks, vv);
    }

    private static Object[] mapArray(Object[] a, UnaryOperator<Object> f) {
        Object[] out = new Object[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = a[i] == null ? null : f.apply(a[i]);
        }
        return out;
    }

    public static Builder builder() {
        return new Builder();
    }

    public <T> T get(StyleKey<T> key) {
        int id = key.id();
        return id < values.length ? key.cast(values[id]) : null;
    }

    public boolean isEmpty() {
        return this == EMPTY || (variantMasks.length == 0 && allNull(values));
    }

    /**
     * Значения для данного набора состояний: базовые, затем все варианты, чья маска
     * полностью входит в состояние, в порядке объявления. Результат нельзя изменять.
     */
    public Object[] resolve(int state) {
        if (state >= 0 && state < CACHE_STATES) {
            Object[] cached = resolvedCache[state];
            if (cached == null) {
                cached = compute(state);
                resolvedCache[state] = cached;
            }
            return cached;
        }
        return compute(state);
    }

    private Object[] compute(int state) {
        int len = values.length;
        for (int i = 0; i < variantMasks.length; i++) {
            if ((state & variantMasks[i]) == variantMasks[i]) {
                len = Math.max(len, variantValues[i].length);
            }
        }
        Object[] out = Arrays.copyOf(values, len);
        for (int i = 0; i < variantMasks.length; i++) {
            if ((state & variantMasks[i]) == variantMasks[i]) {
                overlay(out, variantValues[i]);
            }
        }
        return out;
    }

    /** Новый стиль, где значения и варианты {@code over} перекрывают текущие. */
    public Style merge(Style over) {
        if (over == null || over.isEmpty()) {
            return this;
        }
        if (this.isEmpty()) {
            return over;
        }
        Object[] merged = Arrays.copyOf(values, Math.max(values.length, over.values.length));
        overlay(merged, over.values);
        int n = variantMasks.length;
        int[] masks = Arrays.copyOf(variantMasks, n + over.variantMasks.length);
        System.arraycopy(over.variantMasks, 0, masks, n, over.variantMasks.length);
        Object[][] vv = Arrays.copyOf(variantValues, n + over.variantValues.length);
        System.arraycopy(over.variantValues, 0, vv, n, over.variantValues.length);
        return new Style(merged, masks, vv);
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.values = Arrays.copyOf(values, Math.max(values.length, StyleKey.count()));
        for (int i = 0; i < variantMasks.length; i++) {
            b.addVariant(variantMasks[i], variantValues[i]);
        }
        return b;
    }

    private static void overlay(Object[] target, Object[] src) {
        for (int i = 0; i < src.length; i++) {
            if (src[i] != null) {
                target[i] = src[i];
            }
        }
    }

    private static boolean allNull(Object[] a) {
        for (Object o : a) {
            if (o != null) {
                return false;
            }
        }
        return true;
    }

    public static final class Builder {
        private Object[] values = new Object[StyleKey.count()];
        private int[] masks = new int[0];
        private Object[][] variants = new Object[0][];

        private Builder() {
        }

        public <T> Builder set(StyleKey<T> key, T value) {
            int id = key.id();
            if (id >= values.length) {
                values = Arrays.copyOf(values, Math.max(id + 1, StyleKey.count()));
            }
            values[id] = value;
            return this;
        }

        /** Значения, действующие когда у элемента выставлены все биты {@code stateMask}. */
        public Builder when(int stateMask, Consumer<Builder> variant) {
            Builder v = new Builder();
            variant.accept(v);
            addVariant(stateMask, v.values);
            for (int i = 0; i < v.masks.length; i++) {
                addVariant(stateMask | v.masks[i], v.variants[i]);
            }
            return this;
        }

        /** Значение берётся из токена темы и меняется вместе с ним. */
        public <T> Builder bind(StyleKey<T> key, ThemeVar<T> var) {
            return fromTheme(key, theme -> theme.get(var));
        }

        /** Значение вычисляется из токена: {@code bind(BACKGROUND, Tokens.ACCENT, Brush::solid)}. */
        public <T, V> Builder bind(StyleKey<T> key, ThemeVar<V> var, Function<? super V, ? extends T> derive) {
            return fromTheme(key, theme -> derive.apply(theme.get(var)));
        }

        /** Значение вычисляется из темы целиком (можно сочетать несколько токенов). */
        public <T> Builder fromTheme(StyleKey<T> key, Function<Theme, ? extends T> compute) {
            int id = key.id();
            if (id >= values.length) {
                values = Arrays.copyOf(values, Math.max(id + 1, StyleKey.count()));
            }
            values[id] = new ThemeRef(compute);
            return this;
        }

        /** Убрать значение (тогда действует тема/наследование/умолчание). */
        public Builder unset(StyleKey<?> key) {
            int id = key.id();
            if (id < values.length) {
                values[id] = null;
            }
            return this;
        }

        public Builder hovered(Consumer<Builder> variant) {
            return when(State.HOVERED, variant);
        }

        public Builder pressed(Consumer<Builder> variant) {
            return when(State.PRESSED, variant);
        }

        public Builder focused(Consumer<Builder> variant) {
            return when(State.FOCUSED, variant);
        }

        public Builder disabled(Consumer<Builder> variant) {
            return when(State.DISABLED, variant);
        }

        public Builder checked(Consumer<Builder> variant) {
            return when(State.CHECKED, variant);
        }

        private void addVariant(int mask, Object[] vals) {
            masks = Arrays.copyOf(masks, masks.length + 1);
            masks[masks.length - 1] = mask;
            variants = Arrays.copyOf(variants, variants.length + 1);
            variants[variants.length - 1] = vals;
        }

        public Style build() {
            return new Style(Arrays.copyOf(values, values.length), masks.clone(), variants.clone());
        }
    }
}
