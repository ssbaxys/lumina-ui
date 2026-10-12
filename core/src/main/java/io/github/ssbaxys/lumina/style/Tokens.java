package io.github.ssbaxys.lumina.style;

/**
 * Встроенные токены тем. Стандартные темы задают их значения, а стили виджетов на них ссылаются,
 * поэтому {@code theme.set(Tokens.ACCENT, 0xFFF0506E)} перекрашивает кнопки, переключатели,
 * ползунки и рамки фокуса сразу во всём интерфейсе.
 */
public final class Tokens {
    /** Акцентный цвет: кнопки, переключатели, ползунки, индикаторы. */
    public static final ThemeVar<Integer> ACCENT = ThemeVar.of("accent", Integer.class, 0xFF5B8CFF);
    /** Цвет текста на обычном фоне. */
    public static final ThemeVar<Integer> TEXT = ThemeVar.of("text", Integer.class, 0xFFE6E9F2);
    /** Верх и низ градиента панелей. */
    public static final ThemeVar<Integer> SURFACE = ThemeVar.of("surface", Integer.class, 0xF0202430);
    public static final ThemeVar<Integer> SURFACE_ALT = ThemeVar.of("surface-alt", Integer.class, 0xF0151820);
    /** Тонкие рамки панелей. */
    public static final ThemeVar<Integer> BORDER = ThemeVar.of("border", Integer.class, 0x24FFFFFF);
    /** Цвет теней. */
    public static final ThemeVar<Integer> SHADOW = ThemeVar.of("shadow", Integer.class, 0x90000000);
    /** Утопленные области: дорожки, поля, области прокрутки. */
    public static final ThemeVar<Integer> WELL = ThemeVar.of("well", Integer.class, 0x30000000);
    /** Базовое скругление панелей в пикселях; кнопки и поля берут долю от него. */
    public static final ThemeVar<Float> CORNER = ThemeVar.of("corner", Float.class, 10f);
    /** Множитель длительности всех переходов темы (0 — без анимаций, 2 — вдвое медленнее). */
    public static final ThemeVar<Float> MOTION = ThemeVar.of("motion", Float.class, 1f);

    private Tokens() {
    }

    /** Гарантирует регистрацию встроенных токенов. */
    static void bootstrap() {
    }
}
