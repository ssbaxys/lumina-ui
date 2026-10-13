package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.blocks.BlockModel;
import io.github.ssbaxys.lumina.blocks.BlockStructure;
import io.github.ssbaxys.lumina.blocks.WorldBlocks;
import io.github.ssbaxys.lumina.blocks.WorldCapture;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;

import java.util.function.Supplier;

/**
 * Превью мира вокруг игрока в 3D: коробка блоков (радиус по сторонам, высота вниз и вверх от ног) снимается
 * с клиентского мира порциями по кадрам ({@link WorldCapture}) и рисуется как {@link BlockModel} — с вращением
 * мышью, приближением, перспективой. Сверху — метка игрока со стрелкой взгляда.
 *
 * <ul>
 *   <li>{@link #refresh()} перечитывает коробку по запросу, {@link #refreshInterval(float)} — сам раз в N секунд;</li>
 *   <li>{@link #follow(boolean)} — коробка сдвигается за игроком, когда он отошёл на {@link #FOLLOW_STEP} блока;</li>
 *   <li>{@link #peel(int)} снимает верхние слои, {@link #cutAtPlayer(boolean)} — срез по голове игрока
 *       (заглянуть под крышу), {@link #cutAbove(int)} — по мировой высоте.</li>
 * </ul>
 *
 * <pre>{@code
 * new StructurePreview().radius(10).height(6, 8)     // 21×15×21 блоков вокруг игрока
 *         .fallback(new DemoWorld(42))               // без мира (главное меню) — выдуманный
 *         .follow(true).refreshInterval(2f)
 *         .perspective(0.4f).size(240, 200);
 * }</pre>
 *
 * Мир берётся из {@code Platform.worldBlocks()} (свой источник — {@link #source(Supplier)}).
 */
public class StructurePreview extends BlockModel {
    /** Радиус по умолчанию, блоков. */
    public static final int DEFAULT_RADIUS = 8;
    /** На сколько блоков игрок отходит от середины коробки, прежде чем она сдвинется за ним. */
    public static final int FOLLOW_STEP = 2;
    /** Цвет метки игрока по умолчанию. */
    public static final int DEFAULT_MARKER_COLOR = 0xFFFF4D4D;

    private static final Supplier<WorldBlocks> PLATFORM =
            () -> Lumina.isInitialized() ? Lumina.platform().worldBlocks() : null;

    private final WorldCapture capture = new WorldCapture();
    private final float[] p0 = new float[3];
    private final float[] p1 = new float[3];
    private final float[] p2 = new float[3];
    private Supplier<WorldBlocks> source = PLATFORM;
    private WorldBlocks fallback;
    private WorldBlocks current;
    private boolean live;

    private int radius = DEFAULT_RADIUS;
    private int below = 6;
    private int above = 8;
    private boolean follow = true;
    private float interval = 2f;
    private float idle;
    private boolean refreshRequested = true;
    private Runnable onPass;

    private int peel;
    private int cutWorldY = Integer.MAX_VALUE;
    private boolean cutAtPlayer;

    private boolean showMarker = true;
    private int markerColor = DEFAULT_MARKER_COLOR;
    private boolean hasPlayer;
    private double playerX;
    private double playerY;
    private double playerZ;
    private float playerYaw;

    public StructurePreview() {
        super("structure-preview", null);
        structure(capture.structure());
        camera(45f, 35f);
    }

    // ---------------------------------------------------------------- настройка

    /** Откуда брать мир (по умолчанию — {@code Platform.worldBlocks()}); {@code null} от источника — мира нет. */
    public StructurePreview source(Supplier<WorldBlocks> source) {
        this.source = source == null ? PLATFORM : source;
        refreshRequested = true;
        return this;
    }

    /** Мир, когда источник ничего не дал (главное меню): например, {@code DemoWorld}. {@code null} — пусто. */
    public StructurePreview fallback(WorldBlocks fallback) {
        this.fallback = fallback;
        refreshRequested = true;
        return this;
    }

    /** Радиус по сторонам от игрока, блоков (1–23: коробка до 47 в ширину). */
    public StructurePreview radius(int blocks) {
        this.radius = Math.max(1, Math.min((WorldCapture.MAX_SIZE - 1) / 2, blocks));
        refreshRequested = true;
        return this;
    }

    public int radius() {
        return radius;
    }

    /** Сколько слоёв ниже ног игрока и выше их (вместе со слоем ног — не больше 48). */
    public StructurePreview height(int below, int above) {
        this.below = Math.max(0, below);
        this.above = Math.max(0, above);
        int total = this.below + this.above + 1;
        if (total > WorldCapture.MAX_SIZE) {
            this.above = Math.max(0, WorldCapture.MAX_SIZE - 1 - this.below);
            this.below = WorldCapture.MAX_SIZE - 1 - this.above;
        }
        refreshRequested = true;
        return this;
    }

    public int below() {
        return below;
    }

    public int above() {
        return above;
    }

    /** Сдвигать коробку за игроком (по умолчанию да); без этого она стоит, где снята, до {@link #refresh()}. */
    public StructurePreview follow(boolean follow) {
        this.follow = follow;
        return this;
    }

    public boolean follows() {
        return follow;
    }

    /** Перечитывать коробку раз в {@code seconds} (по умолчанию 2 с); 0 — только по {@link #refresh()}. */
    public StructurePreview refreshInterval(float seconds) {
        this.interval = Math.max(0f, seconds);
        return this;
    }

    public float refreshInterval() {
        return interval;
    }

    /** Сколько блоков читать за кадр (по умолчанию 8192). */
    public StructurePreview budget(int blocks) {
        capture.budget(blocks);
        return this;
    }

    /** Вызывается, когда очередной проход по коробке закончен (снимок полный). */
    public StructurePreview onRefresh(Runnable listener) {
        this.onPass = listener;
        return this;
    }

    /** Перечитать коробку заново, поставив её вокруг игрока (даже если {@link #follow} выключен). */
    public StructurePreview refresh() {
        refreshRequested = true;
        return this;
    }

    /** Показывать метку игрока (по умолчанию да). */
    public StructurePreview showMarker(boolean show) {
        this.showMarker = show;
        return this;
    }

    public StructurePreview markerColor(int argb) {
        this.markerColor = argb;
        return this;
    }

    // ---------------------------------------------------------------- срез

    /** Снять {@code layers} верхних слоёв коробки (0 — показать все). */
    public StructurePreview peel(int layers) {
        this.peel = Math.max(0, Math.min(WorldCapture.MAX_SIZE - 1, layers));
        this.cutWorldY = Integer.MAX_VALUE;
        this.cutAtPlayer = false;
        applyCut();
        return this;
    }

    /** Сколько верхних слоёв снято. */
    public int peel() {
        return peel;
    }

    /** Срез по голове игрока: видно всё до его роста, крыша и потолок пещеры сняты. */
    public StructurePreview cutAtPlayer(boolean on) {
        this.cutAtPlayer = on;
        if (on) {
            this.peel = 0;
            this.cutWorldY = Integer.MAX_VALUE;
        }
        applyCut();
        return this;
    }

    public boolean cutsAtPlayer() {
        return cutAtPlayer;
    }

    /** Показывать слои до мировой высоты {@code worldY} включительно ({@link Integer#MAX_VALUE} — все). */
    @Override
    public BlockModel cutAbove(int worldY) {
        this.cutWorldY = worldY;
        this.peel = 0;
        this.cutAtPlayer = false;
        applyCut();
        return this;
    }

    private void applyCut() {
        BlockStructure.Bounds box = capture.localBox();
        int cut = Integer.MAX_VALUE;
        if (box != null) {
            if (cutAtPlayer && hasPlayer) {
                cut = (int) Math.floor(playerY) + 1 - capture.anchorY();
            } else if (cutWorldY != Integer.MAX_VALUE) {
                cut = (int) Math.max(Integer.MIN_VALUE + 1L, (long) cutWorldY - capture.anchorY());
            } else if (peel > 0) {
                cut = box.maxY() - peel;
            }
        }
        if (cut != cutAboveLayer()) {
            super.cutAbove(cut);
        }
    }

    // ---------------------------------------------------------------- состояние

    /** Снимок мира (координаты — мировые минус якорь, см. {@link WorldCapture}). */
    public WorldCapture capture() {
        return capture;
    }

    /** Показан настоящий мир (а не {@link #fallback} и не пустота). */
    public boolean hasWorld() {
        return live;
    }

    /** Мир, из которого читался последний кадр, или {@code null}. */
    public WorldBlocks world() {
        return current;
    }

    public boolean hasPlayer() {
        return hasPlayer;
    }

    /** Положение игрока в последнем кадре: {@code {x, y, z, yaw}}. */
    public double[] player() {
        return new double[]{playerX, playerY, playerZ, playerYaw};
    }

    /** Копия снимка от угла коробки — например, чтобы поделиться ей ({@link BlockStructure#encode()}). */
    public BlockStructure snapshot() {
        return capture.snapshot();
    }

    // ---------------------------------------------------------------- кадр

    @Override
    public boolean tick(float dt) {
        boolean alive = super.tick(dt);
        if (alive) {
            poll(dt);
        }
        return alive;
    }

    /**
     * Шаг снимка: узнать мир и игрока, при необходимости сдвинуть коробку и прочитать очередную порцию блоков.
     * Вызывается сам на каждом кадре, пока виджет на экране; без экрана (тесты) — вручную.
     *
     * @param dt прошедшее время, секунды (для {@link #refreshInterval})
     */
    public StructurePreview poll(float dt) {
        WorldBlocks fromSource = source.get();
        WorldBlocks w = fromSource != null ? fromSource : fallback;
        live = fromSource != null;
        if (w != current) {
            // другой мир (вошли, вышли, сменили измерение): старый снимок не годится
            current = w;
            capture.clear();
            refreshRequested = true;
        }
        if (w == null) {
            hasPlayer = false;
            frame(null);
            return this;
        }
        hasPlayer = w.hasPlayer();
        if (hasPlayer) {
            playerX = w.playerX();
            playerY = w.playerY();
            playerZ = w.playerZ();
            playerYaw = w.playerYaw();
        }
        if (refreshRequested || !capture.hasBox()) {
            refreshRequested = false;
            recentre(true);
        } else if (follow && hasPlayer && farFromCentre()) {
            recentre(false);
        } else if (!capture.isPassing()) {
            idle += dt;
            // незагруженные чанки дочитываются через секунду, остальное — раз в interval
            boolean missing = capture.unloadedCount() > 0 && idle >= 1f;
            if (interval > 0f && idle >= interval || missing) {
                if (follow) {
                    recentre(true);
                } else {
                    capture.restart();
                }
            }
        }
        frame(capture.localBox());
        applyCut();
        if (capture.step(w)) {
            idle = 0f;
            if (onPass != null) {
                onPass.run();
            }
        }
        return this;
    }

    /** Поставить коробку вокруг игрока; {@code reread} — перечитать, даже если коробка та же. */
    private void recentre(boolean reread) {
        if (hasPlayer) {
            int bx = (int) Math.floor(playerX);
            int by = (int) Math.floor(playerY);
            int bz = (int) Math.floor(playerZ);
            boolean same = capture.hasBox() && sameBox(bx, by, bz);
            capture.around(bx, by, bz, radius, below, above);
            if (same && reread) {
                capture.restart();
            }
        } else if (capture.hasBox()) {
            capture.restart();
        } else {
            capture.around(0, 64, 0, radius, below, above);
        }
        idle = 0f;
    }

    private boolean sameBox(int bx, int by, int bz) {
        BlockStructure.Bounds b = capture.worldBox();
        return b.minX() == bx - radius && b.minY() == by - below && b.minZ() == bz - radius
                && b.sizeX() == 2 * radius + 1 && b.sizeY() == below + above + 1;
    }

    private boolean farFromCentre() {
        BlockStructure.Bounds b = capture.worldBox();
        int cx = b.minX() + radius;
        int cy = b.minY() + below;
        int cz = b.minZ() + radius;
        return Math.abs(Math.floor(playerX) - cx) >= FOLLOW_STEP || Math.abs(Math.floor(playerY) - cy) >= FOLLOW_STEP
                || Math.abs(Math.floor(playerZ) - cz) >= FOLLOW_STEP;
    }

    // ---------------------------------------------------------------- рисование

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        super.paintContent(canvas, partialTick);
        if (showMarker && hasPlayer && capture.hasBox()) {
            canvas.save();
            canvas.clipRect(0f, 0f, width(), height());
            paintMarker(canvas);
            canvas.restore();
        }
    }

    /** Метка игрока: стрелка взгляда на уровне ног, столбик в рост и точка-голова. */
    private void paintMarker(Canvas canvas) {
        float x = (float) (playerX - capture.anchorX());
        float y = (float) (playerY - capture.anchorY());
        float z = (float) (playerZ - capture.anchorZ());
        double yaw = Math.toRadians(playerYaw);
        float fx = (float) -Math.sin(yaw);
        float fz = (float) Math.cos(yaw);
        float gy = y + 0.05f;
        if (!project(x + fx * 0.9f, gy, z + fz * 0.9f, p0)
                || !project(x - fz * 0.45f - fx * 0.35f, gy, z + fx * 0.45f - fz * 0.35f, p1)
                || !project(x + fz * 0.45f - fx * 0.35f, gy, z - fx * 0.45f - fz * 0.35f, p2)) {
            return;
        }
        canvas.fillTriangle(p0[0], p0[1], p1[0], p1[1], p2[0], p2[1], markerColor);
        project(x, gy, z, p0);
        project(x, y + 1.8f, z, p1);
        canvas.drawLine(p0[0], p0[1], p1[0], p1[1], 2f, markerColor);
        float r = 3.5f;
        canvas.fillCircle(p1[0], p1[1], r + 1f, Brush.solid(0xFFFFFFFF));
        canvas.fillCircle(p1[0], p1[1], r, Brush.solid(markerColor));
    }
}
