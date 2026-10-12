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
 *
 * <p>Поведение собирается из готовых «деталей», поэтому новый эффект — это обычно несколько строк:
 * гравитация, сопротивление, рост, покачивание ({@link #wobble}), притяжение к точке ({@link #attract}),
 * «пол» с обработчиком приземления ({@link #floor}), растяжение по скорости ({@link #streak}),
 * выбор кадра ({@link #frames}). Без спрайта ({@code new Particles(null)}) частицы рисуются квадратиками
 * в пиксельном стиле — для снега, капель и искр.</p>
 */
public final class Particles extends Effect {
    /** Постоянный источник частиц: вызывается каждый кадр. */
    @FunctionalInterface
    public interface Emitter {
        void emit(Particles particles, Random random, float dt);
    }

    /** Частица долетела до пола ({@link #floor}): здесь можно выпустить брызги, сыграть звук. */
    @FunctionalInterface
    public interface Landing {
        void land(float x, float y, int color);
    }

    /** Как выбирается кадр спрайта. */
    public enum FrameMode {
        /** От первого до последнего кадра за жизнь частицы (взрыв, дым). */
        BY_LIFE,
        /** Зацикленно с fps спрайта (огонь). */
        LOOP,
        /** Каждой частице — свой случайный кадр на всю жизнь (руны зачарования, лепестки). */
        RANDOM
    }

    private final SpriteSheet sprite;
    private float gravity;
    private float drag;
    private float growth = 1f;
    private boolean fadeOut = true;
    private FrameMode frameMode = FrameMode.BY_LIFE;
    private float wobbleAmp;
    private float wobbleFreq;
    private boolean attracting;
    private float attractX;
    private float attractY;
    private float attractStrength;
    private float floorY = Float.NaN;
    private Landing landing;
    private float ceilingY = Float.NaN;
    private Landing ceilingHit;
    private float streak;
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
    private int[] seed = new int[32];

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
        this.frameMode = byLife ? FrameMode.BY_LIFE : FrameMode.LOOP;
        return this;
    }

    public Particles frames(FrameMode mode) {
        this.frameMode = mode;
        return this;
    }

    /** Покачивание из стороны в сторону: амплитуда скорости (px/с) и частота (Гц). Снег, листья, пузыри. */
    public Particles wobble(float amplitude, float frequency) {
        this.wobbleAmp = amplitude;
        this.wobbleFreq = frequency;
        return this;
    }

    /** Притяжение к точке с ускорением {@code strength} (px/с²): руны к столу зачарования, портал. */
    public Particles attract(float x, float y, float strength) {
        this.attracting = true;
        this.attractX = x;
        this.attractY = y;
        this.attractStrength = strength;
        return this;
    }

    /** Пол на высоте {@code y}: частица, долетевшая до него, исчезает и вызывает {@code onLand}. */
    public Particles floor(float y, Landing onLand) {
        this.floorY = y;
        this.landing = onLand;
        return this;
    }

    /** Потолок на высоте {@code y}: частица, поднявшаяся до него, исчезает (пузыри у поверхности воды). */
    public Particles ceiling(float y, Landing onHit) {
        this.ceilingY = y;
        this.ceilingHit = onHit;
        return this;
    }

    /** Рисовать вытянутой по скорости черточкой длиной {@code seconds} пути (дождь, быстрые искры). */
    public Particles streak(float seconds) {
        this.streak = seconds;
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
            seed = Arrays.copyOf(seed, n);
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
        seed[i] = random.nextInt() & 0x7FFFFFFF;
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
            if (attracting) {
                float dx = attractX - x[i];
                float dy = attractY - y[i];
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                if (d < 2f) {
                    removeAt(i);
                    i--;
                    continue;
                }
                vx[i] += dx / d * attractStrength * dt;
                vy[i] += dy / d * attractStrength * dt;
            }
            vx[i] *= keep;
            vy[i] *= keep;
            float sway = 0f;
            if (wobbleAmp != 0f) {
                sway = (float) Math.sin(age[i] * wobbleFreq * 6.2831855f + (seed[i] & 1023) * 0.0061f) * wobbleAmp;
            }
            x[i] += (vx[i] + sway) * dt;
            y[i] += vy[i] * dt;
            if (y[i] >= floorY) {
                if (landing != null) {
                    landing.land(x[i], floorY, tint[i]);
                }
                removeAt(i);
                i--;
            } else if (y[i] <= ceilingY) {
                if (ceilingHit != null) {
                    ceilingHit.land(x[i], ceilingY, tint[i]);
                }
                removeAt(i);
                i--;
            }
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
        seed[i] = seed[last];
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
            if (streak > 0f) {
                canvas.drawLine(x[i] - vx[i] * streak, y[i] - vy[i] * streak, x[i], y[i], s, c);
                continue;
            }
            if (sprite == null) {
                canvas.fillRect(x[i] - s / 2f, y[i] - s / 2f, s, s, c);
                continue;
            }
            int frame = switch (frameMode) {
                case BY_LIFE -> sprite.frameOf(p);
                case LOOP -> sprite.frameAt(time + (seed[i] & 255) * 0.37f);
                case RANDOM -> seed[i] % sprite.frameCount();
            };
            sprite.draw(canvas, frame, x[i] - s / 2f, y[i] - s / 2f, s, s, c);
        }
        canvas.setBlendMode(prev);
    }

    static int multiply(int a, int b) {
        return Color.argb(Color.alpha(a) * Color.alpha(b) / 255, Color.red(a) * Color.red(b) / 255,
                Color.green(a) * Color.green(b) / 255, Color.blue(a) * Color.blue(b) / 255);
    }
}
