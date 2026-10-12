package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.blocks.BlockMap;
import io.github.ssbaxys.lumina.blocks.BlockModel;
import io.github.ssbaxys.lumina.blocks.BlockStructure;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Slider;
import io.github.ssbaxys.lumina.widget.Toggle;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.Map;

/** 3D-постройки из блоков: готовые модели с вращением мышью и редактор по слоям. */
final class StructuresPage {
    private static final String[] NAMES = {"Дом", "Дерево", "Портал", "Пирамида TNT", "Бассейн"};

    private static final String[] PALETTE = {
            "minecraft:grass_block", "minecraft:dirt", "minecraft:stone", "minecraft:cobblestone",
            "minecraft:oak_planks", "minecraft:oak_log", "minecraft:oak_leaves", "minecraft:glass",
            "minecraft:bricks", "minecraft:sand", "minecraft:water", "minecraft:lava",
            "minecraft:tnt", "minecraft:obsidian", "minecraft:gold_block", "minecraft:diamond_block",
            "minecraft:poppy", "minecraft:torch"};

    /** Размер редактируемой постройки: x, высота, z. */
    private static final int EW = 8;
    private static final int EH = 8;
    private static final int ED = 8;

    private StructuresPage() {
    }

    static UiElement create() {
        BlockStructure[] presets = {house(), tree(), portal(), pyramid(), pool()};
        BlockModel model = new BlockModel(presets[0]).autoRotate(true).size(380, 250);
        return Ui.page(
                Ui.section("Постройки в 3D",
                        "Тяните мышью, чтобы вращать (с инерцией), колесо — приближение. Невидимые грани не рисуются.",
                        gallery(model, presets)),
                Ui.section("Редактор по слоям",
                        "Слева — выбранный слой сверху (ЛКМ — поставить, ПКМ — сломать), справа — живой предпросмотр.",
                        editor(model)));
    }

    // ---------------------------------------------------------------- галерея

    private static UiElement gallery(BlockModel model, BlockStructure[] presets) {
        Button[] buttons = new Button[presets.length];
        for (int i = 0; i < presets.length; i++) {
            int index = i;
            buttons[i] = new Button(NAMES[i], () -> {
                model.structure(presets[index]);
                for (int j = 0; j < buttons.length; j++) {
                    buttons[j].setState(State.CHECKED, j == index);
                }
            });
        }
        buttons[0].setState(State.CHECKED, true);

        Signal<Boolean> spin = Signal.of(true);
        spin.subscribe(model::autoRotate);

        return Ui.column(6,
                Ui.row(6, buttons),
                stage(model),
                Ui.row(8,
                        new Toggle(spin), new Label("Автовращение"),
                        new Button("Изометрия", () -> model.camera(45f, 30f).zoom(1f)).styleClass("button ghost"),
                        new Button("Сверху", () -> model.camera(model.yaw(), 89f)).styleClass("button ghost"),
                        Ui.muted("Перетащите модель мышью")));
    }

    private static Panel stage(UiElement content) {
        Panel stage = Panel.box(new StackLayout(Align.CENTER, Align.CENTER)).add(content);
        stage.setClipChildren(true);
        stage.style(b -> b
                .set(Styles.BACKGROUND, Brush.vertical(0xFF78A7FF, 0xFFCFE0FF))
                .set(Styles.RADIUS, Radii.all(6)));
        return stage;
    }

    // ---------------------------------------------------------------- редактор

    private static UiElement editor(BlockModel gallery) {
        BlockStructure edit = new BlockStructure().fill(0, 0, 0, EW - 1, 0, ED - 1, "minecraft:grass_block");
        Signal<String> brush = Signal.of(PALETTE[2]);
        Signal<Float> layer = Signal.of(1f);
        int[] current = {1};
        boolean[] loading = {false};
        boolean[] slice = {true};

        BlockModel preview = new BlockModel(edit).size(250, 230).cutAbove(1);
        BlockMap map = new BlockMap(EW, ED, 16).editable(brush).grid(true);
        map.onChange((x, z, id) -> {
            if (!loading[0]) {
                edit.set(x, current[0], z, id);
            }
        });
        Runnable load = () -> {
            loading[0] = true;
            for (int z = 0; z < ED; z++) {
                for (int x = 0; x < EW; x++) {
                    map.set(x, z, edit.get(x, current[0], z));
                }
            }
            loading[0] = false;
        };
        layer.subscribe(v -> {
            current[0] = Math.max(0, Math.min(EH - 1, Math.round(v)));
            load.run();
            if (slice[0]) {
                preview.cutAbove(current[0]);
            }
        });
        load.run();

        Panel mapBox = Panel.box(new StackLayout(Align.START, Align.START)).add(map);
        mapBox.style(b -> b
                .set(Styles.BACKGROUND, Brush.solid(0x40000000))
                .set(Styles.RADIUS, Radii.all(4)));
        mapBox.setClipChildren(true);

        Signal<Boolean> sliceOn = Signal.of(true);
        sliceOn.subscribe(on -> {
            slice[0] = on;
            if (on) {
                preview.cutAbove(current[0]);
            } else {
                preview.showAllLayers();
            }
        });

        Panel[] slots = new Panel[PALETTE.length];
        for (int i = 0; i < PALETTE.length; i++) {
            String id = PALETTE[i];
            Panel slot = Panel.box(new StackLayout(Align.CENTER, Align.CENTER)).size(24, 24).add(new IsoIcon(id, 18));
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
        brush.subscribe(cur -> {
            for (int i = 0; i < slots.length; i++) {
                slots[i].setState(State.CHECKED, PALETTE[i].equals(cur));
            }
        });
        slots[2].setState(State.CHECKED, true);

        Panel layerControls = Ui.row(6,
                new Button("Ниже", () -> layer.set((float) Math.max(0, current[0] - 1))).styleClass("button ghost"),
                new Slider(layer, 0, EH - 1).step(1).width(Dim.px(120)),
                new Button("Выше", () -> layer.set((float) Math.min(EH - 1, current[0] + 1))).styleClass("button ghost"),
                Label.bound(layer.map(v -> "Слой y = " + Math.round(v))));

        return Ui.column(6,
                Ui.grid(9, 4, slots),
                layerControls,
                Ui.row(10, Ui.column(6, mapBox, Ui.row(6, new Toggle(sliceOn), Ui.muted("Срез по слою"))),
                        stage(preview)),
                Ui.row(6,
                        new Button("Очистить", () -> {
                            edit.clear().fill(0, 0, 0, EW - 1, 0, ED - 1, "minecraft:grass_block");
                            load.run();
                        }),
                        new Button("Взять из галереи", () -> {
                            copyInto(gallery.structure(), edit);
                            load.run();
                        }).styleClass("button ghost"),
                        Label.bound(brush.map(id -> "Блок: " + id)).style(Ui.MUTED)));
    }

    /** Скопировать постройку в редактор (сдвинув к началу координат и обрезав по его размеру). */
    private static void copyInto(BlockStructure from, BlockStructure to) {
        to.clear();
        BlockStructure.Bounds b = from.bounds();
        if (b == null) {
            return;
        }
        from.forEach((x, y, z, id) -> {
            int nx = x - b.minX();
            int ny = y - b.minY();
            int nz = z - b.minZ();
            if (nx < EW && ny < EH && nz < ED) {
                to.set(nx, ny, nz, id);
            }
        });
    }

    /** Блок палитры в изометрии ({@link BlockModel#drawBlockIcon}). */
    private static final class IsoIcon extends Widget<IsoIcon> {
        private final String blockId;

        IsoIcon(String blockId, float size) {
            super("iso-icon");
            this.blockId = blockId;
            size(size, size);
        }

        @Override
        protected void paintContent(Canvas canvas, float partialTick) {
            float t = Lumina.isInitialized() ? Lumina.platform().nanoTime() / 1e9f : 0f;
            BlockModel.drawBlockIcon(canvas, blockId, 0, 0, width(), t);
        }
    }

    // ---------------------------------------------------------------- готовые постройки

    static BlockStructure house() {
        return BlockStructure.fromLayers(Map.of(
                        'C', "minecraft:cobblestone", 'L', "minecraft:oak_log", 'P', "minecraft:oak_planks",
                        'G', "minecraft:glass", 'D', "minecraft:oak_door", 'S', "minecraft:spruce_planks",
                        'T', "minecraft:torch"),
                "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC", "CCCCCCC",
                "---",
                "LPPPPPL", "PT....P", "P.....P", "P.....P", "P.....P", "P.....P", "LPPDPPL",
                "---",
                "LPGGGPL", "P.....P", "G.....G", "G.....G", "G.....G", "P.....P", "LGPDPGL",
                "---",
                "LPPPPPL", "P.....P", "P.....P", "P.....P", "P.....P", "P.....P", "LPPPPPL",
                "---",
                "SSSSSSS", "SSSSSSS", "SSSSSSS", "SSSSSSS", "SSSSSSS", "SSSSSSS", "SSSSSSS",
                "---",
                ".......", ".SSSSS.", ".SSSSS.", ".SSSSS.", ".SSSSS.", ".SSSSS.", ".......",
                "---",
                ".......", ".......", "..SSS..", "..SSS..", "..SSS..", ".......", ".......");
    }

    static BlockStructure tree() {
        return BlockStructure.fromLayers(Map.of(
                        'D', "minecraft:dirt", 'G', "minecraft:grass_block", 'O', "minecraft:oak_log",
                        'L', "minecraft:oak_leaves", 'R', "minecraft:poppy", 'Y', "minecraft:dandelion",
                        'F', "minecraft:short_grass"),
                "DDDDD", "DDDDD", "DDDDD", "DDDDD", "DDDDD",
                "---",
                "GGGGG", "GGGGG", "GGGGG", "GGGGG", "GGGGG",
                "---",
                "....Y", ".F...", "..O..", "...F.", "R....",
                "---",
                ".....", ".....", "..O..", ".....", ".....",
                "---",
                ".LLL.", "LLLLL", "LLOLL", "LLLLL", ".LLL.",
                "---",
                ".LLL.", "LLLLL", "LLOLL", "LLLLL", ".LLL.",
                "---",
                ".....", ".LLL.", ".LLL.", ".LLL.", ".....",
                "---",
                ".....", "..L..", ".LLL.", "..L..", ".....");
    }

    static BlockStructure portal() {
        return BlockStructure.fromLayers(Map.of(
                        'N', "minecraft:netherrack", 'O', "minecraft:obsidian", 'P', "minecraft:nether_portal",
                        'F', "minecraft:fire"),
                "NNNNNN", "NNNNNN", "NNNNNN",
                "---",
                "F.....", ".OOOO.", ".....F",
                "---",
                "......", ".OPPO.", "......",
                "---",
                "......", ".OPPO.", "......",
                "---",
                "......", ".OPPO.", "......",
                "---",
                "......", ".OOOO.", "......");
    }

    static BlockStructure pyramid() {
        return BlockStructure.fromLayers(Map.of('G', "minecraft:grass_block", 'T', "minecraft:tnt"),
                "GGGGGGG", "GGGGGGG", "GGGGGGG", "GGGGGGG", "GGGGGGG", "GGGGGGG", "GGGGGGG",
                "---",
                ".......", ".TTTTT.", ".TTTTT.", ".TTTTT.", ".TTTTT.", ".TTTTT.", ".......",
                "---",
                ".......", ".......", "..TTT..", "..TTT..", "..TTT..", ".......", ".......",
                "---",
                ".......", ".......", ".......", "...T...", ".......", ".......", ".......");
    }

    static BlockStructure pool() {
        return BlockStructure.fromLayers(Map.of(
                        'B', "minecraft:stone_bricks", 'W', "minecraft:water", 'S', "minecraft:sand",
                        'R', "minecraft:poppy", 'U', "minecraft:sugar_cane"),
                "SSSSSSS", "SBBBBBS", "SBBBBBS", "SBBBBBS", "SSSSSSS",
                "---",
                "BBBBBBB", "BWWWWWB", "BWWWWWB", "BWWWWWB", "BBBBBBB",
                "---",
                "R.....U", ".......", ".......", ".......", "U.....R",
                "---",
                "......U", ".......", ".......", ".......", "U......");
    }
}
