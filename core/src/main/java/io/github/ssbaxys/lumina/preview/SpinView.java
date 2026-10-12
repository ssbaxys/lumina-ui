package io.github.ssbaxys.lumina.preview;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.widget.Widget;

/**
 * Основа виджетов с 3D-моделью, которую крутят мышью: поворот (yaw) и наклон камеры (pitch), инерция после
 * броска, автовращение и приближение колесом. Наследник только рисует модель в квадрате
 * ({@link #paintModel}); квадрат вписан в виджет и умножен на приближение.
 *
 * <p>Знаки углов везде одни: {@code yaw > 0} поворачивает модель лицом влево, {@code pitch > 0} — взгляд
 * сверху. Протяжка вправо поворачивает модель вслед за мышью, вниз — показывает её сверху.</p>
 */
public abstract class SpinView<S extends SpinView<S>> extends Widget<S> implements Animation {
    /** Скорость автовращения по умолчанию, °/с. */
    public static final float DEFAULT_SPIN = 30f;
    /** Градусов поворота на пиксель протяжки. */
    public static final float DRAG_SENSITIVITY = 0.8f;

    private float yaw;
    private float pitch;
    private float minPitch = -60f;
    private float maxPitch = 60f;
    private float zoom = 1f;
    private float zoomTarget = 1f;
    private float spin;
    private float yawVelocity;
    private float dragAccum;
    private boolean dragging;
    private float lastX;
    private float lastY;
    private boolean interactive = true;
    private boolean zoomable = true;
    private boolean attached;
    private boolean ticking;

    protected SpinView(String styleClass, float yaw, float pitch) {
        super(styleClass);
        this.yaw = wrap(yaw);
        this.pitch = pitch;
    }

    // ---------------------------------------------------------------- камера

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    /** Поворот вокруг вертикали, градусы (0 — лицом к зрителю). */
    public S yaw(float degrees) {
        this.yaw = wrap(degrees);
        return self();
    }

    /** Наклон камеры, градусы (в пределах {@link #pitchRange}). */
    public S pitch(float degrees) {
        this.pitch = clampPitch(degrees);
        return self();
    }

    public S camera(float yawDegrees, float pitchDegrees) {
        return yaw(yawDegrees).pitch(pitchDegrees);
    }

    /** Пределы наклона камеры при протяжке (по умолчанию −60..60°). */
    public S pitchRange(float min, float max) {
        this.minPitch = Math.min(min, max);
        this.maxPitch = Math.max(min, max);
        this.pitch = clampPitch(pitch);
        return self();
    }

    /** Автовращение со скоростью {@link #DEFAULT_SPIN} или выключить. */
    public S autoRotate(boolean on) {
        return autoRotate(on ? DEFAULT_SPIN : 0f);
    }

    /** Автовращение, °/с (0 — выключено, отрицательное — в другую сторону). */
    public S autoRotate(float degreesPerSecond) {
        this.spin = degreesPerSecond;
        return self();
    }

    public boolean isAutoRotating() {
        return spin != 0f;
    }

    public float zoom() {
        return zoomTarget;
    }

    /** Приближение (1 — модель вписана в виджет), 0.5..3. */
    public S zoom(float zoom) {
        this.zoomTarget = clampZoom(zoom);
        this.zoom = zoomTarget;
        return self();
    }

    /** Вращение мышью (по умолчанию включено). */
    public S interactive(boolean interactive) {
        this.interactive = interactive;
        return self();
    }

    /** Приближение колесом (по умолчанию включено; в прокручиваемой странице может мешать). */
    public S zoomable(boolean zoomable) {
        this.zoomable = zoomable;
        return self();
    }

    /** Крутят ли модель мышью прямо сейчас. */
    public boolean isDragging() {
        return dragging;
    }

    // ---------------------------------------------------------------- ввод

    @Override
    protected void handleEvent(UiEvent event) {
        if (!interactive) {
            return;
        }
        if (event instanceof PointerEvent p) {
            if (event.type() == PointerEvent.PRESS && p.button() == Keys.MOUSE_LEFT) {
                dragging = true;
                lastX = p.x();
                lastY = p.y();
                yawVelocity = 0f;
                dragAccum = 0f;
                event.consume();
            } else if (event.type() == PointerEvent.DRAG && dragging) {
                float dx = p.x() - lastX;
                float dy = p.y() - lastY;
                lastX = p.x();
                lastY = p.y();
                yaw = wrap(yaw - dx * DRAG_SENSITIVITY);
                pitch = clampPitch(pitch + dy * DRAG_SENSITIVITY);
                dragAccum -= dx * DRAG_SENSITIVITY;
                event.consume();
            } else if (event.type() == PointerEvent.RELEASE) {
                dragging = false;
            }
        } else if (event instanceof ScrollEvent s && zoomable && s.deltaY() != 0f) {
            zoomTarget = clampZoom(zoomTarget * (float) Math.pow(1.15, s.deltaY()));
            event.consume();
            event.stopPropagation();
        }
    }

    private float clampPitch(float p) {
        return Math.max(minPitch, Math.min(maxPitch, p));
    }

    private static float clampZoom(float z) {
        return Math.max(0.5f, Math.min(3f, z));
    }

    static float wrap(float deg) {
        float d = deg % 360f;
        return d < 0f ? d + 360f : d;
    }

    // ---------------------------------------------------------------- анимация

    @Override
    protected void onAttach() {
        attached = true;
        if (!ticking) {
            ticking = true;
            root().animator().start(this);
        }
    }

    @Override
    protected void onDetach() {
        attached = false;
        dragging = false;
    }

    @Override
    public boolean tick(float dt) {
        if (!attached) {
            ticking = false;
            return false;
        }
        if (dragging) {
            if (dt > 0f) {
                yawVelocity += (dragAccum / dt - yawVelocity) * 0.5f;
            }
            dragAccum = 0f;
        } else {
            if (yawVelocity != 0f) {
                // инерция: скорость затухает экспоненциально
                yaw = wrap(yaw + yawVelocity * dt);
                yawVelocity *= (float) Math.exp(-3.5 * dt);
                if (Math.abs(yawVelocity) < 1f) {
                    yawVelocity = 0f;
                }
            }
            if (spin != 0f) {
                yaw = wrap(yaw + spin * dt);
            }
        }
        if (zoom != zoomTarget) {
            zoom += (zoomTarget - zoom) * Math.min(1f, dt * 12f);
            if (Math.abs(zoom - zoomTarget) < 1e-3f) {
                zoom = zoomTarget;
            }
        }
        return true;
    }

    // ---------------------------------------------------------------- рисование

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        setContentSize(64f, 64f);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float x = padding().left();
        float y = padding().top();
        float w = width() - padding().horizontal();
        float h = height() - padding().vertical();
        float side = Math.min(w, h);
        if (side <= 0f) {
            return;
        }
        float s = side * zoom;
        boolean clip = zoom > 1.001f;
        if (clip) {
            canvas.save();
            canvas.clipRect(0f, 0f, width(), height());
        }
        paintModel(canvas, x + (w - s) / 2f, y + (h - s) / 2f, s, yaw, pitch);
        if (clip) {
            canvas.restore();
        }
    }

    /** Нарисовать модель в квадрате {@code size × size} с левым верхним углом в (x, y). */
    protected abstract void paintModel(Canvas canvas, float x, float y, float size, float yaw, float pitch);
}
