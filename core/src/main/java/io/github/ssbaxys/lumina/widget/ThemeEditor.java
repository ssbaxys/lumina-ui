package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markup.ThemeCss;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.ThemeVar;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.theme.ThemeTransition;

import java.util.function.Consumer;

/**
 * Редактор темы: акцент, текст, фон, скругление и скорость анимаций с живым предпросмотром, экспорт
 * темы в CSS-файл (и в буфер обмена) и импорт обратно.
 *
 * <p>Правит тему своего корня на месте ({@code root().theme().set(...)}), поэтому меняется сразу весь
 * интерфейс, а не только предпросмотр. Загруженная тема ставится плавно ({@link ThemeTransition}) или
 * передаётся в {@link #onLoad}.</p>
 *
 * <pre>{@code
 * new ThemeEditor().fileName("моя.css").baseName("dark")
 * }</pre>
 *
 * Стиль "theme-editor" (по умолчанию не задан — прозрачный контейнер).
 */
public class ThemeEditor extends Widget<ThemeEditor> {
    private final Signal<Integer> accent = Signal.of(Tokens.ACCENT.defaultValue());
    private final Signal<Integer> text = Signal.of(Tokens.TEXT.defaultValue());
    private final Signal<Integer> surface = Signal.of(Tokens.SURFACE.defaultValue());
    private final Signal<Float> corner = Signal.of(Tokens.CORNER.defaultValue());
    private final Signal<Float> motion = Signal.of(Tokens.MOTION.defaultValue());
    private final Label status = new Label("");
    private final Panel preview;
    private String fileName = "custom.css";
    private String baseName = "dark";
    private Consumer<Theme> onLoad;
    private boolean syncing;
    private String lastExport;

    public ThemeEditor() {
        super("theme-editor");
        setLayout(FlexLayout.column().gap(6).align(Align.STRETCH));

        addChild(row("Акцент", new ColorField(accent).alpha(false)));
        addChild(row("Текст", new ColorField(text).alpha(false)));
        addChild(row("Фон", new ColorField(surface)));
        addChild(new SliderField("Скругление", corner, 0f, 16f).step(1f).decimals(0));
        addChild(new SliderField("Скорость анимаций", motion, 0f, 2f).step(0.1f).decimals(1));

        preview = Panel.column().gap(6);
        preview.add(new Label("Предпросмотр"),
                Panel.box(FlexLayout.row().gap(6).align(Align.CENTER)).add(
                        new Button("Кнопка"),
                        new Button("Контур").styleClass("button outline"),
                        new Toggle(Signal.of(true))),
                new Slider(Signal.of(0.6f), 0f, 1f).width(Dim.FILL));
        preview.width(Dim.FILL);
        addChild(preview);

        addChild(Panel.box(FlexLayout.row().gap(6).align(Align.CENTER)).add(
                new Button("Экспорт", this::exportTheme).id("theme-export"),
                new Button("Импорт", this::importTheme).id("theme-import"),
                new Button("Из буфера", this::importClipboard).styleClass("button ghost"),
                new Button("Сброс", this::resetTheme).styleClass("button ghost")));
        status.setStyle(Style.builder().set(Styles.OPACITY, 0.7f).build());
        addChild(status);

        accent.subscribe(v -> apply(Tokens.ACCENT, v));
        text.subscribe(v -> apply(Tokens.TEXT, v));
        surface.subscribe(v -> {
            apply(Tokens.SURFACE, v);
            apply(Tokens.SURFACE_ALT, v);
        });
        corner.subscribe(v -> apply(Tokens.CORNER, v));
        motion.subscribe(v -> apply(Tokens.MOTION, v));
    }

    private static Panel row(String title, UiElement field) {
        return Panel.box(FlexLayout.row().gap(8).align(Align.CENTER))
                .add(new Label(title).grow(1f), field)
                .width(Dim.FILL);
    }

    // ---------------------------------------------------------------- настройка

    /** Имя файла в {@code themes/} каталога данных (по умолчанию "custom.css"). */
    public ThemeEditor fileName(String fileName) {
        this.fileName = fileName;
        return this;
    }

    public String fileName() {
        return fileName;
    }

    /** Базовая тема для экспорта ({@code --lumina-base}), см. {@link ThemeCss#bases()}. */
    public ThemeEditor baseName(String baseName) {
        this.baseName = baseName;
        return this;
    }

    /** Что делать с загруженной темой (по умолчанию — плавно поставить в корень). */
    public ThemeEditor onLoad(Consumer<Theme> onLoad) {
        this.onLoad = onLoad;
        return this;
    }

    public Signal<Integer> accent() {
        return accent;
    }

    public Signal<Integer> textColor() {
        return text;
    }

    public Signal<Integer> surface() {
        return surface;
    }

    public Signal<Float> corner() {
        return corner;
    }

    public Signal<Float> motion() {
        return motion;
    }

    /** Строка состояния (результат экспорта/импорта). */
    public String status() {
        return status.text();
    }

    /** Текст последнего экспорта или {@code null}. */
    public String lastExport() {
        return lastExport;
    }

    public Panel preview() {
        return preview;
    }

    // ---------------------------------------------------------------- работа с темой

    @Override
    protected void onAttach() {
        syncFrom(root().theme());
    }

    /** Подтянуть значения полей из темы (без записи обратно). */
    public void syncFrom(Theme theme) {
        syncing = true;
        try {
            accent.set(theme.get(Tokens.ACCENT));
            text.set(theme.get(Tokens.TEXT));
            surface.set(theme.get(Tokens.SURFACE));
            corner.set(theme.get(Tokens.CORNER));
            motion.set(theme.get(Tokens.MOTION));
        } finally {
            syncing = false;
        }
    }

    private <T> void apply(ThemeVar<T> var, T value) {
        if (!syncing && root() != null) {
            root().theme().set(var, value);
        }
    }

    /** Экспорт: CSS в файл {@link #fileName()} и в буфер обмена. */
    public void exportTheme() {
        if (root() == null) {
            return;
        }
        Theme theme = root().theme();
        lastExport = ThemeCss.export(theme, baseName);
        boolean saved = ThemeCss.save(fileName, theme, baseName);
        if (Lumina.isInitialized()) {
            Lumina.platform().setClipboard(lastExport);
        }
        status.text(saved ? "Сохранено в themes/" + fileName + " и скопировано" : "Скопировано в буфер (файл не записан)");
    }

    /** Импорт из файла {@link #fileName()}. */
    public void importTheme() {
        Theme t = ThemeCss.loadFile(fileName);
        if (t == null) {
            status.text("Нет файла themes/" + fileName);
            return;
        }
        load(t);
        status.text("Загружено из themes/" + fileName);
    }

    /** Импорт из буфера обмена. */
    public void importClipboard() {
        String css = Lumina.isInitialized() ? Lumina.platform().getClipboard() : null;
        if (css == null || css.indexOf('{') < 0) {
            status.text("В буфере нет темы");
            return;
        }
        try {
            load(ThemeCss.load(css, ThemeCss.base(baseName)));
            status.text("Загружено из буфера");
        } catch (RuntimeException e) {
            status.text("Ошибка в теме: " + e.getMessage());
        }
    }

    /** Вернуть базовую тему. */
    public void resetTheme() {
        Theme base = ThemeCss.base(baseName);
        if (base != null) {
            load(base);
            status.text("Сброшено к «" + baseName + "»");
        }
    }

    private void load(Theme theme) {
        if (onLoad != null) {
            onLoad.accept(theme);
        } else if (root() != null) {
            ThemeTransition.to(root(), theme, 0.5f);
        }
        syncFrom(theme);
    }
}
