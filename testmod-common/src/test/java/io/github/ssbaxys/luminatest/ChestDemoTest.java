package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.inventory.Slot;
import io.github.ssbaxys.lumina.inventory.Stack;
import io.github.ssbaxys.lumina.menu.MenuActions;
import io.github.ssbaxys.lumina.menu.MenuChannel;
import io.github.ssbaxys.lumina.menu.MenuValues;
import io.github.ssbaxys.lumina.menu.RemoteMenu;
import io.github.ssbaxys.lumina.menu.SlotClick;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.AbstractCanvas;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.ShapePaint;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.ProgressBar;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Окно «Сундука Lumina» без игры: строится, показывает данные сервера, кнопки и жесты уходят на сервер. */
class ChestDemoTest {
    private static final TextMetrics METRICS = new TextMetrics() {
        @Override
        public float width(String text) {
            return text.length() * 6f;
        }

        @Override
        public float lineHeight() {
            return 9f;
        }
    };

    @BeforeAll
    static void init() {
        Lumina.init(new Platform() {
            @Override
            public TextMetrics textMetrics() {
                return METRICS;
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
        });
    }

    /** Записывает щелчки и кнопки; курсор всегда пуст (как после удачного жеста). */
    private static final class Channel implements MenuChannel {
        final List<String> clicks = new ArrayList<>();
        final List<Integer> buttons = new ArrayList<>();
        boolean closed;

        @Override
        public void click(int slot, int button, SlotClick type) {
            clicks.add(type + " " + slot + " " + button);
        }

        @Override
        public boolean isCarrying() {
            return false;
        }

        @Override
        public void clickButton(int buttonId) {
            buttons.add(buttonId);
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    @Test
    void chestWindowWorksHeadless() {
        Channel channel = new Channel();
        RemoteMenu menu = new RemoteMenu(ChestDemo.SPEC, "Сундук Lumina", channel);
        UiRoot root = ChestDemo.create(menu);
        root.resize(640, 480);
        Canvas canvas = new Canvas();
        frames(root, canvas, 2);

        List<Slot> slots = new ArrayList<>();
        collect(root.content(), slots);
        assertEquals(27 + 36, slots.size());

        // Данные с сервера: предметы в слотах и значения.
        menu.setSlot(0, Stack.of("minecraft:diamond", 12));
        menu.setSlot(ChestDemo.SPEC.playerSection().slot(0), Stack.of("minecraft:iron_pickaxe"));
        MenuValues server = new MenuValues(ChestDemo.SPEC)
                .set(ChestDemo.PROGRESS, 50).set(ChestDemo.ITEMS, 12).set(ChestDemo.SORTED, true)
                .set(ChestDemo.TIME, 1_234_567);
        for (int part = 0; part < ChestDemo.SPEC.dataSlotCount(); part++) {
            menu.setData(part, (short) ChestDemo.SPEC.dataPart(server, part));
        }
        menu.publish();
        frames(root, canvas, 2);
        assertTrue(canvas.items.contains("minecraft:diamond"));
        assertTrue(canvas.items.contains("minecraft:iron_pickaxe"), "hotbar slot 0");
        assertNotNull(findLabel(root.content(), "Тик мира: 1234567"), "full ints survive the 16-bit parts");
        assertNotNull(findLabel(root.content(), "Предметов: 12"));
        assertNotNull(findLabel(root.content(), "Цикл сервера: 50%"));
        assertNotNull(findLabel(root.content(), "Разложен"));
        assertEquals(0.5f, find(root.content(), ProgressBar.class).progress(), 0.001f);

        // Кнопки — действия меню.
        findButton(root.content(), "По количеству").press();
        assertEquals(toList(MenuActions.encode(ChestDemo.SORT, ChestDemo.BY_COUNT)), channel.buttons);
        channel.buttons.clear();
        findButton(root.content(), "Наполнить").press();
        assertEquals(toList(MenuActions.encode(ChestDemo.FILL, 0)), channel.buttons);
        findButton(root.content(), "Закрыть").press();
        assertTrue(channel.closed);

        // Перенос из сундука в хотбар — щелчки по слотам меню.
        Slot from = slots.get(0);
        Slot to = slots.get(27 + 27 + 4);
        drag(root, from, to);
        assertEquals(List.of("PICKUP 0 0", "PICKUP " + ChestDemo.SPEC.playerSection().slot(4) + " 0"), channel.clicks);
        assertEquals(12, from.stack().count(), "the model waits for the server");

        // Мимо окна — выброс через сервер, на фон окна — отмена.
        channel.clicks.clear();
        drag(root, from, 4f, 4f);
        assertEquals(List.of("PICKUP 0 0", "PICKUP -999 0"), channel.clicks);
        channel.clicks.clear();
        UiElement window = from.parent().parent();
        drag(root, from, window.screenX() + 3f, window.screenY() + 3f);
        assertEquals(List.of(), channel.clicks);
    }

    private static List<Integer> toList(int[] ids) {
        List<Integer> out = new ArrayList<>();
        for (int id : ids) {
            out.add(id);
        }
        return out;
    }

    private static void drag(UiRoot root, UiElement from, UiElement to) {
        drag(root, from, to.screenX() + to.width() / 2f, to.screenY() + to.height() / 2f);
    }

    private static void drag(UiRoot root, UiElement from, float x1, float y1) {
        root.tick(0f);
        float x0 = from.screenX() + from.width() / 2f;
        float y0 = from.screenY() + from.height() / 2f;
        root.pointerMove(x0, y0);
        root.pointerDown(x0, y0, Keys.MOUSE_LEFT);
        root.pointerMove(x0 + 6f, y0);
        root.pointerMove(x1, y1);
        root.pointerUp(x1, y1, Keys.MOUSE_LEFT);
    }

    private static void collect(UiElement e, List<Slot> out) {
        if (e instanceof Slot s) {
            out.add(s);
        }
        for (UiElement c : e.children()) {
            collect(c, out);
        }
    }

    private static Label findLabel(UiElement e, String text) {
        if (e instanceof Label l && text.equals(l.text())) {
            return l;
        }
        for (UiElement c : e.children()) {
            Label found = findLabel(c, text);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Button findButton(UiElement e, String text) {
        if (e instanceof Button b && text.equals(b.text())) {
            return b;
        }
        for (UiElement c : e.children()) {
            Button found = findButton(c, text);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static <T> T find(UiElement e, Class<T> type) {
        if (type.isInstance(e)) {
            return type.cast(e);
        }
        for (UiElement c : e.children()) {
            T found = find(c, type);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static void frames(UiRoot root, Canvas canvas, int n) {
        for (int i = 0; i < n; i++) {
            canvas.begin(root.width(), root.height());
            root.tick(1f / 60f);
            root.render(canvas, 0f);
            canvas.end();
        }
    }

    private static final class Canvas extends AbstractCanvas {
        final Set<String> items = new HashSet<>();

        void begin(float w, float h) {
            items.clear();
            beginFrame(w, h);
        }

        void end() {
            endFrame();
        }

        @Override
        protected void drawQuad(float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl) {
        }

        @Override
        protected void drawFreeQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                    int c0, int c1, int c2, int c3) {
        }

        @Override
        protected void drawShape(float x0, float y0, float x1, float y1, float rTL, float rTR, float rBR, float rBL,
                                 int tl, int tr, int br, int bl, float stroke, float softness, ShapePaint paint) {
        }

        @Override
        protected void applyBlendMode(BlendMode mode) {
        }

        @Override
        protected void drawTextImpl(String text, float x, float y, float scale, int argb, boolean shadow) {
        }

        @Override
        protected void drawTextureQuadImpl(TextureRef texture, float x0, float y0, float x1, float y1,
                                           float x2, float y2, float x3, float y3,
                                           float u0, float v0, float u1, float v1,
                                           float u2, float v2, float u3, float v3, int tint) {
        }

        @Override
        protected void drawItemImpl(String itemId, float x, float y, float size) {
            items.add(itemId);
        }

        @Override
        protected void applyClip(float x0, float y0, float x1, float y1) {
        }

        @Override
        protected void clearClip() {
        }

        @Override
        public TextMetrics textMetrics() {
            return METRICS;
        }
    }
}
