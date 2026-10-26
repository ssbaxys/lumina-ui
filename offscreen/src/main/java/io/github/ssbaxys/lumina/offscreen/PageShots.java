package io.github.ssbaxys.lumina.offscreen;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.widget.Button;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Снимки всех страниц витрины: {@code PageShots <assets> <out> [scale]}. */
public final class PageShots {
    public static void main(String[] args) throws Exception {
        Assets assets = new Assets(Path.of(args[0]));
        OffscreenPlatform platform = new OffscreenPlatform(assets);
        Lumina.init(platform);
        int scale = args.length > 2 ? Integer.parseInt(args[2]) : 2;
        Class<?> showcase = Class.forName("io.github.ssbaxys.luminatest.Showcase");
        Method create = showcase.getMethod("create", Runnable.class);
        UiRoot root = (UiRoot) create.invoke(null, (Runnable) () -> { });
        root.resize(854, 480);
        SoftwareCanvas canvas = platform.canvas();
        Scenes.settle(root, platform, 0.2f);
        List<Button> nav = new ArrayList<>();
        collect(root.content(), nav);
        int only = args.length > 3 ? Integer.parseInt(args[3]) : -1;
        for (int i = 0; i < nav.size(); i++) {
            if (only >= 0 && i != only) {
                continue;
            }
            Button b = nav.get(i);
            for (UiElement p = b.parent(); p != null; p = p.parent()) {
                if (p instanceof io.github.ssbaxys.lumina.widget.ScrollPanel sp) {
                    sp.scrollIntoView(b, false);
                    break;
                }
            }
            Scenes.settle(root, platform, 0.1f);
            Scenes.click(root, b);
            Scenes.settle(root, platform, 1.2f);
            canvas.begin(854, 480, scale, 2);
            root.render(canvas, 0f);
            canvas.end();
            canvas.save(Path.of(args[1], String.format("%02d.png", i)));
            System.out.println(i + " " + b.text());
        }
    }

    private static void collect(UiElement e, List<Button> out) {
        if (e instanceof Button b && "nav".equals(b.styleClass())) {
            out.add(b);
        }
        for (UiElement c : e.children()) {
            collect(c, out);
        }
    }
}
