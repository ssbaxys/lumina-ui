package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.style.Transition;

/**
 * Встроенные темы библиотеки, расширенные своими классами стилей. Все цвета берутся из токенов
 * ({@link Tokens#ACCENT}, {@link Tokens#TEXT}...), поэтому смена акцента или скругления перекрашивает
 * и навигацию, и карточки, и модификаторы кнопок.
 */
final class ShowcaseTheme {
    private ShowcaseTheme() {
    }

    static Theme dark() {
        return extend(Theme.dark());
    }

    static Theme light() {
        return extend(Theme.light());
    }

    /** Новая тема с токенами (акцент, скругление, скорость), перенесёнными из прежней. */
    static Theme carryTokens(Theme next, Theme previous) {
        return next
                .set(Tokens.ACCENT, previous.get(Tokens.ACCENT))
                .set(Tokens.CORNER, previous.get(Tokens.CORNER))
                .set(Tokens.MOTION, previous.get(Tokens.MOTION));
    }

    private static Theme extend(Theme theme) {
        Transition smooth = Transition.of(0.18f);
        return theme
                .put("nav", Style.builder()
                        .bind(Styles.BACKGROUND, Tokens.TEXT, t -> Brush.solid(Color.withAlpha(t, 0)))
                        .bind(Styles.RADIUS, Tokens.CORNER, c -> Radii.all(c * 0.6f))
                        .set(Styles.PADDING, Insets.of(4, 8))
                        .bind(Styles.TEXT_COLOR, Tokens.TEXT)
                        .set(Styles.TRANSITION, smooth)
                        .hovered(h -> h.bind(Styles.BACKGROUND, Tokens.TEXT, t -> Brush.solid(Color.withAlpha(t, 0x1C))))
                        .pressed(p -> p.set(Styles.SCALE, 0.97f))
                        .checked(c -> c
                                .bind(Styles.BACKGROUND, Tokens.ACCENT, a -> Brush.horizontal(a, Color.lighten(a, 0.08f)))
                                .bind(Styles.TEXT_COLOR, Tokens.ACCENT,
                                        a -> Color.luminance(a) > 0.5f ? 0xFF1D2330 : Color.WHITE)
                                .bind(Styles.SHADOW, Tokens.ACCENT, a -> new Shadow(0, 2, 8, 0, Color.withAlpha(a, 0x70))))
                        .focused(f -> f.set(Styles.BORDER_WIDTH, 1f)
                                .bind(Styles.BORDER_COLOR, Tokens.TEXT, t -> Color.withAlpha(t, 0x90)))
                        .build())
                .put("card", Style.builder()
                        .bind(Styles.BACKGROUND, Tokens.TEXT, t -> Brush.solid(Color.withAlpha(t, 0x12)))
                        .bind(Styles.RADIUS, Tokens.CORNER, c -> Radii.all(c * 0.8f))
                        .set(Styles.PADDING, Insets.all(10))
                        .set(Styles.BORDER_WIDTH, 1f)
                        .bind(Styles.BORDER_COLOR, Tokens.TEXT, t -> Color.withAlpha(t, 0x18))
                        .set(Styles.TRANSITION, Transition.of(0.22f))
                        .hovered(h -> h
                                .set(Styles.SCALE, 1.03f)
                                .bind(Styles.BORDER_COLOR, Tokens.ACCENT, a -> Color.withAlpha(a, 0xA0))
                                .set(Styles.SHADOW, new Shadow(0, 6, 16, 0, 0x50000000)))
                        .build())
                // Модификаторы кнопки: накладываются поверх "button" через styleClass("button danger").
                .put("danger", tinted(0xFFF0506E))
                .put("success", tinted(0xFF34C38F))
                .put("ghost", Style.builder()
                        .bind(Styles.BACKGROUND, Tokens.ACCENT, a -> Brush.solid(Color.withAlpha(a, 0)))
                        .bind(Styles.TEXT_COLOR, Tokens.ACCENT, a -> Color.lighten(a, 0.1f))
                        .set(Styles.BORDER_WIDTH, 1f)
                        .bind(Styles.BORDER_COLOR, Tokens.ACCENT, a -> Color.withAlpha(a, 0x90))
                        .set(Styles.SHADOW, Shadow.NONE)
                        .hovered(h -> h
                                .bind(Styles.BACKGROUND, Tokens.ACCENT, a -> Brush.solid(Color.withAlpha(a, 0x30)))
                                .set(Styles.SHADOW, Shadow.NONE))
                        .pressed(p -> p.bind(Styles.BACKGROUND, Tokens.ACCENT, a -> Brush.solid(Color.withAlpha(a, 0x50))))
                        .build())
                .put("pill", Style.builder()
                        .set(Styles.RADIUS, Radii.all(20))
                        .set(Styles.PADDING, Insets.of(6, 18))
                        .build());
    }

    private static Style tinted(int color) {
        return Style.builder()
                .set(Styles.BACKGROUND, Brush.vertical(color, Color.darken(color, 0.07f)))
                .set(Styles.TEXT_COLOR, Color.WHITE)
                .hovered(h -> h
                        .set(Styles.BACKGROUND, Brush.vertical(Color.lighten(color, 0.06f), color))
                        .set(Styles.SHADOW, new Shadow(0, 4, 14, 0, Color.withAlpha(color, 0x80))))
                .pressed(p -> p.set(Styles.BACKGROUND, Brush.vertical(Color.darken(color, 0.07f), Color.darken(color, 0.15f))))
                .build();
    }
}
