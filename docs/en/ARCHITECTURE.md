# Lumina UI: architecture

## Core idea

The whole UI (widget tree, layout, styles, animations, events) lives in the `core` module,
pure Java 17 without a single Minecraft class. Loader modules are thin: they only implement
primitive drawing, input forwarding and the mod entry point.

```
core/                    pure Java 17, unit tests without launching the game
forge-1.20.1/            Forge 47, Java 17  (ModDevGradle legacyforge)
  src/main/              the luminaui library
  src/testmod/           the luminatest test mod (entry points)
neoforge-1.21.1/         NeoForge 21.1, Java 21 (ModDevGradle)
  src/main/, src/testmod/
testmod-common/          showcase: test mod pages on the core API + a run without the game
```

The `core` sources are compiled straight into each loader's jar, so the output is one mod
with no nested libraries. Adding a new game version means copying a loader module
(~7 small files) and adjusting the rendering API.

## Core layers (`io.github.ssbaxys.lumina`)

| Package | What it does |
|---|---|
| `render` | `Canvas` (drawing interface for widgets), `AbstractCanvas` (transform stack, clip, opacity, culling), `Brush` (solid and gradients), `ColorStops`, `ColorSpace`, `Gradients`, `BlendMode`, `Radii`, `Shadow`, `Outline`, `Color` |
| `layout` | Two-pass measure/arrange layout: `FlexLayout` (flexbox: gap, grow/shrink, justify, align), `StackLayout` (layers), absolute positioning, `Dim` (auto / px / % / fill) |
| `node` | `UiElement` (tree node), `UiRoot` (root: hover, presses, focus, Tab navigation, event dispatch, frame) |
| `style` | `StyleKey` (typed properties with dense ids), `Style` (values + variants for hover/pressed/focused/disabled/checked), `ComputedStyle` (current values, per-property transitions, animation layer), `Theme` (class styles and `ThemeVar`/`Tokens` tokens), `Transition` |
| `anim` | `Animator` (keeps only active animations), `AnimatedFloat` (tween or spring), `Easing` (23 curves, cubic-bezier, steps, spring), `Interpolator`, `Motion` (timeline: sequence/parallel/delay/call), `Keyframes`, `Tween`, `Playback` (loops, yoyo, speed) |
| `event` | Typed events with capture and bubble phases: mouse, wheel, keyboard, characters, focus |
| `reactive` | `Signal`, `Computed` (automatic dependency tracking) for binding data to widgets |
| `widget` | `Widget<S>` (type-safe fluent API), `Panel`, `Label`, `Button`, `ScrollPanel`, `Toggle`, `Slider`, `ProgressBar`, `RingProgress`, `ColorPicker` |
| `fx` | `FxLayer` (effects layer), `Particles` (particles in flat arrays), `SpriteSheet`, `VanillaFx` (fire, explosion, TNT, lava, smoke, shake on vanilla textures) |
| `markdown` | `MarkdownParser` → `Md` tree → `MarkdownLayout` (flat draw operations) → `Markdown` widget |
| `i18n` | `I18n`: current language as a signal, live `tr(...)` translations, tables from code and the game's lang files, language list; `LanguageButton` is a ready switcher |
| `platform` | `Platform`: everything the core needs from the game (font metrics, clipboard, sound, time, language and lang files, settings) |

## How it meets the requirements

**No reflection.** The platform is passed explicitly (`Lumina.init(platform)`), without ServiceLoader.
Style properties and event types get integer ids on registration, so values
and listeners live in arrays by index. Extending (your own properties, events, layouts,
widgets) is plain code: `StyleKey.builder(...).register()`, `EventType.create(...)`,
`implements Layout`, `extends Widget<MyWidget>`.

**Performance.**
- Layout is cached and recomputed only after `invalidateLayout()`, and only along the ancestor chain.
- The style for each state combination is computed once and cached in `Style`; a hover change costs O(1).
- Inherited properties (text color) are not copied down the tree but read lazily from the parent.
- `AbstractCanvas` keeps its state in a flat float array, drops invisible primitives and
  changes the scissor only when the clip really changes. A frame is drawn with almost no allocations.
- Plain rectangles are collected into one buffer and drawn in one call.
- The animator is empty at rest: no work if nothing moves.
- A subtree that is fully invisible (off screen or outside a scroll area) is not traversed at all.
  Elements that draw outside their bounds get `setOverflowsBounds(true)`.

**Looks.** Rounded corners (different radius per corner), borders and soft shadows are drawn by an SDF shader
(`assets/luminaui/shaders/core/rounded_rect.*`) with anti-aliasing of exactly one physical pixel
at any GUI scale. Gradients, opacity and scale are inherited down the tree. Transitions between
states (hover, press, focus) are animated from the theme style.

**Flexibility.** The theme sets styles by widget class (several classes allowed), the inline style goes on top. Styles reference theme tokens, so changing one token smoothly recolors the whole UI. Details: [STYLING.md](STYLING.md). Screens can be described in HTML and CSS with logic bindings: [MARKUP.md](MARKUP.md). Any
layout and any widget can be replaced with your own. Signals bind data to the UI.

## Languages

Language switching is built in and works on the fly: the current language is a signal, so an open screen
is translated immediately, without being recreated.

```java
// 1. Translations: the mod's lang files (assets/<mod>/lang/en_us.json, ru_ru.json) are picked up automatically.
//    You can also set them from code:
I18n.translations("en_us").add("mymod.clicks", "Clicks: %s");
I18n.translations("ru_ru").add("mymod.clicks", "Нажатий: %s");
I18n.declareLanguage("en_us");   // which languages to offer in the switcher
I18n.declareLanguage("ru_ru");   // (tables from code declare themselves)

// 2. Translated widgets; a signal argument is inserted and updates by itself.
Label.tr("mymod.clicks", clicks);
Button.tr("mymod.close").onPress(onClose);
new LanguageButton();            // ready button: next language in a cycle

// 3. Control from code.
I18n.setLanguage("ru_ru");       // own language, independent of the game's; the choice is saved
I18n.followGameLanguage();       // follow the game again (default behavior)
```

- Lookup order: tables from code → custom sources (`I18n.addSource`) → the game's lang files;
  if a key is missing in the current language, `en_us` is used, then the key itself.
- `I18n.get(key, args)` translates "now"; inside `Computed`/`I18n.text(...)` it is recomputed when the language changes.
- Bindings are lazy (`Derived`): a widget listens to the language only while it is in the tree, so there are no leaks.
- The choice is stored in `config/luminaui.properties`; a resource reload (F3+T, changing the game language) re-reads translations.

## Example

```java
Signal<Integer> clicks = Signal.of(0);

Panel window = Panel.column().gap(10).width(Dim.px(260)).add(
        new Label("Lumina UI").scale(1.5f),
        Label.bound(clicks.map(c -> "Clicks: " + c)),
        new Button("Click me", () -> clicks.update(c -> c + 1))
                .style(s -> s.set(Styles.RADIUS, Radii.all(12))));

Minecraft.getInstance().setScreen(new LuminaScreen(Component.literal("Example"),
        screen -> new UiRoot(window)));
```

The full feature showcase is the `luminatest` test mod (see README): the K key, `/luminatest`
or the "Lumina" button in the menu. A small demo of the library itself: `/lumina demo`.

## Next steps

- SDF shape batching: a custom vertex format with radii in attributes, to draw all shapes in one call.
- Widgets: text field, dropdown, tooltips, modal windows, grid.
- Nine-slice textures.
- Loading themes from JSON (resource packs), hot reload.
- Embedding into other screens (overlay on top of `AbstractContainerScreen`) and HUD.
