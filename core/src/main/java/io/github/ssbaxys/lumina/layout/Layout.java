package io.github.ssbaxys.lumina.layout;

import io.github.ssbaxys.lumina.node.UiElement;

/**
 * Стратегия раскладки детей контейнера. Двухпроходная: measure (снизу вверх, размеры)
 * и arrange (сверху вниз, позиции). Своя раскладка — просто ещё одна реализация.
 */
public interface Layout {
    /**
     * Измерить детей и сообщить размер содержимого через {@link UiElement#setContentSize}.
     * Ограничения — без учёта padding контейнера; могут быть {@code +Infinity}.
     */
    void measure(UiElement container, float maxWidth, float maxHeight);

    /** Расставить детей через {@link UiElement#setBounds} внутри области содержимого. */
    void arrange(UiElement container, float contentX, float contentY, float contentWidth, float contentHeight);

    /** Общая часть: абсолютно позиционированные дети. */
    static void arrangeAbsolute(UiElement child, float contentX, float contentY, float contentWidth, float contentHeight) {
        LayoutParams p = child.layoutParams();
        child.measure(contentWidth, contentHeight);
        child.setBounds(contentX + p.left, contentY + p.top, child.measuredWidth(), child.measuredHeight());
    }
}
