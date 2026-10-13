package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.i18n.Translations;

/**
 * Переводы витрины, заданные из кода: ключ, английский, русский. Так же можно положить их в
 * lang-файлы мода ({@code assets/<мод>/lang/<код>.json}) — библиотека читает оба источника.
 */
final class ShowcaseLang {
    private static final String PREFIX = "luminatest.";
    private static boolean registered;
    private static Translations en;
    private static Translations ru;

    private ShowcaseLang() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        I18n.declareLanguage("en_us", "English");
        I18n.declareLanguage("ru_ru", "Русский");
        en = I18n.translations("en_us");
        ru = I18n.translations("ru_ru");

        t("nav.overview",
                "Overview",
                "Обзор");
        t("nav.layout",
                "Layout",
                "Раскладка");
        t("nav.style",
                "Styles & themes",
                "Стили и темы");
        t("nav.anim",
                "Animations",
                "Анимации");
        t("nav.input",
                "Input & events",
                "Ввод и события");
        t("nav.data",
                "Data",
                "Данные");
        t("nav.perf",
                "Performance",
                "Производительность");
        t("nav.colors",
                "Colors",
                "Цвета");
        t("nav.gradients",
                "Gradients",
                "Градиенты");
        t("nav.effects",
                "Effects",
                "Эффекты");
        t("nav.markdown",
                "Markdown",
                "Markdown");
        t("nav.markup",
                "HTML & CSS",
                "HTML и CSS");
        t("nav.blocks",
                "Blocks",
                "Блоки");
        t("nav.modern",
                "Modern style",
                "Современный стиль");
        t("nav.structures",
                "3D builds",
                "3D-постройки");
        t("nav.fields",
                "Text fields",
                "Поля ввода");
        t("nav.buttons",
                "Buttons & checkboxes",
                "Кнопки и флажки");
        t("nav.components",
                "Components",
                "Компоненты");
        t("nav.windows",
                "Windows & wallpapers",
                "Окна и обои");
        t("nav.popups",
                "Menus & tooltips",
                "Меню и подсказки");
        t("nav.datatools",
                "Lists, forms, book",
                "Списки, формы, книга");
        t("nav.themes",
                "Themes & accessibility",
                "Темы и доступность");
        t("nav.items",
                "Items & mobs",
                "Предметы и мобы");
        t("nav.hud",
                "HUD & game data",
                "HUD и данные игры");
        t("nav.tools",
                "Maps, blur, settings",
                "Карты, размытие, настройки");
        t("subtitle",
                "feature showcase",
                "витрина возможностей");
        t("light_theme",
                "Light theme",
                "Светлая тема");
        t("close",
                "Close",
                "Закрыть");
        t("overview.welcome",
                "Welcome to Lumina UI",
                "Добро пожаловать в Lumina UI");
        t("overview.tagline",
                "Beautiful, fast and flexible interfaces for Minecraft",
                "Красивые, быстрые и гибкие интерфейсы для Minecraft");
        t("overview.versions",
                "1.20.1 Forge  •  1.21.1 NeoForge  •  the same UI code",
                "1.20.1 Forge  •  1.21.1 NeoForge  •  один и тот же код UI");
        t("card.sdf.title",
                "SDF rendering",
                "SDF-рендер");
        t("card.sdf.1",
                "Rounded corners, borders,",
                "Скругления, обводки,");
        t("card.sdf.2",
                "soft shadows, gradients",
                "мягкие тени, градиенты");
        t("card.flex.title",
                "Flex layout",
                "Flex-раскладка");
        t("card.flex.1",
                "Rows, columns, grow,",
                "Строки, колонки, grow,");
        t("card.flex.2",
                "alignment, layers",
                "выравнивание, слои");
        t("card.anim.title",
                "Animations",
                "Анимации");
        t("card.anim.1",
                "Tweens, springs and",
                "Твины, пружины и");
        t("card.anim.2",
                "smooth style changes",
                "плавные смены стиля");
        t("card.style.title",
                "Styles & themes",
                "Стили и темы");
        t("card.style.1",
                "Hover/pressed states,",
                "Состояния hover/pressed,");
        t("card.style.2",
                "dark and light theme",
                "тёмная и светлая тема");
        t("card.events.title",
                "Events",
                "События");
        t("card.events.1",
                "Capture and bubbling,",
                "Захват и всплытие,");
        t("card.events.2",
                "focus, keyboard",
                "фокус, клавиатура");
        t("card.reactive.title",
                "Reactivity",
                "Реактивность");
        t("card.reactive.1",
                "Signals and computed",
                "Сигналы и вычисляемые");
        t("card.reactive.2",
                "values for your data",
                "значения для данных");
        t("overview.language",
                "Interface language",
                "Язык интерфейса");
        t("overview.language.hint",
                "Switches on the fly, independent of the game language; the choice is saved",
                "Меняется на лету, независимо от языка игры; выбор сохраняется");
        t("overview.language.current",
                "Current: %s",
                "Сейчас: %s");
        t("overview.language.file",
                "From the mod lang file: %s",
                "Из lang-файла мода: %s");
        t("overview.where",
                "Where to open this showcase",
                "Где открыть эту витрину");
        t("overview.where.key",
                "•  K key in a world (rebindable in Controls)",
                "•  Клавиша K в мире (настраивается в управлении)");
        t("overview.where.command",
                "•  The /luminatest command",
                "•  Команда /luminatest");
        t("overview.where.button",
                "•  The «Lumina» button in the title and pause menus",
                "•  Кнопка «Lumina» в главном меню и меню паузы");
        t("overview.try",
                "Try it",
                "Попробуйте");
        t("overview.try.hint",
                "The cards above react to hover — it is just the «card» theme style.",
                "Карточки выше реагируют на наведение — это просто стиль темы «card».");
        t("overview.try.text",
                "Sections on the left: each one shows a part of the library.",
                "Слева разделы: каждый показывает свою часть библиотеки.");
        t("overview.language.auto",
                "Same as game",
                "Как в игре");
        t("layout.centered",
                "Centered (StackLayout)",
                "По центру (StackLayout)");
        t("layout.header",
                "header",
                "шапка");
        t("layout.menu",
                "menu",
                "меню");
        t("layout.content",
                "content (grow)",
                "содержимое (grow)");
        t("layout.side",
                "sidebar",
                "панель");
        t("layout.footer",
                "footer",
                "подвал");
        t("layout.justify.hint",
                "Distribution along the main axis",
                "Распределение по главной оси");
        t("layout.grow.hint",
                "Free space is split 1 : 2 : 3",
                "Свободное место делится в пропорции 1 : 2 : 3");
        t("layout.align.hint",
                "Alignment along the cross axis",
                "Выравнивание по поперечной оси");
        t("layout.layers",
                "Layers and absolute positions",
                "Слои и абсолютные позиции");
        t("layout.nested",
                "Nested layouts",
                "Вложенные раскладки");
        t("layout.nested.hint",
                "The classic page scheme built from rows and columns",
                "Классическая схема страницы из строк и колонок");
        t("style.pill",
                "pill",
                "пилюля");
        t("style.mixed",
                "mixed",
                "разные");
        t("style.none",
                "none",
                "нет");
        t("style.small",
                "small",
                "малая");
        t("style.large",
                "large",
                "большая");
        t("style.glow",
                "glow",
                "свечение");
        t("style.hard",
                "hard",
                "жёсткая");
        t("style.solid",
                "solid",
                "сплошная");
        t("style.vertical",
                "vertical",
                "вертикальный");
        t("style.horizontal",
                "horizontal",
                "горизонтальный");
        t("style.fade",
                "to transparent",
                "в прозрачность");
        t("style.border",
                "border",
                "обводка");
        t("style.hover",
                "hover me",
                "наведи");
        t("style.press",
                "press me",
                "нажми");
        t("style.color",
                "color",
                "цвет");
        t("style.accent",
                "Accent:",
                "Акцент:");
        t("style.radii",
                "Rounded corners",
                "Скругления");
        t("style.radii.hint",
                "Each corner can have its own radius",
                "Каждый угол может иметь свой радиус");
        t("style.shadows",
                "Shadows",
                "Тени");
        t("style.shadows.hint",
                "Soft shadows are drawn by a shader, no textures",
                "Мягкие тени рисуются шейдером, без текстур");
        t("style.states",
                "States and transitions",
                "Состояния и переходы");
        t("style.states.hint",
                "Style variants for hover/pressed with a smooth transition",
                "Варианты стиля для hover/pressed с плавным переходом");
        t("style.theme",
                "Theme",
                "Тема");
        t("style.theme.hint",
                "Theme switching on the fly: styles blend smoothly",
                "Переключение темы на лету: стили перетекают плавно");
        t("style.inherited",
                "Inherited accent",
                "Наследуемый акцент");
        t("style.inherited.hint",
                "ACCENT_COLOR is set on the root and inherited by every widget",
                "ACCENT_COLOR задаётся на корне и наследуется всеми виджетами");
        t("style.fills",
                "Fills and borders",
                "Заливки и обводки");
        t("anim.run",
                "Run",
                "Запустить");
        t("anim.springs.tip",
                "A spring can be redirected midway — press twice",
                "Пружину можно перенаправить на полпути — нажмите дважды");
        t("anim.morph.label",
                "Style with a transition",
                "Стиль с переходом");
        t("anim.morph.button",
                "Change",
                "Изменить");
        t("anim.easing",
                "Easing functions",
                "Функции плавности");
        t("anim.easing.hint",
                "Same duration — different character of motion",
                "Одинаковая длительность — разный характер движения");
        t("anim.springs",
                "Springs",
                "Пружины");
        t("anim.springs.hint",
                "Physics: stiffness and damping instead of duration",
                "Физика: жёсткость и затухание вместо длительности");
        t("anim.morph",
                "A style transition that affects layout",
                "Переход стиля, влияющий на раскладку");
        t("anim.morph.hint",
                "Padding animates, neighbours shift smoothly",
                "Отступы анимируются, соседи плавно сдвигаются");
        t("anim.endless",
                "Endless animation",
                "Бесконечная анимация");
        t("anim.endless.hint",
                "A custom Animation in the root animator; stops by itself on close",
                "Своя Animation в аниматоре корня; останавливается сама при закрытии");
        t("anim.progress",
                "Smooth progress",
                "Плавный прогресс");
        t("anim.idle",
                "The animator is empty when nothing moves",
                "Аниматор пуст, когда ничего не движется");
        t("input.click",
                "Click",
                "Клик");
        t("input.reset",
                "Reset",
                "Сброс");
        t("input.pressed",
                "Pressed: %s",
                "Нажато: %s");
        t("input.first",
                "First",
                "Первая");
        t("input.second",
                "Second",
                "Вторая");
        t("input.third",
                "Third",
                "Третья");
        t("input.clicks",
                "Clicks",
                "Клики");
        t("input.drag",
                "Dragging",
                "Перетаскивание");
        t("input.drag.hint",
                "Pointer capture: DRAG goes to the element that was pressed",
                "Захват указателя: DRAG приходит тому, на ком нажали");
        t("input.focus",
                "Focus and keyboard",
                "Фокус и клавиатура");
        t("input.focus.hint",
                "Tab / Shift+Tab move focus, Enter or Space press the button",
                "Tab / Shift+Tab переключают фокус, Enter или пробел нажимают кнопку");
        t("input.log",
                "Event log",
                "Журнал событий");
        t("input.log.hint",
                "Capture (parent) fires before bubbling (target)",
                "Захват (родитель) срабатывает раньше всплытия (цель)");
        t("input.drag.handle",
                "Drag me",
                "Тащи меня");
        t("input.zone",
                "Hover, click, scroll the wheel",
                "Наведи, кликни, покрути колесо");
        t("input.log.capture",
                "capture  click  (parent)",
                "capture  click  (родитель)");
        t("input.log.press",
                "press    button %s",
                "press    кнопка %s");
        t("input.log.release",
                "release  button %s",
                "release  кнопка %s");
        t("input.log.bubble",
                "bubble   click  (target)",
                "bubble   click  (цель)");
        t("input.log.scroll.up",
                "scroll   up",
                "scroll   вверх");
        t("input.log.scroll.down",
                "scroll   down",
                "scroll   вниз");
        t("input.key.prompt",
                "Press any key",
                "Нажмите любую клавишу");
        t("input.key.code",
                "Key: code %s",
                "Клавиша: код %s");
        t("data.volume",
                "Volume: %s%%",
                "Громкость: %s%%");
        t("data.action",
                "Action",
                "Действие");
        t("data.on",
                "Enabled",
                "Включено");
        t("data.off",
                "Disabled",
                "Выключено");
        t("data.add",
                "Add task",
                "Добавить задачу");
        t("data.tasks",
                "Tasks: %s",
                "Задач: %s");
        t("data.task",
                "Task #%s",
                "Задача №%s");
        t("data.binding",
                "Signal binding",
                "Привязка к сигналу");
        t("data.binding.hint",
                "The slider, text and bar read the same Signal",
                "Ползунок, текст и индикатор читают один Signal");
        t("data.computed",
                "Computed values",
                "Вычисляемые значения");
        t("data.computed.hint",
                "Computed finds its own signal dependencies",
                "Computed сам находит, от каких сигналов зависит");
        t("data.list",
                "Dynamic list",
                "Динамический список");
        t("data.list.hint",
                "Items are added and removed on the fly",
                "Элементы добавляются и удаляются на лету");
        t("data.state",
                "Widget state",
                "Состояние виджетов");
        t("perf.row",
                "A row of a big list",
                "Строка большого списка");
        t("perf.frame",
                "Frame time",
                "Время кадра");
        t("perf.frame.hint",
                "Measured right in the showcase rendering",
                "Измеряется в самой отрисовке витрины");
        t("perf.grid",
                "Animated grid",
                "Анимированная сетка");
        t("perf.grid.hint",
                "%s shapes change size and color every frame",
                "%s фигур меняют размер и цвет каждый кадр");
        t("perf.rounded",
                "Rounded (SDF shader); off — rectangles in one batch",
                "Скругления (SDF-шейдер); выкл. — прямоугольники одним батчем");
        t("perf.list",
                "Big list",
                "Большой список");
        t("perf.list.hint",
                "%s rows: layout is cached, invisible rows are skipped",
                "%s строк: раскладка кэшируется, невидимые строки не обходятся");
        t("perf.meter.empty",
                "Frame: —",
                "Кадр: —");
        t("perf.meter",
                "Frame: %s ms  •  ~%s FPS",
                "Кадр: %s мс  •  ~%s FPS");
    }

    private static void t(String key, String english, String russian) {
        en.add(PREFIX + key, english);
        ru.add(PREFIX + key, russian);
    }
}
