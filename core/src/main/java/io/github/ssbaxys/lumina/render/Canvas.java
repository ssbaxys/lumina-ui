package io.github.ssbaxys.lumina.render;

/**
 * Поверхность рисования для виджетов. Виджеты знают только этот интерфейс,
 * поэтому один и тот же код работает на любой версии Minecraft.
 *
 * <p>Трансформации — перенос и масштаб (без поворота), состояние сохраняется через
 * {@link #save()}/{@link #restore()} вместе с клипом, прозрачностью и режимом смешивания.</p>
 */
public interface Canvas {
    void save();

    void restore();

    void translate(float dx, float dy);

    void scale(float sx, float sy);

    /** Пересекает текущую область отсечения с прямоугольником (в локальных координатах). */
    void clipRect(float x, float y, float w, float h);

    void multiplyOpacity(float factor);

    float opacity();

    /** Режим смешивания для всего, что рисуется дальше (до {@link #restore()}). Текст всегда обычный. */
    void setBlendMode(BlendMode mode);

    BlendMode blendMode();

    /** Попадает ли прямоугольник в видимую область. Для отсечения невидимых поддеревьев. */
    boolean isVisible(float x, float y, float w, float h);

    // ---------------------------------------------------------------- прямоугольники

    void fillRect(float x, float y, float w, float h, int argb);

    void fillRect(float x, float y, float w, float h, Brush brush);

    void fillRoundRect(float x, float y, float w, float h, Radii radii, Brush brush);

    void strokeRoundRect(float x, float y, float w, float h, Radii radii, float width, int argb);

    /** Обводка градиентом (например, переливающаяся радужная рамка). */
    void strokeRoundRect(float x, float y, float w, float h, Radii radii, float width, Brush brush);

    void drawShadow(float x, float y, float w, float h, Radii radii, Shadow shadow);

    /** Тень с аддитивным смешиванием — свечение, которое не темнит фон, а подсвечивает его. */
    default void drawGlow(float x, float y, float w, float h, Radii radii, Shadow glow) {
        BlendMode prev = blendMode();
        setBlendMode(BlendMode.ADD);
        drawShadow(x, y, w, h, radii, glow);
        setBlendMode(prev);
    }

    // ---------------------------------------------------------------- круги и дуги

    default void fillCircle(float cx, float cy, float radius, Brush brush) {
        fillRoundRect(cx - radius, cy - radius, 2f * radius, 2f * radius, Radii.all(radius), brush);
    }

    default void strokeCircle(float cx, float cy, float radius, float width, Brush brush) {
        strokeRoundRect(cx - radius, cy - radius, 2f * radius, 2f * radius, Radii.all(radius), width, brush);
    }

    /**
     * Дуга окружности толщиной {@code width}: от угла {@code startDeg} (0 — «12 часов», по часовой)
     * на {@code sweepDeg} градусов, цвет плавно идёт от {@code from} к {@code to}. Кольцевые индикаторы,
     * спиннеры, диаграммы.
     */
    void drawArc(float cx, float cy, float radius, float width, float startDeg, float sweepDeg, int from, int to);

    // ---------------------------------------------------------------- произвольные фигуры

    /** Отрезок толщиной {@code width}. */
    void drawLine(float x0, float y0, float x1, float y1, float width, int argb);

    void fillTriangle(float x0, float y0, float x1, float y1, float x2, float y2, int argb);

    /** Произвольный выпуклый четырёхугольник со своим цветом в каждой вершине. */
    void fillQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                  int c0, int c1, int c2, int c3);

    // ---------------------------------------------------------------- текст и текстуры

    void drawText(String text, float x, float y, int argb, boolean shadow);

    void drawTexture(TextureRef texture, float x, float y, float w, float h,
                     float u0, float v0, float u1, float v1, int tint);

    TextMetrics textMetrics();
}
