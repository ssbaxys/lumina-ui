package io.github.ssbaxys.lumina.hud;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.game.GameData;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.Layout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.style.Theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Элементы Lumina поверх игрового экрана (HUD). Загрузчик рисует общий {@link #get()} каждый кадр в событии
 * отрисовки интерфейса игры и вызывает {@link #clientTick()} на каждом тике клиента. Мышь и клавиатура
 * в HUD не попадают: он только рисуется.
 *
 * <pre>{@code
 * Hud hud = Hud.get();
 * hud.add(new SegmentedBar().bind(GameData.health()).bindMax(GameData.maxHealth()), HudAnchor.BOTTOM_LEFT)
 *         .offset(8, 8)
 *         .visibility(HudVisibility.GAMEPLAY);
 * hud.toasts().show(Toast.success("Готово", "Мир сохранён").icon("minecraft:writable_book"));
 * hud.feed().post("Игрок вошёл в игру");
 * }</pre>
 *
 * <p>Каждый элемент привязывается к углу, середине края или центру экрана ({@link HudAnchor}) со смещением,
 * лежит в своём слое ({@link HudEntry#layer}) и показывается по правилу ({@link HudVisibility}): например,
 * прячется по F1, на экране отладки или в чате. Внутри — обычный {@link UiRoot} со своей темой, анимациями
 * и раскладкой; пустой HUD ничего не стоит — загрузчик даже не начинает кадр.</p>
 */
public final class Hud {
    private static Hud global;

    private final Layer layer = new Layer();
    private final UiRoot root;
    private final List<HudEntry> entries = new ArrayList<>();
    private final List<HudEntry> entriesView = Collections.unmodifiableList(entries);
    private final List<Runnable> tickListeners = new ArrayList<>();
    private HudVisibility defaultVisibility = HudVisibility.DEFAULT;
    private ToastStack toasts;
    private NotificationFeed feed;
    private int lastState;

    public Hud() {
        this(Theme.dark());
    }

    public Hud(Theme theme) {
        root = new UiRoot(layer, theme);
    }

    /** Общий HUD, который рисует загрузчик. */
    public static Hud get() {
        Hud h = global;
        if (h == null) {
            h = new Hud();
            global = h;
        }
        return h;
    }

    // ================================================================ элементы

    /** Поставить элемент в угол / на край / в центр экрана без смещения. */
    public HudEntry add(UiElement element, HudAnchor anchor) {
        return add(element, anchor, 0f, 0f);
    }

    /**
     * Поставить элемент на HUD. Смещение — внутрь экрана от привязанного края ({@link HudAnchor}).
     * Уже добавленный элемент просто получает новую привязку и смещение.
     */
    public HudEntry add(UiElement element, HudAnchor anchor, float dx, float dy) {
        HudEntry existing = entry(element);
        if (existing != null) {
            return existing.anchor(anchor).offset(dx, dy);
        }
        HudEntry e = new HudEntry(this, element, anchor, dx, dy);
        int index = insertionIndex(e.layer());
        entries.add(index, e);
        layer.addChild(index, e.slot);
        return e;
    }

    /** Запись элемента или {@code null}, если его нет на HUD. */
    public HudEntry entry(UiElement element) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).element() == element) {
                return entries.get(i);
            }
        }
        return null;
    }

    /** Убрать элемент; {@code false}, если его не было. */
    public boolean remove(UiElement element) {
        for (int i = 0; i < entries.size(); i++) {
            HudEntry e = entries.get(i);
            if (e.element() == element) {
                entries.remove(i);
                layer.removeChild(e.slot);
                e.detached();
                if (element == toasts) {
                    toasts = null;
                }
                if (element == feed) {
                    feed = null;
                }
                return true;
            }
        }
        return false;
    }

    /** Убрать всё (встроенные тосты и лента создадутся заново при следующем обращении). */
    public void clear() {
        while (!entries.isEmpty()) {
            remove(entries.get(entries.size() - 1).element());
        }
    }

    /** Записи снизу вверх (в порядке отрисовки). */
    public List<HudEntry> entries() {
        return entriesView;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /** Первая запись с большим слоем: равные слои рисуются в порядке добавления. */
    private int insertionIndex(int layerIndex) {
        int i = 0;
        while (i < entries.size() && entries.get(i).layer() <= layerIndex) {
            i++;
        }
        return i;
    }

    void reorder(HudEntry e) {
        int at = entries.indexOf(e);
        if (at < 0) {
            return;
        }
        entries.remove(at);
        int index = insertionIndex(e.layer());
        entries.add(index, e);
        if (index != at) {
            layer.removeChild(e.slot);
            layer.addChild(index, e.slot);
        }
    }

    // ================================================================ встроенные элементы

    /** Стопка тостов: по умолчанию в правом верхнем углу, поверх остального, прячется только по F1. */
    public ToastStack toasts() {
        if (toasts == null) {
            toasts = new ToastStack();
            add(toasts, HudAnchor.TOP_RIGHT, 4f, 4f)
                    .layer(1000)
                    .visibility(HudVisibility.ALWAYS.hideWithGui(true));
        }
        return toasts;
    }

    /** Показать тост в общей стопке. */
    public Toast toast(String title, String body) {
        return toasts().show(title, body);
    }

    /** Лента коротких уведомлений: по умолчанию слева посередине. */
    public NotificationFeed feed() {
        if (feed == null) {
            feed = new NotificationFeed();
            add(feed, HudAnchor.CENTER_LEFT, 4f, 0f).layer(900);
        }
        return feed;
    }

    // ================================================================ настройка

    public UiRoot root() {
        return root;
    }

    public Theme theme() {
        return root.theme();
    }

    public void setTheme(Theme theme) {
        root.setTheme(theme);
    }

    /** Масштаб HUD поверх масштаба интерфейса игры (0.25–4). */
    public void setScale(float scale) {
        root.setScale(scale);
    }

    public HudVisibility defaultVisibility() {
        return defaultVisibility;
    }

    /** Правило для записей без своего ({@link HudEntry#visibility}); по умолчанию {@link HudVisibility#DEFAULT}. */
    public void setDefaultVisibility(HudVisibility visibility) {
        this.defaultVisibility = visibility == null ? HudVisibility.DEFAULT : visibility;
    }

    /** Действие на каждом тике клиента (20 раз в секунду), после обновления {@link GameData}. */
    public Subscription onClientTick(Runnable listener) {
        tickListeners.add(listener);
        return () -> tickListeners.remove(listener);
    }

    // ================================================================ кадр и тик (вызывает загрузчик)

    /** Тик клиента: обновить {@link GameData} и вызвать слушателей {@link #onClientTick}. */
    public void clientTick() {
        GameData.update();
        for (int i = 0; i < tickListeners.size(); i++) {
            tickListeners.get(i).run();
        }
    }

    /**
     * Нарисовать HUD на экране {@code width × height} (GUI-координаты игры). Состояние для правил видимости
     * берётся у платформы ({@code Platform.hudState()}).
     */
    public void render(Canvas canvas, float width, float height, float partialTick) {
        render(canvas, width, height, partialTick, Lumina.isInitialized() ? Lumina.platform().hudState() : 0);
    }

    /** То же с явным состоянием игры (маска флагов {@link HudVisibility}). */
    public void render(Canvas canvas, float width, float height, float partialTick, int hudState) {
        if (entries.isEmpty() || !updateVisibility(hudState)) {
            return;
        }
        root.resize(width, height);
        root.render(canvas, partialTick);
    }

    /**
     * Без отрисовки: видимость по состоянию, анимации на {@code dt} секунд и раскладка (для тестов).
     *
     * @return виден ли хоть один элемент
     */
    public boolean tick(float width, float height, float dt, int hudState) {
        boolean any = !entries.isEmpty() && updateVisibility(hudState);
        root.resize(width, height);
        root.tick(dt);
        return any;
    }

    /** Применить правила видимости; {@code true}, если что-то видно. */
    boolean updateVisibility(int hudState) {
        lastState = hudState;
        boolean any = false;
        for (int i = 0, n = entries.size(); i < n; i++) {
            HudEntry e = entries.get(i);
            HudVisibility v = e.visibility() != null ? e.visibility() : defaultVisibility;
            boolean show = v.test(hudState);
            if (show != e.slot.isVisible()) {
                e.slot.setVisible(show);
            }
            any |= show;
        }
        return any;
    }

    /** Состояние игры из последнего кадра (маска флагов {@link HudVisibility}). */
    public int lastHudState() {
        return lastState;
    }

    // ================================================================ раскладка по привязкам

    /** Корневой слой: занимает весь экран, детей расставляет по привязкам записей. */
    private static final class Layer extends UiElement {
        Layer() {
            super(null);
            layoutParams().width = Dim.FILL;
            layoutParams().height = Dim.FILL;
            setHitTestable(false);
            setLayout(new AnchorLayout());
        }
    }

    private static final class AnchorLayout implements Layout {
        @Override
        public void measure(UiElement container, float maxWidth, float maxHeight) {
            List<UiElement> children = container.children();
            for (int i = 0, n = children.size(); i < n; i++) {
                UiElement c = children.get(i);
                if (c.isVisible()) {
                    c.measure(maxWidth, maxHeight);
                }
            }
            container.setContentSize(Float.isFinite(maxWidth) ? maxWidth : 0f, Float.isFinite(maxHeight) ? maxHeight : 0f);
        }

        @Override
        public void arrange(UiElement container, float cx, float cy, float cw, float ch) {
            List<UiElement> children = container.children();
            for (int i = 0, n = children.size(); i < n; i++) {
                UiElement c = children.get(i);
                if (!c.isVisible() || !(c instanceof HudEntry.Slot slot)) {
                    continue;
                }
                HudEntry e = slot.entry;
                c.measure(cw, ch);
                float w = c.measuredWidth();
                float h = c.measuredHeight();
                // Целые пиксели: текст по центру экрана не размывается.
                float x = Math.round(cx + e.anchor().x(cw, w, e.offsetX()));
                float y = Math.round(cy + e.anchor().y(ch, h, e.offsetY()));
                c.setBounds(x, y, w, h);
            }
        }
    }
}
