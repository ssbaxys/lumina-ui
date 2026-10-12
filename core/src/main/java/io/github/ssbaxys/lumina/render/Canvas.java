package io.github.ssbaxys.lumina.render;

/**
 * Поверхность рисования для виджетов. Виджеты знают только этот интерфейс,
 * поэтому один и тот же код работает на любой версии Minecraft.
 *
 * <p>Трансформации — перенос и масштаб (без поворота), состояние сохраняется через
 * {@link #save()}/{@link #restore()} вместе с клипом и прозрачностью.</p>
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

    /** Попадает ли прямоугольник в видимую область. Для отсечения невидимых поддеревьев. */
    boolean isVisible(float x, float y, float w, float h);

    void fillRect(float x, float y, float w, float h, int argb);

    void fillRect(float x, float y, float w, float h, Brush brush);

    void fillRoundRect(float x, float y, float w, float h, Radii radii, Brush brush);

    void strokeRoundRect(float x, float y, float w, float h, Radii radii, float width, int argb);

    void drawShadow(float x, float y, float w, float h, Radii radii, Shadow shadow);

    void drawText(String text, float x, float y, int argb, boolean shadow);

    void drawTexture(TextureRef texture, float x, float y, float w, float h,
                     float u0, float v0, float u1, float v1, int tint);

    TextMetrics textMetrics();
}
