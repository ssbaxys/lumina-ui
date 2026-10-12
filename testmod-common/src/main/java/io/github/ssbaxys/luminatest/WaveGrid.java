package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Widget;

/** Нагрузочный тест: сетка фигур, каждая меняет размер и цвет каждый кадр. */
final class WaveGrid extends Widget<WaveGrid> implements Animation {
    static final int COLS = 24;
    static final int ROWS = 10;

    private final ReadableSignal<Boolean> rounded;
    private float time;
    private boolean alive;

    WaveGrid(ReadableSignal<Boolean> rounded) {
        super(null);
        this.rounded = rounded;
        layoutParams().width = Dim.FILL;
        layoutParams().height = Dim.px(110);
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
        float cw = width() / COLS;
        float ch = height() / ROWS;
        int accent = style.get(Styles.ACCENT_COLOR);
        boolean round = Boolean.TRUE.equals(rounded.get());
        for (int y = 0; y < ROWS; y++) {
            for (int x = 0; x < COLS; x++) {
                float wave = (float) Math.sin(time * 3f + x * 0.45f + y * 0.6f) * 0.5f + 0.5f;
                float s = Math.min(cw, ch) * (0.35f + 0.55f * wave);
                float px = x * cw + (cw - s) / 2f;
                float py = y * ch + (ch - s) / 2f;
                int color = Color.lerp(0xFF22D3EE, accent, wave);
                if (round) {
                    canvas.fillRoundRect(px, py, s, s, Radii.all(s / 2f), Brush.solid(color));
                } else {
                    canvas.fillRect(px, py, s, s, color);
                }
            }
        }
    }
}
