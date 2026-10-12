package io.github.ssbaxys.lumina.platform;

import io.github.ssbaxys.lumina.Lumina;

import java.time.LocalTime;
import java.util.function.Supplier;

/**
 * Источники значений для автоматической смены тем: время суток и биом. Берёт их у платформы
 * ({@link Platform#dayTime()}, {@link Platform#biomeId()}); если мира нет (главное меню) или платформа
 * их не знает — время считается по системным часам, а биом пустой.
 *
 * <pre>{@code
 * ThemeSchedule.byTime(WorldInfo::timeOfDay)...
 * ThemeSchedule.byBiome(WorldInfo::biome)...
 * }</pre>
 */
public final class WorldInfo {
    /** Тиков в сутках Minecraft. */
    public static final float DAY_TICKS = 24000f;

    private static Supplier<LocalTime> clock = LocalTime::now;

    private WorldInfo() {
    }

    /** Время суток в тиках {@code 0..24000}: из мира, иначе по системным часам. */
    public static float timeOfDay() {
        float t = Lumina.isInitialized() ? Lumina.platform().dayTime() : -1f;
        if (t >= 0f) {
            return t % DAY_TICKS;
        }
        LocalTime now = clock.get();
        return ticksOf(now.getHour(), now.getMinute());
    }

    /** Биом под игроком или {@code ""}, если неизвестен. */
    public static String biome() {
        String b = Lumina.isInitialized() ? Lumina.platform().biomeId() : null;
        return b == null ? "" : b;
    }

    /** Часы и минуты (как на часах) → тики: 6:00 = 0, 12:00 = 6000, 18:00 = 12000, 0:00 = 18000. */
    public static float ticksOf(int hour, int minute) {
        float hours = hour + minute / 60f;
        return ((hours - 6f) * 1000f % DAY_TICKS + DAY_TICKS) % DAY_TICKS;
    }

    /** Тики → часы {@code 0..24} (дробные). */
    public static float hoursOf(float ticks) {
        return ((ticks / 1000f + 6f) % 24f + 24f) % 24f;
    }

    /** Тики → «ЧЧ:ММ». */
    public static String formatTime(float ticks) {
        float h = hoursOf(ticks);
        int hh = (int) h;
        int mm = (int) ((h - hh) * 60f);
        return (hh < 10 ? "0" : "") + hh + ":" + (mm < 10 ? "0" : "") + mm;
    }

    /** Подменить системные часы (для тестов); {@code null} — вернуть настоящие. */
    public static void setClock(Supplier<LocalTime> source) {
        clock = source == null ? LocalTime::now : source;
    }
}
