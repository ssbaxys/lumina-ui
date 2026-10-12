package io.github.ssbaxys.lumina.platform;

import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;

/** Всё, что ядру нужно от игры и загрузчика. Реализуется в каждом модуле загрузчика. */
public interface Platform {
    TextMetrics textMetrics();

    String getClipboard();

    void setClipboard(String text);

    /** Стандартный звук нажатия кнопки интерфейса. */
    void playClickSound();

    /** Звук игры по идентификатору ("minecraft:entity.generic.explode"). По умолчанию — тишина. */
    default void playSound(String soundId, float volume, float pitch) {
    }

    /**
     * Текст ресурса по идентификатору "мод:путь" ({@code luminatest:ui/page.html} →
     * {@code assets/luminatest/ui/page.html}) или {@code null}. Загрузчики читают через менеджер ресурсов
     * игры, поэтому подхватываются ресурспаки (и их перезагрузка по F3+T).
     */
    default String readResource(String id) {
        byte[] bytes = readResourceBytes(id);
        return bytes == null ? null : new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Байты ресурса "мод:путь" или {@code null} (текстуры, звуки, данные). */
    default byte[] readResourceBytes(String id) {
        int colon = id.indexOf(':');
        String path = colon < 0 ? "assets/minecraft/" + id : "assets/" + id.substring(0, colon) + "/" + id.substring(colon + 1);
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = Platform.class.getClassLoader();
        }
        try (java.io.InputStream in = cl.getResourceAsStream(path)) {
            return in == null ? null : in.readAllBytes();
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /**
     * Загрузить картинку в видеопамять как текстуру игры: {@code argb} — пиксели построчно, слева направо,
     * сверху вниз. Повторный вызов с тем же {@code key} заменяет текстуру. По умолчанию {@code null}:
     * платформа не умеет, и ядро обходится без картинки ({@code Images} вернёт {@code null}).
     */
    default TextureRef createTexture(String key, int width, int height, int[] argb) {
        return null;
    }

    default long nanoTime() {
        return System.nanoTime();
    }

    // ================================================================ файлы

    /**
     * Каталог для файлов библиотеки (сохранённые темы, настройки). По умолчанию {@code config/luminaui}
     * относительно рабочего каталога — в игре это папка экземпляра Minecraft.
     */
    default java.nio.file.Path dataDirectory() {
        return java.nio.file.Path.of("config", io.github.ssbaxys.lumina.Lumina.MOD_ID);
    }

    /**
     * Текст файла внутри {@link #dataDirectory()} или {@code null}, если файла нет или путь недопустим
     * (абсолютный, с {@code ..}).
     */
    default String readFile(String relativePath) {
        java.nio.file.Path p = DataPaths.resolve(dataDirectory(), relativePath);
        if (p == null || !java.nio.file.Files.isRegularFile(p)) {
            return null;
        }
        try {
            return java.nio.file.Files.readString(p, java.nio.charset.StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            return null;
        }
    }

    /** Записать текст в файл внутри {@link #dataDirectory()} (каталоги создаются); {@code false} — не вышло. */
    default boolean writeFile(String relativePath, String text) {
        java.nio.file.Path p = DataPaths.resolve(dataDirectory(), relativePath);
        if (p == null) {
            return false;
        }
        try {
            if (p.getParent() != null) {
                java.nio.file.Files.createDirectories(p.getParent());
            }
            java.nio.file.Files.writeString(p, text == null ? "" : text, java.nio.charset.StandardCharsets.UTF_8);
            return true;
        } catch (java.io.IOException e) {
            return false;
        }
    }

    /** Имена файлов в подкаталоге {@link #dataDirectory()} (без вложенных), по алфавиту; нет каталога — пусто. */
    default java.util.List<String> listFiles(String relativeDir) {
        java.nio.file.Path dir = DataPaths.resolve(dataDirectory(), relativeDir == null || relativeDir.isEmpty() ? "." : relativeDir);
        if (dir == null || !java.nio.file.Files.isDirectory(dir)) {
            return java.util.List.of();
        }
        try (java.util.stream.Stream<java.nio.file.Path> s = java.nio.file.Files.list(dir)) {
            return s.filter(java.nio.file.Files::isRegularFile).map(f -> f.getFileName().toString()).sorted().toList();
        } catch (java.io.IOException e) {
            return java.util.List.of();
        }
    }

    // ================================================================ мир (для расписания тем)

    /**
     * Время суток текущего мира в тиках, {@code 0..24000} (0 — восход, 6000 — полдень, 13000 — ночь),
     * или отрицательное число, если мира нет. Ядро тогда берёт системные часы ({@code WorldInfo}).
     */
    default float dayTime() {
        return -1f;
    }

    /** Идентификатор биома под игроком ({@code "minecraft:desert"}) или {@code null}, если мира нет. */
    default String biomeId() {
        return null;
    }

    // ================================================================ карта

    /**
     * Карта вокруг игрока для {@code MiniMap}: цвета верхних блоков (как на ванильной карте) радиусом не меньше
     * {@code radius} блоков, или {@code null}, если мира нет. Вызывается каждый кадр: загрузчик кэширует сетку
     * и дочитывает её понемногу ({@code map.MapGridSampler}).
     */
    default io.github.ssbaxys.lumina.map.MapSample sampleMap(int radius) {
        return null;
    }

    // ================================================================ доступность

    /**
     * Озвучить текст экранным диктором игры (подпись элемента в фокусе). По умолчанию ничего не делает;
     * загрузчик может передать текст встроенному диктору Minecraft.
     */
    default void narrate(String text) {
    }

    /** Безопасное разрешение относительных путей внутри каталога данных. */
    final class DataPaths {
        private DataPaths() {
        }

        /** Путь внутри {@code base} или {@code null}, если он выходит за его пределы. */
        public static java.nio.file.Path resolve(java.nio.file.Path base, String relative) {
            if (relative == null || relative.isEmpty()) {
                return null;
            }
            java.nio.file.Path root = base.toAbsolutePath().normalize();
            java.nio.file.Path rel;
            try {
                rel = java.nio.file.Path.of(relative);
            } catch (RuntimeException e) {
                return null;
            }
            if (rel.isAbsolute()) {
                return null;
            }
            java.nio.file.Path p = root.resolve(rel).normalize();
            return p.startsWith(root) ? p : null;
        }
    }
}
