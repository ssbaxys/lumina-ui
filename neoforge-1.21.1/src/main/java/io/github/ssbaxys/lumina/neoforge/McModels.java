package io.github.ssbaxys.lumina.neoforge;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.EntityOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * 3D-модели для {@link McCanvas} на 1.21.1: предметы с переливом и поворотом, сущности и игрок тем же путём,
 * что и окно инвентаря ({@link InventoryScreen#renderEntityInInventory}). Всё только на потоке отрисовки.
 *
 * <p>Сущности создаются на клиенте ({@code EntityType.create(level)}) по одной на тип и внешний вид
 * ({@link EntityOptions#appearanceKey()}) и живут, пока не сменится мир; в мир они не добавляются и не тикают.</p>
 */
final class McModels {
    private static final int MAX_ENTITIES = 64;
    /** Без сдвига: ноги сущности — в переданной точке. */
    private static final Vector3f ORIGIN = new Vector3f();
    private static final Map<String, ItemStack> ITEMS = new HashMap<>();
    /** Значение {@code null} — сущность такого типа не создать (неизвестный id или не живая). */
    private static final Map<String, LivingEntity> ENTITIES = new HashMap<>();
    private static ClientLevel entitiesLevel;

    private McModels() {
    }

    /** Стопка из одного предмета по id (с переливом зачарования, если {@code glint}); кэшируется. */
    static ItemStack stack(String itemId, boolean glint) {
        String key = glint ? itemId + "#glint" : itemId;
        ItemStack stack = ITEMS.get(key);
        if (stack == null) {
            ResourceLocation rl = ResourceLocation.tryParse(itemId);
            stack = rl == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.get(rl));
            if (glint && !stack.isEmpty()) {
                // С 1.20.5 перелив задаётся компонентом, без настоящих зачарований.
                stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, Boolean.TRUE);
            }
            ITEMS.put(key, stack);
        }
        return stack;
    }

    /**
     * Предмет в 3D в квадрате (x, y, size): как {@code GuiGraphics.renderItem}, но с поворотом вокруг центра
     * ({@code yaw} — вокруг вертикали экрана, {@code pitch} — наклон к зрителю) и любым размером.
     */
    static void drawItem3d(GuiGraphics graphics, String itemId, boolean glint, float x, float y, float size,
                           float yaw, float pitch) {
        ItemStack stack = stack(itemId, glint);
        if (stack.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        BakedModel model = mc.getItemRenderer().getModel(stack, mc.level, null, 0);
        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(x + size / 2f, y + size / 2f, 150f);
        // Ось Y вверх только для вершин (нормали не трогаем) — так же делает GuiGraphics.renderItem.
        pose.last().pose().scale(1f, -1f, 1f);
        pose.scale(size, size, size);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(-yaw));
        boolean flat = !model.usesBlockLight();
        if (flat) {
            Lighting.setupForFlatItems();
        }
        mc.getItemRenderer().render(stack, ItemDisplayContext.GUI, false, pose, graphics.bufferSource(),
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
        graphics.flush();
        if (flat) {
            Lighting.setupFor3DItems();
        }
        pose.popPose();
    }

    /**
     * Сущность ногами вниз по центру квадрата (x, y, size), вписанная по габаритам. {@link Canvas#PLAYER} —
     * локальный игрок; его поворот на время отрисовки подменяется и возвращается обратно.
     */
    static void drawEntity(GuiGraphics graphics, String typeId, float x, float y, float size, float yaw, float pitch,
                           EntityOptions options) {
        Minecraft mc = Minecraft.getInstance();
        boolean player = Canvas.PLAYER.equals(typeId);
        LivingEntity entity = player ? mc.player : entity(mc.level, typeId, options);
        if (entity == null) {
            return;
        }
        float h = entity.getBbHeight();
        float w = entity.getBbWidth();
        float scale = size * 0.9f / Math.max(0.1f, Math.max(h, w * 1.3f));
        float feetY = y + size / 2f + h * scale / 2f;

        float bodyRotO = entity.yBodyRotO;
        float bodyRot = entity.yBodyRot;
        float yRotO = entity.yRotO;
        float yRot = entity.getYRot();
        float xRotO = entity.xRotO;
        float xRot = entity.getXRot();
        float headRotO = entity.yHeadRotO;
        float headRot = entity.yHeadRot;

        float body = 180f + yaw;
        float head = body + options.headYaw();
        entity.yBodyRot = body;
        entity.yBodyRotO = body;
        entity.setYRot(head);
        entity.yRotO = head;
        entity.setXRot(options.headPitch());
        entity.xRotO = options.headPitch();
        entity.yHeadRot = head;
        entity.yHeadRotO = head;

        // Как в InventoryScreen: переворот по Z (у GUI ось Y вниз) и наклон камеры по X.
        Quaternionf camera = new Quaternionf().rotateX((float) Math.toRadians(-pitch));
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI).mul(camera);
        // Габариты уже включают атрибут SCALE, а рендерер умножает на него модель, поэтому масштаб — как есть.
        try {
            InventoryScreen.renderEntityInInventory(graphics, x + size / 2f, feetY, scale, ORIGIN, pose, camera, entity);
        } finally {
            if (player) {
                entity.yBodyRotO = bodyRotO;
                entity.yBodyRot = bodyRot;
                entity.yRotO = yRotO;
                entity.setYRot(yRot);
                entity.xRotO = xRotO;
                entity.setXRot(xRot);
                entity.yHeadRotO = headRotO;
                entity.yHeadRot = headRot;
            }
        }
    }

    /** Сущность для отрисовки из кэша; создаётся при первом обращении, кэш сбрасывается со сменой мира. */
    private static LivingEntity entity(ClientLevel level, String typeId, EntityOptions options) {
        if (level == null || typeId == null) {
            return null;
        }
        if (level != entitiesLevel || ENTITIES.size() > MAX_ENTITIES) {
            ENTITIES.clear();
            entitiesLevel = level;
        }
        String key = typeId + "|" + options.appearanceKey();
        if (ENTITIES.containsKey(key)) {
            return ENTITIES.get(key);
        }
        LivingEntity living = create(level, typeId, options);
        ENTITIES.put(key, living);
        return living;
    }

    private static LivingEntity create(ClientLevel level, String typeId, EntityOptions options) {
        ResourceLocation rl = ResourceLocation.tryParse(typeId);
        if (rl == null || !BuiltInRegistries.ENTITY_TYPE.containsKey(rl)) {
            return null;
        }
        try {
            Entity e = BuiltInRegistries.ENTITY_TYPE.get(rl).create(level);
            if (!(e instanceof LivingEntity living)) {
                return null;
            }
            if (options.baby() && living instanceof Mob mob) {
                mob.setBaby(true);
            }
            for (Entry<String, String> eq : options.equipment().entrySet()) {
                ItemStack item = stack(eq.getValue(), false);
                if (!item.isEmpty()) {
                    living.setItemSlot(EquipmentSlot.byName(eq.getKey()), item.copy());
                }
            }
            return living;
        } catch (RuntimeException ex) {
            // Неизвестное место снаряжения или сущность, которой нужен настоящий мир: просто не рисуем.
            return null;
        }
    }
}
