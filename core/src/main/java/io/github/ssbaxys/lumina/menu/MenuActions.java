package io.github.ssbaxys.lumina.menu;

import java.util.function.IntConsumer;

/**
 * Действия меню (своя кнопка → сервер) поверх ванильного нажатия кнопки меню, без своего сетевого канала.
 * Ванильный пакет нажатия на 1.20.1 несёт номер кнопки одним байтом со знаком (-128..127), поэтому пара
 * «действие, число» кодируется короткими кусками по 7 бит: отрицательный номер — «дальше ещё кусок»,
 * неотрицательный — последний кусок числа. Сначала идёт номер действия, затем число (зигзагом, чтобы небольшие
 * отрицательные тоже были короткими). Действие {@code < 128} с числом {@code -64..63} — ровно два нажатия.
 *
 * <p>Пакеты одного игрока сервер обрабатывает по порядку, так что {@link Decoder} на меню собирает пару из
 * нажатий; мусор (слишком длинное число) сбрасывает его без последствий.</p>
 *
 * <pre>{@code
 * MenuActions.encode(SORT, 1, channel::clickButton);          // клиент
 * if (decoder.accept(buttonId)) handle(decoder.action(), decoder.value());   // сервер
 * }</pre>
 */
public final class MenuActions {
    /** Самый длинный кусочный код int: 5 кусков по 7 бит. */
    private static final int MAX_CHUNKS = 5;

    private MenuActions() {
    }

    /**
     * Нажатия кнопок для действия {@code action >= 0} с числом {@code value}, по порядку в {@code out}.
     *
     * @throws IllegalArgumentException если {@code action < 0}
     */
    public static void encode(int action, int value, IntConsumer out) {
        if (action < 0) {
            throw new IllegalArgumentException("action id must be >= 0: " + action);
        }
        writeVar(action, out);
        writeVar((value << 1) ^ (value >> 31), out);
    }

    /** То же массивом (для тестов и отладки). */
    public static int[] encode(int action, int value) {
        int[] buf = new int[MAX_CHUNKS * 2];
        int[] n = {0};
        encode(action, value, id -> buf[n[0]++] = id);
        return java.util.Arrays.copyOf(buf, n[0]);
    }

    private static void writeVar(int v, IntConsumer out) {
        int rest = v;
        while ((rest & ~0x7F) != 0) {
            out.accept((rest & 0x7F) - 128);
            rest >>>= 7;
        }
        out.accept(rest);
    }

    /** Сборщик действий на сервере: по одному на открытое меню. */
    public static final class Decoder {
        private boolean haveAction;
        private int acc;
        private int chunks;
        private int action = -1;
        private int value;
        private int pendingAction;

        /**
         * Принять нажатие кнопки меню.
         *
         * @return {@code true}, если пара собрана: тогда она в {@link #action()} и {@link #value()}
         */
        public boolean accept(int buttonId) {
            if (buttonId < -128 || buttonId > 127) {
                reset();
                return false;
            }
            int chunk = buttonId < 0 ? buttonId + 128 : buttonId;
            acc |= chunk << (7 * chunks);
            chunks++;
            if (buttonId < 0) {
                if (chunks >= MAX_CHUNKS) {
                    reset();
                }
                return false;
            }
            int v = acc;
            acc = 0;
            chunks = 0;
            if (!haveAction) {
                if (v < 0) {
                    reset();
                    return false;
                }
                pendingAction = v;
                haveAction = true;
                return false;
            }
            haveAction = false;
            action = pendingAction;
            value = (v >>> 1) ^ -(v & 1);
            return true;
        }

        /** Последнее собранное действие ({@code -1}, если ещё не было). */
        public int action() {
            return action;
        }

        /** Число последнего собранного действия. */
        public int value() {
            return value;
        }

        /** Забыть недособранное. */
        public void reset() {
            haveAction = false;
            acc = 0;
            chunks = 0;
        }
    }
}
