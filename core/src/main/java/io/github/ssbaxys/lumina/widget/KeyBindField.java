package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.event.CharEvent;
import io.github.ssbaxys.lumina.event.FocusEvent;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.function.IntConsumer;

/**
 * Поле назначения клавиши, как в настройках управления: клик (или Enter) — поле ждёт нажатия,
 * следующая клавиша становится значением, Escape отменяет ожидание, потеря фокуса — тоже.
 * Правый клик сбрасывает назначение к {@link #defaultKey} (по умолчанию — «не задано»).
 * Значение — код клавиши GLFW ({@link Keys}) в {@link Signal}; {@link #NONE} — не задано.
 *
 * <pre>{@code
 * Signal<Integer> jump = Signal.of(Keys.SPACE);
 * new KeyBindField(jump).defaultKey(Keys.SPACE);
 * }</pre>
 *
 * Стили: "keybind" поверх "field"; ожидание клавиши — состояние {@link #LISTENING}.
 */
public class KeyBindField extends Widget<KeyBindField> {
    /** Нет назначенной клавиши. */
    public static final int NONE = -1;
    /** Состояние: поле ждёт нажатия клавиши. */
    public static final int LISTENING = State.FIRST_CUSTOM;

    private final Signal<Integer> value;
    private int defaultKey = NONE;
    private String waitingText = "> Нажмите клавишу <";
    private IntConsumer onBind;
    private Subscription subscription;

    public KeyBindField(Signal<Integer> value) {
        super("field keybind");
        this.value = value;
        setFocusable(true);
    }

    public Signal<Integer> value() {
        return value;
    }

    /** Клавиша, к которой возвращает правый клик. */
    public KeyBindField defaultKey(int key) {
        this.defaultKey = key;
        return this;
    }

    /** Текст, пока поле ждёт клавишу. */
    public KeyBindField waitingText(String text) {
        this.waitingText = text == null ? "" : text;
        invalidateLayout();
        return this;
    }

    /** Вызывается после назначения клавиши (и при сбросе правым кликом). */
    public KeyBindField onBind(IntConsumer onBind) {
        this.onBind = onBind;
        return this;
    }

    public int key() {
        Integer k = value.peek();
        return k == null ? NONE : k;
    }

    public boolean isListening() {
        return hasState(LISTENING);
    }

    /** Начать ожидание клавиши (как клик по полю). */
    public void listen() {
        if (isEnabled()) {
            requestFocus();
            setState(LISTENING, true);
        }
    }

    /** Прекратить ожидание без изменений. */
    public void cancel() {
        setState(LISTENING, false);
    }

    private void bind(int key) {
        setState(LISTENING, false);
        value.set(key);
        if (Lumina.isInitialized()) {
            Lumina.platform().playClickSound();
        }
        if (onBind != null) {
            onBind.accept(key);
        }
    }

    @Override
    protected void onAttach() {
        subscription = value.subscribe(v -> invalidateLayout());
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
        setState(LISTENING, false);
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.PRESS) {
            int button = ((PointerEvent) event).button();
            if (!isEnabled()) {
                return;
            }
            if (button == Keys.MOUSE_LEFT) {
                if (isListening()) {
                    cancel();
                } else {
                    listen();
                }
                event.consume();
            } else if (button == Keys.MOUSE_RIGHT) {
                bind(defaultKey);
                event.consume();
            }
        } else if (event.type() == KeyEvent.PRESS && event.target() == this) {
            int key = ((KeyEvent) event).key();
            if (isListening()) {
                if (key == Keys.ESCAPE) {
                    cancel();
                } else {
                    bind(key);
                }
                event.consume();
            } else if (key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.SPACE) {
                listen();
                event.consume();
            } else if (key == Keys.DELETE || key == Keys.BACKSPACE) {
                bind(defaultKey);
                event.consume();
            }
        } else if (event.type() == CharEvent.TYPED && isListening()) {
            // Символ от только что назначенной клавиши не должен уйти дальше (в поле ввода рядом).
            event.consume();
        } else if (event.type() == FocusEvent.LOST) {
            cancel();
        }
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float w = Math.max(m.width(waitingText), Math.max(m.width(keyName(key())), m.width("Пробел")));
        setContentSize(w, m.lineHeight());
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        int color = style.get(Styles.TEXT_COLOR);
        String s;
        int c;
        if (isListening()) {
            s = waitingText;
            // Мягкая пульсация, пока ждём клавишу.
            float t = (float) ((System.nanoTime() / 1_000_000L) % 1200L) / 1200f;
            float pulse = 0.65f + 0.35f * (float) Math.abs(Math.sin(t * Math.PI));
            c = Color.mulAlpha(style.get(Styles.ACCENT_COLOR), pulse);
        } else {
            int k = key();
            s = keyName(k);
            c = k == NONE ? Color.mulAlpha(color, 0.45f) : color;
        }
        float x = (width() - m.width(s)) / 2f;
        float y = (height() - m.lineHeight()) / 2f + 0.5f;
        canvas.drawText(s, x, y, c, style.get(Styles.TEXT_SHADOW));
    }

    /**
     * Код клавиши из текста: число GLFW ("32"), буква или цифра ("R"), "F5", название из {@link #keyName}
     * ("Пробел") или английское ("space", "enter", "tab", "escape", "left-shift"...). Неизвестное — {@link #NONE}.
     */
    public static int parseKey(String text) {
        if (text == null || text.isBlank()) {
            return NONE;
        }
        String s = text.strip();
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException ignored) {
            // не число
        }
        if (s.length() == 1) {
            char c = Character.toUpperCase(s.charAt(0));
            if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9')) {
                return c;
            }
        }
        String k = s.toLowerCase(java.util.Locale.ROOT).replace('_', '-').replace(' ', '-');
        if (k.length() > 1 && k.charAt(0) == 'f' && k.substring(1).chars().allMatch(Character::isDigit)) {
            int n = Integer.parseInt(k.substring(1));
            return n >= 1 && n <= 25 ? 289 + n : NONE;
        }
        switch (k) {
            case "space" -> {
                return Keys.SPACE;
            }
            case "enter", "return" -> {
                return Keys.ENTER;
            }
            case "tab" -> {
                return Keys.TAB;
            }
            case "escape", "esc" -> {
                return Keys.ESCAPE;
            }
            case "backspace" -> {
                return Keys.BACKSPACE;
            }
            case "delete", "del" -> {
                return Keys.DELETE;
            }
            case "up" -> {
                return Keys.UP;
            }
            case "down" -> {
                return Keys.DOWN;
            }
            case "left" -> {
                return Keys.LEFT;
            }
            case "right" -> {
                return Keys.RIGHT;
            }
            case "shift", "left-shift" -> {
                return 340;
            }
            case "ctrl", "control", "left-ctrl", "left-control" -> {
                return 341;
            }
            case "alt", "left-alt" -> {
                return 342;
            }
            case "none" -> {
                return NONE;
            }
            default -> {
            }
        }
        for (int code = 32; code <= 348; code++) {
            if (keyName(code).equalsIgnoreCase(s)) {
                return code;
            }
        }
        return NONE;
    }

    /** Название клавиши по коду GLFW: "R", "Пробел", "Левый Shift", "F5", "Num 7"... */
    public static String keyName(int key) {
        if (key == NONE) {
            return "Не задано";
        }
        if ((key >= Keys.A && key <= Keys.Z) || (key >= '0' && key <= '9')) {
            return String.valueOf((char) key);
        }
        if (key >= 290 && key <= 314) {
            return "F" + (key - 289);
        }
        if (key >= 320 && key <= 329) {
            return "Num " + (key - 320);
        }
        return switch (key) {
            case Keys.SPACE -> "Пробел";
            case 39 -> "'";
            case 44 -> ",";
            case 45 -> "-";
            case 46 -> ".";
            case 47 -> "/";
            case 59 -> ";";
            case 61 -> "=";
            case 91 -> "[";
            case 92 -> "\\";
            case 93 -> "]";
            case 96 -> "`";
            case Keys.ESCAPE -> "Esc";
            case Keys.ENTER -> "Enter";
            case Keys.TAB -> "Tab";
            case Keys.BACKSPACE -> "Backspace";
            case 260 -> "Insert";
            case Keys.DELETE -> "Delete";
            case Keys.RIGHT -> "Вправо";
            case Keys.LEFT -> "Влево";
            case Keys.DOWN -> "Вниз";
            case Keys.UP -> "Вверх";
            case Keys.PAGE_UP -> "Page Up";
            case Keys.PAGE_DOWN -> "Page Down";
            case Keys.HOME -> "Home";
            case Keys.END -> "End";
            case 280 -> "Caps Lock";
            case 281 -> "Scroll Lock";
            case 282 -> "Num Lock";
            case 283 -> "Print Screen";
            case 284 -> "Pause";
            case 330 -> "Num .";
            case 331 -> "Num /";
            case 332 -> "Num *";
            case 333 -> "Num -";
            case 334 -> "Num +";
            case Keys.KP_ENTER -> "Num Enter";
            case 336 -> "Num =";
            case 340 -> "Левый Shift";
            case 341 -> "Левый Ctrl";
            case 342 -> "Левый Alt";
            case 343 -> "Левый Win";
            case 344 -> "Правый Shift";
            case 345 -> "Правый Ctrl";
            case 346 -> "Правый Alt";
            case 347 -> "Правый Win";
            case 348 -> "Меню";
            default -> "Клавиша " + key;
        };
    }
}
