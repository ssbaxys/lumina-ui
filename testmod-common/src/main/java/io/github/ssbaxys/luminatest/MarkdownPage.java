package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.markdown.Markdown;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;

/** Markdown: полноценная разметка с переносом по ширине, ссылками, задачами и иконками предметов. */
final class MarkdownPage {
    private static final String GUIDE = """
            # Markdown в Lumina UI
            Виджет `Markdown` рисует **полноценную разметку**: *курсив*, ~~зачёркнутый~~, ++подчёркнутый++,
            ==выделение== и ***всё сразу***. Текст переносится по ширине окна — попробуйте изменить размер игры.

            ## Списки и задачи
            - Маркированные списки
              - со вложенностью
                - любой глубины
            1. Нумерованные
            2. тоже есть
            - [x] Задачи можно отмечать кликом
            - [ ] Попробуйте отметить эту
            - [ ] И эту

            ## Код
            Строчный `код` и блоки с подсветкой фона:
            ```java
            new Markdown(text)
                    .onLink(url -> openScreen(url))
                    .onTaskToggle((item, done) -> save());
            ```

            > Цитаты тоже поддерживаются.
            > Внутри них работает **вся разметка**.

            ## Таблицы
            | Эффект | Текстура | Звук |
            |:-------|:--------:|-----:|
            | Огонь | `fire_0` | flintandsteel |
            | Взрыв | `explosion_0..15` | generic.explode |
            | TNT | `tnt_side` | tnt.primed |

            ## Иконки и ссылки
            Предметы прямо в тексте: ![](item:minecraft:diamond) алмаз, ![](item:minecraft:tnt) динамит,
            ![](item:minecraft:blaze_rod) стержень ифрита, текстура ![](texture:minecraft:textures/block/fire_0.png).

            Ссылки кликабельны: [страница эффектов](lumina:effects), [градиенты](lumina:gradients),
            автоссылка <https://github.com/ssbaxys/lumina-ui>.

            ---
            Экранирование: \\*не курсив\\*, snake_case_слова не ломаются.
            """;

    private static final String SHORT = """
            ### Обновление 0.2
            * **Цвета:** RGB, HSV, HSL, OKLCH, разбор `"rgb(255 128 0 / 50%)"`
            * **Градиенты:** линейные, радиальные, конические, до 8 цветов
            * **Анимации:** ключевые кадры, последовательности, циклы
            * **Эффекты:** ![](item:minecraft:flint_and_steel) огонь, ![](item:minecraft:tnt) взрывы
            """;

    private MarkdownPage() {
    }

    static UiElement create() {
        Signal<String> lastLink = Signal.of("—");
        Signal<String> text = Signal.of(Tr.r(GUIDE));
        Markdown md = Markdown.bound(text)
                .onLink(lastLink::set)
                .onTaskToggle((item, done) -> lastLink.set(done ? Tr.r("задача отмечена") : Tr.r("отметка снята")));
        Panel card = Ui.column(0, md).width(Dim.FILL);
        card.style(b -> b
                .set(Styles.BACKGROUND, Brush.solid(0x14808080))
                .set(Styles.RADIUS, Radii.all(8))
                .set(Styles.PADDING, Insets.all(10)));

        return Ui.page(
                Ui.row(6,
                        new Button(Tr.r("Полный пример"), () -> text.set(Tr.r(GUIDE))),
                        new Button(Tr.r("Короткая заметка"), () -> text.set(Tr.r(SHORT))).styleClass("button ghost"),
                        Ui.muted(Tr.r("Последнее действие:")),
                        Label.bound(lastLink)),
                card);
    }
}
