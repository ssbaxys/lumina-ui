package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;

final class OverviewPage {
    private OverviewPage() {
    }

    static UiElement create() {
        return Ui.page(
                Ui.column(4,
                        new Label("Добро пожаловать в Lumina UI").scale(1.8f),
                        Ui.muted("Красивые, быстрые и гибкие интерфейсы для Minecraft"),
                        Ui.muted("1.20.1 Forge  •  1.21.1 NeoForge  •  один и тот же код UI")),
                Panel.box(FlexLayout.row().gap(10).align(Align.STRETCH)).width(Dim.FILL).add(
                        card("SDF-рендер", "Скругления, обводки,", "мягкие тени, градиенты"),
                        card("Flex-раскладка", "Строки, колонки, grow,", "выравнивание, слои"),
                        card("Анимации", "Твины, пружины и", "плавные смены стиля")),
                Panel.box(FlexLayout.row().gap(10).align(Align.STRETCH)).width(Dim.FILL).add(
                        card("Стили и темы", "Состояния hover/pressed,", "тёмная и светлая тема"),
                        card("События", "Захват и всплытие,", "фокус, клавиатура"),
                        card("Реактивность", "Сигналы и вычисляемые", "значения для данных")),
                Ui.section("Где открыть эту витрину", null,
                        new Label("•  Клавиша K в мире (настраивается в управлении)"),
                        new Label("•  Команда /luminatest"),
                        new Label("•  Кнопка «Lumina» в главном меню и меню паузы")),
                Ui.section("Попробуйте", "Карточки выше реагируют на наведение — это просто стиль темы \"card\".",
                        new Label("Слева разделы: каждый показывает свою часть библиотеки.")));
    }

    private static Panel card(String title, String line1, String line2) {
        return new Panel("card", FlexLayout.column().gap(3)).grow(1).add(
                new Label(title).style(b -> b.set(Styles.TEXT_COLOR, 0xFF7AA2FF)),
                Ui.muted(line1),
                Ui.muted(line2));
    }
}
