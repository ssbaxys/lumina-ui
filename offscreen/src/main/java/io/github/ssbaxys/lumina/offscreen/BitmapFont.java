package io.github.ssbaxys.lumina.offscreen;

import io.github.ssbaxys.lumina.render.TextMetrics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Пиксельный шрифт игры по {@code assets/minecraft/font/default.json}: те же глифы, ширины и коды {@code §},
 * что у {@code Font} в Minecraft. Строка высотой 9, тень сдвинута на 1 пиксель и темнее в 4 раза.
 */
public final class BitmapFont implements TextMetrics {
    /** Глиф: маска ячейки, масштаб, ширина в пикселях ячейки, сдвиг по вертикали, шаг. */
    record Glyph(boolean[] mask, int cellW, int cellH, float scale, int width, float up, float advance) {
    }

    static final int[] COLORS = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    private final Map<Integer, Glyph> glyphs = new HashMap<>();
    private final Map<Integer, Float> spaces = new HashMap<>();

    public BitmapFont(Assets assets) {
        load(assets, "minecraft:default", 0);
    }

    @SuppressWarnings("unchecked")
    private void load(Assets assets, String id, int depth) {
        if (depth > 4) {
            return;
        }
        Map<String, Object> json = assets.json(Assets.path(id.replace(":", ":font/") + ".json"));
        if (json == null) {
            return;
        }
        for (Object o : (List<Object>) json.get("providers")) {
            Map<String, Object> p = (Map<String, Object>) o;
            String type = (String) p.get("type");
            if ("reference".equals(type)) {
                load(assets, (String) p.get("id"), depth + 1);
            } else if ("space".equals(type)) {
                for (Map.Entry<String, Object> e : ((Map<String, Object>) p.get("advances")).entrySet()) {
                    spaces.putIfAbsent(e.getKey().codePointAt(0), ((Double) e.getValue()).floatValue());
                }
            } else if ("bitmap".equals(type)) {
                String file = (String) p.get("file");
                int c = file.indexOf(':');
                Assets.Pixels img = assets.image(file.substring(0, c) + ":textures/" + file.substring(c + 1));
                if (img == null) {
                    continue;
                }
                List<String> rows = (List<String>) p.get("chars");
                int height = p.get("height") == null ? 8 : ((Double) p.get("height")).intValue();
                int ascent = ((Double) p.get("ascent")).intValue();
                int cols = rows.get(0).codePointCount(0, rows.get(0).length());
                int cellW = img.width() / cols;
                int cellH = img.height() / rows.size();
                float scale = height / (float) cellH;
                for (int r = 0; r < rows.size(); r++) {
                    int[] cps = rows.get(r).codePoints().toArray();
                    for (int col = 0; col < cps.length; col++) {
                        int cp = cps[col];
                        if (cp == 0 || glyphs.containsKey(cp) || spaces.containsKey(cp)) {
                            continue;
                        }
                        boolean[] mask = new boolean[cellW * cellH];
                        int width = 0;
                        for (int y = 0; y < cellH; y++) {
                            for (int x = 0; x < cellW; x++) {
                                if ((img.at(col * cellW + x, r * cellH + y) >>> 24) != 0) {
                                    mask[y * cellW + x] = true;
                                    width = Math.max(width, x + 1);
                                }
                            }
                        }
                        float advance = (int) (0.5f + width * scale) + 1;
                        glyphs.put(cp, new Glyph(mask, cellW, cellH, scale, width, 7 - ascent, advance));
                    }
                }
            }
        }
    }

    Glyph glyph(int cp) {
        Glyph g = glyphs.get(cp);
        return g != null ? g : glyphs.get((int) '?');
    }

    float advance(int cp, boolean bold) {
        Float s = spaces.get(cp);
        if (s != null) {
            return s;
        }
        Glyph g = glyph(cp);
        return (g == null ? 0f : g.advance()) + (bold ? 1f : 0f);
    }

    @Override
    public float width(String text) {
        float w = 0f;
        boolean bold = false;
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '§' && i < text.length()) {
                char f = Character.toLowerCase(text.charAt(i++));
                if (f == 'l') {
                    bold = true;
                } else if (f == 'r' || "0123456789abcdef".indexOf(f) >= 0) {
                    bold = false;
                }
                continue;
            }
            w += advance(cp, bold);
        }
        return w;
    }

    @Override
    public float lineHeight() {
        return 9f;
    }
}
