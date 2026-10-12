package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.markup.Bindings;
import io.github.ssbaxys.lumina.markup.Markup;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.ImageTexture;
import io.github.ssbaxys.lumina.render.Images;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Checkbox;
import io.github.ssbaxys.lumina.widget.Image;
import io.github.ssbaxys.lumina.widget.ImageButton;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.RadioGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Флажки, радио-группы, кнопки с иконками и загрузкой, картинки и их теги разметки. */
class ButtonsTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    @AfterEach
    void reset() {
        TestSupport.init();
        Images.clearCache();
    }

    private static UiRoot mount(UiElement e) {
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column().align(Align.START)).fill().add(e), Theme.dark());
        root.resize(400, 300);
        root.tick(0f);
        return root;
    }

    private static void clickAt(UiRoot root, float x, float y) {
        root.pointerMove(x, y);
        root.pointerDown(x, y, Keys.MOUSE_LEFT);
        root.pointerUp(x, y, Keys.MOUSE_LEFT);
    }

    private static void click(UiRoot root, UiElement e) {
        clickAt(root, e.screenX() + e.width() / 2f, e.screenY() + e.height() / 2f);
    }

    private static TestSupport.RecordingCanvas render(UiRoot root) {
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(root.width(), root.height());
        root.render(canvas, 0f);
        canvas.end();
        return canvas;
    }

    // ---------------------------------------------------------------- флажки

    @Test
    void checkboxTogglesByBoxLabelAndKeyboard() {
        Signal<Boolean> on = Signal.of(false);
        Checkbox cb = new Checkbox("Accept", on);
        UiRoot root = mount(cb);
        assertEquals(12f + 5f + 6 * 6f, cb.measuredWidth(), 1e-4, "box + gap + label");
        float cy = cb.screenY() + cb.height() / 2f;

        clickAt(root, cb.screenX() + 4f, cy);
        assertTrue(on.peek());
        assertTrue(cb.hasState(State.CHECKED));

        clickAt(root, cb.screenX() + cb.width() - 3f, cy);
        assertFalse(on.peek(), "click on the label toggles too");
        assertFalse(cb.hasState(State.CHECKED));

        root.setFocus(cb);
        root.keyDown(Keys.SPACE, 0, 0);
        assertTrue(on.peek());
        root.keyDown(Keys.ENTER, 0, 0);
        assertFalse(on.peek());

        on.set(true);
        assertTrue(cb.hasState(State.CHECKED), "follows the signal");
        cb.setEnabled(false);
        clickAt(root, cb.screenX() + 4f, cy);
        assertTrue(on.peek(), "disabled checkbox ignores clicks");
    }

    @Test
    void indeterminateDashIsClearedByClick() {
        Signal<Boolean> on = Signal.of(false);
        Checkbox cb = new Checkbox("All", on).indeterminate(true);
        UiRoot root = mount(cb);
        root.tick(1f);
        assertTrue(cb.isIndeterminate());
        assertFalse(cb.hasState(State.CHECKED));
        assertTrue(render(root).ops.stream().anyMatch(op -> op.startsWith("freequad")), "dash is drawn");

        click(root, cb);
        assertFalse(cb.isIndeterminate());
        assertTrue(on.peek(), "indeterminate → checked");

        cb.indeterminate(true);
        root.keyDown(Keys.SPACE, 0, 0);
        assertFalse(cb.isIndeterminate());
        assertTrue(on.peek(), "already true: only the dash goes away");
        click(root, cb);
        assertFalse(on.peek());
    }

    @Test
    void everyKindAnimatesAndRenders() {
        for (Checkbox.Kind kind : Checkbox.Kind.values()) {
            Signal<Boolean> on = Signal.of(false);
            Checkbox cb = new Checkbox(kind.name(), on).kind(kind);
            UiRoot root = mount(cb);
            float w = kind == Checkbox.Kind.SWITCH_DOT ? 12f * 1.8f : 12f;
            assertEquals(w + 5f + kind.name().length() * 6f, cb.measuredWidth(), 1e-3, kind.name());
            int idle = render(root).ops.size();
            click(root, cb);
            assertTrue(on.peek(), kind.name());
            root.tick(0.05f);
            render(root);
            root.tick(1f);
            int checked = render(root).ops.size();
            assertTrue(checked > idle || kind == Checkbox.Kind.SWITCH_DOT, kind + " draws its mark");
            cb.setState(State.HOVERED, true);
            root.tick(0.05f);
            render(root);
            click(root, cb);
            assertFalse(on.peek(), kind.name());
            root.tick(1f);
            render(root);
        }
    }

    @Test
    void radioGroupIsExclusive() {
        Signal<String> mode = Signal.of("b");
        RadioGroup<String> group = RadioGroup.of(mode, "a", "b", "c").labels(String::toUpperCase).horizontal();
        UiRoot root = mount(group);
        List<Checkbox> items = group.items();
        assertEquals(3, items.size());
        assertEquals("B", items.get(1).label());
        assertEquals(items.get(0).y(), items.get(2).y(), 1e-4, "horizontal");
        assertTrue(items.get(2).x() > items.get(1).x());
        assertTrue(items.get(1).isChecked());

        click(root, items.get(2));
        assertEquals("c", mode.peek());
        assertFalse(items.get(0).isChecked());
        assertFalse(items.get(1).isChecked());
        assertTrue(items.get(2).isChecked());

        click(root, items.get(2));
        assertEquals("c", mode.peek(), "clicking the selected option keeps it");

        mode.set("a");
        assertTrue(items.get(0).isChecked());
        assertFalse(items.get(2).isChecked());

        root.setFocus(items.get(0));
        root.keyDown(Keys.RIGHT, 0, 0);
        assertEquals("b", mode.peek());
        assertEquals(items.get(1), root.focused());
        root.keyDown(Keys.UP, 0, 0);
        assertEquals("a", mode.peek());

        group.vertical();
        root.tick(0f);
        root.layoutIfNeeded();
        assertTrue(items.get(1).y() > items.get(0).y(), "vertical");
    }

    // ---------------------------------------------------------------- кнопки

    @Test
    void buttonIconWidensLayout() {
        Button plain = new Button("OK");
        Button item = new Button("OK").item("minecraft:diamond");
        Button texture = new Button("OK").icon(TextureRef.vanilla("item/diamond")).iconSize(16);
        Button round = Button.round(TextureRef.vanilla("item/apple"), null);
        Button roundItem = Button.roundItem("minecraft:compass", null).iconSize(20);
        UiRoot root = mount(Panel.box(FlexLayout.row().gap(4)).add(plain, item, texture, round, roundItem));
        assertEquals(12f + 28f, plain.measuredWidth(), 1e-4);
        assertEquals(plain.measuredWidth() + 12f + 4f, item.measuredWidth(), 1e-4);
        assertEquals(plain.measuredWidth() + 16f + 4f, texture.measuredWidth(), 1e-4);
        assertEquals(16f + 12f, round.measuredWidth(), 1e-4, "round icon button: icon + padding 6");
        assertEquals(round.measuredWidth(), round.measuredHeight(), 1e-4);
        assertEquals(20f + 12f, roundItem.measuredWidth(), 1e-4);
        assertTrue(round.styleClass().contains("icon"));

        List<String> ops = render(root).ops;
        assertTrue(ops.contains("item minecraft:diamond"));
        assertTrue(ops.contains("tex minecraft:textures/item/diamond.png"));
        assertTrue(ops.contains("tex minecraft:textures/item/apple.png"));
    }

    @Test
    void loadingDisablesButton() {
        int[] presses = {0};
        Button b = new Button("Save", () -> presses[0]++);
        UiRoot root = mount(b);
        float w = b.measuredWidth();

        b.loading(true);
        root.layoutIfNeeded();
        assertTrue(b.isLoading());
        assertFalse(b.isEnabled());
        assertTrue(b.hasState(State.DISABLED));
        assertEquals(w + 12f + 4f, b.measuredWidth(), 1e-4, "spinner takes the icon slot");
        click(root, b);
        root.keyDown(Keys.ENTER, 0, 0);
        b.press();
        assertEquals(0, presses[0]);
        assertTrue(render(root).ops.stream().anyMatch(op -> op.startsWith("shape")), "spinner arc");

        b.loading(false);
        assertTrue(b.isEnabled());
        click(root, b);
        assertEquals(1, presses[0]);

        b.enabled(false).loading(true).loading(false);
        assertFalse(b.isEnabled(), "loading restores the previous availability");
        b.enabled(true);

        Signal<Boolean> busy = Signal.of(false);
        Button bound = new Button("Send").loading(busy);
        mount(bound);
        assertFalse(bound.isLoading());
        busy.set(true);
        assertTrue(bound.isLoading());
        assertFalse(bound.isEnabled());
        busy.set(false);
        assertTrue(bound.isEnabled());
    }

    @Test
    void buttonVariantsComeFromTheme() {
        Theme theme = Theme.dark();
        for (String v : new String[]{"primary", "outline", "ghost", "pill", "gradient", "danger", "success", "icon", "big"}) {
            assertNotNull(theme.rawStyle("button." + v), v);
            assertNotEquals(theme.style("button"), theme.style("button " + v), v);
        }
        assertNull(theme.style("icon").get(Styles.PADDING), "modifier only applies together with button");
        Button big = new Button("Big").styleClass("button big");
        Button normal = new Button("Big");
        mount(Panel.box(FlexLayout.row()).add(big, normal));
        assertTrue(big.measuredWidth() > normal.measuredWidth() + 10f, "big: larger padding and font");
        Button gradient = new Button("Shine").styleClass("button gradient");
        UiRoot root = mount(gradient);
        Brush idle = gradient.computedStyle().get(Styles.FOREGROUND);
        assertInstanceOf(Brush.Linear.class, idle);
        gradient.setState(State.HOVERED, true);
        root.tick(0f);
        root.tick(2f);
        assertNotEquals(idle, gradient.computedStyle().get(Styles.FOREGROUND), "hover shine moves");
        assertNotNull(Theme.light().rawStyle("checkbox"));
    }

    @Test
    void imageButtonZoomsOnHover() {
        int[] presses = {0};
        ImageButton card = new ImageButton(TextureRef.vanilla("block/dirt"), "A very long caption for a card",
                () -> presses[0]++).size(60, 40);
        UiRoot root = mount(card);
        List<String> ops = render(root).ops;
        assertTrue(ops.stream().filter(op -> op.equals("tex minecraft:textures/block/dirt.png")).count() > 1,
                "rounded clip is drawn as strips");
        assertTrue(ops.stream().anyMatch(op -> op.startsWith("text ") && op.contains("...")), "caption is shortened");
        card.setState(State.HOVERED, true);
        root.tick(1f);
        render(root);
        click(root, card);
        assertEquals(1, presses[0]);
    }

    // ---------------------------------------------------------------- картинки

    private static BufferedImage sample() {
        BufferedImage img = new BufferedImage(3, 2, BufferedImage.TYPE_INT_ARGB);
        int[] px = {0xFFFF0000, 0x8000FF00, 0xFF0000FF, 0xFFFFFFFF, 0x00000000, 0xFF123456};
        for (int i = 0; i < px.length; i++) {
            img.setRGB(i % 3, i / 3, px[i]);
        }
        return img;
    }

    private static byte[] encode(BufferedImage img, String format) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(img, format, out), format);
        return out.toByteArray();
    }

    private static BufferedImage opaque(int w, int h, int rgb) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }

    @Test
    void imagesDecodePngJpgGifBmp() throws IOException {
        Images.Pixels png = Images.decode(encode(sample(), "png"));
        assertEquals(3, png.width());
        assertEquals(2, png.height());
        assertArrayEquals(new int[]{0xFFFF0000, 0x8000FF00, 0xFF0000FF, 0xFFFFFFFF, 0x00000000, 0xFF123456}, png.argb());
        assertNotNull(System.getProperty("java.awt.headless"), "headless is set before ImageIO is used");

        Images.Pixels jpg = Images.decode(encode(opaque(8, 8, 0x3366CC), "jpg"));
        assertEquals(8, jpg.width());
        int c = jpg.pixel(4, 4);
        assertEquals(0xFF, c >>> 24);
        assertTrue(Math.abs(((c >> 16) & 255) - 0x33) < 12 && Math.abs(((c >> 8) & 255) - 0x66) < 12
                && Math.abs((c & 255) - 0xCC) < 12, Integer.toHexString(c));

        Images.Pixels gif = Images.decode(encode(opaque(5, 4, 0xFF8800), "gif"));
        assertEquals(5, gif.width());
        assertEquals(0xFFFF8800, gif.pixel(2, 2));
        Images.Pixels bmp = Images.decode(encode(opaque(4, 3, 0x00AA55), "bmp"));
        assertEquals(0xFF00AA55, bmp.pixel(3, 2));
    }

    /** Платформа теста, которая «загружает» текстуры в список. */
    private static final class TexturePlatform implements Platform {
        final List<String> keys = new ArrayList<>();
        int[] lastArgb;
        int lastWidth;

        @Override
        public TextMetrics textMetrics() {
            return TestSupport.METRICS;
        }

        @Override
        public String getClipboard() {
            return "";
        }

        @Override
        public void setClipboard(String text) {
        }

        @Override
        public void playClickSound() {
        }

        @Override
        public TextureRef createTexture(String key, int width, int height, int[] argb) {
            keys.add(key);
            lastArgb = argb;
            lastWidth = width;
            return TextureRef.of("test:dynamic/" + key);
        }
    }

    @Test
    void imagesFallBackWhenPlatformCannotCreateTextures(@TempDir Path dir) throws IOException {
        byte[] png = encode(sample(), "png");
        assertNull(Images.fromBytes("pic", png), "default platform: no textures, no exception");
        assertNull(Images.fromBytes("broken", new byte[]{1, 2, 3}), "garbage is reported, not thrown");
        Image empty = new Image(Images.fromBytes("pic", png)).size(20, 20);
        UiRoot root = mount(empty);
        assertTrue(render(root).ops.stream().anyMatch(op -> op.startsWith("shape") || op.startsWith("quad")),
                "placeholder instead of the picture");

        TexturePlatform platform = new TexturePlatform();
        Lumina.init(platform);
        Images.clearCache();
        TextureRef ref = Images.fromBytes("My Pic!", png);
        ImageTexture tex = assertInstanceOf(ImageTexture.class, ref);
        assertEquals("test:dynamic/my_pic_", tex.id());
        assertEquals(3, tex.width());
        assertEquals(2, tex.height());
        assertEquals(3, platform.lastWidth);
        assertEquals(0x8000FF00, platform.lastArgb[1]);
        assertEquals(ref, Images.fromBytes("My Pic!", png), "cached");
        assertEquals(1, platform.keys.size());

        Path file = dir.resolve("Shot.PNG");
        Files.write(file, encode(opaque(30, 10, 0x102030), "png"));
        ImageTexture fromFile = assertInstanceOf(ImageTexture.class, Images.fromFile(file));
        assertEquals(3f, fromFile.aspect(), 1e-6);
        assertTrue(platform.keys.get(1).startsWith("file/shot.png-"), platform.keys.get(1));
        assertNull(Images.fromFile(dir.resolve("missing.png")));

        Image image = Image.of(fromFile);
        UiRoot root2 = mount(Panel.box(FlexLayout.column().align(Align.START)).add(image));
        assertEquals(30f, image.measuredWidth(), 1e-4, "natural size");
        assertEquals(10f, image.measuredHeight(), 1e-4);
        image.radius(3).fit(Image.Fit.COVER).size(20, 20);
        root2.layoutIfNeeded();
        assertTrue(render(root2).ops.stream().filter(op -> op.startsWith("tex test:dynamic/file")).count() > 1,
                "rounded corners");
    }

    // ---------------------------------------------------------------- разметка

    @Test
    void markupTags() {
        Bindings b = new Bindings();
        Markup.Built built = Markup.build("""
                <state name="agree" value="false"/>
                <state name="mode" value="normal"/>
                <state name="level" value="2"/>
                <column>
                  <checkbox id="cb" bind="agree" kind="heart">Согласен</checkbox>
                  <checkbox id="mixed" label="Mixed {{mode}}" indeterminate/>
                  <radio-group id="rg" bind="mode" options="easy, normal, hard" labels="Легко,Нормально,Сложно" direction="row"/>
                  <radio-group id="lvl" bind="level" options="1,2,3" kind="fill"/>
                  <image id="img" src="minecraft:textures/block/dirt.png" fit="cover" width="40" height="20" radius="4"/>
                  <button id="btn" item="minecraft:diamond">Go</button>
                  <button id="ico" icon="minecraft:textures/item/apple.png" class="icon"></button>
                  <button id="busy" loading="saving">Save</button>
                </column>
                """, b);
        UiRoot root = mount(built.root());
        UiElement r = built.root();

        Checkbox cb = assertInstanceOf(Checkbox.class, r.findById("cb"));
        assertEquals(Checkbox.Kind.HEART, cb.kind());
        assertEquals("Согласен", cb.label());
        click(root, cb);
        assertEquals(Boolean.TRUE, b.get("agree").get());

        Checkbox mixed = assertInstanceOf(Checkbox.class, r.findById("mixed"));
        assertTrue(mixed.isIndeterminate());
        assertEquals("Mixed normal", mixed.label());

        @SuppressWarnings("unchecked")
        RadioGroup<String> rg = assertInstanceOf(RadioGroup.class, r.findById("rg"));
        assertEquals(List.of("easy", "normal", "hard"), rg.options());
        assertEquals("Нормально", rg.items().get(1).label());
        assertTrue(rg.items().get(1).isChecked());
        root.layoutIfNeeded();
        assertEquals(rg.items().get(0).y(), rg.items().get(2).y(), 1e-4, "direction=row");
        click(root, rg.items().get(2));
        assertEquals("hard", b.get("mode").get());
        assertEquals("Mixed hard", mixed.label());

        @SuppressWarnings("unchecked")
        RadioGroup<String> lvl = assertInstanceOf(RadioGroup.class, r.findById("lvl"));
        assertTrue(lvl.items().get(1).isChecked(), "number state matches text option");
        assertEquals(Checkbox.Kind.FILL, lvl.items().get(0).kind());
        root.layoutIfNeeded();
        click(root, lvl.items().get(2));
        assertEquals(3, b.get("level").get(), "number stays a number");

        Image img = assertInstanceOf(Image.class, r.findById("img"));
        assertEquals(Image.Fit.COVER, img.fit());
        assertEquals("minecraft:textures/block/dirt.png", img.texture().id());

        Button btn = assertInstanceOf(Button.class, r.findById("btn"));
        assertEquals("minecraft:diamond", btn.iconItem());
        assertEquals("Go", btn.text());
        Button ico = assertInstanceOf(Button.class, r.findById("ico"));
        assertEquals("minecraft:textures/item/apple.png", ico.iconTexture().id());
        assertTrue(ico.styleClass().contains("icon"));
        Button busy = assertInstanceOf(Button.class, r.findById("busy"));
        assertFalse(busy.isLoading());
        b.writable("saving").set(true);
        assertTrue(busy.isLoading());
        render(root);
    }
}
