package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Контекстное меню во всплывающем слое {@link UiRoot}: пункты с подписью, подсказкой сочетания
 * справа, разделители. Открывается у курсора с появлением (прозрачность + лёгкое увеличение),
 * закрывается кликом мимо, Escape или выбором пункта. Стрелки и Enter тоже работают.
 *
 * <pre>{@code
 * new ContextMenu()
 *         .item("Копировать", "Ctrl+C", this::copy)
 *         .separator()
 *         .item("Удалить", this::delete)
 *         .show(root, mouseX, mouseY);
 * }</pre>
 *
 * Стили: "context-menu", "menu-item" (активный пункт — состояние {@link #ACTIVE}), "menu-separator".
 */
public class ContextMenu extends Widget<ContextMenu> {
    /** Состояние подсвеченного пункта (мышью или стрелками). */
    public static final int ACTIVE = State.FIRST_CUSTOM;
    private static final float APPEAR_SCALE = 0.94f;

    /** Пункт меню. Доступность вычисляется при каждом открытии. */
    public static final class Item {
        private final String label;
        private String shortcut;
        private BooleanSupplier enabled = () -> true;
        private final Runnable action;

        private Item(String label, Runnable action) {
            this.label = label;
            this.action = action;
        }

        public static Item of(String label, Runnable action) {
            return new Item(label, action);
        }

        /** Разделитель между группами пунктов. */
        public static Item separator() {
            return new Item(null, null);
        }

        /** Подсказка сочетания справа, например "Ctrl+C". */
        public Item shortcut(String hint) {
            this.shortcut = hint;
            return this;
        }

        public Item enabled(boolean enabled) {
            this.enabled = () -> enabled;
            return this;
        }

        public Item enabledWhen(BooleanSupplier condition) {
            this.enabled = condition;
            return this;
        }

        public String label() {
            return label;
        }

        public String shortcut() {
            return shortcut;
        }

        public boolean isSeparator() {
            return label == null;
        }

        public boolean isEnabled() {
            return !isSeparator() && enabled.getAsBoolean();
        }

        public void run() {
            if (action != null) {
                action.run();
            }
        }
    }

    private final List<Item> items = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private Row active;
    private AnimatedFloat appear;

    public ContextMenu() {
        super("context-menu");
        setLayout(FlexLayout.column().align(Align.STRETCH));
    }

    public ContextMenu item(String label, Runnable action) {
        return add(Item.of(label, action));
    }

    public ContextMenu item(String label, String shortcut, Runnable action) {
        return add(Item.of(label, action).shortcut(shortcut));
    }

    public ContextMenu item(String label, String shortcut, boolean enabled, Runnable action) {
        return add(Item.of(label, action).shortcut(shortcut).enabled(enabled));
    }

    public ContextMenu add(Item item) {
        items.add(item);
        return this;
    }

    public ContextMenu separator() {
        return add(Item.separator());
    }

    public List<Item> items() {
        return Collections.unmodifiableList(items);
    }

    /** Открыть в точке экрана (обычно — у курсора). Повторный вызов переоткрывает меню. */
    public ContextMenu show(UiRoot root, float x, float y) {
        rebuild();
        // Сдвиг на пиксель: пункт под курсором не подсвечивается, пока мышь не сдвинется.
        root.showPopup(this, x + 1f, y + 1f);
        return this;
    }

    public void close() {
        if (root() != null) {
            root().closePopup(this);
        }
    }

    public boolean isOpen() {
        return root() != null && root().isPopupOpen(this);
    }

    /** Выполнить пункт (если доступен) и закрыть меню. */
    public void activate(Item item) {
        if (!item.isEnabled()) {
            return;
        }
        close();
        if (Lumina.isInitialized()) {
            Lumina.platform().playClickSound();
        }
        item.run();
    }

    /** Строки пересобираются при открытии: подписи и доступность — на момент показа. */
    private void rebuild() {
        clearChildren();
        rows.clear();
        active = null;
        boolean pendingSeparator = false;
        for (Item item : items) {
            if (item.isSeparator()) {
                pendingSeparator = !rows.isEmpty();
                continue;
            }
            if (pendingSeparator) {
                addChild(new Separator());
                pendingSeparator = false;
            }
            Row row = new Row(item);
            row.setEnabled(item.isEnabled());
            rows.add(row);
            addChild(row);
        }
    }

    private void setActive(Row row) {
        if (active == row) {
            return;
        }
        if (active != null) {
            active.setState(ACTIVE, false);
        }
        active = row;
        if (row != null) {
            row.setState(ACTIVE, true);
        }
    }

    /** Следующий доступный пункт по кругу. */
    private void moveActive(int dir) {
        if (rows.isEmpty()) {
            return;
        }
        int i = active == null ? (dir > 0 ? -1 : rows.size()) : rows.indexOf(active);
        for (int n = 0; n < rows.size(); n++) {
            i = Math.floorMod(i + dir, rows.size());
            if (rows.get(i).isEnabled()) {
                setActive(rows.get(i));
                return;
            }
        }
    }

    @Override
    protected void onAttach() {
        // Появление: прозрачность и масштаб от угла у курсора (сдвиг компенсирует масштаб от центра).
        appear = new AnimatedFloat(root().animator(), 0f).tween(0.14f, Easing.OUT_CUBIC).listener(this::applyAppear);
        applyAppear(0f);
        appear.animateTo(1f);
    }

    private void applyAppear(float t) {
        float s = APPEAR_SCALE + (1f - APPEAR_SCALE) * t;
        setAnimatedStyle(Styles.OPACITY, t);
        setAnimatedStyle(Styles.SCALE, s);
        setAnimatedStyle(Styles.TRANSLATE_X, -(1f - s) * width() / 2f);
        setAnimatedStyle(Styles.TRANSLATE_Y, -(1f - s) * height() / 2f);
    }

    @Override
    protected void onLayout() {
        if (appear != null) {
            applyAppear(appear.get());
        }
    }

    @Override
    protected void onDetach() {
        setActive(null);
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == KeyEvent.PRESS) {
            int key = ((KeyEvent) event).key();
            if (key == Keys.DOWN) {
                moveActive(1);
                event.consume();
            } else if (key == Keys.UP) {
                moveActive(-1);
                event.consume();
            } else if ((key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.SPACE) && active != null) {
                activate(active.item);
                event.consume();
            }
        } else if (event.type() == PointerEvent.LEAVE && event.target() == this) {
            setActive(null);
        }
    }

    /** Строка открытого меню по подписи пункта, или {@code null}. */
    public Row row(String label) {
        for (Row r : rows) {
            if (r.item.label().equals(label)) {
                return r;
            }
        }
        return null;
    }

    /** Строка меню: подпись слева, сочетание справа приглушённым цветом. */
    public final class Row extends UiElement {
        private static final float GAP = 24f;
        private final Item item;

        Row(Item item) {
            super("menu-item");
            this.item = item;
            layoutParams().minWidth = 90f;
        }

        public Item item() {
            return item;
        }

        @Override
        protected void handleEvent(UiEvent event) {
            if (event.type() == PointerEvent.ENTER) {
                setActive(isEnabled() ? this : null);
            } else if (event.type() == PointerEvent.CLICK && event.target() == this) {
                if (((PointerEvent) event).button() == Keys.MOUSE_LEFT || ((PointerEvent) event).button() == Keys.MOUSE_RIGHT) {
                    activate(item);
                }
                event.consume();
            } else if (event.type() == PointerEvent.PRESS) {
                event.consume();
            }
        }

        @Override
        protected void measureContent(float maxWidth, float maxHeight) {
            TextMetrics m = Lumina.platform().textMetrics();
            float w = m.width(item.label());
            if (item.shortcut() != null) {
                w += GAP + m.width(item.shortcut());
            }
            setContentSize(w, m.lineHeight());
        }

        @Override
        protected void paintContent(Canvas canvas, float partialTick) {
            TextMetrics m = canvas.textMetrics();
            Insets pad = padding();
            float ty = (height() - m.lineHeight()) / 2f + 0.5f;
            int color = style.get(Styles.TEXT_COLOR);
            canvas.drawText(item.label(), pad.left(), ty, color, false);
            if (item.shortcut() != null) {
                float sx = width() - pad.right() - m.width(item.shortcut());
                canvas.drawText(item.shortcut(), sx, ty, Color.mulAlpha(color, 0.5f), false);
            }
        }
    }

    /** Тонкая линия между группами. */
    private static final class Separator extends UiElement {
        Separator() {
            super("menu-separator");
            layoutParams().height = Dim.px(1);
            layoutParams().margin = new Insets(3, 4, 3, 4);
            setHitTestable(false);
        }
    }
}
