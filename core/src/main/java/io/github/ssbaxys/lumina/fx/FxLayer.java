package io.github.ssbaxys.lumina.fx;

import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.ArrayList;
import java.util.Random;

/**
 * Слой эффектов поверх интерфейса: огонь, взрывы, частицы. Обычно растягивается на весь контейнер
 * и не мешает мыши. Тикает только пока есть живые эффекты, поэтому в покое ничего не стоит.
 *
 * <pre>{@code
 * FxLayer fx = new FxLayer();
 * page.add(content, fx);                      // в StackLayout поверх содержимого
 * fx.spawn(VanillaFx.explosion(120, 80, 1f));
 * }</pre>
 */
public class FxLayer extends Widget<FxLayer> implements Animation {
    private final ArrayList<Effect> effects = new ArrayList<>();
    private final ArrayList<Effect> spawned = new ArrayList<>();
    private final Random random = new Random();
    private boolean ticking;
    private boolean scheduled;

    public FxLayer() {
        super(null);
        setHitTestable(false);
        setOverflowsBounds(true);
        layoutParams().width = Dim.FILL;
        layoutParams().height = Dim.FILL;
    }

    public Random random() {
        return random;
    }

    /** Добавить эффект (можно вызывать и из тика другого эффекта: взрыв из TNT). */
    public <E extends Effect> E spawn(E effect) {
        if (ticking) {
            spawned.add(effect);
        } else {
            effects.add(effect);
        }
        schedule();
        return effect;
    }

    /** Эффект по имени из {@link FxRegistry}: {@code spawn("minecraft:snow", 0, 0, width(), height())}. */
    public Effect spawn(String id, float x, float y, float w, float h) {
        return spawn(FxRegistry.create(id, x, y, w, h));
    }

    /** Эффект по имени на весь слой. */
    public Effect spawn(String id) {
        return spawn(id, 0f, 0f, width(), height());
    }

    public void clear() {
        effects.clear();
        spawned.clear();
    }

    public int effectCount() {
        return effects.size() + spawned.size();
    }

    private void schedule() {
        if (!scheduled && root() != null) {
            scheduled = true;
            root().animator().start(this);
        }
    }

    @Override
    protected void onAttach() {
        if (!effects.isEmpty() || !spawned.isEmpty()) {
            schedule();
        }
    }

    @Override
    protected void onDetach() {
        clear();
    }

    @Override
    public boolean tick(float dt) {
        if (root() == null) {
            scheduled = false;
            return false;
        }
        ticking = true;
        try {
            for (int i = effects.size() - 1; i >= 0; i--) {
                if (!effects.get(i).tick(dt, this)) {
                    effects.remove(i);
                }
            }
        } finally {
            ticking = false;
        }
        effects.addAll(spawned);
        spawned.clear();
        if (effects.isEmpty()) {
            scheduled = false;
            return false;
        }
        return true;
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        for (int i = 0, n = effects.size(); i < n; i++) {
            effects.get(i).render(canvas);
        }
    }
}
