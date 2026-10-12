package io.github.ssbaxys.lumina.map;

/**
 * Постепенно обновляемая сетка цветов вокруг игрока — общая часть для загрузчиков. Загрузчику остаётся
 * прочитать один столбец мира ({@link Column}); сетка сама:
 * <ul>
 *   <li>держит запас вокруг игрока и сдвигается, только когда он отошёл на четверть радиуса;</li>
 *   <li>обновляет не больше {@link #budget(int)} столбцов и не чаще {@link #interval(float)} — не каждый кадр;</li>
 *   <li>сначала заполняет неизвестные клетки, затем по кругу перечитывает остальные (стройка, вода);</li>
 *   <li>затеняет склоны, как на ванильной карте (выше северного соседа — светлее, ниже — темнее).</li>
 * </ul>
 */
public final class MapGridSampler {
    /** Чтение одного столбца мира. */
    @FunctionalInterface
    public interface Column {
        /**
         * Цвет верхнего видимого блока столбца (ARGB, без затенения) или 0, если столбец неизвестен
         * (чанк не загружен). Высоту блока записать в {@code height[0]}.
         */
        int sample(int x, int z, int[] height);
    }

    private static final int UNKNOWN = Integer.MIN_VALUE;

    private final Column column;
    private final int[] heightOut = new int[1];
    private int budget = 1024;
    private long intervalNanos = 50_000_000L;
    private long lastRefresh = Long.MIN_VALUE;

    private int size;
    private int originX;
    private int originZ;
    private int[] colors = new int[0];
    private int[] heights = new int[0];
    private int[] bases = new int[0];
    private int cursor;
    private int unknown;
    private long version;

    public MapGridSampler(Column column) {
        this.column = column;
    }

    /** Сколько столбцов читать за одно обновление (по умолчанию 1024). */
    public MapGridSampler budget(int columns) {
        this.budget = Math.max(1, columns);
        return this;
    }

    /** Не чаще раза в {@code seconds} (по умолчанию 0.05 с). */
    public MapGridSampler interval(float seconds) {
        this.intervalNanos = (long) (Math.max(0f, seconds) * 1_000_000_000L);
        return this;
    }

    /** Забыть всё (смена мира или измерения). */
    public void clear() {
        size = 0;
        colors = new int[0];
        heights = new int[0];
        bases = new int[0];
        lastRefresh = Long.MIN_VALUE;
        version++;
    }

    /** Сколько клеток ещё не прочитано. */
    public int unknownCount() {
        return unknown;
    }

    /**
     * Снимок вокруг игрока. При необходимости сдвигает сетку и читает очередную порцию столбцов.
     *
     * @param nowNanos текущее время (для ограничения частоты)
     */
    public MapSample sample(double playerX, double playerZ, float yaw, int radius, long nowNanos) {
        int r = Math.max(4, Math.min(512, radius));
        int margin = r / 4 + 2;
        int wantSize = 2 * (r + margin) + 1;
        int bx = (int) Math.floor(playerX);
        int bz = (int) Math.floor(playerZ);
        if (wantSize != size) {
            allocate(wantSize, bx - wantSize / 2, bz - wantSize / 2);
        } else {
            int cx = originX + size / 2;
            int cz = originZ + size / 2;
            if (Math.abs(bx - cx) > margin || Math.abs(bz - cz) > margin) {
                shift(bx - size / 2, bz - size / 2);
            }
        }
        if (lastRefresh == Long.MIN_VALUE || nowNanos - lastRefresh >= intervalNanos) {
            lastRefresh = nowNanos;
            refresh();
        }
        return new MapSample(originX, originZ, size, colors, version, playerX, playerZ, yaw);
    }

    private void allocate(int newSize, int ox, int oz) {
        size = newSize;
        originX = ox;
        originZ = oz;
        colors = new int[newSize * newSize];
        heights = new int[newSize * newSize];
        bases = new int[newSize * newSize];
        java.util.Arrays.fill(heights, UNKNOWN);
        unknown = newSize * newSize;
        cursor = 0;
        version++;
    }

    /** Сдвинуть сетку: общая часть переносится, новые клетки неизвестны. */
    private void shift(int ox, int oz) {
        int n = size;
        int[] nc = new int[n * n];
        int[] nh = new int[n * n];
        int[] nb = new int[n * n];
        java.util.Arrays.fill(nh, UNKNOWN);
        int dx = ox - originX;
        int dz = oz - originZ;
        int known = 0;
        for (int j = 0; j < n; j++) {
            int sj = j + dz;
            if (sj < 0 || sj >= n) {
                continue;
            }
            for (int i = 0; i < n; i++) {
                int si = i + dx;
                if (si >= 0 && si < n) {
                    nc[j * n + i] = colors[sj * n + si];
                    nh[j * n + i] = heights[sj * n + si];
                    nb[j * n + i] = bases[sj * n + si];
                    if (nh[j * n + i] != UNKNOWN) {
                        known++;
                    }
                }
            }
        }
        colors = nc;
        heights = nh;
        bases = nb;
        originX = ox;
        originZ = oz;
        unknown = n * n - known;
        version++;
    }

    /** Прочитать порцию столбцов: сначала неизвестные ближе к центру, потом по кругу. */
    private void refresh() {
        int n = size;
        int total = n * n;
        if (total == 0) {
            return;
        }
        boolean changed = false;
        int left = budget;
        if (unknown > 0) {
            // По расширяющимся квадратам от центра: ближние клетки появляются первыми.
            int c = n / 2;
            for (int ring = 0; ring <= c && left > 0; ring++) {
                for (int j = c - ring; j <= c + ring && left > 0; j++) {
                    boolean edgeRow = j == c - ring || j == c + ring;
                    for (int i = c - ring; i <= c + ring && left > 0; i += edgeRow ? 1 : 2 * ring) {
                        int idx = j * n + i;
                        if (heights[idx] == UNKNOWN) {
                            changed |= read(i, j);
                            left--;
                        }
                        if (ring == 0) {
                            break;
                        }
                    }
                }
            }
        }
        for (; left > 0; left--) {
            int idx = cursor;
            cursor = (cursor + 1) % total;
            changed |= read(idx % n, idx / n);
        }
        if (changed) {
            version++;
        }
    }

    private boolean read(int i, int j) {
        int n = size;
        int idx = j * n + i;
        heightOut[0] = 0;
        int base = column.sample(originX + i, originZ + j, heightOut);
        boolean wasUnknown = heights[idx] == UNKNOWN;
        if (base == 0) {
            if (!wasUnknown) {
                unknown++;
            }
            heights[idx] = UNKNOWN;
            bases[idx] = 0;
        } else {
            if (wasUnknown) {
                unknown--;
            }
            heights[idx] = heightOut[0];
            bases[idx] = base;
        }
        boolean changed = recolor(idx);
        // Тень южного соседа зависит от высоты этой клетки.
        if (j + 1 < n) {
            changed |= recolor(idx + n);
        }
        return changed;
    }

    /** Пересчитать затенённый цвет клетки по её высоте и высоте северного соседа. */
    private boolean recolor(int idx) {
        int old = colors[idx];
        int h = heights[idx];
        int color = 0;
        if (h != UNKNOWN && bases[idx] != 0) {
            int north = idx >= size ? heights[idx - size] : UNKNOWN;
            float k = north == UNKNOWN || north == h ? 0.86f : h > north ? 1f : 0.71f;
            color = shade(bases[idx], k);
        }
        colors[idx] = color;
        return color != old;
    }

    /** Цвет, умноженный на яркость (альфа сохраняется). */
    public static int shade(int argb, float k) {
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) * k));
        int g = Math.min(255, (int) (((argb >> 8) & 0xFF) * k));
        int b = Math.min(255, (int) ((argb & 0xFF) * k));
        return (argb & 0xFF000000) | r << 16 | g << 8 | b;
    }
}
