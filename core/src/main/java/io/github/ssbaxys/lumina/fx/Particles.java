package io.github.ssbaxys.lumina.fx;

import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;

import java.util.Arrays;
import java.util.Random;

/**
 * Система частиц со спрайтами. Данные лежат «структурой массивов» (x[], y[], vx[]…), поэтому тысячи
 * частиц не создают ни одного объекта за кадр. Частицы можно выпустить разово ({@link #emit}) или
 * испускать постоянно ({@link #emitter}).
 */
public final class Particles extends Effect {
    /** Постоянный источник частиц: вызывается каждый кадр. */
    @FunctionalInterface
    public interface Emitter {
        void emit(Particles particles, Random random, float dt);
    }

    private final SpriteSheet sprite;
    private float gravity;
    private float drag;
    private float growth = 1f;
    private boolean fadeOut = true;
    private boolean animateByLife = true;
    private int tintStart = Color.WHITE;
    private int tintEnd = Color.WHITE;
    private BlendMode blend = BlendMode.NORMAL;
    private Emitter emitter;
    private float emitFor = Float.POSITIVE_INFINITY;
    private float time;
    private final Random random;

    private int count;
    private float[] x = new float[32];
    private float[] y = new float[32];
    private float[] vx = new float[32];
    private float[] vy = new float[32];
    private float[] age = new float[32];
    private float[] life = new float[32];
    private float[] size = new float[32];
    private int[] tint = new int[32];

    public Particles(SpriteSheet sprite) {
        this(sprite, new Random());
    }

    public Particles(SpriteSheet sprite, Random random) {
        this.sprite = sprite;
        this.random = random;
    }

    /** Ускорение вниз, пикселей/с² (отрицательное — вверх, как у дыма). */
    public Particles gravity(float gravity) {
        this.gravity = gravity;
        return this;
    }

    /** Сопротивление: доля скорости, теряемая за секунду (0..1). */
    public Particles drag(float drag) {
        this.drag = drag;
        return this;
    }

    /** Во сколько раз частица вырастет к концу жизни. */
    public Particles growth(float growth) {
        this.growth = growth;
        return this;
    }

    public Particles fadeOut(boolean fadeOut) {
        this.fadeOut = fadeOut;
        return this;
    }

    /** {@code true}: кадры спрайта идут от рождения до смерти; {@code false}: крутятся с fps спрайта. */
    public Particles animateByLife(boolean byLife) {
        this.animateByLife = byLife;
        return this;
    }

    /** Цвет от рождения к смерти (умножается на цвет каждой частицы). */
    public Particles tint(int start, int end) {
        this.tintStart = start;
        this.tintEnd = end;
        return this;
    }

    public Particles blend(BlendMode blend) {
        this.blend = blend;
        return this;
    }

    /** Постоянный источник; {@code seconds} — сколько испускать (бесконечно — до {@link #stop()}). */
    public Particles emitter(Emitter emitter, float seconds) {
        this.emitter = emitter;
        this.emitFor = seconds;
        return this;
    }

    public Random random() {
        return random;
    }

    public int count() {
        return count;
    }

    /** Выпустить частицу. */
    public Particles emit(float px, float py, float pvx, float pvy, float lifeSeconds, float sizePx) {
        return emit(px, py, pvx, pvy, lifeSeconds, sizePx, Color.WHITE);
    }

    public Particles emit(float px, float py, float pvx, float pvy, float lifeSeconds, float sizePx, int color) {
        if (count == x.length) {
            int n = count * 2;
            x = Arrays.copyOf(x, n);
            y = Arrays.copyOf(y, n);
            vx = Arrays.copyOf(vx, n);
            vy = Arrays.copyOf(vy, n);
            age = Arrays.copyOf(age, n);
            life = Arrays.copyOf(life, n);
            size = Arrays.copyOf(size, n);
            tint = Arrays.copyOf(tint, n);
        }
        int i = count++;
        x[i] = px;
        y[i] = py;
        vx[i] = pvx;
        vy[i] = pvy;
        age[i] = 0f;
        life[i] = Math.max(lifeSeconds, 1e-3f);
        size[i] = sizePx;
        tint[i] = color;
        return this;
    }

    @Override
    public boolean tick(float dt, FxLayer layer) {
        time += dt;
        if (emitter != null && !isStopped() && time <= emitFor) {
            emitter.emit(this, random, dt);
        }
        float keep = drag > 0f ? (float) Math.pow(1f - Math.min(drag, 0.999f), dt) : 1f;
        for (int i = 0; i < count; i++) {
            age[i] += dt;
            if (age[i] >= life[i]) {
                removeAt(i);
                i--;
                continue;
            }
            vy[i] += gravity * dt;
            vx[i] *= keep;
            vy[i] *= keep;
            x[i] += vx[i] * dt;
            y[i] += vy[i] * dt;
        }
        boolean emitting = emitter != null && !isStopped() && time <= emitFor;
        return count > 0 || emitting;
    }

    private void removeAt(int i) {
        int last = --count;
        x[i] = x[last];
        y[i] = y[last];
        vx[i] = vx[last];
        vy[i] = vy[last];
        age[i] = age[last];
        life[i] = life[last];
        size[i] = size[last];
        tint[i] = tint[last];
    }

    @Override
    public void render(Canvas canvas) {
        if (count == 0) {
            return;
        }
        BlendMode prev = canvas.blendMode();
        canvas.setBlendMode(blend);
        for (int i = 0; i < count; i++) {
            float p = age[i] / life[i];
            float s = size[i] * (1f + (growth - 1f) * p);
            int c = multiply(tint[i], Color.lerp(tintStart, tintEnd, p));
            if (fadeOut) {
                c = Color.mulAlpha(c, 1f - p * p);
            }
            int frame = animateByLife ? sprite.frameOf(p) : sprite.frameAt(time + i * 0.37f);
            sprite.draw(canvas, frame, x[i] - s / 2f, y[i] - s / 2f, s, s, c);
        }
        canvas.setBlendMode(prev);
    }

    static int multiply(int a, int b) {
        return Color.argb(Color.alpha(a) * Color.alpha(b) / 255, Color.red(a) * Color.red(b) / 255,
                Color.green(a) * Color.green(b) / 255, Color.blue(a) * Color.blue(b) / 255);
    }
}
