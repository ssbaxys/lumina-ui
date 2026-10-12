package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.event.EventListener;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markdown.Markdown;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Всплывающая подсказка элемента: появляется после задержки наведения у курсора, следует за мышью
 * и не выходит за экран. Содержимое — многострочный текст ({@code \n}), Markdown или любой элемент.
 * Показывается пассивным оверлеем {@link UiRoot#showOverlay}: мышь и клавиши проходят сквозь неё.
 *
 * <pre>{@code
 * button.tooltip("Сохранить мир\nCtrl+S");                       // через Widget
 * Tooltips.attach(slot, () -> new ItemCard(stack)).kind(Tooltip.Kind.MODERN).follow(false);
 * Tooltips.attachMarkdown(icon, "**Алмаз**\n\nРедкая руда").delay(0.2f);
 * }</pre>
 *
 * Вид — {@link Kind}: {@code VANILLA} — тёмная рамка с фиолетовой каймой, как у подсказок игры
 * (стиль "tooltip-vanilla"), {@code MODERN} — карточка темы со скруглением и тенью (стиль "tooltip").
 * Нажатие мыши прячет подсказку до следующего движения; одновременно видна одна подсказка — самого
 * глубокого наведённого элемента.
 */
public final class Tooltip {
    /** Оформление подсказки. */
    public enum Kind {
        /** Как в игре: почти чёрный фон, фиолетовая кайма, срезанные углы, текст с тенью. */
        VANILLA,
        /** Карточка темы: скругление, тонкая рамка, тень, плавное появление. */
        MODERN
    }

    private static final int IDLE = 0;
    private static final int WAITING = 1;
    private static final int SHOWN = 2;
    private static final int SUPPRESSED = 3;

    private final WeakReference<UiElement> targetRef;
    private final EventListener<PointerEvent> enter = e -> onEnter();
    private final EventListener<PointerEvent> leave = e -> onLeave();
    private final EventListener<PointerEvent> press = e -> onPress();
    private final EventListener<PointerEvent> move = e -> onMove(e.x(), e.y());
    private final Ticker ticker = new Ticker();

    private String text = "";
    private String markdown;
    private Supplier<? extends UiElement> factory;
    private UiElement fixedContent;
    private Kind kind;
    private float delay;
    private boolean follow = true;
    private float maxWidth = 200f;

    private int phase = IDLE;
    private boolean pinned;
    private float waited;
    private float pressX;
    private float pressY;
    private Box box;

    Tooltip(UiElement target) {
        this.targetRef = new WeakReference<>(target);
        this.kind = Tooltips.defaultKind();
        this.delay = Tooltips.defaultDelay();
        target.on(PointerEvent.ENTER, enter);
        target.on(PointerEvent.LEAVE, leave);
        target.on(PointerEvent.PRESS, press);
        target.on(PointerEvent.MOVE, move);
    }

    /** Снять слушатели с элемента (вызывает {@link Tooltips#detach}). */
    void unhook() {
        hide();
        UiElement t = targetRef.get();
        if (t != null) {
            t.off(PointerEvent.ENTER, enter);
            t.off(PointerEvent.LEAVE, leave);
            t.off(PointerEvent.PRESS, press);
            t.off(PointerEvent.MOVE, move);
        }
    }

    public UiElement target() {
        return targetRef.get();
    }

    // ================================================================ содержимое и настройка

    /** Текст подсказки; строки — через {@code \n}, длинные строки переносятся по {@link #maxWidth}. */
    public Tooltip text(String text) {
        this.text = text == null ? "" : text;
        this.markdown = null;
        this.factory = null;
        this.fixedContent = null;
        rebuild();
        return this;
    }

    public String text() {
        return text;
    }

    /** Содержимое — Markdown (заголовки, списки, иконки предметов {@code ![](item:minecraft:diamond)}). */
    public Tooltip markdown(String source) {
        this.markdown = source == null ? "" : source;
        this.factory = null;
        this.fixedContent = null;
        rebuild();
        return this;
    }

    /** Содержимое — элемент, который создаётся заново при каждом показе (свежие данные). */
    public Tooltip content(Supplier<? extends UiElement> factory) {
        this.factory = factory;
        this.markdown = null;
        this.fixedContent = null;
        rebuild();
        return this;
    }

    /** Содержимое — один и тот же элемент при каждом показе. */
    public Tooltip content(UiElement element) {
        this.fixedContent = element;
        this.factory = null;
        this.markdown = null;
        rebuild();
        return this;
    }

    public Tooltip kind(Kind kind) {
        this.kind = kind == null ? Kind.VANILLA : kind;
        rebuild();
        return this;
    }

    public Kind kind() {
        return kind;
    }

    /** Задержка наведения перед показом, секунды. */
    public Tooltip delay(float seconds) {
        this.delay = Math.max(0f, seconds);
        return this;
    }

    /**
     * {@code true} (по умолчанию) — подсказка у курсора и следует за ним; {@code false} — под элементом
     * (над ним, если внизу нет места), по центру.
     */
    public Tooltip follow(boolean follow) {
        this.follow = follow;
        return this;
    }

    /** Ширина, по которой переносится текст и раскладывается Markdown. */
    public Tooltip maxWidth(float maxWidth) {
        this.maxWidth = Math.max(20f, maxWidth);
        rebuild();
        return this;
    }

    public boolean isShowing() {
        return box != null && box.root() != null;
    }

    /** Открытый элемент подсказки (для тестов) или {@code null}. */
    public UiElement box() {
        return isShowing() ? box : null;
    }

    // ================================================================ показ

    /** Показать сразу, без задержки и без наведения (прячется только {@link #hide()}). */
    public void show() {
        UiElement t = targetRef.get();
        if (t != null && t.root() != null) {
            pinned = true;
            open(t.root());
        }
    }

    public void hide() {
        pinned = false;
        if (phase == SHOWN || phase == WAITING) {
            phase = IDLE;
        }
        close();
    }

    private void onEnter() {
        if (phase == IDLE) {
            phase = WAITING;
            waited = 0f;
            ticker.schedule();
        }
    }

    private void onLeave() {
        if (!pinned) {
            phase = IDLE;
            close();
        }
    }

    private void onPress() {
        if (pinned) {
            return;
        }
        close();
        phase = SUPPRESSED;
        UiElement t = targetRef.get();
        UiRoot r = t == null ? null : t.root();
        pressX = r == null ? 0f : r.mouseX();
        pressY = r == null ? 0f : r.mouseY();
    }

    private void onMove(float x, float y) {
        if (phase == SUPPRESSED && Math.abs(x - pressX) + Math.abs(y - pressY) > 3f) {
            phase = WAITING;
            waited = 0f;
            ticker.schedule();
        } else if (phase == IDLE) {
            onEnter();
        }
    }

    /** Наведён ли элемент глубже нашего, у которого своя подсказка (она важнее). */
    private boolean deeperTooltipHovered(UiRoot r, UiElement t) {
        for (UiElement e = r.hovered(); e != null && e != t; e = e.parent()) {
            Tooltip other = Tooltips.of(e);
            if (other != null && other != this) {
                return true;
            }
        }
        return false;
    }

    private boolean step(float dt) {
        UiElement t = targetRef.get();
        UiRoot r = t == null ? null : t.root();
        if (r == null) {
            hide();
            return false;
        }
        if (pinned) {
            if (follow && isShowing()) {
                place(r, t);
            }
            return isShowing();
        }
        if (!t.hasState(State.HOVERED) || !t.isVisible()) {
            phase = IDLE;
            close();
            return false;
        }
        if (phase == WAITING) {
            waited += dt;
            if (waited >= delay && !deeperTooltipHovered(r, t)) {
                open(r);
            }
            return true;
        }
        if (phase == SHOWN) {
            if (deeperTooltipHovered(r, t)) {
                close();
                phase = WAITING;
                waited = 0f;
            } else if (follow) {
                place(r, t);
            }
            return true;
        }
        return false;
    }

    private void open(UiRoot r) {
        UiElement t = targetRef.get();
        if (t == null) {
            return;
        }
        Tooltips.showing(this);
        phase = SHOWN;
        if (box == null || box.kind != kind) {
            box = new Box(kind);
        }
        box.fill();
        if (!r.overlays().contains(box)) {
            r.showOverlay(box, r.mouseX() + 12f, r.mouseY() - 12f);
        }
        place(r, t);
        ticker.schedule();
    }

    private void close() {
        if (box != null && box.root() != null) {
            box.root().closePopup(box);
        }
        Tooltips.hidden(this);
    }

    /** Новое содержимое: открытая подсказка перестраивается сразу. */
    private void rebuild() {
        if (box != null && (box.kind != kind)) {
            boolean showing = isShowing();
            boolean wasPinned = pinned;
            UiRoot r = showing ? box.root() : null;
            close();
            box = null;
            if (showing) {
                open(r);
                pinned = wasPinned;
            }
        } else if (box != null) {
            box.fill();
        }
    }

    /** У курсора (или под элементом) с переворотом у краёв экрана. */
    private void place(UiRoot r, UiElement t) {
        float w = box.width();
        float h = box.height();
        float sw = r.width();
        float sh = r.height();
        float x;
        float y;
        if (follow) {
            float mx = r.mouseX();
            float my = r.mouseY();
            x = mx + 12f;
            y = kind == Kind.VANILLA ? my - 12f : my + 16f;
            if (x + w > sw - 2f) {
                x = mx - 12f - w;
            }
            if (y + h > sh - 2f) {
                y = kind == Kind.VANILLA ? sh - 2f - h : my - 8f - h;
            }
        } else {
            x = t.screenX() + (t.width() - w) / 2f;
            y = t.screenY() + t.height() + 4f;
            if (y + h > sh - 2f) {
                y = t.screenY() - 4f - h;
            }
        }
        x = Math.max(2f, Math.min(x, sw - w - 2f));
        y = Math.max(2f, Math.min(y, sh - h - 2f));
        r.movePopup(box, x, y);
    }

    /** Таймер наведения и слежение за мышью: в аниматоре корня только пока элемент наведён. */
    private final class Ticker implements Animation {
        private boolean scheduled;

        void schedule() {
            UiElement t = targetRef.get();
            if (!scheduled && t != null && t.root() != null) {
                scheduled = true;
                t.root().animator().start(this);
            }
        }

        @Override
        public boolean tick(float dt) {
            boolean more = step(dt);
            if (!more) {
                scheduled = false;
            }
            return more;
        }
    }

    // ================================================================ элемент подсказки

    /** Карточка подсказки во всплывающем слое: свой текст с переносами или элемент-содержимое. */
    private final class Box extends UiElement {
        final Kind kind;
        private final List<String> lines = new ArrayList<>();
        private String wrappedText;
        private float wrappedWidth = -1f;
        private AnimatedFloat appear;

        Box(Kind kind) {
            super(kind == Kind.VANILLA ? "tooltip-vanilla" : "tooltip");
            this.kind = kind;
            setHitTestable(false);
            setOverflowsBounds(true);
        }

        /** Подставить текущее содержимое подсказки. */
        void fill() {
            UiElement content = factory != null ? factory.get() : fixedContent;
            if (content == null && markdown != null) {
                content = new Markdown(markdown).width(Dim.px(maxWidth));
            }
            if (children().size() != 1 || children().get(0) != content) {
                clearChildren();
                if (content != null) {
                    content.setHitTestable(false);
                    addChild(content);
                }
            }
            // Без детей раскладки нет: тогда размер даёт собственный текст (measureContent).
            if ((content != null) != (layout() != null)) {
                setLayout(content != null ? FlexLayout.column() : null);
            }
            wrappedText = null;
            invalidateLayout();
        }

        private void wrap(TextMetrics m) {
            if (text.equals(wrappedText) && wrappedWidth == maxWidth) {
                return;
            }
            wrappedText = text;
            wrappedWidth = maxWidth;
            lines.clear();
            for (String para : text.split("\n", -1)) {
                StringBuilder line = new StringBuilder();
                for (String word : para.split(" ")) {
                    String next = line.length() == 0 ? word : line + " " + word;
                    if (line.length() > 0 && m.width(next) > maxWidth) {
                        lines.add(line.toString());
                        line.setLength(0);
                        line.append(word);
                    } else {
                        line.setLength(0);
                        line.append(next);
                    }
                }
                lines.add(line.toString());
            }
        }

        /** Шаг строк; у ванильной подсказки после первой строки (названия) ещё 2 пикселя. */
        private float titleGap() {
            return kind == Kind.VANILLA && lines.size() > 1 ? 2f : 0f;
        }

        @Override
        protected void measureContent(float maxW, float maxH) {
            if (!children().isEmpty()) {
                return;
            }
            TextMetrics m = Lumina.platform().textMetrics();
            wrap(m);
            float w = 0f;
            for (String l : lines) {
                w = Math.max(w, m.width(l));
            }
            float step = m.lineHeight() + 1f;
            setContentSize(w, lines.size() * step - 2f + titleGap());
        }

        @Override
        protected void onAttach() {
            if (kind == Kind.MODERN) {
                appear = new AnimatedFloat(root().animator(), 0f).tween(0.12f, Easing.OUT_CUBIC).listener(v -> {
                    setAnimatedStyle(Styles.OPACITY, v);
                    setAnimatedStyle(Styles.TRANSLATE_Y, (1f - v) * 3f);
                });
                setAnimatedStyle(Styles.OPACITY, 0f);
                appear.animateTo(1f);
            }
        }

        @Override
        protected void onDetach() {
            if (appear != null) {
                appear.snapTo(0f);
                appear = null;
            }
            if (phase == SHOWN) {
                phase = IDLE;
            }
            pinned = false;
            Tooltips.hidden(Tooltip.this);
        }

        @Override
        protected void paintBackground(Canvas canvas) {
            if (kind != Kind.VANILLA) {
                super.paintBackground(canvas);
                return;
            }
            // Рамка как у подсказок игры: фон без угловых пикселей и кайма градиентом внутри.
            float w = width();
            float h = height();
            Brush bg = style.get(Styles.BACKGROUND);
            if (bg == null) {
                bg = Brush.solid(0xF0100010);
            }
            canvas.fillRect(1f, 0f, w - 2f, 1f, bg);
            canvas.fillRect(1f, h - 1f, w - 2f, 1f, bg);
            canvas.fillRect(1f, 1f, w - 2f, h - 2f, bg);
            canvas.fillRect(0f, 1f, 1f, h - 2f, bg);
            canvas.fillRect(w - 1f, 1f, 1f, h - 2f, bg);
            Brush border = style.get(Styles.BORDER_BRUSH);
            if (border == null) {
                border = Brush.vertical(0x505000FF, 0x5028007F);
            }
            canvas.fillRect(1f, 2f, 1f, h - 4f, border);
            canvas.fillRect(w - 2f, 2f, 1f, h - 4f, border);
            canvas.fillRect(1f, 1f, w - 2f, 1f, border.sample(0.5f, 0f, 1f, 1f));
            canvas.fillRect(1f, h - 2f, w - 2f, 1f, border.sample(0.5f, 1f, 1f, 1f));
        }

        @Override
        protected void paintContent(Canvas canvas, float partialTick) {
            if (!children().isEmpty()) {
                return;
            }
            TextMetrics m = canvas.textMetrics();
            int color = style.get(Styles.TEXT_COLOR);
            boolean shadow = style.get(Styles.TEXT_SHADOW);
            float x = padding().left();
            float y = padding().top();
            float step = m.lineHeight() + 1f;
            for (int i = 0; i < lines.size(); i++) {
                canvas.drawText(lines.get(i), x, y, color, shadow);
                y += step + (i == 0 ? titleGap() : 0f);
            }
        }
    }
}
