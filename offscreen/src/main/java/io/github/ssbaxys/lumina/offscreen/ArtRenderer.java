package io.github.ssbaxys.lumina.offscreen;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.luminatest.ModrinthArt;

import java.nio.file.Path;
import java.util.List;

/**
 * Рисует картинки для страницы мода ({@link ModrinthArt}) в PNG:
 * {@code ArtRenderer <папка с assets/> <куда> [en|ru|all] [имя]}.
 */
public final class ArtRenderer {
    public static void main(String[] args) throws Exception {
        Assets assets = new Assets(Path.of(args[0]));
        OffscreenPlatform platform = new OffscreenPlatform(assets);
        Lumina.init(platform);
        Path out = Path.of(args[1]);
        String langs = args.length > 2 ? args[2] : "all";
        String only = args.length > 3 ? args[3] : null;
        for (String lang : langs.equals("all") ? new String[]{"en", "ru"} : new String[]{langs}) {
            List<ModrinthArt.Art> arts = ModrinthArt.all(lang.equals("ru"));
            for (ModrinthArt.Art art : arts) {
                if (only != null && !art.name().startsWith(only)) {
                    continue;
                }
                UiRoot root = art.scene().get();
                root.resize(art.width(), art.height());
                Scenes.settle(root, platform, 0.3f);
                if (art.prepare() != null) {
                    art.prepare().accept(root);
                }
                Scenes.settle(root, platform, art.settle());
                // уводим курсор, чтобы ничего не было «под мышью»
                root.pointerMove(-100, -100);
                Scenes.settle(root, platform, 0.4f);
                SoftwareCanvas canvas = platform.canvas();
                canvas.begin(art.width(), art.height(), art.scale(), 2);
                root.render(canvas, 0f);
                canvas.end();
                Path file = out.resolve(lang).resolve(art.name() + ".png");
                canvas.save(file);
                System.out.println(file);
                root.dispose();
            }
        }
    }
}
