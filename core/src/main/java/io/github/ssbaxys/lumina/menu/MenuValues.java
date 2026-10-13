package io.github.ssbaxys.lumina.menu;

/**
 * Простое хранилище значений меню на сервере: массив чисел по описанию {@link MenuSpec}. Сервер пишет в него,
 * меню загрузчика отправляет изменения клиентам на ближайшем тике.
 *
 * <pre>{@code
 * MenuValues values = new MenuValues(SPEC);
 * values.set("progress", 40);
 * values.set(BURNING, true);          // по готовому MenuSpec.Value — без поиска по имени
 * }</pre>
 */
public final class MenuValues implements MenuData {
    private final MenuSpec spec;
    private final int[] values;

    public MenuValues(MenuSpec spec) {
        this.spec = spec;
        this.values = new int[spec.values().size()];
    }

    public MenuSpec spec() {
        return spec;
    }

    @Override
    public int get(int valueIndex) {
        return values[valueIndex];
    }

    public int get(MenuSpec.Value value) {
        return values[value.index()];
    }

    public int get(String name) {
        return get(spec.value(name));
    }

    public boolean getBoolean(MenuSpec.Value value) {
        return values[value.index()] != 0;
    }

    public MenuValues set(MenuSpec.Value value, int v) {
        values[value.index()] = value.kind() == MenuSpec.ValueKind.BOOLEAN ? (v != 0 ? 1 : 0) : v;
        return this;
    }

    public MenuValues set(MenuSpec.Value value, boolean v) {
        values[value.index()] = v ? 1 : 0;
        return this;
    }

    public MenuValues set(String name, int v) {
        return set(spec.value(name), v);
    }

    public MenuValues set(String name, boolean v) {
        return set(spec.value(name), v);
    }
}
