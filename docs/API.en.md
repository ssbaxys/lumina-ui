# Lumina UI: developer guide

This guide is for mod authors who want to build interfaces with Lumina UI: how to add the library, open your first
screen, what the API is made of and how to extend it. Русская версия: [API.md](API.md).

In-depth references (in Russian, with code examples that read the same in any language):

- [STYLING.md](STYLING.md) — colors, gradients, themes, animations, effects, Markdown, every widget, HUD, inventory, 3D;
- [MARKUP.md](MARKUP.md) — interfaces in HTML and CSS with logic bindings;
- [ARCHITECTURE.md](ARCHITECTURE.md) — how the library works inside.

## Contents

1. [Supported versions](#supported-versions)
2. [Adding Lumina to your mod](#adding-lumina-to-your-mod)
3. [Your first screen](#your-first-screen)
4. [Core concepts](#core-concepts)
5. [Layout](#layout)
6. [Styles and themes](#styles-and-themes)
7. [Data and signals](#data-and-signals)
8. [Events and input](#events-and-input)
9. [Animations](#animations)
10. [Widget catalog](#widget-catalog)
11. [HTML and CSS](#html-and-css)
12. [Languages](#languages)
13. [In-game HUD](#in-game-hud)
14. [Server-synced container screens](#server-synced-container-screens)
15. [Config screen](#config-screen)
16. [Extending the library](#extending-the-library)
17. [Rules and tips](#rules-and-tips)

## Supported versions

| Loader | Minecraft | Java | Loader package |
|---|---|---|---|
| Forge 47.x | 1.20.1 | 17 | `io.github.ssbaxys.lumina.forge` |
| NeoForge 21.1.x | 1.21.1 | 21 | `io.github.ssbaxys.lumina.neoforge` |

Almost the whole API (`io.github.ssbaxys.lumina.*`: widgets, layout, styles, animations, markup, HUD) is identical on
both versions: it lives in a pure-Java core that does not touch Minecraft classes. You can keep your UI code in a
shared module and build it for both versions unchanged. Only a few classes depend on the loader: the
`LuminaScreen` screen and the container screens `LuminaMenuType` / `LuminaMenuScreen`.

The library is installed on both client and server. The server only needs it for container screens
(`LuminaMenu`); everything else runs on the client, and client classes are never loaded on a dedicated server.

## Adding Lumina to your mod

### Gradle

Get the library jar from the Modrinth Maven repository. Replace `<version>` with the version number shown on the
file's Modrinth page (each loader has its own file).

```groovy
repositories {
    exclusiveContent {
        forRepository { maven { url = "https://api.modrinth.com/maven" } }
        filter { includeGroup "maven.modrinth" }
    }
}

dependencies {
    // NeoForge 1.21.1 (ModDevGradle)
    implementation "maven.modrinth:lumina-ui:<version>"

    // Forge 1.20.1 (ModDevGradle legacyforge)
    modImplementation "maven.modrinth:lumina-ui:<version>"

    // Forge 1.20.1 (ForgeGradle)
    implementation fg.deobf("maven.modrinth:lumina-ui:<version>")
}
```

You can also skip the repository: put the jar into `libs/` and add `files("libs/luminaui-....jar")` with the same
configuration.

If your shared UI code lives in a separate module without Minecraft, `compileOnly` on the same jar is enough:
the core classes inside are identical for both loaders.

### Mod metadata dependency

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

If your mod only uses Lumina on the client, `side = "CLIENT"` is fine.

### Initialization

There is nothing to call. The `luminaui` mod calls `Lumina.init(...)` itself when the client starts, and registers
its shaders, the HUD and translation reloading. `Lumina.isInitialized()` tells you whether it is ready.

## Your first screen

```java
import io.github.ssbaxys.lumina.forge.LuminaScreen;      // NeoForge: io.github.ssbaxys.lumina.neoforge.LuminaScreen
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
                    Label.bound(clicks.map(c -> "Clicks: " + c)),
                    new Checkbox("Music", music),
                    new Slider(volume, 0, 100),
                    new Button("Press me", () -> clicks.update(c -> c + 1)),
                    new Button("Close", screen::onClose));
            // full-screen layer with the window in the middle
            Panel root = Panel.box(StackLayout.centered()).fill().add(window);
            return new UiRoot(root, ModernTheme.dark());
        }));
    }
}
```

- `LuminaScreen` is a regular Minecraft `Screen` that hosts a `UiRoot`. All input goes to the UI, and whatever the
  UI does not handle (Esc, for example) falls through to the normal screen logic.
- The factory receives the screen itself, handy for a Close button (`screen::onClose`).
- `.parent(previousScreen)` sets the screen to return to on close; `.pauseGame(true)` pauses a singleplayer game.
- The root's content is placed in the top-left corner at its own size. Call `.fill()` to stretch it over the whole
  screen, or wrap a window in `Panel.box(StackLayout.centered()).fill()` to center it, as above.

A small demo of the library itself opens with `/lumina demo`. The full showcase is the `luminatest` test mod
(press K or run `/luminatest`); its sources are in `testmod-common/`.

## Core concepts

| Class | Role |
|---|---|
| `UiElement` | A tree node: children, layout, style, states (hover, pressed, focused, disabled, checked), events, animations. |
| `Widget<S>` | Base of every widget: a type-safe fluent API (`.width(...)`, `.style(...)`, `.on(...)`, `.tooltip(...)`) where each call returns the widget itself. |
| `UiRoot` | The tree root: theme, animator, focus and Tab navigation, popup layer (menus, tooltips), accessibility, gamepad, inspector. |
| `Theme` | Styles per widget class plus tokens (accent, surface, corner radius, animation speed). |
| `Signal<T>` | An observable value: widgets bind to it and update themselves. |
| `Canvas` | Drawing interface for your own widgets: rounded rectangles, shadows, gradients, text, textures, items, entities. |

Common `Widget` methods:

```java
new Button("OK")
        .id("ok")                          // later: root.content().findById("ok")
        .size(120, 20)                     // or .width(Dim.px(120)).height(Dim.percent(0.5f)), .fill()
        .minSize(80, 20).maxSize(200, 40)
        .grow(1).shrink(0)                 // share of free space in flex
        .margin(Insets.all(4))
        .alignSelf(Align.END)
        .absolute(10, 10)                  // out of layout flow, from the parent's top-left corner
        .styleClass("button primary")      // space-separated theme classes
        .style(s -> s.set(Styles.RADIUS, Radii.all(12)))
        .tooltip("Hint")
        .enabled(false).visible(true)
        .on(PointerEvent.ENTER, e -> {})
        .with(b -> b.setFocusable(true));  // any setup without breaking the chain
```

## Layout

Layout runs in two passes (measure, then arrange) and is recomputed only after something changes.

- `Panel.column()` / `Panel.row()` — flexbox: `.gap(px)`, `.justify(Justify.SPACE_BETWEEN)`, `.align(Align.CENTER)`.
  `Justify`: `START`, `CENTER`, `END`, `SPACE_BETWEEN`, `SPACE_AROUND`, `SPACE_EVENLY`.
  `Align`: `START`, `CENTER`, `END`, `STRETCH`.
- `Panel.box(layout)` — a transparent container with no theme style, just for grouping.
- `new Panel("card", StackLayout.centered())` — children stacked on top of each other (background, content, effects).
- `new GridLayout(columns, gap)` — a grid.
- `ResponsivePanel` and `root.breakpoint()` — layout that depends on screen width.
- Sizes: `Dim.AUTO` (fit content), `Dim.px(n)`, `Dim.percent(0..1)`, `Dim.FILL`.
- `ScrollPanel(content)` — vertical scrolling with smooth inertia; off-screen rows are not drawn.

Write your own layout by implementing the `Layout` interface (two methods: `measure` and `arrange`).

## Styles and themes

A style is a set of typed properties (`StyleKey`) plus variants for states. The theme stores styles per class, and a
widget's inline style is applied on top.

```java
button.style(s -> s
        .set(Styles.BACKGROUND, Brush.vertical(0xFF3A7BFF, 0xFF2456C8))
        .set(Styles.RADIUS, Radii.all(8))
        .set(Styles.SHADOW, Shadow.of(2, 8, 0x80000000))
        .hovered(h -> h.set(Styles.SCALE, 1.05f).set(Styles.GLOW, Shadow.of(0, 10, 0x805B8CFF)))
        .pressed(p -> p.set(Styles.SCALE, 0.97f))
        .set(Styles.TRANSITION, Transition.of(0.2f, Easing.OUT_CUBIC)));
```

Themes: `Theme.dark()`, `Theme.light()`, `ModernTheme.dark(accent)`, `ModernTheme.light()`,
`AccessibleThemes.highContrastDark()`. Pass a theme to `new UiRoot(content, theme)` or switch it at runtime with
`root.setTheme(...)` (smoothly with `ThemeTransition`).

Tokens recolor the whole UI in one line:

```java
Theme theme = ModernTheme.dark();
theme.set(Tokens.ACCENT, 0xFFF0506E);   // accent color
theme.set(Tokens.CORNER, 4f);           // corner radius
theme.set(Tokens.MOTION, 0f);           // turn off transition animations
theme.edit("button", b -> b.set(Styles.BORDER_WIDTH, 1f));   // extend a class style
```

Everything about colors (RGB, HSV, HSL, OKLCH), gradients, blend modes, backdrop blur, NineSlice and style
properties is in [STYLING.md](STYLING.md).

## Data and signals

```java
Signal<Integer> coins = Signal.of(120);
coins.set(150);
coins.update(c -> c + 10);
int now = coins.peek();                                  // read without subscribing

ReadableSignal<String> text = coins.map(c -> c + " coins");      // derived value
Computed<Boolean> rich = new Computed<>(() -> coins.get() > 1000);

Label.bound(text);                  // the label updates itself
new Toggle(Signal.of(true));        // input widgets read from and write to a signal
Subscription sub = coins.subscribe(c -> save(c));  // sub.close() to unsubscribe
```

Widgets subscribe to signals only while they are in the tree and unsubscribe on removal, so nothing leaks.
Signals are single-threaded: change them on the render thread (for example via `Minecraft.getInstance().execute(...)`).

For large data: `VirtualList<T>` (tens of thousands of rows), `DataView<T>` (search, filters, sorting, pages),
`Table<T>`, `TreeView`.

## Events and input

Events are typed and go through two phases: capture (root to target) and bubble (target to root).

```java
panel.on(PointerEvent.CLICK, e -> select(e.localX(), e.localY()));
panel.on(ScrollEvent.SCROLL, e -> zoom(e));
field.on(KeyEvent.PRESS, e -> { if (e.key() == Keys.ENTER) { submit(); e.consume(); } });
root.content().onCapture(KeyEvent.PRESS, e -> {});   // intercept before children
```

Types: `PointerEvent` (`PRESS`, `RELEASE`, `MOVE`, `DRAG`, `CLICK`, `ENTER`, `LEAVE`), `ScrollEvent.SCROLL`,
`KeyEvent` (`PRESS`, `RELEASE`), `CharEvent.TYPED`, `FocusEvent` (`GAINED`, `LOST`).
`e.stopPropagation()` stops bubbling, `e.consume()` marks the event as handled.
Custom events: `EventType.create("my-event", true)` and `root.dispatch(target, event)`.

Focus: `setFocusable(true)`, `root.setFocus(el)`; Tab and Shift+Tab move focus; arrow keys and gamepad go through
`root.gamepad()` and `UiRoot.focusDirection(...)`.

## Animations

Any style property can be animated. Animations write to a separate layer, so hover and pressed styles keep working
underneath, and animations stop by themselves when an element is removed.

```java
box.animate(Styles.TRANSLATE_X).keyframe(0, 0f).keyframe(0.5f, 160f, Easing.OUT_BOUNCE).keyframe(1, 0f)
        .duration(2.4f).loop().play();

element.play(Motion.sequence(
        Motion.call(this::sound),
        Motion.parallel(a.animate(Styles.OPACITY).to(1f).motion(), b.animate(Styles.SCALE).to(1.2f).motion()),
        Motion.delay(0.2f),
        VanillaFx.shake(element, 4f, 0.4f)));

AnimatedFloat x = new AnimatedFloat(root.animator(), 0).spring(Spring.BOUNCY);  // for your own widgets
x.animateTo(100);
```

23 easing curves (`Easing`), `cubicBezier`, `steps`, springs. Vanilla effects (fire, explosions, TNT, snow,
particles) come from the `FxLayer` layer and `VanillaFx`, see [STYLING.md](STYLING.md).

## Widget catalog

| Group | Widgets |
|---|---|
| Basics | `Panel`, `Label`, `Button`, `ImageButton`, `Icon`, `Image`, `AnimatedImage` (GIF), `FancyText` |
| Input | `TextField`, `NumberField`, `ColorField`, `Slider`, `SliderField`, `Toggle`, `Checkbox`, `RadioGroup`, `Select`, `KeyBindField`, `DatePicker`, `CalendarView`, `ColorPicker` |
| Forms | `Form`, `FormField`, `Validator`, `Wizard`, `Stepper` |
| Navigation | `Tabs`, `SideNav`, `Pages`, `Paginator`, `Collapsible`, `ScrollPanel` |
| Popups | `ContextMenu`, `Tooltip` / `Tooltips`, `RadialMenu`, `Window` (drag, resize, modal) |
| Data | `Table`, `TreeView`, `VirtualList`, `DataView`, `Chart` (line, area, bar, pie) |
| Indicators | `ProgressBar`, `RingProgress`, `Spinner`, `Skeleton` |
| Text | `Markdown`, `Book`, `SyntaxHighlighter` |
| Game | `Slot`, `SlotGrid`, `SlotGroup`, `InventoryPanel`, `EntityView`, `ItemView`, `BlockMap`, `BlockModel`, `StructurePreview`, `MiniMap`, `WorldMap` |
| Decoration | `Backdrop` (live wallpapers), `FxLayer` (effects), `ThemeEditor`, `LanguageButton` |
| HUD | `Hud`, `SegmentedBar`, `RadialGauge`, `BossBar`, `TimerDisplay`, `Toast`, `NotificationFeed` |

Code examples for each are in [STYLING.md](STYLING.md) and on the showcase pages in `testmod-common/`.

## HTML and CSS

You can describe a screen with markup and styles and wire up logic in the markup or from Java. Markup is turned into
regular widgets once, so it costs nothing per frame.

```java
Signal<Float> volume = Signal.of(60f);
UiElement screen = MarkupView.resource("mymod:ui/settings.html", new Bindings()
                .signal("volume", volume)
                .handler("save", this::save))
        .watch(1f);                                // hot reload (F3+T)
```

```html
<link rel="stylesheet" href="mymod:ui/settings.css">
<column class="settings">
  <h2>Settings</h2>
  <row><label>Volume</label><slider bind="volume" min="0" max="100"/><label>{{volume|int}}%</label></row>
  <button class="primary" on-click="save">Save</button>
</column>
```

Files live in `assets/<mod>/...` and can be overridden by resource packs. All tags, CSS properties, actions and form
validation are in [MARKUP.md](MARKUP.md).

## Languages

The UI language is a signal, so an open screen is translated immediately, without rebuilding it.

```java
// your mod's lang files (assets/mymod/lang/en_us.json, ru_ru.json) are picked up automatically; or from code:
I18n.translations("en_us").add("mymod.clicks", "Clicks: %s");

Label.tr("mymod.clicks", clicks);       // a signal argument is substituted and kept up to date
Button.tr("mymod.close").onPress(onClose);
new LanguageButton();                   // ready-made language switcher

I18n.setLanguage("ru_ru");              // own language independent of the game (choice is saved)
I18n.followGameLanguage();              // follow the game again (default)
```

Lookup order: tables from code, custom sources (`I18n.addSource`), the game's lang files; if a key is missing,
`en_us` is used, then the key itself.

## In-game HUD

```java
Hud hud = Hud.get();
hud.add(new SegmentedBar().bind(GameData.health()).bindMax(GameData.maxHealth()), HudAnchor.BOTTOM_CENTER)
        .offset(0, 52)
        .layer(10)
        .visibility(HudVisibility.GAMEPLAY);   // hide on F1, F3, in chat and under screens
hud.toast("Done", "World saved");
hud.feed().post("minecraft:diamond", "+1 diamond", Toast.SUCCESS);
```

`GameData` provides live game signals: health, food, armor, air, experience, position, biome, FPS and more.
An empty HUD costs nothing. Details: the HUD section of [STYLING.md](STYLING.md).

## Server-synced container screens

Your own chest, furnace or machine with a Lumina screen, owned by the server just like a vanilla chest.

```java
// shared code
static final MenuSpec SPEC = MenuSpec.builder()
        .container("chest", 27).playerInventory()
        .intValue("progress").boolValue("burning").build();
static final LuminaMenuType CHEST = new LuminaMenuType(SPEC);
MENUS.register("chest", CHEST::create);              // DeferredRegister<MenuType<?>>

// server
CHEST.open(player, Component.literal("Chest"), container, index -> progress, (p, action, value) -> sort());

// client: Forge in FMLClientSetupEvent.enqueueWork, NeoForge in RegisterMenuScreensEvent
LuminaMenuScreen.register(CHEST, menu -> new UiRoot(InventoryPanel.chest(menu.title(),
        menu.inventory("chest"), menu.playerInventory(), Slot.Look.MODERN), ModernTheme.dark()));
```

The game syncs the slots, numbers go through `DataSlot`, and your own buttons use `menu.sendAction(id, value)` with no
custom network channel. Details: the container screens section of [STYLING.md](STYLING.md).

## Config screen

```java
ConfigSpec spec = ConfigSpec.builder("mymod/client.toml")
        .category("hud", "Interface").icon("minecraft:compass")
        .bool("showMinimap", "Minimap", true).comment("Show in the corner of the screen")
        .integer("minimapSize", "Size", 96, 48, 192).step(8)
        .color("accent", "Accent", 0xFF5B8CFF)
        .key("zoomKey", "Zoom", Keys.Z)
        .build();
spec.load();
boolean on = spec.bool("showMinimap").peek();       // values are signals
UiElement screen = new ConfigScreen(spec).title("MyMod").autoSave(true);
```

The screen builds category navigation, search and reset-to-default by itself and saves the file to
`config/mymod/client.toml`.

## Extending the library

Everything is extended with plain code, without reflection or annotations.

### Your own widget

```java
public class StripeBar extends Widget<StripeBar> {
    public static final StyleKey<Integer> STRIPE = StyleKey.builder("stripe-color", Integer.class)
            .defaultValue(0xFFFFFFFF).animated(Interpolator.COLOR).register();

    private float value;

    public StripeBar() {
        super("stripe-bar");                     // theme class
        setFocusable(true);
    }

    public StripeBar value(float v) {
        value = v;
        return this;
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        setContentSize(120, 8);                  // natural size without padding
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

Useful `UiElement` hooks: `onAttach` / `onDetach` (signal subscriptions and animations), `onLayout` (after a size
change), `onStateChanged`, `paintBackground`, `paintForeground`, `paintOverlay`. Custom states use bits starting
at `State.FIRST_CUSTOM`.

### Everything else

| What | How |
|---|---|
| Style property | `StyleKey.builder(name, type).defaultValue(...).inherited().affectsLayout().animated(...).register()` |
| Theme token | `ThemeVar.of("brand", Integer.class, 0xFF00FF00)` |
| Event | `EventType.create("name", bubbles)` |
| Layout | `implements Layout` |
| Markup tag | `Markup.tag("card", (node, ctx) -> ...)` |
| CSS property | `CssProperties.register(...)` |
| Effect | `FxRegistry.register("mymod:effect", ...)` |
| Translation source | `I18n.addSource((lang, key) -> ...)` |

## Rules and tips

- **Threading.** The UI is single-threaded: create widgets and change signals on the client render thread.
- **Performance.** Layout and styles are cached, invisible subtrees are skipped, and the animator does nothing when
  idle. Don't rebuild the tree every frame; change signals instead.
- **Drawing outside bounds.** If a widget draws outside its bounds (glow, shadow), call `setOverflowsBounds(true)`,
  otherwise it may be culled while scrolling.
- **Debugging.** The UI inspector (`root.inspector()`, F8 in the showcase) shows the tree, sizes and styles.
- **Accessibility.** `root.accessibility()`: color-blindness correction, text scale, reduced motion, focus narration.
- **Version portability.** Code that uses `io.github.ssbaxys.lumina.*` (except the `forge` and `neoforge` packages)
  moves between Forge 1.20.1 and NeoForge 1.21.1 unchanged.
