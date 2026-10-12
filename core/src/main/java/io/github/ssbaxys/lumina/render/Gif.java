package io.github.ssbaxys.lumina.render;

import io.github.ssbaxys.lumina.Lumina;
import org.w3c.dom.Node;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Анимированный GIF: раскодирование всех кадров (с правилами наложения кадров и задержками) через
 * {@code javax.imageio}, загрузка кадров текстурами и запись GIF (для примеров и тестов).
 *
 * <pre>{@code
 * Gif.Animation anim = Gif.decode(bytes);          // кадры уже собраны в полный размер
 * AnimatedTexture tex = Gif.upload("spinner", anim); // по текстуре на кадр (null без платформы)
 * }</pre>
 *
 * Видео не поддерживается: в ядре нет декодера видео, а тянуть FFmpeg/JavaCV в мод на две версии игры
 * слишком тяжело. Короткие ролики проще перевести в GIF или в полосу кадров ({@code fx.SpriteSheet}).
 */
public final class Gif {
    /** Больше кадров не читаем: защита от гигантских файлов. */
    public static final int MAX_FRAMES = 512;
    /** Предел суммы пикселей всех кадров (память и видеопамять). */
    public static final long MAX_TOTAL_PIXELS = 48L * 1024 * 1024;
    /** Задержка для кадров с нулевой задержкой (как в браузерах). */
    public static final int DEFAULT_DELAY_MS = 100;

    private static final String IMAGE_FORMAT = "javax_imageio_gif_image_1.0";
    private static final String STREAM_FORMAT = "javax_imageio_gif_stream_1.0";

    /** Кадр: пиксели во весь размер картинки и сколько он показывается. */
    public record Frame(Images.Pixels pixels, int delayMs) {
    }

    /**
     * Раскодированная анимация.
     *
     * @param loopCount сколько раз проигрывать; 0 — бесконечно
     */
    public record Animation(int width, int height, List<Frame> frames, int loopCount) {
        public int frameCount() {
            return frames.size();
        }

        /** Длительность одного проигрывания в миллисекундах. */
        public long durationMs() {
            long t = 0L;
            for (Frame f : frames) {
                t += f.delayMs();
            }
            return t;
        }
    }

    private Gif() {
    }

    /** Похоже ли на GIF (по сигнатуре "GIF87a"/"GIF89a"). */
    public static boolean isGif(byte[] bytes) {
        return bytes != null && bytes.length >= 6 && bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F'
                && bytes[3] == '8' && (bytes[4] == '7' || bytes[4] == '9') && bytes[5] == 'a';
    }

    // ================================================================ чтение

    /** Раскодировать GIF целиком: каждый кадр уже наложен на предыдущие по правилам GIF. */
    public static Animation decode(byte[] bytes) throws IOException {
        if (!isGif(bytes)) {
            throw new IOException("Not a GIF");
        }
        Images.ensureHeadless();
        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("gif");
        if (!readers.hasNext()) {
            throw new IOException("No GIF reader");
        }
        ImageReader reader = readers.next();
        try (ImageInputStream in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            reader.setInput(in, false, false);
            int count = Math.min(reader.getNumImages(true), MAX_FRAMES);
            if (count <= 0) {
                throw new IOException("GIF without frames");
            }
            int[] size = screenSize(reader);
            int w = size[0];
            int h = size[1];
            if (w <= 0 || h <= 0) {
                BufferedImage first = reader.read(0);
                w = first.getWidth();
                h = first.getHeight();
            }
            if (w > Images.MAX_SIZE || h > Images.MAX_SIZE) {
                throw new IOException("Unsupported GIF size " + w + "x" + h);
            }
            long maxFrames = Math.max(1L, MAX_TOTAL_PIXELS / ((long) w * h));
            count = (int) Math.min(count, maxFrames);
            int loop = 0;
            int[] canvas = new int[w * h];
            List<Frame> frames = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                BufferedImage img = reader.read(i);
                IIOMetadataNode meta = (IIOMetadataNode) reader.getImageMetadata(i).getAsTree(IMAGE_FORMAT);
                IIOMetadataNode desc = child(meta, "ImageDescriptor");
                IIOMetadataNode gce = child(meta, "GraphicControlExtension");
                int left = intAttr(desc, "imageLeftPosition", 0);
                int top = intAttr(desc, "imageTopPosition", 0);
                String disposal = gce == null ? "none" : gce.getAttribute("disposalMethod");
                int delay = intAttr(gce, "delayTime", 0) * 10;
                if (i == 0) {
                    loop = loopCount(meta);
                }
                int[] before = "restoreToPrevious".equals(disposal) ? canvas.clone() : null;
                int fw = img.getWidth();
                int fh = img.getHeight();
                int[] px = img.getRGB(0, 0, fw, fh, null, 0, fw);
                for (int y = 0; y < fh; y++) {
                    int cy = top + y;
                    if (cy < 0 || cy >= h) {
                        continue;
                    }
                    for (int x = 0; x < fw; x++) {
                        int cx = left + x;
                        int c = px[y * fw + x];
                        if (cx >= 0 && cx < w && (c >>> 24) != 0) {
                            canvas[cy * w + cx] = c;
                        }
                    }
                }
                frames.add(new Frame(new Images.Pixels(w, h, canvas.clone()), delay <= 10 ? DEFAULT_DELAY_MS : delay));
                if ("restoreToBackgroundColor".equals(disposal)) {
                    for (int y = Math.max(0, top); y < Math.min(h, top + fh); y++) {
                        for (int x = Math.max(0, left); x < Math.min(w, left + fw); x++) {
                            canvas[y * w + x] = 0;
                        }
                    }
                } else if (before != null) {
                    canvas = before;
                }
            }
            return new Animation(w, h, List.copyOf(frames), loop);
        } catch (RuntimeException e) {
            throw new IOException("Broken GIF: " + e.getMessage(), e);
        } finally {
            reader.dispose();
        }
    }

    private static int[] screenSize(ImageReader reader) throws IOException {
        IIOMetadata stream = reader.getStreamMetadata();
        if (stream == null) {
            return new int[]{0, 0};
        }
        IIOMetadataNode root = (IIOMetadataNode) stream.getAsTree(STREAM_FORMAT);
        IIOMetadataNode lsd = child(root, "LogicalScreenDescriptor");
        return new int[]{intAttr(lsd, "logicalScreenWidth", 0), intAttr(lsd, "logicalScreenHeight", 0)};
    }

    /** Число повторов из расширения NETSCAPE2.0 (0 — бесконечно; нет расширения — один раз). */
    private static int loopCount(IIOMetadataNode meta) {
        IIOMetadataNode apps = child(meta, "ApplicationExtensions");
        if (apps == null) {
            return 1;
        }
        for (Node n = apps.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n instanceof IIOMetadataNode app && "NETSCAPE".equals(app.getAttribute("applicationID"))
                    && app.getUserObject() instanceof byte[] data && data.length >= 3) {
                return (data[1] & 0xFF) | (data[2] & 0xFF) << 8;
            }
        }
        return 1;
    }

    private static IIOMetadataNode child(IIOMetadataNode parent, String name) {
        if (parent == null) {
            return null;
        }
        for (Node n = parent.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (name.equals(n.getNodeName())) {
                return (IIOMetadataNode) n;
            }
        }
        return null;
    }

    private static int intAttr(IIOMetadataNode node, String name, int fallback) {
        if (node == null) {
            return fallback;
        }
        String v = node.getAttribute(name);
        try {
            return v == null || v.isEmpty() ? fallback : Integer.parseInt(v);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ================================================================ текстуры

    /**
     * Загрузить кадры текстурами ({@code ключ/0}, {@code ключ/1}...). {@code null}, если платформа не умеет
     * создавать текстуры.
     */
    public static AnimatedTexture upload(String key, Animation animation) {
        if (animation == null || !Lumina.isInitialized()) {
            return null;
        }
        int n = animation.frameCount();
        TextureRef[] textures = new TextureRef[n];
        int[] delays = new int[n];
        for (int i = 0; i < n; i++) {
            Frame f = animation.frames().get(i);
            TextureRef ref = Images.upload(key + "/" + i, f.pixels());
            if (ref == null) {
                return null;
            }
            textures[i] = ref;
            delays[i] = f.delayMs();
        }
        return new AnimatedTexture(animation.width(), animation.height(), textures, delays, animation.loopCount());
    }

    // ================================================================ запись

    /**
     * Записать анимированный GIF из кадров одного размера. Прозрачность не сохраняется (GIF-кодер JDK
     * строит палитру без неё), цвета сводятся к 256.
     *
     * @param loopCount 0 — повторять бесконечно
     */
    public static byte[] encode(List<Images.Pixels> frames, int delayMs, int loopCount) throws IOException {
        if (frames == null || frames.isEmpty()) {
            throw new IOException("No frames");
        }
        Images.ensureHeadless();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
        if (!writers.hasNext()) {
            throw new IOException("No GIF writer");
        }
        ImageWriter writer = writers.next();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            writer.prepareWriteSequence(null);
            for (int i = 0; i < frames.size(); i++) {
                Images.Pixels p = frames.get(i);
                BufferedImage img = new BufferedImage(p.width(), p.height(), BufferedImage.TYPE_INT_RGB);
                img.setRGB(0, 0, p.width(), p.height(), p.argb(), 0, p.width());
                IIOMetadata meta = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(img), param);
                IIOMetadataNode root = (IIOMetadataNode) meta.getAsTree(IMAGE_FORMAT);
                IIOMetadataNode gce = child(root, "GraphicControlExtension");
                if (gce == null) {
                    gce = new IIOMetadataNode("GraphicControlExtension");
                    root.appendChild(gce);
                }
                gce.setAttribute("disposalMethod", "none");
                gce.setAttribute("userInputFlag", "FALSE");
                gce.setAttribute("transparentColorFlag", "FALSE");
                gce.setAttribute("delayTime", Integer.toString(Math.max(1, delayMs / 10)));
                gce.setAttribute("transparentColorIndex", "0");
                if (i == 0) {
                    IIOMetadataNode apps = new IIOMetadataNode("ApplicationExtensions");
                    IIOMetadataNode app = new IIOMetadataNode("ApplicationExtension");
                    app.setAttribute("applicationID", "NETSCAPE");
                    app.setAttribute("authenticationCode", "2.0");
                    app.setUserObject(new byte[]{1, (byte) (loopCount & 0xFF), (byte) ((loopCount >> 8) & 0xFF)});
                    apps.appendChild(app);
                    root.appendChild(apps);
                }
                meta.setFromTree(IMAGE_FORMAT, root);
                writer.writeToSequence(new IIOImage(img, null, meta), param);
            }
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
