package io.github.ssbaxys.lumina.markdown;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Многострочный текст в разметке Markdown: заголовки, **жирный**, *курсив*, ~~зачёркнутый~~,
 * ++подчёркнутый++, ==выделение==, `код`, блоки кода, цитаты, вложенные списки, задачи «- [x]»,
 * таблицы с выравниванием, ссылки, разделители и иконки {@code ![](item:minecraft:diamond)}.
 * Блоки кода с языком ({@code ```java}, json, css, html…) подсвечиваются ({@link SyntaxHighlighter}).
 *
 * <p>Текст разбирается один раз при изменении, раскладка пересчитывается только при смене ширины.
 * Цвета берутся из стиля ({@link Styles#TEXT_COLOR}, {@link Styles#ACCENT_COLOR}) в момент рисования,
 * поэтому смена темы не требует перераскладки.</p>
 */
public class Markdown extends Widget<Markdown> {
    private static final float UNBOUNDED_WIDTH = 320f;

    private String source = "";
    private List<Md.Block> blocks = List.of();
    private MarkdownLayout layout;
    private float layoutWidth = Float.NaN;
    private int hoveredLink = -1;
    private Consumer<String> onLink;
    private BiConsumer<Md.ListItem, Boolean> onTask;
    private ReadableSignal<String> binding;
    private Subscription subscription;
    private int[] cachedPalette;
    private int paletteText;
    private int paletteAccent;
    private int paletteRevision;

    public Markdown(String source) {
        super("markdown");
        layoutParams().width = Dim.FILL;
        text(source);
    }

    public static Markdown bound(ReadableSignal<String> signal) {
        return new Markdown(signal.get()).bind(signal);
    }

    public String text() {
        return source;
    }

    public Markdown text(String source) {
        String s = source == null ? "" : source;
        if (!Objects.equals(this.source, s)) {
            this.source = s;
            this.blocks = MarkdownParser.parse(s);
            this.layout = null;
            this.hoveredLink = -1;
            invalidateLayout();
        }
        return this;
    }

    /** Разобранное дерево (например, чтобы прочитать отметки задач). */
    public List<Md.Block> document() {
        return blocks;
    }

    /** Клик по ссылке: получает URL как написан в тексте. */
    public Markdown onLink(Consumer<String> handler) {
        this.onLink = handler;
        return this;
    }

    /** Переключение задачи «- [ ]» кликом. Без обработчика задачи всё равно переключаются. */
    public Markdown onTaskToggle(BiConsumer<Md.ListItem, Boolean> handler) {
        this.onTask = handler;
        return this;
    }

    public Markdown bind(ReadableSignal<String> signal) {
        unbind();
        this.binding = signal;
        if (root() != null) {
            subscribe();
        }
        return this;
    }

    private void subscribe() {
        text(binding.get());
        subscription = binding.subscribe(this::text);
    }

    private void unbind() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    @Override
    protected void onAttach() {
        if (binding != null && subscription == null) {
            subscribe();
        }
    }

    @Override
    protected void onDetach() {
        unbind();
        hoveredLink = -1;
    }

    // ================================================================ раскладка

    private MarkdownLayout layoutFor(float width) {
        if (layout == null || Float.compare(width, layoutWidth) != 0) {
            layout = MarkdownLayout.build(blocks, width, Lumina.platform().textMetrics());
            layoutWidth = width;
        }
        return layout;
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        float w = Float.isInfinite(maxWidth) || maxWidth <= 0f ? UNBOUNDED_WIDTH : maxWidth;
        MarkdownLayout l = layoutFor(w);
        setContentSize(layoutParams().width == Dim.FILL ? w : l.width, l.height);
    }

    @Override
    protected void onLayout() {
        float w = Math.max(1f, width() - padding().horizontal());
        if (layout == null || Float.compare(w, layoutWidth) != 0) {
            layoutFor(w);
        }
    }

    // ================================================================ рисование

    /** Палитра кэшируется, пока не сменились цвета стиля или тема. */
    private int[] palette() {
        int text = style.get(Styles.TEXT_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        Theme theme = root() != null ? root().theme() : null;
        int revision = theme != null ? theme.revision() : 0;
        if (cachedPalette == null || paletteText != text || paletteAccent != accent || paletteRevision != revision) {
            cachedPalette = palette(text, accent, theme);
            paletteText = text;
            paletteAccent = accent;
            paletteRevision = revision;
        }
        return cachedPalette;
    }

    /** Цвета ролей раскладки из цвета текста и акцента (и токенов подсветки кода из темы). */
    static int[] palette(int text, int accent, Theme theme) {
        int[] c = new int[MarkdownLayout.ROLES];
        c[MarkdownLayout.TEXT] = text;
        c[MarkdownLayout.MUTED] = Color.mulAlpha(text, 0.68f);
        c[MarkdownLayout.LINK] = Color.lighten(accent, 0.08f);
        c[MarkdownLayout.CODE] = Color.mix(text, 0xFFFFB86C, 0.45f);
        c[MarkdownLayout.CODE_BG] = Color.withAlpha(Color.mix(text, Color.BLACK, 0.5f), 0x2C);
        c[MarkdownLayout.QUOTE_BAR] = Color.withAlpha(accent, 0xB0);
        c[MarkdownLayout.LINE] = Color.withAlpha(text, 0x30);
        c[MarkdownLayout.HEAD_BG] = Color.withAlpha(text, 0x12);
        c[MarkdownLayout.HIGHLIGHT] = 0x66FFD84D;
        c[MarkdownLayout.ACCENT] = accent;
        int[] syntax = SyntaxHighlighter.palette(text, accent, theme);
        System.arraycopy(syntax, 0, c, MarkdownLayout.SYNTAX, SyntaxHighlighter.KINDS);
        return c;
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        MarkdownLayout l = layout;
        if (l == null) {
            return;
        }
        canvas.save();
        canvas.translate(padding().left(), padding().top());
        l.paint(canvas, palette(), style.get(Styles.TEXT_SHADOW), hoveredLink);
        canvas.restore();
    }

    // ================================================================ мышь

    @Override
    protected void handleEvent(UiEvent event) {
        if (!(event instanceof PointerEvent pe) || layout == null) {
            return;
        }
        float lx = pe.localX() - padding().left();
        float ly = pe.localY() - padding().top();
        if (event.type() == PointerEvent.MOVE) {
            hoveredLink = linkAt(lx, ly);
        } else if (event.type() == PointerEvent.LEAVE) {
            hoveredLink = -1;
        } else if (event.type() == PointerEvent.CLICK && pe.button() == Keys.MOUSE_LEFT) {
            MarkdownLayout.Check check = checkAt(lx, ly);
            if (check != null) {
                boolean now = !Boolean.TRUE.equals(check.item().checked());
                check.item().setChecked(now);
                Lumina.platform().playSound("minecraft:ui.button.click", 0.25f, now ? 1.2f : 0.9f);
                if (onTask != null) {
                    onTask.accept(check.item(), now);
                }
                event.consume();
                return;
            }
            int link = linkAt(lx, ly);
            if (link >= 0 && onLink != null) {
                onLink.accept(layout.links.get(link));
                event.consume();
            }
        }
    }

    private int linkAt(float x, float y) {
        return layout.linkAt(x, y);
    }

    private MarkdownLayout.Check checkAt(float x, float y) {
        for (MarkdownLayout.Check c : layout.checks) {
            if (x >= c.x() - 1f && x < c.x() + c.size() + 1f && y >= c.y() - 1f && y < c.y() + c.size() + 1f) {
                return c;
            }
        }
        return null;
    }
}
