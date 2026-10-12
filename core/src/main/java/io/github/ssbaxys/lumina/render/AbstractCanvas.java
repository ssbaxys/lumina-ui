package io.github.ssbaxys.lumina.render;

/**
 * Общая часть всех бэкендов: стек трансформаций, клипа и прозрачности, отсечение невидимого.
 *
 * <p>Бэкенд загрузчика получает уже готовые экранные координаты и реализует всего несколько
 * примитивов. Состояние хранится в плоском float-массиве, поэтому кадр рисуется без аллокаций.</p>
 */
public abstract class AbstractCanvas implements Canvas {
    private static final int STATE_SIZE = 10;
    private static final BlendMode[] BLEND_MODES = BlendMode.values();
    /** Сетка для градиентов, когда шейдер недоступен. */
    private static final int FALLBACK_GRID = 8;

    private float[] stack = new float[STATE_SIZE * 32];
    private int depth;

    private float tx;
    private float ty;
    private float sx = 1f;
    private float sy = 1f;
    private float opacity = 1f;
    private float clipX0;
    private float clipY0;
    private float clipX1;
    private float clipY1;

    private BlendMode blend = BlendMode.NORMAL;
    private BlendMode appliedBlend = BlendMode.NORMAL;
    private ColorFilter colorFilter;
    private final ShapePaint paint = new ShapePaint();

    private float viewWidth;
    private float viewHeight;

    private boolean clipDirty;
    private boolean clipApplied;
    private float appliedX0;
    private float appliedY0;
    private float appliedX1;
    private float appliedY1;

    /** Начинает кадр. Размеры — в GUI-координатах экрана. */
    protected void beginFrame(float width, float height) {
        depth = 0;
        tx = 0f;
        ty = 0f;
        sx = 1f;
        sy = 1f;
        opacity = 1f;
        viewWidth = width;
        viewHeight = height;
        clipX0 = 0f;
        clipY0 = 0f;
        clipX1 = width;
        clipY1 = height;
        clipDirty = false;
        clipApplied = false;
        blend = BlendMode.NORMAL;
        appliedBlend = BlendMode.NORMAL;
    }

    protected void endFrame() {
        if (appliedBlend != BlendMode.NORMAL) {
            appliedBlend = BlendMode.NORMAL;
            applyBlendMode(BlendMode.NORMAL);
        }
        if (clipApplied) {
            clearClip();
            clipApplied = false;
        }
        if (depth != 0) {
            int d = depth;
            depth = 0;
            throw new IllegalStateException("Unbalanced Canvas.save()/restore(): depth " + d);
        }
    }

    // ---------------------------------------------------------------- состояние

    @Override
    public final void save() {
        int base = depth * STATE_SIZE;
        if (base + STATE_SIZE > stack.length) {
            float[] grown = new float[stack.length * 2];
            System.arraycopy(stack, 0, grown, 0, stack.length);
            stack = grown;
        }
        float[] s = stack;
        s[base] = tx;
        s[base + 1] = ty;
        s[base + 2] = sx;
        s[base + 3] = sy;
        s[base + 4] = opacity;
        s[base + 5] = clipX0;
        s[base + 6] = clipY0;
        s[base + 7] = clipX1;
        s[base + 8] = clipY1;
        s[base + 9] = blend.ordinal();
        depth++;
    }

    @Override
    public final void restore() {
        if (depth == 0) {
            throw new IllegalStateException("Canvas.restore() without save()");
        }
        depth--;
        int base = depth * STATE_SIZE;
        float[] s = stack;
        tx = s[base];
        ty = s[base + 1];
        sx = s[base + 2];
        sy = s[base + 3];
        opacity = s[base + 4];
        blend = BLEND_MODES[(int) s[base + 9]];
        if (clipX0 != s[base + 5] || clipY0 != s[base + 6] || clipX1 != s[base + 7] || clipY1 != s[base + 8]) {
            clipX0 = s[base + 5];
            clipY0 = s[base + 6];
            clipX1 = s[base + 7];
            clipY1 = s[base + 8];
            clipDirty = true;
        }
    }

    @Override
    public final void translate(float dx, float dy) {
        tx += dx * sx;
        ty += dy * sy;
    }

    @Override
    public final void scale(float scaleX, float scaleY) {
        sx *= scaleX;
        sy *= scaleY;
    }

    @Override
    public final void clipRect(float x, float y, float w, float h) {
        float x0 = Math.max(clipX0, tx + x * sx);
        float y0 = Math.max(clipY0, ty + y * sy);
        float x1 = Math.min(clipX1, tx + (x + w) * sx);
        float y1 = Math.min(clipY1, ty + (y + h) * sy);
        clipX0 = x0;
        clipY0 = y0;
        clipX1 = Math.max(x0, x1);
        clipY1 = Math.max(y0, y1);
        clipDirty = true;
    }

    @Override
    public final void multiplyOpacity(float factor) {
        opacity *= factor < 0f ? 0f : Math.min(factor, 1f);
    }

    @Override
    public final float opacity() {
        return opacity;
    }

    @Override
    public final void setBlendMode(BlendMode mode) {
        blend = mode == null ? BlendMode.NORMAL : mode;
    }

    @Override
    public final BlendMode blendMode() {
        return blend;
    }

    @Override
    public final void setColorFilter(ColorFilter filter) {
        colorFilter = filter == ColorFilter.IDENTITY ? null : filter;
    }

    @Override
    public final ColorFilter colorFilter() {
        return colorFilter;
    }

    /** Цвет через фильтр (если он задан). */
    private int filtered(int argb) {
        ColorFilter f = colorFilter;
        return f == null ? argb : f.filter(argb);
    }

    /** Кисть через фильтр: у градиента меняются цвета опор. */
    private Brush filtered(Brush brush) {
        ColorFilter f = colorFilter;
        return f == null || brush == null ? brush : brush.map(f);
    }

    @Override
    public final boolean isVisible(float x, float y, float w, float h) {
        if (opacity <= 0f) {
            return false;
        }
        float x0 = tx + x * sx;
        float y0 = ty + y * sy;
        return x0 < clipX1 && y0 < clipY1 && x0 + w * sx > clipX0 && y0 + h * sy > clipY0;
    }

    private boolean culled(float x0, float y0, float x1, float y1) {
        return x1 <= clipX0 || y1 <= clipY0 || x0 >= clipX1 || y0 >= clipY1;
    }

    private void syncClip() {
        if (blend != appliedBlend) {
            appliedBlend = blend;
            applyBlendMode(blend);
        }
        if (!clipDirty) {
            return;
        }
        clipDirty = false;
        boolean fullScreen = clipX0 <= 0f && clipY0 <= 0f && clipX1 >= viewWidth && clipY1 >= viewHeight;
        if (fullScreen) {
            if (clipApplied) {
                clearClip();
                clipApplied = false;
            }
            return;
        }
        if (clipApplied && appliedX0 == clipX0 && appliedY0 == clipY0 && appliedX1 == clipX1 && appliedY1 == clipY1) {
            return;
        }
        appliedX0 = clipX0;
        appliedY0 = clipY0;
        appliedX1 = clipX1;
        appliedY1 = clipY1;
        clipApplied = true;
        applyClip(clipX0, clipY0, clipX1, clipY1);
    }

    // ---------------------------------------------------------------- примитивы

    @Override
    public final void fillRect(float x, float y, float w, float h, int argb) {
        int c = Color.mulAlpha(filtered(argb), opacity);
        if (w <= 0f || h <= 0f || Color.alpha(c) == 0) {
            return;
        }
        float x0 = tx + x * sx;
        float y0 = ty + y * sy;
        float x1 = x0 + w * sx;
        float y1 = y0 + h * sy;
        if (culled(x0, y0, x1, y1)) {
            return;
        }
        syncClip();
        drawQuad(x0, y0, x1, y1, c, c, c, c);
    }

    @Override
    public final void fillRect(float x, float y, float w, float h, Brush brush) {
        if (brush instanceof Brush.Solid s) {
            fillRect(x, y, w, h, s.color());
            return;
        }
        if (brush == null || w <= 0f || h <= 0f || opacity <= 0f || brush.isInvisible()) {
            return;
        }
        brush = filtered(brush);
        float x0 = tx + x * sx;
        float y0 = ty + y * sy;
        float x1 = x0 + w * sx;
        float y1 = y0 + h * sy;
        if (culled(x0, y0, x1, y1)) {
            return;
        }
        if (brush instanceof Brush.Linear l && l.isVertexExact()) {
            syncClip();
            drawQuad(x0, y0, x1, y1, corner(brush, 0, 0, w, h), corner(brush, 1, 0, w, h),
                    corner(brush, 1, 1, w, h), corner(brush, 0, 1, w, h));
        } else if (supportsShapes()) {
            shape(x, y, w, h, Radii.ZERO, brush, 0f, 0f, 0f);
        } else {
            syncClip();
            tessellate(x0, y0, x1, y1, brush, w, h);
        }
    }

    @Override
    public final void fillRoundRect(float x, float y, float w, float h, Radii radii, Brush brush) {
        if (radii == null || radii.isZero()) {
            fillRect(x, y, w, h, brush);
            return;
        }
        if (brush == null || w <= 0f || h <= 0f || opacity <= 0f) {
            return;
        }
        shape(x, y, w, h, radii, filtered(brush), 0f, 0f, 0f);
    }

    @Override
    public final void strokeRoundRect(float x, float y, float w, float h, Radii radii, float width, int argb) {
        if (width <= 0f || w <= 0f || h <= 0f || opacity <= 0f) {
            return;
        }
        int c = filtered(argb);
        shape(x, y, w, h, radii == null ? Radii.ZERO : radii, c, c, c, c, null, width, 0f, 0f);
    }

    @Override
    public final void strokeRoundRect(float x, float y, float w, float h, Radii radii, float width, Brush brush) {
        if (brush instanceof Brush.Solid s) {
            strokeRoundRect(x, y, w, h, radii, width, s.color());
            return;
        }
        if (brush == null || width <= 0f || w <= 0f || h <= 0f || opacity <= 0f) {
            return;
        }
        shape(x, y, w, h, radii == null ? Radii.ZERO : radii, filtered(brush), width, 0f, 0f);
    }

    @Override
    public final void drawShadow(float x, float y, float w, float h, Radii radii, Shadow shadow) {
        if (shadow == null || shadow.isNone() || w <= 0f || h <= 0f || opacity <= 0f) {
            return;
        }
        float s = shadow.spread();
        Radii r = (radii == null ? Radii.ZERO : radii).expand(s);
        int c = filtered(shadow.color());
        shape(x + shadow.offsetX() - s, y + shadow.offsetY() - s, w + 2 * s, h + 2 * s, r, c, c, c, c, null, 0f,
                Math.max(shadow.blur(), 0.5f), shadow.blur());
    }

    @Override
    public final void drawArc(float cx, float cy, float radius, float width, float startDeg, float sweepDeg,
                              int from, int to) {
        if (radius <= 0f || width <= 0f || sweepDeg == 0f || opacity <= 0f) {
            return;
        }
        if (sweepDeg < 0f) {
            startDeg += sweepDeg;
            sweepDeg = -sweepDeg;
            int t = from;
            from = to;
            to = t;
        }
        Brush brush;
        if (sweepDeg >= 360f) {
            brush = new Brush.Conic(0.5f, 0.5f, startDeg, ColorStops.even(from, to), ColorSpace.OKLAB, false, 0f);
        } else {
            float f = sweepDeg / 360f;
            brush = new Brush.Conic(0.5f, 0.5f, startDeg, ColorStops.builder()
                    .add(0f, from).add(f, to).add(f, Color.withAlpha(to, 0)).add(1f, Color.withAlpha(to, 0)).build(),
                    ColorSpace.OKLAB, false, 0f);
        }
        float r = radius + width / 2f;
        strokeRoundRect(cx - r, cy - r, 2f * r, 2f * r, Radii.all(r), width, brush);
    }

    @Override
    public final void drawLine(float x0, float y0, float x1, float y1, float width, int argb) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len <= 0f || width <= 0f) {
            return;
        }
        float nx = -dy / len * width / 2f;
        float ny = dx / len * width / 2f;
        fillQuad(x0 + nx, y0 + ny, x1 + nx, y1 + ny, x1 - nx, y1 - ny, x0 - nx, y0 - ny, argb, argb, argb, argb);
    }

    @Override
    public final void fillTriangle(float x0, float y0, float x1, float y1, float x2, float y2, int argb) {
        fillQuad(x0, y0, x1, y1, x2, y2, x2, y2, argb, argb, argb, argb);
    }

    @Override
    public final void fillQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                               int c0, int c1, int c2, int c3) {
        if (opacity <= 0f) {
            return;
        }
        float ax = tx + x0 * sx;
        float ay = ty + y0 * sy;
        float bx = tx + x1 * sx;
        float by = ty + y1 * sy;
        float cx = tx + x2 * sx;
        float cy = ty + y2 * sy;
        float dx = tx + x3 * sx;
        float dy = ty + y3 * sy;
        if (culled(Math.min(Math.min(ax, bx), Math.min(cx, dx)), Math.min(Math.min(ay, by), Math.min(cy, dy)),
                Math.max(Math.max(ax, bx), Math.max(cx, dx)), Math.max(Math.max(ay, by), Math.max(cy, dy)))) {
            return;
        }
        c0 = Color.mulAlpha(filtered(c0), opacity);
        c1 = Color.mulAlpha(filtered(c1), opacity);
        c2 = Color.mulAlpha(filtered(c2), opacity);
        c3 = Color.mulAlpha(filtered(c3), opacity);
        syncClip();
        // Порядок обхода как у drawQuad (верх-лево, низ-лево, низ-право, верх-право), иначе грань может отсечься.
        float cross = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        if (cross <= 0f) {
            drawFreeQuad(ax, ay, bx, by, cx, cy, dx, dy, c0, c1, c2, c3);
        } else {
            drawFreeQuad(ax, ay, dx, dy, cx, cy, bx, by, c0, c3, c2, c1);
        }
    }

    private static int corner(Brush brush, float u, float v, float w, float h) {
        return brush.sample(u, v, w, h);
    }

    /** Запасной путь без шейдера: градиент сеткой квадов с цветами в узлах. */
    private void tessellate(float x0, float y0, float x1, float y1, Brush brush, float w, float h) {
        int n = FALLBACK_GRID;
        float cw = (x1 - x0) / n;
        float ch = (y1 - y0) / n;
        for (int j = 0; j < n; j++) {
            float v0 = j / (float) n;
            float v1 = (j + 1) / (float) n;
            for (int i = 0; i < n; i++) {
                float u0 = i / (float) n;
                float u1 = (i + 1) / (float) n;
                drawQuad(x0 + i * cw, y0 + j * ch, x0 + (i + 1) * cw, y0 + (j + 1) * ch,
                        Color.mulAlpha(brush.sample(u0, v0, w, h), opacity),
                        Color.mulAlpha(brush.sample(u1, v0, w, h), opacity),
                        Color.mulAlpha(brush.sample(u1, v1, w, h), opacity),
                        Color.mulAlpha(brush.sample(u0, v1, w, h), opacity));
            }
        }
    }

    private void shape(float x, float y, float w, float h, Radii r, Brush brush, float stroke, float softness, float margin) {
        if (brush instanceof Brush.Solid s) {
            int c = s.color();
            shape(x, y, w, h, r, c, c, c, c, null, stroke, softness, margin);
        } else if (brush instanceof Brush.Linear l && l.isVertexExact()) {
            shape(x, y, w, h, r, corner(brush, 0, 0, w, h), corner(brush, 1, 0, w, h),
                    corner(brush, 1, 1, w, h), corner(brush, 0, 1, w, h), null, stroke, softness, margin);
        } else if (!brush.isInvisible()) {
            if (!supportsShapes()) {
                shape(x, y, w, h, r, corner(brush, 0, 0, w, h), corner(brush, 1, 0, w, h),
                        corner(brush, 1, 1, w, h), corner(brush, 0, 1, w, h), null, stroke, softness, margin);
                return;
            }
            int white = Color.WHITE;
            shape(x, y, w, h, r, white, white, white, white, (Brush.Gradient) brush, stroke, softness, margin);
        }
    }

    private void shape(float x, float y, float w, float h, Radii r, int tl, int tr, int br, int bl,
                       Brush.Gradient gradient, float stroke, float softness, float margin) {
        float x0 = tx + x * sx;
        float y0 = ty + y * sy;
        float x1 = x0 + w * sx;
        float y1 = y0 + h * sy;
        float m = margin * sx;
        if (culled(x0 - m, y0 - m, x1 + m, y1 + m)) {
            return;
        }
        ShapePaint p = null;
        if (gradient != null) {
            p = paint;
            p.setGradient(gradient, x0, y0, x1, y1, opacity);
        } else {
            tl = Color.mulAlpha(tl, opacity);
            tr = Color.mulAlpha(tr, opacity);
            br = Color.mulAlpha(br, opacity);
            bl = Color.mulAlpha(bl, opacity);
            if (Color.alpha(tl) == 0 && Color.alpha(tr) == 0 && Color.alpha(br) == 0 && Color.alpha(bl) == 0) {
                return;
            }
        }
        float k = Math.min(sx, sy);
        float max = Math.min(x1 - x0, y1 - y0) * 0.5f;
        syncClip();
        drawShape(x0, y0, x1, y1,
                Math.min(r.topLeft() * k, max), Math.min(r.topRight() * k, max),
                Math.min(r.bottomRight() * k, max), Math.min(r.bottomLeft() * k, max),
                tl, tr, br, bl, stroke * k, softness * k, p);
    }

    @Override
    public final void drawText(String text, float x, float y, int argb, boolean shadow) {
        int c = Color.mulAlpha(filtered(argb), opacity);
        // Шрифт Minecraft считает альфу < 4 непрозрачной, поэтому почти прозрачный текст не рисуем вовсе.
        if (text == null || text.isEmpty() || Color.alpha(c) < 4) {
            return;
        }
        float x0 = tx + x * sx;
        float y0 = ty + y * sy;
        if (y0 >= clipY1 || x0 >= clipX1 || y0 + textMetrics().lineHeight() * sy <= clipY0) {
            return;
        }
        syncClip();
        drawTextImpl(text, x0, y0, sx, c, shadow);
    }

    @Override
    public final void drawTexture(TextureRef texture, float x, float y, float w, float h,
                                  float u0, float v0, float u1, float v1, int tint) {
        if (w <= 0f || h <= 0f) {
            return;
        }
        drawTextureQuad(texture, x, y, x, y + h, x + w, y + h, x + w, y,
                u0, v0, u0, v1, u1, v1, u1, v0, tint);
    }

    @Override
    public final void drawTextureQuad(TextureRef texture,
                                      float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                      float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                                      int tint) {
        int c = Color.mulAlpha(tint, opacity);
        if (texture == null || Color.alpha(c) == 0) {
            return;
        }
        float ax = tx + x0 * sx;
        float ay = ty + y0 * sy;
        float bx = tx + x1 * sx;
        float by = ty + y1 * sy;
        float cx = tx + x2 * sx;
        float cy = ty + y2 * sy;
        float dx = tx + x3 * sx;
        float dy = ty + y3 * sy;
        if (culled(Math.min(Math.min(ax, bx), Math.min(cx, dx)), Math.min(Math.min(ay, by), Math.min(cy, dy)),
                Math.max(Math.max(ax, bx), Math.max(cx, dx)), Math.max(Math.max(ay, by), Math.max(cy, dy)))) {
            return;
        }
        syncClip();
        float cross = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        if (cross <= 0f) {
            drawTextureQuadImpl(texture, ax, ay, bx, by, cx, cy, dx, dy, u0, v0, u1, v1, u2, v2, u3, v3, c);
        } else {
            drawTextureQuadImpl(texture, ax, ay, dx, dy, cx, cy, bx, by, u0, v0, u3, v3, u2, v2, u1, v1, c);
        }
    }

    @Override
    public final void drawItem(String itemId, float x, float y, float size) {
        if (itemId == null || size <= 0f || opacity <= 0f) {
            return;
        }
        float x0 = tx + x * sx;
        float y0 = ty + y * sy;
        float s = size * Math.min(sx, sy);
        if (culled(x0, y0, x0 + s, y0 + s)) {
            return;
        }
        syncClip();
        drawItemImpl(itemId, x0, y0, s);
    }

    @Override
    public final void drawItem(String itemId, float x, float y, float size, boolean glint) {
        if (!glint) {
            drawItem(itemId, x, y, size);
            return;
        }
        if (itemId == null || size <= 0f || opacity <= 0f) {
            return;
        }
        float x0 = tx + x * sx;
        float y0 = ty + y * sy;
        float s = size * Math.min(sx, sy);
        if (culled(x0, y0, x0 + s, y0 + s)) {
            return;
        }
        syncClip();
        drawItemImpl(itemId, x0, y0, s, true);
    }

    @Override
    public final void drawItem3d(String itemId, float x, float y, float size, float yaw, float pitch, boolean glint) {
        if (itemId == null || size <= 0f || opacity <= 0f) {
            return;
        }
        float x0 = tx + x * sx;
        float y0 = ty + y * sy;
        float s = size * Math.min(sx, sy);
        if (culled(x0, y0, x0 + s, y0 + s)) {
            return;
        }
        syncClip();
        drawItem3dImpl(itemId, x0, y0, s, yaw, pitch, glint);
    }

    @Override
    public final void drawEntity(String entityTypeId, float x, float y, float size, float yaw, float pitch,
                                 EntityOptions options) {
        if (entityTypeId == null || size <= 0f || opacity <= 0f) {
            return;
        }
        float x0 = tx + x * sx;
        float y0 = ty + y * sy;
        float s = size * Math.min(sx, sy);
        if (culled(x0, y0, x0 + s, y0 + s)) {
            return;
        }
        syncClip();
        drawEntityImpl(entityTypeId, x0, y0, s, yaw, pitch, options == null ? EntityOptions.DEFAULT : options);
    }

    // ---------------------------------------------------------------- для бэкенда

    /** Прямоугольник без скругления; цвета углов уже с учётом прозрачности. Координаты экранные. */
    protected abstract void drawQuad(float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl);

    /**
     * Произвольный четырёхугольник; вершины уже в экранных координатах и в том же порядке обхода,
     * что у {@link #drawQuad}. Может идти в тот же пакет, что и прямоугольники.
     */
    protected abstract void drawFreeQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                         int c0, int c1, int c2, int c3);

    /**
     * Скруглённая фигура через SDF. {@code stroke > 0} — только обводка такой толщины,
     * {@code softness > 0} — размытый край (для теней).
     *
     * @param paint {@code null} — цвета вершин; иначе градиент для шейдера (цвета вершин тогда белые)
     */
    protected abstract void drawShape(float x0, float y0, float x1, float y1,
                                      float rTL, float rTR, float rBR, float rBL,
                                      int tl, int tr, int br, int bl, float stroke, float softness, ShapePaint paint);

    /**
     * Умеет ли бэкенд рисовать {@link #drawShape} шейдером (а значит, и градиенты). Если нет,
     * градиенты рисуются сеткой цветов вершин.
     */
    protected boolean supportsShapes() {
        return true;
    }

    /** Сменить функцию смешивания; всё накопленное до этого нужно дорисовать по-старому. */
    protected abstract void applyBlendMode(BlendMode mode);

    protected abstract void drawTextImpl(String text, float x, float y, float scale, int argb, boolean shadow);

    /** Текстурированный четырёхугольник; вершины экранные, в порядке обхода как у {@link #drawQuad}. */
    protected abstract void drawTextureQuadImpl(TextureRef texture,
                                                float x0, float y0, float x1, float y1,
                                                float x2, float y2, float x3, float y3,
                                                float u0, float v0, float u1, float v1,
                                                float u2, float v2, float u3, float v3, int tint);

    /** Предмет размером {@code size} экранных пикселей с левым верхним углом в (x, y). */
    protected abstract void drawItemImpl(String itemId, float x, float y, float size);

    /** Предмет с переливом зачарования; по умолчанию — обычный предмет. */
    protected void drawItemImpl(String itemId, float x, float y, float size, boolean glint) {
        drawItemImpl(itemId, x, y, size);
    }

    /** Повёрнутый 3D-предмет в экранном квадрате; по умолчанию — плоская иконка без поворота. */
    protected void drawItem3dImpl(String itemId, float x, float y, float size, float yaw, float pitch, boolean glint) {
        drawItemImpl(itemId, x, y, size, glint);
    }

    /** Сущность в экранном квадрате (см. {@link Canvas#drawEntity}); по умолчанию ничего не рисует. */
    protected void drawEntityImpl(String entityTypeId, float x, float y, float size, float yaw, float pitch,
                                  EntityOptions options) {
    }

    /** Включить отсечение экранным прямоугольником (заменяет предыдущее). */
    protected abstract void applyClip(float x0, float y0, float x1, float y1);

    protected abstract void clearClip();
}
