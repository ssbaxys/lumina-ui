package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.game.GameData;
import io.github.ssbaxys.lumina.hud.BossBar;
import io.github.ssbaxys.lumina.hud.Countdown;
import io.github.ssbaxys.lumina.hud.Hud;
import io.github.ssbaxys.lumina.hud.HudAnchor;
import io.github.ssbaxys.lumina.hud.HudVisibility;
import io.github.ssbaxys.lumina.hud.RadialGauge;
import io.github.ssbaxys.lumina.hud.SegmentedBar;
import io.github.ssbaxys.lumina.hud.TimerDisplay;
import io.github.ssbaxys.lumina.hud.Toast;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Демонстрационный HUD поверх игры: панель с координатами, биомом и временем, полосы здоровья и сытости
 * над ванильными, опыт как боссбар, таймер и шкала воздуха (только под водой). Включается со страницы
 * «HUD и данные игры» и живёт, пока его не выключат, — даже после закрытия витрины.
 */
final class HudDemo {
    /** Общий таймер витрины: его видно и на странице, и на HUD. */
    static final Countdown TIMER = Countdown.seconds(10);

    private static final List<UiElement> ELEMENTS = new ArrayList<>();

    private HudDemo() {
    }

    static boolean isActive() {
        return !ELEMENTS.isEmpty();
    }

    static void setActive(boolean on) {
        if (on == isActive()) {
            return;
        }
        Hud hud = Hud.get();
        if (!on) {
            for (UiElement e : ELEMENTS) {
                hud.remove(e);
            }
            ELEMENTS.clear();
            hud.feed().post(Tr.r("Демо-HUD выключен"));
            return;
        }

        // Левый верхний угол: где я и когда.
        Panel info = new Panel("hud-panel", FlexLayout.column().gap(2)).add(
                live(GameData.position(), p -> "XYZ: " + p),
                live(GameData.facing(), f -> Tr.r("Взгляд: ") + facing(f)),
                live(GameData.biome(), b -> Tr.r("Биом: ") + shortId(b)),
                live(GameData.clock(), t -> Tr.r("Время: ") + t),
                live(GameData.fps(), f -> "FPS: " + f));
        add(hud, info, HudAnchor.TOP_LEFT, 4, 4, HudVisibility.DEFAULT);

        // Над ванильными сердцами и едой — свои полосы.
        SegmentedBar hearts = new SegmentedBar().color(0xFFE5484D).bind(GameData.health()).bindMax(GameData.maxHealth());
        SegmentedBar food = new SegmentedBar().color(0xFFD9A441).max(20).lowBelow(0.3f).bind(GameData.food());
        Panel bars = Panel.box(FlexLayout.row().gap(10).align(Align.CENTER)).add(hearts, food);
        add(hud, bars, HudAnchor.BOTTOM_CENTER, 0, 52, HudVisibility.GAMEPLAY);

        // Опыт боссбаром сверху, под ним — таймер, если запущен.
        BossBar xp = new BossBar(Tr.r("Опыт")).colors(0xFF7ED957, 0xFFC6F35E).notches(10).bind(GameData.xpProgress());
        add(hud, xp, HudAnchor.TOP_CENTER, 0, 4, HudVisibility.DEFAULT);
        TimerDisplay timer = new TimerDisplay(TIMER).label(Tr.r("Таймер")).tenthsBelow(5f);
        add(hud, timer, HudAnchor.TOP_CENTER, 0, 24,
                HudVisibility.DEFAULT.when(() -> TIMER.isRunning() || TIMER.isFinished()));

        // Воздух — только когда он убывает.
        RadialGauge air = new RadialGauge(Tr.r("воздух")).max(300).percent(true).lowBelow(0.3f)
                .colors(0xFF4DC3FF, 0xFFB4ECFF).bind(GameData.air());
        add(hud, air, HudAnchor.CENTER_RIGHT, 8, 0,
                HudVisibility.GAMEPLAY.when(() -> GameData.air().get() < GameData.maxAir().get()));

        hud.toasts().show(Toast.success(Tr.r("Демо-HUD включён"), Tr.r("Закройте витрину, чтобы увидеть его поверх игры"))
                .icon("minecraft:spyglass"));
        hud.feed().post("minecraft:compass", Tr.r("Демо-HUD включён"), Toast.SUCCESS);
    }

    private static void add(Hud hud, UiElement e, HudAnchor anchor, float dx, float dy, HudVisibility visibility) {
        hud.add(e, anchor, dx, dy).visibility(visibility);
        ELEMENTS.add(e);
    }

    /** Подпись, которая подписывается на сигнал только пока она в дереве (без утечек в статических сигналах). */
    static <T> Label live(ReadableSignal<T> signal, Function<? super T, String> format) {
        return new LiveLabel<>(signal, format);
    }

    static String shortId(String id) {
        if (id == null || id.isEmpty()) {
            return "—";
        }
        int colon = id.indexOf(':');
        return colon >= 0 && id.startsWith("minecraft:") ? id.substring(colon + 1) : id;
    }

    static String facing(String f) {
        return switch (f) {
            case "north" -> Tr.r("север");
            case "south" -> Tr.r("юг");
            case "west" -> Tr.r("запад");
            case "east" -> Tr.r("восток");
            default -> f;
        };
    }

    private static final class LiveLabel<T> extends Label {
        private final ReadableSignal<T> signal;
        private final Function<? super T, String> format;
        private Subscription subscription;

        LiveLabel(ReadableSignal<T> signal, Function<? super T, String> format) {
            super(format.apply(signal.get()));
            this.signal = signal;
            this.format = format;
        }

        @Override
        protected void onAttach() {
            super.onAttach();
            text(format.apply(signal.get()));
            if (subscription == null) {
                subscription = signal.subscribe(v -> text(format.apply(v)));
            }
        }

        @Override
        protected void onDetach() {
            super.onDetach();
            if (subscription != null) {
                subscription.close();
                subscription = null;
            }
        }
    }
}
