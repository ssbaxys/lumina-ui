package io.github.ssbaxys.lumina.preview;

import io.github.ssbaxys.lumina.inventory.Stack;
import io.github.ssbaxys.lumina.render.Canvas;

/**
 * Крупный предмет в 3D, который крутится мышью или сам ({@link Canvas#drawItem3d}): блоки видны объёмными,
 * обычные предметы — как выброшенные на землю. Если бэкенд не умеет поворачивать предметы, рисуется плоская
 * иконка.
 *
 * <pre>{@code
 * new ItemView("minecraft:diamond_sword").autoRotate(true).size(96, 96);
 * new ItemView(Stack.of("minecraft:netherite_chestplate").withGlint(true)).camera(30, 15);
 * }</pre>
 */
public class ItemView extends SpinView<ItemView> {
    private Stack stack;

    public ItemView(String itemId) {
        this(Stack.of(itemId));
    }

    public ItemView(Stack stack) {
        super("item-view", 0f, 0f);
        this.stack = stack == null ? Stack.EMPTY : stack;
    }

    public Stack stack() {
        return stack;
    }

    public ItemView stack(Stack stack) {
        this.stack = stack == null ? Stack.EMPTY : stack;
        return this;
    }

    public ItemView item(String itemId) {
        return stack(Stack.of(itemId));
    }

    @Override
    protected void paintModel(Canvas canvas, float x, float y, float size, float yaw, float pitch) {
        if (!stack.isEmpty()) {
            canvas.drawItem3d(stack.itemId(), x, y, size, yaw, pitch, stack.hasGlint());
        }
    }
}
