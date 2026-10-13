package io.github.ssbaxys.lumina.config;

import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.ColorField;
import io.github.ssbaxys.lumina.widget.FieldBase;
import io.github.ssbaxys.lumina.widget.KeyBindField;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ScrollPanel;
import io.github.ssbaxys.lumina.widget.Select;
import io.github.ssbaxys.lumina.widget.SideNav;
import io.github.ssbaxys.lumina.widget.SliderField;
import io.github.ssbaxys.lumina.widget.TextField;
import io.github.ssbaxys.lumina.widget.Toggle;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * Экран настроек, построенный по {@link ConfigSpec}: слева разделы ({@link SideNav}), справа строки параметров
 * с подходящими полями ({@link Toggle}, {@link SliderField}, {@link TextField}, {@link Select}, {@link ColorField},
 * {@link KeyBindField}), у каждой — кнопка «по умолчанию». Сверху — поиск по всем разделам, «Сбросить всё» и
 * «Сохранить» (или автосохранение).
 *
 * <pre>{@code
 * spec.load();
 * UiElement screen = new ConfigScreen(spec).title("Мой мод").autoSave(true).fill();
 * }</pre>
 */
public class ConfigScreen extends Widget<ConfigScreen> {
    private static final Style ROW = Style.builder()
            .bind(Styles.BACKGROUND, Tokens.TEXT, t -> Brush.solid(Color.withAlpha(t, 0x0A)))
            .set(Styles.RADIUS, Radii.all(6))
            .set(Styles.PADDING, Insets.of(6, 8))
            .hovered(h -> h.bind(Styles.BACKGROUND, Tokens.TEXT, t -> Brush.solid(Color.withAlpha(t, 0x14))))
            .build();
    private static final Style MUTED = Style.builder().set(Styles.OPACITY, 0.62f).build();

    private final ConfigSpec spec;
    private final Signal<Integer> category = Signal.of(0);
    private final Signal<String> query = Signal.of("");
    private final Label title = new Label(I18n.ru("Настройки")).scale(1.25f);
    private final Label status = new Label("");
    private final Panel rows = Panel.box(FlexLayout.column().gap(4).align(Align.STRETCH));
    private final ScrollPanel scroll;
    private final TextField search;
    private final List<Subscription> rowSubscriptions = new ArrayList<>();
    private final List<Subscription> subscriptions = new ArrayList<>();
    private boolean autoSave;
    private boolean dirty;
    private Runnable onSave;

    public ConfigScreen(ConfigSpec spec) {
        super("config-screen");
        this.spec = spec;
        setLayout(FlexLayout.row().gap(10).align(Align.STRETCH));
        SideNav nav = new SideNav(category);
        for (ConfigSpec.Category c : spec.categories()) {
            nav.item(c.title(), c.icon());
        }
        nav.id("config-nav");
        nav.setVisible(spec.categories().size() > 1);

        search = new TextField(query).placeholder(I18n.ru("Поиск настроек")).icon(FieldBase.SEARCH).clearButton(true)
                .width(Dim.px(150)).id("config-search");
        Button reset = new Button(I18n.ru("Сбросить всё"), this::resetAll).styleClass("button ghost").id("config-reset");
        Button save = new Button(I18n.ru("Сохранить"), this::save).id("config-save");
        Panel header = Panel.box(FlexLayout.row().gap(8).align(Align.CENTER))
                .add(title.grow(1), search, reset, save).width(Dim.FILL);
        status.style(MUTED).id("config-status");
        scroll = new ScrollPanel(rows).grow(1).width(Dim.FILL);
        scroll.setStyleClass(null);
        rows.style(b -> b.set(Styles.PADDING, new Insets(0, 4, 0, 0)));
        Panel main = Panel.box(FlexLayout.column().gap(8).align(Align.STRETCH)).add(header, status, scroll).grow(1);
        main.layoutParams().height = Dim.FILL;
        addChild(nav);
        addChild(main);
    }

    public ConfigSpec spec() {
        return spec;
    }

    public ConfigScreen title(String text) {
        title.text(text);
        return this;
    }

    /** Сохранять файл после каждого изменения (кнопка «Сохранить» тогда не нужна, но остаётся). */
    public ConfigScreen autoSave(boolean autoSave) {
        this.autoSave = autoSave;
        return this;
    }

    /** Вызывается после успешного сохранения. */
    public ConfigScreen onSave(Runnable onSave) {
        this.onSave = onSave;
        return this;
    }

    /** Номер открытого раздела. */
    public Signal<Integer> category() {
        return category;
    }

    /** Строка поиска (пустая — показывается раздел). */
    public Signal<String> query() {
        return query;
    }

    public boolean isDirty() {
        return dirty;
    }

    /** Строки параметров, которые сейчас показаны (для тестов и своих украшений). */
    public List<UiElement> rows() {
        return rows.children();
    }

    public boolean save() {
        boolean ok = spec.save();
        if (ok) {
            dirty = false;
            status.text(I18n.ru("Сохранено: ") + spec.fileName());
            if (onSave != null) {
                onSave.run();
            }
        } else {
            status.text(I18n.ru("Не удалось сохранить ") + spec.fileName());
        }
        return ok;
    }

    public void resetAll() {
        spec.resetAll();
        status.text(I18n.ru("Все параметры возвращены к значениям по умолчанию"));
    }

    @Override
    protected void onAttach() {
        subscriptions.add(category.subscribe(i -> rebuild()));
        subscriptions.add(query.subscribe(q -> rebuild()));
        for (ConfigSpec.Option<?> o : spec.options()) {
            subscriptions.add(o.value().subscribe(v -> changed()));
        }
        rebuild();
    }

    @Override
    protected void onDetach() {
        for (Subscription s : subscriptions) {
            s.close();
        }
        subscriptions.clear();
        closeRows();
    }

    private void changed() {
        dirty = true;
        if (autoSave) {
            save();
        } else {
            status.text(I18n.ru("Есть несохранённые изменения"));
        }
    }

    private void closeRows() {
        for (Subscription s : rowSubscriptions) {
            s.close();
        }
        rowSubscriptions.clear();
    }

    /** Перестроить строки: раздел или результаты поиска. */
    private void rebuild() {
        closeRows();
        rows.clearChildren();
        String q = query.peek();
        List<ConfigSpec.Option<?>> shown;
        boolean searching = q != null && !q.isBlank();
        if (searching) {
            shown = spec.search(q);
        } else if (spec.categories().isEmpty()) {
            shown = List.of();
        } else {
            int i = Math.max(0, Math.min(spec.categories().size() - 1, category.peek() == null ? 0 : category.peek()));
            shown = new ArrayList<>(spec.categories().get(i).options());
        }
        if (shown.isEmpty()) {
            rows.add(new Label(searching ? I18n.ru("Ничего не найдено") : I18n.ru("Здесь пока пусто")).style(MUTED));
        }
        String lastCategory = null;
        for (ConfigSpec.Option<?> o : shown) {
            if (searching && !o.category().id().equals(lastCategory)) {
                lastCategory = o.category().id();
                rows.add(new Label(o.category().title()).style(MUTED));
            }
            rows.add(row(o));
        }
        scroll.scrollTo(0f, false);
    }

    private Panel row(ConfigSpec.Option<?> o) {
        Label label = new Label(o.label());
        Panel text = Panel.box(FlexLayout.column().gap(2)).add(label).grow(1).clip(true);
        if (!o.comment().isBlank()) {
            text.add(new Label(o.comment()).style(MUTED));
        }
        UiElement control = control(o);
        Button reset = new Button("↺", o::reset).styleClass("button ghost");
        reset.tooltip(I18n.ru("По умолчанию"));
        reset.setEnabled(!o.isDefault());
        rowSubscriptions.add(o.value().subscribe(v -> reset.setEnabled(!o.isDefault())));
        Panel row = Panel.box(FlexLayout.row().gap(8).align(Align.CENTER)).add(text, control, reset).width(Dim.FILL);
        row.style(ROW);
        row.id("config-row-" + o.path());
        return row;
    }

    @SuppressWarnings("unchecked")
    private UiElement control(ConfigSpec.Option<?> o) {
        return switch (o.type()) {
            case BOOL -> new Toggle(((ConfigSpec.Option<Boolean>) o).value());
            case INT -> intSlider((ConfigSpec.Option<Integer>) o);
            case FLOAT -> {
                ConfigSpec.Option<Float> f = (ConfigSpec.Option<Float>) o;
                yield new SliderField(f.value(), f.min(), f.max()).step(f.step()).decimals(decimals(f.step()))
                        .width(Dim.px(200));
            }
            case STRING -> new TextField(((ConfigSpec.Option<String>) o).value()).maxLength(o.maxLength())
                    .width(Dim.px(150));
            case ENUM -> select(o);
            case COLOR -> new ColorField(((ConfigSpec.Option<Integer>) o).value()).alpha(o.alpha());
            case KEY -> new KeyBindField(((ConfigSpec.Option<Integer>) o).value())
                    .defaultKey(((ConfigSpec.Option<Integer>) o).defaultValue()).width(Dim.px(110));
        };
    }

    private static int decimals(float step) {
        if (step >= 1f) {
            return 0;
        }
        return step >= 0.1f ? 1 : step >= 0.01f ? 2 : 3;
    }

    /** Целое значение через ползунок с дробным сигналом-мостом. */
    private UiElement intSlider(ConfigSpec.Option<Integer> o) {
        Signal<Float> bridge = Signal.of(o.get().floatValue());
        rowSubscriptions.add(bridge.subscribe(v -> o.set(Math.round(v))));
        rowSubscriptions.add(o.value().subscribe(v -> bridge.set(v.floatValue())));
        return new SliderField(bridge, o.min(), o.max()).step(o.step()).decimals(0).width(Dim.px(200));
    }

    private static <T> Select<T> select(ConfigSpec.Option<T> o) {
        return new Select<>(o.value(), o.choices()).labels(o::labelOf).width(Dim.px(150));
    }
}
