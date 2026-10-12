package io.github.ssbaxys.lumina.forge;

import io.github.ssbaxys.lumina.map.MapGridSampler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/**
 * Один столбец клиентского мира для {@link MapGridSampler}: цвет верхнего блока по ванильной палитре карт
 * ({@link MapColor}). Незагруженный чанк — 0 (сетка перечитает его позже). В Незере верх — потолок из
 * бедрока, как и у ванильной карты.
 */
final class McMapColumn implements MapGridSampler.Column {
    private final ClientLevel level;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    McMapColumn(ClientLevel level) {
        this.level = level;
    }

    @Override
    public int sample(int x, int z, int[] height) {
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return 0;
        }
        int min = level.getMinBuildHeight();
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
        // бесцветное для карты (стекло, барьер) пропускаем вниз, но недолго
        for (int steps = 0; y >= min && steps < 64; y--, steps++) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            MapColor color = state.getMapColor(level, pos);
            if (color != MapColor.NONE) {
                height[0] = y;
                return 0xFF000000 | color.col;
            }
        }
        height[0] = Math.max(min, y);
        return 0xFF000000;
    }
}
