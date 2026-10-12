package io.github.ssbaxys.lumina.style;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.anim.Animator;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.anim.Interpolator;

/**
 * Текущие (отображаемые) значения стиля одного элемента, включая плавные переходы.
 *
 * <p>Наследуемые свойства не копируются вниз по дереву: {@link #get} лениво спрашивает родителя.
 * Поэтому анимация цвета текста у кнопки автоматически видна во всех её потомках.</p>
 */
public final class ComputedStyle implements Animation {
    /** Обратный вызов при изменении значений. */
    public interface Listener {
        void onStyleChanged(boolean layoutChanged);
    }

    private static final Object[] NONE = new Object[0];

    private final Listener listener;
    private ComputedStyle parent;

    private Object[] current = NONE;
    private Object[] from = NONE;
    private Object[] target = NONE;
    private Object[] blend;
    private float elapsed;
    private float duration;
    private Easing easing = Easing.LINEAR;
    private boolean animating;
    private boolean scheduled;
    private boolean animatesLayout;

    public ComputedStyle(Listener listener) {
        this.listener = listener;
    }

    public void setParent(ComputedStyle parent) {
        this.parent = parent;
    }

    public <T> T get(StyleKey<T> key) {
        int id = key.id();
        Object[] c = current;
        Object v = id < c.length ? c[id] : null;
        if (v != null) {
            return key.cast(v);
        }
        if (key.inherited() && parent != null) {
            return parent.get(key);
        }
        return key.defaultValue();
    }

    /**
     * Пересчитать стиль для нового состояния.
     *
     * @param animator {@code null}, чтобы применить без перехода
     */
    public void update(Style style, int state, Animator animator) {
        Object[] next = style.resolve(state);
        if (next == target && !animating && current == target) {
            return;
        }
        Transition tr = valueOf(next, Styles.TRANSITION);
        boolean layout = layoutDiffers(current, next);
        if (animator == null || tr.duration() <= 0f || current == NONE) {
            target = next;
            current = next;
            animating = false;
            listener.onStyleChanged(layout);
            return;
        }
        from = animating ? snapshot() : current;
        target = next;
        elapsed = 0f;
        duration = tr.duration();
        easing = tr.easing();
        animatesLayout = layout;
        animating = true;
        if (!scheduled) {
            scheduled = true;
            animator.start(this);
        }
    }

    @Override
    public boolean tick(float dt) {
        if (!animating) {
            scheduled = false;
            return false;
        }
        elapsed += dt;
        float p = Math.min(1f, elapsed / duration);
        if (p >= 1f) {
            current = target;
            animating = false;
            scheduled = false;
            listener.onStyleChanged(animatesLayout);
            return false;
        }
        float t = easing.apply(p);
        int len = Math.max(from.length, target.length);
        if (blend == null || blend.length < len) {
            blend = new Object[len];
        }
        for (int i = 0; i < len; i++) {
            blend[i] = mix(i, at(from, i), at(target, i), t);
        }
        current = blend;
        listener.onStyleChanged(animatesLayout);
        return true;
    }

    private Object[] snapshot() {
        return current == blend ? blend.clone() : current;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object mix(int id, Object a, Object b, float t) {
        if (a == b || (a != null && a.equals(b))) {
            return b;
        }
        StyleKey key = StyleKey.byId(id);
        Interpolator interp = key.interpolator();
        if (interp == null) {
            return b;
        }
        Object av = a != null ? a : key.defaultValue();
        Object bv = b != null ? b : key.defaultValue();
        return interp.lerp(av, bv, t);
    }

    private static Object at(Object[] a, int i) {
        return i < a.length ? a[i] : null;
    }

    private static <T> T valueOf(Object[] values, StyleKey<T> key) {
        Object v = at(values, key.id());
        return v != null ? key.cast(v) : key.defaultValue();
    }

    private static boolean layoutDiffers(Object[] a, Object[] b) {
        int len = Math.max(a.length, b.length);
        for (int i = 0; i < len; i++) {
            Object x = at(a, i);
            Object y = at(b, i);
            if (x != y && (x == null || !x.equals(y)) && StyleKey.byId(i).affectsLayout()) {
                return true;
            }
        }
        return false;
    }
}
