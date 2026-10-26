package io.github.ssbaxys.lumina.offscreen;

import io.github.ssbaxys.lumina.platform.Platform;
import io.github.ssbaxys.lumina.render.ImageTexture;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;

/**
 * Платформа без игры: шрифт и текстуры из {@link Assets}, свои часы (кадр можно «перемотать» на нужное
 * время анимации), тишина вместо звуков.
 */
public final class OffscreenPlatform implements Platform {
    private final Assets assets;
    private final BitmapFont font;
    private final SoftwareCanvas canvas;
    private String clipboard = "";
    private final java.util.Map<String, java.util.Map<String, Object>> langs = new java.util.HashMap<>();
    private long nanos = 1_000_000_000L;

    public OffscreenPlatform(Assets assets) {
        this.assets = assets;
        this.font = new BitmapFont(assets);
        this.canvas = new SoftwareCanvas(assets, font);
    }

    public SoftwareCanvas canvas() {
        return canvas;
    }

    public BitmapFont font() {
        return font;
    }

    /** Сдвинуть часы платформы. */
    public void advance(float seconds) {
        nanos += (long) (seconds * 1e9);
    }

    @Override
    public long nanoTime() {
        return nanos;
    }

    @Override
    public TextMetrics textMetrics() {
        return font;
    }

    @Override
    public String getClipboard() {
        return clipboard;
    }

    @Override
    public void setClipboard(String text) {
        clipboard = text == null ? "" : text;
    }

    @Override
    public void playClickSound() {
    }

    @Override
    @SuppressWarnings("unchecked")
    public String translate(String language, String key) {
        java.util.Map<String, Object> table = langs.computeIfAbsent(language, l -> {
            java.util.Map<String, Object> m = assets.json("assets/minecraft/lang/" + l + ".json");
            return m == null ? java.util.Map.of() : m;
        });
        Object v = table.get(key);
        return v instanceof String str ? str : null;
    }

    @Override
    public byte[] readResourceBytes(String id) {
        return assets.bytes(Assets.path(id));
    }

    @Override
    public TextureRef createTexture(String key, int width, int height, int[] argb) {
        String id = "lumina:dynamic/" + key;
        canvas.registerTexture(id, width, height, argb);
        return new ImageTexture(id, width, height);
    }
}
