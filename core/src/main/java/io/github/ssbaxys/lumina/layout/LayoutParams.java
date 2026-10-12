package io.github.ssbaxys.lumina.layout;

import io.github.ssbaxys.lumina.geom.Insets;

/** Параметры элемента для родительской раскладки (изменяемые; после изменения вызовите invalidateLayout). */
public final class LayoutParams {
    public Dim width = Dim.AUTO;
    public Dim height = Dim.AUTO;
    public float minWidth;
    public float minHeight;
    public float maxWidth = Float.POSITIVE_INFINITY;
    public float maxHeight = Float.POSITIVE_INFINITY;
    public Insets margin = Insets.ZERO;
    /** Доля свободного места по главной оси (flex-grow). */
    public float grow;
    /** Доля сжатия при нехватке места (flex-shrink). */
    public float shrink = 1f;
    /** Переопределяет выравнивание родителя; {@code null} — как у родителя. */
    public Align alignSelf;
    /** Абсолютное позиционирование: вне потока, координаты {@link #left}/{@link #top} от области содержимого. */
    public boolean absolute;
    public float left;
    public float top;

    public float clampWidth(float w) {
        return Math.max(minWidth, Math.min(maxWidth, w));
    }

    public float clampHeight(float h) {
        return Math.max(minHeight, Math.min(maxHeight, h));
    }
}
