package io.github.ssbaxys.lumina.blocks;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 2D-карта из блоков игры (вид сбоку): каждая клетка — id блока или пусто.
 * Блоки рисуются своими текстурами из ресурсов игры, вода, лава и огонь анимируются.
 *
 * <pre>{@code
 * BlockMap map = BlockMap.of(16,
 *         "....T....",
 *         "GGGGGGWWG",
 *         "DDDDDDDDD",
 *         "SSSCSSSSS")
 *     .key('G', "minecraft:grass_block").key('D', "minecraft:dirt").key('S', "minecraft:stone")
 *     .key('C', "minecraft:coal_ore").key('W', "minecraft:water").key('T', "minecraft:tnt");
 * map.set(4, 0, "minecraft:fire");
 * map.editable(brush);   // ЛКМ ставит блок из brush, ПКМ ломает
 * }</pre>
 */
public class BlockMap extends Widget<BlockMap> implements Animation {
    /** Клетка изменилась (из кода или мышью). {@code blockId} — {@code null}, если блок убран. */
    @FunctionalInterface
    public interface Listener {
        void changed(int x, int y, String blockId);
    }

    private final int cols;
    private final int rows;
    /** Нахлёст непрозрачных блоков на соседей, в единицах интерфейса. */
    private static final float SEAM = 0.5f;
    private final float tile;
    private final String[] cells;
    private final char[] chars;
    private final Map<Character, String> key = new LinkedHashMap<>();
    private ReadableSignal<String> brush;
    private Listener listener;
    private int hoverX = -1;
    private int hoverY = -1;
    private int painting;
    private boolean grid;
    private float time;
    private boolean alive;

    public BlockMap(int cols, int rows, float tile) {
        super("block-map");
        this.cols = Math.max(1, cols);
        this.rows = Math.max(1, rows);
        this.tile = tile;
        this.cells = new String[this.cols * this.rows];
        this.chars = new char[this.cols * this.rows];
        size(this.cols * tile, this.rows * tile);
    }

    /** Карта из строк-рядов: символ = блок по {@link #key}; символы без ключа (пробел, точка) — пусто. */
    public static BlockMap of(float tile, String... rows) {
        int w = 0;
        for (String r : rows) {
            w = Math.max(w, r.length());
        }
        BlockMap map = new BlockMap(w, rows.length, tile);
        for (int y = 0; y < rows.length; y++) {
            for (int x = 0; x < rows[y].length(); x++) {
                map.chars[y * map.cols + x] = rows[y].charAt(x);
            }
        }
        return map;
    }

    /** Символ рядов → блок. Применяется к уже заданным рядам. */
    public BlockMap key(char symbol, String blockId) {
        key.put(symbol, blockId);
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == symbol) {
                cells[i] = blockId;
            }
        }
        return this;
    }

    public int columns() {
        return cols;
    }

    public int rowCount() {
        return rows;
    }

    public float tileSize() {
        return tile;
    }

    /** Блок в клетке или {@code null}. */
    public String get(int x, int y) {
        return inside(x, y) ? cells[y * cols + x] : null;
    }

    /** Поставить блок ({@code null} — убрать). */
    public BlockMap set(int x, int y, String blockId) {
        if (inside(x, y)) {
            int i = y * cols + x;
            if (!java.util.Objects.equals(cells[i], blockId)) {
                cells[i] = blockId;
                chars[i] = symbolOf(blockId);
                if (listener != null) {
                    listener.changed(x, y, blockId);
                }
            }
        }
        return this;
    }

    /** Залить прямоугольник клеток (включительно). */
    public BlockMap fill(int x0, int y0, int x1, int y1, String blockId) {
        for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
            for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++) {
                set(x, y, blockId);
            }
        }
        return this;
    }

    public BlockMap clear() {
        return fill(0, 0, cols - 1, rows - 1, null);
    }

    /**
     * Карта обратно в ряды по ключу (для сохранения). Блоки без символа в ключе получают свободные
     * символы, добавленные в ключ; пустые клетки — точка.
     */
    public String[] toRows() {
        String[] out = new String[rows];
        for (int y = 0; y < rows; y++) {
            StringBuilder sb = new StringBuilder(cols);
            for (int x = 0; x < cols; x++) {
                String id = cells[y * cols + x];
                sb.append(id == null ? '.' : symbolFor(id));
            }
            out[y] = sb.toString();
        }
        return out;
    }

    /** Ключ символ → блок (в том числе символы, выданные {@link #toRows}). */
    public Map<Character, String> keyMap() {
        return java.util.Collections.unmodifiableMap(key);
    }

    /** Редактирование мышью: ЛКМ (и протяжка) ставит блок из {@code brush}, ПКМ ломает. */
    public BlockMap editable(ReadableSignal<String> brush) {
        this.brush = brush;
        return this;
    }

    public BlockMap onChange(Listener listener) {
        this.listener = listener;
        return this;
    }

    /** Показать сетку клеток (удобно в редакторе). */
    public BlockMap grid(boolean show) {
        this.grid = show;
        return this;
    }

    private boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < cols && y < rows;
    }

    private char symbolOf(String blockId) {
        if (blockId == null) {
            return 0;
        }
        for (Map.Entry<Character, String> e : key.entrySet()) {
            if (e.getValue().equals(blockId)) {
                return e.getKey();
            }
        }
        return 0;
    }

    private char symbolFor(String blockId) {
        char c = symbolOf(blockId);
        if (c != 0) {
            return c;
        }
        String pool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789#@$%&*+=?!";
        for (int i = 0; i < pool.length(); i++) {
            char p = pool.charAt(i);
            if (!key.containsKey(p)) {
                key.put(p, blockId);
                return p;
            }
        }
        return '?';
    }

    // ---------------------------------------------------------------- ввод

    @Override
    protected void handleEvent(UiEvent event) {
        if (brush == null || !(event instanceof PointerEvent p)) {
            return;
        }
        int x = (int) Math.floor(p.localX() / tile);
        int y = (int) Math.floor(p.localY() / tile);
        if (event.type() == PointerEvent.PRESS) {
            painting = p.button() == Keys.MOUSE_RIGHT ? 2 : p.button() == Keys.MOUSE_LEFT ? 1 : 0;
            paint(x, y);
            event.consume();
        } else if (event.type() == PointerEvent.DRAG) {
            hoverX = x;
            hoverY = y;
            paint(x, y);
            event.consume();
        } else if (event.type() == PointerEvent.RELEASE) {
            painting = 0;
        } else if (event.type() == PointerEvent.MOVE) {
            hoverX = x;
            hoverY = y;
        } else if (event.type() == PointerEvent.LEAVE) {
            hoverX = -1;
            hoverY = -1;
        }
    }

    private void paint(int x, int y) {
        if (painting == 1) {
            set(x, y, brush.get());
        } else if (painting == 2) {
            set(x, y, null);
        }
    }

    // ---------------------------------------------------------------- рисование

    @Override
    protected void onAttach() {
        alive = true;
        root().animator().start(this);
    }

    @Override
    protected void onDetach() {
        alive = false;
    }

    @Override
    public boolean tick(float dt) {
        time += dt;
        return alive;
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                String id = cells[y * cols + x];
                if (id != null) {
                    // Непрозрачный блок чуть заходит на соседа справа и снизу: при дробном масштабе
                    // интерфейса граница может лечь посреди пикселя экрана, и тогда между блоками
                    // просвечивает фон.
                    float w = tile;
                    float h = tile;
                    if (opaque(id)) {
                        if (x + 1 < cols && cells[y * cols + x + 1] != null && opaque(cells[y * cols + x + 1])) {
                            w += SEAM;
                        }
                        if (y + 1 < rows && cells[(y + 1) * cols + x] != null && opaque(cells[(y + 1) * cols + x])) {
                            h += SEAM;
                        }
                    }
                    drawBlock(canvas, id, x * tile, y * tile, w, h, time);
                }
            }
        }
        if (grid) {
            for (int x = 0; x <= cols; x++) {
                canvas.fillRect(x * tile, 0, 1f, rows * tile, 0x22FFFFFF);
            }
            for (int y = 0; y <= rows; y++) {
                canvas.fillRect(0, y * tile, cols * tile, 1f, 0x22FFFFFF);
            }
        }
        if (brush != null && inside(hoverX, hoverY)) {
            float hx = hoverX * tile;
            float hy = hoverY * tile;
            String b = brush.get();
            if (b != null && cells[hoverY * cols + hoverX] == null) {
                canvas.save();
                canvas.multiplyOpacity(0.45f);
                drawBlock(canvas, b, hx, hy, tile, time);
                canvas.restore();
            }
            canvas.strokeRoundRect(hx, hy, tile, tile, Radii.all(0), 1f, Color.WHITE);
        }
    }

    /** Нарисовать один блок сбоку в квадрате (x, y, size): текстурой, а без неё — иконкой предмета. */
    public static void drawBlock(Canvas canvas, String blockId, float x, float y, float size, float time) {
        drawBlock(canvas, blockId, x, y, size, size, time);
    }

    private static void drawBlock(Canvas canvas, String blockId, float x, float y, float w, float h, float time) {
        BlockTextures.Face face = BlockTextures.side(blockId);
        if (face != null) {
            face.sheet().draw(canvas, face.sheet().frameAt(time), x, y, w, h, face.tint());
        } else {
            canvas.drawItem(blockId, x, y, Math.min(w, h));
        }
    }

    /** Блок без прозрачности: вода, стекло, лёд и листва просвечивают, и нахлёст на них был бы заметен. */
    private static boolean opaque(String blockId) {
        BlockTextures.Face face = BlockTextures.side(blockId);
        if (face == null || (face.tint() >>> 24) != 0xFF) {
            return false;
        }
        return !(blockId.contains("glass") || blockId.contains("ice") || blockId.contains("leaves")
                || blockId.contains("water") || blockId.contains("slime") || blockId.contains("honey"));
    }
}
