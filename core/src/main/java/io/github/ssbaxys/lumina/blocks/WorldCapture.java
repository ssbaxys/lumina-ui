package io.github.ssbaxys.lumina.blocks;

/**
 * Снимок коробки мира в {@link BlockStructure}, который читается порциями по кадрам — как
 * {@code map.MapGridSampler} для карты:
 * <ul>
 *   <li>коробка не больше {@link #MAX_SIZE} блоков по каждой оси (объём до 48³);</li>
 *   <li>{@link #step(WorldBlocks)} читает не больше {@link #budget(int)} блоков, слоями снизу вверх;</li>
 *   <li>воздух не хранится; незагруженные чанки ({@link WorldBlocks#UNLOADED}) оставляют прежний блок;</li>
 *   <li>в постройку пишутся только изменения, поэтому {@link BlockModel} перестраивается, лишь когда мир
 *       действительно поменялся;</li>
 *   <li>сдвиг коробки ({@link #box}) переносит общую часть, убирает ушедшее и начинает новый проход.</li>
 * </ul>
 *
 * <p>Координаты постройки — мировые минус {@link #anchorX()}/{@link #anchorY()}/{@link #anchorZ()}: якорь
 * ставится по первой коробке и не меняется при сдвигах, поэтому старые блоки не переписываются.</p>
 *
 * <pre>{@code
 * WorldCapture capture = new WorldCapture().box(x - 8, y - 4, z - 8, x + 8, y + 8, z + 8);
 * // каждый кадр:
 * capture.step(Lumina.platform().worldBlocks());
 * model.structure(capture.structure());
 * }</pre>
 */
public final class WorldCapture {
    /** Наибольший размер коробки по одной оси. */
    public static final int MAX_SIZE = 48;
    /** Наибольший объём коробки. */
    public static final int MAX_VOLUME = MAX_SIZE * MAX_SIZE * MAX_SIZE;
    /** Дальше от якоря коробка не уходит: постройка начинается заново (упаковка координат — ±1 млн). */
    private static final int REANCHOR = 1 << 19;

    private final BlockStructure structure = new BlockStructure();
    private int budget = 8192;

    private boolean hasBox;
    private int x0;
    private int y0;
    private int z0;
    private int sx;
    private int sy;
    private int sz;
    private int anchorX;
    private int anchorY;
    private int anchorZ;
    /** Что сейчас записано в постройку для каждой клетки коробки ({@code null} — воздух или неизвестно). */
    private String[] cells = new String[0];

    private int cursor;
    private boolean passing;
    private int passes;
    private int unloaded;
    private int unloadedInPass;

    // ---------------------------------------------------------------- настройка

    /** Сколько блоков читать за один {@link #step} (по умолчанию 8192: полный 48³ — за 14 кадров). */
    public WorldCapture budget(int blocks) {
        this.budget = Math.max(1, blocks);
        return this;
    }

    public int budget() {
        return budget;
    }

    /**
     * Коробка мира (углы включительно, в любом порядке). Больше {@link #MAX_SIZE} по оси — обрезается вокруг
     * середины. Та же коробка — ничего не делает; другая — общая часть сохраняется, начинается новый проход.
     */
    public WorldCapture box(int ax, int ay, int az, int bx, int by, int bz) {
        int[] x = clampAxis(ax, bx);
        int[] y = clampAxis(ay, by);
        int[] z = clampAxis(az, bz);
        int nsx = x[1] - x[0] + 1;
        int nsy = y[1] - y[0] + 1;
        int nsz = z[1] - z[0] + 1;
        if (hasBox && x[0] == x0 && y[0] == y0 && z[0] == z0 && nsx == sx && nsy == sy && nsz == sz) {
            return this;
        }
        if (!hasBox || Math.abs(x[0] - anchorX) > REANCHOR || Math.abs(y[0] - anchorY) > REANCHOR
                || Math.abs(z[0] - anchorZ) > REANCHOR) {
            structure.clear();
            anchorX = x[0];
            anchorY = y[0];
            anchorZ = z[0];
            cells = new String[nsx * nsy * nsz];
        } else {
            cells = moveCells(x[0], y[0], z[0], nsx, nsy, nsz);
        }
        hasBox = true;
        x0 = x[0];
        y0 = y[0];
        z0 = z[0];
        sx = nsx;
        sy = nsy;
        sz = nsz;
        restart();
        return this;
    }

    /** Коробка с серединой в точке: {@code radius} в стороны, {@code below} вниз и {@code above} вверх. */
    public WorldCapture around(int x, int y, int z, int radius, int below, int above) {
        return box(x - radius, y - below, z - radius, x + radius, y + above, z + radius);
    }

    private static int[] clampAxis(int a, int b) {
        int lo = Math.min(a, b);
        int hi = Math.max(a, b);
        if ((long) hi - lo + 1 > MAX_SIZE) {
            int mid = (int) (((long) lo + hi) >> 1);
            lo = mid - MAX_SIZE / 2 + 1;
            hi = lo + MAX_SIZE - 1;
        }
        return new int[]{lo, hi};
    }

    /** Перенести общую часть в новую коробку; ушедшие блоки убрать из постройки. */
    private String[] moveCells(int nx0, int ny0, int nz0, int nsx, int nsy, int nsz) {
        String[] next = new String[nsx * nsy * nsz];
        for (int y = 0; y < sy; y++) {
            for (int z = 0; z < sz; z++) {
                for (int x = 0; x < sx; x++) {
                    String id = cells[(y * sz + z) * sx + x];
                    if (id == null) {
                        continue;
                    }
                    int wx = x0 + x;
                    int wy = y0 + y;
                    int wz = z0 + z;
                    int i = wx - nx0;
                    int j = wy - ny0;
                    int k = wz - nz0;
                    if (i >= 0 && j >= 0 && k >= 0 && i < nsx && j < nsy && k < nsz) {
                        next[(j * nsz + k) * nsx + i] = id;
                    } else {
                        structure.set(wx - anchorX, wy - anchorY, wz - anchorZ, null);
                    }
                }
            }
        }
        return next;
    }

    // ---------------------------------------------------------------- чтение

    /** Начать новый проход с начала коробки (перечитать всё: стройка, взрывы, вода). */
    public WorldCapture restart() {
        cursor = 0;
        unloadedInPass = 0;
        passing = hasBox;
        return this;
    }

    /**
     * Прочитать очередную порцию блоков. {@code world == null} (мира нет) — ничего не делает.
     *
     * @return {@code true}, если этой порцией закончился проход
     */
    public boolean step(WorldBlocks world) {
        if (!passing || world == null) {
            return false;
        }
        int total = sx * sy * sz;
        int end = Math.min(total, cursor + budget);
        int layer = sx * sz;
        for (int idx = cursor; idx < end; idx++) {
            int y = idx / layer;
            int rest = idx - y * layer;
            int z = rest / sx;
            int x = rest - z * sx;
            String id = world.blockAt(x0 + x, y0 + y, z0 + z);
            if (id == WorldBlocks.UNLOADED) {
                unloadedInPass++;
                continue;
            }
            String old = cells[idx];
            if (old == null ? id != null : !old.equals(id)) {
                cells[idx] = id;
                structure.set(x0 + x - anchorX, y0 + y - anchorY, z0 + z - anchorZ, id);
            }
        }
        cursor = end;
        if (cursor >= total) {
            passing = false;
            passes++;
            unloaded = unloadedInPass;
            return true;
        }
        return false;
    }

    /** Дочитать текущий проход целиком за один вызов (для тестов и маленьких коробок). */
    public WorldCapture finish(WorldBlocks world) {
        if (world != null) {
            while (passing) {
                step(world);
            }
        }
        return this;
    }

    /** Забыть всё: постройка пуста, коробки нет. */
    public void clear() {
        structure.clear();
        hasBox = false;
        cells = new String[0];
        sx = sy = sz = 0;
        passing = false;
        cursor = 0;
        unloaded = 0;
        unloadedInPass = 0;
    }

    // ---------------------------------------------------------------- состояние

    /** Постройка со снимком (координаты — мировые минус якорь). Принадлежит снимку: не меняйте её. */
    public BlockStructure structure() {
        return structure;
    }

    public boolean hasBox() {
        return hasBox;
    }

    /** Коробка в мировых координатах или {@code null}. */
    public BlockStructure.Bounds worldBox() {
        return hasBox ? new BlockStructure.Bounds(x0, y0, z0, x0 + sx - 1, y0 + sy - 1, z0 + sz - 1) : null;
    }

    /** Коробка в координатах постройки (для {@link BlockModel#frame}) или {@code null}. */
    public BlockStructure.Bounds localBox() {
        if (!hasBox) {
            return null;
        }
        int lx = x0 - anchorX;
        int ly = y0 - anchorY;
        int lz = z0 - anchorZ;
        return new BlockStructure.Bounds(lx, ly, lz, lx + sx - 1, ly + sy - 1, lz + sz - 1);
    }

    public int anchorX() {
        return anchorX;
    }

    public int anchorY() {
        return anchorY;
    }

    public int anchorZ() {
        return anchorZ;
    }

    /** Идёт ли проход. */
    public boolean isPassing() {
        return passing;
    }

    /** Сколько проходов закончено. */
    public int passes() {
        return passes;
    }

    /** Доля текущего прохода, 0..1 (1 — проход закончен). */
    public float progress() {
        int total = sx * sy * sz;
        return !passing || total == 0 ? 1f : cursor / (float) total;
    }

    /** Сколько блоков в последнем законченном проходе пришлось на незагруженные чанки. */
    public int unloadedCount() {
        return unloaded;
    }

    /**
     * Снимок, сдвинутый так, что угол коробки — (0, 0, 0): копия, которой можно делиться
     * ({@link BlockStructure#encode()}).
     */
    public BlockStructure snapshot() {
        BlockStructure s = new BlockStructure();
        int dx = anchorX - x0;
        int dy = anchorY - y0;
        int dz = anchorZ - z0;
        structure.forEach((x, y, z, id) -> s.set(x + dx, y + dy, z + dz, id));
        return s;
    }

    // ---------------------------------------------------------------- сразу

    /**
     * Прочитать коробку мира целиком за один вызов (углы включительно; больше {@link #MAX_SIZE} по оси —
     * обрезается вокруг середины). Координаты результата — от угла коробки: (0, 0, 0) — её минимальный угол.
     * Незагруженные блоки пропускаются. {@code world == null} — {@code null}.
     */
    public static BlockStructure capture(WorldBlocks world, int ax, int ay, int az, int bx, int by, int bz) {
        if (world == null) {
            return null;
        }
        int[] x = clampAxis(ax, bx);
        int[] y = clampAxis(ay, by);
        int[] z = clampAxis(az, bz);
        BlockStructure s = new BlockStructure();
        for (int wy = y[0]; wy <= y[1]; wy++) {
            for (int wz = z[0]; wz <= z[1]; wz++) {
                for (int wx = x[0]; wx <= x[1]; wx++) {
                    String id = world.blockAt(wx, wy, wz);
                    if (id != null && id != WorldBlocks.UNLOADED) {
                        s.set(wx - x[0], wy - y[0], wz - z[0], id);
                    }
                }
            }
        }
        return s;
    }
}
