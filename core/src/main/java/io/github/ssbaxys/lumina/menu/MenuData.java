package io.github.ssbaxys.lumina.menu;

/**
 * Значения меню на сервере: откуда меню загрузчика берёт числа для синхронизации ({@link MenuSpec#values()}).
 * Вызывается на каждом тике сервера, пока меню открыто, поэтому может считать значение на лету — из блока,
 * машины или мира. Готовое хранилище — {@link MenuValues}.
 *
 * <pre>{@code
 * MenuData data = index -> index == PROGRESS.index() ? furnace.progress : furnace.burning ? 1 : 0;
 * }</pre>
 */
@FunctionalInterface
public interface MenuData {
    /** Текущее значение номер {@code valueIndex} ({@link MenuSpec.Value#index()}); у флага — 0 или 1. */
    int get(int valueIndex);
}
