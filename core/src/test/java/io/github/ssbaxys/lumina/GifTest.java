package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.render.AnimatedTexture;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Gif;
import io.github.ssbaxys.lumina.render.Images;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.widget.AnimatedImage;
import io.github.ssbaxys.lumina.widget.Image;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Анимированный GIF: чтение кадров и задержек, текстуры, проигрывание в Image и AnimatedImage. */
class GifTest {
    static final List<String> uploads = new ArrayList<>();
    static CapturePlatform platform;

    @BeforeEach
    void init() {
        uploads.clear();
        platform = new CapturePlatform() {
            @Override
            public TextureRef createTexture(String key, int width, int height, int[] argb) {
                uploads.add(key + " " + width + "x" + height);
                return TextureRef.of("luminaui:dynamic/" + key);
            }
        };
        Lumina.init(platform);
        Images.clearCache();
    }

    static List<Images.Pixels> frames(int n, int w, int h) {
        int[] colors = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFF00};
        List<Images.Pixels> out = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int[] px = new int[w * h];
            java.util.Arrays.fill(px, colors[i % colors.length]);
            px[0] = 0xFF000000;
            out.add(new Images.Pixels(w, h, px));
        }
        return out;
    }

    @Test
    void encodeAndDecodeRoundTrip() throws IOException {
        byte[] gif = Gif.encode(frames(3, 8, 6), 120, 0);
        assertTrue(Gif.isGif(gif));
        Gif.Animation anim = Gif.decode(gif);
        assertEquals(8, anim.width());
        assertEquals(6, anim.height());
        assertEquals(3, anim.frameCount());
        assertEquals(0, anim.loopCount(), "NETSCAPE loop forever");
        assertEquals(360, anim.durationMs());
        assertEquals(120, anim.frames().get(1).delayMs());
        assertEquals(0xFF00FF00, anim.frames().get(1).pixels().pixel(3, 3) | 0xFF000000);
        int red = anim.frames().get(0).pixels().pixel(4, 4);
        assertTrue(Color.red(red) > 240 && Color.green(red) < 16, Integer.toHexString(red));
    }

    @Test
    void zeroDelayBecomesDefaultAndBrokenDataFails() throws IOException {
        Gif.Animation anim = Gif.decode(Gif.encode(frames(2, 4, 4), 0, 3));
        assertEquals(Gif.DEFAULT_DELAY_MS, anim.frames().get(0).delayMs());
        assertEquals(3, anim.loopCount());
        assertThrows(IOException.class, () -> Gif.decode(new byte[]{'G', 'I', 'F', '8', '9', 'a', 1, 2}));
        assertFalse(Gif.isGif(new byte[]{1, 2, 3}));
    }

    @Test
    void animatedTextureTiming() {
        TextureRef[] frames = {TextureRef.of("a"), TextureRef.of("b"), TextureRef.of("c")};
        AnimatedTexture t = new AnimatedTexture(4, 4, frames, new int[]{100, 50, 200}, 0);
        assertEquals("a", t.id());
        assertEquals(350, t.duration());
        assertEquals(0, t.frameAt(0));
        assertEquals(0, t.frameAt(99));
        assertEquals(1, t.frameAt(100));
        assertEquals(2, t.frameAt(150));
        assertEquals(0, t.frameAt(350), "loops forever");
        assertEquals(2, t.frameAt(350, false), "once: stays on the last frame");
        AnimatedTexture twice = new AnimatedTexture(4, 4, frames, new int[]{100, 50, 200}, 2);
        assertEquals(0, twice.frameAt(360));
        assertEquals(2, twice.frameAt(800));
    }

    @Test
    void imagesLoadAnimatedGifAsFrames() throws IOException {
        byte[] gif = Gif.encode(frames(4, 5, 5), 100, 0);
        TextureRef ref = Images.fromBytes("spin", gif);
        AnimatedTexture anim = assertInstanceOf(AnimatedTexture.class, ref);
        assertEquals(4, anim.frameCount());
        assertEquals(List.of("spin/0 5x5", "spin/1 5x5", "spin/2 5x5", "spin/3 5x5"), uploads);
        assertSame(ref, Images.fromBytes("spin", gif), "cached");
        assertEquals(1f, anim.aspect(), 1e-6);
    }

    @Test
    void imageWidgetPlaysGifWithFrameDelays() throws IOException {
        AnimatedTexture anim = (AnimatedTexture) Images.fromBytes("play", Gif.encode(frames(3, 4, 4), 100, 0));
        Image image = new Image(anim).size(32, 32);
        UiRoot root = new UiRoot(Panel.box(new StackLayout(Align.START, Align.START)).add(image), Theme.dark());
        root.resize(100, 100);
        root.tick(0f);
        assertEquals(anim.frame(0), image.currentTexture());
        root.tick(0.05f);
        root.tick(0.06f);
        assertEquals(anim.frame(1), image.currentTexture());
        root.tick(0.1f);
        assertEquals(anim.frame(2), image.currentTexture());
        root.tick(0.1f);
        assertEquals(anim.frame(0), image.currentTexture(), "loops");
        TestSupport.RecordingCanvas c = new TestSupport.RecordingCanvas();
        c.begin(100, 100);
        root.render(c, 0f);
        c.end();
        assertTrue(c.ops.stream().anyMatch(o -> o.startsWith("tex luminaui:dynamic/play/")), c.ops.toString());
    }

    @Test
    void animatedImageControls() throws IOException {
        AnimatedTexture anim = (AnimatedTexture) Images.fromBytes("ctl", Gif.encode(frames(3, 4, 4), 100, 0));
        List<Integer> seen = new ArrayList<>();
        AnimatedImage img = AnimatedImage.of(anim).size(16, 16).onFrame(seen::add);
        UiRoot root = new UiRoot(Panel.box(new StackLayout(Align.START, Align.START)).add(img), Theme.dark());
        root.resize(100, 100);
        root.tick(0.15f);
        assertEquals(1, img.frame());
        img.pause();
        root.tick(0.5f);
        assertEquals(1, img.frame(), "paused");
        img.seek(2).play();
        assertEquals(2, img.frame());
        img.speed(2f);
        root.tick(0.05f);
        assertEquals(0, img.frame(), "2x speed, looped");
        img.loop(AnimatedImage.Loop.ONCE).stop().play();
        root.tick(1f);
        assertEquals(2, img.frame(), "once: last frame");
        assertTrue(seen.contains(1) && seen.contains(2));
        assertNotNull(img.animation());
    }
}
