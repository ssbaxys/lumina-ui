package io.github.ssbaxys.lumina.blocks;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.map.DemoMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Выдуманный мир из блоков, когда настоящего нет (главное меню, витрина, тесты): рельеф {@link DemoMap}
 * (те же горы и океаны, что на демо-карте), трава, песок, вода до уровня моря, камень с рудой, деревья
 * и цветы. Игрок гуляет по кругу по равнине.
 *
 * <pre>{@code
 * new StructurePreview().fallback(new DemoWorld(42));   // в мире — мир, без него — демо
 * }</pre>
 */
public final class DemoWorld implements WorldBlocks {
    /** Уровень моря: вода до этой высоты включительно. */
    public static final int SEA_LEVEL = 62;

    private static final String GRASS = "minecraft:grass_block";
    private static final String DIRT = "minecraft:dirt";
    private static final String STONE = "minecraft:stone";
    private static final String SAND = "minecraft:sand";
    private static final String GRAVEL = "minecraft:gravel";
    private static final String SNOW = "minecraft:snow_block";
    private static final String WATER = "minecraft:water";
    private static final String LOG = "minecraft:oak_log";
    private static final String LEAVES = "minecraft:oak_leaves";

    private final long seed;
    private final DemoMap terrain;
    private final Map<Long, Integer> heights = new HashMap<>();
    private final int homeX;
    private final int homeZ;
    private boolean walking = true;
    private float walkRadius = 10f;
    private float walkSpeed = 0.012f;
    private double fixedX;
    private double fixedZ;
    private float fixedYaw;

    public DemoWorld(long seed) {
        this.seed = seed;
        this.terrain = new DemoMap(seed);
        int[] home = findHome();
        this.homeX = home[0];
        this.homeZ = home[1];
    }

    /** Игрок гуляет по кругу радиусом {@code radius} блоков вокруг дома со скоростью {@code turnsPerSecond}. */
    public DemoWorld walk(float radius, float turnsPerSecond) {
        this.walking = true;
        this.walkRadius = radius;
        this.walkSpeed = turnsPerSecond;
        return this;
    }

    /** Поставить игрока в точку (прогулка выключается). */
    public DemoWorld player(double x, double z, float yaw) {
        this.walking = false;
        this.fixedX = x;
        this.fixedZ = z;
        this.fixedYaw = yaw;
        return this;
    }

    /** Точка на равнине, вокруг которой гуляет игрок: {@code {x, z}}. */
    public int[] home() {
        return new int[]{homeX, homeZ};
    }

    // ---------------------------------------------------------------- рельеф

    /** Высота верхнего твёрдого блока столбца. */
    public int height(int x, int z) {
        long key = (long) x << 32 | (z & 0xFFFFFFFFL);
        Integer h = heights.get(key);
        if (h == null) {
            if (heights.size() > 65_536) {
                heights.clear();
            }
            h = terrain.height(x, z);
            heights.put(key, h);
        }
        return h;
    }

    @Override
    public String blockAt(int x, int y, int z) {
        int h = height(x, z);
        if (y <= h) {
            return ground(x, y, z, h);
        }
        if (y <= SEA_LEVEL) {
            return WATER;
        }
        if (y > h + 7) {
            return null;
        }
        if (isTree(x, z, h) && y <= h + 4) {
            return LOG;
        }
        if (y >= h + 1 && leaves(x, y, z)) {
            return LEAVES;
        }
        if (y == h + 1 && h > SEA_LEVEL + 1 && h <= 78) {
            double r = hash(x, z, seed + 5);
            if (r < 0.10) {
                return "minecraft:short_grass";
            }
            if (r < 0.12) {
                return "minecraft:poppy";
            }
            if (r < 0.135) {
                return "minecraft:dandelion";
            }
        }
        return null;
    }

    private String ground(int x, int y, int z, int h) {
        String top;
        String under;
        if (h > 88) {
            top = SNOW;
            under = STONE;
        } else if (h > 78) {
            top = STONE;
            under = STONE;
        } else if (h <= SEA_LEVEL + 1) {
            top = h < SEA_LEVEL - 4 ? GRAVEL : SAND;
            under = top;
        } else {
            top = GRASS;
            under = DIRT;
        }
        if (y == h) {
            return top;
        }
        if (y >= h - 3) {
            return under;
        }
        double r = hash(x * 31 + y, z * 17 - y, seed + 9);
        if (r < 0.004 && y < 40) {
            return "minecraft:diamond_ore";
        }
        if (r < 0.02) {
            return "minecraft:iron_ore";
        }
        if (r < 0.045) {
            return "minecraft:coal_ore";
        }
        return STONE;
    }

    /** В столбце растёт дерево: трава, редкий хэш и соседи не слишком близко. */
    private boolean isTree(int x, int z, int h) {
        return h > SEA_LEVEL + 1 && h <= 78 && hash(x, z, seed + 3) < 0.03 && Math.floorMod(x + 2 * z, 3) == 0;
    }

    /** Листва дуба: широкие слои над 3-м и 4-м блоком ствола, узкий над 5-м и «крест» сверху. */
    private boolean leaves(int x, int y, int z) {
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                int tx = x + dx;
                int tz = z + dz;
                int th = height(tx, tz);
                int dy = y - th;
                if (dy < 3 || dy > 6 || !isTree(tx, tz, th)) {
                    continue;
                }
                if (dx == 0 && dz == 0 && dy <= 4) {
                    continue;
                }
                int ax = Math.abs(dx);
                int az = Math.abs(dz);
                boolean in = switch (dy) {
                    case 3, 4 -> !(ax == 2 && az == 2);
                    case 5 -> ax <= 1 && az <= 1;
                    default -> ax + az <= 1;
                };
                if (in) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------- игрок

    private double[] position() {
        if (!walking) {
            return new double[]{fixedX, fixedZ, fixedYaw};
        }
        long now = Lumina.isInitialized() ? Lumina.platform().nanoTime() : System.nanoTime();
        double a = (now / 1_000_000_000.0) * walkSpeed * Math.PI * 2;
        double x = homeX + 0.5 + Math.cos(a) * walkRadius;
        double z = homeZ + 0.5 + Math.sin(a) * walkRadius;
        // идёт по касательной (-sin a, cos a); у Minecraft направление взгляда — (-sin yaw, cos yaw)
        float yaw = (float) Math.toDegrees(a);
        return new double[]{x, z, walkSpeed < 0 ? yaw + 180f : yaw};
    }

    @Override
    public double playerX() {
        return position()[0];
    }

    @Override
    public double playerY() {
        double[] p = position();
        int bx = (int) Math.floor(p[0]);
        int bz = (int) Math.floor(p[1]);
        return Math.max(height(bx, bz), SEA_LEVEL) + 1;
    }

    @Override
    public double playerZ() {
        return position()[1];
    }

    @Override
    public float playerYaw() {
        return (float) position()[2];
    }

    /** Ближайшая к началу координат суша невысоко над морем, у которой суша и по сторонам. */
    private int[] findHome() {
        for (int ring = 0; ring <= 64; ring++) {
            for (int j = -ring; j <= ring; j++) {
                for (int i = -ring; i <= ring; i++) {
                    if (Math.max(Math.abs(i), Math.abs(j)) != ring) {
                        continue;
                    }
                    int x = i * 8;
                    int z = j * 8;
                    int h = terrain.height(x, z);
                    if (h >= SEA_LEVEL + 2 && h <= 74 && dry(x + 12, z) && dry(x - 12, z) && dry(x, z + 12)
                            && dry(x, z - 12)) {
                        return new int[]{x, z};
                    }
                }
            }
        }
        return new int[]{0, 0};
    }

    private boolean dry(int x, int z) {
        int h = terrain.height(x, z);
        return h > SEA_LEVEL && h <= 80;
    }

    private static double hash(int x, int z, long seed) {
        long h = seed * 0x9E3779B97F4A7C15L + x * 0xC2B2AE3D27D4EB4FL + z * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 11) / (double) (1L << 53);
    }
}
