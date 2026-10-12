package io.github.ssbaxys.lumina.node;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.input.Gamepad;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.input.NavDirection;

/**
 * Управление интерфейсом с геймпада. Каждый кадр корень опрашивает {@code Platform.pollGamepad()} и:
 * <ul>
 *   <li>крестовина или левый стик — фокус к ближайшему элементу в этом направлении
 *       ({@link UiRoot#focusDirection}); удержание повторяет шаг;</li>
 *   <li>A — нажать элемент в фокусе (Enter, а если он его не обработал — клик в центр);</li>
 *   <li>B — назад: Escape (закрывает всплывающее окно), иначе {@link #onBack} (экран загрузчика закрывается);</li>
 *   <li>бамперы LB/RB — соседняя вкладка ближайшего {@link TabSwitchable} ({@code Tabs}, {@code SideNav}).</li>
 * </ul>
 * Без геймпада ({@code pollGamepad() == null}) ничего не делает.
 */
public final class GamepadNavigator {
    private final UiRoot root;
    private final boolean[] previous = new boolean[Gamepad.BUTTON_COUNT];
    private boolean enabled = true;
    private boolean connected;
    private float deadzone = 0.5f;
    private float repeatDelay = 0.35f;
    private float repeatRate = 0.11f;
    private NavDirection held;
    private float heldTime;
    private float nextRepeat;
    private Runnable onBack;

    GamepadNavigator(UiRoot root) {
        this.root = root;
    }

    public GamepadNavigator enabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            reset();
        }
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Был ли геймпад при последнем опросе. */
    public boolean isConnected() {
        return connected;
    }

    /** Мёртвая зона стика (0..1, по умолчанию 0.5). */
    public GamepadNavigator deadzone(float deadzone) {
        this.deadzone = Math.max(0.05f, Math.min(0.95f, deadzone));
        return this;
    }

    /** Повтор шага при удержании: задержка до первого повтора и промежуток между повторами, в секундах. */
    public GamepadNavigator repeat(float delay, float rate) {
        this.repeatDelay = Math.max(0.05f, delay);
        this.repeatRate = Math.max(0.02f, rate);
        return this;
    }

    /** Что делать по кнопке B, если Escape никто не обработал (обычно — закрыть экран). */
    public GamepadNavigator onBack(Runnable onBack) {
        this.onBack = onBack;
        return this;
    }

    /** Опросить платформу и обработать состояние (вызывает корень каждый кадр). */
    void update(float dt) {
        if (!enabled || !Lumina.isInitialized()) {
            return;
        }
        Gamepad.State state = Lumina.platform().pollGamepad();
        if (state == null) {
            if (connected) {
                reset();
            }
            connected = false;
            return;
        }
        connected = true;
        handle(state, dt);
    }

    private void reset() {
        java.util.Arrays.fill(previous, false);
        held = null;
    }

    /**
     * Обработать снимок геймпада, как если бы он пришёл от платформы (для тестов и своих источников ввода).
     *
     * @param dt время с прошлого снимка, секунды (для повтора шага при удержании)
     */
    public void handle(Gamepad.State state, float dt) {
        NavDirection dir = direction(state);
        if (dir != held) {
            held = dir;
            heldTime = 0f;
            nextRepeat = repeatDelay;
            if (dir != null) {
                root.focusDirection(dir);
            }
        } else if (dir != null) {
            heldTime += dt;
            while (heldTime >= nextRepeat) {
                nextRepeat += repeatRate;
                root.focusDirection(dir);
            }
        }
        if (pressed(state, Gamepad.A)) {
            activate();
        }
        if (pressed(state, Gamepad.B)) {
            back();
        }
        if (pressed(state, Gamepad.LEFT_BUMPER)) {
            switchTab(-1);
        }
        if (pressed(state, Gamepad.RIGHT_BUMPER)) {
            switchTab(1);
        }
        for (int i = 0; i < previous.length; i++) {
            previous[i] = state.button(i);
        }
    }

    private boolean pressed(Gamepad.State s, int button) {
        return s.button(button) && !previous[button];
    }

    private NavDirection direction(Gamepad.State s) {
        if (s.button(Gamepad.DPAD_UP)) {
            return NavDirection.UP;
        }
        if (s.button(Gamepad.DPAD_DOWN)) {
            return NavDirection.DOWN;
        }
        if (s.button(Gamepad.DPAD_LEFT)) {
            return NavDirection.LEFT;
        }
        if (s.button(Gamepad.DPAD_RIGHT)) {
            return NavDirection.RIGHT;
        }
        float x = s.axis(Gamepad.AXIS_LEFT_X);
        float y = s.axis(Gamepad.AXIS_LEFT_Y);
        if (Math.max(Math.abs(x), Math.abs(y)) < deadzone) {
            return null;
        }
        if (Math.abs(x) > Math.abs(y)) {
            return x > 0 ? NavDirection.RIGHT : NavDirection.LEFT;
        }
        return y > 0 ? NavDirection.DOWN : NavDirection.UP;
    }

    /** A: Enter элементу в фокусе, а если он его не обработал — клик в его центр. Без фокуса — фокус на первый. */
    public void activate() {
        UiElement f = root.focused();
        if (f == null) {
            root.focusDirection(NavDirection.DOWN);
            return;
        }
        boolean handled = root.keyDown(Keys.ENTER, 0, 0);
        root.keyUp(Keys.ENTER, 0, 0);
        if (!handled && f.root() == root) {
            float x = f.screenX() + f.width() / 2f;
            float y = f.screenY() + f.height() / 2f;
            root.dispatch(f, new PointerEvent(PointerEvent.PRESS, x, y, Keys.MOUSE_LEFT));
            root.dispatch(f, new PointerEvent(PointerEvent.RELEASE, x, y, Keys.MOUSE_LEFT));
            root.dispatch(f, new PointerEvent(PointerEvent.CLICK, x, y, Keys.MOUSE_LEFT));
        }
    }

    /** B: Escape, а если его никто не обработал — {@link #onBack}. */
    public void back() {
        if (!root.keyDown(Keys.ESCAPE, 0, 0) && onBack != null) {
            onBack.run();
        }
    }

    /** Бампер: соседняя вкладка у ближайшего {@link TabSwitchable}. */
    public boolean switchTab(int delta) {
        for (UiElement e = root.focused(); e != null; e = e.parent()) {
            if (e instanceof TabSwitchable t && e.isVisible() && t.switchTab(delta)) {
                return true;
            }
        }
        TabSwitchable t = findTabs(root.content());
        return t != null && t.switchTab(delta);
    }

    private static TabSwitchable findTabs(UiElement e) {
        if (!e.isVisible()) {
            return null;
        }
        if (e instanceof TabSwitchable t) {
            return t;
        }
        for (UiElement c : e.children()) {
            TabSwitchable found = findTabs(c);
            if (found != null) {
                return found;
            }
        }
        return null;
    }
}
