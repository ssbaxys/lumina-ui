package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.blocks.BlockMap;
import io.github.ssbaxys.lumina.blocks.BlockModel;
import io.github.ssbaxys.lumina.blocks.BlockStructure;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockStructureTest {
    private static final String STONE = "minecraft:stone";

    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    @Test
    void setFillBoundsAndListeners() {
        BlockStructure s = new BlockStructure();
        assertNull(s.bounds());
        List<String> changes = new ArrayList<>();
        s.onChange((x, y, z, id) -> changes.add(x + "," + y + "," + z + "=" + id));
        s.fill(0, 0, 0, 2, 1, 3, STONE);
        assertEquals(3 * 2 * 4, s.size());
        assertEquals(new BlockStructure.Bounds(0, 0, 0, 2, 1, 3), s.bounds());
        s.set(-5, 7, 1, "minecraft:tnt");
        assertEquals(new BlockStructure.Bounds(-5, 0, 0, 2, 7, 3), s.bounds());
        assertEquals("minecraft:tnt", s.get(-5, 7, 1));
        s.remove(-5, 7, 1);
        assertNull(s.get(-5, 7, 1));
        assertEquals(new BlockStructure.Bounds(0, 0, 0, 2, 1, 3), s.bounds(), "bounds shrink after removal");
        assertEquals("-5,7,1=null", changes.get(changes.size() - 1));
        int before = changes.size();
        s.set(0, 0, 0, STONE);
        assertEquals(before, changes.size(), "setting the same block is not a change");
        int[] count = {0};
        s.forEach((x, y, z, id) -> count[0]++);
        assertEquals(24, count[0]);
        s.clear();
        assertTrue(s.isEmpty());
    }

    @Test
    void fromLayersParsesBottomFirst() {
        BlockStructure s = BlockStructure.fromLayers(Map.of('S', STONE, 'G', "minecraft:glass"),
                "SSS",
                "S.S",
                "---",
                "G..",
                "..G");
        assertEquals(STONE, s.get(0, 0, 0));
        assertNull(s.get(1, 0, 1), "dot is air");
        assertEquals("minecraft:glass", s.get(0, 1, 0));
        assertEquals("minecraft:glass", s.get(2, 1, 1));
        assertEquals(new BlockStructure.Bounds(0, 0, 0, 2, 1, 1), s.bounds());

        BlockStructure parsed = BlockStructure.parse("S=minecraft:stone; T = minecraft:tnt", """
                  SS
                  ---
                  T.
                """);
        assertEquals(STONE, parsed.get(1, 0, 0));
        assertEquals("minecraft:tnt", parsed.get(0, 1, 0));
        assertEquals(3, parsed.size());
    }

    @Test
    void extrudeTurnsMapIntoSlab() {
        BlockMap map = BlockMap.of(16, ".T", "SS").key('S', STONE).key('T', "minecraft:tnt");
        BlockStructure s = BlockStructure.extrude(map, 3);
        assertEquals(9, s.size());
        assertEquals(STONE, s.get(0, 0, 2), "bottom row of the map is y = 0");
        assertEquals("minecraft:tnt", s.get(1, 1, 1));
        assertNull(s.get(0, 1, 0));
    }

    @Test
    void hiddenFacesAreCulled() {
        BlockStructure cube = new BlockStructure().fill(0, 0, 0, 1, 1, 1, STONE);
        BlockModel model = new BlockModel(cube);
        assertEquals(24, model.exposedFaceCount(), "2x2x2 solid cube shows 4 faces per side");
        // изометрия видит три стороны из шести
        assertEquals(12, model.visibleFaceCount());

        // стекло не закрывает соседей, но соседние стёкла скрывают общую грань
        BlockStructure glass = new BlockStructure().set(0, 0, 0, STONE).set(1, 0, 0, "minecraft:glass")
                .set(2, 0, 0, "minecraft:glass");
        assertEquals(6 + 4 + 5, new BlockModel(glass).exposedFaceCount());

        // цветок рисуется двумя диагоналями и никого не закрывает
        BlockStructure flower = new BlockStructure().set(0, 0, 0, "minecraft:grass_block").set(0, 1, 0, "minecraft:poppy");
        assertEquals(6 + 2, new BlockModel(flower).exposedFaceCount());
        assertFalse(BlockModel.occludes("minecraft:oak_leaves"));
        assertTrue(BlockModel.occludes("minecraft:red_mushroom_block"));

        // срез по слою открывает верх нижнего слоя
        BlockModel cut = new BlockModel(cube).cutAbove(0);
        assertEquals(4 + 4 + 8, cut.exposedFaceCount());
    }

    @Test
    void facesAreSortedBackToFront() {
        BlockStructure row = new BlockStructure();
        for (int z = 0; z < 4; z++) {
            row.set(0, 0, z, STONE);
        }
        BlockModel model = new BlockModel(row).camera(0f, 30f);   // камера смотрит с +z
        List<int[]> order = model.drawOrder();
        assertFalse(order.isEmpty());
        int lastZ = Integer.MIN_VALUE;
        for (int[] f : order) {
            assertTrue(f[2] >= lastZ, "far blocks (small z) are painted first");
            lastZ = f[2];
        }
        // с противоположной стороны порядок обратный
        order = model.camera(180f, 30f).drawOrder();
        assertEquals(3, order.get(0)[2]);
        assertEquals(0, order.get(order.size() - 1)[2]);

        // ближняя грань нижнего блока под верхним рисуется раньше верхнего блока ближе к камере
        BlockStructure stair = new BlockStructure().set(0, 0, 0, STONE).set(0, 1, 1, STONE);
        order = new BlockModel(stair).camera(0f, 30f).drawOrder();
        assertEquals(0, order.get(0)[2]);
        assertEquals(1, order.get(order.size() - 1)[2]);
    }

    @Test
    void dragRotatesAndScrollZooms() {
        BlockModel model = new BlockModel(new BlockStructure().fill(0, 0, 0, 2, 2, 2, STONE)).size(100, 100);
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).fill().add(model), Theme.dark());
        root.resize(200, 200);
        root.tick(0f);
        float cx = model.screenX() + 50;
        float cy = model.screenY() + 50;
        float yaw0 = model.yaw();
        float pitch0 = model.pitch();
        root.pointerMove(cx, cy);
        root.pointerDown(cx, cy, Keys.MOUSE_LEFT);
        root.pointerMove(cx + 20, cy + 10);
        root.tick(0.016f);
        root.pointerUp(cx + 20, cy + 10, Keys.MOUSE_LEFT);
        float yaw1 = model.yaw();
        assertEquals(yaw0 - 20 * BlockModel.DRAG_SENSITIVITY, yaw1, 1e-3, "drag right turns the model");
        assertEquals(pitch0 + 10 * BlockModel.DRAG_SENSITIVITY, model.pitch(), 1e-3);
        root.tick(0.1f);
        assertNotEquals(yaw1, model.yaw(), "inertia keeps the model spinning after release");

        float z0 = model.zoom();
        root.scroll(cx, cy, 0, 1);
        assertTrue(model.zoom() > z0);

        model.autoRotate(true);
        float y2 = model.yaw();
        for (int i = 0; i < 200; i++) {
            root.tick(0.05f);
        }
        assertNotEquals(y2, model.yaw());
    }

    @Test
    void structureTagInMarkup() {
        Markup.Built built = Markup.build("""
                <structure yaw="30deg" pitch="20" auto-rotate key="G=minecraft:grass_block; T=minecraft:tnt">
                  GG
                  GG
                  ---
                  T.
                  ..
                </structure>
                """, new Bindings());
        BlockModel model = (BlockModel) built.root();
        assertEquals(30f, model.yaw(), 1e-4);
        assertEquals(20f, model.pitch(), 1e-4);
        assertTrue(model.isAutoRotating());
        assertEquals(5, model.structure().size());
        assertEquals("minecraft:tnt", model.structure().get(0, 1, 0));
        assertEquals("minecraft:grass_block", model.structure().get(1, 0, 1));
    }

    @Test
    void rendersToCanvas() {
        BlockStructure s = BlockStructure.fromLayers(Map.of('S', STONE, 'W', "minecraft:water", 'F', "minecraft:fire"),
                "SSS", "SWS", "SSS", "---", "F..");
        BlockModel model = new BlockModel(s).size(120, 120);
        Panel box = Panel.box(FlexLayout.column()).fill().add(model);
        UiRoot root = new UiRoot(box, Theme.dark());
        root.resize(200, 200);
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        for (int i = 0; i < 3; i++) {
            root.tick(0.05f);
            canvas.begin(200, 200);
            root.render(canvas, 0f);
            canvas.end();
        }
        long quads = canvas.ops.stream().filter(op -> op.startsWith("freequad")).count();
        assertEquals(model.visibleFaceCount(), quads, "one quad per visible face without textures");
        assertTrue(canvas.ops.stream().anyMatch(op -> op.startsWith("shape")), "ground shadow");

        canvas.begin(64, 64);
        BlockModel.drawBlockIcon(canvas, "minecraft:tnt", 0, 0, 32, 0f);
        BlockModel.drawBlockIcon(canvas, "minecraft:poppy", 32, 0, 32, 0f);
        canvas.end();
        assertEquals(5, canvas.ops.stream().filter(op -> op.startsWith("freequad")).count());
    }
}
