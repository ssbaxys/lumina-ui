package io.github.ssbaxys.lumina.markup;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.blocks.BlockMap;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.fx.FxLayer;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.Direction;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.Justify;
import io.github.ssbaxys.lumina.layout.LayoutParams;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.markdown.Markdown;
import io.github.ssbaxys.lumina.node.StyleAnimation;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Computed;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Images;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.StyleKey;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Checkbox;
import io.github.ssbaxys.lumina.widget.ColorPicker;
import io.github.ssbaxys.lumina.widget.Icon;
import io.github.ssbaxys.lumina.widget.Image;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ProgressBar;
import io.github.ssbaxys.lumina.widget.RadioGroup;
import io.github.ssbaxys.lumina.widget.RingProgress;
import io.github.ssbaxys.lumina.widget.ScrollPanel;
import io.github.ssbaxys.lumina.widget.Slider;
import io.github.ssbaxys.lumina.widget.Toggle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Интерфейс из HTML-подобной разметки и CSS. Всё превращается в обычные виджеты и стили Lumina
 * один раз при построении: в кадре разметка ничего не стоит.
 *
 * <pre>{@code
 * <style>
 *   .counter { gap: 8px; align-items: center; }
 *   button.big:hover { transform: scale(1.1); transition: transform .3s out-back; }
 * </style>
 * <state name="count" value="0"/>
 * <row class="counter">
 *   <label>Нажатий: {{count}}</label>
 *   <button class="big" on-click="inc:count; sound:minecraft:block.note_block.pling">+1</button>
 * </row>
 * }</pre>
 *
 * Теги: {@code div column row stack scroll panel label span h1 h2 h3 p markdown button toggle slider
 * progress ring color-picker item img fx blocks spacer hr state style link}. Свои теги — {@link #tag}.
 * Атрибуты: {@code id class style width height grow gap align justify padding margin if enabled bind
 * on-click} и атрибуты конкретного тега. Логика — через {@link Bindings}.
 */
public final class Markup {
    /** Создаёт элемент для тега. Дети (если элемент — контейнер) добавляет {@link Context#children}. */
    @FunctionalInterface
    public interface TagFactory {
        UiElement create(Node node, Context ctx);
    }

    /** Источник ресурсов для {@code <link href>} и {@code src}: по умолчанию — {@link io.github.ssbaxys.lumina.platform.Platform#readResource}. */
    @FunctionalInterface
    public interface Resources {
        String read(String id);
    }

    private static final Map<String, TagFactory> TAGS = new HashMap<>();
    private static final Set<String> VOID = Set.of("img", "item", "br", "hr", "state", "link", "spacer", "toggle",
            "slider", "progress", "ring", "color-picker", "fx", "input", "meta");
    private static final Set<String> RAW = Set.of("style", "markdown", "script", "blocks");
    private static final Pattern TEMPLATE = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*(?:\\|\\s*(\\w+)\\s*)?}}");

    static {
        for (String t : new String[]{"div", "column", "section", "body", "main", "nav", "footer", "header"}) {
            tag(t, (n, c) -> c.children(n, Panel.box(FlexLayout.column())));
        }
        tag("row", (n, c) -> c.children(n, Panel.box(FlexLayout.row().align(Align.CENTER))));
        tag("stack", (n, c) -> c.children(n, Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH))));
        tag("panel", (n, c) -> c.children(n, new Panel()));
        tag("scroll", (n, c) -> {
            Panel content = c.children(n, Panel.box(FlexLayout.column().align(Align.STRETCH)));
            ScrollPanel scroll = new ScrollPanel(content);
            return scroll;
        });
        for (String t : new String[]{"label", "span", "text"}) {
            tag(t, (n, c) -> c.label(n, 1f));
        }
        tag("h1", (n, c) -> c.label(n, 1.8f));
        tag("h2", (n, c) -> c.label(n, 1.5f));
        tag("h3", (n, c) -> c.label(n, 1.25f));
        tag("p", (n, c) -> c.markdown(n));
        tag("markdown", (n, c) -> c.markdown(n));
        tag("button", (n, c) -> {
            Button b = new Button("");
            c.bindText(n, b::text);
            String click = n.attr("on-click");
            if (click != null) {
                b.onPress(() -> c.bindings.run(click, b));
            }
            if ("false".equals(n.attr("sound"))) {
                b.clickSound(false);
            }
            return b;
        });
        tag("toggle", (n, c) -> new Toggle(c.signal(n, "bind", false)));
        tag("slider", (n, c) -> {
            Signal<Float> v = c.floatSignal(n, "bind", n.floatAttr("min", 0f));
            Slider s = new Slider(v, n.floatAttr("min", 0f), n.floatAttr("max", 100f));
            if (n.attr("step") != null) {
                s.step(n.floatAttr("step", 1f));
            }
            return s;
        });
        tag("progress", (n, c) -> {
            ProgressBar p = new ProgressBar();
            ReadableSignal<Float> v = c.floatValue(n);
            if (v != null) {
                p.bind(v);
            }
            return p;
        });
        tag("ring", (n, c) -> {
            RingProgress r = new RingProgress();
            if (n.attr("spinner") != null) {
                r.spinner();
            }
            ReadableSignal<Float> v = c.floatValue(n);
            if (v != null) {
                r.bind(v);
            }
            if (n.attr("size") != null) {
                r.size(n.floatAttr("size", 40f), n.floatAttr("size", 40f));
            }
            return r;
        });
        tag("color-picker", (n, c) -> {
            Signal<Integer> v = c.signal(n, "bind", 0xFF5B8CFF);
            if (n.attr("value") != null) {
                v.set(CssValues.color(n.attr("value")));
            }
            return new ColorPicker(v).alpha(!"false".equals(n.attr("alpha")));
        });
        tag("item", (n, c) -> Icon.item(n.attr("id") != null && n.attr("item") == null ? n.attr("id") : n.attr("item"),
                n.floatAttr("size", 16f)));
        tag("img", (n, c) -> Icon.texture(TextureRef.of(n.attr("src")), n.floatAttr("size", 16f)));
        tag("spacer", (n, c) -> Panel.box(FlexLayout.column()).grow(1f));
        tag("hr", (n, c) -> Panel.box(FlexLayout.column()).height(Dim.px(1)).width(Dim.FILL)
                .style(b -> b.set(Styles.BACKGROUND, Brush.solid(0x30808080))));
        tag("br", (n, c) -> Panel.box(FlexLayout.column()).height(Dim.px(6)));
        tag("fx", (n, c) -> new MarkupFx(n.attr("effect")));
        tag("blocks", (n, c) -> {
            java.util.List<String> lines = new java.util.ArrayList<>();
            for (String line : n.text.split("\n")) {
                if (!line.isBlank()) {
                    lines.add(line.strip());
                }
            }
            BlockMap map = BlockMap.of(n.floatAttr("tile", 16f), lines.toArray(new String[0]));
            String key = n.attr("key");
            if (key != null) {
                for (String pair : key.split(";")) {
                    int eq = pair.indexOf('=');
                    if (eq > 0 && pair.substring(0, eq).strip().length() == 1) {
                        map.key(pair.substring(0, eq).strip().charAt(0), pair.substring(eq + 1).strip());
                    }
                }
            }
            if (n.attr("brush") != null) {
                map.editable(c.signal(n, "brush", (String) null));
            }
            return map.grid("true".equals(n.attr("grid")));
        });
        // Флажки, радио-группы, картинки; иконка и загрузка у <button>. Эти теги не пустые:
        // пишите <checkbox .../> и <image .../> с косой чертой (или с закрывающим тегом).
        tag("checkbox", (n, c) -> {
            Checkbox box = new Checkbox(c.signal(n, "bind", false));
            if (n.attr("kind") != null) {
                box.kind(checkboxKind(n.attr("kind")));
            }
            if (n.attr("label") != null) {
                c.bindText(n, box::label, n.attr("label"));
            } else {
                c.bindText(n, box::label);
            }
            String mixed = n.attr("indeterminate");
            if (mixed != null && !mixed.equals("false")) {
                box.indeterminate(true);
            }
            if (n.attr("size") != null) {
                box.boxSize(n.floatAttr("size", 12f));
            }
            if (n.attr("color") != null) {
                box.color(CssValues.color(n.attr("color")));
            }
            return box;
        });
        tag("radio-group", (n, c) -> {
            List<String> options = new ArrayList<>();
            for (String o : (n.attr("options") == null ? "" : n.attr("options")).split(",")) {
                if (!o.isBlank()) {
                    options.add(o.strip());
                }
            }
            String first = options.isEmpty() ? "" : options.get(0);
            Signal<Object> raw = n.attr("bind") != null ? c.bindings.writable(n.attr("bind"))
                    : Signal.of(n.attr("value") != null ? n.attr("value") : first);
            if (raw.peek() == null) {
                raw.set(n.attr("value") != null ? n.attr("value") : first);
            }
            // Варианты — строки; значение в состоянии может быть числом ("1"), поэтому сравниваем как текст.
            Signal<String> local = Signal.of(String.valueOf(raw.peek()));
            Subscription in = raw.subscribe(v -> local.set(String.valueOf(v)));
            Subscription out = local.subscribe(v -> {
                Object prev = raw.peek();
                if (!v.equals(String.valueOf(prev))) {
                    raw.set(prev instanceof String || prev == null ? v : Bindings.guess(v));
                }
            });
            c.disposers.add(in::close);
            c.disposers.add(out::close);
            RadioGroup<String> group = new RadioGroup<>(local, options);
            if (n.attr("labels") != null) {
                String[] labels = n.attr("labels").split(",");
                group.labels(o -> {
                    int i = options.indexOf(o);
                    return i >= 0 && i < labels.length ? labels[i].strip() : o;
                });
            }
            String dir = n.attr("direction");
            if (n.attr("horizontal") != null || (dir != null && dir.startsWith("row"))) {
                group.horizontal();
            }
            if (n.attr("kind") != null) {
                group.kind(checkboxKind(n.attr("kind")));
            }
            return group;
        });
        tag("image", (n, c) -> {
            Image image = new Image(imageRef(n.attr("src"), true)).fit(Image.Fit.parse(n.attr("fit")));
            if (n.attr("radius") != null) {
                image.radius(n.floatAttr("radius", 0f));
            }
            return image;
        });
        TagFactory plainButton = TAGS.get("button");
        tag("button", (n, c) -> {
            Button b = (Button) plainButton.create(n, c);
            String icon = n.attr("icon");
            if (icon != null && !icon.isEmpty()) {
                b.icon(imageRef(icon, false));
            }
            String item = n.attr("item");
            if (item != null && !item.isEmpty()) {
                b.item(item);
            }
            if (n.attr("icon-size") != null) {
                b.iconSize(n.floatAttr("icon-size", 12f));
            }
            String loading = n.attr("loading");
            if (loading != null && !loading.equals("false")) {
                if (loading.isEmpty() || loading.equals("true")) {
                    b.loading(true);
                } else {
                    b.loading(c.signal(n, "loading", false));
                }
            }
            return b;
        });
    }

    /** "check", "heart", "switch-dot"... → вид флажка (неизвестное — CHECK). */
    static Checkbox.Kind checkboxKind(String s) {
        try {
            return Checkbox.Kind.valueOf(s.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
        } catch (IllegalArgumentException e) {
            System.err.println("[Lumina markup] unknown checkbox kind: " + s);
            return Checkbox.Kind.CHECK;
        }
    }

    /**
     * {@code src} картинки: "file:путь" или путь без пространства имён — файл с диска, иначе ресурс
     * "мод:путь". {@code measure} — узнать размер ресурса (для пропорций), иначе ссылка как есть.
     */
    static TextureRef imageRef(String src, boolean measure) {
        if (src == null || src.isBlank()) {
            return null;
        }
        String s = src.strip();
        boolean drive = s.length() > 2 && s.charAt(1) == ':' && (s.charAt(2) == '/' || s.charAt(2) == '\\');
        if (s.startsWith("file:")) {
            return Images.fromFile(s.substring(5));
        }
        if (drive || s.indexOf(':') < 0) {
            return Images.fromFile(s);
        }
        return measure && Lumina.isInitialized() ? Images.fromResource(s) : TextureRef.of(s);
    }

    private Markup() {
    }

    /** Свой тег: {@code Markup.tag("inventory-slot", (node, ctx) -> new MySlot(node.attr("item")))}. */
    public static synchronized void tag(String name, TagFactory factory) {
        TAGS.put(name.toLowerCase(Locale.ROOT), factory);
    }

    /** Построить интерфейс из разметки. */
    public static Built build(String html, Bindings bindings) {
        return build(html, bindings, Markup::readResource, null);
    }

    /**
     * Построить интерфейс: {@code extraCss} (может быть {@code null}) — общая таблица стилей, к которой
     * добавятся {@code <style>} и {@code <link>} из разметки.
     */
    public static Built build(String html, Bindings bindings, Resources resources, Stylesheet extraCss) {
        Node root = parse(html);
        Stylesheet css = new Stylesheet();
        if (extraCss != null) {
            css.merge(extraCss);
        }
        List<String> linked = new ArrayList<>();
        collectStyles(root, css, resources, linked);
        Context ctx = new Context(bindings, css, resources);
        declareState(root, bindings);
        List<UiElement> top = new ArrayList<>();
        for (Node n : root.children) {
            UiElement e = ctx.build(n);
            if (e != null) {
                top.add(e);
            }
        }
        UiElement result;
        if (top.size() == 1) {
            result = top.get(0);
        } else {
            Panel p = Panel.box(FlexLayout.column().align(Align.STRETCH));
            top.forEach(p::addChild);
            result = p;
        }
        return new Built(result, ctx.disposers, linked, css);
    }

    /** Результат построения: корневой элемент и подписки, которые надо закрыть при пересборке. */
    public record Built(UiElement root, List<Runnable> disposers, List<String> linkedResources, Stylesheet stylesheet) {
        public void dispose() {
            disposers.forEach(Runnable::run);
            disposers.clear();
        }
    }

    static String readResource(String id) {
        return Lumina.isInitialized() ? Lumina.platform().readResource(id) : null;
    }

    private static void collectStyles(Node n, Stylesheet css, Resources resources, List<String> linked) {
        for (Node c : n.children) {
            if ("style".equals(c.tag)) {
                css.add(c.text);
            } else if ("link".equals(c.tag) && c.attr("href") != null) {
                String text = resources.read(c.attr("href"));
                linked.add(c.attr("href"));
                if (text != null) {
                    css.add(text);
                } else {
                    System.err.println("[Lumina markup] stylesheet not found: " + c.attr("href"));
                }
            } else {
                collectStyles(c, css, resources, linked);
            }
        }
    }

    private static void declareState(Node n, Bindings b) {
        for (Node c : n.children) {
            if ("state".equals(c.tag) && c.attr("name") != null) {
                b.state(c.attr("name"), Bindings.guess(c.attr("value") == null ? "" : c.attr("value")));
            } else {
                declareState(c, b);
            }
        }
    }

    // ================================================================ дерево разметки

    /** Узел разметки: тег, атрибуты, дети, текст. Доступен своим тегам ({@link TagFactory}). */
    public static final class Node {
        final String tag;
        final Map<String, String> attrs = new LinkedHashMap<>();
        final List<Node> children = new ArrayList<>();
        String text = "";
        Node parent;
        private List<String> classes;

        Node(String tag) {
            this.tag = tag;
        }

        public String tag() {
            return tag;
        }

        public String attr(String name) {
            return attrs.get(name);
        }

        public float floatAttr(String name, float def) {
            String v = attrs.get(name);
            return v == null ? def : CssValues.length(v);
        }

        public Map<String, String> attributes() {
            return attrs;
        }

        public List<Node> children() {
            return children;
        }

        /** Текст внутри тега (для текстовых тегов — склеенный текст детей). */
        public String text() {
            return text;
        }

        public Node parent() {
            return parent;
        }

        public List<String> classes() {
            if (classes == null) {
                String c = attrs.get("class");
                classes = c == null || c.isBlank() ? List.of() : List.of(c.trim().split("\\s+"));
            }
            return classes;
        }
    }

    /** Разобрать разметку в дерево (терпимо к ошибкам, как браузер). */
    public static Node parse(String html) {
        Node root = new Node("#root");
        Node cur = root;
        int i = 0;
        int n = html.length();
        StringBuilder text = new StringBuilder();
        while (i < n) {
            char c = html.charAt(i);
            if (c != '<') {
                text.append(c);
                i++;
                continue;
            }
            if (html.startsWith("<!--", i)) {
                int end = html.indexOf("-->", i);
                i = end < 0 ? n : end + 3;
                continue;
            }
            if (html.startsWith("<!", i) || html.startsWith("<?", i)) {
                int end = html.indexOf('>', i);
                i = end < 0 ? n : end + 1;
                continue;
            }
            flushText(cur, text);
            if (i + 1 < n && html.charAt(i + 1) == '/') {
                int end = html.indexOf('>', i);
                String name = html.substring(i + 2, end < 0 ? n : end).trim().toLowerCase(Locale.ROOT);
                for (Node up = cur; up != null && up != root; up = up.parent) {
                    if (up.tag.equals(name)) {
                        cur = up.parent;
                        break;
                    }
                }
                i = end < 0 ? n : end + 1;
                continue;
            }
            int j = i + 1;
            while (j < n && !Character.isWhitespace(html.charAt(j)) && html.charAt(j) != '>' && html.charAt(j) != '/') {
                j++;
            }
            Node node = new Node(html.substring(i + 1, j).toLowerCase(Locale.ROOT));
            // атрибуты
            boolean selfClose = false;
            while (j < n) {
                char a = html.charAt(j);
                if (a == '>') {
                    j++;
                    break;
                }
                if (a == '/' && j + 1 < n && html.charAt(j + 1) == '>') {
                    selfClose = true;
                    j += 2;
                    break;
                }
                if (Character.isWhitespace(a)) {
                    j++;
                    continue;
                }
                int k = j;
                while (k < n && "=>/ \t\r\n".indexOf(html.charAt(k)) < 0) {
                    k++;
                }
                String name = html.substring(j, k).toLowerCase(Locale.ROOT);
                String value = "";
                while (k < n && Character.isWhitespace(html.charAt(k))) {
                    k++;
                }
                if (k < n && html.charAt(k) == '=') {
                    k++;
                    while (k < n && Character.isWhitespace(html.charAt(k))) {
                        k++;
                    }
                    char q = k < n ? html.charAt(k) : ' ';
                    if (q == '"' || q == '\'') {
                        int end = html.indexOf(q, k + 1);
                        value = html.substring(k + 1, end < 0 ? n : end);
                        k = end < 0 ? n : end + 1;
                    } else {
                        int start = k;
                        while (k < n && !Character.isWhitespace(html.charAt(k)) && html.charAt(k) != '>') {
                            k++;
                        }
                        value = html.substring(start, k);
                    }
                }
                node.attrs.put(name, decode(value));
                j = k;
            }
            i = j;
            node.parent = cur;
            cur.children.add(node);
            if (RAW.contains(node.tag) && !selfClose) {
                String close = "</" + node.tag;
                int end = html.toLowerCase(Locale.ROOT).indexOf(close, i);
                node.text = html.substring(i, end < 0 ? n : end);
                if (!"style".equals(node.tag)) {
                    node.text = dedent(decode(node.text));
                }
                int gt = end < 0 ? n : html.indexOf('>', end);
                i = gt < 0 ? n : gt + 1;
            } else if (!selfClose && !VOID.contains(node.tag)) {
                cur = node;
            }
        }
        flushText(cur, text);
        return root;
    }

    private static void flushText(Node cur, StringBuilder text) {
        if (text.length() == 0) {
            return;
        }
        String t = decode(text.toString().replaceAll("\\s+", " "));
        text.setLength(0);
        if (t.isBlank()) {
            return;
        }
        Node node = new Node("#text");
        node.text = t.trim();
        node.parent = cur;
        cur.children.add(node);
    }

    static String decode(String s) {
        if (s.indexOf('&') < 0) {
            return s;
        }
        return s.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'")
                .replace("&nbsp;", " ").replace("&amp;", "&");
    }

    /** Убрать общий отступ строк (текст markdown внутри вложенной разметки). */
    static String dedent(String s) {
        String[] lines = s.split("\n", -1);
        int min = Integer.MAX_VALUE;
        for (String l : lines) {
            if (l.isBlank()) {
                continue;
            }
            int k = 0;
            while (k < l.length() && l.charAt(k) == ' ') {
                k++;
            }
            min = Math.min(min, k);
        }
        StringBuilder sb = new StringBuilder();
        for (String l : lines) {
            sb.append(l.isBlank() ? "" : l.substring(Math.min(min, l.length()))).append('\n');
        }
        return sb.toString().strip();
    }

    // ================================================================ построение

    /** Контекст построения: доступен своим тегам. */
    public static final class Context {
        final Bindings bindings;
        final Stylesheet css;
        final Resources resources;
        final List<Runnable> disposers = new ArrayList<>();

        Context(Bindings bindings, Stylesheet css, Resources resources) {
            this.bindings = bindings;
            this.css = css;
            this.resources = resources;
        }

        public Bindings bindings() {
            return bindings;
        }

        UiElement build(Node n) {
            if (n.tag.equals("#text")) {
                Node wrap = new Node("span");
                wrap.parent = n.parent;
                wrap.text = n.text;
                return build(wrap, label(wrap, 1f));
            }
            if (n.tag.equals("style") || n.tag.equals("link") || n.tag.equals("state") || n.tag.equals("script")
                    || n.tag.equals("meta") || n.tag.equals("head") || n.tag.equals("title")) {
                return null;
            }
            TagFactory f = TAGS.get(n.tag);
            if (f == null) {
                System.err.println("[Lumina markup] unknown tag <" + n.tag + ">, treated as <div>");
                f = TAGS.get("div");
            }
            UiElement e = f.create(n, this);
            return e == null ? null : build(n, e);
        }

        private UiElement build(Node n, UiElement e) {
            String id = n.attr("id");
            if (id != null) {
                e.setId(id);
            }
            if (!n.classes().isEmpty()) {
                String base = e.styleClass();
                e.setStyleClass((base == null ? "" : base + " ") + String.join(" ", n.classes()));
            }
            List<Stylesheet.Rule> matched = css.matching(n);
            applyStyle(n, e, matched);
            applyLayout(n, e, matched);
            applyBehaviour(n, e);
            String animation = css.layoutValue(matched, "animation");
            if (animation != null) {
                animate(e, animation);
            }
            return e;
        }

        /** Построить детей узла и добавить в контейнер. */
        public Panel children(Node n, Panel container) {
            for (Node c : n.children) {
                UiElement child = build(c);
                if (child != null) {
                    container.addChild(child);
                }
            }
            return container;
        }

        Label label(Node n, float scale) {
            Label l = new Label("");
            if (scale != 1f) {
                l.scale(scale);
            }
            bindText(n, l::text);
            return l;
        }

        UiElement markdown(Node n) {
            String src = n.attr("src");
            String text = src != null ? resources.read(src) : n.tag.equals("p") ? inlineMarkdown(n) : textOf(n);
            Markdown md = new Markdown(text == null ? "" : "");
            bindText(n, md::text, text == null ? "" : text);
            String link = n.attr("on-link");
            if (link != null) {
                md.onLink(url -> bindings.run(link.replace("$url", url), md));
            }
            return md;
        }

        /** Текст узла с шаблонами {@code {{имя}}} → в {@code setter}, обновляется при изменении сигналов. */
        public void bindText(Node n, java.util.function.Consumer<String> setter) {
            bindText(n, setter, textOf(n));
        }

        void bindText(Node n, java.util.function.Consumer<String> setter, String template) {
            if (template.indexOf("{{") < 0) {
                setter.accept(template);
                return;
            }
            Computed<String> text = new Computed<>(() -> render(template));
            setter.accept(text.get());
            Subscription sub = text.subscribe(setter::accept);
            disposers.add(sub::close);
            disposers.add(text::dispose);
        }

        private String render(String template) {
            Matcher m = TEMPLATE.matcher(template);
            StringBuilder sb = new StringBuilder();
            int last = 0;
            while (m.find()) {
                sb.append(template, last, m.start());
                ReadableSignal<?> s = bindings.get(m.group(1));
                Object v = s == null ? "{{" + m.group(1) + "}}" : s.get();
                sb.append(format(v, m.group(2)));
                last = m.end();
            }
            return sb.append(template.substring(last)).toString();
        }

        private static String format(Object v, String filter) {
            if (filter != null && v instanceof Number num) {
                return switch (filter) {
                    case "int" -> String.valueOf(Math.round(num.doubleValue()));
                    case "percent" -> Math.round(num.doubleValue() * 100) + "%";
                    case "hex" -> io.github.ssbaxys.lumina.render.Color.toHex(num.intValue());
                    default -> String.valueOf(v);
                };
            }
            if (v instanceof Float f) {
                return f == Math.rint(f) ? String.valueOf(Math.round(f)) : String.format(Locale.ROOT, "%.2f", f);
            }
            return String.valueOf(v);
        }

        /** Строчные HTML-теги внутри {@code <p>} → Markdown: b, strong, i, em, u, s, del, mark, code, a, br, item. */
        private static String inlineMarkdown(Node n) {
            if (!n.text.isEmpty()) {
                return n.text;
            }
            StringBuilder sb = new StringBuilder();
            for (Node c : n.children) {
                String inner = c.tag.equals("#text") ? c.text : inlineMarkdown(c);
                String piece = switch (c.tag) {
                    case "b", "strong" -> "**" + inner + "**";
                    case "i", "em" -> "*" + inner + "*";
                    case "u", "ins" -> "++" + inner + "++";
                    case "s", "del", "strike" -> "~~" + inner + "~~";
                    case "mark" -> "==" + inner + "==";
                    case "code" -> "`" + inner + "`";
                    case "a" -> "[" + inner + "](" + c.attr("href") + ")";
                    case "br" -> "\\\n";
                    case "item" -> "![](item:" + (c.attr("item") != null ? c.attr("item") : c.attr("id")) + ")";
                    case "img" -> "![](texture:" + c.attr("src") + ")";
                    default -> inner;
                };
                if (sb.length() > 0 && !piece.isEmpty() && !c.tag.equals("br") && sb.charAt(sb.length() - 1) != '\n'
                        && needsSpace(sb.charAt(sb.length() - 1), piece.charAt(0))) {
                    sb.append(' ');
                }
                sb.append(piece);
            }
            return sb.toString();
        }

        private static boolean needsSpace(char before, char after) {
            return !Character.isWhitespace(before) && ".,!?:;)".indexOf(after) < 0;
        }

        private static String textOf(Node n) {
            if (!n.text.isEmpty() || n.children.isEmpty()) {
                return n.text;
            }
            StringBuilder sb = new StringBuilder();
            for (Node c : n.children) {
                if (c.tag.equals("#text")) {
                    if (sb.length() > 0) {
                        sb.append(' ');
                    }
                    sb.append(c.text);
                }
            }
            return sb.toString();
        }

        /** Сигнал из атрибута {@code attr} (например, {@code bind="volume"}); без атрибута — свой. */
        @SuppressWarnings("unchecked")
        public <T> Signal<T> signal(Node n, String attr, T initial) {
            String name = n.attr(attr);
            if (name == null) {
                return Signal.of(initial);
            }
            Signal<Object> s = bindings.writable(name);
            if (s.peek() == null) {
                s.set(initial);
            }
            return (Signal<T>) (Signal<?>) s;
        }

        /** Для слайдера: сигнал с Float (целые из {@code <state>} приводятся). */
        Signal<Float> floatSignal(Node n, String attr, float initial) {
            String name = n.attr(attr);
            if (name == null) {
                return Signal.of(n.attr("value") != null ? n.floatAttr("value", initial) : initial);
            }
            Signal<Object> s = bindings.writable(name);
            Object v = s.peek();
            if (!(v instanceof Float)) {
                s.set(v instanceof Number num ? num.floatValue() : initial);
            }
            @SuppressWarnings("unchecked")
            Signal<Float> f = (Signal<Float>) (Signal<?>) s;
            return f;
        }

        /** {@code value="0.4"}, {@code value="{{p}}"} или {@code bind="p"} (с {@code max} для нормировки). */
        ReadableSignal<Float> floatValue(Node n) {
            float max = n.floatAttr("max", 1f);
            String name = n.attr("bind");
            String value = n.attr("value");
            if (name == null && value != null) {
                Matcher m = TEMPLATE.matcher(value);
                if (m.matches()) {
                    name = m.group(1);
                } else {
                    return Signal.of(CssValues.number(value) / max);
                }
            }
            if (name == null) {
                return null;
            }
            ReadableSignal<?> s = bindings.get(name);
            if (s == null) {
                s = bindings.state(name, 0f);
            }
            ReadableSignal<?> src = s;
            Computed<Float> c = new Computed<>(() -> {
                Object v = src.get();
                return v instanceof Number num ? num.floatValue() / max : 0f;
            });
            disposers.add(c::dispose);
            return c;
        }

        // ---------------------------------------------------------------- стиль

        private void applyStyle(Node n, UiElement e, List<Stylesheet.Rule> matched) {
            String inline = n.attr("style");
            if (matched.isEmpty() && inline == null) {
                return;
            }
            Style.Builder b = e.inlineStyle().toBuilder();
            for (Stylesheet.Rule r : matched) {
                css.applyDeclarations(b, r.declarations(), r.selector().subject().states());
            }
            if (inline != null) {
                css.applyDeclarations(b, Stylesheet.declarations(inline), 0);
            }
            e.setStyle(b.build());
        }

        // ---------------------------------------------------------------- раскладка

        private void applyLayout(Node n, UiElement e, List<Stylesheet.Rule> matched) {
            Map<String, String> props = new HashMap<>();
            for (String p : Stylesheet.LAYOUT_PROPS) {
                String v = css.layoutValue(matched, p);
                if (v != null) {
                    props.put(p, v);
                }
            }
            if (n.attr("style") != null) {
                for (Stylesheet.Declaration d : Stylesheet.declarations(n.attr("style"))) {
                    if (Stylesheet.LAYOUT_PROPS.contains(d.name())) {
                        props.put(d.name(), d.value());
                    }
                }
            }
            for (String a : new String[]{"width", "height", "gap", "grow", "margin", "align", "justify", "direction"}) {
                if (n.attr(a) != null) {
                    props.put(switch (a) {
                        case "align" -> "align-items";
                        case "justify" -> "justify-content";
                        case "direction" -> "flex-direction";
                        default -> a;
                    }, n.attr(a));
                }
            }
            if (n.attr("padding") != null) {
                e.setStyle(e.inlineStyle().toBuilder().set(Styles.PADDING, CssValues.insets(n.attr("padding"))).build());
            }
            if (props.isEmpty()) {
                return;
            }
            LayoutParams lp = e.layoutParams();
            for (Map.Entry<String, String> p : props.entrySet()) {
                String v = p.getValue().trim();
                try {
                    switch (p.getKey()) {
                        case "width" -> lp.width = dim(v);
                        case "height" -> lp.height = dim(v);
                        case "min-width" -> lp.minWidth = CssValues.length(v);
                        case "min-height" -> lp.minHeight = CssValues.length(v);
                        case "max-width" -> lp.maxWidth = CssValues.length(v);
                        case "max-height" -> lp.maxHeight = CssValues.length(v);
                        case "margin" -> lp.margin = CssValues.insets(v);
                        case "flex-grow", "grow" -> lp.grow = Float.parseFloat(v);
                        case "flex-shrink" -> lp.shrink = Float.parseFloat(v);
                        case "position" -> lp.absolute = v.equals("absolute");
                        case "left" -> lp.left = CssValues.length(v);
                        case "top" -> lp.top = CssValues.length(v);
                        case "overflow" -> e.setClipChildren(v.equals("hidden") || v.equals("scroll"));
                        case "display" -> e.setVisible(!v.equals("none"));
                        case "gap", "flex-direction", "align-items", "justify-content" -> flex(e, p.getKey(), v);
                        default -> {
                        }
                    }
                } catch (RuntimeException ex) {
                    System.err.println("[Lumina markup] " + p.getKey() + ": " + v + " — " + ex.getMessage());
                }
            }
            e.invalidateLayout();
        }

        private static Dim dim(String v) {
            return switch (v) {
                case "auto" -> Dim.AUTO;
                case "fill", "100%" -> Dim.FILL;
                default -> v.endsWith("%") ? Dim.percent(CssValues.number(v)) : Dim.px(CssValues.length(v));
            };
        }

        private static void flex(UiElement e, String prop, String v) {
            if (!(e.layout() instanceof FlexLayout fl)) {
                if (prop.equals("flex-direction") && e instanceof Panel) {
                    e.setLayout(v.startsWith("row") ? FlexLayout.row() : FlexLayout.column());
                }
                return;
            }
            switch (prop) {
                case "gap" -> fl.gap(CssValues.length(v));
                case "flex-direction" -> fl.direction(v.startsWith("row") ? Direction.ROW : Direction.COLUMN);
                case "align-items" -> fl.align(switch (v) {
                    case "center" -> Align.CENTER;
                    case "end", "flex-end" -> Align.END;
                    case "stretch" -> Align.STRETCH;
                    default -> Align.START;
                });
                case "justify-content" -> fl.justify(switch (v) {
                    case "center" -> Justify.CENTER;
                    case "end", "flex-end" -> Justify.END;
                    case "space-between" -> Justify.SPACE_BETWEEN;
                    case "space-around" -> Justify.SPACE_AROUND;
                    case "space-evenly" -> Justify.SPACE_EVENLY;
                    default -> Justify.START;
                });
                default -> {
                }
            }
        }

        // ---------------------------------------------------------------- поведение

        private void applyBehaviour(Node n, UiElement e) {
            String click = n.attr("on-click");
            if (click != null && !(e instanceof Button)) {
                e.on(PointerEvent.CLICK, ev -> bindings.run(click, e));
            }
            String hover = n.attr("on-hover");
            if (hover != null) {
                e.on(PointerEvent.ENTER, ev -> bindings.run(hover, e));
            }
            String change = n.attr("on-change");
            if (change != null && n.attr("bind") != null) {
                ReadableSignal<?> s = bindings.get(n.attr("bind"));
                if (s != null) {
                    Subscription sub = s.subscribe(v -> bindings.run(change, e));
                    disposers.add(sub::close);
                }
            }
            condition(n.attr("if"), e::setVisible);
            condition(n.attr("enabled"), e::setEnabled);
        }

        /** {@code if="flag"}, {@code if="!flag"}, {@code if="tab==2"}, {@code if="mode!=easy"}. */
        private void condition(String expr, java.util.function.Consumer<Boolean> apply) {
            if (expr == null) {
                return;
            }
            String x = expr.trim();
            boolean negate = x.startsWith("!") && !x.contains("=");
            if (negate) {
                x = x.substring(1).trim();
            }
            String name = x;
            String cmp = null;
            boolean equals = true;
            int op = x.indexOf("==");
            if (op < 0) {
                op = x.indexOf("!=");
                equals = false;
            }
            if (op > 0) {
                name = x.substring(0, op).trim();
                cmp = x.substring(op + 2).trim();
            }
            ReadableSignal<?> s = bindings.get(name);
            if (s == null) {
                s = bindings.state(name, cmp == null ? Boolean.FALSE : null);
            }
            String want = cmp;
            boolean eq = equals;
            Function<Object, Boolean> test = v -> {
                boolean r = want == null ? (v instanceof Boolean b ? b : v != null && !"".equals(v) && !Integer.valueOf(0).equals(v))
                        : String.valueOf(v).equals(want) == eq;
                return negate != r;
            };
            apply.accept(test.apply(s.get()));
            Subscription sub = s.subscribe(v -> apply.accept(test.apply(v)));
            disposers.add(sub::close);
        }

        // ---------------------------------------------------------------- анимации

        /** {@code animation: имя 2s [кривая] [задержка] [infinite|N] [alternate]}. */
        @SuppressWarnings({"unchecked", "rawtypes"})
        private void animate(UiElement e, String value) {
            for (String one : CssValues.split(value, ',')) {
                String name = null;
                float dur = 1f;
                float delay = 0f;
                int times = 0;
                int loops = 1;
                boolean yoyo = false;
                Easing easing = Easing.cubicBezier(0.25f, 0.1f, 0.25f, 1f);
                for (String p : CssValues.split(one, ' ')) {
                    if (CssValues.isTime(p)) {
                        if (times++ == 0) {
                            dur = CssValues.time(p);
                        } else {
                            delay = CssValues.time(p);
                        }
                    } else if (p.equals("infinite")) {
                        loops = -1;
                    } else if (p.equals("alternate")) {
                        yoyo = true;
                    } else if (p.matches("\\d+")) {
                        loops = Integer.parseInt(p);
                    } else if (css.keyframes.containsKey(p)) {
                        name = p;
                    } else if (!p.equals("normal") && !p.equals("forwards") && !p.equals("both")) {
                        try {
                            easing = CssValues.easing(p);
                        } catch (RuntimeException ignored) {
                            name = p;
                        }
                    }
                }
                if (name == null) {
                    continue;
                }
                for (Map.Entry<StyleKey<?>, List<Object[]>> track : css.keyframeTracks(name).entrySet()) {
                    StyleAnimation anim = e.animate((StyleKey) track.getKey());
                    for (Object[] kf : track.getValue()) {
                        anim.keyframe((Float) kf[0], kf[1]);
                    }
                    anim.duration(dur).delay(delay).ease(easing);
                    if (loops < 0) {
                        anim.loop();
                    } else {
                        anim.loops(loops);
                    }
                    if (yoyo) {
                        anim.yoyo();
                    }
                    anim.play();
                }
            }
        }
    }

    /** {@code <fx effect="minecraft:snow">}: слой эффектов, сам запускает эффект на весь размер. */
    static final class MarkupFx extends FxLayer {
        private final String effect;
        private boolean started;

        MarkupFx(String effect) {
            this.effect = effect;
        }

        @Override
        protected void onLayout() {
            if (!started && effect != null && width() > 0f && root() != null) {
                started = true;
                for (String id : effect.split("[,\\s]+")) {
                    spawn(id);
                }
            }
        }

        @Override
        protected void onDetach() {
            super.onDetach();
            started = false;
        }
    }
}
