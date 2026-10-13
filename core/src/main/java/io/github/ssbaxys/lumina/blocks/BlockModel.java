package io.github.ssbaxys.lumina.blocks;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.ScrollEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 3D-модель постройки ({@link BlockStructure}): камера с поворотом (yaw) и наклоном (pitch), по умолчанию —
 * классическая изометрия 45°/30° без перспективы; {@link #perspective(float)} включает настоящую перспективу
 * (дальнее меньше ближнего). Мышью модель вращается
 * (с инерцией), колесом — приближается; можно включить медленное автовращение.
 *
 * <p>Рисуются только грани, не закрытые непрозрачным соседом; стекло, листва, вода, огонь, факелы
 * и т. п. соседей не закрывают. Грани сортируются от дальних к ближним (алгоритм художника)
 * только при смене камеры, затеняются как в игре (верх 1.0, север/юг 0.8, запад/восток 0.6,
 * низ 0.5); анимированные текстуры идут по времени. Без текстуры грань заливается цветом,
 * вычисленным из id блока.</p>
 *
 * <pre>{@code
 * BlockModel model = new BlockModel(house).autoRotate(true).size(200, 200);
 * model.camera(30, 20);   // свой ракурс
 * }</pre>
 */
public class BlockModel extends Widget<BlockModel> implements Animation {
    /** Скорость автовращения по умолчанию, °/с. */
    public static final float DEFAULT_SPIN = 20f;
    /** Градусов поворота на пиксель протяжки. */
    public static final float DRAG_SENSITIVITY = 0.6f;

    // Грани: верх, низ, север (-z), юг (+z), запад (-x), восток (+x) и две диагонали «креста» (цветы, огонь).
    private static final int UP = 0;
    private static final int DOWN = 1;
    private static final int CROSS_A = 6;
    private static final int CROSS_B = 7;
    private static final int[][] NORMALS = {{0, 1, 0}, {0, -1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
    private static final float[] SHADE = {1f, 0.5f, 0.8f, 0.8f, 0.6f, 0.6f, 0.9f, 0.9f};
    /** Углы грани, как её видно снаружи: левый верхний, левый нижний, правый нижний, правый верхний. */
    private static final float[][] CORNERS = {
            {0, 1, 0, 0, 1, 1, 1, 1, 1, 1, 1, 0},
            {0, 0, 1, 0, 0, 0, 1, 0, 0, 1, 0, 1},
            {1, 1, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0},
            {0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1, 1},
            {0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1},
            {1, 1, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0},
            {0, 1, 0, 0, 0, 0, 1, 0, 1, 1, 1, 1},
            {0, 1, 1, 0, 0, 1, 1, 0, 0, 1, 1, 0},
    };
    private static final String[] SEE_THROUGH = {"glass", "_leaves", "water", "lava", "fire", "torch", "_door",
            "trapdoor", "_pane", "iron_bars", "fence", "ladder", "rail", "portal", "cobweb", "_sapling", "slab",
            "stairs", "sign", "lantern", "chain", "vine", "_button", "pressure_plate", "lever", "redstone_wire",
            "barrier", "spawner", "beacon", "scaffolding", "kelp", "seagrass", "fern", "tall_grass", "short_grass",
            "dead_bush", "sugar_cane", "_mushroom", "tulip", "poppy", "dandelion", "orchid", "allium", "bluet",
            "daisy", "cornflower", "lily", "bamboo", "_carpet", "slime_block", "honey_block", "bush", "_rose"};
    private static final String[] CROSS = {"fire", "torch", "_sapling", "poppy", "dandelion", "tulip", "orchid",
            "allium", "azure_bluet", "oxeye_daisy", "cornflower", "lily_of_the_valley", "short_grass", "fern",
            "tall_grass", "dead_bush", "sugar_cane", "cobweb", "sweet_berry_bush", "wither_rose", "seagrass"};
    private static final Map<String, Integer> KIND = new ConcurrentHashMap<>();
    private static final int OPAQUE = 0;
    private static final int CLEAR = 1;
    private static final int CROSSED = 2;
    private static final Map<String, Integer> COLORS = Map.ofEntries(
            Map.entry("grass_block", 0xFF866043), Map.entry("dirt", 0xFF866043), Map.entry("stone", 0xFF7D7D7D),
            Map.entry("cobblestone", 0xFF6F6F6F), Map.entry("oak_planks", 0xFFA2834F), Map.entry("oak_log", 0xFF6B5130),
            Map.entry("oak_leaves", 0xFF3E7A1E), Map.entry("glass", 0x80C8E8F0), Map.entry("water", 0xA03F76E4),
            Map.entry("lava", 0xFFD9601A), Map.entry("sand", 0xFFDBCFA3), Map.entry("tnt", 0xFFC8432F),
            Map.entry("obsidian", 0xFF1B1529), Map.entry("nether_portal", 0xB07A2BD9), Map.entry("netherrack", 0xFF6F3535),
            Map.entry("bricks", 0xFF96614F), Map.entry("gold_block", 0xFFF3CF3E), Map.entry("diamond_block", 0xFF62DBD5),
            Map.entry("bedrock", 0xFF444444), Map.entry("fire", 0xE0F0A030), Map.entry("snow_block", 0xFFF0FBFB),
            Map.entry("stone_bricks", 0xFF7A7A7A), Map.entry("spruce_planks", 0xFF735531),
            Map.entry("oak_door", 0xFF8C6B3F), Map.entry("torch", 0xE0FFD25A), Map.entry("poppy", 0xE0D23A2A),
            Map.entry("dandelion", 0xE0F2D33A), Map.entry("short_grass", 0xC060A040), Map.entry("sugar_cane", 0xD08DC266),
            Map.entry("coal_ore", 0xFF6A6A6A), Map.entry("iron_ore", 0xFF8C7F74), Map.entry("diamond_ore", 0xFF6F8F8F),
            Map.entry("gravel", 0xFF847F7E), Map.entry("soul_fire", 0xE040D0E0), Map.entry("ice", 0xB091B7F5));
    private static final Brush SHADOW = Brush.radial(0.5f, 0.5f, 0.5f, 0x55000000, 0x00000000);

    // изометрия для иконок блоков
    private static final float ICON_C = (float) Math.cos(Math.toRadians(45));
    private static final float ICON_SP = 0.5f;
    private static final float ICON_CP = (float) Math.cos(Math.toRadians(30));
    private static final int[] ICON_CUBE = {3, 5, UP};
    private static final int[] ICON_CROSS = {CROSS_A, CROSS_B};

    private BlockStructure structure;
    private BlockStructure builtFor;
    private int builtRevision = -1;
    private int builtCut;
    private int cutAbove = Integer.MAX_VALUE;

    // грани (параллельные массивы)
    private int faceCount;
    private int[] fx = new int[0];
    private int[] fy = new int[0];
    private int[] fz = new int[0];
    private byte[] fdir = new byte[0];
    private BlockTextures.Face[] ftex = new BlockTextures.Face[0];
    private int[] fcolor = new int[0];
    private float[] fdepth = new float[0];
    private float[] screen = new float[0];
    private int[] order = new int[0];
    private int[] sortTmp = new int[0];
    private int drawCount;

    // кэш проекции
    private boolean projected;
    private float pYaw;
    private float pPitch;
    private float pScale;
    private float pPersp;
    private float pW;
    private float pH;
    private float shadowX;
    private float shadowY;
    private float shadowR;

    // камера
    private float yaw = 45f;
    private float pitch = 30f;
    private float zoom = 1f;
    private float zoomTarget = 1f;
    private float perspective;
    private float tile;
    private float spin;
    private float yawVelocity;
    private float dragAccum;
    private boolean dragging;
    private float lastX;
    private float lastY;
    private boolean interactive = true;
    private boolean zoomable = true;
    private boolean shadow = true;

    private float time;
    private boolean alive;

    public BlockModel(BlockStructure structure) {
        super("block-model");
        this.structure = structure == null ? new BlockStructure() : structure;
    }

    // ---------------------------------------------------------------- настройка

    public BlockStructure structure() {
        return structure;
    }

    /** Показать другую постройку. */
    public BlockModel structure(BlockStructure structure) {
        this.structure = structure == null ? new BlockStructure() : structure;
        if (tile > 0f) {
            invalidateLayout();
        }
        return self();
    }

    public float yaw() {
        return yaw;
    }

    public float pitch() {
        return pitch;
    }

    /** Поворот вокруг вертикали, градусы (45 — классическая изометрия). */
    public BlockModel yaw(float degrees) {
        this.yaw = wrap(degrees);
        return self();
    }

    /** Наклон камеры, градусы: 0 — сбоку, 90 — сверху, отрицательный — снизу. */
    public BlockModel pitch(float degrees) {
        this.pitch = Math.max(-89f, Math.min(89f, degrees));
        if (tile > 0f) {
            invalidateLayout();
        }
        return self();
    }

    public BlockModel camera(float yawDegrees, float pitchDegrees) {
        return yaw(yawDegrees).pitch(pitchDegrees);
    }

    /** Автовращение со скоростью {@link #DEFAULT_SPIN} или выключить. */
    public BlockModel autoRotate(boolean on) {
        return autoRotate(on ? DEFAULT_SPIN : 0f);
    }

    /** Автовращение, °/с (0 — выключено, отрицательное — в другую сторону). */
    public BlockModel autoRotate(float degreesPerSecond) {
        this.spin = degreesPerSecond;
        return self();
    }

    public boolean isAutoRotating() {
        return spin != 0f;
    }

    public float zoom() {
        return zoomTarget;
    }

    /** Приближение (1 — модель вписана в виджет). */
    public BlockModel zoom(float zoom) {
        this.zoomTarget = clampZoom(zoom);
        this.zoom = zoomTarget;
        return self();
    }

    /** Вращение мышью (по умолчанию включено). */
    public BlockModel interactive(boolean interactive) {
        this.interactive = interactive;
        return self();
    }

    /** Приближение колесом (по умолчанию включено). */
    public BlockModel zoomable(boolean zoomable) {
        this.zoomable = zoomable;
        return self();
    }

    public float perspective() {
        return perspective;
    }

    /**
     * Сила перспективы от 0 (изометрия, параллельные рёбра остаются параллельными) до 1 (камера совсем
     * близко); 0.3–0.5 выглядит как обычная камера. Размер модели при вращении не меняется.
     */
    public BlockModel perspective(float strength) {
        this.perspective = Math.max(0f, Math.min(1f, strength));
        return self();
    }

    /** Мягкая тень-эллипс под моделью. */
    public BlockModel shadow(boolean shadow) {
        this.shadow = shadow;
        return self();
    }

    /**
     * Постоянный размер блока в пикселях вместо вписывания в виджет; размер виджета по умолчанию
     * тогда считается по габаритам постройки.
     */
    public BlockModel tile(float pixels) {
        this.tile = Math.max(0f, pixels);
        invalidateLayout();
        return self();
    }

    /** Показывать только слои до {@code y} включительно (срез, как в редакторе слоёв). */
    public BlockModel cutAbove(int y) {
        this.cutAbove = y;
        return self();
    }

    /** Показывать все слои. */
    public BlockModel showAllLayers() {
        return cutAbove(Integer.MAX_VALUE);
    }

    // ---------------------------------------------------------------- геометрия

    /** Сколько граней не закрыто соседями (с любой стороны; не зависит от камеры). */
    public int exposedFaceCount() {
        ensureFaces();
        return faceCount;
    }

    /** Сколько граней рисуется при текущей камере (без граней, повёрнутых от зрителя). */
    public int visibleFaceCount() {
        ensureProjection();
        return drawCount;
    }

    /**
     * Порядок отрисовки при текущей камере: {@code {x, y, z, грань}} от дальних к ближним.
     * Грань: 0 верх, 1 низ, 2 север, 3 юг, 4 запад, 5 восток, 6–7 диагонали. Для отладки и тестов.
     */
    public List<int[]> drawOrder() {
        ensureProjection();
        List<int[]> out = new ArrayList<>(drawCount);
        for (int k = 0; k < drawCount; k++) {
            int i = order[k];
            out.add(new int[]{fx[i], fy[i], fz[i], fdir[i]});
        }
        return out;
    }

    /** Закрывает ли блок грани соседей (стекло, листва, вода, огонь, факелы и т. п. — нет). */
    public static boolean occludes(String blockId) {
        return kind(blockId) == OPAQUE;
    }

    private static int kind(String blockId) {
        Integer k = KIND.get(blockId);
        if (k == null) {
            String name = blockId.substring(blockId.indexOf(':') + 1);
            // полные блоки с «прозрачными» словами в имени (red_mushroom_block, bamboo_planks) — непрозрачные
            boolean solid = name.endsWith("_block") && !name.equals("slime_block") && !name.equals("honey_block")
                    || name.endsWith("_planks");
            if (!solid && (name.equals("grass") || matches(name, CROSS))) {
                k = CROSSED;
            } else if (!solid && (name.equals("ice") || name.equals("frosted_ice") || name.equals("snow")
                    || name.equals("air") || matches(name, SEE_THROUGH))) {
                k = CLEAR;
            } else {
                k = OPAQUE;
            }
            KIND.put(blockId, k);
        }
        return k;
    }

    private static boolean matches(String name, String[] patterns) {
        for (String p : patterns) {
            if (name.contains(p)) {
                return true;
            }
        }
        return false;
    }

    private void ensureFaces() {
        if (builtFor == structure && builtRevision == structure.revision() && builtCut == cutAbove) {
            return;
        }
        if (tile > 0f && builtFor != null) {
            invalidateLayout();
        }
        builtFor = structure;
        builtRevision = structure.revision();
        builtCut = cutAbove;
        projected = false;
        faceCount = 0;
        int cut = cutAbove;
        BlockStructure s = structure;
        s.forEach((x, y, z, id) -> {
            if (y > cut) {
                return;
            }
            int k = kind(id);
            if (k == CROSSED) {
                addFace(x, y, z, CROSS_A, id);
                addFace(x, y, z, CROSS_B, id);
                return;
            }
            for (int d = 0; d < 6; d++) {
                int[] n = NORMALS[d];
                int ny = y + n[1];
                String nb = ny > cut ? null : s.get(x + n[0], ny, z + n[2]);
                if (nb != null && (occludes(nb) || nb.equals(id) && kind(nb) != CROSSED)) {
                    continue;
                }
                addFace(x, y, z, d, id);
            }
        });
    }

    private void addFace(int x, int y, int z, int dir, String id) {
        int i = faceCount;
        if (i == fx.length) {
            int cap = Math.max(64, i * 2);
            fx = Arrays.copyOf(fx, cap);
            fy = Arrays.copyOf(fy, cap);
            fz = Arrays.copyOf(fz, cap);
            fdir = Arrays.copyOf(fdir, cap);
            ftex = Arrays.copyOf(ftex, cap);
            fcolor = Arrays.copyOf(fcolor, cap);
            fdepth = Arrays.copyOf(fdepth, cap);
            screen = Arrays.copyOf(screen, cap * 8);
            order = Arrays.copyOf(order, cap);
            sortTmp = Arrays.copyOf(sortTmp, cap);
        }
        fx[i] = x;
        fy[i] = y;
        fz[i] = z;
        fdir[i] = (byte) dir;
        BlockTextures.Face tex = faceTexture(id, dir, x, y, z);
        ftex[i] = tex;
        fcolor[i] = shade(tex != null ? tex.tint() : fallbackColor(id, dir), SHADE[dir]);
        faceCount++;
    }

    private BlockTextures.Face faceTexture(String id, int dir, int x, int y, int z) {
        if (dir == UP) {
            return BlockTextures.top(id);
        }
        if (dir == DOWN) {
            return BlockTextures.bottom(id);
        }
        // Нижняя половина двери — своя текстура (oak_door_bottom), верхняя — oak_door_top.
        if (id.endsWith("_door") && id.equals(structure.get(x, y + 1, z))) {
            return BlockTextures.bottom(id);
        }
        return BlockTextures.side(id);
    }

    /** Цвет грани без текстуры: известный для частых блоков, иначе — из хэша id. */
    static int fallbackColor(String id, int dir) {
        String name = id.substring(id.indexOf(':') + 1);
        if (dir == UP && name.equals("grass_block")) {
            return 0xFF7FB238;
        }
        Integer c = COLORS.get(name);
        if (c != null) {
            return c;
        }
        int h = id.hashCode() * 0x9E3779B1;
        int base = Color.hsv((h >>> 8) % 360, 0.35f + ((h >>> 4) & 15) / 60f, 0.6f + (h & 15) / 60f);
        return kind(id) == OPAQUE ? base : Color.withAlpha(base, 0xB0);
    }

    /** Умножить RGB на яркость, альфа без изменений. */
    private static int shade(int argb, float f) {
        if (f >= 1f) {
            return argb;
        }
        return argb & 0xFF000000 | (int) (Color.red(argb) * f) << 16 | (int) (Color.green(argb) * f) << 8
                | (int) (Color.blue(argb) * f);
    }

    // ---------------------------------------------------------------- проекция

    private float fitScale(float w, float h) {
        BlockStructure.Bounds b = structure.bounds();
        if (b == null) {
            return 1f;
        }
        // Вписываем описанную сферу, а не текущий силуэт: иначе модель при вращении то растёт, то сжимается.
        float r = radius(b);
        float s = Math.min(w, h) * 0.86f / (2f * r);
        if (perspective > 0f) {
            float d = cameraDistance(r);
            s *= (d - r) / d;
        }
        return Math.max(0.01f, s);
    }

    private static float radius(BlockStructure.Bounds b) {
        return Math.max(0.5f, 0.5f * (float) Math.sqrt(
                b.sizeX() * b.sizeX() + b.sizeY() * b.sizeY() + b.sizeZ() * b.sizeZ()));
    }

    /** Расстояние от центра модели до камеры в блоках: чем сильнее перспектива, тем ближе. */
    private float cameraDistance(float radius) {
        return radius * (1f + 1.5f / perspective);
    }

    private float viewWidth() {
        return width() > 0f ? width() : 160f;
    }

    private float viewHeight() {
        return height() > 0f ? height() : 160f;
    }

    private void ensureProjection() {
        ensureFaces();
        float w = viewWidth();
        float h = viewHeight();
        float scale = (tile > 0f ? tile : fitScale(w, h)) * zoom;
        if (projected && pYaw == yaw && pPitch == pitch && pScale == scale && pPersp == perspective
                && pW == w && pH == h) {
            return;
        }
        projected = true;
        pYaw = yaw;
        pPitch = pitch;
        pScale = scale;
        pPersp = perspective;
        pW = w;
        pH = h;
        drawCount = 0;
        BlockStructure.Bounds b = structure.bounds();
        if (b == null) {
            return;
        }
        float cy = (float) Math.cos(Math.toRadians(yaw));
        float sy = (float) Math.sin(Math.toRadians(yaw));
        float cp = (float) Math.cos(Math.toRadians(pitch));
        float sp = (float) Math.sin(Math.toRadians(pitch));
        // экран = O + x·ex + y·ey + z·ez; глубина (к зрителю) = (x, y, z)·dir
        float exX = scale * cy;
        float exY = scale * sy * sp;
        float eyY = -scale * cp;
        float ezX = -scale * sy;
        float ezY = scale * cy * sp;
        float dX = sy * cp;
        float dY = sp;
        float dZ = cy * cp;
        float cx = (b.minX() + b.maxX() + 1) * 0.5f;
        float cyy = (b.minY() + b.maxY() + 1) * 0.5f;
        float cz = (b.minZ() + b.maxZ() + 1) * 0.5f;
        float ox = w * 0.5f;
        float oy = h * 0.5f;
        boolean persp = perspective > 0f;
        float camD = persp ? cameraDistance(radius(b)) : 0f;
        for (int i = 0; i < faceCount; i++) {
            int dir = fdir[i];
            float bx = fx[i] - cx;
            float by = fy[i] - cyy;
            float bz = fz[i] - cz;
            // центр грани (у «креста» — центр блока)
            float mx = bx + 0.5f;
            float my = by + 0.5f;
            float mz = bz + 0.5f;
            if (dir < 6) {
                int[] n = NORMALS[dir];
                mx += n[0] * 0.5f;
                my += n[1] * 0.5f;
                mz += n[2] * 0.5f;
                // грань видна, если смотрит на камеру: при перспективе — на её точку, а не только по направлению
                float vx = persp ? dX * camD - mx : dX;
                float vy = persp ? dY * camD - my : dY;
                float vz = persp ? dZ * camD - mz : dZ;
                if (n[0] * vx + n[1] * vy + n[2] * vz <= 1e-4f) {
                    continue;
                }
            }
            // Глубина по центру грани, а не блока: иначе верх соседнего блока мог лечь поверх ближней стены.
            if (persp) {
                float ex = dX * camD - mx;
                float ey = dY * camD - my;
                float ez = dZ * camD - mz;
                fdepth[i] = -(float) Math.sqrt(ex * ex + ey * ey + ez * ez);
            } else {
                fdepth[i] = mx * dX + my * dY + mz * dZ;
            }
            order[drawCount++] = i;
            float[] c = CORNERS[dir];
            int o = i * 8;
            for (int k = 0; k < 4; k++) {
                float px = bx + c[k * 3];
                float py = by + c[k * 3 + 1];
                float pz = bz + c[k * 3 + 2];
                float sx = px * exX + pz * ezX;
                float sy2 = px * exY + py * eyY + pz * ezY;
                if (persp) {
                    float f = camD / Math.max(0.05f, camD - (px * dX + py * dY + pz * dZ));
                    sx *= f;
                    sy2 *= f;
                }
                screen[o + k * 2] = ox + sx;
                screen[o + k * 2 + 1] = oy + sy2;
            }
        }
        sortByDepth();
        // тень: эллипс под центром основания
        float rh = 0.5f * (float) Math.sqrt(b.sizeX() * b.sizeX() + b.sizeZ() * b.sizeZ());
        shadowX = ox;
        shadowY = oy + (b.minY() - cyy) * eyY;
        shadowR = rh * scale * 1.1f;
    }

    /** Устойчивая сортировка слиянием индексов граней по глубине: дальние (меньшая глубина) первыми. */
    private void sortByDepth() {
        int n = drawCount;
        int[] a = order;
        int[] t = sortTmp;
        for (int width = 1; width < n; width *= 2) {
            for (int lo = 0; lo < n - width; lo += 2 * width) {
                int mid = lo + width;
                int hi = Math.min(lo + 2 * width, n);
                if (fdepth[a[mid - 1]] <= fdepth[a[mid]]) {
                    continue;
                }
                int i = lo;
                int j = mid;
                int k = lo;
                while (i < mid && j < hi) {
                    t[k++] = fdepth[a[j]] < fdepth[a[i]] ? a[j++] : a[i++];
                }
                while (i < mid) {
                    t[k++] = a[i++];
                }
                while (j < hi) {
                    t[k++] = a[j++];
                }
                System.arraycopy(t, lo, a, lo, hi - lo);
            }
        }
    }

    // ---------------------------------------------------------------- ввод

    @Override
    protected void handleEvent(UiEvent event) {
        if (!interactive) {
            return;
        }
        if (event instanceof PointerEvent p) {
            if (event.type() == PointerEvent.PRESS && p.button() == Keys.MOUSE_LEFT) {
                dragging = true;
                lastX = p.x();
                lastY = p.y();
                yawVelocity = 0f;
                dragAccum = 0f;
                event.consume();
            } else if (event.type() == PointerEvent.DRAG && dragging) {
                float dx = p.x() - lastX;
                float dy = p.y() - lastY;
                lastX = p.x();
                lastY = p.y();
                yaw = wrap(yaw - dx * DRAG_SENSITIVITY);
                pitch = Math.max(-89f, Math.min(89f, pitch + dy * DRAG_SENSITIVITY));
                dragAccum -= dx * DRAG_SENSITIVITY;
                event.consume();
            } else if (event.type() == PointerEvent.RELEASE) {
                dragging = false;
            }
        } else if (event instanceof ScrollEvent s && zoomable && s.deltaY() != 0f) {
            zoomTarget = clampZoom(zoomTarget * (float) Math.pow(1.15, s.deltaY()));
            event.consume();
            event.stopPropagation();
        }
    }

    private static float clampZoom(float z) {
        return Math.max(0.3f, Math.min(5f, z));
    }

    private static float wrap(float deg) {
        float d = deg % 360f;
        return d < 0f ? d + 360f : d;
    }

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
        if (dragging) {
            if (dt > 0f) {
                yawVelocity += (dragAccum / dt - yawVelocity) * 0.5f;
            }
            dragAccum = 0f;
        } else {
            if (yawVelocity != 0f) {
                // инерция: скорость затухает экспоненциально
                yaw = wrap(yaw + yawVelocity * dt);
                yawVelocity *= (float) Math.exp(-3.5 * dt);
                if (Math.abs(yawVelocity) < 1f) {
                    yawVelocity = 0f;
                }
            }
            if (spin != 0f) {
                yaw = wrap(yaw + spin * dt);
            }
        }
        if (zoom != zoomTarget) {
            zoom += (zoomTarget - zoom) * Math.min(1f, dt * 12f);
            if (Math.abs(zoom - zoomTarget) < 1e-3f) {
                zoom = zoomTarget;
            }
        }
        return alive;
    }

    // ---------------------------------------------------------------- рисование

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        BlockStructure.Bounds b = structure.bounds();
        if (tile <= 0f || b == null) {
            setContentSize(160f, 160f);
            return;
        }
        float sp = Math.abs((float) Math.sin(Math.toRadians(pitch)));
        float cp = (float) Math.cos(Math.toRadians(pitch));
        float rh = 0.5f * (float) Math.sqrt(b.sizeX() * b.sizeX() + b.sizeZ() * b.sizeZ());
        float hh = 0.5f * b.sizeY();
        setContentSize((float) Math.ceil(2f * rh * tile / 0.86f), (float) Math.ceil(2f * (rh * sp + hh * cp) * tile / 0.86f));
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        ensureProjection();
        if (drawCount == 0) {
            return;
        }
        canvas.save();
        canvas.clipRect(0f, 0f, width(), height());
        if (shadow && pitch > 5f) {
            // при взгляде сверху тень бледнеет: она почти целиком под моделью
            float r = shadowR;
            float ry = r * (float) Math.sin(Math.toRadians(pitch));
            float fade = Math.min(1f, 1.4f * (float) Math.cos(Math.toRadians(pitch)));
            canvas.save();
            canvas.multiplyOpacity(fade);
            canvas.fillRoundRect(shadowX - r, shadowY - ry, 2f * r, 2f * ry, Radii.ZERO, SHADOW);
            canvas.restore();
        }
        float[] s = screen;
        for (int k = 0; k < drawCount; k++) {
            int i = order[k];
            int o = i * 8;
            drawFace(canvas, ftex[i], fcolor[i], time, s[o], s[o + 1], s[o + 2], s[o + 3], s[o + 4], s[o + 5], s[o + 6], s[o + 7]);
        }
        canvas.restore();
    }

    private static void drawFace(Canvas canvas, BlockTextures.Face tex, int color, float time,
                                 float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3) {
        if (tex != null) {
            tex.sheet().drawQuad(canvas, tex.sheet().frameAt(time), x0, y0, x1, y1, x2, y2, x3, y3, color);
        } else {
            canvas.fillQuad(x0, y0, x1, y1, x2, y2, x3, y3, color, color, color, color);
        }
    }

    /**
     * Один блок в изометрии (верх, юг слева, восток справа), вписанный в квадрат (x, y, size):
     * для палитр и списков. Цветы и огонь — «крестом».
     */
    public static void drawBlockIcon(Canvas canvas, String blockId, float x, float y, float size, float time) {
        float s = size / (ICON_CP + 2f * ICON_C * ICON_SP);
        // проекция при yaw = 45°, pitch = 30°; центр куба — в центре квадрата
        float exX = s * ICON_C;
        float exY = s * ICON_C * ICON_SP;
        float eyY = -s * ICON_CP;
        float ezX = -s * ICON_C;
        float ezY = s * ICON_C * ICON_SP;
        float ox = x + size * 0.5f - 0.5f * (exX + ezX);
        float oy = y + size * 0.5f - 0.5f * (exY + eyY + ezY);
        int[] dirs = kind(blockId) == CROSSED ? ICON_CROSS : ICON_CUBE;
        for (int dir : dirs) {
            float[] c = CORNERS[dir];
            BlockTextures.Face tex = dir == UP ? BlockTextures.top(blockId) : BlockTextures.side(blockId);
            int color = shade(tex != null ? tex.tint() : fallbackColor(blockId, dir), SHADE[dir]);
            drawFace(canvas, tex, color, time,
                    ox + c[0] * exX + c[2] * ezX, oy + c[0] * exY + c[1] * eyY + c[2] * ezY,
                    ox + c[3] * exX + c[5] * ezX, oy + c[3] * exY + c[4] * eyY + c[5] * ezY,
                    ox + c[6] * exX + c[8] * ezX, oy + c[6] * exY + c[7] * eyY + c[8] * ezY,
                    ox + c[9] * exX + c[11] * ezX, oy + c[9] * exY + c[10] * eyY + c[11] * ezY);
        }
    }
}
