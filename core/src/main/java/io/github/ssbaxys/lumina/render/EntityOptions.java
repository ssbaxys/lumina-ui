package io.github.ssbaxys.lumina.render;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Настройки сущности для {@link Canvas#drawEntity}: куда смотрит голова, детёныш ли это и что надето.
 * Неизменяемый объект; каждый {@code with}-метод возвращает копию.
 *
 * <pre>{@code
 * EntityOptions zombie = EntityOptions.DEFAULT
 *         .equip("mainhand", "minecraft:iron_sword")
 *         .equip("head", "minecraft:golden_helmet");
 * canvas.drawEntity("minecraft:zombie", 0, 0, 64, 20, 10, zombie.look(15, -5));
 * }</pre>
 *
 * Места снаряжения — как в игре: {@code mainhand offhand head chest legs feet}. У игрока
 * ({@link Canvas#PLAYER}) снаряжение и «детёныш» не меняются: он рисуется в своей броне.
 */
public final class EntityOptions {
    public static final EntityOptions DEFAULT = new EntityOptions(0f, 0f, false, Map.of());

    private final float headYaw;
    private final float headPitch;
    private final boolean baby;
    private final Map<String, String> equipment;
    private final String key;

    private EntityOptions(float headYaw, float headPitch, boolean baby, Map<String, String> equipment) {
        this.headYaw = headYaw;
        this.headPitch = headPitch;
        this.baby = baby;
        this.equipment = equipment;
        StringBuilder sb = new StringBuilder(baby ? "baby" : "adult");
        for (Map.Entry<String, String> e : equipment.entrySet()) {
            sb.append(';').append(e.getKey()).append('=').append(e.getValue());
        }
        this.key = sb.toString();
    }

    /** Поворот головы относительно тела, градусы (плюс — влево от зрителя). */
    public float headYaw() {
        return headYaw;
    }

    /** Наклон головы, градусы (минус — смотрит вверх, как у сущностей игры). */
    public float headPitch() {
        return headPitch;
    }

    public boolean baby() {
        return baby;
    }

    /** Снаряжение: место → id предмета, в порядке добавления. */
    public Map<String, String> equipment() {
        return equipment;
    }

    /**
     * Ключ внешнего вида (детёныш и снаряжение, без позы): бэкенд держит по одной сущности на тип и ключ,
     * поза меняется каждый кадр без пересоздания.
     */
    public String appearanceKey() {
        return key;
    }

    /** Куда смотрит голова относительно тела. */
    public EntityOptions look(float headYaw, float headPitch) {
        if (headYaw == this.headYaw && headPitch == this.headPitch) {
            return this;
        }
        return new EntityOptions(headYaw, headPitch, baby, equipment);
    }

    public EntityOptions baby(boolean baby) {
        return baby == this.baby ? this : new EntityOptions(headYaw, headPitch, baby, equipment);
    }

    /** Надеть предмет в место {@code slot} ({@code null} или пустой id — снять). */
    public EntityOptions equip(String slot, String itemId) {
        if (slot == null || slot.isEmpty()) {
            return this;
        }
        Map<String, String> next = new LinkedHashMap<>(equipment);
        if (itemId == null || itemId.isEmpty()) {
            next.remove(slot);
        } else {
            next.put(slot, itemId);
        }
        return new EntityOptions(headYaw, headPitch, baby, Collections.unmodifiableMap(next));
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EntityOptions other && other.headYaw == headYaw && other.headPitch == headPitch
                && other.key.equals(key);
    }

    @Override
    public int hashCode() {
        return key.hashCode() * 31 + Float.hashCode(headYaw) * 7 + Float.hashCode(headPitch);
    }

    @Override
    public String toString() {
        return "EntityOptions[" + key + ", head " + headYaw + "/" + headPitch + "]";
    }
}
