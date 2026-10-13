package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.render.NineSlice;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Backdrop;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Chart;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Slider;
import io.github.ssbaxys.lumina.widget.Toggle;
import io.github.ssbaxys.lumina.widget.Window;
import io.github.ssbaxys.lumina.reactive.Signal;

/** Окна, которые можно таскать и растягивать, модальный диалог, живые обои и рамки «9 частей». */
final class WindowsPage {
    private static final String[] KIND_NAMES = {"Сияние", "Звёзды", "Волны", "Перелив", "Ретро"};

    private WindowsPage() {
    }

    static UiElement create() {
        return Ui.page(
                Ui.section(Tr.r("Окна и живые обои"),
                        Tr.r("Окна таскаются за заголовок, растягиваются за уголок, Esc или крестик закрывают. ")
                                + Tr.r("Обои под ними анимированы и сдвигаются за мышью."),
                        desk()),
                Ui.section(Tr.r("Рамка из текстуры"),
                        Tr.r("NineSlice растягивает картинку рамки на любой размер: углы остаются чёткими. ")
                                + Tr.r("В CSS: nine-slice: url(minecraft:textures/gui/demo_background.png) 248 166 4 in 256 256 / 2"),
                        nineSlice()));
    }

    private static UiElement desk() {
        Panel desk = Panel.box(new StackLayout(Align.START, Align.START)).width(Dim.FILL).height(Dim.px(250));
        desk.setClipChildren(true);
        desk.style(b -> b.set(Styles.RADIUS, Radii.all(8)));
        Backdrop[] bg = {new Backdrop(Backdrop.Kind.AURORA).parallax(10f)};
        desk.add(bg[0]);
        int[] opened = {0};
        Runnable spawn = () -> {
            int n = ++opened[0];
            Signal<Float> volume = Signal.of(50f);
            Window w = new Window(Tr.r("Окно ") + n,
                    Ui.row(6, new Label(Tr.r("Звук")), new Slider(volume, 0, 100).width(Dim.AUTO).minSize(90, 14).grow(1f)),
                    Ui.row(6, new Label(Tr.r("Музыка")), new Toggle(Signal.of(n % 2 == 0))),
                    Label.bound(volume.map(v -> Tr.r("Громкость: ") + Math.round(v) + "%")).style(Ui.MUTED))
                    .at(16 + (n * 26) % 200, 12 + (n * 18) % 90)
                    .minSize(120, 70);
            desk.add(w);
        };
        spawn.run();
        Chart mini = new Chart(Chart.Kind.LINE).labels("1", "2", "3", "4", "5").series("TPS", 0xFFA6E3A1, 20, 19.5f, 17, 20, 18.8f)
                .max(20).legend(false).unit(" TPS");
        // Без фиксированного размера: график растягивается вместе с окном.
        mini.width(Dim.AUTO).height(Dim.AUTO).minSize(170, 80).grow(1f);
        desk.add(new Window(Tr.r("Сервер"), mini).at(230, 20));

        Panel kinds = Ui.row(4);
        for (int i = 0; i < KIND_NAMES.length; i++) {
            Backdrop.Kind kind = Backdrop.Kind.values()[i];
            kinds.add(new Button(Tr.r(KIND_NAMES[i]), () -> {
                desk.removeChild(bg[0]);
                bg[0] = new Backdrop(kind).parallax(10f);
                desk.addChild(0, bg[0]);
            }).styleClass("button ghost"));
        }
        Button modal = new Button(Tr.r("Диалог"), () -> {
            Window dialog = new Window(Tr.r("Удалить мир?"),
                    Ui.muted(Tr.r("Это действие нельзя отменить.")),
                    Ui.row(6)).at(120, 80).modal(true).resizable(false);
            Panel actions = (Panel) dialog.body().children().get(1);
            actions.add(new Button(Tr.r("Удалить"), dialog::close).styleClass("button danger"),
                    new Button(Tr.r("Отмена"), dialog::close).styleClass("button ghost"));
            desk.add(dialog);
        });
        return Ui.column(6, Ui.row(6, new Button(Tr.r("Новое окно"), spawn), modal, kinds), desk);
    }

    private static UiElement nineSlice() {
        NineSlice frame = NineSlice.of("minecraft:textures/gui/demo_background.png", 248, 166, 4).inFile(256, 256);
        Panel small = framed(frame, 90, 50, "90×50");
        Panel wide = framed(frame, 220, 50, "220×50");
        Panel big = framed(frame.scaled(2f), 160, 90, Tr.r("масштаб 2"));
        return Ui.row(10, small, wide, big);
    }

    private static Panel framed(NineSlice frame, float w, float h, String text) {
        Panel p = Ui.centered(new Label(text).style(s -> s.set(Styles.TEXT_COLOR, 0xFF404040)));
        p.size(w, h);
        p.style(b -> b.set(Styles.NINE_SLICE, frame));
        return p;
    }
}
