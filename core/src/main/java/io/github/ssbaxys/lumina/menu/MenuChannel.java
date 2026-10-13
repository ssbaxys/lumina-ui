package io.github.ssbaxys.lumina.menu;

/**
 * Связь клиентского {@link RemoteMenu} с меню игры; реализует бэкенд загрузчика. Щелчки уходят в игру так же,
 * как из ванильных окон (игра сразу предсказывает результат у себя и отправляет щелчок серверу), кнопки — как
 * нажатия кнопок меню.
 */
public interface MenuChannel {
    /** Щелчок по слоту меню {@code slot} (или {@link SlotClick#OUTSIDE}). */
    void click(int slot, int button, SlotClick type);

    /** Есть ли сейчас что-то на курсоре меню (с учётом только что отправленных щелчков). */
    boolean isCarrying();

    /** Нажатие кнопки меню {@code buttonId} (у сервера — {@code clickMenuButton}). */
    void clickButton(int buttonId);

    /** Жест закончен: можно сразу перечитать слоты, чтобы интерфейс не ждал следующего кадра. */
    default void flush() {
    }

    /** Закрыть меню (экран). */
    default void close() {
    }
}
