package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.i18n.Language;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Panel;

final class OverviewPage {
    private OverviewPage() {
    }

    static UiElement create() {
        return Ui.page(
                Ui.column(4,
                        Ui.text("luminatest.overview.welcome").scale(1.8f),
                        Ui.muted("luminatest.overview.tagline"),
                        Ui.muted("luminatest.overview.versions")),
                Panel.box(FlexLayout.row().gap(10).align(Align.STRETCH)).width(Dim.FILL).add(
                        card("sdf"),
                        card("flex"),
                        card("anim")),
                Panel.box(FlexLayout.row().gap(10).align(Align.STRETCH)).width(Dim.FILL).add(
                        card("style"),
                        card("events"),
                        card("reactive")),
                Ui.section("luminatest.overview.language", "luminatest.overview.language.hint",
                        languages(),
                        Ui.text("luminatest.overview.language.current", I18n.text(() -> I18n.languageName(I18n.current()))),
                        // Этот ключ лежит не в коде, а в lang-файле мода (assets/luminatest/lang).
                        Ui.muted("luminatest.overview.language.file", I18n.tr("key.luminatest.open"))),
                Ui.section("luminatest.overview.where", null,
                        Ui.text("luminatest.overview.where.key"),
                        Ui.text("luminatest.overview.where.command"),
                        Ui.text("luminatest.overview.where.button")),
                Ui.section("luminatest.overview.try", "luminatest.overview.try.hint",
                        Ui.text("luminatest.overview.try.text")));
    }

    /** Кнопка на каждый объявленный язык и «как в игре». */
    private static Panel languages() {
        Panel row = Ui.row(6);
        for (Language language : I18n.languages()) {
            row.add(new Button(language.name(), () -> I18n.setLanguage(language.code())));
        }
        return row.add(Button.tr("luminatest.overview.language.auto").onPress(I18n::followGameLanguage));
    }

    /** Карточка: заголовок и две строки по ключам {@code luminatest.card.<name>.*}. */
    private static Panel card(String name) {
        String key = "luminatest.card." + name;
        return new Panel("card", FlexLayout.column().gap(3)).grow(1).add(
                Ui.text(key + ".title").style(b -> b.set(Styles.TEXT_COLOR, 0xFF7AA2FF)),
                Ui.muted(key + ".1"),
                Ui.muted(key + ".2"));
    }
}
