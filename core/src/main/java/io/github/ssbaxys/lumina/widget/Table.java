package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.data.DataView;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Таблица: колонки с заголовками, сортировка по клику на заголовок, «зебра», подсветка строки
 * под мышью и выбор строки. Ячейки рисуются напрямую, без виджета на ячейку, поэтому большие
 * таблицы не создают тысяч элементов. Рисуются только строки, попадающие в видимую область
 * (например, окно {@link ScrollPanel}), поэтому и таблица на тысячи строк в прокрутке не тормозит.
 *
 * <p>С {@link #bind(DataView)} строки берутся из представления данных (поиск, фильтры, страницы),
 * а клик по заголовку сортирует всё представление, а не только видимую страницу.</p>
 *
 * <pre>{@code
 * Table<Player> t = new Table<Player>()
 *     .column("Игрок", 2, Player::name)
 *     .column("Очки", 1, p -> String.valueOf(p.score()), Comparator.comparingInt(Player::score))
 *     .rows(players);
 * }</pre>
 */
public class Table<T> extends Widget<Table<T>> {
    /** Своя отрисовка ячейки (иконка, полоска, цвет). */
    @FunctionalInterface
    public interface CellPainter<T> {
        void paint(Canvas canvas, T row, float x, float y, float w, float h);
    }

    private record Column<T>(String title, float weight, Function<T, String> text, Comparator<T> order,
                             CellPainter<T> painter) {
    }

    private static final float ROW = 16f;
    private static final float HEADER = 18f;

    private final List<Column<T>> columns = new ArrayList<>();
    private final List<T> source = new ArrayList<>();
    private final List<T> shown = new ArrayList<>();
    private final Signal<T> selection = Signal.of(null);
    private int sortColumn = -1;
    private boolean descending;
    private int hoverRow = -1;
    private int hoverHeader = -1;
    private DataView<T> view;
    private ReadableSignal<List<T>> binding;
    private Subscription subscription;
    private int paintedFirst;
    private int paintedLast = -1;

    public Table() {
        super("table");
        setFocusable(true);
    }

    public Table<T> column(String title, float weight, Function<T, String> text) {
        return column(title, weight, text, Comparator.comparing(text, String.CASE_INSENSITIVE_ORDER));
    }

    public Table<T> column(String title, float weight, Function<T, String> text, Comparator<T> order) {
        columns.add(new Column<>(title, weight, text, order, null));
        invalidateLayout();
        return this;
    }

    /** Колонка со своей отрисовкой. */
    public Table<T> column(String title, float weight, CellPainter<T> painter, Comparator<T> order) {
        columns.add(new Column<>(title, weight, r -> "", order, painter));
        invalidateLayout();
        return this;
    }

    public Table<T> rows(List<T> rows) {
        source.clear();
        source.addAll(rows);
        resort();
        return this;
    }

    public List<T> rows() {
        return List.copyOf(shown);
    }

    /** Строки из сигнала (обновляются сами, пока таблица в дереве). */
    public Table<T> bind(ReadableSignal<List<T>> signal) {
        unbind();
        this.binding = signal;
        rows(signal.get());
        if (root() != null) {
            subscription = signal.subscribe(this::rows);
        }
        return this;
    }

    /**
     * Строки текущей страницы представления; сортировка по заголовку уходит в само представление
     * ({@link DataView#sort}), чтобы порядок был общим для всех страниц.
     */
    public Table<T> bind(DataView<T> view) {
        this.view = view;
        return bind(view.visible());
    }

    private void unbind() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    @Override
    protected void onAttach() {
        if (binding != null && subscription == null) {
            rows(binding.get());
            subscription = binding.subscribe(this::rows);
        }
    }

    @Override
    protected void onDetach() {
        unbind();
        hoverRow = -1;
        hoverHeader = -1;
    }

    /** Диапазон строк, нарисованных в последнем кадре (для проверки отсечения), {@code [first, last]}. */
    public int[] paintedRows() {
        return new int[]{paintedFirst, paintedLast};
    }

    /** Выбранная строка (или {@code null}). */
    public Signal<T> selection() {
        return selection;
    }

    /** Сортировать по колонке; повторный вызов с той же колонкой меняет направление. */
    public Table<T> sortBy(int column) {
        if (sortColumn == column) {
            descending = !descending;
        } else {
            sortColumn = column;
            descending = false;
        }
        if (view != null) {
            Comparator<T> c = column < columns.size() ? columns.get(column).order() : null;
            view.sort(c, descending);
            return this;
        }
        resort();
        return this;
    }

    private void resort() {
        shown.clear();
        shown.addAll(source);
        if (sortColumn >= 0 && sortColumn < columns.size() && columns.get(sortColumn).order() != null) {
            Comparator<T> c = columns.get(sortColumn).order();
            shown.sort(descending ? c.reversed() : c);
        }
        invalidateLayout();
    }

    private float totalWeight() {
        float w = 0f;
        for (Column<T> c : columns) {
            w += c.weight();
        }
        return Math.max(0.0001f, w);
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float w = 0f;
        for (Column<T> c : columns) {
            w += m.width(c.title()) + 16f;
        }
        setContentSize(Float.isInfinite(maxWidth) ? w : Math.max(w, maxWidth), HEADER + shown.size() * ROW);
    }

    private int columnAt(float lx) {
        float x = padding().left();
        float inner = width() - padding().horizontal();
        float total = totalWeight();
        for (int i = 0; i < columns.size(); i++) {
            float cw = inner * columns.get(i).weight() / total;
            if (lx >= x && lx < x + cw) {
                return i;
            }
            x += cw;
        }
        return -1;
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (!(event instanceof PointerEvent p)) {
            return;
        }
        float ly = p.localY() - padding().top();
        if (event.type() == PointerEvent.MOVE) {
            hoverHeader = ly < HEADER ? columnAt(p.localX()) : -1;
            int r = (int) Math.floor((ly - HEADER) / ROW);
            hoverRow = ly >= HEADER && r < shown.size() ? r : -1;
        } else if (event.type() == PointerEvent.LEAVE) {
            hoverRow = -1;
            hoverHeader = -1;
        } else if (event.type() == PointerEvent.CLICK && p.button() == Keys.MOUSE_LEFT) {
            if (ly < HEADER) {
                int c = columnAt(p.localX());
                if (c >= 0 && columns.get(c).order() != null) {
                    sortBy(c);
                }
            } else {
                int r = (int) Math.floor((ly - HEADER) / ROW);
                if (r >= 0 && r < shown.size()) {
                    selection.set(shown.get(r));
                }
            }
            event.consume();
        }
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        int text = style.get(Styles.TEXT_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        float left = padding().left();
        float top = padding().top();
        float inner = width() - padding().horizontal();
        float total = totalWeight();
        float ty = (ROW - m.lineHeight()) / 2f + 1f;

        canvas.fillRect(left, top + HEADER - 1f, inner, 1f, Color.withAlpha(text, 0x30));
        float x = left;
        for (int i = 0; i < columns.size(); i++) {
            Column<T> c = columns.get(i);
            float cw = inner * c.weight() / total;
            int hc = i == hoverHeader ? text : Color.withAlpha(text, 0xA0);
            String title = c.title() + (i == sortColumn ? (descending ? " ▼" : " ▲") : "");
            canvas.drawText(title, x + 4f, top + (HEADER - m.lineHeight()) / 2f, hc, false);
            x += cw;
        }

        T selected = selection.peek();
        int first = firstVisibleRow(canvas, left, top + HEADER, inner);
        int last = lastVisibleRow(canvas, left, top + HEADER, inner, first);
        paintedFirst = first;
        paintedLast = last;
        for (int r = first; r <= last; r++) {
            T row = shown.get(r);
            float ry = top + HEADER + r * ROW;
            if (row == selected) {
                canvas.fillRect(left, ry, inner, ROW, Color.withAlpha(accent, 0x50));
            } else if (r == hoverRow) {
                canvas.fillRect(left, ry, inner, ROW, Color.withAlpha(text, 0x18));
            } else if (r % 2 == 1) {
                canvas.fillRect(left, ry, inner, ROW, Color.withAlpha(text, 0x08));
            }
            x = left;
            for (Column<T> c : columns) {
                float cw = inner * c.weight() / total;
                if (c.painter() != null) {
                    c.painter().paint(canvas, row, x + 4f, ry, cw - 8f, ROW);
                } else {
                    String s = fit(m, c.text().apply(row), cw - 8f);
                    canvas.drawText(s, x + 4f, ry + ty, text, false);
                }
                x += cw;
            }
        }
    }

    /**
     * Первая строка, попадающая в видимую область. Видимые строки идут подряд, поэтому хватает двоичного
     * поиска по {@link Canvas#isVisible}: «видно ли что-нибудь от начала таблицы до конца строки r» с ростом
     * r меняется с «нет» на «да» один раз. Клип учитывает все области прокрутки и сдвиги над таблицей.
     */
    private int firstVisibleRow(Canvas canvas, float left, float top, float w) {
        int lo = 0;
        int hi = shown.size();
        while (lo < hi) {
            int mid = (lo + hi) >>> 1;
            if (canvas.isVisible(left, top, w, (mid + 1) * ROW)) {
                hi = mid;
            } else {
                lo = mid + 1;
            }
        }
        return lo;
    }

    /** Последняя видимая строка: «видно ли что-нибудь от начала строки r до конца таблицы» — «да», потом «нет». */
    private int lastVisibleRow(Canvas canvas, float left, float top, float w, int first) {
        int n = shown.size();
        int lo = first;
        int hi = n - 1;
        int ans = first - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (canvas.isVisible(left, top + mid * ROW, w, (n - mid) * ROW)) {
                ans = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        return ans;
    }

    /** Обрезать текст с многоточием под ширину. */
    static String fit(TextMetrics m, String s, float width) {
        if (s == null) {
            return "";
        }
        if (m.width(s) <= width) {
            return s;
        }
        String dots = "…";
        int n = s.length();
        while (n > 0 && m.width(s.substring(0, n)) + m.width(dots) > width) {
            n--;
        }
        return s.substring(0, n) + dots;
    }
}
