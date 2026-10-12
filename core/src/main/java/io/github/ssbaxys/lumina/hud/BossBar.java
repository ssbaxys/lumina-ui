package io.github.ssbaxys.lumina.hud;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.anim.Animation;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.widget.Widget;

import java.util.Objects;

/**
 * Полоса как у босса: заголовок по центру и широкая полоса под ним, с делениями ({@link #notches})
 * и градиентом от {@code from} к {@code to}. Значение плавно догоняет новое.
 *
 * <pre>{@code
 * new BossBar("Иссушитель").colors(0xFFB04DFF, 0xFFE94DFF).notches(10).progress(0.8f);
 * new BossBar("Опыт").bind(GameData.xpProgress()).bindTitle(GameData.xpLevel().map(l -> "Уровень " + l));
 * }</pre>
 */
public class BossBar extends Widget<BossBar> implements Animation {
    private String title;
    private float progress = 1f;
    private float shown = 1f;
    private float max = 1f;
    private int notches;
    private float barHeight = 5f;
    private Integer from;
    private Integer to;
    private boolean ticking;
    private ReadableSignal<? extends Number> binding;
    private ReadableSignal<String> titleBinding;
    private Subscription sub;
    private Subscription titleSub;

    public BossBar(String title) {
        super("hud-bar");
        this.title = title == null ? "" : title;
        layoutParams().width = Dim.px(182);
        setHitTestable(false);
    }

    public String title() {
        return title;
    }

    public BossBar title(String title) {
        String t = title == null ? "" : title;
        if (!Objects.equals(this.title, t)) {
            boolean hadTitle = !this.title.isEmpty();
            this.title = t;
            if (hadTitle != !t.isEmpty()) {
                invalidateLayout();
            }
        }
        return this;
    }

    /** Значение 0..max (по умолчанию max = 1). */
    public BossBar progress(float value) {
        progress = Math.max(0f, Math.min(1f, value / max));
        startTicking();
        return this;
    }

    /** Доля 0..1. */
    public float progress() {
        return progress;
    }

    /** Максимум для {@link #progress(float)} и привязки (например, 20 для сытости). */
    public BossBar max(float max) {
        this.max = Math.max(0.0001f, max);
        return this;
    }

    /** Деления поверх полосы, как у ванильных боссбаров: 6, 10, 12, 20 (0 — без делений). */
    public BossBar notches(int count) {
        this.notches = Math.max(0, count);
        return this;
    }

    public BossBar barHeight(float height) {
        this.barHeight = height;
        invalidateLayout();
        return this;
    }

    /** Один цвет полосы. */
    public BossBar color(int argb) {
        return colors(argb, argb);
    }

    /** Градиент полосы слева направо. */
    public BossBar colors(int from, int to) {
        this.from = from;
        this.to = to;
        return this;
    }

    public BossBar bind(ReadableSignal<? extends Number> signal) {
        if (sub != null) {
            sub.close();
            sub = null;
        }
        binding = signal;
        if (root() != null) {
            subscribe();
        }
        return this;
    }

    public BossBar bindTitle(ReadableSignal<String> signal) {
        if (titleSub != null) {
            titleSub.close();
            titleSub = null;
        }
        titleBinding = signal;
        if (root() != null) {
            subscribeTitle();
        }
        return this;
    }

    private void subscribe() {
        progress(binding.get().floatValue());
        shown = progress;
        sub = binding.subscribe(v -> progress(v.floatValue()));
    }

    private void subscribeTitle() {
        title(titleBinding.get());
        titleSub = titleBinding.subscribe(this::title);
    }

    @Override
    protected void onAttach() {
        if (binding != null && sub == null) {
            subscribe();
        }
        if (titleBinding != null && titleSub == null) {
            subscribeTitle();
        }
        startTicking();
    }

    @Override
    protected void onDetach() {
        if (sub != null) {
            sub.close();
            sub = null;
        }
        if (titleSub != null) {
            titleSub.close();
            titleSub = null;
        }
    }

    private void startTicking() {
        if (!ticking && root() != null && shown != progress) {
            ticking = true;
            root().animator().start(this);
        }
    }

    @Override
    public boolean tick(float dt) {
        float k = Math.min(1f, dt * 8f);
        shown += (progress - shown) * k;
        if (Math.abs(progress - shown) < 0.001f) {
            shown = progress;
        }
        ticking = root() != null && shown != progress;
        return ticking;
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float titleH = title.isEmpty() ? 0f : m.lineHeight() + 2f;
        setContentSize(Float.isFinite(maxWidth) ? Math.min(maxWidth, 182f) : 182f, titleH + barHeight);
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        float x = padding().left();
        float y = padding().top();
        float w = width() - padding().horizontal();
        if (!title.isEmpty()) {
            float tw = m.width(title);
            canvas.drawText(title, x + (w - tw) / 2f, y, style.get(Styles.TEXT_COLOR), style.get(Styles.TEXT_SHADOW));
            y += m.lineHeight() + 2f;
        }
        int accent = style.get(Styles.ACCENT_COLOR);
        int a = from != null ? from : accent;
        int b = to != null ? to : Color.lighten(Color.rotateHue(accent, 30f), 0.08f);
        canvas.fillRect(x, y, w, barHeight, style.get(HudStyles.TRACK_COLOR));
        float fw = w * shown;
        if (fw > 0f) {
            // Градиент по заполненной части: правый край — цвет, соответствующий доле.
            int right = Color.lerp(a, b, shown);
            canvas.fillQuad(x, y, x, y + barHeight, x + fw, y + barHeight, x + fw, y, a, a, right, right);
            canvas.fillRect(x, y, fw, 1f, Color.mulAlpha(Color.WHITE, 0.3f));
        }
        if (notches > 1) {
            int notch = Color.mulAlpha(Color.BLACK, 0.45f);
            for (int i = 1; i < notches; i++) {
                float nx = x + w * i / notches;
                canvas.fillRect(nx - 0.5f, y, 1f, barHeight, notch);
            }
        }
    }
}
