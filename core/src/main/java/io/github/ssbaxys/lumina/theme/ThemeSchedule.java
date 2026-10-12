package io.github.ssbaxys.lumina.theme;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.platform.WorldInfo;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.ThemeBlend;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

/**
 * Автоматическая смена темы по значению из игры: времени суток, биому или чему угодно ещё.
 * Расписание периодически спрашивает источник и плавно переводит корень в выбранную тему
 * ({@link ThemeTransition}: цвета перетекают, а не переключаются).
 *
 * <pre>{@code
 * ThemeSchedule.byTime(WorldInfo::timeOfDay)
 *         .dayNight(Theme.light(), Theme.dark())          // закат и рассвет — плавное смешивание
 *         .build()
 *         .attach(root);
 *
 * ThemeSchedule.byBiome(WorldInfo::biome)
 *         .when("minecraft:desert", desertTheme)
 *         .when(ThemeSchedule.contains("snow"), snowTheme)
 *         .otherwise(Theme.dark())
 *         .build().transition(2f).attach(root);
 * }</pre>
 *
 * Источники по умолчанию — {@link WorldInfo} (берёт значения у платформы, без мира — системные часы).
 * Для витрины и тестов подойдёт любой {@code Supplier}, например значение ползунка.
 */
public final class ThemeSchedule {
    private final Supplier<Theme> selector;
    private float transitionSeconds = 1.2f;
    private float pollSeconds = 0.25f;
    private UnaryOperator<Theme> decorator;
    private int version;

    private ThemeSchedule(Supplier<Theme> selector) {
        this.selector = selector;
    }

    /** Расписание из своей функции выбора темы. */
    public static ThemeSchedule of(Supplier<Theme> selector) {
        return new ThemeSchedule(Objects.requireNonNull(selector));
    }

    /** По времени суток в тиках (0..24000). */
    public static TimeOfDay byTime(DoubleSupplier ticks) {
        return new TimeOfDay(ticks);
    }

    /** По времени суток мира (или системных часов без мира). */
    public static TimeOfDay byWorldTime() {
        return new TimeOfDay(WorldInfo::timeOfDay);
    }

    /** По биому: {@code "minecraft:desert"}. */
    public static ByValue<String> byBiome(Supplier<String> biome) {
        return new ByValue<>(biome);
    }

    /** По биому под игроком. */
    public static ByValue<String> byWorldBiome() {
        return new ByValue<>(WorldInfo::biome);
    }

    /** По любому значению. */
    public static <T> ByValue<T> byValue(Supplier<T> provider) {
        return new ByValue<>(provider);
    }

    /** Условие «строка содержит фрагмент» для биомов: {@code contains("snow")}. */
    public static Predicate<String> contains(String fragment) {
        return s -> s != null && s.contains(fragment);
    }

    /** Длительность перехода между темами, секунды (по умолчанию 1.2). */
    public ThemeSchedule transition(float seconds) {
        this.transitionSeconds = Math.max(0f, seconds);
        return this;
    }

    public float transitionSeconds() {
        return transitionSeconds;
    }

    /** Как часто спрашивать источник, секунды (по умолчанию 0.25). */
    public ThemeSchedule poll(float seconds) {
        this.pollSeconds = Math.max(0f, seconds);
        return this;
    }

    /**
     * Доработка выбранной темы перед установкой (на копии): звуки, высокий контраст, свои классы.
     * Смена доработки применяется при следующем опросе.
     */
    public ThemeSchedule decorate(UnaryOperator<Theme> decorator) {
        this.decorator = decorator;
        version++;
        return this;
    }

    /** Тема, которую расписание выбрало бы сейчас (без доработки). */
    public Theme current() {
        return selector.get();
    }

    /** Подключить к корню: тема будет меняться, пока не закрыта подписка. */
    public Subscription attach(UiRoot root) {
        Attachment a = new Attachment(root);
        root.animator().start(a);
        return a::close;
    }

    /** Подключение к корню: опрос источника и переходы. */
    private final class Attachment implements Animation {
        private final UiRoot root;
        private float sincePoll = Float.POSITIVE_INFINITY;
        private ThemeTransition transition;
        private Theme lastBase;
        private Object[] lastTokens;
        private int seenVersion = -1;
        private boolean closed;

        Attachment(UiRoot root) {
            this.root = root;
        }

        void close() {
            closed = true;
        }

        @Override
        public boolean tick(float dt) {
            if (closed) {
                return false;
            }
            sincePoll += dt;
            if (sincePoll >= pollSeconds) {
                sincePoll = 0f;
                update();
            }
            return true;
        }

        private void update() {
            Theme base = selector.get();
            if (base == null) {
                return;
            }
            Theme target = decorator == null ? base : decorator.apply(base.copy());
            Object[] tokens = ThemeBlend.snapshot(target);
            boolean ours = transition != null && root.theme() == transition.theme();
            if (ours && seenVersion == version && lastBase != null && lastBase.hasSameStyles(base)) {
                if (ThemeBlend.differs(tokens, lastTokens)) {
                    transition.retarget(tokens);
                    lastTokens = tokens;
                }
                return;
            }
            transition = ThemeTransition.to(root, target, transitionSeconds);
            lastBase = base;
            lastTokens = tokens;
            seenVersion = version;
        }
    }

    // ================================================================ по времени суток

    /** Ключевые темы по времени суток; между соседними — плавное смешивание перед сменой. */
    public static final class TimeOfDay {
        private final DoubleSupplier ticks;
        private final TreeMap<Float, Theme> keys = new TreeMap<>();
        private float blend = 1000f;

        private TimeOfDay(DoubleSupplier ticks) {
            this.ticks = ticks;
        }

        /** С тика {@code tick} (0 — восход, 6000 — полдень, 12000 — закат, 18000 — полночь) — тема {@code theme}. */
        public TimeOfDay at(float tick, Theme theme) {
            keys.put(norm(tick), Objects.requireNonNull(theme));
            return this;
        }

        /** День с восхода (0) и ночь с 12500 (после заката). */
        public TimeOfDay dayNight(Theme day, Theme night) {
            return at(0f, day).at(12500f, night);
        }

        /** Сколько тиков до смены тема смешивается со следующей (по умолчанию 1000 ≈ 50 секунд игры). */
        public TimeOfDay blend(float ticks) {
            this.blend = Math.max(0f, ticks);
            return this;
        }

        /** Тема для момента {@code tick}: ключевая или смесь двух соседних. */
        public Theme themeAt(float tick) {
            if (keys.isEmpty()) {
                return null;
            }
            float t = norm(tick);
            Map.Entry<Float, Theme> cur = keys.floorEntry(t);
            if (cur == null) {
                cur = keys.lastEntry();
            }
            Map.Entry<Float, Theme> next = keys.higherEntry(t);
            if (next == null) {
                next = keys.firstEntry();
            }
            if (next.getKey().equals(cur.getKey()) || blend <= 0f) {
                return cur.getValue();
            }
            float dist = norm(next.getKey() - t);
            if (dist >= blend) {
                return cur.getValue();
            }
            float f = 1f - dist / blend;
            f = f * f * (3f - 2f * f);
            return ThemeBlend.mix(cur.getValue(), next.getValue(), f);
        }

        public ThemeSchedule build() {
            return ThemeSchedule.of(() -> themeAt((float) ticks.getAsDouble()));
        }

        private static float norm(float tick) {
            return ((tick % WorldInfo.DAY_TICKS) + WorldInfo.DAY_TICKS) % WorldInfo.DAY_TICKS;
        }
    }

    // ================================================================ по значению (биом)

    /** Правила «значение → тема», проверяются по порядку; ни одно не подошло — {@link #otherwise}. */
    public static final class ByValue<T> {
        private final Supplier<T> provider;
        private final List<Predicate<? super T>> tests = new ArrayList<>();
        private final List<Theme> themes = new ArrayList<>();
        private Theme otherwise;

        private ByValue(Supplier<T> provider) {
            this.provider = provider;
        }

        public ByValue<T> when(T value, Theme theme) {
            return when(v -> Objects.equals(v, value), theme);
        }

        public ByValue<T> when(Predicate<? super T> test, Theme theme) {
            tests.add(test);
            themes.add(Objects.requireNonNull(theme));
            return this;
        }

        public ByValue<T> otherwise(Theme theme) {
            this.otherwise = theme;
            return this;
        }

        /** Тема для значения ({@code otherwise}, если ни одно правило не подошло; может быть {@code null}). */
        public Theme themeFor(T value) {
            for (int i = 0; i < tests.size(); i++) {
                if (tests.get(i).test(value)) {
                    return themes.get(i);
                }
            }
            return otherwise;
        }

        public ThemeSchedule build() {
            return ThemeSchedule.of(() -> themeFor(provider.get()));
        }
    }
}
