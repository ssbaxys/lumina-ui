package io.github.ssbaxys.lumina.input;

/**
 * Геймпад в раскладке GLFW (Xbox): номера кнопок и осей и снимок состояния, который отдаёт
 * {@code Platform.pollGamepad()}. Загрузчики читают его через {@code glfwGetGamepadState}.
 */
public final class Gamepad {
    public static final int A = 0;
    public static final int B = 1;
    public static final int X = 2;
    public static final int Y = 3;
    public static final int LEFT_BUMPER = 4;
    public static final int RIGHT_BUMPER = 5;
    public static final int BACK = 6;
    public static final int START = 7;
    public static final int GUIDE = 8;
    public static final int LEFT_THUMB = 9;
    public static final int RIGHT_THUMB = 10;
    public static final int DPAD_UP = 11;
    public static final int DPAD_RIGHT = 12;
    public static final int DPAD_DOWN = 13;
    public static final int DPAD_LEFT = 14;
    public static final int BUTTON_COUNT = 15;

    public static final int AXIS_LEFT_X = 0;
    public static final int AXIS_LEFT_Y = 1;
    public static final int AXIS_RIGHT_X = 2;
    public static final int AXIS_RIGHT_Y = 3;
    public static final int AXIS_LEFT_TRIGGER = 4;
    public static final int AXIS_RIGHT_TRIGGER = 5;
    public static final int AXIS_COUNT = 6;

    private Gamepad() {
    }

    /**
     * Снимок геймпада: нажатые кнопки ({@link #BUTTON_COUNT}) и оси ({@link #AXIS_COUNT}, −1..1; ось Y стика
     * вниз положительна, как в GLFW).
     */
    public record State(boolean[] buttons, float[] axes) {
        public State {
            buttons = java.util.Arrays.copyOf(buttons, BUTTON_COUNT);
            axes = java.util.Arrays.copyOf(axes, AXIS_COUNT);
        }

        /** Ничего не нажато. */
        public static State idle() {
            return new State(new boolean[BUTTON_COUNT], new float[AXIS_COUNT]);
        }

        /** Нажаты только эти кнопки (для тестов и эмуляции). */
        public static State pressed(int... buttons) {
            boolean[] b = new boolean[BUTTON_COUNT];
            for (int i : buttons) {
                if (i >= 0 && i < BUTTON_COUNT) {
                    b[i] = true;
                }
            }
            return new State(b, new float[AXIS_COUNT]);
        }

        /** Левый стик отклонён на (x, y). */
        public static State stick(float x, float y) {
            float[] a = new float[AXIS_COUNT];
            a[AXIS_LEFT_X] = x;
            a[AXIS_LEFT_Y] = y;
            return new State(new boolean[BUTTON_COUNT], a);
        }

        public boolean button(int index) {
            return index >= 0 && index < buttons.length && buttons[index];
        }

        public float axis(int index) {
            return index >= 0 && index < axes.length ? axes[index] : 0f;
        }
    }
}
