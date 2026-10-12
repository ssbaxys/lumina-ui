package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.Layout;
import io.github.ssbaxys.lumina.layout.LayoutParams;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;

/**
 * Окно внутри экрана: заголовок за который его таскают, уголок для изменения размера и крестик.
 * Нажатие на окно поднимает его над остальными. Положение задаётся абсолютно внутри родителя
 * (подойдёт любой контейнер), окно не выходит за его границы.
 *
 * <pre>{@code
 * Panel desk = Panel.box(new StackLayout(Align.START, Align.START)).fill();
 * desk.add(new Window("Инвентарь", content).at(20, 20).size(160, 120).resizable(true));
 * }</pre>
 */
public class Window extends Widget<Window> {
    public static final float HEADER = 20f;
    private static final float GRIP = 9f;

    private final String title;
    private final Panel body;
    private boolean resizable = true;
    private boolean closable = true;
    private boolean modal;
    private float minW = 80f;
    private float minH = 50f;
    private Runnable onClose;
    private int drag; // 0 — нет, 1 — перенос, 2 — размер
    private float grabX;
    private float grabY;
    private boolean hoverClose;
    private boolean closing;

    public Window(String title, UiElement... content) {
        super("window");
        this.title = title;
        this.body = Panel.box(FlexLayout.column().gap(6)).add(content);
        setFocusable(true);
        setClipChildren(true);
        LayoutParams p = layoutParams();
        p.absolute = true;
        setLayout(new Layout() {
            @Override
            public void measure(UiElement container, float maxWidth, float maxHeight) {
                body.measure(maxWidth, Float.POSITIVE_INFINITY);
                TextMetrics m = Lumina.platform().textMetrics();
                container.setContentSize(Math.max(body.measuredWidth(), m.width(title) + 24f),
                        HEADER + body.measuredHeight());
            }

            @Override
            public void arrange(UiElement container, float x, float y, float w, float h) {
                body.measure(w, Math.max(0f, h - HEADER));
                body.setBounds(x, y + HEADER, w, Math.max(0f, h - HEADER));
            }
        });
        addChild(body);
    }

    /** Содержимое окна. */
    public Panel body() {
        return body;
    }

    public String title() {
        return title;
    }

    /** Положение левого верхнего угла внутри родителя. */
    public Window at(float x, float y) {
        layoutParams().left = x;
        layoutParams().top = y;
        invalidateLayout();
        return this;
    }

    public Window resizable(boolean resizable) {
        this.resizable = resizable;
        return this;
    }

    public Window closable(boolean closable) {
        this.closable = closable;
        return this;
    }

    public Window minSize(float w, float h) {
        this.minW = w;
        this.minH = h;
        return this;
    }

    /**
     * Модальное окно: затемняет всё остальное внутри родителя и перехватывает клики по фону
     * (рисуется последним, поэтому держите его последним ребёнком).
     */
    public Window modal(boolean modal) {
        this.modal = modal;
        setOverflowsBounds(modal);
        return this;
    }

    public Window onClose(Runnable onClose) {
        this.onClose = onClose;
        return this;
    }

    /** Закрыть с анимацией и убрать из родителя. */
    public void close() {
        if (closing || parent() == null) {
            return;
        }
        closing = true;
        animate(Styles.OPACITY).to(0f).duration(0.15f).ease(Easing.IN_CUBIC).play();
        animate(Styles.SCALE).to(0.92f).duration(0.15f).ease(Easing.IN_CUBIC).onFinish(() -> {
            UiElement p = parent();
            if (p != null) {
                p.removeChild(this);
            }
            if (onClose != null) {
                onClose.run();
            }
        }).play();
    }

    public boolean isClosing() {
        return closing;
    }

    @Override
    protected void onAttach() {
        closing = false;
        animate(Styles.OPACITY).from(0f).to(1f).duration(0.18f).ease(Easing.OUT_CUBIC).keep(false).play();
        animate(Styles.SCALE).from(0.9f).to(1f).duration(0.25f).ease(Easing.OUT_BACK).keep(false).play();
    }

    private boolean onCloseButton(float lx, float ly) {
        return closable && ly < HEADER && lx > width() - HEADER - 2f;
    }

    private boolean onGrip(float lx, float ly) {
        return resizable && lx > width() - GRIP && ly > height() - GRIP;
    }

    @Override
    public UiElement hitTest(float px, float py) {
        UiElement hit = super.hitTest(px, py);
        // модальное окно ловит клики мимо себя, чтобы они не дошли до экрана под ним
        return hit == null && modal && isVisible() && !closing ? this : hit;
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event instanceof PointerEvent p) {
            float lx = p.localX();
            float ly = p.localY();
            boolean inside = lx >= 0f && ly >= 0f && lx < width() && ly < height();
            if (event.type() == PointerEvent.PRESS && p.button() == Keys.MOUSE_LEFT) {
                if (!inside) {
                    event.consume();
                    return;
                }
                bringToFront();
                if (onGrip(lx, ly)) {
                    drag = 2;
                    grabX = width() - lx;
                    grabY = height() - ly;
                    event.consume();
                } else if (ly < HEADER && event.target() == this && !onCloseButton(lx, ly)) {
                    drag = 1;
                    grabX = lx;
                    grabY = ly;
                    event.consume();
                }
            } else if (event.type() == PointerEvent.DRAG && drag != 0) {
                UiElement parent = parent();
                float pw = parent == null ? Float.MAX_VALUE : parent.width();
                float ph = parent == null ? Float.MAX_VALUE : parent.height();
                float px = parent == null ? p.x() : p.x() - parent.screenX();
                float py = parent == null ? p.y() : p.y() - parent.screenY();
                LayoutParams lp = layoutParams();
                if (drag == 1) {
                    lp.left = clamp(px - grabX, 0f, Math.max(0f, pw - width()));
                    lp.top = clamp(py - grabY, 0f, Math.max(0f, ph - height()));
                } else {
                    float w = clamp(px - lp.left + grabX, minW, Math.max(minW, pw - lp.left));
                    float h = clamp(py - lp.top + grabY, minH, Math.max(minH, ph - lp.top));
                    lp.width = Dim.px(w);
                    lp.height = Dim.px(h);
                }
                invalidateLayout();
                event.consume();
            } else if (event.type() == PointerEvent.RELEASE) {
                drag = 0;
            } else if (event.type() == PointerEvent.MOVE) {
                hoverClose = onCloseButton(lx, ly);
            } else if (event.type() == PointerEvent.LEAVE) {
                hoverClose = false;
            } else if (event.type() == PointerEvent.CLICK && p.button() == Keys.MOUSE_LEFT
                    && onCloseButton(lx, ly) && event.target() == this) {
                close();
                event.consume();
            } else if (event.type() == PointerEvent.CLICK && !inside && modal) {
                event.consume();
            }
        } else if (event.type() == KeyEvent.PRESS && event.target() == this
                && ((KeyEvent) event).key() == Keys.ESCAPE && closable) {
            close();
            event.consume();
        }
    }

    private static float clamp(float v, float lo, float hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    protected void paintBackground(Canvas canvas) {
        if (modal && parent() != null) {
            // затемнение под окном на весь родитель
            canvas.fillRect(-x(), -y(), parent().width(), parent().height(), 0x80000000);
        }
        super.paintBackground(canvas);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        int text = style.get(Styles.TEXT_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        canvas.fillRect(0f, HEADER - 1f, width(), 1f, Color.withAlpha(text, 0x20));
        float maxTitle = width() - 10f - (closable ? HEADER : 0f);
        canvas.drawText(Table.fit(m, title, maxTitle), 7f, (HEADER - m.lineHeight()) / 2f + 1f,
                isFocused() || drag == 1 ? accent : text, false);
        if (closable) {
            float cx = width() - HEADER / 2f - 1f;
            float cy = HEADER / 2f;
            if (hoverClose) {
                canvas.fillCircle(cx, cy, 7f, Brush.solid(0xC0E04848));
            }
            int c = hoverClose ? 0xFFFFFFFF : Color.withAlpha(text, 0xA0);
            canvas.drawLine(cx - 3f, cy - 3f, cx + 3f, cy + 3f, 1.4f, c);
            canvas.drawLine(cx + 3f, cy - 3f, cx - 3f, cy + 3f, 1.4f, c);
        }
    }

    @Override
    protected void paintOverlay(Canvas canvas, float partialTick) {
        if (resizable) {
            int c = Color.withAlpha(style.get(Styles.TEXT_COLOR), drag == 2 ? 0xC0 : 0x50);
            float r = width() - 3f;
            float b = height() - 3f;
            canvas.drawLine(r - 6f, b, r, b - 6f, 1f, c);
            canvas.drawLine(r - 3f, b, r, b - 3f, 1f, c);
        }
    }
}
