package io.github.ssbaxys.lumina.platform;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Небольшой файл настроек библиотеки (.properties). Ошибки чтения и записи не мешают игре. */
public final class SettingsFile {
    private final Path path;
    private Properties values;

    public SettingsFile(Path path) {
        this.path = path;
    }

    public String read(String key) {
        return load().getProperty(key);
    }

    public void write(String key, String value) {
        Properties p = load();
        if (value.equals(p.getProperty(key))) {
            return;
        }
        p.setProperty(key, value);
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            try (Writer w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                p.store(w, "Lumina UI");
            }
        } catch (IOException ignored) {
            // настройка просто не сохранится
        }
    }

    private Properties load() {
        if (values == null) {
            values = new Properties();
            if (Files.isRegularFile(path)) {
                try (Reader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    values.load(r);
                } catch (IOException | IllegalArgumentException ignored) {
                    // повреждённый файл — работаем со значениями по умолчанию
                }
            }
        }
        return values;
    }
}
