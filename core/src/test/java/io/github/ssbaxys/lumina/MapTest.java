package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.map.DemoMap;
import io.github.ssbaxys.lumina.map.MapGridSampler;
import io.github.ssbaxys.lumina.map.MapSample;
import io.github.ssbaxys.lumina.map.MapSource;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.MiniMap;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.WorldMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Мини-карта: постепенная сетка, источники, отрисовка, поворот, метки и статичная карта мира. */
class MapTest {
    CapturePlatform platform;
    final List<String> uploads = new ArrayList<>();
    boolean textures;

    @BeforeEach
    void init() {
        uploads.clear();
        textures = false;
        platform = new CapturePlatform() {
            @Override
            public TextureRef createTexture(String key, int width, int height, int[] argb) {
                if (!textures) {
                    return null;
                }
                uploads.add(key);
                return TextureRef.of("luminaui:dynamic/" + key);
            }
        };
        Lumina.init(platform);
    }

    @Test
    void samplerReadsInBudgetsAndNotEveryFrame() {
        int[] reads = {0};
        MapGridSampler sampler = new MapGridSampler((x, z, h) -> {
            reads[0]++;
            h[0] = 64 + (z & 1);
            return 0xFF336699;
        }).budget(100).interval(0.05f);
        MapSample s = sampler.sample(0.5, 0.5, 0f, 8, 0L);
        assertEquals(100, reads[0]);
        int size = s.size();
        assertTrue(size >= 17, "size " + size);
        assertEquals(size * size - 100, sampler.unknownCount());
        // центр прочитан первым
        assertNotEquals(0, s.colorAt(0, 0));
        sampler.sample(0.5, 0.5, 0f, 8, 10_000_000L);
        assertEquals(100, reads[0], "too soon: no refresh");
        long v = s.version();
        MapSample s2 = sampler.sample(0.5, 0.5, 0f, 8, 60_000_000L);
        assertEquals(200, reads[0]);
        assertTrue(s2.version() > v);
        for (long t = 1; t < 50; t++) {
            sampler.sample(0.5, 0.5, 0f, 8, t * 60_000_000L);
        }
        assertEquals(0, sampler.unknownCount());
        // склоны затенены: соседние ряды разной высоты дают разные цвета
        MapSample full = sampler.sample(0.5, 0.5, 0f, 8, 99 * 60_000_000L);
        assertNotEquals(full.colorAt(0, 0), full.colorAt(0, 1));
    }

    @Test
    void samplerShiftsGridOnlyAfterWalkingAway() {
        MapGridSampler sampler = new MapGridSampler((x, z, h) -> {
            h[0] = 64;
            return 0xFF000000 | (x & 0xFF) << 8 | (z & 0xFF);
        }).budget(100000).interval(0f);
        MapSample a = sampler.sample(0, 0, 0f, 16, 0L);
        MapSample b = sampler.sample(3, 0, 0f, 16, 1L);
        assertEquals(a.originX(), b.originX(), "small step: same grid");
        MapSample c = sampler.sample(40, 0, 0f, 16, 2L);
        assertNotEquals(a.originX(), c.originX());
        assertTrue(c.colorAt(40, 0) != 0);
        assertEquals(0, sampler.unknownCount());
    }

    @Test
    void demoMapAndSourceFallback() {
        DemoMap demo = new DemoMap(3).player(10, 20, 90f);
        MapSample s = MapSource.platform().or(demo).sample(32);
        assertNotNull(s);
        assertEquals(10, s.playerX(), 1e-9);
        assertEquals(90f, s.yaw());
        assertNotEquals(0, s.colorAt(10, 20));
        assertNull(MapSource.platform().sample(32), "no world in tests");
        assertNotEquals(0, demo.colorAt(500, -300));
    }

    private UiRoot mount(Panel content) {
        UiRoot root = new UiRoot(content, Theme.dark());
        root.resize(300, 300);
        root.tick(0f);
        return root;
    }

    @Test
    void miniMapDrawsCellsWithoutTexturesAndMarkersClamp() {
        DemoMap demo = new DemoMap(5).player(0.5, 0.5, 180f);
        MiniMap map = new MiniMap().source(demo).size(100, 100).zoom(2f)
                .marker("north", 0.5, -10.5, 0xFFFF0000, "Север")
                .marker("far", 5000, 0, 0xFF00FF00, "");
        UiRoot root = mount(Panel.box(new StackLayout(Align.START, Align.START)).add(map));
        CapturePlatform.ColorCanvas c = new CapturePlatform.ColorCanvas();
        c.begin(300, 300);
        root.render(c, 0f);
        c.end();
        assertTrue(c.ops.stream().filter(o -> o.equals("freequad")).count() > 100, "cells drawn");
        assertNotNull(map.lastSample());
        // Игрок смотрит на север (yaw 180), карта вращается: метка севернее игрока — над центром.
        int red = c.colors.indexOf(0xFFFF0000);
        assertTrue(red >= 0, "north marker drawn");
        String op = c.ops.get(red);
        float y = Float.parseFloat(op.split(" ")[1].split(",")[1]);
        assertTrue(y < 50f - 10f, op);
        // далёкая метка прижата к краю и без подписи
        assertTrue(c.colors.contains(0xFF00FF00));
        assertTrue(c.ops.stream().anyMatch(o -> o.startsWith("text Север")), "label of a visible marker");
    }

    @Test
    void miniMapUploadsTextureOnlyWhenChanged() {
        textures = true;
        DemoMap demo = new DemoMap(9).player(0, 0, 0f);
        MiniMap map = new MiniMap().source(demo).size(80, 80).uploadInterval(0.2f).shape(MiniMap.Shape.SQUARE)
                .rotate(false);
        UiRoot root = mount(Panel.box(new StackLayout(Align.START, Align.START)).add(map));
        TestSupport.RecordingCanvas c = new TestSupport.RecordingCanvas();
        for (int i = 0; i < 5; i++) {
            c.begin(300, 300);
            root.render(c, 0f);
            c.end();
        }
        assertEquals(1, uploads.size(), "throttled: " + uploads);
        assertTrue(c.ops.stream().anyMatch(o -> o.startsWith("tex luminaui:dynamic/minimap/")));
        platform.advance(0.3f);
        demo.player(200, 0, 0f);
        c.begin(300, 300);
        root.render(c, 0f);
        c.end();
        assertEquals(2, uploads.size());
    }

    @Test
    void miniMapZoomsWithWheel() {
        MiniMap map = new MiniMap().source(new DemoMap(1).player(0, 0, 0f)).size(100, 100).zoom(2f);
        UiRoot root = mount(Panel.box(new StackLayout(Align.START, Align.START)).add(map));
        root.scroll(50, 50, 0f, 1f);
        assertEquals(2.5f, map.zoom(), 1e-5);
        root.scroll(50, 50, 0f, -1f);
        assertEquals(2f, map.zoom(), 1e-5);
        assertTrue(map.viewRadius() > 30);
    }

    @Test
    void worldMapBuildsOnceAndPans() {
        int[] calls = {0};
        WorldMap world = new WorldMap((x, z) -> {
            calls[0]++;
            return 0xFF00AA00;
        }, 0, 0, 64).size(100, 80).zoom(2f).marker(0, 0, 0xFFFFFF00, "Спавн");
        UiRoot root = mount(Panel.box(new StackLayout(Align.START, Align.START)).add(world));
        CapturePlatform.ColorCanvas c = new CapturePlatform.ColorCanvas();
        for (int i = 0; i < 3; i++) {
            c.begin(300, 300);
            root.render(c, 0f);
            c.end();
        }
        assertEquals(64 * 64, calls[0], "colors built once");
        assertTrue(c.colors.contains(0xFF00AA00));
        root.pointerMove(50, 40);
        root.pointerDown(50, 40, Keys.MOUSE_LEFT);
        root.pointerMove(30, 40);
        root.pointerUp(30, 40, Keys.MOUSE_LEFT);
        assertEquals(10.0, world.viewX(), 1e-6, "dragged left by 20px at zoom 2 → +10 blocks");
        world.view(1000, 1000);
        assertEquals(32.0, world.viewX(), 1e-6, "clamped to area");
    }
}
