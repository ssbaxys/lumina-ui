package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorSpace;
import io.github.ssbaxys.lumina.render.ColorStops;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.Styles;

/**
 * Выбор цвета: квадрат насыщенность/яркость, полоса оттенка и (по желанию) полоса прозрачности.
 * Значение — ARGB в {@link Signal}; оттенок запоминается отдельно, поэтому не теряется на сером и чёрном.
 * Стрелки клавиатуры двигают точку в квадрате, PageUp/PageDown — оттенок.
 */
public class ColorPicker extends Widget<ColorPicker> {
    private static final float STRIP = 10f;
    private static final float GAP = 6f;
    private static final Shadow THUMB_SHADOW = new Shadow(0, 1, 3, 0, 0x80000000);
    private static final Brush HUE = Brush.linear(180f, ColorStops.even(
            0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000))
            .inSpace(ColorSpace.SRGB);

    private enum Part { NONE, SV, HUE, ALPHA }

    private final Signal<Integer> value;
    private final float[] hsv = new float[3];
    private float alpha = 1f;
    private boolean showAlpha = true;
    private Part dragging = Part.NONE;
    private int lastWritten;
    private Subscription subscription;

    public ColorPicker() {
        this(Signal.of(0xFF5B8CFF));
    }

    public ColorPicker(Signal<Integer> value) {
        super("color-picker");
        this.value = value;
        setFocusable(true);
        layoutParams().width = Dim.px(150);
        layoutParams().height = Dim.px(100);
        readFrom(value.peek());
    }

    public Signal<Integer> value() {
        return value;
    }

    /** Показывать ли полосу прозрачности (без неё альфа всегда 255). */
    public ColorPicker alpha(boolean show) {
        this.showAlpha = show;
        if (!show) {
            alpha = 1f;
            write();
        }
        return this;
    }

    public float hue() {
        return hsv[0];
    }

    public float saturation() {
        return hsv[1];
    }

    public float brightness() {
        return hsv[2];
    }

    private void readFrom(int argb) {
        float hue = hsv[0];
        Color.toHsv(argb, hsv);
        // На сером/чёрном оттенок не определён: оставляем прежний, чтобы ползунок не прыгал.
        if (hsv[1] <= 0.0001f || hsv[2] <= 0.0001f) {
            hsv[0] = hue;
        }
        alpha = showAlpha ? Color.alphaF(argb) : 1f;
        lastWritten = argb;
    }

    private void write() {
        int c = Color.hsva(hsv[0], hsv[1], hsv[2], alpha);
        lastWritten = c;
        value.set(c);
    }

    private float svWidth() {
        return Math.max(10f, width() - (showAlpha ? 2f : 1f) * (STRIP + GAP));
    }

    private float hueX() {
        return svWidth() + GAP;
    }

    private float alphaX() {
        return hueX() + STRIP + GAP;
    }

    @Override
    protected void onAttach() {
        subscription = value.subscribe(c -> {
            if (c != lastWritten) {
                readFrom(c);
            }
        });
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    private Part partAt(float x) {
        if (x < svWidth() + GAP / 2f) {
            return Part.SV;
        }
        if (!showAlpha || x < alphaX() - GAP / 2f) {
            return Part.HUE;
        }
        return Part.ALPHA;
    }

    private void dragTo(float x, float y) {
        float h = Math.max(1f, height());
        float fy = clamp01(y / h);
        switch (dragging) {
            case SV -> {
                hsv[1] = clamp01(x / svWidth());
                hsv[2] = 1f - fy;
            }
            case HUE -> hsv[0] = fy * 360f;
            case ALPHA -> alpha = 1f - fy;
            default -> {
                return;
            }
        }
        write();
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (!isEnabled()) {
            return;
        }
        if (event.type() == PointerEvent.PRESS && ((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
            PointerEvent pe = (PointerEvent) event;
            dragging = partAt(pe.localX());
            dragTo(pe.localX(), pe.localY());
            event.consume();
        } else if (event.type() == PointerEvent.DRAG && dragging != Part.NONE) {
            PointerEvent pe = (PointerEvent) event;
            dragTo(pe.localX(), pe.localY());
            event.consume();
        } else if (event.type() == PointerEvent.RELEASE) {
            dragging = Part.NONE;
        } else if (event.type() == KeyEvent.PRESS) {
            int key = ((KeyEvent) event).key();
            float d = 0.02f;
            boolean handled = true;
            if (key == Keys.LEFT) {
                hsv[1] = clamp01(hsv[1] - d);
            } else if (key == Keys.RIGHT) {
                hsv[1] = clamp01(hsv[1] + d);
            } else if (key == Keys.UP) {
                hsv[2] = clamp01(hsv[2] + d);
            } else if (key == Keys.DOWN) {
                hsv[2] = clamp01(hsv[2] - d);
            } else if (key == Keys.PAGE_UP) {
                hsv[0] = (hsv[0] + 355f) % 360f;
            } else if (key == Keys.PAGE_DOWN) {
                hsv[0] = (hsv[0] + 5f) % 360f;
            } else {
                handled = false;
            }
            if (handled) {
                write();
                event.consume();
            }
        }
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float h = height();
        float sw = svWidth();
        Radii r = style.get(Styles.RADIUS);
        Radii small = Radii.all(3f);
        int pure = Color.hsv(hsv[0], 1f, 1f);
        int accent = style.get(Styles.ACCENT_COLOR);

        // Квадрат: белый → чистый оттенок по горизонтали, сверху вниз затемнение до чёрного.
        canvas.fillRoundRect(0, 0, sw, h, r, Brush.horizontal(Color.WHITE, pure));
        canvas.fillRoundRect(0, 0, sw, h, r, Brush.vertical(0x00000000, Color.BLACK));
        float px = hsv[1] * sw;
        float py = (1f - hsv[2]) * h;
        int current = Color.hsv(hsv[0], hsv[1], hsv[2]);
        thumb(canvas, px, py, 4f, current);

        float hx = hueX();
        canvas.fillRoundRect(hx, 0, STRIP, h, small, HUE);
        bar(canvas, hx, hsv[0] / 360f * h, pure);

        if (showAlpha) {
            float ax = alphaX();
            checker(canvas, ax, 0, STRIP, h);
            canvas.fillRoundRect(ax, 0, STRIP, h, small, Brush.vertical(Color.withAlpha(current, 0xFF),
                    Color.withAlpha(current, 0)));
            bar(canvas, ax, (1f - alpha) * h, Color.withAlpha(current, alpha));
        }
        if (isFocused()) {
            canvas.strokeRoundRect(-1.5f, -1.5f, sw + 3f, h + 3f, Radii.all(r.topLeft() + 1.5f), 1f, accent);
        }
    }

    private static void thumb(Canvas canvas, float cx, float cy, float radius, int fill) {
        Radii r = Radii.all(radius);
        canvas.drawShadow(cx - radius, cy - radius, 2f * radius, 2f * radius, r, THUMB_SHADOW);
        canvas.fillCircle(cx, cy, radius, Brush.solid(fill));
        canvas.strokeCircle(cx, cy, radius, 1.5f, Brush.solid(Color.WHITE));
    }

    private static void bar(Canvas canvas, float x, float y, int fill) {
        Radii r = Radii.all(2f);
        canvas.drawShadow(x - 1f, y - 2.5f, STRIP + 2f, 5f, r, THUMB_SHADOW);
        canvas.fillRoundRect(x - 1f, y - 2.5f, STRIP + 2f, 5f, r, Brush.solid(Color.WHITE));
        canvas.fillRoundRect(x, y - 1.5f, STRIP, 3f, Radii.all(1f), Brush.solid(Color.withAlpha(fill, 0xFF)));
    }

    /** Шахматка под полосой прозрачности. */
    static void checker(Canvas canvas, float x, float y, float w, float h) {
        float cell = w / 2f;
        canvas.fillRect(x, y, w, h, 0xFFFFFFFF);
        int rows = (int) Math.ceil(h / cell);
        for (int row = 0; row < rows; row++) {
            float cy = y + row * cell;
            float ch = Math.min(cell, y + h - cy);
            canvas.fillRect(x + (row % 2) * cell, cy, cell, ch, 0xFFCCCCCC);
        }
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : Math.min(v, 1f);
    }
}
