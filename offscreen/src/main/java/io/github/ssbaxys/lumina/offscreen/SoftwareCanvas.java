package io.github.ssbaxys.lumina.offscreen;

import io.github.ssbaxys.lumina.render.AbstractCanvas;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.ShapePaint;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Бэкенд {@link io.github.ssbaxys.lumina.render.Canvas} без видеокарты и без игры: рисует кадр в память
 * и сохраняет PNG с прозрачностью. Фигуры считаются по той же SDF-формуле и с теми же градиентами,
 * что шейдер {@code rounded_rect.fsh}, текст — пиксельным шрифтом игры ({@link BitmapFont}).
 *
 * <p>Кадр рисуется с запасом {@code supersample} и сглаживается при сохранении. Нужен для картинок
 * в описание мода, тестов «по скриншоту» и превью тем без запуска Minecraft.</p>
 */
public final class SoftwareCanvas extends AbstractCanvas {
    /** Текстура-копия кадра (размытие фона и маски скруглённого клипа). Цвета предумножены. */
    record FrameTexture(String id, int width, int height, float[] rgba, boolean replace) implements TextureRef {
    }

    private final Assets assets;
    private final BitmapFont font;
    private final Map<String, Assets.Pixels> created = new HashMap<>();
    private final Map<String, Boolean> itemFlat = new HashMap<>();

    private float guiWidth;
    private float guiHeight;
    /** Физических пикселей буфера на один GUI-пиксель. */
    private float k;
    private int supersample;
    private int bw;
    private int bh;
    private float[] buf;

    private BlendMode mode = BlendMode.NORMAL;
    private int cx0;
    private int cy0;
    private int cx1;
    private int cy1;
    private int frameSerial;

    public SoftwareCanvas(Assets assets, BitmapFont font) {
        this.assets = assets;
        this.font = font;
    }

    /** Текстура, загруженная платформой ({@code Platform.createTexture}). */
    public void registerTexture(String id, int width, int height, int[] argb) {
        created.put(id, new Assets.Pixels(width, height, argb.clone()));
    }

    /**
     * Начать кадр {@code width × height} GUI-пикселей; на выходе будет {@code width·scale × height·scale}
     * пикселей PNG.
     */
    public void begin(float width, float height, float scale, int supersample) {
        this.guiWidth = width;
        this.guiHeight = height;
        this.supersample = Math.max(1, supersample);
        this.k = scale * this.supersample;
        this.bw = Math.round(width * k);
        this.bh = Math.round(height * k);
        if (buf == null || buf.length != bw * bh * 4) {
            buf = new float[bw * bh * 4];
        } else {
            java.util.Arrays.fill(buf, 0f);
        }
        mode = BlendMode.NORMAL;
        clearClip();
        beginFrame(width, height);
    }

    public void end() {
        endFrame();
    }

    /** Итог кадра: сглаженный ARGB без предумножения. */
    public BufferedImage image() {
        int s = supersample;
        int ow = bw / s;
        int oh = bh / s;
        BufferedImage img = new BufferedImage(ow, oh, BufferedImage.TYPE_INT_ARGB);
        float inv = 1f / (s * s);
        for (int y = 0; y < oh; y++) {
            for (int x = 0; x < ow; x++) {
                float r = 0, g = 0, b = 0, a = 0;
                for (int j = 0; j < s; j++) {
                    int row = ((y * s + j) * bw + x * s) * 4;
                    for (int i = 0; i < s; i++) {
                        int o = row + i * 4;
                        r += buf[o];
                        g += buf[o + 1];
                        b += buf[o + 2];
                        a += buf[o + 3];
                    }
                }
                a *= inv;
                int argb = 0;
                if (a > 0.0005f) {
                    float ia = inv / a;
                    argb = (clamp255(a) << 24) | (clamp255(r * ia) << 16) | (clamp255(g * ia) << 8) | clamp255(b * ia);
                }
                img.setRGB(x, y, argb);
            }
        }
        return img;
    }

    public void save(Path file) throws IOException {
        java.nio.file.Files.createDirectories(file.toAbsolutePath().getParent());
        ImageIO.write(image(), "png", file.toFile());
    }

    private static int clamp255(float v) {
        int i = Math.round(v * 255f);
        return i < 0 ? 0 : Math.min(i, 255);
    }

    @Override
    public TextMetrics textMetrics() {
        return font;
    }

    // ================================================================ смешивание пикселя

    /** Смешать пиксель: цвет без предумножения, {@code a} — итоговая непрозрачность. */
    private void blend(int px, int py, float r, float g, float b, float a) {
        if (a <= 0f) {
            return;
        }
        if (a > 1f) {
            a = 1f;
        }
        int o = (py * bw + px) * 4;
        float[] d = buf;
        float sr = r * a;
        float sg = g * a;
        float sb = b * a;
        switch (mode) {
            case ADD -> {
                d[o] = Math.min(d[o] + sr, 1f);
                d[o + 1] = Math.min(d[o + 1] + sg, 1f);
                d[o + 2] = Math.min(d[o + 2] + sb, 1f);
                d[o + 3] = Math.min(d[o + 3] + a * (1f - d[o + 3]) + Math.max(Math.max(sr, sg), sb) * 0f, 1f);
                float m = Math.max(Math.max(d[o], d[o + 1]), d[o + 2]);
                if (m > d[o + 3]) {
                    d[o + 3] = m;
                }
            }
            case MULTIPLY -> {
                float da = d[o + 3];
                d[o] = sr * d[o] + sr * (1f - da) + d[o] * (1f - a);
                d[o + 1] = sg * d[o + 1] + sg * (1f - da) + d[o + 1] * (1f - a);
                d[o + 2] = sb * d[o + 2] + sb * (1f - da) + d[o + 2] * (1f - a);
                d[o + 3] = a + da * (1f - a);
            }
            case SCREEN -> {
                d[o] = sr + d[o] - sr * d[o];
                d[o + 1] = sg + d[o + 1] - sg * d[o + 1];
                d[o + 2] = sb + d[o + 2] - sb * d[o + 2];
                d[o + 3] = a + d[o + 3] - a * d[o + 3];
            }
            default -> {
                float ia = 1f - a;
                d[o] = sr + d[o] * ia;
                d[o + 1] = sg + d[o + 1] * ia;
                d[o + 2] = sb + d[o + 2] * ia;
                d[o + 3] = a + d[o + 3] * ia;
            }
        }
    }

    /** То же для предумноженного цвета (копии кадра). */
    private void blendPremul(int px, int py, float pr, float pg, float pb, float pa, float cover, boolean replace) {
        int o = (py * bw + px) * 4;
        float[] d = buf;
        if (replace) {
            d[o] += (pr - d[o]) * cover;
            d[o + 1] += (pg - d[o + 1]) * cover;
            d[o + 2] += (pb - d[o + 2]) * cover;
            d[o + 3] += (pa - d[o + 3]) * cover;
            return;
        }
        pr *= cover;
        pg *= cover;
        pb *= cover;
        pa *= cover;
        float ia = 1f - pa;
        d[o] = pr + d[o] * ia;
        d[o + 1] = pg + d[o + 1] * ia;
        d[o + 2] = pb + d[o + 2] * ia;
        d[o + 3] = pa + d[o + 3] * ia;
    }

    // ================================================================ клип и смешивание

    @Override
    protected void applyClip(float x0, float y0, float x1, float y1) {
        cx0 = Math.max(0, Math.round(x0 * k));
        cy0 = Math.max(0, Math.round(y0 * k));
        cx1 = Math.min(bw, Math.round(x1 * k));
        cy1 = Math.min(bh, Math.round(y1 * k));
    }

    @Override
    protected void clearClip() {
        cx0 = 0;
        cy0 = 0;
        cx1 = bw;
        cy1 = bh;
    }

    @Override
    protected void applyBlendMode(BlendMode m) {
        mode = m;
    }

    // ================================================================ прямоугольники

    @Override
    protected void drawQuad(float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl) {
        float X0 = x0 * k;
        float Y0 = y0 * k;
        float X1 = x1 * k;
        float Y1 = y1 * k;
        int ix0 = Math.max(cx0, (int) Math.ceil(X0 - 0.5f));
        int iy0 = Math.max(cy0, (int) Math.ceil(Y0 - 0.5f));
        int ix1 = Math.min(cx1, (int) Math.ceil(X1 - 0.5f));
        int iy1 = Math.min(cy1, (int) Math.ceil(Y1 - 0.5f));
        if (ix0 >= ix1 || iy0 >= iy1) {
            return;
        }
        boolean flat = tl == tr && tr == br && br == bl;
        float w = Math.max(X1 - X0, 1e-4f);
        float h = Math.max(Y1 - Y0, 1e-4f);
        float[] c = new float[4];
        for (int y = iy0; y < iy1; y++) {
            float v = (y + 0.5f - Y0) / h;
            for (int x = ix0; x < ix1; x++) {
                if (flat) {
                    unpack(tl, c);
                } else {
                    bilerp(tl, tr, br, bl, (x + 0.5f - X0) / w, v, c);
                }
                blend(x, y, c[0], c[1], c[2], c[3]);
            }
        }
    }

    private static void unpack(int argb, float[] out) {
        out[0] = ((argb >> 16) & 255) / 255f;
        out[1] = ((argb >> 8) & 255) / 255f;
        out[2] = (argb & 255) / 255f;
        out[3] = (argb >>> 24) / 255f;
    }

    private static void bilerp(int tl, int tr, int br, int bl, float u, float v, float[] out) {
        u = u < 0 ? 0 : Math.min(u, 1);
        v = v < 0 ? 0 : Math.min(v, 1);
        for (int ch = 0; ch < 4; ch++) {
            int sh = ch == 3 ? 24 : 16 - ch * 8;
            float a = ((tl >>> sh) & 255);
            float b = ((tr >>> sh) & 255);
            float c = ((br >>> sh) & 255);
            float d = ((bl >>> sh) & 255);
            float top = a + (b - a) * u;
            float bot = d + (c - d) * u;
            out[ch] = (top + (bot - top) * v) / 255f;
        }
    }

    // ================================================================ выпуклые многоугольники

    /** Закраска пикселя внутри четырёхугольника по интерполированным атрибутам вершин. */
    private interface Shader {
        void shade(int px, int py, float[] attr);
    }

    /**
     * Выпуклый четырёхугольник (вершины уже в пикселях буфера), разбитый на два треугольника, как в GL:
     * пиксель закрашивается, если его центр внутри. {@code attrs} — по {@code n} чисел на вершину.
     */
    private void rasterQuad(float[] xs, float[] ys, float[] attrs, int n, Shader shader) {
        float[] out = new float[n];
        rasterTri(xs, ys, attrs, n, 0, 1, 2, shader, out);
        rasterTri(xs, ys, attrs, n, 0, 2, 3, shader, out);
    }

    private void rasterTri(float[] xs, float[] ys, float[] attrs, int n, int a, int b, int c, Shader shader,
                           float[] out) {
        float ax = xs[a], ay = ys[a], bx = xs[b], by = ys[b], qx = xs[c], qy = ys[c];
        float area = (bx - ax) * (qy - ay) - (by - ay) * (qx - ax);
        if (Math.abs(area) < 1e-6f) {
            return;
        }
        int ix0 = Math.max(cx0, (int) Math.floor(Math.min(ax, Math.min(bx, qx))));
        int iy0 = Math.max(cy0, (int) Math.floor(Math.min(ay, Math.min(by, qy))));
        int ix1 = Math.min(cx1, (int) Math.ceil(Math.max(ax, Math.max(bx, qx))) + 1);
        int iy1 = Math.min(cy1, (int) Math.ceil(Math.max(ay, Math.max(by, qy))) + 1);
        float inv = 1f / area;
        for (int y = iy0; y < iy1; y++) {
            float py = y + 0.5f;
            for (int x = ix0; x < ix1; x++) {
                float px = x + 0.5f;
                float w0 = ((bx - px) * (qy - py) - (by - py) * (qx - px)) * inv;
                float w1 = ((qx - px) * (ay - py) - (qy - py) * (ax - px)) * inv;
                float w2 = 1f - w0 - w1;
                // правило «верх-лево» упрощено: ребро, общее с соседним треугольником, берёт только один
                if (w0 < 0f || w1 < 0f || w2 < 0f || (w1 == 0f && c == 3)) {
                    continue;
                }
                for (int i = 0; i < n; i++) {
                    out[i] = w0 * attrs[a * n + i] + w1 * attrs[b * n + i] + w2 * attrs[c * n + i];
                }
                shader.shade(x, y, out);
            }
        }
    }

    @Override
    protected void drawFreeQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                int c0, int c1, int c2, int c3) {
        float[] xs = {x0 * k, x1 * k, x2 * k, x3 * k};
        float[] ys = {y0 * k, y1 * k, y2 * k, y3 * k};
        float[] at = new float[16];
        int[] cs = {c0, c1, c2, c3};
        float[] tmp = new float[4];
        for (int i = 0; i < 4; i++) {
            unpack(cs[i], tmp);
            System.arraycopy(tmp, 0, at, i * 4, 4);
        }
        rasterQuad(xs, ys, at, 4, (px, py, a) -> blend(px, py, a[0], a[1], a[2], a[3]));
    }

    // ================================================================ SDF-фигуры (как rounded_rect.fsh)

    @Override
    protected void drawShape(float x0, float y0, float x1, float y1, float rTL, float rTR, float rBR, float rBL,
                             int tl, int tr, int br, int bl, float stroke, float softness, ShapePaint paint) {
        float hw = (x1 - x0) * 0.5f;
        float hh = (y1 - y0) * 0.5f;
        float cxs = (x0 + x1) * 0.5f;
        float cys = (y0 + y1) * 0.5f;
        float pad = softness + 1f;
        float qx0 = x0 - pad, qy0 = y0 - pad, qx1 = x1 + pad, qy1 = y1 + pad;
        int ix0 = Math.max(cx0, (int) Math.floor(qx0 * k));
        int iy0 = Math.max(cy0, (int) Math.floor(qy0 * k));
        int ix1 = Math.min(cx1, (int) Math.ceil(qx1 * k));
        int iy1 = Math.min(cy1, (int) Math.ceil(qy1 * k));
        if (ix0 >= ix1 || iy0 >= iy1) {
            return;
        }
        boolean grad = paint != null && paint.kind() != ShapePaint.VERTEX;
        Gradient g = grad ? new Gradient(paint) : null;
        float qw = qx1 - qx0;
        float qh = qy1 - qy0;
        boolean flat = tl == tr && tr == br && br == bl;
        float[] c = new float[4];
        float[] gc = new float[4];
        float aa = 1f / k;
        for (int y = iy0; y < iy1; y++) {
            float gy = (y + 0.5f) / k;
            float py = gy - cys;
            for (int x = ix0; x < ix1; x++) {
                float gx = (x + 0.5f) / k;
                float px = gx - cxs;
                float d = sdf(px, py, hw, hh, rBR, rTR, rBL, rTL);
                if (stroke > 0f) {
                    d = Math.abs(d + stroke * 0.5f) - stroke * 0.5f;
                }
                float alpha;
                if (softness > 0f) {
                    alpha = 1f - smoothstep(-softness, softness, d);
                } else {
                    alpha = clamp01(0.5f - d / aa);
                }
                if (alpha <= 0f) {
                    continue;
                }
                if (flat) {
                    unpack(tl, c);
                } else {
                    bilerp(tl, tr, br, bl, (gx - qx0) / qw, (gy - qy0) / qh, c);
                }
                if (g != null) {
                    g.eval(px, py, gc);
                    c[0] *= gc[0];
                    c[1] *= gc[1];
                    c[2] *= gc[2];
                    c[3] *= gc[3];
                }
                if (c[3] * alpha < 0.002f) {
                    continue;
                }
                blend(x, y, c[0], c[1], c[2], c[3] * alpha);
            }
        }
    }

    private static float sdf(float px, float py, float bx, float by, float rBR, float rTR, float rBL, float rTL) {
        float r0 = px > 0f ? rBR : rBL;
        float r1 = px > 0f ? rTR : rTL;
        float r = py > 0f ? r0 : r1;
        float qx = Math.abs(px) - bx + r;
        float qy = Math.abs(py) - by + r;
        float mx = Math.max(qx, 0f);
        float my = Math.max(qy, 0f);
        return Math.min(Math.max(qx, qy), 0f) + (float) Math.sqrt(mx * mx + my * my) - r;
    }

    private static float smoothstep(float e0, float e1, float x) {
        float t = clamp01((x - e0) / (e1 - e0));
        return t * t * (3f - 2f * t);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }

    /** Градиент шейдера, перенесённый на CPU один в один. */
    private static final class Gradient {
        final int kind;
        final int n;
        final int space;
        final boolean repeating;
        final float offset;
        final float g0, g1, g2, g3;
        final float[][] c = new float[8][4];
        final float[] s = new float[8];

        Gradient(ShapePaint p) {
            kind = p.kind();
            n = p.stopCount();
            space = p.space();
            repeating = p.repeating();
            offset = p.offset();
            g0 = p.geometry(0);
            g1 = p.geometry(1);
            g2 = p.geometry(2);
            g3 = p.geometry(3);
            for (int i = 0; i < 8; i++) {
                for (int ch = 0; ch < 4; ch++) {
                    c[i][ch] = p.color(i, ch);
                }
                s[i] = p.position(i);
            }
        }

        void eval(float px, float py, float[] out) {
            float t;
            if (kind == 1) {
                t = px * g0 + py * g1 + g2;
            } else if (kind == 2) {
                float dx = (px - g0) * g2;
                float dy = (py - g1) * g3;
                t = (float) Math.sqrt(dx * dx + dy * dy);
            } else {
                float dx = px - g0;
                float dy = py - g1;
                float a = (float) Math.atan2(dx, -dy) - g2;
                float f = (float) (a / (2 * Math.PI));
                t = f - (float) Math.floor(f);
            }
            t += offset;
            if (repeating) {
                float first = s[0];
                float period = s[n - 1] - first;
                if (period > 1e-5f) {
                    float m = (t - first) / period;
                    t = first + (m - (float) Math.floor(m)) * period;
                }
            }
            if (t <= s[0]) {
                System.arraycopy(c[0], 0, out, 0, 4);
                return;
            }
            for (int i = 1; i < 8 && i < n; i++) {
                if (t <= s[i]) {
                    float span = s[i] - s[i - 1];
                    float f = span <= 1e-5f ? 1f : (t - s[i - 1]) / span;
                    mixStops(c[i - 1], c[i], f, space, out);
                    return;
                }
            }
            System.arraycopy(c[n - 1], 0, out, 0, 4);
        }
    }

    // ---- цветовые пространства (rounded_rect.fsh)

    private static float toLin(float c) {
        return c < 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
    }

    private static float toSrgb(float c) {
        c = Math.max(c, 0f);
        return c < 0.0031308f ? c * 12.92f : 1.055f * (float) Math.pow(c, 1.0 / 2.4) - 0.055f;
    }

    private static float[] linToOklab(float r, float g, float b) {
        float l = 0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b;
        float m = 0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b;
        float s = 0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b;
        l = (float) Math.cbrt(l);
        m = (float) Math.cbrt(m);
        s = (float) Math.cbrt(s);
        return new float[]{
                0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
                1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
                0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s};
    }

    private static float[] oklabToLin(float L, float A, float B) {
        float l = L + 0.3963377774f * A + 0.2158037573f * B;
        float m = L - 0.1055613458f * A - 0.0638541728f * B;
        float s = L - 0.0894841775f * A - 1.2914855480f * B;
        l = l * l * l;
        m = m * m * m;
        s = s * s * s;
        return new float[]{
                4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s,
                -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s,
                -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s};
    }

    private static float[] rgbToHsv(float r, float g, float b) {
        float mx = Math.max(r, Math.max(g, b));
        float mn = Math.min(r, Math.min(g, b));
        float d = mx - mn;
        float h = 0f;
        if (d > 0f) {
            if (mx == r) {
                h = (g - b) / d;
                h = h - 6f * (float) Math.floor(h / 6f);
            } else if (mx == g) {
                h = (b - r) / d + 2f;
            } else {
                h = (r - g) / d + 4f;
            }
        }
        return new float[]{h * 60f, mx > 0f ? d / mx : 0f, mx};
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        float[] o = new float[3];
        float[] base = {5f, 3f, 1f};
        for (int i = 0; i < 3; i++) {
            float kk = base[i] + h / 60f;
            kk = kk - 6f * (float) Math.floor(kk / 6f);
            o[i] = v - v * s * clamp01(Math.min(kk, 4f - kk));
        }
        return o;
    }

    private static float[] toSpace(float[] c, int space) {
        if (space == 1) {
            return new float[]{toLin(c[0]), toLin(c[1]), toLin(c[2])};
        }
        if (space == 2) {
            return linToOklab(toLin(c[0]), toLin(c[1]), toLin(c[2]));
        }
        if (space == 3) {
            float[] lab = linToOklab(toLin(c[0]), toLin(c[1]), toLin(c[2]));
            float h = (float) Math.toDegrees(Math.atan2(lab[2], lab[1]));
            return new float[]{lab[0], (float) Math.hypot(lab[1], lab[2]), h < 0 ? h + 360f : h};
        }
        if (space == 4) {
            return rgbToHsv(c[0], c[1], c[2]);
        }
        return new float[]{c[0], c[1], c[2]};
    }

    private static float[] fromSpace(float[] c, int space) {
        if (space == 1) {
            return new float[]{toSrgb(c[0]), toSrgb(c[1]), toSrgb(c[2])};
        }
        if (space == 2) {
            float[] l = oklabToLin(c[0], c[1], c[2]);
            return new float[]{toSrgb(l[0]), toSrgb(l[1]), toSrgb(l[2])};
        }
        if (space == 3) {
            double h = Math.toRadians(c[2]);
            float[] l = oklabToLin(c[0], c[1] * (float) Math.cos(h), c[1] * (float) Math.sin(h));
            return new float[]{toSrgb(l[0]), toSrgb(l[1]), toSrgb(l[2])};
        }
        if (space == 4) {
            float h = c[0] - 360f * (float) Math.floor(c[0] / 360f);
            return hsvToRgb(h, c[1], c[2]);
        }
        return c;
    }

    private static void mixStops(float[] a, float[] b, float t, int space, float[] out) {
        float alpha = a[3] + (b[3] - a[3]) * t;
        if (alpha <= 0f) {
            out[0] = out[1] = out[2] = out[3] = 0f;
            return;
        }
        float[] x = toSpace(a, space);
        float[] y = toSpace(b, space);
        float[] o = new float[3];
        for (int i = 0; i < 3; i++) {
            o[i] = (x[i] * a[3] + (y[i] * b[3] - x[i] * a[3]) * t) / alpha;
        }
        if (space == 3 || space == 4) {
            int hi = space == 3 ? 2 : 0;
            float chromaA = space == 3 ? x[1] : x[1] * x[2];
            float chromaB = space == 3 ? y[1] : y[1] * y[2];
            float ha = (chromaA < 1e-4f || a[3] == 0f) ? y[hi] : x[hi];
            float hb = (chromaB < 1e-4f || b[3] == 0f) ? x[hi] : y[hi];
            float dh = hb - ha;
            dh -= 360f * (float) Math.floor((dh + 180f) / 360f);
            o[hi] = ha + dh * t;
        }
        float[] rgb = fromSpace(o, space);
        out[0] = clamp01(rgb[0]);
        out[1] = clamp01(rgb[1]);
        out[2] = clamp01(rgb[2]);
        out[3] = alpha;
    }

    // ================================================================ текст

    @Override
    protected void drawTextImpl(String text, float x, float y, float scale, int argb, boolean shadow) {
        if ((argb & 0xFC000000) == 0) {
            argb |= 0xFF000000;
        }
        if (shadow) {
            int sc = ((argb & 0xFCFCFC) >> 2) | (argb & 0xFF000000);
            drawTextRun(text, x + scale, y + scale, scale, sc, true);
        }
        drawTextRun(text, x, y, scale, argb, false);
    }

    private void drawTextRun(String text, float x, float y, float scale, int base, boolean isShadow) {
        int color = base;
        boolean bold = false;
        boolean underline = false;
        boolean strike = false;
        boolean italic = false;
        float pen = x;
        int alpha = base & 0xFF000000;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '§' && i < text.length()) {
                char f = Character.toLowerCase(text.charAt(i++));
                int idx = "0123456789abcdef".indexOf(f);
                if (idx >= 0) {
                    int rgb = BitmapFont.COLORS[idx];
                    if (isShadow) {
                        rgb = (rgb & 0xFCFCFC) >> 2;
                    }
                    color = alpha | rgb;
                    bold = underline = strike = italic = false;
                } else if (f == 'l') {
                    bold = true;
                } else if (f == 'n') {
                    underline = true;
                } else if (f == 'm') {
                    strike = true;
                } else if (f == 'o') {
                    italic = true;
                } else if (f == 'r') {
                    color = base;
                    bold = underline = strike = italic = false;
                }
                continue;
            }
            float adv = font.advance(cp, bold) * scale;
            if (cp != ' ') {
                BitmapFont.Glyph g = font.glyph(cp);
                if (g != null) {
                    drawGlyph(g, pen, y, scale, color, italic);
                    if (bold) {
                        drawGlyph(g, pen + scale, y, scale, color, italic);
                    }
                }
            }
            if (underline) {
                drawQuad(pen - scale, y + 9 * scale, pen + adv, y + 10 * scale, color, color, color, color);
            }
            if (strike) {
                drawQuad(pen - scale, y + 3.5f * scale, pen + adv, y + 4.5f * scale, color, color, color, color);
            }
            pen += adv;
        }
    }

    private void drawGlyph(BitmapFont.Glyph g, float x, float y, float scale, int color, boolean italic) {
        float px = g.scale() * scale;
        float top = y + g.up() * scale;
        for (int gy = 0; gy < g.cellH(); gy++) {
            float skew = italic ? (1f - (gy + 0.5f) / g.cellH()) * 2f * scale : 0f;
            int run = -1;
            for (int gx = 0; gx <= g.cellW(); gx++) {
                boolean on = gx < g.cellW() && g.mask()[gy * g.cellW() + gx];
                if (on && run < 0) {
                    run = gx;
                } else if (!on && run >= 0) {
                    drawQuad(x + run * px + skew, top + gy * px, x + gx * px + skew, top + (gy + 1) * px,
                            color, color, color, color);
                    run = -1;
                }
            }
        }
    }

    // ================================================================ текстуры

    @Override
    protected void drawTextureQuadImpl(TextureRef texture, float x0, float y0, float x1, float y1,
                                       float x2, float y2, float x3, float y3,
                                       float u0, float v0, float u1, float v1,
                                       float u2, float v2, float u3, float v3, int tint) {
        float[] xs = {x0 * k, x1 * k, x2 * k, x3 * k};
        float[] ys = {y0 * k, y1 * k, y2 * k, y3 * k};
        float[] at = {u0, v0, u1, v1, u2, v2, u3, v3};
        float tr = ((tint >> 16) & 255) / 255f;
        float tg = ((tint >> 8) & 255) / 255f;
        float tb = (tint & 255) / 255f;
        float ta = (tint >>> 24) / 255f;
        if (texture instanceof FrameTexture f) {
            rasterQuad(xs, ys, at, 2, (px, py, a) -> {
                int sx = clampi((int) (a[0] * f.width()), f.width());
                int sy = clampi((int) (a[1] * f.height()), f.height());
                int o = (sy * f.width() + sx) * 4;
                float[] p = f.rgba();
                if (f.replace()) {
                    blendPremul(px, py, p[o], p[o + 1], p[o + 2], p[o + 3], 1f, true);
                } else {
                    blendPremul(px, py, p[o] * tr * ta, p[o + 1] * tg * ta, p[o + 2] * tb * ta, p[o + 3] * ta, 1f, false);
                }
            });
            return;
        }
        Assets.Pixels img = pixels(texture);
        if (img == null) {
            return;
        }
        rasterQuad(xs, ys, at, 2, (px, py, a) -> {
            float u = a[0] - (float) Math.floor(a[0]);
            float v = a[1] - (float) Math.floor(a[1]);
            int c = img.at(clampi((int) (u * img.width()), img.width()), clampi((int) (v * img.height()), img.height()));
            float ca = (c >>> 24) / 255f * ta;
            if (ca <= 0f) {
                return;
            }
            blend(px, py, ((c >> 16) & 255) / 255f * tr, ((c >> 8) & 255) / 255f * tg, (c & 255) / 255f * tb, ca);
        });
    }

    private static int clampi(int v, int size) {
        return v < 0 ? 0 : Math.min(v, size - 1);
    }

    private Assets.Pixels pixels(TextureRef t) {
        Assets.Pixels p = created.get(t.id());
        return p != null ? p : assets.image(t.id());
    }

    // ================================================================ копии кадра

    @Override
    protected TextureRef captureSnapshot(int slot) {
        return new FrameTexture("lumina:frame/snap" + (frameSerial++), bw, bh, buf.clone(), true);
    }

    @Override
    protected int snapshotSlots() {
        return 8;
    }

    @Override
    protected boolean backdropBlurAvailable() {
        return true;
    }

    @Override
    protected TextureRef captureBackdrop(float radius) {
        float[] a = buf.clone();
        float[] tmp = new float[a.length];
        int r = Math.max(1, Math.round(radius * k / 2f));
        // три прохода коробочного размытия ≈ гауссово
        for (int pass = 0; pass < 3; pass++) {
            boxBlur(a, tmp, bw, bh, r, true);
            boxBlur(tmp, a, bw, bh, r, false);
        }
        return new FrameTexture("lumina:frame/blur" + (frameSerial++), bw, bh, a, false);
    }

    private static void boxBlur(float[] src, float[] dst, int w, int h, int r, boolean horizontal) {
        int len = horizontal ? w : h;
        int lines = horizontal ? h : w;
        float norm = 1f / (2 * r + 1);
        float[] acc = new float[4];
        for (int line = 0; line < lines; line++) {
            acc[0] = acc[1] = acc[2] = acc[3] = 0f;
            for (int i = -r; i <= r; i++) {
                int o = index(line, clampi(i, len), horizontal, w) * 4;
                for (int ch = 0; ch < 4; ch++) {
                    acc[ch] += src[o + ch];
                }
            }
            for (int i = 0; i < len; i++) {
                int o = index(line, i, horizontal, w) * 4;
                for (int ch = 0; ch < 4; ch++) {
                    dst[o + ch] = acc[ch] * norm;
                }
                int out = index(line, clampi(i - r, len), horizontal, w) * 4;
                int in = index(line, clampi(i + r + 1, len), horizontal, w) * 4;
                for (int ch = 0; ch < 4; ch++) {
                    acc[ch] += src[in + ch] - src[out + ch];
                }
            }
        }
    }

    private static int index(int line, int i, boolean horizontal, int w) {
        return horizontal ? line * w + i : i * w + line;
    }

    // ================================================================ предметы

    @Override
    protected void drawItemImpl(String itemId, float x, float y, float size) {
        drawItemImpl(itemId, x, y, size, false);
    }

    @Override
    protected void drawItemImpl(String itemId, float x, float y, float size, boolean glint) {
        String id = itemId.contains(":") ? itemId : "minecraft:" + itemId;
        String name = id.substring(id.indexOf(':') + 1);
        Map<String, Object> model = assets.model(id.replace(":", ":item/"));
        List<String> parents = Assets.parents(model);
        boolean block = false;
        for (String p : parents) {
            if (p.contains(":block/")) {
                block = true;
                break;
            }
        }
        if (!block) {
            String layer0 = Assets.resolveTexture(model, "#layer0");
            Assets.Pixels px = assets.sprite(layer0 != null ? layer0 : "minecraft:item/" + name);
            if (px == null) {
                return;
            }
            String key = "lumina:sprite/" + (layer0 != null ? layer0 : name);
            created.putIfAbsent(key, px);
            TextureRef ref = TextureRef.of(key);
            int tint = 0xFFFFFFFF;
            drawTextureQuadImpl(ref, x, y, x, y + size, x + size, y + size, x + size, y, 0, 0, 0, 1, 1, 1, 1, 0, tint);
            String layer1 = Assets.resolveTexture(model, "#layer1");
            if (layer1 != null && assets.sprite(layer1) != null) {
                String k1 = "lumina:sprite/" + layer1;
                created.putIfAbsent(k1, assets.sprite(layer1));
                drawTextureQuadImpl(TextureRef.of(k1), x, y, x, y + size, x + size, y + size, x + size, y,
                        0, 0, 0, 1, 1, 1, 1, 0, tint);
            }
            if (glint) {
                drawGlint(x, y, size, px);
            }
            return;
        }
        drawBlockIcon(model, x, y, size);
    }

    private void drawGlint(float x, float y, float size, Assets.Pixels mask) {
        BlendMode prev = mode;
        mode = BlendMode.ADD;
        int n = mask.width();
        float cell = size / n;
        for (int gy = 0; gy < n; gy++) {
            for (int gx = 0; gx < n; gx++) {
                if ((mask.at(gx, gy) >>> 24) == 0) {
                    continue;
                }
                float t = (gx + gy) / (float) (2 * n);
                float wave = (float) Math.max(0, Math.sin(t * Math.PI * 3));
                int a = (int) (0x50 * wave + 0x18);
                int c = (a << 24) | 0x8040CC;
                drawQuad(x + gx * cell, y + gy * cell, x + (gx + 1) * cell, y + (gy + 1) * cell, c, c, c, c);
            }
        }
        mode = prev;
    }

    /** Блок в изометрии, как в слоте инвентаря: верх, левая и правая грани с разной освещённостью. */
    private void drawBlockIcon(Map<String, Object> model, float x, float y, float size) {
        String top = firstTexture(model, "#up", "#top", "#end", "#all", "#texture", "#particle");
        String left = firstTexture(model, "#south", "#front", "#side", "#all", "#texture", "#particle");
        String right = firstTexture(model, "#east", "#side", "#all", "#texture", "#particle");
        String overlay = Assets.resolveTexture(model, "#overlay");
        float cxp = x + size / 2f;
        float hw = size * 0.40f;
        float dh = hw * 0.5f;
        float side = hw * 1.22f;
        float top0 = y + (size - (2 * dh + side)) / 2f;
        // вершины: верх, лево, низ (центр), право верхней грани
        float tX = cxp, tY = top0;
        float lX = cxp - hw, lY = top0 + dh;
        float cX = cxp, cY = top0 + 2 * dh;
        float rX = cxp + hw, rY = top0 + dh;
        face(top, tint(top), 1f, lX, lY, tX, tY, rX, rY, cX, cY);
        face(left, tint(left), 0.8f, lX, lY, cX, cY, cX, cY + side, lX, lY + side);
        face(right, tint(right), 0.6f, cX, cY, rX, rY, rX, rY + side, cX, cY + side);
        if (overlay != null) {
            face(overlay, 0xFF91BD59, 0.8f, lX, lY, cX, cY, cX, cY + side, lX, lY + side);
            face(overlay, 0xFF91BD59, 0.6f, cX, cY, rX, rY, rX, rY + side, cX, cY + side);
        }
    }

    private static int tint(String texture) {
        if (texture == null) {
            return 0xFFFFFFFF;
        }
        if (texture.endsWith("grass_block_top") || texture.endsWith("/grass") || texture.endsWith("fern")
                || texture.endsWith("vine")) {
            return 0xFF91BD59;
        }
        if (texture.contains("leaves") && !texture.contains("cherry") && !texture.contains("azalea")) {
            return 0xFF77AB2F;
        }
        return 0xFFFFFFFF;
    }

    private static String firstTexture(Map<String, Object> model, String... refs) {
        for (String r : refs) {
            String t = Assets.resolveTexture(model, r);
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    /** Грань: вершины в порядке левый-верх, правый-верх, правый-низ, левый-низ текстуры. */
    private void face(String texture, int tint, float shade, float ax, float ay, float bx, float by,
                      float cx, float cy, float dx, float dy) {
        Assets.Pixels px = assets.sprite(texture);
        if (px == null) {
            return;
        }
        String key = "lumina:sprite/" + texture;
        // анимированные текстуры (вода, лава): берём первый кадр
        if (px.height() > px.width()) {
            int[] frame = java.util.Arrays.copyOf(px.argb(), px.width() * px.width());
            px = new Assets.Pixels(px.width(), px.width(), frame);
        }
        created.putIfAbsent(key, px);
        int r = (int) (((tint >> 16) & 255) * shade);
        int g = (int) (((tint >> 8) & 255) * shade);
        int b = (int) ((tint & 255) * shade);
        drawTextureQuadImpl(TextureRef.of(key), ax, ay, dx, dy, cx, cy, bx, by,
                0, 0, 0, 1, 1, 1, 1, 0, 0xFF000000 | (r << 16) | (g << 8) | b);
    }

    public float guiWidth() {
        return guiWidth;
    }

    public float guiHeight() {
        return guiHeight;
    }
}
