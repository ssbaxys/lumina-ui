package io.github.ssbaxys.lumina.preview;

import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.EntityOptions;

/**
 * Сущность игры в 3D, как в окне инвентаря: моб ({@code "minecraft:zombie"}) или сам игрок в его броне.
 * Крутится мышью с инерцией, может вращаться сам, приближается колесом, а с {@link #followMouse} — смотрит на
 * курсор, как игрок в инвентаре. Рисует бэкенд ({@link Canvas#drawEntity}); без мира (в главном меню) пусто.
 *
 * <pre>{@code
 * new EntityView("minecraft:creeper").autoRotate(true).size(64, 80);
 * new EntityView("minecraft:zombie").equip("mainhand", "minecraft:iron_sword").baby(true);
 * EntityView.player().followMouse(true).size(90, 120);
 * }</pre>
 */
public class EntityView extends SpinView<EntityView> {
    private String type;
    private EntityOptions options = EntityOptions.DEFAULT;
    private boolean followMouse;
    private EntityOptions looking;
    private float lookYaw = Float.NaN;
    private float lookPitch = Float.NaN;

    public EntityView(String entityTypeId) {
        super("entity-view", -25f, 10f);
        this.type = entityTypeId;
    }

    /** Локальный игрок с его бронёй и предметами в руках. */
    public static EntityView player() {
        return new EntityView(Canvas.PLAYER);
    }

    public String type() {
        return type;
    }

    public EntityView type(String entityTypeId) {
        this.type = entityTypeId;
        return this;
    }

    public boolean isPlayer() {
        return Canvas.PLAYER.equals(type);
    }

    public EntityOptions options() {
        return options;
    }

    public EntityView options(EntityOptions options) {
        this.options = options == null ? EntityOptions.DEFAULT : options;
        this.looking = null;
        return this;
    }

    /** Детёныш (у мобов, у которых он бывает). */
    public EntityView baby(boolean baby) {
        return options(options.baby(baby));
    }

    /** Предмет в руке или броня: {@code equip("head", "minecraft:diamond_helmet")}. */
    public EntityView equip(String slot, String itemId) {
        return options(options.equip(slot, itemId));
    }

    /** Поворот головы относительно тела (если не следит за мышью). */
    public EntityView look(float headYaw, float headPitch) {
        return options(options.look(headYaw, headPitch));
    }

    public boolean followsMouse() {
        return followMouse;
    }

    /** Смотреть на курсор: голова (и немного тело) поворачиваются за мышью, как у игрока в инвентаре. */
    public EntityView followMouse(boolean follow) {
        this.followMouse = follow;
        return this;
    }

    /**
     * Куда смотреть на курсор из точки (cx, cy), как в игре: {@code atan(d / 40)}, до ±20° головой и ещё
     * столько же телом по горизонтали, до ±20° по вертикали. Возвращает {yaw тела, yaw головы, pitch головы}.
     */
    static float[] lookAt(float cx, float cy, float mouseX, float mouseY, float[] out) {
        float fx = (float) Math.atan((cx - mouseX) / 40f);
        float fy = (float) Math.atan((cy - mouseY) / 40f);
        out[0] = fx * 20f;
        out[1] = fx * 20f;
        out[2] = -fy * 20f;
        return out;
    }

    private final float[] look = new float[3];

    @Override
    protected void paintModel(Canvas canvas, float x, float y, float size, float yaw, float pitch) {
        if (type == null) {
            return;
        }
        EntityOptions o = options;
        float bodyYaw = yaw;
        UiRoot r = root();
        if (followMouse && r != null && r.mouseX() >= 0f) {
            // Глаза — примерно на высоте трети квадрата от верха.
            float cx = screenX() + x + size / 2f;
            float cy = screenY() + y + size * 0.3f;
            lookAt(cx, cy, r.mouseX(), r.mouseY(), look);
            bodyYaw += look[0];
            // Округление до полуградуса: новые настройки создаются только при заметном сдвиге мыши.
            float hy = Math.round((options.headYaw() + look[1]) * 2f) / 2f;
            float hp = Math.round((options.headPitch() + look[2]) * 2f) / 2f;
            if (looking == null || hy != lookYaw || hp != lookPitch) {
                lookYaw = hy;
                lookPitch = hp;
                looking = options.look(hy, hp);
            }
            o = looking;
        }
        canvas.drawEntity(type, x, y, size, bodyYaw, pitch, o);
    }
}
