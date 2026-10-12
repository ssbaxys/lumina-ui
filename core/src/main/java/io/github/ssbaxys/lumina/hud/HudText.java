package io.github.ssbaxys.lumina.hud;

import io.github.ssbaxys.lumina.render.TextMetrics;

import java.util.ArrayList;
import java.util.List;

/** Перенос и обрезка текста для виджетов HUD; вызываются при раскладке, а не каждый кадр. */
final class HudText {
    private static final String ELLIPSIS = "…";

    private HudText() {
    }

    /** Обрезать строку до ширины {@code max}, добавив «…». */
    static String ellipsize(String text, float max, TextMetrics m) {
        if (text.isEmpty() || m.width(text) <= max) {
            return text;
        }
        float room = max - m.width(ELLIPSIS);
        int end = text.length();
        while (end > 0 && m.width(text.substring(0, end)) > room) {
            end--;
        }
        return text.substring(0, end).stripTrailing() + ELLIPSIS;
    }

    /** Перенос по словам в ширину {@code max}; строк не больше {@code maxLines} (последняя с «…»). */
    static List<String> wrap(String text, float max, TextMetrics m, int maxLines) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }
        for (String paragraph : text.split("\n", -1)) {
            StringBuilder line = new StringBuilder();
            for (String word : paragraph.split(" ")) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (line.length() > 0 && m.width(candidate) > max) {
                    out.add(line.toString());
                    line.setLength(0);
                    line.append(word);
                } else {
                    line.setLength(0);
                    line.append(candidate);
                }
            }
            out.add(line.toString());
        }
        for (int i = 0; i < out.size(); i++) {
            out.set(i, ellipsize(out.get(i), max, m));
        }
        if (out.size() > maxLines) {
            List<String> cut = new ArrayList<>(out.subList(0, maxLines));
            String last = cut.get(maxLines - 1);
            if (!last.endsWith(ELLIPSIS)) {
                String dotted = last + ELLIPSIS;
                while (m.width(dotted) > max && !last.isEmpty()) {
                    last = last.substring(0, last.length() - 1);
                    dotted = last.stripTrailing() + ELLIPSIS;
                }
                cut.set(maxLines - 1, dotted);
            }
            return cut;
        }
        return out;
    }

    /**
     * Секунды → «М:СС» или «Ч:ММ:СС»; {@code tenths} — с десятыми («0:09.4»). {@code roundUp} — для обратного
     * отсчёта: 9.3 с показываются как «0:10», а «0:00» — только когда время вышло.
     */
    static String formatTime(float seconds, boolean tenths, boolean roundUp) {
        float s = Math.max(0f, seconds);
        StringBuilder sb = new StringBuilder(10);
        if (tenths) {
            int t = roundUp ? (int) Math.ceil(s * 10f - 1e-3f) : (int) (s * 10f);
            appendClock(sb, t / 10);
            sb.append('.').append(t % 10);
        } else {
            appendClock(sb, roundUp ? (int) Math.ceil(s - 1e-4f) : (int) s);
        }
        return sb.toString();
    }

    /** Ключ отображаемого значения: меняется ровно тогда, когда меняется строка {@link #formatTime}. */
    static int timeKey(float seconds, boolean tenths, boolean roundUp) {
        float s = Math.max(0f, seconds) * (tenths ? 10f : 1f);
        return roundUp ? (int) Math.ceil(s - (tenths ? 1e-3f : 1e-4f)) : (int) s;
    }

    private static void appendClock(StringBuilder sb, int total) {
        int h = total / 3600;
        int m = (total / 60) % 60;
        int sec = total % 60;
        if (h > 0) {
            sb.append(h).append(':');
            if (m < 10) {
                sb.append('0');
            }
        }
        sb.append(m).append(':');
        if (sec < 10) {
            sb.append('0');
        }
        sb.append(sec);
    }
}
