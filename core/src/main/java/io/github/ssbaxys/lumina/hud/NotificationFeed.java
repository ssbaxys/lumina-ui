package io.github.ssbaxys.lumina.hud;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * Компактная лента уведомлений (как сообщения о событиях в углу экрана): короткие строки с цветной
 * меткой и необязательной иконкой-предметом. Новая строка въезжает, старые через {@link #lifetime}
 * секунд тают; больше {@link #capacity} строк не бывает — самая старая уходит сразу.
 *
 * <pre>{@code
 * Hud.get().feed().post("Найден алмаз!", 0xFF4DD0E1);
 * feed.post("minecraft:experience_bottle", "+30 опыта", Toast.SUCCESS);
 * }</pre>
 *
 * <p>Всё хранится в массивах фиксированного размера и рисуется без выделения памяти в кадре.</p>
 */
public class NotificationFeed extends Widget<NotificationFeed> implements Animation {
    private static final float SLIDE = 0.22f;
    private static final float FADE = 0.8f;
    private static final float ROW_GAP = 2f;
    private static final float ICON = 8f;

    private int capacity = 6;
    private float lifetime = 6f;
    private float lineWidth = 200f;
    private boolean newestOnTop;
    private boolean alignRight;

    private String[] texts = new String[capacity];
    private String[] icons = new String[capacity];
    private int[] colors = new int[capacity];
    private float[] ages = new float[capacity];
    private float[] widths = new float[capacity];
    private int count;
    private boolean ticking;

    public NotificationFeed() {
        super("hud-feed");
        setHitTestable(false);
    }

    /** Сколько строк сразу (по умолчанию 6). */
    public NotificationFeed capacity(int lines) {
        int n = Math.max(1, lines);
        if (n != capacity) {
            int keep = Math.min(count, n);
            String[] t = new String[n];
            String[] ic = new String[n];
            int[] c = new int[n];
            float[] a = new float[n];
            float[] w = new float[n];
            // Оставляем самые новые: они в конце массивов.
            int from = count - keep;
            System.arraycopy(texts, from, t, 0, keep);
            System.arraycopy(icons, from, ic, 0, keep);
            System.arraycopy(colors, from, c, 0, keep);
            System.arraycopy(ages, from, a, 0, keep);
            System.arraycopy(widths, from, w, 0, keep);
            texts = t;
            icons = ic;
            colors = c;
            ages = a;
            widths = w;
            capacity = n;
            count = keep;
            invalidateLayout();
        }
        return this;
    }

    /** Секунд до исчезновения строки (по умолчанию 6). */
    public NotificationFeed lifetime(float seconds) {
        this.lifetime = Math.max(0.5f, seconds);
        return this;
    }

    /** Наибольшая ширина строки: длинный текст обрезается с «…». */
    public NotificationFeed lineWidth(float width) {
        this.lineWidth = Math.max(20f, width);
        return this;
    }

    /** Новые строки сверху (по умолчанию снизу, как в чате). */
    public NotificationFeed newestOnTop(boolean top) {
        this.newestOnTop = top;
        return this;
    }

    /** Строки прижаты к правому краю (для ленты у правого края экрана). */
    public NotificationFeed alignRight(boolean right) {
        this.alignRight = right;
        return this;
    }

    // ================================================================ строки

    public void post(String text) {
        post(null, text, 0);
    }

    /** Строка с цветной меткой слева ({@code 0} — акцент темы). */
    public void post(String text, int color) {
        post(null, text, color);
    }

    /** Строка с иконкой-предметом и цветной меткой. */
    public void post(String itemId, String text, int color) {
        if (count == capacity) {
            removeAt(0);
        }
        TextMetrics m = Lumina.platform().textMetrics();
        String icon = itemId == null || itemId.isEmpty() ? null : itemId;
        float room = lineWidth - 9f - (icon != null ? ICON + 3f : 0f);
        String line = HudText.ellipsize(text == null ? "" : text, room, m);
        texts[count] = line;
        icons[count] = icon;
        colors[count] = color;
        ages[count] = 0f;
        widths[count] = m.width(line) + 9f + (icon != null ? ICON + 3f : 0f);
        count++;
        invalidateLayout();
        startTicking();
    }

    /** Текущие строки от старой к новой. */
    public List<String> lines() {
        List<String> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(texts[i]);
        }
        return out;
    }

    public int size() {
        return count;
    }

    public void clear() {
        for (int i = 0; i < count; i++) {
            texts[i] = null;
            icons[i] = null;
        }
        count = 0;
        invalidateLayout();
    }

    private void removeAt(int i) {
        int tail = count - i - 1;
        System.arraycopy(texts, i + 1, texts, i, tail);
        System.arraycopy(icons, i + 1, icons, i, tail);
        System.arraycopy(colors, i + 1, colors, i, tail);
        System.arraycopy(ages, i + 1, ages, i, tail);
        System.arraycopy(widths, i + 1, widths, i, tail);
        count--;
        texts[count] = null;
        icons[count] = null;
    }

    // ================================================================ время

    @Override
    protected void onAttach() {
        startTicking();
    }

    private void startTicking() {
        if (!ticking && count > 0 && root() != null) {
            ticking = true;
            root().animator().start(this);
        }
    }

    @Override
    public boolean tick(float dt) {
        if (root() == null) {
            ticking = false;
            return false;
        }
        int before = count;
        for (int i = count - 1; i >= 0; i--) {
            ages[i] += dt;
            if (ages[i] >= lifetime) {
                removeAt(i);
            }
        }
        if (count != before) {
            invalidateLayout();
        }
        ticking = count > 0;
        return ticking;
    }

    // ================================================================ раскладка и отрисовка

    private float rowHeight() {
        return Lumina.platform().textMetrics().lineHeight() + 4f;
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        float w = 0f;
        for (int i = 0; i < count; i++) {
            w = Math.max(w, widths[i]);
        }
        float h = count == 0 ? 0f : count * rowHeight() + (count - 1) * ROW_GAP;
        setContentSize(Math.min(w, lineWidth), h);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        if (count == 0) {
            return;
        }
        TextMetrics m = canvas.textMetrics();
        float rh = m.lineHeight() + 4f;
        int row = style.get(HudStyles.ROW_COLOR);
        int text = style.get(Styles.TEXT_COLOR);
        int accent = style.get(Styles.ACCENT_COLOR);
        boolean shadow = style.get(Styles.TEXT_SHADOW);
        float left = padding().left();
        float innerW = width() - padding().horizontal();
        boolean reduced = root() != null && root().accessibility().reducedMotion();
        for (int i = 0; i < count; i++) {
            int slot = newestOnTop ? count - 1 - i : i;
            float age = ages[slot];
            float alpha = Math.min(1f, Math.min(age / SLIDE, (lifetime - age) / FADE));
            if (alpha <= 0f) {
                continue;
            }
            float slide = reduced ? 0f : (1f - Easing.OUT_CUBIC.apply(Math.min(1f, age / SLIDE))) * 24f;
            float w = widths[slot];
            float x = alignRight ? left + innerW - w + slide : left - slide;
            float y = padding().top() + i * (rh + ROW_GAP);
            int mark = colors[slot] != 0 ? colors[slot] : accent;
            canvas.fillRect(x, y, w, rh, Color.mulAlpha(row, alpha));
            canvas.fillRect(x, y, 2f, rh, Color.mulAlpha(mark, alpha));
            float tx = x + 5f;
            if (icons[slot] != null) {
                canvas.save();
                canvas.multiplyOpacity(alpha);
                canvas.drawItem(icons[slot], tx, y + (rh - ICON) / 2f, ICON);
                canvas.restore();
                tx += ICON + 3f;
            }
            canvas.drawText(texts[slot], tx, y + 2f, Color.mulAlpha(text, alpha), shadow);
        }
    }
}
