package io.github.ssbaxys.lumina.node;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.a11y.Accessibility;
import io.github.ssbaxys.lumina.anim.Animator;
import io.github.ssbaxys.lumina.event.CharEvent;
import io.github.ssbaxys.lumina.event.EventType;
import io.github.ssbaxys.lumina.event.FocusEvent;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Breakpoint;
import io.github.ssbaxys.lumina.layout.Breakpoints;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorFilter;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.sound.UiSound;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Tokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Корень интерфейса: владеет деревом, темой, аниматором, фокусом и наведением.
 * Не знает о Minecraft: экран загрузчика просто пересылает сюда ввод и вызывает {@link #render}.
 *
 * <p>Поверх содержимого — слой всплывающих окон ({@link #showPopup}): контекстные меню, подсказки,
 * выпадающие списки. Они рисуются после содержимого и первыми получают мышь и клавиатуру.</p>
 *
 * <p>Корень также отвечает за то, что действует на весь интерфейс сразу: масштаб ({@link #setScale}),
 * класс ширины для адаптивной раскладки ({@link #breakpoint()}), настройки доступности
 * ({@link #accessibility()}) и звуки наведения ({@code Styles.HOVER_SOUND}).</p>
 */
public final class UiRoot {
    /** Всплывающее окно: элемент, запрошенная точка и закрывается ли оно кликом мимо. */
    private record Popup(UiElement element, float x, float y, boolean dismissable) {
    }

    private final UiElement content;
    private final List<Popup> popups = new ArrayList<>();
    private final Animator animator = new Animator();
    private Theme theme;
    private int themeRevision;

    private float width;
    private float height;
    private float physicalWidth;
    private float physicalHeight;
    private float scale = 1f;
    private Breakpoints breakpoints = Breakpoints.DEFAULT;
    private final Signal<Breakpoint> breakpoint = Signal.of(Breakpoints.DEFAULT.of(0f));
    private final Accessibility accessibility = new Accessibility();
    private long lastFrameNanos = -1L;
    private long hoverSoundInterval = 70_000_000L;
    private long lastHoverSound = Long.MIN_VALUE;

    private float mouseX = -1f;
    private float mouseY = -1f;
    private UiElement hovered;
    private UiElement pressed;
    private int pressedButton = -1;
    private UiElement focused;

    public UiRoot(UiElement content) {
        this(content, Theme.dark());
    }

    public UiRoot(UiElement content, Theme theme) {
        this.content = content;
        this.theme = theme;
        this.themeRevision = theme.revision();
        accessibility.onChange(this::onAccessibilityChanged);
        content.attach(this);
    }

    public UiElement content() {
        return content;
    }

    public Animator animator() {
        return animator;
    }

    public Theme theme() {
        return theme;
    }

    /** Сменить тему. Свойства с TRANSITION в новой теме плавно перетекают в новые значения. */
    public void setTheme(Theme theme) {
        this.theme = theme;
        this.themeRevision = theme.revision();
        content.refreshStyleTree(true);
        content.invalidateLayout();
        for (Popup p : popups) {
            p.element.refreshStyleTree(true);
            p.element.invalidateLayout();
        }
    }

    /** Ширина интерфейса в его единицах (размер экрана, делённый на {@link #scale()}). */
    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    // ================================================================ масштаб и адаптивность

    public float scale() {
        return scale;
    }

    /**
     * Масштаб всего интерфейса (0.25–4): раскладка идёт в уменьшенных единицах ({@link #width()} = экран /
     * масштаб), отрисовка увеличивается, координаты мыши пересчитываются обратно. Например, 0.75 вмещает больше
     * на маленьком экране, 1.5 — крупнее для плохо видящих.
     */
    public void setScale(float scale) {
        float s = Math.max(0.25f, Math.min(4f, scale));
        if (s != this.scale) {
            this.scale = s;
            applySize();
        }
    }

    /** Класс ширины интерфейса; меняется при изменении размера или масштаба. */
    public ReadableSignal<Breakpoint> breakpoint() {
        return breakpoint;
    }

    /** Текущий класс ширины (без подписки). */
    public Breakpoint currentBreakpoint() {
        return breakpoint.peek();
    }

    public Breakpoints breakpoints() {
        return breakpoints;
    }

    public void setBreakpoints(Breakpoints breakpoints) {
        this.breakpoints = breakpoints == null ? Breakpoints.DEFAULT : breakpoints;
        breakpoint.set(this.breakpoints.of(width));
    }

    // ================================================================ доступность и звук

    /** Настройки доступности этого корня: фильтр цвета, размер текста, без анимаций, рамка фокуса, диктор. */
    public Accessibility accessibility() {
        return accessibility;
    }

    private void onAccessibilityChanged() {
        content.refreshStyleTree(false);
        content.invalidateLayoutTree();
        for (Popup p : popups) {
            p.element.refreshStyleTree(false);
            p.element.invalidateLayoutTree();
        }
    }

    /** Минимальный промежуток между звуками наведения, в секундах (чтобы проведение мышью по списку не трещало). */
    public void setHoverSoundInterval(float seconds) {
        hoverSoundInterval = (long) (Math.max(0f, seconds) * 1_000_000_000L);
    }

    public float hoverSoundInterval() {
        return hoverSoundInterval / 1_000_000_000f;
    }

    /** Положение мыши в координатах корня ({@code -1}, пока мышь не заходила). */
    public float mouseX() {
        return mouseX;
    }

    public float mouseY() {
        return mouseY;
    }

    /** Размер экрана (в GUI-координатах загрузчика); интерфейс получает его с учётом {@link #scale()}. */
    public void resize(float width, float height) {
        physicalWidth = width;
        physicalHeight = height;
        applySize();
    }

    private void applySize() {
        float w = physicalWidth / scale;
        float h = physicalHeight / scale;
        if (w != this.width || h != this.height) {
            this.width = w;
            this.height = h;
            content.invalidateLayout();
        }
        breakpoint.set(breakpoints.of(w));
    }

    public void dispose() {
        closePopups();
        content.detach();
        animator.clear();
        hovered = null;
        pressed = null;
        focused = null;
    }

    // ================================================================ кадр

    /** Продвинуть анимации, разложить при необходимости и нарисовать. */
    public void render(Canvas canvas, float partialTick) {
        long now = Lumina.isInitialized() ? Lumina.platform().nanoTime() : System.nanoTime();
        float dt = lastFrameNanos < 0L ? 0f : Math.min((now - lastFrameNanos) / 1_000_000_000f, 0.1f);
        lastFrameNanos = now;
        tick(dt);
        ColorFilter filter = accessibility.colorFilter();
        ColorFilter previous = canvas.colorFilter();
        if (filter != null) {
            canvas.setColorFilter(filter);
        }
        try {
            boolean scaled = scale != 1f;
            if (scaled) {
                canvas.save();
                canvas.scale(scale, scale);
            }
            content.render(canvas, partialTick);
            renderPopups(canvas, partialTick);
            renderFocusRing(canvas);
            if (scaled) {
                canvas.restore();
            }
        } finally {
            if (filter != null) {
                canvas.setColorFilter(previous);
            }
        }
    }

    /** Рамка вокруг элемента в фокусе (при {@link Accessibility#focusAlwaysVisible()}): акцент с контрастной каймой. */
    private void renderFocusRing(Canvas canvas) {
        UiElement f = focused;
        if (!accessibility.focusAlwaysVisible() || f == null || f.root() != this || !shownChain(f)) {
            return;
        }
        int accent = Color.withAlpha(theme.get(Tokens.ACCENT), 255);
        int halo = Color.readableOn(accent);
        Radii radii = f.computedStyle().get(Styles.RADIUS);
        float x = f.screenX();
        float y = f.screenY();
        float w = f.width();
        float h = f.height();
        float d = 3f;
        canvas.strokeRoundRect(x - d - 1f, y - d - 1f, w + 2f * (d + 1f), h + 2f * (d + 1f), radii.expand(d + 1f), 4f, halo);
        canvas.strokeRoundRect(x - d, y - d, w + 2f * d, h + 2f * d, radii.expand(d), 2f, accent);
    }

    private static boolean shownChain(UiElement e) {
        for (UiElement p = e; p != null; p = p.parent()) {
            if (!p.isVisible()) {
                return false;
            }
        }
        return true;
    }

    /** Нарисовать только слой всплывающих окон (если содержимое рисуется отдельно). */
    public void renderPopups(Canvas canvas, float partialTick) {
        for (int i = 0; i < popups.size(); i++) {
            popups.get(i).element.render(canvas, partialTick);
        }
    }

    /** Анимации + раскладка. Отдельно от render, чтобы можно было тестировать без Canvas. */
    public void tick(float dt) {
        if (theme.revision() != themeRevision) {
            // Тему поменяли на месте (токен, стиль класса): перетекаем в новые значения.
            themeRevision = theme.revision();
            content.refreshStyleTree(true);
            for (Popup p : popups) {
                p.element.refreshStyleTree(true);
            }
        }
        if (!animator.isIdle()) {
            animator.tick(dt);
        }
        layoutIfNeeded();
        updateHover();
    }

    public void layoutIfNeeded() {
        content.measure(width, height);
        content.setBounds(0f, 0f, content.measuredWidth(), content.measuredHeight());
        for (int i = 0; i < popups.size(); i++) {
            layoutPopup(popups.get(i));
        }
    }

    /** Окно своего размера в точке; не влезает — открывается в другую сторону от точки или прижимается к краю. */
    private void layoutPopup(Popup p) {
        UiElement e = p.element;
        e.measure(width, height);
        float w = e.measuredWidth();
        float h = e.measuredHeight();
        e.setBounds(fit(p.x, w, width), fit(p.y, h, height), w, h);
    }

    private static float fit(float at, float size, float limit) {
        if (at + size <= limit) {
            return Math.max(0f, at);
        }
        if (at - size >= 0f) {
            return at - size;
        }
        return Math.max(0f, limit - size);
    }

    // ================================================================ всплывающие окна

    /** Показать всплывающее окно в точке экрана; закрывается кликом мимо и Escape. */
    public void showPopup(UiElement popup, float x, float y) {
        showPopup(popup, x, y, true);
    }

    /**
     * Показать всплывающее окно поверх содержимого. Окно измеряется по своему размеру и не выходит
     * за экран. {@code dismissable = false} — клик мимо его не закрывает (только Escape или код).
     */
    public void showPopup(UiElement popup, float x, float y, boolean dismissable) {
        if (popup.parent() != null) {
            throw new IllegalArgumentException("Popup must not have a parent: " + popup);
        }
        closePopup(popup);
        Popup p = new Popup(popup, x, y, dismissable);
        popups.add(p);
        popup.attach(this);
        layoutPopup(p);
        updateHover();
    }

    /** Закрыть окно; {@code false}, если оно не было открыто. */
    public boolean closePopup(UiElement popup) {
        for (int i = 0; i < popups.size(); i++) {
            if (popups.get(i).element == popup) {
                popups.remove(i);
                release(popup);
                popup.detach();
                updateHover();
                return true;
            }
        }
        return false;
    }

    /** Закрыть все окна. */
    public void closePopups() {
        while (!popups.isEmpty()) {
            closePopup(popups.get(popups.size() - 1).element);
        }
    }

    public boolean isPopupOpen(UiElement popup) {
        return indexOfPopup(popup) >= 0;
    }

    /** Открытые окна снизу вверх. */
    public List<UiElement> popups() {
        List<UiElement> out = new ArrayList<>(popups.size());
        for (Popup p : popups) {
            out.add(p.element);
        }
        return Collections.unmodifiableList(out);
    }

    private int indexOfPopup(UiElement popup) {
        for (int i = 0; i < popups.size(); i++) {
            if (popups.get(i).element == popup) {
                return i;
            }
        }
        return -1;
    }

    /** Лежит ли элемент внутри какого-нибудь окна. */
    private boolean inPopup(UiElement e) {
        for (Popup p : popups) {
            if (p.element.isAncestorOrSelfOf(e)) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(UiElement e, float x, float y) {
        return e.isVisible() && x >= e.x() && y >= e.y() && x < e.x() + e.width() && y < e.y() + e.height();
    }

    /** Снять наведение и нажатие с элементов закрываемого окна, чтобы они не залипли. */
    private void release(UiElement popup) {
        if (hovered != null && popup.isAncestorOrSelfOf(hovered)) {
            for (UiElement e = hovered; e != null; e = e.parent()) {
                e.setState(State.HOVERED, false);
            }
            hovered = null;
        }
        if (pressed != null && popup.isAncestorOrSelfOf(pressed)) {
            setStateChain(pressed, State.PRESSED, false);
            pressed = null;
            pressedButton = -1;
        }
    }

    /** Попадание мыши: сначала окна сверху вниз, затем содержимое. Окно заслоняет то, что под ним. */
    private UiElement hitTest(float x, float y) {
        for (int i = popups.size() - 1; i >= 0; i--) {
            UiElement p = popups.get(i).element;
            UiElement hit = p.hitTest(x, y);
            if (hit != null) {
                return hit;
            }
            if (contains(p, x, y)) {
                return null;
            }
        }
        return content.hitTest(x, y);
    }

    /** Нажатие мимо всех окон закрывает те, что закрываются кликом мимо. */
    private boolean dismissPopupsAt(float x, float y) {
        if (popups.isEmpty() || (hovered != null && inPopup(hovered))) {
            return false;
        }
        for (Popup p : popups) {
            if (contains(p.element, x, y)) {
                return false;
            }
        }
        boolean closed = false;
        for (int i = popups.size() - 1; i >= 0; i--) {
            if (popups.get(i).dismissable) {
                closePopup(popups.get(i).element);
                closed = true;
            }
        }
        return closed;
    }

    // ================================================================ мышь

    public boolean pointerMove(float x, float y) {
        x /= scale;
        y /= scale;
        mouseX = x;
        mouseY = y;
        updateHover();
        boolean consumed = false;
        if (pressed != null) {
            consumed = dispatch(pressed, new PointerEvent(PointerEvent.DRAG, x, y, pressedButton));
        }
        if (hovered != null) {
            consumed |= dispatch(hovered, new PointerEvent(PointerEvent.MOVE, x, y, -1));
        }
        return consumed;
    }

    public boolean pointerDown(float x, float y, int button) {
        x /= scale;
        y /= scale;
        mouseX = x;
        mouseY = y;
        layoutIfNeeded();
        updateHover();
        if (dismissPopupsAt(x, y)) {
            // Клик, закрывший меню, дальше не идёт: иначе он нечаянно нажмёт то, что под меню.
            return true;
        }
        UiElement target = hovered;
        UiElement focusTarget = target;
        while (focusTarget != null && !focusTarget.isFocusable()) {
            focusTarget = focusTarget.parent();
        }
        // Клик по пункту меню не отнимает фокус у поля, которое это меню открыло.
        if (focusTarget != null || target == null || !inPopup(target)) {
            setFocus(focusTarget);
        }
        if (target == null) {
            return false;
        }
        pressed = target;
        pressedButton = button;
        if (button == Keys.MOUSE_LEFT) {
            setStateChain(target, State.PRESSED, true);
        }
        dispatch(target, new PointerEvent(PointerEvent.PRESS, x, y, button));
        return true;
    }

    public boolean pointerUp(float x, float y, int button) {
        x /= scale;
        y /= scale;
        mouseX = x;
        mouseY = y;
        updateHover();
        UiElement p = pressed;
        if (p == null || button != pressedButton) {
            return false;
        }
        pressed = null;
        pressedButton = -1;
        setStateChain(p, State.PRESSED, false);
        dispatch(p, new PointerEvent(PointerEvent.RELEASE, x, y, button));
        if (hovered != null && p.isAncestorOrSelfOf(hovered) && p.root() == this) {
            dispatch(p, new PointerEvent(PointerEvent.CLICK, x, y, button));
        }
        return true;
    }

    public boolean scroll(float x, float y, float deltaX, float deltaY) {
        x /= scale;
        y /= scale;
        mouseX = x;
        mouseY = y;
        updateHover();
        return hovered != null && dispatch(hovered, new ScrollEvent(x, y, deltaX, deltaY));
    }

    public UiElement hovered() {
        return hovered;
    }

    private void updateHover() {
        UiElement hit = mouseX < 0f ? null : hitTest(mouseX, mouseY);
        UiElement old = hovered;
        if (hit == old) {
            return;
        }
        hovered = hit;
        for (UiElement e = old; e != null; e = e.parent()) {
            if (hit == null || !e.isAncestorOrSelfOf(hit)) {
                e.setState(State.HOVERED, false);
                dispatch(e, new PointerEvent(PointerEvent.LEAVE, mouseX, mouseY, -1));
            }
        }
        for (UiElement e = hit; e != null; e = e.parent()) {
            if (old == null || !e.isAncestorOrSelfOf(old)) {
                e.setState(State.HOVERED, true);
                dispatch(e, new PointerEvent(PointerEvent.ENTER, mouseX, mouseY, -1));
            }
        }
        playHoverSound(hit, old);
    }

    /**
     * Звук наведения: у ближайшего к цели элемента со своим {@code Styles.HOVER_SOUND}, только если мышь в него
     * именно вошла (переход между его детьми звука не даёт) и прошло не меньше {@link #hoverSoundInterval()}.
     */
    private void playHoverSound(UiElement hit, UiElement old) {
        for (UiElement e = hit; e != null; e = e.parent()) {
            if (old != null && e.isAncestorOrSelfOf(old)) {
                return;
            }
            UiSound sound = e.computedStyle().get(Styles.HOVER_SOUND);
            if (sound != null) {
                if (!sound.isNone() && e.isEnabled() && Lumina.isInitialized()) {
                    long now = Lumina.platform().nanoTime();
                    if (lastHoverSound == Long.MIN_VALUE || now - lastHoverSound >= hoverSoundInterval) {
                        lastHoverSound = now;
                        sound.play();
                    }
                }
                return;
            }
        }
    }

    private static void setStateChain(UiElement from, int bits, boolean on) {
        for (UiElement e = from; e != null; e = e.parent()) {
            e.setState(bits, on);
        }
    }

    // ================================================================ клавиатура и фокус

    public boolean keyDown(int key, int scanCode, int modifiers) {
        if (!popups.isEmpty()) {
            // Верхнее окно первым получает клавиши (стрелки в меню), Escape его закрывает.
            UiElement top = popups.get(popups.size() - 1).element;
            UiElement target = focused != null && top.isAncestorOrSelfOf(focused) ? focused : top;
            if (dispatch(target, new KeyEvent(KeyEvent.PRESS, key, scanCode, modifiers))) {
                return true;
            }
            if (key == Keys.ESCAPE) {
                closePopup(top);
                return true;
            }
        }
        KeyEvent e = new KeyEvent(KeyEvent.PRESS, key, scanCode, modifiers);
        if (dispatch(focused != null ? focused : content, e)) {
            return true;
        }
        if (key == Keys.TAB) {
            focusNext((modifiers & Keys.MOD_SHIFT) != 0);
            return true;
        }
        return false;
    }

    public boolean keyUp(int key, int scanCode, int modifiers) {
        return dispatch(focused != null ? focused : content, new KeyEvent(KeyEvent.RELEASE, key, scanCode, modifiers));
    }

    public boolean charTyped(char c, int modifiers) {
        return focused != null && dispatch(focused, new CharEvent(c, modifiers));
    }

    public UiElement focused() {
        return focused;
    }

    public void setFocus(UiElement element) {
        if (element == focused) {
            return;
        }
        UiElement old = focused;
        focused = element;
        if (old != null) {
            old.setState(State.FOCUSED, false);
            dispatch(old, new FocusEvent(FocusEvent.LOST));
        }
        if (element != null) {
            element.setState(State.FOCUSED, true);
            dispatch(element, new FocusEvent(FocusEvent.GAINED));
            if (accessibility.narration() && Lumina.isInitialized()) {
                String label = element.accessibleLabel();
                if (label != null && !label.isBlank()) {
                    Lumina.platform().narrate(label);
                }
            }
        }
    }

    /** Tab / Shift+Tab: следующий фокусируемый элемент в порядке дерева. */
    public void focusNext(boolean backwards) {
        List<UiElement> list = new ArrayList<>();
        collectFocusable(content, list);
        if (list.isEmpty()) {
            return;
        }
        int i = list.indexOf(focused);
        int next;
        if (i < 0) {
            next = backwards ? list.size() - 1 : 0;
        } else {
            next = Math.floorMod(i + (backwards ? -1 : 1), list.size());
        }
        setFocus(list.get(next));
    }

    private static void collectFocusable(UiElement e, List<UiElement> out) {
        if (!e.isVisible()) {
            return;
        }
        if (e.isFocusable()) {
            out.add(e);
        }
        for (UiElement c : e.children()) {
            collectFocusable(c, out);
        }
    }

    void onDetached(UiElement e) {
        if (hovered == e) {
            hovered = null;
        }
        if (pressed == e) {
            pressed = null;
        }
        if (focused == e) {
            focused = null;
        }
    }

    // ================================================================ диспетчеризация

    /**
     * Отправить событие: захват от корня к цели, затем всплытие от цели к корню.
     *
     * @return было ли событие помечено как обработанное
     */
    public boolean dispatch(UiElement target, UiEvent event) {
        if (target == null) {
            return false;
        }
        int depth = 0;
        for (UiElement e = target; e != null; e = e.parent()) {
            depth++;
        }
        UiElement[] path = new UiElement[depth];
        int i = 0;
        for (UiElement e = target; e != null; e = e.parent()) {
            path[i++] = e;
        }
        EventType<?> type = event.type();
        for (int k = depth - 1; k >= 0; k--) {
            event.route(target, path[k]);
            path[k].dispatchCapture(event);
            if (event.isPropagationStopped()) {
                return event.isConsumed();
            }
        }
        for (int k = 0; k < depth; k++) {
            event.route(target, path[k]);
            path[k].dispatchBubble(event);
            if (event.isPropagationStopped() || !type.bubbles()) {
                break;
            }
        }
        return event.isConsumed();
    }
}
