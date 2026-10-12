package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Диаграмма: линии, области, столбцы, круговая или кольцевая. Значения при появлении «вырастают»
 * из нуля, а при {@link #values} плавно перетекают к новым. Наведение показывает значение.
 *
 * <pre>{@code
 * new Chart(Chart.Kind.BAR)
 *     .labels("Пн", "Вт", "Ср")
 *     .series("Шахта", 0xFF8AB4F8, 12, 30, 18)
 *     .series("Ферма", 0xFFF6C177, 8, 10, 25);
 * }</pre>
 */
public class Chart extends Widget<Chart> implements Animation {
    public enum Kind {
        LINE, AREA, BAR, PIE, DONUT
    }

    private static final class Series {
        final String name;
        final int color;
        float[] target;
        float[] shown;

        Series(String name, int color, float[] values) {
            this.name = name;
            this.color = color;
            this.target = values.clone();
            this.shown = new float[values.length];
        }
    }

    /** Цвета по умолчанию для круговой диаграммы и серий без цвета. */
    public static final int[] PALETTE = {0xFF8AB4F8, 0xFFF6C177, 0xFF9CCFD8, 0xFFEB6F92, 0xFFC4A7E7, 0xFF31748F,
            0xFFA6E3A1, 0xFFF38BA8};

    private final Kind kind;
    private final List<Series> series = new ArrayList<>();
    private String[] labels = new String[0];
    private float fixedMax = Float.NaN;
    private int gridLines = 4;
    private boolean legend = true;
    private String unit = "";
    private boolean animating;
    private int hover = -1;
    private int hoverSeries = -1;

    public Chart(Kind kind) {
        super("chart");
        this.kind = kind;
        size(kind == Kind.PIE || kind == Kind.DONUT ? 180 : 240, 120);
    }

    public Kind kind() {
        return kind;
    }

    public Chart series(String name, int color, float... values) {
        series.add(new Series(name, color, values));
        restart();
        return this;
    }

    /** Новые значения серии: столбцы и линии перетекают к ним. */
    public Chart values(int seriesIndex, float... values) {
        Series s = series.get(seriesIndex);
        float[] shown = Arrays.copyOf(s.shown, values.length);
        s.target = values.clone();
        s.shown = shown;
        restart();
        return this;
    }

    public float[] values(int seriesIndex) {
        return series.get(seriesIndex).target.clone();
    }

    /** Подписи по оси X (или названия долей круговой диаграммы). */
    public Chart labels(String... labels) {
        this.labels = labels.clone();
        return this;
    }

    /** Верх шкалы; по умолчанию — наибольшее значение с запасом. */
    public Chart max(float max) {
        this.fixedMax = max;
        return this;
    }

    public Chart grid(int lines) {
        this.gridLines = Math.max(0, lines);
        return this;
    }

    public Chart legend(boolean show) {
        this.legend = show;
        return this;
    }

    /** Подпись к значениям во всплывающей подсказке: «шт.», «%», « блоков». */
    public Chart unit(String unit) {
        this.unit = unit == null ? "" : unit;
        return this;
    }

    private void restart() {
        if (!animating && root() != null) {
            animating = true;
            root().animator().start(this);
        }
    }

    @Override
    protected void onAttach() {
        animating = false;
        restart();
    }

    @Override
    protected void onDetach() {
        animating = false;
    }

    @Override
    public boolean tick(float dt) {
        if (!animating) {
            return false;
        }
        boolean moving = false;
        float k = 1f - (float) Math.exp(-dt * 9f);
        for (Series s : series) {
            for (int i = 0; i < s.shown.length; i++) {
                float d = s.target[i] - s.shown[i];
                if (Math.abs(d) > Math.max(1e-3f, Math.abs(s.target[i]) * 1e-3f)) {
                    s.shown[i] += d * k;
                    moving = true;
                } else {
                    s.shown[i] = s.target[i];
                }
            }
        }
        animating = moving;
        return moving;
    }

    /** Закончить анимацию сразу (тесты, скриншоты). */
    public Chart settle() {
        for (Series s : series) {
            s.shown = s.target.clone();
        }
        return this;
    }

    private int points() {
        int n = 0;
        for (Series s : series) {
            n = Math.max(n, s.target.length);
        }
        return n;
    }

    private float scaleMax() {
        if (!Float.isNaN(fixedMax)) {
            return fixedMax;
        }
        float m = 0f;
        for (Series s : series) {
            for (float v : s.target) {
                m = Math.max(m, v);
            }
        }
        return niceCeil(m);
    }

    /** Округление вверх до «красивого» числа: 1, 2, 2.5, 5 × 10ⁿ. */
    static float niceCeil(float v) {
        if (v <= 0f) {
            return 1f;
        }
        double p = Math.pow(10, Math.floor(Math.log10(v)));
        for (double f : new double[]{1, 2, 2.5, 5, 10}) {
            if (f * p >= v) {
                return (float) (f * p);
            }
        }
        return (float) (10 * p);
    }

    static String format(float v) {
        if (Math.abs(v - Math.round(v)) < 1e-3f) {
            return Integer.toString(Math.round(v));
        }
        return String.format(Locale.ROOT, "%.1f", v);
    }

    // ---------------------------------------------------------------- геометрия

    private float legendHeight() {
        return legend && (series.size() > 1 || isRound()) ? 12f : 0f;
    }

    private boolean isRound() {
        return kind == Kind.PIE || kind == Kind.DONUT;
    }

    private float axisWidth(TextMetrics m) {
        return m.width(format(scaleMax())) + 4f;
    }

    // ---------------------------------------------------------------- ввод

    @Override
    protected void handleEvent(UiEvent event) {
        if (!(event instanceof PointerEvent p)) {
            return;
        }
        if (event.type() == PointerEvent.LEAVE) {
            hover = -1;
            hoverSeries = -1;
        } else if (event.type() == PointerEvent.MOVE) {
            if (isRound()) {
                hoverSeries = 0;
                hover = sliceAt(p.localX(), p.localY());
            } else {
                hoverSeries = -1;
                hover = columnAt(p.localX());
            }
        }
    }

    private int columnAt(float lx) {
        TextMetrics m = Lumina.platform().textMetrics();
        float left = padding().left() + axisWidth(m);
        float w = width() - padding().right() - left;
        int n = points();
        if (n == 0 || lx < left || lx > left + w) {
            return -1;
        }
        if (kind == Kind.BAR) {
            return Math.min(n - 1, (int) ((lx - left) / (w / n)));
        }
        return n == 1 ? 0 : Math.round((lx - left) / w * (n - 1));
    }

    private int sliceAt(float lx, float ly) {
        if (series.isEmpty()) {
            return -1;
        }
        float cx = pieCx();
        float cy = pieCy();
        float r = pieRadius();
        float dx = lx - cx;
        float dy = ly - cy;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d > r || (kind == Kind.DONUT && d < r * 0.55f)) {
            return -1;
        }
        float angle = (float) Math.toDegrees(Math.atan2(dx, -dy));
        if (angle < 0f) {
            angle += 360f;
        }
        float[] v = series.get(0).target;
        float total = 0f;
        for (float x : v) {
            total += Math.max(0f, x);
        }
        float acc = 0f;
        for (int i = 0; i < v.length; i++) {
            acc += Math.max(0f, v[i]) / Math.max(1e-6f, total) * 360f;
            if (angle <= acc) {
                return i;
            }
        }
        return -1;
    }

    private float pieRadius() {
        float h = height() - padding().vertical() - legendHeight();
        float w = width() - padding().horizontal();
        return Math.max(4f, Math.min(w * 0.5f, h) / 2f - 2f);
    }

    private float pieCx() {
        return padding().left() + pieRadius() + 2f;
    }

    private float pieCy() {
        return padding().top() + (height() - padding().vertical() - legendHeight()) / 2f;
    }

    // ---------------------------------------------------------------- отрисовка

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        if (series.isEmpty()) {
            return;
        }
        if (isRound()) {
            paintPie(canvas);
        } else {
            paintAxes(canvas);
        }
    }

    private void paintAxes(Canvas canvas) {
        TextMetrics m = canvas.textMetrics();
        int text = style.get(Styles.TEXT_COLOR);
        float max = scaleMax();
        float left = padding().left() + axisWidth(m);
        float top = padding().top() + 4f;
        float right = width() - padding().right();
        float bottom = height() - padding().bottom() - legendHeight() - (labels.length > 0 ? 11f : 0f);
        float w = right - left;
        float h = bottom - top;
        if (w <= 4f || h <= 4f) {
            return;
        }
        // сетка и шкала
        for (int i = 0; i <= gridLines; i++) {
            float y = bottom - h * i / Math.max(1, gridLines);
            canvas.fillRect(left, y, w, 1f, Color.withAlpha(text, i == 0 ? 0x50 : 0x18));
            String s = format(max * i / Math.max(1, gridLines));
            canvas.drawText(s, left - 3f - m.width(s), y - m.lineHeight() / 2f, Color.withAlpha(text, 0x90), false);
        }
        int n = points();
        float slot = kind == Kind.BAR ? w / Math.max(1, n) : (n > 1 ? w / (n - 1) : w);
        if (hover >= 0 && hover < n) {
            float hx = kind == Kind.BAR ? left + slot * hover : left + slot * hover - 1f;
            canvas.fillRect(hx, top, kind == Kind.BAR ? slot : 2f, h, Color.withAlpha(text, 0x14));
        }
        for (int i = 0; i < labels.length && i < n; i++) {
            float cx = kind == Kind.BAR ? left + slot * (i + 0.5f) : left + (n > 1 ? slot * i : w / 2f);
            String s = Table.fit(m, labels[i], kind == Kind.BAR || n > 1 ? slot : w);
            float x = Math.max(left - 2f, Math.min(right - m.width(s), cx - m.width(s) / 2f));
            canvas.drawText(s, x, bottom + 3f, Color.withAlpha(text, i == hover ? 0xFF : 0x90), false);
        }

        if (kind == Kind.BAR) {
            int k = series.size();
            float groupW = slot * 0.72f;
            float barW = groupW / k;
            for (int si = 0; si < k; si++) {
                Series s = series.get(si);
                for (int i = 0; i < s.shown.length; i++) {
                    float v = Math.max(0f, Math.min(max, s.shown[i]));
                    float bh = h * v / max;
                    float x = left + slot * i + (slot - groupW) / 2f + barW * si;
                    int c = i == hover ? Color.lighten(s.color, 0.12f) : s.color;
                    canvas.fillRoundRect(x + 0.5f, bottom - bh, Math.max(1f, barW - 1f), bh,
                            Radii.top(Math.min(3f, barW / 3f)),
                            Brush.vertical(c, Color.withAlpha(c, 0xB0)));
                }
            }
        } else {
            for (Series s : series) {
                int pts = s.shown.length;
                if (pts == 0) {
                    continue;
                }
                float[] xs = new float[pts];
                float[] ys = new float[pts];
                for (int i = 0; i < pts; i++) {
                    xs[i] = pts > 1 && n > 1 ? left + slot * i : left + w / 2f;
                    ys[i] = bottom - h * Math.max(0f, Math.min(max, s.shown[i])) / max;
                }
                if (kind == Kind.AREA) {
                    int top0 = Color.withAlpha(s.color, 0x70);
                    int bot0 = Color.withAlpha(s.color, 0x08);
                    for (int i = 0; i + 1 < pts; i++) {
                        // столбик между точками; вертикальный градиент по высоте области
                        float ta = (bottom - ys[i]) / h;
                        float tb = (bottom - ys[i + 1]) / h;
                        canvas.fillQuad(xs[i], ys[i], xs[i + 1], ys[i + 1], xs[i + 1], bottom, xs[i], bottom,
                                Color.lerp(bot0, top0, ta), Color.lerp(bot0, top0, tb), bot0, bot0);
                    }
                }
                for (int i = 0; i + 1 < pts; i++) {
                    canvas.drawLine(xs[i], ys[i], xs[i + 1], ys[i + 1], 1.5f, s.color);
                }
                for (int i = 0; i < pts; i++) {
                    float r = i == hover ? 3f : 2f;
                    canvas.fillCircle(xs[i], ys[i], r, Brush.solid(s.color));
                }
            }
        }
        paintLegend(canvas, m);
        if (hover >= 0 && hover < n) {
            paintTooltip(canvas, m, left, top, right);
        }
    }

    private void paintTooltip(Canvas canvas, TextMetrics m, float left, float top, float right) {
        List<String> lines = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();
        if (hover < labels.length) {
            lines.add(labels[hover]);
            colors.add(0);
        }
        for (Series s : series) {
            if (hover < s.target.length) {
                lines.add((series.size() > 1 ? s.name + ": " : "") + format(s.target[hover]) + unit);
                colors.add(s.color);
            }
        }
        float tw = 0f;
        for (String l : lines) {
            tw = Math.max(tw, m.width(l));
        }
        tw += 14f;
        float th = lines.size() * (m.lineHeight() + 1f) + 6f;
        int n = points();
        float w = right - left;
        float anchor = kind == Kind.BAR ? left + w / n * (hover + 0.5f) : left + (n > 1 ? w / (n - 1) * hover : w / 2f);
        float x = anchor + 6f + tw > right ? anchor - 6f - tw : anchor + 6f;
        float y = top;
        canvas.fillRoundRect(x, y, tw, th, Radii.all(4f), Brush.solid(0xE0101018));
        for (int i = 0; i < lines.size(); i++) {
            float ly = y + 3f + i * (m.lineHeight() + 1f);
            int c = colors.get(i);
            if (c != 0) {
                canvas.fillRoundRect(x + 4f, ly + 2f, 4f, 4f, Radii.all(1f), Brush.solid(c));
            }
            canvas.drawText(lines.get(i), x + (c != 0 ? 11f : 4f), ly, 0xFFFFFFFF, false);
        }
    }

    private void paintPie(Canvas canvas) {
        TextMetrics m = canvas.textMetrics();
        int text = style.get(Styles.TEXT_COLOR);
        float[] v = series.get(0).shown;
        float[] target = series.get(0).target;
        float total = 0f;
        float shownTotal = 0f;
        for (int i = 0; i < v.length; i++) {
            total += Math.max(0f, target[i]);
            shownTotal += Math.max(0f, v[i]);
        }
        float r = pieRadius();
        float cx = pieCx();
        float cy = pieCy();
        float inner = kind == Kind.DONUT ? r * 0.55f : 0.5f;
        // при появлении круг «дорисовывается» по часовой стрелке
        float reveal = total <= 0f ? 0f : Math.min(1f, shownTotal / total);
        float start = 0f;
        for (int i = 0; i < v.length; i++) {
            float sweep = total <= 0f ? 0f : Math.max(0f, target[i]) / total * 360f * reveal;
            int c = PALETTE[i % PALETTE.length];
            float grow = i == hover ? 3f : 0f;
            if (sweep > 0f) {
                canvas.drawArc(cx, cy, inner, r - inner + grow, start, sweep, c, Color.lighten(c, 0.08f));
            }
            start += sweep;
        }
        if (kind == Kind.DONUT) {
            String center = hover >= 0 && hover < target.length ? format(target[hover]) + unit : format(total) + unit;
            canvas.drawText(center, cx - m.width(center) / 2f, cy - m.lineHeight() / 2f, text, false);
        }
        // подписи справа от круга
        float lx = cx + r + 10f;
        float ly = cy - target.length * (m.lineHeight() + 2f) / 2f;
        for (int i = 0; i < target.length; i++) {
            String name = i < labels.length ? labels[i] : "#" + (i + 1);
            int pct = total <= 0f ? 0 : Math.round(Math.max(0f, target[i]) / total * 100f);
            String s = Table.fit(m, name + "  " + pct + "%", width() - padding().right() - lx - 9f);
            canvas.fillRoundRect(lx, ly + 2f, 5f, 5f, Radii.all(1.5f), Brush.solid(PALETTE[i % PALETTE.length]));
            canvas.drawText(s, lx + 9f, ly, i == hover ? text : Color.withAlpha(text, 0xB0), false);
            ly += m.lineHeight() + 2f;
        }
    }

    private void paintLegend(Canvas canvas, TextMetrics m) {
        if (legendHeight() <= 0f) {
            return;
        }
        int text = style.get(Styles.TEXT_COLOR);
        float x = padding().left() + axisWidth(m);
        float y = height() - padding().bottom() - m.lineHeight();
        for (Series s : series) {
            canvas.fillRoundRect(x, y + 2f, 6f, 6f, Radii.all(2f), Brush.solid(s.color));
            canvas.drawText(s.name, x + 9f, y, Color.withAlpha(text, 0xC0), false);
            x += 9f + m.width(s.name) + 10f;
        }
    }
}
