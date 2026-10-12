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
