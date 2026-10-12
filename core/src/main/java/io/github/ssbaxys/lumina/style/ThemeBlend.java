package io.github.ssbaxys.lumina.style;

import io.github.ssbaxys.lumina.render.Color;

import java.util.List;

/**
 * Смешивание тем по токенам: цветовые токены интерполируются в OKLAB (без «грязной» середины),
 * числовые — линейно, остальные переключаются на середине пути. Стили классов берутся из целевой темы.
 *
 * <pre>{@code
 * Theme dusk = ThemeBlend.mix(Theme.light(), Theme.dark(), 0.5f);
 * Object[] a = ThemeBlend.snapshot(day);  ThemeBlend.apply(working, a, b, t);   // покадрово, без копий
 * }</pre>
 */
public final class ThemeBlend {
    private ThemeBlend() {
    }

    /** Новая тема: копия {@code to} с токенами между {@code from} (t = 0) и {@code to} (t = 1). */
    public static Theme mix(Theme from, Theme to, float t) {
        Theme out = to.copy();
        apply(out, snapshot(from), snapshot(to), t);
        return out;
    }

    /** Значения всех токенов темы (по id токена). */
    public static Object[] snapshot(Theme theme) {
        List<ThemeVar<?>> vars = vars();
        Object[] out = new Object[vars.size()];
        for (ThemeVar<?> v : vars) {
            out[v.id()] = theme.get(v);
        }
        return out;
    }

    /** Записать в {@code target} токены между двумя снимками ({@link #snapshot}). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void apply(Theme target, Object[] from, Object[] to, float t) {
        float k = Math.max(0f, Math.min(1f, t));
        for (ThemeVar v : vars()) {
            int id = v.id();
            Object a = id < from.length ? from[id] : null;
            Object b = id < to.length ? to[id] : null;
            Object value = lerp(a, b, k);
            if (value != null) {
                target.set(v, value);
            }
        }
    }

    /** Значение между двумя значениями токена. */
    public static Object lerp(Object a, Object b, float t) {
        if (a == null || t >= 1f) {
            return b;
        }
        if (b == null || t <= 0f) {
            return a;
        }
        if (a instanceof Integer ca && b instanceof Integer cb) {
            return ca.equals(cb) ? cb : Color.mix(ca, cb, t);
        }
        if (a instanceof Float fa && b instanceof Float fb) {
            return fa + (fb - fa) * t;
        }
        return t < 0.5f ? a : b;
    }

    /** Отличаются ли снимки токенов. */
    public static boolean differs(Object[] a, Object[] b) {
        return !java.util.Arrays.equals(a, b);
    }

    private static List<ThemeVar<?>> vars() {
        // Вызов загружает класс Tokens, то есть регистрирует встроенные токены.
        Tokens.bootstrap();
        return ThemeVar.all();
    }
}
