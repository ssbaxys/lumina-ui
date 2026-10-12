package io.github.ssbaxys.lumina.hud;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.anim.Interpolator;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.StyleKey;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.style.Transition;

/**
 * Свойства стиля и классы темы для виджетов HUD. Классы входят во все встроенные темы
 * ({@link Theme#dark()}, {@link Theme#light()} и производные); для своей темы с нуля — {@link #install}.
 *
 * <ul>
 *   <li>{@code "toast"} — карточка тоста ({@link Toast});</li>
 *   <li>{@code "hud-bar"} — полосы и шкалы ({@link SegmentedBar}, {@link BossBar}, {@link RadialGauge});</li>
 *   <li>{@code "hud-feed"} — лента уведомлений ({@link NotificationFeed});</li>
 *   <li>{@code "hud-timer"} — таймер ({@link TimerDisplay});</li>
 *   <li>{@code "hud-panel"} — полупрозрачная подложка для группы элементов HUD.</li>
 * </ul>
 */
public final class HudStyles {
    /** Цвет пустой части полос и шкал. */
    public static final StyleKey<Integer> TRACK_COLOR = StyleKey.builder("hud-track-color", Integer.class)
            .defaultValue(0x70000000).animated(Interpolator.COLOR).register();
    /** Цвет «плохого» значения: мало здоровья, таймер на исходе. */
    public static final StyleKey<Integer> LOW_COLOR = StyleKey.builder("hud-low-color", Integer.class)
            .defaultValue(0xFFF0506E).inherited().animated(Interpolator.COLOR).register();
    /** Подложка строк ленты уведомлений. */
    public static final StyleKey<Integer> ROW_COLOR = StyleKey.builder("hud-row-color", Integer.class)
            .defaultValue(0x90000000).animated(Interpolator.COLOR).register();

    private HudStyles() {
    }

    /** Добавить классы HUD в тему (существующие стили с теми же именами заменяются). */
    public static Theme install(Theme theme) {
        Transition smooth = new Transition(0.2f, Easing.OUT_CUBIC);
        return theme
                .put("toast", Style.builder()
                        .fromTheme(Styles.BACKGROUND, t -> Brush.vertical(
                                Color.withAlpha(t.get(Tokens.SURFACE), 0xF2), Color.withAlpha(t.get(Tokens.SURFACE_ALT), 0xF2)))
                        .bind(Styles.RADIUS, Tokens.CORNER, c -> Radii.all(c * 0.6f))
                        .set(Styles.BORDER_WIDTH, 1f)
                        .bind(Styles.BORDER_COLOR, Tokens.BORDER)
                        .bind(Styles.SHADOW, Tokens.SHADOW, c -> new Shadow(0, 4, 14, 0, c))
                        .set(Styles.PADDING, Insets.of(6, 8))
                        .bind(Styles.TEXT_COLOR, Tokens.TEXT)
                        .bind(Styles.ACCENT_COLOR, Tokens.ACCENT)
                        .bind(TRACK_COLOR, Tokens.TEXT, t -> Color.withAlpha(t, 0x24))
                        .build())
                .put("hud-bar", Style.builder()
                        .set(Styles.TEXT_COLOR, Color.WHITE)
                        .set(Styles.TEXT_SHADOW, true)
                        .bind(Styles.ACCENT_COLOR, Tokens.ACCENT)
                        .set(TRACK_COLOR, 0x70000000)
                        .set(Styles.TRANSITION, smooth)
                        .build())
                .put("hud-feed", Style.builder()
                        .set(Styles.TEXT_COLOR, Color.WHITE)
                        .set(Styles.TEXT_SHADOW, true)
                        .bind(Styles.ACCENT_COLOR, Tokens.ACCENT)
                        .bind(ROW_COLOR, Tokens.SHADOW, c -> Color.mulAlpha(Color.withAlpha(c, 0xFF), 0.55f))
                        .build())
                .put("hud-timer", Style.builder()
                        .bind(Styles.BACKGROUND, Tokens.SURFACE, c -> Brush.solid(Color.withAlpha(c, 0xC8)))
                        .bind(Styles.RADIUS, Tokens.CORNER, c -> Radii.all(c * 0.6f))
                        .set(Styles.BORDER_WIDTH, 1f)
                        .bind(Styles.BORDER_COLOR, Tokens.BORDER)
                        .set(Styles.PADDING, Insets.of(4, 8))
                        .bind(Styles.TEXT_COLOR, Tokens.TEXT)
                        .bind(Styles.ACCENT_COLOR, Tokens.ACCENT)
                        .bind(TRACK_COLOR, Tokens.TEXT, t -> Color.withAlpha(t, 0x24))
                        .build())
                .put("hud-panel", Style.builder()
                        .bind(Styles.BACKGROUND, Tokens.SURFACE, c -> Brush.solid(Color.withAlpha(c, 0xB0)))
                        .bind(Styles.RADIUS, Tokens.CORNER, c -> Radii.all(c * 0.5f))
                        .set(Styles.BORDER_WIDTH, 1f)
                        .bind(Styles.BORDER_COLOR, Tokens.BORDER)
                        .set(Styles.PADDING, Insets.all(6))
                        .bind(Styles.TEXT_COLOR, Tokens.TEXT)
                        .bind(Styles.ACCENT_COLOR, Tokens.ACCENT)
                        .set(Styles.TEXT_SHADOW, false)
                        .build());
    }
}
