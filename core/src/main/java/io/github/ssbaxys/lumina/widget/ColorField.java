package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;

import java.util.Locale;

/**
 * Поле цвета в hex: "#RRGGBB" или "#AARRGGBB" (и короткое "#RGB"), привязанное к {@link Signal}
 * с ARGB. Слева — образец цвета на шахматке. Корректный ввод применяется сразу, по Enter и при потере
 * фокуса текст приводится к полной форме; неверный — вспышка ошибки и откат.
 */
public class ColorField extends FieldBase<ColorField> {
    private final Signal<Integer> value;
    private boolean alpha = true;

    public ColorField(Signal<Integer> value) {
        super(value);
        this.value = value;
        layoutParams().width = Dim.px(100);
        live(true);
        maxLength(9);
        charFilter(c -> c == '#' || Character.digit(c, 16) >= 0);
        filter(s -> s.matches("#?[0-9a-fA-F]*"));
        icon(this::paintSwatch);
        reload();
    }

    public Signal<Integer> value() {
        return value;
    }

    /** Разрешить прозрачность (иначе альфа всегда 255 и показывается "#RRGGBB"). */
    public ColorField alpha(boolean alpha) {
        this.alpha = alpha;
        reload();
        return self();
    }

    /** Пункт меню "Сбросить по умолчанию". */
    public ColorField defaultValue(int argb) {
        return resetItem(() -> format(argb));
    }

    /** Разобрать "#RGB", "#RRGGBB" или "#AARRGGBB" (решётка необязательна); {@code null}, если не цвет. */
    public static Integer parse(String text) {
        String h = text.trim();
        if (h.startsWith("#")) {
            h = h.substring(1);
        }
        if (h.length() != 3 && h.length() != 6 && h.length() != 8) {
            return null;
        }
        for (int i = 0; i < h.length(); i++) {
            if (Character.digit(h.charAt(i), 16) < 0) {
                return null;
            }
        }
        return Color.hex(h);
    }

    public String format(int argb) {
        return alpha && Color.alpha(argb) != 255
                ? String.format(Locale.ROOT, "#%08X", argb)
                : String.format(Locale.ROOT, "#%06X", argb & 0xFFFFFF);
    }

    private int current() {
        Integer v = value.peek();
        return v == null ? Color.BLACK : v;
    }

    @Override
    protected String externalText() {
        return format(current());
    }

    @Override
    protected boolean commitText(String text) {
        Integer c = parse(text);
        if (c == null) {
            return false;
        }
        value.set(alpha ? c : c | 0xFF000000);
        return true;
    }

    /** Образец: шахматка под цветом, чтобы была видна прозрачность. */
    private void paintSwatch(Canvas canvas, float x, float y, float size, int color) {
        float s = size + 1f;
        float y0 = y - 0.5f;
        Radii r = Radii.all(2f);
        int c = current();
        if (Color.alpha(c) < 255) {
            float half = s / 2f;
            canvas.fillRect(x, y0, s, s, 0xFFFFFFFF);
            canvas.fillRect(x, y0, half, half, 0xFFBBBBBB);
            canvas.fillRect(x + half, y0 + half, half, half, 0xFFBBBBBB);
        }
        canvas.fillRoundRect(x, y0, s, s, r, Brush.solid(c));
        canvas.strokeRoundRect(x, y0, s, s, r, 1f, Color.mulAlpha(color, 0.6f));
    }
}
