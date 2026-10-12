package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.StackLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Страницы с анимированным переходом: показывает одну страницу по номеру из сигнала. Удобно
 * в паре с {@link Tabs} или {@link SideNav}: {@code new Pages(tab).page(this::general).page(this::video)}.
 * Страница создаётся при первом показе; с {@link #keepAlive} она потом не пересоздаётся
 * и сохраняет прокрутку и состояние.
 */
public class Pages extends Widget<Pages> {
    public enum Effect {
        /** Мгновенно. */
        NONE,
        /** Растворение. */
        FADE,
        /** Сдвиг вбок по направлению смены номера. */
        SLIDE,
        /** Сдвиг снизу вверх. */
        SLIDE_UP,
        /** Новая страница вырастает, старая уменьшается. */
        ZOOM,
        /** Переворот карточки (сжатие по горизонтали). */
        FLIP
    }

    private final Signal<Integer> index;
    private final List<Supplier<UiElement>> factories = new ArrayList<>();
    private final List<UiElement> cache = new ArrayList<>();
    private Effect effect = Effect.SLIDE;
    private float duration = 0.28f;
    private boolean keepAlive = true;
    private UiElement current;
    private int shown = -1;
    private Subscription subscription;

    public Pages(Signal<Integer> index) {
        super(null);
        this.index = index;
        setLayout(new StackLayout(Align.START, Align.START));
        setClipChildren(true);
    }

    public Signal<Integer> index() {
        return index;
    }

    /** Страница, которая создаётся при первом показе. */
    public Pages page(Supplier<UiElement> factory) {
        factories.add(factory);
        cache.add(null);
        if (root() != null && factories.size() - 1 == currentIndex()) {
            show(currentIndex(), false);
        }
        return this;
    }

    /** Готовые страницы. */
    public Pages add(UiElement... pages) {
        for (UiElement p : pages) {
            page(() -> p);
        }
        return this;
    }

    public Pages effect(Effect effect) {
        this.effect = effect;
        return this;
    }

    public Pages duration(float seconds) {
        this.duration = seconds;
        return this;
    }

    /** Хранить скрытые страницы (по умолчанию да) или создавать заново при каждом показе. */
    public Pages keepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    /** Показанная сейчас страница. */
    public UiElement current() {
        return current;
    }

    public int pageCount() {
        return factories.size();
    }

    private int currentIndex() {
        Integer i = index.peek();
        return i == null ? 0 : i;
    }

    @Override
    protected void onAttach() {
        subscription = index.subscribe(i -> show(i == null ? 0 : i, shown >= 0));
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    private UiElement pageAt(int i) {
        UiElement page = cache.get(i);
        if (page == null) {
            page = factories.get(i).get();
            page.layoutParams().width = Dim.FILL;
            page.layoutParams().height = Dim.FILL;
            if (keepAlive) {
                cache.set(i, page);
            }
        }
        return page;
    }

    private void show(int i, boolean animate) {
        if (i < 0 || i >= factories.size() || (i == shown && current != null)) {
            return;
        }
        int dir = shown < 0 || i >= shown ? 1 : -1;
        UiElement old = current;
        UiElement next = pageAt(i);
        shown = i;
        current = next;
        next.stopAnimations();
        clearEffects(next);
        next.setVisible(true);
        if (next.parent() != this) {
            addChild(next);
        } else {
            next.bringToFront();
        }
        if (old != null && old != next) {
            old.stopAnimations();
            if (animate && effect != Effect.NONE) {
                animateOut(old, dir);
            } else {
                retire(old);
            }
        }
        if (animate && effect != Effect.NONE) {
            animateIn(next, dir);
        }
    }

    private void retire(UiElement page) {
        clearEffects(page);
        if (page == current) {
            return;
        }
        if (keepAlive) {
            page.setVisible(false);
        } else {
            removeChild(page);
        }
    }

    private static void clearEffects(UiElement page) {
        page.clearAnimatedStyle(Styles.OPACITY);
        page.clearAnimatedStyle(Styles.TRANSLATE_X);
        page.clearAnimatedStyle(Styles.TRANSLATE_Y);
        page.clearAnimatedStyle(Styles.SCALE);
        page.clearAnimatedStyle(Styles.SCALE_X);
    }

    private void animateIn(UiElement page, int dir) {
        float w = Math.max(40f, width());
        float h = Math.max(40f, height());
        switch (effect) {
            case FADE -> page.animate(Styles.OPACITY).from(0f).to(1f).duration(duration).ease(Easing.OUT_CUBIC).keep(false).play();
            case SLIDE -> {
                page.animate(Styles.TRANSLATE_X).from(dir * w * 0.35f).to(0f).duration(duration).ease(Easing.EMPHASIZED).keep(false).play();
                page.animate(Styles.OPACITY).from(0f).to(1f).duration(duration * 0.8f).ease(Easing.OUT_CUBIC).keep(false).play();
            }
            case SLIDE_UP -> {
                page.animate(Styles.TRANSLATE_Y).from(h * 0.2f).to(0f).duration(duration).ease(Easing.EMPHASIZED).keep(false).play();
                page.animate(Styles.OPACITY).from(0f).to(1f).duration(duration * 0.8f).ease(Easing.OUT_CUBIC).keep(false).play();
            }
            case ZOOM -> {
                page.animate(Styles.SCALE).from(0.92f).to(1f).duration(duration).ease(Easing.OUT_BACK).keep(false).play();
                page.animate(Styles.OPACITY).from(0f).to(1f).duration(duration * 0.7f).ease(Easing.OUT_CUBIC).keep(false).play();
            }
            case FLIP -> {
                page.setAnimatedStyle(Styles.SCALE_X, 0f);
                page.animate(Styles.SCALE_X).from(0f).to(1f).duration(duration / 2f).delay(duration / 2f)
                        .ease(Easing.OUT_CUBIC).keep(false).play();
            }
            case NONE -> {
            }
        }
    }

    private void animateOut(UiElement page, int dir) {
        float w = Math.max(40f, width());
        Runnable done = () -> retire(page);
        switch (effect) {
            case SLIDE -> {
                page.animate(Styles.TRANSLATE_X).to(-dir * w * 0.35f).duration(duration).ease(Easing.EMPHASIZED).play();
                page.animate(Styles.OPACITY).to(0f).duration(duration * 0.6f).ease(Easing.OUT_CUBIC).onFinish(done).play();
            }
            case SLIDE_UP -> page.animate(Styles.OPACITY).to(0f).duration(duration * 0.5f).onFinish(done).play();
            case ZOOM -> {
                page.animate(Styles.SCALE).to(1.06f).duration(duration).ease(Easing.OUT_CUBIC).play();
                page.animate(Styles.OPACITY).to(0f).duration(duration * 0.6f).onFinish(done).play();
            }
            case FLIP -> page.animate(Styles.SCALE_X).to(0f).duration(duration / 2f).ease(Easing.IN_CUBIC).onFinish(done).play();
            default -> page.animate(Styles.OPACITY).to(0f).duration(duration).ease(Easing.OUT_CUBIC).onFinish(done).play();
        }
    }
}
