package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.anim.Spring;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Widget;

/** Свой виджет: точка бежит по дорожке с заданной плавностью или пружиной. */
final class Runner extends Widget<Runner> {
    private final Easing easing;
    private final Spring spring;
    private AnimatedFloat position;
    private boolean forward;

    Runner(Easing easing, Spring spring) {
        super(null);
        this.easing = easing;
        this.spring = spring;
        layoutParams().height = Dim.px(12);
        layoutParams().grow = 1f;
    }

    void run() {
        forward = !forward;
        if (position != null) {
            position.animateTo(forward ? 1f : 0f);
        }
    }

    @Override
    protected void onAttach() {
        position = new AnimatedFloat(root().animator(), forward ? 1f : 0f);
        if (spring != null) {
            position.spring(spring);
        } else {
            position.tween(1.2f, easing);
        }
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        float w = width();
        float h = height();
        canvas.fillRoundRect(0, h / 2f - 1.5f, w, 3f, Radii.all(1.5f), Brush.solid(0x30808080));
        float d = 10f;
        float t = position != null ? position.get() : 0f;
        int accent = style.get(Styles.ACCENT_COLOR);
        canvas.fillRoundRect(t * (w - d), (h - d) / 2f, d, d, Radii.all(d / 2f), Brush.solid(accent));
    }
}
