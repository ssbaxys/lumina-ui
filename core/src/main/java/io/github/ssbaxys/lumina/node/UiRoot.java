package io.github.ssbaxys.lumina.node;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.Animator;
import io.github.ssbaxys.lumina.event.CharEvent;
import io.github.ssbaxys.lumina.event.EventType;
import io.github.ssbaxys.lumina.event.FocusEvent;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Theme;

import java.util.ArrayList;
import java.util.List;

/**
 * Корень интерфейса: владеет деревом, темой, аниматором, фокусом и наведением.
 * Не знает о Minecraft: экран загрузчика просто пересылает сюда ввод и вызывает {@link #render}.
 */
public final class UiRoot {
    private final UiElement content;
    private final Animator animator = new Animator();
    private Theme theme;
    private int themeRevision;

    private float width;
    private float height;
    private long lastFrameNanos = -1L;

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
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public void resize(float width, float height) {
        if (width != this.width || height != this.height) {
            this.width = width;
            this.height = height;
            content.invalidateLayout();
        }
    }

    public void dispose() {
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
        content.render(canvas, partialTick);
    }

    /** Анимации + раскладка. Отдельно от render, чтобы можно было тестировать без Canvas. */
    public void tick(float dt) {
        if (theme.revision() != themeRevision) {
            // Тему поменяли на месте (токен, стиль класса): перетекаем в новые значения.
            themeRevision = theme.revision();
            content.refreshStyleTree(true);
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
    }

    // ================================================================ мышь

    public boolean pointerMove(float x, float y) {
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
        mouseX = x;
        mouseY = y;
        layoutIfNeeded();
        updateHover();
        UiElement target = hovered;
        UiElement focusTarget = target;
        while (focusTarget != null && !focusTarget.isFocusable()) {
            focusTarget = focusTarget.parent();
        }
        setFocus(focusTarget);
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
        mouseX = x;
        mouseY = y;
        updateHover();
        return hovered != null && dispatch(hovered, new ScrollEvent(x, y, deltaX, deltaY));
    }

    public UiElement hovered() {
        return hovered;
    }

    private void updateHover() {
        UiElement hit = mouseX < 0f ? null : content.hitTest(mouseX, mouseY);
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
    }

    private static void setStateChain(UiElement from, int bits, boolean on) {
        for (UiElement e = from; e != null; e = e.parent()) {
            e.setState(bits, on);
        }
    }

    // ================================================================ клавиатура и фокус

    public boolean keyDown(int key, int scanCode, int modifiers) {
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
