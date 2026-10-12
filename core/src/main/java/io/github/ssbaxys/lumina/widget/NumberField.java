package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.Locale;

/**
 * Числовое поле, привязанное к {@link Signal} с {@code Float}. Ввод только чисел; значение пишется
 * по Enter и при потере фокуса, приводится к диапазону и числу знаков. Стрелки вверх/вниз и колесо
 * (в фокусе) меняют на шаг, с Shift — на десять шагов. Если задан префикс ({@link #prefix}), его
 * можно тянуть мышью влево-вправо, чтобы менять значение. Неверный ввод вспыхивает красным.
 */
public class NumberField extends FieldBase<NumberField> {
    private final Signal<Float> value;
    private float min = Float.NEGATIVE_INFINITY;
    private float max = Float.POSITIVE_INFINITY;
    private float step = 1f;
    private int decimals = 2;
    private boolean scrub = true;
    private boolean scrubbing;
    private float scrubStartX;
    private float scrubStartValue;

    public NumberField(Signal<Float> value) {
        super(value);
        this.value = value;
        layoutParams().width = Dim.px(60);
        charFilter(c -> (c >= '0' && c <= '9') || c == '.' || c == ',' || c == '-' || c == '+');
        filter(s -> s.matches("[-+]?\\d*([.,]\\d*)?"));
        reload();
    }

    public NumberField(Signal<Float> value, float min, float max) {
        this(value);
        range(min, max);
    }

    public Signal<Float> value() {
        return value;
    }

    public NumberField range(float min, float max) {
        this.min = min;
        this.max = max;
        return self();
    }

    /** Шаг стрелок, колеса и перетаскивания. */
    public NumberField step(float step) {
        this.step = step;
        return self();
    }

    /** Знаков после запятой при показе и записи. */
    public NumberField decimals(int decimals) {
        this.decimals = Math.max(0, decimals);
        reload();
        return self();
    }

    /** Менять значение перетаскиванием префикса (по умолчанию включено). */
    public NumberField scrub(boolean scrub) {
        this.scrub = scrub;
        return self();
    }

    /** Пункт меню "Сбросить по умолчанию". */
    public NumberField defaultValue(float defaultValue) {
        return resetItem(() -> format(defaultValue));
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }

    /** Значение к диапазону и точности. */
    public float clamp(float v) {
        v = Math.max(min, Math.min(max, v));
        double k = Math.pow(10, decimals);
        return (float) (Math.round(v * k) / k);
    }

    public String format(float v) {
        return String.format(Locale.ROOT, "%." + decimals + "f", v);
    }

    /** Разобрать число ("1,5" тоже); {@code null}, если это не число. */
    public static Float parse(String text) {
        String t = text.trim().replace(',', '.');
        if (t.isEmpty() || t.equals("-") || t.equals("+") || t.equals(".")) {
            return null;
        }
        try {
            float v = Float.parseFloat(t);
            return Float.isFinite(v) ? v : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Изменить значение на {@code steps} шагов (от введённого, если оно уже число). */
    public void nudge(float steps) {
        Float typed = parse(text());
        float base = typed != null ? typed : current();
        set(base + steps * step);
    }

    private float current() {
        Float v = value.peek();
        return v == null ? 0f : v;
    }

    private void set(float v) {
        float next = clamp(v);
        value.set(next);
        // Текст обновляем сразу, даже в фокусе: значение задано не набором с клавиатуры.
        reload();
        select(text().length(), text().length());
    }

    @Override
    protected String externalText() {
        return format(current());
    }

    @Override
    protected boolean commitText(String text) {
        Float v = parse(text);
        if (v == null) {
            return false;
        }
        value.set(clamp(v));
        return true;
    }

    @Override
    protected boolean onKey(int key, int modifiers) {
        float k = (modifiers & Keys.MOD_SHIFT) != 0 ? 10f : 1f;
        if (key == Keys.UP) {
            nudge(k);
            return true;
        }
        if (key == Keys.DOWN) {
            nudge(-k);
            return true;
        }
        return false;
    }

    @Override
    protected int prefixColor(int textColor) {
        return scrubbing ? style.get(Styles.ACCENT_COLOR) : Color.mulAlpha(textColor, 0.55f);
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.PRESS && scrub && isEditable()
                && ((PointerEvent) event).button() == Keys.MOUSE_LEFT && overPrefix(((PointerEvent) event).localX())) {
            if (isDirty()) {
                commit();
            }
            scrubbing = true;
            scrubStartX = ((PointerEvent) event).x();
            scrubStartValue = current();
            event.consume();
            return;
        }
        if (event.type() == PointerEvent.DRAG && scrubbing) {
            float dx = ((PointerEvent) event).x() - scrubStartX;
            float perPixel = step > 0f ? step : 0.01f;
            float v = scrubStartValue + Math.round(dx / 2f) * perPixel;
            set(v);
            event.consume();
            return;
        }
        if (event.type() == PointerEvent.RELEASE && scrubbing) {
            scrubbing = false;
        }
        if (event.type() == ScrollEvent.SCROLL && isFocused() && isEditable()) {
            float dy = ((ScrollEvent) event).deltaY();
            if (dy != 0f) {
                nudge(Math.signum(dy));
                event.consume();
                event.stopPropagation();
            }
            return;
        }
        super.handleEvent(event);
    }
}
