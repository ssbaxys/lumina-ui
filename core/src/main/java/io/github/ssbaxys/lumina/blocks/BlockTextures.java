package io.github.ssbaxys.lumina.blocks;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.fx.SpriteSheet;
import io.github.ssbaxys.lumina.render.TextureRef;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Как блок выглядит сбоку: текстура из ресурсов игры (с учётом ресурспаков), её анимация и оттенок.
 * Имя текстуры подбирается по id блока: {@code minecraft:tnt} → {@code block/tnt_side},
 * {@code minecraft:water} → {@code block/water_still} с синим оттенком и т. д.
 * Свой вид для любого блока — {@link #register}.
 */
public final class BlockTextures {
    /** Вид блока сбоку: анимированная (или одна) текстура и оттенок ARGB. */
    public record Face(SpriteSheet sheet, int tint) {
    }

    private static final String[] SUFFIXES = {"", "_side", "_front", "_still", "_0", "_top"};
    private static final Pattern FRAMETIME = Pattern.compile("\"frametime\"\\s*:\\s*(\\d+)");
    private static final int FOLIAGE = 0xFF48B518;
    private static final Map<String, Face> CUSTOM = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Face>> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Integer> TINTS = new ConcurrentHashMap<>(Map.of(
            "minecraft:water", 0xC83F76E4,
            "minecraft:birch_leaves", 0xFF80A755,
            "minecraft:spruce_leaves", 0xFF619961));

    private BlockTextures() {
    }

    /** Свой вид блока: {@code register("mymod:crystal", TextureRef.of("mymod:textures/block/crystal.png"), 0xFFFFFFFF)}. */
    public static void register(String blockId, TextureRef texture, int tint) {
        CUSTOM.put(blockId, new Face(sheet(texture), tint));
        CACHE.remove(blockId);
    }

    /** Свой вид блока целиком (например, покадровая анимация из отдельных файлов). */
    public static void register(String blockId, Face face) {
        CUSTOM.put(blockId, face);
        CACHE.remove(blockId);
    }

    /** Вид блока сбоку или {@code null}, если текстура не нашлась (тогда рисуют иконку предмета). */
    public static Face side(String blockId) {
        Face custom = CUSTOM.get(blockId);
        if (custom != null) {
            return custom;
        }
        return CACHE.computeIfAbsent(blockId, id -> Optional.ofNullable(resolve(id))).orElse(null);
    }

    /** Забыть найденные текстуры (после смены ресурспаков). */
    public static void clearCache() {
        CACHE.clear();
    }

    private static Face resolve(String blockId) {
        if (!Lumina.isInitialized()) {
            return null;
        }
        int colon = blockId.indexOf(':');
        String ns = colon < 0 ? "minecraft" : blockId.substring(0, colon);
        String name = colon < 0 ? blockId : blockId.substring(colon + 1);
        for (String suffix : SUFFIXES) {
            String id = ns + ":textures/block/" + name + suffix + ".png";
            byte[] png = Lumina.platform().readResourceBytes(id);
            if (png != null) {
                return new Face(sheet(TextureRef.of(id)), tint(ns + ":" + name));
            }
        }
        return null;
    }

    private static int tint(String blockId) {
        Integer t = TINTS.get(blockId);
        if (t != null) {
            return t;
        }
        String name = blockId.substring(blockId.indexOf(':') + 1);
        // Листва и трава в игре окрашиваются по биому; здесь — цвет равнин.
        if (name.endsWith("_leaves") && !name.startsWith("azalea") && !name.startsWith("flowering")
                && !name.startsWith("cherry") || name.equals("vine") || name.equals("grass") || name.equals("short_grass")
                || name.equals("fern") || name.equals("tall_grass") || name.equals("large_fern")) {
            return FOLIAGE;
        }
        return 0xFFFFFFFF;
    }

    /** Анимированная полоса, если картинка выше, чем шире; скорость — из {@code .png.mcmeta}, как в игре. */
    private static SpriteSheet sheet(TextureRef texture) {
        float fps = 20f;
        if (Lumina.isInitialized()) {
            String meta = Lumina.platform().readResource(texture.id() + ".mcmeta");
            if (meta != null) {
                Matcher m = FRAMETIME.matcher(meta);
                if (m.find()) {
                    fps = 20f / Math.max(1, Integer.parseInt(m.group(1)));
                }
            }
        }
        return SpriteSheet.animated(texture, fps, 1);
    }
}
