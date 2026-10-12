# HTML и CSS в Lumina UI

Интерфейс можно описать HTML-подобной разметкой и CSS, а логику подключить прямо в разметке или обработчиками из Java.
Разметка один раз превращается в обычные виджеты и стили Lumina, поэтому в кадре она ничего не стоит.
Рефлексии нет: теги, свойства CSS и действия — это таблицы зарегистрированных функций.

Живой пример — раздел «HTML и CSS» в витрине (`luminatest`), файлы
`testmod-common/src/main/resources/assets/luminatest/ui/markup_demo.html` и `markup_demo.css`.

## Быстрый старт

```java
Signal<Float> volume = Signal.of(60f);
UiElement screen = MarkupView.resource("mymod:ui/settings.html", new Bindings()
                .signal("volume", volume)          // данные из Java
                .handler("save", this::save))      // логика из Java
        .watch(1f);                                // горячая перезагрузка
```

```html
<!-- assets/mymod/ui/settings.html -->
<link rel="stylesheet" href="mymod:ui/settings.css">
<state name="music" value="true"/>

<column class="settings">
  <h2>Настройки</h2>
  <row><label>Музыка</label><toggle bind="music"/></row>
  <row enabled="music"><label>Громкость</label><slider bind="volume" min="0" max="100" step="1"/><label>{{volume|int}}%</label></row>
  <button class="primary" on-click="save; sound:minecraft:ui.button.click">Сохранить</button>
</column>
```

```css
/* assets/mymod/ui/settings.css */
:root { --accent: #ff8800; --pad: 8px; }
.settings { gap: 6px; padding: var(--pad); background: linear-gradient(to bottom, #202030, #101018); border-radius: 8px; }
button.primary:hover { transform: scale(1.06); glow: 6px var(--accent); transition: transform .25s out-back, glow .2s; }
```

Без Java тоже можно: `Markup.build(html, new Bindings()).root()` возвращает готовый элемент.

## Теги

| Тег | Что создаёт | Атрибуты |
|---|---|---|
| `div` `column` `section` `body` `main` `nav` `header` `footer` | колонка | |
| `row` | строка (выравнивание по центру) | |
| `stack` | слои друг над другом | |
| `panel` | панель с фоном темы | |
| `scroll` | прокручиваемая колонка | |
| `label` `span` `text`, `h1` `h2` `h3` | текст (заголовки крупнее) | шаблоны `{{…}}` |
| `p` | абзац; строчные `b strong i em u s del mark code a br item` становятся Markdown | |
| `markdown` | Markdown-текст | `src="мод:путь.md"`, `on-link="open:$url"` |
| `button` | кнопка | `on-click`, `sound="false"` |
| `toggle` | переключатель | `bind` |
| `slider` | ползунок | `bind`, `min`, `max`, `step` |
| `progress`, `ring` | полоса и кольцо прогресса | `bind`, `max` |
| `color-picker` | выбор цвета | `bind`, `value`, `alpha="false"` |
| `item` | предмет игры | `item="minecraft:diamond"`, `size` |
| `img` | текстура | `src`, `size` |
| `fx` | эффект из реестра FX | `effect="minecraft:snow"` |
| `blocks` | 2D-карта из блоков, ряды — текстом внутри тега, `.` — пусто | `tile`, `key="G=minecraft:grass_block; S=minecraft:stone"`, `brush` (редактор), `grid="true"` |
| `spacer` `hr` `br` | распорка, линия, отступ | |
| `state` | переменная состояния | `name`, `value` |
| `style`, `link rel="stylesheet"` | CSS внутри файла и из ресурса | `href` |

Общие атрибуты: `id`, `class`, `style="…"` (inline-CSS), `width`, `height`, `grow`, `gap`, `margin`, `padding`,
`align`, `justify`, `direction`, `if`, `enabled`, `bind`, `on-click`, `on-hover`, `on-change`.
Классы добавляются к классу виджета, так что тема по-прежнему оформляет `button`, а CSS добавляет `button.primary`.

## CSS

**Селекторы:** тег, `.класс`, `#id`, `*`, их сочетания (`button.primary#ok`), потомок (`.menu button`),
прямой потомок (`.menu > .item`), список через запятую. Порядок применения — по специфичности, затем по порядку в файле.

**Состояния:** `:hover`, `:active`/`:pressed`, `:focus`, `:disabled`, `:checked`. Можно сочетать: `.tab:checked:hover`.

**Значения:**
- длины `px`, `em` (1em = 9px), проценты для чисел;
- цвета `#rgb`, `#rrggbb`, `#aarrggbb`, `rgb()`, `rgba()`, `hsl()`, имена (`white`, `gold`…);
- `linear-gradient(to right, …)`, `radial-gradient(circle at 30% 30%, …)`, `conic-gradient(from 90deg, …)`;
- кривые: `ease`, `ease-in-out`, `linear`, `cubic-bezier(…)`, `steps(n)`, `spring`, а также пресеты Lumina (`out-back`, `out-elastic`, `out-bounce`…).

**Свойства оформления:** `background`, `color`, `accent-color`, `opacity`, `border`, `border-width`, `border-color`,
`border-image` (градиентная рамка), `border-radius`, `box-shadow`, `glow`, `outline`, `foreground`, `padding`,
`transform` (`scale()`, `scaleX()`, `translate()`…), `scale`, `translate`, `font-size`, `text-shadow`,
`mix-blend-mode`, `transition`. Любое свойство стиля доступно и под своим именем (`text-color`, `font-scale`).

**Свойства раскладки:** `width`, `height`, `min-*`/`max-*`, `margin`, `gap`, `flex-direction`, `align-items`,
`justify-content`, `flex-grow`, `flex-shrink`, `position`, `left`, `top`, `overflow`, `display: none`.

**Переменные:** `:root { --gold: #ffcc33; }` и `var(--gold, запасное)`. Переменные темы `--accent`, `--text`,
`--surface`, `--surface-alt`, `--border`, `--shadow`, `--well`, `--corner`, `--motion` связаны с токенами:
стиль `background: var(--accent)` перекрашивается вместе с темой. Если `:root` задаёт их, меняется сама тема.

**Переходы и анимации:**

```css
.card:hover { transform: scale(1.05); transition: transform .3s out-back, background .2s; }
@keyframes pulse { from { opacity: .4; } 50% { opacity: 1; } to { opacity: .4; } }
.badge { animation: pulse 1.2s ease-in-out infinite; }   /* имя длительность [кривая] [задержка] [infinite|N] [alternate] */
```

**Оформить весь мод одной таблицей:** `Stylesheet.parse(css).applyTo(theme)` — простые селекторы (`.button:hover`)
дописываются в классы темы, а `:root` меняет её токены. Так ванильно выглядящий экран можно перекрасить без единого HTML.

## Логика

**Шаблоны.** `{{имя}}` в тексте подставляет значение сигнала и обновляется само. Фильтры: `{{v|int}}`,
`{{v|percent}}`, `{{v|hex}}`.

**Состояние без Java.** `<state name="coins" value="120"/>` создаёт сигнал. Тип угадывается по значению (число, `true`/`false`, строка).
Состояние хранится в `Bindings` и переживает горячую перезагрузку.

**Привязка.** `bind="volume"` у `toggle`, `slider`, `progress`, `ring`, `color-picker` связывает поле с сигналом в обе стороны.
`on-change="save"` вызывается при каждом изменении.

**Действия** в `on-click`, `on-hover`, `on-change` перечисляются через `;`:

| Действие | Смысл |
|---|---|
| `inc:coins`, `inc:coins=10` | прибавить (по умолчанию 1) |
| `dec:coins=40` | вычесть |
| `toggle:music` | переключить флаг |
| `set:tab=2` | присвоить |
| `sound:minecraft:entity.player.levelup` | проиграть звук |
| `save` | обработчик из Java |

**Условия.** `if="music"`, `if="!music"`, `if="tab==2"`, `if="mode!=easy"` показывают и скрывают элемент;
`enabled="…"` включает и выключает его.

**Java.** `Bindings` — единственная точка связи:

```java
new Bindings()
        .signal("player", playerName)                         // любой ReadableSignal
        .handler("save", this::save)                          // Runnable
        .handler("shake", source -> source.play(VanillaFx.shake(source, 4f, .4f)))   // получает элемент-источник
        .action("give", (arg, source) -> give(arg));          // своё действие: on-click="give:minecraft:apple"
```

## Горячая перезагрузка

`MarkupView.resource(id, bindings).watch(1f)` раз в секунду сверяет HTML и подключённые через `<link>` CSS.
Файлы читаются через менеджер ресурсов игры, поэтому их можно переопределить ресурспаком: правите файл, жмёте F3+T,
экран пересобирается, а состояние в `Bindings` сохраняется. `reload()` пересобирает вручную, `onBuild(…)` сообщает о каждой сборке.
Для тестов и редакторов есть `resources(id -> …)` — свой источник файлов.

## Расширение

```java
// свой тег
Markup.tag("inventory-slot", (node, ctx) -> new MySlot(node.attr("item")));
// свой контейнер: дети добавит ctx.children
Markup.tag("card", (node, ctx) -> ctx.children(node, Panel.box(FlexLayout.column()).styleClass("card")));
// своё свойство CSS для своего StyleKey
CssProperties.register("rarity-glow", (value, out) -> out.put(MyStyles.RARITY, Rarity.parse(value)));
// своя переменная темы
CssProperties.token("--brand", MyTokens.BRAND);
// группа для transition: rarity .3s
CssProperties.transitionGroup("rarity", MyStyles.RARITY);
```
