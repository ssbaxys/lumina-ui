package io.github.ssbaxys.lumina.layout;

/**
 * Класс ширины экрана для адаптивной раскладки ({@code UiRoot.breakpoint()}): узкий, средний, широкий.
 * Пороги задаёт {@link Breakpoints}.
 */
public enum Breakpoint {
    /** Узкий экран (маленькое окно, крупный масштаб GUI): всё в одну колонку. */
    COMPACT,
    /** Средний. */
    MEDIUM,
    /** Широкий: колонки рядом. */
    WIDE;

    /** Значение для текущего класса ширины: {@code bp.pick(1, 2, 3)}. */
    public <T> T pick(T compact, T medium, T wide) {
        return switch (this) {
            case COMPACT -> compact;
            case MEDIUM -> medium;
            case WIDE -> wide;
        };
    }

    public boolean atLeast(Breakpoint other) {
        return ordinal() >= other.ordinal();
    }
}
