package io.github.ssbaxys.lumina.hud;

import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;

/**
 * Элемент, размещённый на HUD: привязка к краю экрана, смещение, слой и правило видимости.
 * Возвращается из {@link Hud#add}; все сеттеры — цепочкой.
 *
 * <pre>{@code
 * hud.add(bar, HudAnchor.BOTTOM_CENTER).offset(0, 40).layer(10).visibility(HudVisibility.GAMEPLAY);
 * }</pre>
 */
public final class HudEntry {
    private final Hud hud;
    private final UiElement element;
    final Slot slot;
    private HudAnchor anchor;
    private float offsetX;
    private float offsetY;
    private int layer;
    private HudVisibility visibility;
    private boolean attached = true;

    HudEntry(Hud hud, UiElement element, HudAnchor anchor, float offsetX, float offsetY) {
        this.hud = hud;
        this.element = element;
        this.anchor = anchor == null ? HudAnchor.TOP_LEFT : anchor;
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.slot = new Slot(this);
        slot.addChild(element);
    }

    public UiElement element() {
        return element;
    }

    public HudAnchor anchor() {
        return anchor;
    }

    public HudEntry anchor(HudAnchor anchor) {
        HudAnchor a = anchor == null ? HudAnchor.TOP_LEFT : anchor;
        if (a != this.anchor) {
            this.anchor = a;
            slot.invalidateLayout();
        }
        return this;
    }

    /** Смещение внутрь экрана от привязанного края (см. {@link HudAnchor}). */
    public HudEntry offset(float dx, float dy) {
        if (dx != offsetX || dy != offsetY) {
            offsetX = dx;
            offsetY = dy;
            slot.invalidateLayout();
        }
        return this;
    }

    public float offsetX() {
        return offsetX;
    }

    public float offsetY() {
        return offsetY;
    }

    public int layer() {
        return layer;
    }

    /** Слой: больший рисуется поверх меньшего; при равных — в порядке добавления. */
    public HudEntry layer(int layer) {
        if (layer != this.layer) {
            this.layer = layer;
            if (attached) {
                hud.reorder(this);
            }
        }
        return this;
    }

    /** Правило видимости; {@code null} — общее правило HUD ({@link Hud#setDefaultVisibility}). */
    public HudEntry visibility(HudVisibility visibility) {
        this.visibility = visibility;
        return this;
    }

    /** Своё правило видимости или {@code null}. */
    public HudVisibility visibility() {
        return visibility;
    }

    /** Был ли элемент показан в последнем кадре (по правилу видимости). */
    public boolean isShown() {
        return attached && slot.isVisible();
    }

    /** Левый край элемента на экране после последней раскладки. */
    public float x() {
        return slot.x();
    }

    /** Верхний край элемента на экране после последней раскладки. */
    public float y() {
        return slot.y();
    }

    public float width() {
        return slot.width();
    }

    public float height() {
        return slot.height();
    }

    public boolean isAttached() {
        return attached;
    }

    /** Убрать элемент с HUD. */
    public void remove() {
        if (attached) {
            hud.remove(element);
        }
    }

    void detached() {
        attached = false;
        slot.removeChild(element);
    }

    /** Обёртка элемента: на ней — видимость по правилу, так что своя видимость элемента не трогается. */
    static final class Slot extends UiElement {
        final HudEntry entry;

        Slot(HudEntry entry) {
            super(null);
            this.entry = entry;
            setLayout(StackLayout.topLeft());
            setHitTestable(false);
        }

        @Override
        public String toString() {
            return "HudSlot[" + entry.anchor + ", " + entry.element + "]";
        }
    }
}
