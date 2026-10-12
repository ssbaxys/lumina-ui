package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorSpace;
import io.github.ssbaxys.lumina.render.ColorStops;
import io.github.ssbaxys.lumina.render.Gradients;
import io.github.ssbaxys.lumina.render.Radii;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorBrushTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    private static void assertColor(int expected, int actual, int tolerance) {
        for (int shift = 0; shift <= 24; shift += 8) {
            int e = expected >>> shift & 0xFF;
            int a = actual >>> shift & 0xFF;
            assertTrue(Math.abs(e - a) <= tolerance,
                    String.format("expected %08X but was %08X", expected, actual));
        }
    }

    @Test
    void parsesManyNotations() {
        assertEquals(0xFFFF0000, Color.parse("#f00"));
        assertEquals(0x80FF0000, Color.parse("#80ff0000"), "#AARRGGBB like Java ints");
        assertEquals(0xFF00FF00, Color.parse("rgb(0, 255, 0)"));
        assertColor(0x800000FF, Color.parse("rgba(0 0 255 / 50%)"), 1);
        assertColor(0xFFFF0000, Color.parse("hsl(0, 100%, 50%)"), 1);
        assertColor(0xFF00FFFF, Color.parse("hsv(0.5turn, 1, 1)"), 1);
        assertEquals(Color.WHITE, Color.parse("white"));
        assertColor(0xFFFFFFFF, Color.parse("oklch(100% 0 0)"), 1);
    }

    @Test
    void roundTripsColorSpaces() {
        int[] samples = {0xFF5B8CFF, 0xFFFF7A00, 0xFF123456, 0xFFFFFFFF, 0xFF000000, 0x80FF00FF};
        float[] tmp = new float[3];
        for (int c : samples) {
            Color.toHsv(c, tmp);
            assertColor(c, Color.hsva(tmp[0], tmp[1], tmp[2], Color.alphaF(c)), 1);
            Color.toHsl(c, tmp);
            assertColor(c, Color.hsla(tmp[0], tmp[1], tmp[2], Color.alphaF(c)), 1);
            Color.toOklch(c, tmp);
            assertColor(c, Color.oklcha(tmp[0], tmp[1], tmp[2], Color.alphaF(c)), 1);
            assertEquals(c, Color.parse(Color.toHex(c)));
        }
    }

    @Test
    void colorOperationsBehave() {
        int blue = 0xFF5B8CFF;
        assertTrue(Color.luminance(Color.lighten(blue, 0.2f)) > Color.luminance(blue));
        assertTrue(Color.luminance(Color.darken(blue, 0.2f)) < Color.luminance(blue));
        int gray = Color.grayscale(blue);
        assertEquals(Color.red(gray), Color.green(gray), 2);
        assertEquals(Color.green(gray), Color.blue(gray), 2);
        assertEquals(0xFF000000, Color.readableOn(Color.WHITE));
        assertEquals(Color.WHITE, Color.readableOn(Color.BLACK));
        assertEquals(21f, Color.contrast(Color.WHITE, Color.BLACK), 0.1f);
        assertColor(0xFF808080, Color.over(0x80FFFFFF, Color.BLACK), 1);
        // половина пути красный→зелёный в OKLCH обходит оттенок через жёлтый, а не через грязно-коричневый
        int mid = Color.lerp(0xFFFF0000, 0xFF00FF00, 0.5f, ColorSpace.OKLCH);
        assertTrue(Color.red(mid) > 150 && Color.green(mid) > 120, Color.toHex(mid));
    }

    @Test
    void colorStopsSampleAndHardStops() {
        ColorStops stops = ColorStops.builder()
                .add(0f, 0xFFFF0000)
                .hard(0.5f, 0xFFFF0000, 0xFF0000FF)
                .add(1f, 0xFF0000FF)
                .build();
        assertEquals(0xFFFF0000, stops.sample(0.49f, ColorSpace.SRGB));
        assertEquals(0xFF0000FF, stops.sample(0.51f, ColorSpace.SRGB));
        assertEquals(0xFFFF0000, stops.sample(-1f, ColorSpace.SRGB));
        ColorStops even = ColorStops.even(Color.BLACK, Color.WHITE);
        assertColor(0xFF808080, even.sample(0.5f, ColorSpace.SRGB), 1);
        assertEquals(even, ColorStops.even(Color.BLACK, Color.WHITE));
        assertEquals(Color.WHITE, even.reversed().sample(0f, ColorSpace.SRGB));
    }

    @Test
    void gradientsSampleByGeometry() {
        Brush.Linear down = Brush.vertical(Color.BLACK, Color.WHITE);
        assertTrue(down.isVertexExact());
        assertEquals(Color.BLACK, down.sample(0.5f, 0f, 10f, 10f));
        assertEquals(Color.WHITE, down.sample(0.5f, 1f, 10f, 10f));
        Brush.Gradient right = Brush.linear(90f, Color.BLACK, Color.WHITE);
        assertEquals(Color.BLACK, right.sample(0f, 0.5f, 10f, 10f));
        assertEquals(Color.WHITE, right.sample(1f, 0.5f, 10f, 10f));
        Brush.Gradient radial = Brush.radial(Color.WHITE, Color.BLACK);
        assertEquals(Color.WHITE, radial.sample(0.5f, 0.5f, 10f, 10f));
        assertFalse(Brush.linear(0f, Color.BLACK, Color.WHITE).inSpace(ColorSpace.OKLAB) instanceof Brush.Linear l
                && l.isVertexExact());
        assertEquals(12, Gradients.all().size());
    }

    @Test
    void brushLerpCrossesKinds() {
        Brush a = Brush.solid(Color.BLACK);
        Brush b = Brush.vertical(Color.WHITE, Color.WHITE);
        Brush mid = Brush.lerp(a, b, 0.5f);
        assertInstanceOf(Brush.Gradient.class, mid);
        assertColor(0xFF808080, ((Brush.Gradient) mid).sample(0.5f, 0.5f, 1f, 1f), 2);
        assertEquals(b, Brush.lerp(a, b, 1f));
    }

    @Test
    void canvasRoutesGradientsAndBlendModes() {
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(100, 100);
        canvas.fillRoundRect(0, 0, 50, 50, Radii.all(4), Brush.conic(0f, Color.BLACK, Color.WHITE));
        canvas.save();
        canvas.setBlendMode(BlendMode.ADD);
        canvas.fillRect(0, 0, 10, 10, Color.WHITE);
        canvas.restore();
        canvas.fillRect(0, 0, 10, 10, Color.WHITE);
        canvas.end();
        assertTrue(canvas.ops.stream().anyMatch(o -> o.contains("grad=3x2")), String.valueOf(canvas.ops));
        int add = canvas.ops.indexOf("blend ADD");
        int normal = canvas.ops.indexOf("blend NORMAL");
        assertTrue(add >= 0 && normal > add, String.valueOf(canvas.ops));
    }
}
