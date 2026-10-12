package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.map.MapColorProvider;
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
 * Статичная карта мира (предпросмотр сида, карта сервера, карта из файла): область {@code size × size}
 * блоков вокруг центра строится один раз из {@link MapColorProvider} и загружается текстурой. Перетаскивание
 * двигает карту, колесо меняет масштаб, метки подписываются.
 *
 * <pre>{@code
 * new WorldMap(new DemoMap(42), 0, 0, 256).size(220, 160).marker(0, 0, 0xFFFFD84A, "Спавн");
 * }</pre>
 */
public class WorldMap extends Widget<WorldMap> {
    /** Метка на карте. */
    public record Marker(double x, double z, int color, String label) {
    }

    private static int nextId;

    private final String textureKey = "worldmap/" + (nextId++);
    private final List<Marker> markers = new ArrayList<>();
    private final MapColorProvider provider;
    private final int centerX;
    private final int centerZ;
    private final int size;
    private int[] colors;
    private TextureRef texture;
    private boolean uploaded;
    private double viewX;
    private double viewZ;
    private float zoom = 1f;
    private boolean dragging;
    private float dragX;
    private float dragY;
    private double dragViewX;
    private double dragViewZ;

    /**
     * @param size сторона области в блоках (8–1024)
     */
    public WorldMap(MapColorProvider provider, int centerX, int centerZ, int size) {
        super("worldmap");
        this.provider = provider;
        this.centerX = centerX;
        this.centerZ = centerZ;
        this.size = Math.max(8, Math.min(1024, size));
        this.viewX = centerX;
        this.viewZ = centerZ;
        layoutParams().width = Dim.px(200);
        layoutParams().height = Dim.px(140);
    }

    public WorldMap marker(double x, double z, int color, String label) {
        markers.add(new Marker(x, z, color, label == null ? "" : label));
        return this;
    }

    /** Пикселей на блок. */
    public WorldMap zoom(float zoom) {
        this.zoom = Math.max(0.25f, Math.min(8f, zoom));
        return this;
    }

    public float zoom() {
        return zoom;
    }

    /** Центр обзора в мировых координатах. */
    public WorldMap view(double x, double z) {
        this.viewX = Math.max(centerX - size / 2.0, Math.min(centerX + size / 2.0, x));
        this.viewZ = Math.max(centerZ - size / 2.0, Math.min(centerZ + size / 2.0, z));
        return this;
    }

    public double viewX() {
        return viewX;
    }

    public double viewZ() {
        return viewZ;
    }

    /** Перечитать карту у провайдера (например, мир изменился). */
    public WorldMap refresh() {
        colors = null;
        uploaded = false;
        return this;
    }

    /** Цвета области (строятся при первом обращении). */
    public int[] colors() {
        if (colors == null) {
            int[] c = new int[size * size];
            int ox = centerX - size / 2;
            int oz = centerZ - size / 2;
            for (int j = 0; j < size; j++) {
                for (int i = 0; i < size; i++) {
                    c[j * size + i] = provider.colorAt(ox + i, oz + j);
                }
            }
            colors = c;
        }
        return colors;
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == ScrollEvent.SCROLL) {
            float dy = ((ScrollEvent) event).deltaY();
            if (dy != 0f) {
                zoom(zoom * (dy > 0 ? 1.25f : 0.8f));
                event.consume();
                event.stopPropagation();
            }
        } else if (event.type() == PointerEvent.PRESS && ((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
            PointerEvent p = (PointerEvent) event;
            dragging = true;
            dragX = p.x();
            dragY = p.y();
            dragViewX = viewX;
            dragViewZ = viewZ;
            event.consume();
        } else if (event.type() == PointerEvent.DRAG && dragging) {
            PointerEvent p = (PointerEvent) event;
            view(dragViewX - (p.x() - dragX) / zoom, dragViewZ - (p.y() - dragY) / zoom);
            event.consume();
        } else if (event.type() == PointerEvent.RELEASE) {
            dragging = false;
        }
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float x = padding().left();
        float y = padding().top();
        float w = width() - padding().horizontal();
        float h = height() - padding().vertical();
        if (w <= 0f || h <= 0f) {
            return;
        }
        Radii radii = style.get(Styles.RADIUS);
        canvas.fillRoundRect(x, y, w, h, radii, Brush.solid(0xFF11151C));
        int ox = centerX - size / 2;
        int oz = centerZ - size / 2;
        // видимая область мира
        double wx0 = viewX - w / 2.0 / zoom;
        double wz0 = viewZ - h / 2.0 / zoom;
        if (!uploaded && Lumina.isInitialized()) {
            texture = Lumina.platform().createTexture(textureKey, size, size, colors());
            uploaded = true;
        }
        canvas.save();
        canvas.clipRoundRect(x, y, w, h, radii);
        if (texture != null) {
            // только пересечение обзора с областью карты: за краем текстура не повторяется
            double ax0 = Math.max(wx0, ox);
            double az0 = Math.max(wz0, oz);
            double ax1 = Math.min(wx0 + w / zoom, ox + size);
            double az1 = Math.min(wz0 + h / zoom, oz + size);
            if (ax1 > ax0 && az1 > az0) {
                canvas.drawTexture(texture, x + (float) ((ax0 - wx0) * zoom), y + (float) ((az0 - wz0) * zoom),
                        (float) ((ax1 - ax0) * zoom), (float) ((az1 - az0) * zoom),
                        (float) ((ax0 - ox) / size), (float) ((az0 - oz) / size),
                        (float) ((ax1 - ox) / size), (float) ((az1 - oz) / size), Color.WHITE);
            }
        } else {
            paintCells(canvas, x, y, w, h, wx0, wz0, ox, oz);
        }
        canvas.restore();
        TextMetrics tm = canvas.textMetrics();
        for (Marker m : markers) {
            float mx = x + (float) ((m.x() - wx0) * zoom);
            float my = y + (float) ((m.z() - wz0) * zoom);
            if (mx < x || my < y || mx > x + w || my > y + h) {
                continue;
            }
            canvas.fillCircle(mx, my, 4f, Brush.solid(0xC0000000));
            canvas.fillCircle(mx, my, 3f, Brush.solid(m.color()));
            if (!m.label().isEmpty()) {
                canvas.drawText(m.label(), mx + 5f, my - tm.lineHeight() / 2f, Color.WHITE, true);
            }
        }
        canvas.strokeRoundRect(x, y, w, h, radii, 1f, 0x50FFFFFF);
    }

    /** Без текстур: клетки по несколько блоков. */
    private void paintCells(Canvas canvas, float x, float y, float w, float h, double wx0, double wz0, int ox, int oz) {
        int[] c = colors();
        int step = Math.max(1, (int) Math.ceil(Math.max(w, h) / zoom / 48f));
        int bx0 = (int) Math.floor(wx0);
        int bz0 = (int) Math.floor(wz0);
        int bx1 = (int) Math.ceil(wx0 + w / zoom);
        int bz1 = (int) Math.ceil(wz0 + h / zoom);
        for (int bz = bz0 - Math.floorMod(bz0, step); bz < bz1; bz += step) {
            for (int bx = bx0 - Math.floorMod(bx0, step); bx < bx1; bx += step) {
                int i = bx - ox;
                int j = bz - oz;
                if (i < 0 || j < 0 || i >= size || j >= size) {
                    continue;
                }
                int color = c[j * size + i];
                if (Color.alpha(color) == 0) {
                    continue;
                }
                float sx = x + (float) ((bx - wx0) * zoom);
                float sy = y + (float) ((bz - wz0) * zoom);
                canvas.fillRect(sx, sy, step * zoom, step * zoom, color);
            }
        }
    }
}
