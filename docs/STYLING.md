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

`BlockStructure` — разреженная 3D-сетка блоков (y вверх), `BlockModel` рисует её: изометрия 45°/30° по умолчанию,
`perspective(0.45f)` — настоящая перспектива (дальнее меньше ближнего), вращение мышью с инерцией, приближение
колесом, автовращение. Модель вписана по описанной сфере, поэтому при вращении не меняет размер. Рисуются только
открытые грани (стекло, листва, вода, огонь, факелы соседей не закрывают), от дальних к ближним по центру грани,
с затенением как в игре; верх и низ берутся из `_top`/`_bottom`/`_end` (`BlockTextures.top/bottom`). Без текстуры грань заливается
цветом из id блока.

```java
BlockStructure house = BlockStructure.fromLayers(Map.of('P', "minecraft:oak_planks", 'G', "minecraft:glass"),
        "PPP", "PPP",      // нижний слой: строка — ряд по z, символ — x
        "---",
        "PGP", "P.P");     // следующий слой; '.' — воздух
house.fill(0, 2, 0, 2, 2, 1, "minecraft:spruce_planks");
BlockStructure wall = BlockStructure.extrude(blockMap, 3);   // 2D-карта → 3D
BlockModel model = new BlockModel(house).autoRotate(true).size(200, 200);
model.camera(30, 20).cutAbove(1).perspective(0.45f);         // ракурс, срез по слою, перспектива
BlockModel.drawBlockIcon(canvas, "minecraft:tnt", x, y, 16, time);  // один блок в изометрии
```

В разметке — тег `<structure>` (см. [MARKUP.md](MARKUP.md)).

### Обмен постройками текстом

`encode()` превращает постройку в одну строку, как схематику: формат, размер, угол, палитра и блоки по слоям
с повторами (`3A` — три первых блока палитры подряд, `.` — воздух, после 52 разных блоков — `_Ab`).
`BlockStructure.decode(text)` возвращает её обратно; пробелы и переносы внутри блоков не мешают, так что строку
можно вставить из чата. `normalized()` сдвигает постройку к (0, 0, 0).

```java
String text = house.encode();   // "lumina:1;3x2x2;0,0,0;minecraft:stone,minecraft:glass;4AB2A"
BlockStructure copy = BlockStructure.decode(text);
```

### Превью мира (`widget.StructurePreview`)

3D-вид блоков вокруг игрока: коробка `radius` в стороны, `below`/`above` слоёв от ног (не больше 48 по оси)
читается из клиентского мира порциями по кадрам (`blocks.WorldCapture`, по умолчанию 8192 блока за кадр, слоями
снизу вверх). Воздух не хранится, незагруженные чанки оставляют прежние блоки и дочитываются через секунду,
а в постройку пишутся только изменения — модель перестраивается, лишь когда мир правда поменялся. Сверху — метка
игрока со стрелкой взгляда. Это `BlockModel`, поэтому вращение мышью, колесо и `perspective(...)` работают как обычно;
вписывается модель по коробке (`frame`), а не по блокам, и не прыгает, пока снимок дочитывается.

```java
StructurePreview preview = new StructurePreview()
        .radius(10).height(5, 9)          // 21×15×21 вокруг игрока
        .fallback(new DemoWorld(7))       // без мира (главное меню) — выдуманный мир, игрок гуляет по кругу
        .follow(true)                     // коробка идёт за игроком (сдвиг через каждые 2 блока)
        .refreshInterval(2f)              // перечитывать раз в 2 с; 0 — только refresh()
        .onRefresh(() -> status.text("блоков: " + preview.structure().size()));
preview.perspective(0.4f).size(380, 250);
preview.peel(3);                          // снять 3 верхних слоя
preview.cutAtPlayer(true);                // срез по голове игрока — заглянуть под крышу
preview.refresh();                        // перечитать сейчас и поставить коробку вокруг игрока
String shared = preview.snapshot().encode();   // снимок от угла коробки — строкой
```

Мир даёт `Platform.worldBlocks()` — объект `blocks.WorldBlocks` (`blockAt(x, y, z)` → id, `null` — воздух,
`WorldBlocks.UNLOADED` — чанк не загружен; положение и поворот игрока). Загрузчики читают
`ClientLevel.getBlockState`, id кэшируют на каждый блок, незагруженные чанки (`hasChunk`) не трогают. Свой
источник — `source(() -> myWorld)`. Разовый снимок без кадров — `Platform.captureWorld(x0, y0, z0, x1, y1, z1)`
(постройка от минимального угла, тоже до 48 по оси) или `WorldCapture.capture(world, ...)`.

В разметке — тег `<world-preview>` (см. [MARKUP.md](MARKUP.md)).

## Слоты инвентаря, сущности и предметы в 3D

### Стопки и инвентарь (`inventory.Stack`, `inventory.Inventory`)

`Stack` — неизменяемая стопка для интерфейса, не зависящая от версии игры: id предмета, количество, своё
название, строки описания, перелив зачарования и прочность. `Inventory` — модель ячеек: слоты только показывают её
и меняют через неё, поэтому мод синхронизирует данные одной подпиской `onChange`, а правила проверяются без экрана.

```java
Stack sword = Stack.of("minecraft:diamond_sword").withName("Экскалибур")
        .withLore("Выкован в Незере").withGlint(true).withDurability(400, 1561);   // предел стопки — 1
Stack coal = Stack.parse("coal*48");                                           // "minecraft:coal", 48
Inventory chest = new Inventory(27);
chest.set(0, sword);
Stack rest = chest.insert(Stack.of("minecraft:stone", 200));   // в такие же стопки, потом в пустые; вернёт остаток
chest.filter((index, s) -> index != 26 || s.itemId().endsWith("_ingot")).limit(64);
chest.onChange((inv, index, before, after) -> sync(index, after));
```

Стопки складываются (`canStackWith`), если совпадает всё, кроме количества. Предел — 64, у предметов с
прочностью 1, свой — `withMaxStackSize(16)`; инвентарь может урезать его `limit(n)`.

### Слоты и сетки (`Slot`, `SlotGrid`, `InventoryPanel`)

`Slot` показывает одну ячейку: предмет, количество в правом нижнем углу (тысячи — `1.5k`), полоску прочности
цветом от зелёного к красному и перелив. Вид — `Slot.Look.VANILLA` (серая ячейка 18×18 с объёмной кромкой и белой
подсветкой) или `MODERN` (скруглённая ячейка темы из токенов `well`, `border`, `accent`; фон, радиус и рамку можно
задать стилем). У непустого слота — подсказка как в игре (`ItemTooltip`: название, бирюзовое у зачарованных,
описание, прочность у повреждённых), вид подсказки — `tooltipKind`.

```java
Slot single = new Slot(Stack.of("minecraft:diamond", 12));       // свой инвентарь на одну ячейку
Slot helmet = new Slot(armor, 0).placeholder("minecraft:iron_helmet");   // бледный силуэт в пустом слоте
Slot result = new Slot(craft, 0).dropTarget(false);               // только брать
SlotGrid hotbar = new SlotGrid(player, 0, 9, 9).look(Slot.Look.MODERN).slotSize(22).gap(3);
InventoryPanel chestUi = InventoryPanel.chest("Сундук", chest, player, Slot.Look.VANILLA);
InventoryPanel mine = new InventoryPanel(Slot.Look.MODERN).title("Печь").add(row).add(hotbar, 4);
Slot.paintStack(canvas, stack, x, y, 16);                         // стопка в своём виджете
```

`InventoryPanel.chest` строит окно сундука: сетка 9 в ряд, «Инвентарь», рюкзак (ячейки игрока 9..35) и хотбар
(0..8), Shift+клик ходит между сундуком и игроком. Инвентарь игрока в ядре — обычная модель на 36 ячеек; с
настоящим инвентарём и сервером её связывают окна-контейнеры (ниже).

### Перетаскивание (`SlotGroup`)

- ЛКМ-перетаскивание (от 3 px) берёт стопку целиком, ПКМ — половину с округлением вверх; под курсором летит
  «призрак» (пассивный оверлей корня), источник показывает остаток. Модель не меняется до сброса, так что отмена
  ничего не откатывает, а мод видит по одному изменению на ячейку.
- Сброс на пустой слот или такую же стопку докладывает, сколько влезет; остаток остаётся в источнике. Вся стопка
  на другой предмет — обмен местами (если обе ячейки примут чужое). Слот под курсором подсвечивается: обычной
  подсветкой, если положить можно, красной — если нельзя.
- Мимо слотов — `onDropOutside` (вернуть `true`, если стопку забрали), иначе отмена.
- Shift+клик — `quickMove`: свой `onQuickMove` или перенос в инвентари из `Slot.quickMoveTo` (диапазоны ячеек
  тоже можно). Shift ядро узнаёт через `Platform.isShiftDown()`.

```java
SlotGroup group = new SlotGroup()
        .validator((from, to, stack) -> to.inventory() != shop)          // проверка сброса поверх фильтров
        .onTransfer((from, to, moved) -> playSound())                   // to == null — Shift+клик или выброс
        .onDropOutside((from, stack) -> { throwItem(stack); return true; })
        .onQuickMove((slot, stack) -> false);                           // false — перенос по умолчанию
chestGrid.group(group);
group.beginDrag(slot, false); group.dropOn(otherSlot);                  // то же из кода (тесты, клавиатура)
```

Без своей группы слоты в общей `SlotGroup.shared()`: перетаскивать можно между любыми слотами.

### Окна-контейнеры с сервером (`menu.MenuSpec`, `menu.RemoteMenu`)

Окно сундука, печи или своей машины, которым владеет сервер, как ванильным сундуком. `MenuSpec` — описание,
общее для сервера и клиента (обычно константа): разделы ячеек и синхронизируемые числа. На загрузчике
`LuminaMenu` — обычное `AbstractContainerMenu` с ванильными слотами (содержимое и курсор рассылает
`broadcastChanges`, Shift+клик по умолчанию — как у сундука), значения идут `DataSlot` (целое — двумя 16-битными
частями), окно — `LuminaMenuScreen` с `UiRoot` из вашей фабрики.

```java
// общий код
static final MenuSpec SPEC = MenuSpec.builder()
        .container("chest", 27).playerInventory()        // слоты меню: 0..26, затем рюкзак и хотбар
        .intValue("progress").boolValue("burning").build();
static final LuminaMenuType CHEST = new LuminaMenuType(SPEC);
MENUS.register("chest", CHEST::create);                   // DeferredRegister<MenuType<?>>

// сервер
CHEST.open(player, Component.literal("Сундук"), container, index -> progress, (p, action, value) -> sort());

// клиент: Forge — FMLClientSetupEvent.enqueueWork, NeoForge — RegisterMenuScreensEvent
LuminaMenuScreen.register(CHEST, menu -> new UiRoot(InventoryPanel.chest(menu.title(),
        menu.inventory("chest"), menu.playerInventory(), Slot.Look.MODERN), ModernTheme.dark()));
new ProgressBar().bind(menu.intValue("progress").map(p -> p / 100f));
new Button("Сортировать", () -> menu.sendAction(SORT, 0));
```

`RemoteMenu` даёт на каждый раздел обычный `Inventory`, но удалённый (`Inventory.remote`): слоты и сетки
выглядят и перетаскиваются как всегда, а при сбросе отправляют ванильные щелчки через
`handleInventoryMouseClick` — сервер проверяет, игра сразу предсказывает результат. Перенос — «взять» (всё или
половину), «положить» и, если что-то осталось на курсоре, «вернуть»; Shift+клик — быстрый перенос (маршрут решает
сервер, `Slot.quickMoveTo` не нужен); мимо слотов — выброс, если `onDropOutside` вернул `true`; клавиши 1–9 и F над
слотом — обмен с хотбаром и второй рукой, Q — выбросить (с Ctrl — стопку). Без `remote` инвентарь локальный,
как раньше; смешивать удалённые и локальные ячейки в одном переносе нельзя.

Содержимое слотов окно сверяет каждый кадр (ссылка и количество, полностью — раз в тик) и строит `Stack` только
для изменившихся: название, строки подсказки игры (с цветами), прочность, предел стопки и сама стопка игры
(`Stack.platformStack()`), чтобы зелья и головы рисовались точно. Значения — сигналы `intValue`/`boolValue`,
обновляются разом раз в кадр. Свои кнопки — `sendAction(id, число)`: пара кодируется нажатиями кнопки меню
(`clickMenuButton`, 7 бит на нажатие, обычно два), свой сетевой канал не нужен. Демо — `/luminatest chest`.

### Сущности и предметы в 3D (`preview.EntityView`, `preview.ItemView`)

`EntityView` рисует моба или самого игрока так же, как игра в окне инвентаря: модель вписана в виджет ногами вниз,
крутится мышью с инерцией, может вращаться сама, приближается колесом (`zoomable(false)` — не мешать прокрутке
страницы), а с `followMouse(true)` смотрит на курсор. Сущность создаётся на клиенте по одной на тип и внешний вид,
поэтому её видно только в мире (в главном меню — пусто). `ItemView` — крупный предмет в 3D: блоки объёмные,
остальное — как выброшенное на землю; бэкенд без поворота рисует плоскую иконку.

```java
new EntityView("minecraft:zombie").equip("mainhand", "minecraft:iron_sword").baby(true).autoRotate(true).size(64, 80);
EntityView.player().followMouse(true).size(110, 150);          // вы в своей броне
new EntityView("minecraft:creeper").camera(-25, 10).zoom(1.5f); // yaw > 0 — лицом влево, pitch > 0 — сверху
new ItemView(Stack.of("minecraft:diamond_sword").withGlint(true)).autoRotate(40f).size(72, 72);
```

Общая основа — `SpinView`: `yaw/pitch/camera`, `pitchRange`, `autoRotate`, `zoom`, `interactive`, `zoomable`.
Для своих виджетов — методы `Canvas`:

| Метод | Что рисует |
|---|---|
| `drawItem(id, x, y, size, glint)` | предмет с переливом зачарования |
| `drawItem3d(id, x, y, size, yaw, pitch, glint)` | предмет в 3D, повёрнутый вокруг центра |
| `drawEntity(type, x, y, size, yaw, pitch, options)` | сущность, вписанная в квадрат; `Canvas.PLAYER` — игрок |
| `drawPlayer(x, y, size, yaw, pitch, options)` | то же, что `drawEntity(Canvas.PLAYER, …)` |

`EntityOptions` — поза головы (`look(yaw, pitch)`), детёныш (`baby`) и снаряжение (`equip("head", id)`; места —
`mainhand offhand head chest legs feet`). По умолчанию у `Canvas` эти методы ничего не рисуют (или рисуют плоскую
иконку), так что свой бэкенд может их не поддерживать.

В разметке — теги `<slot>`, `<entity>`, `<player>`, `<item-view>` (см. [MARKUP.md](MARKUP.md)). Пример всего вместе —
страница `ItemsPage` в тестовом моде.

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
panel.style(b -> b.set(Styles.NINE_SLICE, NineSlice.of("minecraft:textures/gui/demo_background.png", 248, 166, 4).inFile(256, 256).scaled(2)));
```

В CSS: `nine-slice: url(minecraft:textures/gui/demo_background.png) 248 166 4 in 256 256 / 2` (рамка 1 или 4 числами; после `in` размер всего файла, если рамка лежит в его углу, как у ванильных картинок 256×256; после `/` масштаб).

**Страницы с переходом** (`Pages`): показывает одну страницу по номеру из сигнала, удобно вместе с `Tabs` и `SideNav`.
Эффекты `FADE`, `SLIDE` (по направлению смены номера), `SLIDE_UP`, `ZOOM`, `FLIP`; страница создаётся при первом показе
и по умолчанию сохраняется (`keepAlive`).

```java
Signal<Integer> tab = Signal.of(0);
new Tabs(tab, "Экран", "Вид");
new Pages(tab).effect(Pages.Effect.SLIDE).page(this::screen).page(this::view);
```

Готовый пример экрана настроек в современном стиле — раздел «Современный стиль» витрины (`ModernPage`).

## Всплывающие виджеты

Выпадающие списки, подсказки, радиальное меню и календарь живут во **всплывающем слое** `UiRoot`: рисуются поверх
содержимого и первыми получают мышь и клавиши. Escape закрывает верхнее окно, клик мимо — тоже.

- `root.showPopup(element, x, y)` / `closePopup` / `popups()` — окна (меню, списки);
- `root.showOverlay(element, x, y)` / `overlays()` — **пассивные оверлеи** (подсказки): рисуются поверх всех окон,
  но мышь проходит сквозь них, клавиши им не достаются, а клик мимо их не закрывает;
- `root.movePopup(element, x, y)` — передвинуть открытое окно или оверлей (подсказка за мышью);
- отпускание клавиши (`keyUp`) тоже сначала уходит верхнему окну — так радиальное меню выбирает пункт.

**Выпадающий список** (`Select<T>`): поле с текущим значением и стрелкой, список открывается под полем (если внизу нет
места — над ним) с раскрытием по вертикали. Длинный список прокручивается колесом (рисуются только видимые строки),
строка поиска отбирает варианты по подстроке без учёта регистра, у пунктов бывают иконки предметов. Клавиатура:
закрытый — Enter, пробел или Alt+↓ открывают, ↑/↓ меняют значение на месте, буква прыгает к варианту;
открытый — ↑/↓, PageUp/PageDown, Home/End, Enter выбирает, Escape закрывает.

```java
Signal<String> ore = Signal.of("minecraft:diamond");
new Select<>(ore, List.of("minecraft:diamond", "minecraft:emerald", "minecraft:gold_ingot"))
        .labels(id -> names.get(id))   // подпись варианта
        .icons(id -> id)               // иконка — id предмета (или null)
        .searchable(true)              // строка поиска
        .maxRows(7)                    // видимых строк, дальше — прокрутка
        .placeholder("Выберите руду")  // пока значение null
        .onChange(id -> log(id));
Select.of(difficulty, Difficulty.values()).labels(Difficulty::title);
```

Стили: `"select"` — поле (как `"field"`; открытый список — состояние `Select.OPEN`), `"dropdown"` — карточка списка
(фон, рамка, тень; подсвеченная строка — `ACCENT_COLOR`, текст — `TEXT_COLOR`).

**Подсказки** (`Tooltip`, `Tooltips`): появляются после задержки наведения (0.5 с), следуют за мышью и переворачиваются
у краёв экрана. Нажатие мыши прячет подсказку до следующего движения; видна одна подсказка — самого глубокого
наведённого элемента. Подсказка элемента пропадает, если элемент убрали из дерева.

```java
button.tooltip("Сохранить мир\nCtrl+S");                          // текст, строки через \n
button.tooltip(Tooltip.Kind.MODERN, "Карточка темы");             // свой вид
icon.tooltipMarkdown("**Алмаз** ![](item:minecraft:diamond)");    // Markdown
Tooltips.attach(slot, () -> buildCard(slot))                      // любой элемент, создаётся при показе
        .kind(Tooltip.Kind.MODERN).follow(false).delay(0.2f).maxWidth(220);
Tooltips.attach(label, "у любого UiElement");                     // подпись тоже станет «наводимой»
Tooltips.of(button).text("Новый текст");                          // открытая подсказка обновится сразу
Tooltips.detach(button);
Tooltips.setDefaultKind(Tooltip.Kind.MODERN);                     // вид и задержка по умолчанию
Tooltips.setDefaultDelay(0.3f);
```

Виды: `VANILLA` — как в игре: почти чёрный фон `0xF0100010`, фиолетовая кайма градиентом `0x505000FF → 0x5028007F`,
срезанные углы, белый текст с тенью, после первой строки (названия) лишние 2 пикселя (стиль `"tooltip-vanilla"`:
`BACKGROUND`, `BORDER_BRUSH`, `TEXT_COLOR`, `PADDING`); `MODERN` — карточка темы со скруглением, рамкой, тенью и
мягким появлением (стиль `"tooltip"` на токенах `SURFACE`, `BORDER`, `SHADOW`, `TEXT`, `CORNER`).
`follow(false)` ставит подсказку под элементом по центру (над ним, если внизу нет места). `show()` / `hide()` —
показать и спрятать из кода.

**Радиальное меню** (`RadialMenu`): секторы вокруг точки открытия. Сектор выбирается направлением мыши от центра
(и за пределами кольца), наведённый выдвигается и перекрашивается в акцент, в центре — его подпись. Пункт может
открывать вложенное меню (маленький треугольник у края сектора); правая кнопка, клик в центре, Backspace или Escape —
назад. Стрелки перебирают секторы, Enter выбирает, цифры 1–9 — пункт по номеру.

```java
RadialMenu wheel = new RadialMenu()
        .title("Действия")
        .item("Меч", "minecraft:diamond_sword", this::sword)
        .item("Факел", this::torch)                                  // без иконки — подпись в секторе
        .submenu("Еда", "minecraft:apple",
                RadialMenu.Item.of("Хлеб", this::bread).icon("minecraft:bread"),
                RadialMenu.Item.of("Торт", this::cake).enabled(false))
        .radius(30, 88);
wheel.show(root, mouseX, mouseY);                       // выбор кликом
wheel.showHeld(root, mouseX, mouseY, Keys.R);           // пока зажата R: отпускание выбирает
RadialMenu.bindKey(screenContent, Keys.R, this::wheel); // нажатие R над элементом (или его потомком в фокусе)
RadialMenu.bindRightClick(area, this::wheel);           // правая кнопка над элементом
```

Меню занимает весь экран (фон стиля — затемнение). Стиль `"radial-menu"`: `BACKGROUND` — затемнение,
`PopupStyles.WEDGE_COLOR` (`radial-wedge-color`) — секторы и центр, `ACCENT_COLOR` — наведённый сектор, `TEXT_COLOR` — подписи.
`bindKey` срабатывает, когда клавиша доходит до элемента: без фокуса клавиши получает `root.content()`.

**Назначение клавиш** (`KeyBindField`): клик или Enter — поле ждёт клавишу («> Нажмите клавишу <» с пульсацией),
следующая клавиша (любая, даже Tab) становится значением, Escape и потеря фокуса отменяют ожидание, правый клик
или Delete возвращают `defaultKey`. Значение — код GLFW в `Signal<Integer>`, `KeyBindField.NONE` — не задано.
`KeyBindField.keyName(code)` даёт название («Пробел», «Левый Shift», «F5», «Num 7»), `parseKey("space")` — обратно.
Стиль `"field keybind"`, ожидание — состояние `KeyBindField.LISTENING`.

**Даты** (`DatePicker`, `CalendarView`): поле `12.10.2026` открывает календарь на месяц во всплывающем окне.
Неделя с понедельника, выбранный день — заливка акцентом, сегодня — обводка, дни соседних месяцев приглушены,
дни вне `range(min, max)` недоступны. Месяц листается стрелками заголовка, колесом и PageUp/PageDown (сетка
коротко сдвигается), дни — стрелками, Enter выбирает. `CalendarView` можно поставить и прямо в интерфейс.

```java
Signal<LocalDate> date = Signal.of(LocalDate.now());
new DatePicker(date).range(LocalDate.now(), null).format(DateTimeFormatter.ofPattern("d.M.yy"));
new CalendarView(date).onPick(d -> log(d));
```

Стили: поле — `"select"`, всплывающий календарь — `"dropdown calendar"`, встроенный — `"calendar"`.
Все новые классы есть в `Theme.dark()`, `Theme.light()` и `ModernTheme` и построены на токенах.
В разметке — теги `<select>`, `<keybind>`, `<date-picker>`, `<calendar>` и атрибут `tooltip` (см. [MARKUP.md](MARKUP.md)).
Пример всего вместе — страница витрины `PopupsPage`.

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

## Темы в файлах, автосмена, доступность, звуки и адаптивность

### Тема в CSS (`markup.ThemeCss`)

`ThemeCss.export(theme, "dark")` пишет тему как CSS относительно базовой темы: в `:root` — все токены
(`--accent`, `--surface`, `--motion`...) и `--lumina-base: dark`, дальше — правила классов. У классов, которые есть
в базе, пишутся только значения, заданные числами и отличающиеся от базы; привязки к токенам (`bind`) остаются
в базе и после загрузки снова следуют за токенами. Классы, которых в базе нет, сохраняются целиком
(привязки — уже вычисленными значениями). Состояния — `:hover`, `:active`, `:focus`, `:disabled`, `:checked`,
модификаторы — `button.primary`. Чего CSS выразить не может (свои биты состояний, нестандартные значения),
попадает в файл комментарием.

```java
String css = ThemeCss.export(theme, "dark");
Theme back = ThemeCss.load(css);          // база по --lumina-base, затем правила
ThemeCss.save("my-theme", theme, "dark"); // <dataDirectory>/themes/my-theme.css
Theme mine = ThemeCss.loadFile("my-theme");
List<String> all = ThemeCss.savedThemes();
```

Базы: `dark`, `light`, `modern-dark`, `modern-light`, `high-contrast-dark`, `high-contrast-light`; свои —
`ThemeCss.registerBase(name, factory)`. `export(theme)` без имени базы сравнивает с тёмной темой и базу не пишет.
Ограничение: порядок вариантов внутри одного класса при загрузке берётся из CSS, поэтому редкие сочетания
состояний (hover+focus у вариантов, перекрывающих друг друга) могут разрешиться не так, как в исходной теме.
Файлы читаются и пишутся через `Platform.readFile/writeFile` внутри `Platform.dataDirectory()`
(по умолчанию `config/luminaui`); абсолютные пути и `..` отклоняются. Имена файлов лучше давать латиницей.

### Редактор темы (`widget.ThemeEditor`)

Цвета акцента, текста и поверхности, скругление и скорость анимаций; меняет тему своего корня сразу.
Кнопки «Экспорт» (файл `fileName(...)` + буфер обмена), «Импорт», «Из буфера», «Сброс».

```java
new ThemeEditor().fileName("my-theme.css").baseName("dark").onLoad(t -> ...);
```

### Плавная смена темы и автосмена (`theme.ThemeTransition`, `theme.ThemeSchedule`)

`ThemeTransition.to(root, target, 0.8f)` ставит корню рабочую копию и плавно переводит токены к целевой теме
(цвета смешиваются в OKLAB). При «меньше движения» смена мгновенная.

`ThemeSchedule` выбирает тему по времени суток (тики 0–24000) или по биому и переключает её плавно:

```java
ThemeSchedule.byWorldTime()
        .at(0f, Theme.light())
        .at(12500f, Theme.dark())
        .blend(1000f)              // смешивание перед каждой сменой
        .build()
        .transition(1.2f)
        .attach(root);             // Subscription; close() останавливает

ThemeSchedule.byWorldBiome()
        .when("minecraft:desert", desertTheme)
        .when(ThemeSchedule.contains("snow"), snowTheme)
        .otherwise(Theme.dark())
        .build()
        .decorate(t -> SoundTheme.SOFT.applyTo(t))  // поверх каждой выбранной темы
        .attach(root);
```

Время и биом даёт `platform.WorldInfo`: `timeOfDay()` берёт `Platform.dayTime()` (в игре — время мира),
а без мира — системные часы; `biome()` — `Platform.biomeId()`. Для тестов и примеров можно передать свои
источники: `ThemeSchedule.byTime(() -> slider.value().peek())`.

### Доступность (`a11y`)

Настройки у каждого корня: `root.accessibility()`.

```java
root.accessibility()
        .colorVision(ColorVision.DEUTERANOPIA)                          // коррекция (daltonize)
        .colorVision(ColorVision.PROTANOPIA, Accessibility.ColorMode.SIMULATE) // как видит человек
        .textScale(1.25f)          // все размеры шрифта × 1.25 (0.5–3)
        .reducedMotion(true)       // переходы стилей и тем — мгновенно
        .focusAlwaysVisible(true)  // заметная рамка фокуса поверх всего
        .narration(true);          // фокус озвучивается через Platform.narrate
```

Фильтр цвета накладывается на весь кадр на уровне `Canvas` (`setColorFilter`): заливки, градиенты, рамки,
тени и текст; текстуры и предметы не фильтруются. Свой фильтр — `extraFilter(ColorFilter)`,
например `ColorFilter.grayscale()`. Текст для озвучки — `UiElement.accessibleLabel()`; у кнопок, флажков
и надписей он берётся из текста, задать свой — `setAccessibleLabel(...)`.

Темы высокого контраста: `AccessibleThemes.highContrastDark()`, `highContrastLight()` или
`AccessibleThemes.highContrast(theme)` — чёрный/белый фон, текст не ниже 7:1, акцент не ниже 4.5:1,
рамки толщиной 2 и видимый контур фокуса. Ещё есть `filterColors(theme, filter)` и `reducedMotion(theme)`.

### Звуки интерфейса (`sound`)

Ключи стиля `HOVER_SOUND` и `CLICK_SOUND` (в CSS `hover-sound`, `click-sound`) со значением `UiSound`:

```css
.button { hover-sound: minecraft:block.note_block.hat 0.12 1.6; click-sound: minecraft:ui.button.click 0.25 1; }
.toggle { click-sound: none; }
```

Звук наведения играет только при входе указателя на включённый элемент и не чаще раза в 70 мс
(`root.setHoverSoundInterval(секунды)`). Без `click-sound` кнопки щёлкают как в игре.
Готовые наборы: `SoundTheme.VANILLA`, `SOFT`, `RETRO`, `NONE`; `SoundTheme.custom(name, hover, click)`,
`scaled(громкость)`. `SoundTheme.RETRO.applyTo(theme, "nav")` назначает звуки кнопкам, переключателям,
флажкам, пунктам меню и перечисленным классам.

### Адаптивность и масштаб (`layout`)

`UiRoot` сообщает класс ширины: `root.breakpoint()` (сигнал), `root.currentBreakpoint()` —
`COMPACT` (< 400), `MEDIUM` (< 640), `WIDE`; границы — `root.setBreakpoints(new Breakpoints(400, 640))`.

```java
ResponsivePanel.rowFrom(Breakpoint.MEDIUM, 8).add(a, b, c);   // столбец на узких, строка с MEDIUM
ResponsivePanel.columns(1, 2, 3, 8).add(cards);               // сетка 1/2/3 колонки
ResponsivePanel.columns(1, 2, 2, 6).byContainerWidth(true);   // по своей ширине, а не по ширине корня
new Panel("grid", new GridLayout(3, 8, 6));                   // просто равные колонки
```

`root.setScale(1.5f)` увеличивает весь интерфейс (0.25–4): раскладка считается в логических пикселях
(`root.width()` = физическая ширина / масштаб), координаты мыши делятся на масштаб, класс ширины тоже
пересчитывается.

### Хуки платформы

| Метод `Platform` | По умолчанию | Назначение |
|---|---|---|
| `dataDirectory()` | `config/luminaui` | папка для файлов тем |
| `readFile/writeFile/listFiles` | через `java.nio` | файлы внутри `dataDirectory()` |
| `dayTime()` | `-1` (нет мира) | время мира в тиках 0–24000 |
| `biomeId()` | `null` | id биома под игроком |
| `narrate(text)` | ничего | озвучка для экранного диктора |
| `isShiftDown()` | `false` | Shift+клик по слоту (быстрый перенос) |
| `playSound(id, volume, pitch)` | ничего | звуки интерфейса (уже был) |
| `pollGameData(GameState out)` | `false` (нет мира) | снимок игрока для `GameData`, на каждом тике клиента |
| `hudState()` | `0` | флаги F1 / F3 / чат / экран для правил видимости HUD, каждый кадр |
| `createTexture(key, w, h, argb)` | `null` | текстура из пикселей (кадры GIF, мини-карта) |
| `sampleMap(radius)` | `null` | цвета карты вокруг игрока для `MiniMap` |
| `worldBlocks()` | `null` | блоки клиентского мира по одному для `StructurePreview` и `WorldCapture` |
| `captureWorld(x0, y0, z0, x1, y1, z1)` | через `worldBlocks()` | коробка мира разом одной постройкой |
| `pollGamepad()` | `null` | состояние геймпада для навигации |

Пример всего вместе — страница `ThemesPage` в тестовом моде.

## HUD поверх игры и живые данные (`hud`, `game`)

### HUD (`hud.Hud`)

`Hud.get()` — общий HUD, который загрузчик рисует каждый кадр поверх интерфейса игры (Forge 1.20.1 —
оверлей `luminaui:hud` над всеми ванильными, NeoForge 1.21.1 — слой `luminaui:hud`) и тикает на каждом
тике клиента. Мышь и клавиатура в HUD не попадают. Пустой HUD ничего не стоит: кадр даже не начинается.

```java
Hud hud = Hud.get();
hud.add(new SegmentedBar().bind(GameData.health()).bindMax(GameData.maxHealth()), HudAnchor.BOTTOM_CENTER)
        .offset(0, 52)                       // внутрь экрана от привязанного края
        .layer(10)                           // больший слой — поверх
        .visibility(HudVisibility.GAMEPLAY); // прятать по F1, F3, в чате и под экранами
hud.toast("Готово", "Мир сохранён");
hud.feed().post("minecraft:diamond", "+1 алмаз", Toast.SUCCESS);
hud.remove(element);
```

Привязки `HudAnchor`: `TOP_LEFT`, `TOP_CENTER`, `TOP_RIGHT`, `CENTER_LEFT`, `CENTER`, `CENTER_RIGHT`,
`BOTTOM_LEFT`, `BOTTOM_CENTER`, `BOTTOM_RIGHT`. Смещение считается **внутрь экрана**, как `right: 4px`
в CSS: у правых привязок положительный `dx` сдвигает влево, у нижних положительный `dy` — вверх.
Элемент, поменявший размер, остаётся прижатым к своему краю.

Правила видимости `HudVisibility` проверяются каждый кадр по маске `Platform.hudState()`:

| Правило | Прячется |
|---|---|
| `ALWAYS` | никогда |
| `DEFAULT` (общее правило HUD) | по F1 и на экране отладки F3 |
| `GAMEPLAY` | по F1, F3, в чате и под любым открытым экраном |
| `.hideWithGui/hideInDebug/hideInChat/hideInScreens(bool)` | своя комбинация |
| `.when(() -> условие)` | дополнительное условие показа (дешёвое: читается каждый кадр) |

Своё общее правило — `hud.setDefaultVisibility(...)`; своя видимость элемента (`setVisible`) не трогается —
правило применяется к обёртке. `hud.setTheme(theme)`, `hud.setScale(1.25f)`, `hud.onClientTick(runnable)`.
Свой экземпляр `new Hud()` можно рисовать самому: `hud.render(canvas, w, h, partialTick)`.

### Виджеты HUD

| Виджет | Что это |
|---|---|
| `Toast` | уведомление: иконка-предмет, заголовок, текст до 3 строк, полоса прогресса, время жизни (полоска внизу). `Toast.info/success/warning/error(...)`, `.icon(id)`, `.timeout(сек)` (0 — до `dismiss()`), `.progress(0..1)` / `.bindProgress(signal)`, `.onClose(...)`. Въезжает с ближнего края, уходит и схлопывается; под мышью время не идёт |
| `ToastStack` | стопка тостов: `maxVisible(n)` (лишние — в очереди), `newestOnTop`, `slideFrom(±1)`, `dismissAll()`. Общая — `hud.toasts()` (справа сверху) |
| `NotificationFeed` | компактная лента строк с цветной меткой и иконкой, тают через `lifetime` секунд, не больше `capacity`. Общая — `hud.feed()` (слева посередине) |
| `Countdown` + `TimerDisplay` | обратный отсчёт / секундомер по реальным часам (`start/pause/toggle/reset/restart/add`, `onFinish`); виджет показывает «М:СС», с `tenthsBelow(5)` — десятые, последние `warnBelow` секунд краснеет и пульсирует |
| `SegmentedBar` | полоса из делений как сердца: `segments`, `segmentSize`, `color`, `lowBelow` (мигает), след урона |
| `BossBar` | широкая полоса с заголовком, градиентом `colors(from, to)` и делениями `notches(6/10/12/20)` |
| `RadialGauge` | круговая шкала на 270° (`sweep`) с числом или процентом (`percent(true)`) и подписью |

Все полосы принимают любой числовой сигнал (`bind(ReadableSignal<? extends Number>)`), отписываются,
уходя из дерева, и рисуются без выделения памяти в кадре (строки — только при смене значения).

Классы темы (во всех встроенных темах; для своей с нуля — `HudStyles.install(theme)`): `toast`, `hud-bar`,
`hud-feed`, `hud-timer`, `hud-panel` (полупрозрачная подложка для группы). Свои свойства:
`HudStyles.TRACK_COLOR` (пустая часть полос), `LOW_COLOR` (мало / время на исходе), `ROW_COLOR` (строки ленты).

### Данные игры (`game.GameData`)

Сигналы, которые загрузчик обновляет на каждом тике клиента через `Platform.pollGameData` (уведомляют только
при реальном изменении): `inWorld`, `health`, `maxHealth`, `healthFraction`, `absorption`, `food`,
`foodFraction`, `saturation`, `armor`, `air`, `maxAir`, `airFraction`, `xpLevel`, `xpProgress`, `x/y/z`
(точные), `blockX/Y/Z`, `position` (`"12 64 -30"`), `yaw`, `pitch`, `facing`, `biome`, `dimension`,
`dayTime` (0–23999), `clock` (`"06:00"`), `fps`, `selectedSlot`, `selectedItem`, `selectedCount`,
`inventory` (снимок `Inventory`: `itemId(i)`, `count(i)`, `total(id)`, `contains(id)`; новый объект только
при изменении). Без мира — значения как у нового игрока.

```java
Label.bound(GameData.position());
GameData.inventory().subscribe(inv -> diamonds.set(inv.total("minecraft:diamond")));
GameData.setSource(out -> { out.health = 7; return true; });   // свой источник (тесты, повтор)
```

Подписки на статические сигналы `GameData` живут, пока их не закрыть: в виджетах подписывайтесь в
`onAttach` и закрывайте в `onDetach` (так делают все виджеты библиотеки), а не через `map(...)` при каждом
создании страницы.

В разметке — `Bindings.gameData()` (или `bindings.withGameData()`): короткие имена из `GameData.named()`.

```html
<label>❤ {{health}} / {{maxHealth}} · еда {{food}} · {{pos}} · {{time}} · {{biome}}</label>
<progress value="{{healthFraction}}"/>
```

Пример всего вместе — страница `HudPage` и демо-HUD `HudDemo` в тестовом моде.

## Инструменты: размытие, обрезка, GIF, карты, частицы, инспектор, геймпад, настройки

Всё вместе — страница `ToolsPage` в тестовом моде.

### Размытие фона (`backdrop-blur`)

`Styles.BACKDROP_BLUR` (радиус в пикселях, анимируется) размывает то, что уже нарисовано под элементом;
поверх рисуются фон, рамка и дети, поэтому полупрозрачный фон даёт «стекло».

```java
card.style(b -> b
        .set(Styles.BACKDROP_BLUR, 8f)
        .set(Styles.BACKGROUND, Brush.solid(0x30FFFFFF))
        .set(Styles.RADIUS, Radii.all(12)));
```

```css
.glass { backdrop-blur: 8; }                 /* или */
.glass { backdrop-filter: blur(8px); }       /* none — выключить */
```

Холст: `Canvas.drawBackdropBlur(x, y, w, h, radii, radius)` и `supportsBackdropBlur()`. Бэкенд, который
умеет копировать кадр, переопределяет у `AbstractCanvas` хук `captureBackdrop(radius)` — размытая копия всего
кадра (UV (0,0) — левый верхний угол экрана); ядро вырезает из неё фигуру элемента. В игре кадр копируется
в свой буфер и размывается ванильной цепочкой `minecraft:shaders/post/blur.json` (в 1.21.1 радиус передаётся
в шейдер, в 1.20.1 он фиксирован в файле цепочки). Если копирования нет или что-то сломалось, рисуется матовая
подложка `Canvas.frostedFallback`: светлый вертикальный градиент, прозрачность которого растёт с радиусом,
и блик сверху. Ничего не ломается и в тестах, и на чужих бэкендах.

### Точная скруглённая обрезка (`clip-rounded`)

`Styles.CLIP_ROUNDED` вместе с `Styles.RADIUS` обрезает детей по скруглению, а не по прямоугольнику. Работает
у любого элемента, который обрезает детей (`setClipChildren(true)`), в том числе у `ScrollPanel`; у `Image`
то же свойство включает точную маску вместо нарезки текстуры полосами.

```java
new ScrollPanel(list).style(b -> b.set(Styles.RADIUS, Radii.all(14)).set(Styles.CLIP_ROUNDED, true));
```

Холст: `clipRoundRect(x, y, w, h, radii)`; по умолчанию — обычный `clipRect`, `supportsRoundClip()` говорит,
есть ли точная маска. `AbstractCanvas` делает её без трафарета: перед детьми снимает копию кадра
(`captureSnapshot(slot)`), а на парном `restore()` закрашивает из неё четыре угла за дугой веерами
треугольников. Одновременно открыто не больше `snapshotSlots()` скруглённых клипов (в игре — 8); глубже —
прямоугольная обрезка. Цена — одна копия кадра на каждый такой элемент в кадре.

### Анимированный GIF (`render.Gif`, `widget.AnimatedImage`)

`Images.fromBytes/fromFile/fromResource` для GIF из нескольких кадров возвращают `AnimatedTexture`: кадры
читает `javax.imageio` (с учётом способов смены кадра), каждый кадр загружается через
`Platform.createTexture` под ключом `<ключ>/<номер>`. Задержки берутся из файла (0–10 мс считаются как 100 мс,
как в браузерах), число повторов — из блока NETSCAPE (нет блока — один проход). Обычный `Image` проигрывает
такую текстуру сам. `AnimatedImage` даёт управление:

```java
AnimatedImage gif = AnimatedImage.resource("mymod:textures/gui/loading.gif")
        .size(48, 48)
        .loop(AnimatedImage.Loop.FOREVER)   // AS_FILE — как в файле, ONCE — один раз
        .speed(1.5f)
        .onFrame(i -> ...);
gif.pause(); gif.seek(0); gif.play();
```

`Gif.encode(frames, delayMs, loopCount)` собирает GIF из `Images.Pixels` (удобно для тестов и картинок,
нарисованных кодом), `Gif.decode(bytes)` отдаёт кадры без загрузки текстур. Ограничения: не больше 512 кадров
и разумного общего числа пикселей.

**Видео не поддерживается.** В ядре нет декодера видео (`javax.imageio` его не умеет), а FFmpeg/JavaCV —
это десятки мегабайт нативных библиотек под каждую платформу, что для библиотеки интерфейса слишком тяжело.
Короткую анимацию лучше сохранить GIF-ом или набором кадров.

### Мини-карта и карта мира (`widget.MiniMap`, `widget.WorldMap`, пакет `map`)

```java
MiniMap map = new MiniMap()
        .source(MapSource.platform().or(new DemoMap(42)))  // в мире — карта игры, без мира — демо
        .shape(MiniMap.Shape.ROUND)     // или SQUARE
        .rotate(true)                   // по взгляду игрока; false — север вверху
        .zoom(1.5f).zoomRange(0.5f, 6f) // пикселей на блок; колесо мыши меняет масштаб
        .showNorth(true).showCoordinates(true)
        .marker("home", 0, 0, 0xFFFFE08A, "Дом");
```

Источник — `MapSource`: `MapSample sample(int radius)` с сеткой ARGB-цветов, положением и поворотом игрока.
`MapSource.platform()` берёт `Platform.sampleMap(radius)`: загрузчик читает цвет верхнего блока
(`BlockState.getMapColor`) через `map.MapGridSampler` — сетка кэшируется, сдвигается только когда игрок отошёл,
за одно обновление читает не больше `budget` столбцов и не чаще `interval`; склоны затеняются, как на ванильной
карте. Свой источник (сервер, план базы) — любая реализация `MapSource`. Текстура карты загружается через
`createTexture` не чаще `uploadInterval(...)` и только когда сетка изменилась; без текстур карта рисуется
клетками. Метки за краем прижимаются к краю и теряют подпись.

`WorldMap(provider, centerX, centerZ, size)` — статичная карта области по `MapColorProvider.colorAt(x, z)`:
цвета строятся один раз (`refresh()` — заново), перетаскивание двигает вид, колесо меняет масштаб.

### Частицы за меню (`Backdrop.Kind.PARTICLES`)

```java
new Backdrop(Backdrop.Kind.PARTICLES)
        .particles(Backdrop.ParticleStyle.SNOW)  // SPARKS, SNOW, DUST
        .particleCount(90)
        .repel(60f, 900f)                        // радиус и сила отталкивания от курсора
        .speed(1f);
```

Искры поднимаются и гаснут (рисуются со сложением цветов), снег падает и покачивается, пыль медленно плавает.
Курсор расталкивает частицы, потом они плавно возвращаются к своей скорости. Свои цвета — `colors(...)`.

### Инспектор интерфейса (`root.inspector()`)

```java
root.inspector().enabled(true);         // теперь F8 открывает и закрывает
root.inspector().toggleKey(Keys.F12);   // другая клавиша
root.inspector().open();
```

Выключен по умолчанию. Открытый инспектор перехватывает мышь: наведение подсвечивает элемент (внешние
отступы, границы, внутренние отступы) с подписью `Класс.классы-стиля#id`, клик выбирает, правый клик снимает
выбор. Панель сбоку показывает дерево элементов (клик по строке выбирает, колесо прокручивает) и сведения
о выбранном: класс Java, место и размер, padding/margin, флаги и вычисленный стиль (только отличия от значений
по умолчанию). Стрелки ходят по дереву (вверх — к родителю), Esc закрывает, кнопки в заголовке закрывают
панель и переносят её на другую сторону.

### Геймпад (`root.gamepad()`, `UiRoot.focusDirection`)

Корень каждый кадр опрашивает `Platform.pollGamepad()` (в игре — первый геймпад GLFW):

| Кнопка | Действие |
|---|---|
| крестовина, левый стик | фокус в сторону (`focusDirection`), с автоповтором при удержании |
| A | нажать: Enter элементу в фокусе, а если он его не принял — клик по центру |
| B | Escape (закрывает всплывающее окно), иначе `onBack` — экран загрузчика закрывается |
| LB / RB | соседняя вкладка у ближайших `Tabs` / `SideNav` (`TabSwitchable`) |

`focusDirection(NavDirection)` работает и без геймпада: выбирает ближайший фокусируемый элемент в направлении
(расстояние по направлению плюс штраф за сдвиг вбок), без фокуса — левый верхний. Фокус, полученный с клавиатуры
или геймпада, прокручивает `ScrollPanel` к элементу (`UiElement.revealChild`). Настройки: `enabled`,
`deadzone`, `repeat(delay, rate)`, `onBack(...)`.

### Экран настроек из описания (`config.ConfigSpec`, `config.ConfigScreen`)

```java
ConfigSpec spec = ConfigSpec.builder("mymod/client.toml")
        .header("Настройки MyMod")
        .category("hud", "Интерфейс").icon("minecraft:compass")
        .bool("showMinimap", "Мини-карта", true).comment("Показывать в углу экрана")
        .integer("minimapSize", "Размер", 96, 48, 192).step(8)
        .number("opacity", "Прозрачность", 0.85f, 0.2f, 1f).step(0.05f)
        .key("zoomKey", "Приблизить", Keys.Z)
        .category("look", "Вид")
        .choice("quality", "Качество", Quality.MEDIUM, Quality.values()).labels(q -> ...)
        .color("accent", "Акцент", 0xFF5B8CFF).alpha(false)
        .string("greeting", "Приветствие", "Привет!").maxLength(24)
        .build();
spec.load();                                   // Platform.readFile
boolean on = spec.bool("showMinimap").peek();  // значения — сигналы
new ConfigScreen(spec).title("MyMod").autoSave(true);
```

Без рефлексии: каждое значение описано явно и хранится в `Signal`. `set` приводит значение к допустимому
(диапазон, длина, вариант, без альфы). `ConfigScreen` строит боковую навигацию по разделам и строки:
`Toggle`, `SliderField`, `TextField`, `Select`, `ColorField`, `KeyBindField`, у каждой — кнопка сброса к значению
по умолчанию; поиск ищет по названиям, ключам, комментариям и разделам; «Сбросить всё», «Сохранить» и
автосохранение. Файл — простой TOML: разделы `[категория]`, `ключ = значение`, комментарии с подписью и
диапазоном; неизвестные и испорченные строки при чтении пропускаются.
