package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.map.MapSample;
import io.github.ssbaxys.lumina.map.MapSource;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.ArrayList;
import java.util.List;

/**
 * Мини-карта: круглая или квадратная, вращается вместе с игроком или всегда смотрит на север, с масштабом
 * (колесо мыши), стрелкой игрока, метками и буквой «С» (север) на краю.
 *
 * <pre>{@code
 * new MiniMap().size(96, 96)
 *         .source(MapSource.platform().or(new DemoMap(7)))   // мир, а без мира — выдуманная карта
 *         .shape(MiniMap.Shape.ROUND).rotate(true).zoom(2f)
 *         .marker("home", 120, -40, 0xFFFF5555, "Дом");
 * }</pre>
 *
 * Карта загружается текстурой ({@code Platform.createTexture}) не чаще {@link #uploadInterval(float)} и только
 * если источник сообщил об изменениях; без текстур (тесты) рисуется сеткой цветных клеток.
 */
public class MiniMap extends Widget<MiniMap> {
    /** Форма карты. */
    public enum Shape {
        ROUND,
        SQUARE
    }

    /** Метка на карте: точка в мире, цвет и подпись (может быть пустой). */
    public record Marker(String id, double x, double z, int color, String label) {
    }

    private static int nextId;

    private final String textureKey = "minimap/" + (nextId++);
    private final List<Marker> markers = new ArrayList<>();
    private MapSource source = MapSource.platform();
    private Shape shape = Shape.ROUND;
    private boolean rotate = true;
    private float zoom = 2f;
    private float minZoom = 0.5f;
    private float maxZoom = 8f;
    private boolean showNorth = true;
    private boolean showCoordinates;
    private long uploadIntervalNanos = 250_000_000L;

    private MapSample last;
    private TextureRef texture;
    private long textureVersion = Long.MIN_VALUE;
    private int textureOriginX;
    private int textureOriginZ;
    private int textureSize;
    private long lastUpload = Long.MIN_VALUE;
    private boolean texturesUnsupported;

    public MiniMap() {
        super("minimap");
        layoutParams().width = io.github.ssbaxys.lumina.layout.Dim.px(96);
        layoutParams().height = io.github.ssbaxys.lumina.layout.Dim.px(96);
    }

    /** Источник карты (по умолчанию — мир через платформу). */
    public MiniMap source(MapSource source) {
        this.source = source == null ? MapSource.platform() : source;
        textureVersion = Long.MIN_VALUE;
        return this;
    }

    public MiniMap shape(Shape shape) {
        this.shape = shape == null ? Shape.ROUND : shape;
        return this;
    }

    public Shape shape() {
        return shape;
    }

    /** {@code true} — карта вращается, игрок всегда смотрит вверх; {@code false} — север всегда сверху. */
    public MiniMap rotate(boolean rotate) {
        this.rotate = rotate;
        return this;
    }

    public boolean rotates() {
        return rotate;
    }

    /** Пикселей на блок. */
    public MiniMap zoom(float zoom) {
        this.zoom = Math.max(minZoom, Math.min(maxZoom, zoom));
        return this;
    }

    public float zoom() {
        return zoom;
    }

    public MiniMap zoomRange(float min, float max) {
        this.minZoom = Math.max(0.05f, Math.min(min, max));
        this.maxZoom = Math.max(this.minZoom, max);
        return zoom(zoom);
    }

    public MiniMap showNorth(boolean show) {
        this.showNorth = show;
        return this;
    }

    /** Координаты игрока под картой. */
    public MiniMap showCoordinates(boolean show) {
        this.showCoordinates = show;
        invalidateLayout();
        return this;
    }

    /** Как часто можно перезагружать текстуру карты (по умолчанию 0.25 с). */
    public MiniMap uploadInterval(float seconds) {
        this.uploadIntervalNanos = (long) (Math.max(0f, seconds) * 1_000_000_000L);
        return this;
    }

    /** Добавить или заменить метку с этим id. */
    public MiniMap marker(String id, double x, double z, int color, String label) {
        removeMarker(id);
        markers.add(new Marker(id, x, z, color, label == null ? "" : label));
        return this;
    }

    public boolean removeMarker(String id) {
        return markers.removeIf(m -> m.id().equals(id));
    }

    public List<Marker> markers() {
        return List.copyOf(markers);
    }

    /** Последний полученный снимок карты (или {@code null}). */
    public MapSample lastSample() {
        return last;
    }

    /** Радиус обзора в блоках при текущем размере и масштабе (с запасом на углы и поворот). */
    public int viewRadius() {
        float half = Math.max(width(), height()) / 2f;
        return (int) Math.ceil(half * 1.42f / zoom) + 2;
    }

    // ---------------------------------------------------------------- ввод

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == ScrollEvent.SCROLL) {
            float dy = ((ScrollEvent) event).deltaY();
            if (dy != 0f) {
                zoom(zoom * (dy > 0 ? 1.25f : 0.8f));
                event.consume();
                event.stopPropagation();
            }
        }
    }

    // ---------------------------------------------------------------- геометрия

    /** Мир → экран (относительно центра карты): поворот так, чтобы взгляд игрока смотрел вверх. */
    private float angle(MapSample s) {
        return rotate ? (float) Math.toRadians(180f - s.yaw()) : 0f;
    }

    /** Экранная точка (относительно центра) → мировые координаты. */
    private double[] toWorld(MapSample s, float sx, float sy, double[] out) {
        float a = angle(s);
        double cos = Math.cos(a);
        double sin = Math.sin(a);
        double rx = sx / zoom;
        double ry = sy / zoom;
        // обратный поворот
        out[0] = s.playerX() + rx * cos + ry * sin;
        out[1] = s.playerZ() - rx * sin + ry * cos;
        return out;
    }

    /** Мировая точка → экран относительно центра. */
    private float[] toScreen(MapSample s, double wx, double wz, float[] out) {
        float a = angle(s);
        double rx = (wx - s.playerX()) * zoom;
        double rz = (wz - s.playerZ()) * zoom;
        out[0] = (float) (rx * Math.cos(a) - rz * Math.sin(a));
        out[1] = (float) (rx * Math.sin(a) + rz * Math.cos(a));
        return out;
    }

    // ---------------------------------------------------------------- отрисовка

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        setContentSize(96f, 96f + (showCoordinates ? 12f : 0f));
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float w = width() - padding().horizontal();
        float h = height() - padding().vertical() - (showCoordinates ? 12f : 0f);
        if (w <= 2f || h <= 2f) {
            return;
        }
        float left = padding().left();
        float top = padding().top();
        float d = Math.min(w, h);
        float cx = left + w / 2f;
        float cy = top + h / 2f;
        float r = d / 2f;
        boolean round = shape == Shape.ROUND;
        Radii frame = round ? Radii.all(r) : style.get(Styles.RADIUS);
        float fx = round ? cx - r : left;
        float fy = round ? cy - r : top;
        float fw = round ? d : w;
        float fh = round ? d : h;
        canvas.fillRoundRect(fx, fy, fw, fh, frame, Brush.solid(0xFF11151C));

        MapSample s = source.sample(viewRadius());
        last = s;
        TextMetrics tm = canvas.textMetrics();
        if (s == null) {
            String msg = I18n.ru("Нет карты");
            canvas.drawText(msg, cx - tm.width(msg) / 2f, cy - tm.lineHeight() / 2f, 0x90FFFFFF, false);
            paintFrame(canvas, fx, fy, fw, fh, frame);
            return;
        }
        TextureRef tex = upload(s);
        if (tex != null) {
            paintTexture(canvas, s, tex, cx, cy, r, fx, fy, fw, fh, round);
        } else {
            paintCells(canvas, s, cx, cy, r, fx, fy, fw, fh, round);
        }
        paintMarkers(canvas, s, cx, cy, r, fx, fy, fw, fh, round);
        paintPlayer(canvas, s, cx, cy);
        if (showNorth && rotate) {
            float[] p = toScreen(s, s.playerX(), s.playerZ() - 1000.0, new float[2]);
            float len = (float) Math.hypot(p[0], p[1]);
            float edge = round ? r - 6f : Math.min(fw, fh) / 2f - 6f;
            float nx = cx + p[0] / len * edge;
            float ny = cy + p[1] / len * edge;
            canvas.fillCircle(nx, ny, 5f, Brush.solid(0xD0101418));
            canvas.drawText(I18n.ru("С"), nx - tm.width(I18n.ru("С")) / 2f, ny - tm.lineHeight() / 2f + 0.5f, 0xFFFF6B6B, false);
        }
        paintFrame(canvas, fx, fy, fw, fh, frame);
        if (showCoordinates) {
            String c = (int) Math.floor(s.playerX()) + ", " + (int) Math.floor(s.playerZ());
            canvas.drawText(c, left + (w - tm.width(c)) / 2f, top + h + 2f, style.get(Styles.TEXT_COLOR), false);
        }
    }

    private void paintFrame(Canvas canvas, float x, float y, float w, float h, Radii frame) {
        int border = style.get(Styles.BORDER_COLOR);
        if (Color.alpha(border) == 0) {
            border = 0x70FFFFFF;
        }
        float bw = Math.max(1.5f, style.get(Styles.BORDER_WIDTH));
        canvas.strokeRoundRect(x, y, w, h, frame, bw, border);
    }

    /** Загрузить текстуру, если источник сообщил об изменениях и прошло достаточно времени. */
    private TextureRef upload(MapSample s) {
        if (texturesUnsupported || !Lumina.isInitialized()) {
            return null;
        }
        boolean stale = s.version() != textureVersion || s.originX() != textureOriginX
                || s.originZ() != textureOriginZ || s.size() != textureSize;
        long now = Lumina.platform().nanoTime();
        if (stale && (texture == null || lastUpload == Long.MIN_VALUE || now - lastUpload >= uploadIntervalNanos)) {
            TextureRef t = Lumina.platform().createTexture(textureKey, s.size(), s.size(), s.colors());
            if (t == null) {
                texturesUnsupported = true;
                return null;
            }
            texture = t;
            textureVersion = s.version();
            textureOriginX = s.originX();
            textureOriginZ = s.originZ();
            textureSize = s.size();
            lastUpload = now;
        }
        return texture;
    }

    private void uv(MapSample s, float sx, float sy, double[] world, float[] uv) {
        toWorld(s, sx, sy, world);
        uv[0] = (float) ((world[0] - textureOriginX) / textureSize);
        uv[1] = (float) ((world[1] - textureOriginZ) / textureSize);
    }

    private void paintTexture(Canvas canvas, MapSample s, TextureRef tex, float cx, float cy, float r,
                              float fx, float fy, float fw, float fh, boolean round) {
        double[] world = new double[2];
        float[] a = new float[2];
        float[] b = new float[2];
        float[] c = new float[2];
        if (!round) {
            float[] d = new float[2];
            uv(s, fx - cx, fy - cy, world, a);
            uv(s, fx - cx, fy + fh - cy, world, b);
            uv(s, fx + fw - cx, fy + fh - cy, world, c);
            uv(s, fx + fw - cx, fy - cy, world, d);
            canvas.save();
            canvas.clipRoundRect(fx, fy, fw, fh, style.get(Styles.RADIUS));
            canvas.drawTextureQuad(tex, fx, fy, fx, fy + fh, fx + fw, fy + fh, fx + fw, fy,
                    a[0], a[1], b[0], b[1], c[0], c[1], d[0], d[1], Color.WHITE);
            canvas.restore();
            return;
        }
        int segments = Math.max(24, Math.min(64, (int) (r * 0.8f)));
        uv(s, 0f, 0f, world, a);
        float rr = r - 0.5f;
        float px = rr;
        float py = 0f;
        uv(s, px, py, world, b);
        for (int i = 1; i <= segments; i++) {
            double t = Math.PI * 2 * i / segments;
            float nx = (float) Math.cos(t) * rr;
            float ny = (float) Math.sin(t) * rr;
            uv(s, nx, ny, world, c);
            canvas.drawTextureQuad(tex, cx, cy, cx + px, cy + py, cx + nx, cy + ny, cx + nx, cy + ny,
                    a[0], a[1], b[0], b[1], c[0], c[1], c[0], c[1], Color.WHITE);
            px = nx;
            py = ny;
            b[0] = c[0];
            b[1] = c[1];
        }
    }

    /** Без текстур: сетка клеток (по несколько блоков на клетку), каждая — повёрнутый четырёхугольник. */
    private void paintCells(Canvas canvas, MapSample s, float cx, float cy, float r,
                            float fx, float fy, float fw, float fh, boolean round) {
        int radius = viewRadius();
        int step = Math.max(1, (int) Math.ceil(radius * 2 / 48.0));
        int px = (int) Math.floor(s.playerX());
        int pz = (int) Math.floor(s.playerZ());
        float[] p0 = new float[2];
        float[] p1 = new float[2];
        float[] p2 = new float[2];
        float[] p3 = new float[2];
        float margin = step * zoom;
        for (int z = pz - radius; z <= pz + radius; z += step) {
            for (int x = px - radius; x <= px + radius; x += step) {
                int color = s.colorAt(x, z);
                if (Color.alpha(color) == 0) {
                    continue;
                }
                toScreen(s, x + step / 2.0, z + step / 2.0, p0);
                if (round ? Math.hypot(p0[0], p0[1]) > r - margin * 0.5f
                        : Math.abs(p0[0]) > fw / 2f - margin * 0.5f || Math.abs(p0[1]) > fh / 2f - margin * 0.5f) {
                    continue;
                }
                toScreen(s, x, z, p0);
                toScreen(s, x, z + step, p1);
                toScreen(s, x + step, z + step, p2);
                toScreen(s, x + step, z, p3);
                canvas.fillQuad(cx + p0[0], cy + p0[1], cx + p1[0], cy + p1[1], cx + p2[0], cy + p2[1],
                        cx + p3[0], cy + p3[1], color, color, color, color);
            }
        }
    }

    private void paintMarkers(Canvas canvas, MapSample s, float cx, float cy, float r,
                              float fx, float fy, float fw, float fh, boolean round) {
        TextMetrics tm = canvas.textMetrics();
        float[] p = new float[2];
        for (Marker m : markers) {
            toScreen(s, m.x(), m.z(), p);
            boolean clamped = false;
            if (round) {
                float len = (float) Math.hypot(p[0], p[1]);
                float limit = r - 4f;
                if (len > limit) {
                    p[0] = p[0] / len * limit;
                    p[1] = p[1] / len * limit;
                    clamped = true;
                }
            } else {
                float hx = fw / 2f - 4f;
                float hy = fh / 2f - 4f;
                float k = Math.max(Math.abs(p[0]) / hx, Math.abs(p[1]) / hy);
                if (k > 1f) {
                    p[0] /= k;
                    p[1] /= k;
                    clamped = true;
                }
            }
            float mx = cx + p[0];
            float my = cy + p[1];
            float size = clamped ? 2.5f : 3.5f;
            canvas.fillCircle(mx, my, size + 1f, Brush.solid(0xC0000000));
            canvas.fillCircle(mx, my, size, Brush.solid(m.color()));
            if (!clamped && !m.label().isEmpty()) {
                float lw = tm.width(m.label());
                canvas.drawText(m.label(), mx - lw / 2f, my - size - tm.lineHeight() - 1f, Color.WHITE, true);
            }
        }
    }

    private void paintPlayer(Canvas canvas, MapSample s, float cx, float cy) {
        double a;
        if (rotate) {
            a = -Math.PI / 2;
        } else {
            // взгляд Minecraft: (−sin yaw, cos yaw) по (x, z); экран: x вправо, z вниз
            double yaw = Math.toRadians(s.yaw());
            a = Math.atan2(Math.cos(yaw), -Math.sin(yaw));
        }
        float len = 6f;
        float wing = 4f;
        float tipX = cx + (float) Math.cos(a) * len;
        float tipY = cy + (float) Math.sin(a) * len;
        float lx = cx + (float) Math.cos(a + 2.5) * wing;
        float ly = cy + (float) Math.sin(a + 2.5) * wing;
        float rx = cx + (float) Math.cos(a - 2.5) * wing;
        float ry = cy + (float) Math.sin(a - 2.5) * wing;
        float bx = cx - (float) Math.cos(a) * 1.5f;
        float by = cy - (float) Math.sin(a) * 1.5f;
        canvas.fillCircle(cx, cy, 7f, Brush.solid(0x40000000));
        canvas.fillQuad(tipX, tipY, lx, ly, bx, by, rx, ry, Color.WHITE, Color.WHITE, 0xFFD0D8E8, Color.WHITE);
        canvas.drawLine(tipX, tipY, lx, ly, 1f, 0xFF202020);
        canvas.drawLine(tipX, tipY, rx, ry, 1f, 0xFF202020);
    }
}
