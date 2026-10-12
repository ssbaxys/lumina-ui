package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Spring;
import io.github.ssbaxys.lumina.data.DataView;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;

/**
 * Переключатель страниц «« ‹ 1 2 3 … 20 › »»: первая и последняя страницы, соседи текущей и
 * многоточия между ними. Подсветка текущей страницы переезжает пружиной. Страницы в сигнале
 * считаются с нуля, на экране — с единицы. Стрелки влево/вправо, Home/End и колесо тоже листают.
 *
 * <pre>{@code
 * new Paginator(view.page(), view.pageCount());     // в паре с DataView
 * new Paginator(view);                               // то же самое
 * }</pre>
 */
public class Paginator extends Widget<Paginator> {
    private static final float CELL = 16f;
    private static final float GAP = 2f;
    /** Номер ячейки «многоточие». */
    private static final int ELLIPSIS = -1;
    // Служебные ячейки-стрелки.
    private static final int FIRST = -2;
    private static final int PREV = -3;
    private static final int NEXT = -4;
    private static final int LAST = -5;

    private final Signal<Integer> page;
    private final ReadableSignal<Integer> pageCount;
    private int siblings = 1;
    private boolean edgeArrows = true;
    private int[] cells = new int[0];
    private float[] xs = new float[0];
    private float[] ws = new float[0];
    private int hover = -2_000;
    private AnimatedFloat highlightX;
    private AnimatedFloat highlightW;
    private Subscription pageSub;
    private Subscription countSub;

    public Paginator(Signal<Integer> page, ReadableSignal<Integer> pageCount) {
        super("paginator");
        this.page = page;
        this.pageCount = pageCount;
        setFocusable(true);
        rebuild();
    }

    public Paginator(DataView<?> view) {
        this(view.page(), view.pageCount());
    }

    /** Сколько соседей текущей страницы показывать с каждой стороны (по умолчанию 1). */
    public Paginator siblings(int siblings) {
        this.siblings = Math.max(0, siblings);
        rebuild();
        return this;
    }

    /** Показывать ли «« и »» (к первой и последней странице). */
    public Paginator edgeArrows(boolean show) {
        this.edgeArrows = show;
        rebuild();
        return this;
    }

    public int current() {
        Integer p = page.peek();
        return p == null ? 0 : p;
    }

    public int count() {
        Integer c = pageCount.get();
        return c == null ? 1 : Math.max(1, c);
    }

    /** Ячейки как на экране: номера страниц с нуля, {@code -1} — многоточие (стрелки не входят). */
    public int[] visiblePages() {
        return pages(current(), count(), siblings);
    }

    /** Номера страниц для показа: первая, последняя, соседи текущей и многоточия. */
    static int[] pages(int current, int count, int siblings) {
        int slots = 2 * siblings + 5;
        if (count <= slots) {
            int[] all = new int[count];
            for (int i = 0; i < count; i++) {
                all[i] = i;
            }
            return all;
        }
        int left = Math.max(current - siblings, 1);
        int right = Math.min(current + siblings, count - 2);
        boolean leftDots = left > 2;
        boolean rightDots = right < count - 3;
        if (!leftDots) {
            left = 1;
            right = Math.max(right, 2 * siblings + 2);
        }
        if (!rightDots) {
            right = count - 2;
            left = Math.min(left, count - 3 - 2 * siblings);
        }
        int[] out = new int[right - left + 3 + (leftDots ? 1 : 0) + (rightDots ? 1 : 0)];
        int k = 0;
        out[k++] = 0;
        if (leftDots) {
            out[k++] = ELLIPSIS;
        }
        for (int i = left; i <= right; i++) {
            out[k++] = i;
        }
        if (rightDots) {
            out[k++] = ELLIPSIS;
        }
        out[k] = count - 1;
        return out;
    }

    private void go(int p) {
        page.set(Math.max(0, Math.min(count() - 1, p)));
    }

    // ================================================================ раскладка

    private static String label(int cell) {
        return switch (cell) {
            case ELLIPSIS -> "…";
            case FIRST -> "«";
            case PREV -> "‹";
            case NEXT -> "›";
            case LAST -> "»";
            default -> String.valueOf(cell + 1);
        };
    }

    private void rebuild() {
        int[] pages = pages(current(), count(), siblings);
        int extra = edgeArrows ? 4 : 2;
        int[] c = new int[pages.length + extra];
        int k = 0;
        if (edgeArrows) {
            c[k++] = FIRST;
        }
        c[k++] = PREV;
        for (int p : pages) {
            c[k++] = p;
        }
        c[k++] = NEXT;
        if (edgeArrows) {
            c[k] = LAST;
        }
        cells = c;
        xs = new float[c.length];
        ws = new float[c.length];
        if (Lumina.isInitialized()) {
            TextMetrics m = Lumina.platform().textMetrics();
            float x = padding().left();
            for (int i = 0; i < c.length; i++) {
                ws[i] = Math.max(CELL, m.width(label(c[i])) + 8f);
                xs[i] = x;
                x += ws[i] + GAP;
            }
        }
        invalidateLayout();
        moveHighlight(true);
    }

    private int currentCell() {
        int cur = current();
        for (int i = 0; i < cells.length; i++) {
            if (cells[i] == cur) {
                return i;
            }
        }
        return -1;
    }

    private void moveHighlight(boolean animate) {
        int i = currentCell();
        if (highlightX == null || i < 0) {
            return;
        }
        if (animate) {
            highlightX.animateTo(xs[i]);
            highlightW.animateTo(ws[i]);
        } else {
            highlightX.snapTo(xs[i]);
            highlightW.snapTo(ws[i]);
        }
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        float w = 0f;
        for (int i = 0; i < ws.length; i++) {
            w += ws[i] + (i > 0 ? GAP : 0f);
        }
        setContentSize(w, CELL);
    }

    @Override
    protected void onLayout() {
        rebuildPositions();
    }

    /** Отступы могли измениться — пересчитать x ячеек. */
    private void rebuildPositions() {
        float x = padding().left();
        for (int i = 0; i < xs.length; i++) {
            xs[i] = x;
            x += ws[i] + GAP;
        }
        moveHighlight(false);
    }

    @Override
    protected void onAttach() {
        highlightX = new AnimatedFloat(root().animator(), 0f).spring(Spring.SNAPPY);
        highlightW = new AnimatedFloat(root().animator(), CELL).spring(Spring.SNAPPY);
        pageSub = page.subscribe(p -> rebuild());
        countSub = pageCount.subscribe(c -> rebuild());
        rebuild();
        moveHighlight(false);
    }

    @Override
    protected void onDetach() {
        if (pageSub != null) {
            pageSub.close();
            pageSub = null;
        }
        if (countSub != null) {
            countSub.close();
            countSub = null;
        }
    }

    // ================================================================ ввод

    private int cellAt(float lx, float ly) {
        if (ly < padding().top() || ly > padding().top() + CELL) {
            return -1;
        }
        for (int i = 0; i < xs.length; i++) {
            if (lx >= xs[i] && lx < xs[i] + ws[i]) {
                return i;
            }
        }
        return -1;
    }

    private boolean cellEnabled(int cell) {
        int cur = current();
        return switch (cell) {
            case ELLIPSIS -> false;
            case FIRST, PREV -> cur > 0;
            case NEXT, LAST -> cur < count() - 1;
            default -> true;
        };
    }

    private void activate(int cell) {
        int cur = current();
        switch (cell) {
            case FIRST -> go(0);
            case PREV -> go(cur - 1);
            case NEXT -> go(cur + 1);
            case LAST -> go(count() - 1);
            case ELLIPSIS -> {
            }
            default -> go(cell);
        }
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.MOVE) {
            PointerEvent p = (PointerEvent) event;
            int i = cellAt(p.localX(), p.localY());
            hover = i >= 0 ? cells[i] : -2_000;
        } else if (event.type() == PointerEvent.LEAVE) {
            hover = -2_000;
        } else if (event.type() == PointerEvent.CLICK && ((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
            PointerEvent p = (PointerEvent) event;
            int i = cellAt(p.localX(), p.localY());
            if (i >= 0 && isEnabled() && cellEnabled(cells[i])) {
                if (Lumina.isInitialized()) {
                    Lumina.platform().playClickSound();
                }
                activate(cells[i]);
                event.consume();
            }
        } else if (event.type() == KeyEvent.PRESS && event.target() == this) {
            int key = ((KeyEvent) event).key();
            int cur = current();
            switch (key) {
                case Keys.LEFT -> go(cur - 1);
                case Keys.RIGHT -> go(cur + 1);
                case Keys.HOME -> go(0);
                case Keys.END -> go(count() - 1);
                default -> {
                    return;
                }
            }
            event.consume();
        }
    }

    // ================================================================ отрисовка

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        int text = style.get(Styles.TEXT_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        float top = padding().top();
        Radii r = Radii.all(CELL / 2f);
        if (highlightX != null && currentCell() >= 0) {
            canvas.fillRoundRect(highlightX.get(), top, highlightW.get(), CELL, r, Brush.solid(accent));
        }
        int cur = current();
        int onAccent = Color.luminance(accent) > 0.5f ? 0xFF1D2330 : Color.WHITE;
        float ty = top + (CELL - m.lineHeight()) / 2f + 0.5f;
        for (int i = 0; i < cells.length; i++) {
            int cell = cells[i];
            boolean enabled = cellEnabled(cell);
            if (cell == hover && enabled && cell != cur) {
                canvas.fillRoundRect(xs[i], top, ws[i], CELL, r, Brush.solid(Color.withAlpha(text, 0x20)));
            }
            String s = label(cell);
            int color;
            if (cell == cur) {
                color = onAccent;
            } else if (!enabled) {
                color = Color.mulAlpha(text, cell == ELLIPSIS ? 0.6f : 0.3f);
            } else {
                color = cell < 0 ? Color.mulAlpha(text, 0.8f) : text;
            }
            canvas.drawText(s, xs[i] + (ws[i] - m.width(s)) / 2f, ty, color, false);
        }
        if (isFocused() && currentCell() >= 0 && highlightX != null) {
            canvas.strokeRoundRect(highlightX.get() - 1.5f, top - 1.5f, highlightW.get() + 3f, CELL + 3f,
                    Radii.all(CELL / 2f + 1.5f), 1f, Color.withAlpha(accent, 0x80));
        }
    }
}
