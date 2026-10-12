package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.inventory.Slot;
import io.github.ssbaxys.lumina.inventory.Stack;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.preview.EntityView;
import io.github.ssbaxys.lumina.preview.ItemView;
import io.github.ssbaxys.lumina.render.AbstractCanvas;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.EntityOptions;
import io.github.ssbaxys.lumina.render.ShapePaint;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Сущности и крупные предметы в 3D: вызовы Canvas, вращение мышью, слежение за курсором, теги разметки. */
class EntityViewTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    /** Canvas, запоминающий сущности, повёрнутые предметы и клип. */
    static final class ModelCanvas extends AbstractCanvas {
        final List<Object[]> entities = new ArrayList<>();
        final List<String> ops = new ArrayList<>();

        void begin(float w, float h) {
            beginFrame(w, h);
        }

        void end() {
            endFrame();
        }

        @Override
        protected void drawQuad(float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl) {
        }

        @Override
        protected void drawFreeQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                    int c0, int c1, int c2, int c3) {
        }

        @Override
        protected void drawShape(float x0, float y0, float x1, float y1, float rTL, float rTR, float rBR, float rBL,
                                 int tl, int tr, int br, int bl, float stroke, float softness, ShapePaint paint) {
        }

        @Override
        protected void applyBlendMode(BlendMode mode) {
        }

        @Override
        protected void drawTextImpl(String text, float x, float y, float scale, int argb, boolean shadow) {
        }

        @Override
        protected void drawTextureQuadImpl(TextureRef texture, float x0, float y0, float x1, float y1,
                                           float x2, float y2, float x3, float y3,
                                           float u0, float v0, float u1, float v1,
                                           float u2, float v2, float u3, float v3, int tint) {
        }

        @Override
        protected void drawItemImpl(String itemId, float x, float y, float size) {
            ops.add("item " + itemId);
        }

        @Override
        protected void applyClip(float x0, float y0, float x1, float y1) {
            ops.add("clip " + x0 + "," + y0 + "," + x1 + "," + y1);
        }

        @Override
        protected void clearClip() {
        }

        @Override
        public TextMetrics textMetrics() {
            return TestSupport.METRICS;
        }

        @Override
        protected void drawEntityImpl(String type, float x, float y, float size, float yaw, float pitch,
                                      EntityOptions options) {
            entities.add(new Object[]{type, x, y, size, yaw, pitch, options});
            ops.add("entity " + type);
        }

        @Override
        protected void drawItem3dImpl(String itemId, float x, float y, float size, float yaw, float pitch,
                                      boolean glint) {
            ops.add("item3d " + itemId + " " + size + (glint ? " glint" : ""));
        }
    }

    private static UiRoot mount(UiElement... e) {
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).fill().add(e), Theme.dark());
        root.resize(400, 300);
        root.tick(0f);
        return root;
    }

    private static ModelCanvas frame(UiRoot root) {
        ModelCanvas c = new ModelCanvas();
        c.begin(400, 300);
        root.render(c, 0f);
        c.end();
        return c;
    }

    @Test
    void entityViewDrawsFittedSquare() {
        EntityView zombie = new EntityView("minecraft:zombie").equip("mainhand", "minecraft:iron_sword").size(80, 100);
        UiRoot root = mount(zombie);
        ModelCanvas c = frame(root);
        assertEquals(1, c.entities.size());
        Object[] e = c.entities.get(0);
        assertEquals("minecraft:zombie", e[0]);
        assertEquals(80f, (float) e[3], "square fits the smaller side");
        assertEquals(10f, (float) e[2], "centered vertically");
        assertEquals(-25f + 360f, (float) e[4], 0.01f, "three-quarter view by default");
        EntityOptions o = (EntityOptions) e[6];
        assertEquals(Map.of("mainhand", "minecraft:iron_sword"), o.equipment());
        assertTrue(o.appearanceKey().contains("mainhand=minecraft:iron_sword"));
    }

    @Test
    void dragRotatesAndAutoRotateSpins() {
        EntityView view = new EntityView("minecraft:creeper").size(100, 100);
        UiRoot root = mount(view);
        float yaw = view.yaw();
        float x = view.screenX() + 50f;
        float y = view.screenY() + 50f;
        root.pointerMove(x, y);
        root.pointerDown(x, y, Keys.MOUSE_LEFT);
        root.pointerMove(x + 10f, y + 5f);
        assertTrue(view.isDragging());
        assertEquals((yaw - 8f + 360f) % 360f, view.yaw(), 0.01f, "drag right turns the model after the mouse");
        assertEquals(14f, view.pitch(), 0.01f, "drag down shows it from above");
        root.pointerUp(x + 10f, y + 5f, Keys.MOUSE_LEFT);
        assertFalse(view.isDragging());

        // Инерция после броска затухает.
        for (int i = 0; i < 200; i++) {
            root.tick(0.05f);
        }
        float settled = view.yaw();
        root.tick(0.05f);
        assertEquals(settled, view.yaw(), 0.001f);

        view.autoRotate(true);
        float before = view.yaw();
        root.tick(0.5f);
        assertEquals((before + 15f) % 360f, view.yaw(), 0.01f);
        view.pitch(200f);
        assertEquals(60f, view.pitch(), "pitch is clamped");
    }

    @Test
    void scrollZoomsAndClips() {
        EntityView view = new EntityView("minecraft:pig").size(60, 60);
        UiRoot root = mount(view);
        root.scroll(view.screenX() + 30f, view.screenY() + 30f, 0f, 3f);
        assertTrue(view.zoom() > 1.4f);
        for (int i = 0; i < 60; i++) {
            root.tick(0.05f);
        }
        ModelCanvas c = frame(root);
        assertTrue((float) c.entities.get(0)[3] > 80f);
        assertTrue(c.ops.stream().anyMatch(o -> o.startsWith("clip")), "zoomed model is clipped to the view");
        view.zoomable(false).zoom(1f);
        root.scroll(view.screenX() + 30f, view.screenY() + 30f, 0f, 3f);
        assertEquals(1f, view.zoom());
    }

    @Test
    void followMouseTurnsHeadTowardsCursor() {
        EntityView player = EntityView.player().followMouse(true).size(100, 100);
        UiRoot root = mount(player);
        assertTrue(player.isPlayer());
        root.pointerMove(5f, 150f);   // левее и ниже
        ModelCanvas c = frame(root);
        Object[] e = c.entities.get(0);
        assertEquals(Canvas.PLAYER, e[0]);
        EntityOptions o = (EntityOptions) e[6];
        assertTrue(o.headYaw() > 5f, "head turns left, towards the cursor: " + o.headYaw());
        assertTrue(o.headPitch() > 5f, "looks down: " + o.headPitch());
        assertTrue((float) e[4] > player.yaw() || (float) e[4] < player.yaw() - 300f, "body follows a little");

        root.pointerMove(300f, 0f);   // правее и выше
        o = (EntityOptions) frame(root).entities.get(0)[6];
        assertTrue(o.headYaw() < -5f);
        assertTrue(o.headPitch() < -5f);
        EntityOptions again = (EntityOptions) frame(root).entities.get(0)[6];
        assertSame(o, again, "no new options while the mouse stands still");
    }

    @Test
    void itemViewDrawsRotatedItem() {
        ItemView view = new ItemView(Stack.of("minecraft:diamond_sword").withGlint(true)).autoRotate(true).size(64, 64);
        UiRoot root = mount(view);
        ModelCanvas c = frame(root);
        assertTrue(c.ops.contains("item3d minecraft:diamond_sword 64.0 glint"), c.ops.toString());

        // Бэкенд без 3D рисует плоскую иконку.
        TestSupport.RecordingCanvas flat = new TestSupport.RecordingCanvas();
        flat.begin(400, 300);
        root.render(flat, 0f);
        flat.end();
        assertTrue(flat.ops.contains("item minecraft:diamond_sword"));
    }

    @Test
    void canvasCullsAndTransformsEntities() {
        ModelCanvas c = new ModelCanvas();
        c.begin(100, 100);
        c.save();
        c.translate(10f, 20f);
        c.scale(2f, 2f);
        c.drawEntity("minecraft:cow", 5f, 5f, 20f, 0f, 0f, null);
        c.drawEntity("minecraft:cow", 500f, 5f, 20f, 0f, 0f, null);
        c.drawPlayer(0f, 0f, 10f, 0f, 0f, EntityOptions.DEFAULT);
        c.restore();
        c.end();
        assertEquals(2, c.entities.size(), "off-screen entity is culled");
        Object[] e = c.entities.get(0);
        assertEquals(20f, (float) e[1]);
        assertEquals(30f, (float) e[2]);
        assertEquals(40f, (float) e[3]);
        assertSame(EntityOptions.DEFAULT, e[6]);
        assertEquals(Canvas.PLAYER, c.entities.get(1)[0]);

        EntityOptions o = EntityOptions.DEFAULT.baby(true).equip("head", "minecraft:carved_pumpkin");
        assertEquals(o.appearanceKey(), o.look(30f, 10f).appearanceKey(), "pose is not part of the appearance");
        assertTrue(o.equip("head", null).equipment().isEmpty());
    }

    @Test
    void markupTagsBuildViews() {
        Markup.Built built = Markup.build("""
                <column>
                  <slot id="s" item="minecraft:diamond_sword" glint durability="100/250" name="Меч" lore="а|б" look="modern">
                  <slot id="c" item="minecraft:coal" count="12" size="24"/>
                  <entity id="z" type="minecraft:zombie" auto-rotate baby mainhand="minecraft:iron_sword" yaw="10" pitch="5">
                  <player id="p" follow-mouse zoomable="false"/>
                  <item-view id="i" item="minecraft:tnt" auto-rotate="45"/>
                </column>
                """, new Bindings());
        UiElement root = built.root();
        Slot s = assertInstanceOf(Slot.class, root.findById("s"));
        assertEquals(Slot.Look.MODERN, s.look());
        assertTrue(s.stack().hasGlint());
        assertEquals("Меч", s.stack().displayName());
        assertEquals(List.of("а", "б"), s.stack().lore());
        assertEquals(150, s.stack().damage());
        Slot coal = assertInstanceOf(Slot.class, root.findById("c"));
        assertEquals(12, coal.stack().count());

        EntityView z = assertInstanceOf(EntityView.class, root.findById("z"));
        assertEquals("minecraft:zombie", z.type());
        assertTrue(z.isAutoRotating());
        assertTrue(z.options().baby());
        assertEquals("minecraft:iron_sword", z.options().equipment().get("mainhand"));
        assertEquals(10f, z.yaw());
        assertEquals(5f, z.pitch());

        EntityView p = assertInstanceOf(EntityView.class, root.findById("p"));
        assertTrue(p.isPlayer());
        assertTrue(p.followsMouse());
        ItemView i = assertInstanceOf(ItemView.class, root.findById("i"));
        assertEquals("minecraft:tnt", i.stack().itemId());
        assertTrue(i.isAutoRotating());

        UiRoot ui = mount(root);
        assertEquals(24f, coal.width());
        frame(ui);
    }
}
