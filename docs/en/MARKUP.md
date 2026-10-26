# HTML and CSS in Lumina UI

You can describe a UI with HTML-like markup and CSS, and wire up logic right in the markup or with Java handlers.
Markup is turned into ordinary Lumina widgets and styles once, so it costs nothing per frame.
There is no reflection: tags, CSS properties and actions are tables of registered functions.

A live example is the "HTML and CSS" section of the showcase (`luminatest`), files
`testmod-common/src/main/resources/assets/luminatest/ui/markup_demo.html` and `markup_demo.css`.

## Quick start

```java
Signal<Float> volume = Signal.of(60f);
UiElement screen = MarkupView.resource("mymod:ui/settings.html", new Bindings()
                .signal("volume", volume)          // data from Java
                .handler("save", this::save))      // logic from Java
        .watch(1f);                                // hot reload
```

```html
<!-- assets/mymod/ui/settings.html -->
<link rel="stylesheet" href="mymod:ui/settings.css">
<state name="music" value="true"/>

<column class="settings">
  <h2>Settings</h2>
  <row><label>Music</label><toggle bind="music"/></row>
  <row enabled="music"><label>Volume</label><slider bind="volume" min="0" max="100" step="1"/><label>{{volume|int}}%</label></row>
  <button class="primary" on-click="save; sound:minecraft:ui.button.click">Save</button>
</column>
```

```css
/* assets/mymod/ui/settings.css */
:root { --accent: #ff8800; --pad: 8px; }
.settings { gap: 6px; padding: var(--pad); background: linear-gradient(to bottom, #202030, #101018); border-radius: 8px; }
button.primary:hover { transform: scale(1.06); glow: 6px var(--accent); transition: transform .25s out-back, glow .2s; }
```

It works without Java too: `Markup.build(html, new Bindings()).root()` returns a ready element.

## Tags

| Tag | Creates | Attributes |
|---|---|---|
| `div` `column` `section` `body` `main` `nav` `header` `footer` | column | |
| `row` | row (centered vertically) | |
| `stack` | layers on top of each other | |
| `panel` | panel with the theme background | |
| `scroll` | scrollable column | |
| `label` `span` `text`, `h1` `h2` `h3` | text (headings are larger) | `{{…}}` templates |
| `p` | paragraph; inline `b strong i em u s del mark code a br item` become Markdown | |
| `markdown` | Markdown text | `src="mod:path.md"`, `on-link="open:$url"` |
| `button` | button | `on-click`, `sound="false"` |
| `toggle` | switch | `bind` |
| `slider` | slider | `bind`, `min`, `max`, `step` |
| `progress`, `ring` | progress bar and ring | `bind`, `max` |
| `color-picker` | color picker | `bind`, `value`, `alpha="false"` |
| `item` | game item | `item="minecraft:diamond"`, `size` |
| `img` | texture | `src`, `size` |
| `fx` | effect from the FX registry | `effect="minecraft:snow"` |
| `blocks` | 2D block map, rows as text inside the tag, `.` is empty | `tile`, `key="G=minecraft:grass_block; S=minecraft:stone"`, `brush` (editor), `grid="true"` |
| `structure` | 3D structure, layers as text separated by `---` lines (bottom first), `.` is air; or a `BlockStructure.encode()` string in `data` | `key`, `data="lumina:1;…"`, `tile`, `yaw`, `pitch`, `auto-rotate` (no value, or °/s), `perspective` (no value, or strength 0–1), `zoom`, `shadow="false"`, `interactive="false"` |
| `world-preview` | 3D preview of the world around the player with a player marker; without a world, a demo world | `radius` (8), `below` (6), `above` (8), `follow="false"`, `interval` (s, 2; 0 means never re-read), `peel` (top layers to remove), `cut-at-player`, `marker="false"`, `demo` (seed, or `false` for no demo), `size`, plus `yaw`, `pitch`, `auto-rotate`, `perspective`, `zoom`, `shadow`, `interactive` as for `structure` |
| `slot` | inventory slot with its own cell; can be dragged into other slots | `item="minecraft:diamond"` (or `item="coal*12"`), `count`, `name`, `lore="line\|line"`, `glint`, `durability="left/total"`, `max-stack`, `look="modern"`, `size` (18), `placeholder="id"`, `takeable="false"`, `droppable="false"` |
| `entity` | 3D entity, rotates with the mouse | `type="minecraft:zombie"`, `yaw`, `pitch`, `auto-rotate` (no value, or °/s), `zoom`, `follow-mouse`, `baby`, `mainhand` `offhand` `head` `chest` `legs` `feet` (item id), `interactive="false"`, `zoomable="false"`, `size` |
| `player` | the player in their armor (like `entity`, without `type`, `baby` and equipment) | `follow-mouse`, `yaw`, `pitch`, `auto-rotate`, `zoom`, `interactive`, `zoomable`, `size` |
| `item-view` | large 3D item | `item`, `glint`, `yaw`, `pitch`, `auto-rotate`, `zoom`, `interactive`, `zoomable`, `size` |
| `select` | dropdown; options in `options` or as child `<option value="…" item="…">Label</option>` | `bind`, `options="a,b,c"`, `labels="A,B,C"`, `icons="minecraft:a,,minecraft:c"`, `search`, `placeholder`, `rows` |
| `keybind` | key binding field | `bind` (GLFW code or name: `R`, `space`, `F5`, `left-shift`), `value`, `default` |
| `date-picker` | date field with a popup calendar | `bind`, `value="2026-10-12"`, `min`, `max` (`today`), `placeholder` |
| `calendar` | inline month calendar | `bind`, `value`, `min`, `max` |
| `input`, `number` | text and number field | `bind`, `placeholder`, `type="password"`/`"email"`, `maxlength`, `min`, `max`, `step`, `variant`, `prefix`, `suffix`; validation: see "Forms" |
| `field` | form row: label, child field, hint and error under the field | `label`, `hint` |
| `form` | form column: collects validated fields, `<button type="submit">` submits it | `on-submit`, `on-invalid`, `valid="name"` ("all valid" flag), `disable-invalid="true"` |
| `book` | book from Markdown inside the tag, page break is `<!-- page -->` | `look="modern"`, `toc` (or `toc="Title"`), `spread="false"`, `src`, `on-link` |
| `spacer` `hr` `br` | spacer, line, gap | |
| `state` | state variable | `name`, `value` |
| `style`, `link rel="stylesheet"` | CSS inside the file and from a resource | `href` |

Common attributes: `id`, `class`, `style="…"` (inline CSS), `width`, `height`, `grow`, `gap`, `margin`, `padding`,
`align`, `justify`, `direction`, `if`, `enabled`, `bind`, `on-click`, `on-hover`, `on-change`, `tooltip`.
Classes are added to the widget's own class, so the theme still styles `button` while CSS adds `button.primary`.

**Tooltips** work on any tag: `tooltip="Save world\nCtrl+S"` (lines separated by `\n`, `{{…}}` templates update),
`tooltip-markdown="**Diamond**"` instead of plain text, `tooltip-kind="modern"` (default `vanilla`, like the game),
`tooltip-delay="300ms"`, `tooltip-follow="false"` (under the element instead of at the cursor).

```html
<state name="mode" value="normal"/>
<select bind="mode" options="peaceful,easy,normal,hard" labels="Peaceful,Easy,Normal,Hard"
        tooltip="World difficulty"/>
<select bind="ore" search placeholder="Ore">
  <option value="diamond" item="minecraft:diamond">Diamond</option>
  <option value="emerald" item="minecraft:emerald">Emerald</option>
</select>
<keybind bind="drop" default="Q"/>
<date-picker bind="day" min="today"/>
```

The `slot`, `entity`, `player` and `item-view` tags are empty too:

```html
<row gap="8">
  <slot item="minecraft:diamond_sword" glint durability="1161/1561" name="Excalibur" lore="Ancient|Sharp"/>
  <slot item="minecraft:coal" count="48" look="modern"/>
  <entity type="minecraft:zombie" mainhand="minecraft:iron_sword" baby auto-rotate width="60" height="80"/>
  <player follow-mouse zoomable="false" width="60" height="80"/>
  <item-view item="minecraft:beacon" auto-rotate="60" size="48"/>
</row>
```

Markup slots can be dragged between each other and into Java slots (the shared `SlotGroup.shared()`); more about slots,
dragging and 3D in [STYLING.md](STYLING.md#inventory-slots-entities-and-items-in-3d).

`world-preview` is empty as well; in a world it shows the blocks around the player, in the main menu a demo world:

```html
<world-preview radius="10" below="5" above="9" peel="2" perspective width="380" height="250"/>
```

More in [STYLING.md](STYLING.md#world-preview-widgetstructurepreview).

The `keybind`, `date-picker` and `calendar` tags are empty (like `input`); `select` is not: write `<select …/>` or close it with `</select>`.
`bind` on `select` holds the option value (a number from `<state>` stays a number), on `keybind` the key code,
on `date-picker` a `LocalDate` (shown as `2026-10-12` in templates).

## CSS

**Selectors:** tag, `.class`, `#id`, `*`, their combinations (`button.primary#ok`), descendant (`.menu button`),
child (`.menu > .item`), comma lists. Rules apply by specificity, then by order in the file.

**States:** `:hover`, `:active`/`:pressed`, `:focus`, `:disabled`, `:checked`. They combine: `.tab:checked:hover`.

**Values:**
- lengths `px`, `em` (1em = 9px), percentages for numbers;
- colors `#rgb`, `#rrggbb`, `#aarrggbb`, `rgb()`, `rgba()`, `hsl()`, names (`white`, `gold`…);
- `linear-gradient(to right, …)`, `radial-gradient(circle at 30% 30%, …)`, `conic-gradient(from 90deg, …)`;
- easing: `ease`, `ease-in-out`, `linear`, `cubic-bezier(…)`, `steps(n)`, `spring`, plus Lumina presets (`out-back`, `out-elastic`, `out-bounce`…).

**Visual properties:** `background`, `color`, `accent-color`, `opacity`, `border`, `border-width`, `border-color`,
`border-image` (gradient border), `border-radius`, `box-shadow`, `glow`, `outline`, `foreground`, `padding`,
`transform` (`scale()`, `scaleX()`, `translate()`…), `scale`, `translate`, `font-size`, `text-shadow`,
`mix-blend-mode`, `transition`. Every style property is also available under its own name (`text-color`, `font-scale`).

**Layout properties:** `width`, `height`, `min-*`/`max-*`, `margin`, `gap`, `flex-direction`, `align-items`,
`justify-content`, `flex-grow`, `flex-shrink`, `position`, `left`, `top`, `overflow`, `display: none`.

**Variables:** `:root { --gold: #ffcc33; }` and `var(--gold, fallback)`. The theme variables `--accent`, `--text`,
`--surface`, `--surface-alt`, `--border`, `--shadow`, `--well`, `--corner`, `--motion` are tied to tokens:
a `background: var(--accent)` style is recolored together with the theme. If `:root` sets them, the theme itself changes.

**Transitions and animations:**

```css
.card:hover { transform: scale(1.05); transition: transform .3s out-back, background .2s; }
@keyframes pulse { from { opacity: .4; } 50% { opacity: 1; } to { opacity: .4; } }
.badge { animation: pulse 1.2s ease-in-out infinite; }   /* name duration [easing] [delay] [infinite|N] [alternate] */
```

**Style a whole mod with one stylesheet:** `Stylesheet.parse(css).applyTo(theme)`: simple selectors (`.button:hover`)
are merged into the theme's classes, and `:root` changes its tokens. This way you can recolor a vanilla-looking screen without any HTML.

## Logic

**Templates.** `{{name}}` in text inserts a signal's value and updates by itself. Filters: `{{v|int}}`,
`{{v|percent}}`, `{{v|hex}}`.

**State without Java.** `<state name="coins" value="120"/>` creates a signal. The type is guessed from the value (number, `true`/`false`, string).
State is stored in `Bindings` and survives hot reload.

**Binding.** `bind="volume"` on `toggle`, `slider`, `progress`, `ring`, `color-picker`, `select`, `keybind`, `date-picker`
links the field to a signal both ways.
`on-change="save"` runs on every change.

**Actions** in `on-click`, `on-hover`, `on-change` are separated by `;`:

| Action | Meaning |
|---|---|
| `inc:coins`, `inc:coins=10` | add (1 by default) |
| `dec:coins=40` | subtract |
| `toggle:music` | flip a flag |
| `set:tab=2` | assign |
| `sound:minecraft:entity.player.levelup` | play a sound |
| `save` | Java handler |

**Conditions.** `if="music"`, `if="!music"`, `if="tab==2"`, `if="mode!=easy"` show and hide an element;
`enabled="…"` enables and disables it.

**Java.** `Bindings` is the only connection point:

```java
new Bindings()
        .signal("player", playerName)                         // any ReadableSignal
        .handler("save", this::save)                          // Runnable
        .handler("shake", source -> source.play(VanillaFx.shake(source, 4f, .4f)))   // receives the source element
        .action("give", (arg, source) -> give(arg));          // custom action: on-click="give:minecraft:apple"
```

## Forms

Validation is set with attributes, like in HTML, on `<input>` and `<number>`:

| Attribute | Check |
|---|---|
| `required`, `required="Custom message"` | field is not empty |
| `minlength="3"` | not shorter (while `maxlength` simply blocks typing more) |
| `pattern="[a-z]+"` + `title="Lowercase only"` | the whole text matches the regex; `title` is the message |
| `type="email"` | email address |
| `min`, `max` on `<input>` | number in range |
| `error="…"` | one message instead of the default ones for all checks of the field |

An error appears once the field loses focus after an edit (or on form submit), then updates on every edit.
To have the error text slide out under the field, wrap the field in `<field>`:

```html
<state name="nick" value=""/>
<form on-submit="register" valid="formOk">
  <field label="Nickname" hint="3–16 characters">
    <input bind="nick" required minlength="3" pattern="[A-Za-z0-9_]+" title="Latin letters and digits only"/>
  </field>
  <field label="Email"><input bind="mail" type="email" required="Enter your email"/></field>
  <button type="submit">Create</button>
  <label if="!formOk">Fill in the fields</label>
</form>
```

`<button type="submit">` validates all form fields, shows errors, focuses the first invalid field and only
on success calls `on-submit` (otherwise `on-invalid`). An invalid field gets the `FieldBase.INVALID` state
(red border); form rows are styled with the `form-field`, `form-label`, `form-hint`, `form-error` classes.

## Hot reload

`MarkupView.resource(id, bindings).watch(1f)` checks the HTML and the CSS linked with `<link>` once a second.
Files are read through the game's resource manager, so a resource pack can override them: edit the file, press F3+T,
the screen rebuilds, and the state in `Bindings` is kept. `reload()` rebuilds manually, `onBuild(…)` reports every build.
For tests and editors there is `resources(id -> …)`, a custom file source.

## Extending

```java
// custom tag
Markup.tag("inventory-slot", (node, ctx) -> new MySlot(node.attr("item")));
// custom container: ctx.children adds the children
Markup.tag("card", (node, ctx) -> ctx.children(node, Panel.box(FlexLayout.column()).styleClass("card")));
// custom CSS property for your own StyleKey
CssProperties.register("rarity-glow", (value, out) -> out.put(MyStyles.RARITY, Rarity.parse(value)));
// custom theme variable
CssProperties.token("--brand", MyTokens.BRAND);
// group for transition: rarity .3s
CssProperties.transitionGroup("rarity", MyStyles.RARITY);
```
