package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;

/** Индикатор загрузки: кольцо, точки, полоски или пульс. Цвет — акцент темы. */
public class Spinner extends Widget<Spinner> implements Animation {
    public enum Kind {
        /** Дуга, которая вращается и меняет длину. */
        RING,
        /** Три прыгающие точки. */
        DOTS,
        /** Полоски эквалайзера. */
        BARS,
        /** Расходящийся круг. */
        PULSE
    }

    private final Kind kind;
    private float time;
    private boolean alive;

    public Spinner() {
        this(Kind.RING, 20f);
    }

    public Spinner(Kind kind, float size) {
        super("spinner");
        this.kind = kind;
        size(kind == Kind.DOTS || kind == Kind.BARS ? size * 1.6f : size, size);
        setHitTestable(false);
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
        int accent = style.get(Styles.ACCENT_COLOR);
        float w = width();
        float h = height();
        switch (kind) {
            case RING -> {
                float r = Math.min(w, h) / 2f - 2f;
                float sweep = 60f + 200f * (0.5f + 0.5f * (float) Math.sin(time * 3f));
                float start = time * 300f + (float) Math.sin(time * 3f) * 60f;
                canvas.drawArc(w / 2f, h / 2f, r + 1f, 2.5f, 0f, 360f, Color.withAlpha(accent, 0x30), Color.withAlpha(accent, 0x30));
                canvas.drawArc(w / 2f, h / 2f, r + 1f, 2.5f, start, sweep, Color.withAlpha(accent, 0x60), accent);
            }
            case DOTS -> {
                float d = h / 3.2f;
                for (int i = 0; i < 3; i++) {
                    float phase = (float) Math.sin(time * 6f - i * 0.8f);
                    float lift = Math.max(0f, phase) * h * 0.3f;
                    float cx = w / 2f + (i - 1) * d * 1.6f;
                    float cy = h / 2f - lift;
                    int c = Color.withAlpha(accent, 0.45f + 0.55f * Math.max(0f, phase));
                    canvas.fillCircle(cx, cy, d / 2f, Brush.solid(c));
                }
            }
            case BARS -> {
                int n = 4;
                float bw = w / (n * 1.6f);
                for (int i = 0; i < n; i++) {
                    float k = 0.3f + 0.7f * (0.5f + 0.5f * (float) Math.sin(time * 7f + i * 1.1f));
                    float bh = h * k;
                    float x = (w - (n * bw + (n - 1) * bw * 0.6f)) / 2f + i * bw * 1.6f;
                    canvas.fillRoundRect(x, (h - bh) / 2f, bw, bh, Radii.all(bw / 2f), Brush.solid(accent));
                }
            }
            case PULSE -> {
                for (int i = 0; i < 2; i++) {
                    float t = (time * 0.9f + i * 0.5f) % 1f;
                    float r = Math.min(w, h) / 2f * t;
                    canvas.fillCircle(w / 2f, h / 2f, r, Brush.solid(Color.withAlpha(accent, (1f - t) * 0.6f)));
                }
                canvas.fillCircle(w / 2f, h / 2f, Math.min(w, h) / 6f, Brush.solid(accent));
            }
        }
    }
}
