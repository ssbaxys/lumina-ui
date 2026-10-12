package io.github.ssbaxys.lumina.render;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Готовые красивые градиенты. Все — диагональные (135°), их легко повернуть: {@code SUNSET.rotate(90)}. */
public final class Gradients {
    public static final Brush.Linear SUNSET = Brush.linear(135, 0xFFFF7E5F, 0xFFFEB47B);
    public static final Brush.Linear OCEAN = Brush.linear(135, 0xFF2E3192, 0xFF1BFFFF);
    public static final Brush.Linear AURORA = Brush.linear(135, 0xFF00C9A7, 0xFF845EC2, 0xFFD65DB1);
    public static final Brush.Linear NEON = Brush.linear(135, 0xFFF0506E, 0xFFA855F7, 0xFF22D3EE);
    public static final Brush.Linear FIRE = Brush.linear(135, 0xFFF83600, 0xFFF9D423);
    public static final Brush.Linear MINT = Brush.linear(135, 0xFF34C38F, 0xFF22D3EE);
    public static final Brush.Linear PEACH = Brush.linear(135, 0xFFFFDDE1, 0xFFEE9CA7);
    public static final Brush.Linear NIGHT = Brush.linear(135, 0xFF0F2027, 0xFF203A43, 0xFF2C5364);
    public static final Brush.Linear GOLD = Brush.linear(135, 0xFFBF953F, 0xFFFCF6BA, 0xFFB38728, 0xFFFBF5B7);
    public static final Brush.Linear BERRY = Brush.linear(135, 0xFF8E2DE2, 0xFF4A00E0);
    public static final Brush.Linear LIME = Brush.linear(135, 0xFFA8E063, 0xFF56AB2F);
    public static final Brush.Linear RAINBOW = Brush.rainbow(90);

    private static final Map<String, Brush.Linear> ALL;

    static {
        Map<String, Brush.Linear> m = new LinkedHashMap<>();
        m.put("SUNSET", SUNSET);
        m.put("OCEAN", OCEAN);
        m.put("AURORA", AURORA);
        m.put("NEON", NEON);
        m.put("FIRE", FIRE);
        m.put("MINT", MINT);
        m.put("PEACH", PEACH);
        m.put("NIGHT", NIGHT);
        m.put("GOLD", GOLD);
        m.put("BERRY", BERRY);
        m.put("LIME", LIME);
        m.put("RAINBOW", RAINBOW);
        ALL = Collections.unmodifiableMap(m);
    }

    private Gradients() {
    }

    /** Все пресеты по именам (в порядке объявления). */
    public static Map<String, Brush.Linear> all() {
        return ALL;
    }
}
