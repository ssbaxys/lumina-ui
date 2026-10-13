package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Spring;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Layout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Список на десятки тысяч строк: рисуется и раскладывается только то, что видно. Строки одной высоты,
 * поэтому видимый диапазон считается делением, а не обходом.
 *
 * <p>Два способа рисовать строки:</p>
 * <ul>
 *   <li>{@link RowPainter} — строка рисуется напрямую на Canvas, без элементов вовсе (самое дешёвое);</li>
 *   <li>{@link #rows(Supplier, RowBinder)} — строка — настоящий виджет. Создаётся столько элементов,
 *       сколько строк помещается в окно (+1), и при прокрутке они переиспользуются: в них подставляются
 *       другие записи.</li>
 * </ul>
 *
 * <pre>{@code
 * VirtualList<Item> list = new VirtualList<Item>(18, (c, item, i, x, y, w, h, row) -> {
 *         c.drawItem(item.id(), x + 2, y + 1, 16);
 *         c.drawText(item.name(), x + 22, y + 5, row.textColor(), false);
 *     })
 *     .bind(view.visible())
 *     .onActivate(item -> give(item));
 * list.selection().subscribe(this::preview);
 * }</pre>
 *
 * <p>Прокрутка плавная (пружина), с колесом, ползунком и клавишами: стрелки, PageUp/PageDown, Home/End
 * двигают выбор; Enter и двойной клик — {@link #onActivate}.</p>
 */
public class VirtualList<T> extends Widget<VirtualList<T>> {
    /** Состояние строки при рисовании: выбрана ли, под мышью ли, и цвета из стиля списка. */
    public static final class RowState {
        boolean selected;
        boolean hovered;
        int text;
        int accent;

        public boolean selected() {
            return selected;
        }

        public boolean hovered() {
            return hovered;
        }

        /** Цвет текста строки (на выбранной строке тоже читаемый). */
        public int textColor() {
            return text;
        }

        public int accentColor() {
            return accent;
        }
    }

    /** Отрисовка строки напрямую: {@code (x, y, w, h)} — прямоугольник строки. */
    @FunctionalInterface
    public interface RowPainter<T> {
        void paint(Canvas canvas, T item, int index, float x, float y, float w, float h, RowState row);
    }

    /** Подставить запись в переиспользуемый элемент строки. */
    @FunctionalInterface
    public interface RowBinder<E extends UiElement, T> {
        void bind(E element, T item, int index);
    }

    private static final float SCROLL_STEP = 3f;
    private static final float BAR_WIDTH = 4f;
    private static final long DOUBLE_CLICK_NANOS = 400_000_000L;

    private final float rowHeight;
    private RowPainter<T> painter;
    private Supplier<? extends UiElement> rowFactory;
    private RowBinder<UiElement, T> rowBinder;
    private final List<UiElement> pool = new ArrayList<>();
    private int[] poolIndex = new int[0];
    private Object[] poolItem = new Object[0];

    private List<T> items = List.of();
    private ReadableSignal<List<T>> binding;
    private Subscription bindingSubscription;
    private final Signal<T> selection = Signal.of(null);
    private int selectedIndex = -1;
    private int hoverIndex = -1;
    private int visibleRows = 10;
    private boolean zebra = true;
    private String emptyText = I18n.ru("Пусто");
    private Consumer<T> onActivate;
    private final RowState rowState = new RowState();

    private AnimatedFloat offset;
    private float scrollTarget;
    private boolean draggingBar;
    private float dragStartY;
    private float dragStartOffset;
    private long lastClickNanos;
    private int lastClickIndex = -1;

    /** Список, где строки рисует {@code painter}. */
    public VirtualList(float rowHeight, RowPainter<T> painter) {
        this(rowHeight);
        this.painter = painter;
    }

    /** Список с текстом строки из {@code text} (самый простой случай). */
    public static <T> VirtualList<T> ofText(float rowHeight, Function<? super T, String> text) {
        return new VirtualList<T>(rowHeight, (c, item, i, x, y, w, h, row) -> {
            TextMetrics m = c.textMetrics();
            c.drawText(Table.fit(m, text.apply(item), w - 8f), x + 4f, y + (h - m.lineHeight()) / 2f + 0.5f,
                    row.textColor(), false);
        });
    }

    protected VirtualList(float rowHeight) {
        super("virtual-list");
        this.rowHeight = Math.max(1f, rowHeight);
        setFocusable(true);
        setClipChildren(true);
        setLayout(new RowsLayout());
        // Выбор, заданный извне, находит свою строку.
        selection.subscribe(v -> {
            int i = v == null ? -1 : indexOf(v);
            if (i != selectedIndex) {
                selectedIndex = i;
                invalidatePool();
            }
        });
    }

    // ================================================================ настройка

    /**
     * Строки-виджеты: {@code create} делает пустую строку, {@code bind} подставляет в неё запись.
     * Элементов создаётся не больше, чем строк видно одновременно.
     */
    @SuppressWarnings("unchecked")
    public <E extends UiElement> VirtualList<T> rows(Supplier<E> create, RowBinder<E, T> bind) {
        this.rowFactory = create;
        this.rowBinder = (RowBinder<UiElement, T>) (RowBinder<?, T>) bind;
        clearPool();
        invalidateLayout();
        return this;
    }

    public VirtualList<T> painter(RowPainter<T> painter) {
        this.painter = painter;
        return this;
    }

    /** Данные (список не копируется: после изменения на месте вызовите {@link #refresh()}). */
    public VirtualList<T> items(List<T> items) {
        List<T> next = items == null ? List.of() : items;
        T selected = selection.peek();
        this.items = next;
        // Выбранная запись остаётся выбранной, если она есть в новых данных.
        selectedIndex = selected == null ? -1 : indexOf(selected);
        if (selected != null && selectedIndex < 0) {
            selection.set(null);
        }
        hoverIndex = -1;
        if (scrollTarget > maxScroll()) {
            scrollTo(maxScroll(), false);
        }
        refresh();
        return this;
    }

    public List<T> items() {
        return items;
    }

    /** Данные из сигнала, например {@code DataView.visible()}. */
    public VirtualList<T> bind(ReadableSignal<List<T>> signal) {
        unbind();
        this.binding = signal;
        if (root() != null) {
            subscribe();
        } else {
            items(signal.get());
        }
        return this;
    }

    /** Сколько строк показывать, если высота списка не задана (по умолчанию 10). */
    public VirtualList<T> visibleRows(int rows) {
        this.visibleRows = Math.max(1, rows);
        invalidateLayout();
        return this;
    }

    /** Чередовать фон строк. */
    public VirtualList<T> zebra(boolean zebra) {
        this.zebra = zebra;
        return this;
    }

    /** Подпись, когда список пуст. */
    public VirtualList<T> emptyText(String text) {
        this.emptyText = text == null ? "" : text;
        return this;
    }

    /** Enter или двойной клик по строке. */
    public VirtualList<T> onActivate(Consumer<T> onActivate) {
        this.onActivate = onActivate;
        return this;
    }

    public float rowHeight() {
        return rowHeight;
    }

    /** Перерисовать строки после изменения данных на месте. */
    public void refresh() {
        if (selectedIndex >= items.size()) {
            selectedIndex = -1;
        }
        invalidatePool();
        invalidateLayout();
    }

    // ================================================================ выбор

    /** Выбранная запись (или {@code null}). Можно задавать извне: {@code selection().set(item)}. */
    public Signal<T> selection() {
        return selection;
    }

    public int selectedIndex() {
        return selectedIndex;
    }

    /** Выбрать строку по номеру и прокрутить к ней; -1 снимает выбор. */
    public void select(int index) {
        if (items.isEmpty() || index < 0) {
            selectedIndex = -1;
            selection.set(null);
            return;
        }
        selectedIndex = Math.max(0, Math.min(items.size() - 1, index));
        selection.set(items.get(selectedIndex));
        ensureVisible(selectedIndex, true);
        invalidatePool();
    }

    private int indexOf(T item) {
        // Сначала дешёвая проверка по прежнему месту.
        if (selectedIndex >= 0 && selectedIndex < items.size() && Objects.equals(items.get(selectedIndex), item)) {
            return selectedIndex;
        }
        return items.indexOf(item);
    }

    // ================================================================ прокрутка

    private float viewHeight() {
        return Math.max(0f, height() - padding().vertical());
    }

    public float maxScroll() {
        return Math.max(0f, items.size() * rowHeight - viewHeight());
    }

    public float scrollOffset() {
        return offset == null ? scrollTarget : offset.get();
    }

    public void scrollTo(float y, boolean animate) {
        scrollTarget = Math.max(0f, Math.min(maxScroll(), y));
        if (offset == null) {
            return;
        }
        if (animate) {
            // Дальний прыжок (Home/End по тысячам строк): сразу почти до цели, плавно — последний экран.
            float view = Math.max(rowHeight, viewHeight());
            float distance = scrollTarget - offset.get();
            if (Math.abs(distance) > 3f * view) {
                offset.snapTo(scrollTarget - Math.signum(distance) * view);
            }
            offset.animateTo(scrollTarget);
        } else {
            offset.snapTo(scrollTarget);
        }
    }

    /** Прокрутить так, чтобы строка была видна целиком. */
    public void ensureVisible(int index, boolean animate) {
        float top = index * rowHeight;
        float view = viewHeight();
        if (view <= 0f) {
            scrollTarget = top;
            return;
        }
        if (top < scrollTarget) {
            scrollTo(top, animate);
        } else if (top + rowHeight > scrollTarget + view) {
            scrollTo(top + rowHeight - view, animate);
        }
    }

    /** Первая видимая строка (можно частично). */
    public int firstVisible() {
        return Math.max(0, (int) Math.floor(scrollOffset() / rowHeight));
    }

    /** Последняя видимая строка (включительно), -1 — пусто. */
    public int lastVisible() {
        if (items.isEmpty()) {
            return -1;
        }
        return Math.min(items.size() - 1, (int) Math.ceil((scrollOffset() + viewHeight()) / rowHeight) - 1);
    }

    private int rowAt(float localY) {
        float y = localY - padding().top() + scrollOffset();
        if (localY < padding().top() || localY >= height() - padding().bottom()) {
            return -1;
        }
        int r = (int) Math.floor(y / rowHeight);
        return r >= 0 && r < items.size() ? r : -1;
    }

    // ================================================================ жизненный цикл

    @Override
    protected void onAttach() {
        offset = new AnimatedFloat(root().animator(), scrollTarget).spring(Spring.SNAPPY).listener(v -> positionRows());
        if (binding != null && bindingSubscription == null) {
            subscribe();
        }
    }

    @Override
    protected void onDetach() {
        unbind0();
        if (offset != null) {
            offset.snapTo(offset.target());
        }
        draggingBar = false;
    }

    private void subscribe() {
        items(binding.get());
        bindingSubscription = binding.subscribe(this::items);
    }

    private void unbind() {
        unbind0();
        binding = null;
    }

    private void unbind0() {
        if (bindingSubscription != null) {
            bindingSubscription.close();
            bindingSubscription = null;
        }
    }

    @Override
    protected void onLayout() {
        if (scrollTarget > maxScroll()) {
            scrollTo(maxScroll(), false);
        }
    }

    // ================================================================ строки-виджеты

    private void clearPool() {
        for (UiElement e : pool) {
            removeChild(e);
        }
        pool.clear();
        poolIndex = new int[0];
        poolItem = new Object[0];
    }

    private void invalidatePool() {
        Arrays.fill(poolIndex, -1);
    }

    /** Сколько элементов-строк нужно для текущей высоты. */
    private int poolSize() {
        return items.isEmpty() ? 0 : Math.min(items.size(), (int) Math.ceil(viewHeight() / rowHeight) + 1);
    }

    /** Пул под текущую высоту: недостающие строки создаются, лишние прячутся. */
    private void ensurePool() {
        int need = poolSize();
        while (pool.size() < need) {
            UiElement row = rowFactory.get();
            pool.add(row);
            addChild(row);
        }
        if (poolIndex.length != pool.size()) {
            poolIndex = new int[pool.size()];
            poolItem = new Object[pool.size()];
            Arrays.fill(poolIndex, -1);
        }
    }

    /**
     * Расставить строки-виджеты по текущей прокрутке. Строка {@code i} всегда живёт в элементе
     * {@code i % pool}, поэтому при прокрутке перепривязываются только въехавшие строки.
     */
    private void positionRows() {
        if (rowFactory == null) {
            return;
        }
        int n = pool.size();
        if (n == 0) {
            return;
        }
        Insets pad = padding();
        float w = Math.max(0f, width() - pad.horizontal() - (maxScroll() > 0f ? BAR_WIDTH + 2f : 0f));
        float off = scrollOffset();
        int first = firstVisible();
        int last = lastVisible();
        for (int k = 0; k < n; k++) {
            UiElement row = pool.get(k);
            // Номер строки, которая сейчас попадает в этот элемент.
            int i = first + Math.floorMod(k - first, n);
            if (i > last || i >= items.size()) {
                row.setVisible(false);
                continue;
            }
            T item = items.get(i);
            if (poolIndex[k] != i || poolItem[k] != item) {
                poolIndex[k] = i;
                poolItem[k] = item;
                rowBinder.bind(row, item, i);
                row.setState(State.CHECKED, i == selectedIndex);
            }
            row.setVisible(true);
            row.measure(w, rowHeight);
            row.setBounds(pad.left(), pad.top() + i * rowHeight - off, w, rowHeight);
        }
    }

    /** Строки-виджеты раскладываются сами по прокрутке, а не потоком. */
    private final class RowsLayout implements Layout {
        @Override
        public void measure(UiElement container, float maxWidth, float maxHeight) {
            float h = Math.min(items.size(), visibleRows) * rowHeight;
            if (items.isEmpty()) {
                h = rowHeight * 2f;
            }
            float w = Float.isInfinite(maxWidth) ? 160f : maxWidth;
            container.setContentSize(w, Float.isInfinite(maxHeight) ? h : Math.min(h, maxHeight));
        }

        @Override
        public void arrange(UiElement container, float cx, float cy, float cw, float ch) {
            if (rowFactory != null) {
                ensurePool();
                positionRows();
            }
        }
    }

    // ================================================================ ввод

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == ScrollEvent.SCROLL) {
            if (maxScroll() > 0f) {
                scrollTo(scrollTarget - ((ScrollEvent) event).deltaY() * SCROLL_STEP * rowHeight, true);
                event.consume();
                event.stopPropagation();
            }
        } else if (event.type() == PointerEvent.MOVE) {
            hoverIndex = draggingBar ? -1 : rowAt(((PointerEvent) event).localY());
        } else if (event.type() == PointerEvent.LEAVE) {
            hoverIndex = -1;
        } else if (event.type() == PointerEvent.PRESS) {
            PointerEvent p = (PointerEvent) event;
            if (p.button() != Keys.MOUSE_LEFT) {
                return;
            }
            if (p.localX() >= width() - BAR_WIDTH * 3f && maxScroll() > 0f) {
                draggingBar = true;
                dragStartY = p.y();
                dragStartOffset = scrollTarget;
                event.consume();
                return;
            }
            int r = rowAt(p.localY());
            if (r >= 0) {
                long now = Lumina.isInitialized() ? Lumina.platform().nanoTime() : System.nanoTime();
                boolean dbl = r == lastClickIndex && now - lastClickNanos < DOUBLE_CLICK_NANOS;
                lastClickNanos = now;
                lastClickIndex = r;
                select(r);
                if (dbl && onActivate != null) {
                    onActivate.accept(items.get(r));
                    lastClickIndex = -1;
                }
                event.consume();
            }
        } else if (event.type() == PointerEvent.DRAG && draggingBar) {
            PointerEvent p = (PointerEvent) event;
            float track = height() - thumbHeight();
            if (track > 0f) {
                scrollTo(dragStartOffset + (p.y() - dragStartY) / track * maxScroll(), false);
            }
            event.consume();
        } else if (event.type() == PointerEvent.RELEASE) {
            draggingBar = false;
        } else if (event.type() == KeyEvent.PRESS && event.target() == this) {
            if (keyPressed(((KeyEvent) event).key())) {
                event.consume();
            }
        }
    }

    private boolean keyPressed(int key) {
        if (items.isEmpty()) {
            return false;
        }
        int page = Math.max(1, (int) (viewHeight() / rowHeight) - 1);
        int cur = selectedIndex;
        switch (key) {
            case Keys.DOWN -> select(cur < 0 ? firstVisible() : cur + 1);
            case Keys.UP -> select(cur < 0 ? firstVisible() : cur - 1);
            case Keys.PAGE_DOWN -> select(Math.max(0, cur) + page);
            case Keys.PAGE_UP -> select(Math.max(0, cur) - page);
            case Keys.HOME -> select(0);
            case Keys.END -> select(items.size() - 1);
            case Keys.ENTER, Keys.KP_ENTER, Keys.SPACE -> {
                if (cur < 0 || onActivate == null) {
                    return false;
                }
                onActivate.accept(items.get(cur));
            }
            default -> {
                return false;
            }
        }
        return true;
    }

    // ================================================================ отрисовка

    private float thumbHeight() {
        float view = height();
        float total = items.size() * rowHeight + padding().vertical();
        return Math.max(16f, view * view / Math.max(total, 1f));
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        Insets pad = padding();
        float left = pad.left();
        float top = pad.top();
        float inner = width() - pad.horizontal();
        float view = viewHeight();
        int text = style.get(Styles.TEXT_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        canvas.save();
        canvas.clipRect(left, top, inner, view);
        if (items.isEmpty()) {
            TextMetrics m = canvas.textMetrics();
            canvas.drawText(emptyText, left + (inner - m.width(emptyText)) / 2f, top + (view - m.lineHeight()) / 2f,
                    Color.mulAlpha(text, 0.5f), false);
            canvas.restore();
            return;
        }
        float off = scrollOffset();
        int first = firstVisible();
        int last = lastVisible();
        float rowW = maxScroll() > 0f ? inner - BAR_WIDTH - 2f : inner;
        RowState rs = rowState;
        rs.accent = accent;
        for (int i = first; i <= last; i++) {
            float y = top + i * rowHeight - off;
            boolean sel = i == selectedIndex;
            boolean hov = i == hoverIndex;
            if (sel) {
                canvas.fillRoundRect(left, y, rowW, rowHeight, Radii.all(3f), Brush.solid(Color.withAlpha(accent, 0x55)));
            } else if (hov) {
                canvas.fillRoundRect(left, y, rowW, rowHeight, Radii.all(3f), Brush.solid(Color.withAlpha(text, 0x18)));
            } else if (zebra && i % 2 == 1) {
                canvas.fillRect(left, y, rowW, rowHeight, Color.withAlpha(text, 0x08));
            }
            if (painter != null) {
                rs.selected = sel;
                rs.hovered = hov;
                rs.text = text;
                painter.paint(canvas, items.get(i), i, left, y, rowW, rowHeight, rs);
            }
        }
        canvas.restore();
    }

    @Override
    protected void paintOverlay(Canvas canvas, float partialTick) {
        float max = maxScroll();
        if (max <= 0f) {
            return;
        }
        float th = thumbHeight();
        float ty = (height() - th) * (scrollOffset() / max);
        boolean active = hasState(State.HOVERED) || draggingBar;
        int text = style.get(Styles.TEXT_COLOR);
        canvas.fillRoundRect(width() - BAR_WIDTH - 2f, ty + 2f, BAR_WIDTH, th - 4f, Radii.all(BAR_WIDTH / 2f),
                Brush.solid(Color.withAlpha(text, active ? 0xB0 : 0x50)));
        if (isFocused() && selectedIndex >= 0) {
            float y = padding().top() + selectedIndex * rowHeight - scrollOffset();
            if (y + rowHeight > 0f && y < height()) {
                canvas.save();
                canvas.clipRect(0f, padding().top(), width(), viewHeight());
                canvas.strokeRoundRect(padding().left(), y, width() - padding().horizontal() - BAR_WIDTH - 2f, rowHeight,
                        Radii.all(3f), 1f, Color.withAlpha(style.get(Styles.ACCENT_COLOR), 0xC0));
                canvas.restore();
            }
        }
    }
}
