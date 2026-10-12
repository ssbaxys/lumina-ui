package io.github.ssbaxys.lumina.markup;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Signal;

import java.util.HashMap;
import java.util.Map;

/**
 * Логика для разметки: данные (сигналы) и обработчики по именам. Разметка ссылается на них так:
 * {@code {{count}}} в тексте, {@code bind="volume"} у полей ввода, {@code on-click="save"} у кнопок,
 * {@code if="advanced"} для показа/скрытия.
 *
 * <p>Без Java тоже можно: {@code <state name="count" value="0"/>} объявляет сигнал прямо в разметке, а
 * встроенные действия {@code inc:count}, {@code dec:count}, {@code toggle:flag}, {@code set:tab=2},
 * {@code sound:minecraft:block.note_block.pling} работают из коробки. Несколько действий — через {@code ;}.</p>
 */
public final class Bindings {
    /** Обработчик события из разметки; {@code source} — элемент, на котором оно произошло. */
    @FunctionalInterface
    public interface Handler {
        void handle(UiElement source);
    }

    private final Map<String, ReadableSignal<?>> signals = new HashMap<>();
    private final Map<String, Handler> handlers = new HashMap<>();
    private final Map<String, Action> actions = new HashMap<>();

    public Bindings() {
        action("inc", (arg, src) -> numeric(arg, 1));
        action("dec", (arg, src) -> numeric(arg, -1));
        action("toggle", (arg, src) -> {
            Signal<Object> s = writable(arg);
            s.set(!Boolean.TRUE.equals(s.peek()));
        });
        action("set", (arg, src) -> {
            int eq = arg.indexOf('=');
            Signal<Object> s = writable(arg.substring(0, eq).trim());
            s.set(convert(arg.substring(eq + 1).trim(), s.peek()));
        });
        action("sound", (arg, src) -> {
            if (Lumina.isInitialized()) {
                Lumina.platform().playSound(arg, 1f, 1f);
            }
        });
    }

    /** Встроенное или своё действие вида {@code имя:аргумент}. */
    @FunctionalInterface
    public interface Action {
        void run(String argument, UiElement source);
    }

    public Bindings action(String name, Action action) {
        actions.put(name, action);
        return this;
    }

    public Bindings signal(String name, ReadableSignal<?> signal) {
        signals.put(name, signal);
        return this;
    }

    public Bindings handler(String name, Handler handler) {
        handlers.put(name, handler);
        return this;
    }

    public Bindings handler(String name, Runnable handler) {
        return handler(name, src -> handler.run());
    }

    public ReadableSignal<?> get(String name) {
        return signals.get(name);
    }

    /** Сигнал по имени; если его нет — создаётся с начальным значением (для {@code <state>}). */
    public Signal<Object> state(String name, Object initial) {
        ReadableSignal<?> existing = signals.get(name);
        if (existing instanceof Signal<?>) {
            return writable(name);
        }
        Signal<Object> s = Signal.of(initial);
        signals.put(name, s);
        return s;
    }

    @SuppressWarnings("unchecked")
    public Signal<Object> writable(String name) {
        ReadableSignal<?> s = signals.get(name);
        if (!(s instanceof Signal<?>)) {
            if (s != null) {
                throw new IllegalStateException("Signal '" + name + "' is read-only");
            }
            Signal<Object> created = Signal.of(null);
            signals.put(name, created);
            return created;
        }
        return (Signal<Object>) s;
    }

    /** Выполнить строку из атрибута {@code on-click}: имена обработчиков и действия через {@code ;}. */
    public void run(String script, UiElement source) {
        for (String part : script.split(";")) {
            String cmd = part.trim();
            if (cmd.isEmpty()) {
                continue;
            }
            Handler h = handlers.get(cmd);
            if (h != null) {
                h.handle(source);
                continue;
            }
            int colon = cmd.indexOf(':');
            Action a = colon > 0 ? actions.get(cmd.substring(0, colon)) : null;
            if (a == null) {
                System.err.println("[Lumina markup] no handler or action: " + cmd);
                continue;
            }
            a.run(cmd.substring(colon + 1), source);
        }
    }

    private void numeric(String arg, int sign) {
        String name = arg;
        float step = 1f;
        int by = arg.indexOf('=');
        if (by > 0) {
            name = arg.substring(0, by).trim();
            step = Float.parseFloat(arg.substring(by + 1).trim());
        }
        Signal<Object> s = writable(name);
        Object v = s.peek();
        if (v instanceof Integer i) {
            s.set(i + Math.round(sign * step));
        } else if (v instanceof Float f) {
            s.set(f + sign * step);
        } else if (v instanceof Double d) {
            s.set(d + sign * step);
        } else {
            s.set(Math.round(sign * step));
        }
    }

    /** Строку из разметки — в тип текущего значения сигнала (или угадать тип). */
    static Object convert(String text, Object like) {
        if (like instanceof Integer) {
            return Integer.parseInt(text);
        }
        if (like instanceof Float) {
            return Float.parseFloat(text);
        }
        if (like instanceof Double) {
            return Double.parseDouble(text);
        }
        if (like instanceof Boolean) {
            return Boolean.parseBoolean(text);
        }
        if (like instanceof String) {
            return text;
        }
        return guess(text);
    }

    static Object guess(String text) {
        if (text.equals("true") || text.equals("false")) {
            return Boolean.parseBoolean(text);
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException ignored) {
            // не целое
        }
        try {
            return Float.parseFloat(text);
        } catch (NumberFormatException ignored) {
            return text;
        }
    }
}
