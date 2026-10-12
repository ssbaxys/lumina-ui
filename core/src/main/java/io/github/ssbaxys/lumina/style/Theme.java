package io.github.ssbaxys.lumina.style;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;

import java.util.HashMap;
import java.util.Map;

/**
 * Тема: стили по имени класса виджета ("button", "panel"...). Элемент берёт стиль из темы и
 * накладывает поверх свой inline-стиль. Смена темы на лету перестраивает стили всего дерева.
 */
public final class Theme {
    private static int globalRevision;

    private final Map<String, Style> styles = new HashMap<>();
    private int revision = ++globalRevision;

    public Style style(String styleClass) {
        Style s = styleClass == null ? null : styles.get(styleClass);
        return s == null ? Style.EMPTY : s;
    }

    public Theme put(String styleClass, Style style) {
        styles.put(styleClass, style);
        revision = ++globalRevision;
        return this;
    }

    /** Меняется при любом изменении темы; элементы по нему сбрасывают кэш стиля. */
    public int revision() {
        return revision;
    }

    /** Тёмная «стеклянная» тема по умолчанию. */
    public static Theme dark() {
        return build(new Palette(
                0xF0202430, 0xF0151820, 0x24FFFFFF, 0x90000000, 0xFFE6E9F2,
                0xFF5B8CFF, 0xFF3F6FE8, 0xFF7AA2FF, 0xFF2F57C0, 0x30000000));
    }

    /** Светлая тема. */
    public static Theme light() {
        return build(new Palette(
                0xF8FFFFFF, 0xF8EEF1F7, 0x22000000, 0x40000000, 0xFF1D2330,
                0xFF3D6DF2, 0xFF2F5BDA, 0xFF5C86F7, 0xFF2448B0, 0x14000000));
    }

    private record Palette(int panelTop, int panelBottom, int panelBorder, int panelShadow, int text,
                           int accent, int accentDark, int accentLight, int accentPressed, int well) {
    }

    private static Theme build(Palette p) {
        Transition smooth = new Transition(0.16f, Easing.OUT_CUBIC);
        return new Theme()
                .put("panel", Style.builder()
                        .set(Styles.BACKGROUND, Brush.vertical(p.panelTop, p.panelBottom))
                        .set(Styles.RADIUS, Radii.all(10))
                        .set(Styles.BORDER_WIDTH, 1f)
                        .set(Styles.BORDER_COLOR, p.panelBorder)
                        .set(Styles.SHADOW, new Shadow(0, 8, 24, 0, p.panelShadow))
                        .set(Styles.PADDING, Insets.all(12))
                        .set(Styles.TEXT_COLOR, p.text)
                        .set(Styles.ACCENT_COLOR, p.accent)
                        .set(Styles.TRANSITION, new Transition(0.3f, Easing.OUT_CUBIC))
                        .build())
                .put("label", Style.builder().build())
                .put("button", Style.builder()
                        .set(Styles.BACKGROUND, Brush.vertical(p.accent, p.accentDark))
                        .set(Styles.RADIUS, Radii.all(6))
                        .set(Styles.PADDING, Insets.of(6, 14))
                        .set(Styles.TEXT_COLOR, Color.WHITE)
                        .set(Styles.SHADOW, new Shadow(0, 2, 6, 0, 0x60000000))
                        .set(Styles.TRANSITION, smooth)
                        .hovered(h -> h
                                .set(Styles.BACKGROUND, Brush.vertical(p.accentLight, p.accent))
                                .set(Styles.SHADOW, new Shadow(0, 4, 14, 0, Color.withAlpha(p.accent, 0x80))))
                        .pressed(pr -> pr
                                .set(Styles.SCALE, 0.96f)
                                .set(Styles.BACKGROUND, Brush.vertical(p.accentDark, p.accentPressed)))
                        .focused(f -> f
                                .set(Styles.BORDER_WIDTH, 1.5f)
                                .set(Styles.BORDER_COLOR, 0xCCFFFFFF))
                        .disabled(d -> d.set(Styles.OPACITY, 0.45f))
                        .build())
                .put("scroll", Style.builder()
                        .set(Styles.RADIUS, Radii.all(6))
                        .set(Styles.BACKGROUND, Brush.solid(p.well))
                        .set(Styles.TRANSITION, smooth)
                        .build())
                .put("toggle", Style.builder()
                        .set(Styles.RADIUS, Radii.all(7))
                        .set(Styles.TRANSITION, smooth)
                        .focused(f -> f.set(Styles.BORDER_WIDTH, 1f).set(Styles.BORDER_COLOR, 0xCCFFFFFF))
                        .disabled(d -> d.set(Styles.OPACITY, 0.45f))
                        .build())
                .put("slider", Style.builder()
                        .set(Styles.TRANSITION, smooth)
                        .disabled(d -> d.set(Styles.OPACITY, 0.45f))
                        .build())
                .put("progress", Style.builder()
                        .set(Styles.RADIUS, Radii.all(3))
                        .set(Styles.BACKGROUND, Brush.solid(p.well))
                        .build());
    }
}
