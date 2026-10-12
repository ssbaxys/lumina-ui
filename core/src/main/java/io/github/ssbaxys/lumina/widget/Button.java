package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.Lumina;
import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.PointerEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Subscription;
import io.github.ssbaxys.lumina.render.Canvas;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.TextMetrics;
import io.github.ssbaxys.lumina.render.TextureRef;
import io.github.ssbaxys.lumina.sound.UiSounds;
import io.github.ssbaxys.lumina.style.Styles;

/**
 * Кнопка с текстом и (по желанию) иконкой слева: текстура ({@link #icon}) или предмет ({@link #item}).
 * Без текста — кнопка-иконка ({@link #round}). Нажатие мышью (левая кнопка) или Enter/Space в фокусе
 * вызывает {@link #onPress}. Режим {@link #loading} — спиннер вместо иконки, кнопка недоступна.
 *
 * <p>Внешний вид задаётся стилем "button" темы и модификаторами: {@code styleClass("button outline")},
 * а также primary, ghost, pill, gradient, danger, success, icon, big.</p>
 */
public class Button extends Widget<Button> {
    private static final float ICON_GAP = 4f;

    private String text;
    private Runnable onPress;
    private boolean clickSound = true;
    private TextureRef iconTexture;
    private String iconItem;
    private float iconSize;
    private boolean loading;
    private boolean enabledBeforeLoading = true;
    private ReadableSignal<Boolean> loadingBinding;
    private Subscription loadingSubscription;

    public Button(String text) {
        super("button");
        this.text = text == null ? "" : text;
        setFocusable(true);
    }

    public Button(String text, Runnable onPress) {
        this(text);
        this.onPress = onPress;
    }

    /** Круглая кнопка-иконка с текстурой: {@code Button.round(TextureRef.vanilla("item/apple"), this::eat)}. */
    public static Button round(TextureRef icon, Runnable onPress) {
        return new Button("", onPress).icon(icon).styleClass("button icon");
    }

    /** Круглая кнопка-иконка с предметом: {@code Button.roundItem("minecraft:compass", this::openMap)}. */
    public static Button roundItem(String itemId, Runnable onPress) {
        return new Button("", onPress).item(itemId).styleClass("button icon");
    }

    public String text() {
        return text;
    }

    public Button text(String text) {
        this.text = text == null ? "" : text;
        invalidateLayout();
        return this;
    }

    public Button onPress(Runnable onPress) {
        this.onPress = onPress;
        return this;
    }

    /** Звук нажатия: {@code Styles.CLICK_SOUND} из стиля, без него — стандартный щелчок; {@code false} — тишина. */
    public Button clickSound(boolean enabled) {
        this.clickSound = enabled;
        return this;
    }

    @Override
    protected String defaultAccessibleLabel() {
        return text.isEmpty() ? null : text;
    }

    // ---------------------------------------------------------------- иконка

    /** Иконка-текстура слева от текста ({@code null} — убрать). */
    public Button icon(TextureRef texture) {
        this.iconTexture = texture;
        this.iconItem = null;
        invalidateLayout();
        return this;
    }

    /** Иконка-предмет слева от текста: {@code .item("minecraft:diamond")}. */
    public Button item(String itemId) {
        this.iconItem = itemId;
        this.iconTexture = null;
        invalidateLayout();
        return this;
    }

    /** Размер иконки; по умолчанию 12 рядом с текстом и 16 без текста. */
    public Button iconSize(float size) {
        this.iconSize = size;
        invalidateLayout();
        return this;
    }

    public TextureRef iconTexture() {
        return iconTexture;
    }

    public String iconItem() {
        return iconItem;
    }

    public boolean hasIcon() {
        return iconTexture != null || iconItem != null;
    }

    /** Итоговый размер иконки (и спиннера). */
    public float effectiveIconSize() {
        return iconSize > 0f ? iconSize : text.isEmpty() ? 16f : 12f;
    }

    // ---------------------------------------------------------------- загрузка

    /** Спиннер вместо иконки, кнопка недоступна; после выключения доступность возвращается прежней. */
    public Button loading(boolean on) {
        if (on == loading) {
            return this;
        }
        if (on) {
            enabledBeforeLoading = isEnabled();
            loading = true;
            setEnabled(false);
        } else {
            loading = false;
            setEnabled(enabledBeforeLoading);
        }
        invalidateLayout();
        return this;
    }

    /** Состояние загрузки из сигнала (например, пока идёт запрос). */
    public Button loading(ReadableSignal<Boolean> signal) {
        closeLoading();
        this.loadingBinding = signal;
        if (root() != null) {
            subscribeLoading();
        }
        return this;
    }

    public boolean isLoading() {
        return loading;
    }

    private void subscribeLoading() {
        loading(Boolean.TRUE.equals(loadingBinding.get()));
        loadingSubscription = loadingBinding.subscribe(v -> loading(Boolean.TRUE.equals(v)));
    }

    private void closeLoading() {
        if (loadingSubscription != null) {
            loadingSubscription.close();
            loadingSubscription = null;
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        if (loading && enabled) {
            // Пока идёт загрузка, кнопка недоступна; просьба включить запомнится до её конца.
            enabledBeforeLoading = true;
            return;
        }
        super.setEnabled(enabled);
    }

    @Override
    protected void onAttach() {
        if (loadingBinding != null && loadingSubscription == null) {
            subscribeLoading();
        }
    }

    @Override
    protected void onDetach() {
        closeLoading();
    }

    // ---------------------------------------------------------------- нажатие

    public void press() {
        if (!isEnabled() || loading) {
            return;
        }
        if (clickSound) {
            UiSounds.click(this, true);
        }
        if (onPress != null) {
            onPress.run();
        }
    }

    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() == PointerEvent.CLICK && event.target() == this) {
            if (((PointerEvent) event).button() == Keys.MOUSE_LEFT) {
                press();
                event.consume();
            }
        } else if (event.type() == KeyEvent.PRESS && event.target() == this) {
            int key = ((KeyEvent) event).key();
            if (key == Keys.ENTER || key == Keys.KP_ENTER || key == Keys.SPACE) {
                press();
                event.consume();
            }
        }
    }

    // ---------------------------------------------------------------- раскладка и отрисовка

    private boolean hasIconSlot() {
        return hasIcon() || loading;
    }

    private float textScale() {
        return style.get(Styles.FONT_SCALE);
    }

    @Override
    protected void measureContent(float maxWidth, float maxHeight) {
        TextMetrics m = Lumina.platform().textMetrics();
        float k = textScale();
        float tw = text.isEmpty() ? 0f : m.width(text) * k;
        float slot = hasIconSlot() ? effectiveIconSize() : 0f;
        float gap = slot > 0f && tw > 0f ? ICON_GAP : 0f;
        float th = text.isEmpty() && slot > 0f ? 0f : m.lineHeight() * k;
        setContentSize(slot + gap + tw, Math.max(slot, th));
    }

    @Override
    protected void paintContent(Canvas canvas, float partialTick) {
        TextMetrics m = canvas.textMetrics();
        float k = textScale();
        float tw = text.isEmpty() ? 0f : m.width(text) * k;
        float slot = hasIconSlot() ? effectiveIconSize() : 0f;
        float gap = slot > 0f && tw > 0f ? ICON_GAP : 0f;
        float x = (width() - (slot + gap + tw)) / 2f;
        int textColor = style.get(Styles.TEXT_COLOR);
        if (slot > 0f) {
            float iy = (height() - slot) / 2f;
            if (loading) {
                paintSpinner(canvas, x + slot / 2f, iy + slot / 2f, slot, textColor);
            } else if (iconItem != null) {
                canvas.drawItem(iconItem, x, iy, slot);
            } else {
                canvas.drawTexture(iconTexture, x, iy, slot, slot, Color.WHITE);
            }
            x += slot + gap;
        }
        if (tw > 0f) {
            float lh = m.lineHeight() * k;
            float ty = (height() - lh) / 2f + 0.5f;
            boolean shadow = style.get(Styles.TEXT_SHADOW);
            if (k != 1f) {
                canvas.save();
                canvas.translate(x, ty);
                canvas.scale(k, k);
                canvas.drawText(text, 0f, 0f, textColor, shadow);
                canvas.restore();
            } else {
                canvas.drawText(text, x, ty, textColor, shadow);
            }
        }
    }

    /** Крутящаяся дуга с хвостом, цвета текста. */
    private static void paintSpinner(Canvas canvas, float cx, float cy, float size, int color) {
        float thickness = Math.max(1.5f, size / 7f);
        float radius = size / 2f - thickness / 2f - 0.5f;
        long ms = Lumina.isInitialized() ? Lumina.platform().nanoTime() / 1_000_000L : 0L;
        float spin = (ms % 900L) * 0.4f;
        canvas.drawArc(cx, cy, radius, thickness, spin, 280f, Color.withAlpha(color, 0), color);
    }
}
