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
    public static final SpriteSheet FIRE_0 = SpriteSheet.animated(TextureRef.vanilla("block/fire_0"), 20f, 32);
    public static final SpriteSheet FIRE_1 = SpriteSheet.animated(TextureRef.vanilla("block/fire_1"), 20f, 32);
    public static final SpriteSheet SOUL_FIRE_0 = SpriteSheet.animated(TextureRef.vanilla("block/soul_fire_0"), 20f, 32);
    public static final SpriteSheet SOUL_FIRE_1 = SpriteSheet.animated(TextureRef.vanilla("block/soul_fire_1"), 20f, 32);
    public static final SpriteSheet EXPLOSION = numbered("particle/explosion_", 16, 40f);
    public static final SpriteSheet BIG_SMOKE = numbered("particle/big_smoke_", 12, 12f);
    /** Обычный дым/пыль: в игре кадры идут от крупного к мелкому, поэтому номера обратные. */
    public static final SpriteSheet SMOKE = numberedReversed("particle/generic_", 8, 12f);
    public static final SpriteSheet SPARK = numbered("particle/spark_", 8, 16f);
    public static final SpriteSheet FLAME = SpriteSheet.single(TextureRef.vanilla("particle/flame"));
    public static final SpriteSheet SOUL_FLAME = SpriteSheet.single(TextureRef.vanilla("particle/soul_fire_flame"));
    public static final SpriteSheet LAVA = SpriteSheet.single(TextureRef.vanilla("particle/lava"));
    public static final SpriteSheet HEART = SpriteSheet.single(TextureRef.vanilla("particle/heart"));
    public static final SpriteSheet ANGRY = SpriteSheet.single(TextureRef.vanilla("particle/angry"));
    public static final SpriteSheet NOTE = SpriteSheet.single(TextureRef.vanilla("particle/note"));
    public static final SpriteSheet HAPPY = SpriteSheet.single(TextureRef.vanilla("particle/glint"));
    public static final SpriteSheet CRIT = SpriteSheet.single(TextureRef.vanilla("particle/critical_hit"));
    public static final SpriteSheet MAGIC_CRIT = SpriteSheet.single(TextureRef.vanilla("particle/enchanted_hit"));
    public static final SpriteSheet BUBBLE = SpriteSheet.single(TextureRef.vanilla("particle/bubble"));
    public static final SpriteSheet BUBBLE_POP = numbered("particle/bubble_pop_", 5, 20f);
    public static final SpriteSheet SPLASH = numbered("particle/splash_", 4, 20f);
    public static final SpriteSheet DRIP_HANG = SpriteSheet.single(TextureRef.vanilla("particle/drip_hang"));
    public static final SpriteSheet DRIP_FALL = SpriteSheet.single(TextureRef.vanilla("particle/drip_fall"));
    public static final SpriteSheet GLITTER = numberedReversed("particle/glitter_", 8, 12f);
    public static final SpriteSheet SPELL = numberedReversed("particle/spell_", 8, 12f);
    public static final SpriteSheet CHERRY = numbered("particle/cherry_", 12, 1f);
    /** Руны стандартного галактического алфавита со стола зачарования: sga_a … sga_z. */
    public static final SpriteSheet ENCHANT_GLYPHS;

    public static final TextureRef RAIN_TEXTURE = TextureRef.vanilla("environment/rain");
    public static final TextureRef SNOW_TEXTURE = TextureRef.vanilla("environment/snow");

    static {
        TextureRef[] glyphs = new TextureRef[26];
        for (int i = 0; i < 26; i++) {
            glyphs[i] = TextureRef.vanilla("particle/sga_" + (char) ('a' + i));
        }
        ENCHANT_GLYPHS = SpriteSheet.frames(1f, glyphs);
    }

    public static final TextureRef TNT_TOP = TextureRef.vanilla("block/tnt_top");
    public static final TextureRef TNT_SIDE = TextureRef.vanilla("block/tnt_side");

    public static final String SOUND_EXPLODE = "minecraft:entity.generic.explode";
    public static final String SOUND_FUSE = "minecraft:entity.tnt.primed";
    public static final String SOUND_FIRE = "minecraft:item.flintandsteel.use";
    public static final String SOUND_FIRE_AMBIENT = "minecraft:block.fire.ambient";
    public static final String SOUND_LEVEL_UP = "minecraft:entity.player.levelup";
    public static final String SOUND_ENCHANT = "minecraft:block.enchantment_table.use";
    public static final String SOUND_TOTEM = "minecraft:item.totem.use";
    public static final String SOUND_NOTE = "minecraft:block.note_block.harp";
    public static final String SOUND_VILLAGER_YES = "minecraft:entity.villager.yes";
    public static final String SOUND_VILLAGER_NO = "minecraft:entity.villager.no";

    private VanillaFx() {
    }

    /** Имена встроенных эффектов для {@link FxRegistry}; вызывается реестром сам. */
    static void registerDefaults() {
        FxRegistry.register("minecraft:fire", (x, y, w, h) -> fire(x, y, w, h));
        FxRegistry.register("minecraft:soul_fire", (x, y, w, h) -> soulFire(x, y, w, h));
        FxRegistry.register("minecraft:explosion", (x, y, w, h) -> explosion(x + w / 2f, y + h / 2f,
                Math.max(0.3f, Math.min(w, h) / 64f)));
        FxRegistry.register("minecraft:tnt", (x, y, w, h) -> primedTnt(x + w / 2f, y, y + h, 22f, 3f));
        FxRegistry.register("minecraft:snow", VanillaFx::snow);
        FxRegistry.register("minecraft:rain", VanillaFx::rain);
        FxRegistry.register("minecraft:cherry_leaves", VanillaFx::cherryLeaves);
        FxRegistry.register("minecraft:flames", (x, y, w, h) -> flames(x + w / 2f, y + h, w));
        FxRegistry.register("minecraft:lava", (x, y, w, h) -> lavaPop(x + w / 2f, y + h, 14));
        FxRegistry.register("minecraft:smoke", (x, y, w, h) -> smokePuff(x + w / 2f, y + h / 2f, Math.min(w, h) / 2f));
        FxRegistry.register("minecraft:hearts", (x, y, w, h) -> hearts(x + w / 2f, y + h / 2f, 6));
        FxRegistry.register("minecraft:angry_villager", (x, y, w, h) -> angry(x + w / 2f, y + h / 2f, 4));
        FxRegistry.register("minecraft:happy_villager", (x, y, w, h) -> happy(x + w / 2f, y + h / 2f, Math.min(w, h) / 2f, 12));
        FxRegistry.register("minecraft:note", (x, y, w, h) -> note(x + w / 2f, y + h / 2f, (int) (Math.random() * 25)));
        FxRegistry.register("minecraft:crit", (x, y, w, h) -> crit(x + w / 2f, y + h / 2f, false));
        FxRegistry.register("minecraft:enchanted_hit", (x, y, w, h) -> crit(x + w / 2f, y + h / 2f, true));
        FxRegistry.register("minecraft:enchant", (x, y, w, h) -> enchant(x + w / 2f, y + h / 2f, Math.max(w, h) / 2f));
        FxRegistry.register("minecraft:totem", (x, y, w, h) -> totem(x + w / 2f, y + h / 2f));
        FxRegistry.register("minecraft:bubbles", (x, y, w, h) -> bubbles(x + w / 2f, y + h, w, y));
        FxRegistry.register("minecraft:dripping_water", (x, y, w, h) -> drips(x + w / 2f, y, w, y + h, false));
        FxRegistry.register("minecraft:dripping_lava", (x, y, w, h) -> drips(x + w / 2f, y, w, y + h, true));
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
        return new Fire(x, y, w, h, FIRE_0, FIRE_0);
    }

    /** Голубой огонь душ. */
    public static Fire soulFire(float x, float y, float w, float h) {
        return new Fire(x, y, w, h, SOUL_FIRE_0, SOUL_FIRE_0);
    }

    /**
     * Огонь горящего блока, как его видно сбоку: обе плоскости модели {@code fire_floor} с одной текстурой
     * (задняя зеркальная), непрозрачные, полной яркости; анимация 20 кадров/с, как у ванильного атласа.
     * Над огнём изредка поднимается тёмный {@code large_smoke}, иногда трещит {@code block.fire.ambient}.
     * Ни свечения, ни полупрозрачных слоёв: в игре их нет.
     */
    public static final class Fire extends Effect {
        private final float x;
        private final float y;
        private final float w;
        private final float h;
        private final SpriteSheet front;
        private final SpriteSheet back;
        private final Particles smoke;
        private float time;
        private float height;
        private float nextAmbient = 1.5f;

        Fire(float x, float y, float w, float h, SpriteSheet front, SpriteSheet back) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.front = front;
            this.back = back;
            // LargeSmokeParticle: серый 0..0.3, медленно поднимается и растёт, кадры big_smoke по возрасту.
            this.smoke = new Particles(BIG_SMOKE).gravity(-14f).drag(0.5f).growth(0.6f).animateByLife(true)
                    .tint(0xF0FFFFFF, 0x00FFFFFF);
            sound(SOUND_FIRE, 1f, 1f);
        }

        @Override
        public boolean tick(float dt, FxLayer layer) {
            time += dt;
            // Ванильный огонь появляется сразу; здесь — за ~0.1 с, гаснет за ~0.15 с.
            float target = isStopped() ? 0f : 1f;
            height += (target - height) * Math.min(1f, dt * 20f);
            Random r = layer.random();
            if (!isStopped()) {
                float block = Math.max(1f, w / 16f);
                if (r.nextFloat() < dt * 1.6f * block) {
                    int g = (int) (r.nextFloat() * 0.3f * 255f);
                    smoke.emit(x + r.nextFloat() * w, y + h * (0.3f + 0.4f * r.nextFloat()),
                            (r.nextFloat() - 0.5f) * 4f, -10f - r.nextFloat() * 8f,
                            1.4f + r.nextFloat() * 1.2f, h * (0.25f + 0.2f * r.nextFloat()),
                            0xFF000000 | g << 16 | g << 8 | g);
                }
                nextAmbient -= dt;
                if (nextAmbient <= 0f) {
                    nextAmbient = 2f + r.nextFloat() * 4f;
                    sound(SOUND_FIRE_AMBIENT, 0.6f + r.nextFloat() * 0.4f, 0.7f + r.nextFloat() * 0.3f);
                }
            }
            smoke.tick(dt, layer);
            return height > 0.01f || smoke.count() > 0;
        }

        @Override
        public void render(Canvas canvas) {
            float vis = Math.max(0f, Math.min(1f, height));
            if (vis > 0.01f) {
                float hh = h * vis;
                float top = y + h - hh;
                int frame = front.frameAt(time);
                back.drawCroppedMirrored(canvas, frame, x, top, w, hh, 1f - vis, 1f, Color.WHITE);
                front.drawCropped(canvas, frame, x, top, w, hh, 1f - vis, 1f, Color.WHITE);
            }
            smoke.render(canvas);
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

    // ================================================================ погода

    /** Снегопад над прямоугольником: ванильная текстура снега слоями + отдельные хлопья. */
    public static Weather snow(float x, float y, float w, float h) {
        Particles flakes = new Particles(null).wobble(10f, 0.6f).tint(0xF0FFFFFF, 0xF0FFFFFF);
        flakes.floor(y + h, null);
        flakes.emitter((p, r, dt) -> {
            if (r.nextFloat() < dt * w * 0.12f) {
                float size = r.nextFloat() < 0.3f ? 2f : 1f;
                p.emit(x + r.nextFloat() * w, y - 2f, (r.nextFloat() - 0.5f) * 6f, 14f + r.nextFloat() * 16f,
                        h / 14f + 2f, size);
            }
        }, Float.POSITIVE_INFINITY);
        return new Weather(SNOW_TEXTURE, x, y, w, h, 0.18f, 0.08f, flakes);
    }

    /** Дождь: ванильная текстура дождя, косые капли и брызги на «полу» (нижний край прямоугольника). */
    public static Weather rain(float x, float y, float w, float h) {
        Particles splashes = new Particles(SPLASH).gravity(200f).tint(0xD0C0D8FF, 0x80C0D8FF);
        Particles drops = new Particles(null).streak(0.035f).tint(0x90A8C8FF, 0x90A8C8FF);
        drops.floor(y + h, (px, py, c) -> {
            if (splashes.random().nextFloat() < 0.35f) {
                splashes.emit(px, py - 1f, (splashes.random().nextFloat() - 0.5f) * 30f, -30f, 0.25f, 5f);
            }
        });
        drops.emitter((p, r, dt) -> {
            int n = Math.round(dt * w * 0.6f + r.nextFloat());
            for (int i = 0; i < n; i++) {
                p.emit(x + r.nextFloat() * (w + 20f) - 10f, y - 4f, -12f, 240f + r.nextFloat() * 60f, h / 200f + 0.3f, 1f);
            }
        }, Float.POSITIVE_INFINITY);
        return new Weather(RAIN_TEXTURE, x, y, w, h, 1.6f, 0f, Effect.group(drops, splashes));
    }

    /**
     * Погода: ванильная текстура осадков, прокручиваемая двумя слоями (ближний быстрее), плюс частицы.
     * Плавно появляется и плавно стихает по {@link #stop()}.
     */
    public static final class Weather extends Effect {
        private final TextureRef texture;
        private final float x;
        private final float y;
        private final float w;
        private final float h;
        private final float speed;
        private final float sway;
        private final Effect particles;
        private float time;
        private float strength;

        Weather(TextureRef texture, float x, float y, float w, float h, float speed, float sway, Effect particles) {
            this.texture = texture;
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.speed = speed;
            this.sway = sway;
            this.particles = particles;
        }

        @Override
        public boolean tick(float dt, FxLayer layer) {
            time += dt;
            float target = isStopped() ? 0f : 1f;
            strength += (target - strength) * Math.min(1f, dt * 2f);
            if (isStopped() && !particles.isStopped()) {
                particles.stop();
            }
            boolean alive = particles.tick(dt, layer);
            return alive || strength > 0.01f;
        }

        @Override
        public void render(Canvas canvas) {
            if (strength > 0.01f) {
                canvas.save();
                canvas.clipRect(x, y, w, h);
                // Как в игре: текстура 1 блок × 4 блока, дальний слой медленнее и прозрачнее.
                drawLayer(canvas, 64f, time * speed * 0.6f, 0.13f, Color.withAlpha(Color.WHITE, 0.35f * strength));
                drawLayer(canvas, 96f, time * speed, 0.37f, Color.withAlpha(Color.WHITE, 0.7f * strength));
                canvas.restore();
            }
            particles.render(canvas);
        }

        private void drawLayer(Canvas canvas, float tile, float scroll, float phase, int tint) {
            float tileH = tile * 4f;
            float swayPx = sway > 0f ? (float) Math.sin(time * 0.7f + phase * 10f) * tile * sway : 0f;
            float v = scroll % 1f;
            for (float tx = x - tile + swayPx % tile; tx < x + w; tx += tile) {
                // Каждая колонка со своим сдвигом, чтобы узор не повторялся ровными рядами.
                float col = (float) Math.floor((tx - x) / tile);
                float off = ((col * 0.618f + phase) % 1f + 1f) % 1f;
                for (float ty = y - tileH; ty < y + h; ty += tileH) {
                    drawWrapped(canvas, tx, ty, tile, tileH, ((off - v) % 1f + 1f) % 1f, tint);
                }
            }
        }

        /** Плитка со сдвигом текстуры по вертикали: разрезается на две части в месте «шва». */
        private void drawWrapped(Canvas canvas, float px, float py, float pw, float ph, float v0, int tint) {
            float firstH = (1f - v0) * ph;
            canvas.drawTexture(texture, px, py, pw, firstH, 0f, v0, 1f, 1f, tint);
            if (v0 > 0f) {
                canvas.drawTexture(texture, px, py + firstH, pw, ph - firstH, 0f, 0f, 1f, v0, tint);
            }
        }
    }

    // ================================================================ частицы мобов и предметов

    /** Сердечки, как при размножении или приручении. */
    public static Particles hearts(float x, float y, int count) {
        Particles p = new Particles(HEART).gravity(-12f).drag(0.5f).fadeOut(true);
        Random r = p.random();
        for (int i = 0; i < count; i++) {
            p.emit(x + (r.nextFloat() - 0.5f) * 24f, y + (r.nextFloat() - 0.5f) * 10f, (r.nextFloat() - 0.5f) * 12f,
                    -18f - r.nextFloat() * 14f, 1.2f + r.nextFloat() * 0.6f, 8f);
        }
        sound(SOUND_VILLAGER_YES, 0.6f, 1.2f);
        return p;
    }

    /** Злой житель: тёмные тучки. */
    public static Particles angry(float x, float y, int count) {
        Particles p = new Particles(ANGRY).drag(0.8f).growth(1.2f);
        Random r = p.random();
        for (int i = 0; i < count; i++) {
            p.emit(x + (r.nextFloat() - 0.5f) * 24f, y + (r.nextFloat() - 0.5f) * 10f, (r.nextFloat() - 0.5f) * 20f,
                    -10f, 0.9f + r.nextFloat() * 0.4f, 9f);
        }
        sound(SOUND_VILLAGER_NO, 0.6f, 1f);
        return p;
    }

    /** Довольный житель / костная мука: зелёные искорки. */
    public static Particles happy(float x, float y, float radius, int count) {
        Particles p = new Particles(HAPPY).gravity(-6f).growth(0.6f);
        Random r = p.random();
        for (int i = 0; i < count; i++) {
            p.emit(x + (r.nextFloat() - 0.5f) * radius * 2f, y + (r.nextFloat() - 0.5f) * radius * 2f, 0f,
                    -4f - r.nextFloat() * 6f, 0.6f + r.nextFloat() * 0.8f, 6f);
        }
        return p;
    }

    /** Ноты нотного блока: цвет по высоте ноты, как в игре; {@code pitch} 0..24. */
    public static Particles note(float x, float y, int pitch) {
        Particles p = new Particles(NOTE).drag(0.7f).fadeOut(true);
        float k = Math.max(0, Math.min(24, pitch)) / 24f;
        int color = Color.rgbaF(
                Math.max(0f, (float) Math.sin((k + 0f) * Math.PI * 2) * 0.65f + 0.35f),
                Math.max(0f, (float) Math.sin((k + 1f / 3f) * Math.PI * 2) * 0.65f + 0.35f),
                Math.max(0f, (float) Math.sin((k + 2f / 3f) * Math.PI * 2) * 0.65f + 0.35f), 1f);
        p.emit(x, y, (p.random().nextFloat() - 0.5f) * 10f, -30f, 0.9f, 9f, color);
        sound(SOUND_NOTE, 0.5f, (float) Math.pow(2, (pitch - 12) / 12.0));
        return p;
    }

    /** Критический удар: рыжие осколки; {@code magic} — голубые, как от зачарованного оружия. */
    public static Particles crit(float x, float y, boolean magic) {
        Particles p = new Particles(magic ? MAGIC_CRIT : CRIT).gravity(160f).drag(0.6f);
        Random r = p.random();
        for (int i = 0; i < 16; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            float sp = 60f + r.nextFloat() * 70f;
            p.emit(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp - 40f, 0.4f + r.nextFloat() * 0.3f, 6f);
        }
        return p;
    }

    /**
     * Руны со стола зачарования: вылетают из области вокруг и стягиваются к точке (tx, ty).
     * Пока не остановлен, испускает новые.
     */
    public static Particles enchant(float tx, float ty, float radius) {
        Particles p = new Particles(ENCHANT_GLYPHS).frames(Particles.FrameMode.RANDOM).attract(tx, ty, 140f)
                .drag(0.6f).fadeOut(false).tint(0xFFFFFFFF, 0xFFD0C8FF);
        p.emitter((ps, r, dt) -> {
            if (r.nextFloat() < dt * 14f) {
                double a = r.nextDouble() * Math.PI * 2;
                float d = radius * (0.6f + 0.4f * r.nextFloat());
                ps.emit(tx + (float) Math.cos(a) * d, ty + (float) Math.sin(a) * d * 0.6f - 10f,
                        (r.nextFloat() - 0.5f) * 10f, -25f - r.nextFloat() * 15f, 3f, 6f);
            }
        }, Float.POSITIVE_INFINITY);
        sound(SOUND_ENCHANT, 0.7f, 1f);
        return p;
    }

    /** Тотем бессмертия: фонтан зелёно-жёлтых блёсток. */
    public static Particles totem(float x, float y) {
        Particles p = new Particles(GLITTER).gravity(90f).drag(0.5f).animateByLife(true);
        Random r = p.random();
        for (int i = 0; i < 70; i++) {
            double a = r.nextDouble() * Math.PI * 2;
            float sp = 60f + r.nextFloat() * 110f;
            int c = r.nextBoolean() ? Color.rgbaF(0.6f + r.nextFloat() * 0.2f, 0.85f + r.nextFloat() * 0.15f, 0.2f, 1f)
                    : Color.rgbaF(0.95f, 0.85f + r.nextFloat() * 0.15f, 0.25f, 1f);
            p.emit(x, y, (float) Math.cos(a) * sp, (float) Math.sin(a) * sp - 90f, 0.8f + r.nextFloat() * 0.8f, 7f, c);
        }
        sound(SOUND_TOTEM, 0.6f, 1f);
        return p;
    }

    /** Пузыри из-под воды: поднимаются, покачиваются и лопаются у поверхности {@code surfaceY}. */
    public static Effect bubbles(float x, float y, float width, float surfaceY) {
        Particles pops = new Particles(BUBBLE_POP).animateByLife(true).fadeOut(false);
        Particles rising = new Particles(BUBBLE).wobble(14f, 1.4f).fadeOut(false);
        rising.emitter((p, r, dt) -> {
            if (r.nextFloat() < dt * 8f) {
                p.emit(x + (r.nextFloat() - 0.5f) * width, y, 0f, -30f - r.nextFloat() * 20f, 6f, 5f + r.nextFloat() * 2f);
            }
        }, Float.POSITIVE_INFINITY);
        rising.ceiling(surfaceY, (px, py, c) -> pops.emit(px, py, 0f, 0f, 0.25f, 7f));
        return Effect.group(rising, pops);
    }

    /** Лепестки вишни: медленно падают и кружатся. */
    public static Particles cherryLeaves(float x, float y, float w, float h) {
        Particles p = new Particles(CHERRY).frames(Particles.FrameMode.RANDOM).wobble(18f, 0.35f).fadeOut(false);
        p.floor(y + h, null);
        p.emitter((ps, r, dt) -> {
            if (r.nextFloat() < dt * w * 0.03f) {
                ps.emit(x + r.nextFloat() * w, y - 4f, 6f + r.nextFloat() * 8f, 12f + r.nextFloat() * 10f, h / 12f + 2f, 5f);
            }
        }, Float.POSITIVE_INFINITY);
        return p;
    }

    /** Капли с потолка (y): висят, срываются и падают до пола; вода или лава. */
    public static Effect drips(float x, float y, float width, float floorY, boolean lava) {
        int color = lava ? 0xFFFF7A1A : 0xFF3F76E4;
        Particles splash = new Particles(lava ? LAVA : SPLASH).gravity(200f);
        Particles p = new Particles(DRIP_FALL).gravity(300f).fadeOut(false).tint(color, color);
        p.floor(floorY, (px, py, c) -> splash.emit(px, py - 1f, 0f, -20f, 0.3f, lava ? 4f : 5f, lava ? Color.WHITE : 0xFF8FB0FF));
        p.emitter((ps, r, dt) -> {
            if (r.nextFloat() < dt * 1.5f) {
                ps.emit(x + (r.nextFloat() - 0.5f) * width, y, 0f, 0f, 3f, 5f);
            }
        }, Float.POSITIVE_INFINITY);
        return Effect.group(p, splash);
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
