package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.anim.Spring;
import io.github.ssbaxys.lumina.event.CharEvent;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Выпадающий список: поле с текущим значением и стрелкой; нажатие открывает список вариантов во
 * всплывающем слое {@link UiRoot} (появляется с анимацией, не влезает вниз — открывается вверх).
 * Длинный список прокручивается колесом, можно включить строку поиска ({@link #searchable}),
 * у пунктов бывают иконки предметов ({@link #icons}). Значение — в {@link Signal}.
 *
 * <p>Клавиатура: закрытый список — Enter, пробел или Alt+↓ открывают, ↑/↓ меняют значение на месте,
 * буква прыгает к варианту на эту букву; открытый — ↑/↓, PageUp/PageDown, Home/End, Enter выбирает,
 * Escape закрывает.</p>
 *
 * <pre>{@code
 * Signal<String> ore = Signal.of("minecraft:diamond");
 * Select.of(ore, "minecraft:diamond", "minecraft:emerald", "minecraft:gold_ingot")
 *         .labels(id -> names.get(id))
 *         .icons(id -> id)
 *         .searchable(true);
 * }</pre>
 *
 * Стили: "select" (поле), "dropdown" (карточка списка: фон, текст, акцент подсвеченного пункта).
 */
public class Select<T> extends Widget<Select<T>> {
    /** Состояние поля, пока список открыт. */
    public static final int OPEN = State.FIRST_CUSTOM;
    private static final float ROW_H = 18f;
    private static final float ICON = 16f;
    private static final float GAP = 5f;
    private static final float ARROW_W = 12f;
    private static final float APPEAR_SCALE = 0.9f;

    private final Signal<T> value;
    private final List<T> options;
    private Function<? super T, String> labeler = String::valueOf;
    private Function<? super T, String> iconer;
    private boolean searchable;
    private String placeholder = "";
    private String searchPlaceholder = "Поиск…";
    private String emptyText = "Ничего не найдено";
    private int maxRows = 8;
    private Consumer<? super T> onChange;

    private Dropdown dropdown;
    private AnimatedFloat arrow;
    private Subscription subscription;

    public Select(Signal<T> value, List<T> options) {
        super("select");
        this.value = value;
        this.options = List.copyOf(options);
        setFocusable(true);
    }

    @SafeVarargs
    public static <T> Select<T> of(Signal<T> value, T... options) {
        return new Select<>(value, Arrays.asList(options));
    }

    // ================================================================ настройка

    public Signal<T> value() {
        return value;
    }

    public List<T> options() {
        return options;
    }

    /** Подпись варианта (по умолчанию {@code String.valueOf}). */
    public Select<T> labels(Function<? super T, String> labeler) {
        this.labeler = labeler;
        invalidateLayout();
        return this;
    }

    /** Иконка варианта — id предмета игры ({@code "minecraft:diamond"}) или {@code null}. */
    public Select<T> icons(Function<? super T, String> itemIds) {
        this.iconer = itemIds;
        invalidateLayout();
        return this;
    }

    /** Строка поиска над списком: ввод отбирает варианты, в подписи которых есть этот текст. */
    public Select<T> searchable(boolean searchable) {
        this.searchable = searchable;
        return this;
    }

    public Select<T> searchPlaceholder(String text) {
        this.searchPlaceholder = text == null ? "" : text;
        return this;
    }

    /** Текст в пустом поле (значение {@code null}). */
    public Select<T> placeholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
        invalidateLayout();
        return this;
    }

    /** Сообщение, когда поиск ничего не нашёл. */
    public Select<T> emptyText(String text) {
        this.emptyText = text == null ? "" : text;
        return this;
    }

    /** Сколько строк видно без прокрутки. */
    public Select<T> maxRows(int rows) {
        this.maxRows = Math.max(1, rows);
        return this;
    }

    /** Вызывается, когда пользователь выбрал вариант (мышью или клавиатурой). */
    public Select<T> onChange(Consumer<? super T> onChange) {
        this.onChange = onChange;
        return this;
    }

    public String labelOf(T option) {
        return option == null ? "" : Objects.requireNonNullElse(labeler.apply(option), "");
    }

    private String iconOf(T option) {
        return option == null || iconer == null ? null : iconer.apply(option);
    }

    // ================================================================ открытие

    public boolean isOpen() {
        return dropdown != null && dropdown.root() != null;
    }

    /** Открытый список (для тестов и тонкой настройки) или {@code null}. */
    public UiElement dropdown() {
        return isOpen() ? dropdown : null;
    }

    /** Строка поиска открытого списка или {@code null}. */
    public TextField searchField() {
        return isOpen() ? dropdown.search : null;
    }

    /** Варианты, которые сейчас видны в открытом списке (после поиска). */
    public List<T> visibleOptions() {
        if (!isOpen()) {
            return List.of();
        }
        List<T> out = new ArrayList<>(dropdown.list.count);
        for (int i = 0; i < dropdown.list.count; i++) {
            out.add(options.get(dropdown.list.filtered[i]));
        }
        return Collections.unmodifiableList(out);
    }

    /** Подсвеченный (мышью или стрелками) вариант открытого списка или {@code null}. */
    public T activeOption() {
        if (!isOpen() || dropdown.list.active < 0) {
            return null;
        }
        return options.get(dropdown.list.filtered[dropdown.list.active]);
    }

    public void toggle() {
        if (isOpen()) {
            close();
        } else {
            open();
        }
    }

    /** Открыть список под полем (или над ним, если внизу нет места). */
    public void open() {
        UiRoot root = root();
        if (root == null || isOpen() || options.isEmpty() || !isEnabled()) {
            return;
        }
        dropdown = new Dropdown();
        float sx = screenX();
        float sy = screenY();
        float below = sy + height() + 2f;
        root.showPopup(dropdown, sx, below);
        float dw = dropdown.width();
        float dh = dropdown.height();
        float x = sx + dw > root.width() ? Math.max(0f, sx + width() - dw) : sx;
        float y = below;
        if (below + dh > root.height() && sy - 2f - dh >= 0f) {
            y = sy - 2f - dh;
            dropdown.above = true;
        }
        root.movePopup(dropdown, x, y);
        dropdown.list.revealActive(false);
        setState(OPEN, true);
        if (arrow != null) {
            arrow.animateTo(1f);
        }
        if (dropdown.search != null) {
            root.setFocus(dropdown.search);
        }
    }

    public void close() {
        if (isOpen()) {
            dropdown.root().closePopup(dropdown);
        }
    }

    /** Список закрылся (выбором, Escape, кликом мимо или из кода). */
    private void onClosed(Dropdown closed) {
        if (closed != dropdown) {
            return;
        }
        setState(OPEN, false);
        if (arrow != null) {
            arrow.animateTo(0f);
        }
        if (closed.search != null && closed.search.isFocused() && root() != null) {
            requestFocus();
        }
    }

    /** Выбрать вариант как пользователь: записать значение, закрыть список, сообщить {@link #onChange}. */
    public void pick(T option) {
        close();
        if (Lumina.isInitialized()) {
            Lumina.platform().playClickSound();
        }
        value.set(option);
        if (onChange != null) {
            onChange.accept(option);
        }
    }

    // ================================================================ поле

    private int currentIndex() {
        T v = value.peek();
        // indexOf(null) у неизменяемого списка бросает исключение, поэтому пустое значение — отдельно.
        return v == null ? -1 : options.indexOf(v);
    }

    /** Сдвиг значения на месте (закрытый список, стрелки). */
    private void step(int dir) {
        if (options.isEmpty()) {
            return;
        }
        int i = currentIndex();
        int next = i < 0 ? (dir > 0 ? 0 : options.size() - 1) : Math.max(0, Math.min(options.size() - 1, i + dir));
        if (next != i) {
            value.set(options.get(next));
            if (onChange != null) {
                onChange.accept(options.get(next));
            }
        }
    }

    /** Следующий после {@code from} вариант, подпись которого начинается с символа {@code c}. */
    private static int nextStartingWith(char c, int from, int count, java.util.function.IntFunction<String> label) {
        String p = String.valueOf(c).toLowerCase(Locale.ROOT);
        for (int k = 1; k <= count; k++) {
            int i = Math.floorMod(from + k, count);
            if (label.apply(i).toLowerCase(Locale.ROOT).startsWith(p)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onAttach() {
        arrow = new AnimatedFloat(root().animator(), isOpen() ? 1f : 0f).tween(0.18f, Easing.OUT_CUBIC);
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
            switch (k.key()) {
                case Keys.ENTER, Keys.KP_ENTER, Keys.SPACE -> open();
                case Keys.DOWN, Keys.UP -> {
                    if (k.hasModifier(Keys.MOD_ALT)) {
                        open();
                    } else {
                        step(k.key() == Keys.DOWN ? 1 : -1);
                    }
                }
                case Keys.HOME -> step(-options.size());
                case Keys.END -> step(options.size());
                default -> {
                    return;
                }
            }
            event.consume();
        } else if (event.type() == CharEvent.TYPED && event.target() == this) {
            char c = ((CharEvent) event).character();
            if (c <= ' ') {
                return;
            }
            if (isOpen()) {
                dropdown.list.jump(c);
            } else if (searchable) {
                open();
                if (dropdown != null && dropdown.search != null) {
                    dropdown.search.insert(String.valueOf(c));
                }
            } else {
                int i = nextStartingWith(c, currentIndex(), options.size(), k -> labelOf(options.get(k)));
                if (i >= 0) {
                    value.set(options.get(i));
                    if (onChange != null) {
                        onChange.accept(options.get(i));
                    }
                }
            }
            event.consume();
        }
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float w = m.width(placeholder);
        boolean anyIcon = false;
        for (T o : options) {
            w = Math.max(w, m.width(labelOf(o)));
            anyIcon |= iconOf(o) != null;
        }
        if (anyIcon) {
            w += ICON + GAP;
        }
        setContentSize(w + GAP + ARROW_W, Math.max(m.lineHeight(), anyIcon ? ICON - 4f : 0f));
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        float h = height();
        float x = padding().left();
        float right = width() - padding().right();
        int color = style.get(Styles.TEXT_COLOR);
        T current = value.peek();
        String icon = iconOf(current);
        if (icon != null) {
            canvas.drawItem(icon, x, (h - ICON) / 2f, ICON);
            x += ICON + GAP;
        }
        float textRight = right - ARROW_W - GAP;
        float ty = (h - m.lineHeight()) / 2f + 0.5f;
        if (current == null) {
            canvas.drawText(ellipsize(placeholder, textRight - x, m), x, ty, Color.mulAlpha(color, 0.45f), false);
        } else {
            canvas.drawText(ellipsize(labelOf(current), textRight - x, m), x, ty, color, style.get(Styles.TEXT_SHADOW));
        }
        // Стрелка-«галочка»: при открытии переворачивается.
        float t = arrow != null ? arrow.get() : (isOpen() ? 1f : 0f);
        float cx = right - ARROW_W / 2f;
        float cy = h / 2f;
        float d = 1.6f * (1f - 2f * t);
        int ac = Color.mulAlpha(color, 0.75f);
        canvas.drawLine(cx - 3.5f, cy - d, cx, cy + d, 1.3f, ac);
        canvas.drawLine(cx, cy + d, cx + 3.5f, cy - d, 1.3f, ac);
    }

    /** Обрезать строку многоточием по ширине. */
    static String ellipsize(String s, float max, TextMetrics m) {
        if (s == null || m.width(s) <= max) {
            return s;
        }
        int n = s.length();
        while (n > 0 && m.width(s.substring(0, n) + "…") > max) {
            n--;
        }
        return s.substring(0, n) + "…";
    }

    // ================================================================ всплывающий список

    /** Карточка всплывающего списка: строка поиска (необязательная) и список вариантов. */
    private final class Dropdown extends UiElement {
        final TextField search;
        final Options list;
        boolean above;
        private AnimatedFloat appear;
        private Subscription querySub;

        Dropdown() {
            super("dropdown");
            setLayout(FlexLayout.column().gap(4).align(Align.STRETCH));
            layoutParams().minWidth = Select.this.width();
            Options options = new Options();
            list = options;
            if (searchable) {
                Signal<String> query = Signal.of("");
                search = new TextField(query).placeholder(searchPlaceholder).icon(FieldBase.SEARCH)
                        .standardMenu(false).width(Dim.FILL);
                search.onSubmit(t -> options.pickActive());
                querySub = query.subscribe(options::filter);
                addChild(search);
            } else {
                search = null;
            }
            addChild(options);
        }

        @Override
        protected void onAttach() {
            appear = new AnimatedFloat(root().animator(), 0f).tween(0.16f, Easing.OUT_CUBIC).listener(this::applyAppear);
            applyAppear(0f);
            appear.animateTo(1f);
        }

        /** Появление: прозрачность и раскрытие по вертикали от края у поля. */
        private void applyAppear(float t) {
            float s = APPEAR_SCALE + (1f - APPEAR_SCALE) * t;
            setAnimatedStyle(Styles.OPACITY, t);
            setAnimatedStyle(Styles.SCALE_Y, s);
            setAnimatedStyle(Styles.TRANSLATE_Y, (above ? 1f : -1f) * (1f - s) * height() / 2f);
        }

        @Override
        protected void onLayout() {
            if (appear != null) {
                applyAppear(appear.get());
            }
        }

        @Override
        protected void onDetach() {
            if (querySub != null) {
                querySub.close();
                querySub = null;
            }
            onClosed(this);
        }

        @Override
        protected void handleEvent(UiEvent event) {
            if (event.type() != KeyEvent.PRESS) {
                return;
            }
            int key = ((KeyEvent) event).key();
            boolean typing = search != null;
            switch (key) {
                case Keys.DOWN -> list.move(1);
                case Keys.UP -> list.move(-1);
                case Keys.PAGE_DOWN -> list.move(maxRows);
                case Keys.PAGE_UP -> list.move(-maxRows);
                case Keys.HOME -> list.moveTo(0);
                case Keys.END -> list.moveTo(list.count - 1);
                case Keys.ENTER, Keys.KP_ENTER -> list.pickActive();
                case Keys.SPACE -> {
                    if (typing) {
                        return;
                    }
                    list.pickActive();
                }
                case Keys.TAB -> close();
                default -> {
                    return;
                }
            }
            event.consume();
        }
    }

    /** Сам список: рисует только видимые строки, прокрутка пружиной, полоса прокрутки справа. */
    private final class Options extends UiElement {
        int[] filtered = new int[0];
        int count;
        int active = -1;
        private AnimatedFloat scroll;
        private float scrollTarget;
        private final boolean anyIcon;

        Options() {
            super(null);
            boolean icons = false;
            for (T o : options) {
                icons |= iconOf(o) != null;
            }
            anyIcon = icons;
            filter("");
            active = Math.max(-1, indexInFiltered(currentIndex()));
            if (active < 0 && count > 0) {
                active = 0;
            }
        }

        private int indexInFiltered(int option) {
            for (int i = 0; i < count; i++) {
                if (filtered[i] == option) {
                    return i;
                }
            }
            return -1;
        }

        void filter(String query) {
            String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
            int keep = active >= 0 && active < count ? filtered[active] : -1;
            if (filtered.length < options.size()) {
                filtered = new int[options.size()];
            }
            count = 0;
            for (int i = 0; i < options.size(); i++) {
                if (q.isEmpty() || labelOf(options.get(i)).toLowerCase(Locale.ROOT).contains(q)) {
                    filtered[count++] = i;
                }
            }
            int again = indexInFiltered(keep);
            active = again >= 0 ? again : (count > 0 ? 0 : -1);
            setScroll(0f, false);
            revealActive(false);
        }

        private float maxScroll() {
            return Math.max(0f, count * ROW_H - height());
        }

        private void setScroll(float y, boolean animate) {
            scrollTarget = Math.max(0f, Math.min(maxScroll(), y));
            if (scroll == null) {
                return;
            }
            if (animate) {
                scroll.animateTo(scrollTarget);
            } else {
                scroll.snapTo(scrollTarget);
            }
        }

        private float scrollY() {
            return scroll != null ? scroll.get() : scrollTarget;
        }

        void revealActive(boolean animate) {
            if (active < 0 || height() <= 0f) {
                return;
            }
            float top = active * ROW_H;
            if (top < scrollTarget) {
                setScroll(top, animate);
            } else if (top + ROW_H > scrollTarget + height()) {
                setScroll(top + ROW_H - height(), animate);
            }
        }

        void move(int delta) {
            if (count == 0) {
                return;
            }
            moveTo(active < 0 ? (delta > 0 ? 0 : count - 1) : active + delta);
        }

        void moveTo(int index) {
            if (count == 0) {
                return;
            }
            active = Math.max(0, Math.min(count - 1, index));
            revealActive(true);
        }

        void jump(char c) {
            int i = nextStartingWith(c, active, count, k -> labelOf(options.get(filtered[k])));
            if (i >= 0) {
                moveTo(i);
            }
        }

        void pickActive() {
            if (active >= 0 && active < count) {
                pick(options.get(filtered[active]));
            }
        }

        private int rowAt(float ly) {
            int i = (int) Math.floor((ly + scrollY()) / ROW_H);
            return ly >= 0f && ly < height() && i >= 0 && i < count ? i : -1;
        }

        @Override
        protected void onAttach() {
            scroll = new AnimatedFloat(root().animator(), scrollTarget).spring(Spring.SNAPPY);
        }

        @Override
        protected void onLayout() {
            if (scrollTarget > maxScroll()) {
                setScroll(maxScroll(), false);
            }
        }

        @Override
        protected void measureContent(float maxWidth, float maxHeight) {
            TextMetrics m = Lumina.platform().textMetrics();
            float w = m.width(emptyText);
            for (T o : options) {
                w = Math.max(w, m.width(labelOf(o)));
            }
            w += 12f + 8f + (anyIcon ? ICON + GAP : 0f) + 6f;
            int rows = Math.max(1, Math.min(maxRows, options.size()));
            setContentSize(w, rows * ROW_H);
        }

        @Override
        protected void handleEvent(UiEvent event) {
            if (event.type() == PointerEvent.MOVE) {
                int i = rowAt(((PointerEvent) event).localY());
                if (i >= 0) {
                    active = i;
                }
            } else if (event.type() == ScrollEvent.SCROLL) {
                if (maxScroll() > 0f) {
                    setScroll(scrollTarget - ((ScrollEvent) event).deltaY() * ROW_H * 2f, true);
                }
                event.consume();
                event.stopPropagation();
            } else if (event.type() == PointerEvent.PRESS) {
                event.consume();
            } else if (event.type() == PointerEvent.CLICK && ((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
                int i = rowAt(((PointerEvent) event).localY());
                if (i >= 0) {
                    pick(options.get(filtered[i]));
                }
                event.consume();
            }
        }

        @Override
        protected void paintContent(Canvas canvas, float partialTick) {
            TextMetrics m = canvas.textMetrics();
            UiElement card = parent();
            int text = card.computedStyle().get(Styles.TEXT_COLOR);
            int accent = card.computedStyle().get(Styles.ACCENT_COLOR);
            int onAccent = Color.luminance(accent) > 0.5f ? 0xFF1D2330 : Color.WHITE;
            float w = width();
            float h = height();
            canvas.save();
            canvas.clipRect(0f, 0f, w, h);
            if (count == 0) {
                float tw = m.width(emptyText);
                canvas.drawText(emptyText, (w - tw) / 2f, (Math.min(h, ROW_H * 2f) - m.lineHeight()) / 2f,
                        Color.mulAlpha(text, 0.5f), false);
            }
            float sy = scrollY();
            int first = Math.max(0, (int) Math.floor(sy / ROW_H));
            int last = Math.min(count - 1, (int) Math.floor((sy + h) / ROW_H));
            float barRoom = maxScroll() > 0f ? 6f : 0f;
            int selected = currentIndex();
            for (int i = first; i <= last; i++) {
                T option = options.get(filtered[i]);
                float y = i * ROW_H - sy;
                boolean on = i == active;
                if (on) {
                    canvas.fillRoundRect(0f, y + 1f, w - barRoom, ROW_H - 2f, Radii.all(4f), Brush.solid(accent));
                }
                int c = on ? onAccent : text;
                float x = 4f;
                if (filtered[i] == selected) {
                    // Галочка у текущего значения.
                    float cy = y + ROW_H / 2f;
                    canvas.drawLine(x, cy, x + 2.5f, cy + 2.5f, 1.4f, on ? onAccent : accent);
                    canvas.drawLine(x + 2.5f, cy + 2.5f, x + 7f, cy - 3f, 1.4f, on ? onAccent : accent);
                }
                x += 12f;
                String icon = iconOf(option);
                if (icon != null) {
                    canvas.drawItem(icon, x, y + (ROW_H - ICON) / 2f, ICON);
                }
                if (anyIcon) {
                    x += ICON + GAP;
                }
                canvas.drawText(ellipsize(labelOf(option), w - barRoom - x - 4f, m), x,
                        y + (ROW_H - m.lineHeight()) / 2f + 0.5f, c, false);
            }
            canvas.restore();
            float max = maxScroll();
            if (max > 0f) {
                float total = count * ROW_H;
                float th = Math.max(12f, h * h / total);
                float ty = (h - th) * (sy / max);
                canvas.fillRoundRect(w - 4f, ty, 3f, th, Radii.all(1.5f), Brush.solid(Color.mulAlpha(text, 0.35f)));
            }
        }
    }
}
