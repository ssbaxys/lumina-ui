# Colors, gradients, themes, animations, effects, Markdown

A short reference for the styling features. All of it works the same on Forge 1.20.1 and NeoForge 1.21.1,
because it lives in the core; the loaders only draw primitives.

## Colors (`render.Color`)

A color is a plain ARGB `int`, with no objects and no allocations.

```java
Color.rgb(255, 128, 0);  Color.rgba(255, 128, 0, 0.5f);  Color.hex("#5B8CFF");
Color.hsv(200, 0.8f, 1f);  Color.hsl(200, 0.8f, 0.6f);  Color.oklch(0.7f, 0.15f, 250);
Color.parse("rgb(255 128 0 / 50%)");  Color.parse("hsl(200, 80%, 60%)");  Color.parse("tomato");
Color.rainbow(t);                        // rainbow for t ∈ [0, 1]
```

Operations are computed in OKLCH, so "10% lighter" looks the same for any hue:
`lighten`, `darken`, `saturate`, `desaturate`, `rotateHue`, `withHue`, `complement`, `grayscale`, `invert`,
`withAlpha`, `mulAlpha`, `luminance`, `contrast` (WCAG), `readableOn` (black or white text on a background),
`over` (compositing). Mixing: `Color.lerp(a, b, t)` (sRGB), `Color.mix(a, b, t)` (OKLAB),
`Color.lerp(a, b, t, ColorSpace.OKLCH)`.

The hex format matches a Java int: `#AARRGGBB` (or `#RRGGBB`).

## Brushes and gradients (`render.Brush`)

| Brush | Example |
|---|---|
| Solid | `Brush.solid(0xFF5B8CFF)` |
| Linear | `Brush.linear(135, a, b, c)`, `Brush.vertical(a, b)`, `Brush.horizontal(a, b)` |
| Radial | `Brush.radial(center, edge)`, `.at(0.3f, 0.3f)`, `.withRadius(0.7f, 0.35f)` (ellipse) |
| Conic | `Brush.conic(angle, colors...)`, `Brush.rainbowWheel()` |

- Up to 8 colors, arbitrary positions and hard stops:
  `ColorStops.builder().add(0, a).hard(0.5f, a, b).add(1, b).build()`.
- `.withRepeating(true)` repeats the pattern (stripes, rings), `.withOffset(x)` shifts it (moving stripes).
- `.inSpace(ColorSpace.OKLAB / OKLCH / LINEAR / HSV / SRGB)` picks the mixing color space.
- Angles work as in CSS: 0° points up, 90° points right.
- Ready-made sets are in `Gradients`: SUNSET, OCEAN, AURORA, NEON, FIRE, MINT, PEACH, NIGHT, GOLD, BERRY, LIME, RAINBOW.

Gradients are computed per pixel in the SDF shader, with dithering against banding. Simple two-color linear
gradients take a fast path through vertex colors. Brushes can be animated (`Brush.lerp`), including the
transition solid ↔ gradient.

Canvas also has `drawArc` (arcs with a gradient), `fillCircle` and `strokeCircle`, `drawLine`, `fillTriangle`,
`fillQuad`, `drawTextureQuad` (a texture on an arbitrary quad), `drawItem("minecraft:tnt", x, y, size)`
and `setBlendMode(NORMAL | ADD | MULTIPLY | SCREEN)`.

## Style properties

Extra keys in `Styles`:

| Key | What it does |
|---|---|
| `BORDER_BRUSH` | Gradient border (overrides `BORDER_COLOR`) |
| `GLOW` | Glow (a shadow with additive blending) |
| `OUTLINE` | An outline outside the element with a gap: `new Outline(width, gap, color)` |
| `FOREGROUND` | A brush drawn over the children (dimming, highlight) |
| `TRANSLATE_X`, `TRANSLATE_Y` | Offset without affecting layout |
| `SCALE_X`, `SCALE_Y` | Per-axis scale (combined with `SCALE`) |
| `FONT_SCALE` | Text scale; inherited and affects layout |
| `BLEND` | Blend mode of the element and its children |

## Themes and tokens

A theme stores class styles and **token** values (`ThemeVar`). Styles refer to tokens instead of numbers:

```java
theme.put("card", Style.builder()
        .bind(Styles.BORDER_COLOR, Tokens.ACCENT)                                   // as is
        .bind(Styles.RADIUS, Tokens.CORNER, Radii::all)                              // with a conversion
        .fromTheme(Styles.BACKGROUND, t -> Brush.vertical(t.get(Tokens.SURFACE), t.get(Tokens.SURFACE_ALT)))
        .build());

theme.set(Tokens.ACCENT, 0xFFF0506E);   // the whole UI smoothly changes color
theme.set(Tokens.CORNER, 4f);           // all corners get smaller
theme.set(Tokens.MOTION, 0f);           // turn off transition animations (or 2f for half speed)
```

Built-in tokens: `ACCENT`, `TEXT`, `SURFACE`, `SURFACE_ALT`, `BORDER`, `SHADOW`, `WELL`, `CORNER`, `MOTION`.
Create your own like this: `ThemeVar.of("brand", Integer.class, 0xFF00FF00)`.

More:
- Several classes separated by spaces: `styleClass("button danger pill")`. Classes are applied in order.
- `theme.edit("button", b -> ...)` extends a class style, and `theme.copy()` makes an independent copy of a theme.
- Each property can have its own transition:
  `Transition.of(0.2f).key(Styles.SCALE, 0.5f, Easing.OUT_ELASTIC).key(Styles.RADIUS, 0.3f, Easing.OUT_CUBIC, 0.4f).instant(Styles.BORDER_WIDTH)`.

## Animations

```java
// Any style property, keyframes, loops and yoyo:
box.animate(Styles.TRANSLATE_X).keyframe(0, 0f).keyframe(0.5f, 160f, Easing.OUT_BOUNCE).keyframe(1, 0f)
        .duration(2.4f).loop().play();
box.animate(Styles.BACKGROUND).from(Brush.solid(a)).to(Gradients.NEON).duration(1).yoyo().loops(4).play();

// A timeline made of parts:
element.play(Motion.sequence(
        Motion.call(() -> sound()),
        Motion.parallel(a.animate(Styles.OPACITY).to(1f).motion(), b.animate(Styles.SCALE).to(1.2f).motion()),
        Motion.delay(0.2f),
        VanillaFx.shake(element, 4f, 0.4f)));
```

- `Playback` controls playback: `loops`, `loop()`, `yoyo`, `speed`, `pause`/`resume`, `onFinish`.
- Animations write to a separate layer above the style, so hover and pressed keep working underneath.
  When an element is removed from the tree, its animations stop by themselves.
- Curves: 23 built-in (`Easing.presets()`), plus `Easing.cubicBezier`, `steps(n)`, `spring(damping, oscillations)`,
  `reversed()` and `mirrored()`.

## Vanilla effects (`fx`)

`FxLayer` is an effects layer you can put on top of anything (for example, in a `StackLayout`).

```java
fx.spawn(VanillaFx.fire(x, y, 32, 32));                 // fire; .stop() fades it out
fx.spawn(VanillaFx.soulFire(x, y, 32, 32));
fx.spawn(VanillaFx.explosion(cx, cy, 1f));              // fireballs, flash, smoke, sparks, sound
fx.spawn(VanillaFx.primedTnt(x, -20, floorY, 22, 3f)    // falls, bounces, flashes, explodes
        .velocity(40, 0).onExplode(() -> panel.play(VanillaFx.shake(panel, 6, 0.5f))));
fx.spawn(VanillaFx.flames(x, y, 24));  fx.spawn(VanillaFx.lavaPop(x, y, 14));  fx.spawn(VanillaFx.smokePuff(x, y, 20));
```

Weather and particles:
- `snow` and `rain` use the vanilla precipitation textures in two layers, plus flakes, drops and splashes; `cherryLeaves`, `drips` (water or lava), `bubbles`.
- Particles: `hearts`, `angry`, `happy`, `note` (color depends on the note pitch), `crit` (normal and magic), `totem`, `enchant` (runes pulled toward a point).

Effects use vanilla textures and sounds by id, so they pick up the player's resource packs.

**Registry by name.** All built-in effects are registered as `minecraft:*`. Add your own in one line:

```java
FxRegistry.register("mymod:confetti", (x, y, w, h) -> new Particles(null).gravity(140).wobble(30, 1.2f)
        .emitter((p, r, dt) -> p.emit(x + r.nextFloat() * w, y, 0, 40, 2f, 2f, Color.hsv(r.nextFloat() * 360, 0.7f, 1)), 3f));
fx.spawn("mymod:confetti");                 // over the whole layer
fx.spawn("minecraft:snow", x, y, w, h);     // a built-in one by name
```

Options for your own particles (`Particles`): `gravity`, `drag`, `growth`, `tint`, `blend`, `wobble` (sway),
`attract` (pull toward a point), `floor` and `ceiling` (with a landing handler), `streak` (dashes along velocity),
`frames(BY_LIFE | LOOP | RANDOM)`, `emitter`. Without a sprite (`new Particles(null)`) particles are drawn as pixel squares.
Effects can be combined: `Effect.group(a, b)`. An effect without its own class: `Effect.of(tick, render)`.
For animated textures there is `SpriteSheet`.

## Markdown (`markdown.Markdown`)

```java
new Markdown(text)
        .onLink(url -> ...)                        // link click
        .onTaskToggle((item, done) -> ...);        // click on a "- [ ]" task
Markdown.bound(signal);                            // text from a signal
```

Supported:
- `#` headings and underlined headings;
- `**bold**`, `*italic*`, `~~strikethrough~~`, `++underline++`, `==highlight==`, `` `code` ``;
- code blocks, quotes;
- nested bulleted and numbered lists, tasks;
- tables with alignment;
- links and autolinks;
- horizontal rules, escaping;
- icons `![](item:minecraft:diamond)` and `![](texture:minecraft:textures/block/stone.png)`;
- syntax highlighting for code blocks with a language: java, json, css, html and more (see "Data, forms and books").

Text wraps to the width. Parsing happens once when the text changes, layout when the width changes.
Colors come from the theme style.

## New widgets

- `ColorPicker`: a saturation/brightness square, a hue bar and an alpha bar; works from the keyboard; the value is stored in a `Signal<Integer>`.
- `RingProgress`: a circular indicator with a gradient arc and a percentage in the middle, or an endless spinner (`.spinner()`).

## Block maps

`BlockMap` draws a 2D map (side view) made of game blocks. The texture is picked by block id from the game resources
(with resource packs): `minecraft:tnt` → `block/tnt_side`, `minecraft:water` → `block/water_still` tinted blue,
leaves and grass are tinted as in plains. Water, lava and fire animate at the speed from `.png.mcmeta`.

```java
BlockMap map = BlockMap.of(16,
        "....T....",
        "GGGGGGWWG",
        "DDDDDDDDD")
    .key('G', "minecraft:grass_block").key('D', "minecraft:dirt")
    .key('W', "minecraft:water").key('T', "minecraft:tnt");
map.set(4, 0, "minecraft:fire");          // place; null removes
map.fill(0, 2, 8, 2, "minecraft:stone");  // fill a rectangle
map.editable(brush).grid(true);           // editor: LMB places the brush block, RMB breaks
map.onChange((x, y, id) -> save());
String[] rows = map.toRows();             // back to rows + map.keyMap() for saving
```

A custom block look: `BlockTextures.register("mymod:crystal", TextureRef.of("mymod:textures/block/crystal.png"), 0xFFFFFFFF)`.
A block without a texture is drawn as its item icon. In markup: the `<blocks>` tag (see [MARKUP.md](MARKUP.md)).

## 3D structures

`BlockStructure` is a sparse 3D grid of blocks (y up), and `BlockModel` draws it: 45°/30° isometric by default,
`perspective(0.45f)` for real perspective (far is smaller than near), mouse rotation with inertia, wheel zoom,
auto-rotation. The model is fitted by its bounding sphere, so it keeps its size while rotating. Only exposed faces
are drawn (glass, leaves, water, fire and torches don't hide their neighbors), sorted far to near by face center,
with shading as in the game; top and bottom come from `_top`/`_bottom`/`_end` (`BlockTextures.top/bottom`). A face
without a texture is filled with a color derived from the block id.

```java
BlockStructure house = BlockStructure.fromLayers(Map.of('P', "minecraft:oak_planks", 'G', "minecraft:glass"),
        "PPP", "PPP",      // bottom layer: a line is a row along z, a character is x
        "---",
        "PGP", "P.P");     // next layer; '.' is air
house.fill(0, 2, 0, 2, 2, 1, "minecraft:spruce_planks");
BlockStructure wall = BlockStructure.extrude(blockMap, 3);   // 2D map → 3D
BlockModel model = new BlockModel(house).autoRotate(true).size(200, 200);
model.camera(30, 20).cutAbove(1).perspective(0.45f);         // camera angle, layer cut, perspective
BlockModel.drawBlockIcon(canvas, "minecraft:tnt", x, y, 16, time);  // a single isometric block
```

In markup: the `<structure>` tag (see [MARKUP.md](MARKUP.md)).

### Sharing structures as text

`encode()` turns a structure into a single string, like a schematic: format, size, corner, palette and blocks by
layer with run lengths (`3A` is three of the first palette block in a row, `.` is air, after 52 different blocks
`_Ab`). `BlockStructure.decode(text)` turns it back; spaces and line breaks inside the blocks don't matter, so the
string can be pasted from chat. `normalized()` moves the structure to (0, 0, 0).

```java
String text = house.encode();   // "lumina:1;3x2x2;0,0,0;minecraft:stone,minecraft:glass;4AB2A"
BlockStructure copy = BlockStructure.decode(text);
```

### World preview (`widget.StructurePreview`)

A 3D view of the blocks around the player: a box `radius` to the sides and `below`/`above` layers from the feet
(at most 48 per axis), read from the client world in chunks over several frames (`blocks.WorldCapture`, 8192 blocks
per frame by default, layer by layer from the bottom up). Air is not stored, unloaded chunks keep their previous
blocks and are re-read a second later, and only changes are written into the structure, so the model is rebuilt
only when the world really changed. On top there is a player marker with a view arrow. It is a `BlockModel`, so mouse
rotation, the wheel and `perspective(...)` work as usual; the model is fitted to the box (`frame`), not to the
blocks, and doesn't jump while the snapshot is still loading.

```java
StructurePreview preview = new StructurePreview()
        .radius(10).height(5, 9)          // 21×15×21 around the player
        .fallback(new DemoWorld(7))       // no world (main menu): a made-up world, the player walks in a circle
        .follow(true)                     // the box follows the player (moves every 2 blocks)
        .refreshInterval(2f)              // re-read every 2 s; 0 means only refresh()
        .onRefresh(() -> status.text("blocks: " + preview.structure().size()));
preview.perspective(0.4f).size(380, 250);
preview.peel(3);                          // remove the top 3 layers
preview.cutAtPlayer(true);                // cut at the player's head to look under the roof
preview.refresh();                        // re-read now and center the box on the player
String shared = preview.snapshot().encode();   // snapshot from the box corner as a string
```

The world comes from `Platform.worldBlocks()`, a `blocks.WorldBlocks` object (`blockAt(x, y, z)` → id, `null` is
air, `WorldBlocks.UNLOADED` means the chunk is not loaded; player position and rotation). The loaders read
`ClientLevel.getBlockState`, cache the id per block state and don't touch unloaded chunks (`hasChunk`). Your own
source: `source(() -> myWorld)`. A one-off snapshot without frames: `Platform.captureWorld(x0, y0, z0, x1, y1, z1)`
(structure from the minimum corner, also up to 48 per axis) or `WorldCapture.capture(world, ...)`.

In markup: the `<world-preview>` tag (see [MARKUP.md](MARKUP.md)).

## Inventory slots, entities and items in 3D

### Stacks and inventories (`inventory.Stack`, `inventory.Inventory`)

`Stack` is an immutable stack for the UI that doesn't depend on the game version: item id, count, custom name,
lore lines, enchantment glint and durability. `Inventory` is the slot model: slots only show it and change it
through it, so a mod syncs data with one `onChange` subscription, and rules can be checked without a screen.

```java
Stack sword = Stack.of("minecraft:diamond_sword").withName("Excalibur")
        .withLore("Forged in the Nether").withGlint(true).withDurability(400, 1561);   // stack limit is 1
Stack coal = Stack.parse("coal*48");                                           // "minecraft:coal", 48
Inventory chest = new Inventory(27);
chest.set(0, sword);
Stack rest = chest.insert(Stack.of("minecraft:stone", 200));   // into matching stacks, then empty slots; returns the rest
chest.filter((index, s) -> index != 26 || s.itemId().endsWith("_ingot")).limit(64);
chest.onChange((inv, index, before, after) -> sync(index, after));
```

Stacks merge (`canStackWith`) when everything except the count matches. The limit is 64, 1 for items with
durability, or your own with `withMaxStackSize(16)`; an inventory can lower it with `limit(n)`.

### Slots and grids (`Slot`, `SlotGrid`, `InventoryPanel`)

`Slot` shows one slot: the item, the count in the bottom-right corner (thousands as `1.5k`), a durability bar
colored green to red, and the glint. The look is `Slot.Look.VANILLA` (a gray 18×18 slot with a beveled edge and a
white highlight) or `MODERN` (a rounded theme slot using the `well`, `border`, `accent` tokens; background, radius
and border can be styled). A non-empty slot has a tooltip as in the game (`ItemTooltip`: name, aqua for enchanted
items, lore, durability when damaged); the tooltip look is `tooltipKind`.

```java
Slot single = new Slot(Stack.of("minecraft:diamond", 12));       // its own one-slot inventory
Slot helmet = new Slot(armor, 0).placeholder("minecraft:iron_helmet");   // faint silhouette in an empty slot
Slot result = new Slot(craft, 0).dropTarget(false);               // take only
SlotGrid hotbar = new SlotGrid(player, 0, 9, 9).look(Slot.Look.MODERN).slotSize(22).gap(3);
InventoryPanel chestUi = InventoryPanel.chest("Chest", chest, player, Slot.Look.VANILLA);
InventoryPanel mine = new InventoryPanel(Slot.Look.MODERN).title("Furnace").add(row).add(hotbar, 4);
Slot.paintStack(canvas, stack, x, y, 16);                         // a stack in your own widget
```

`InventoryPanel.chest` builds a chest window: a grid of 9 per row, "Inventory", the backpack (player slots 9..35)
and the hotbar (0..8); Shift+click moves between the chest and the player. In the core the player inventory is a
plain 36-slot model; container screens (below) connect it to the real inventory and the server.

### Drag and drop (`SlotGroup`)

- LMB drag (from 3 px) takes the whole stack, RMB takes half rounded up; a "ghost" follows the cursor (a passive
  root overlay) and the source shows what's left. The model doesn't change until the drop, so cancelling rolls
  nothing back, and the mod sees one change per slot.
- Dropping on an empty slot or a matching stack adds as much as fits; the rest stays in the source. A whole stack
  dropped on a different item swaps them (if both slots accept the other item). The slot under the cursor is
  highlighted normally if the drop is allowed and red if it isn't.
- Dropping outside the slots calls `onDropOutside` (return `true` if the stack was taken), otherwise it cancels.
- Shift+click is `quickMove`: your `onQuickMove` or a transfer into the inventories from `Slot.quickMoveTo`
  (slot ranges work too). The core reads Shift through `Platform.isShiftDown()`.

```java
SlotGroup group = new SlotGroup()
        .validator((from, to, stack) -> to.inventory() != shop)          // drop check on top of filters
        .onTransfer((from, to, moved) -> playSound())                   // to == null means Shift+click or a throw
        .onDropOutside((from, stack) -> { throwItem(stack); return true; })
        .onQuickMove((slot, stack) -> false);                           // false means the default transfer
chestGrid.group(group);
group.beginDrag(slot, false); group.dropOn(otherSlot);                  // the same from code (tests, keyboard)
```

Without their own group, slots share `SlotGroup.shared()`: you can drag between any slots.

### Server-synced container screens (`menu.MenuSpec`, `menu.RemoteMenu`)

A chest, furnace or custom machine window owned by the server, just like a vanilla chest. `MenuSpec` is a
description shared by server and client (usually a constant): slot sections and synced numbers. On the loader,
`LuminaMenu` is a regular `AbstractContainerMenu` with vanilla slots (`broadcastChanges` sends the contents and the
cursor, Shift+click defaults to chest behavior), values go through `DataSlot` (an int as two 16-bit halves), and the
screen is a `LuminaMenuScreen` with the `UiRoot` from your factory.

```java
// shared code
static final MenuSpec SPEC = MenuSpec.builder()
        .container("chest", 27).playerInventory()        // menu slots: 0..26, then backpack and hotbar
        .intValue("progress").boolValue("burning").build();
static final LuminaMenuType CHEST = new LuminaMenuType(SPEC);
MENUS.register("chest", CHEST::create);                   // DeferredRegister<MenuType<?>>

// server
CHEST.open(player, Component.literal("Chest"), container, index -> progress, (p, action, value) -> sort());

// client: Forge in FMLClientSetupEvent.enqueueWork, NeoForge in RegisterMenuScreensEvent
LuminaMenuScreen.register(CHEST, menu -> new UiRoot(InventoryPanel.chest(menu.title(),
        menu.inventory("chest"), menu.playerInventory(), Slot.Look.MODERN), ModernTheme.dark()));
new ProgressBar().bind(menu.intValue("progress").map(p -> p / 100f));
new Button("Sort", () -> menu.sendAction(SORT, 0));
```

`RemoteMenu` gives each section a regular `Inventory`, but a remote one (`Inventory.remote`): slots and grids look
and drag as usual, but a drop sends vanilla clicks through `handleInventoryMouseClick`: the server validates and the
game predicts the result immediately. A transfer is "pick up" (all or half), "place" and, if something is left on
the cursor, "put back"; Shift+click is a quick move (the server decides the route, `Slot.quickMoveTo` is not
needed); outside the slots it's a throw if `onDropOutside` returned `true`; keys 1–9 and F over a slot swap with the
hotbar and offhand, Q throws (with Ctrl the whole stack). Without `remote` the inventory is local as before; you
can't mix remote and local slots in one transfer.

The screen checks slot contents every frame (reference and count, fully once per tick) and builds a `Stack` only
for the changed ones: name, the game's tooltip lines (with colors), durability, stack limit and the game stack
itself (`Stack.platformStack()`) so potions and heads render exactly. Values are `intValue`/`boolValue` signals,
updated together once per frame. Your own buttons use `sendAction(id, number)`: the pair is encoded as menu button
clicks (`clickMenuButton`, 7 bits per click, usually two), so no custom network channel is needed. Demo:
`/luminatest chest`.

### Entities and items in 3D (`preview.EntityView`, `preview.ItemView`)

`EntityView` draws a mob or the player the way the game does in the inventory screen: the model is fitted into the
widget feet down, rotates with the mouse with inertia, can spin by itself, zooms with the wheel (`zoomable(false)`
to not interfere with page scrolling), and with `followMouse(true)` looks at the cursor. The entity is created on
the client, one per type and appearance, so it is visible only in a world (empty in the main menu). `ItemView` is a
large 3D item: blocks are solid, everything else looks like a dropped item; a backend without rotation draws a flat
icon.

```java
new EntityView("minecraft:zombie").equip("mainhand", "minecraft:iron_sword").baby(true).autoRotate(true).size(64, 80);
EntityView.player().followMouse(true).size(110, 150);          // you, in your armor
new EntityView("minecraft:creeper").camera(-25, 10).zoom(1.5f); // yaw > 0 faces left, pitch > 0 is from above
new ItemView(Stack.of("minecraft:diamond_sword").withGlint(true)).autoRotate(40f).size(72, 72);
```

The common base is `SpinView`: `yaw/pitch/camera`, `pitchRange`, `autoRotate`, `zoom`, `interactive`, `zoomable`.
For your own widgets, `Canvas` has:

| Method | What it draws |
|---|---|
| `drawItem(id, x, y, size, glint)` | an item with enchantment glint |
| `drawItem3d(id, x, y, size, yaw, pitch, glint)` | a 3D item rotated around its center |
| `drawEntity(type, x, y, size, yaw, pitch, options)` | an entity fitted into a square; `Canvas.PLAYER` is the player |
| `drawPlayer(x, y, size, yaw, pitch, options)` | the same as `drawEntity(Canvas.PLAYER, …)` |

`EntityOptions`: head pose (`look(yaw, pitch)`), baby (`baby`) and equipment (`equip("head", id)`; slots are
`mainhand offhand head chest legs feet`). By default these `Canvas` methods draw nothing (or a flat icon), so a
custom backend doesn't have to support them.

In markup: the `<slot>`, `<entity>`, `<player>`, `<item-view>` tags (see [MARKUP.md](MARKUP.md)). An example of all
of it together is the `ItemsPage` page in the test mod.

## Components

**Text with effects** (`FancyText`) draws a string character by character with the regular game font:

```java
new FancyText("Lumina UI").scale(2).gradient(0xFFFF8A00, 0xFFE52E71).flow(0.4f).outline(0xFF000000);
new FancyText("Rainbow").rainbow().wave(2f, 6f);        // rainbow and wave (amplitude, speed)
new FancyText("Shake!").shake(0.8f);
new FancyText("Hello!").typewriter(24f).typeSound("minecraft:ui.button.click").onTyped(this::next);
```

**Table** (`Table<T>`): columns with width weights, sorting by clicking a header, row selection, zebra stripes,
long text truncated with an ellipsis. You can draw a cell yourself (icon, bar):

```java
Table<Ore> t = new Table<Ore>()
        .column("Ore", 2, Ore::name)
        .column("Mined", 1, o -> String.valueOf(o.count()), Comparator.comparingInt(Ore::count))
        .column("Chance", 1, (canvas, ore, x, y, w, h) -> canvas.fillRect(x, y + 5, w * ore.chance(), 6, 0xFF5B8CFF), null)
        .rows(ores);
t.selection().subscribe(o -> show(o));
```

**Tree** (`TreeView`): nodes with item icons, expanded by the arrow, a double click or the keyboard.
`TreeView.node("World", "minecraft:grass_block").add(node("Nether"), ...)`, `selection()`, `onSelect(...)`.

**Charts** (`Chart`): `LINE`, `AREA`, `BAR`, `PIE`, `DONUT`. Values grow in on appearance, `values(series, ...)`
changes them smoothly, hovering shows a tooltip.

```java
new Chart(Chart.Kind.BAR).labels("Mon", "Tue", "Wed").series("Stone", 0xFF8AB4F8, 120, 180, 90).unit(" pcs");
```

**Loading**: `Spinner` (`RING`, `DOTS`, `BARS`, `PULSE`) and `Skeleton` (`line(w)`, `fullLine()`, `circle(d)`) with a moving shine.

**Windows** (`Window`): placed absolutely inside any container (`at(x, y)`), dragged by the title bar, resized by
the corner; a click brings the window to the front (`UiElement.bringToFront()`), Esc and the close button close it
with an animation. `modal(true)` dims everything below the window and blocks clicks.

**Live wallpapers** (`Backdrop`): `AURORA`, `STARS`, `WAVES`, `FLOW`, `GRID`; `colors(...)`, `speed(...)`,
`parallax(pixels)` makes the layers follow the mouse. Put them as the first layer in a `StackLayout`; they don't
capture the mouse.

**Texture frame** (`NineSlice`): corners don't stretch, edges and the middle do.

```java
panel.style(b -> b.set(Styles.NINE_SLICE, NineSlice.of("minecraft:textures/gui/demo_background.png", 248, 166, 4).inFile(256, 256).scaled(2)));
```

In CSS: `nine-slice: url(minecraft:textures/gui/demo_background.png) 248 166 4 in 256 256 / 2` (the border as 1 or 4 numbers; after `in` the size of the whole file if the frame sits in its corner, like vanilla 256×256 images; after `/` the scale).

**Pages with transitions** (`Pages`): shows one page by the number from a signal, handy together with `Tabs` and
`SideNav`. Effects `FADE`, `SLIDE` (in the direction of the change), `SLIDE_UP`, `ZOOM`, `FLIP`; a page is created on
first show and kept by default (`keepAlive`).

```java
Signal<Integer> tab = Signal.of(0);
new Tabs(tab, "Screen", "View");
new Pages(tab).effect(Pages.Effect.SLIDE).page(this::screen).page(this::view);
```

A ready-made modern settings screen is the "Modern style" section of the showcase (`ModernPage`).

## Popup widgets

Dropdowns, tooltips, the radial menu and the calendar live in the **popup layer** of `UiRoot`: they are drawn above
the content and get the mouse and keys first. Escape closes the top popup, and so does a click outside.

- `root.showPopup(element, x, y)` / `closePopup` / `popups()` are popups (menus, lists);
- `root.showOverlay(element, x, y)` / `overlays()` are **passive overlays** (tooltips): drawn above all popups,
  but the mouse passes through them, they get no keys, and a click outside doesn't close them;
- `root.movePopup(element, x, y)` moves an open popup or overlay (a tooltip following the mouse);
- a key release (`keyUp`) also goes to the top popup first; that's how the radial menu picks an item.

**Dropdown** (`Select<T>`): a field with the current value and an arrow; the list opens below the field (above it if
there is no room) with a vertical reveal. A long list scrolls with the wheel (only visible rows are drawn), a search
line filters options by substring ignoring case, and items can have item icons. Keyboard: when closed, Enter, Space
or Alt+↓ open it, ↑/↓ change the value in place, a letter jumps to an option; when open, ↑/↓, PageUp/PageDown,
Home/End, Enter picks, Escape closes.

```java
Signal<String> ore = Signal.of("minecraft:diamond");
new Select<>(ore, List.of("minecraft:diamond", "minecraft:emerald", "minecraft:gold_ingot"))
        .labels(id -> names.get(id))   // option label
        .icons(id -> id)               // icon is an item id (or null)
        .searchable(true)              // search line
        .maxRows(7)                    // visible rows, then scrolling
        .placeholder("Pick an ore")    // while the value is null
        .onChange(id -> log(id));
Select.of(difficulty, Difficulty.values()).labels(Difficulty::title);
```

Styles: `"select"` is the field (like `"field"`; an open list is the `Select.OPEN` state), `"dropdown"` is the list
card (background, border, shadow; the highlighted row uses `ACCENT_COLOR`, text uses `TEXT_COLOR`).

**Tooltips** (`Tooltip`, `Tooltips`): appear after a hover delay (0.5 s), follow the mouse and flip at the screen
edges. A mouse press hides the tooltip until the next move; only one tooltip is visible, the one of the deepest
hovered element. An element's tooltip disappears when the element is removed from the tree.

```java
button.tooltip("Save world\nCtrl+S");                             // text, lines separated by \n
button.tooltip(Tooltip.Kind.MODERN, "Theme card");                // custom look
icon.tooltipMarkdown("**Diamond** ![](item:minecraft:diamond)");  // Markdown
Tooltips.attach(slot, () -> buildCard(slot))                      // any element, built when shown
        .kind(Tooltip.Kind.MODERN).follow(false).delay(0.2f).maxWidth(220);
Tooltips.attach(label, "on any UiElement");                       // a label becomes hoverable too
Tooltips.of(button).text("New text");                             // an open tooltip updates immediately
Tooltips.detach(button);
Tooltips.setDefaultKind(Tooltip.Kind.MODERN);                     // default look and delay
Tooltips.setDefaultDelay(0.3f);
```

Looks: `VANILLA` is like the game: almost black background `0xF0100010`, a purple gradient border
`0x505000FF → 0x5028007F`, cut corners, white text with a shadow, 2 extra pixels after the first line (the name)
(style `"tooltip-vanilla"`: `BACKGROUND`, `BORDER_BRUSH`, `TEXT_COLOR`, `PADDING`); `MODERN` is a theme card with
rounded corners, a border, a shadow and a soft fade-in (style `"tooltip"` on the `SURFACE`, `BORDER`, `SHADOW`,
`TEXT`, `CORNER` tokens). `follow(false)` places the tooltip centered below the element (above it if there is no
room). `show()` / `hide()` show and hide it from code.

**Radial menu** (`RadialMenu`): sectors around the point where it opened. A sector is picked by the mouse direction
from the center (even outside the ring); the hovered one slides out and turns accent-colored, with its label in the
center. An item can open a submenu (a small triangle at the sector edge); right click, a click in the center,
Backspace or Escape go back. Arrow keys cycle sectors, Enter picks, digits 1–9 pick an item by number.

```java
RadialMenu wheel = new RadialMenu()
        .title("Actions")
        .item("Sword", "minecraft:diamond_sword", this::sword)
        .item("Torch", this::torch)                                  // no icon: the label sits in the sector
        .submenu("Food", "minecraft:apple",
                RadialMenu.Item.of("Bread", this::bread).icon("minecraft:bread"),
                RadialMenu.Item.of("Cake", this::cake).enabled(false))
        .radius(30, 88);
wheel.show(root, mouseX, mouseY);                       // pick with a click
wheel.showHeld(root, mouseX, mouseY, Keys.R);           // while R is held: releasing picks
RadialMenu.bindKey(screenContent, Keys.R, this::wheel); // R pressed over the element (or its focused descendant)
RadialMenu.bindRightClick(area, this::wheel);           // right click over the element
```

The menu covers the whole screen (the style background dims it). Style `"radial-menu"`: `BACKGROUND` is the dimming,
`PopupStyles.WEDGE_COLOR` (`radial-wedge-color`) the sectors and center, `ACCENT_COLOR` the hovered sector,
`TEXT_COLOR` the labels. `bindKey` fires when the key reaches the element: without focus, keys go to `root.content()`.

**Key binding** (`KeyBindField`): a click or Enter makes the field wait for a key ("> Press a key <" pulsing); the
next key (any, even Tab) becomes the value, Escape and losing focus cancel waiting, right click or Delete restore
`defaultKey`. The value is a GLFW code in a `Signal<Integer>`, `KeyBindField.NONE` means unset.
`KeyBindField.keyName(code)` gives a name ("Space", "Left Shift", "F5", "Num 7"), `parseKey("space")` goes back.
Style `"field keybind"`, waiting is the `KeyBindField.LISTENING` state.

**Dates** (`DatePicker`, `CalendarView`): a `12.10.2026` field opens a month calendar in a popup. Weeks start on
Monday, the selected day is filled with the accent, today is outlined, days of neighboring months are dimmed, days
outside `range(min, max)` are disabled. Months change with the header arrows, the wheel and PageUp/PageDown (the
grid briefly slides), days with the arrows, Enter picks. `CalendarView` can also be placed directly in the UI.

```java
Signal<LocalDate> date = Signal.of(LocalDate.now());
new DatePicker(date).range(LocalDate.now(), null).format(DateTimeFormatter.ofPattern("d.M.yy"));
new CalendarView(date).onPick(d -> log(d));
```

Styles: the field is `"select"`, the popup calendar is `"dropdown calendar"`, the inline one is `"calendar"`.
All the new classes exist in `Theme.dark()`, `Theme.light()` and `ModernTheme` and are built on tokens.
In markup: the `<select>`, `<keybind>`, `<date-picker>`, `<calendar>` tags and the `tooltip` attribute (see [MARKUP.md](MARKUP.md)).
An example of everything together is the showcase page `PopupsPage`.

## Data, forms and books

### A list with tens of thousands of rows (`VirtualList<T>`)

Rows have the same height; only the visible part is laid out and drawn, so 10,000 or 100,000 entries cost as much as
ten. Scrolling is smooth (a spring; a long jump with Home/End snaps close to the target and animates the last screen),
with the wheel and a scrollbar; ↑/↓, PageUp/PageDown, Home/End move the selection, Enter and a double click call
`onActivate`.

```java
// Rows are drawn straight onto the Canvas, without a single element per row.
VirtualList<Item> list = new VirtualList<Item>(18, (c, item, i, x, y, w, h, row) -> {
            c.drawItem(item.id(), x + 2, y + 1, 16);
            c.drawText(item.name(), x + 22, y + 5, row.textColor(), false);   // row.selected(), row.hovered()
        })
        .bind(view.visible())            // or .items(list)
        .emptyText("Nothing found")
        .onActivate(this::give)
        .height(Dim.px(180));
list.selection().subscribe(this::preview);   // the selected entry; selection().set(x) selects from outside
VirtualList.ofText(16, Item::name);          // plain text

// Widget rows: as many are created as are visible (+1); scrolling puts other entries into them.
new VirtualList<Item>(24, null).rows(ItemRow::new, (row, item, index) -> row.show(item));
```

Methods: `items()`, `refresh()` (after editing the list in place), `select(i)`, `selectedIndex()`, `scrollTo(y, animate)`,
`ensureVisible(i, animate)`, `firstVisible()`/`lastVisible()`, `visibleRows(n)` (height without an explicit size), `zebra(false)`.
Style: `virtual-list`.

### Search, filters, sorting and pages (`data.DataView<T>`)

A view over a list: the results are signals that `VirtualList`, `Table` and `Paginator` read.

```java
DataView<Item> view = new DataView<>(items)            // or DataView.of(signalWithList)
        .searchBy(i -> i.name() + " " + i.category())  // search: all query words, ignoring case
        .sort(Comparator.comparing(Item::name))        // sort(c, true) descending, sort(null) data order
        .pageSize(50);                                 // 0 means no pages
new TextField(view.query());                           // the search line is a plain Signal<String>
view.setFilter("blocks", Item::isBlock);               // a named filter; setFilter("blocks", null) removes it
view.filterWhen("rare", onlyRare, i -> i.rarity() > 1);    // active while the signal is true
view.visible();   // entries on the current page    view.filtered();  // all matches    view.total();  // how many
view.page(); view.pageCount(); view.nextPage(); view.previousPage(); view.dispose();
```

A new query or filter goes back to the first page; an out-of-range page number is clamped. Search strings are
lowercased once when the data changes.

**Page switcher** (`Paginator`): "« ‹ 1 … 4 5 6 … 20 › »", the current page highlight slides with a spring,
←/→/Home/End and clicks. `new Paginator(view)` or `new Paginator(page, pageCount)`; `siblings(2)`, `edgeArrows(false)`.
Style: `paginator`.

**Table** (`Table`) draws only the rows in the visible area (the `ScrollPanel` window, the screen edge), so a table
with thousands of rows in a scroll panel stays fast. `table.bind(view)` shows the view's current page, and clicking a
header sorts the whole view; `table.bind(signal)` takes rows from any signal.

### Input validation and forms

Validators (`Validator`) attach to any field (`TextField`, `NumberField`, `ColorField`):

```java
new TextField(login).validate(Validator.required(), Validator.minLength(3),
        Validator.pattern("[A-Za-z0-9_]+", "Only Latin letters, digits and _"));
new NumberField(age).validate(Validator.range(13, 120, "Age must be 13 to 120"));
new TextField(repeat).validate(Validator.sameAs(password::peek, "Passwords don't match"));
Validator.of(s -> s.chars().anyMatch(Character::isDigit), "Needs a digit");   // your own check
// more: Validator.email(), integer(), maxLength(16), v.and(other), v.message("Custom message"); field.required()
```

All ready-made validators except `required` accept empty text. The error shows when the field loses focus after an
edit, and then updates on every edit. On a field: `error()` is the shown error (a signal), `valid()` whether the text
passes right now (a signal), `validate()` checks and shows, `check()` checks silently, `setError("Name taken")` sets
your own error (a server reply), `resetValidation()`. An invalid field gets the `FieldBase.INVALID` state and a red
border.

**Form row** (`FormField`): a label, the field, a hint (`hint`) and an error that slides out below the field, making
room. **Form** (`Form`) collects the fields:

```java
Form form = new Form()
        .onSubmit(() -> register(nick.peek()))
        .onInvalid(() -> toast("Fix the errors"));
panel.add(
        form.field("Nickname", new TextField(nick).required()).hint("3–16 characters"),
        form.field("Email", new TextField(mail).validate(Validator.required(), Validator.email())),
        form.submitButton(new Button("Create"), false));    // true disables the button while the form is invalid
form.check(() -> agree.peek());      // your own whole-form check
form.valid();                        // "all valid" signal, changes on every edit
form.validate(); form.submit(); form.reset(); form.errors();
new Form().collect(markupRoot);      // all validated fields inside an element
```

`submit()` commits pending input, shows errors on all fields at once and focuses the first invalid one.
Styles: `form-field`, `form-label`, `form-hint`, `form-error`. In markup: `<form>`, `<field>` and the `required`,
`minlength`, `pattern` attributes (see [MARKUP.md](MARKUP.md)).

### Setup wizard (`Wizard`) and step indicator (`Stepper`)

`Stepper` shows numbered circles with a progress line and labels: completed steps get a check mark, the current one
pulses, the line stretches to the step with a spring. `new Stepper(step, "Account", "Profile", "Done").clickable(i -> i < step.peek())`.

`Wizard` is a `Stepper`, the step page (sliding via `Pages`) and Back / Next / Finish buttons. Next runs the step's
check first; if it fails, the page shakes and stays.

```java
new Wizard()
        .step("Server", serverPage, serverForm)               // the check is the step's Form (errors under fields)
        .step("Mode", this::modePage, () -> mode.peek() != null)
        .step("Done", this::summary)
        .labels("Back", "Next", "Create server")
        .onFinish(this::create);
wizard.step();   // Signal<Integer> with the step number; next(), back(), isFinished()
```

Styles: `stepper`, `wizard`, `wizard-nav`.

### Book (`markdown.Book`)

Markdown split into pages: a break is a `<!-- page -->` or `\newpage` line; whatever doesn't fit flows to the next
page by itself (long paragraphs are split by words).

```java
new Book(text);                                              // like a Minecraft book: texture, "Page 1 of 5", arrows
new Book(text).look(Book.Look.MODERN).tableOfContents(true)  // a spread on a theme card + table of contents as page one
        .width(Dim.px(340)).height(Dim.px(220));
book.page().set(4); book.next(); book.previous(); book.goTo(0); book.pageCount(); book.toc();
```

Pages turn with the arrows, the wheel, ←/→, PageUp/PageDown, Home/End, with a page flip animation and the
`item.book.page_turn` sound (`sound(false)`, `flipDuration(0.3f)`). The table of contents is built from `#` and `##`
headings (`tocLevel(3)`), and its entries are links. Links `[text](#page:5)` and `[text](#chapter-name)` turn the
book, the rest go to `onLink`. `spread(false)` is a single-page modern book. Styles: `book` and `book.vanilla`.

### Syntax highlighting (`markdown.SyntaxHighlighter`)

Code blocks with a language in `Markdown` and `Book` are highlighted: `java`, `kotlin`, `javascript`/`js`/`ts`,
`json`, `css`, `html`/`xml`/`svg`. Parsing happens once at layout; multi-line comments and text blocks are handled.
Colors adapt to the theme (light or dark text, accent for keywords); set your own with theme tokens:

```java
theme.set(SyntaxHighlighter.STRING_COLOR, 0xFF98C379).set(SyntaxHighlighter.KEYWORD_COLOR, 0xFFC678DD);
SyntaxHighlighter.register((line, state, out) -> { out.span(0, line.length(), SyntaxHighlighter.COMMENT); return 0; },
        "mylang");                                     // your own language: marks up a line, returns the state
SyntaxHighlighter.highlight("java", code);             // line pieces with token kinds
```

An example of this whole section is the showcase page `DataToolsPage`.

## Themes in files, auto-switching, accessibility, sounds and responsiveness

### Theme as CSS (`markup.ThemeCss`)

`ThemeCss.export(theme, "dark")` writes a theme as CSS relative to a base theme: `:root` holds all tokens
(`--accent`, `--surface`, `--motion`...) and `--lumina-base: dark`, followed by class rules. For classes that exist
in the base, only values set as numbers and different from the base are written; token bindings (`bind`) stay in the
base and follow the tokens again after loading. Classes missing from the base are saved in full (bindings as
computed values). States are `:hover`, `:active`, `:focus`, `:disabled`, `:checked`, modifiers are `button.primary`.
Whatever CSS can't express (custom state bits, non-standard values) goes into the file as a comment.

```java
String css = ThemeCss.export(theme, "dark");
Theme back = ThemeCss.load(css);          // base from --lumina-base, then the rules
ThemeCss.save("my-theme", theme, "dark"); // <dataDirectory>/themes/my-theme.css
Theme mine = ThemeCss.loadFile("my-theme");
List<String> all = ThemeCss.savedThemes();
```

Bases: `dark`, `light`, `modern-dark`, `modern-light`, `high-contrast-dark`, `high-contrast-light`; your own with
`ThemeCss.registerBase(name, factory)`. `export(theme)` without a base name compares against the dark theme and
doesn't write a base. Limitation: the order of variants within one class comes from the CSS when loading, so rare
state combinations (hover+focus on overlapping variants) may resolve differently than in the original theme.
Files are read and written via `Platform.readFile/writeFile` inside `Platform.dataDirectory()`
(`config/luminaui` by default); absolute paths and `..` are rejected. Prefer Latin file names.

### Theme editor (`widget.ThemeEditor`)

Accent, text and surface colors, corner radius and animation speed; it changes its root's theme immediately.
Buttons: Export (file `fileName(...)` + clipboard), Import, From clipboard, Reset.

```java
new ThemeEditor().fileName("my-theme.css").baseName("dark").onLoad(t -> ...);
```

### Smooth theme changes and auto-switching (`theme.ThemeTransition`, `theme.ThemeSchedule`)

`ThemeTransition.to(root, target, 0.8f)` gives the root a working copy and smoothly moves the tokens to the target
theme (colors blend in OKLAB). With reduced motion the change is instant.

`ThemeSchedule` picks a theme by time of day (ticks 0–24000) or by biome and switches it smoothly:

```java
ThemeSchedule.byWorldTime()
        .at(0f, Theme.light())
        .at(12500f, Theme.dark())
        .blend(1000f)              // blending before each switch
        .build()
        .transition(1.2f)
        .attach(root);             // Subscription; close() stops it

ThemeSchedule.byWorldBiome()
        .when("minecraft:desert", desertTheme)
        .when(ThemeSchedule.contains("snow"), snowTheme)
        .otherwise(Theme.dark())
        .build()
        .decorate(t -> SoundTheme.SOFT.applyTo(t))  // applied on top of each chosen theme
        .attach(root);
```

Time and biome come from `platform.WorldInfo`: `timeOfDay()` uses `Platform.dayTime()` (world time in game) and the
system clock without a world; `biome()` uses `Platform.biomeId()`. For tests and examples you can pass your own
sources: `ThemeSchedule.byTime(() -> slider.value().peek())`.

### Accessibility (`a11y`)

Each root has its own settings: `root.accessibility()`.

```java
root.accessibility()
        .colorVision(ColorVision.DEUTERANOPIA)                          // correction (daltonize)
        .colorVision(ColorVision.PROTANOPIA, Accessibility.ColorMode.SIMULATE) // how a person sees it
        .textScale(1.25f)          // all font sizes × 1.25 (0.5–3)
        .reducedMotion(true)       // style and theme transitions become instant
        .focusAlwaysVisible(true)  // a visible focus ring on top of everything
        .narration(true);          // focus is narrated via Platform.narrate
```

The color filter applies to the whole frame at the `Canvas` level (`setColorFilter`): fills, gradients, borders,
shadows and text; textures and items are not filtered. Your own filter: `extraFilter(ColorFilter)`, for example
`ColorFilter.grayscale()`. The narration text is `UiElement.accessibleLabel()`; buttons, checkboxes and labels take
it from their text, set your own with `setAccessibleLabel(...)`.

High-contrast themes: `AccessibleThemes.highContrastDark()`, `highContrastLight()` or
`AccessibleThemes.highContrast(theme)`: black/white background, text at least 7:1, accent at least 4.5:1, 2-pixel
borders and a visible focus outline. There are also `filterColors(theme, filter)` and `reducedMotion(theme)`.

### UI sounds (`sound`)

The `HOVER_SOUND` and `CLICK_SOUND` style keys (`hover-sound`, `click-sound` in CSS) take a `UiSound` value:

```css
.button { hover-sound: minecraft:block.note_block.hat 0.12 1.6; click-sound: minecraft:ui.button.click 0.25 1; }
.toggle { click-sound: none; }
```

The hover sound plays only when the pointer enters an enabled element, at most once every 70 ms
(`root.setHoverSoundInterval(seconds)`). Without `click-sound`, buttons click like in the game.
Ready-made sets: `SoundTheme.VANILLA`, `SOFT`, `RETRO`, `NONE`; `SoundTheme.custom(name, hover, click)`,
`scaled(volume)`. `SoundTheme.RETRO.applyTo(theme, "nav")` assigns sounds to buttons, toggles, checkboxes, menu
items and the listed classes.

### Responsiveness and scale (`layout`)

`UiRoot` reports a width class: `root.breakpoint()` (a signal), `root.currentBreakpoint()`:
`COMPACT` (< 400), `MEDIUM` (< 640), `WIDE`; set the bounds with `root.setBreakpoints(new Breakpoints(400, 640))`.

```java
ResponsivePanel.rowFrom(Breakpoint.MEDIUM, 8).add(a, b, c);   // a column when narrow, a row from MEDIUM
ResponsivePanel.columns(1, 2, 3, 8).add(cards);               // a 1/2/3-column grid
ResponsivePanel.columns(1, 2, 2, 6).byContainerWidth(true);   // by its own width instead of the root's
new Panel("grid", new GridLayout(3, 8, 6));                   // just equal columns
```

`root.setScale(1.5f)` enlarges the whole UI (0.25–4): layout is computed in logical pixels
(`root.width()` = physical width / scale), mouse coordinates are divided by the scale, and the width class is
recomputed too.

### Platform hooks

| `Platform` method | Default | Purpose |
|---|---|---|
| `dataDirectory()` | `config/luminaui` | folder for theme files |
| `readFile/writeFile/listFiles` | via `java.nio` | files inside `dataDirectory()` |
| `dayTime()` | `-1` (no world) | world time in ticks 0–24000 |
| `biomeId()` | `null` | biome id under the player |
| `narrate(text)` | nothing | narration for the screen reader |
| `isShiftDown()` | `false` | Shift+click on a slot (quick move) |
| `playSound(id, volume, pitch)` | nothing | UI sounds |
| `pollGameData(GameState out)` | `false` (no world) | player snapshot for `GameData`, every client tick |
| `hudState()` | `0` | F1 / F3 / chat / screen flags for HUD visibility rules, every frame |
| `createTexture(key, w, h, argb)` | `null` | a texture from pixels (GIF frames, minimap) |
| `sampleMap(radius)` | `null` | map colors around the player for `MiniMap` |
| `worldBlocks()` | `null` | client world blocks one by one for `StructurePreview` and `WorldCapture` |
| `captureWorld(x0, y0, z0, x1, y1, z1)` | via `worldBlocks()` | a world box at once as one structure |
| `pollGamepad()` | `null` | gamepad state for navigation |

An example of all of it together is the `ThemesPage` page in the test mod.

## In-game HUD and live data (`hud`, `game`)

### HUD (`hud.Hud`)

`Hud.get()` is the shared HUD that the loader draws every frame on top of the game interface (Forge 1.20.1: the
`luminaui:hud` overlay above all vanilla ones, NeoForge 1.21.1: the `luminaui:hud` layer) and ticks on every client
tick. The mouse and keyboard don't reach the HUD. An empty HUD costs nothing: the frame doesn't even start.

```java
Hud hud = Hud.get();
hud.add(new SegmentedBar().bind(GameData.health()).bindMax(GameData.maxHealth()), HudAnchor.BOTTOM_CENTER)
        .offset(0, 52)                       // inward from the anchored edge
        .layer(10)                           // higher layer is on top
        .visibility(HudVisibility.GAMEPLAY); // hide on F1, F3, in chat and under screens
hud.toast("Done", "World saved");
hud.feed().post("minecraft:diamond", "+1 diamond", Toast.SUCCESS);
hud.remove(element);
```

`HudAnchor` anchors: `TOP_LEFT`, `TOP_CENTER`, `TOP_RIGHT`, `CENTER_LEFT`, `CENTER`, `CENTER_RIGHT`,
`BOTTOM_LEFT`, `BOTTOM_CENTER`, `BOTTOM_RIGHT`. The offset points **inward**, like `right: 4px` in CSS: for right
anchors a positive `dx` moves left, for bottom anchors a positive `dy` moves up. An element that changes size stays
pinned to its edge.

`HudVisibility` rules are checked every frame against the `Platform.hudState()` mask:

| Rule | Hidden |
|---|---|
| `ALWAYS` | never |
| `DEFAULT` (the HUD-wide rule) | on F1 and on the F3 debug screen |
| `GAMEPLAY` | on F1, F3, in chat and under any open screen |
| `.hideWithGui/hideInDebug/hideInChat/hideInScreens(bool)` | your own combination |
| `.when(() -> condition)` | an extra show condition (keep it cheap: read every frame) |

Your own HUD-wide rule: `hud.setDefaultVisibility(...)`; an element's own visibility (`setVisible`) is left alone,
since the rule applies to a wrapper. `hud.setTheme(theme)`, `hud.setScale(1.25f)`, `hud.onClientTick(runnable)`.
You can draw your own `new Hud()` instance yourself: `hud.render(canvas, w, h, partialTick)`.

### HUD widgets

| Widget | What it is |
|---|---|
| `Toast` | a notification: item icon, title, up to 3 lines of text, a progress bar, a lifetime (a bar at the bottom). `Toast.info/success/warning/error(...)`, `.icon(id)`, `.timeout(sec)` (0 means until `dismiss()`), `.progress(0..1)` / `.bindProgress(signal)`, `.onClose(...)`. Slides in from the nearest edge, leaves and collapses; time stops while hovered |
| `ToastStack` | a stack of toasts: `maxVisible(n)` (extra ones wait in a queue), `newestOnTop`, `slideFrom(±1)`, `dismissAll()`. The shared one is `hud.toasts()` (top right) |
| `NotificationFeed` | a compact feed of lines with a colored tag and an icon, fading after `lifetime` seconds, at most `capacity`. The shared one is `hud.feed()` (middle left) |
| `Countdown` + `TimerDisplay` | countdown / stopwatch on the real clock (`start/pause/toggle/reset/restart/add`, `onFinish`); the widget shows "M:SS", with `tenthsBelow(5)` tenths, and turns red and pulses for the last `warnBelow` seconds |
| `SegmentedBar` | a bar made of segments like hearts: `segments`, `segmentSize`, `color`, `lowBelow` (blinks), damage trail |
| `BossBar` | a wide bar with a title, a `colors(from, to)` gradient and `notches(6/10/12/20)` |
| `RadialGauge` | a 270° (`sweep`) circular gauge with a number or a percentage (`percent(true)`) and a label |

All bars take any numeric signal (`bind(ReadableSignal<? extends Number>)`), unsubscribe when they leave the tree,
and draw without allocating memory per frame (strings only when the value changes).

Theme classes (in all built-in themes; for a theme built from scratch use `HudStyles.install(theme)`): `toast`,
`hud-bar`, `hud-feed`, `hud-timer`, `hud-panel` (a translucent backing for a group). Custom properties:
`HudStyles.TRACK_COLOR` (empty part of bars), `LOW_COLOR` (low / time running out), `ROW_COLOR` (feed rows).

### Game data (`game.GameData`)

Signals that the loader updates on every client tick via `Platform.pollGameData` (they notify only on a real
change): `inWorld`, `health`, `maxHealth`, `healthFraction`, `absorption`, `food`, `foodFraction`, `saturation`,
`armor`, `air`, `maxAir`, `airFraction`, `xpLevel`, `xpProgress`, `x/y/z` (exact), `blockX/Y/Z`, `position`
(`"12 64 -30"`), `yaw`, `pitch`, `facing`, `biome`, `dimension`, `dayTime` (0–23999), `clock` (`"06:00"`), `fps`,
`selectedSlot`, `selectedItem`, `selectedCount`, `inventory` (an `Inventory` snapshot: `itemId(i)`, `count(i)`,
`total(id)`, `contains(id)`; a new object only on change). Without a world the values are those of a new player.

```java
Label.bound(GameData.position());
GameData.inventory().subscribe(inv -> diamonds.set(inv.total("minecraft:diamond")));
GameData.setSource(out -> { out.health = 7; return true; });   // your own source (tests, replay)
```

Subscriptions to the static `GameData` signals live until you close them: in widgets, subscribe in `onAttach` and
close in `onDetach` (all library widgets do this), not via `map(...)` every time a page is built.

In markup: `Bindings.gameData()` (or `bindings.withGameData()`) with short names from `GameData.named()`.

```html
<label>❤ {{health}} / {{maxHealth}} · food {{food}} · {{pos}} · {{time}} · {{biome}}</label>
<progress value="{{healthFraction}}"/>
```

An example of everything together is the `HudPage` page and the `HudDemo` demo HUD in the test mod.

## Tools: blur, clipping, GIF, maps, particles, inspector, gamepad, settings

Everything together: the `ToolsPage` page in the test mod.

### Backdrop blur (`backdrop-blur`)

`Styles.BACKDROP_BLUR` (radius in pixels, animatable) blurs what is already drawn under the element; the background,
border and children are drawn on top, so a translucent background gives a "glass" look.

```java
card.style(b -> b
        .set(Styles.BACKDROP_BLUR, 8f)
        .set(Styles.BACKGROUND, Brush.solid(0x30FFFFFF))
        .set(Styles.RADIUS, Radii.all(12)));
```

```css
.glass { backdrop-blur: 8; }                 /* or */
.glass { backdrop-filter: blur(8px); }       /* none turns it off */
```

Canvas: `Canvas.drawBackdropBlur(x, y, w, h, radii, radius)` and `supportsBackdropBlur()`. A backend that can copy
the frame overrides the `AbstractCanvas` hook `captureBackdrop(radius)`, a blurred copy of the whole frame (UV (0,0)
is the top-left of the screen); the core cuts the element's shape out of it. In game the frame is copied into its own
buffer and blurred with the vanilla `minecraft:shaders/post/blur.json` chain (in 1.21.1 the radius is passed to the
shader, in 1.20.1 it is fixed in the chain file). If copying isn't available or something fails, a frosted fallback
`Canvas.frostedFallback` is drawn: a light vertical gradient whose opacity grows with the radius, and a highlight on
top. Nothing breaks, in tests or on other backends.

### Exact rounded clipping (`clip-rounded`)

`Styles.CLIP_ROUNDED` together with `Styles.RADIUS` clips children to the rounded shape instead of a rectangle. It
works on any element that clips its children (`setClipChildren(true)`), including `ScrollPanel`; on `Image` the same
property turns on an exact mask instead of slicing the texture into strips.

```java
new ScrollPanel(list).style(b -> b.set(Styles.RADIUS, Radii.all(14)).set(Styles.CLIP_ROUNDED, true));
```

Canvas: `clipRoundRect(x, y, w, h, radii)`; by default it is a plain `clipRect`, and `supportsRoundClip()` says
whether an exact mask is available. `AbstractCanvas` does it without a stencil: before the children it takes a copy
of the frame (`captureSnapshot(slot)`), and on the matching `restore()` it repaints the four corners outside the arc
from it with triangle fans. At most `snapshotSlots()` rounded clips can be open at once (8 in game); deeper ones fall
back to rectangular clipping. The cost is one frame copy per such element per frame.

### Animated GIF (`render.Gif`, `widget.AnimatedImage`)

For multi-frame GIFs, `Images.fromBytes/fromFile/fromResource` return an `AnimatedTexture`: frames are read by
`javax.imageio` (respecting frame disposal), each frame is uploaded via `Platform.createTexture` under the key
`<key>/<number>`. Delays come from the file (0–10 ms count as 100 ms, like in browsers), the loop count from the
NETSCAPE block (no block means one pass). A regular `Image` plays such a texture by itself. `AnimatedImage` gives
control:

```java
AnimatedImage gif = AnimatedImage.resource("mymod:textures/gui/loading.gif")
        .size(48, 48)
        .loop(AnimatedImage.Loop.FOREVER)   // AS_FILE as in the file, ONCE a single pass
        .speed(1.5f)
        .onFrame(i -> ...);
gif.pause(); gif.seek(0); gif.play();
```

`Gif.encode(frames, delayMs, loopCount)` builds a GIF from `Images.Pixels` (handy for tests and images drawn by
code), `Gif.decode(bytes)` returns frames without uploading textures. Limits: at most 512 frames and a reasonable
total pixel count.

**Video is not supported.** The core has no video decoder (`javax.imageio` can't do it), and FFmpeg/JavaCV means
tens of megabytes of native libraries per platform, which is too heavy for a UI library. Save a short animation as
a GIF or a set of frames instead.

### Minimap and world map (`widget.MiniMap`, `widget.WorldMap`, `map` package)

```java
MiniMap map = new MiniMap()
        .source(MapSource.platform().or(new DemoMap(42)))  // the game map in a world, a demo without one
        .shape(MiniMap.Shape.ROUND)     // or SQUARE
        .rotate(true)                   // follows the player's view; false keeps north up
        .zoom(1.5f).zoomRange(0.5f, 6f) // pixels per block; the mouse wheel changes the zoom
        .showNorth(true).showCoordinates(true)
        .marker("home", 0, 0, 0xFFFFE08A, "Home");
```

The source is a `MapSource`: `MapSample sample(int radius)` with a grid of ARGB colors and the player's position and
rotation. `MapSource.platform()` uses `Platform.sampleMap(radius)`: the loader reads the top block's color
(`BlockState.getMapColor`) through `map.MapGridSampler`; the grid is cached and shifts only when the player moves
away, one update reads at most `budget` columns and no more often than `interval`; slopes are shaded like on the
vanilla map. Your own source (a server, a base plan) is any `MapSource` implementation. The map texture is uploaded
via `createTexture` no more often than `uploadInterval(...)` and only when the grid changed; without textures the map
is drawn as cells. Markers beyond the edge stick to the edge and lose their label.

`WorldMap(provider, centerX, centerZ, size)` is a static map of an area from `MapColorProvider.colorAt(x, z)`: colors
are built once (`refresh()` rebuilds), dragging moves the view, the wheel zooms.

### Particles behind a menu (`Backdrop.Kind.PARTICLES`)

```java
new Backdrop(Backdrop.Kind.PARTICLES)
        .particles(Backdrop.ParticleStyle.SNOW)  // SPARKS, SNOW, DUST
        .particleCount(90)
        .repel(60f, 900f)                        // cursor repel radius and strength
        .speed(1f);
```

Sparks rise and fade (drawn with additive blending), snow falls and sways, dust drifts slowly. The cursor pushes
particles away, then they smoothly return to their speed. Your own colors: `colors(...)`.

### UI inspector (`root.inspector()`)

```java
root.inspector().enabled(true);         // now F8 opens and closes it
root.inspector().toggleKey(Keys.F12);   // a different key
root.inspector().open();
```

Off by default. An open inspector captures the mouse: hovering highlights an element (margins, bounds, padding) with
a `Class.style-classes#id` label, a click selects, a right click clears the selection. A side panel shows the element
tree (click a row to select, the wheel scrolls) and details of the selected element: Java class, position and size,
padding/margin, flags and the computed style (only differences from the defaults). Arrow keys walk the tree (up goes
to the parent), Esc closes, the header buttons close the panel and move it to the other side.

### Gamepad (`root.gamepad()`, `UiRoot.focusDirection`)

The root polls `Platform.pollGamepad()` every frame (in game, the first GLFW gamepad):

| Button | Action |
|---|---|
| D-pad, left stick | move focus in a direction (`focusDirection`), with auto-repeat while held |
| A | press: Enter to the focused element, and if it doesn't take it, a click at its center |
| B | Escape (closes a popup), otherwise `onBack`: the loader screen closes |
| LB / RB | neighboring tab of the nearest `Tabs` / `SideNav` (`TabSwitchable`) |

`focusDirection(NavDirection)` works without a gamepad too: it picks the nearest focusable element in that direction
(distance along the direction plus a penalty for sideways offset), or the top-left one without focus. Focus that came
from the keyboard or gamepad scrolls a `ScrollPanel` to the element (`UiElement.revealChild`). Settings: `enabled`,
`deadzone`, `repeat(delay, rate)`, `onBack(...)`.

### Settings screen from a description (`config.ConfigSpec`, `config.ConfigScreen`)

```java
ConfigSpec spec = ConfigSpec.builder("mymod/client.toml")
        .header("MyMod settings")
        .category("hud", "Interface").icon("minecraft:compass")
        .bool("showMinimap", "Minimap", true).comment("Show in the corner of the screen")
        .integer("minimapSize", "Size", 96, 48, 192).step(8)
        .number("opacity", "Opacity", 0.85f, 0.2f, 1f).step(0.05f)
        .key("zoomKey", "Zoom", Keys.Z)
        .category("look", "Look")
        .choice("quality", "Quality", Quality.MEDIUM, Quality.values()).labels(q -> ...)
        .color("accent", "Accent", 0xFF5B8CFF).alpha(false)
        .string("greeting", "Greeting", "Hello!").maxLength(24)
        .build();
spec.load();                                   // Platform.readFile
boolean on = spec.bool("showMinimap").peek();  // values are signals
new ConfigScreen(spec).title("MyMod").autoSave(true);
```

No reflection: every value is described explicitly and stored in a `Signal`. `set` coerces the value to a valid one
(range, length, option, no alpha). `ConfigScreen` builds side navigation by category and rows: `Toggle`,
`SliderField`, `TextField`, `Select`, `ColorField`, `KeyBindField`, each with a reset-to-default button; search looks
through names, keys, comments and categories; Reset all, Save and auto-save. The file is simple TOML: `[category]`
sections, `key = value`, comments with the label and range; unknown and broken lines are skipped when reading.
