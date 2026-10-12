package io.github.ssbaxys.lumina.inventory;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.event.EventType;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.widget.Tooltip;
import io.github.ssbaxys.lumina.widget.Tooltips;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Слот инвентаря: показывает ячейку {@link Inventory} — предмет, количество, полоску прочности, перелив
 * зачарования — и даёт перетаскивать стопки мышью (правила — в {@link SlotGroup}).
 *
 * <pre>{@code
 * Inventory chest = new Inventory(27);
 * Slot slot = new Slot(chest, 0).look(Slot.Look.MODERN);
 * Slot helmet = new Slot(armor, 0).placeholder("minecraft:iron_helmet");     // силуэт в пустом слоте
 * Slot single = new Slot(Stack.of("minecraft:diamond", 12));                  // свой инвентарь на одну ячейку
 * slot.quickMoveTo(playerInventory);                                          // куда уходит Shift+клик
 * }</pre>
 *
 * Вид — {@link Look}: {@code VANILLA} — серая вдавленная ячейка 18×18, как в сундуке, и белая подсветка при
 * наведении; {@code MODERN} — скруглённая ячейка темы (токены {@code well}, {@code border}, {@code accent};
 * фон, радиус и рамку можно задать стилем). Пока над слотом несут стопку, он подсвечивается: обычной
 * подсветкой, если её можно положить, и красной — если нельзя.
 *
 * <p>ЛКМ-перетаскивание берёт стопку целиком, ПКМ — половину; Shift+клик — быстрый перенос. Подсказка
 * (название, описание, прочность) показывается у непустого слота и прячется, пока стопку несут.</p>
 */
public class Slot extends Widget<Slot> {
    /** Оформление слота. */
    public enum Look {
        /** Как в игре: серая ячейка с тёмной тенью сверху-слева и светлой кромкой снизу-справа. */
        VANILLA,
        /** Скруглённая ячейка темы с мягкой подсветкой акцентом. */
        MODERN
    }

    /** Куда переносит Shift+клик: ячейки {@code from} (включительно) .. {@code to} (исключительно). */
    record Target(Inventory inventory, int from, int to) {
    }

    /** Размер ванильного слота. */
    public static final float SIZE = 18f;
    /** На сколько пикселей нужно сдвинуть мышь с зажатой кнопкой, чтобы начать перетаскивание. */
    public static final float DRAG_THRESHOLD = 3f;

    private static final String[] LABELS = new String[100];

    static {
        for (int i = 2; i < LABELS.length; i++) {
            LABELS[i] = Integer.toString(i);
        }
        LABELS[0] = "";
        LABELS[1] = "";
    }

    private Inventory inventory;
    private int index;
    private Look look = Look.VANILLA;
    private SlotGroup group;
    private SlotGroup registeredIn;
    private final List<Target> quickTargets = new ArrayList<>(0);
    private boolean takeable = true;
    private boolean dropTarget = true;
    private boolean tooltips = true;
    private Tooltip.Kind tooltipKind;
    private String placeholder;

    private Subscription subscription;
    private boolean tooltipAttached;
    private boolean armed;
    private int armedButton;
    private float pressX;
    private float pressY;

    /** Слот со своим инвентарём на одну пустую ячейку. */
    public Slot() {
        this(new Inventory(1), 0);
    }

    /** Слот со своим инвентарём на одну ячейку с этой стопкой. */
    public Slot(Stack stack) {
        this(Inventory.of(stack), 0);
    }

    /** Слот, показывающий ячейку {@code index} инвентаря. */
    public Slot(Inventory inventory, int index) {
        super("slot");
        if (index < 0 || index >= inventory.size()) {
            throw new IndexOutOfBoundsException("slot " + index + " of " + inventory.size());
        }
        this.inventory = inventory;
        this.index = index;
        size(SIZE, SIZE);
    }

    // ================================================================ данные

    public Inventory inventory() {
        return inventory;
    }

    public int index() {
        return index;
    }

    /** Стопка в ячейке. */
    public Stack stack() {
        return inventory.get(index);
    }

    /** Положить стопку в ячейку (через модель: подписчики инвентаря узнают об этом). */
    public Slot stack(Stack stack) {
        inventory.set(index, stack);
        return this;
    }

    /** Показывать другую ячейку. */
    public Slot bind(Inventory inventory, int index) {
        if (index < 0 || index >= inventory.size()) {
            throw new IndexOutOfBoundsException("slot " + index + " of " + inventory.size());
        }
        boolean attached = root() != null;
        if (attached) {
            unsubscribe();
        }
        this.inventory = inventory;
        this.index = index;
        if (attached) {
            subscribe();
        }
        refreshTooltip();
        return this;
    }

    // ================================================================ настройка

    public Look look() {
        return look;
    }

    public Slot look(Look look) {
        this.look = look == null ? Look.VANILLA : look;
        return this;
    }

    /** Группа перетаскивания ({@code null} — {@link SlotGroup#shared() общая}). */
    public SlotGroup group() {
        return group != null ? group : SlotGroup.shared();
    }

    public Slot group(SlotGroup group) {
        this.group = group;
        if (registeredIn != null && registeredIn != group()) {
            registeredIn.unregister(this);
            registeredIn = group();
            registeredIn.register(this);
        }
        return this;
    }

    /** Добавить инвентарь, куда целиком уходит Shift+клик (по порядку добавления). */
    public Slot quickMoveTo(Inventory target) {
        return quickMoveTo(target, 0, target.size());
    }

    /** Добавить диапазон ячеек {@code from..to-1}, куда уходит Shift+клик (хотбар ↔ рюкзак в одном инвентаре). */
    public Slot quickMoveTo(Inventory target, int from, int to) {
        quickTargets.add(new Target(target, from, to));
        return this;
    }

    /** Забыть, куда уходит Shift+клик. */
    public Slot clearQuickMove() {
        quickTargets.clear();
        return this;
    }

    List<Target> quickMoveTargets() {
        return Collections.unmodifiableList(quickTargets);
    }

    /** Можно ли брать из слота (по умолчанию да). Слот-витрина: {@code takeable(false)}. */
    public Slot takeable(boolean takeable) {
        this.takeable = takeable;
        return this;
    }

    public boolean isTakeable() {
        return takeable && isEnabled();
    }

    /** Можно ли класть в слот (по умолчанию да). Слот результата крафта: {@code dropTarget(false)}. */
    public Slot dropTarget(boolean dropTarget) {
        this.dropTarget = dropTarget;
        return this;
    }

    public boolean isDropTarget() {
        return dropTarget && isEnabled();
    }

    /** Подсказка с названием и описанием предмета (по умолчанию включена). */
    public Slot tooltips(boolean on) {
        this.tooltips = on;
        refreshTooltip();
        return this;
    }

    /** Вид подсказки ({@code null} — {@link Tooltips#defaultKind()}). */
    public Slot tooltipKind(Tooltip.Kind kind) {
        this.tooltipKind = kind;
        if (tooltipAttached) {
            Tooltip t = Tooltips.of(this);
            if (t != null) {
                t.kind(kind == null ? Tooltips.defaultKind() : kind);
            }
        }
        return this;
    }

    /** Бледный силуэт предмета в пустом слоте (места брони, топливо в печи). */
    public Slot placeholder(String itemId) {
        this.placeholder = itemId;
        return this;
    }

    /** Размер иконки предмета: 16 у слота 18×18, дальше — пропорционально. */
    public float itemSize() {
        return Math.max(4f, Math.min(width() > 0f ? width() : SIZE, height() > 0f ? height() : SIZE) * (16f / 18f));
    }

    /** Что видно в слоте: остаток, если из него сейчас несут стопку. */
    public Stack displayedStack() {
        SlotGroup g = group();
        return g.source() == this ? g.remainder() : stack();
    }

    @Override
    protected String defaultAccessibleLabel() {
        Stack s = stack();
        return s.isEmpty() ? null : s.count() > 1 ? s.displayName() + " ×" + s.count() : s.displayName();
    }

    // ================================================================ жизненный цикл

    @Override
    protected void onAttach() {
        subscribe();
        registeredIn = group();
        registeredIn.register(this);
        refreshTooltip();
    }

    @Override
    protected void onDetach() {
        unsubscribe();
        if (registeredIn != null) {
            registeredIn.unregister(this);
            registeredIn = null;
        }
        armed = false;
    }

    private void subscribe() {
        unsubscribe();
        subscription = inventory.onChange((inv, i, before, after) -> {
            if (i == index && before.isEmpty() != after.isEmpty()) {
                refreshTooltip();
            }
        });
    }

    private void unsubscribe() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    /** Подсказка есть только у непустого слота и не пока несут стопку. */
    void refreshTooltip() {
        boolean want = tooltips && !stack().isEmpty() && !group().isDragging();
        if (want == tooltipAttached) {
            return;
        }
        tooltipAttached = want;
        if (want) {
            Tooltips.attach(this, () -> new ItemTooltip(stack()))
                    .kind(tooltipKind == null ? Tooltips.defaultKind() : tooltipKind);
        } else {
            Tooltips.detach(this);
        }
    }

    // ================================================================ ввод

    private static boolean shiftDown() {
        return Lumina.isInitialized() && Lumina.platform().isShiftDown();
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (!(event instanceof PointerEvent p) || !isEnabled()) {
            return;
        }
        SlotGroup g = group();
        EventType<?> type = event.type();
        if (type == PointerEvent.PRESS) {
            armed = false;
            int b = p.button();
            if ((b != Keys.MOUSE_LEFT && b != Keys.MOUSE_RIGHT) || !isTakeable() || stack().isEmpty()) {
                return;
            }
            if (b == Keys.MOUSE_LEFT && shiftDown()) {
                g.quickMove(this);
            } else {
                armed = true;
                armedButton = b;
                pressX = p.x();
                pressY = p.y();
            }
            event.consume();
        } else if (type == PointerEvent.DRAG) {
            if (g.source() == this) {
                g.dragTo(p.x(), p.y());
                event.consume();
            } else if (armed && Math.abs(p.x() - pressX) + Math.abs(p.y() - pressY) >= DRAG_THRESHOLD) {
                armed = false;
                if (g.beginDrag(this, armedButton == Keys.MOUSE_RIGHT)) {
                    g.dragTo(p.x(), p.y());
                }
                event.consume();
            }
        } else if (type == PointerEvent.RELEASE) {
            armed = false;
            if (g.source() == this) {
                UiRoot r = root();
                g.dropAt(r == null ? null : r.hovered());
                event.consume();
            }
        }
    }

    // ================================================================ рисование

    @Override
    protected void paintBackground(Canvas canvas) {
        float w = width();
        float h = height();
        if (look == Look.VANILLA) {
            canvas.fillRect(0f, 0f, w, h, 0xFF8B8B8B);
            canvas.fillRect(0f, 0f, w - 1f, 1f, 0xFF373737);
            canvas.fillRect(0f, 1f, 1f, h - 2f, 0xFF373737);
            canvas.fillRect(1f, h - 1f, w - 1f, 1f, 0xFFFFFFFF);
            canvas.fillRect(w - 1f, 1f, 1f, h - 2f, 0xFFFFFFFF);
            return;
        }
        Theme theme = root() != null ? root().theme() : null;
        Brush bg = style.get(Styles.BACKGROUND);
        if (bg == null) {
            bg = Brush.solid(theme != null ? theme.get(Tokens.WELL) : 0x30000000);
        }
        Radii r = modernRadii();
        canvas.fillRoundRect(0f, 0f, w, h, r, bg);
        float bw = style.get(Styles.BORDER_WIDTH);
        int bc = bw > 0f ? style.get(Styles.BORDER_COLOR) : theme != null ? theme.get(Tokens.BORDER) : 0x24FFFFFF;
        canvas.strokeRoundRect(0f, 0f, w, h, r, bw > 0f ? bw : 1f, bc);
    }

    private Radii modernRadii() {
        Radii r = style.get(Styles.RADIUS);
        return r.topLeft() > 0f || r.topRight() > 0f || r.bottomLeft() > 0f || r.bottomRight() > 0f ? r : Radii.all(4f);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float w = width();
        float h = height();
        float s = itemSize();
        float ix = (w - s) / 2f;
        float iy = (h - s) / 2f;
        Stack shown = displayedStack();
        if (!shown.isEmpty()) {
            paintStack(canvas, shown, ix, iy, s, labelFor(shown.count()));
        } else if (placeholder != null) {
            // Предметы не бывают полупрозрачными: силуэт гасится слоем цвета ячейки поверх.
            canvas.drawItem(placeholder, ix, iy, s);
            int veil = look == Look.VANILLA ? 0xB08B8B8B : 0xA0000000;
            canvas.fillRect(ix, iy, s, s, veil);
        }
    }

    @Override
    protected void paintOverlay(Canvas canvas, float partialTick) {
        if (!hasState(State.HOVERED) || !isEnabled()) {
            return;
        }
        SlotGroup g = group();
        boolean dragging = g.isDragging();
        if (dragging && g.source() == this) {
            return;
        }
        boolean ok = !dragging || g.wouldAccept(this);
        float w = width();
        float h = height();
        if (look == Look.VANILLA) {
            canvas.fillRect(1f, 1f, w - 2f, h - 2f, ok ? 0x80FFFFFF : 0x60FF4040);
            return;
        }
        Theme theme = root() != null ? root().theme() : null;
        int accent = ok ? (theme != null ? theme.get(Tokens.ACCENT) : 0xFF5B8CFF) : 0xFFF0506E;
        Radii r = modernRadii();
        canvas.fillRoundRect(0f, 0f, w, h, r, Brush.solid(Color.withAlpha(accent, 0x38)));
        canvas.strokeRoundRect(0f, 0f, w, h, r, 1f, Color.withAlpha(accent, 0xD0));
    }

    /** Подпись количества без лишних строк в кадре: до 99 — из таблицы. */
    static String labelFor(int count) {
        return count >= 0 && count < LABELS.length ? LABELS[count] : Stack.countLabel(count);
    }

    /**
     * Нарисовать стопку как в слоте: предмет {@code size × size} в (x, y), полоска прочности и количество
     * в правом нижнем углу. Пригодится для своих виджетов.
     */
    public static void paintStack(Canvas canvas, Stack stack, float x, float y, float size) {
        paintStack(canvas, stack, x, y, size, labelFor(stack.count()));
    }

    static void paintStack(Canvas canvas, Stack stack, float x, float y, float size, String label) {
        if (stack.isEmpty()) {
            return;
        }
        canvas.drawItem(stack.itemId(), x, y, size, stack.hasGlint());
        float k = size / 16f;
        if (stack.isDamaged()) {
            float d = stack.durability();
            float bx = x + 2f * k;
            float by = y + 13f * k;
            canvas.fillRect(bx, by, 13f * k, 2f * k, 0xFF000000);
            canvas.fillRect(bx, by, Math.round(13f * d) * k, k, durabilityColor(d));
        }
        if (!label.isEmpty()) {
            TextMetrics m = canvas.textMetrics();
            canvas.drawText(label, x + size + 1f - m.width(label), y + size - 7f, 0xFFFFFFFF, true);
        }
    }

    /** Цвет полоски прочности как в игре: от зелёного (целый) к красному. */
    static int durabilityColor(float durability) {
        float h6 = Math.max(0f, Math.min(1f, durability)) * 2f;
        int r = h6 < 1f ? 255 : Math.round((2f - h6) * 255f);
        int g = h6 < 1f ? Math.round(h6 * 255f) : 255;
        return 0xFF000000 | (r << 16) | (g << 8);
    }
}
