package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.node.UiElement;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * Подсказки для любых элементов: {@code Tooltips.attach(element, "текст")}. У элемента одна подсказка:
 * повторный {@code attach} меняет содержимое существующей. Виджеты умеют то же короче —
 * {@code button.tooltip("текст")}.
 *
 * <pre>{@code
 * Tooltips.setDefaultKind(Tooltip.Kind.MODERN);    // вид по умолчанию для новых подсказок
 * Tooltips.attach(icon, "Алмаз\nРедкая руда").delay(0.3f);
 * Tooltips.attachMarkdown(icon, "**Алмаз**\n\n![](item:minecraft:diamond) x64");
 * Tooltips.attach(slot, () -> buildCard(slot)).follow(false);
 * Tooltips.detach(icon);
 * }</pre>
 */
public final class Tooltips {
    /** Подсказки по элементам; ключи слабые — подсказка не держит выброшенный элемент. */
    private static final Map<UiElement, Tooltip> ATTACHED = new WeakHashMap<>();
    private static float defaultDelay = 0.5f;
    private static Tooltip.Kind defaultKind = Tooltip.Kind.VANILLA;
    private static Tooltip shown;

    private Tooltips() {
    }

    /** Подсказка с текстом (строки через {@code \n}). */
    public static Tooltip attach(UiElement element, String text) {
        return get(element).text(text);
    }

    /** Подсказка с Markdown. */
    public static Tooltip attachMarkdown(UiElement element, String markdown) {
        return get(element).markdown(markdown);
    }

    /** Подсказка с произвольным содержимым: элемент создаётся при каждом показе. */
    public static Tooltip attach(UiElement element, Supplier<? extends UiElement> content) {
        return get(element).content(content);
    }

    /** Подсказка элемента или {@code null}. */
    public static Tooltip of(UiElement element) {
        return ATTACHED.get(element);
    }

    /** Убрать подсказку элемента (и спрятать, если показана). */
    public static void detach(UiElement element) {
        Tooltip t = ATTACHED.remove(element);
        if (t != null) {
            t.unhook();
        }
    }

    /** Показанная сейчас подсказка или {@code null}. */
    public static Tooltip current() {
        return shown;
    }

    /** Спрятать показанную подсказку (например, перед сменой экрана). */
    public static void hideAll() {
        if (shown != null) {
            shown.hide();
        }
    }

    public static float defaultDelay() {
        return defaultDelay;
    }

    /** Задержка наведения по умолчанию для новых подсказок, секунды (0.5 из коробки). */
    public static void setDefaultDelay(float seconds) {
        defaultDelay = Math.max(0f, seconds);
    }

    public static Tooltip.Kind defaultKind() {
        return defaultKind;
    }

    /** Вид по умолчанию для новых подсказок ({@link Tooltip.Kind#VANILLA} из коробки). */
    public static void setDefaultKind(Tooltip.Kind kind) {
        defaultKind = kind == null ? Tooltip.Kind.VANILLA : kind;
    }

    private static Tooltip get(UiElement element) {
        Tooltip t = ATTACHED.get(element);
        if (t == null) {
            t = new Tooltip(element);
            ATTACHED.put(element, t);
            // Подсказке нужно наведение: подпись (Label) по умолчанию мышь не ловит.
            element.setHitTestable(true);
        }
        return t;
    }

    /** Одновременно видна одна подсказка: новая прячет прежнюю. */
    static void showing(Tooltip t) {
        if (shown != null && shown != t) {
            Tooltip old = shown;
            shown = null;
            old.hide();
        }
        shown = t;
    }

    static void hidden(Tooltip t) {
        if (shown == t) {
            shown = null;
        }
    }
}
