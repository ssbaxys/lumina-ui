package io.github.ssbaxys.lumina.node;

import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.ComputedStyle;
import io.github.ssbaxys.lumina.style.StyleKey;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Инспектор интерфейса прямо в игре, как инструменты разработчика в браузере. Включается для корня
 * ({@code root.inspector().enabled(true)}), после чего клавиша (по умолчанию F8) открывает и закрывает его.
 * <ul>
 *   <li>элемент под мышью подсвечивается: внешние отступы (margin) — оранжевым, границы — синим,
 *       внутренние отступы (padding) — зелёным; над ним — класс и размер;</li>
 *   <li>клик выбирает элемент (интерфейс клик не получает), правый клик снимает выбор;</li>
 *   <li>панель сбоку: дерево элементов (клик по строке — выбор, колесо — прокрутка) и вычисленный стиль
 *       выбранного элемента (все значения, отличные от значений по умолчанию);</li>
 *   <li>стрелки: вверх — к родителю, вниз — к первому ребёнку, влево/вправо — к соседям; Escape — закрыть.</li>
 * </ul>
 */
public final class Inspector {
    private static final float PANEL_W = 236f;
    private static final float ROW = 10f;
    private static final float HEADER = 16f;
    private static final int MAX_ROWS = 4000;
    private static final int PANEL_BG = 0xF0141821;
    private static final int PANEL_TEXT = 0xFFE6E9F2;
    private static final int PANEL_MUTED = 0xFF8C95A8;
    private static final int ACCENT = 0xFF5B8CFF;
    private static final int MARGIN_FILL = 0x50F6A04D;
    private static final int PADDING_FILL = 0x5062C462;
    private static final int BOUNDS_FILL = 0x305B8CFF;

    /** Строка дерева: элемент и глубина. */
    public record Row(UiElement element, int depth) {
    }

    private final UiRoot root;
    private boolean enabled;
    private int toggleKey = Keys.F8;
    private boolean open;
    private boolean panelLeft;
    private UiElement hovered;
    private UiElement selected;
    private UiElement hoveredRow;
    private float treeScroll;
    private float mouseX = -1f;
    private float mouseY = -1f;

    Inspector(UiRoot root) {
        this.root = root;
    }

    // ================================================================ настройка

    /** Разрешить открывать инспектор клавишей {@link #toggleKey()}. */
    public Inspector enabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            close();
        }
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** Клавиша открытия (код GLFW, по умолчанию {@link Keys#F8}). */
    public Inspector toggleKey(int key) {
        this.toggleKey = key;
        return this;
    }

    public int toggleKey() {
        return toggleKey;
    }

    public Inspector open() {
        open = true;
        return this;
    }

    public Inspector close() {
        open = false;
        hovered = null;
        hoveredRow = null;
        return this;
    }

    public Inspector toggle() {
        return open ? close() : open();
    }

    public boolean isOpen() {
        return open;
    }

    /** Панель слева (иначе справа). */
    public Inspector panelLeft(boolean left) {
        this.panelLeft = left;
        return this;
    }

    // ================================================================ выбор

    /** Выбранный кликом элемент или {@code null}. */
    public UiElement selected() {
        return selected != null && selected.root() == root ? selected : null;
    }

    public Inspector select(UiElement element) {
        this.selected = element;
        revealRow(element);
        return this;
    }

    /** Элемент под мышью (без учёта панели). */
    public UiElement hovered() {
        return hovered;
    }

    /** Что показывается: строка дерева под мышью, выбранный элемент или элемент под мышью. */
    public UiElement target() {
        if (hoveredRow != null && hoveredRow.root() == root) {
            return hoveredRow;
        }
        UiElement s = selected();
        return s != null ? s : hovered;
    }

    /**
     * Самый глубокий видимый элемент в точке корня, включая те, что не ловят мышь (надписи, картинки).
     * Окна проверяются раньше содержимого.
     */
    public UiElement pick(float x, float y) {
        List<UiElement> layers = new ArrayList<>(root.overlays());
        layers.addAll(root.popups());
        for (int i = layers.size() - 1; i >= 0; i--) {
            UiElement hit = pickIn(layers.get(i), x, y);
            if (hit != null) {
                return hit;
            }
        }
        return pickIn(root.content(), x, y);
    }

    private static UiElement pickIn(UiElement e, float x, float y) {
        if (!e.isVisible()) {
            return null;
        }
        float ex = e.screenX();
        float ey = e.screenY();
        boolean inside = x >= ex && y >= ey && x < ex + e.width() && y < ey + e.height();
        if (!inside && e.clipsChildren()) {
            return null;
        }
        List<UiElement> children = e.children();
        for (int i = children.size() - 1; i >= 0; i--) {
            UiElement hit = pickIn(children.get(i), x, y);
            if (hit != null) {
                return hit;
            }
        }
        return inside ? e : null;
    }

    // ================================================================ описание

    /** Короткое имя: класс Java, классы стиля и id ({@code Button.button.primary#ok}). */
    public static String name(UiElement e) {
        StringBuilder sb = new StringBuilder(e.getClass().getSimpleName().isEmpty() ? "UiElement" : e.getClass().getSimpleName());
        String cls = e.styleClass();
        if (cls != null && !cls.isBlank()) {
            sb.append('.').append(cls.trim().replaceAll("\\s+", "."));
        }
        if (e.id() != null) {
            sb.append('#').append(e.id());
        }
        return sb.toString();
    }

    /**
     * Описание элемента строками: класс, стиль, геометрия, отступы и все свойства вычисленного стиля,
     * отличные от значений по умолчанию ({@code "radius: Radii[...]"}).
     */
    public List<String> describe(UiElement e) {
        List<String> out = new ArrayList<>();
        if (e == null) {
            return out;
        }
        out.add(name(e));
        out.add(I18n.ru("класс: ") + e.getClass().getName());
        if (e.styleClass() != null) {
            out.add(I18n.ru("стиль: ") + e.styleClass());
        }
        out.add(String.format(Locale.ROOT, I18n.ru("место: %.1f, %.1f  размер: %.1f × %.1f"),
                e.screenX(), e.screenY(), e.width(), e.height()));
        out.add("padding: " + insets(e.padding()));
        out.add("margin: " + insets(e.layoutParams().margin));
        StringBuilder flags = new StringBuilder(I18n.ru("флаги:"));
        if (e.isFocusable()) {
            flags.append(I18n.ru(" фокус"));
        }
        if (e.isFocused()) {
            flags.append(I18n.ru(" (в фокусе)"));
        }
        if (!e.isEnabled()) {
            flags.append(I18n.ru(" выключен"));
        }
        if (e.clipsChildren()) {
            flags.append(I18n.ru(" обрезает"));
        }
        out.add(flags.append(I18n.ru(" детей: ")).append(e.children().size()).toString());
        out.add(I18n.ru("— вычисленный стиль —"));
        ComputedStyle cs = e.computedStyle();
        int count = StyleKey.count();
        for (int id = 0; id < count; id++) {
            StyleKey<?> key = StyleKey.byId(id);
            Object v = cs.get(key);
            if (v != null && !Objects.equals(v, key.defaultValue())) {
                out.add(key.name() + ": " + format(key, v));
            }
        }
        return out;
    }

    private static String insets(Insets i) {
        return String.format(Locale.ROOT, "%s %s %s %s", num(i.top()), num(i.right()), num(i.bottom()), num(i.left()));
    }

    private static String num(float v) {
        return v == (int) v ? Integer.toString((int) v) : String.format(Locale.ROOT, "%.2f", v);
    }

    private static String format(StyleKey<?> key, Object v) {
        if (v instanceof Integer c && key.type() == Integer.class) {
            return Color.toHex(c);
        }
        if (v instanceof Float f) {
            return num(f);
        }
        return String.valueOf(v);
    }

    /** Всё дерево плоским списком (окна после содержимого), не больше нескольких тысяч строк. */
    public List<Row> treeRows() {
        List<Row> rows = new ArrayList<>();
        flatten(root.content(), 0, rows);
        for (UiElement p : root.popups()) {
            flatten(p, 0, rows);
        }
        return rows;
    }

    private static void flatten(UiElement e, int depth, List<Row> out) {
        if (out.size() >= MAX_ROWS || !e.isVisible()) {
            return;
        }
        out.add(new Row(e, depth));
        for (UiElement c : e.children()) {
            flatten(c, depth + 1, out);
        }
    }

    private void revealRow(UiElement e) {
        if (e == null) {
            return;
        }
        List<Row> rows = treeRows();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i).element() == e) {
                float top = i * ROW;
                float view = treeHeight();
                if (top < treeScroll || top + ROW > treeScroll + view) {
                    treeScroll = Math.max(0f, top - view / 2f);
                }
                return;
            }
        }
    }

    // ================================================================ ввод

    /** Клавиша: открыть/закрыть, Escape, стрелки по дереву. {@code true} — обработана. */
    boolean keyDown(int key) {
        if (enabled && key == toggleKey) {
            toggle();
            return true;
        }
        if (!open) {
            return false;
        }
        UiElement t = selected() != null ? selected() : hovered;
        switch (key) {
            case Keys.ESCAPE -> {
                close();
                return true;
            }
            case Keys.UP -> {
                if (t != null && t.parent() != null) {
                    select(t.parent());
                }
                return true;
            }
            case Keys.DOWN -> {
                if (t != null && !t.children().isEmpty()) {
                    select(t.children().get(0));
                }
                return true;
            }
            case Keys.LEFT, Keys.RIGHT -> {
                if (t != null && t.parent() != null) {
                    List<UiElement> sib = t.parent().children();
                    int i = sib.indexOf(t) + (key == Keys.LEFT ? -1 : 1);
                    if (i >= 0 && i < sib.size()) {
                        select(sib.get(i));
                    }
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    void pointerMove(float x, float y) {
        mouseX = x;
        mouseY = y;
        hoveredRow = null;
        if (!open) {
            return;
        }
        if (inPanel(x, y)) {
            hovered = null;
            hoveredRow = rowAt(y);
        } else {
            hovered = pick(x, y);
        }
    }

    /** Нажатие при открытом инспекторе: выбор элемента или действие в панели; интерфейс его не получает. */
    boolean pointerDown(float x, float y, int button) {
        if (!open) {
            return false;
        }
        pointerMove(x, y);
        if (inPanel(x, y)) {
            float px = panelX();
            if (y < HEADER + 4f) {
                if (x > px + PANEL_W - 16f) {
                    close();
                } else if (x > px + PANEL_W - 34f) {
                    panelLeft = !panelLeft;
                }
                return true;
            }
            UiElement row = rowAt(y);
            if (row != null) {
                selected = row;
            }
            return true;
        }
        if (button == Keys.MOUSE_RIGHT) {
            selected = null;
        } else {
            select(pick(x, y));
        }
        return true;
    }

    boolean pointerUp() {
        return open;
    }

    boolean scroll(float x, float y, float dy) {
        if (!open || !inPanel(x, y)) {
            return false;
        }
        float max = Math.max(0f, treeRows().size() * ROW - treeHeight());
        treeScroll = Math.max(0f, Math.min(max, treeScroll - dy * ROW * 3f));
        return true;
    }

    // ================================================================ геометрия панели

    private float panelX() {
        return panelLeft ? 4f : Math.max(4f, root.width() - PANEL_W - 4f);
    }

    private float panelH() {
        return Math.max(40f, root.height() - 8f);
    }

    private float treeHeight() {
        return Math.max(ROW * 3f, (panelH() - HEADER - 8f) * 0.45f);
    }

    private boolean inPanel(float x, float y) {
        float px = panelX();
        return x >= px && x < px + PANEL_W && y >= 4f && y < 4f + panelH();
    }

    private UiElement rowAt(float y) {
        float top = 4f + HEADER + 4f;
        if (y < top || y >= top + treeHeight()) {
            return null;
        }
        int i = (int) ((y - top + treeScroll) / ROW);
        List<Row> rows = treeRows();
        return i >= 0 && i < rows.size() ? rows.get(i).element() : null;
    }

    // ================================================================ отрисовка

    /** Нарисовать подсветку и панель (вызывает корень поверх всего, в координатах корня). */
    void render(Canvas canvas) {
        if (!open) {
            return;
        }
        if (hovered != null && hovered.root() != root) {
            hovered = null;
        }
        if (hovered == null && mouseX >= 0f && !inPanel(mouseX, mouseY)) {
            hovered = pick(mouseX, mouseY);
        }
        UiElement t = target();
        if (t != null && t.root() == root) {
            highlight(canvas, t);
        }
        UiElement sel = selected();
        if (sel != null && sel != t) {
            canvas.strokeRoundRect(sel.screenX(), sel.screenY(), sel.width(), sel.height(), Radii.ZERO, 1f, 0xFFFFC14F);
        }
        paintPanel(canvas, t);
    }

    private void highlight(Canvas canvas, UiElement e) {
        float x = e.screenX();
        float y = e.screenY();
        float w = e.width();
        float h = e.height();
        Insets m = e.layoutParams().margin;
        Insets p = e.padding();
        // margin: четыре полосы вокруг
        canvas.fillRect(x - m.left(), y - m.top(), w + m.horizontal(), m.top(), MARGIN_FILL);
        canvas.fillRect(x - m.left(), y + h, w + m.horizontal(), m.bottom(), MARGIN_FILL);
        canvas.fillRect(x - m.left(), y, m.left(), h, MARGIN_FILL);
        canvas.fillRect(x + w, y, m.right(), h, MARGIN_FILL);
        canvas.fillRect(x, y, w, h, BOUNDS_FILL);
        // padding: полосы внутри
        canvas.fillRect(x, y, w, p.top(), PADDING_FILL);
        canvas.fillRect(x, y + h - p.bottom(), w, p.bottom(), PADDING_FILL);
        canvas.fillRect(x, y + p.top(), p.left(), h - p.vertical(), PADDING_FILL);
        canvas.fillRect(x + w - p.right(), y + p.top(), p.right(), h - p.vertical(), PADDING_FILL);
        canvas.strokeRoundRect(x, y, w, h, Radii.ZERO, 1f, ACCENT);
        TextMetrics tm = canvas.textMetrics();
        String tag = name(e) + "  " + num(w) + "×" + num(h);
        float tw = tm.width(tag) + 8f;
        float ty = y - m.top() - tm.lineHeight() - 6f;
        if (ty < 0f) {
            ty = y + h + m.bottom() + 2f;
        }
        float tx = Math.max(0f, Math.min(x, root.width() - tw));
        canvas.fillRoundRect(tx, ty, tw, tm.lineHeight() + 4f, Radii.all(3f), Brush.solid(0xF0202633));
        canvas.drawText(tag, tx + 4f, ty + 2f, PANEL_TEXT, false);
    }

    private void paintPanel(Canvas canvas, UiElement t) {
        TextMetrics tm = canvas.textMetrics();
        float px = panelX();
        float py = 4f;
        float ph = panelH();
        canvas.fillRoundRect(px, py, PANEL_W, ph, Radii.all(6f), Brush.solid(PANEL_BG));
        canvas.strokeRoundRect(px, py, PANEL_W, ph, Radii.all(6f), 1f, 0x40FFFFFF);
        canvas.drawText(I18n.ru("Инспектор (") + keyLabel(toggleKey) + ")", px + 6f, py + 4f, PANEL_TEXT, false);
        canvas.drawText("<>", px + PANEL_W - 32f, py + 4f, PANEL_MUTED, false);
        canvas.drawText("x", px + PANEL_W - 12f, py + 4f, PANEL_MUTED, false);

        // дерево
        float top = py + HEADER + 4f;
        float th = treeHeight();
        List<Row> rows = treeRows();
        float max = Math.max(0f, rows.size() * ROW - th);
        treeScroll = Math.min(treeScroll, max);
        canvas.save();
        canvas.clipRect(px, top, PANEL_W, th);
        int first = (int) (treeScroll / ROW);
        UiElement sel = selected();
        for (int i = first; i < rows.size(); i++) {
            float ry = top + i * ROW - treeScroll;
            if (ry > top + th) {
                break;
            }
            Row row = rows.get(i);
            if (row.element() == sel) {
                canvas.fillRect(px + 2f, ry, PANEL_W - 4f, ROW, Color.withAlpha(ACCENT, 0x60));
            } else if (row.element() == t) {
                canvas.fillRect(px + 2f, ry, PANEL_W - 4f, ROW, 0x30FFFFFF);
            }
            float indent = Math.min(row.depth(), 20) * 6f;
            String label = fit(tm, name(row.element()), PANEL_W - 12f - indent);
            canvas.drawText(label, px + 6f + indent, ry + 1f, row.element().isVisible() ? PANEL_TEXT : PANEL_MUTED, false);
        }
        canvas.restore();
        float sep = top + th + 3f;
        canvas.fillRect(px + 4f, sep, PANEL_W - 8f, 1f, 0x30FFFFFF);

        // свойства
        float y = sep + 4f;
        canvas.save();
        canvas.clipRect(px, y, PANEL_W, py + ph - y - 2f);
        if (t == null) {
            canvas.drawText(I18n.ru("Наведите мышь на элемент"), px + 6f, y, PANEL_MUTED, false);
        } else {
            for (String line : describe(t)) {
                if (y > py + ph) {
                    break;
                }
                boolean head = line.startsWith("—");
                canvas.drawText(fit(tm, line, PANEL_W - 12f), px + 6f, y, head ? PANEL_MUTED : PANEL_TEXT, false);
                y += ROW;
            }
        }
        canvas.restore();
    }

    private static String keyLabel(int key) {
        return key >= Keys.F1 && key <= Keys.F1 + 24 ? "F" + (key - Keys.F1 + 1) : I18n.ru("клавиша ") + key;
    }

    /** Обрезать строку по ширине с многоточием. */
    private static String fit(TextMetrics tm, String s, float width) {
        if (tm.width(s) <= width) {
            return s;
        }
        int n = s.length();
        while (n > 1 && tm.width(s.substring(0, n) + "…") > width) {
            n--;
        }
        return s.substring(0, n) + "…";
    }
}
