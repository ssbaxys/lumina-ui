package io.github.ssbaxys.lumina.a11y;

import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorFilter;
import io.github.ssbaxys.lumina.render.Outline;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.ThemeVar;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Tokens;

import java.util.List;

/**
 * Темы и преобразования тем для доступности. Все методы возвращают новую тему, исходная не меняется.
 *
 * <pre>{@code
 * root.setTheme(AccessibleThemes.highContrast(Theme.dark()));
 * root.setTheme(AccessibleThemes.filterColors(theme, ColorVision.PROTANOPIA.correct()));  // только токены
 * }</pre>
 */
public final class AccessibleThemes {
    /** Минимальный контраст текста и акцента с фоном (WCAG AA для обычного текста). */
    public static final float MIN_CONTRAST = 4.5f;

    /** Классы, которым высокий контраст добавляет толстую рамку фокуса. */
    private static final List<String> FOCUSABLE = List.of("button", "image-button", "toggle", "checkbox", "slider",
            "field", "color-picker", "menu-item", "nav", "section", "window", "tabs", "side-nav");

    private AccessibleThemes() {
    }

    /** Высококонтрастная тёмная тема: белый текст на чёрном, яркий жёлтый акцент. */
    public static Theme highContrastDark() {
        return highContrast(Theme.dark().set(Tokens.ACCENT, 0xFFFFD400));
    }

    /** Высококонтрастная светлая тема: чёрный текст на белом, тёмно-синий акцент. */
    public static Theme highContrastLight() {
        return highContrast(Theme.light().set(Tokens.ACCENT, 0xFF0033B3));
    }

    /**
     * Высококонтрастный вариант темы: сплошной чёрный (или белый — для светлых тем) фон, текст и рамки
     * противоположного цвета, акцент с контрастом не меньше {@link #MIN_CONTRAST}, без теней, рамки панелей
     * толщиной 2 и заметное кольцо фокуса у интерактивных классов.
     */
    public static Theme highContrast(Theme base) {
        boolean dark = Color.luminance(Color.over(base.get(Tokens.SURFACE), Color.BLACK)) < 0.4f;
        int bg = dark ? Color.BLACK : Color.WHITE;
        int fg = dark ? Color.WHITE : Color.BLACK;
        int accent = ensureContrast(Color.withAlpha(base.get(Tokens.ACCENT), 255), bg, MIN_CONTRAST);
        Theme t = base.copy()
                .set(Tokens.TEXT, fg)
                .set(Tokens.SURFACE, bg)
                .set(Tokens.SURFACE_ALT, bg)
                .set(Tokens.BORDER, fg)
                .set(Tokens.WELL, dark ? 0xFF1E1E1E : 0xFFE6E6E6)
                .set(Tokens.SHADOW, Color.TRANSPARENT)
                .set(Tokens.ACCENT, accent);
        Outline ring = Outline.of(2f, 2f, accent);
        for (String cls : FOCUSABLE) {
            if (t.rawStyle(cls) != null) {
                t.edit(cls, b -> b.focused(f -> f.set(Styles.OUTLINE, ring)));
            }
        }
        for (String cls : List.of("panel", "window", "section", "field", "context-menu")) {
            if (t.rawStyle(cls) != null) {
                t.edit(cls, b -> b.set(Styles.BORDER_WIDTH, 2f));
            }
        }
        return t;
    }

    /**
     * Цвет, достаточно контрастный к фону: если контраста не хватает, цвет осветляется (на тёмном фоне)
     * или затемняется (на светлом) шагами по 5% светлоты, пока не станет не меньше {@code ratio}.
     */
    public static int ensureContrast(int color, int background, float ratio) {
        boolean darkBg = Color.luminance(background) < 0.5f;
        int c = color;
        for (int i = 0; i < 20 && Color.contrast(c, background) < ratio; i++) {
            c = darkBg ? Color.lighten(c, 0.05f) : Color.darken(c, 0.05f);
        }
        if (Color.contrast(c, background) < ratio) {
            c = Color.withAlpha(darkBg ? Color.WHITE : Color.BLACK, Color.alpha(color));
        }
        return c;
    }

    /**
     * Пропустить через фильтр цветовые токены темы (коррекция для дальтоников только для цветов темы,
     * без фильтра на весь кадр). Цвета, заданные в стилях числами, не меняются.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static Theme filterColors(Theme base, ColorFilter filter) {
        Theme t = base.copy();
        if (filter == null) {
            return t;
        }
        for (ThemeVar v : ThemeVar.all()) {
            if (v.type() == Integer.class) {
                t.set(v, filter.filter((Integer) base.get(v)));
            }
        }
        return t;
    }

    /** Тема без анимаций переходов ({@link Tokens#MOTION} = 0). */
    public static Theme reducedMotion(Theme base) {
        return base.copy().set(Tokens.MOTION, 0f);
    }
}
