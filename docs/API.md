# Lumina UI: руководство разработчика

Это руководство для авторов модов, которые хотят строить интерфейсы на Lumina UI: как подключить библиотеку,
открыть первый экран, из чего состоит API и как его расширять. English version: [API.en.md](API.en.md).

Подробные справочники по отдельным темам:

- [STYLING.md](STYLING.md) — цвета, градиенты, темы, анимации, эффекты, Markdown, все виджеты, HUD, инвентарь, 3D;
- [MARKUP.md](MARKUP.md) — интерфейс на HTML и CSS с привязкой логики;
- [ARCHITECTURE.md](ARCHITECTURE.md) — как библиотека устроена внутри.

## Содержание

1. [Поддерживаемые версии](#поддерживаемые-версии)
2. [Подключение к своему моду](#подключение-к-своему-моду)
3. [Первый экран](#первый-экран)
4. [Основные понятия](#основные-понятия)
5. [Раскладка](#раскладка)
6. [Стили и темы](#стили-и-темы)
7. [Данные и сигналы](#данные-и-сигналы)
8. [События и ввод](#события-и-ввод)
9. [Анимации](#анимации)
10. [Каталог виджетов](#каталог-виджетов)
11. [HTML и CSS](#html-и-css)
12. [Языки](#языки)
13. [HUD поверх игры](#hud-поверх-игры)
14. [Окна-контейнеры с сервером](#окна-контейнеры-с-сервером)
15. [Экран настроек](#экран-настроек)
16. [Расширение библиотеки](#расширение-библиотеки)
17. [Правила и советы](#правила-и-советы)

## Поддерживаемые версии

| Загрузчик | Minecraft | Java | Пакет загрузочного слоя |
|---|---|---|---|
| Forge 47.x | 1.20.1 | 17 | `io.github.ssbaxys.lumina.forge` |
| NeoForge 21.1.x | 1.21.1 | 21 | `io.github.ssbaxys.lumina.neoforge` |

Почти весь API (`io.github.ssbaxys.lumina.*`: виджеты, раскладка, стили, анимации, разметка, HUD) одинаков на обеих
версиях: он живёт в ядре на чистой Java и не зависит от классов Minecraft. Код интерфейса можно держать в общем
модуле и собирать под обе версии без изменений. От загрузчика зависят только несколько классов: экран
`LuminaScreen` и окна контейнеров `LuminaMenuType` / `LuminaMenuScreen`.

Библиотека ставится и на клиент, и на сервер. Серверу она нужна только для окон-контейнеров
(`LuminaMenu`); всё остальное работает на клиенте, и на сервере клиентские классы не загружаются.

## Подключение к своему моду

### Gradle

Jar библиотеки берётся из Maven-репозитория Modrinth. Вместо `<версия>` подставьте номер версии со страницы файла
на Modrinth (отдельный файл для каждого загрузчика).

```groovy
repositories {
    exclusiveContent {
        forRepository { maven { url = "https://api.modrinth.com/maven" } }
        filter { includeGroup "maven.modrinth" }
    }
}

dependencies {
    // NeoForge 1.21.1 (ModDevGradle)
    implementation "maven.modrinth:lumina-ui:<версия>"

    // Forge 1.20.1 (ModDevGradle legacyforge)
    modImplementation "maven.modrinth:lumina-ui:<версия>"

    // Forge 1.20.1 (ForgeGradle)
    implementation fg.deobf("maven.modrinth:lumina-ui:<версия>")
}
```

Можно и без репозитория: положите jar в папку `libs/` и подключите `files("libs/luminaui-....jar")` той же
конфигурацией.

Если общий код интерфейса лежит в отдельном модуле без Minecraft, ему достаточно `compileOnly` на тот же jar:
классы ядра в нём одинаковые для обоих загрузчиков.

### Зависимость в описании мода

Forge 1.20.1, `META-INF/mods.toml`:

```toml
[[dependencies.mymod]]
modId = "luminaui"
mandatory = true
versionRange = "[0.1.0,)"
ordering = "AFTER"
side = "BOTH"
```

NeoForge 1.21.1, `META-INF/neoforge.mods.toml`:

```toml
[[dependencies.mymod]]
modId = "luminaui"
type = "required"
versionRange = "[0.1.0,)"
ordering = "AFTER"
side = "BOTH"
```

Если ваш мод использует Lumina только на клиенте, можно поставить `side = "CLIENT"`.

### Инициализация

Ничего вызывать не нужно. Мод `luminaui` сам вызывает `Lumina.init(...)` при запуске клиента, регистрирует шейдеры,
HUD и перезагрузку переводов. Проверить готовность можно через `Lumina.isInitialized()`.

## Первый экран

```java
import io.github.ssbaxys.lumina.forge.LuminaScreen;      // на NeoForge: io.github.ssbaxys.lumina.neoforge.LuminaScreen
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.style.ModernTheme;
import io.github.ssbaxys.lumina.widget.*;

public final class HelloScreen {
    public static void open() {
        Signal<Integer> clicks = Signal.of(0);
        Signal<Boolean> music = Signal.of(true);
        Signal<Float> volume = Signal.of(60f);

        Minecraft.getInstance().setScreen(new LuminaScreen(Component.literal("Hello"), screen -> {
            Panel window = Panel.column().gap(10).width(Dim.px(260)).add(
                    new Label("Lumina UI").scale(1.5f),
                    Label.bound(clicks.map(c -> "Нажатий: " + c)),
                    new Checkbox("Музыка", music),
                    new Slider(volume, 0, 100),
                    new Button("Нажми", () -> clicks.update(c -> c + 1)),
                    new Button("Закрыть", screen::onClose));
            // слой на весь экран, окно по центру
            Panel root = Panel.box(StackLayout.centered()).fill().add(window);
            return new UiRoot(root, ModernTheme.dark());
        }));
    }
}
```

- `LuminaScreen` — обычный `Screen` Minecraft, внутри которого живёт `UiRoot`. Весь ввод идёт в интерфейс, а то,
  что интерфейс не обработал (например, Esc), — в стандартную логику экрана.
- Фабрика получает сам экран: удобно для кнопки «Закрыть» (`screen::onClose`).
- `.parent(previousScreen)` — на какой экран вернуться при закрытии; `.pauseGame(true)` — ставить игру на паузу
  в одиночной игре.
- Содержимое корня ставится в левый верхний угол по своему размеру. Чтобы растянуть его на весь экран, вызовите
  `.fill()`; чтобы поставить окно по центру — оберните его в `Panel.box(StackLayout.centered()).fill()`, как выше.

Готовое маленькое демо самой библиотеки открывается командой `/lumina demo`, полная витрина возможностей —
тестовый мод `luminatest` (клавиша K или `/luminatest`), его исходники лежат в `testmod-common/`.

## Основные понятия

| Класс | Роль |
|---|---|
| `UiElement` | Узел дерева: дети, раскладка, стиль, состояния (hover, pressed, focused, disabled, checked), события, анимации. |
| `Widget<S>` | Основа всех виджетов: типобезопасный fluent-API (`.width(...)`, `.style(...)`, `.on(...)`, `.tooltip(...)`), каждый вызов возвращает сам виджет. |
| `UiRoot` | Корень дерева: тема, аниматор, фокус и Tab-навигация, всплывающий слой (меню, подсказки), доступность, геймпад, инспектор. |
| `Theme` | Стили по классам виджетов и токены (акцент, поверхность, скругление, скорость анимаций). |
| `Signal<T>` | Наблюдаемое значение: виджеты привязываются к нему и обновляются сами. |
| `Canvas` | Интерфейс рисования для своих виджетов: прямоугольники со скруглением, тени, градиенты, текст, текстуры, предметы, сущности. |

Общие методы `Widget`:

```java
new Button("OK")
        .id("ok")                          // потом root.content().findById("ok")
        .size(120, 20)                     // или .width(Dim.px(120)).height(Dim.percent(0.5f)), .fill()
        .minSize(80, 20).maxSize(200, 40)
        .grow(1).shrink(0)                 // доля свободного места во flex
        .margin(Insets.all(4))
        .alignSelf(Align.END)
        .absolute(10, 10)                  // вне потока раскладки, от левого верхнего угла родителя
        .styleClass("button primary")      // классы темы через пробел
        .style(s -> s.set(Styles.RADIUS, Radii.all(12)))
        .tooltip("Подсказка")
        .enabled(false).visible(true)
        .on(PointerEvent.ENTER, e -> {})
        .with(b -> b.setFocusable(true));  // любая настройка без разрыва цепочки
```

## Раскладка

Раскладка двухпроходная (измерение, затем расстановка) и пересчитывается только после изменений.

- `Panel.column()` / `Panel.row()` — flexbox: `.gap(px)`, `.justify(Justify.SPACE_BETWEEN)`, `.align(Align.CENTER)`.
  `Justify`: `START`, `CENTER`, `END`, `SPACE_BETWEEN`, `SPACE_AROUND`, `SPACE_EVENLY`.
  `Align`: `START`, `CENTER`, `END`, `STRETCH`.
- `Panel.box(layout)` — прозрачный контейнер без стиля темы, только для группировки.
- `new Panel("card", StackLayout.centered())` — дети лежат друг над другом (фон, содержимое, эффекты).
- `new GridLayout(колонки, отступ)` — сетка.
- `ResponsivePanel` и `root.breakpoint()` — раскладка в зависимости от ширины экрана.
- Размеры: `Dim.AUTO` (по содержимому), `Dim.px(n)`, `Dim.percent(0..1)`, `Dim.FILL`.
- `ScrollPanel(content)` — вертикальная прокрутка с плавной инерцией; невидимые строки не рисуются.

Свою раскладку можно написать, реализовав интерфейс `Layout` (два метода: `measure` и `arrange`).

## Стили и темы

Стиль — набор типизированных свойств (`StyleKey`) плюс варианты для состояний. Тема хранит стили по классам,
а inline-стиль виджета накладывается поверх.

```java
button.style(s -> s
        .set(Styles.BACKGROUND, Brush.vertical(0xFF3A7BFF, 0xFF2456C8))
        .set(Styles.RADIUS, Radii.all(8))
        .set(Styles.SHADOW, Shadow.of(2, 8, 0x80000000))
        .hovered(h -> h.set(Styles.SCALE, 1.05f).set(Styles.GLOW, Shadow.of(0, 10, 0x805B8CFF)))
        .pressed(p -> p.set(Styles.SCALE, 0.97f))
        .set(Styles.TRANSITION, Transition.of(0.2f, Easing.OUT_CUBIC)));
```

Темы: `Theme.dark()`, `Theme.light()`, `ModernTheme.dark(акцент)`, `ModernTheme.light()`,
`AccessibleThemes.highContrastDark()`. Тема передаётся в `new UiRoot(content, theme)` или меняется на лету через
`root.setTheme(...)` (плавно — `ThemeTransition`).

Токены перекрашивают весь интерфейс одной строкой:

```java
Theme theme = ModernTheme.dark();
theme.set(Tokens.ACCENT, 0xFFF0506E);   // акцент
theme.set(Tokens.CORNER, 4f);           // скругления
theme.set(Tokens.MOTION, 0f);           // выключить анимации переходов
theme.edit("button", b -> b.set(Styles.BORDER_WIDTH, 1f));   // дополнить стиль класса
```

Всё о цветах (RGB, HSV, HSL, OKLCH), градиентах, режимах смешивания, размытии фона, NineSlice и свойствах стиля —
в [STYLING.md](STYLING.md).

## Данные и сигналы

```java
Signal<Integer> coins = Signal.of(120);
coins.set(150);
coins.update(c -> c + 10);
int now = coins.peek();                                  // прочитать без подписки

ReadableSignal<String> text = coins.map(c -> c + " монет");      // вычисляемое значение
Computed<Boolean> rich = new Computed<>(() -> coins.get() > 1000);

Label.bound(text);                  // надпись обновляется сама
new Toggle(Signal.of(true));        // поля ввода пишут в сигнал и читают из него
Subscription sub = coins.subscribe(c -> save(c));  // sub.close() — отписаться
```

Виджеты подписываются на сигналы, только пока находятся в дереве, и отписываются сами при удалении — утечек нет.
Сигналы однопоточные: меняйте их из потока рендера (например, через `Minecraft.getInstance().execute(...)`).

Для больших данных: `VirtualList<T>` (десятки тысяч строк), `DataView<T>` (поиск, фильтры, сортировка, страницы),
`Table<T>`, `TreeView`.

## События и ввод

События типизированы, идут в две фазы: захват (от корня к цели) и всплытие (от цели к корню).

```java
panel.on(PointerEvent.CLICK, e -> select(e.localX(), e.localY()));
panel.on(ScrollEvent.SCROLL, e -> zoom(e));
field.on(KeyEvent.PRESS, e -> { if (e.key() == Keys.ENTER) { submit(); e.consume(); } });
root.content().onCapture(KeyEvent.PRESS, e -> {});   // перехватить раньше детей
```

Типы: `PointerEvent` (`PRESS`, `RELEASE`, `MOVE`, `DRAG`, `CLICK`, `ENTER`, `LEAVE`), `ScrollEvent.SCROLL`,
`KeyEvent` (`PRESS`, `RELEASE`), `CharEvent.TYPED`, `FocusEvent` (`GAINED`, `LOST`).
`e.stopPropagation()` останавливает всплытие, `e.consume()` помечает событие обработанным.
Свои события: `EventType.create("my-event", true)` и `root.dispatch(target, event)`.

Фокус: `setFocusable(true)`, `root.setFocus(el)`, Tab и Shift+Tab переключают фокус, стрелки и геймпад —
через `root.gamepad()` и `UiRoot.focusDirection(...)`.

## Анимации

Любое свойство стиля можно анимировать; анимации пишут в отдельный слой, поэтому hover и pressed под ними
продолжают работать, а при удалении элемента анимации останавливаются сами.

```java
box.animate(Styles.TRANSLATE_X).keyframe(0, 0f).keyframe(0.5f, 160f, Easing.OUT_BOUNCE).keyframe(1, 0f)
        .duration(2.4f).loop().play();

element.play(Motion.sequence(
        Motion.call(this::sound),
        Motion.parallel(a.animate(Styles.OPACITY).to(1f).motion(), b.animate(Styles.SCALE).to(1.2f).motion()),
        Motion.delay(0.2f),
        VanillaFx.shake(element, 4f, 0.4f)));

AnimatedFloat x = new AnimatedFloat(root.animator(), 0).spring(Spring.BOUNCY);  // для своих виджетов
x.animateTo(100);
```

23 кривые (`Easing`), `cubicBezier`, `steps`, пружины. Ванильные эффекты (огонь, взрывы, TNT, снег, частицы) —
слой `FxLayer` и `VanillaFx`, см. [STYLING.md](STYLING.md).

## Каталог виджетов

| Группа | Виджеты |
|---|---|
| Базовые | `Panel`, `Label`, `Button`, `ImageButton`, `Icon`, `Image`, `AnimatedImage` (GIF), `FancyText` |
| Ввод | `TextField`, `NumberField`, `ColorField`, `Slider`, `SliderField`, `Toggle`, `Checkbox`, `RadioGroup`, `Select`, `KeyBindField`, `DatePicker`, `CalendarView`, `ColorPicker` |
| Формы | `Form`, `FormField`, `Validator`, `Wizard`, `Stepper` |
| Навигация | `Tabs`, `SideNav`, `Pages`, `Paginator`, `Collapsible`, `ScrollPanel` |
| Всплывающие | `ContextMenu`, `Tooltip` / `Tooltips`, `RadialMenu`, `Window` (перетаскивание, изменение размера, модальность) |
| Данные | `Table`, `TreeView`, `VirtualList`, `DataView`, `Chart` (линии, области, столбцы, круговые) |
| Индикаторы | `ProgressBar`, `RingProgress`, `Spinner`, `Skeleton` |
| Текст | `Markdown`, `Book`, `SyntaxHighlighter` |
| Игра | `Slot`, `SlotGrid`, `SlotGroup`, `InventoryPanel`, `EntityView`, `ItemView`, `BlockMap`, `BlockModel`, `StructurePreview`, `MiniMap`, `WorldMap` |
| Оформление | `Backdrop` (живые обои), `FxLayer` (эффекты), `ThemeEditor`, `LanguageButton` |
| HUD | `Hud`, `SegmentedBar`, `RadialGauge`, `BossBar`, `TimerDisplay`, `Toast`, `NotificationFeed` |

Примеры кода для каждого — в [STYLING.md](STYLING.md) и на страницах витрины в `testmod-common/`.

## HTML и CSS

Экран можно описать разметкой и стилями, а логику подключить в разметке или из Java. Разметка один раз
превращается в обычные виджеты, поэтому в кадре ничего не стоит.

```java
Signal<Float> volume = Signal.of(60f);
UiElement screen = MarkupView.resource("mymod:ui/settings.html", new Bindings()
                .signal("volume", volume)
                .handler("save", this::save))
        .watch(1f);                                // горячая перезагрузка (F3+T)
```

```html
<link rel="stylesheet" href="mymod:ui/settings.css">
<column class="settings">
  <h2>Настройки</h2>
  <row><label>Громкость</label><slider bind="volume" min="0" max="100"/><label>{{volume|int}}%</label></row>
  <button class="primary" on-click="save">Сохранить</button>
</column>
```

Файлы лежат в `assets/<мод>/...` и могут переопределяться ресурспаками. Все теги, свойства CSS, действия и
проверки форм — в [MARKUP.md](MARKUP.md).

## Языки

Язык интерфейса — сигнал, поэтому открытый экран переводится сразу, без пересоздания.

```java
// lang-файлы мода (assets/mymod/lang/en_us.json, ru_ru.json) подхватываются сами; можно и из кода:
I18n.translations("ru_ru").add("mymod.clicks", "Нажатий: %s");

Label.tr("mymod.clicks", clicks);       // аргумент-сигнал подставляется и обновляется сам
Button.tr("mymod.close").onPress(onClose);
new LanguageButton();                   // готовый переключатель языка

I18n.setLanguage("ru_ru");              // свой язык независимо от игры (выбор сохраняется)
I18n.followGameLanguage();              // снова как в игре (по умолчанию)
```

Порядок поиска перевода: таблицы из кода, свои источники (`I18n.addSource`), lang-файлы игры; если ключа нет,
берётся `en_us`, затем сам ключ.

## HUD поверх игры

```java
Hud hud = Hud.get();
hud.add(new SegmentedBar().bind(GameData.health()).bindMax(GameData.maxHealth()), HudAnchor.BOTTOM_CENTER)
        .offset(0, 52)
        .layer(10)
        .visibility(HudVisibility.GAMEPLAY);   // прятать по F1, F3, в чате и под экранами
hud.toast("Готово", "Мир сохранён");
hud.feed().post("minecraft:diamond", "+1 алмаз", Toast.SUCCESS);
```

`GameData` даёт живые сигналы игры: здоровье, голод, броня, воздух, опыт и другие. Пустой HUD ничего не стоит.
Подробно — раздел «HUD поверх игры» в [STYLING.md](STYLING.md).

## Окна-контейнеры с сервером

Свой сундук, печь или машина с окном на Lumina, которым владеет сервер, как ванильным сундуком.

```java
// общий код
static final MenuSpec SPEC = MenuSpec.builder()
        .container("chest", 27).playerInventory()
        .intValue("progress").boolValue("burning").build();
static final LuminaMenuType CHEST = new LuminaMenuType(SPEC);
MENUS.register("chest", CHEST::create);              // DeferredRegister<MenuType<?>>

// сервер
CHEST.open(player, Component.literal("Сундук"), container, index -> progress, (p, action, value) -> sort());

// клиент: Forge — в FMLClientSetupEvent.enqueueWork, NeoForge — в RegisterMenuScreensEvent
LuminaMenuScreen.register(CHEST, menu -> new UiRoot(InventoryPanel.chest(menu.title(),
        menu.inventory("chest"), menu.playerInventory(), Slot.Look.MODERN), ModernTheme.dark()));
```

Слоты синхронизирует игра, числа идут через `DataSlot`, свои кнопки — `menu.sendAction(id, число)` без своего
сетевого канала. Подробно — в [STYLING.md](STYLING.md), раздел «Окна-контейнеры с сервером».

## Экран настроек

```java
ConfigSpec spec = ConfigSpec.builder("mymod/client.toml")
        .category("hud", "Интерфейс").icon("minecraft:compass")
        .bool("showMinimap", "Мини-карта", true).comment("Показывать в углу экрана")
        .integer("minimapSize", "Размер", 96, 48, 192).step(8)
        .color("accent", "Акцент", 0xFF5B8CFF)
        .key("zoomKey", "Приблизить", Keys.Z)
        .build();
spec.load();
boolean on = spec.bool("showMinimap").peek();       // значения — сигналы
UiElement screen = new ConfigScreen(spec).title("MyMod").autoSave(true);
```

Экран сам строит навигацию по разделам, поиск, сброс к значениям по умолчанию и сохраняет файл в
`config/mymod/client.toml`.

## Расширение библиотеки

Всё расширяется обычным кодом, без рефлексии и аннотаций.

### Свой виджет

```java
public class StripeBar extends Widget<StripeBar> {
    public static final StyleKey<Integer> STRIPE = StyleKey.builder("stripe-color", Integer.class)
            .defaultValue(0xFFFFFFFF).animated(Interpolator.COLOR).register();

    private float value;

    public StripeBar() {
        super("stripe-bar");                     // класс темы
        setFocusable(true);
    }

    public StripeBar value(float v) {
        value = v;
        return this;
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        setContentSize(120, 8);                  // естественный размер без padding
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        canvas.fillRoundRect(0, 0, width() * value, height(), Radii.all(4),
                Brush.solid(style.get(STRIPE)));
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.CLICK) {
            value(((PointerEvent) event).localX() / width());
            event.consume();
        }
    }
}
```

Полезные точки расширения `UiElement`: `onAttach` / `onDetach` (подписки на сигналы и анимации),
`onLayout` (после изменения размера), `onStateChanged`, `paintBackground`, `paintForeground`, `paintOverlay`.
Свои состояния — биты начиная с `State.FIRST_CUSTOM`.

### Остальное

| Что | Как |
|---|---|
| Свойство стиля | `StyleKey.builder(name, type).defaultValue(...).inherited().affectsLayout().animated(...).register()` |
| Токен темы | `ThemeVar.of("brand", Integer.class, 0xFF00FF00)` |
| Событие | `EventType.create("name", bubbles)` |
| Раскладка | `implements Layout` |
| Тег разметки | `Markup.tag("card", (node, ctx) -> ...)` |
| Свойство CSS | `CssProperties.register(...)` |
| Эффект | `FxRegistry.register("mymod:effect", ...)` |
| Источник переводов | `I18n.addSource((lang, key) -> ...)` |

## Правила и советы

- **Поток.** Интерфейс однопоточный: создавайте виджеты и меняйте сигналы в потоке рендера клиента.
- **Производительность.** Раскладка и стили кэшируются, невидимые поддеревья не обходятся, аниматор ничего не
  делает в покое. Не пересоздавайте дерево каждый кадр — меняйте сигналы.
- **Элементы за границами.** Если виджет рисует за своими границами (свечение, тень), вызовите
  `setOverflowsBounds(true)`, иначе он может отсекаться при прокрутке.
- **Отладка.** Инспектор интерфейса (`root.inspector()`, F8 в витрине) показывает дерево, размеры и стили.
- **Доступность.** `root.accessibility()`: коррекция цвета для дальтонизма, масштаб текста, уменьшение движения,
  озвучка фокуса.
- **Совместимость версий.** Код на `io.github.ssbaxys.lumina.*` (кроме пакетов `forge` и `neoforge`) переносится
  между Forge 1.20.1 и NeoForge 1.21.1 без изменений.
