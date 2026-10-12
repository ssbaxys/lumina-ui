package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorStops;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;

/**
 * Заглушка на время загрузки: серый блок с бегущим бликом. Строки текста, аватар, карточка —
 * просто разные размеры и скругления: {@code Skeleton.line(120)}, {@code Skeleton.circle(24)}.
 */
public class Skeleton extends Widget<Skeleton> implements Animation {
    private float radius;
    private float time;
    private boolean alive;

    public Skeleton(float width, float height, float radius) {
        super("skeleton");
        this.radius = radius;
        size(width, height);
        setHitTestable(false);
    }

    /** Строка текста. */
    public static Skeleton line(float width) {
        return new Skeleton(width, 8f, 4f);
    }

    /** Строка на всю ширину. */
    public static Skeleton fullLine() {
        Skeleton s = new Skeleton(0, 8f, 4f);
        s.layoutParams().width = Dim.FILL;
        return s;
    }

    /** Круг (аватар, иконка). */
    public static Skeleton circle(float size) {
        return new Skeleton(size, size, size / 2f);
    }

    @Override
    protected void onAttach() {
        alive = true;
        root().animator().start(this);
    }

    @Override
    protected void onDetach() {
        alive = false;
    }

    @Override
    public boolean tick(float dt) {
        time += dt;
        return alive;
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        int text = style.get(Styles.TEXT_COLOR);
        int base = Color.withAlpha(text, 0x1C);
        int shine = Color.withAlpha(text, 0x38);
        // Блик проходит слева направо раз в 1.4 с; позиции стопов сдвигаются по времени.
        float p = (time / 1.4f) % 1f * 1.6f - 0.3f;
        float a = Math.max(0f, Math.min(1f, p - 0.2f));
        float b = Math.max(0f, Math.min(1f, p));
        float c = Math.max(0f, Math.min(1f, p + 0.2f));
        Brush brush;
        if (c <= 0f || a >= 1f) {
            brush = Brush.solid(base);
        } else {
            brush = Brush.linear(90f, ColorStops.of(
                    new float[]{0f, a, b, c, 1f}, new int[]{base, base, shine, base, base}));
        }
        canvas.fillRoundRect(0, 0, width(), height(), Radii.all(Math.min(radius, height() / 2f)), brush);
    }
}
