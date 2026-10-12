package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.blocks.BlockMap;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BlockMapTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    @Test
    void rowsKeysAndExport() {
        BlockMap map = BlockMap.of(16, "..T", "GGG", "DSD")
                .key('G', "minecraft:grass_block").key('D', "minecraft:dirt").key('S', "minecraft:stone");
        assertEquals(3, map.columns());
        assertEquals(48f, map.width() == 0 ? map.layoutParams().width.value() : map.width(), 1e-6);
        assertNull(map.get(2, 0), "symbol without key is air");
        map.key('T', "minecraft:tnt");
        assertEquals("minecraft:tnt", map.get(2, 0));
        assertEquals("minecraft:stone", map.get(1, 2));
        map.set(0, 0, "minecraft:glass");
        String[] rows = map.toRows();
        char glass = rows[0].charAt(0);
        assertEquals("minecraft:glass", map.keyMap().get(glass));
        assertEquals(".T", rows[0].substring(1));
        assertArrayEquals(new String[]{"GGG", "DSD"}, new String[]{rows[1], rows[2]});
    }

    @Test
    void editorPlacesAndBreaksBlocks() {
        Signal<String> brush = Signal.of("minecraft:stone");
        List<String> changes = new ArrayList<>();
        BlockMap map = new BlockMap(4, 2, 10).editable(brush).onChange((x, y, id) -> changes.add(x + "," + y + "=" + id));
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).fill().add(map), Theme.dark());
        root.resize(200, 100);
        root.tick(0f);
        float ox = map.screenX();
        float oy = map.screenY();
        root.pointerMove(ox + 5, oy + 5);
        root.pointerDown(ox + 5, oy + 5, Keys.MOUSE_LEFT);
        root.pointerMove(ox + 25, oy + 5);
        root.pointerUp(ox + 25, oy + 5, Keys.MOUSE_LEFT);
        assertEquals("minecraft:stone", map.get(0, 0));
        assertEquals("minecraft:stone", map.get(2, 0), "dragging paints");
        root.pointerDown(ox + 5, oy + 5, Keys.MOUSE_RIGHT);
        root.pointerUp(ox + 5, oy + 5, Keys.MOUSE_RIGHT);
        assertNull(map.get(0, 0));
        assertEquals("0,0=null", changes.get(changes.size() - 1));
    }

    @Test
    void blocksTagInMarkup() {
        Markup.Built built = Markup.build("""
                <state name="brush" value="minecraft:dirt"/>
                <blocks tile="8" key="G=minecraft:grass_block; S=minecraft:stone" brush="brush">
                  ..G..
                  GGSGG
                </blocks>
                """, new Bindings());
        BlockMap map = (BlockMap) built.root();
        assertEquals(5, map.columns());
        assertEquals(2, map.rowCount());
        assertEquals("minecraft:stone", map.get(2, 1));
        assertEquals("minecraft:grass_block", map.get(2, 0));
        assertNull(map.get(0, 0));
    }
}
