package io.github.ssbaxys.lumina.map;

/**
 * Снимок карты вокруг игрока: квадрат {@code size × size} блоков с левым верхним (северо-западным) углом
 * в {@code (originX, originZ)} и положение игрока. Цвета ARGB по строкам: {@code colors[(z - originZ) * size +
 * (x - originX)]}; прозрачный цвет (альфа 0) — блок неизвестен (чанк не загружен).
 *
 * <p>Массив может принадлежать источнику и обновляться им между кадрами: менять его нельзя, а признак
 * изменения — {@link #version()}.</p>
 *
 * @param yaw поворот игрока в градусах, как в Minecraft: 0 — на юг (+Z), 90 — на запад, 180 — на север
 */
public record MapSample(int originX, int originZ, int size, int[] colors, long version,
                        double playerX, double playerZ, float yaw) {
    /** Цвет блока или 0, если он вне снимка или неизвестен. */
    public int colorAt(int x, int z) {
        int i = x - originX;
        int j = z - originZ;
        if (i < 0 || j < 0 || i >= size || j >= size) {
            return 0;
        }
        return colors[j * size + i];
    }

    /** Тот же снимок с другим положением игрока (цвета общие). */
    public MapSample withPlayer(double x, double z, float yaw) {
        return new MapSample(originX, originZ, size, colors, version, x, z, yaw);
    }
}
