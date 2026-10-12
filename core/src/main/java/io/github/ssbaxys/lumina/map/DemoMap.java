package io.github.ssbaxys.lumina.map;

import io.github.ssbaxys.lumina.Lumina;

/**
 * Выдуманный мир для карт, когда настоящего нет (главное меню, витрина, тесты): океаны, пляжи, равнины,
 * леса и заснеженные горы из шума, игрок гуляет по кругу. Это и {@link MapSource} для {@code MiniMap},
 * и {@link MapColorProvider} для {@code WorldMap}.
 *
 * <pre>{@code
 * new MiniMap().source(MapSource.platform().or(new DemoMap(42)));
 * }</pre>
 */
public final class DemoMap implements MapSource, MapColorProvider {
    private static final int DEEP = 0xFF2B4A9C;
    private static final int WATER = 0xFF3F6FD8;
    private static final int SAND = 0xFFE3D39A;
    private static final int GRASS = 0xFF74AE4C;
    private static final int FOREST = 0xFF3C7A2A;
    private static final int STONE = 0xFF8C8C8C;
    private static final int SNOW = 0xFFF2F6FA;

    private final long seed;
    private final MapGridSampler sampler;
    private boolean walking = true;
    private double playerX;
    private double playerZ;
    private float yaw;
    private float walkRadius = 48f;
    private float walkSpeed = 0.12f;

    public DemoMap(long seed) {
        this.seed = seed;
        this.sampler = new MapGridSampler((x, z, h) -> {
            h[0] = height(x, z);
            return baseColor(x, z);
        }).budget(4096).interval(0f);
    }

    /** Игрок гуляет по кругу радиусом {@code radius} блоков со скоростью {@code turnsPerSecond}. */
    public DemoMap walk(float radius, float turnsPerSecond) {
        this.walking = true;
        this.walkRadius = radius;
        this.walkSpeed = turnsPerSecond;
        return this;
    }

    /** Поставить игрока в точку (прогулка выключается). */
    public DemoMap player(double x, double z, float yaw) {
        this.walking = false;
        this.playerX = x;
        this.playerZ = z;
        this.yaw = yaw;
        return this;
    }

    @Override
    public MapSample sample(int radius) {
        if (walking) {
            long now = Lumina.isInitialized() ? Lumina.platform().nanoTime() : System.nanoTime();
            double a = (now / 1_000_000_000.0) * walkSpeed * Math.PI * 2;
            playerX = Math.cos(a) * walkRadius;
            playerZ = Math.sin(a) * walkRadius;
            // Движение по касательной: (-sin a, cos a); yaw Minecraft: направление (-sin yaw, cos yaw).
            yaw = (float) Math.toDegrees(Math.atan2(Math.sin(a), Math.cos(a)));
        }
        return sampler.sample(playerX, playerZ, yaw, radius, 0L);
    }

    @Override
    public int colorAt(int x, int z) {
        int base = baseColor(x, z);
        int h = height(x, z);
        int north = height(x, z - 1);
        return MapGridSampler.shade(base, h == north ? 0.86f : h > north ? 1f : 0.71f);
    }

    /** Высота рельефа в блоках (62 — уровень моря). */
    public int height(int x, int z) {
        double n = fbm(x / 96.0, z / 96.0);
        return (int) Math.round(40 + n * 60);
    }

    private int baseColor(int x, int z) {
        int h = height(x, z);
        if (h < 54) {
            return DEEP;
        }
        if (h < 62) {
            return WATER;
        }
        if (h < 64) {
            return SAND;
        }
        if (h > 88) {
            return SNOW;
        }
        if (h > 78) {
            return STONE;
        }
        return noise(x / 14.0, z / 14.0, seed + 17) > 0.55 ? FOREST : GRASS;
    }

    // ---------------------------------------------------------------- шум

    private double fbm(double x, double z) {
        double sum = 0;
        double amp = 0.5;
        double norm = 0;
        for (int o = 0; o < 4; o++) {
            sum += noise(x, z, seed + o * 31L) * amp;
            norm += amp;
            amp *= 0.5;
            x *= 2.03;
            z *= 2.03;
        }
        return sum / norm;
    }

    /** Сглаженный шум значений 0..1. */
    private static double noise(double x, double z, long seed) {
        int x0 = (int) Math.floor(x);
        int z0 = (int) Math.floor(z);
        double fx = x - x0;
        double fz = z - z0;
        double sx = fx * fx * (3 - 2 * fx);
        double sz = fz * fz * (3 - 2 * fz);
        double a = hash(x0, z0, seed);
        double b = hash(x0 + 1, z0, seed);
        double c = hash(x0, z0 + 1, seed);
        double d = hash(x0 + 1, z0 + 1, seed);
        return (a + (b - a) * sx) + ((c + (d - c) * sx) - (a + (b - a) * sx)) * sz;
    }

    private static double hash(int x, int z, long seed) {
        long h = seed * 0x9E3779B97F4A7C15L + x * 0xC2B2AE3D27D4EB4FL + z * 0x165667B19E3779F9L;
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (h >>> 11) / (double) (1L << 53);
    }
}
