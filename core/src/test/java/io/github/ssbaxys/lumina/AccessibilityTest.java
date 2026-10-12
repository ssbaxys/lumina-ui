package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.a11y.Accessibility;
import io.github.ssbaxys.lumina.a11y.AccessibleThemes;
import io.github.ssbaxys.lumina.a11y.ColorVision;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorFilter;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Фильтры цвета, высокий контраст, крупный текст, без анимаций, рамка фокуса и озвучка. */
class AccessibilityTest {
    private CapturePlatform platform;

    @BeforeEach
    void init() {
        platform = CapturePlatform.install();
    }

    private static float distance(int a, int b) {
        float dr = Color.red(a) - Color.red(b);
        float dg = Color.green(a) - Color.green(b);
        float db = Color.blue(a) - Color.blue(b);
        return (float) Math.sqrt(dr * dr + dg * dg + db * db);
    }

    @Test
    void colorVisionSimulationAndCorrection() {
        int red = 0xFFE04040;
        int green = 0xFF40B040;
        for (ColorVision v : new ColorVision[]{ColorVision.PROTANOPIA, ColorVision.DEUTERANOPIA}) {
            // Серые не меняются, альфа сохраняется.
            assertTrue(distance(v.simulate(0xFF808080), 0xFF808080) <= 2f, v.name());
            assertEquals(0x40, Color.alpha(v.correct(0x40E04040)), v.name());
            // Красный и зелёный при таком зрении ближе друг к другу, чем на самом деле…
            float seen = distance(v.simulate(red), v.simulate(green));
            assertTrue(seen < distance(red, green), v.name());
            // …а после коррекции различаются лучше.
            float corrected = distance(v.simulate(v.correct(red)), v.simulate(v.correct(green)));
            assertTrue(corrected > seen, v.name() + " " + corrected + " vs " + seen);
        }
        int blue = 0xFF3050E0;
        int teal = 0xFF30B0A0;
        assertTrue(distance(ColorVision.TRITANOPIA.simulate(blue), ColorVision.TRITANOPIA.simulate(teal)) < distance(blue, teal));
        assertEquals(red, ColorVision.NORMAL.correct(red));
        assertSame(ColorFilter.IDENTITY, ColorVision.NORMAL.simulate());
        ColorFilter cached = ColorVision.DEUTERANOPIA.correct();
        assertEquals(cached.filter(red), cached.filter(red));
        assertEquals(ColorVision.DEUTERANOPIA.correct(red), cached.filter(red));
    }

    @Test
    void canvasFilterAppliesToWholeFrameAndIsRemovedAfter() {
        Panel box = Panel.box(new StackLayout(Align.START, Align.START)).size(40, 20);
        box.style(b -> b.set(Styles.BACKGROUND, Brush.solid(0xFFE04040)));
        UiRoot root = new UiRoot(Panel.box(new StackLayout(Align.START, Align.START)).fill().add(box));
        root.resize(100, 100);
        CapturePlatform.ColorCanvas canvas = new CapturePlatform.ColorCanvas();

        canvas.begin(100, 100);
        root.render(canvas, 0f);
        canvas.end();
        assertTrue(canvas.colors.contains(0xFFE04040));

        root.accessibility().colorVision(ColorVision.DEUTERANOPIA, Accessibility.ColorMode.SIMULATE);
        canvas.begin(100, 100);
        root.render(canvas, 0f);
        canvas.end();
        assertTrue(canvas.colors.contains(ColorVision.DEUTERANOPIA.simulate(0xFFE04040)), canvas.colors.toString());
        assertTrue(!canvas.colors.contains(0xFFE04040));
        assertNull(canvas.colorFilter(), "filter is per frame");

        root.accessibility().extraFilter(ColorFilter.grayscale()).colorVision(ColorVision.NORMAL);
        canvas.begin(100, 100);
        root.render(canvas, 0f);
        canvas.end();
        assertTrue(canvas.colors.contains(Color.grayscale(0xFFE04040)));
    }

    @Test
    void highContrastThemesMeetWcag() {
        for (Theme t : new Theme[]{AccessibleThemes.highContrastDark(), AccessibleThemes.highContrastLight(),
                AccessibleThemes.highContrast(Theme.dark().set(Tokens.ACCENT, 0xFF202060)),
                AccessibleThemes.highContrast(Theme.light().set(Tokens.ACCENT, 0xFFFFFF80))}) {
            int bg = t.get(Tokens.SURFACE);
            assertTrue(Color.contrast(t.get(Tokens.TEXT), bg) >= 7f);
            assertTrue(Color.contrast(t.get(Tokens.ACCENT), bg) >= AccessibleThemes.MIN_CONTRAST,
                    Color.toHex(t.get(Tokens.ACCENT)) + " on " + Color.toHex(bg));
            assertEquals(2f, t.style("panel").resolve(0)[Styles.BORDER_WIDTH.id()]);
            assertTrue(t.style("button").resolve(State.FOCUSED)[Styles.OUTLINE.id()] != null);
        }
        Theme filtered = AccessibleThemes.filterColors(Theme.dark(), ColorVision.PROTANOPIA.correct());
        assertEquals(ColorVision.PROTANOPIA.correct(Theme.dark().get(Tokens.ACCENT)), filtered.get(Tokens.ACCENT));
        assertEquals(0f, AccessibleThemes.reducedMotion(Theme.dark()).get(Tokens.MOTION));
    }

    @Test
    void largerTextScalesEveryFontSize() {
        Label label = new Label("Привет");
        Button big = new Button("Большая").styleClass("button big");
        Panel col = Panel.column().add(label, big);
        UiRoot root = new UiRoot(col);
        root.resize(400, 300);
        root.tick(0f);
        float h = label.measuredHeight();
        float bigScale = big.computedStyle().get(Styles.FONT_SCALE);
        assertEquals(1.25f, bigScale);

        root.accessibility().textScale(2f);
        root.tick(0f);
        assertEquals(2f, label.computedStyle().get(Styles.FONT_SCALE));
        assertEquals(h * 2f, label.measuredHeight(), 0.01f);
        assertEquals(2.5f, big.computedStyle().get(Styles.FONT_SCALE), 1e-4f);

        root.accessibility().textScale(1f);
        root.tick(0f);
        assertEquals(h, label.measuredHeight(), 0.01f);
    }

    @Test
    void reducedMotionMakesTransitionsInstant() {
        Button b = new Button("OK");
        UiRoot root = new UiRoot(Panel.column().add(b));
        root.resize(200, 100);
        root.tick(0f);
        Object idle = b.computedStyle().get(Styles.BACKGROUND);
        b.setState(State.HOVERED, true);
        assertEquals(idle, b.computedStyle().get(Styles.BACKGROUND), "animated by default");
        b.setState(State.HOVERED, false);
        root.tick(1f);

        root.accessibility().reducedMotion(true);
        b.setState(State.HOVERED, true);
        assertNotEquals(idle, b.computedStyle().get(Styles.BACKGROUND), "instant with reduced motion");
        assertEquals(root.theme().style("button").resolve(State.HOVERED)[Styles.BACKGROUND.id()],
                b.computedStyle().get(Styles.BACKGROUND));
    }

    @Test
    void focusRingAndNarration() {
        Button ok = new Button("Готово");
        Button other = new Button("Отмена");
        UiRoot root = new UiRoot(Panel.column().add(ok, other));
        root.resize(200, 100);
        CapturePlatform.ColorCanvas canvas = new CapturePlatform.ColorCanvas();
        root.setFocus(ok);
        canvas.begin(200, 100);
        root.render(canvas, 0f);
        canvas.end();
        java.util.List<String> plainOps = new java.util.ArrayList<>(canvas.ops);

        root.accessibility().focusAlwaysVisible(true);
        canvas.begin(200, 100);
        root.render(canvas, 0f);
        canvas.end();
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("shape 8.0,8.0,80.0,37.0 s=4.0")), plainOps + " vs " + canvas.ops);
        assertTrue(canvas.ops.stream().anyMatch(o -> o.startsWith("shape 9.0,9.0,79.0,36.0 s=2.0")), canvas.ops.toString());
        assertTrue(plainOps.stream().noneMatch(o -> o.endsWith("s=4.0")));
        assertTrue(platform.narrated.isEmpty());

        root.accessibility().narration(true);
        root.setFocus(other);
        assertEquals("Отмена", platform.narrated.get(platform.narrated.size() - 1));
        other.setAccessibleLabel("Отменить изменения");
        root.setFocus(ok);
        root.setFocus(other);
        assertEquals("Отменить изменения", platform.narrated.get(platform.narrated.size() - 1));
    }
}
