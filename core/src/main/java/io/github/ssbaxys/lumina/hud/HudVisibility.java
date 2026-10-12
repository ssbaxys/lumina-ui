package io.github.ssbaxys.lumina.hud;

import java.util.function.BooleanSupplier;

/**
 * Правило видимости элемента HUD: когда его прятать. Проверяется каждый кадр по маске состояния игры
 * ({@code Platform.hudState()}) — дёшево, без выделения памяти. Неизменяемое: методы возвращают копию.
 *
 * <pre>{@code
 * HudVisibility.DEFAULT                       // прятать по F1 и на экране отладки F3
 * HudVisibility.GAMEPLAY                      // ещё и в чате, и под любым открытым экраном
 * HudVisibility.DEFAULT.hideInChat(true).when(() -> GameData.inWorld().get())
 * }</pre>
 */
public final class HudVisibility {
    /** Интерфейс скрыт клавишей F1. */
    public static final int GUI_HIDDEN = 1;
    /** Открыт экран отладки F3. */
    public static final int DEBUG_SCREEN = 1 << 1;
    /** Открыт чат. */
    public static final int CHAT_OPEN = 1 << 2;
    /** Открыт любой экран (инвентарь, меню паузы, чат, экран мода). */
    public static final int SCREEN_OPEN = 1 << 3;

    /** Виден всегда. */
    public static final HudVisibility ALWAYS = new HudVisibility(0, null);
    /** Прячется по F1 и на экране отладки — как ванильный HUD. */
    public static final HudVisibility DEFAULT = new HudVisibility(GUI_HIDDEN | DEBUG_SCREEN, null);
    /** Только в игре: прячется по F1, на F3, в чате и под любым экраном. */
    public static final HudVisibility GAMEPLAY = new HudVisibility(GUI_HIDDEN | DEBUG_SCREEN | CHAT_OPEN | SCREEN_OPEN, null);

    private final int hideMask;
    private final BooleanSupplier condition;

    private HudVisibility(int hideMask, BooleanSupplier condition) {
        this.hideMask = hideMask;
        this.condition = condition;
    }

    /** Прятать, если в состоянии игры есть любой из флагов {@code mask}. */
    public static HudVisibility hiddenWhen(int mask) {
        return new HudVisibility(mask, null);
    }

    public HudVisibility hideWithGui(boolean hide) {
        return flag(GUI_HIDDEN, hide);
    }

    public HudVisibility hideInDebug(boolean hide) {
        return flag(DEBUG_SCREEN, hide);
    }

    public HudVisibility hideInChat(boolean hide) {
        return flag(CHAT_OPEN, hide);
    }

    public HudVisibility hideInScreens(boolean hide) {
        return flag(SCREEN_OPEN, hide);
    }

    /**
     * Дополнительное условие показа (вызывается каждый кадр, пока флаги не спрятали элемент — пусть будет
     * дешёвым, например чтение сигнала). Заменяет прежнее условие; {@code null} — без условия.
     */
    public HudVisibility when(BooleanSupplier condition) {
        return new HudVisibility(hideMask, condition);
    }

    private HudVisibility flag(int bit, boolean on) {
        int mask = on ? hideMask | bit : hideMask & ~bit;
        return mask == hideMask ? this : new HudVisibility(mask, condition);
    }

    /** Маска флагов, при которых элемент прячется. */
    public int hideMask() {
        return hideMask;
    }

    /** Виден ли элемент при таком состоянии игры. */
    public boolean test(int hudState) {
        return (hudState & hideMask) == 0 && (condition == null || condition.getAsBoolean());
    }

    @Override
    public String toString() {
        return "HudVisibility[hide=" + Integer.toBinaryString(hideMask) + (condition != null ? ", when=…" : "") + "]";
    }
}
