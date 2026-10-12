package io.github.ssbaxys.lumina.inventory;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.TextMetrics;

import java.util.ArrayList;
import java.util.List;

/**
 * Содержимое подсказки предмета, как в игре: название (бирюзовое у зачарованных), строки описания серым
 * и прочность у повреждённых. Слоты показывают его сами; для своих виджетов:
 * {@code Tooltips.attach(icon, () -> new ItemTooltip(stack))}.
 */
public class ItemTooltip extends UiElement {
    /** Цвет названия обычного предмета. */
    public static final int NAME_COLOR = 0xFFFFFFFF;
    /** Цвет названия зачарованного предмета (как «редкий» в игре). */
    public static final int GLINT_NAME_COLOR = 0xFF55FFFF;
    /** Цвет строк описания. */
    public static final int LORE_COLOR = 0xFFAAAAAA;

    private final List<String> lines = new ArrayList<>();
    private final List<Integer> colors = new ArrayList<>();

    public ItemTooltip(Stack stack) {
        super("item-tooltip");
        setHitTestable(false);
        Stack s = stack == null ? Stack.EMPTY : stack;
        if (s.isEmpty()) {
            return;
        }
        add(s.displayName(), s.hasGlint() ? GLINT_NAME_COLOR : NAME_COLOR);
        for (String l : s.lore()) {
            add(l, LORE_COLOR);
        }
        if (s.isDamaged()) {
            add("Прочность: " + (s.maxDamage() - s.damage()) + " / " + s.maxDamage(), LORE_COLOR);
        }
    }

    private void add(String line, int color) {
        lines.add(line == null ? "" : line);
        colors.add(color);
    }

    /** Строки подсказки (для тестов и своих подсказок). */
    public List<String> lines() {
        return lines;
    }

    private float step(TextMetrics m) {
        return m.lineHeight() + 1f;
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float w = 0f;
        for (String l : lines) {
            w = Math.max(w, m.width(l));
        }
        float h = lines.isEmpty() ? 0f : lines.size() * step(m) - 2f + (lines.size() > 1 ? 2f : 0f);
        setContentSize(w, h);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        float x = padding().left();
        float y = padding().top();
        for (int i = 0; i < lines.size(); i++) {
            canvas.drawText(lines.get(i), x, y, colors.get(i), true);
            // После названия — 2 пикселя, как в подсказках игры.
            y += step(m) + (i == 0 ? 2f : 0f);
        }
    }
}
