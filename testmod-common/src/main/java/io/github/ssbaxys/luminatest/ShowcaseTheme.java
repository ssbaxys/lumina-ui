package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Transition;

/** Встроенные темы библиотеки, расширенные своими классами стилей ("nav", "card"). */
final class ShowcaseTheme {
    private ShowcaseTheme() {
    }

    static Theme dark() {
        return extend(Theme.dark(), 0xFFE6E9F2, 0xFF5B8CFF, 0x14FFFFFF);
    }

    static Theme light() {
        return extend(Theme.light(), 0xFF1D2330, 0xFF3D6DF2, 0x0C000000);
    }

    private static Theme extend(Theme theme, int text, int accent, int cardBg) {
        Transition smooth = Transition.of(0.18f);
        return theme
                .put("nav", Style.builder()
                        .set(Styles.BACKGROUND, Brush.solid(Color.withAlpha(text, 0)))
                        .set(Styles.RADIUS, Radii.all(6))
                        .set(Styles.PADDING, Insets.of(5, 8))
                        .set(Styles.TEXT_COLOR, text)
                        .set(Styles.TRANSITION, smooth)
                        .hovered(h -> h.set(Styles.BACKGROUND, Brush.solid(Color.withAlpha(text, 0x1C))))
                        .pressed(p -> p.set(Styles.SCALE, 0.97f))
                        .checked(c -> c
                                .set(Styles.BACKGROUND, Brush.horizontal(accent, Color.lerp(accent, 0xFFFFFFFF, 0.2f)))
                                .set(Styles.TEXT_COLOR, Color.WHITE)
                                .set(Styles.SHADOW, new Shadow(0, 2, 8, 0, Color.withAlpha(accent, 0x70))))
                        .focused(f -> f.set(Styles.BORDER_WIDTH, 1f).set(Styles.BORDER_COLOR, Color.withAlpha(text, 0x90)))
                        .build())
                .put("card", Style.builder()
                        .set(Styles.BACKGROUND, Brush.solid(cardBg))
                        .set(Styles.RADIUS, Radii.all(8))
                        .set(Styles.PADDING, Insets.all(10))
                        .set(Styles.BORDER_WIDTH, 1f)
                        .set(Styles.BORDER_COLOR, Color.withAlpha(text, 0x18))
                        .set(Styles.TRANSITION, Transition.of(0.22f))
                        .hovered(h -> h
                                .set(Styles.SCALE, 1.03f)
                                .set(Styles.BORDER_COLOR, Color.withAlpha(accent, 0xA0))
                                .set(Styles.SHADOW, new Shadow(0, 6, 16, 0, 0x50000000)))
                        .build());
    }
}
