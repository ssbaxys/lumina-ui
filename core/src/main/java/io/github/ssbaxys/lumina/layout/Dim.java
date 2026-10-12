package io.github.ssbaxys.lumina.layout;

/** Размер по одной оси: авто (по содержимому), пиксели, процент от доступного или «заполнить». */
public final class Dim {
    public enum Kind { AUTO, PX, PERCENT, FILL }

    public static final Dim AUTO = new Dim(Kind.AUTO, 0f);
    public static final Dim FILL = new Dim(Kind.FILL, 1f);

    private final Kind kind;
    private final float value;

    private Dim(Kind kind, float value) {
        this.kind = kind;
        this.value = value;
    }

    public static Dim px(float value) {
        return new Dim(Kind.PX, value);
    }

    /** @param fraction доля от доступного места, 0.5 = 50% */
    public static Dim percent(float fraction) {
        return new Dim(Kind.PERCENT, fraction);
    }

    public Kind kind() {
        return kind;
    }

    public float value() {
        return value;
    }

    public boolean isAuto() {
        return kind == Kind.AUTO;
    }

    /** Фиксированный размер или {@code NaN}, если размер определяется содержимым / неизвестен. */
    public float resolve(float available) {
        return switch (kind) {
            case PX -> value;
            case PERCENT -> Float.isFinite(available) ? available * value : Float.NaN;
            case FILL -> Float.isFinite(available) ? available : Float.NaN;
            case AUTO -> Float.NaN;
        };
    }

    @Override
    public String toString() {
        return switch (kind) {
            case AUTO -> "auto";
            case FILL -> "fill";
            case PX -> value + "px";
            case PERCENT -> (value * 100f) + "%";
        };
    }
}
