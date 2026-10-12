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

    // Буквы (GLFW-коды по раскладке US): сочетания Ctrl+A/C/X/V/Z/Y.
    public static final int A = 65;
    public static final int B = 66;
    public static final int C = 67;
    public static final int D = 68;
    public static final int E = 69;
    public static final int F = 70;
    public static final int G = 71;
    public static final int H = 72;
    public static final int I = 73;
    public static final int J = 74;
    public static final int K = 75;
    public static final int L = 76;
    public static final int M = 77;
    public static final int N = 78;
    public static final int O = 79;
    public static final int P = 80;
    public static final int Q = 81;
    public static final int R = 82;
    public static final int S = 83;
    public static final int T = 84;
    public static final int U = 85;
    public static final int V = 86;
    public static final int W = 87;
    public static final int X = 88;
    public static final int Y = 89;
    public static final int Z = 90;

    public static final int MOD_SHIFT = 0x1;
    public static final int MOD_CONTROL = 0x2;
    public static final int MOD_ALT = 0x4;
    /** Cmd на macOS (Super на остальных). */
    public static final int MOD_SUPER = 0x8;

    public static final int MOUSE_LEFT = 0;
    public static final int MOUSE_RIGHT = 1;
    public static final int MOUSE_MIDDLE = 2;

    private Keys() {
    }
}
