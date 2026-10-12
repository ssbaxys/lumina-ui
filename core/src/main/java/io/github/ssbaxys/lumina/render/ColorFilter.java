package io.github.ssbaxys.lumina.render;

import java.util.function.IntUnaryOperator;

/**
 * Преобразование цвета ARGB → ARGB для всего, что рисуется на {@link Canvas}: режимы для дальтоников,
 * оттенки серого, инверсия. Задаётся на уровне корня ({@code UiRoot.accessibility().colorFilter(...)}),
 * и {@link AbstractCanvas} пропускает через него заливки, рамки, тени, градиенты и текст.
 * Текстуры и предметы не меняются (цвет их пикселей считает видеокарта).
 *
 * <p>Альфа должна сохраняться: фильтр меняет только цвет.</p>
 */
@FunctionalInterface
public interface ColorFilter extends IntUnaryOperator {
    /** Ничего не меняет. */
    ColorFilter IDENTITY = c -> c;

    int filter(int argb);

    @Override
    default int applyAsInt(int argb) {
        return filter(argb);
    }

    /** Сначала этот фильтр, потом {@code next}. */
    default ColorFilter then(ColorFilter next) {
        if (next == null || next == IDENTITY) {
            return this;
        }
        ColorFilter self = this;
        return c -> next.filter(self.filter(c));
    }

    /**
     * Фильтр с кэшем последних цветов: в кадре одни и те же цвета повторяются сотни раз, а дорогие
     * фильтры (с переходом в линейное пространство) считать каждый раз незачем. Однопоточный.
     */
    static ColorFilter cached(ColorFilter f) {
        if (f == null || f == IDENTITY || f instanceof Cached) {
            return f;
        }
        return new Cached(f);
    }

    /** Цветовая матрица 3×3 над линейным RGB (по строкам); альфа не меняется. */
    static ColorFilter linearMatrix(float... m) {
        if (m.length != 9) {
            throw new IllegalArgumentException("3x3 matrix expected");
        }
        float[] k = m.clone();
        return c -> {
            float r = Color.srgbToLinear(Color.red(c) / 255f);
            float g = Color.srgbToLinear(Color.green(c) / 255f);
            float b = Color.srgbToLinear(Color.blue(c) / 255f);
            float nr = k[0] * r + k[1] * g + k[2] * b;
            float ng = k[3] * r + k[4] * g + k[5] * b;
            float nb = k[6] * r + k[7] * g + k[8] * b;
            return Color.argb(Color.alpha(c), channel(nr), channel(ng), channel(nb));
        };
    }

    /** Оттенки серого (по яркости). */
    static ColorFilter grayscale() {
        return Color::grayscale;
    }

    private static int channel(float linear) {
        float v = Color.linearToSrgb(Math.max(0f, Math.min(1f, linear)));
        return Math.round(v * 255f);
    }

    /** Кэш с прямой адресацией на 1024 цвета. */
    final class Cached implements ColorFilter {
        private static final int SIZE = 1024;
        private final ColorFilter inner;
        private final int[] keys = new int[SIZE];
        private final int[] values = new int[SIZE];
        private final boolean[] used = new boolean[SIZE];

        private Cached(ColorFilter inner) {
            this.inner = inner;
        }

        @Override
        public int filter(int argb) {
            int h = argb * 0x9E3779B9;
            int i = (h ^ (h >>> 16)) & (SIZE - 1);
            if (used[i] && keys[i] == argb) {
                return values[i];
            }
            int v = inner.filter(argb);
            keys[i] = argb;
            values[i] = v;
            used[i] = true;
            return v;
        }
    }
}
