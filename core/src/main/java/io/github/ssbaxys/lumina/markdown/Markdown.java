package io.github.ssbaxys.lumina.markdown;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Многострочный текст в разметке Markdown: заголовки, **жирный**, *курсив*, ~~зачёркнутый~~,
 * ++подчёркнутый++, ==выделение==, `код`, блоки кода, цитаты, вложенные списки, задачи «- [x]»,
 * таблицы с выравниванием, ссылки, разделители и иконки {@code ![](item:minecraft:diamond)}.
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

    private int[] palette() {
        int text = style.get(Styles.TEXT_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        int[] c = new int[10];
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
        return c;
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        MarkdownLayout l = layout;
        if (l == null) {
            return;
        }
        int[] c = palette();
        boolean shadow = style.get(Styles.TEXT_SHADOW);
        canvas.save();
        canvas.translate(padding().left(), padding().top());

        for (MarkdownLayout.Box b : l.boxes) {
            if (b.radius() > 0f) {
                canvas.fillRoundRect(b.x(), b.y(), b.w(), b.h(), Radii.all(b.radius()), Brush.solid(c[b.role()]));
            } else {
                canvas.fillRect(b.x(), b.y(), b.w(), b.h(), c[b.role()]);
            }
        }
        if (hoveredLink >= 0) {
            for (MarkdownLayout.LinkArea a : l.linkAreas) {
                if (a.link() == hoveredLink) {
                    canvas.fillRoundRect(a.x() - 1f, a.y(), a.w() + 2f, a.h(), Radii.all(2f),
                            Brush.solid(Color.withAlpha(c[MarkdownLayout.ACCENT], 0x30)));
                }
            }
        }
        for (MarkdownLayout.Bullet b : l.bullets) {
            int color = c[MarkdownLayout.MUTED];
            switch (b.kind()) {
                case 0 -> canvas.fillCircle(b.cx(), b.cy(), b.r(), Brush.solid(color));
                case 1 -> canvas.strokeCircle(b.cx(), b.cy(), b.r(), 1f, Brush.solid(color));
                default -> canvas.fillRect(b.cx() - b.r(), b.cy() - b.r(), 2f * b.r(), 2f * b.r(), color);
            }
        }
        for (MarkdownLayout.Check ch : l.checks) {
            paintCheck(canvas, ch, c);
        }
        for (MarkdownLayout.Run r : l.runs) {
            int color = r.link() >= 0 && r.link() == hoveredLink ? Color.lighten(c[r.role()], 0.12f) : c[r.role()];
            if (r.scale() != 1f) {
                canvas.save();
                canvas.translate(r.x(), r.y());
                canvas.scale(r.scale(), r.scale());
                canvas.drawText(r.text(), 0f, 0f, color, shadow);
                canvas.restore();
            } else {
                canvas.drawText(r.text(), r.x(), r.y(), color, shadow);
            }
        }
        for (MarkdownLayout.Icon icon : l.icons) {
            String url = icon.url();
            if (url.startsWith("item:")) {
                canvas.drawItem(url.substring(5), icon.x(), icon.y(), icon.size());
            } else {
                canvas.drawTexture(TextureRef.of(url.substring(8)), icon.x(), icon.y(), icon.size(), icon.size(),
                        Color.WHITE);
            }
        }
        canvas.restore();
    }

    private static void paintCheck(Canvas canvas, MarkdownLayout.Check ch, int[] c) {
        float x = ch.x();
        float y = ch.y();
        float s = ch.size();
        Radii r = Radii.all(2f);
        if (Boolean.TRUE.equals(ch.item().checked())) {
            canvas.fillRoundRect(x, y, s, s, r, Brush.solid(c[MarkdownLayout.ACCENT]));
            int mark = Color.luminance(c[MarkdownLayout.ACCENT]) > 0.5f ? 0xFF1D2330 : Color.WHITE;
            canvas.drawLine(x + s * 0.22f, y + s * 0.52f, x + s * 0.42f, y + s * 0.72f, 1.4f, mark);
            canvas.drawLine(x + s * 0.42f, y + s * 0.72f, x + s * 0.8f, y + s * 0.28f, 1.4f, mark);
        } else {
            canvas.strokeRoundRect(x, y, s, s, r, 1f, c[MarkdownLayout.MUTED]);
        }
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
        for (MarkdownLayout.LinkArea a : layout.linkAreas) {
            if (x >= a.x() && x < a.x() + a.w() && y >= a.y() && y < a.y() + a.h()) {
                return a.link();
            }
        }
        return -1;
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
