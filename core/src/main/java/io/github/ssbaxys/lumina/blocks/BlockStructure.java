package io.github.ssbaxys.lumina.blocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Постройка из блоков в 3D: разреженная сетка id блоков по целым координатам (x, y, z), ось y — вверх.
 * Рисуется виджетом {@link BlockModel}.
 *
 * <pre>{@code
 * BlockStructure house = BlockStructure.fromLayers(Map.of('P', "minecraft:oak_planks", 'G', "minecraft:glass"),
 *         "PPP",          // нижний слой (y = 0): строка — ряд по z, символ — x
 *         "PPP",
 *         "---",
 *         "PGP",          // слой y = 1
 *         "P.P");         // '.' и символы без ключа — воздух
 * house.set(1, 2, 1, "minecraft:tnt");
 * BlockStructure wall = BlockStructure.extrude(blockMap, 3);   // 2D-карта → 3 блока в глубину
 * }</pre>
 */
public final class BlockStructure {
    /** Блок изменился. {@code blockId} — {@code null}, если блок убран. */
    @FunctionalInterface
    public interface Listener {
        void changed(int x, int y, int z, String blockId);
    }

    /** Обход блоков постройки. */
    @FunctionalInterface
    public interface Visitor {
        void visit(int x, int y, int z, String blockId);
    }

    /** Габариты постройки (включительно). */
    public record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public int sizeX() {
            return maxX - minX + 1;
        }

        public int sizeY() {
            return maxY - minY + 1;
        }

        public int sizeZ() {
            return maxZ - minZ + 1;
        }
    }

    /** Строка-разделитель слоёв в {@link #fromLayers(Map, String...)} и {@link #parse}. */
    public static final String LAYER_SEPARATOR = "---";

    private static final int BITS = 21;
    private static final int OFFSET = 1 << (BITS - 1);
    private static final long MASK = (1L << BITS) - 1;

    private final HashMap<Long, String> blocks = new HashMap<>();
    private final List<Listener> listeners = new ArrayList<>(1);
    private Bounds bounds;
    private boolean boundsDirty;
    private int revision;

    public BlockStructure() {
    }

    // ---------------------------------------------------------------- создание

    /**
     * Постройка из слоёв снизу вверх. Слои разделяются строкой {@code ---}; в слое строка — ряд
     * с координатой z (первая строка — z = 0), символ — x. Символы без ключа (точка, пробел) — воздух.
     */
    public static BlockStructure fromLayers(Map<Character, String> key, String... lines) {
        List<String[]> layers = new ArrayList<>();
        List<String> cur = new ArrayList<>();
        for (String line : lines) {
            if (line.strip().equals(LAYER_SEPARATOR)) {
                layers.add(cur.toArray(new String[0]));
                cur.clear();
            } else {
                cur.add(line);
            }
        }
        layers.add(cur.toArray(new String[0]));
        return layers(key, layers.toArray(new String[0][]));
    }

    /** Постройка из слоёв снизу вверх: {@code layers[y][z].charAt(x)}. */
    public static BlockStructure layers(Map<Character, String> key, String[][] layersBottomToTop) {
        BlockStructure s = new BlockStructure();
        for (int y = 0; y < layersBottomToTop.length; y++) {
            String[] rows = layersBottomToTop[y];
            for (int z = 0; z < rows.length; z++) {
                String row = rows[z];
                for (int x = 0; x < row.length(); x++) {
                    String id = key.get(row.charAt(x));
                    if (id != null) {
                        s.set(x, y, z, id);
                    }
                }
            }
        }
        return s;
    }

    /**
     * Из текста (как в теге разметки {@code <structure>}): слои через строки {@code ---}, нижний первым;
     * пустые строки пропускаются, отступы срезаются. Ключ — {@code "G=minecraft:grass_block; S=minecraft:stone"}.
     */
    public static BlockStructure parse(String key, String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            if (!line.isBlank()) {
                lines.add(line.strip());
            }
        }
        return fromLayers(parseKey(key), lines.toArray(new String[0]));
    }

    /** Ключ {@code "G=minecraft:grass_block; S=minecraft:stone"} → символ → блок. */
    public static Map<Character, String> parseKey(String key) {
        Map<Character, String> map = new LinkedHashMap<>();
        if (key != null) {
            for (String pair : key.split(";")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && pair.substring(0, eq).strip().length() == 1) {
                    map.put(pair.substring(0, eq).strip().charAt(0), pair.substring(eq + 1).strip());
                }
            }
        }
        return map;
    }

    /**
     * 2D-карту (вид сбоку) — в 3D: каждая клетка становится столбиком из {@code depth} блоков по z.
     * Нижний ряд карты — y = 0, столбец карты — x.
     */
    public static BlockStructure extrude(BlockMap map, int depth) {
        BlockStructure s = new BlockStructure();
        int rows = map.rowCount();
        for (int r = 0; r < rows; r++) {
            for (int x = 0; x < map.columns(); x++) {
                String id = map.get(x, r);
                if (id != null) {
                    s.fill(x, rows - 1 - r, 0, x, rows - 1 - r, Math.max(1, depth) - 1, id);
                }
            }
        }
        return s;
    }

    /** Копия постройки (без слушателей). */
    public BlockStructure copy() {
        BlockStructure s = new BlockStructure();
        s.blocks.putAll(blocks);
        s.boundsDirty = true;
        return s;
    }

    // ---------------------------------------------------------------- блоки

    private static long pack(int x, int y, int z) {
        return ((x + OFFSET) & MASK) << (2 * BITS) | ((y + OFFSET) & MASK) << BITS | ((z + OFFSET) & MASK);
    }

    private static int unpackX(long k) {
        return (int) ((k >>> (2 * BITS)) & MASK) - OFFSET;
    }

    private static int unpackY(long k) {
        return (int) ((k >>> BITS) & MASK) - OFFSET;
    }

    private static int unpackZ(long k) {
        return (int) (k & MASK) - OFFSET;
    }

    /** Блок в точке или {@code null} (воздух). */
    public String get(int x, int y, int z) {
        return blocks.get(pack(x, y, z));
    }

    public boolean contains(int x, int y, int z) {
        return blocks.containsKey(pack(x, y, z));
    }

    /** Поставить блок ({@code null} — убрать). */
    public BlockStructure set(int x, int y, int z, String blockId) {
        long k = pack(x, y, z);
        String old = blockId == null ? blocks.remove(k) : blocks.put(k, blockId);
        if (Objects.equals(old, blockId)) {
            return this;
        }
        if (blockId == null || bounds == null || boundsDirty) {
            boundsDirty = true;
        } else {
            Bounds b = bounds;
            bounds = new Bounds(Math.min(b.minX, x), Math.min(b.minY, y), Math.min(b.minZ, z),
                    Math.max(b.maxX, x), Math.max(b.maxY, y), Math.max(b.maxZ, z));
        }
        revision++;
        for (int i = 0; i < listeners.size(); i++) {
            listeners.get(i).changed(x, y, z, blockId);
        }
        return this;
    }

    public BlockStructure remove(int x, int y, int z) {
        return set(x, y, z, null);
    }

    /** Залить параллелепипед (углы включительно, в любом порядке). {@code null} — очистить. */
    public BlockStructure fill(int x0, int y0, int z0, int x1, int y1, int z1, String blockId) {
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
            for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
                for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
                    set(x, y, z, blockId);
                }
            }
        }
        return this;
    }

    /** Залить габариты {@code box}. */
    public BlockStructure fill(Bounds box, String blockId) {
        return fill(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ, blockId);
    }

    /** Убрать все блоки (слушатели получат каждый убранный блок). */
    public BlockStructure clear() {
        if (listeners.isEmpty()) {
            if (!blocks.isEmpty()) {
                blocks.clear();
                boundsDirty = true;
                revision++;
            }
            return this;
        }
        for (Long k : new ArrayList<>(blocks.keySet())) {
            set(unpackX(k), unpackY(k), unpackZ(k), null);
        }
        return this;
    }

    /** Число блоков. */
    public int size() {
        return blocks.size();
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    /** Габариты или {@code null}, если блоков нет. */
    public Bounds bounds() {
        if (boundsDirty) {
            boundsDirty = false;
            bounds = null;
            int minX = Integer.MAX_VALUE;
            int minY = Integer.MAX_VALUE;
            int minZ = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE;
            int maxY = Integer.MIN_VALUE;
            int maxZ = Integer.MIN_VALUE;
            for (long k : blocks.keySet()) {
                int x = unpackX(k);
                int y = unpackY(k);
                int z = unpackZ(k);
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                maxZ = Math.max(maxZ, z);
            }
            if (!blocks.isEmpty()) {
                bounds = new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
            }
        }
        return bounds;
    }

    /** Обойти все блоки (порядок не определён). */
    public void forEach(Visitor visitor) {
        for (Map.Entry<Long, String> e : blocks.entrySet()) {
            long k = e.getKey();
            visitor.visit(unpackX(k), unpackY(k), unpackZ(k), e.getValue());
        }
    }

    /** Счётчик изменений: растёт при каждой правке (по нему {@link BlockModel} понимает, что пора перестроиться). */
    public int revision() {
        return revision;
    }

    // ---------------------------------------------------------------- слушатели

    public BlockStructure onChange(Listener listener) {
        listeners.add(listener);
        return this;
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
}
