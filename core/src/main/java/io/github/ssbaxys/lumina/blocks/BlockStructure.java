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

    /** Копия, сдвинутая так, что минимальный угол габаритов стал (0, 0, 0). Пустая постройка — пустая копия. */
    public BlockStructure normalized() {
        Bounds b = bounds();
        BlockStructure s = new BlockStructure();
        if (b != null) {
            forEach((x, y, z, id) -> s.set(x - b.minX, y - b.minY, z - b.minZ, id));
        }
        return s;
    }

    // ---------------------------------------------------------------- текстовый формат

    /** Начало строки {@link #encode()}: формат и его версия. */
    public static final String FORMAT = "lumina:1";
    /** Символы палитры: первые 52 блока — одной буквой, дальше — {@code _} и две буквы. */
    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    /** Больше блоков в палитре не бывает: 52 одиночных символа + 52² двойных. */
    public static final int MAX_PALETTE = ALPHABET.length() + ALPHABET.length() * ALPHABET.length();

    /**
     * Постройка одной строкой, чтобы делиться ей, как схематикой:
     * {@code lumina:1;5x4x3;0,0,0;minecraft:stone,minecraft:glass;3AB.4A...}. Части через {@code ;}:
     * формат, размер (x×y×z), минимальный угол, палитра через запятую и блоки по слоям снизу вверх
     * (внутри слоя — ряды по z, в ряду — x). Блок — буква палитры ({@code A} — первый, {@code _Ab} — 53-й и
     * дальше), {@code .} — воздух; число перед символом — сколько раз он повторяется. Воздух в конце не пишется.
     * Обратно — {@link #decode(String)}.
     *
     * @throws IllegalArgumentException если в id есть {@code , ;} или пробелы либо разных блоков больше
     *                                  {@link #MAX_PALETTE}
     */
    public String encode() {
        Bounds b = bounds();
        if (b == null) {
            return FORMAT + ";0x0x0;0,0,0;;";
        }
        Map<String, Integer> palette = new LinkedHashMap<>();
        StringBuilder data = new StringBuilder();
        String prev = null;
        int run = 0;
        int pendingAir = 0;
        for (int y = b.minY; y <= b.maxY; y++) {
            for (int z = b.minZ; z <= b.maxZ; z++) {
                for (int x = b.minX; x <= b.maxX; x++) {
                    String id = get(x, y, z);
                    String sym;
                    if (id == null) {
                        sym = ".";
                    } else {
                        Integer index = palette.get(id);
                        if (index == null) {
                            checkId(id);
                            index = palette.size();
                            if (index >= MAX_PALETTE) {
                                throw new IllegalArgumentException("too many different blocks: " + (index + 1));
                            }
                            palette.put(id, index);
                        }
                        sym = symbol(index);
                    }
                    if (sym.equals(prev)) {
                        run++;
                        continue;
                    }
                    pendingAir = flushRun(data, prev, run, pendingAir);
                    prev = sym;
                    run = 1;
                }
            }
        }
        flushRun(data, prev, run, pendingAir);
        StringBuilder out = new StringBuilder(FORMAT.length() + data.length() + 16 * palette.size() + 32);
        out.append(FORMAT).append(';')
                .append(b.sizeX()).append('x').append(b.sizeY()).append('x').append(b.sizeZ()).append(';')
                .append(b.minX).append(',').append(b.minY).append(',').append(b.minZ).append(';')
                .append(String.join(",", palette.keySet())).append(';')
                .append(data);
        return out.toString();
    }

    /** Дописать серию символов; воздух откладывается, чтобы не писать его в конце. Возвращает отложенный воздух. */
    private static int flushRun(StringBuilder data, String sym, int run, int pendingAir) {
        if (sym == null || run == 0) {
            return pendingAir;
        }
        if (sym.equals(".")) {
            return pendingAir + run;
        }
        if (pendingAir > 0) {
            appendRun(data, ".", pendingAir);
        }
        appendRun(data, sym, run);
        return 0;
    }

    private static void appendRun(StringBuilder data, String sym, int run) {
        if (run > 1) {
            data.append(run);
        }
        data.append(sym);
    }

    private static String symbol(int index) {
        int n = ALPHABET.length();
        if (index < n) {
            return String.valueOf(ALPHABET.charAt(index));
        }
        int i = index - n;
        return "_" + ALPHABET.charAt(i / n) + ALPHABET.charAt(i % n);
    }

    private static void checkId(String id) {
        for (int i = 0; i < id.length(); i++) {
            char c = id.charAt(i);
            if (c == ',' || c == ';' || Character.isWhitespace(c)) {
                throw new IllegalArgumentException("block id can't be encoded: '" + id + "'");
            }
        }
        if (id.isEmpty()) {
            throw new IllegalArgumentException("empty block id");
        }
    }

    /**
     * Постройка из строки {@link #encode()}. Пробелы и переводы строк внутри блоков пропускаются (строку можно
     * переносить); минимальный угол можно опустить ({@code lumina:1;5x4x3;;палитра;блоки} — с нуля).
     *
     * @throws IllegalArgumentException если строка не в этом формате или повреждена
     */
    public static BlockStructure decode(String text) {
        if (text == null) {
            throw new IllegalArgumentException("null structure");
        }
        String[] parts = text.strip().split(";", -1);
        if (parts.length != 5 || !parts[0].strip().equals(FORMAT)) {
            throw new IllegalArgumentException("not a " + FORMAT + " structure");
        }
        String[] size = parts[1].strip().split("x");
        if (size.length != 3) {
            throw new IllegalArgumentException("bad size: " + parts[1]);
        }
        int sx = parseInt(size[0], "size");
        int sy = parseInt(size[1], "size");
        int sz = parseInt(size[2], "size");
        int ox = 0;
        int oy = 0;
        int oz = 0;
        if (!parts[2].isBlank()) {
            String[] o = parts[2].strip().split(",");
            if (o.length != 3) {
                throw new IllegalArgumentException("bad origin: " + parts[2]);
            }
            ox = parseInt(o[0], "origin");
            oy = parseInt(o[1], "origin");
            oz = parseInt(o[2], "origin");
        }
        if (sx < 0 || sy < 0 || sz < 0) {
            throw new IllegalArgumentException("negative size: " + parts[1]);
        }
        long volume = (long) sx * sy * sz;
        List<String> palette = new ArrayList<>();
        if (!parts[3].isBlank()) {
            for (String id : parts[3].split(",")) {
                palette.add(id.strip());
            }
        }
        BlockStructure s = new BlockStructure();
        String data = parts[4].replaceAll("\\s+", "");
        long pos = 0;
        int i = 0;
        int n = data.length();
        while (i < n) {
            char c = data.charAt(i);
            long count = 1;
            if (c >= '0' && c <= '9') {
                count = 0;
                while (i < n && data.charAt(i) >= '0' && data.charAt(i) <= '9') {
                    count = count * 10 + (data.charAt(i) - '0');
                    if (count > volume) {
                        throw new IllegalArgumentException("run longer than the structure");
                    }
                    i++;
                }
                if (i >= n) {
                    throw new IllegalArgumentException("count without a block at the end");
                }
                c = data.charAt(i);
            }
            String id;
            if (c == '.') {
                id = null;
                i++;
            } else {
                int index;
                if (c == '_') {
                    if (i + 2 >= n) {
                        throw new IllegalArgumentException("truncated palette symbol");
                    }
                    int hi = ALPHABET.indexOf(data.charAt(i + 1));
                    int lo = ALPHABET.indexOf(data.charAt(i + 2));
                    if (hi < 0 || lo < 0) {
                        throw new IllegalArgumentException("bad palette symbol at " + i);
                    }
                    index = ALPHABET.length() + hi * ALPHABET.length() + lo;
                    i += 3;
                } else {
                    index = ALPHABET.indexOf(c);
                    if (index < 0) {
                        throw new IllegalArgumentException("unexpected '" + c + "' at " + i);
                    }
                    i++;
                }
                if (index >= palette.size()) {
                    throw new IllegalArgumentException("block #" + (index + 1) + " is not in the palette");
                }
                id = palette.get(index);
            }
            if (pos + count > volume) {
                throw new IllegalArgumentException("more blocks than " + sx + "x" + sy + "x" + sz);
            }
            if (id != null) {
                for (long k = pos; k < pos + count; k++) {
                    int x = (int) (k % sx);
                    int z = (int) (k / sx % sz);
                    int y = (int) (k / ((long) sx * sz));
                    s.set(ox + x, oy + y, oz + z, id);
                }
            }
            pos += count;
        }
        return s;
    }

    private static int parseInt(String s, String what) {
        try {
            return Integer.parseInt(s.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("bad " + what + ": " + s);
        }
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
