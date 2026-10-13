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

    /** Код языка, выбранного в игре (например, {@code ru_ru}). */
    default String gameLanguage() {
        return "en_us";
    }

    /** Название языка для показа или {@code null}, если игра такого не знает. */
    default String languageName(String language) {
        return null;
    }

    /** Перевод ключа из lang-файлов игры на указанный язык (не обязательно текущий) или {@code null}. */
    default String translate(String language, String key) {
        return null;
    }

    /** Сохранённая настройка библиотеки или {@code null}. */
    default String readSetting(String key) {
        return null;
    }

    default void writeSetting(String key, String value) {
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

    // ================================================================ HUD и данные игры

    /**
     * Заполнить снимок состояния игрока (здоровье, координаты, инвентарь...) — вызывается на каждом тике
     * клиента из {@link io.github.ssbaxys.lumina.game.GameData#update()}. {@code out} уже сброшен в значения
     * по умолчанию и переиспользуется: реализация не должна его запоминать. По умолчанию мира нет.
     *
     * @return {@code true}, если игрок в мире и данные заполнены
     */
    default boolean pollGameData(io.github.ssbaxys.lumina.game.GameState out) {
        return false;
    }

    /**
     * Что сейчас мешает HUD — битовая маска флагов {@code HudVisibility.GUI_HIDDEN}, {@code DEBUG_SCREEN},
     * {@code CHAT_OPEN}, {@code SCREEN_OPEN}. Вызывается каждый кадр, поэтому должна быть дешёвой.
     * По умолчанию {@code 0}: ничто не мешает.
     */
    default int hudState() {
        return 0;
    }

    // ================================================================ карта и геймпад

    /**
     * Карта вокруг игрока для {@code MiniMap}: цвета верхних блоков (как на ванильной карте) радиусом не меньше
     * {@code radius} блоков, или {@code null}, если мира нет. Вызывается каждый кадр: загрузчик кэширует сетку
     * и дочитывает её понемногу ({@code map.MapGridSampler}).
     */
    default io.github.ssbaxys.lumina.map.MapSample sampleMap(int radius) {
        return null;
    }

    /**
     * Блоки клиентского мира по одному для снимков ({@code blocks.WorldCapture}, {@code StructurePreview}) или
     * {@code null}, если мира нет (по умолчанию). Загрузчик держит один объект на мир: при смене мира или
     * измерения возвращает новый.
     */
    default io.github.ssbaxys.lumina.blocks.WorldBlocks worldBlocks() {
        return null;
    }

    /**
     * Блоки мира в коробке (углы включительно) одной постройкой с началом в минимальном углу, или {@code null},
     * если мира нет. Читает всё сразу, поэтому коробка обрезается до 48 блоков по оси; для живого вида, который
     * дочитывается по кадрам, — {@code blocks.WorldCapture}.
     */
    default io.github.ssbaxys.lumina.blocks.BlockStructure captureWorld(int x0, int y0, int z0, int x1, int y1, int z1) {
        return io.github.ssbaxys.lumina.blocks.WorldCapture.capture(worldBlocks(), x0, y0, z0, x1, y1, z1);
    }

    /**
     * Состояние первого подключённого геймпада или {@code null}, если его нет (по умолчанию — нет).
     * Корень опрашивает его каждый кадр для навигации ({@code GamepadNavigator}).
     */
    default io.github.ssbaxys.lumina.input.Gamepad.State pollGamepad() {
        return null;
    }

    // ================================================================ доступность

    /**
     * Озвучить текст экранным диктором игры (подпись элемента в фокусе). По умолчанию ничего не делает;
     * загрузчик может передать текст встроенному диктору Minecraft.
     */
    default void narrate(String text) {
    }

    // ================================================================ ввод

    /**
     * Зажат ли сейчас Shift (Shift+клик по слоту переносит стопку быстро). Событиям мыши ядра модификаторы
     * не передаются, поэтому их спрашивают у игры. По умолчанию {@code false}.
     */
    default boolean isShiftDown() {
        return false;
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
