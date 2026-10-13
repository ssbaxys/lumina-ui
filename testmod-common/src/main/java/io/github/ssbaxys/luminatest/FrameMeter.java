package io.github.ssbaxys.luminatest;

import io.github.ssbaxys.lumina.i18n.I18n;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.widget.Label;

import java.util.Locale;

/** Время кадра, измеренное прямо в отрисовке (сглаженное, обновляется 4 раза в секунду). */
final class FrameMeter extends Label {
    private long last = -1L;
    private float avgMs = 16.7f;
    private float sinceUpdate;

    FrameMeter() {
        super(I18n.get("luminatest.perf.meter.empty"));
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        long now = System.nanoTime();
        if (last >= 0L) {
            float ms = (now - last) / 1_000_000f;
            avgMs += (ms - avgMs) * 0.1f;
            sinceUpdate += ms;
            if (sinceUpdate >= 250f) {
                sinceUpdate = 0f;
                text(I18n.get("luminatest.perf.meter", String.format(Locale.ROOT, "%.2f", avgMs), Math.round(1000f / Math.max(avgMs, 0.01f))));
            }
        }
        last = now;
        super.paintContent(canvas, partialTick);
    }
}
