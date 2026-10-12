package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.markup.Stylesheet;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ScrollPanel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Размытие фона и отсечение по скруглённой фигуре: запасной путь и путь со снимками кадра. */
class BlurClipTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    /** Canvas, который умеет снимки кадра (как бэкенд загрузчика). */
    static final class SnapshotCanvas extends TestSupport.RecordingCanvas {
        final List<Float> blurs = new ArrayList<>();
        int snapshots;
        int slots = 4;

        @Override
        protected TextureRef captureBackdrop(float radius) {
            blurs.add(radius);
            ops.add("capture-blur " + radius);
            return TextureRef.of("lumina:snap/blur");
        }

        @Override
        protected boolean backdropBlurAvailable() {
            return true;
        }

        @Override
        protected TextureRef captureSnapshot(int slot) {
            snapshots++;
            ops.add("capture-snapshot " + slot);
            return TextureRef.of("lumina:snap/" + slot);
        }

        @Override
        protected int snapshotSlots() {
            return slots;
        }
    }

    private static long count(List<String> ops, String prefix) {
        return ops.stream().filter(o -> o.startsWith(prefix)).count();
    }

    @Test
    void blurFallsBackToFrostedTintWithoutBackend() {
        TestSupport.RecordingCanvas c = new TestSupport.RecordingCanvas();
        assertFalse(c.supportsBackdropBlur());
        assertFalse(c.supportsRoundClip());
        c.begin(200, 100);
        c.drawBackdropBlur(10, 10, 80, 40, Radii.all(8), 10f);
        c.end();
        assertTrue(count(c.ops, "shape") >= 1, c.ops.toString());
        assertEquals(0, count(c.ops, "tex"));
    }

    @Test
    void blurUsesBackendSnapshotWithScreenUvs() {
        SnapshotCanvas c = new SnapshotCanvas();
        assertTrue(c.supportsBackdropBlur());
        c.begin(200, 100);
        c.save();
        c.scale(2f, 2f);
        c.drawBackdropBlur(10, 10, 40, 20, Radii.ZERO, 6f);
        c.restore();
        c.end();
        assertEquals(List.of(12f), c.blurs, "radius scaled to screen pixels");
        assertEquals(1, count(c.ops, "tex lumina:snap/blur"));
    }

    @Test
    void elementWithBackdropBlurStyleBlursBeforeBackground() {
        Panel card = Panel.column().size(100, 60).style(b -> b
                .set(Styles.BACKDROP_BLUR, 8f)
                .set(Styles.RADIUS, Radii.all(10))
                .set(Styles.BACKGROUND, Brush.solid(0x40FFFFFF)));
        Panel host = Panel.box(new StackLayout(Align.START, Align.START)).add(card);
        UiRoot root = new UiRoot(host, Theme.dark());
        root.resize(300, 200);
        SnapshotCanvas c = new SnapshotCanvas();
        c.begin(300, 200);
        root.render(c, 0f);
        c.end();
        int blur = c.ops.indexOf("capture-blur 8.0");
        assertTrue(blur >= 0, c.ops.toString());
        assertTrue(c.ops.subList(blur, c.ops.size()).stream().anyMatch(o -> o.startsWith("tex lumina:snap/blur")));
        // фон рисуется после размытия
        assertTrue(c.ops.subList(blur, c.ops.size()).stream().anyMatch(o -> o.startsWith("shape")));
    }

    @Test
    void roundClipRestoresCornersFromSnapshot() {
        SnapshotCanvas c = new SnapshotCanvas();
        assertTrue(c.supportsRoundClip());
        c.begin(200, 100);
        c.save();
        c.clipRoundRect(10, 10, 100, 50, Radii.all(10));
        c.fillRect(0, 0, 200, 100, 0xFFFF0000);
        int before = (int) count(c.ops, "tex lumina:snap/0");
        c.restore();
        c.end();
        assertEquals(0, before, "corners are painted only on restore");
        assertEquals(1, c.snapshots);
        // четыре угла по 8 треугольников
        assertEquals(32, count(c.ops, "tex lumina:snap/0"));
    }

    @Test
    void roundClipWithoutSlotsIsRectangular() {
        SnapshotCanvas c = new SnapshotCanvas();
        c.slots = 0;
        c.begin(200, 100);
        c.save();
        c.clipRoundRect(10, 10, 100, 50, Radii.all(10));
        c.fillRect(0, 0, 200, 100, 0xFFFF0000);
        c.restore();
        c.end();
        assertEquals(0, c.snapshots);
        assertTrue(count(c.ops, "clip 10.0,10.0,110.0,60.0") >= 1, c.ops.toString());
    }

    @Test
    void nestedRoundClipsUseSeparateSlots() {
        SnapshotCanvas c = new SnapshotCanvas();
        c.begin(200, 100);
        c.save();
        c.clipRoundRect(0, 0, 150, 90, Radii.all(8));
        c.save();
        c.clipRoundRect(10, 10, 60, 40, Radii.all(6));
        c.fillRect(0, 0, 200, 100, 0xFF00FF00);
        c.restore();
        assertEquals(32, count(c.ops, "tex lumina:snap/1"));
        assertEquals(0, count(c.ops, "tex lumina:snap/0"));
        c.restore();
        c.end();
        assertEquals(32, count(c.ops, "tex lumina:snap/0"));
    }

    @Test
    void scrollPanelClipsRoundedWhenAsked() {
        Panel content = Panel.box(FlexLayout.column()).size(80, 300);
        ScrollPanel scroll = new ScrollPanel(content).size(100, 80);
        scroll.style(b -> b.set(Styles.RADIUS, Radii.all(12)).set(Styles.CLIP_ROUNDED, true));
        Panel host = Panel.box(new StackLayout(Align.START, Align.START)).add(scroll);
        UiRoot root = new UiRoot(host, Theme.dark());
        root.resize(200, 200);
        SnapshotCanvas c = new SnapshotCanvas();
        c.begin(200, 200);
        root.render(c, 0f);
        c.end();
        assertEquals(1, c.snapshots, c.ops.toString());

        scroll.style(b -> b.set(Styles.CLIP_ROUNDED, false));
        root.tick(0f);
        c.snapshots = 0;
        c.begin(200, 200);
        root.render(c, 0f);
        c.end();
        assertEquals(0, c.snapshots);
    }

    @Test
    void cssBackdropFilterAndClipRounded() {
        Theme theme = Theme.dark();
        Stylesheet.parse(".card { backdrop-filter: blur(6px); clip-rounded: true; }").applyTo(theme);
        assertEquals(6f, theme.style("card").get(Styles.BACKDROP_BLUR), 1e-6);
        assertEquals(Boolean.TRUE, theme.style("card").get(Styles.CLIP_ROUNDED));
        Stylesheet.parse(".glass { backdrop-blur: 4; }").applyTo(theme);
        assertEquals(4f, theme.style("glass").get(Styles.BACKDROP_BLUR), 1e-6);
    }
}
