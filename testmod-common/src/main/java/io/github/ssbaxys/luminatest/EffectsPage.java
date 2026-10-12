package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.fx.Effect;
import io.github.ssbaxys.lumina.fx.FxLayer;
import io.github.ssbaxys.lumina.fx.SpriteSheet;
import io.github.ssbaxys.lumina.fx.VanillaFx;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Transition;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Panel;

import java.util.Random;
import java.util.function.Supplier;

/**
 * Ванильные эффекты прямо в интерфейсе: огонь, огонь душ, взрывы, падающий TNT, лава, дым и тряска.
 * Всё рисуется ванильными текстурами (подхватывает ресурспаки) и звучит ванильными звуками.
 */
final class EffectsPage {
    private static final float STAGE_H = 200f;
    private static final float BLOCK = 16f;
    private static final float FLOOR = STAGE_H - 2f * BLOCK;
    private static final TextureRef GRASS_TOP = TextureRef.vanilla("block/grass_block_side");
    private static final TextureRef DIRT = TextureRef.vanilla("block/dirt");
    private static final TextureRef NETHERRACK = TextureRef.vanilla("block/netherrack");
    private static final TextureRef SOUL_SOIL = TextureRef.vanilla("block/soul_soil");

    private EffectsPage() {
    }

    static UiElement create() {
        FxLayer fx = new FxLayer();
        Random random = new Random();
        Panel stage = Panel.box(new StackLayout(Align.STRETCH, Align.STRETCH)).width(Dim.FILL).height(Dim.px(STAGE_H));
        stage.setClipChildren(true);
        stage.style(b -> b.set(Styles.RADIUS, Radii.all(8)));
        stage.add(Sketch.wide(STAGE_H, EffectsPage::paintScene), fx);
        // Клик по сцене — взрыв в точке клика.
        stage.onClick(e -> {
            fx.spawn(VanillaFx.explosion(e.localX(), e.localY(), 0.8f));
            stage.play(VanillaFx.shake(stage, 4f, 0.4f));
        });

        Effect[] fire = {null};
        Effect[] soul = {null};
        Effect[] fountain = {null};

        Panel controls = Ui.row(6,
                new Button("Огонь", () -> fire[0] = toggle(fx, fire[0],
                        () -> VanillaFx.fire(stage.width() * 0.2f - 16f, FLOOR - 32f, 32f, 32f))).clickSound(false),
                new Button("Огонь душ", () -> soul[0] = toggle(fx, soul[0],
                        () -> VanillaFx.soulFire(stage.width() * 0.8f - 16f, FLOOR - 32f, 32f, 32f))).clickSound(false),
                new Button("Уронить TNT", () -> dropTnt(fx, stage, random)).clickSound(false),
                new Button("Взрыв", () -> {
                    fx.spawn(VanillaFx.explosion(40f + random.nextFloat() * (stage.width() - 80f),
                            40f + random.nextFloat() * (FLOOR - 60f), 1f));
                    stage.play(VanillaFx.shake(stage, 5f, 0.45f));
                }).clickSound(false));
        Panel controls2 = Ui.row(6,
                new Button("Ливень TNT", () -> {
                    for (int i = 0; i < 5; i++) {
                        dropTnt(fx, stage, random);
                    }
                }).clickSound(false),
                new Button("Пламя", () -> fountain[0] = toggle(fx, fountain[0],
                        () -> VanillaFx.flames(stage.width() / 2f, FLOOR, 24f))).clickSound(false),
                new Button("Лава", () -> fx.spawn(VanillaFx.lavaPop(stage.width() * (0.3f + random.nextFloat() * 0.4f),
                        FLOOR, 14))).clickSound(false),
                new Button("Дым", () -> fx.spawn(VanillaFx.smokePuff(stage.width() / 2f, FLOOR - 30f, 24f)))
                        .clickSound(false),
                new Button("Погасить", fx::clear).styleClass("button ghost").clickSound(false));

        Panel sprites = Ui.row(16,
                Ui.labeled(spriteLoop(VanillaFx.FIRE_0, 32), "fire_0"),
                Ui.labeled(spriteLoop(VanillaFx.SOUL_FIRE_0, 32), "soul_fire_0"),
                Ui.labeled(spriteLoop(VanillaFx.EXPLOSION, 32), "explosion"),
                Ui.labeled(spriteLoop(VanillaFx.BIG_SMOKE, 32), "big_smoke"),
                Ui.labeled(spriteLoop(VanillaFx.SPARK, 32), "spark"),
                Ui.labeled(new Sketch(40, 40, (c, w, h, t) ->
                        VanillaFx.drawBlock(c, VanillaFx.TNT_TOP, VanillaFx.TNT_SIDE, w / 2f, 4f + (float) Math.sin(t * 3) * 2f,
                                30f, Color.WHITE)), "блок"));

        Panel items = Ui.row(10);
        for (String id : new String[]{"minecraft:tnt", "minecraft:flint_and_steel", "minecraft:fire_charge",
                "minecraft:blaze_powder", "minecraft:lava_bucket", "minecraft:diamond_sword", "minecraft:grass_block",
                "minecraft:end_crystal"}) {
            Sketch icon = new Sketch(24, 24, (c, w, h, t) -> c.drawItem(id, 4f, 4f, 16f));
            icon.style(b -> b.set(Styles.BACKGROUND, Brush.solid(0x30808080)).set(Styles.RADIUS, Radii.all(4))
                    .set(Styles.TRANSITION, Transition.of(0.15f))
                    .hovered(h -> h.set(Styles.SCALE, 1.3f)));
            items.add(icon);
        }

        return Ui.page(
                Ui.section("Сцена", "Кликните по сцене — будет взрыв. TNT падает, отскакивает, мигает и взрывается, "
                        + "окно трясётся", stage, controls, controls2),
                Ui.section("Анимированные спрайты", "SpriteSheet: полосы кадров как у ванильных текстур и "
                        + "наборы отдельных кадров частиц", sprites),
                Ui.section("Предметы", "canvas.drawItem(\"minecraft:tnt\", x, y, size) — как в инвентаре", items));
    }

    private static Effect toggle(FxLayer fx, Effect current, Supplier<Effect> create) {
        if (current != null && !current.isStopped()) {
            current.stop();
            return null;
        }
        return fx.spawn(create.get());
    }

    private static void dropTnt(FxLayer fx, Panel stage, Random random) {
        float x = 30f + random.nextFloat() * (stage.width() - 60f);
        fx.spawn(VanillaFx.primedTnt(x, -24f, FLOOR, 22f, 2.2f + random.nextFloat() * 1.2f)
                .velocity((random.nextFloat() - 0.5f) * 80f, 0f)
                .onExplode(() -> stage.play(VanillaFx.shake(stage, 6f, 0.5f))));
    }

    /** Небо, звёзды и два ряда блоков: трава/земля в середине, незерак слева, песок душ справа. */
    private static void paintScene(Canvas canvas, float w, float h, float t) {
        canvas.fillRect(0, 0, w, h, Brush.vertical(0xFF0B1026, 0xFF3B2F5C));
        for (int i = 0; i < 24; i++) {
            float sx = (i * 97.13f) % w;
            float sy = (i * 41.7f) % (FLOOR - 20f);
            float tw = 0.5f + 0.5f * (float) Math.sin(t * 2f + i);
            canvas.fillRect(sx, sy, 1f, 1f, Color.withAlpha(Color.WHITE, 0.3f + 0.6f * tw));
        }
        int cols = (int) Math.ceil(w / BLOCK);
        for (int i = 0; i < cols; i++) {
            float x = i * BLOCK;
            float f = x / w;
            TextureRef top = f < 0.33f ? NETHERRACK : f > 0.66f ? SOUL_SOIL : GRASS_TOP;
            TextureRef under = f < 0.33f ? NETHERRACK : f > 0.66f ? SOUL_SOIL : DIRT;
            canvas.drawTexture(top, x, FLOOR, BLOCK, BLOCK, Color.WHITE);
            canvas.drawTexture(under, x, FLOOR + BLOCK, BLOCK, BLOCK, 0xFFB0B0B0);
        }
    }

    private static Sketch spriteLoop(SpriteSheet sheet, float size) {
        return new Sketch(size, size, (c, w, h, t) -> sheet.draw(c, sheet.frameAt(t), 0, 0, w, h, Color.WHITE));
    }
}
