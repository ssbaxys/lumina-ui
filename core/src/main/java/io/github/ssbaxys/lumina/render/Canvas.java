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

    /**
     * Фильтр цвета для всего, что рисуется дальше ({@code null} — без фильтра): режимы для дальтоников и т. п.
     * Не входит в {@link #save()}/{@link #restore()} — его ставит и снимает корень интерфейса на кадр.
     * По умолчанию бэкенд фильтры не поддерживает и вызов ничего не делает.
     */
    default void setColorFilter(ColorFilter filter) {
    }

    default ColorFilter colorFilter() {
        return null;
    }

    /**
     * Отсечение по скруглённому прямоугольнику: всё, что нарисовано до парного {@link #restore()}, видно только
     * внутри фигуры. Бэкенд, который не умеет точную маску ({@link #supportsRoundClip()} = {@code false}),
     * отсекает по обычному прямоугольнику — углы тогда не скругляются.
     */
    default void clipRoundRect(float x, float y, float w, float h, Radii radii) {
        clipRect(x, y, w, h);
    }

    /** Умеет ли бэкенд точно отсекать по скруглённой фигуре ({@link #clipRoundRect}). */
    default boolean supportsRoundClip() {
        return false;
    }

    /**
     * Размыть то, что уже нарисовано под прямоугольником («матовое стекло», CSS {@code backdrop-filter: blur}).
     * {@code radius} — сила размытия в пикселях. Без поддержки бэкенда ({@link #supportsBackdropBlur()})
     * рисуется полупрозрачная матовая подложка ({@link #frostedFallback}).
     */
    default void drawBackdropBlur(float x, float y, float w, float h, Radii radii, float radius) {
        frostedFallback(this, x, y, w, h, radii, radius);
    }

    /** Умеет ли бэкенд настоящее размытие фона ({@link #drawBackdropBlur}). */
    default boolean supportsBackdropBlur() {
        return false;
    }

    /**
     * Запасной вид размытия: светлая полупрозрачная заливка с бликом сверху. Чем больше радиус, тем она
     * плотнее, поэтому фон за карточкой всё равно «приглушается», как за матовым стеклом.
     */
    static void frostedFallback(Canvas canvas, float x, float y, float w, float h, Radii radii, float radius) {
        if (radius <= 0f || w <= 0f || h <= 0f) {
            return;
        }
        float k = Math.min(1f, radius / 12f);
        int top = Color.withAlpha(0xFFDDE4F0, (int) (0x18 + 0x30 * k));
        int bottom = Color.withAlpha(0xFF9AA6BC, (int) (0x14 + 0x28 * k));
        canvas.fillRoundRect(x, y, w, h, radii, Brush.vertical(top, bottom));
        float glare = Math.min(h, Math.max(2f, h * 0.35f));
        canvas.fillRoundRect(x, y, w, glare, radii == null ? Radii.ZERO : new Radii(radii.topLeft(), radii.topRight(), 0f, 0f),
                Brush.vertical(Color.withAlpha(0xFFFFFFFF, (int) (0x10 * k + 0x06)), 0x00FFFFFF));
    }

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

    /** Текстура целиком в прямоугольнике. */
    default void drawTexture(TextureRef texture, float x, float y, float w, float h, int tint) {
        drawTexture(texture, x, y, w, h, 0f, 0f, 1f, 1f, tint);
    }

    /**
     * Часть текстуры (u0..u1, v0..v1) в прямоугольнике со скруглёнными углами. По умолчанию фигура
     * режется на горизонтальные полосы-трапеции, поэтому работает на любом бэкенде без трафарета.
     */
    default void drawTextureRounded(TextureRef texture, float x, float y, float w, float h,
                                    float u0, float v0, float u1, float v1, Radii radii, int tint) {
        if (w <= 0f || h <= 0f) {
            return;
        }
        float max = Math.min(w, h) / 2f;
        float tl = radii == null ? 0f : Math.min(radii.topLeft(), max);
        float tr = radii == null ? 0f : Math.min(radii.topRight(), max);
        float br = radii == null ? 0f : Math.min(radii.bottomRight(), max);
        float bl = radii == null ? 0f : Math.min(radii.bottomLeft(), max);
        if (tl <= 0f && tr <= 0f && br <= 0f && bl <= 0f) {
            drawTexture(texture, x, y, w, h, u0, v0, u1, v1, tint);
            return;
        }
        float top = Math.max(tl, tr);
        float bottom = Math.max(bl, br);
        int topSteps = Math.max(2, Math.min(10, (int) Math.ceil(top / 1.5f)));
        int bottomSteps = Math.max(2, Math.min(10, (int) Math.ceil(bottom / 1.5f)));
        float prev = 0f;
        for (int i = 1; i <= topSteps; i++) {
            float yy = top * i / topSteps;
            roundedStrip(texture, x, y, w, h, u0, v0, u1, v1, tl, tr, br, bl, prev, yy, tint);
            prev = yy;
        }
        if (h - bottom > prev) {
            roundedStrip(texture, x, y, w, h, u0, v0, u1, v1, tl, tr, br, bl, prev, h - bottom, tint);
            prev = h - bottom;
        }
        for (int i = 1; i <= bottomSteps; i++) {
            float yy = h - bottom + bottom * i / bottomSteps;
            if (yy > prev) {
                roundedStrip(texture, x, y, w, h, u0, v0, u1, v1, tl, tr, br, bl, prev, yy, tint);
                prev = yy;
            }
        }
    }

    private void roundedStrip(TextureRef texture, float x, float y, float w, float h,
                              float u0, float v0, float u1, float v1,
                              float tl, float tr, float br, float bl, float ya, float yb, int tint) {
        float la = roundedInset(ya, h, tl, bl);
        float lb = roundedInset(yb, h, tl, bl);
        float ra = w - roundedInset(ya, h, tr, br);
        float rb = w - roundedInset(yb, h, tr, br);
        float du = (u1 - u0) / w;
        float dv = (v1 - v0) / h;
        drawTextureQuad(texture,
                x + la, y + ya, x + lb, y + yb, x + rb, y + yb, x + ra, y + ya,
                u0 + la * du, v0 + ya * dv, u0 + lb * du, v0 + yb * dv,
                u0 + rb * du, v0 + yb * dv, u0 + ra * du, v0 + ya * dv, tint);
    }

    /** Отступ края скруглённого прямоугольника от его стороны на высоте {@code yy}. */
    private static float roundedInset(float yy, float h, float rTop, float rBottom) {
        if (rTop > 0f && yy < rTop) {
            float d = rTop - yy;
            return rTop - (float) Math.sqrt(Math.max(0f, rTop * rTop - d * d));
        }
        if (rBottom > 0f && yy > h - rBottom) {
            float d = yy - (h - rBottom);
            return rBottom - (float) Math.sqrt(Math.max(0f, rBottom * rBottom - d * d));
        }
        return 0f;
    }

    /**
     * Текстура на произвольном выпуклом четырёхугольнике (вершины по порядку обхода, UV для каждой):
     * изометрические кубики, наклонённые и повёрнутые спрайты.
     */
    void drawTextureQuad(TextureRef texture,
                         float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                         float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3, int tint);

    /**
     * Предмет игры (как в инвентаре, в том числе 3D-блоки): {@code drawItem("minecraft:tnt", x, y, 32)}.
     * Прозрачность и режим смешивания на предметы не действуют.
     */
    void drawItem(String itemId, float x, float y, float size);

    TextMetrics textMetrics();
}
