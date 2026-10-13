package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.blocks.BlockStructure;
import io.github.ssbaxys.lumina.blocks.DemoWorld;
import io.github.ssbaxys.lumina.blocks.WorldBlocks;
import io.github.ssbaxys.lumina.blocks.WorldCapture;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.StructurePreview;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Снимок мира и его 3D-превью на поддельной платформе: порции, воздух, незагруженные чанки, слежение, срез. */
class StructurePreviewTest {
    private static final String STONE = "minecraft:stone";
    private static final String DIRT = "minecraft:dirt";
    private static final String GRASS = "minecraft:grass_block";

    /** Плоский мир: камень, земля, трава на y = 64, выше — воздух; всё с x ≥ {@code unloadedFromX} — не загружено. */
    static final class FakeWorld implements WorldBlocks {
        final Map<Long, String> extra = new HashMap<>();
        int reads;
        int unloadedFromX = Integer.MAX_VALUE;
        double x = 0.5;
        double y = 65;
        double z = 0.5;
        float yaw;

        static long key(int x, int y, int z) {
            return ((long) x * 73856093L) ^ ((long) y * 19349663L) ^ ((long) z * 83492791L);
        }

        FakeWorld put(int x, int y, int z, String id) {
            extra.put(key(x, y, z), id == null ? "air" : id);
            return this;
        }

        @Override
        public String blockAt(int x, int y, int z) {
            reads++;
            if (x >= unloadedFromX) {
                return UNLOADED;
            }
            String e = extra.get(key(x, y, z));
            if (e != null) {
                return e.equals("air") ? null : e;
            }
            if (y < 64) {
                return y < 61 ? STONE : DIRT;
            }
            return y == 64 ? GRASS : null;
        }

        @Override
        public double playerX() {
            return x;
        }

        @Override
        public double playerY() {
            return y;
        }

        @Override
        public double playerZ() {
            return z;
        }

        @Override
        public float playerYaw() {
            return yaw;
        }
    }

    CapturePlatform platform;
    WorldBlocks world;

    @BeforeEach
    void init() {
        world = null;
        platform = new CapturePlatform() {
            @Override
            public WorldBlocks worldBlocks() {
                return world;
            }
        };
        Lumina.init(platform);
    }

    @Test
    void captureReadsInBudgetsSkipsAirAndKeepsUnloaded() {
        FakeWorld w = new FakeWorld();
        WorldCapture c = new WorldCapture().budget(300).box(0, 60, 0, 9, 69, 9);
        assertFalse(c.step(null), "no world: nothing happens");
        assertEquals(0, w.reads);
        assertFalse(c.step(w));
        assertEquals(300, w.reads, "one budget per step");
        assertFalse(c.step(w));
        assertFalse(c.step(w));
        assertTrue(c.step(w), "fourth step ends the pass");
        assertEquals(1000, w.reads);
        assertEquals(1, c.passes());
        assertFalse(c.isPassing());
        // воздух не хранится: 5 слоёв (60..64) по 10×10
        assertEquals(500, c.structure().size());
        assertEquals(GRASS, c.structure().get(0, 4, 0));
        assertNull(c.structure().get(0, 5, 0));
        assertEquals(new BlockStructure.Bounds(0, 0, 0, 9, 9, 9), c.localBox());

        // тот же мир — постройка не меняется (модель не перестраивается)
        int rev = c.structure().revision();
        c.restart().finish(w);
        assertEquals(rev, c.structure().revision());

        // незагруженная часть хранит прежние блоки, загруженная — обновляется
        w.unloadedFromX = 5;
        w.put(2, 65, 2, "minecraft:tnt").put(7, 65, 7, "minecraft:tnt").put(1, 64, 1, null);
        c.restart().finish(w);
        assertEquals("minecraft:tnt", c.structure().get(2, 5, 2));
        assertNull(c.structure().get(7, 5, 7), "unloaded chunk is not read");
        assertEquals(GRASS, c.structure().get(7, 4, 7), "unloaded chunk keeps the old block");
        assertNull(c.structure().get(1, 4, 1), "broken block removed");
        assertEquals(500, c.unloadedCount());

        // снимок для обмена — от угла коробки
        BlockStructure snap = c.snapshot();
        assertEquals(c.structure().size(), snap.size());
        assertEquals("minecraft:tnt", snap.get(2, 5, 2));
    }

    @Test
    void boxIsCappedAndShiftKeepsTheOverlap() {
        FakeWorld w = new FakeWorld();
        WorldCapture c = new WorldCapture().box(0, 0, 0, 99, 99, 99);
        BlockStructure.Bounds big = c.worldBox();
        assertEquals(WorldCapture.MAX_SIZE, big.sizeX());
        assertEquals(WorldCapture.MAX_SIZE, big.sizeY());
        assertEquals(WorldCapture.MAX_SIZE, big.sizeZ());
        assertTrue(big.minX() > 0 && big.maxX() < 99, "clipped around the middle: " + big);

        c.clear();
        c.around(0, 64, 0, 4, 2, 2).finish(w);
        int ax = c.anchorX();
        assertEquals(-4, ax);
        assertEquals(9 * 9 * 3, c.structure().size(), "62, 63, 64 are solid");
        // сдвиг на 3 по x: ушедшие столбцы убраны сразу, общая часть осталась ещё до чтения
        c.around(3, 64, 0, 4, 2, 2);
        assertTrue(c.isPassing());
        assertEquals(-4, c.anchorX(), "anchor stays");
        assertNull(c.structure().get(-4 - ax, 64 - c.anchorY(), 0 - c.anchorZ()));
        assertEquals(GRASS, c.structure().get(-1 - ax, 64 - c.anchorY(), 0 - c.anchorZ()));
        assertEquals(6 * 9 * 3, c.structure().size());
        c.finish(w);
        assertEquals(9 * 9 * 3, c.structure().size());
        assertEquals(GRASS, c.structure().get(7 - ax, 64 - c.anchorY(), 0 - c.anchorZ()));
        assertEquals(new BlockStructure.Bounds(-1, 62, -4, 7, 66, 4), c.worldBox());
    }

    @Test
    void platformCapturesABoxAtOnce() {
        assertNull(Lumina.platform().captureWorld(0, 0, 0, 1, 1, 1), "no world");
        world = new FakeWorld();
        BlockStructure s = Lumina.platform().captureWorld(2, 65, 2, -2, 63, -2);
        assertNotNull(s);
        assertEquals(50, s.size(), "y 63 and 64, 5×5 each; air skipped");
        assertEquals(new BlockStructure.Bounds(0, 0, 0, 4, 1, 4), s.bounds());
        assertEquals(GRASS, s.get(0, 1, 0));
        assertEquals(DIRT, s.get(4, 0, 4));
        BlockStructure capped = Lumina.platform().captureWorld(0, 0, 0, 200, 64, 0);
        assertTrue(capped.bounds().sizeX() <= WorldCapture.MAX_SIZE);
    }

    @Test
    void previewFollowsThePlayerPeelsLayersAndFallsBack() {
        FakeWorld w = new FakeWorld();
        world = w;
        int[] refreshed = {0};
        StructurePreview p = new StructurePreview().radius(4).height(2, 3).refreshInterval(0f)
                .onRefresh(() -> refreshed[0]++);
        p.poll(0.016f);
        assertTrue(p.hasWorld());
        assertSame(w, p.world());
        assertTrue(p.hasPlayer());
        assertEquals(1, refreshed[0], "small box: one step is a full pass");
        assertEquals(new BlockStructure.Bounds(-4, 63, -4, 4, 68, 4), p.capture().worldBox());
        assertEquals(p.capture().localBox(), p.frame(), "model framed by the box, not by the blocks");
        assertEquals(9 * 9 * 2, p.structure().size());

        // шаг меньше FOLLOW_STEP — коробка стоит; дальше — сдвигается
        w.x = 1.5;
        p.poll(0.016f);
        assertEquals(-4, p.capture().worldBox().minX());
        w.x = 3.5;
        p.poll(0.016f);
        assertEquals(-1, p.capture().worldBox().minX());
        assertEquals(2, refreshed[0]);

        // без слежения коробка ждёт refresh()
        p.follow(false);
        w.x = 20.5;
        p.poll(0.016f);
        assertEquals(-1, p.capture().worldBox().minX());
        p.refresh().poll(0.016f);
        assertEquals(16, p.capture().worldBox().minX());

        // срезы
        BlockStructure.Bounds box = p.capture().localBox();
        p.peel(2);
        assertEquals(box.maxY() - 2, p.cutAboveLayer());
        p.cutAtPlayer(true);
        assertEquals(66 - p.capture().anchorY(), p.cutAboveLayer(), "up to the player's head");
        assertEquals(0, p.peel());
        p.cutAbove(64);
        assertFalse(p.cutsAtPlayer());
        assertEquals(64 - p.capture().anchorY(), p.cutAboveLayer(), "world height");
        p.showAllLayers();
        assertEquals(Integer.MAX_VALUE, p.cutAboveLayer());

        // перечитывание по интервалу
        p.refreshInterval(1f);
        int passes = p.capture().passes();
        p.poll(0.6f);
        p.poll(0.6f);
        assertEquals(passes + 1, p.capture().passes());

        // мира нет — выдуманный; вернулся мир — снимок заново
        world = null;
        DemoWorld demo = new DemoWorld(3).player(0.5, 0.5, 0f);
        p.fallback(demo).refreshInterval(0f).budget(100_000);
        p.poll(0.016f);
        assertFalse(p.hasWorld());
        assertSame(demo, p.world());
        assertTrue(p.structure().size() > 0);
        assertNotNull(p.structure().bounds());
        world = w;
        p.poll(0.016f);
        assertTrue(p.hasWorld());
        assertEquals(9 * 9 * 2, p.structure().size(), "demo blocks dropped on world change");
    }

    @Test
    void drawsTheModelAndThePlayerMarker() {
        world = new FakeWorld();
        StructurePreview p = new StructurePreview().radius(3).height(1, 2);
        p.size(160, 160);
        Panel host = Panel.box(new StackLayout(Align.START, Align.START)).fill().add(p);
        UiRoot root = new UiRoot(host, Theme.dark());
        root.resize(200, 200);
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        int withMarker = frames(root, canvas);
        assertTrue(p.capture().passes() > 0, "ticks read the world");
        assertTrue(p.visibleFaceCount() > 0);
        assertTrue(canvas.ops.stream().filter(op -> op.startsWith("freequad")).count() >= p.visibleFaceCount() + 2,
                "faces + marker arrow and pole");
        p.showMarker(false);
        int without = frames(root, canvas);
        assertTrue(withMarker > without, withMarker + " vs " + without);

        // середина коробки — в середине виджета
        BlockStructure.Bounds b = p.frame();
        float[] out = new float[3];
        assertTrue(p.project((b.minX() + b.maxX() + 1) / 2f, (b.minY() + b.maxY() + 1) / 2f,
                (b.minZ() + b.maxZ() + 1) / 2f, out));
        assertEquals(80f, out[0], 0.01f);
        assertEquals(80f, out[1], 0.01f);
    }

    private static int frames(UiRoot root, TestSupport.RecordingCanvas canvas) {
        for (int i = 0; i < 3; i++) {
            root.tick(0.05f);
            canvas.begin(200, 200);
            root.render(canvas, 0f);
            canvas.end();
        }
        return canvas.ops.size();
    }

    @Test
    void markupTagAndDemoWorld() {
        StructurePreview p = (StructurePreview) Markup.build("""
                <world-preview radius="5" below="3" above="4" follow="false" interval="0" peel="1" marker="false"
                               perspective yaw="30" size="120"/>
                """, new Bindings()).root();
        assertEquals(5, p.radius());
        assertEquals(3, p.below());
        assertEquals(4, p.above());
        assertFalse(p.follows());
        assertEquals(0f, p.refreshInterval());
        assertEquals(1, p.peel());
        assertEquals(0.45f, p.perspective(), 1e-4);
        assertEquals(30f, p.yaw(), 1e-4);
        p.budget(100_000).poll(0.016f);
        assertFalse(p.hasWorld());
        assertTrue(p.world() instanceof DemoWorld, "demo fallback by default");
        assertTrue(p.structure().size() > 0);

        // демо-мир: игрок стоит на суше, под ним трава, глубже — камень
        DemoWorld demo = new DemoWorld(42).player(0, 0, 0f);
        int[] home = demo.home();
        int h = demo.height(home[0], home[1]);
        assertTrue(h > DemoWorld.SEA_LEVEL, "home is dry: " + h);
        assertEquals(GRASS, demo.blockAt(home[0], h, home[1]));
        String deep = demo.blockAt(home[0], h - 10, home[1]);
        assertTrue(Set.of(STONE, "minecraft:coal_ore", "minecraft:iron_ore").contains(deep), deep);
        assertNull(demo.blockAt(home[0], h + 20, home[1]));
        DemoWorld walker = new DemoWorld(42);
        double y = walker.playerY();
        assertEquals(Math.max(DemoWorld.SEA_LEVEL, walker.height((int) Math.floor(walker.playerX()),
                (int) Math.floor(walker.playerZ()))) + 1, y, 1e-9);
    }
}
