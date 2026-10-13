package io.github.ssbaxys.luminatest;

import java.util.HashMap;
import java.util.Map;

/** Английские тексты витрины: ключ — русская строка из кода страниц, значение — перевод. */
final class ShowcaseEnglish {
    static final Map<String, String> TEXT = new HashMap<>();

    private ShowcaseEnglish() {
    }

    private static void part0() {
        TEXT.put("Галерея кривых",
                "Easing gallery");
        TEXT.put("Все 23 встроенные кривые; есть ещё steps(n), spring(), cubicBezier()",
                "All 23 built-in curves; there are also steps(n), spring(), cubicBezier()");
        TEXT.put("Ключевые кадры",
                "Keyframes");
        TEXT.put("animate(key).keyframe(...).loop().yoyo() — любое свойство стиля",
                "animate(key).keyframe(...).loop().yoyo() — any style property");
        TEXT.put("Последовательности",
                "Sequences");
        TEXT.put("Motion.sequence / parallel / delay / call — как таймлайн",
                "Motion.sequence / parallel / delay / call — like a timeline");
        TEXT.put("дыхание",
                "breathing");
        TEXT.put("сжатие",
                "squash");
        TEXT.put("готово",
                "done");
        TEXT.put("падают по очереди…",
                "dropping one by one…");
        TEXT.put("крайние вместе",
                "outer ones together");
        TEXT.put("Запустить",
                "Run");
        TEXT.put("Карта из блоков",
                "Block map");
        TEXT.put("Каждая клетка — блок игры со своей текстурой; вода и огонь анимируются. Кликните по TNT.",
                "Each cell is a game block with its own texture; water and fire are animated. Click the TNT.");
        TEXT.put("Редактор",
                "Editor");
        TEXT.put("ЛКМ — поставить выбранный блок (можно вести мышью), ПКМ — сломать.",
                "LMB places the selected block (you can drag), RMB breaks it.");
        TEXT.put("Очистить",
                "Clear");
        TEXT.put("Как код",
                "As code");
        TEXT.put("Блок: ",
                "Block: ");
        TEXT.put("Флажки",
                "Checkboxes");
        TEXT.put("Checkbox: свой вид анимации у каждого Kind; подпись тоже кликабельна, Space/Enter в фокусе",
                "Checkbox: every Kind has its own animation; the label is clickable too, Space/Enter when focused");
        TEXT.put("Неопределённое состояние",
                "Indeterminate state");
        TEXT.put("Kind.CHECK: черта, когда выбрана только часть",
                "Kind.CHECK: a dash when only some are selected");
        TEXT.put("Радио-группы",
                "Radio groups");
        TEXT.put("RadioGroup<T>: ровно один вариант в Signal<T>, стрелки двигают выбор",
                "RadioGroup<T>: exactly one option in a Signal<T>, arrow keys move the selection");
        TEXT.put("Варианты кнопок",
                "Button variants");
        TEXT.put("Иконки",
                "Icons");
        TEXT.put(".item(\"minecraft:diamond\"), .icon(TextureRef), Button.round(...) — круглые кнопки-иконки",
                ".item(\"minecraft:diamond\"), .icon(TextureRef), Button.round(...) — round icon buttons");
        TEXT.put("Загрузка",
                "Loading");
        TEXT.put(".loading(true): спиннер вместо иконки, кнопка недоступна",
                ".loading(true): a spinner instead of the icon, the button is disabled");
        TEXT.put("Кнопки-картинки",
                "Image buttons");
        TEXT.put("ImageButton: картинка на всю кнопку, подпись снизу, при наведении — приближение",
                "ImageButton: a picture across the whole button, caption below, zooms on hover");
        TEXT.put("Картинки",
                "Images");
        TEXT.put("Image: CONTAIN, COVER, STRETCH и скругление; Images.fromFile читает PNG/JPG/GIF/BMP с диска",
                "Image: CONTAIN, COVER, STRETCH and rounding; Images.fromFile reads PNG/JPG/GIF/BMP from disk");
        TEXT.put("Крупный",
                "Large");
        TEXT.put("Недоступный",
                "Disabled");
        TEXT.put("Мечи",
                "Swords");
        TEXT.put("Броня",
                "Armor");
        TEXT.put("Зелья",
                "Potions");
        TEXT.put("Выбрать всё",
                "Select all");
        TEXT.put("Нормальный",
                "Normal");
        TEXT.put("Мирный",
                "Peaceful");
        TEXT.put("Лёгкий",
                "Easy");
        TEXT.put("Сложный",
                "Hard");
        TEXT.put("Сложность: ",
                "Difficulty: ");
        TEXT.put("Размер: ",
                "Size: ");
        TEXT.put(" (одна переменная на две группы)",
                " (one variable for two groups)");
        TEXT.put("Обычная",
                "Regular");
        TEXT.put("Большая кнопка",
                "Big button");
        TEXT.put("Недоступная",
                "Disabled");
        TEXT.put("Алмаз",
                "Diamond");
        TEXT.put("Изумруд",
                "Emerald");
        TEXT.put("Яблоко",
                "Apple");
        TEXT.put("Жемчуг",
                "Pearl");
        TEXT.put("Взорвать",
                "Detonate");
        TEXT.put("круглые: styleClass(\"button icon ...\")",
                "round: styleClass(\"button icon ...\")");
        TEXT.put("Играть",
                "Play");
        TEXT.put("Новый мир",
                "New world");
        TEXT.put("Сохранить",
                "Save");
        TEXT.put("Отправить",
                "Send");
        TEXT.put("Ждём сервер",
                "Waiting for server");
        TEXT.put("нажмите — пара секунд «работы»",
                "click — a couple of seconds of «work»");
        TEXT.put("Север",
                "North");
        TEXT.put("Восток",
                "East");
        TEXT.put("Юг",
                "South");
        TEXT.put("Запад",
                "West");
        TEXT.put("Небо",
                "Sky");
        TEXT.put("Земля",
                "Ground");
        TEXT.put("Панорама: ",
                "Panorama: ");
        TEXT.put("Дуб",
                "Oak");
        TEXT.put("Кирпичи",
                "Bricks");
        TEXT.put("Алмазная руда",
                "Diamond ore");
        TEXT.put("Вишня",
                "Cherry");
        TEXT.put("Аметист",
                "Amethyst");
        TEXT.put("Выбрано: ",
                "Selected: ");
        TEXT.put("Сделайте скриншот (F2) — здесь появится последний из папки screenshots",
                "Take a screenshot (F2) — the latest one from the screenshots folder will appear here");
        TEXT.put("Не удалось загрузить ",
                "Failed to load ");
        TEXT.put("Последний скриншот",
                "Latest screenshot");
        TEXT.put("контраст с белым %.1f:1",
                "contrast with white %.1f:1");
        TEXT.put("Красить тему",
                "Tint the theme");
        TEXT.put("Готовые:",
                "Presets:");
        TEXT.put("Выбор цвета",
                "Color picker");
        TEXT.put("Квадрат насыщенности и яркости, оттенок, прозрачность. ",
                "Saturation and brightness square, hue, opacity. ");
        TEXT.put("Цвет сразу становится акцентом темы",
                "The color immediately becomes the theme accent");
        TEXT.put("RGB и HSV",
                "RGB and HSV");
        TEXT.put("Каналы двигаются вместе: меняете один — пересчитываются остальные",
                "Channels move together: change one and the others are recalculated");
        TEXT.put("Операции над цветом",
                "Color operations");
        TEXT.put("Считаются в OKLCH — светлота и насыщенность ощущаются ровно",
                "Computed in OKLCH — lightness and saturation feel even");
        TEXT.put("Смешивание в разных пространствах",
                "Mixing in different spaces");
        TEXT.put("Один и тот же переход между двумя цветами",
                "The same transition between two colors");
        TEXT.put("исходный",
                "original");
        TEXT.put("светлее",
                "lighter");
        TEXT.put("темнее",
                "darker");
        TEXT.put("насыщ.",
                "saturated");
        TEXT.put("тусклее",
                "duller");
        TEXT.put("дополн.",
                "complement");
        TEXT.put("серый",
                "gray");
        TEXT.put("инверсия",
                "inverted");
        TEXT.put("текст",
                "text");
        TEXT.put("sRGB темнеет в середине, OKLAB ровный, OKLCH и HSV идут по кругу оттенков",
                "sRGB darkens in the middle, OKLAB is even, OKLCH and HSV go around the hue wheel");
        TEXT.put("Уголь",
                "Coal");
        TEXT.put("Железо",
                "Iron");
        TEXT.put("Медь",
                "Copper");
        TEXT.put("Золото",
                "Gold");
        TEXT.put("Редстоун",
                "Redstone");
        TEXT.put("Лазурит",
                "Lapis");
        TEXT.put("Текст с эффектами",
                "Text with effects");
        TEXT.put("Градиент, радуга, обводка, волна, дрожание и печатная машинка — посимвольно обычным шрифтом.",
                "Gradient, rainbow, outline, wave, shake and typewriter — per character with the regular font.");
        TEXT.put("Таблица",
                "Table");
        TEXT.put("Клик по заголовку сортирует (повторный — в обратную сторону), клик по строке выбирает.",
                "Clicking a header sorts (again — in reverse), clicking a row selects it.");
        TEXT.put("Дерево",
                "Tree");
        TEXT.put("Стрелка или двойной клик раскрывают узел; стрелки клавиатуры тоже работают.",
                "The arrow or a double click expands a node; keyboard arrows work too.");
        TEXT.put("Графики",
                "Charts");
        TEXT.put("Значения вырастают при появлении и плавно перетекают к новым; наведите на точку.",
                "Values grow on appearance and flow smoothly to new ones; hover a point.");
        TEXT.put("Спиннеры и скелетоны для экранов, которые ждут данных.",
                "Spinners and skeletons for screens waiting for data.");
        TEXT.put("Добро пожаловать в Lumina UI! Этот текст печатается сам.",
                "Welcome to Lumina UI! This text types itself.");
        TEXT.put("Радуга",
                "Rainbow");
        TEXT.put("Волна",
                "Wave");
        TEXT.put("Тряска!",
                "Shake!");
        TEXT.put("Обводка",
                "Outline");
    }

    private static void part1() {
        TEXT.put("Ещё раз",
                "Again");
        TEXT.put("Руда",
                "Ore");
        TEXT.put("Кирка",
                "Pickaxe");
        TEXT.put("деревянная",
                "wooden");
        TEXT.put("каменная",
                "stone");
        TEXT.put("железная",
                "iron");
        TEXT.put("Добыто",
                "Mined");
        TEXT.put("Шанс",
                "Chance");
        TEXT.put("Ничего не выбрано",
                "Nothing selected");
        TEXT.put(", добыто ",
                ", mined ");
        TEXT.put("Мир",
                "World");
        TEXT.put("Верхний мир",
                "Overworld");
        TEXT.put("Деревня",
                "Village");
        TEXT.put("Кузница",
                "Smithy");
        TEXT.put("Библиотека",
                "Library");
        TEXT.put("Шахта",
                "Mine");
        TEXT.put("Незер",
                "Nether");
        TEXT.put("Крепость",
                "Fortress");
        TEXT.put("Бастион",
                "Bastion");
        TEXT.put("Энд",
                "End");
        TEXT.put("Город Энда",
                "End City");
        TEXT.put("Выберите узел",
                "Select a node");
        TEXT.put("Узел: ",
                "Node: ");
        TEXT.put("Пн",
                "Mon");
        TEXT.put("Вт",
                "Tue");
        TEXT.put("Ср",
                "Wed");
        TEXT.put("Чт",
                "Thu");
        TEXT.put("Пт",
                "Fri");
        TEXT.put("Сб",
                "Sat");
        TEXT.put("Вс",
                "Sun");
        TEXT.put("Камень",
                "Stone");
        TEXT.put(" шт.",
                " pcs");
        TEXT.put("Игроки онлайн",
                "Players online");
        TEXT.put("Блоки",
                "Blocks");
        TEXT.put("Новые данные",
                "New data");
        TEXT.put("в сети · 12 ч",
                "online · 12 h");
        TEXT.put("Любит строить башни из булыжника и терять кирки в лаве.",
                "Loves building cobblestone towers and losing pickaxes in lava.");
        TEXT.put("кольцо",
                "ring");
        TEXT.put("точки",
                "dots");
        TEXT.put("полоски",
                "bars");
        TEXT.put("пульс",
                "pulse");
        TEXT.put("Загрузить / сбросить",
                "Load / reset");
        TEXT.put("Обычный",
                "Common");
        TEXT.put("Необычный",
                "Uncommon");
        TEXT.put("Редкий",
                "Rare");
        TEXT.put("Эпический",
                "Epic");
        TEXT.put("Ресурсы",
                "Resources");
        TEXT.put("Золотой слиток",
                "Gold Ingot");
        TEXT.put("Железный слиток",
                "Iron Ingot");
        TEXT.put("Незеритовый слиток",
                "Netherite Ingot");
        TEXT.put("Блок травы",
                "Grass Block");
        TEXT.put("Дубовые доски",
                "Oak Planks");
        TEXT.put("Стекло",
                "Glass");
        TEXT.put("Динамит",
                "TNT");
        TEXT.put("Книжная полка",
                "Bookshelf");
        TEXT.put("Обсидиан",
                "Obsidian");
        TEXT.put("Алмазный меч",
                "Diamond Sword");
        TEXT.put("Инструменты",
                "Tools");
        TEXT.put("Железная кирка",
                "Iron Pickaxe");
        TEXT.put("Лук",
                "Bow");
        TEXT.put("Удочка",
                "Fishing Rod");
        TEXT.put("Щит",
                "Shield");
        TEXT.put("Еда",
                "Food");
        TEXT.put("Хлеб",
                "Bread");
        TEXT.put("Стейк",
                "Steak");
        TEXT.put("Золотая морковь",
                "Golden Carrot");
        TEXT.put("Торт",
                "Cake");
        TEXT.put("Список на 10 000 предметов",
                "A list of 10,000 items");
        TEXT.put("VirtualList рисует только видимые строки; поиск, фильтры и сортировка — DataView",
                "VirtualList draws only the visible rows; search, filters and sorting come from DataView");
        TEXT.put("Таблица со страницами",
                "Table with pages");
        TEXT.put("Тот же поиск, по 10 строк на страницу; клик по заголовку сортирует все страницы",
                "The same search, 10 rows per page; clicking a header sorts all pages");
        TEXT.put("Регистрация",
                "Sign-up");
        TEXT.put("Проверки полей, ошибки под полями, кнопка проверяет всю форму",
                "Field checks, errors under the fields, the button validates the whole form");
        TEXT.put("Мастер настройки",
                "Setup wizard");
        TEXT.put("Шаги с проверкой: «Далее» не пустит дальше с ошибкой",
                "Steps with validation: «Next» will not let you continue with an error");
        TEXT.put("Книга",
                "Book");
        TEXT.put("Страницы из Markdown, перелистывание стрелками, колесом и клавишами",
                "Pages from Markdown, turned with the arrows, the wheel and the keys");
        TEXT.put("Подсветка кода",
                "Code highlighting");
        TEXT.put("Блоки ```java, ```json, ```css и ```html раскрашиваются по теме",
                "```java, ```json, ```css and ```html blocks are colored by the theme");
        TEXT.put("блоки",
                "blocks");
        TEXT.put("редкие",
                "rare");
        TEXT.put("По номеру",
                "By number");
        TEXT.put("По названию",
                "By name");
        TEXT.put("По редкости",
                "By rarity");
        TEXT.put("Поиск: «алмаз», «еда 12»…",
                "Search: «diamond», «food 12»…");
        TEXT.put("Только блоки",
                "Blocks only");
        TEXT.put("Редкие и выше",
                "Rare and above");
        TEXT.put("Выберите предмет (двойной клик — «выдать»)",
                "Select an item (double click — «give»)");
        TEXT.put("Ничего не найдено",
                "Nothing found");
        TEXT.put("Выдано: ",
                "Given: ");
        TEXT.put("Найдено: ",
                "Found: ");
        TEXT.put(" из ",
                " of ");
        TEXT.put("Порядок:",
                "Order:");
        TEXT.put("Поиск по названию",
                "Search by name");
        TEXT.put("Предмет",
                "Item");
        TEXT.put("Категория",
                "Category");
        TEXT.put("Редкость",
                "Rarity");
        TEXT.put("Кол-во",
                "Qty");
        TEXT.put("Страница ",
                "Page ");
        TEXT.put(" · строк: ",
                " · rows: ");
        TEXT.put("Заполните форму и нажмите «Создать аккаунт»",
                "Fill in the form and press «Create account»");
        TEXT.put("Придумайте ник",
                "Choose a nickname");
        TEXT.put("Только латиница, цифры и _",
                "Latin letters, digits and _ only");
        TEXT.put("не меньше 6 символов",
                "at least 6 characters");
        TEXT.put("Добавьте хотя бы одну цифру",
                "Add at least one digit");
        TEXT.put("Повторите пароль",
                "Repeat the password");
        TEXT.put("Пароли не совпадают",
                "Passwords do not match");
        TEXT.put("Возраст от 13 до 120 лет",
                "Age from 13 to 120");
        TEXT.put("Согласен с правилами сервера",
                "I agree to the server rules");
        TEXT.put("Нужно согласиться с правилами",
                "You must agree to the rules");
        TEXT.put("Аккаунт «",
                "Account «");
        TEXT.put("» создан! Письмо отправлено на ",
                "» created! An email was sent to ");
        TEXT.put("Исправьте ошибки: ",
                "Fix the errors: ");
        TEXT.put("Создать аккаунт",
                "Create account");
        TEXT.put("Сбросить",
                "Reset");
        TEXT.put("Форма очищена",
                "Form cleared");
        TEXT.put("✔ Все поля заполнены верно",
                "✔ All fields are filled in correctly");
        TEXT.put("Есть незаполненные поля",
                "Some fields are empty");
        TEXT.put("Ник",
                "Nickname");
        TEXT.put("3–16 символов",
                "3–16 characters");
    }

    private static void part2() {
        TEXT.put("Почта",
                "Email");
        TEXT.put("Возраст",
                "Age");
        TEXT.put("Пароль",
                "Password");
        TEXT.put("Повтор пароля",
                "Repeat password");
        TEXT.put("Выживание",
                "Survival");
        TEXT.put("Как будет называться сервер?",
                "What will the server be called?");
        TEXT.put("Название",
                "Name");
        TEXT.put("Мой сервер",
                "My server");
        TEXT.put("Введите название",
                "Enter a name");
        TEXT.put("Сколько игроков смогут зайти одновременно?",
                "How many players can join at once?");
        TEXT.put("Слотов",
                "Slots");
        TEXT.put("От 1 до 100 игроков",
                "From 1 to 100 players");
        TEXT.put("Сервер",
                "Server");
        TEXT.put("Режим",
                "Mode");
        TEXT.put("Выберите режим игры",
                "Choose a game mode");
        TEXT.put("Творческий",
                "Creative");
        TEXT.put("Хардкор",
                "Hardcore");
        TEXT.put("Приключение",
                "Adventure");
        TEXT.put("Игроки",
                "Players");
        TEXT.put("Готово",
                "Done");
        TEXT.put("Проверьте настройки",
                "Check the settings");
        TEXT.put("Название: ",
                "Name: ");
        TEXT.put("Режим: ",
                "Mode: ");
        TEXT.put("Игроков: ",
                "Players: ");
        TEXT.put("Назад",
                "Back");
        TEXT.put("Далее",
                "Next");
        TEXT.put("Создать сервер",
                "Create server");
        TEXT.put("Сервер «",
                "Server «");
        TEXT.put("» создан: ",
                "» created: ");
        TEXT.put(" слотов",
                " slots");
        TEXT.put("Современная книга: страница ",
                "Modern book: page ");
        TEXT.put("« Оглавление",
                "« Contents");
        TEXT.put("Ванильный вид: текстура книги игры,",
                "Vanilla look: the game's book texture,");
        TEXT.put("одна страница и стрелки внизу.",
                "one page and arrows at the bottom.");
        TEXT.put("Разрыв страницы — <!-- page -->.",
                "Page break — <!-- page -->.");
        TEXT.put("Огонь",
                "Fire");
        TEXT.put("Огонь душ",
                "Soul fire");
        TEXT.put("Уронить TNT",
                "Drop TNT");
        TEXT.put("Взрыв",
                "Explosion");
        TEXT.put("Ливень TNT",
                "TNT rain");
        TEXT.put("Пламя",
                "Flame");
        TEXT.put("Лава",
                "Lava");
        TEXT.put("Дым",
                "Smoke");
        TEXT.put("Погасить",
                "Put out");
        TEXT.put("Снег",
                "Snow");
        TEXT.put("Дождь",
                "Rain");
        TEXT.put("Лепестки вишни",
                "Cherry petals");
        TEXT.put("Капли",
                "Drips");
        TEXT.put("Пузыри",
                "Bubbles");
        TEXT.put("Сердечки",
                "Hearts");
        TEXT.put("Злой житель",
                "Angry villager");
        TEXT.put("Довольный",
                "Happy");
        TEXT.put("Нота",
                "Note");
        TEXT.put("Крит",
                "Crit");
        TEXT.put("Тотем",
                "Totem");
        TEXT.put("Зачарование",
                "Enchanting");
        TEXT.put("Случайный из реестра",
                "Random from registry");
        TEXT.put(" эффектов в реестре",
                " effects in the registry");
        TEXT.put("блок",
                "block");
        TEXT.put("Сцена",
                "Scene");
        TEXT.put("Кликните по сцене — будет взрыв. TNT падает, отскакивает, мигает и взрывается, ",
                "Click the scene for an explosion. TNT falls, bounces, flashes and explodes, ");
        TEXT.put("окно трясётся",
                "the window shakes");
        TEXT.put("Погода",
                "Weather");
        TEXT.put("Ванильные текстуры дождя и снега двумя слоями, брызги, хлопья, лепестки, капли",
                "Vanilla rain and snow textures in two layers, splashes, flakes, petals, drips");
        TEXT.put("Частицы мобов и предметов",
                "Mob and item particles");
        TEXT.put("Сердечки, жители, ноты (цвет по высоте), криты, тотем, ",
                "Hearts, villagers, notes (color by pitch), crits, totem, ");
        TEXT.put("руны зачарования",
                "enchanting runes");
        TEXT.put("Свои эффекты",
                "Custom effects");
        TEXT.put("FxRegistry.register(\"мод:имя\", (x, y, w, h) -> эффект) — и ",
                "FxRegistry.register(\"mod:name\", (x, y, w, h) -> effect) — and ");
        TEXT.put("fx.spawn(\"мод:имя\", ...). Частицы собираются из готовых деталей",
                "fx.spawn(\"mod:name\", ...). Particles are assembled from ready-made parts");
        TEXT.put("Анимированные спрайты",
                "Animated sprites");
        TEXT.put("SpriteSheet: полосы кадров как у ванильных текстур и ",
                "SpriteSheet: frame strips like vanilla textures and ");
        TEXT.put("наборы отдельных кадров частиц",
                "sets of separate particle frames");
        TEXT.put("Предметы",
                "Items");
        TEXT.put("canvas.drawItem(\"minecraft:tnt\", x, y, size) — как в инвентаре",
                "canvas.drawItem(\"minecraft:tnt\", x, y, size) — like in the inventory");
        TEXT.put("Золотое яблоко",
                "Golden Apple");
        TEXT.put("Огниво",
                "Flint and Steel");
        TEXT.put("Стержень ифрита",
                "Blaze Rod");
        TEXT.put("Имя игрока",
                "Player name");
        TEXT.put("С заливкой",
                "Filled");
        TEXT.put("Подчёркивание",
                "Underline");
        TEXT.put("Капсула",
                "Capsule");
        TEXT.put("Варианты оформления",
                "Style variants");
        TEXT.put("Рамка плавно перетекает в акцентный цвет при фокусе; у подчёркивания линия растёт от центра.",
                "The border flows smoothly into the accent color on focus; the underline grows from the center.");
        TEXT.put("Значение обновляется на лету. Правая кнопка по полю — меню с отменой и сбросом.",
                "The value updates on the fly. Right-click the field for a menu with undo and reset.");
        TEXT.put("Привет, ",
                "Hello, ");
        TEXT.put("Поиск предметов...",
                "Search items...");
        TEXT.put("Поиск",
                "Search");
        TEXT.put("Значок, крестик очистки и фильтрация списка при каждом нажатии.",
                "An icon, a clear button and list filtering on every keystroke.");
        TEXT.put("Только чтение",
                "Read only");
        TEXT.put("Отключено",
                "Disabled");
        TEXT.put("Сообщение",
                "Message");
        TEXT.put("Префиксы, фильтры и состояния",
                "Prefixes, filters and states");
        TEXT.put("Префикс и суффикс, допустимые символы, длина, пароль, только чтение. Ctrl+Z / Ctrl+Y — история.",
                "Prefix and suffix, allowed characters, length, password, read only. Ctrl+Z / Ctrl+Y — history.");
        TEXT.put("# и hex, до 6",
                "# and hex, up to 6");
        TEXT.put("только цифры",
                "digits only");
        TEXT.put("Enter — отправить, Esc — отменить ввод. Отправлено:",
                "Enter — submit, Esc — cancel input. Submitted:");
        TEXT.put("Толщина рамки",
                "Border width");
        TEXT.put("Скругление",
                "Rounding");
        TEXT.put("Непрозрачность",
                "Opacity");
        TEXT.put("Размытие тени",
                "Shadow blur");
        TEXT.put("Потяните за букву: значение меняется перетаскиванием",
                "Drag the letter: the value changes by dragging");
        TEXT.put("Числа и ползунки",
                "Numbers and sliders");
        TEXT.put("Ползунок и поле на одном сигнале. Колесо и стрелки вверх/вниз в фокусе меняют на шаг, Shift — на 10 шагов. ",
                "A slider and a field on one signal. The wheel and up/down arrows in focus change it by a step, Shift — by 10 steps. ");
        TEXT.put("Неверный ввод вспыхивает красным.",
                "Invalid input flashes red.");
        TEXT.put("Цвета",
                "Colors");
        TEXT.put("Hex \"#RRGGBB\" или \"#AARRGGBB\" с образцом на шахматке; верный ввод применяется сразу.",
                "Hex \"#RRGGBB\" or \"#AARRGGBB\" with a swatch on a checkerboard; valid input applies immediately.");
        TEXT.put("общий сигнал с палитрой",
                "shared signal with the palette");
        TEXT.put("без прозрачности",
                "no transparency");
        TEXT.put("полупрозрачный",
                "translucent");
        TEXT.put("Щёлкните здесь правой кнопкой мыши",
                "Right-click here");
        TEXT.put("Открыть...",
                "Open...");
        TEXT.put("Открыть",
                "Open");
        TEXT.put("Сохранить как...",
                "Save as...");
        TEXT.put("Сохранить как",
                "Save as");
        TEXT.put("Печать",
                "Print");
        TEXT.put("Выйти",
                "Exit");
        TEXT.put("Контекстное меню",
                "Context menu");
        TEXT.put("Всплывающий слой поверх экрана: стрелки и Enter, Escape или клик мимо закрывают меню.",
                "A popup layer above the screen: arrows and Enter, Escape or a click outside closes the menu.");
        TEXT.put("Последний пункт:",
                "Last item:");
    }

    private static void part3() {
        TEXT.put("В разметке",
                "In markup");
        TEXT.put("Теги <input>, <number> и <color-field> с привязкой к состоянию.",
                "The <input>, <number> and <color-field> tags bound to state.");
        TEXT.put("линейный",
                "linear");
        TEXT.put("радиальный",
                "radial");
        TEXT.put("эллипс",
                "ellipse");
        TEXT.put("конический",
                "conic");
        TEXT.put("полосы",
                "stripes");
        TEXT.put("кольца",
                "rings");
        TEXT.put("бегущая рамка",
                "running border");
        TEXT.put("течёт",
                "flowing");
        TEXT.put("конус-спиннер",
                "cone spinner");
        TEXT.put("смена кистей",
                "brush change");
        TEXT.put("наведи",
                "hover me");
        TEXT.put("рамка-градиент",
                "gradient border");
        TEXT.put("свечение",
                "glow");
        TEXT.put("передний слой",
                "front layer");
        TEXT.put("Виды градиентов",
                "Gradient kinds");
        TEXT.put("Считаются в шейдере попиксельно: до 8 цветов, жёсткие переходы, повтор",
                "Computed per pixel in a shader: up to 8 colors, hard stops, repeat");
        TEXT.put("Любой угол",
                "Any angle");
        TEXT.put("Угол как в CSS: 0° — вверх, 90° — вправо",
                "The angle is like in CSS: 0° — up, 90° — right");
        TEXT.put("Пространства цвета",
                "Color spaces");
        TEXT.put("Пространство смешивания задаётся у каждой кисти",
                "The mixing space is set per brush");
        TEXT.put("Готовые градиенты",
                "Ready-made gradients");
        TEXT.put("Класс Gradients",
                "The Gradients class");
        TEXT.put("Анимированные",
                "Animated");
        TEXT.put("Кисти тоже анимируются: поворот, сдвиг, плавная смена",
                "Brushes animate too: rotation, shift, smooth change");
        TEXT.put("Рамки, контур, свечение",
                "Borders, outline, glow");
        TEXT.put("Новые свойства стиля: BORDER_BRUSH, OUTLINE, GLOW, FOREGROUND",
                "New style properties: BORDER_BRUSH, OUTLINE, GLOW, FOREGROUND");
        TEXT.put("Режимы смешивания",
                "Blend modes");
        TEXT.put("ADD — для света и огня, MULTIPLY — для теней и тонировки",
                "ADD — for light and fire, MULTIPLY — for shadows and tinting");
        TEXT.put("Демо-HUD выключен",
                "Demo HUD is off");
        TEXT.put("Взгляд: ",
                "Facing: ");
        TEXT.put("Биом: ",
                "Biome: ");
        TEXT.put("Время: ",
                "Time: ");
        TEXT.put("Опыт",
                "XP");
        TEXT.put("Таймер",
                "Timer");
        TEXT.put("воздух",
                "air");
        TEXT.put("Демо-HUD включён",
                "Demo HUD is on");
        TEXT.put("Закройте витрину, чтобы увидеть его поверх игры",
                "Close the showcase to see it over the game");
        TEXT.put("север",
                "north");
        TEXT.put("юг",
                "south");
        TEXT.put("запад",
                "west");
        TEXT.put("восток",
                "east");
        TEXT.put("Данные игры",
                "Game data");
        TEXT.put("GameData опрашивает игру на каждом тике; в главном меню — значения по умолчанию",
                "GameData polls the game every tick; in the main menu — default values");
        TEXT.put("Тосты",
                "Toasts");
        TEXT.put("Въезжают сбоку, живут несколько секунд и схлопываются; лишние ждут в очереди",
                "Slide in from the side, live for a few seconds and collapse; extra ones wait in a queue");
        TEXT.put("Обратный отсчёт по реальным часам: идёт, даже если его не видно",
                "A countdown by the real clock: it runs even when not visible");
        TEXT.put("Лента уведомлений",
                "Notification feed");
        TEXT.put("Короткие строки, которые тают через несколько секунд",
                "Short lines that fade after a few seconds");
        TEXT.put("HUD поверх игры",
                "HUD over the game");
        TEXT.put("Hud.get() рисуется загрузчиком каждый кадр; закройте витрину, чтобы увидеть",
                "Hud.get() is drawn by the loader every frame; close the showcase to see it");
        TEXT.put("Здоровье",
                "Health");
        TEXT.put("Сытость",
                "Hunger");
        TEXT.put("насыщение ",
                "saturation ");
        TEXT.put("Воздух",
                "Air");
        TEXT.put("кадр/с",
                "fps");
        TEXT.put("опыт",
                "xp");
        TEXT.put("Полоса опыта",
                "XP bar");
        TEXT.put("В мире",
                "In world");
        TEXT.put("да",
                "yes");
        TEXT.put("нет (главное меню)",
                "no (main menu)");
        TEXT.put("Координаты",
                "Coordinates");
        TEXT.put("Взгляд",
                "Facing");
        TEXT.put("Биом",
                "Biome");
        TEXT.put("Измерение",
                "Dimension");
        TEXT.put("Время суток",
                "Time of day");
        TEXT.put("Уровень",
                "Level");
        TEXT.put("В руке",
                "In hand");
        TEXT.put("пусто",
                "empty");
        TEXT.put("Панель быстрого доступа (снимок инвентаря)",
                "Hotbar (inventory snapshot)");
        TEXT.put("Подсказка",
                "Hint");
        TEXT.put("Нажмите K, чтобы открыть витрину",
                "Press K to open the showcase");
        TEXT.put("С иконкой",
                "With an icon");
        TEXT.put("Добыча",
                "Loot");
        TEXT.put(" отправлен в инвентарь",
                " sent to the inventory");
        TEXT.put("Ошибка",
                "Error");
        TEXT.put("Нет связи",
                "No connection");
        TEXT.put("Сервер не ответил за 30 секунд. Повторная попытка…",
                "The server did not respond in 30 seconds. Retrying…");
        TEXT.put("С прогрессом",
                "With progress");
        TEXT.put("В игровой HUD",
                "To the game HUD");
        TEXT.put("Из витрины",
                "From the showcase");
        TEXT.put("Этот тост в общем HUD — он виден поверх игры",
                "This toast is in the shared HUD — it is visible over the game");
        TEXT.put("Убрать все",
                "Dismiss all");
        TEXT.put("Загрузка мира",
                "Loading world");
        TEXT.put("Генерация чанков…",
                "Generating chunks…");
        TEXT.put("Готово!",
                "Done!");
        TEXT.put("Время вышло",
                "Time is up");
        TEXT.put("Таймер витрины закончился",
                "The showcase timer has finished");
        TEXT.put("Таймер закончился",
                "Timer finished");
        TEXT.put("10 секунд",
                "10 seconds");
        TEXT.put("1 минута",
                "1 minute");
        TEXT.put("Пауза / дальше",
                "Pause / resume");
        TEXT.put("Сброс",
                "Reset");
        TEXT.put("Секундомер",
                "Stopwatch");
        TEXT.put("Событие",
                "Event");
        TEXT.put("Игрок Steve",
                "Player Steve");
        TEXT.put(" вошёл в игру",
                " joined the game");
        TEXT.put("Опасность",
                "Danger");
        TEXT.put("Рядом крипер!",
                "A creeper is nearby!");
        TEXT.put("Показать демо-HUD поверх игры",
                "Show the demo HUD over the game");
        TEXT.put("Панель с координатами слева сверху, свои полосы над ванильными, опыт и таймер сверху,",
                "A panel with coordinates at the top left, custom bars above the vanilla ones, XP and a timer at the top,");
        TEXT.put("шкала воздуха справа — только под водой. Прячется по F1 и на экране отладки.",
                "an air gauge on the right — only underwater. Hidden by F1 and on the debug screen.");
        TEXT.put("Зомби",
                "Zombie");
        TEXT.put("Крипер",
                "Creeper");
        TEXT.put("Житель",
                "Villager");
        TEXT.put("Скелет",
                "Skeleton");
        TEXT.put("Эндермен",
                "Enderman");
        TEXT.put("Железный голем",
                "Iron Golem");
        TEXT.put("Свинья",
                "Pig");
        TEXT.put("Овца",
                "Sheep");
        TEXT.put("Волк",
                "Wolf");
        TEXT.put("Курица",
                "Chicken");
        TEXT.put("Паук",
                "Spider");
        TEXT.put("Лиса",
                "Fox");
        TEXT.put("Перенесите что-нибудь: здесь появится, что и куда ушло.",
                "Move something: what went where will appear here.");
        TEXT.put("Перенесено: ",
                "Moved: ");
        TEXT.put(" (Shift+клик или выброшено)",
                " (Shift+click or dropped)");
        TEXT.put(" → ячейка ",
                " → slot ");
        TEXT.put("Сундук и инвентарь",
                "Chest and inventory");
    }

    private static void part4() {
        TEXT.put("ЛКМ-перетаскивание — вся стопка, ПКМ — половина, Shift+клик — быстрый перенос, мимо окна — ",
                "LMB drag — the whole stack, RMB — half, Shift+click — quick move, outside the window — ");
        TEXT.put("выбросить. Справа тот же сундук в современном виде: оба окна показывают одни данные.",
                "drop. On the right is the same chest in a modern look: both windows show the same data.");
        TEXT.put("Броня и мусорка",
                "Armor and trash");
        TEXT.put("В ячейки брони ложится только своя броня (фильтр инвентаря), мусорка уничтожает всё, что в неё ",
                "Armor slots accept only matching armor (inventory filter), the trash destroys everything put ");
        TEXT.put("положили. Пустые ячейки брони показывают бледный силуэт.",
                "into it. Empty armor slots show a pale silhouette.");
        TEXT.put("Мобы",
                "Mobs");
        TEXT.put("Каждого можно крутить мышью; переключатель включает автовращение. Мобы рисуются игрой, как ",
                "Each one can be rotated with the mouse; the toggle turns on auto-rotation. Mobs are drawn by the game, like ");
        TEXT.put("игрок в окне инвентаря, поэтому видны только в мире (не в главном меню).",
                "the player in the inventory window, so they are visible only in a world (not in the main menu).");
        TEXT.put("Игрок",
                "Player");
        TEXT.put("Вы сами в своей броне и с предметами в руках. Слева — смотрит на курсор, справа — вращается.",
                "Yourself in your armor and with items in hand. Left — looks at the cursor, right — rotates.");
        TEXT.put("Следит за мышью",
                "Follows the mouse");
        TEXT.put("Автовращение",
                "Auto-rotation");
        TEXT.put("Крупные предметы",
                "Large items");
        TEXT.put("Предметы в 3D: блоки объёмные, остальное — как выброшенное на землю. Крутятся мышью и сами.",
                "Items in 3D: blocks are volumetric, the rest look like items dropped on the ground. Rotated by mouse and on their own.");
        TEXT.put("Блок",
                "Block");
        TEXT.put("Зачарованный меч",
                "Enchanted sword");
        TEXT.put("Тотем (мышью)",
                "Totem (by mouse)");
        TEXT.put("Из разметки",
                "From markup");
        TEXT.put("Теги <slot>, <entity>, <player> и <item-view>.",
                "The <slot>, <entity>, <player> and <item-view> tags.");
        TEXT.put("Экскалибур",
                "Excalibur");
        TEXT.put("Выкован в недрах Незера",
                "Forged in the depths of the Nether");
        TEXT.put("Острый, как в первый день",
                "As sharp as on the first day");
        TEXT.put("Сундук",
                "Chest");
        TEXT.put("Добыча (Shift+клик — надеть)",
                "Loot (Shift+click — equip)");
        TEXT.put("Мусорка",
                "Trash");
        TEXT.put("Зомби-ребёнок",
                "Baby zombie");
        TEXT.put("задача отмечена",
                "task checked");
        TEXT.put("отметка снята",
                "check removed");
        TEXT.put("Полный пример",
                "Full example");
        TEXT.put("Короткая заметка",
                "Short note");
        TEXT.put("Последнее действие:",
                "Last action:");
        TEXT.put("сборка 1",
                "build 1");
        TEXT.put("сборка ",
                "build ");
        TEXT.put("HTML и CSS",
                "HTML and CSS");
        TEXT.put("Экран ниже загружен из файлов мода; правка в ресурспаке + F3+T обновляет его на лету.",
                "The screen below is loaded from the mod's files; editing it in a resource pack + F3+T updates it on the fly.");
        TEXT.put("Исходники",
                "Sources");
        TEXT.put("Скрыть",
                "Hide");
        TEXT.put("Пересобрать",
                "Rebuild");
        TEXT.put("Ресурс `",
                "Resource `");
        TEXT.put("` не найден.",
                "` not found.");
        TEXT.put("Главная",
                "Home");
        TEXT.put("Настройки",
                "Settings");
        TEXT.put("Дополнительно",
                "Advanced");
        TEXT.put("Музыка",
                "Music");
        TEXT.put("Разработка",
                "Development");
        TEXT.put("Добро пожаловать",
                "Welcome");
        TEXT.put("Это экран настроек, собранный из виджетов Lumina в современной теме.",
                "This is a settings screen built from Lumina widgets in the modern theme.");
        TEXT.put("Рамки, цвета, анимации",
                "Borders, colors, animations");
        TEXT.put("Громкость по категориям",
                "Volume by category");
        TEXT.put("Инспектор и отладка",
                "Inspector and debugging");
        TEXT.put("Начать",
                "Start");
        TEXT.put("Документация",
                "Documentation");
        TEXT.put("Экран",
                "Screen");
        TEXT.put("Вид",
                "View");
        TEXT.put("Шрифт",
                "Font");
        TEXT.put("Расширения",
                "Extensions");
        TEXT.put("Текст",
                "Text");
        TEXT.put("Высокое",
                "High");
        TEXT.put("Графика",
                "Graphics");
        TEXT.put("Качество",
                "Quality");
        TEXT.put("Низкое",
                "Low");
        TEXT.put("Среднее",
                "Medium");
        TEXT.put("Ультра",
                "Ultra");
        TEXT.put("Плавное освещение",
                "Smooth lighting");
        TEXT.put("Облака",
                "Clouds");
        TEXT.put("Дальность прорисовки",
                "Render distance");
        TEXT.put("Галочка",
                "Check");
        TEXT.put("Крестик",
                "Cross");
        TEXT.put("Заливка",
                "Fill");
        TEXT.put("Сердце",
                "Heart");
        TEXT.put("Звезда",
                "Star");
        TEXT.put("Точка",
                "Dot");
        TEXT.put("Громкость",
                "Volume");
        TEXT.put("Общая",
                "Master");
        TEXT.put("Враждебные",
                "Hostile");
        TEXT.put("Голос",
                "Voice");
        TEXT.put("/команда",
                "/command");
        TEXT.put("Отладка",
                "Debug");
        TEXT.put("Показывать границы элементов",
                "Show element bounds");
        TEXT.put("Счётчик кадров",
                "Frame counter");
        TEXT.put("Логировать события",
                "Log events");
        TEXT.put("Масштаб интерфейса",
                "Interface scale");
        TEXT.put("Консоль",
                "Console");
        TEXT.put("Окно",
                "Window");
        TEXT.put("Полноэкранный режим",
                "Fullscreen");
        TEXT.put("Вертикальная синхронизация",
                "Vertical sync");
        TEXT.put("Ограничение FPS",
                "FPS limit");
        TEXT.put("Предпросмотр",
                "Preview");
        TEXT.put("ПКМ — меню",
                "RMB — menu");
        TEXT.put("Острые углы",
                "Sharp corners");
        TEXT.put("Без тени",
                "No shadow");
        TEXT.put("Поменять цвета местами",
                "Swap colors");
        TEXT.put("Сбросить по умолчанию",
                "Reset to default");
        TEXT.put("Мягкий",
                "Soft");
        TEXT.put("Острый",
                "Sharp");
        TEXT.put("Неон",
                "Neon");
        TEXT.put("Стиль рамки",
                "Border style");
        TEXT.put("Скруглённые углы",
                "Rounded corners");
        TEXT.put("Тень",
                "Shadow");
        TEXT.put("Градиентная рамка",
                "Gradient border");
        TEXT.put("Радиус",
                "Radius");
        TEXT.put("Толщина",
                "Width");
        TEXT.put("Пресеты",
                "Presets");
        TEXT.put("Цвет сверху",
                "Top color");
        TEXT.put("Цвет снизу",
                "Bottom color");
        TEXT.put("Тема",
                "Theme");
        TEXT.put("Акцент",
                "Accent");
        TEXT.put("Скорость анимаций",
                "Animation speed");
        TEXT.put("Съешь же ещё этих мягких булок",
                "The quick brown fox jumps over the lazy dog");
        TEXT.put("Размер",
                "Size");
        TEXT.put("Начертание",
                "Style");
        TEXT.put("Жирный",
                "Bold");
        TEXT.put("Моно",
                "Mono");
        TEXT.put("Мини-карта",
                "Minimap");
        TEXT.put("Часы на экране",
                "On-screen clock");
        TEXT.put("Приближение",
                "Zoom");
        TEXT.put("Сортировка сундуков",
                "Chest sorting");
        TEXT.put("Имя",
                "Name");
        TEXT.put("Заполненное",
                "Filled");
        TEXT.put("С подчёркиванием",
                "Underlined");
    }

    private static void part5() {
        TEXT.put("Поля ввода",
                "Text fields");
        TEXT.put("Обычное",
                "Regular");
        TEXT.put("Линия",
                "Line");
        TEXT.put("Пилюля",
                "Pill");
        TEXT.put("незнакомец",
                "stranger");
        TEXT.put("Медный слиток",
                "Copper Ingot");
        TEXT.put("Кварц",
                "Quartz");
        TEXT.put("Осколок аметиста",
                "Amethyst Shard");
        TEXT.put("Жемчуг Края",
                "Ender Pearl");
        TEXT.put("Сгусток слизи",
                "Slimeball");
        TEXT.put("Печенье",
                "Cookie");
        TEXT.put("Компас",
                "Compass");
        TEXT.put("Равнины",
                "Plains");
        TEXT.put("Лес",
                "Forest");
        TEXT.put("Пустыня",
                "Desert");
        TEXT.put("Тайга",
                "Taiga");
        TEXT.put("Джунгли",
                "Jungle");
        TEXT.put("Саванна",
                "Savanna");
        TEXT.put("Болото",
                "Swamp");
        TEXT.put("Выберите биом",
                "Choose a biome");
        TEXT.put("Предмет: ",
                "Item: ");
        TEXT.put("не выбран",
                "not selected");
        TEXT.put("Выпадающие списки",
                "Dropdowns");
        TEXT.put("Список открывается под полем (у нижнего края — над ним), стрелки и Enter, буква прыгает к варианту, ",
                "The list opens below the field (above it at the bottom edge), arrows and Enter, a letter jumps to an option, ");
        TEXT.put("колесо прокручивает длинный список, поиск отбирает варианты по подстроке.",
                "the wheel scrolls a long list, search filters options by substring.");
        TEXT.put("простой",
                "simple");
        TEXT.put("иконки и поиск",
                "icons and search");
        TEXT.put("с подсказкой",
                "with a tooltip");
        TEXT.put("Алмазный меч\nПри ударе:\n 7 ед. урона\n 1,6 скорости атаки",
                "Diamond Sword\nWhen in Main Hand:\n 7 Attack Damage\n 1.6 Attack Speed");
        TEXT.put("Современная",
                "Modern");
        TEXT.put("Карточка темы со скруглением,\nрамкой и мягким появлением",
                "A theme card with rounding,\na border and a soft appearance");
        TEXT.put("**Жемчуг Края** ![](item:minecraft:ender_pearl)\n\n",
                "**Ender Pearl** ![](item:minecraft:ender_pearl)\n\n");
        TEXT.put("- телепортирует при броске\n- стакается до *16*",
                "- teleports when thrown\n- stacks up to *16*");
        TEXT.put("Счётчик",
                "Counter");
        TEXT.put("Нажатий: 0",
                "Clicks: 0");
        TEXT.put("Нажатий: ",
                "Clicks: ");
        TEXT.put("Подсказки",
                "Tooltips");
        TEXT.put("Появляются после задержки наведения, следуют за мышью и не выходят за экран. Нажатие прячет подсказку.",
                "Appear after a hover delay, follow the mouse and stay on screen. Pressing hides the tooltip.");
        TEXT.put("как в игре",
                "like in game");
        TEXT.put("современная",
                "modern");
        TEXT.put("свой элемент",
                "custom element");
        TEXT.put("живой текст",
                "live text");
        TEXT.put("Поглощение II (2:00)",
                "Absorption II (2:00)");
        TEXT.put("Регенерация II (0:05)",
                "Regeneration II (0:05)");
        TEXT.put("Редкость: необычная",
                "Rarity: uncommon");
        TEXT.put("Щёлкните сюда, затем зажмите R",
                "Click here, then hold R");
        TEXT.put("или нажмите правую кнопку мыши",
                "or press the right mouse button");
        TEXT.put("Радиальное меню",
                "Radial menu");
        TEXT.put("Сектор выбирается направлением мыши; отпустите R — пункт сработает. Стрелки и цифры тоже работают, ",
                "A sector is chosen by mouse direction; release R and the item fires. Arrows and digits work too, ");
        TEXT.put("правая кнопка или клик в центре — назад из вложенного меню.",
                "the right button or a click in the center goes back from a submenu.");
        TEXT.put("Действия",
                "Actions");
        TEXT.put("Меч",
                "Sword");
        TEXT.put("Факел",
                "Torch");
        TEXT.put("Доски",
                "Planks");
        TEXT.put("Прыжок",
                "Jump");
        TEXT.put("Инвентарь",
                "Inventory");
        TEXT.put("Выбросить",
                "Drop");
        TEXT.put("Красться",
                "Sneak");
        TEXT.put("Чат",
                "Chat");
        TEXT.put("Скриншот",
                "Screenshot");
        TEXT.put("Назначение клавиш",
                "Key bindings");
        TEXT.put("Щелчок — поле ждёт клавишу; Escape отменяет, правая кнопка или Delete — вернуть по умолчанию.",
                "Click — the field waits for a key; Escape cancels, the right button or Delete restores the default.");
        TEXT.put("Только ближайшие три месяца",
                "Only the next three months");
        TEXT.put(" — через ",
                " — in ");
        TEXT.put(" дн.",
                " days");
        TEXT.put("Дата",
                "Date");
        TEXT.put("Календарь во всплывающем окне: стрелки — дни, PageUp/PageDown и колесо — месяцы, Enter — выбрать.",
                "A calendar in a popup: arrows — days, PageUp/PageDown and the wheel — months, Enter — select.");
        TEXT.put("общий сигнал с календарём",
                "shared signal with the calendar");
        TEXT.put("с ограничением",
                "with a limit");
        TEXT.put("Теги <select>, <keybind>, <date-picker> и атрибут tooltip.",
                "The <select>, <keybind>, <date-picker> tags and the tooltip attribute.");
        TEXT.put("Дом",
                "House");
        TEXT.put("Портал",
                "Portal");
        TEXT.put("Пирамида TNT",
                "TNT pyramid");
        TEXT.put("Бассейн",
                "Pool");
        TEXT.put("Постройки в 3D",
                "Builds in 3D");
        TEXT.put("Тяните мышью, чтобы вращать (с инерцией), колесо — приближение. Невидимые грани не рисуются.",
                "Drag with the mouse to rotate (with inertia), the wheel zooms. Invisible faces are not drawn.");
        TEXT.put("Редактор по слоям",
                "Layer editor");
        TEXT.put("Слева — выбранный слой сверху (ЛКМ — поставить, ПКМ — сломать), справа — живой предпросмотр.",
                "Left — the selected layer from above (LMB — place, RMB — break), right — a live preview.");
        TEXT.put("Изометрия",
                "Isometric");
        TEXT.put("Сверху",
                "Top");
        TEXT.put("Перетащите модель мышью",
                "Drag the model with the mouse");
        TEXT.put("Ниже",
                "Lower");
        TEXT.put("Выше",
                "Higher");
        TEXT.put("Слой y = ",
                "Layer y = ");
        TEXT.put("Срез по слою",
                "Slice by layer");
        TEXT.put("Взять из галереи",
                "Take from the gallery");
        TEXT.put("Токен акцента",
                "Accent token");
        TEXT.put("theme.set(Tokens.ACCENT, цвет) — всё, что на нём построено, перекрашивается",
                "theme.set(Tokens.ACCENT, color) — everything built on it is recolored");
        TEXT.put("Токены темы",
                "Theme tokens");
        TEXT.put("Стили ссылаются на токены, а не на числа",
                "Styles refer to tokens, not numbers");
        TEXT.put("Несколько классов",
                "Multiple classes");
        TEXT.put("styleClass(\"button danger pill\") — классы накладываются по порядку",
                "styleClass(\"button danger pill\") — classes are applied in order");
        TEXT.put("Переходы по свойствам",
                "Per-property transitions");
        TEXT.put("Transition.key(свойство, длительность, кривая, задержка)",
                "Transition.key(property, duration, curve, delay)");
        TEXT.put("Трансформации и шрифт",
                "Transforms and font");
        TEXT.put("скругление всех панелей и кнопок",
                "rounding of all panels and buttons");
        TEXT.put("множитель всех переходов, 0 — без анимаций",
                "multiplier of all transitions, 0 — no animations");
        TEXT.put("Кнопка",
                "Button");
        TEXT.put("Ещё одна",
                "Another one");
        TEXT.put("масштаб: 0.5 с, OUT_ELASTIC",
                "scale: 0.5 s, OUT_ELASTIC");
        TEXT.put("заливка: 1.2 с, плавно",
                "fill: 1.2 s, smooth");
        TEXT.put("скругление: после задержки 0.4 с",
                "rounding: after a 0.4 s delay");
        TEXT.put("рамка: мгновенно",
                "border: instant");
        TEXT.put("сдвиг",
                "shift");
        TEXT.put("растяжение",
                "stretch");
        TEXT.put("Протанопия",
                "Protanopia");
        TEXT.put("Дейтеранопия",
                "Deuteranopia");
        TEXT.put("Тританопия",
                "Tritanopia");
        TEXT.put("Ванильные",
                "Vanilla");
        TEXT.put("Мягкие",
                "Soft");
        TEXT.put("Ретро",
                "Retro");
        TEXT.put("Без звука",
                "Silent");
        TEXT.put("Снежные равнины",
                "Snowy plains");
        TEXT.put("узкий",
                "narrow");
        TEXT.put("средний",
                "medium");
        TEXT.put("широкий",
                "wide");
        TEXT.put("Ширина интерфейса: ",
                "Interface width: ");
        TEXT.put(" — класс «",
                " — class «");
        TEXT.put("Тема выбирается по ползунку времени; на закате и рассвете цвета смешиваются",
                "The theme is picked by the time slider; at sunset and sunrise the colors blend");
        TEXT.put("Тема выбирается по биому",
                "The theme is picked by biome");
    }

    private static void part6() {
        TEXT.put("Автосмена выключена",
                "Auto-switching is off");
        TEXT.put(" тиков)",
                " ticks)");
        TEXT.put("Редактор темы",
                "Theme editor");
        TEXT.put("Цвета, скругление и скорость меняют тему всей витрины на лету",
                "Colors, rounding and speed change the theme of the whole showcase on the fly");
        TEXT.put("Показать CSS",
                "Show CSS");
        TEXT.put("/* … ещё ",
                "/* … ");
        TEXT.put(" строк */\n",
                " more lines */\n");
        TEXT.put("Проверить: экспорт → импорт",
                "Check: export → import");
        TEXT.put("Тема прочитана обратно без потерь",
                "The theme was read back without loss");
        TEXT.put("Токены отличаются",
                "Tokens differ");
        TEXT.put("Экспорт и импорт",
                "Export and import");
        TEXT.put("Тема сохраняется в CSS: токены в :root, стили классов — обычными правилами",
                "The theme is saved to CSS: tokens in :root, class styles as ordinary rules");
        TEXT.put("По времени суток",
                "By time of day");
        TEXT.put("По биому",
                "By biome");
        TEXT.put("Выключено",
                "Off");
        TEXT.put("Автосмена темы",
                "Theme auto-switching");
        TEXT.put("В игре время и биом берутся из мира (WorldInfo); здесь их заменяют ползунок и переключатель",
                "In game the time and biome come from the world (WorldInfo); here a slider and a switch replace them");
        TEXT.put("Доступность",
                "Accessibility");
        TEXT.put("Действует на весь интерфейс поверх любой темы",
                "Applies to the whole interface on top of any theme");
        TEXT.put("Высокий контраст",
                "High contrast");
        TEXT.put("Крупный текст",
                "Large text");
        TEXT.put("Без анимаций",
                "No animations");
        TEXT.put("Рамка фокуса всегда",
                "Always show the focus ring");
        TEXT.put("Озвучивать фокус",
                "Narrate focus");
        TEXT.put("Симуляция вместо коррекции",
                "Simulation instead of correction");
        TEXT.put("Цветовое зрение:",
                "Color vision:");
        TEXT.put("Озвучка идёт через Platform.narrate: загрузчик передаёт текст диктору игры",
                "Narration goes through Platform.narrate: the loader passes the text to the game narrator");
        TEXT.put("Звуки интерфейса",
                "Interface sounds");
        TEXT.put("Звук наведения играет при входе мыши в кнопку и не чаще раза в 70 мс",
                "The hover sound plays when the mouse enters a button, at most once per 70 ms");
        TEXT.put("Наведи",
                "Hover me");
        TEXT.put("И сюда",
                "And here");
        TEXT.put("Флажок",
                "Checkbox");
        TEXT.put("Меню",
                "Menu");
        TEXT.put("Содержимое",
                "Content");
        TEXT.put("Детали",
                "Details");
        TEXT.put("Карточка ",
                "Card ");
        TEXT.put("По своей ширине ",
                "By own width ");
        TEXT.put("Адаптивная раскладка",
                "Responsive layout");
        TEXT.put("Строка на широком экране и колонка на узком; сетка 1/2/3 колонки; масштаб всего интерфейса",
                "A row on a wide screen and a column on a narrow one; a 1/2/3 column grid; scale of the whole interface");
        TEXT.put("Узкий блок (220 пикселей) — раскладка по своей ширине:",
                "Narrow block (220 pixels) — laid out by its own width:");
        TEXT.put("Масштаб интерфейса:",
                "Interface scale:");
        TEXT.put("Матовое стекло",
                "Frosted glass");
        TEXT.put("за ним — живой фон",
                "a live background behind it");
        TEXT.put("Размытие фона",
                "Background blur");
        TEXT.put("Стиль backdrop-blur размывает то, что уже нарисовано под элементом. В игре — ванильный шейдер ",
                "The backdrop-blur style blurs what is already drawn under the element. In game — the vanilla blur ");
        TEXT.put("размытия; где его нет, рисуется матовая подложка",
                "shader; where it is missing, a frosted backing is drawn");
        TEXT.put("Строка ",
                "Row ");
        TEXT.put("Текстуры недоступны",
                "Textures unavailable");
        TEXT.put("Скруглённая обрезка",
                "Rounded clipping");
        TEXT.put("clip-rounded: true обрезает детей по radius. Бэкенд закрашивает углы снимком фона; без снимков ",
                "clip-rounded: true clips children by radius. The backend paints the corners with a background snapshot; without snapshots ");
        TEXT.put("обрезка прямоугольная",
                "the clipping is rectangular");
        TEXT.put("Анимированный GIF",
                "Animated GIF");
        TEXT.put("кадр 1",
                "frame 1");
        TEXT.put("кадр ",
                "frame ");
        TEXT.put("Пауза",
                "Pause");
        TEXT.put("Кадры читает javax.imageio, каждый кадр — своя текстура; задержки и число повторов берутся из файла. ",
                "Frames are read by javax.imageio, each frame is its own texture; delays and the loop count come from the file. ");
        TEXT.put("Этот GIF собран в коде через Gif.encode",
                "This GIF is built in code with Gif.encode");
        TEXT.put("В мире цвета верхних блоков читает загрузчик (по кусочку за кадр); без мира — демо-ландшафт. ",
                "In a world the loader reads the colors of the top blocks (a piece per frame); without a world — a demo landscape. ");
        TEXT.put("Колесо — масштаб, у карты мира — перетаскивание",
                "The wheel zooms, the world map can be dragged");
        TEXT.put("круглая, по взгляду",
                "round, by facing");
        TEXT.put("квадратная, север вверху",
                "square, north up");
        TEXT.put("карта мира",
                "world map");
        TEXT.put("Искры",
                "Sparks");
        TEXT.put("Пыль",
                "Dust");
        TEXT.put("Частицы за меню",
                "Particles behind the menu");
        TEXT.put("Backdrop.Kind.PARTICLES: искры, снег или пыль. Частицы разлетаются от курсора",
                "Backdrop.Kind.PARTICLES: sparks, snow or dust. Particles scatter from the cursor");
        TEXT.put("Открыть инспектор",
                "Open inspector");
        TEXT.put("Инспектор и геймпад",
                "Inspector and gamepad");
        TEXT.put("Инспектор показывает границы, отступы, классы и вычисленный стиль; клик выбирает элемент, стрелки ",
                "The inspector shows bounds, margins, classes and the computed style; a click selects an element, arrows ");
        TEXT.put("ходят по дереву, Esc закрывает",
                "walk the tree, Esc closes it");
        TEXT.put("Клавиша F8",
                "F8 key");
        TEXT.put("Геймпад: A — нажать, B — назад, крестовина и стик — фокус, ",
                "Gamepad: A — press, B — back, D-pad and stick — focus, ");
        TEXT.put("бамперы — вкладки",
                "bumpers — tabs");
        TEXT.put("Настройки витрины Lumina",
                "Lumina showcase settings");
        TEXT.put("Интерфейс",
                "Interface");
        TEXT.put("Показывать в углу экрана",
                "Show in the screen corner");
        TEXT.put("Размер карты",
                "Map size");
        TEXT.put("Прозрачность",
                "Transparency");
        TEXT.put("Приблизить карту",
                "Zoom the map");
        TEXT.put("Внешний вид",
                "Appearance");
        TEXT.put("Качество частиц",
                "Particle quality");
        TEXT.put("Приветствие",
                "Greeting");
        TEXT.put("Привет!",
                "Hello!");
        TEXT.put("Настройки витрины",
                "Showcase settings");
        TEXT.put("Экран настроек из описания",
                "A settings screen from a description");
        TEXT.put("ConfigSpec описывает настройки без рефлексии; ConfigScreen сам строит строки, поиск, сброс и ",
                "ConfigSpec describes settings without reflection; ConfigScreen builds the rows, search, reset and ");
        TEXT.put("сохранение в TOML",
                "saving to TOML by itself");
        TEXT.put("Сияние",
                "Glow");
        TEXT.put("Звёзды",
                "Stars");
        TEXT.put("Волны",
                "Waves");
        TEXT.put("Перелив",
                "Shimmer");
        TEXT.put("Окна и живые обои",
                "Windows and live wallpapers");
        TEXT.put("Окна таскаются за заголовок, растягиваются за уголок, Esc или крестик закрывают. ",
                "Windows are dragged by the title, resized by the corner, closed with Esc or the cross. ");
        TEXT.put("Обои под ними анимированы и сдвигаются за мышью.",
                "The wallpaper under them is animated and shifts with the mouse.");
        TEXT.put("Рамка из текстуры",
                "Texture frame");
        TEXT.put("NineSlice растягивает картинку рамки на любой размер: углы остаются чёткими. ",
                "NineSlice stretches a frame picture to any size: the corners stay crisp. ");
        TEXT.put("В CSS: nine-slice: url(minecraft:textures/gui/demo_background.png) 248 166 4 in 256 256 / 2",
                "In CSS: nine-slice: url(minecraft:textures/gui/demo_background.png) 248 166 4 in 256 256 / 2");
        TEXT.put("Окно ",
                "Window ");
        TEXT.put("Звук",
                "Sound");
        TEXT.put("Громкость: ",
                "Volume: ");
        TEXT.put("Диалог",
                "Dialog");
        TEXT.put("Удалить мир?",
                "Delete the world?");
        TEXT.put("Это действие нельзя отменить.",
                "This action cannot be undone.");
        TEXT.put("Удалить",
                "Delete");
        TEXT.put("Отмена",
                "Cancel");
        TEXT.put("Новое окно",
                "New window");
        TEXT.put("масштаб 2",
                "scale 2");
        TEXT.put("""
            # Дневник Стива
            День первый. Я очнулся на берегу и понял, что до заката нужно построить **укрытие**.

            Дерево рубится кулаком — медленно, но верно.
            <!-- page -->
            ## Первая ночь
            Зомби стучат в дверь, а я считаю *блоки угля*. Факелы — лучшие друзья шахтёра.

            > Не копай прямо вниз.
            <!-- page -->
            ## Шахта
            На глубине **Y = −58** нашлись алмазы! Ещё нужно:

            - железо для ведра;
            - обсидиан для портала;
            - ![](item:minecraft:ender_eye) око Края.
            <!-- page -->
            ## Незер
            Портал загудел, и мир окрасился в красный. Гасты плачут, а я ищу крепость.
            <!-- page -->
            ## Край
            Дракон повержен. Вернуться [к началу](#дневник-стива) или [в шахту](#шахта).
            """,
                """
# Diary of Steve
Day one. I woke up on the shore and realized I had to build a **shelter** before sunset.

Wood can be chopped with a fist — slowly but surely.
<!-- page -->
## First night
Zombies are knocking on the door while I count *coal blocks*. Torches are the best friends of a miner.

> Never dig straight down.
<!-- page -->
## Mine
At depth **Y = −58** I found diamonds! Still needed:

- iron for a bucket;
- obsidian for a portal;
- ![](item:minecraft:ender_eye) an eye of ender.
<!-- page -->
## Nether
The portal hummed and the world turned red. Ghasts are crying while I look for a fortress.
<!-- page -->
## The End
The dragon is defeated. Go back [to the beginning](#diary-of-steve) or [to the mine](#mine).
""");
        TEXT.put("""
            ```java
            // Список на десятки тысяч строк
            DataView<Item> view = new DataView<>(items).searchBy(Item::name).pageSize(50);
            new VirtualList<Item>(18, (c, item, i, x, y, w, h, row) -> c.drawText(item.name(), x + 4, y + 5, row.textColor(), false))
                    .bind(view.visible());
            ```

            ```json
            { "pack": { "pack_format": 15, "description": "Lumina" }, "replace": false }
            ```

            ```css
            .card:hover { background: #2a2f3a; transform: scale(1.04); transition: transform .2s out-back; }
            ```

            ```html
            <form on-submit="send"><field label="Ник"><input bind="nick" required minlength="3"/></field></form>
            ```
            """,
                """
```java
// A list of tens of thousands of rows
DataView<Item> view = new DataView<>(items).searchBy(Item::name).pageSize(50);
new VirtualList<Item>(18, (c, item, i, x, y, w, h, row) -> c.drawText(item.name(), x + 4, y + 5, row.textColor(), false))
        .bind(view.visible());
```

```json
{ "pack": { "pack_format": 15, "description": "Lumina" }, "replace": false }
```

```css
.card:hover { background: #2a2f3a; transform: scale(1.04); transition: transform .2s out-back; }
```

```html
<form on-submit="send"><field label="Nickname"><input bind="nick" required minlength="3"/></field></form>
```
""");
        TEXT.put("""
            <state name="nick" value="Alex"/>
            <column gap="6">
              <row gap="8">
                <input bind="nick" placeholder="Ник" variant="filled" width="140"/>
                <label>Привет, {{nick}}!</label>
              </row>
              <row gap="8">
                <number bind="fov" value="70" min="30" max="110" decimals="0" prefix="FOV" width="90"/>
                <color-field bind="tint" value="#F0506E" width="110"/>
              </row>
            </column>""",
                """
<state name="nick" value="Alex"/>
<column gap="6">
  <row gap="8">
    <input bind="nick" placeholder="Nickname" variant="filled" width="140"/>
    <label>Hello, {{nick}}!</label>
  </row>
  <row gap="8">
    <number bind="fov" value="70" min="30" max="110" decimals="0" prefix="FOV" width="90"/>
    <color-field bind="tint" value="#F0506E" width="110"/>
  </row>
</column>""");
        TEXT.put("""
                                <row gap="10">
                                  <slot item="minecraft:golden_apple" count="3" glint name="Чарованное яблоко"
                                        lore="Даёт поглощение|И регенерацию"/>
                                  <slot item="minecraft:iron_pickaxe" durability="80/250" look="modern"/>
                                  <entity type="minecraft:zombie" mainhand="minecraft:iron_sword" baby auto-rotate
                                          zoomable="false" width="60" height="70"/>
                                  <player follow-mouse zoomable="false" width="60" height="80"/>
                                  <item-view item="minecraft:beacon" auto-rotate="60" zoomable="false" size="48"/>
                                </row>
                                """,
                """
<row gap="10">
  <slot item="minecraft:golden_apple" count="3" glint name="Enchanted apple"
        lore="Grants absorption|And regeneration"/>
  <slot item="minecraft:iron_pickaxe" durability="80/250" look="modern"/>
  <entity type="minecraft:zombie" mainhand="minecraft:iron_sword" baby auto-rotate
          zoomable="false" width="60" height="70"/>
  <player follow-mouse zoomable="false" width="60" height="80"/>
  <item-view item="minecraft:beacon" auto-rotate="60" zoomable="false" size="48"/>
</row>
""");
        TEXT.put("""
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
            """,
                """
# Markdown in Lumina UI
The `Markdown` widget renders **full markup**: *italic*, ~~strikethrough~~, ++underlined++,
==highlight== and ***everything at once***. Text wraps to the window width — try resizing the game.

## Lists and tasks
- Bulleted lists
  - with nesting
    - of any depth
1. Numbered ones
2. are here too
- [x] Tasks can be checked with a click
- [ ] Try checking this one
- [ ] And this one

## Code
Inline `code` and blocks with a highlighted background:
```java
new Markdown(text)
        .onLink(url -> openScreen(url))
        .onTaskToggle((item, done) -> save());
```

> Quotes are supported too.
> **All markup** works inside them.

## Tables
| Effect | Texture | Sound |
|:-------|:--------:|-----:|
| Fire | `fire_0` | flintandsteel |
| Explosion | `explosion_0..15` | generic.explode |
| TNT | `tnt_side` | tnt.primed |

## Icons and links
Items right in the text: ![](item:minecraft:diamond) diamond, ![](item:minecraft:tnt) TNT,
![](item:minecraft:blaze_rod) blaze rod, a texture ![](texture:minecraft:textures/block/fire_0.png).

Links are clickable: [effects page](lumina:effects), [gradients](lumina:gradients),
an autolink <https://github.com/ssbaxys/lumina-ui>.

---
Escaping: \\*not italic\\*, snake_case_words do not break.
""");
        TEXT.put("""
            ### Обновление 0.2
            * **Цвета:** RGB, HSV, HSL, OKLCH, разбор `"rgb(255 128 0 / 50%)"`
            * **Градиенты:** линейные, радиальные, конические, до 8 цветов
            * **Анимации:** ключевые кадры, последовательности, циклы
            * **Эффекты:** ![](item:minecraft:flint_and_steel) огонь, ![](item:minecraft:tnt) взрывы
            """,
                """
### Update 0.2
* **Colors:** RGB, HSV, HSL, OKLCH, parsing `"rgb(255 128 0 / 50%)"`
* **Gradients:** linear, radial, conic, up to 8 colors
* **Animations:** keyframes, sequences, loops
* **Effects:** ![](item:minecraft:flint_and_steel) fire, ![](item:minecraft:tnt) explosions
""");
        TEXT.put("""
                # Markdown
                Текст **жирный**, *курсив*, `код` и ![](item:minecraft:diamond) иконки.

                - [x] Современная тема
                - [x] Вкладки и боковое меню
                - [ ] Ваш экран настроек
                """,
                """
# Markdown
Text **bold**, *italic*, `code` and ![](item:minecraft:diamond) icons.

- [x] Modern theme
- [x] Tabs and a side menu
- [ ] Your settings screen
""");
        TEXT.put("""
            <state name="mode" value="normal"/>
            <state name="drop" value="Q"/>
            <state name="day" value="2026-10-12"/>
            <column gap="6">
              <row gap="8">
                <select bind="mode" options="peaceful,easy,normal,hard"
                        labels="Мирный,Лёгкий,Нормальный,Сложный" width="130"
                        tooltip="Сложность мира\\nМеняет урон мобов"/>
                <label>Режим: {{mode}}</label>
              </row>
              <row gap="8">
                <keybind bind="drop" default="Q" width="110"/>
                <date-picker bind="day" min="2026-01-01" width="110"/>
                <label tooltip="Дата: {{day}}" tooltip-kind="modern">Наведите сюда</label>
              </row>
            </column>""",
                """
<state name="mode" value="normal"/>
<state name="drop" value="Q"/>
<state name="day" value="2026-10-12"/>
<column gap="6">
  <row gap="8">
    <select bind="mode" options="peaceful,easy,normal,hard"
            labels="Peaceful,Easy,Normal,Hard" width="130"
            tooltip="World difficulty\\nChanges mob damage"/>
    <label>Mode: {{mode}}</label>
  </row>
  <row gap="8">
    <keybind bind="drop" default="Q" width="110"/>
    <date-picker bind="day" min="2026-01-01" width="110"/>
    <label tooltip="Date: {{day}}" tooltip-kind="modern">Hover here</label>
  </row>
</column>""");
    }

    private static void part7() {
        TEXT.put("Разложен",
                "Sorted");
        TEXT.put("Не разложен",
                "Not sorted");
        TEXT.put("Сундук Lumina",
                "Lumina Chest");
        TEXT.put("Цикл сервера: ",
                "Server cycle: ");
        TEXT.put("Предметов: ",
                "Items: ");
        TEXT.put("Тик мира: ",
                "World tick: ");
        TEXT.put("По имени",
                "By name");
        TEXT.put("По количеству",
                "By count");
        TEXT.put("Наполнить",
                "Fill");
        TEXT.put("Закрыть",
                "Close");
        TEXT.put("Shift — быстро, 1–9 и F — в хотбар,",
                "Shift — quick move, 1–9 and F — to the hotbar,");
        TEXT.put("Q — выбросить. Всё проверяет сервер.",
                "Q — drop. The server checks everything.");
        TEXT.put("Превью мира",
                "World preview");
        TEXT.put("Блоки вокруг игрока читаются из мира порциями по кадрам и перечитываются раз в 2 с; коробка ",
                "Blocks around the player are read from the world in portions per frame and re-read every 2 s; the box ");
        TEXT.put("идёт за игроком. В главном меню мира нет — показан выдуманный, игрок гуляет по кругу.",
                "follows the player. There is no world in the main menu — a made-up one is shown and the player walks in a circle.");
        TEXT.put("Читаем мир…",
                "Reading the world…");
        TEXT.put("Все слои",
                "All layers");
        TEXT.put("мир",
                "world");
        TEXT.put("демо-мир",
                "demo world");
        TEXT.put(" · блоков: ",
                " · blocks: ");
        TEXT.put(" · не загружено: ",
                " · not loaded: ");
        TEXT.put("Срез по игроку",
                "Slice at the player");
        TEXT.put("Снято слоёв: ",
                "Layers removed: ");
        TEXT.put("Обновить",
                "Refresh");
        TEXT.put("Снять слой",
                "Remove layer");
        TEXT.put("Вернуть слой",
                "Restore layer");
        TEXT.put("За игроком",
                "Follow the player");
        TEXT.put("Перспектива",
                "Perspective");
        TEXT.put("В галерею",
                "To the gallery");
        TEXT.put("Копировать текстом",
                "Copy as text");
        TEXT.put("Скопировано: ",
                "Copied: ");
        TEXT.put(" символов (BlockStructure.decode вернёт постройку)",
                " characters (BlockStructure.decode will return the build)");
    }

    static {
        part0();
        part1();
        part2();
        part3();
        part4();
        part5();
        part6();
        part7();
    }
}
