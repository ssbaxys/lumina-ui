package io.github.ssbaxys.lumina.style;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;

/**
 * Современная плоская тема, как в окнах настроек десктопных приложений: ровные тёмные карточки,
 * тонкие рамки, кнопки-«таблетки», мягкий акцент. Строится поверх {@link Theme#dark()} и
 * переопределяет классы "panel", "button", "section", "tabs", "side-nav" и остальные.
 *
 * <pre>{@code
 * root.setTheme(ModernTheme.dark());                // розово-бежевый акцент
 * root.setTheme(ModernTheme.dark(0xFF8AB4F8));       // свой акцент
 * }</pre>
 */
public final class ModernTheme {
    public static final int DEFAULT_ACCENT = 0xFFE9C1B7;

    private ModernTheme() {
    }

    public static Theme dark() {
        return dark(DEFAULT_ACCENT);
    }

    public static Theme dark(int accent) {
        return modern(Theme.dark(), accent, 0xFFEDE7E4, 0xF5191716, 0xFF231F1E, 0x1EFFFFFF, 0xFF141211);
    }

    public static Theme light() {
        return light(0xFFB0705F);
    }

    public static Theme light(int accent) {
        return modern(Theme.light(), accent, 0xFF2A2321, 0xF8FBF8F7, 0xFFF1ECEA, 0x22000000, 0xFFE9E3E1);
    }

    private static Theme modern(Theme base, int accent, int text, int surface, int raised, int border, int well) {
        Transition smooth = new Transition(0.18f, Easing.OUT_CUBIC);
        int onAccent = Color.luminance(accent) > 0.5f ? 0xFF1E1A19 : Color.WHITE;
        return base
                .set(Tokens.ACCENT, accent)
                .set(Tokens.TEXT, text)
                .set(Tokens.SURFACE, surface)
                .set(Tokens.SURFACE_ALT, surface)
                .set(Tokens.BORDER, border)
                .set(Tokens.SHADOW, 0x66000000)
                .set(Tokens.WELL, well)
                .set(Tokens.CORNER, 9f)
                .put("panel", Style.builder()
                        .bind(Styles.BACKGROUND, Tokens.SURFACE, Brush::solid)
                        .bind(Styles.RADIUS, Tokens.CORNER, Radii::all)
                        .set(Styles.BORDER_WIDTH, 1f)
                        .bind(Styles.BORDER_COLOR, Tokens.BORDER)
                        .set(Styles.SHADOW, new Shadow(0, 6, 18, 0, 0x55000000))
                        .set(Styles.PADDING, Insets.all(12))
                        .bind(Styles.TEXT_COLOR, Tokens.TEXT)
                        .bind(Styles.ACCENT_COLOR, Tokens.ACCENT)
                        .set(Styles.TRANSITION, new Transition(0.3f, Easing.OUT_CUBIC))
                        .build())
                .put("section", Style.builder()
                        .bind(Styles.BACKGROUND, Tokens.SURFACE, Brush::solid)
                        .bind(Styles.RADIUS, Tokens.CORNER, Radii::all)
                        .set(Styles.BORDER_WIDTH, 1f)
                        .bind(Styles.BORDER_COLOR, Tokens.BORDER)
                        .set(Styles.PADDING, new Insets(8, 14, 12, 14))
                        .set(Styles.TRANSITION, smooth)
                        .focused(f -> f.bind(Styles.BORDER_COLOR, Tokens.ACCENT, a -> Color.withAlpha(a, 0x90)))
                        .build())
                .put("button", Style.builder()
                        .set(Styles.BACKGROUND, Brush.solid(raised))
                        .set(Styles.RADIUS, Radii.all(11))
                        .set(Styles.BORDER_WIDTH, 1f)
                        .set(Styles.BORDER_COLOR, Color.withAlpha(text, 0x40))
                        .set(Styles.PADDING, Insets.of(5, 16))
                        .set(Styles.TEXT_COLOR, text)
                        .set(Styles.TRANSITION, smooth)
                        .hovered(h -> h
                                .set(Styles.BACKGROUND, Brush.solid(Color.lerp(raised, text, 0.08f)))
                                .bind(Styles.BORDER_COLOR, Tokens.ACCENT, a -> Color.withAlpha(a, 0xB0)))
                        .pressed(p -> p.set(Styles.SCALE, 0.97f))
                        .focused(f -> f.bind(Styles.BORDER_COLOR, Tokens.ACCENT))
                        .disabled(d -> d.set(Styles.OPACITY, 0.4f))
                        .build())
                .put("tabs", Style.builder()
                        .set(Styles.PADDING, Insets.of(6, 10))
                        .set(Styles.TEXT_COLOR, text)
                        .set(Styles.ACCENT_COLOR, accent)
                        .build())
                .put("side-nav", Style.builder()
                        .set(Styles.PADDING, Insets.of(8, 4))
                        .set(Styles.TEXT_COLOR, text)
                        .set(Styles.ACCENT_COLOR, accent)
                        .build())
                .put("toggle", Style.builder()
                        .set(Styles.ACCENT_COLOR, accent)
                        .set(Styles.TRANSITION, smooth)
                        .focused(f -> f.set(Styles.BORDER_WIDTH, 1f).set(Styles.BORDER_COLOR, onAccent))
                        .disabled(d -> d.set(Styles.OPACITY, 0.4f))
                        .build())
                .put("slider", Style.builder()
                        .set(Styles.ACCENT_COLOR, accent)
                        .set(Styles.TRANSITION, smooth)
                        .disabled(d -> d.set(Styles.OPACITY, 0.4f))
                        .build());
    }
}
