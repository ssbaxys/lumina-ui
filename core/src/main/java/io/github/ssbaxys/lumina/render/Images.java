package io.github.ssbaxys.lumina.render;

import io.github.ssbaxys.lumina.Lumina;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

/**
 * Картинки из любых файлов: PNG, JPG, GIF, BMP. Анимированный GIF (больше одного кадра) загружается
 * целиком и возвращается как {@link AnimatedTexture}: {@code Image} проигрывает его сам. Декодирует {@code javax.imageio} в ядре,
 * в видеопамять загружает платформа ({@link io.github.ssbaxys.lumina.platform.Platform#createTexture}).
 *
 * <pre>{@code
 * TextureRef photo = Images.fromFile(Path.of("screenshots/2024-01-01.png"));
 * TextureRef logo = Images.fromResource("mymod:textures/gui/logo.jpg");
 * }</pre>
 *
 * Если платформа не умеет создавать текстуры, методы возвращают {@code null} (PNG из ресурсов —
 * ссылку на сам ресурс: его игра загрузит сама). Результаты кэшируются по ключу.
 */
public final class Images {
    /** Не больше этого по каждой стороне: защита от гигантских файлов. */
    public static final int MAX_SIZE = 8192;

    /** Раскодированная картинка: пиксели ARGB построчно сверху вниз. */
    public record Pixels(int width, int height, int[] argb) {
        public int pixel(int x, int y) {
            return argb[y * width + x];
        }
    }

    private static final Map<String, TextureRef> CACHE = new HashMap<>();
    private static volatile boolean headlessChecked;

    private Images() {
    }

    // ---------------------------------------------------------------- декодирование

    /** Раскодировать байты файла картинки. */
    public static Pixels decode(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length == 0) {
            throw new IOException("Empty image data");
        }
        ensureHeadless();
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (in == null) {
                throw new IOException("Cannot open image stream");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("Unsupported image format");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int w = reader.getWidth(0);
                int h = reader.getHeight(0);
                if (w <= 0 || h <= 0 || w > MAX_SIZE || h > MAX_SIZE) {
                    throw new IOException("Unsupported image size " + w + "x" + h);
                }
                BufferedImage image = reader.read(0);
                int iw = image.getWidth();
                int ih = image.getHeight();
                return new Pixels(iw, ih, image.getRGB(0, 0, iw, ih, null, 0, iw));
            } finally {
                reader.dispose();
            }
        }
    }

    /** AWT нужен ImageIO только как библиотека: без окон и без конфликта с GLFW (особенно на macOS). */
    static void ensureHeadless() {
        if (!headlessChecked) {
            synchronized (Images.class) {
                if (System.getProperty("java.awt.headless") == null) {
                    System.setProperty("java.awt.headless", "true");
                }
                headlessChecked = true;
            }
        }
    }

    // ---------------------------------------------------------------- текстуры

    /** Загрузить готовые пиксели как текстуру; {@code null}, если платформа не умеет. */
    public static TextureRef upload(String key, Pixels pixels) {
        if (!Lumina.isInitialized() || pixels == null) {
            return null;
        }
        TextureRef ref = Lumina.platform().createTexture(safeKey(key), pixels.width(), pixels.height(), pixels.argb());
        if (ref == null) {
            return null;
        }
        return ref instanceof ImageTexture ? ref : new ImageTexture(ref.id(), pixels.width(), pixels.height());
    }

    /** Картинка из байтов файла под ключом {@code key} (одинаковый ключ — та же текстура из кэша). */
    public static TextureRef fromBytes(String key, byte[] bytes) {
        String cacheKey = "bytes:" + key;
        TextureRef cached = cached(cacheKey);
        if (cached != null) {
            return cached;
        }
        try {
            return remember(cacheKey, load(key, bytes));
        } catch (IOException | RuntimeException e) {
            System.err.println("[Lumina images] " + key + ": " + e.getMessage());
            return null;
        }
    }

    /** Картинка или, если это GIF с несколькими кадрами, {@link AnimatedTexture}. */
    private static TextureRef load(String key, byte[] bytes) throws IOException {
        if (Gif.isGif(bytes)) {
            Gif.Animation anim = Gif.decode(bytes);
            if (anim.frameCount() > 1) {
                return Gif.upload(key, anim);
            }
        }
        return upload(key, decode(bytes));
    }

    /** Картинка с диска. Изменённый файл (по времени изменения) перечитывается. */
    public static TextureRef fromFile(Path path) {
        Path abs = path.toAbsolutePath().normalize();
        String key = "file/" + abs.getFileName() + "-" + Integer.toHexString(abs.toString().hashCode());
        try {
            String cacheKey = "file:" + abs + "@" + Files.getLastModifiedTime(abs).toMillis();
            TextureRef cached = cached(cacheKey);
            if (cached != null) {
                return cached;
            }
            return remember(cacheKey, load(key, Files.readAllBytes(abs)));
        } catch (IOException | RuntimeException e) {
            System.err.println("[Lumina images] " + abs + ": " + e);
            return null;
        }
    }

    public static TextureRef fromFile(String path) {
        return fromFile(Path.of(path));
    }

    /**
     * Картинка из ресурсов "мод:путь" (через ресурспаки игры). PNG, если платформа не создаёт текстуры,
     * рисуется прямо по идентификатору ресурса.
     */
    public static TextureRef fromResource(String id) {
        String cacheKey = "res:" + id;
        TextureRef cached = cached(cacheKey);
        if (cached != null) {
            return cached;
        }
        byte[] bytes = Lumina.isInitialized() ? Lumina.platform().readResourceBytes(id) : null;
        if (bytes == null) {
            return TextureRef.of(id);
        }
        try {
            if (Gif.isGif(bytes)) {
                Gif.Animation anim = Gif.decode(bytes);
                if (anim.frameCount() > 1) {
                    return remember(cacheKey, Gif.upload("res/" + id, anim));
                }
            }
            Pixels pixels = decode(bytes);
            boolean png = id.toLowerCase(Locale.ROOT).endsWith(".png");
            // PNG игра грузит сама (и перезагружает с ресурспаками): в память второй раз не кладём.
            TextureRef ref = png ? new ImageTexture(id, pixels.width(), pixels.height()) : upload("res/" + id, pixels);
            return remember(cacheKey, ref);
        } catch (IOException | RuntimeException e) {
            System.err.println("[Lumina images] " + id + ": " + e.getMessage());
            return null;
        }
    }

    /** Забыть всё загруженное (следующий запрос прочитает файлы заново). */
    public static synchronized void clearCache() {
        CACHE.clear();
    }

    private static synchronized TextureRef cached(String key) {
        return CACHE.get(key);
    }

    private static synchronized TextureRef remember(String key, TextureRef ref) {
        if (ref != null) {
            CACHE.put(key, ref);
        }
        return ref;
    }

    /** Ключ, пригодный для пути ресурса игры: строчные латинские буквы, цифры и {@code / . _ -}. */
    public static String safeKey(String key) {
        String k = key == null || key.isEmpty() ? "image" : key.toLowerCase(Locale.ROOT);
        StringBuilder sb = new StringBuilder(k.length());
        for (int i = 0; i < k.length(); i++) {
            char c = k.charAt(i);
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '/' || c == '.' || c == '_' || c == '-';
            sb.append(ok ? c : '_');
        }
        return sb.toString();
    }
}
