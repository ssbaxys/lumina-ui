package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Backdrop;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Фон из частиц: движение каждого вида и отталкивание от курсора. */
class ParticlesBackdropTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    private static UiRoot mount(Backdrop b) {
        UiRoot root = new UiRoot(Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH)).fill().add(b), Theme.dark());
        root.resize(300, 200);
        root.tick(0f);
        return root;
    }

    @Test
    void everyStyleMovesAndPaints() {
        for (Backdrop.ParticleStyle style : Backdrop.ParticleStyle.values()) {
            Backdrop b = new Backdrop(Backdrop.Kind.PARTICLES).particles(style).particleCount(40);
            UiRoot root = mount(b);
            root.tick(0.016f);
            float[] start = b.particlePosition(0);
            assertNotNull(start, style.name());
            for (int i = 0; i < 30; i++) {
                root.tick(0.05f);
            }
            float[] end = b.particlePosition(0);
            assertTrue(start[0] != end[0] || start[1] != end[1], style + " moved");
            TestSupport.RecordingCanvas c = new TestSupport.RecordingCanvas();
            c.begin(300, 200);
            root.render(c, 0f);
            c.end();
            assertTrue(c.ops.size() > 20, style + ": " + c.ops.size());
            if (style == Backdrop.ParticleStyle.SPARKS) {
                assertTrue(c.ops.contains("blend ADD"), "sparks glow additively");
            }
            assertEquals(style, b.particleStyle());
        }
    }

    @Test
    void cursorPushesParticlesAway() {
        Backdrop b = new Backdrop(Backdrop.Kind.PARTICLES).particles(Backdrop.ParticleStyle.DUST)
                .particleCount(1).repel(80f, 4000f).speed(1f);
        UiRoot root = mount(b);
        root.tick(0.016f);
        float[] p = b.particlePosition(0);
        // мышь чуть левее частицы: её должно унести вправо
        root.pointerMove(p[0] - 5f, p[1]);
        for (int i = 0; i < 10; i++) {
            root.tick(0.016f);
        }
        float[] q = b.particlePosition(0);
        float moved = q[0] - p[0];
        assertTrue(moved > 3f, "pushed right by " + moved);
    }
}
