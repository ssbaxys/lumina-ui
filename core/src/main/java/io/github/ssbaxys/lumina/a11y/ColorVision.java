package io.github.ssbaxys.lumina.a11y;

import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorFilter;

/**
 * Типы цветового зрения. Для каждого есть два фильтра:
 * <ul>
 *   <li>{@link #simulate()} — как увидит интерфейс человек с таким зрением (проверка дизайна);</li>
 *   <li>{@link #correct()} — коррекция (daltonize): различие цветов, которое теряется, переносится
 *       в каналы, которые человек различает, — красный и зелёный перестают сливаться.</li>
 * </ul>
 * Симуляция — матрицы Machado, Oliveira, Fernandes (2009) для полной степени, в линейном RGB;
 * коррекция — схема Fidaner et al. поверх неё.
 */
public enum ColorVision {
    /** Обычное зрение: фильтры ничего не меняют. */
    NORMAL(null, null),
    /** Протанопия: нет «красных» колбочек. */
    PROTANOPIA(new float[]{
            0.152286f, 1.052583f, -0.204868f,
            0.114503f, 0.786281f, 0.099216f,
            -0.003882f, -0.048116f, 1.051998f},
            new float[]{0f, 0f, 0f, 0.7f, 1f, 0f, 0.7f, 0f, 1f}),
    /** Дейтеранопия: нет «зелёных» колбочек (самый частый случай). */
    DEUTERANOPIA(new float[]{
            0.367322f, 0.860646f, -0.227968f,
            0.280085f, 0.672501f, 0.047413f,
            -0.011820f, 0.042940f, 0.968881f},
            new float[]{0f, 0f, 0f, 0.7f, 1f, 0f, 0.7f, 0f, 1f}),
    /** Тританопия: нет «синих» колбочек. */
    TRITANOPIA(new float[]{
            1.255528f, -0.076749f, -0.178779f,
            -0.078411f, 0.930809f, 0.147602f,
            0.004733f, 0.691367f, 0.303900f},
            new float[]{1f, 0f, 0.7f, 0f, 1f, 0.7f, 0f, 0f, 0f});

    private final float[] simulation;
    private final float[] shift;

    ColorVision(float[] simulation, float[] shift) {
        this.simulation = simulation;
        this.shift = shift;
    }

    /** Фильтр «как видит человек с таким зрением» (с кэшем). */
    public ColorFilter simulate() {
        return this == NORMAL ? ColorFilter.IDENTITY : ColorFilter.cached(this::simulate);
    }

    /** Фильтр-коррекция (daltonize) с кэшем. */
    public ColorFilter correct() {
        return this == NORMAL ? ColorFilter.IDENTITY : ColorFilter.cached(this::correct);
    }

    /** Один цвет глазами человека с таким зрением. Альфа сохраняется. */
    public int simulate(int argb) {
        if (simulation == null) {
            return argb;
        }
        float[] lin = linear(argb);
        float[] s = mul(simulation, lin);
        return pack(argb, s);
    }

    /**
     * Коррекция одного цвета: ошибка (исходный − видимый) переносится матрицей сдвига в различимые каналы
     * и добавляется к исходному. Серые цвета не меняются.
     */
    public int correct(int argb) {
        if (simulation == null) {
            return argb;
        }
        float[] lin = linear(argb);
        float[] seen = mul(simulation, lin);
        float[] err = {lin[0] - seen[0], lin[1] - seen[1], lin[2] - seen[2]};
        float[] d = mul(shift, err);
        return pack(argb, new float[]{lin[0] + d[0], lin[1] + d[1], lin[2] + d[2]});
    }

    private static float[] linear(int c) {
        return new float[]{
                Color.srgbToLinear(Color.red(c) / 255f),
                Color.srgbToLinear(Color.green(c) / 255f),
                Color.srgbToLinear(Color.blue(c) / 255f)};
    }

    private static float[] mul(float[] m, float[] v) {
        return new float[]{
                m[0] * v[0] + m[1] * v[1] + m[2] * v[2],
                m[3] * v[0] + m[4] * v[1] + m[5] * v[2],
                m[6] * v[0] + m[7] * v[1] + m[8] * v[2]};
    }

    private static int pack(int original, float[] lin) {
        return Color.argb(Color.alpha(original), channel(lin[0]), channel(lin[1]), channel(lin[2]));
    }

    private static int channel(float linear) {
        return Math.round(Color.linearToSrgb(Math.max(0f, Math.min(1f, linear))) * 255f);
    }
}
