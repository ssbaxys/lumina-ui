package io.github.ssbaxys.lumina.markup;

import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Виджет, содержимое которого описано разметкой: из строки или из ресурса мода/ресурспака.
 *
 * <pre>{@code
 * MarkupView.resource("mymod:ui/settings.html", new Bindings()
 *         .signal("volume", volume)
 *         .handler("save", this::save))
 *     .watch(1f);   // горячая перезагрузка: правите файл в ресурспаке, жмёте F3+T — экран обновляется
 * }</pre>
 *
 * Состояние живёт в {@link Bindings}, поэтому при пересборке значения не теряются.
 */
public class MarkupView extends Widget<MarkupView> implements Animation {
    private final String resourceId;
    private String source;
    private final Bindings bindings;
    private Markup.Resources resources = Markup::readResource;
    private Stylesheet baseCss;
    private Markup.Built built;
    private List<String> watched = List.of();
    private List<String> watchedTexts = List.of();
    private float watchInterval;
    private float sinceCheck;
    private boolean ticking;
    private int generation;
    private Consumer<MarkupView> onBuild;

    private MarkupView(String resourceId, String source, Bindings bindings) {
        super(null);
        this.resourceId = resourceId;
        this.source = source;
        this.bindings = bindings;
        setLayout(new StackLayout(Align.STRETCH, Align.START));
        layoutParams().width = Dim.FILL;
    }

    /** Из строки разметки. */
    public static MarkupView of(String html, Bindings bindings) {
        return new MarkupView(null, html, bindings);
    }

    /** Из ресурса "мод:путь" (assets/мод/путь), в том числе переопределённого ресурспаком. */
    public static MarkupView resource(String id, Bindings bindings) {
        return new MarkupView(id, null, bindings);
    }

    public Bindings bindings() {
        return bindings;
    }

    /** Общая таблица стилей для этой разметки (например, оформление всего мода). */
    public MarkupView stylesheet(Stylesheet css) {
        this.baseCss = css;
        rebuildIfAttached();
        return this;
    }

    /** Свой источник ресурсов (по умолчанию — менеджер ресурсов игры). */
    public MarkupView resources(Markup.Resources resources) {
        this.resources = resources;
        return this;
    }

    /** Проверять файл и подключённые CSS раз в {@code seconds} и пересобирать при изменении. 0 — выключить. */
    public MarkupView watch(float seconds) {
        this.watchInterval = seconds;
        startTicking();
        return this;
    }

    /** Вызывается после каждой сборки (в том числе после горячей перезагрузки). */
    public MarkupView onBuild(Consumer<MarkupView> callback) {
        this.onBuild = callback;
        return this;
    }

    /** Заменить разметку (для строкового варианта). */
    public MarkupView setSource(String html) {
        this.source = html;
        rebuildIfAttached();
        return this;
    }

    /** Сколько раз интерфейс собирался (растёт при горячей перезагрузке). */
    public int generation() {
        return generation;
    }

    /** Корневой элемент собранной разметки. */
    public UiElement content() {
        return built == null ? null : built.root();
    }

    public void reload() {
        rebuildIfAttached();
    }

    private void rebuildIfAttached() {
        if (root() != null) {
            rebuild();
        }
    }

    private void rebuild() {
        dispose();
        String html = resourceId != null ? resources.read(resourceId) : source;
        List<String> files = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        if (resourceId != null) {
            files.add(resourceId);
            texts.add(html);
        }
        if (html == null) {
            addChild(new Label(I18n.ru("Не найден ресурс ") + resourceId));
            watched = files;
            watchedTexts = texts;
            return;
        }
        try {
            built = Markup.build(html, bindings, resources, baseCss);
            addChild(built.root());
            for (String linked : built.linkedResources()) {
                files.add(linked);
                texts.add(resources.read(linked));
            }
        } catch (RuntimeException e) {
            addChild(new Label(I18n.ru("Ошибка разметки: ") + e));
        }
        watched = files;
        watchedTexts = texts;
        generation++;
        if (onBuild != null) {
            onBuild.accept(this);
        }
    }

    private void dispose() {
        if (built != null) {
            built.dispose();
            built = null;
        }
        clearChildren();
    }

    @Override
    protected void onAttach() {
        rebuild();
        startTicking();
    }

    @Override
    protected void onDetach() {
        dispose();
    }

    private void startTicking() {
        if (!ticking && watchInterval > 0f && root() != null) {
            ticking = true;
            root().animator().start(this);
        }
    }

    @Override
    public boolean tick(float dt) {
        if (root() == null || watchInterval <= 0f) {
            ticking = false;
            return false;
        }
        sinceCheck += dt;
        if (sinceCheck >= watchInterval) {
            sinceCheck = 0f;
            for (int i = 0; i < watched.size(); i++) {
                if (!Objects.equals(resources.read(watched.get(i)), watchedTexts.get(i))) {
                    rebuild();
                    break;
                }
            }
        }
        return true;
    }
}
