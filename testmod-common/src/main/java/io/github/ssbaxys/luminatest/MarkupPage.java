package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.fx.VanillaFx;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.markdown.Markdown;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.MarkupView;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;

/**
 * Экран, целиком описанный HTML и CSS ({@code assets/luminatest/ui/markup_demo.*}).
 * Из Java приходят только два обработчика: {@code shake} и {@code applyAccent}; остальная логика — в разметке.
 */
final class MarkupPage {
    private static final String HTML = "luminatest:ui/markup_demo.html";
    private static final String CSS = "luminatest:ui/markup_demo.css";

    private MarkupPage() {
    }

    static UiElement create() {
        Signal<Integer> accent = Signal.of(ShowcaseTheme.dark().get(Tokens.ACCENT));
        MarkupView[] view = {null};
        Bindings bindings = new Bindings()
                .signal("accent", accent)
                .handler("shake", source -> view[0].play(VanillaFx.shake(view[0], 4f, 0.4f)))
                .handler("applyAccent", source -> {
                    if (source.root() != null) {
                        source.root().theme().set(Tokens.ACCENT, accent.peek());
                    }
                });
        Signal<String> generation = Signal.of(Tr.r("сборка 1"));
        view[0] = MarkupView.resource(html(), bindings)
                .watch(1f)
                .onBuild(v -> generation.set(Tr.r("сборка ") + v.generation()));

        Signal<String> code = Signal.of("");
        Markdown source = Markdown.bound(code);
        Panel sourceCard = Ui.column(0, source).width(Dim.FILL);
        sourceCard.style(b -> b
                .set(Styles.BACKGROUND, Brush.solid(0x14808080))
                .set(Styles.RADIUS, Radii.all(8))
                .set(Styles.PADDING, Insets.all(8)));
        sourceCard.setVisible(false);

        return Ui.page(
                Ui.section(Tr.r("HTML и CSS"),
                        Tr.r("Экран ниже загружен из файлов мода; правка в ресурспаке + F3+T обновляет его на лету."),
                        view[0]),
                Ui.section(Tr.r("Исходники"), null,
                        Ui.row(6,
                                new Button("HTML", () -> showSource(code, sourceCard, html(), "html")),
                                new Button("CSS", () -> showSource(code, sourceCard, CSS, "css")).styleClass("button ghost"),
                                new Button(Tr.r("Скрыть"), () -> sourceCard.setVisible(false)).styleClass("button ghost"),
                                new Button(Tr.r("Пересобрать"), () -> view[0].reload()).styleClass("button ghost"),
                                Label.bound(generation).style(Ui.MUTED)),
                        sourceCard));
    }

    /** Разметка демо-экрана на текущем языке витрины. */
    private static String html() {
        return Tr.russian() ? HTML : "luminatest:ui/markup_demo_en.html";
    }

    private static void showSource(Signal<String> code, Panel card, String id, String lang) {
        String text = Lumina.isInitialized() ? Lumina.platform().readResource(id) : null;
        code.set(text == null ? Tr.r("Ресурс `") + id + Tr.r("` не найден.") : "```" + lang + "\n" + text.strip() + "\n```");
        card.setVisible(true);
    }
}
