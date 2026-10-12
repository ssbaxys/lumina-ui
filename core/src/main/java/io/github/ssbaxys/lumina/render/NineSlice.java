package io.github.ssbaxys.lumina.render;

/**
 * Текстура «9 частей»: углы рисуются как есть, края и середина растягиваются. Так одна маленькая
 * картинка рамки (ванильное окно, свиток, панель из ресурспака) подходит под панель любого размера.
 *
 * <pre>{@code
 * NineSlice frame = NineSlice.of(TextureRef.of("minecraft:textures/gui/demo_background.png"), 248, 166, 4);
 * panel.style(b -> b.set(Styles.NINE_SLICE, frame));
 * }</pre>
 *
 * @param texWidth  ширина картинки в пикселях
 * @param texHeight высота картинки в пикселях
 * @param left      ширина левой рамки в пикселях картинки (так же top, right, bottom)
 * @param scale     во сколько раз увеличивать рамку на экране (2 — пиксели вдвое крупнее)
 * @param tint      умножающий цвет ARGB
 */
public record NineSlice(TextureRef texture, int texWidth, int texHeight,
                        int left, int top, int right, int bottom, float scale, int tint) {

    public NineSlice {
        if (texWidth <= 0 || texHeight <= 0) {
            throw new IllegalArgumentException("texture size must be positive");
        }
        if (left + right > texWidth || top + bottom > texHeight) {
            throw new IllegalArgumentException("borders larger than the texture");
        }
    }

    /** Одинаковая рамка со всех сторон. */
    public static NineSlice of(TextureRef texture, int texWidth, int texHeight, int border) {
        return new NineSlice(texture, texWidth, texHeight, border, border, border, border, 1f, 0xFFFFFFFF);
    }

    public static NineSlice of(String textureId, int texWidth, int texHeight, int border) {
        return of(TextureRef.of(textureId), texWidth, texHeight, border);
    }

    public NineSlice scaled(float scale) {
        return new NineSlice(texture, texWidth, texHeight, left, top, right, bottom, scale, tint);
    }

    public NineSlice tinted(int argb) {
        return new NineSlice(texture, texWidth, texHeight, left, top, right, bottom, scale, argb);
    }

    /** Нарисовать в прямоугольнике; если он меньше рамки, рамка пропорционально сжимается. */
    public void draw(Canvas canvas, float x, float y, float w, float h) {
        if (w <= 0f || h <= 0f) {
            return;
        }
        float l = left * scale;
        float r = right * scale;
        float t = top * scale;
        float b = bottom * scale;
        if (l + r > w) {
            float k = w / (l + r);
            l *= k;
            r *= k;
        }
        if (t + b > h) {
            float k = h / (t + b);
            t *= k;
            b *= k;
        }
        float[] xs = {x, x + l, x + w - r, x + w};
        float[] ys = {y, y + t, y + h - b, y + h};
        float[] us = {0f, (float) left / texWidth, 1f - (float) right / texWidth, 1f};
        float[] vs = {0f, (float) top / texHeight, 1f - (float) bottom / texHeight, 1f};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                float cw = xs[col + 1] - xs[col];
                float ch = ys[row + 1] - ys[row];
                if (cw > 0f && ch > 0f) {
                    canvas.drawTexture(texture, xs[col], ys[row], cw, ch, us[col], vs[row], us[col + 1], vs[row + 1], tint);
                }
            }
        }
    }
}
