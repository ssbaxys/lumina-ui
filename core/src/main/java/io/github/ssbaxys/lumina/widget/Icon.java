package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextureRef;

/** Иконка: предмет игры ({@link #item}) или текстура ({@link #texture}), вписанная в размер виджета. */
public class Icon extends Widget<Icon> {
    private final String itemId;
    private final TextureRef texture;

    private Icon(String itemId, TextureRef texture, float size) {
        super("icon");
        this.itemId = itemId;
        this.texture = texture;
        size(size, size);
    }

    /** Предмет как в инвентаре: {@code Icon.item("minecraft:diamond_sword", 16)}. */
    public static Icon item(String itemId, float size) {
        return new Icon(itemId, null, size);
    }

    /** Текстура целиком: {@code Icon.texture(TextureRef.of("minecraft:textures/item/apple.png"), 16)}. */
    public static Icon texture(TextureRef texture, float size) {
        return new Icon(null, texture, size);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float x = padding().left();
        float y = padding().top();
        float w = width() - padding().horizontal();
        float h = height() - padding().vertical();
        if (itemId != null) {
            float s = Math.min(w, h);
            canvas.drawItem(itemId, x + (w - s) / 2f, y + (h - s) / 2f, s);
        } else {
            canvas.drawTexture(texture, x, y, w, h, Color.WHITE);
        }
    }
}
