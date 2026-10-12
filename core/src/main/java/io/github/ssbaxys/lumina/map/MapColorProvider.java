package io.github.ssbaxys.lumina.map;

/** Цвет карты для любого блока (x, z): для статичной карты мира {@code WorldMap}. 0 — неизвестно. */
@FunctionalInterface
public interface MapColorProvider {
    int colorAt(int x, int z);
}
