package io.github.ssbaxys.lumina.sound;

import io.github.ssbaxys.lumina.Lumina;

/**
 * Звук интерфейса: идентификатор звука игры, громкость и высота. Хранится в стиле
 * ({@code Styles.HOVER_SOUND}, {@code Styles.CLICK_SOUND}) и проигрывается через
 * {@code Platform.playSound}, поэтому подхватывает ресурспаки игрока.
 *
 * <pre>{@code
 * UiSound.of("minecraft:block.note_block.hat", 0.15f, 1.6f)
 * UiSound.NONE                                    // явная тишина (перекрывает звук по умолчанию)
 * }</pre>
 */
public record UiSound(String id, float volume, float pitch) {
    /** Тишина. */
    public static final UiSound NONE = new UiSound("", 0f, 1f);

    public UiSound {
        id = id == null ? "" : id;
    }

    public static UiSound of(String id) {
        return new UiSound(id, 1f, 1f);
    }

    public static UiSound of(String id, float volume, float pitch) {
        return new UiSound(id, volume, pitch);
    }

    public boolean isNone() {
        return id.isEmpty() || volume <= 0f;
    }

    /** Та же запись с другой громкостью. */
    public UiSound withVolume(float volume) {
        return new UiSound(id, volume, pitch);
    }

    /** Проиграть (если платформа есть и звук не пустой). */
    public void play() {
        if (!isNone() && Lumina.isInitialized()) {
            Lumina.platform().playSound(id, volume, pitch);
        }
    }

    /** {@code "minecraft:ui.button.click 0.25 1"} или {@code "none"} — как в CSS. */
    @Override
    public String toString() {
        return isNone() ? "none" : id + " " + fmt(volume) + " " + fmt(pitch);
    }

    /** Разобрать запись из {@link #toString()}: id [громкость [высота]] или none. */
    public static UiSound parse(String text) {
        String t = text.trim();
        if (t.isEmpty() || t.equalsIgnoreCase("none")) {
            return NONE;
        }
        String[] p = t.split("\\s+");
        float volume = p.length > 1 ? Float.parseFloat(p[1]) : 1f;
        float pitch = p.length > 2 ? Float.parseFloat(p[2]) : 1f;
        return new UiSound(p[0], volume, pitch);
    }

    private static String fmt(float f) {
        return f == (int) f ? Integer.toString((int) f) : Float.toString(f);
    }
}
