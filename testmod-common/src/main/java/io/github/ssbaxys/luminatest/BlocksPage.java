package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.blocks.BlockMap;
import io.github.ssbaxys.lumina.fx.FxLayer;
import io.github.ssbaxys.lumina.fx.VanillaFx;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.markdown.Markdown;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Icon;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;

import java.util.Map;

/** 2D-карты из блоков игры: готовая сцена (TNT взрывается по клику) и редактор. */
final class BlocksPage {
    private static final String[] SCENE = {
            "................................",
            "....LLL.........................",
            "...LLLLL..................T.....",
            "....LOL..........G.......TTT....",
            ".....O.....GGGGGGDGG...GGGGGGG..",
            "GGGGGGGGGGGDDDDDDDDDWWWDDDDDDDGG",
            "DDDDDDDDDDDDSSSSSSSSSWSSSSSDDDDD",
            "SSSSCSSSSSSSSSSSISSSSSSSSSSSSSSS",
            "SSSSSSSSNNNNSSSSSSSSSSXXXSSSSSDS",
            "SSSDSSSSNFFNSSSSSSSSSSXVVXSSSSSS",
            "SSSSSSSSNNNNSSSSSSSSSSXXXSSLLASS",
            "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
    };

    private static final Map<Character, String> KEY = Map.ofEntries(
            Map.entry('G', "minecraft:grass_block"),
            Map.entry('D', "minecraft:dirt"),
            Map.entry('S', "minecraft:stone"),
            Map.entry('C', "minecraft:coal_ore"),
            Map.entry('I', "minecraft:iron_ore"),
            Map.entry('A', "minecraft:diamond_ore"),
            Map.entry('L', "minecraft:oak_leaves"),
            Map.entry('O', "minecraft:oak_log"),
            Map.entry('W', "minecraft:water"),
            Map.entry('T', "minecraft:tnt"),
            Map.entry('N', "minecraft:netherrack"),
            Map.entry('F', "minecraft:fire"),
            Map.entry('X', "minecraft:soul_soil"),
            Map.entry('V', "minecraft:soul_fire"),
            Map.entry('B', "minecraft:bedrock"));

    private static final String[] PALETTE = {
            "minecraft:grass_block", "minecraft:dirt", "minecraft:stone", "minecraft:cobblestone",
            "minecraft:oak_planks", "minecraft:oak_log", "minecraft:oak_leaves", "minecraft:glass",
            "minecraft:bricks", "minecraft:sand", "minecraft:water", "minecraft:lava",
            "minecraft:tnt", "minecraft:gold_block", "minecraft:diamond_block", "minecraft:netherrack",
            "minecraft:fire", "minecraft:soul_fire"};

    private BlocksPage() {
    }

    static UiElement create() {
        return Ui.page(
                Ui.section("Карта из блоков",
                        "Каждая клетка — блок игры со своей текстурой; вода и огонь анимируются. Кликните по TNT.",
                        scene()),
                Ui.section("Редактор",
                        "ЛКМ — поставить выбранный блок (можно вести мышью), ПКМ — сломать.",
                        editor()));
    }

    private static UiElement scene() {
        BlockMap map = BlockMap.of(16, SCENE);
        KEY.forEach(map::key);
        FxLayer fx = new FxLayer();
        Panel stage = Panel.box(new StackLayout(Align.START, Align.START)).add(map, fx);
        stage.setClipChildren(true);
        stage.style(b -> b
                .set(Styles.BACKGROUND, Brush.vertical(0xFF78A7FF, 0xFFB9D2FF))
                .set(Styles.RADIUS, Radii.all(6)));
        stage.onClick(e -> {
            int cx = (int) (e.localX() / map.tileSize());
            int cy = (int) (e.localY() / map.tileSize());
            if ("minecraft:tnt".equals(map.get(cx, cy))) {
                explode(map, fx, stage, cx, cy);
            }
        });
        return stage;
    }

    /** Взрыв TNT: соседние TNT детонируют цепочкой, блоки в радиусе 2 исчезают (кроме бедрока). */
    private static void explode(BlockMap map, FxLayer fx, Panel stage, int cx, int cy) {
        float t = map.tileSize();
        map.set(cx, cy, null);
        fx.spawn(VanillaFx.explosion((cx + 0.5f) * t, (cy + 0.5f) * t, 0.7f));
        stage.play(VanillaFx.shake(stage, 4f, 0.4f));
        for (int y = cy - 2; y <= cy + 2; y++) {
            for (int x = cx - 2; x <= cx + 2; x++) {
                String id = map.get(x, y);
                if (id == null || id.equals("minecraft:bedrock") || (x - cx) * (x - cx) + (y - cy) * (y - cy) > 5) {
                    continue;
                }
                if (id.equals("minecraft:tnt")) {
                    explode(map, fx, stage, x, y);
                } else {
                    map.set(x, y, null);
                }
            }
        }
    }

    private static UiElement editor() {
        Signal<String> brush = Signal.of(PALETTE[0]);
        BlockMap map = new BlockMap(32, 10, 16).editable(brush).grid(true);
        map.fill(0, 9, 31, 9, "minecraft:grass_block");
        Panel canvas = Ui.column(0, map);
        canvas.style(b -> b
                .set(Styles.BACKGROUND, Brush.vertical(0xFF78A7FF, 0xFFB9D2FF))
                .set(Styles.RADIUS, Radii.all(6)));
        canvas.setClipChildren(true);

        Panel[] slots = new Panel[PALETTE.length];
        for (int i = 0; i < PALETTE.length; i++) {
            String id = PALETTE[i];
            Panel slot = Panel.box(new StackLayout(Align.CENTER, Align.CENTER)).size(24, 24).add(Icon.item(id, 16));
            slot.style(b -> b
                    .set(Styles.BACKGROUND, Brush.solid(0x30808080))
                    .set(Styles.RADIUS, Radii.all(4))
                    .hovered(h -> h.set(Styles.BACKGROUND, Brush.solid(0x50808080)))
                    .when(State.CHECKED, c -> c
                            .set(Styles.BORDER_WIDTH, 1.5f)
                            .set(Styles.BORDER_COLOR, 0xFFFFFFFF)
                            .set(Styles.BACKGROUND, Brush.solid(0x60FFFFFF))));
            slot.onClick(e -> brush.set(id));
            slots[i] = slot;
        }
        brush.subscribe(current -> {
            for (int i = 0; i < slots.length; i++) {
                slots[i].setState(State.CHECKED, PALETTE[i].equals(current));
            }
        });
        slots[0].setState(State.CHECKED, true);

        Signal<String> code = Signal.of("");
        Markdown export = Markdown.bound(code);
        Panel exportCard = Ui.column(0, export);
        exportCard.style(b -> b.set(Styles.PADDING, Insets.all(6)));
        exportCard.setVisible(false);

        return Ui.column(6,
                Ui.grid(9, 4, slots),
                canvas,
                Ui.row(6,
                        new Button("Очистить", () -> map.clear().fill(0, 9, 31, 9, "minecraft:grass_block")),
                        new Button("Как код", () -> {
                            code.set(asCode(map));
                            exportCard.setVisible(true);
                        }).styleClass("button ghost"),
                        Label.bound(brush.map(id -> "Блок: " + id)).style(Ui.MUTED)),
                exportCard);
    }

    private static String asCode(BlockMap map) {
        StringBuilder sb = new StringBuilder("```java\nBlockMap map = BlockMap.of(16,\n");
        String[] rows = map.toRows();
        for (int i = 0; i < rows.length; i++) {
            sb.append("        \"").append(rows[i]).append(i + 1 < rows.length ? "\",\n" : "\")");
        }
        map.keyMap().forEach((c, id) -> sb.append("\n    .key('").append(c).append("', \"").append(id).append("\")"));
        return sb.append(";\n```").toString();
    }
}
