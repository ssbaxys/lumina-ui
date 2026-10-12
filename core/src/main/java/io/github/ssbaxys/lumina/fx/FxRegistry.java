package io.github.ssbaxys.lumina.fx;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Эффекты по именам: {@code fx.spawn("minecraft:snow", x, y, w, h)}. Встроенные регистрируются под
 * {@code minecraft:*} (см. {@link VanillaFx}); моды добавляют свои под своим пространством имён:
 *
 * <pre>{@code
 * FxRegistry.register("mymod:sparkles", (x, y, w, h) -> new Particles(MY_SHEET).gravity(-20)
 *         .emitter((p, r, dt) -> p.emit(x + r.nextFloat() * w, y + h, 0, -30, 1.2f, 6), 2f));
 * }</pre>
 *
 * Регистрировать нужно до использования, обычно при инициализации мода; чтение — без блокировок.
 */
public final class FxRegistry {
    /** Создаёт эффект в прямоугольнике (x, y, w, h) слоя; точечные эффекты берут его центр. */
    @FunctionalInterface
    public interface Factory {
        Effect create(float x, float y, float w, float h);
    }

    private static final Map<String, Factory> FACTORIES = new LinkedHashMap<>();

    static {
        VanillaFx.registerDefaults();
    }

    private FxRegistry() {
    }

    public static synchronized void register(String id, Factory factory) {
        FACTORIES.put(id, factory);
    }

    public static Factory get(String id) {
        return FACTORIES.get(id);
    }

    /** Новый эффект по имени. */
    public static Effect create(String id, float x, float y, float w, float h) {
        Factory f = FACTORIES.get(id);
        if (f == null) {
            throw new IllegalArgumentException("Unknown effect: " + id + " (known: " + FACTORIES.keySet() + ")");
        }
        return f.create(x, y, w, h);
    }

    /** Все зарегистрированные имена в порядке регистрации. */
    public static Set<String> ids() {
        return Collections.unmodifiableSet(FACTORIES.keySet());
    }
}
