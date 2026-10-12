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
 * Как выглядят грани блока: текстура из ресурсов игры (с учётом ресурспаков), её анимация и оттенок.
 * Имя текстуры подбирается по id блока: {@code minecraft:tnt} → {@code block/tnt_side},
 * {@code minecraft:water} → {@code block/water_still} с синим оттенком и т. д.
 * Верх и низ ({@link #top}, {@link #bottom}) ищутся по суффиксам {@code _top}, {@code _bottom},
 * {@code _end}, а если их нет — берётся вид сбоку.
 * Свой вид для любого блока — {@link #register}, {@link #registerTop}, {@link #registerBottom}.
 */
public final class BlockTextures {
    /** Вид блока сбоку: анимированная (или одна) текстура и оттенок ARGB. */
    public record Face(SpriteSheet sheet, int tint) {
    }

    private static final String[] SUFFIXES = {"", "_side", "_front", "_still", "_0", "_top"};
    private static final String[] TOP_SUFFIXES = {"_top", "_end"};
    private static final String[] BOTTOM_SUFFIXES = {"_bottom", "_end", "_top"};
    /** Низ блока, нарисованный текстурой другого блока (как в моделях игры). */
    private static final Map<String, String> BOTTOM_AS = Map.of(
            "grass_block", "dirt",
            "podzol", "dirt",
            "mycelium", "dirt",
            "dirt_path", "dirt",
            "crafting_table", "oak_planks");
    private static final int GRASS = 0xFF91BD59;
    private static final Pattern FRAMETIME = Pattern.compile("\"frametime\"\\s*:\\s*(\\d+)");
    private static final int FOLIAGE = 0xFF48B518;
    private static final Map<String, Face> CUSTOM = new ConcurrentHashMap<>();
    private static final Map<String, Face> CUSTOM_TOP = new ConcurrentHashMap<>();
    private static final Map<String, Face> CUSTOM_BOTTOM = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Face>> CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Face>> CACHE_TOP = new ConcurrentHashMap<>();
    private static final Map<String, Optional<Face>> CACHE_BOTTOM = new ConcurrentHashMap<>();
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

    /** Свой вид верхней грани (3D-модели). */
    public static void registerTop(String blockId, Face face) {
        CUSTOM_TOP.put(blockId, face);
        CACHE_TOP.remove(blockId);
    }

    /** Свой вид нижней грани (3D-модели). */
    public static void registerBottom(String blockId, Face face) {
        CUSTOM_BOTTOM.put(blockId, face);
        CACHE_BOTTOM.remove(blockId);
    }

    /** Вид блока сбоку или {@code null}, если текстура не нашлась (тогда рисуют иконку предмета). */
    public static Face side(String blockId) {
        Face custom = CUSTOM.get(blockId);
        if (custom != null) {
            return custom;
        }
        return CACHE.computeIfAbsent(blockId, id -> Optional.ofNullable(resolve(id, SUFFIXES))).orElse(null);
    }

    /**
     * Верхняя грань: {@code grass_block_top} (с цветом травы), {@code oak_log_top}, {@code tnt_top}…
     * Без своей текстуры — как сбоку; {@code null}, если не нашлось ничего.
     */
    public static Face top(String blockId) {
        Face custom = CUSTOM_TOP.get(blockId);
        if (custom != null) {
            return custom;
        }
        if (CUSTOM.containsKey(blockId)) {
            return CUSTOM.get(blockId);
        }
        Face f = CACHE_TOP.computeIfAbsent(blockId, id -> Optional.ofNullable(resolve(id, TOP_SUFFIXES))).orElse(null);
        return f != null ? f : side(blockId);
    }

    /** Нижняя грань: {@code tnt_bottom}, торец бревна, земля под травой… Без своей — как сбоку. */
    public static Face bottom(String blockId) {
        Face custom = CUSTOM_BOTTOM.get(blockId);
        if (custom != null) {
            return custom;
        }
        if (CUSTOM.containsKey(blockId)) {
            return CUSTOM.get(blockId);
        }
        Face f = CACHE_BOTTOM.computeIfAbsent(blockId, id -> {
            String alias = BOTTOM_AS.get(id.substring(id.indexOf(':') + 1));
            if (alias != null) {
                return Optional.ofNullable(side(namespace(id) + ":" + alias));
            }
            return Optional.ofNullable(resolve(id, BOTTOM_SUFFIXES));
        }).orElse(null);
        return f != null ? f : side(blockId);
    }

    /** Забыть найденные текстуры (после смены ресурспаков). */
    public static void clearCache() {
        CACHE.clear();
        CACHE_TOP.clear();
        CACHE_BOTTOM.clear();
    }

    private static String namespace(String blockId) {
        int colon = blockId.indexOf(':');
        return colon < 0 ? "minecraft" : blockId.substring(0, colon);
    }

    private static Face resolve(String blockId, String[] suffixes) {
        if (!Lumina.isInitialized()) {
            return null;
        }
        int colon = blockId.indexOf(':');
        String ns = namespace(blockId);
        String name = colon < 0 ? blockId : blockId.substring(colon + 1);
        for (String suffix : suffixes) {
            String id = ns + ":textures/block/" + name + suffix + ".png";
            byte[] png = Lumina.platform().readResourceBytes(id);
            if (png != null) {
                int tint = name.equals("grass_block") && suffix.equals("_top") ? GRASS : tint(ns + ":" + name);
                return new Face(sheet(TextureRef.of(id)), tint);
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
