package io.github.ssbaxys.lumina.fx;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.Motion;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorStops;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.Random;

/**
 * Эффекты в духе ванильного Minecraft на ванильных же текстурах: огонь, взрывы, падающий и
 * мигающий TNT, языки пламени, дым, искры, тряска интерфейса. Ядро ссылается на текстуры только
 * по идентификаторам ресурсов — рисует их бэкенд загрузчика, поэтому работает на 1.20.1 и 1.21.1
 * и автоматически подхватывает ресурспаки игрока.
 */
public final class VanillaFx {
    public static final SpriteSheet FIRE_0 = SpriteSheet.strip(TextureRef.vanilla("block/fire_0"), 32, 20f);
    public static final SpriteSheet FIRE_1 = SpriteSheet.strip(TextureRef.vanilla("block/fire_1"), 32, 20f);
    public static final SpriteSheet SOUL_FIRE_0 = SpriteSheet.strip(TextureRef.vanilla("block/soul_fire_0"), 32, 20f);
    public static final SpriteSheet SOUL_FIRE_1 = SpriteSheet.strip(TextureRef.vanilla("block/soul_fire_1"), 32, 20f);
    public static final SpriteSheet EXPLOSION = numbered("particle/explosion_", 16, 40f);
    public static final SpriteSheet BIG_SMOKE = numbered("particle/big_smoke_", 12, 12f);
    /** Обычный дым/пыль: в игре кадры идут от крупного к мелкому, поэтому номера обратные. */
    public static final SpriteSheet SMOKE = numberedReversed("particle/generic_", 8, 12f);
    public static final SpriteSheet SPARK = numbered("particle/spark_", 8, 16f);
    public static final SpriteSheet FLAME = SpriteSheet.single(TextureRef.vanilla("particle/flame"));
    public static final SpriteSheet SOUL_FLAME = SpriteSheet.single(TextureRef.vanilla("particle/soul_fire_flame"));
    public static final SpriteSheet LAVA = SpriteSheet.single(TextureRef.vanilla("particle/lava"));

    public static final TextureRef TNT_TOP = TextureRef.vanilla("block/tnt_top");
    public static final TextureRef TNT_SIDE = TextureRef.vanilla("block/tnt_side");

    public static final String SOUND_EXPLODE = "minecraft:entity.generic.explode";
    public static final String SOUND_FUSE = "minecraft:entity.tnt.primed";
    public static final String SOUND_FIRE = "minecraft:item.flintandsteel.use";

    private VanillaFx() {
    }

    private static SpriteSheet numbered(String prefix, int n, float fps) {
        TextureRef[] t = new TextureRef[n];
        for (int i = 0; i < n; i++) {
            t[i] = TextureRef.vanilla(prefix + i);
        }
        return SpriteSheet.frames(fps, t);
    }

    private static SpriteSheet numberedReversed(String prefix, int n, float fps) {
        TextureRef[] t = new TextureRef[n];
        for (int i = 0; i < n; i++) {
            t[i] = TextureRef.vanilla(prefix + (n - 1 - i));
        }
        return SpriteSheet.frames(fps, t);
    }

    private static void sound(String id, float volume, float pitch) {
        if (Lumina.isInitialized()) {
            Lumina.platform().playSound(id, volume, pitch);
        }
    }

    // ================================================================ огонь

    /** Огонь как у горящего блока, в прямоугольнике (x, y, w, h). Гаснет плавно по {@link Effect#stop()}. */
    public static Fire fire(float x, float y, float w, float h) {
        return new Fire(x, y, w, h, FIRE_0, FIRE_1, FLAME);
    }

    /** Голубой огонь душ. */
    public static Fire soulFire(float x, float y, float w, float h) {
        return new Fire(x, y, w, h, SOUL_FIRE_0, SOUL_FIRE_1, SOUL_FLAME);
    }

    public static final class Fire extends Effect {
        private final float x;
        private final float y;
        private final float w;
        private final float h;
        private final SpriteSheet front;
        private final SpriteSheet back;
        private final Particles smoke;
        private final Particles flames;
        private float time;
        private float height;

        Fire(float x, float y, float w, float h, SpriteSheet front, SpriteSheet back, SpriteSheet flame) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.front = front;
            this.back = back;
            this.smoke = new Particles(BIG_SMOKE).gravity(-30f).drag(0.6f).growth(1.6f).animateByLife(true)
                    .tint(0xD0FFFFFF, 0x40FFFFFF);
            this.flames = new Particles(flame).gravity(-60f).drag(0.4f).growth(0.3f).blend(BlendMode.ADD);
            sound(SOUND_FIRE, 1f, 1f);
        }

        @Override
        public boolean tick(float dt, FxLayer layer) {
            time += dt;
            // Разгорается за ~0.3 с, гаснет за ~0.5 с.
            float target = isStopped() ? 0f : 1f;
            height += (target - height) * Math.min(1f, dt * (isStopped() ? 4f : 7f));
            Random r = layer.random();
            if (!isStopped()) {
                if (r.nextFloat() < dt * 6f * (w / 32f)) {
                    smoke.emit(x + r.nextFloat() * w, y + h * (1f - height) + 4f, (r.nextFloat() - 0.5f) * 8f,
                            -12f - r.nextFloat() * 10f, 1.2f + r.nextFloat(), w * 0.35f + r.nextFloat() * 6f);
                }
                if (r.nextFloat() < dt * 10f * (w / 32f)) {
                    flames.emit(x + w * (0.2f + 0.6f * r.nextFloat()), y + h * (1f - 0.5f * height),
                            (r.nextFloat() - 0.5f) * 10f, -20f - r.nextFloat() * 20f, 0.5f + r.nextFloat() * 0.4f,
                            w * 0.16f);
                }
            }
            smoke.tick(dt, layer);
            flames.tick(dt, layer);
            return height > 0.01f || smoke.count() > 0 || flames.count() > 0;
        }

        @Override
        public void render(Canvas canvas) {
            smoke.render(canvas);
            float vis = Math.max(0f, Math.min(1f, height));
            if (vis > 0.01f) {
                float hh = h * vis;
                int frame = front.frameAt(time);
                // Как у блока: две плоскости огня с разными кадрами, задняя чуть выше и шире.
                back.drawCropped(canvas, frame + 7, x - w * 0.08f, y + h - hh * 1.08f, w * 1.16f, hh * 1.08f,
                        1f - vis, 1f, 0xD0FFFFFF);
                front.drawCropped(canvas, frame, x, y + h - hh, w, hh, 1f - vis, 1f, Color.WHITE);
            }
            flames.render(canvas);
        }
    }

    // ================================================================ взрыв

    /** Взрыв как от TNT: огненные клубы, вспышка, дым и искры. {@code scale} 1 ≈ взрыв размером в 4 блока по 16 px. */
    public static Effect explosion(float cx, float cy, float scale) {
        return new Explosion(cx, cy, scale);
    }

    static final class Explosion extends Effect {
        private final float cx;
        private final float cy;
        private final float scale;
        private final Particles balls;
        private final Particles smoke;
        private final Particles sparks;
        private float time;

        Explosion(float cx, float cy, float scale) {
            this.cx = cx;
            this.cy = cy;
            this.scale = scale;
            this.balls = new Particles(EXPLOSION).animateByLife(true).fadeOut(false);
            this.smoke = new Particles(BIG_SMOKE).gravity(-25f * scale).drag(0.8f).growth(1.8f).tint(0xE0FFFFFF, 0x30FFFFFF);
            this.sparks = new Particles(SPARK).gravity(260f * scale).drag(0.9f).blend(BlendMode.ADD)
                    .tint(0xFFFFF2B0, 0xFFFF6A00).animateByLife(true);
            sound(SOUND_EXPLODE, 1f, 0.9f + (float) Math.random() * 0.2f);
        }

        @Override
        public boolean tick(float dt, FxLayer layer) {
            float before = time;
            time += dt;
            Random r = layer.random();
            float radius = 36f * scale;
            // Как HugeExplosionSeedParticle: 8 тиков по 6 клубов в случайных местах.
            float seedTime = 0.4f;
            if (before < seedTime) {
                int n = Math.max(1, Math.round(Math.min(time, seedTime) * 120f) - Math.round(before * 120f));
                for (int i = 0; i < n; i++) {
                    float px = cx + (r.nextFloat() - r.nextFloat()) * radius;
                    float py = cy + (r.nextFloat() - r.nextFloat()) * radius;
                    float gray = 0.6f + r.nextFloat() * 0.4f;
                    int c = Color.rgbaF(gray, gray, gray, 1f);
                    balls.emit(px, py, 0f, 0f, 0.3f + r.nextFloat() * 0.2f, (20f + r.nextFloat() * 18f) * scale, c);
                }
                if (before == 0f) {
                    for (int i = 0; i < 18; i++) {
                        double a = r.nextDouble() * Math.PI * 2;
                        float sp = (40f + r.nextFloat() * 50f) * scale;
                        smoke.emit(cx, cy, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp - 10f * scale,
                                1.4f + r.nextFloat() * 1.2f, (14f + r.nextFloat() * 12f) * scale);
                    }
                    for (int i = 0; i < 26; i++) {
                        double a = r.nextDouble() * Math.PI * 2;
                        float sp = (120f + r.nextFloat() * 160f) * scale;
                        sparks.emit(cx, cy, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp - 60f * scale,
                                0.5f + r.nextFloat() * 0.5f, (5f + r.nextFloat() * 4f) * scale);
                    }
                }
            }
            balls.tick(dt, layer);
            smoke.tick(dt, layer);
            sparks.tick(dt, layer);
            return time < seedTime || balls.count() > 0 || smoke.count() > 0 || sparks.count() > 0;
        }

        @Override
        public void render(Canvas canvas) {
            smoke.render(canvas);
            float flash = 1f - time / 0.3f;
            if (flash > 0f) {
                float r = 70f * scale * (0.6f + 0.4f * (1f - flash));
                BlendMode prev = canvas.blendMode();
                canvas.setBlendMode(BlendMode.ADD);
                canvas.fillCircle(cx, cy, r, Brush.radial(ColorStops.builder()
                        .add(0f, Color.withAlpha(0xFFFFF4D0, flash))
                        .add(0.4f, Color.withAlpha(0xFFFFA040, flash * 0.6f))
                        .add(1f, 0x00FF6000).build()));
                canvas.setBlendMode(prev);
            }
            balls.render(canvas);
            sparks.render(canvas);
        }
    }

    // ================================================================ TNT

    /**
     * Подожжённый TNT: падает с гравитацией, отскакивает от «пола» {@code floorY}, мигает белым, дымит
     * и через {@code fuseSeconds} взрывается в том же слое. {@code size} — ширина кубика в пикселях.
     */
    public static PrimedTnt primedTnt(float x, float y, float floorY, float size, float fuseSeconds) {
        return new PrimedTnt(x, y, floorY, size, fuseSeconds);
    }

    public static final class PrimedTnt extends Effect {
        private float x;
        private float y;
        private float vx;
        private float vy;
        private final float floorY;
        private final float size;
        private final float fuse;
        private float time;
        private final Particles smoke;
        private Runnable onExplode;
        private boolean exploded;

        PrimedTnt(float x, float y, float floorY, float size, float fuse) {
            this.x = x;
            this.y = y;
            this.floorY = floorY;
            this.size = size;
            this.fuse = fuse;
            this.smoke = new Particles(SMOKE).gravity(-40f).drag(0.5f).tint(0xC0FFFFFF, 0x40FFFFFF);
            sound(SOUND_FUSE, 1f, 1f);
        }

        /** Начальная скорость (пикселей/с). */
        public PrimedTnt velocity(float vx, float vy) {
            this.vx = vx;
            this.vy = vy;
            return this;
        }

        /** Что сделать при взрыве (например, встряхнуть окно {@link #shake}). */
        public PrimedTnt onExplode(Runnable onExplode) {
            this.onExplode = onExplode;
            return this;
        }

        private float height() {
            return size * 1.06f;
        }

        @Override
        public boolean tick(float dt, FxLayer layer) {
            time += dt;
            if (!exploded) {
                float g = 9.8f * size * 2.2f;
                vy += g * dt;
                vx *= (float) Math.pow(0.4, dt);
                x += vx * dt;
                y += vy * dt;
                if (y + height() > floorY) {
                    y = floorY - height();
                    vy = Math.abs(vy) < 20f ? 0f : -vy * 0.45f;
                    vx *= 0.7f;
                }
                Random r = layer.random();
                if (r.nextFloat() < dt * 14f) {
                    smoke.emit(x + (r.nextFloat() - 0.5f) * size * 0.3f, y - 2f, (r.nextFloat() - 0.5f) * 6f,
                            -15f - r.nextFloat() * 10f, 0.8f + r.nextFloat() * 0.4f, size * 0.35f);
                }
                if (time >= fuse || isStopped()) {
                    exploded = true;
                    layer.spawn(explosion(x, y + height() / 2f, size / 24f));
                    if (onExplode != null) {
                        onExplode.run();
                    }
                }
            }
            smoke.tick(dt, layer);
            return !exploded || smoke.count() > 0;
        }

        @Override
        public void render(Canvas canvas) {
            smoke.render(canvas);
            if (exploded) {
                return;
            }
            drawBlock(canvas, TNT_TOP, TNT_SIDE, x, y, size, Color.WHITE);
            // Как у ванильного TNT: мигание каждые 5 тиков, всё чаще и ярче к концу фитиля.
            int ticks = (int) ((fuse - time) * 20f);
            if ((ticks / 5) % 2 == 0) {
                float a = 0.55f + 0.4f * Math.min(1f, time / fuse);
                BlendMode prev = canvas.blendMode();
                canvas.setBlendMode(BlendMode.ADD);
                drawBlock(canvas, TNT_TOP, TNT_SIDE, x, y, size, Color.withAlpha(Color.WHITE, a));
                canvas.setBlendMode(prev);
            }
        }
    }

    /**
     * Изометрический блок как иконка в инвентаре. (cx, topY) — верхняя вершина, {@code size} — ширина.
     * Боковые грани затемнены, как у ванильных иконок.
     */
    public static void drawBlock(Canvas canvas, TextureRef top, TextureRef side, float cx, float topY, float size, int tint) {
        float half = size / 2f;
        float q = size / 4f;
        float h = size * 0.56f;
        float lx = cx - half;
        float rx = cx + half;
        float my = topY + q;
        float by = topY + 2f * q;
        int left = Particles.multiply(tint, 0xFFCCCCCC);
        int right = Particles.multiply(tint, 0xFF999999);
        canvas.drawTextureQuad(top, cx, topY, rx, my, cx, by, lx, my, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, tint);
        canvas.drawTextureQuad(side, lx, my, cx, by, cx, by + h, lx, my + h, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, left);
        canvas.drawTextureQuad(side, cx, by, rx, my, rx, my + h, cx, by + h, 0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f, right);
    }

    // ================================================================ прочее

    /** Фонтан языков пламени (как у костра), пока не остановлен. */
    public static Particles flames(float x, float y, float width) {
        return new Particles(FLAME).gravity(-50f).drag(0.5f).growth(0.4f).blend(BlendMode.ADD)
                .emitter((p, r, dt) -> {
                    if (r.nextFloat() < dt * 18f) {
                        p.emit(x + (r.nextFloat() - 0.5f) * width, y, (r.nextFloat() - 0.5f) * 12f,
                                -25f - r.nextFloat() * 25f, 0.6f + r.nextFloat() * 0.5f, 6f + r.nextFloat() * 4f);
                    }
                }, Float.POSITIVE_INFINITY);
    }

    /** Брызги лавы: подпрыгивают и падают. */
    public static Particles lavaPop(float x, float y, int count) {
        Particles p = new Particles(LAVA).gravity(320f).blend(BlendMode.ADD);
        Random r = p.random();
        for (int i = 0; i < count; i++) {
            p.emit(x, y, (r.nextFloat() - 0.5f) * 120f, -120f - r.nextFloat() * 120f, 0.8f + r.nextFloat() * 0.6f,
                    5f + r.nextFloat() * 4f);
        }
        return p;
    }

    /** Клуб дыма. */
    public static Particles smokePuff(float x, float y, float radius) {
        Particles p = new Particles(BIG_SMOKE).gravity(-30f).drag(0.7f).growth(1.5f).tint(0xD0FFFFFF, 0x20FFFFFF);
        Random r = p.random();
        for (int i = 0; i < 10; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            float sp = 20f + r.nextFloat() * 30f;
            p.emit(x + (float) Math.cos(a) * radius * 0.3f, y + (float) Math.sin(a) * radius * 0.3f,
                    (float) Math.cos(a) * sp, (float) Math.sin(a) * sp, 1f + r.nextFloat(), radius * 0.6f);
        }
        return p;
    }

    /**
     * Тряска элемента (как экран от взрыва): затухающие смещения через слой анимаций.
     * {@code element.play(VanillaFx.shake(element, 6, 0.5f))}.
     */
    public static Motion shake(UiElement element, float strength, float duration) {
        return new Motion() {
            @Override
            public float duration() {
                return duration;
            }

            @Override
            public void apply(float t) {
                if (t >= duration) {
                    element.clearAnimatedStyle(Styles.TRANSLATE_X);
                    element.clearAnimatedStyle(Styles.TRANSLATE_Y);
                    return;
                }
                float k = 1f - t / duration;
                float amp = strength * k * k;
                element.setAnimatedStyle(Styles.TRANSLATE_X, (float) Math.sin(t * 91f) * amp);
                element.setAnimatedStyle(Styles.TRANSLATE_Y, (float) Math.cos(t * 73f) * amp);
            }
        };
    }
}
