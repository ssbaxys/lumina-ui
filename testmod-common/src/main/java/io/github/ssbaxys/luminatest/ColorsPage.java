package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorSpace;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.style.Transition;
import io.github.ssbaxys.lumina.widget.ColorPicker;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Slider;
import io.github.ssbaxys.lumina.widget.Toggle;

import java.util.Locale;
import java.util.function.IntUnaryOperator;

/** Цвета: выбор цвета, RGB/HSV-ползунки, операции над цветом, смешивание в разных пространствах. */
final class ColorsPage {
    private ColorsPage() {
    }

    static UiElement create() {
        Signal<Integer> color = Signal.of(Tokens.ACCENT.defaultValue());
        Signal<Boolean> live = Signal.of(true);

        ColorPicker picker = new ColorPicker(color).size(170, 110);
        Panel preview = Panel.box(FlexLayout.column()).size(56, 56);
        preview.style(b -> b.set(Styles.RADIUS, Radii.all(8)).set(Styles.BACKGROUND, Brush.solid(color.peek())));
        color.subscribe(c -> preview.setStyle(preview.inlineStyle().toBuilder()
                .set(Styles.BACKGROUND, Brush.solid(c))
                .set(Styles.GLOW, new Shadow(0, 0, 12, 0, Color.withAlpha(c, 0x90)))
                .build()));
        // Живое применение к теме: меняется токен ACCENT — перекрашивается вся витрина.
        color.subscribe(c -> {
            UiRoot root = picker.root();
            if (live.peek() && root != null) {
                root.theme().set(Tokens.ACCENT, Color.withAlpha(c, 0xFF));
            }
        });

        Panel info = Ui.column(3,
                Label.bound(color.map(Color::toHex)),
                Label.bound(color.map(c -> String.format(Locale.ROOT, "rgb(%d, %d, %d)",
                        Color.red(c), Color.green(c), Color.blue(c)))).style(Ui.MUTED),
                Label.bound(color.map(c -> {
                    float[] hsl = Color.toHsl(c, new float[3]);
                    return String.format(Locale.ROOT, "hsl(%.0f, %.0f%%, %.0f%%)", hsl[0], hsl[1] * 100, hsl[2] * 100);
                })).style(Ui.MUTED),
                Label.bound(color.map(c -> {
                    float[] lch = Color.toOklch(c, new float[3]);
                    return String.format(Locale.ROOT, "oklch(%.0f%% %.3f %.0f)", lch[0] * 100, lch[1], lch[2]);
                })).style(Ui.MUTED),
                Label.bound(color.map(c -> String.format(Locale.ROOT, Tr.r("контраст с белым %.1f:1"),
                        Color.contrast(c, Color.WHITE)))).style(Ui.MUTED));

        Panel pickerRow = Panel.box(FlexLayout.row().gap(14).align(Align.START)).add(picker,
                Ui.column(8, preview, Ui.row(6, new Toggle(live), new Label(Tr.r("Красить тему")))), info);

        Panel chips = Ui.row(6, new Label(Tr.r("Готовые:")));
        for (String name : new String[]{"#5B8CFF", "tomato", "gold", "hsl(150, 70%, 45%)", "oklch(70% 0.2 300)",
                "rgb(34 211 238)", "hotpink", "orange"}) {
            int c = Color.parse(name);
            Panel chip = Ui.box(c, 16, 16);
            chip.style(b -> b.set(Styles.RADIUS, Radii.all(8))
                    .set(Styles.TRANSITION, Transition.of(0.15f))
                    .hovered(h -> h.set(Styles.SCALE, 1.25f)));
            chip.onClick(e -> color.set(c));
            chips.add(chip);
        }

        return Ui.page(
                Ui.section(Tr.r("Выбор цвета"), Tr.r("Квадрат насыщенности и яркости, оттенок, прозрачность. ")
                        + Tr.r("Цвет сразу становится акцентом темы"), pickerRow, chips),
                Ui.section(Tr.r("RGB и HSV"), Tr.r("Каналы двигаются вместе: меняете один — пересчитываются остальные"),
                        channelSliders(color)),
                Ui.section(Tr.r("Операции над цветом"), Tr.r("Считаются в OKLCH — светлота и насыщенность ощущаются ровно"),
                        operations(color)),
                Ui.section(Tr.r("Смешивание в разных пространствах"), Tr.r("Один и тот же переход между двумя цветами"),
                        mixing()));
    }

    private static Panel channelSliders(Signal<Integer> color) {
        Panel col = Ui.column(5).width(Dim.FILL);
        String[] names = {"R", "G", "B", "H", "S", "V"};
        float[] max = {255, 255, 255, 360, 100, 100};
        for (int i = 0; i < names.length; i++) {
            int ch = i;
            Signal<Float> v = Signal.of(channel(color.peek(), ch));
            boolean[] syncing = {false};
            v.subscribe(x -> {
                if (!syncing[0]) {
                    color.set(withChannel(color.peek(), ch, x));
                }
            });
            color.subscribe(c -> {
                syncing[0] = true;
                v.set(channel(c, ch));
                syncing[0] = false;
            });
            Sketch track = Sketch.wide(6, (canvas, w, h, t) -> {
                int c = color.peek();
                int steps = ch >= 3 ? 7 : 2;
                int[] stops = new int[steps];
                for (int s = 0; s < steps; s++) {
                    stops[s] = withChannel(c, ch, max[ch] * s / (steps - 1));
                }
                canvas.fillRoundRect(0, 0, w, h, Radii.all(3), Brush.linear(90, stops).inSpace(ColorSpace.SRGB));
            });
            col.add(Ui.row(8,
                    new Label(names[i]).width(Dim.px(12)),
                    new Slider(v, 0, max[i]).step(1f).width(Dim.px(150)),
                    Ui.column(0, track).width(Dim.px(120)),
                    Label.bound(v.map(x -> String.valueOf(Math.round(x))))));
        }
        return col;
    }

    private static float channel(int c, int ch) {
        float[] hsv = Color.toHsv(c, new float[3]);
        return switch (ch) {
            case 0 -> Color.red(c);
            case 1 -> Color.green(c);
            case 2 -> Color.blue(c);
            case 3 -> Math.round(hsv[0]);
            case 4 -> Math.round(hsv[1] * 100f);
            default -> Math.round(hsv[2] * 100f);
        };
    }

    private static int withChannel(int c, int ch, float v) {
        int i = Math.round(v);
        if (ch < 3) {
            return ch == 0 ? Color.withRed(c, i) : ch == 1 ? Color.withGreen(c, i) : Color.withBlue(c, i);
        }
        float[] hsv = Color.toHsv(c, new float[3]);
        hsv[ch - 3] = ch == 3 ? v : v / 100f;
        return Color.hsva(hsv[0], hsv[1], hsv[2], Color.alphaF(c));
    }

    private static Panel operations(Signal<Integer> color) {
        Panel row = Panel.box(FlexLayout.row().gap(8).align(Align.START)).width(Dim.FILL);
        op(row, color, Tr.r("исходный"), c -> c);
        op(row, color, Tr.r("светлее"), c -> Color.lighten(c, 0.15f));
        op(row, color, Tr.r("темнее"), c -> Color.darken(c, 0.15f));
        op(row, color, Tr.r("насыщ."), c -> Color.saturate(c, 0.08f));
        op(row, color, Tr.r("тусклее"), c -> Color.desaturate(c, 0.08f));
        op(row, color, "+30°", c -> Color.rotateHue(c, 30f));
        op(row, color, Tr.r("дополн."), Color::complement);
        op(row, color, Tr.r("серый"), Color::grayscale);
        op(row, color, Tr.r("инверсия"), Color::invert);
        op(row, color, Tr.r("текст"), c -> Color.readableOn(c));
        return row;
    }

    private static void op(Panel row, Signal<Integer> color, String caption, IntUnaryOperator fn) {
        Sketch chip = new Sketch(36, 26, (canvas, w, h, t) ->
                canvas.fillRoundRect(0, 0, w, h, Radii.all(5), Brush.solid(fn.applyAsInt(Color.withAlpha(color.peek(), 0xFF)))));
        row.add(Ui.labeled(chip, caption));
    }

    private static Panel mixing() {
        Panel col = Ui.column(5).width(Dim.FILL);
        int[][] pairs = {{0xFFFF2040, 0xFF2060FF}, {0xFFFFE000, 0xFF2040FF}, {0xFF00E0A0, 0xFFFF40C0}};
        for (ColorSpace space : ColorSpace.values()) {
            Panel row = Ui.row(8, new Label(space.name()).width(Dim.px(52)));
            for (int[] p : pairs) {
                Sketch bar = new Sketch(110, 14, (canvas, w, h, t) ->
                        canvas.fillRoundRect(0, 0, w, h, Radii.all(4), Brush.linear(90, p[0], p[1]).inSpace(space)));
                row.add(bar);
            }
            col.add(row);
        }
        col.add(Ui.muted(Tr.r("sRGB темнеет в середине, OKLAB ровный, OKLCH и HSV идут по кругу оттенков")));
        return col;
    }
}
