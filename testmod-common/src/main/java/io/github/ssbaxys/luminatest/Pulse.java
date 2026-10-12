package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Widget;

/** Бесконечная анимация через собственную {@link Animation}: живёт, пока виджет в дереве. */
final class Pulse extends Widget<Pulse> implements Animation {
    private float time;
    private boolean alive;

    Pulse() {
        super(null);
        size(48, 48);
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
        float cx = width() / 2f;
        float cy = height() / 2f;
        int accent = style.get(Styles.ACCENT_COLOR);
        for (int i = 0; i < 3; i++) {
            float phase = (time * 0.7f + i / 3f) % 1f;
            float r = 5f + phase * 18f;
            canvas.strokeRoundRect(cx - r, cy - r, 2 * r, 2 * r, Radii.all(r), 1.5f, Color.withAlpha(accent, 1f - phase));
        }
        float s = 5f + (float) Math.sin(time * 4f) * 1f;
        canvas.fillRoundRect(cx - s, cy - s, 2 * s, 2 * s, Radii.all(s), Brush.solid(accent));
    }
}
