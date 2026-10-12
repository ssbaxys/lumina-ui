package io.github.ssbaxys.lumina.node;

/**
 * Элемент с вкладками или разделами, которые переключаются бамперами геймпада ({@code Tabs}, {@code SideNav}).
 * {@link GamepadNavigator} ищет его сначала среди предков элемента в фокусе, затем во всём дереве.
 */
public interface TabSwitchable {
    /** Перейти на {@code delta} вкладок (по кругу); {@code false}, если переключать нечего. */
    boolean switchTab(int delta);
}
