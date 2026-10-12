package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.PopupStyles;
import io.github.ssbaxys.lumina.style.StyleKey;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Радиальное (круговое) меню действий: секторы с иконками и подписями вокруг точки открытия.
 * Сектор выбирается направлением мыши от центра (далеко от кольца — тоже), наведённый сектор
 * выдвигается и подсвечивается, в центре — его подпись. Пункты могут открывать вложенные меню.
 *
 * <p>Два способа открыть: {@link #show} — выбор кликом (правая кнопка или клик в центре — назад,
 * Escape — назад или закрыть); {@link #showHeld} — пока зажата клавиша: отпускание выбирает
 * наведённый сектор. Стрелки перебирают секторы, Enter выбирает, цифры 1–9 — сразу пункт по номеру.</p>
 *
 * <pre>{@code
 * RadialMenu.bindKey(screenRoot, Keys.R, () -> new RadialMenu()
 *         .item("Меч", "minecraft:diamond_sword", this::sword)
 *         .item("Лук", "minecraft:bow", this::bow)
 *         .submenu("Еда", "minecraft:apple",
 *                 RadialMenu.Item.of("Яблоко", this::apple).icon("minecraft:apple"),
 *                 RadialMenu.Item.of("Хлеб", this::bread).icon("minecraft:bread")));
 * }</pre>
 *
 * Занимает весь экран во всплывающем слое {@link UiRoot} (затемнение — фон стиля). Стиль
 * "radial-menu": BACKGROUND — затемнение экрана, {@link #WEDGE_COLOR} — секторы,
 * ACCENT_COLOR — наведённый сектор, TEXT_COLOR — подписи.
 */
public class RadialMenu extends Widget<RadialMenu> {
    /** Цвет секторов и центрального круга. */
    public static final StyleKey<Integer> WEDGE_COLOR = PopupStyles.WEDGE_COLOR;

    private static final float ICON = 16f;
    private static final float GAP = 2f;
    private static final float POP_OUT = 6f;
    private static final float SEGMENT_DEG = 10f;

    /** Пункт меню: подпись, иконка (предмет или текстура), действие или вложенные пункты. */
    public static final class Item {
        private final String label;
        private final Runnable action;
        private final List<Item> children = new ArrayList<>();
        private String itemId;
        private TextureRef texture;
        private BooleanSupplier enabled = () -> true;

        private Item(String label, Runnable action) {
            this.label = label == null ? "" : label;
            this.action = action;
        }

        public static Item of(String label, Runnable action) {
            return new Item(label, action);
        }

        /** Пункт, который открывает вложенное меню. */
        public static Item submenu(String label, Item... children) {
            Item item = new Item(label, null);
            Collections.addAll(item.children, children);
            return item;
        }

        /** Иконка — предмет игры ("minecraft:apple"). */
        public Item icon(String itemId) {
            this.itemId = itemId;
            return this;
        }

        public Item texture(TextureRef texture) {
            this.texture = texture;
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

        public Item add(Item child) {
            children.add(child);
            return this;
        }

        public String label() {
            return label;
        }

        public String itemId() {
            return itemId;
        }

        public boolean hasSubmenu() {
            return !children.isEmpty();
        }

        public List<Item> children() {
            return Collections.unmodifiableList(children);
        }

        public boolean isEnabled() {
            return enabled.getAsBoolean();
        }
    }

    private final List<Item> items = new ArrayList<>();
    private final ArrayDeque<List<Item>> trail = new ArrayDeque<>();
    private final ArrayDeque<String> titles = new ArrayDeque<>();
    private List<Item> level = items;
    private String title = "";
    private float innerRadius = 30f;
    private float outerRadius = 88f;
    private float requestX;
    private float requestY;
    private float cx;
    private float cy;
    private int hot = -1;
    private boolean centerHot;
    private int holdKey = -1;
    private AnimatedFloat appear;
    private AnimatedFloat[] heat = new AnimatedFloat[0];

    public RadialMenu() {
        super("radial-menu");
        layoutParams().width = Dim.FILL;
        layoutParams().height = Dim.FILL;
    }

    // ================================================================ пункты

    public RadialMenu add(Item item) {
        items.add(item);
        return this;
    }

    public RadialMenu item(String label, Runnable action) {
        return add(Item.of(label, action));
    }

    /** Пункт с иконкой предмета. */
    public RadialMenu item(String label, String itemId, Runnable action) {
        return add(Item.of(label, action).icon(itemId));
    }

    /** Пункт с вложенным меню. */
    public RadialMenu submenu(String label, String itemId, Item... children) {
        return add(Item.submenu(label, children).icon(itemId));
    }

    /** Подпись в центре, пока ни один сектор не наведён. */
    public RadialMenu title(String title) {
        this.title = title == null ? "" : title;
        return this;
    }

    /** Радиусы кольца: внутренний (центральный круг) и внешний. */
    public RadialMenu radius(float inner, float outer) {
        this.innerRadius = Math.max(8f, inner);
        this.outerRadius = Math.max(innerRadius + 16f, outer);
        return this;
    }

    public List<Item> items() {
        return Collections.unmodifiableList(items);
    }

    /** Пункты текущего уровня (во вложенном меню — его пункты). */
    public List<Item> currentItems() {
        return Collections.unmodifiableList(level);
    }

    /** Глубина вложенности: 0 — верхний уровень. */
    public int depth() {
        return trail.size();
    }

    /** Наведённый пункт или {@code null}. */
    public Item hoveredItem() {
        return hot >= 0 && hot < level.size() ? level.get(hot) : null;
    }

    /** Центр кольца в координатах экрана. */
    public float centerX() {
        return cx;
    }

    public float centerY() {
        return cy;
    }

    // ================================================================ открытие

    /** Открыть в точке экрана; выбор — кликом. */
    public RadialMenu show(UiRoot root, float x, float y) {
        return open(root, x, y, -1);
    }

    /**
     * Открыть, пока зажата клавиша {@code key}: отпускание выбирает наведённый сектор
     * (вложенное меню при этом открывается и дальше работает по клику).
     */
    public RadialMenu showHeld(UiRoot root, float x, float y, int key) {
        return open(root, x, y, key);
    }

    private RadialMenu open(UiRoot root, float x, float y, int key) {
        requestX = x < 0f ? root.width() / 2f : x;
        requestY = y < 0f ? root.height() / 2f : y;
        holdKey = key;
        trail.clear();
        titles.clear();
        setLevel(items);
        invalidateLayout();
        root.showPopup(this, 0f, 0f);
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

    /** Открыть по клавише: нажатие над {@code host} (или его потомком в фокусе) открывает меню у курсора. */
    public static void bindKey(UiElement host, int key, Supplier<RadialMenu> factory) {
        host.on(KeyEvent.PRESS, e -> {
            UiRoot r = host.root();
            if (e.key() == key && r != null) {
                factory.get().showHeld(r, r.mouseX(), r.mouseY(), key);
                e.consume();
            }
        });
    }

    /** Открыть правой кнопкой мыши над {@code host}. */
    public static void bindRightClick(UiElement host, Supplier<RadialMenu> factory) {
        host.on(PointerEvent.PRESS, e -> {
            UiRoot r = host.root();
            if (e.button() == Keys.MOUSE_RIGHT && r != null) {
                factory.get().show(r, e.x(), e.y());
                e.consume();
            }
        });
    }

    private void setLevel(List<Item> next) {
        level = next;
        hot = -1;
        centerHot = false;
        if (root() != null) {
            makeHeat();
            if (appear != null) {
                appear.snapTo(0f);
                appear.animateTo(1f);
            }
            pointAt(root().mouseX(), root().mouseY());
        }
    }

    private void makeHeat() {
        heat = new AnimatedFloat[level.size()];
        for (int i = 0; i < heat.length; i++) {
            heat[i] = new AnimatedFloat(root().animator(), 0f).tween(0.12f, Easing.OUT_CUBIC);
        }
    }

    /** Выполнить пункт: вложенное меню открывается, обычный пункт закрывает меню и срабатывает. */
    public void activate(Item item) {
        if (item == null || !item.isEnabled()) {
            return;
        }
        if (Lumina.isInitialized()) {
            Lumina.platform().playClickSound();
        }
        if (item.hasSubmenu()) {
            trail.push(level);
            titles.push(item.label());
            holdKey = -1;
            setLevel(item.children);
            return;
        }
        close();
        if (item.action != null) {
            item.action.run();
        }
    }

    /** Назад из вложенного меню; на верхнем уровне — закрыть. */
    public void back() {
        if (trail.isEmpty()) {
            close();
        } else {
            titles.pop();
            setLevel(trail.pop());
        }
    }

    // ================================================================ наведение

    private void setHot(int i) {
        if (i == hot) {
            return;
        }
        if (hot >= 0 && hot < heat.length) {
            heat[hot].animateTo(0f);
        }
        hot = i;
        if (hot >= 0 && hot < heat.length) {
            heat[hot].animateTo(1f);
        }
    }

    /** Сектор по направлению от центра к точке (координаты экрана). */
    private void pointAt(float x, float y) {
        if (x < 0f || level.isEmpty()) {
            return;
        }
        float dx = x - cx;
        float dy = y - cy;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        centerHot = dist < innerRadius;
        if (dist < innerRadius * 0.7f || dist > outerRadius + 60f) {
            setHot(-1);
            return;
        }
        setHot(sectorAt(dx, dy));
    }

    private int sectorAt(float dx, float dy) {
        float slice = 360f / level.size();
        float angle = (float) Math.toDegrees(Math.atan2(dx, -dy));
        if (angle < 0f) {
            angle += 360f;
        }
        return Math.floorMod((int) Math.floor((angle + slice / 2f) / slice), level.size());
    }

    // ================================================================ жизненный цикл и ввод

    @Override
    protected void onAttach() {
        appear = new AnimatedFloat(root().animator(), 0f).tween(0.22f, Easing.OUT_BACK)
                .listener(t -> setAnimatedStyle(Styles.OPACITY, Math.max(0f, Math.min(1f, t * 1.4f))));
        setAnimatedStyle(Styles.OPACITY, 0f);
        appear.animateTo(1f);
        makeHeat();
    }

    @Override
    protected void onDetach() {
        holdKey = -1;
    }

    @Override
    protected void onLayout() {
        float margin = outerRadius + POP_OUT + 4f;
        cx = width() < 2f * margin ? width() / 2f : Math.max(margin, Math.min(width() - margin, requestX));
        cy = height() < 2f * margin ? height() / 2f : Math.max(margin, Math.min(height() - margin, requestY));
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.MOVE) {
            PointerEvent p = (PointerEvent) event;
            pointAt(p.x(), p.y());
        } else if (event.type() == PointerEvent.PRESS) {
            PointerEvent p = (PointerEvent) event;
            pointAt(p.x(), p.y());
            if (p.button() == Keys.MOUSE_RIGHT) {
                back();
            } else if (p.button() == Keys.MOUSE_LEFT) {
                if (hot >= 0) {
                    activate(level.get(hot));
                } else if (centerHot) {
                    back();
                } else {
                    close();
                }
            }
            event.consume();
        } else if (event.type() == PointerEvent.CLICK || event.type() == PointerEvent.RELEASE) {
            event.consume();
        } else if (event.type() == KeyEvent.PRESS) {
            if (keyPressed(((KeyEvent) event).key())) {
                event.consume();
            }
        } else if (event.type() == KeyEvent.RELEASE) {
            int key = ((KeyEvent) event).key();
            if (key == holdKey) {
                holdKey = -1;
                Item item = hoveredItem();
                if (item != null && item.isEnabled()) {
                    activate(item);
                } else {
                    close();
                }
                event.consume();
            }
        }
    }

    private boolean keyPressed(int key) {
        int n = level.size();
        if (key == holdKey) {
            return true; // автоповтор зажатой клавиши
        }
        switch (key) {
            case Keys.RIGHT, Keys.DOWN -> {
                if (n > 0) {
                    setHot(hot < 0 ? 0 : (hot + 1) % n);
                }
            }
            case Keys.LEFT, Keys.UP -> {
                if (n > 0) {
                    setHot(hot < 0 ? n - 1 : Math.floorMod(hot - 1, n));
                }
            }
            case Keys.ENTER, Keys.KP_ENTER, Keys.SPACE -> activate(hoveredItem());
            case Keys.BACKSPACE -> back();
            case Keys.ESCAPE -> {
                if (trail.isEmpty()) {
                    return false; // корень закроет верхнее окно сам
                }
                back();
            }
            default -> {
                int digit = key - '1';
                if (digit >= 0 && digit < 9 && digit < n) {
                    setHot(digit);
                    activate(level.get(digit));
                    return true;
                }
                return false;
            }
        }
        return true;
    }

    // ================================================================ отрисовка

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        int n = level.size();
        if (n == 0) {
            return;
        }
        TextMetrics m = canvas.textMetrics();
        float t = appear != null ? appear.get() : 1f;
        float grow = 0.7f + 0.3f * t;
        float r0 = innerRadius * grow;
        float r1 = outerRadius * grow;
        int wedge = style.get(WEDGE_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        int text = style.get(Styles.TEXT_COLOR);
        int onAccent = Color.luminance(accent) > 0.5f ? 0xFF1D2330 : Color.WHITE;
        float slice = 360f / n;
        for (int i = 0; i < n; i++) {
            Item item = level.get(i);
            float h = i < heat.length ? heat[i].get() : 0f;
            float outer = r1 + POP_OUT * h;
            float a0 = i * slice - slice / 2f;
            float a1 = a0 + slice;
            int fill = Color.lerp(wedge, Color.withAlpha(accent, Color.alpha(wedge)), h);
            if (!item.isEnabled()) {
                fill = Color.mulAlpha(fill, 0.5f);
            }
            paintWedge(canvas, r0 + 2f, outer, a0, a1, fill);

            float mid = (float) Math.toRadians(i * slice);
            float rm = (r0 + outer) / 2f + 1f;
            float ix = cx + rm * (float) Math.sin(mid);
            float iy = cy - rm * (float) Math.cos(mid);
            int fg = Color.lerp(text, onAccent, h);
            if (!item.isEnabled()) {
                fg = Color.mulAlpha(fg, 0.45f);
            }
            if (item.itemId != null) {
                canvas.drawItem(item.itemId, ix - ICON / 2f, iy - ICON / 2f, ICON);
            } else if (item.texture != null) {
                canvas.drawTexture(item.texture, ix - ICON / 2f, iy - ICON / 2f, ICON, ICON, fg);
            } else {
                float chord = 2f * rm * (float) Math.sin(Math.toRadians(Math.min(slice, 180f) / 2f)) - 6f;
                String s = Select.ellipsize(item.label(), Math.max(12f, chord), m);
                canvas.drawText(s, ix - m.width(s) / 2f, iy - m.lineHeight() / 2f + 0.5f, fg, false);
            }
            if (item.hasSubmenu()) {
                // Маленький треугольник у внешнего края: «здесь вложенное меню».
                float ro = outer - 5f;
                float d = (float) Math.toRadians(4f);
                float tx = cx + ro * (float) Math.sin(mid);
                float ty = cy - ro * (float) Math.cos(mid);
                float bx0 = cx + (ro - 4f) * (float) Math.sin(mid - d);
                float by0 = cy - (ro - 4f) * (float) Math.cos(mid - d);
                float bx1 = cx + (ro - 4f) * (float) Math.sin(mid + d);
                float by1 = cy - (ro - 4f) * (float) Math.cos(mid + d);
                canvas.fillTriangle(tx, ty, bx0, by0, bx1, by1, Color.mulAlpha(fg, 0.8f));
            }
        }

        // Центр: подпись наведённого пункта, «Назад» во вложенном меню или заголовок.
        float rc = r0 - 1f;
        canvas.fillCircle(cx, cy, rc, Brush.solid(centerHot && hot < 0 ? Color.lerp(wedge, accent, 0.35f) : wedge));
        Item over = hoveredItem();
        String label;
        if (over != null) {
            label = over.label();
        } else if (!trail.isEmpty()) {
            label = centerHot ? "Назад" : titles.peek();
        } else {
            label = title;
        }
        if (label != null && !label.isEmpty()) {
            float lw = m.width(label);
            float ly = cy - m.lineHeight() / 2f + 0.5f;
            if (lw > 2f * rc - 6f) {
                canvas.fillRoundRect(cx - lw / 2f - 5f, ly - 3f, lw + 10f, m.lineHeight() + 5f,
                        Radii.all(4f), Brush.solid(wedge));
            }
            canvas.drawText(label, cx - lw / 2f, ly, text, false);
        }
    }

    /** Сектор кольца из выпуклых четырёхугольников; зазор между секторами одинаков в пикселях. */
    private void paintWedge(Canvas canvas, float r0, float r1, float a0, float a1, int color) {
        float gi = (float) Math.toDegrees(GAP / 2f / r0);
        float go = (float) Math.toDegrees(GAP / 2f / r1);
        float i0 = a0 + gi;
        float i1 = a1 - gi;
        float o0 = a0 + go;
        float o1 = a1 - go;
        int seg = Math.max(1, (int) Math.ceil((a1 - a0) / SEGMENT_DEG));
        float pix = 0f;
        float piy = 0f;
        float pox = 0f;
        float poy = 0f;
        for (int k = 0; k <= seg; k++) {
            float f = (float) k / seg;
            double ai = Math.toRadians(i0 + (i1 - i0) * f);
            double ao = Math.toRadians(o0 + (o1 - o0) * f);
            float ix = cx + r0 * (float) Math.sin(ai);
            float iy = cy - r0 * (float) Math.cos(ai);
            float ox = cx + r1 * (float) Math.sin(ao);
            float oy = cy - r1 * (float) Math.cos(ao);
            if (k > 0) {
                canvas.fillQuad(pix, piy, pox, poy, ox, oy, ix, iy, color, color, color, color);
            }
            pix = ix;
            piy = iy;
            pox = ox;
            poy = oy;
        }
    }
}
