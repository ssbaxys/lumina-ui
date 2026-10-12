package io.github.ssbaxys.lumina.style;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Outline;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Тема: стили по имени класса виджета ("button", "panel"...) и значения токенов ({@link ThemeVar}).
 *
 * <ul>
 *   <li>Элемент берёт стиль своего класса и накладывает поверх inline-стиль. Классов может быть
 *       несколько через пробел ({@code "button danger"}) — они накладываются по порядку.</li>
 *   <li>Стили ссылаются на токены ({@link Style.Builder#bind}); {@link #set(ThemeVar, Object)} меняет
 *       токен на лету, и все элементы плавно перетекают в новые значения.</li>
 *   <li>Смена темы целиком — {@code UiRoot.setTheme}; {@link #copy()} даёт независимую копию для правок.</li>
 * </ul>
 */
public final class Theme {
    private static int globalRevision;

    private final Map<String, Style> styles;
    private Object[] vars;
    private final Map<String, Style> resolved = new HashMap<>();
    private int revision = ++globalRevision;

    public Theme() {
        this(new HashMap<>(), new Object[0]);
    }

    private Theme(Map<String, Style> styles, Object[] vars) {
        this.styles = styles;
        this.vars = vars;
    }

    /** Готовый (с подставленными токенами) стиль для одного или нескольких классов через пробел. */
    public Style style(String styleClass) {
        if (styleClass == null || styleClass.isEmpty()) {
            return Style.EMPTY;
        }
        Style cached = resolved.get(styleClass);
        if (cached != null) {
            return cached;
        }
        Style merged;
        if (styleClass.indexOf(' ') < 0) {
            merged = styles.getOrDefault(styleClass, Style.EMPTY);
        } else {
            merged = Style.EMPTY;
            for (String cls : styleClass.trim().split("\\s+")) {
                merged = merged.merge(styles.getOrDefault(cls, Style.EMPTY));
            }
        }
        Style result = resolve(merged);
        resolved.put(styleClass, result);
        return result;
    }

    /** Стиль класса как он задан (со ссылками на токены), или {@code null}. */
    public Style rawStyle(String styleClass) {
        return styles.get(styleClass);
    }

    public Theme put(String styleClass, Style style) {
        styles.put(styleClass, style);
        changed();
        return this;
    }

    /** Дополнить стиль класса: {@code theme.edit("button", b -> b.set(Styles.RADIUS, Radii.all(2)))}. */
    public Theme edit(String styleClass, Consumer<Style.Builder> edit) {
        Style.Builder b = styles.getOrDefault(styleClass, Style.EMPTY).toBuilder();
        edit.accept(b);
        return put(styleClass, b.build());
    }

    /** Изменить токен. Элементы подхватят новое значение в следующем кадре с переходом из их стиля. */
    public <T> Theme set(ThemeVar<T> var, T value) {
        int id = var.id();
        if (id >= vars.length) {
            vars = Arrays.copyOf(vars, Math.max(id + 1, ThemeVar.count()));
        }
        if (!java.util.Objects.equals(vars[id], value)) {
            vars[id] = value;
            changed();
        }
        return this;
    }

    public <T> T get(ThemeVar<T> var) {
        int id = var.id();
        Object v = id < vars.length ? vars[id] : null;
        return v != null ? var.cast(v) : var.defaultValue();
    }

    /** Подставить токены в стиль и применить множитель длительности {@link Tokens#MOTION}. */
    public Style resolve(Style style) {
        float motion = get(Tokens.MOTION);
        if (!style.isThemed() && motion == 1f) {
            return style;
        }
        return style.mapValues(v -> {
            Object out = v instanceof Style.ThemeRef ref ? ref.compute().apply(this) : v;
            return motion != 1f && out instanceof Transition t ? t.scaled(motion) : out;
        });
    }

    /** Независимая копия: стили и токены можно менять, не трогая оригинал. */
    public Theme copy() {
        return new Theme(new HashMap<>(styles), vars.clone());
    }

    /** Меняется при любом изменении темы; элементы по нему сбрасывают кэш стиля. */
    public int revision() {
        return revision;
    }

    private void changed() {
        resolved.clear();
        revision = ++globalRevision;
    }

    // ================================================================ встроенные темы

    /** Тёмная «стеклянная» тема по умолчанию. */
    public static Theme dark() {
        return base()
                .set(Tokens.ACCENT, 0xFF5B8CFF)
                .set(Tokens.TEXT, 0xFFE6E9F2)
                .set(Tokens.SURFACE, 0xF0202430)
                .set(Tokens.SURFACE_ALT, 0xF0151820)
                .set(Tokens.BORDER, 0x24FFFFFF)
                .set(Tokens.SHADOW, 0x90000000)
                .set(Tokens.WELL, 0x30000000);
    }

    /** Светлая тема. */
    public static Theme light() {
        return base()
                .set(Tokens.ACCENT, 0xFF3D6DF2)
                .set(Tokens.TEXT, 0xFF1D2330)
                .set(Tokens.SURFACE, 0xF8FFFFFF)
                .set(Tokens.SURFACE_ALT, 0xF8EEF1F7)
                .set(Tokens.BORDER, 0x22000000)
                .set(Tokens.SHADOW, 0x40000000)
                .set(Tokens.WELL, 0x14000000);
    }

    /** Стили виджетов, целиком построенные на токенах: тема = набор значений токенов. */
    private static Theme base() {
        Transition smooth = new Transition(0.16f, Easing.OUT_CUBIC);
        return new Theme()
                .put("panel", Style.builder()
                        .fromTheme(Styles.BACKGROUND, t -> Brush.vertical(t.get(Tokens.SURFACE), t.get(Tokens.SURFACE_ALT)))
                        .bind(Styles.RADIUS, Tokens.CORNER, Radii::all)
                        .set(Styles.BORDER_WIDTH, 1f)
                        .bind(Styles.BORDER_COLOR, Tokens.BORDER)
                        .bind(Styles.SHADOW, Tokens.SHADOW, c -> new Shadow(0, 8, 24, 0, c))
                        .set(Styles.PADDING, Insets.all(12))
                        .bind(Styles.TEXT_COLOR, Tokens.TEXT)
                        .bind(Styles.ACCENT_COLOR, Tokens.ACCENT)
                        .set(Styles.TRANSITION, new Transition(0.3f, Easing.OUT_CUBIC))
                        .build())
                .put("label", Style.builder().build())
                .put("button", Style.builder()
                        .bind(Styles.BACKGROUND, Tokens.ACCENT, a -> Brush.vertical(a, Color.darken(a, 0.07f)))
                        .bind(Styles.RADIUS, Tokens.CORNER, c -> Radii.all(c * 0.6f))
                        .set(Styles.PADDING, Insets.of(6, 14))
                        .bind(Styles.TEXT_COLOR, Tokens.ACCENT, a -> Color.luminance(a) > 0.5f ? 0xFF1D2330 : Color.WHITE)
                        .set(Styles.SHADOW, new Shadow(0, 2, 6, 0, 0x60000000))
                        .set(Styles.TRANSITION, smooth)
                        .hovered(h -> h
                                .bind(Styles.BACKGROUND, Tokens.ACCENT, a -> Brush.vertical(Color.lighten(a, 0.06f), a))
                                .bind(Styles.SHADOW, Tokens.ACCENT, a -> new Shadow(0, 4, 14, 0, Color.withAlpha(a, 0x80))))
                        .pressed(pr -> pr
                                .set(Styles.SCALE, 0.96f)
                                .bind(Styles.BACKGROUND, Tokens.ACCENT,
                                        a -> Brush.vertical(Color.darken(a, 0.07f), Color.darken(a, 0.15f))))
                        .focused(f -> f
                                .set(Styles.BORDER_WIDTH, 1.5f)
                                .set(Styles.BORDER_COLOR, 0xCCFFFFFF))
                        .disabled(d -> d.set(Styles.OPACITY, 0.45f))
                        .build())
                .put("scroll", Style.builder()
                        .bind(Styles.RADIUS, Tokens.CORNER, c -> Radii.all(c * 0.6f))
                        .bind(Styles.BACKGROUND, Tokens.WELL, Brush::solid)
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
                        .bind(Styles.BACKGROUND, Tokens.WELL, Brush::solid)
                        .build())
                .put("color-picker", Style.builder()
                        .bind(Styles.RADIUS, Tokens.CORNER, c -> Radii.all(c * 0.5f))
                        .set(Styles.TRANSITION, smooth)
                        .disabled(d -> d.set(Styles.OPACITY, 0.45f))
                        .build())
                // Поля ввода: "field" — рамка, варианты добавляются вторым классом ("field filled").
                .put("field", Style.builder()
                        .bind(Styles.BACKGROUND, Tokens.WELL, Brush::solid)
                        .bind(Styles.RADIUS, Tokens.CORNER, c -> Radii.all(c * 0.5f))
                        .set(Styles.BORDER_WIDTH, 1f)
                        .bind(Styles.BORDER_COLOR, Tokens.BORDER)
                        .set(Styles.PADDING, Insets.of(5, 7))
                        .bind(Styles.TEXT_COLOR, Tokens.TEXT)
                        .bind(Styles.ACCENT_COLOR, Tokens.ACCENT)
                        .set(Styles.TRANSITION, smooth)
                        .hovered(h -> h.bind(Styles.BORDER_COLOR, Tokens.TEXT, t -> Color.withAlpha(t, 0x50)))
                        .focused(f -> f
                                .bind(Styles.BORDER_COLOR, Tokens.ACCENT)
                                .bind(Styles.OUTLINE, Tokens.ACCENT, a -> Outline.of(2f, 0f, Color.withAlpha(a, 0x40))))
                        .disabled(d -> d.set(Styles.OPACITY, 0.45f))
                        .build())
                .put("filled", Style.builder()
                        .bind(Styles.BACKGROUND, Tokens.TEXT, t -> Brush.solid(Color.withAlpha(t, 0x14)))
                        .set(Styles.BORDER_COLOR, Color.TRANSPARENT)
                        .hovered(h -> h
                                .bind(Styles.BACKGROUND, Tokens.TEXT, t -> Brush.solid(Color.withAlpha(t, 0x20)))
                                .set(Styles.BORDER_COLOR, Color.TRANSPARENT))
                        .focused(f -> f
                                .bind(Styles.BACKGROUND, Tokens.TEXT, t -> Brush.solid(Color.withAlpha(t, 0x1A)))
                                .bind(Styles.BORDER_COLOR, Tokens.ACCENT))
                        .build())
                .put("underline", Style.builder()
                        .set(Styles.BACKGROUND, Brush.solid(Color.TRANSPARENT))
                        .set(Styles.BORDER_WIDTH, 0f)
                        .set(Styles.RADIUS, Radii.ZERO)
                        .set(Styles.PADDING, Insets.of(5, 2))
                        .set(Styles.UNDERLINE, true)
                        .bind(Styles.BORDER_COLOR, Tokens.TEXT, t -> Color.withAlpha(t, 0x40))
                        .hovered(h -> h.bind(Styles.BORDER_COLOR, Tokens.TEXT, t -> Color.withAlpha(t, 0x70)))
                        .focused(f -> f
                                .bind(Styles.BORDER_COLOR, Tokens.TEXT, t -> Color.withAlpha(t, 0x40))
                                .set(Styles.OUTLINE, Outline.NONE))
                        .build())
                .put("pill", Style.builder()
                        .set(Styles.RADIUS, Radii.all(100))
                        .set(Styles.PADDING, Insets.of(5, 10))
                        .build())
                // Контекстное меню: тёмная карточка, подсвеченный пункт — состояние ContextMenu.ACTIVE.
                .put("context-menu", Style.builder()
                        .fromTheme(Styles.BACKGROUND, t -> Brush.vertical(
                                Color.withAlpha(t.get(Tokens.SURFACE), 0xFA), Color.withAlpha(t.get(Tokens.SURFACE_ALT), 0xFA)))
                        .set(Styles.RADIUS, Radii.all(6))
                        .set(Styles.BORDER_WIDTH, 1f)
                        .bind(Styles.BORDER_COLOR, Tokens.BORDER)
                        .bind(Styles.SHADOW, Tokens.SHADOW, c -> new Shadow(0, 6, 18, 0, c))
                        .set(Styles.PADDING, Insets.all(4))
                        .bind(Styles.TEXT_COLOR, Tokens.TEXT)
                        .bind(Styles.ACCENT_COLOR, Tokens.ACCENT)
                        .build())
                .put("menu-item", Style.builder()
                        .set(Styles.RADIUS, Radii.all(4))
                        .set(Styles.PADDING, Insets.of(4, 8))
                        .set(Styles.TRANSITION, new Transition(0.08f, Easing.OUT_CUBIC))
                        .when(State.FIRST_CUSTOM, a -> a
                                .bind(Styles.BACKGROUND, Tokens.ACCENT, Brush::solid)
                                .bind(Styles.TEXT_COLOR, Tokens.ACCENT, c -> Color.luminance(c) > 0.5f ? 0xFF1D2330 : Color.WHITE))
                        .disabled(d -> d.set(Styles.OPACITY, 0.4f))
                        .build())
                .put("menu-separator", Style.builder()
                        .bind(Styles.BACKGROUND, Tokens.BORDER, Brush::solid)
                        .build());
    }
}
