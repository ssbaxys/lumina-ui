package io.github.ssbaxys.lumina.forge;

import io.github.ssbaxys.lumina.blocks.WorldBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Блоки клиентского мира для снимков ядра ({@code WorldCapture}, {@code StructurePreview}): id блока по
 * {@link BuiltInRegistries#BLOCK}, строка кэшируется на каждый {@link Block}. Незагруженный чанк —
 * {@link WorldBlocks#UNLOADED}, воздух и всё вне высоты мира — {@code null}. Один объект на мир:
 * {@link #current()} создаёт новый при смене мира или измерения.
 */
final class McWorldBlocks implements WorldBlocks {
    private static McWorldBlocks current;

    private final ClientLevel level;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private final Map<Block, String> ids = new IdentityHashMap<>();

    private McWorldBlocks(ClientLevel level) {
        this.level = level;
    }

    /** Блоки текущего мира или {@code null}, если мира (или игрока) нет. */
    static McWorldBlocks current() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || mc.player == null) {
            current = null;
            return null;
        }
        if (current == null || current.level != level) {
            current = new McWorldBlocks(level);
        }
        return current;
    }

    @Override
    public String blockAt(int x, int y, int z) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
            return null;
        }
        if (!level.hasChunk(x >> 4, z >> 4)) {
            return UNLOADED;
        }
        pos.set(x, y, z);
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return null;
        }
        Block block = state.getBlock();
        String id = ids.get(block);
        if (id == null) {
            id = BuiltInRegistries.BLOCK.getKey(block).toString();
            ids.put(block, id);
        }
        return id;
    }

    private static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    @Override
    public boolean hasPlayer() {
        return player() != null;
    }

    @Override
    public double playerX() {
        LocalPlayer p = player();
        return p == null ? 0 : p.getX();
    }

    @Override
    public double playerY() {
        LocalPlayer p = player();
        return p == null ? 0 : p.getY();
    }

    @Override
    public double playerZ() {
        LocalPlayer p = player();
        return p == null ? 0 : p.getZ();
    }

    @Override
    public float playerYaw() {
        LocalPlayer p = player();
        return p == null ? 0f : p.getYRot();
    }
}
