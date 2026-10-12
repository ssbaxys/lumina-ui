package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Spring;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.node.TabSwitchable;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.ArrayList;
import java.util.List;

/**
 * Боковая навигация в современном стиле: иконка над подписью, за иконкой выбранного пункта —
 * «таблетка» акцентного цвета, которая пружиной переезжает между пунктами.
 */
public class SideNav extends Widget<SideNav> implements TabSwitchable {
    private static final float ICON = 16f;
    private static final float ITEM_H = 44f;
    private static final float PILL_W = 34f;
    private static final float PILL_H = 22f;

    private record Item(String label, String itemId, TextureRef texture) {
    }

    private final Signal<Integer> selected;
    private final List<Item> items = new ArrayList<>();
    private int hover = -1;
    private AnimatedFloat pillY;
    private Subscription subscription;

    public SideNav(Signal<Integer> selected) {
        super("side-nav");
        this.selected = selected;
        setFocusable(true);
    }

    public Signal<Integer> selected() {
        return selected;
    }

    /** Пункт с иконкой предмета игры. */
    public SideNav item(String label, String itemId) {
        items.add(new Item(label, itemId, null));
        invalidateLayout();
        return this;
    }

    /** Пункт с иконкой-текстурой. */
    public SideNav item(String label, TextureRef icon) {
        items.add(new Item(label, null, icon));
        invalidateLayout();
        return this;
    }

    /** Соседняя вкладка по кругу (бамперы геймпада). */
    @Override
    public boolean switchTab(int delta) {
        int n = items.size();
        if (n <= 1 || !isEnabled()) {
            return false;
        }
        selected.set(Math.floorMod(index() + delta, n));
        return true;
    }

    private int index() {
        Integer i = selected.peek();
        return i == null ? 0 : Math.max(0, Math.min(items.size() - 1, i));
    }

    private float top() {
        float inner = height() - padding().vertical();
        return padding().top() + Math.max(0f, (inner - items.size() * ITEM_H) / 2f);
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float w = PILL_W + 8f;
        for (Item it : items) {
            w = Math.max(w, m.width(it.label()) + 8f);
        }
        setContentSize(w, items.size() * ITEM_H);
    }

    @Override
    protected void onAttach() {
        pillY = new AnimatedFloat(root().animator(), index() * ITEM_H).spring(Spring.SNAPPY);
        subscription = selected.subscribe(i -> pillY.animateTo(index() * ITEM_H));
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    private int itemAt(float ly) {
        int i = (int) Math.floor((ly - top()) / ITEM_H);
        return i >= 0 && i < items.size() ? i : -1;
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.MOVE) {
            hover = itemAt(((PointerEvent) event).localY());
        } else if (event.type() == PointerEvent.LEAVE) {
            hover = -1;
        } else if (event.type() == PointerEvent.CLICK && ((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
            int i = itemAt(((PointerEvent) event).localY());
            if (i >= 0 && isEnabled()) {
                selected.set(i);
                event.consume();
            }
        } else if (event.type() == KeyEvent.PRESS && event.target() == this) {
            int key = ((KeyEvent) event).key();
            if ((key == Keys.UP || key == Keys.DOWN) && !items.isEmpty()) {
                selected.set(Math.floorMod(index() + (key == Keys.UP ? -1 : 1), items.size()));
                event.consume();
            }
        }
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        int text = style.get(Styles.TEXT_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        float cx = width() / 2f;
        float top = top();
        if (!items.isEmpty()) {
            float py = top + (pillY != null ? pillY.get() : index() * ITEM_H) + 4f;
            canvas.fillRoundRect(cx - PILL_W / 2f, py, PILL_W, PILL_H, Radii.all(PILL_H / 2f),
                    Brush.solid(Color.withAlpha(accent, 0x48)));
        }
        int current = index();
        for (int i = 0; i < items.size(); i++) {
            Item it = items.get(i);
            float y = top + i * ITEM_H;
            float iy = y + 4f + (PILL_H - ICON) / 2f;
            if (i == hover && i != current) {
                canvas.fillRoundRect(cx - PILL_W / 2f, y + 4f, PILL_W, PILL_H, Radii.all(PILL_H / 2f),
                        Brush.solid(Color.withAlpha(text, 0x14)));
            }
            if (it.itemId() != null) {
                canvas.drawItem(it.itemId(), cx - ICON / 2f, iy, ICON);
            } else if (it.texture() != null) {
                canvas.drawTexture(it.texture(), cx - ICON / 2f, iy, ICON, ICON, i == current ? accent : text);
            }
            int c = i == current ? text : Color.withAlpha(text, i == hover ? 0xE0 : 0xA8);
            float lw = m.width(it.label());
            canvas.drawText(it.label(), cx - lw / 2f, y + 4f + PILL_H + 4f, c, false);
        }
    }
}
