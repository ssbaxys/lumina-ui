package io.github.ssbaxys.lumina.hud;

/**
 * Точка привязки элемента HUD к экрану: углы, середины краёв и центр. Элемент прижимается к этой точке
 * своей соответствующей точкой ({@link #TOP_RIGHT} — правым верхним углом к правому верхнему углу экрана).
 *
 * <p>Смещение записи ({@link HudEntry#offset}) отсчитывается <b>внутрь экрана</b> от привязанного края, как
 * {@code right: 4px} в CSS: у {@code TOP_RIGHT} положительный {@code dx} сдвигает влево, у {@code BOTTOM_*}
 * положительный {@code dy} — вверх. У центральных привязок смещение обычное: вправо и вниз.</p>
 */
public enum HudAnchor {
    TOP_LEFT(0f, 0f),
    TOP_CENTER(0.5f, 0f),
    TOP_RIGHT(1f, 0f),
    CENTER_LEFT(0f, 0.5f),
    CENTER(0.5f, 0.5f),
    CENTER_RIGHT(1f, 0.5f),
    BOTTOM_LEFT(0f, 1f),
    BOTTOM_CENTER(0.5f, 1f),
    BOTTOM_RIGHT(1f, 1f);

    private final float fx;
    private final float fy;

    HudAnchor(float fx, float fy) {
        this.fx = fx;
        this.fy = fy;
    }

    /** Доля по горизонтали: 0 — левый край, 0.5 — середина, 1 — правый край. */
    public float fx() {
        return fx;
    }

    /** Доля по вертикали: 0 — верх, 0.5 — середина, 1 — низ. */
    public float fy() {
        return fy;
    }

    /** Левый край элемента шириной {@code size} на экране шириной {@code screen} со смещением внутрь. */
    public float x(float screen, float size, float offset) {
        return (screen - size) * fx + (fx == 1f ? -offset : offset);
    }

    /** Верхний край элемента высотой {@code size} на экране высотой {@code screen} со смещением внутрь. */
    public float y(float screen, float size, float offset) {
        return (screen - size) * fy + (fy == 1f ? -offset : offset);
    }

    /** Привязан к правому краю. */
    public boolean isRight() {
        return fx == 1f;
    }

    /** Привязан к нижнему краю. */
    public boolean isBottom() {
        return fy == 1f;
    }
}
