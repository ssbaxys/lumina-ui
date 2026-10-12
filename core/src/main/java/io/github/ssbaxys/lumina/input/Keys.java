package io.github.ssbaxys.lumina.input;

/** Коды клавиш и модификаторов (совпадают с GLFW), чтобы ядро не зависело от LWJGL. */
public final class Keys {
    public static final int SPACE = 32;
    public static final int ESCAPE = 256;
    public static final int ENTER = 257;
    public static final int TAB = 258;
    public static final int BACKSPACE = 259;
    public static final int DELETE = 261;
    public static final int RIGHT = 262;
    public static final int LEFT = 263;
    public static final int DOWN = 264;
    public static final int UP = 265;
    public static final int PAGE_UP = 266;
    public static final int PAGE_DOWN = 267;
    public static final int HOME = 268;
    public static final int END = 269;
    public static final int KP_ENTER = 335;

    public static final int MOD_SHIFT = 0x1;
    public static final int MOD_CONTROL = 0x2;
    public static final int MOD_ALT = 0x4;

    public static final int MOUSE_LEFT = 0;
    public static final int MOUSE_RIGHT = 1;
    public static final int MOUSE_MIDDLE = 2;

    private Keys() {
    }
}
