# Цвета, градиенты, темы, анимации, эффекты, Markdown

Короткий справочник по возможностям оформления. Всё это работает одинаково на Forge 1.20.1 и NeoForge 1.21.1,
потому что живёт в ядре; загрузчики только рисуют примитивы.

## Цвета (`render.Color`)

Цвет — обычный `int` ARGB, без объектов и аллокаций.

```java
Color.rgb(255, 128, 0);  Color.rgba(255, 128, 0, 0.5f);  Color.hex("#5B8CFF");
Color.hsv(200, 0.8f, 1f);  Color.hsl(200, 0.8f, 0.6f);  Color.oklch(0.7f, 0.15f, 250);
Color.parse("rgb(255 128 0 / 50%)");  Color.parse("hsl(200, 80%, 60%)");  Color.parse("tomato");
Color.rainbow(t);                        // радуга по t ∈ [0, 1]
```

Операции считаются в OKLCH, поэтому «светлее на 10%» выглядит одинаково для любого оттенка:
`lighten`, `darken`, `saturate`, `desaturate`, `rotateHue`, `withHue`, `complement`, `grayscale`, `invert`,
`withAlpha`, `mulAlpha`, `luminance`, `contrast` (WCAG), `readableOn` (чёрный или белый текст на фоне),
`over` (наложение). Смешивание: `Color.lerp(a, b, t)` (sRGB), `Color.mix(a, b, t)` (OKLAB),
`Color.lerp(a, b, t, ColorSpace.OKLCH)`.

Формат hex — как у int в Java: `#AARRGGBB` (или `#RRGGBB`).

## Кисти и градиенты (`render.Brush`)

| Кисть | Пример |
|---|---|
| Сплошная | `Brush.solid(0xFF5B8CFF)` |
| Линейная | `Brush.linear(135, a, b, c)`, `Brush.vertical(a, b)`, `Brush.horizontal(a, b)` |
| Радиальная | `Brush.radial(центр, край)`, `.at(0.3f, 0.3f)`, `.withRadius(0.7f, 0.35f)` (эллипс) |
| Коническая | `Brush.conic(угол, цвета...)`, `Brush.rainbowWheel()` |

- До 8 цветов, произвольные позиции и жёсткие переходы:
  `ColorStops.builder().add(0, a).hard(0.5f, a, b).add(1, b).build()`.
- `.withRepeating(true)` повторяет узор (полосы, кольца), `.withOffset(x)` сдвигает его (бегущие полосы).
- `.inSpace(ColorSpace.OKLAB / OKLCH / LINEAR / HSV / SRGB)` выбирает пространство смешивания.
- Угол как в CSS: 0° смотрит вверх, 90° вправо.
- Готовые наборы есть в `Gradients`: SUNSET, OCEAN, AURORA, NEON, FIRE, MINT, PEACH, NIGHT, GOLD, BERRY, LIME, RAINBOW.

Градиенты считаются в SDF-шейдере попиксельно, с дизерингом против полос. Простые двухцветные линейные
градиенты идут быстрым путём через цвета вершин. Кисти анимируются (`Brush.lerp`), в том числе
переход сплошная ↔ градиент.

Canvas умеет также `drawArc` (дуги с градиентом), `fillCircle` и `strokeCircle`, `drawLine`, `fillTriangle`,
`fillQuad`, `drawTextureQuad` (текстура на произвольном четырёхугольнике), `drawItem("minecraft:tnt", x, y, size)`
и `setBlendMode(NORMAL | ADD | MULTIPLY | SCREEN)`.

## Свойства стиля

Новые ключи в `Styles`:

| Ключ | Что делает |
|---|---|
| `BORDER_BRUSH` | Рамка градиентом (перекрывает `BORDER_COLOR`) |
| `GLOW` | Свечение (тень с аддитивным смешиванием) |
| `OUTLINE` | Контур снаружи элемента с отступом: `new Outline(ширина, отступ, цвет)` |
| `FOREGROUND` | Кисть поверх детей (затемнение, блик) |
| `TRANSLATE_X`, `TRANSLATE_Y` | Сдвиг без влияния на раскладку |
| `SCALE_X`, `SCALE_Y` | Масштаб по осям (вместе с `SCALE`) |
| `FONT_SCALE` | Масштаб текста; наследуется и влияет на раскладку |
| `BLEND` | Режим смешивания элемента и его детей |

## Темы и токены

Тема хранит стили классов и значения **токенов** (`ThemeVar`). Стили ссылаются на токены, а не на числа:

```java
theme.put("card", Style.builder()
        .bind(Styles.BORDER_COLOR, Tokens.ACCENT)                                   // как есть
        .bind(Styles.RADIUS, Tokens.CORNER, Radii::all)                              // с преобразованием
        .fromTheme(Styles.BACKGROUND, t -> Brush.vertical(t.get(Tokens.SURFACE), t.get(Tokens.SURFACE_ALT)))
        .build());

theme.set(Tokens.ACCENT, 0xFFF0506E);   // весь интерфейс плавно перекрашивается
theme.set(Tokens.CORNER, 4f);           // все скругления меньше
theme.set(Tokens.MOTION, 0f);           // выключить анимации переходов (или 2f — вдвое медленнее)
```

Встроенные токены: `ACCENT`, `TEXT`, `SURFACE`, `SURFACE_ALT`, `BORDER`, `SHADOW`, `WELL`, `CORNER`, `MOTION`.
Свои токены создаются так: `ThemeVar.of("brand", Integer.class, 0xFF00FF00)`.

Ещё возможности:
- Несколько классов через пробел: `styleClass("button danger pill")`. Классы накладываются по порядку.
- `theme.edit("button", b -> ...)` дополняет стиль класса, а `theme.copy()` создаёт независимую копию темы.
- Переход для каждого свойства задаётся отдельно:
  `Transition.of(0.2f).key(Styles.SCALE, 0.5f, Easing.OUT_ELASTIC).key(Styles.RADIUS, 0.3f, Easing.OUT_CUBIC, 0.4f).instant(Styles.BORDER_WIDTH)`.

## Анимации

```java
// Любое свойство стиля, ключевые кадры, циклы и yoyo:
box.animate(Styles.TRANSLATE_X).keyframe(0, 0f).keyframe(0.5f, 160f, Easing.OUT_BOUNCE).keyframe(1, 0f)
        .duration(2.4f).loop().play();
box.animate(Styles.BACKGROUND).from(Brush.solid(a)).to(Gradients.NEON).duration(1).yoyo().loops(4).play();

// Таймлайн из частей:
element.play(Motion.sequence(
        Motion.call(() -> sound()),
        Motion.parallel(a.animate(Styles.OPACITY).to(1f).motion(), b.animate(Styles.SCALE).to(1.2f).motion()),
        Motion.delay(0.2f),
        VanillaFx.shake(element, 4f, 0.4f)));
```

- `Playback` управляет проигрыванием: `loops`, `loop()`, `yoyo`, `speed`, `pause`/`resume`, `onFinish`.
- Анимации пишут в отдельный слой поверх стиля, поэтому hover и pressed под ними продолжают работать.
  Когда элемент удаляется из дерева, его анимации останавливаются сами.
- Кривые: 23 встроенные (`Easing.presets()`), а также `Easing.cubicBezier`, `steps(n)`, `spring(затухание, колебания)`,
  `reversed()` и `mirrored()`.

## Ванильные эффекты (`fx`)

`FxLayer` — это слой эффектов, который кладётся поверх чего угодно (например, в `StackLayout`).

```java
fx.spawn(VanillaFx.fire(x, y, 32, 32));                 // огонь; .stop() плавно гасит
fx.spawn(VanillaFx.soulFire(x, y, 32, 32));
fx.spawn(VanillaFx.explosion(cx, cy, 1f));              // огненные клубы, вспышка, дым, искры, звук
fx.spawn(VanillaFx.primedTnt(x, -20, floorY, 22, 3f)    // падает, отскакивает, мигает, взрывается
        .velocity(40, 0).onExplode(() -> panel.play(VanillaFx.shake(panel, 6, 0.5f))));
fx.spawn(VanillaFx.flames(x, y, 24));  fx.spawn(VanillaFx.lavaPop(x, y, 14));  fx.spawn(VanillaFx.smokePuff(x, y, 20));
```

Погода и частицы:
- `snow` и `rain` используют ванильные текстуры осадков двумя слоями, плюс хлопья, капли и брызги; `cherryLeaves`, `drips` (вода или лава), `bubbles`.
- Частицы: `hearts`, `angry`, `happy`, `note` (цвет зависит от высоты ноты), `crit` (обычный и магический), `totem`, `enchant` (руны стягиваются к точке).

Эффекты используют ванильные текстуры и звуки по идентификаторам, поэтому подхватывают ресурспаки игрока.

**Реестр по именам.** Все встроенные эффекты зарегистрированы как `minecraft:*`. Свои эффекты добавляются одной строкой:

```java
FxRegistry.register("mymod:confetti", (x, y, w, h) -> new Particles(null).gravity(140).wobble(30, 1.2f)
        .emitter((p, r, dt) -> p.emit(x + r.nextFloat() * w, y, 0, 40, 2f, 2f, Color.hsv(r.nextFloat() * 360, 0.7f, 1)), 3f));
fx.spawn("mymod:confetti");                 // на весь слой
fx.spawn("minecraft:snow", x, y, w, h);     // встроенный по имени
```

Детали для своих частиц (`Particles`): `gravity`, `drag`, `growth`, `tint`, `blend`, `wobble` (покачивание),
`attract` (притяжение к точке), `floor` и `ceiling` (с обработчиком приземления), `streak` (черточки по скорости),
`frames(BY_LIFE | LOOP | RANDOM)`, `emitter`. Без спрайта (`new Particles(null)`) частицы рисуются пиксельными квадратиками.
Готовые эффекты можно объединять: `Effect.group(a, b)`. Эффект без своего класса: `Effect.of(тик, отрисовка)`.
Для анимированных текстур есть `SpriteSheet`.

## Markdown (`markdown.Markdown`)

```java
new Markdown(text)
        .onLink(url -> ...)                        // клик по ссылке
        .onTaskToggle((item, done) -> ...);        // клик по задаче «- [ ]»
Markdown.bound(signal);                            // текст из сигнала
```

Поддерживается:
- заголовки `#` и подчёркиванием;
- `**жирный**`, `*курсив*`, `~~зачёркнутый~~`, `++подчёркнутый++`, `==выделение==`, `` `код` ``;
- блоки кода, цитаты;
- вложенные маркированные и нумерованные списки, задачи;
- таблицы с выравниванием;
- ссылки и автоссылки;
- разделители, экранирование;
- иконки `![](item:minecraft:diamond)` и `![](texture:minecraft:textures/block/stone.png)`;
- подсветка блоков кода с языком: java, json, css, html и другие (см. «Данные, формы и книги»).

Текст переносится по ширине. Разбор выполняется один раз при смене текста, раскладка — при смене ширины.
Цвета берутся из стиля темы.

## Новые виджеты

- `ColorPicker`: квадрат насыщенности и яркости, полоса оттенка и полоса прозрачности; работает с клавиатуры; значение хранится в `Signal<Integer>`.
- `RingProgress`: круговой индикатор с градиентной дугой и процентом в центре или бесконечный спиннер (`.spinner()`).

## Карты из блоков

`BlockMap` рисует 2D-карту (вид сбоку) из блоков игры. Текстура подбирается по id блока из ресурсов игры
(с ресурспаками): `minecraft:tnt` → `block/tnt_side`, `minecraft:water` → `block/water_still` с синим оттенком,
листва и трава окрашиваются как на равнинах. Вода, лава и огонь анимируются со скоростью из `.png.mcmeta`.

```java
BlockMap map = BlockMap.of(16,
        "....T....",
        "GGGGGGWWG",
        "DDDDDDDDD")
    .key('G', "minecraft:grass_block").key('D', "minecraft:dirt")
    .key('W', "minecraft:water").key('T', "minecraft:tnt");
map.set(4, 0, "minecraft:fire");          // поставить, null — убрать
map.fill(0, 2, 8, 2, "minecraft:stone");  // залить прямоугольник
map.editable(brush).grid(true);           // редактор: ЛКМ ставит блок из brush, ПКМ ломает
map.onChange((x, y, id) -> save());
String[] rows = map.toRows();             // обратно в ряды + map.keyMap() — для сохранения
```

Свой вид блока: `BlockTextures.register("mymod:crystal", TextureRef.of("mymod:textures/block/crystal.png"), 0xFFFFFFFF)`.
Блок без найденной текстуры рисуется иконкой предмета. В разметке — тег `<blocks>` (см. [MARKUP.md](MARKUP.md)).

## Постройки в 3D

`BlockStructure` — разреженная 3D-сетка блоков (y вверх), `BlockModel` рисует её в ортографической проекции:
изометрия 45°/30° по умолчанию, вращение мышью с инерцией, приближение колесом, автовращение. Рисуются только
открытые грани (стекло, листва, вода, огонь, факелы соседей не закрывают), от дальних к ближним, с затенением как
в игре; верх и низ берутся из `_top`/`_bottom`/`_end` (`BlockTextures.top/bottom`). Без текстуры грань заливается
цветом из id блока.

```java
BlockStructure house = BlockStructure.fromLayers(Map.of('P', "minecraft:oak_planks", 'G', "minecraft:glass"),
        "PPP", "PPP",      // нижний слой: строка — ряд по z, символ — x
        "---",
        "PGP", "P.P");     // следующий слой; '.' — воздух
house.fill(0, 2, 0, 2, 2, 1, "minecraft:spruce_planks");
BlockStructure wall = BlockStructure.extrude(blockMap, 3);   // 2D-карта → 3D
BlockModel model = new BlockModel(house).autoRotate(true).size(200, 200);
model.camera(30, 20).cutAbove(1);                             // ракурс и срез по слою
BlockModel.drawBlockIcon(canvas, "minecraft:tnt", x, y, 16, time);  // один блок в изометрии
```

В разметке — тег `<structure>` (см. [MARKUP.md](MARKUP.md)).

## Компоненты

**Текст с эффектами** (`FancyText`) рисует строку посимвольно обычным шрифтом игры:

```java
new FancyText("Lumina UI").scale(2).gradient(0xFFFF8A00, 0xFFE52E71).flow(0.4f).outline(0xFF000000);
new FancyText("Радуга").rainbow().wave(2f, 6f);        // радуга и волна (амплитуда, скорость)
new FancyText("Тряска!").shake(0.8f);
new FancyText("Привет!").typewriter(24f).typeSound("minecraft:ui.button.click").onTyped(this::next);
```

**Таблица** (`Table<T>`): колонки с весами ширины, сортировка кликом по заголовку, выбор строки, «зебра»,
обрезка длинного текста многоточием. Ячейку можно рисовать самому (иконка, полоска):

```java
Table<Ore> t = new Table<Ore>()
        .column("Руда", 2, Ore::name)
        .column("Добыто", 1, o -> String.valueOf(o.count()), Comparator.comparingInt(Ore::count))
        .column("Шанс", 1, (canvas, ore, x, y, w, h) -> canvas.fillRect(x, y + 5, w * ore.chance(), 6, 0xFF5B8CFF), null)
        .rows(ores);
t.selection().subscribe(o -> show(o));
```

**Дерево** (`TreeView`): узлы с иконками предметов, раскрытие стрелкой, двойным кликом или клавишами.
`TreeView.node("Мир", "minecraft:grass_block").add(node("Незер"), ...)`, `selection()`, `onSelect(...)`.

**Графики** (`Chart`): `LINE`, `AREA`, `BAR`, `PIE`, `DONUT`. Значения вырастают при появлении, `values(серия, ...)`
плавно меняет их, наведение показывает подсказку.

```java
new Chart(Chart.Kind.BAR).labels("Пн", "Вт", "Ср").series("Камень", 0xFF8AB4F8, 120, 180, 90).unit(" шт.");
```

**Загрузка**: `Spinner` (`RING`, `DOTS`, `BARS`, `PULSE`) и `Skeleton` (`line(w)`, `fullLine()`, `circle(d)`) с бегущим бликом.

**Окна** (`Window`): ставятся внутрь любого контейнера абсолютно (`at(x, y)`), таскаются за заголовок,
растягиваются за уголок, нажатие поднимает окно наверх (`UiElement.bringToFront()`), Esc и крестик закрывают
с анимацией. `modal(true)` затемняет всё под окном и не пропускает клики.

**Живые обои** (`Backdrop`): `AURORA`, `STARS`, `WAVES`, `FLOW`, `GRID`; `colors(...)`, `speed(...)`,
`parallax(пиксели)` — слои смещаются за мышью. Кладутся первым слоем в `StackLayout`, мышь не перехватывают.

**Рамка из текстуры** (`NineSlice`): углы не растягиваются, края и середина — да.

```java
panel.style(b -> b.set(Styles.NINE_SLICE, NineSlice.of("minecraft:textures/gui/demo_background.png", 248, 166, 4).scaled(2)));
```

В CSS: `nine-slice: url(minecraft:textures/gui/demo_background.png) 248 166 4 / 2` (рамка 1 или 4 числами, после `/` масштаб).

**Страницы с переходом** (`Pages`): показывает одну страницу по номеру из сигнала, удобно вместе с `Tabs` и `SideNav`.
Эффекты `FADE`, `SLIDE` (по направлению смены номера), `SLIDE_UP`, `ZOOM`, `FLIP`; страница создаётся при первом показе
и по умолчанию сохраняется (`keepAlive`).

```java
Signal<Integer> tab = Signal.of(0);
new Tabs(tab, "Экран", "Вид");
new Pages(tab).effect(Pages.Effect.SLIDE).page(this::screen).page(this::view);
```

Готовый пример экрана настроек в современном стиле — раздел «Современный стиль» витрины (`ModernPage`).

## Данные, формы и книги

### Список на десятки тысяч строк (`VirtualList<T>`)

Строки одной высоты; рисуется и раскладывается только видимое, поэтому 10 000 или 100 000 записей стоят столько же,
сколько десять. Прокрутка плавная (пружина; дальний прыжок по Home/End сразу подводит к цели и доезжает последний экран),
с колесом и ползунком; клавиши ↑/↓, PageUp/PageDown, Home/End двигают выбор, Enter и двойной клик вызывают `onActivate`.

```java
// Строки рисуются прямо на Canvas — без единого элемента на строку.
VirtualList<Item> list = new VirtualList<Item>(18, (c, item, i, x, y, w, h, row) -> {
            c.drawItem(item.id(), x + 2, y + 1, 16);
            c.drawText(item.name(), x + 22, y + 5, row.textColor(), false);   // row.selected(), row.hovered()
        })
        .bind(view.visible())            // или .items(list)
        .emptyText("Ничего не найдено")
        .onActivate(this::give)
        .height(Dim.px(180));
list.selection().subscribe(this::preview);   // выбранная запись; selection().set(x) выбирает извне
VirtualList.ofText(16, Item::name);          // просто текст

// Строки-виджеты: создаётся столько, сколько видно (+1), при прокрутке в них подставляются другие записи.
new VirtualList<Item>(24, null).rows(ItemRow::new, (row, item, index) -> row.show(item));
```

Методы: `items()`, `refresh()` (после правки списка на месте), `select(i)`, `selectedIndex()`, `scrollTo(y, анимация)`,
`ensureVisible(i, анимация)`, `firstVisible()`/`lastVisible()`, `visibleRows(n)` (высота без явного размера), `zebra(false)`.
Стиль — `virtual-list`.

### Поиск, фильтры, сортировка и страницы (`data.DataView<T>`)

Представление над списком: результат — сигналы, их читают `VirtualList`, `Table` и `Paginator`.

```java
DataView<Item> view = new DataView<>(items)            // или DataView.of(сигналСоСписком)
        .searchBy(i -> i.name() + " " + i.category())  // поиск: все слова запроса, без учёта регистра
        .sort(Comparator.comparing(Item::name))        // sort(c, true) — по убыванию, sort(null) — как в данных
        .pageSize(50);                                 // 0 — без страниц
new TextField(view.query());                           // строка поиска — обычный Signal<String>
view.setFilter("блоки", Item::isBlock);                // именованный фильтр; setFilter("блоки", null) снимает
view.filterWhen("редкие", onlyRare, i -> i.rarity() > 1);   // действует, пока сигнал true
view.visible();   // записи текущей страницы    view.filtered();  // все совпавшие    view.total();  // сколько
view.page(); view.pageCount(); view.nextPage(); view.previousPage(); view.dispose();
```

Новый запрос или фильтр возвращает на первую страницу; номер страницы вне границ приводится к ним. Строки поиска
приводятся к нижнему регистру один раз при смене данных.

**Переключатель страниц** (`Paginator`): «« ‹ 1 … 4 5 6 … 20 › »», подсветка текущей страницы переезжает пружиной,
←/→/Home/End и клик. `new Paginator(view)` или `new Paginator(page, pageCount)`; `siblings(2)`, `edgeArrows(false)`.
Стиль — `paginator`.

**Таблица** (`Table`) рисует только строки, попавшие в видимую область (окно `ScrollPanel`, край экрана), поэтому
таблица на тысячи строк в прокрутке не тормозит. `table.bind(view)` показывает текущую страницу представления, а клик
по заголовку сортирует всё представление; `table.bind(signal)` — строки из любого сигнала.

### Проверка ввода и формы

Проверки (`Validator`) вешаются на любое поле (`TextField`, `NumberField`, `ColorField`):

```java
new TextField(login).validate(Validator.required(), Validator.minLength(3),
        Validator.pattern("[A-Za-z0-9_]+", "Только латиница, цифры и _"));
new NumberField(age).validate(Validator.range(13, 120, "Возраст от 13 до 120"));
new TextField(repeat).validate(Validator.sameAs(password::peek, "Пароли не совпадают"));
Validator.of(s -> s.chars().anyMatch(Character::isDigit), "Нужна цифра");   // своя проверка
// ещё: Validator.email(), integer(), maxLength(16), v.and(other), v.message("Своё сообщение"); field.required()
```

Все готовые проверки, кроме `required`, пропускают пустой текст. Ошибка показывается, когда поле теряет фокус после
правки, и дальше обновляется на каждой правке. У поля: `error()` — показанная ошибка (сигнал), `valid()` — проходит ли
текст проверки прямо сейчас (сигнал), `validate()` — проверить и показать, `check()` — проверить молча,
`setError("Имя занято")` — своя ошибка (ответ сервера), `resetValidation()`. Неверное поле получает состояние
`FieldBase.INVALID` и красную рамку.

**Строка формы** (`FormField`): подпись, поле, пояснение (`hint`) и ошибка, которая выезжает под полем, раздвигая место.
**Форма** (`Form`) собирает поля:

```java
Form form = new Form()
        .onSubmit(() -> register(nick.peek()))
        .onInvalid(() -> toast("Исправьте ошибки"));
panel.add(
        form.field("Ник", new TextField(nick).required()).hint("3–16 символов"),
        form.field("Почта", new TextField(mail).validate(Validator.required(), Validator.email())),
        form.submitButton(new Button("Создать"), false));    // true — кнопка недоступна, пока форма неверна
form.check(() -> agree.peek());      // своя проверка всей формы
form.valid();                        // сигнал «всё верно», меняется на каждой правке
form.validate(); form.submit(); form.reset(); form.errors();
new Form().collect(markupRoot);      // все поля с проверками внутри элемента
```

`submit()` записывает незаписанный ввод, показывает ошибки у всех полей сразу и ставит фокус на первое неверное.
Стили: `form-field`, `form-label`, `form-hint`, `form-error`. В разметке — `<form>`, `<field>` и атрибуты `required`,
`minlength`, `pattern` (см. [MARKUP.md](MARKUP.md)).

### Мастер настройки (`Wizard`) и индикатор шагов (`Stepper`)

`Stepper` — пронумерованные кружки с линией прогресса и подписями: пройденные шаги с галочкой, текущий пульсирует,
линия пружиной дотягивается до шага. `new Stepper(step, "Аккаунт", "Профиль", "Готово").clickable(i -> i < step.peek())`.

`Wizard` — `Stepper`, страница шага (сдвиг через `Pages`) и кнопки «Назад» / «Далее» / «Готово». «Далее» сначала
выполняет проверку шага; не прошла — страница вздрагивает и остаётся.

```java
new Wizard()
        .step("Сервер", serverPage, serverForm)               // проверка — Form шага (ошибки под полями)
        .step("Режим", this::modePage, () -> mode.peek() != null)
        .step("Готово", this::summary)
        .labels("Назад", "Далее", "Создать сервер")
        .onFinish(this::create);
wizard.step();   // Signal<Integer> с номером шага; next(), back(), isFinished()
```

Стили: `stepper`, `wizard`, `wizard-nav`.

### Книга (`markdown.Book`)

Markdown, разбитый на страницы: разрыв — строка `<!-- page -->` или `\newpage`; что не влезло, само переносится на
следующую страницу (длинные абзацы режутся по словам).

```java
new Book(text);                                              // как книга Minecraft: текстура, «Страница 1 из 5», стрелки
new Book(text).look(Book.Look.MODERN).tableOfContents(true)  // разворот на карточке темы + оглавление первой страницей
        .width(Dim.px(340)).height(Dim.px(220));
book.page().set(4); book.next(); book.previous(); book.goTo(0); book.pageCount(); book.toc();
```

Листается стрелками, колесом, ←/→, PageUp/PageDown, Home/End, с анимацией переворота листа и звуком
`item.book.page_turn` (`sound(false)`, `flipDuration(0.3f)`). Оглавление собирается из заголовков `#` и `##`
(`tocLevel(3)`), его пункты — ссылки. Ссылки `[текст](#page:5)` и `[текст](#название-главы)` листают книгу,
остальные уходят в `onLink`. `spread(false)` — современная книга в одну страницу. Стили: `book` и `book.vanilla`.

### Подсветка кода (`markdown.SyntaxHighlighter`)

Блоки кода в `Markdown` и `Book` с языком подсвечиваются: `java`, `kotlin`, `javascript`/`js`/`ts`, `json`, `css`,
`html`/`xml`/`svg`. Разбор идёт один раз при раскладке, многострочные комментарии и текстовые блоки учитываются.
Цвета подбираются под тему (светлый или тёмный текст, акцент в ключевых словах); задать свои — токенами темы:

```java
theme.set(SyntaxHighlighter.STRING_COLOR, 0xFF98C379).set(SyntaxHighlighter.KEYWORD_COLOR, 0xFFC678DD);
SyntaxHighlighter.register((line, state, out) -> { out.span(0, line.length(), SyntaxHighlighter.COMMENT); return 0; },
        "mylang");                                     // свой язык: размечает строку, возвращает состояние
SyntaxHighlighter.highlight("java", code);             // куски строк с видами лексем
```

Пример всего раздела — страница витрины `DataToolsPage`.
