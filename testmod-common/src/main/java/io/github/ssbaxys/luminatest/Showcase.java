package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.LanguageButton;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ScrollPanel;
import io.github.ssbaxys.lumina.widget.Toggle;

import java.util.List;
import java.util.function.Supplier;

/**
 * Витрина Lumina UI: окно с боковой навигацией и страницами по областям библиотеки.
 * Весь код — только API ядра; загрузчики лишь открывают экран.
 */
public final class Showcase {
    private record PageDef(String titleKey, Supplier<UiElement> factory) {
    }

    private Showcase() {
    }

    public static UiRoot create(Runnable onClose) {
        ShowcaseLang.register();
        Signal<Boolean> light = Signal.of(false);

        List<PageDef> pages = List.of(
                new PageDef("luminatest.nav.overview", OverviewPage::create),
                new PageDef("luminatest.nav.modern", ModernPage::create),
                new PageDef("luminatest.nav.layout", LayoutPage::create),
                new PageDef("luminatest.nav.style", () -> StylePage.create(light)),
                new PageDef("luminatest.nav.colors", ColorsPage::create),
                new PageDef("luminatest.nav.gradients", GradientsPage::create),
                new PageDef("luminatest.nav.anim", AnimationPage::create),
                new PageDef("luminatest.nav.effects", EffectsPage::create),
                new PageDef("luminatest.nav.markdown", MarkdownPage::create),
                new PageDef("luminatest.nav.markup", MarkupPage::create),
                new PageDef("luminatest.nav.blocks", BlocksPage::create),
                new PageDef("luminatest.nav.structures", StructuresPage::create),
                new PageDef("luminatest.nav.items", ItemsPage::create),
                new PageDef("luminatest.nav.hud", HudPage::create),
                new PageDef("luminatest.nav.fields", FieldsPage::create),
                new PageDef("luminatest.nav.buttons", ButtonsPage::create),
                new PageDef("luminatest.nav.components", ComponentsPage::create),
                new PageDef("luminatest.nav.popups", PopupsPage::create),
                new PageDef("luminatest.nav.datatools", DataToolsPage::create),
                new PageDef("luminatest.nav.themes", ThemesPage::create),
                new PageDef("luminatest.nav.windows", WindowsPage::create),
                new PageDef("luminatest.nav.tools", ToolsPage::create),
                new PageDef("luminatest.nav.input", InputPage::create),
                new PageDef("luminatest.nav.data", DataPage::create),
                new PageDef("luminatest.nav.perf", PerformancePage::create));

        Panel content = Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH)).grow(1);
        content.setClipChildren(true);

        Panel nav = Panel.box(FlexLayout.column().gap(3)).width(Dim.px(124));
        nav.add(new Label("Lumina UI").scale(1.4f), Ui.muted("luminatest.subtitle"), Ui.column(0).size(1, 6));

        // Разделов много: на маленьком экране список прокручивается, а кнопки внизу остаются на месте.
        Panel links = Panel.box(FlexLayout.column().gap(3).align(Align.STRETCH));
        ScrollPanel linkScroll = new ScrollPanel(links).width(Dim.FILL).height(Dim.FILL);
        linkScroll.setStyleClass(null);
        nav.add(linkScroll);

        Button[] buttons = new Button[pages.size()];
        for (int i = 0; i < pages.size(); i++) {
            int index = i;
            buttons[i] = Button.tr(pages.get(i).titleKey())
                    .styleClass("nav")
                    .width(Dim.FILL)
                    .clickSound(false)
                    .onPress(() -> select(index, pages, buttons, content));
            links.add(buttons[i]);
        }
        nav.add(Ui.column(0).size(1, 2),
                new LanguageButton().width(Dim.FILL),
                Ui.column(0).size(1, 2),
                Ui.row(6, new Toggle(light), Ui.text("luminatest.light_theme")),
                Ui.column(0).size(1, 4),
                Button.tr("luminatest.close").onPress(onClose).width(Dim.FILL));

        Panel window = Panel.row().gap(12)
                .width(Dim.percent(0.92f)).height(Dim.percent(0.9f))
                .maxSize(780f, 520f)
                .add(nav, content);

        Panel screen = Panel.box(new StackLayout(Align.CENTER, Align.CENTER))
                .fill()
                .style(b -> b.set(Styles.BACKGROUND, Brush.vertical(0x50000000, 0xA0000000)))
                .add(window);

        UiRoot root = new UiRoot(screen, ShowcaseTheme.dark());
        light.subscribe(on -> root.setTheme(ShowcaseTheme.carryTokens(
                on ? ShowcaseTheme.light() : ShowcaseTheme.dark(), root.theme())));
        select(0, pages, buttons, content);
        return root;
    }

    private static void select(int index, List<PageDef> pages, Button[] buttons, Panel content) {
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setState(State.CHECKED, i == index);
        }
        content.clearChildren();
        UiElement page = pages.get(index).factory().get();
        Ui.prepareFadeIn(page);
        content.addChild(page);
        Ui.playFadeIn(page);
    }
}
