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
    private record PageDef(String title, Supplier<UiElement> factory) {
    }

    private Showcase() {
    }

    public static UiRoot create(Runnable onClose) {
        Signal<Boolean> light = Signal.of(false);

        List<PageDef> pages = List.of(
                new PageDef("Обзор", OverviewPage::create),
                new PageDef("Раскладка", LayoutPage::create),
                new PageDef("Стили и темы", () -> StylePage.create(light)),
                new PageDef("Цвета", ColorsPage::create),
                new PageDef("Градиенты", GradientsPage::create),
                new PageDef("Анимации", AnimationPage::create),
                new PageDef("Эффекты", EffectsPage::create),
                new PageDef("Markdown", MarkdownPage::create),
                new PageDef("HTML и CSS", MarkupPage::create),
                new PageDef("Ввод и события", InputPage::create),
                new PageDef("Данные", DataPage::create),
                new PageDef("Производительность", PerformancePage::create));

        Panel content = Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH)).grow(1);
        content.setClipChildren(true);

        Panel nav = Panel.box(FlexLayout.column().gap(3)).width(Dim.px(124));
        nav.add(new Label("Lumina UI").scale(1.4f), Ui.muted("витрина возможностей"), Ui.column(0).size(1, 6));

        // Разделов много: на маленьком экране список прокручивается, а кнопки внизу остаются на месте.
        Panel links = Panel.box(FlexLayout.column().gap(3).align(Align.STRETCH));
        ScrollPanel linkScroll = new ScrollPanel(links).width(Dim.FILL).height(Dim.FILL);
        linkScroll.setStyleClass(null);
        nav.add(linkScroll);

        Button[] buttons = new Button[pages.size()];
        for (int i = 0; i < pages.size(); i++) {
            int index = i;
            buttons[i] = new Button(pages.get(i).title())
                    .styleClass("nav")
                    .width(Dim.FILL)
                    .clickSound(false)
                    .onPress(() -> select(index, pages, buttons, content));
            links.add(buttons[i]);
        }
        nav.add(Ui.column(0).size(1, 2),
                Ui.row(6, new Toggle(light), new Label("Светлая тема")),
                Ui.column(0).size(1, 4),
                new Button("Закрыть", onClose).width(Dim.FILL));

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
