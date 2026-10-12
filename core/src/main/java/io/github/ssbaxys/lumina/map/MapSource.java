package io.github.ssbaxys.lumina.map;

import io.github.ssbaxys.lumina.Lumina;

/**
 * Откуда {@code MiniMap} берёт карту: обычно из мира через платформу ({@link #platform()}), без мира —
 * из своего источника, например {@link DemoMap}.
 */
@FunctionalInterface
public interface MapSource {
    /**
     * Карта вокруг игрока радиусом не меньше {@code radius} блоков или {@code null}, если её нет
     * (нет мира). Вызывается каждый кадр, поэтому должна быть дешёвой: тяжёлую работу источник делает
     * постепенно и кэширует.
     */
    MapSample sample(int radius);

    /** Карта текущего мира через {@code Platform.sampleMap}. */
    static MapSource platform() {
        return radius -> Lumina.isInitialized() ? Lumina.platform().sampleMap(radius) : null;
    }

    /** Этот источник, а если он ничего не дал — {@code fallback}. */
    default MapSource or(MapSource fallback) {
        return radius -> {
            MapSample s = sample(radius);
            return s != null || fallback == null ? s : fallback.sample(radius);
        };
    }
}
