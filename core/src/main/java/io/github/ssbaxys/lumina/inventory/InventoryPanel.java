package io.github.ssbaxys.lumina.inventory;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Widget;

/**
 * Окно-контейнер для сеток слотов: колонка подписей и {@link SlotGrid}. Ванильный вид — светло-серая
 * панель с объёмной рамкой и тёмными подписями без тени, как у сундука; современный — панель темы.
 *
 * <pre>{@code
 * Inventory chest = new Inventory(27);
 * Inventory player = new Inventory(36);   // 0..8 — хотбар, 9..35 — рюкзак
 * InventoryPanel ui = InventoryPanel.chest("Сундук", chest, player, Slot.Look.VANILLA);
 * InventoryPanel custom = new InventoryPanel(Slot.Look.MODERN).title("Печь").add(furnaceRow);
 * }</pre>
 */
public class InventoryPanel extends Widget<InventoryPanel> {
    /** Цвет подписей ванильного окна. */
    public static final int VANILLA_TITLE = 0xFF404040;

    private final Slot.Look look;

    public InventoryPanel(Slot.Look look) {
        super(look == Slot.Look.MODERN ? "panel" : "inventory-panel");
        this.look = look == null ? Slot.Look.VANILLA : look;
        setLayout(FlexLayout.column().gap(3).align(Align.START));
        if (this.look == Slot.Look.VANILLA) {
            setStyle(inlineStyle().toBuilder().set(Styles.PADDING, new Insets(6, 7, 7, 7)).build());
        }
    }

    public Slot.Look look() {
        return look;
    }

    /** Подпись (название окна или раздела). */
    public InventoryPanel title(String text) {
        Label l = new Label(text);
        if (look == Slot.Look.VANILLA) {
            l.style(b -> b.set(Styles.TEXT_COLOR, VANILLA_TITLE).set(Styles.TEXT_SHADOW, false));
        }
        addChild(l);
        return this;
    }

    public InventoryPanel add(UiElement... elements) {
        for (UiElement e : elements) {
            addChild(e);
        }
        return this;
    }

    /** Сетка с отступом сверху (как хотбар под рюкзаком). */
    public InventoryPanel add(UiElement element, float marginTop) {
        element.layoutParams().margin = new Insets(marginTop, 0, 0, 0);
        addChild(element);
        return this;
    }

    /**
     * Сундук над инвентарём игрока: {@code chest} по 9 в ряд, рюкзак ({@code player} 9..35) и хотбар (0..8).
     * Shift+клик переносит из сундука в игрока и обратно, внутри игрока — между рюкзаком и хотбаром.
     */
    public static InventoryPanel chest(String title, Inventory chest, Inventory player, Slot.Look look) {
        SlotGrid chestGrid = new SlotGrid(chest, 9).look(look).quickMoveTo(player, 9, 36).quickMoveTo(player, 0, 9);
        InventoryPanel p = new InventoryPanel(look).title(title).add(chestGrid);
        addPlayer(p, player, look, chest);
        return p;
    }

    /** Только инвентарь игрока (36 ячеек: 0..8 — хотбар, 9..35 — рюкзак). */
    public static InventoryPanel player(Inventory player, Slot.Look look) {
        InventoryPanel p = new InventoryPanel(look);
        addPlayer(p, player, look, null);
        return p;
    }

    private static void addPlayer(InventoryPanel p, Inventory player, Slot.Look look, Inventory other) {
        if (player.size() < 36) {
            throw new IllegalArgumentException("player inventory needs 36 slots, got " + player.size());
        }
        SlotGrid main = new SlotGrid(player, 9, 27, 9).look(look);
        SlotGrid hotbar = new SlotGrid(player, 0, 9, 9).look(look);
        if (other != null) {
            main.quickMoveTo(other);
            hotbar.quickMoveTo(other);
        } else {
            main.quickMoveTo(player, 0, 9);
            hotbar.quickMoveTo(player, 9, 36);
        }
        p.title("Инвентарь").add(main).add(hotbar, 1f);
    }

    @Override
    protected void paintBackground(Canvas canvas) {
        if (look != Slot.Look.VANILLA) {
            super.paintBackground(canvas);
            return;
        }
        float w = width();
        float h = height();
        // Чёрный контур без угловых пикселей, белая кромка сверху-слева, тёмная снизу-справа.
        canvas.fillRect(1f, 0f, w - 2f, 1f, 0xFF000000);
        canvas.fillRect(1f, h - 1f, w - 2f, 1f, 0xFF000000);
        canvas.fillRect(0f, 1f, 1f, h - 2f, 0xFF000000);
        canvas.fillRect(w - 1f, 1f, 1f, h - 2f, 0xFF000000);
        canvas.fillRect(1f, 1f, w - 2f, h - 2f, 0xFFC6C6C6);
        canvas.fillRect(1f, 1f, w - 3f, 2f, 0xFFFFFFFF);
        canvas.fillRect(1f, 3f, 2f, h - 5f, 0xFFFFFFFF);
        canvas.fillRect(3f, h - 3f, w - 4f, 2f, 0xFF555555);
        canvas.fillRect(w - 3f, 2f, 2f, h - 5f, 0xFF555555);
    }
}
