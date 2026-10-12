package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;

/**
 * Кнопка с текстом. Нажатие мышью (левая кнопка) или Enter/Space в фокусе вызывает {@link #onPress}.
 * Внешний вид целиком задаётся стилем "button" темы (hover, pressed, focus, disabled — с переходами).
 */
public class Button extends Widget<Button> {
    private String text;
    private Runnable onPress;
    private boolean clickSound = true;

    public Button(String text) {
        super("button");
        this.text = text;
        setFocusable(true);
    }

    public Button(String text, Runnable onPress) {
        this(text);
        this.onPress = onPress;
    }

    public String text() {
        return text;
    }

    public Button text(String text) {
        this.text = text;
        invalidateLayout();
        return this;
    }

    public Button onPress(Runnable onPress) {
        this.onPress = onPress;
        return this;
    }

    public Button clickSound(boolean enabled) {
        this.clickSound = enabled;
        return this;
    }

    public void press() {
        if (!isEnabled()) {
            return;
        }
        if (clickSound && Lumina.isInitialized()) {
            Lumina.platform().playClickSound();
        }
        if (onPress != null) {
            onPress.run();
        }
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.CLICK && event.target() == this) {
            if (((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
                press();
                event.consume();
            }
        } else if (event.type() == KeyEvent.PRESS && event.target() == this) {
            int key = ((KeyEvent) event).key();
            if (key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.SPACE) {
                press();
                event.consume();
            }
        }
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        setContentSize(m.width(text), m.lineHeight());
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        float tx = (width() - m.width(text)) / 2f;
        float ty = (height() - m.lineHeight()) / 2f + 0.5f;
        canvas.drawText(text, tx, ty, style.get(Styles.TEXT_COLOR), style.get(Styles.TEXT_SHADOW));
    }
}
