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
| `structure` | 3D-постройка, слои — текстом через строки `---` (нижний первым), `.` — воздух; или строка `BlockStructure.encode()` в `data` | `key`, `data="lumina:1;…"`, `tile`, `yaw`, `pitch`, `auto-rotate` (без значения или °/с), `perspective` (без значения или сила 0–1), `zoom`, `shadow="false"`, `interactive="false"` |
| `world-preview` | 3D-превью мира вокруг игрока с его меткой; без мира — демо-мир | `radius` (8), `below` (6), `above` (8), `follow="false"`, `interval` (с, 2; 0 — не перечитывать), `peel` (снять верхних слоёв), `cut-at-player`, `marker="false"`, `demo` (seed или `false` — без демо), `size`, а также `yaw`, `pitch`, `auto-rotate`, `perspective`, `zoom`, `shadow`, `interactive` как у `structure` |
| `slot` | слот инвентаря со своей ячейкой; перетаскивается в другие слоты | `item="minecraft:diamond"` (или `item="coal*12"`), `count`, `name`, `lore="строка\|строка"`, `glint`, `durability="осталось/всего"`, `max-stack`, `look="modern"`, `size` (18), `placeholder="id"`, `takeable="false"`, `droppable="false"` |
| `entity` | сущность в 3D, крутится мышью | `type="minecraft:zombie"`, `yaw`, `pitch`, `auto-rotate` (без значения или °/с), `zoom`, `follow-mouse`, `baby`, `mainhand` `offhand` `head` `chest` `legs` `feet` (id предмета), `interactive="false"`, `zoomable="false"`, `size` |
| `player` | игрок в своей броне (как `entity`, без `type`, `baby` и снаряжения) | `follow-mouse`, `yaw`, `pitch`, `auto-rotate`, `zoom`, `interactive`, `zoomable`, `size` |
| `item-view` | крупный предмет в 3D | `item`, `glint`, `yaw`, `pitch`, `auto-rotate`, `zoom`, `interactive`, `zoomable`, `size` |
| `select` | выпадающий список; варианты в `options` или дочерними `<option value="…" item="…">Подпись</option>` | `bind`, `options="a,b,c"`, `labels="А,Б,В"`, `icons="minecraft:a,,minecraft:c"`, `search`, `placeholder`, `rows` |
| `keybind` | поле назначения клавиши | `bind` (код GLFW или название: `R`, `space`, `F5`, `left-shift`), `value`, `default` |
| `date-picker` | поле даты с календарём во всплывающем окне | `bind`, `value="2026-10-12"`, `min`, `max` (`today` — сегодня), `placeholder` |
| `calendar` | встроенный календарь на месяц | `bind`, `value`, `min`, `max` |
| `input`, `number` | поле текста и числа | `bind`, `placeholder`, `type="password"`/`"email"`, `maxlength`, `min`, `max`, `step`, `variant`, `prefix`, `suffix`; проверки — см. «Формы» |
| `field` | строка формы: подпись, поле-ребёнок, пояснение и ошибка под полем | `label`, `hint` |
| `form` | колонка-форма: собирает поля с проверками, `<button type="submit">` её отправляет | `on-submit`, `on-invalid`, `valid="имя"` (флаг «всё верно»), `disable-invalid="true"` |
| `book` | книга из Markdown внутри тега, разрыв страницы — `<!-- page -->` | `look="modern"`, `toc` (или `toc="Заголовок"`), `spread="false"`, `src`, `on-link` |
| `spacer` `hr` `br` | распорка, линия, отступ | |
| `state` | переменная состояния | `name`, `value` |
| `style`, `link rel="stylesheet"` | CSS внутри файла и из ресурса | `href` |

Общие атрибуты: `id`, `class`, `style="…"` (inline-CSS), `width`, `height`, `grow`, `gap`, `margin`, `padding`,
`align`, `justify`, `direction`, `if`, `enabled`, `bind`, `on-click`, `on-hover`, `on-change`, `tooltip`.
Классы добавляются к классу виджета, так что тема по-прежнему оформляет `button`, а CSS добавляет `button.primary`.

**Подсказки** — у любого тега: `tooltip="Сохранить мир\nCtrl+S"` (строки через `\n`, шаблоны `{{…}}` обновляются),
`tooltip-markdown="**Алмаз**"` вместо текста, `tooltip-kind="modern"` (по умолчанию `vanilla` — как в игре),
`tooltip-delay="300ms"`, `tooltip-follow="false"` (под элементом, а не у курсора).

```html
<state name="mode" value="normal"/>
<select bind="mode" options="peaceful,easy,normal,hard" labels="Мирный,Лёгкий,Нормальный,Сложный"
        tooltip="Сложность мира"/>
<select bind="ore" search placeholder="Руда">
  <option value="diamond" item="minecraft:diamond">Алмаз</option>
  <option value="emerald" item="minecraft:emerald">Изумруд</option>
</select>
<keybind bind="drop" default="Q"/>
<date-picker bind="day" min="today"/>
```

Теги `slot`, `entity`, `player` и `item-view` тоже пустые:

```html
<row gap="8">
  <slot item="minecraft:diamond_sword" glint durability="1161/1561" name="Экскалибур" lore="Древний|Острый"/>
  <slot item="minecraft:coal" count="48" look="modern"/>
  <entity type="minecraft:zombie" mainhand="minecraft:iron_sword" baby auto-rotate width="60" height="80"/>
  <player follow-mouse zoomable="false" width="60" height="80"/>
  <item-view item="minecraft:beacon" auto-rotate="60" size="48"/>
</row>
```

Слоты из разметки перетаскиваются между собой и в слоты из Java (общая `SlotGroup.shared()`); подробнее о слотах,
перетаскивании и 3D — в [STYLING.md](STYLING.md#слоты-инвентаря-сущности-и-предметы-в-3d).

`world-preview` тоже пустой; в мире показывает блоки вокруг игрока, в главном меню — демо-мир:

```html
<world-preview radius="10" below="5" above="9" peel="2" perspective width="380" height="250"/>
```

Подробнее — в [STYLING.md](STYLING.md#превью-мира-widgetstructurepreview).

Теги `keybind`, `date-picker` и `calendar` пустые (как `input`), `select` — нет: пишите `<select …/>` или с `</select>`.
`bind` у `select` хранит значение варианта (число из `<state>` останется числом), у `keybind` — код клавиши,
у `date-picker` — `LocalDate` (в шаблоне выводится как `2026-10-12`).

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

**Привязка.** `bind="volume"` у `toggle`, `slider`, `progress`, `ring`, `color-picker`, `select`, `keybind`, `date-picker`
связывает поле с сигналом в обе стороны.
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

## Формы

Проверки задаются атрибутами, как в HTML, у `<input>` и `<number>`:

| Атрибут | Проверка |
|---|---|
| `required`, `required="Своё сообщение"` | поле не пустое |
| `minlength="3"` | не короче (а `maxlength` просто не даёт ввести больше) |
| `pattern="[a-z]+"` + `title="Только строчные"` | весь текст подходит под регулярное выражение; `title` — сообщение |
| `type="email"` | адрес почты |
| `min`, `max` у `<input>` | число в диапазоне |
| `error="…"` | одно сообщение вместо стандартных для всех проверок поля |

Ошибка появляется, когда поле потеряло фокус после правки (или при отправке формы), и дальше обновляется на каждой правке.
Чтобы текст ошибки выезжал под полем, оберните поле в `<field>`:

```html
<state name="nick" value=""/>
<form on-submit="register" valid="formOk">
  <field label="Ник" hint="3–16 символов">
    <input bind="nick" required minlength="3" pattern="[A-Za-z0-9_]+" title="Только латиница и цифры"/>
  </field>
  <field label="Почта"><input bind="mail" type="email" required="Укажите почту"/></field>
  <button type="submit">Создать</button>
  <label if="!formOk">Заполните поля</label>
</form>
```

`<button type="submit">` проверяет все поля формы, показывает ошибки, ставит фокус на первое неверное поле и только
при успехе вызывает `on-submit` (иначе — `on-invalid`). Неверное поле получает состояние `FieldBase.INVALID`
(красная рамка); строки формы оформляются классами `form-field`, `form-label`, `form-hint`, `form-error`.

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
