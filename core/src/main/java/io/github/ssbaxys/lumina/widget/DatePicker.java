package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

/**
 * Поле даты: показывает дату ({@code 12.10.2026}), нажатие открывает {@link CalendarView} во всплывающем
 * слое под полем (над ним, если внизу нет места). Выбор дня записывает дату в сигнал и закрывает
 * календарь; Escape и клик мимо закрывают без изменений. С клавиатуры: Enter, пробел или Alt+↓
 * открывают, в календаре стрелки двигают день, PageUp/PageDown — месяц.
 *
 * <pre>{@code
 * Signal<LocalDate> date = Signal.of(LocalDate.now());
 * new DatePicker(date).range(LocalDate.now(), null);   // только будущие даты
 * }</pre>
 *
 * Стили: "select" (поле, как у выпадающего списка), всплывающий календарь — "dropdown calendar".
 */
public class DatePicker extends Widget<DatePicker> {
    private static final DateTimeFormatter DEFAULT_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final float GLYPH = 10f;

    private final Signal<LocalDate> value;
    private DateTimeFormatter format = DEFAULT_FORMAT;
    private String placeholder = "дд.мм.гггг";
    private LocalDate min;
    private LocalDate max;
    private LocalDate today;
    private Consumer<LocalDate> onChange;
    private CalendarView popup;
    private Subscription subscription;

    public DatePicker(Signal<LocalDate> value) {
        super("select");
        this.value = value;
        setFocusable(true);
    }

    public Signal<LocalDate> value() {
        return value;
    }

    /** Формат даты в поле (по умолчанию {@code dd.MM.yyyy}). */
    public DatePicker format(DateTimeFormatter format) {
        this.format = format == null ? DEFAULT_FORMAT : format;
        invalidateLayout();
        return this;
    }

    public DatePicker placeholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
        invalidateLayout();
        return this;
    }

    /** Допустимые даты (включительно); {@code null} — без границы. */
    public DatePicker range(LocalDate min, LocalDate max) {
        this.min = min;
        this.max = max;
        return this;
    }

    /** «Сегодня» для подсветки в календаре (по умолчанию — системная дата). */
    public DatePicker today(LocalDate today) {
        this.today = today;
        return this;
    }

    public DatePicker onChange(Consumer<LocalDate> onChange) {
        this.onChange = onChange;
        return this;
    }

    public String text() {
        LocalDate d = value.peek();
        return d == null ? "" : format.format(d);
    }

    // ================================================================ календарь

    public boolean isOpen() {
        return popup != null && popup.root() != null;
    }

    /** Открытый календарь или {@code null}. */
    public CalendarView calendar() {
        return isOpen() ? popup : null;
    }

    public void toggle() {
        if (isOpen()) {
            close();
        } else {
            open();
        }
    }

    public void open() {
        UiRoot root = root();
        if (root == null || isOpen() || !isEnabled()) {
            return;
        }
        CalendarView cal = new CalendarView(value) {
            @Override
            protected void onDetach() {
                super.onDetach();
                onClosed(this);
            }
        }.range(min, max).today(today);
        cal.setStyleClass("dropdown calendar");
        cal.setFocusable(false);
        cal.onPick(d -> {
            close();
            if (onChange != null) {
                onChange.accept(d);
            }
        });
        popup = cal;
        float sx = screenX();
        float sy = screenY();
        float below = sy + height() + 2f;
        root.showPopup(cal, sx, below);
        float h = cal.height();
        boolean above = below + h > root.height() && sy - 2f - h >= 0f;
        float x = sx + cal.width() > root.width() ? Math.max(0f, sx + width() - cal.width()) : sx;
        root.movePopup(cal, x, above ? sy - 2f - h : below);
        setState(Select.OPEN, true);
        // Появление: прозрачность и раскрытие от края у поля.
        new AnimatedFloat(root.animator(), 0f).tween(0.16f, Easing.OUT_CUBIC).listener(t -> {
            float s = 0.9f + 0.1f * t;
            cal.setAnimatedStyle(Styles.OPACITY, t);
            cal.setAnimatedStyle(Styles.SCALE_Y, s);
            cal.setAnimatedStyle(Styles.TRANSLATE_Y, (above ? 1f : -1f) * (1f - s) * cal.height() / 2f);
        }).animateTo(1f);
        cal.setAnimatedStyle(Styles.OPACITY, 0f);
    }

    public void close() {
        if (isOpen()) {
            popup.root().closePopup(popup);
        }
    }

    /** Календарь закрылся (выбором, Escape, кликом мимо или из кода). */
    private void onClosed(CalendarView closed) {
        if (closed == popup) {
            setState(Select.OPEN, false);
        }
    }

    // ================================================================ поле

    @Override
    protected void onAttach() {
        subscription = value.subscribe(v -> invalidateLayout());
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
        close();
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.PRESS) {
            if (((PointerEvent) event).button() == Keys.MOUSE_LEFT && isEnabled()) {
                toggle();
                event.consume();
            }
        } else if (event.type() == KeyEvent.PRESS && event.target() == this && !isOpen()) {
            KeyEvent k = (KeyEvent) event;
            int key = k.key();
            if (key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.SPACE
                    || (key == Keys.DOWN && k.hasModifier(Keys.MOD_ALT))) {
                open();
                event.consume();
            }
        }
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float w = Math.max(m.width(placeholder), m.width(format.format(LocalDate.of(2000, 12, 28))));
        setContentSize(w + 6f + GLYPH, m.lineHeight());
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        int color = style.get(Styles.TEXT_COLOR);
        float x = padding().left();
        float ty = (height() - m.lineHeight()) / 2f + 0.5f;
        LocalDate d = value.peek();
        if (d == null) {
            canvas.drawText(placeholder, x, ty, Color.mulAlpha(color, 0.45f), false);
        } else {
            canvas.drawText(format.format(d), x, ty, color, style.get(Styles.TEXT_SHADOW));
        }
        // Значок календаря: листок с двумя «колечками» и строкой.
        float gx = width() - padding().right() - GLYPH;
        float gy = (height() - GLYPH) / 2f + 0.5f;
        int gc = Color.mulAlpha(isOpen() ? style.get(Styles.ACCENT_COLOR) : color, 0.75f);
        canvas.strokeRoundRect(gx, gy + 1f, GLYPH, GLYPH - 1f, Radii.all(1.5f), 1f, gc);
        canvas.fillRect(gx, gy + 3f, GLYPH, 1f, gc);
        canvas.fillRect(gx + 2.5f, gy, 1f, 2.5f, gc);
        canvas.fillRect(gx + GLYPH - 3.5f, gy, 1f, 2.5f, gc);
        canvas.fillRect(gx + 2.5f, gy + 5.5f, 2f, 2f, gc);
    }
}
