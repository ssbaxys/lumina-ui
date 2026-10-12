package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorStops;
import io.github.ssbaxys.lumina.render.Radii;

import java.util.Random;

/**
 * Живые обои под интерфейсом: северное сияние, звёздное небо, волны, переливающийся градиент
 * или ретро-сетка. Мышь не ловит; с {@link #parallax} слои слегка смещаются за курсором.
 *
 * <pre>{@code
 * Panel screen = Panel.box(new StackLayout(Align.START, Align.START)).fill()
 *         .add(new Backdrop(Backdrop.Kind.AURORA).fill(), content);
 * }</pre>
 */
public class Backdrop extends Widget<Backdrop> implements Animation {
    public enum Kind {
        /** Мягкие цветные пятна плывут по тёмному фону. */
        AURORA,
        /** Мерцающие звёзды в несколько слоёв и редкие падающие звёзды. */
        STARS,
        /** Полупрозрачные волны снизу. */
        WAVES,
        /** Медленно вращающийся конический градиент. */
        FLOW,
        /** Ретро-сетка с солнцем, бегущая к зрителю. */
        GRID
    }

    private static final int STAR_COUNT = 140;

    private final Kind kind;
    private int[] colors;
    private int base;
    private float speed = 1f;
    private float parallax;
    private float time;
    private boolean alive;
    private float camX;
    private float camY;
    private final float[] stars;
    private float shootT = -1f;
    private float shootX;
    private float shootY;
    private final Random random = new Random(7L);

    public Backdrop(Kind kind) {
        super("backdrop");
        this.kind = kind;
        this.colors = defaults(kind);
        this.base = kind == Kind.GRID ? 0xFF140B26 : 0xFF0B0E17;
        setHitTestable(false);
        fill();
        // x, y (доли), глубина 0..1, фаза мерцания
        stars = new float[STAR_COUNT * 4];
        for (int i = 0; i < STAR_COUNT; i++) {
            stars[i * 4] = random.nextFloat();
            stars[i * 4 + 1] = random.nextFloat();
            stars[i * 4 + 2] = random.nextFloat();
            stars[i * 4 + 3] = random.nextFloat() * 6.283f;
        }
    }

    private static int[] defaults(Kind kind) {
        return switch (kind) {
            case AURORA -> new int[]{0xFF2BD9A8, 0xFF5B8CFF, 0xFFB46CFF, 0xFF1FA2C9};
            case STARS -> new int[]{0xFFFFFFFF, 0xFFBFD4FF, 0xFFFFE6B8};
            case WAVES -> new int[]{0xFF2F6FD8, 0xFF3FA7E0, 0xFF6FD6F0};
            case FLOW -> new int[]{0xFF6A5AE0, 0xFFE05A9C, 0xFFF0A84A, 0xFF4AC6F0};
            case GRID -> new int[]{0xFFFF4FD8, 0xFFFFC14F, 0xFF4FE3FF};
        };
    }

    public Kind kind() {
        return kind;
    }

    /** Свои цвета (пятна, волны, звёзды). */
    public Backdrop colors(int... colors) {
        if (colors.length > 0) {
            this.colors = colors.clone();
        }
        return this;
    }

    /** Цвет фона под эффектом. */
    public Backdrop base(int argb) {
        this.base = argb;
        return this;
    }

    public Backdrop speed(float speed) {
        this.speed = speed;
        return this;
    }

    /** Сдвиг слоёв за мышью в пикселях (0 — выключено). */
    public Backdrop parallax(float pixels) {
        this.parallax = pixels;
        return this;
    }

    @Override
    protected void onAttach() {
        alive = true;
        root().animator().start(this);
    }

    @Override
    protected void onDetach() {
        alive = false;
    }

    @Override
    public boolean tick(float dt) {
        time += dt * speed;
        UiRoot r = root();
        if (parallax > 0f && r != null && r.mouseX() >= 0f && r.width() > 0f) {
            float tx = (r.mouseX() / r.width() - 0.5f) * 2f;
            float ty = (r.mouseY() / r.height() - 0.5f) * 2f;
            float k = 1f - (float) Math.exp(-dt * 4f);
            camX += (tx - camX) * k;
            camY += (ty - camY) * k;
        }
        if (kind == Kind.STARS) {
            if (shootT < 0f && random.nextFloat() < dt * 0.25f) {
                shootT = 0f;
                shootX = 0.2f + random.nextFloat() * 0.7f;
                shootY = random.nextFloat() * 0.4f;
            } else if (shootT >= 0f) {
                shootT += dt;
                if (shootT > 0.9f) {
                    shootT = -1f;
                }
            }
        }
        return alive;
    }

    private int color(int i) {
        return colors[Math.floorMod(i, colors.length)];
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float w = width();
        float h = height();
        if (w <= 0f || h <= 0f) {
            return;
        }
        canvas.fillRect(0, 0, w, h, base);
        switch (kind) {
            case AURORA -> paintAurora(canvas, w, h);
            case STARS -> paintStars(canvas, w, h);
            case WAVES -> paintWaves(canvas, w, h);
            case FLOW -> paintFlow(canvas, w, h);
            case GRID -> paintGrid(canvas, w, h);
        }
    }

    private void paintAurora(Canvas canvas, float w, float h) {
        int n = Math.max(3, colors.length);
        for (int i = 0; i < n; i++) {
            float t = time * (0.07f + i * 0.023f);
            float cx = w * (0.5f + 0.38f * (float) Math.sin(t * 2.1f + i * 1.7f)) - camX * parallax * (0.4f + i * 0.2f);
            float cy = h * (0.5f + 0.34f * (float) Math.cos(t * 1.6f + i * 2.3f)) - camY * parallax * (0.4f + i * 0.2f);
            float r = Math.max(w, h) * (0.35f + 0.08f * (float) Math.sin(t * 3f + i));
            int c = color(i);
            canvas.fillRoundRect(cx - r, cy - r, 2f * r, 2f * r, Radii.ZERO,
                    Brush.radial(ColorStops.of(new float[]{0f, 0.45f, 1f},
                            new int[]{Color.withAlpha(c, 0x70), Color.withAlpha(c, 0x24), Color.withAlpha(c, 0)})));
        }
        // лёгкая виньетка
        canvas.fillRect(0, 0, w, h, Brush.radial(ColorStops.of(new float[]{0.55f, 1f},
                new int[]{0x00000000, 0x90000000})).withRadius(0.75f, 0.75f));
    }

    private void paintStars(Canvas canvas, float w, float h) {
        canvas.fillRect(0, 0, w, h, Brush.vertical(Color.withAlpha(color(1), 0x00), Color.withAlpha(color(1), 0x22)));
        for (int i = 0; i < STAR_COUNT; i++) {
            float depth = stars[i * 4 + 2];
            float drift = time * (2f + depth * 6f);
            float x = (stars[i * 4] * w + drift - camX * parallax * depth) % w;
            if (x < 0f) {
                x += w;
            }
            float y = stars[i * 4 + 1] * h - camY * parallax * depth;
            float tw = 0.55f + 0.45f * (float) Math.sin(time * (1.5f + depth * 2f) + stars[i * 4 + 3]);
            float size = 0.6f + depth * 1.4f;
            int c = Color.withAlpha(color(i), (int) (255 * tw * (0.35f + 0.65f * depth)));
            canvas.fillRect(x, y, size, size, c);
            if (depth > 0.93f) {
                canvas.fillRect(x - size, y + size / 2f - 0.25f, size * 3f, 0.5f, Color.withAlpha(c, 0x60));
                canvas.fillRect(x + size / 2f - 0.25f, y - size, 0.5f, size * 3f, Color.withAlpha(c, 0x60));
            }
        }
        if (shootT >= 0f) {
            float p = shootT / 0.9f;
            float sx = shootX * w - p * w * 0.35f;
            float sy = shootY * h + p * h * 0.25f;
            int a = (int) (255 * Math.sin(p * Math.PI));
            canvas.drawLine(sx, sy, sx + 26f, sy - 18f, 1.2f, Color.withAlpha(0xFFFFFFFF, a / 3));
            canvas.drawLine(sx, sy, sx + 10f, sy - 7f, 1.4f, Color.withAlpha(0xFFFFFFFF, a));
        }
    }

    private void paintWaves(Canvas canvas, float w, float h) {
        int layers = Math.max(3, colors.length);
        int steps = Math.max(12, (int) (w / 8f));
        float dx = w / steps;
        for (int l = 0; l < layers; l++) {
            int c = color(l);
            float baseY = h * (0.55f + l * 0.12f) - camY * parallax * (0.3f + l * 0.25f);
            float amp = h * (0.05f - l * 0.008f);
            float freq = 1.4f + l * 0.6f;
            float phase = time * (0.6f + l * 0.25f) + l * 1.3f;
            float shift = camX * parallax * (0.3f + l * 0.25f);
            int top = Color.withAlpha(c, 0x90);
            int bottom = Color.withAlpha(Color.darken(c, 0.25f), 0xC0);
            for (int i = 0; i < steps; i++) {
                float x0 = i * dx;
                float x1 = x0 + dx;
                float y0 = baseY + amp * (float) Math.sin((x0 + shift) / w * 6.283f * freq + phase);
                float y1 = baseY + amp * (float) Math.sin((x1 + shift) / w * 6.283f * freq + phase);
                canvas.fillQuad(x0, y0, x1, y1, x1, h, x0, h, top, top, bottom, bottom);
            }
        }
    }

    private void paintFlow(Canvas canvas, float w, float h) {
        int n = colors.length;
        int[] cs = new int[n + 1];
        float[] ps = new float[n + 1];
        for (int i = 0; i <= n; i++) {
            cs[i] = color(i);
            ps[i] = (float) i / n;
        }
        float cx = 0.5f + 0.15f * (float) Math.sin(time * 0.3f) - camX * parallax / Math.max(1f, w);
        float cy = 0.5f + 0.15f * (float) Math.cos(time * 0.25f) - camY * parallax / Math.max(1f, h);
        canvas.fillRect(0, 0, w, h, Brush.conic(time * 20f, ColorStops.of(ps, cs)).at(cx, cy));
        // приглушить и сделать мягче к краям
        canvas.fillRect(0, 0, w, h, Brush.radial(ColorStops.of(new float[]{0f, 1f},
                new int[]{Color.withAlpha(base, 0x60), Color.withAlpha(base, 0xD8)})).withRadius(0.8f, 0.8f));
    }

    private void paintGrid(Canvas canvas, float w, float h) {
        float horizon = h * 0.55f - camY * parallax * 0.3f;
        int line = color(0);
        // небо и солнце
        canvas.fillRect(0, 0, w, horizon, Brush.vertical(base, Color.withAlpha(color(0), 0x50)));
        float sr = Math.min(w, h) * 0.18f;
        float sx = w / 2f - camX * parallax * 0.3f;
        canvas.fillRoundRect(sx - sr, horizon - sr * 1.2f, 2f * sr, 2f * sr, Radii.all(sr),
                Brush.vertical(color(1), color(0)));
        for (int i = 0; i < 5; i++) {
            float y = horizon - sr * 0.5f + i * sr * 0.16f;
            canvas.fillRect(sx - sr, y, 2f * sr, 1.2f + i * 0.6f, base);
        }
        canvas.fillRect(0, horizon, w, h - horizon, base);
        // горизонтальные линии бегут к зрителю
        float depth = h - horizon;
        float scroll = (time * 0.6f) % 1f;
        for (int i = 0; i < 14; i++) {
            float z = (i + 1f - scroll) / 14f;
            float y = horizon + depth * z * z;
            canvas.fillRect(0, y, w, 1f, Color.withAlpha(line, (int) (40 + 200 * z)));
        }
        // лучи к точке схода
        float vx = w / 2f - camX * parallax;
        for (int i = -12; i <= 12; i++) {
            float bx = w / 2f + i * w / 10f;
            canvas.drawLine(vx + i * 4f, horizon, bx, h, 1f, Color.withAlpha(line, 0xA0));
        }
        canvas.fillRect(0, horizon, w, 1.5f, color(2));
    }
}
