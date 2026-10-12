package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.a11y.ColorVision;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Breakpoint;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.markdown.Markdown;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.AbstractCanvas;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ShapePaint;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.sound.SoundTheme;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Tokens;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Checkbox;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ResponsivePanel;
import io.github.ssbaxys.lumina.widget.Slider;
import io.github.ssbaxys.lumina.widget.ThemeEditor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Страница тем: редактор, экспорт, расписание, доступность, звуки и адаптивность работают из интерфейса. */
class ThemesPageTest {
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

    @TempDir
    static Path dataDir;

    static final List<String> sounds = new ArrayList<>();

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
                sounds.add("vanilla-click");
            }

            @Override
            public void playSound(String soundId, float volume, float pitch) {
                sounds.add(soundId);
            }

            @Override
            public Path dataDirectory() {
                return dataDir;
            }
        });
    }

    @Test
    void everySectionWorks() {
        Panel host = Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH)).fill();
        Theme showcase = ShowcaseTheme.dark();
        UiRoot root = new UiRoot(host, showcase);
        root.resize(640, 3000);
        UiElement page = ThemesPage.create();
        host.add(page);
        Canvas canvas = new Canvas();
        frames(root, canvas, 40);
        assertTrue(canvas.draws > 0);
        assertEquals(Breakpoint.WIDE, root.currentBreakpoint());
        assertEquals(SoundTheme.VANILLA.click(), root.theme().rawStyle("button").get(Styles.CLICK_SOUND));

        // Редактор правит тему корня.
        ThemeEditor editor = (ThemeEditor) root.content().findById("editor");
        assertNotNull(editor);
        editor.accent().set(0xFF00B4A0);
        assertEquals(0xFF00B4A0, (int) root.theme().get(Tokens.ACCENT));
        editor.exportTheme();
        assertTrue(dataDir.resolve("themes/showcase.css").toFile().isFile(), editor.status());

        // Показать CSS и проверить экспорт → импорт.
        ((Button) root.content().findById("show-css")).press();
        Markdown code = find(root.content(), Markdown.class);
        assertNotNull(code);
        assertTrue(code.text().contains("--accent: #00B4A0;"), code.text());
        ((Button) root.content().findById("round-trip")).press();
        assertNotNull(findLabel(root.content(), "Тема прочитана обратно без потерь"));

        // Расписание по времени суток: ползунок заменяет время мира.
        click(root, checkbox(root, "По времени суток"));
        assertTrue(checkbox(root, "По времени суток").isChecked(), "time mode selected");
        Slider time = (Slider) root.content().findById("time-slider");
        time.value().set(18000f);
        frames(root, canvas, 150);
        assertEquals(ShowcaseTheme.dark().get(Tokens.ACCENT), root.theme().get(Tokens.ACCENT), "night");
        time.value().set(6000f);
        frames(root, canvas, 150);
        assertEquals(ShowcaseTheme.light().get(Tokens.TEXT), root.theme().get(Tokens.TEXT), "day");

        // По биому.
        click(root, checkbox(root, "По биому"));
        click(root, checkbox(root, "Пустыня"));
        frames(root, canvas, 150);
        assertEquals(0xFFC98B2B, (int) root.theme().get(Tokens.ACCENT));

        // Звуки: набор применяется к теме из расписания.
        click(root, checkbox(root, "Ретро"));
        frames(root, canvas, 60);
        assertEquals(SoundTheme.RETRO.click(), root.theme().rawStyle("button").get(Styles.CLICK_SOUND));

        // Доступность.
        click(root, toggleNear(root, "Высокий контраст"));
        frames(root, canvas, 150);
        assertEquals(Color.contrast(root.theme().get(Tokens.TEXT), root.theme().get(Tokens.SURFACE)) >= 7f, true);
        click(root, toggleNear(root, "Крупный текст"));
        assertEquals(1.25f, root.accessibility().textScale());
        click(root, toggleNear(root, "Рамка фокуса всегда"));
        assertTrue(root.accessibility().focusAlwaysVisible());
        click(root, checkbox(root, "Дейтеранопия"));
        assertEquals(ColorVision.DEUTERANOPIA, root.accessibility().colorVision());
        assertNotNull(root.accessibility().colorFilter());
        frames(root, canvas, 10);
        click(root, toggleNear(root, "Крупный текст"));
        assertEquals(1f, root.accessibility().textScale());
        click(root, checkbox(root, "Обычное"));
        root.accessibility().focusAlwaysVisible(false);

        // Адаптивность и масштаб.
        ResponsivePanel grid = (ResponsivePanel) root.content().findById("grid");
        frames(root, canvas, 5);
        assertEquals(Breakpoint.WIDE, grid.current());
        assertEquals(Breakpoint.COMPACT, ((ResponsivePanel) root.content().findById("own-width")).current());
        click(root, checkbox(root, "1.5×"));
        assertEquals(1.5f, root.scale());
        frames(root, canvas, 5);
        assertEquals(Breakpoint.MEDIUM, root.currentBreakpoint());
        assertEquals(Breakpoint.MEDIUM, grid.current());

        host.removeChild(page);
        assertSame(showcase, root.theme(), "theme restored after leaving the page");
        assertEquals(1f, root.scale());
        frames(root, canvas, 20);
        assertSame(showcase, root.theme(), "schedule stopped");
    }

    private static Checkbox checkbox(UiRoot root, String label) {
        Checkbox c = findCheckbox(root.content(), label);
        assertNotNull(c, label);
        return c;
    }

    private static UiElement toggleNear(UiRoot root, String label) {
        Label l = findLabel(root.content(), label);
        assertNotNull(l, label);
        return l.parent().children().get(0);
    }

    /** Клик по центру элемента (в координатах экрана, с учётом масштаба корня). */
    private static void click(UiRoot root, UiElement e) {
        root.tick(0f); // свежая раскладка: прошлые действия могли сдвинуть элементы
        float s = root.scale();
        float x = (e.screenX() + Math.min(6f, e.width() / 2f)) * s;
        float y = (e.screenY() + e.height() / 2f) * s;
        root.pointerMove(x, y);
        root.pointerDown(x, y, Keys.MOUSE_LEFT);
        root.pointerUp(x, y, Keys.MOUSE_LEFT);
    }

    private static <T> T find(UiElement e, Class<T> type) {
        if (type.isInstance(e) && e.isVisible()) {
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

    private static Checkbox findCheckbox(UiElement e, String label) {
        if (e instanceof Checkbox c && label.equals(c.label())) {
            return c;
        }
        for (UiElement c : e.children()) {
            Checkbox found = findCheckbox(c, label);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static Label findLabel(UiElement e, String text) {
        if (e instanceof Label l && text.equals(l.text()) && e.isVisible()) {
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

    private static void frames(UiRoot root, Canvas canvas, int n) {
        for (int i = 0; i < n; i++) {
            canvas.begin(root.width() * root.scale(), root.height() * root.scale());
            root.tick(1f / 60f);
            root.render(canvas, 0f);
            canvas.end();
        }
    }

    private static final class Canvas extends AbstractCanvas {
        int draws;

        void begin(float w, float h) {
            draws = 0;
            beginFrame(w, h);
        }

        void end() {
            endFrame();
        }

        @Override
        protected void drawQuad(float x0, float y0, float x1, float y1, int tl, int tr, int br, int bl) {
            draws++;
        }

        @Override
        protected void drawFreeQuad(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                                    int c0, int c1, int c2, int c3) {
            draws++;
        }

        @Override
        protected void drawShape(float x0, float y0, float x1, float y1, float rTL, float rTR, float rBR, float rBL,
                                 int tl, int tr, int br, int bl, float stroke, float softness, ShapePaint paint) {
            draws++;
        }

        @Override
        protected void applyBlendMode(BlendMode mode) {
        }

        @Override
        protected void drawTextImpl(String text, float x, float y, float scale, int argb, boolean shadow) {
            draws++;
        }

        @Override
        protected void drawTextureQuadImpl(TextureRef texture, float x0, float y0, float x1, float y1,
                                           float x2, float y2, float x3, float y3,
                                           float u0, float v0, float u1, float v1,
                                           float u2, float v2, float u3, float v3, int tint) {
            draws++;
        }

        @Override
        protected void drawItemImpl(String itemId, float x, float y, float size) {
            draws++;
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
