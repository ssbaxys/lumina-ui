package io.github.ssbaxys.lumina.offscreen;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ресурсы игры для рендера без Minecraft: папка с распакованными {@code assets/} (например, из
 * {@code client-extra-*.jar} сборки) плюс ресурсы на classpath (ассеты модов).
 */
public final class Assets {
    /** Картинка в памяти: ARGB построчно. */
    public record Pixels(int width, int height, int[] argb) {
        int at(int x, int y) {
            return argb[y * width + x];
        }
    }

    private final Path root;
    private final java.util.zip.ZipFile zip;
    private final Map<String, Pixels> images = new HashMap<>();
    private final Map<String, Map<String, Object>> models = new HashMap<>();

    /**
     * @param root каталог, внутри которого лежит {@code assets/<namespace>/...}, или jar/zip с ними
     *             (например, {@code client-extra-*.jar} из сборки загрузчика); может быть {@code null}
     */
    public Assets(Path root) {
        java.util.zip.ZipFile z = null;
        if (root != null && Files.isRegularFile(root)) {
            try {
                z = new java.util.zip.ZipFile(root.toFile());
            } catch (IOException e) {
                throw new IllegalArgumentException("Cannot open " + root, e);
            }
        }
        this.zip = z;
        this.root = z == null ? root : null;
    }

    /** Байты по пути внутри ресурсов ({@code assets/minecraft/...}) или {@code null}. */
    public byte[] bytes(String path) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = Assets.class.getClassLoader();
        }
        try (InputStream in = cl.getResourceAsStream(path)) {
            if (in != null) {
                return in.readAllBytes();
            }
        } catch (IOException ignored) {
        }
        if (zip != null) {
            java.util.zip.ZipEntry e = zip.getEntry(path);
            if (e != null) {
                try (InputStream in = zip.getInputStream(e)) {
                    return in.readAllBytes();
                } catch (IOException ignored) {
                }
            }
        }
        if (root != null) {
            Path p = root.resolve(path);
            if (Files.isRegularFile(p)) {
                try {
                    return Files.readAllBytes(p);
                } catch (IOException ignored) {
                }
            }
        }
        return null;
    }

    /** "ns:path" → "assets/ns/path". */
    public static String path(String id) {
        int c = id.indexOf(':');
        return c < 0 ? "assets/minecraft/" + id : "assets/" + id.substring(0, c) + "/" + id.substring(c + 1);
    }

    /** Картинка по id ресурса ("minecraft:textures/block/tnt_side.png") или {@code null}. */
    public Pixels image(String id) {
        return images.computeIfAbsent(id, k -> {
            byte[] b = bytes(path(k));
            if (b == null) {
                return null;
            }
            try {
                BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(b));
                if (img == null) {
                    return null;
                }
                int w = img.getWidth();
                int h = img.getHeight();
                return new Pixels(w, h, img.getRGB(0, 0, w, h, null, 0, w));
            } catch (IOException e) {
                return null;
            }
        });
    }

    public void put(String id, Pixels pixels) {
        images.put(id, pixels);
    }

    @SuppressWarnings("unchecked")
    Map<String, Object> json(String path) {
        byte[] b = bytes(path);
        return b == null ? null : (Map<String, Object>) MiniJson.parse(new String(b, StandardCharsets.UTF_8));
    }

    /** Модель предмета/блока с развёрнутыми родителями: текстуры слиты, ключ {@code "_parents"} — цепочка. */
    @SuppressWarnings("unchecked")
    Map<String, Object> model(String id) {
        return models.computeIfAbsent(id, k -> {
            Map<String, Object> merged = new HashMap<>();
            Map<String, String> textures = new HashMap<>();
            java.util.ArrayList<String> parents = new java.util.ArrayList<>();
            String cur = k;
            for (int guard = 0; cur != null && guard < 16; guard++) {
                String ns = cur.contains(":") ? cur : "minecraft:" + cur;
                parents.add(ns);
                Map<String, Object> m = json(path(ns.replace(":", ":models/") + ".json"));
                if (m == null) {
                    break;
                }
                Object t = m.get("textures");
                if (t instanceof Map<?, ?> tm) {
                    for (Map.Entry<?, ?> e : tm.entrySet()) {
                        textures.putIfAbsent((String) e.getKey(), (String) e.getValue());
                    }
                }
                if (!merged.containsKey("elements") && m.get("elements") != null) {
                    merged.put("elements", m.get("elements"));
                }
                if (!merged.containsKey("display") && m.get("display") != null) {
                    merged.put("display", m.get("display"));
                }
                cur = (String) m.get("parent");
            }
            merged.put("textures", textures);
            merged.put("_parents", parents);
            return merged;
        });
    }

    /** Ссылка вида "#side" → "minecraft:block/grass_block_side". */
    @SuppressWarnings("unchecked")
    static String resolveTexture(Map<String, Object> model, String ref) {
        Map<String, String> t = (Map<String, String>) model.get("textures");
        for (int guard = 0; ref != null && ref.startsWith("#") && guard < 16; guard++) {
            ref = t.get(ref.substring(1));
        }
        if (ref == null) {
            return null;
        }
        return ref.contains(":") ? ref : "minecraft:" + ref;
    }

    /** Текстура "minecraft:block/dirt" → картинка. */
    Pixels sprite(String textureId) {
        if (textureId == null) {
            return null;
        }
        int c = textureId.indexOf(':');
        return image(textureId.substring(0, c) + ":textures/" + textureId.substring(c + 1) + ".png");
    }

    @SuppressWarnings("unchecked")
    static List<String> parents(Map<String, Object> model) {
        return (List<String>) model.get("_parents");
    }
}
