package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.platform.Platform;

/**
 * Точка входа библиотеки. Модуль загрузчика вызывает {@link #init(Platform)} один раз
 * при старте клиента; всё остальное ядро получает платформу отсюда, без рефлексии и ServiceLoader.
 */
public final class Lumina {
    public static final String MOD_ID = "luminaui";

    private static Platform platform;

    private Lumina() {
    }

    public static void init(Platform platform) {
        Lumina.platform = platform;
    }

    public static Platform platform() {
        Platform p = platform;
        if (p == null) {
            throw new IllegalStateException("Lumina is not initialized: Lumina.init(Platform) was not called");
        }
        return p;
    }

    public static boolean isInitialized() {
        return platform != null;
    }
}
