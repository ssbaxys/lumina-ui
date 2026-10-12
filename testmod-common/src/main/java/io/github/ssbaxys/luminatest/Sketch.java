package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.widget.Widget;

/**
 * Холст с произвольной отрисовкой и временем: для демонстраций, которые проще нарисовать, чем собрать
 * из виджетов. Перерисовывается каждый кадр, пока находится в дереве.
 */
final class Sketch extends Widget<Sketch> implements Animation {
    @FunctionalInterface
    interface Painter {
        void paint(Canvas canvas, float width, float height, float time);
    }

    private final Painter painter;
    private float time;
    private boolean alive;

    Sketch(float width, float height, Painter painter) {
        super(null);
        this.painter = painter;
        size(width, height);
    }

    /** На всю ширину родителя. */
    static Sketch wide(float height, Painter painter) {
        return new Sketch(0, height, painter).width(Dim.FILL);
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
        painter.paint(canvas, width(), height(), time);
    }
}
