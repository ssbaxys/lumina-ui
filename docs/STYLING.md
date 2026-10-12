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
- иконки `![](item:minecraft:diamond)` и `![](texture:minecraft:textures/block/stone.png)`.

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
