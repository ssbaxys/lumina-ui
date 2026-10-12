package io.github.ssbaxys.lumina.node;

import io.github.ssbaxys.lumina.anim.Motion;
import io.github.ssbaxys.lumina.anim.Playback;
import io.github.ssbaxys.lumina.event.EventListener;
import io.github.ssbaxys.lumina.event.EventType;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Layout;
import io.github.ssbaxys.lumina.layout.LayoutParams;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.NineSlice;
import io.github.ssbaxys.lumina.render.Outline;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.ComputedStyle;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.StyleKey;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Узел дерева интерфейса: раскладка, стиль, события и отрисовка.
 *
 * <p>Координаты {@code x, y} — относительно родителя. Флаги упакованы в один int, слушатели
 * событий лежат в массиве по id типа, кэш раскладки сбрасывается только по
 * {@link #invalidateLayout()}. Fluent-API для пользователей — в {@code Widget}.</p>
 */
public abstract class UiElement {
    private static final int F_VISIBLE = 1;
    private static final int F_ENABLED = 1 << 1;
    private static final int F_FOCUSABLE = 1 << 2;
    private static final int F_CLIP = 1 << 3;
    private static final int F_HIT_SELF = 1 << 4;
    private static final int F_NEEDS_MEASURE = 1 << 5;
    private static final int F_NEEDS_ARRANGE = 1 << 6;
    private static final int F_OVERFLOW = 1 << 7;

    private UiElement parent;
    private final ArrayList<UiElement> children = new ArrayList<>(0);
    private final List<UiElement> childrenView = Collections.unmodifiableList(children);
    private UiRoot root;

    private float x;
    private float y;
    private float width;
    private float height;
    private float measuredWidth;
    private float measuredHeight;
    private float contentWidth;
    private float contentHeight;
    private float lastAvailWidth = Float.NaN;
    private float lastAvailHeight = Float.NaN;
    private float layoutSlot;

    private final LayoutParams layoutParams = new LayoutParams();
    private Layout layout;

    private int flags = F_VISIBLE | F_ENABLED | F_HIT_SELF | F_NEEDS_MEASURE | F_NEEDS_ARRANGE;
    private int state;

    private String styleClass;
    private Style inlineStyle = Style.EMPTY;
    private Style effectiveStyle;
    private Theme effectiveTheme;
    private int effectiveRevision;
    protected final ComputedStyle style = new ComputedStyle(this::onStyleChanged);

    private Playback[] playbacks;
    private int playbackCount;

    private EventListener<?>[][] bubbleListeners;
    private EventListener<?>[][] captureListeners;

    private String id;
    private String accessibleLabel;

    protected UiElement(String styleClass) {
        this.styleClass = styleClass;
    }

    // ================================================================ дерево

    public UiElement parent() {
        return parent;
    }

    public List<UiElement> children() {
        return childrenView;
    }

    public UiRoot root() {
        return root;
    }

    public void addChild(UiElement child) {
        addChild(children.size(), child);
    }

    public void addChild(int index, UiElement child) {
        if (child == this || child.isAncestorOrSelfOf(this)) {
            throw new IllegalArgumentException("Cannot add an element to its own subtree");
        }
        if (child.parent != null) {
            child.parent.removeChild(child);
        }
        child.parent = this;
        child.style.setParent(style);
        children.add(index, child);
        if (root != null) {
            child.attach(root);
        }
        invalidateLayout();
    }

    public boolean removeChild(UiElement child) {
        if (child.parent != this) {
            return false;
        }
        children.remove(child);
        if (root != null) {
            child.detach();
        }
        child.parent = null;
        child.style.setParent(null);
        invalidateLayout();
        return true;
    }

    public void clearChildren() {
        for (int i = children.size() - 1; i >= 0; i--) {
            removeChild(children.get(i));
        }
    }

    /**
     * Поднять над соседями: рисуется последним и первым получает мышь. Элемент не отсоединяется
     * от дерева, поэтому нажатие и перетаскивание, начатые на нём, продолжаются.
     */
    public void bringToFront() {
        if (parent != null && parent.children.get(parent.children.size() - 1) != this) {
            parent.children.remove(this);
            parent.children.add(this);
            parent.invalidateLayout();
        }
    }

    public boolean isAncestorOrSelfOf(UiElement other) {
        for (UiElement e = other; e != null; e = e.parent) {
            if (e == this) {
                return true;
            }
        }
        return false;
    }

    final void attach(UiRoot root) {
        this.root = root;
        refreshStyle(false);
        startPendingAnimations();
        onAttach();
        for (int i = 0; i < children.size(); i++) {
            children.get(i).attach(root);
        }
    }

    final void detach() {
        for (int i = 0; i < children.size(); i++) {
            children.get(i).detach();
        }
        stopAnimations();
        onDetach();
        UiRoot r = root;
        root = null;
        if (r != null) {
            r.onDetached(this);
        }
    }

    /** Элемент появился в живом дереве (можно подписываться на сигналы, запускать анимации). */
    protected void onAttach() {
    }

    /** Элемент убран из живого дерева: здесь освобождаются подписки. */
    protected void onDetach() {
    }

    public String id() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Подпись для диктора и вспомогательных технологий: заданная {@link #setAccessibleLabel}, иначе
     * подпись по умолчанию у виджета (текст кнопки, флажка, метки) или {@code null}.
     */
    public String accessibleLabel() {
        return accessibleLabel != null ? accessibleLabel : defaultAccessibleLabel();
    }

    public void setAccessibleLabel(String label) {
        this.accessibleLabel = label;
    }

    /** Подпись, если её не задали явно. Виджеты с текстом возвращают свой текст. */
    protected String defaultAccessibleLabel() {
        return null;
    }

    /** Поиск потомка по id (обход в глубину). */
    public UiElement findById(String id) {
        if (id.equals(this.id)) {
            return this;
        }
        for (int i = 0; i < children.size(); i++) {
            UiElement found = children.get(i).findById(id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    // ================================================================ флаги и состояние

    private boolean flag(int f) {
        return (flags & f) != 0;
    }

    private void setFlag(int f, boolean on) {
        flags = on ? flags | f : flags & ~f;
    }

    public boolean isVisible() {
        return flag(F_VISIBLE);
    }

    public void setVisible(boolean visible) {
        if (visible != isVisible()) {
            setFlag(F_VISIBLE, visible);
            invalidateLayout();
        }
    }

    public boolean isEnabled() {
        return flag(F_ENABLED);
    }

    public void setEnabled(boolean enabled) {
        setFlag(F_ENABLED, enabled);
        setState(State.DISABLED, !enabled);
    }

    public boolean isFocusable() {
        return flag(F_FOCUSABLE) && isVisible() && isEnabled();
    }

    public void setFocusable(boolean focusable) {
        setFlag(F_FOCUSABLE, focusable);
    }

    public boolean clipsChildren() {
        return flag(F_CLIP);
    }

    public void setClipChildren(boolean clip) {
        setFlag(F_CLIP, clip);
    }

    /**
     * Рисует ли элемент (или его дети) за пределами своих границ. По умолчанию нет: тогда целиком
     * невидимое поддерево (за краем экрана или области прокрутки) пропускается без обхода.
     */
    public boolean overflowsBounds() {
        return flag(F_OVERFLOW);
    }

    public void setOverflowsBounds(boolean overflow) {
        setFlag(F_OVERFLOW, overflow);
    }

    /** Может ли сам элемент быть целью мыши (дети проверяются в любом случае). */
    public void setHitTestable(boolean hittable) {
        setFlag(F_HIT_SELF, hittable);
    }

    public int state() {
        return state;
    }

    public boolean hasState(int bits) {
        return (state & bits) == bits;
    }

    public void setState(int bits, boolean on) {
        int next = on ? state | bits : state & ~bits;
        if (next != state) {
            state = next;
            refreshStyle(true);
            onStateChanged();
        }
    }

    protected void onStateChanged() {
    }

    public boolean isFocused() {
        return hasState(State.FOCUSED);
    }

    public void requestFocus() {
        if (root != null) {
            root.setFocus(this);
        }
    }

    // ================================================================ стиль

    public String styleClass() {
        return styleClass;
    }

    /** Класс стиля темы; несколько классов — через пробел ({@code "button danger"}). */
    public void setStyleClass(String styleClass) {
        this.styleClass = styleClass;
        effectiveStyle = null;
        refreshStyle(true);
    }

    public Style inlineStyle() {
        return inlineStyle;
    }

    /** Inline-стиль поверх стиля темы. */
    public void setStyle(Style style) {
        this.inlineStyle = style == null ? Style.EMPTY : style;
        effectiveStyle = null;
        refreshStyle(true);
    }

    public ComputedStyle computedStyle() {
        return style;
    }

    final void refreshStyle(boolean animate) {
        UiRoot r = root;
        if (r == null) {
            return;
        }
        Theme theme = r.theme();
        if (effectiveStyle == null || effectiveTheme != theme || effectiveRevision != theme.revision()) {
            Style s = theme.style(styleClass).merge(theme.resolve(inlineStyle));
            float textScale = r.accessibility().textScale();
            if (textScale != 1f) {
                // Крупный текст: все явные размеры шрифта × множитель, а корню — сам множитель (его наследуют все).
                s = s.mapKey(Styles.FONT_SCALE, v -> v * textScale);
                if (parent == null) {
                    s = Style.builder().set(Styles.FONT_SCALE, textScale).build().merge(s);
                }
            }
            effectiveStyle = s;
            effectiveTheme = theme;
            effectiveRevision = theme.revision();
        }
        boolean transitions = animate && !r.accessibility().reducedMotion();
        style.update(effectiveStyle, state, transitions ? r.animator() : null);
    }

    final void refreshStyleTree(boolean animate) {
        effectiveStyle = null;
        refreshStyle(animate);
        for (int i = 0; i < children.size(); i++) {
            children.get(i).refreshStyleTree(animate);
        }
    }

    private void onStyleChanged(boolean layoutChanged) {
        if (layoutChanged) {
            invalidateLayout();
        }
    }

    // ================================================================ анимации

    /**
     * Анимировать свойство стиля: {@code button.animate(Styles.SCALE).to(1.2f).duration(0.3f).play()}.
     * Значения пишутся в слой поверх стиля и не мешают переходам hover/pressed под ним.
     */
    public <T> StyleAnimation<T> animate(StyleKey<T> key) {
        return new StyleAnimation<>(this, key);
    }

    /** Проиграть движение, привязанное к элементу: остановится само, когда элемент уйдёт из дерева. */
    public Playback play(Motion motion) {
        return play(new Playback(motion));
    }

    public Playback play(Playback playback) {
        if (playbacks == null) {
            playbacks = new Playback[4];
        } else if (playbackCount == playbacks.length) {
            playbacks = Arrays.copyOf(playbacks, playbackCount * 2);
        }
        compactPlaybacks();
        playbacks[playbackCount++] = playback;
        if (root != null) {
            playback.start(root.animator());
        }
        return playback;
    }

    /** Остановить все анимации элемента (значения остаются как есть). */
    public void stopAnimations() {
        for (int i = 0; i < playbackCount; i++) {
            playbacks[i].stop();
            playbacks[i] = null;
        }
        playbackCount = 0;
    }

    /** Значение свойства из слоя анимаций (поверх стиля); {@code null} — убрать. */
    public <T> void setAnimatedStyle(StyleKey<T> key, T value) {
        style.setOverride(key, value);
    }

    public void clearAnimatedStyle(StyleKey<?> key) {
        style.clearOverride(key);
    }

    private void startPendingAnimations() {
        for (int i = 0; i < playbackCount; i++) {
            if (!playbacks[i].isRunning()) {
                playbacks[i].start(root.animator());
            }
        }
    }

    private void compactPlaybacks() {
        if (root == null) {
            return;
        }
        int w = 0;
        for (int i = 0; i < playbackCount; i++) {
            if (playbacks[i].isRunning()) {
                playbacks[w++] = playbacks[i];
            }
        }
        for (int i = w; i < playbackCount; i++) {
            playbacks[i] = null;
        }
        playbackCount = w;
    }

    // ================================================================ раскладка

    public LayoutParams layoutParams() {
        return layoutParams;
    }

    public Layout layout() {
        return layout;
    }

    public void setLayout(Layout layout) {
        this.layout = layout;
        invalidateLayout();
    }

    /** Сбросить кэш раскладки этого элемента и всех предков. */
    public void invalidateLayout() {
        for (UiElement e = this; e != null; e = e.parent) {
            e.flags |= F_NEEDS_MEASURE | F_NEEDS_ARRANGE;
        }
    }

    /** Сбросить кэш раскладки всего поддерева: после смены наследуемых свойств (размер шрифта) у корня. */
    final void invalidateLayoutTree() {
        flags |= F_NEEDS_MEASURE | F_NEEDS_ARRANGE;
        for (int i = 0; i < children.size(); i++) {
            children.get(i).invalidateLayoutTree();
        }
    }

    public Insets padding() {
        return style.get(Styles.PADDING);
    }

    /**
     * Измерить элемент при доступном месте {@code availWidth × availHeight} (может быть бесконечным).
     * Результат кэшируется до {@link #invalidateLayout()} или смены ограничений.
     */
    public final void measure(float availWidth, float availHeight) {
        if (!flag(F_NEEDS_MEASURE)
                && Float.compare(availWidth, lastAvailWidth) == 0
                && Float.compare(availHeight, lastAvailHeight) == 0) {
            return;
        }
        LayoutParams p = layoutParams;
        Insets pad = padding();
        float fixedW = p.width.resolve(availWidth);
        float fixedH = p.height.resolve(availHeight);
        float innerW = Math.max(0f, (Float.isNaN(fixedW) ? availWidth : fixedW) - pad.horizontal());
        float innerH = Math.max(0f, (Float.isNaN(fixedH) ? availHeight : fixedH) - pad.vertical());
        contentWidth = 0f;
        contentHeight = 0f;
        if (layout != null) {
            layout.measure(this, innerW, innerH);
        } else {
            measureContent(innerW, innerH);
        }
        float w = Float.isNaN(fixedW) ? contentWidth + pad.horizontal() : fixedW;
        float h = Float.isNaN(fixedH) ? contentHeight + pad.vertical() : fixedH;
        measuredWidth = p.clampWidth(w);
        measuredHeight = p.clampHeight(h);
        lastAvailWidth = availWidth;
        lastAvailHeight = availHeight;
        setFlag(F_NEEDS_MEASURE, false);
    }

    /** Размер собственного содержимого листового элемента (без padding). Сообщить через {@link #setContentSize}. */
    protected void measureContent(float maxWidth, float maxHeight) {
    }

    public final void setContentSize(float w, float h) {
        contentWidth = w;
        contentHeight = h;
    }

    /** Задать положение и размер (вызывает родительская раскладка) и, если нужно, разложить детей. */
    public final void setBounds(float x, float y, float w, float h) {
        boolean resized = w != width || h != height;
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
        if (resized || flag(F_NEEDS_ARRANGE)) {
            setFlag(F_NEEDS_ARRANGE, false);
            Insets pad = padding();
            float cw = Math.max(0f, w - pad.horizontal());
            float ch = Math.max(0f, h - pad.vertical());
            if (layout != null) {
                layout.arrange(this, pad.left(), pad.top(), cw, ch);
            }
            onLayout();
        }
    }

    /** После раскладки: здесь можно пересчитать кэши, зависящие от размера. */
    protected void onLayout() {
    }

    /** Служебное поле для реализаций {@link Layout}. */
    public final float layoutSlot() {
        return layoutSlot;
    }

    public final void setLayoutSlot(float v) {
        layoutSlot = v;
    }

    public float x() {
        return x;
    }

    public float y() {
        return y;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float measuredWidth() {
        return measuredWidth;
    }

    public float measuredHeight() {
        return measuredHeight;
    }

    public float contentWidth() {
        return contentWidth;
    }

    public float contentHeight() {
        return contentHeight;
    }

    /** Абсолютная координата в системе корня (без учёта масштаба SCALE). */
    public float screenX() {
        return parent == null ? x : parent.screenX() + x;
    }

    public float screenY() {
        return parent == null ? y : parent.screenY() + y;
    }

    // ================================================================ отрисовка

    public final void render(Canvas canvas, float partialTick) {
        if (!isVisible()) {
            return;
        }
        float opacity = style.get(Styles.OPACITY);
        if (opacity <= 0f) {
            return;
        }
        canvas.save();
        canvas.translate(x + style.get(Styles.TRANSLATE_X), y + style.get(Styles.TRANSLATE_Y));
        float scale = style.get(Styles.SCALE);
        float scaleX = scale * style.get(Styles.SCALE_X);
        float scaleY = scale * style.get(Styles.SCALE_Y);
        if (scaleX != 1f || scaleY != 1f) {
            canvas.translate(width / 2f, height / 2f);
            canvas.scale(scaleX, scaleY);
            canvas.translate(-width / 2f, -height / 2f);
        }
        if (opacity < 1f) {
            canvas.multiplyOpacity(opacity);
        }
        BlendMode blend = style.get(Styles.BLEND);
        if (blend != BlendMode.NORMAL) {
            canvas.setBlendMode(blend);
        }
        boolean clip = clipsChildren();
        float m = Math.max(extent(style.get(Styles.SHADOW)), extent(style.get(Styles.GLOW)));
        Outline outline = style.get(Styles.OUTLINE);
        if (!outline.isNone()) {
            m = Math.max(m, outline.offset() + outline.width());
        }
        if (flag(F_OVERFLOW) || canvas.isVisible(-m, -m, width + 2f * m, height + 2f * m)) {
            paintBackground(canvas);
            paintContent(canvas, partialTick);
            if (!children.isEmpty()) {
                if (clip) {
                    canvas.save();
                    canvas.clipRect(0, 0, width, height);
                }
                for (int i = 0, n = children.size(); i < n; i++) {
                    children.get(i).render(canvas, partialTick);
                }
                if (clip) {
                    canvas.restore();
                }
            }
            paintForeground(canvas);
            paintOverlay(canvas, partialTick);
        }
        canvas.restore();
    }

    private static float extent(Shadow s) {
        return s.isNone() ? 0f : s.blur() + s.spread() + Math.max(Math.abs(s.offsetX()), Math.abs(s.offsetY()));
    }

    /** Свечение, тень, фон, рамка и внешняя обводка из стиля. */
    protected void paintBackground(Canvas canvas) {
        Radii radii = style.get(Styles.RADIUS);
        Shadow glow = style.get(Styles.GLOW);
        if (!glow.isNone()) {
            canvas.drawGlow(0, 0, width, height, radii, glow);
        }
        Shadow shadow = style.get(Styles.SHADOW);
        if (!shadow.isNone()) {
            canvas.drawShadow(0, 0, width, height, radii, shadow);
        }
        Brush bg = style.get(Styles.BACKGROUND);
        if (bg != null) {
            canvas.fillRoundRect(0, 0, width, height, radii, bg);
        }
        NineSlice frame = style.get(Styles.NINE_SLICE);
        if (frame != null) {
            frame.draw(canvas, 0, 0, width, height);
        }
        float bw = style.get(Styles.BORDER_WIDTH);
        if (bw > 0f) {
            Brush borderBrush = style.get(Styles.BORDER_BRUSH);
            if (borderBrush != null) {
                canvas.strokeRoundRect(0, 0, width, height, radii, bw, borderBrush);
            } else {
                int bc = style.get(Styles.BORDER_COLOR);
                if (Color.alpha(bc) != 0) {
                    canvas.strokeRoundRect(0, 0, width, height, radii, bw, bc);
                }
            }
        }
        Outline outline = style.get(Styles.OUTLINE);
        if (!outline.isNone()) {
            float d = outline.offset() + outline.width();
            canvas.strokeRoundRect(-d, -d, width + 2f * d, height + 2f * d, radii.expand(d), outline.width(), outline.color());
        }
    }

    /** Заливка {@link Styles#FOREGROUND} поверх детей. */
    protected void paintForeground(Canvas canvas) {
        Brush fg = style.get(Styles.FOREGROUND);
        if (fg != null) {
            canvas.fillRoundRect(0, 0, width, height, style.get(Styles.RADIUS), fg);
        }
    }

    /** Собственное содержимое (до детей). */
    protected void paintContent(Canvas canvas, float partialTick) {
    }

    /** Поверх детей (полосы прокрутки, индикаторы фокуса). */
    protected void paintOverlay(Canvas canvas, float partialTick) {
    }

    // ================================================================ попадание мыши

    /** @param px, py координаты в системе родителя */
    public UiElement hitTest(float px, float py) {
        if (!isVisible()) {
            return null;
        }
        float lx = px - x - style.get(Styles.TRANSLATE_X);
        float ly = py - y - style.get(Styles.TRANSLATE_Y);
        float scale = style.get(Styles.SCALE);
        float scaleX = scale * style.get(Styles.SCALE_X);
        float scaleY = scale * style.get(Styles.SCALE_Y);
        if (scaleX != 1f && scaleX != 0f) {
            lx = (lx - width / 2f) / scaleX + width / 2f;
        }
        if (scaleY != 1f && scaleY != 0f) {
            ly = (ly - height / 2f) / scaleY + height / 2f;
        }
        boolean inside = lx >= 0f && ly >= 0f && lx < width && ly < height;
        if (clipsChildren() && !inside) {
            return null;
        }
        for (int i = children.size() - 1; i >= 0; i--) {
            UiElement hit = children.get(i).hitTest(lx, ly);
            if (hit != null) {
                return hit;
            }
        }
        return inside && flag(F_HIT_SELF) ? this : null;
    }

    // ================================================================ события

    /** Слушатель фазы всплытия. */
    public <E extends UiEvent> UiElement on(EventType<E> type, EventListener<? super E> listener) {
        bubbleListeners = addListener(bubbleListeners, type, listener);
        return this;
    }

    /** Слушатель фазы захвата: вызывается раньше, чем у потомков. */
    public <E extends UiEvent> UiElement onCapture(EventType<E> type, EventListener<? super E> listener) {
        captureListeners = addListener(captureListeners, type, listener);
        return this;
    }

    public <E extends UiEvent> void off(EventType<E> type, EventListener<? super E> listener) {
        removeListener(bubbleListeners, type, listener);
        removeListener(captureListeners, type, listener);
    }

    /** Встроенная обработка событий виджетом (вызывается в фазе всплытия до пользовательских слушателей). */
    protected void handleEvent(UiEvent event) {
    }

    final void dispatchCapture(UiEvent event) {
        invoke(captureListeners, event);
    }

    final void dispatchBubble(UiEvent event) {
        handleEvent(event);
        if (!event.isPropagationStopped()) {
            invoke(bubbleListeners, event);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void invoke(EventListener<?>[][] table, UiEvent event) {
        if (table == null) {
            return;
        }
        int id = event.type().id();
        if (id >= table.length || table[id] == null) {
            return;
        }
        EventListener[] ls = table[id];
        for (EventListener l : ls) {
            l.handle(event);
            if (event.isPropagationStopped()) {
                return;
            }
        }
    }

    private static EventListener<?>[][] addListener(EventListener<?>[][] table, EventType<?> type, EventListener<?> l) {
        int id = type.id();
        if (table == null) {
            table = new EventListener<?>[Math.max(EventType.count(), id + 1)][];
        } else if (id >= table.length) {
            table = Arrays.copyOf(table, Math.max(EventType.count(), id + 1));
        }
        EventListener<?>[] old = table[id];
        if (old == null) {
            table[id] = new EventListener<?>[]{l};
        } else {
            EventListener<?>[] next = Arrays.copyOf(old, old.length + 1);
            next[old.length] = l;
            table[id] = next;
        }
        return table;
    }

    private static void removeListener(EventListener<?>[][] table, EventType<?> type, EventListener<?> l) {
        int id = type.id();
        if (table == null || id >= table.length || table[id] == null) {
            return;
        }
        EventListener<?>[] old = table[id];
        for (int i = 0; i < old.length; i++) {
            if (old[i] == l) {
                EventListener<?>[] next = new EventListener<?>[old.length - 1];
                System.arraycopy(old, 0, next, 0, i);
                System.arraycopy(old, i + 1, next, i, old.length - i - 1);
                table[id] = next.length == 0 ? null : next;
                return;
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + (id != null ? "#" + id : "") + "[" + x + "," + y + " " + width + "x" + height + "]";
    }
}
