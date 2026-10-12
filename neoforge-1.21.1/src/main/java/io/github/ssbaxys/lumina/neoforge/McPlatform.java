package io.github.ssbaxys.lumina.neoforge;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.game.GameState;
import io.github.ssbaxys.lumina.hud.HudVisibility;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.Images;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Optional;

final class McPlatform implements Platform {
    private final TextMetrics metrics = new TextMetrics() {
        @Override
        public float width(String text) {
            return Minecraft.getInstance().font.width(text);
        }

        @Override
        public float lineHeight() {
            return Minecraft.getInstance().font.lineHeight;
        }
    };

    /** Строковые id предметов, биомов и измерений: считаются один раз, а не на каждом тике. */
    private final Map<Item, String> itemIds = new IdentityHashMap<>();
    private ResourceKey<?> biomeKey;
    private String biomeName = "";
    private ResourceKey<?> dimensionKey;
    private String dimensionName = "";

    @Override
    public TextMetrics textMetrics() {
        return metrics;
    }

    @Override
    public String getClipboard() {
        return Minecraft.getInstance().keyboardHandler.getClipboard();
    }

    @Override
    public void setClipboard(String text) {
        Minecraft.getInstance().keyboardHandler.setClipboard(text);
    }

    @Override
    public void playClickSound() {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    @Override
    public void playSound(String soundId, float volume, float pitch) {
        ResourceLocation id = ResourceLocation.tryParse(soundId);
        SoundEvent sound = id == null ? null : BuiltInRegistries.SOUND_EVENT.get(id);
        if (sound != null) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, volume));
        }
    }

    /** Время суток текущего мира (0..24000) для расписания тем; без мира — -1 (ядро возьмёт системные часы). */
    @Override
    public float dayTime() {
        var level = Minecraft.getInstance().level;
        return level == null ? -1f : (float) (level.getDayTime() % 24000L);
    }

    /** Биом под игроком ("minecraft:plains") или null, если мира нет. */
    @Override
    public void narrate(String text) {
        if (text != null && !text.isBlank()) {
            Minecraft.getInstance().getNarrator().sayNow(Component.literal(text));
        }
    }

    @Override
    public String biomeId() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return null;
        }
        return mc.level.getBiome(mc.player.blockPosition()).unwrapKey().map(k -> k.location().toString()).orElse(null);
    }

    /** Через менеджер ресурсов игры: учитываются ресурспаки и их перезагрузка (F3+T). */
    @Override
    public byte[] readResourceBytes(String resourceId) {
        ResourceLocation id = ResourceLocation.tryParse(resourceId);
        if (id == null) {
            return null;
        }
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            return Platform.super.readResourceBytes(resourceId);
        }
        try (InputStream in = resource.get().open()) {
            return in.readAllBytes();
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * Пиксели → {@link NativeImage} → {@link DynamicTexture} под {@code luminaui:dynamic/<ключ>}. Регистрация
     * в менеджере текстур — на потоке отрисовки; рисуется по идентификатору, как любой ресурс.
     */
    @Override
    public TextureRef createTexture(String key, int width, int height, int[] argb) {
        if (width <= 0 || height <= 0 || argb == null || argb.length < width * height) {
            return null;
        }
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(Lumina.MOD_ID, "dynamic/" + Images.safeKey(key));
        NativeImage image = new NativeImage(NativeImage.Format.RGBA, width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int c = argb[y * width + x];
                // NativeImage хранит ABGR: меняем местами красный и синий канал.
                image.setPixelRGBA(x, y, (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16));
            }
        }
        if (RenderSystem.isOnRenderThread()) {
            registerTexture(id, image);
        } else {
            RenderSystem.recordRenderCall(() -> registerTexture(id, image));
        }
        return TextureRef.of(id.toString());
    }

    /** Старая текстура с тем же ключом закрывается менеджером текстур. */
    private static void registerTexture(ResourceLocation id, NativeImage image) {
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
    }

    // ================================================================ HUD и данные игры

    /** Флаги для правил видимости HUD: F1, экран отладки, чат, любой экран. Каждый кадр, без выделений. */
    @Override
    public int hudState() {
        Minecraft mc = Minecraft.getInstance();
        int state = 0;
        if (mc.options.hideGui) {
            state |= HudVisibility.GUI_HIDDEN;
        }
        if (mc.getDebugOverlay().showDebugScreen()) {
            state |= HudVisibility.DEBUG_SCREEN;
        }
        if (mc.screen != null) {
            state |= HudVisibility.SCREEN_OPEN;
            if (mc.screen instanceof ChatScreen) {
                state |= HudVisibility.CHAT_OPEN;
            }
        }
        return state;
    }

    /** Снимок игрока для {@code GameData} на каждом тике клиента. */
    @Override
    public boolean pollGameData(GameState out) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null) {
            return false;
        }
        out.health = player.getHealth();
        out.maxHealth = player.getMaxHealth();
        out.absorption = player.getAbsorptionAmount();
        FoodData food = player.getFoodData();
        out.food = food.getFoodLevel();
        out.saturation = food.getSaturationLevel();
        out.armor = player.getArmorValue();
        out.air = player.getAirSupply();
        out.maxAir = player.getMaxAirSupply();
        out.xpLevel = player.experienceLevel;
        out.xpProgress = player.experienceProgress;
        out.x = player.getX();
        out.y = player.getY();
        out.z = player.getZ();
        out.yaw = player.getYRot();
        out.pitch = player.getXRot();
        out.facing = player.getDirection().getName();
        ResourceKey<?> biome = level.getBiome(player.blockPosition()).unwrapKey().orElse(null);
        if (biome != biomeKey) {
            biomeKey = biome;
            biomeName = biome == null ? "" : biome.location().toString();
        }
        out.biome = biomeName;
        ResourceKey<?> dimension = level.dimension();
        if (dimension != dimensionKey) {
            dimensionKey = dimension;
            dimensionName = dimension.location().toString();
        }
        out.dimension = dimensionName;
        out.dayTime = (int) (level.getDayTime() % 24000L);
        out.fps = mc.getFps();
        var inventory = player.getInventory();
        out.selectedSlot = inventory.selected;
        int slots = Math.min(GameState.SLOTS, inventory.getContainerSize());
        out.slotCount = slots;
        for (int i = 0; i < slots; i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) {
                out.setSlot(i, null, 0);
            } else {
                out.setSlot(i, itemId(stack.getItem()), stack.getCount());
            }
        }
        return true;
    }

    private String itemId(Item item) {
        String id = itemIds.get(item);
        if (id == null) {
            id = BuiltInRegistries.ITEM.getKey(item).toString();
            itemIds.put(item, id);
        }
        return id;
    }
}
