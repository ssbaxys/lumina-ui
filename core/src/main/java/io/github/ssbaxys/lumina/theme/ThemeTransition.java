package io.github.ssbaxys.lumina.theme;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.ThemeBlend;

/**
 * Плавная смена темы корня: токены (цвета, скругление, скорость) перетекают из текущей темы в новую
 * за заданное время, а стили классов новой темы включаются сразу и догоняются своими переходами.
 *
 * <pre>{@code
 * ThemeTransition.to(root, Theme.light(), 0.8f);
 * }</pre>
 *
 * В корень ставится рабочая копия целевой темы ({@link #theme()}); её токены меняются покадрово.
 * При {@code accessibility().reducedMotion()} смена мгновенная.
 */
public final class ThemeTransition implements Animation {
    private final UiRoot root;
    private final Theme working;
    private final Object[] from;
    private Object[] to;
    private final float duration;
    private float elapsed;
    private boolean finished;
    private boolean cancelled;

    private ThemeTransition(UiRoot root, Theme working, Object[] from, Object[] to, float duration) {
        this.root = root;
        this.working = working;
        this.from = from;
        this.to = to;
        this.duration = duration;
    }

    /** Перейти к теме {@code target} за {@code seconds} секунд. Сама {@code target} не меняется. */
    public static ThemeTransition to(UiRoot root, Theme target, float seconds) {
        Object[] from = ThemeBlend.snapshot(root.theme());
        Object[] to = ThemeBlend.snapshot(target);
        Theme working = target.copy();
        boolean instant = seconds <= 0f || root.accessibility().reducedMotion();
        ThemeTransition t = new ThemeTransition(root, working, from, to, Math.max(seconds, 0f));
        if (instant) {
            t.finished = true;
        } else {
            ThemeBlend.apply(working, from, to, 0f);
        }
        root.setTheme(working);
        if (!instant) {
            root.animator().start(t);
        }
        return t;
    }

    /** Рабочая тема, стоящая в корне. */
    public Theme theme() {
        return working;
    }

    public boolean isFinished() {
        return finished;
    }

    /** Остановить на текущих значениях. */
    public void cancel() {
        cancelled = true;
    }

    /** Новая цель для токенов без смены стилей (используется расписанием): переход начинается заново. */
    void retarget(Object[] next) {
        Object[] now = ThemeBlend.snapshot(working);
        System.arraycopy(now, 0, from, 0, Math.min(now.length, from.length));
        to = next;
        elapsed = 0f;
        if (finished) {
            finished = false;
            root.animator().start(this);
        }
    }

    @Override
    public boolean tick(float dt) {
        if (cancelled || finished) {
            return false;
        }
        if (root.theme() != working) {
            // Тему корня заменили другим способом — переход больше не нужен.
            finished = true;
            return false;
        }
        elapsed += dt;
        float p = duration <= 0f ? 1f : Math.min(1f, elapsed / duration);
        ThemeBlend.apply(working, from, to, Easing.IN_OUT_CUBIC.apply(p));
        if (p >= 1f) {
            finished = true;
            return false;
        }
        return true;
    }
}
