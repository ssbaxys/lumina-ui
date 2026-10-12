package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.game.GameData;
import io.github.ssbaxys.lumina.game.GameState;
import io.github.ssbaxys.lumina.game.Inventory;
import io.github.ssbaxys.lumina.hud.BossBar;
import io.github.ssbaxys.lumina.hud.Countdown;
import io.github.ssbaxys.lumina.hud.Hud;
import io.github.ssbaxys.lumina.hud.HudStyles;
import io.github.ssbaxys.lumina.hud.NotificationFeed;
import io.github.ssbaxys.lumina.hud.RadialGauge;
import io.github.ssbaxys.lumina.hud.SegmentedBar;
import io.github.ssbaxys.lumina.hud.TimerDisplay;
import io.github.ssbaxys.lumina.hud.Toast;
import io.github.ssbaxys.lumina.hud.ToastStack;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Button;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.Toggle;
import io.github.ssbaxys.lumina.widget.Widget;

/**
 * Страница «HUD и данные игры»: живые значения {@code GameData} в виджетах HUD, тосты, таймер, лента
 * уведомлений и переключатель демонстрационного HUD поверх игры ({@link HudDemo}).
 */
public final class HudPage {
    private static final String[] LOOT = {
            "minecraft:diamond", "minecraft:emerald", "minecraft:gold_ingot", "minecraft:redstone", "minecraft:lapis_lazuli"};
    private static final String[] LOOT_NAMES = {"Алмаз", "Изумруд", "Золотой слиток", "Редстоун", "Лазурит"};

    private HudPage() {
    }

    public static UiElement create() {
        ToastStack toasts = new ToastStack().maxVisible(3).id("preview-toasts");
        NotificationFeed feed = new NotificationFeed().capacity(5).lifetime(8f).lineWidth(220f).id("preview-feed");
        return Ui.page(
                Ui.section("Данные игры", "GameData опрашивает игру на каждом тике; в главном меню — значения по умолчанию",
                        gameData()),
                Ui.section("Тосты", "Въезжают сбоку, живут несколько секунд и схлопываются; лишние ждут в очереди",
                        toastSection(toasts)),
                Ui.section("Таймер", "Обратный отсчёт по реальным часам: идёт, даже если его не видно",
                        timerSection(toasts)),
                Ui.section("Лента уведомлений", "Короткие строки, которые тают через несколько секунд",
                        feedSection(feed)),
                Ui.section("HUD поверх игры", "Hud.get() рисуется загрузчиком каждый кадр; закройте витрину, чтобы увидеть",
                        demoSection()));
    }

    // ================================================================ данные игры

    private static UiElement gameData() {
        SegmentedBar hearts = new SegmentedBar().color(0xFFE5484D).bind(GameData.health()).bindMax(GameData.maxHealth()).id("hearts");
        SegmentedBar food = new SegmentedBar().color(0xFFD9A441).max(20).lowBelow(0.3f).bind(GameData.food());
        SegmentedBar armor = new SegmentedBar().color(0xFFB8C4D6).max(20).lowBelow(0f).bind(GameData.armor());
        Panel bars = Ui.column(5,
                Ui.row(8, Ui.muted("Здоровье").width(Dim.px(64)), hearts,
                        HudDemo.live(GameData.health(), h -> number(h) + " / " + number(GameData.maxHealth().get())).id("health-text")),
                Ui.row(8, Ui.muted("Сытость").width(Dim.px(64)), food,
                        HudDemo.live(GameData.saturation(), s -> "насыщение " + number(s))),
                Ui.row(8, Ui.muted("Броня").width(Dim.px(64)), armor));

        Panel gauges = Ui.row(14,
                Ui.labeled(new RadialGauge("воздух").max(300).percent(true).lowBelow(0.3f)
                        .colors(0xFF4DC3FF, 0xFFB4ECFF).bind(GameData.air()).size(44, 44), "Воздух"),
                Ui.labeled(new RadialGauge("кадр/с").max(144).lowBelow(0.2f).bind(GameData.fps()).size(44, 44), "FPS"),
                Ui.labeled(new RadialGauge("опыт").max(1).percent(true).sweep(360)
                        .colors(0xFF7ED957, 0xFFC6F35E).bind(GameData.xpProgress()).size(44, 44), "Опыт"));

        BossBar xp = new BossBar("Полоса опыта").colors(0xFF7ED957, 0xFFC6F35E).notches(10).bind(GameData.xpProgress());

        Panel facts = Ui.grid(2, 6,
                fact("В мире", HudDemo.live(GameData.inWorld(), w -> w ? "да" : "нет (главное меню)").id("in-world")),
                fact("Координаты", HudDemo.live(GameData.position(), p -> p).id("position")),
                fact("Взгляд", HudDemo.live(GameData.facing(), HudDemo::facing)),
                fact("Биом", HudDemo.live(GameData.biome(), HudDemo::shortId).id("biome")),
                fact("Измерение", HudDemo.live(GameData.dimension(), HudDemo::shortId)),
                fact("Время суток", HudDemo.live(GameData.clock(), t -> t).id("clock")),
                fact("Уровень", HudDemo.live(GameData.xpLevel(), l -> Integer.toString(l))),
                fact("В руке", HudDemo.live(GameData.selectedItem(), i -> i.isEmpty() ? "пусто" : HudDemo.shortId(i)).id("held")));

        Panel hotbar = Ui.column(4, Ui.muted("Панель быстрого доступа (снимок инвентаря)"), new HotbarView().id("hotbar"));
        return Ui.column(10, bars, gauges, xp, facts, hotbar);
    }

    private static Panel fact(String name, Label value) {
        return Ui.row(6, Ui.muted(name + ":").width(Dim.px(80)), value).width(Dim.px(230));
    }

    private static String number(float v) {
        return v == Math.rint(v) ? Integer.toString(Math.round(v)) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }

    // ================================================================ тосты

    private static UiElement toastSection(ToastStack toasts) {
        int[] loot = {0};
        Button info = new Button("Обычный", () -> toasts.show("Подсказка", "Нажмите K, чтобы открыть витрину")).id("toast-info");
        Button success = new Button("С иконкой", () -> {
            int i = loot[0]++ % LOOT.length;
            toasts.show(Toast.success("Добыча", LOOT_NAMES[i] + " отправлен в инвентарь").icon(LOOT[i]));
        }).id("toast-success");
        Button error = new Button("Ошибка", () -> toasts.show(Toast.error("Нет связи", "Сервер не ответил за 30 секунд. Повторная попытка…")))
                .id("toast-error");
        Button progress = new Button("С прогрессом").id("toast-progress");
        progress.onPress(() -> startLoading(progress, toasts));
        Button global = new Button("В игровой HUD", () -> Hud.get().toasts().show(
                Toast.info("Из витрины", "Этот тост в общем HUD — он виден поверх игры").icon("minecraft:paper")))
                .id("toast-global");
        Button clear = new Button("Убрать все", toasts::dismissAll).styleClass("button ghost");

        Panel area = Panel.box(new StackLayout(Align.END, Align.START)).width(Dim.FILL).height(Dim.px(170));
        area.style(b -> b.set(Styles.BACKGROUND, io.github.ssbaxys.lumina.render.Brush.solid(0x18808080))
                .set(Styles.RADIUS, io.github.ssbaxys.lumina.render.Radii.all(6))
                .set(Styles.PADDING, io.github.ssbaxys.lumina.geom.Insets.all(6)));
        area.setClipChildren(true);
        area.add(toasts);
        return Ui.column(8, Ui.row(6, info, success, error, progress), Ui.row(6, global, clear), area);
    }

    /** Тост с полосой: «загрузка» заполняется за три секунды и закрывается сама. */
    private static void startLoading(UiElement owner, ToastStack toasts) {
        if (owner.root() == null) {
            return;
        }
        Signal<Float> p = Signal.of(0f);
        Toast t = toasts.show(Toast.info("Загрузка мира", "Генерация чанков…").icon("minecraft:grass_block")
                .timeout(0).bindProgress(p));
        owner.root().animator().start(new Animation() {
            private float time;

            @Override
            public boolean tick(float dt) {
                time += dt;
                p.set(Math.min(1f, time / 3f));
                if (time >= 3.3f) {
                    t.body("Готово!");
                    t.dismiss();
                    return false;
                }
                return true;
            }
        });
    }

    // ================================================================ таймер

    private static UiElement timerSection(ToastStack toasts) {
        Countdown timer = HudDemo.TIMER;
        TimerDisplay display = new TimerDisplay(timer).label("Таймер").scale(1.5f).tenthsBelow(5f).id("timer");
        Runnable finished = () -> {
            toasts.show(Toast.warning("Время вышло", "Таймер витрины закончился").icon("minecraft:clock"));
            if (HudDemo.isActive()) {
                Hud.get().feed().post("minecraft:clock", "Таймер закончился", Toast.WARNING);
            }
        };
        Button ten = new Button("10 секунд", () -> {
            display.countdown(timer);
            timer.onFinish(finished).restart(10f);
        }).id("timer-10");
        Button minute = new Button("1 минута", () -> {
            display.countdown(timer);
            timer.onFinish(finished).restart(60f);
        }).id("timer-60");
        Button pause = new Button("Пауза / дальше", () -> display.countdown().toggle()).id("timer-pause");
        Button reset = new Button("Сброс", () -> display.countdown().reset()).styleClass("button ghost");
        Button stopwatch = new Button("Секундомер", () -> display.countdown(Countdown.stopwatch().start())).id("timer-stopwatch");
        return Ui.column(8, display, Ui.row(6, ten, minute, pause, reset, stopwatch));
    }

    // ================================================================ лента

    private static UiElement feedSection(NotificationFeed feed) {
        int[] n = {0};
        Button event = new Button("Событие", () -> feed.post("Игрок Steve" + (++n[0]) + " вошёл в игру")).id("feed-event");
        Button loot = new Button("Добыча", () -> {
            int i = n[0]++ % LOOT.length;
            feed.post(LOOT[i], "+1 " + LOOT_NAMES[i], Toast.SUCCESS);
        }).id("feed-loot");
        Button danger = new Button("Опасность", () -> feed.post("Рядом крипер!", Toast.ERROR)).id("feed-danger");
        Panel area = Panel.box(new StackLayout(Align.START, Align.END)).width(Dim.FILL).height(Dim.px(100));
        area.add(feed);
        return Ui.column(8, Ui.row(6, event, loot, danger), area);
    }

    // ================================================================ демо-HUD

    private static UiElement demoSection() {
        Signal<Boolean> on = Signal.of(HudDemo.isActive());
        on.subscribe(HudDemo::setActive);
        Toggle toggle = new Toggle(on).id("demo-toggle");
        return Ui.column(6,
                Ui.row(8, toggle, new Label("Показать демо-HUD поверх игры")),
                Ui.muted("Панель с координатами слева сверху, свои полосы над ванильными, опыт и таймер сверху,"),
                Ui.muted("шкала воздуха справа — только под водой. Прячется по F1 и на экране отладки."));
    }

    /**
     * Девять слотов панели быстрого доступа из снимка инвентаря: предмет, количество и рамка выбранного.
     * Строки количеств создаются только при изменении инвентаря.
     */
    static final class HotbarView extends Widget<HotbarView> {
        private static final float SLOT = 20f;
        private final String[] items = new String[GameState.HOTBAR];
        private final String[] counts = new String[GameState.HOTBAR];
        private int selected;
        private Subscription invSub;
        private Subscription selSub;

        HotbarView() {
            super(null);
            size(GameState.HOTBAR * (SLOT + 2f) - 2f, SLOT);
            setHitTestable(false);
        }

        private void apply(Inventory inv) {
            for (int i = 0; i < GameState.HOTBAR; i++) {
                String id = inv.itemId(i);
                items[i] = id.isEmpty() ? null : id;
                int c = inv.count(i);
                counts[i] = c > 1 ? Integer.toString(c) : null;
            }
        }

        /** Предмет в слоте (для тестов). */
        String item(int slot) {
            return items[slot];
        }

        @Override
        protected void onAttach() {
            apply(GameData.inventory().get());
            selected = GameData.selectedSlot().get();
            invSub = GameData.inventory().subscribe(this::apply);
            selSub = GameData.selectedSlot().subscribe(s -> selected = s);
        }

        @Override
        protected void onDetach() {
            invSub.close();
            selSub.close();
        }

        @Override
        protected void paintContent(Canvas canvas, float partialTick) {
            int text = style.get(Styles.TEXT_COLOR);
            int accent = style.get(Styles.ACCENT_COLOR);
            int track = style.get(HudStyles.TRACK_COLOR);
            TextMetrics m = canvas.textMetrics();
            for (int i = 0; i < GameState.HOTBAR; i++) {
                float x = i * (SLOT + 2f);
                canvas.fillRect(x, 0, SLOT, SLOT, Color.mulAlpha(track, 0.6f));
                if (i == selected) {
                    canvas.fillRect(x, 0, SLOT, 1f, accent);
                    canvas.fillRect(x, SLOT - 1f, SLOT, 1f, accent);
                    canvas.fillRect(x, 0, 1f, SLOT, accent);
                    canvas.fillRect(x + SLOT - 1f, 0, 1f, SLOT, accent);
                }
                if (items[i] != null) {
                    canvas.drawItem(items[i], x + 2f, 2f, 16f);
                }
                if (counts[i] != null) {
                    canvas.drawText(counts[i], x + SLOT - 1f - m.width(counts[i]), SLOT - m.lineHeight(), text, true);
                }
            }
        }
    }
}
