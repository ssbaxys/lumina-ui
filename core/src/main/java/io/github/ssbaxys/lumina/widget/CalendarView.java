package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.function.Consumer;

/**
 * Календарь на месяц: заголовок с переключением месяцев, дни недели с понедельника, сетка 6×7.
 * Выбранный день — заливка акцентом, сегодня — обводка, дни соседних месяцев приглушены, дни вне
 * {@link #range} недоступны. Месяц листается стрелками в заголовке, колесом и PageUp/PageDown
 * (с коротким сдвигом сетки), дни — стрелками клавиатуры, Enter выбирает.
 *
 * <p>Сам по себе — встроенный календарь; во всплывающем окне его показывает {@link DatePicker}.</p>
 *
 * Стиль "calendar": TEXT_COLOR, ACCENT_COLOR, PADDING.
 */
public class CalendarView extends Widget<CalendarView> {
    static final String[] MONTHS = {"Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
            "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"};
    private static final String[] WEEKDAYS = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
    private static final float CELL_W = 22f;
    private static final float CELL_H = 18f;
    private static final float HEADER = 20f;
    private static final float WEEK_ROW = 14f;
    private static final float ARROW_HIT = 18f;
    private static final int PREV = -2;
    private static final int NEXT = -3;

    private final Signal<LocalDate> value;
    private YearMonth month;
    private LocalDate cursor;
    private LocalDate today;
    private boolean todayFixed;
    private LocalDate min;
    private LocalDate max;
    private Consumer<LocalDate> onPick;
    private int hover = -1;
    private boolean keyboard;
    private AnimatedFloat slide;
    private Subscription subscription;

    public CalendarView(Signal<LocalDate> value) {
        super("calendar");
        this.value = value;
        LocalDate v = value.peek();
        this.today = LocalDate.now();
        this.cursor = v != null ? v : today;
        this.month = YearMonth.from(cursor);
        setFocusable(true);
    }

    public Signal<LocalDate> value() {
        return value;
    }

    /** Показанный месяц. */
    public YearMonth month() {
        return month;
    }

    /** Показать месяц (без анимации). */
    public CalendarView month(YearMonth month) {
        this.month = month;
        return this;
    }

    /** День под клавиатурным курсором. */
    public LocalDate cursor() {
        return cursor;
    }

    /** Допустимые даты (включительно); {@code null} — без границы. */
    public CalendarView range(LocalDate min, LocalDate max) {
        this.min = min;
        this.max = max;
        return this;
    }

    /** «Сегодня» для подсветки (по умолчанию — системная дата; удобно для тестов). */
    public CalendarView today(LocalDate today) {
        this.today = today;
        this.todayFixed = today != null;
        if (this.today == null) {
            this.today = LocalDate.now();
        }
        return this;
    }

    /** Вызывается при выборе дня мышью или Enter (после записи в сигнал). */
    public CalendarView onPick(Consumer<LocalDate> onPick) {
        this.onPick = onPick;
        return this;
    }

    public boolean isAllowed(LocalDate d) {
        return (min == null || !d.isBefore(min)) && (max == null || !d.isAfter(max));
    }

    /** Перелистнуть на {@code delta} месяцев (с анимацией). */
    public void shiftMonth(int delta) {
        if (delta == 0) {
            return;
        }
        month = month.plusMonths(delta);
        if (!YearMonth.from(cursor).equals(month)) {
            cursor = month.atDay(Math.min(cursor.getDayOfMonth(), month.lengthOfMonth()));
        }
        if (slide != null) {
            slide.snapTo(delta > 0 ? 1f : -1f);
            slide.animateTo(0f);
        }
    }

    /** Выбрать день: запись в сигнал, переход к его месяцу, {@link #onPick}. */
    public void pick(LocalDate day) {
        if (day == null || !isAllowed(day)) {
            return;
        }
        cursor = day;
        int delta = (day.getYear() - month.getYear()) * 12 + day.getMonthValue() - month.getMonthValue();
        shiftMonth(delta);
        value.set(day);
        if (Lumina.isInitialized()) {
            Lumina.platform().playClickSound();
        }
        if (onPick != null) {
            onPick.accept(day);
        }
    }

    /** Курсор на день; если он в другом месяце — перелистнуть. */
    private void moveCursor(LocalDate d) {
        cursor = d;
        keyboard = true;
        YearMonth ym = YearMonth.from(d);
        if (!ym.equals(month)) {
            int delta = (ym.getYear() - month.getYear()) * 12 + ym.getMonthValue() - month.getMonthValue();
            month = month.plusMonths(delta);
            if (slide != null) {
                slide.snapTo(delta > 0 ? 1f : -1f);
                slide.animateTo(0f);
            }
        }
    }

    /** Первый день сетки (понедельник не позже 1-го числа). */
    private LocalDate gridStart() {
        LocalDate first = month.atDay(1);
        return first.minusDays(first.getDayOfWeek().getValue() - 1);
    }

    /** Дата ячейки сетки 0..41. */
    public LocalDate dateAt(int cell) {
        return gridStart().plusDays(cell);
    }

    private float gridLeft() {
        return padding().left() + Math.max(0f, (width() - padding().horizontal() - 7f * CELL_W) / 2f);
    }

    private float gridTop() {
        return padding().top() + HEADER + WEEK_ROW;
    }

    /** Ячейка, стрелка заголовка или -1 в локальных координатах. */
    private int hitAt(float lx, float ly) {
        float top = padding().top();
        if (ly >= top && ly < top + HEADER) {
            if (lx < padding().left() + ARROW_HIT) {
                return PREV;
            }
            if (lx >= width() - padding().right() - ARROW_HIT) {
                return NEXT;
            }
            return -1;
        }
        float gx = lx - gridLeft();
        float gy = ly - gridTop();
        if (gx < 0f || gy < 0f || gx >= 7f * CELL_W || gy >= 6f * CELL_H) {
            return -1;
        }
        return (int) (gy / CELL_H) * 7 + (int) (gx / CELL_W);
    }

    @Override
    protected void onAttach() {
        if (!todayFixed) {
            today = LocalDate.now();
        }
        slide = new AnimatedFloat(root().animator(), 0f).tween(0.22f, Easing.OUT_CUBIC);
        subscription = value.subscribe(v -> {
            if (v != null) {
                cursor = v;
                month = YearMonth.from(v);
            }
        });
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
        hover = -1;
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.MOVE) {
            PointerEvent p = (PointerEvent) event;
            hover = hitAt(p.localX(), p.localY());
            keyboard = false;
        } else if (event.type() == PointerEvent.LEAVE && event.target() == this) {
            hover = -1;
        } else if (event.type() == PointerEvent.PRESS) {
            PointerEvent p = (PointerEvent) event;
            if (p.button() == Keys.MOUSE_LEFT && isEnabled()) {
                int hit = hitAt(p.localX(), p.localY());
                if (hit == PREV) {
                    shiftMonth(-1);
                } else if (hit == NEXT) {
                    shiftMonth(1);
                } else if (hit >= 0) {
                    pick(dateAt(hit));
                }
            }
            event.consume();
        } else if (event.type() == ScrollEvent.SCROLL) {
            float dy = ((ScrollEvent) event).deltaY();
            if (dy != 0f) {
                shiftMonth(dy > 0f ? -1 : 1);
            }
            event.consume();
            event.stopPropagation();
        } else if (event.type() == KeyEvent.PRESS && event.target() == this) {
            int key = ((KeyEvent) event).key();
            switch (key) {
                case Keys.LEFT -> moveCursor(cursor.minusDays(1));
                case Keys.RIGHT -> moveCursor(cursor.plusDays(1));
                case Keys.UP -> moveCursor(cursor.minusWeeks(1));
                case Keys.DOWN -> moveCursor(cursor.plusWeeks(1));
                case Keys.PAGE_UP -> moveCursor(cursor.minusMonths(1));
                case Keys.PAGE_DOWN -> moveCursor(cursor.plusMonths(1));
                case Keys.HOME -> moveCursor(cursor.withDayOfMonth(1));
                case Keys.END -> moveCursor(cursor.withDayOfMonth(cursor.lengthOfMonth()));
                case Keys.ENTER, Keys.KP_ENTER, Keys.SPACE -> pick(cursor);
                default -> {
                    return;
                }
            }
            event.consume();
        }
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        setContentSize(7f * CELL_W, HEADER + WEEK_ROW + 6f * CELL_H);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        int text = style.get(Styles.TEXT_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        int onAccent = Color.luminance(accent) > 0.5f ? 0xFF1D2330 : Color.WHITE;
        float s = slide != null ? slide.get() : 0f;
        float left = padding().left();
        float right = width() - padding().right();
        float top = padding().top();
        float lh = m.lineHeight();

        // Заголовок: стрелки и «Месяц Год».
        float hy = top + HEADER / 2f;
        chevron(canvas, left + ARROW_HIT / 2f, hy, -1f, hover == PREV ? text : Color.mulAlpha(text, 0.6f));
        chevron(canvas, right - ARROW_HIT / 2f, hy, 1f, hover == NEXT ? text : Color.mulAlpha(text, 0.6f));
        String title = MONTHS[month.getMonthValue() - 1] + " " + month.getYear();
        float fade = 1f - Math.abs(s);
        canvas.drawText(title, (left + right - m.width(title)) / 2f + s * 10f, hy - lh / 2f + 0.5f,
                Color.mulAlpha(text, 0.4f + 0.6f * fade), false);

        float gl = gridLeft();
        float wy = top + HEADER + (WEEK_ROW - lh) / 2f;
        for (int i = 0; i < 7; i++) {
            String w = WEEKDAYS[i];
            int c = Color.mulAlpha(text, i >= 5 ? 0.45f : 0.6f);
            canvas.drawText(w, gl + i * CELL_W + (CELL_W - m.width(w)) / 2f, wy, c, false);
        }

        // Сетка дней со сдвигом при смене месяца.
        float gt = gridTop();
        float shift = s * 14f;
        LocalDate start = gridStart();
        LocalDate selected = value.peek();
        canvas.save();
        canvas.multiplyOpacity(0.25f + 0.75f * fade);
        for (int cell = 0; cell < 42; cell++) {
            LocalDate d = start.plusDays(cell);
            float x = gl + (cell % 7) * CELL_W + shift;
            float y = gt + (cell / 7) * CELL_H;
            boolean inMonth = d.getMonthValue() == month.getMonthValue();
            boolean allowed = isAllowed(d);
            boolean isSelected = d.equals(selected);
            boolean hot = cell == hover || (keyboard && d.equals(cursor));
            Radii r = Radii.all(5f);
            if (isSelected) {
                canvas.fillRoundRect(x + 1f, y + 1f, CELL_W - 2f, CELL_H - 2f, r, Brush.solid(accent));
            } else if (hot && allowed) {
                canvas.fillRoundRect(x + 1f, y + 1f, CELL_W - 2f, CELL_H - 2f, r, Brush.solid(Color.withAlpha(text, 0x22)));
            }
            if (d.equals(today) && !isSelected) {
                canvas.strokeRoundRect(x + 1f, y + 1f, CELL_W - 2f, CELL_H - 2f, r, 1f, accent);
            }
            String label = String.valueOf(d.getDayOfMonth());
            int c = isSelected ? onAccent : text;
            if (!inMonth) {
                c = Color.mulAlpha(c, 0.35f);
            }
            if (!allowed) {
                c = Color.mulAlpha(c, 0.3f);
            }
            canvas.drawText(label, x + (CELL_W - m.width(label)) / 2f, y + (CELL_H - lh) / 2f + 0.5f, c, false);
        }
        canvas.restore();
    }

    /** Стрелка ‹ или › из двух отрезков. */
    private static void chevron(Canvas canvas, float cx, float cy, float dir, int color) {
        float w = 2.5f * dir;
        canvas.drawLine(cx + w, cy - 4f, cx - w, cy, 1.4f, color);
        canvas.drawLine(cx - w, cy, cx + w, cy + 4f, 1.4f, color);
    }
}
