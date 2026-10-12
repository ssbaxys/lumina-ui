package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.ArrayList;
import java.util.List;

/**
 * Дерево: узлы с подписью и иконкой предмета, раскрытие по клику на стрелку (или двойному клику),
 * выбор узла, стрелки клавиатуры. Рисуется напрямую, без виджета на узел.
 *
 * <pre>{@code
 * TreeView.Node root = TreeView.node("Мир")
 *     .add(TreeView.node("Верхний мир", "minecraft:grass_block").add(TreeView.node("Деревня")))
 *     .add(TreeView.node("Незер", "minecraft:netherrack"));
 * TreeView tree = new TreeView(root).onSelect(n -> show(n.label()));
 * }</pre>
 */
public class TreeView extends Widget<TreeView> {
    private static final float ROW = 16f;
    private static final float INDENT = 12f;

    /** Узел дерева. {@link #data} — любые свои данные. */
    public static final class Node {
        private final String label;
        private final String itemId;
        private final List<Node> children = new ArrayList<>();
        private boolean expanded;
        public Object data;

        Node(String label, String itemId) {
            this.label = label;
            this.itemId = itemId;
        }

        public Node add(Node... nodes) {
            children.addAll(List.of(nodes));
            return this;
        }

        public Node expanded(boolean open) {
            this.expanded = open;
            return this;
        }

        public String label() {
            return label;
        }

        public List<Node> children() {
            return children;
        }

        public boolean isExpanded() {
            return expanded;
        }
    }

    private record Row(Node node, int depth) {
    }

    private final Node root;
    private final boolean showRoot;
    private final List<Row> rows = new ArrayList<>();
    private final Signal<Node> selection = Signal.of(null);
    private int hover = -1;
    private long lastClick;
    private Node lastClicked;

    public TreeView(Node root) {
        this(root, true);
    }

    public TreeView(Node root, boolean showRoot) {
        super("tree");
        this.root = root;
        this.showRoot = showRoot;
        root.expanded = true;
        setFocusable(true);
        rebuild();
    }

    public static Node node(String label) {
        return new Node(label, null);
    }

    public static Node node(String label, String itemId) {
        return new Node(label, itemId);
    }

    public Signal<Node> selection() {
        return selection;
    }

    public TreeView onSelect(java.util.function.Consumer<Node> listener) {
        selection.subscribe(n -> {
            if (n != null) {
                listener.accept(n);
            }
        });
        return this;
    }

    /** Пересобрать видимые строки (после изменения узлов). */
    public void rebuild() {
        rows.clear();
        if (showRoot) {
            collect(root, 0);
        } else {
            for (Node c : root.children) {
                collect(c, 0);
            }
        }
        invalidateLayout();
    }

    private void collect(Node n, int depth) {
        rows.add(new Row(n, depth));
        if (n.expanded) {
            for (Node c : n.children) {
                collect(c, depth + 1);
            }
        }
    }

    public void toggle(Node n) {
        if (!n.children.isEmpty()) {
            n.expanded = !n.expanded;
            rebuild();
        }
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float w = 0f;
        for (Row r : rows) {
            w = Math.max(w, r.depth() * INDENT + 30f + m.width(r.node().label()));
        }
        setContentSize(w, rows.size() * ROW);
    }

    private int rowAt(float ly) {
        int i = (int) Math.floor((ly - padding().top()) / ROW);
        return i >= 0 && i < rows.size() ? i : -1;
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event instanceof PointerEvent p) {
            if (event.type() == PointerEvent.MOVE) {
                hover = rowAt(p.localY());
            } else if (event.type() == PointerEvent.LEAVE) {
                hover = -1;
            } else if (event.type() == PointerEvent.CLICK && p.button() == Keys.MOUSE_LEFT) {
                int i = rowAt(p.localY());
                if (i >= 0) {
                    Row r = rows.get(i);
                    float arrowX = padding().left() + r.depth() * INDENT;
                    long now = System.currentTimeMillis();
                    boolean dbl = r.node() == lastClicked && now - lastClick < 350;
                    lastClick = now;
                    lastClicked = r.node();
                    if (p.localX() < arrowX + 10f || dbl) {
                        toggle(r.node());
                    }
                    selection.set(r.node());
                    event.consume();
                }
            }
        } else if (event.type() == KeyEvent.PRESS && event.target() == this) {
            int key = ((KeyEvent) event).key();
            int i = -1;
            for (int k = 0; k < rows.size(); k++) {
                if (rows.get(k).node() == selection.peek()) {
                    i = k;
                }
            }
            if (key == Keys.DOWN || key == Keys.UP) {
                int next = Math.max(0, Math.min(rows.size() - 1, i + (key == Keys.DOWN ? 1 : -1)));
                if (!rows.isEmpty()) {
                    selection.set(rows.get(next).node());
                }
                event.consume();
            } else if ((key == Keys.RIGHT || key == Keys.LEFT || key == Keys.ENTER) && i >= 0) {
                Node n = rows.get(i).node();
                if ((key == Keys.RIGHT) != n.expanded || key == Keys.ENTER) {
                    toggle(n);
                }
                event.consume();
            }
        }
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        int text = style.get(Styles.TEXT_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        float left = padding().left();
        float top = padding().top();
        float w = width() - padding().horizontal();
        Node selected = selection.peek();
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            float y = top + i * ROW;
            if (r.node() == selected) {
                canvas.fillRect(left, y, w, ROW, Color.withAlpha(accent, 0x50));
            } else if (i == hover) {
                canvas.fillRect(left, y, w, ROW, Color.withAlpha(text, 0x16));
            }
            float x = left + r.depth() * INDENT;
            // направляющие линии уровней
            for (int d = 1; d <= r.depth(); d++) {
                canvas.fillRect(left + d * INDENT - 7f, y, 1f, ROW, Color.withAlpha(text, 0x20));
            }
            if (!r.node().children.isEmpty()) {
                float cx = x + 4f;
                float cy = y + ROW / 2f;
                int ac = Color.withAlpha(text, 0xC0);
                if (r.node().expanded) {
                    canvas.drawLine(cx - 3f, cy - 1.5f, cx, cy + 1.5f, 1.2f, ac);
                    canvas.drawLine(cx, cy + 1.5f, cx + 3f, cy - 1.5f, 1.2f, ac);
                } else {
                    canvas.drawLine(cx - 1.5f, cy - 3f, cx + 1.5f, cy, 1.2f, ac);
                    canvas.drawLine(cx + 1.5f, cy, cx - 1.5f, cy + 3f, 1.2f, ac);
                }
            }
            x += 10f;
            if (r.node().itemId != null) {
                canvas.drawItem(r.node().itemId, x, y + 1f, 14f);
                x += 16f;
            }
            canvas.drawText(r.node().label, x, y + (ROW - m.lineHeight()) / 2f + 1f, text, false);
        }
    }
}
