package io.github.ssbaxys.lumina.a11y;

import io.github.ssbaxys.lumina.render.ColorFilter;

/**
 * Настройки доступности одного корня интерфейса ({@code UiRoot.accessibility()}). Действуют поверх любой
 * темы, поэтому не теряются при её смене (расписание тем, редактор, светлая/тёмная):
 *
 * <ul>
 *   <li>{@link #colorVision} — фильтр цвета для дальтоников на весь кадр: коррекция или симуляция;</li>
 *   <li>{@link #textScale} — крупный текст: множитель {@code Styles.FONT_SCALE} всех элементов;</li>
 *   <li>{@link #reducedMotion} — без анимаций переходов стиля (смена состояний и темы мгновенная);</li>
 *   <li>{@link #focusAlwaysVisible} — заметная рамка вокруг элемента в фокусе, в любой теме;</li>
 *   <li>{@link #narration} — озвучивать подпись элемента при получении фокуса ({@code Platform.narrate}).</li>
 * </ul>
 *
 * Высококонтрастная тема — отдельная тема ({@link AccessibleThemes#highContrast}), а не флаг.
 *
 * <pre>{@code
 * root.accessibility().colorVision(ColorVision.DEUTERANOPIA).textScale(1.25f).reducedMotion(true);
 * }</pre>
 */
public final class Accessibility {
    /** Что делать с цветами при выбранном {@link ColorVision}. */
    public enum ColorMode {
        /** Коррекция (daltonize): помочь различать цвета. */
        CORRECT,
        /** Симуляция: показать, как интерфейс видит человек с таким зрением. */
        SIMULATE
    }

    private ColorVision colorVision = ColorVision.NORMAL;
    private ColorMode colorMode = ColorMode.CORRECT;
    private ColorFilter extraFilter;
    private ColorFilter filter;
    private float textScale = 1f;
    private boolean reducedMotion;
    private boolean focusAlwaysVisible;
    private boolean narration;
    private Runnable listener;

    /** Подписка владельца (корня) на изменения. Один слушатель. */
    public void onChange(Runnable listener) {
        this.listener = listener;
    }

    private void changed() {
        if (listener != null) {
            listener.run();
        }
    }

    // ---------------------------------------------------------------- цвет

    public ColorVision colorVision() {
        return colorVision;
    }

    public ColorMode colorMode() {
        return colorMode;
    }

    public Accessibility colorVision(ColorVision vision) {
        return colorVision(vision, colorMode);
    }

    public Accessibility colorVision(ColorVision vision, ColorMode mode) {
        ColorVision v = vision == null ? ColorVision.NORMAL : vision;
        ColorMode m = mode == null ? ColorMode.CORRECT : mode;
        if (v != colorVision || m != colorMode) {
            colorVision = v;
            colorMode = m;
            filter = null;
            changed();
        }
        return this;
    }

    /** Свой фильтр поверх режима зрения (например, {@code ColorFilter.grayscale()}); {@code null} — убрать. */
    public Accessibility extraFilter(ColorFilter f) {
        if (f != extraFilter) {
            extraFilter = f;
            filter = null;
            changed();
        }
        return this;
    }

    /** Итоговый фильтр кадра или {@code null}, если цвета не меняются. */
    public ColorFilter colorFilter() {
        if (filter == null) {
            ColorFilter base = colorVision == ColorVision.NORMAL ? ColorFilter.IDENTITY
                    : colorMode == ColorMode.SIMULATE ? colorVision.simulate() : colorVision.correct();
            filter = extraFilter == null ? base : base == ColorFilter.IDENTITY ? extraFilter : base.then(extraFilter);
        }
        return filter == ColorFilter.IDENTITY ? null : filter;
    }

    // ---------------------------------------------------------------- текст, движение, фокус, диктор

    public float textScale() {
        return textScale;
    }

    /** Множитель размера текста (1 — как в теме, 1.25 — крупнее). Ограничен диапазоном 0.5–3. */
    public Accessibility textScale(float scale) {
        float s = Math.max(0.5f, Math.min(3f, scale));
        if (s != textScale) {
            textScale = s;
            changed();
        }
        return this;
    }

    public boolean reducedMotion() {
        return reducedMotion;
    }

    /** Без переходов стиля: hover, нажатие, смена темы применяются сразу. */
    public Accessibility reducedMotion(boolean on) {
        if (on != reducedMotion) {
            reducedMotion = on;
            changed();
        }
        return this;
    }

    public boolean focusAlwaysVisible() {
        return focusAlwaysVisible;
    }

    /** Рамка фокуса поверх всего интерфейса, даже если тема её не рисует. */
    public Accessibility focusAlwaysVisible(boolean on) {
        if (on != focusAlwaysVisible) {
            focusAlwaysVisible = on;
            changed();
        }
        return this;
    }

    public boolean narration() {
        return narration;
    }

    /** Озвучивать элемент, получивший фокус (через {@code Platform.narrate}). */
    public Accessibility narration(boolean on) {
        if (on != narration) {
            narration = on;
            changed();
        }
        return this;
    }

    /** Всё по умолчанию. */
    public Accessibility reset() {
        colorVision(ColorVision.NORMAL, ColorMode.CORRECT);
        extraFilter(null);
        textScale(1f);
        reducedMotion(false);
        focusAlwaysVisible(false);
        narration(false);
        return this;
    }
}
