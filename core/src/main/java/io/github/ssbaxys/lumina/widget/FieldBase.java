package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.event.CharEvent;
import io.github.ssbaxys.lumina.event.FocusEvent;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Основа однострочных полей ввода: каретка, выделение (мышь, двойной клик, Shift+стрелки), буфер
 * обмена, отмена/повтор, горизонтальная прокрутка, фильтры, контекстное меню и анимированная рамка.
 * Значение живёт в сигнале наследника: {@link TextField} — строка, {@link NumberField} — число,
 * {@link ColorField} — цвет. Наследник переводит значение в текст и обратно.
 *
 * <p>Вид — стиль "field" и варианты через класс: {@code "field filled"}, {@code "field underline"},
 * {@code "field pill"} (см. {@link #variant}).</p>
 */
public abstract class FieldBase<S extends FieldBase<S>> extends Widget<S> {
    /** Цвет вспышки ошибки. */
    public static final int ERROR_COLOR = 0xFFF0506E;

    /** Значок в начале поля, рисуется в квадрате {@code size} цветом подписи. */
    @FunctionalInterface
    public interface Glyph {
        void paint(Canvas canvas, float x, float y, float size, int color);
    }

    /** Лупа для поля поиска. */
    public static final Glyph SEARCH = (c, x, y, size, color) -> {
        float r = size * 0.3f;
        float cx = x + r + 0.5f;
        float cy = y + r + 0.5f;
        c.strokeCircle(cx, cy, r, 1.2f, Brush.solid(color));
        float d = r * 0.72f;
        c.drawLine(cx + d, cy + d, x + size - 1f, y + size - 1f, 1.5f, color);
    };

    private static final float GLYPH = 9f;
    private static final float GAP = 4f;
    private static final float BLINK = 0.53f;
    private static final long MULTI_CLICK_NANOS = 400_000_000L;
    private static final int UNDO_LIMIT = 100;
    // Виды правок: подряд идущий ввод (или удаление) склеивается в один шаг отмены.
    private static final int EDIT_NONE = 0;
    private static final int EDIT_TYPE = 1;
    private static final int EDIT_DELETE = 2;
    private static final int EDIT_OTHER = 3;

    private record Snapshot(String text, int caret, int anchor) {
    }

    private final ReadableSignal<?> source;
    private Subscription subscription;

    private String text = "";
    private int caret;
    private int anchor;
    private float scrollX;
    private String lastExternal = "";

    private String placeholder = "";
    private int maxLength = Integer.MAX_VALUE;
    private Predicate<String> filter;
    private IntPredicate charFilter;
    private boolean live;
    private boolean readOnly;
    private boolean password;
    private String prefix;
    private String suffix;
    private Glyph icon;
    private boolean clearButton;
    private Consumer<String> onSubmit;
    private boolean standardMenu = true;
    private final List<ContextMenu.Item> menuItems = new ArrayList<>();

    private final ArrayDeque<Snapshot> undo = new ArrayDeque<>();
    private final ArrayDeque<Snapshot> redo = new ArrayDeque<>();
    private int lastEdit = EDIT_NONE;

    private boolean dragging;
    private int clicks;
    private long lastClickNanos;
    private float lastClickX;
    private long blinkStart = now();

    private AnimatedFloat focusAnim;
    private AnimatedFloat errorAnim;
    private ContextMenu contextMenu;

    /** @param source сигнал значения: при его изменении текст поля обновляется */
    protected FieldBase(ReadableSignal<?> source) {
        super("field");
        this.source = source;
        setFocusable(true);
        layoutParams().width = Dim.px(140);
    }

    // ================================================================ значение ↔ текст

    /** Текст, соответствующий текущему значению сигнала. */
    protected abstract String externalText();

    /** Записать текст в сигнал. {@code false} — текст неверен (значение не меняется). */
    protected abstract boolean commitText(String text);

    /** Загрузить текст из значения (наследники вызывают в конце конструктора). */
    protected final void reload() {
        lastExternal = externalText();
        setBuffer(lastExternal);
    }

    private void onSourceChanged() {
        String ext = externalText();
        // Пока пользователь редактирует, внешнее изменение не затирает его ввод.
        if (!ext.equals(text) && (!isFocused() || text.equals(lastExternal))) {
            setBuffer(ext);
        }
        lastExternal = ext;
    }

    private void setBuffer(String value) {
        text = value == null ? "" : value;
        caret = Math.min(caret, text.length());
        anchor = caret;
        ensureCaretVisible();
    }

    /** Записать текст в значение (Enter, потеря фокуса). Неверный ввод — вспышка ошибки и откат. */
    public boolean commit() {
        boolean ok = commitText(text);
        if (!ok) {
            flashError();
        }
        lastExternal = externalText();
        if (!lastExternal.equals(text)) {
            setBuffer(lastExternal);
        }
        return ok;
    }

    /** Отличается ли текст от значения (есть незаписанный ввод). */
    public boolean isDirty() {
        return !text.equals(externalText());
    }

    // ================================================================ настройка

    /** Вариант оформления: "outlined" (по умолчанию), "filled", "underline", "pill". */
    public S variant(String variant) {
        setStyleClass(variant == null || variant.isEmpty() || variant.equals("outlined") ? "field" : "field " + variant);
        return self();
    }

    public S placeholder(String placeholder) {
        this.placeholder = placeholder == null ? "" : placeholder;
        return self();
    }

    public S maxLength(int maxLength) {
        this.maxLength = Math.max(0, maxLength);
        return self();
    }

    /** Проверка итоговой строки: правка, после которой условие ложно, отклоняется. */
    public S filter(Predicate<String> filter) {
        this.filter = filter;
        return self();
    }

    /** Допустимые символы: остальные отбрасываются при вводе и вставке. */
    public S charFilter(IntPredicate charFilter) {
        this.charFilter = charFilter;
        return self();
    }

    /** {@code true} — значение пишется при каждой правке, иначе только по Enter и потере фокуса. */
    public S live(boolean live) {
        this.live = live;
        return self();
    }

    public S readOnly(boolean readOnly) {
        this.readOnly = readOnly;
        return self();
    }

    /** Скрывать символы звёздочками (копирование при этом запрещено). */
    public S password(boolean password) {
        this.password = password;
        return self();
    }

    /** Подпись перед текстом, например "#" или "W". */
    public S prefix(String prefix) {
        this.prefix = prefix;
        invalidateLayout();
        return self();
    }

    /** Подпись после текста, например "px" или "%". */
    public S suffix(String suffix) {
        this.suffix = suffix;
        invalidateLayout();
        return self();
    }

    public S icon(Glyph icon) {
        this.icon = icon;
        invalidateLayout();
        return self();
    }

    /** Крестик очистки справа, пока поле не пустое. */
    public S clearButton(boolean show) {
        this.clearButton = show;
        return self();
    }

    public S onSubmit(Consumer<String> onSubmit) {
        this.onSubmit = onSubmit;
        return self();
    }

    /** Свой пункт в контекстном меню поля (после стандартных). */
    public S menuItem(String label, Runnable action) {
        return menuItem(ContextMenu.Item.of(label, action));
    }

    public S menuItem(ContextMenu.Item item) {
        menuItems.add(item);
        return self();
    }

    /** Показывать ли стандартные пункты (отмена, буфер обмена, выделить всё). */
    public S standardMenu(boolean show) {
        this.standardMenu = show;
        return self();
    }

    /** Пункт "Сбросить по умолчанию": доступен, когда текст отличается от умолчания. */
    protected final S resetItem(Supplier<String> defaultText) {
        return menuItem(ContextMenu.Item.of("Сбросить по умолчанию", () -> {
            replace(0, text.length(), defaultText.get(), EDIT_OTHER);
            commit();
        }).enabledWhen(() -> isEditable() && !text.equals(defaultText.get())));
    }

    // ================================================================ состояние

    public String text() {
        return text;
    }

    /** Задать текст из кода (с записью в значение); работает и для полей только для чтения. */
    public S setText(String value) {
        String v = value == null ? "" : value;
        if (isEditable()) {
            replace(0, text.length(), v, EDIT_OTHER);
            commit();
        } else if (commitText(v)) {
            reload();
        }
        return self();
    }

    public String placeholderText() {
        return placeholder;
    }

    public int caret() {
        return caret;
    }

    public int selectionStart() {
        return Math.min(caret, anchor);
    }

    public int selectionEnd() {
        return Math.max(caret, anchor);
    }

    public boolean hasSelection() {
        return caret != anchor;
    }

    public String selectedText() {
        return text.substring(selectionStart(), selectionEnd());
    }

    public boolean isEditable() {
        return isEnabled() && !readOnly;
    }

    public boolean canUndo() {
        return !undo.isEmpty();
    }

    public boolean canRedo() {
        return !redo.isEmpty();
    }

    /** Выделить диапазон; каретка встаёт в {@code end}. */
    public void select(int start, int end) {
        anchor = clampIndex(start);
        caret = clampIndex(end);
        lastEdit = EDIT_NONE;
        resetBlink();
        ensureCaretVisible();
    }

    public void selectAll() {
        select(0, text.length());
    }

    // ================================================================ правка

    /** Вставить строку вместо выделения (как ввод с клавиатуры, но отдельным шагом отмены). */
    public boolean insert(String s) {
        return replace(selectionStart(), selectionEnd(), s, EDIT_OTHER);
    }

    public void copy() {
        if (hasSelection() && !password && Lumina.isInitialized()) {
            Lumina.platform().setClipboard(selectedText());
        }
    }

    public void cut() {
        if (hasSelection() && !password && isEditable()) {
            copy();
            replace(selectionStart(), selectionEnd(), "", EDIT_OTHER);
        }
    }

    public void paste() {
        String clip = Lumina.isInitialized() ? Lumina.platform().getClipboard() : null;
        if (clip != null && !clip.isEmpty()) {
            replace(selectionStart(), selectionEnd(), clip, EDIT_OTHER);
        }
    }

    public boolean undo() {
        if (undo.isEmpty() || !isEditable()) {
            return false;
        }
        redo.push(snapshot());
        restore(undo.pop());
        return true;
    }

    public boolean redo() {
        if (redo.isEmpty() || !isEditable()) {
            return false;
        }
        undo.push(snapshot());
        restore(redo.pop());
        return true;
    }

    private Snapshot snapshot() {
        return new Snapshot(text, caret, anchor);
    }

    private void restore(Snapshot s) {
        text = s.text;
        caret = s.caret;
        anchor = s.anchor;
        lastEdit = EDIT_NONE;
        edited();
    }

    /**
     * Заменить диапазон строкой с учётом фильтров и длины.
     *
     * @return была ли правка (отклонённая или урезанная правка вспыхивает ошибкой)
     */
    private boolean replace(int from, int to, String insert, int kind) {
        if (!isEditable()) {
            return false;
        }
        int a = clampIndex(Math.min(from, to));
        int b = clampIndex(Math.max(from, to));
        String clean = sanitize(insert);
        boolean rejected = clean.length() != insert.length();
        int room = maxLength - (text.length() - (b - a));
        if (clean.length() > room) {
            clean = clean.substring(0, Math.max(0, room));
            rejected = true;
        }
        String next = text.substring(0, a) + clean + text.substring(b);
        if (filter != null && !filter.test(next)) {
            flashError();
            return false;
        }
        if (rejected) {
            flashError();
        }
        if (next.equals(text)) {
            return false;
        }
        if (kind == EDIT_OTHER || kind != lastEdit) {
            undo.push(snapshot());
            if (undo.size() > UNDO_LIMIT) {
                undo.removeLast();
            }
        }
        redo.clear();
        lastEdit = kind;
        text = next;
        caret = anchor = a + clean.length();
        edited();
        return true;
    }

    /** Одна строка: переводы строк и табы — в пробелы, прочие управляющие символы и запрещённые — прочь. */
    private String sanitize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\n' || c == '\t') {
                c = ' ';
            } else if (c < 32 || c == 127) {
                continue;
            }
            if (charFilter == null || charFilter.test(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void edited() {
        resetBlink();
        ensureCaretVisible();
        if (live) {
            commitText(text);
        }
    }

    private void deleteRange(int from, int to) {
        if (hasSelection()) {
            replace(selectionStart(), selectionEnd(), "", EDIT_DELETE);
        } else {
            replace(from, to, "", EDIT_DELETE);
        }
    }

    private void submit() {
        commit();
        if (onSubmit != null) {
            onSubmit.accept(text);
        }
    }

    /** Красная вспышка рамки и короткая дрожь: ввод отклонён. */
    public void flashError() {
        if (errorAnim != null) {
            errorAnim.snapTo(1f);
            errorAnim.animateTo(0f);
        }
    }

    // ================================================================ каретка и слова

    private int clampIndex(int i) {
        return Math.max(0, Math.min(text.length(), i));
    }

    private void moveCaret(int to, boolean extend) {
        caret = clampIndex(to);
        if (!extend) {
            anchor = caret;
        }
        lastEdit = EDIT_NONE;
        resetBlink();
        ensureCaretVisible();
    }

    private static int charClass(char c) {
        if (Character.isLetterOrDigit(c) || c == '_') {
            return 2;
        }
        return Character.isWhitespace(c) ? 0 : 1;
    }

    private int wordLeft(int i) {
        if (password) {
            return 0;
        }
        while (i > 0 && charClass(text.charAt(i - 1)) == 0) {
            i--;
        }
        if (i > 0) {
            int k = charClass(text.charAt(i - 1));
            while (i > 0 && charClass(text.charAt(i - 1)) == k) {
                i--;
            }
        }
        return i;
    }

    private int wordRight(int i) {
        int n = text.length();
        if (password) {
            return n;
        }
        if (i < n) {
            int k = charClass(text.charAt(i));
            while (k != 0 && i < n && charClass(text.charAt(i)) == k) {
                i++;
            }
        }
        while (i < n && charClass(text.charAt(i)) == 0) {
            i++;
        }
        return i;
    }

    private void selectWord(int i) {
        if (text.isEmpty()) {
            return;
        }
        if (password) {
            selectAll();
            return;
        }
        int at = Math.min(i, text.length() - 1);
        int k = charClass(text.charAt(at));
        int start = at;
        int end = at + 1;
        while (start > 0 && charClass(text.charAt(start - 1)) == k) {
            start--;
        }
        while (end < text.length() && charClass(text.charAt(end)) == k) {
            end++;
        }
        select(start, end);
    }

    // ================================================================ геометрия

    private static TextMetrics metrics() {
        return Lumina.platform().textMetrics();
    }

    private String shown() {
        return password ? "*".repeat(text.length()) : text;
    }

    private float xOf(int index) {
        return metrics().width(shown().substring(0, index));
    }

    /** Левый край области текста (после значка и префикса). */
    protected float textLeft() {
        float x = padding().left();
        if (icon != null) {
            x += GLYPH + GAP;
        }
        if (prefix != null) {
            x += metrics().width(prefix) + GAP;
        }
        return x;
    }

    protected float textRight() {
        float x = width() - padding().right();
        if (showClear()) {
            x -= GLYPH + GAP;
        }
        if (suffix != null) {
            x -= metrics().width(suffix) + GAP;
        }
        return x;
    }

    /** Попадает ли точка (локальная X) на значок или префикс. */
    protected boolean overPrefix(float localX) {
        return (icon != null || prefix != null) && localX < textLeft() - GAP / 2f;
    }

    private boolean showClear() {
        return clearButton && !text.isEmpty() && isEditable();
    }

    private int indexAt(float localX) {
        String s = shown();
        float x = localX - textLeft() + scrollX;
        if (x <= 0f) {
            return 0;
        }
        TextMetrics m = metrics();
        float prev = 0f;
        for (int i = 1; i <= s.length(); i++) {
            float w = m.width(s.substring(0, i));
            if (x < (prev + w) / 2f) {
                return i - 1;
            }
            prev = w;
        }
        return s.length();
    }

    /** Прокрутить так, чтобы каретка была видна. */
    private void ensureCaretVisible() {
        if (!Lumina.isInitialized()) {
            return;
        }
        float view = textRight() - textLeft();
        if (view <= 0f) {
            return;
        }
        float cx = xOf(caret);
        if (cx - scrollX > view - 1f) {
            scrollX = cx - view + 1f;
        }
        if (cx - scrollX < 0f) {
            scrollX = cx;
        }
        scrollX = Math.max(0f, Math.min(scrollX, Math.max(0f, xOf(text.length()) - view + 1f)));
    }

    /** Текущая прокрутка текста в пикселях. */
    public float scrollOffset() {
        return scrollX;
    }

    private static long now() {
        return Lumina.isInitialized() ? Lumina.platform().nanoTime() : System.nanoTime();
    }

    private void resetBlink() {
        blinkStart = now();
    }

    // ================================================================ жизненный цикл

    @Override
    protected void onAttach() {
        focusAnim = new AnimatedFloat(root().animator(), isFocused() ? 1f : 0f).tween(0.22f, Easing.OUT_CUBIC);
        errorAnim = new AnimatedFloat(root().animator(), 0f).tween(0.6f, Easing.OUT_QUAD).listener(e ->
                setAnimatedStyle(Styles.TRANSLATE_X, e > 0.001f ? (float) Math.sin(e * 6f * Math.PI) * 2f * e : null));
        subscription = source.subscribe(v -> onSourceChanged());
        onSourceChanged();
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
        if (contextMenu != null) {
            contextMenu.close();
            contextMenu = null;
        }
        dragging = false;
    }

    @Override
    protected void onStateChanged() {
        if (focusAnim != null) {
            focusAnim.animateTo(isFocused() ? 1f : 0f);
        }
    }

    @Override
    protected void onLayout() {
        ensureCaretVisible();
    }

    // ================================================================ ввод

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == CharEvent.TYPED) {
            if (isFocused()) {
                char c = ((CharEvent) event).character();
                if (c >= 32 && c != 127) {
                    replace(selectionStart(), selectionEnd(), String.valueOf(c), EDIT_TYPE);
                }
                event.consume();
            }
        } else if (event.type() == KeyEvent.PRESS) {
            if (event.target() == this && isFocused() && keyPressed((KeyEvent) event)) {
                event.consume();
            }
        } else if (event.type() == PointerEvent.PRESS) {
            if (isEnabled()) {
                pointerPressed((PointerEvent) event);
            }
        } else if (event.type() == PointerEvent.DRAG) {
            if (dragging) {
                moveCaret(indexAt(((PointerEvent) event).localX()), true);
                event.consume();
            }
        } else if (event.type() == PointerEvent.RELEASE) {
            dragging = false;
        } else if (event.type() == FocusEvent.GAINED) {
            resetBlink();
            // Фокус с клавиатуры (Tab) выделяет всё; клик сразу же поставит каретку сам.
            selectAll();
        } else if (event.type() == FocusEvent.LOST) {
            dragging = false;
            if (isDirty()) {
                commit();
            }
        }
    }

    private void pointerPressed(PointerEvent e) {
        float lx = e.localX();
        if (e.button() == Keys.MOUSE_LEFT) {
            if (showClear() && lx >= textRight() + (suffix != null ? metrics().width(suffix) + GAP : 0f)) {
                setText("");
                e.consume();
                return;
            }
            long t = now();
            boolean repeat = t - lastClickNanos < MULTI_CLICK_NANOS && Math.abs(lx - lastClickX) < 4f;
            clicks = repeat ? clicks + 1 : 1;
            lastClickNanos = t;
            lastClickX = lx;
            int i = indexAt(lx);
            if (clicks == 2) {
                selectWord(i);
            } else if (clicks >= 3) {
                selectAll();
            } else {
                moveCaret(i, false);
            }
            dragging = clicks == 1;
            e.consume();
        } else if (e.button() == Keys.MOUSE_RIGHT && (standardMenu || !menuItems.isEmpty())) {
            int i = indexAt(lx);
            if (!hasSelection() || i < selectionStart() || i > selectionEnd()) {
                moveCaret(i, false);
            }
            openMenu(e.x(), e.y());
            e.consume();
        }
    }

    /** Клавиши для наследников (стрелки в числовом поле); {@code true} — обработано. */
    protected boolean onKey(int key, int modifiers) {
        return false;
    }

    private boolean keyPressed(KeyEvent e) {
        int key = e.key();
        boolean shift = e.hasModifier(Keys.MOD_SHIFT);
        boolean ctrl = e.hasModifier(Keys.MOD_CONTROL) || e.hasModifier(Keys.MOD_SUPER);
        if (onKey(key, e.modifiers())) {
            return true;
        }
        switch (key) {
            case Keys.LEFT -> {
                if (hasSelection() && !shift && !ctrl) {
                    moveCaret(selectionStart(), false);
                } else {
                    moveCaret(ctrl ? wordLeft(caret) : caret - 1, shift);
                }
            }
            case Keys.RIGHT -> {
                if (hasSelection() && !shift && !ctrl) {
                    moveCaret(selectionEnd(), false);
                } else {
                    moveCaret(ctrl ? wordRight(caret) : caret + 1, shift);
                }
            }
            case Keys.HOME -> moveCaret(0, shift);
            case Keys.END -> moveCaret(text.length(), shift);
            case Keys.BACKSPACE -> deleteRange(ctrl ? wordLeft(caret) : caret - 1, caret);
            case Keys.DELETE -> deleteRange(caret, ctrl ? wordRight(caret) : caret + 1);
            case Keys.ENTER, Keys.KP_ENTER -> submit();
            case Keys.ESCAPE -> {
                // Escape откатывает незаписанный ввод; если откатывать нечего — достаётся экрану.
                if (live || !isDirty()) {
                    return false;
                }
                reload();
            }
            case Keys.TAB -> {
                return false;
            }
            default -> {
                if (ctrl) {
                    switch (key) {
                        case Keys.A -> selectAll();
                        case Keys.C -> copy();
                        case Keys.X -> cut();
                        case Keys.V -> paste();
                        case Keys.Z -> {
                            if (shift) {
                                redo();
                            } else {
                                undo();
                            }
                        }
                        case Keys.Y -> redo();
                        default -> {
                        }
                    }
                }
                // Печатные клавиши не уходят игре (иначе E закроет экран инвентаря).
                return key < Keys.ESCAPE;
            }
        }
        return true;
    }

    private void openMenu(float x, float y) {
        ContextMenu menu = new ContextMenu();
        if (standardMenu) {
            boolean editable = isEditable();
            String clip = Lumina.isInitialized() ? Lumina.platform().getClipboard() : null;
            menu.add(ContextMenu.Item.of("Отменить", this::undo).shortcut("Ctrl+Z").enabled(editable && canUndo()))
                    .add(ContextMenu.Item.of("Повторить", this::redo).shortcut("Ctrl+Y").enabled(editable && canRedo()))
                    .separator()
                    .add(ContextMenu.Item.of("Вырезать", this::cut).shortcut("Ctrl+X")
                            .enabled(editable && hasSelection() && !password))
                    .add(ContextMenu.Item.of("Копировать", this::copy).shortcut("Ctrl+C")
                            .enabled(hasSelection() && !password))
                    .add(ContextMenu.Item.of("Вставить", this::paste).shortcut("Ctrl+V")
                            .enabled(editable && clip != null && !clip.isEmpty()))
                    .separator()
                    .add(ContextMenu.Item.of("Выделить всё", this::selectAll).shortcut("Ctrl+A").enabled(!text.isEmpty()));
        }
        if (!menuItems.isEmpty()) {
            menu.separator();
            menuItems.forEach(menu::add);
        }
        contextMenu = menu.show(root(), x, y);
    }

    // ================================================================ отрисовка

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = metrics();
        float w = Math.max(40f, m.width(placeholder));
        if (icon != null) {
            w += GLYPH + GAP;
        }
        if (prefix != null) {
            w += m.width(prefix) + GAP;
        }
        if (suffix != null) {
            w += m.width(suffix) + GAP;
        }
        setContentSize(w, m.lineHeight());
    }

    /** Цвет значка и префикса (числовое поле подсвечивает его при перетаскивании). */
    protected int prefixColor(int textColor) {
        return Color.mulAlpha(textColor, 0.55f);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        float h = height();
        float lh = m.lineHeight();
        float ty = (h - lh) / 2f + 0.5f;
        int color = style.get(Styles.TEXT_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        boolean shadow = style.get(Styles.TEXT_SHADOW);
        float x = padding().left();
        if (icon != null) {
            icon.paint(canvas, x, (h - GLYPH) / 2f, GLYPH, prefixColor(color));
            x += GLYPH + GAP;
        }
        if (prefix != null) {
            canvas.drawText(prefix, x, ty, prefixColor(color), shadow);
        }
        float left = textLeft();
        float right = textRight();
        if (suffix != null) {
            canvas.drawText(suffix, right + GAP, ty, Color.mulAlpha(color, 0.55f), shadow);
        }
        if (showClear()) {
            float cx = width() - padding().right() - GLYPH / 2f;
            float cy = h / 2f;
            float d = GLYPH * 0.3f;
            int c = Color.mulAlpha(color, 0.6f);
            canvas.drawLine(cx - d, cy - d, cx + d, cy + d, 1.2f, c);
            canvas.drawLine(cx - d, cy + d, cx + d, cy - d, 1.2f, c);
        }

        canvas.save();
        canvas.clipRect(left - 1f, 0f, right - left + 2f, h);
        String s = shown();
        boolean focused = isFocused();
        if (s.isEmpty()) {
            canvas.drawText(placeholder, left, ty, Color.mulAlpha(color, 0.4f), false);
        } else {
            if (focused && hasSelection()) {
                float sx = xOf(selectionStart());
                canvas.fillRect(left + sx - scrollX, ty - 1.5f, xOf(selectionEnd()) - sx, lh + 1f,
                        Color.withAlpha(accent, 0x66));
            }
            canvas.drawText(s, left - scrollX, ty, color, shadow);
        }
        if (focused && isEditable() && ((now() - blinkStart) / 1_000_000_000f / BLINK) % 2f < 1f) {
            canvas.fillRect(left + xOf(caret) - scrollX, ty - 1.5f, 1f, lh + 1f, accent);
        }
        canvas.restore();
    }

    @Override
    protected void paintOverlay(Canvas canvas, float partialTick) {
        float w = width();
        float h = height();
        boolean underline = style.get(Styles.UNDERLINE);
        if (underline) {
            float f = focusAnim != null ? focusAnim.get() : (isFocused() ? 1f : 0f);
            canvas.fillRect(0f, h - 1f, w, 1f, style.get(Styles.BORDER_COLOR));
            if (f > 0.001f) {
                float lw = w * f;
                canvas.fillRect((w - lw) / 2f, h - 2f, lw, 2f, style.get(Styles.ACCENT_COLOR));
            }
        }
        float err = errorAnim != null ? errorAnim.get() : 0f;
        if (err > 0.001f) {
            int red = Color.withAlpha(ERROR_COLOR, err);
            if (underline) {
                canvas.fillRect(0f, h - 2f, w, 2f, red);
            } else {
                canvas.strokeRoundRect(0f, 0f, w, h, style.get(Styles.RADIUS),
                        Math.max(1f, style.get(Styles.BORDER_WIDTH)) + 0.5f, red);
            }
        }
    }
}
