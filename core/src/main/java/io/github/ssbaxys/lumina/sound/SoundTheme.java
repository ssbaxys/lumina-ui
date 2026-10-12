package io.github.ssbaxys.lumina.sound;

import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;

import java.util.List;

/**
 * Набор звуков интерфейса: наведение и нажатие. Применяется к теме — дописывает
 * {@link Styles#HOVER_SOUND} и {@link Styles#CLICK_SOUND} в стили интерактивных классов:
 *
 * <pre>{@code
 * SoundTheme.SOFT.applyTo(theme);                       // кнопки, переключатели, флажки, пункты меню
 * SoundTheme.RETRO.applyTo(theme, "nav", "card");        // и свои классы
 * SoundTheme.custom("моя", UiSound.of("mymod:ui.hover", 0.2f, 1f), UiSound.of("mymod:ui.click")).applyTo(theme);
 * }</pre>
 *
 * Все встроенные наборы собраны из ванильных звуков, поэтому их переопределяют ресурспаки.
 */
public final class SoundTheme {
    /** Классы, которым набор назначается всегда. */
    public static final List<String> INTERACTIVE_CLASSES = List.of(
            "button", "image-button", "toggle", "checkbox", "menu-item");

    /** Как в игре: щелчок кнопки и едва слышный щелчок при наведении. */
    public static final SoundTheme VANILLA = new SoundTheme("vanilla",
            UiSound.of("minecraft:ui.button.click", 0.05f, 1.8f),
            UiSound.of("minecraft:ui.button.click", 0.25f, 1f));
    /** Мягкие деревянные щелчки и тихий «хэт» нотного блока. */
    public static final SoundTheme SOFT = new SoundTheme("soft",
            UiSound.of("minecraft:block.note_block.hat", 0.12f, 1.6f),
            UiSound.of("minecraft:block.wooden_button.click_on", 0.35f, 1.5f));
    /** 8-битные «пиу» нотного блока bit. */
    public static final SoundTheme RETRO = new SoundTheme("retro",
            UiSound.of("minecraft:block.note_block.bit", 0.15f, 2f),
            UiSound.of("minecraft:block.note_block.bit", 0.35f, 1.2f));
    /** Полная тишина (в том числе без стандартного щелчка кнопок). */
    public static final SoundTheme NONE = new SoundTheme("none", UiSound.NONE, UiSound.NONE);

    private final String name;
    private final UiSound hover;
    private final UiSound click;

    private SoundTheme(String name, UiSound hover, UiSound click) {
        this.name = name;
        this.hover = hover;
        this.click = click;
    }

    public static SoundTheme custom(String name, UiSound hover, UiSound click) {
        return new SoundTheme(name, hover == null ? UiSound.NONE : hover, click == null ? UiSound.NONE : click);
    }

    /** Встроенные наборы по порядку: vanilla, soft, retro, none. */
    public static List<SoundTheme> builtIn() {
        return List.of(VANILLA, SOFT, RETRO, NONE);
    }

    /** Встроенный набор по имени или {@code null}. */
    public static SoundTheme byName(String name) {
        for (SoundTheme t : builtIn()) {
            if (t.name.equalsIgnoreCase(name)) {
                return t;
            }
        }
        return null;
    }

    public String name() {
        return name;
    }

    public UiSound hover() {
        return hover;
    }

    public UiSound click() {
        return click;
    }

    /** Тот же набор с громкостью × {@code factor}. */
    public SoundTheme scaled(float factor) {
        return new SoundTheme(name, hover.withVolume(hover.volume() * factor), click.withVolume(click.volume() * factor));
    }

    /**
     * Дописать звуки в стили {@link #INTERACTIVE_CLASSES} и {@code extraClasses} (тема меняется на месте и
     * возвращается). Прежние звуки этих классов заменяются.
     */
    public Theme applyTo(Theme theme, String... extraClasses) {
        for (String cls : INTERACTIVE_CLASSES) {
            apply(theme, cls);
        }
        for (String cls : extraClasses) {
            apply(theme, cls);
        }
        return theme;
    }

    private void apply(Theme theme, String cls) {
        theme.edit(cls, b -> b.set(Styles.HOVER_SOUND, hover).set(Styles.CLICK_SOUND, click));
    }

    @Override
    public String toString() {
        return "SoundTheme[" + name + "]";
    }
}
