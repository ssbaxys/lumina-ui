package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.style.State;
import io.github.ssbaxys.lumina.style.Style;
import io.github.ssbaxys.lumina.style.Styles;
import io.github.ssbaxys.lumina.style.Theme;
import io.github.ssbaxys.lumina.style.Transition;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Panel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class StyleTest {
    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    @Test
    void variantsApplyByStateAndAreCached() {
        Style s = Style.builder()
                .set(Styles.OPACITY, 1f)
                .hovered(h -> h.set(Styles.OPACITY, 0.8f))
                .when(State.HOVERED | State.PRESSED, p -> p.set(Styles.OPACITY, 0.5f))
                .build();
        assertEquals(1f, s.resolve(0)[Styles.OPACITY.id()]);
        assertEquals(0.8f, s.resolve(State.HOVERED)[Styles.OPACITY.id()]);
        assertEquals(0.5f, s.resolve(State.HOVERED | State.PRESSED)[Styles.OPACITY.id()]);
        assertSame(s.resolve(State.HOVERED), s.resolve(State.HOVERED));
    }

    @Test
    void inheritedTextColorFlowsFromParentAndInlineOverridesTheme() {
        Theme theme = new Theme().put("panel", Style.builder().set(Styles.TEXT_COLOR, 0xFF112233).build());
        Label label = new Label("x");
        Panel panel = new Panel().add(label);
        new UiRoot(panel, theme);
        assertEquals(0xFF112233, label.computedStyle().get(Styles.TEXT_COLOR));
        panel.setStyle(Style.builder().set(Styles.TEXT_COLOR, Color.WHITE).build());
        assertEquals(Color.WHITE, label.computedStyle().get(Styles.TEXT_COLOR));
    }

    @Test
    void transitionInterpolatesOverTime() {
        Panel p = Panel.box(FlexLayout.column()).style(Style.builder()
                .set(Styles.OPACITY, 0f)
                .set(Styles.TRANSITION, new Transition(1f, Easing.LINEAR))
                .hovered(h -> h.set(Styles.OPACITY, 1f))
                .build());
        UiRoot root = new UiRoot(p, new Theme());
        p.setState(State.HOVERED, true);
        assertEquals(0f, p.computedStyle().get(Styles.OPACITY), 1e-4);
        root.animator().tick(0.25f);
        assertEquals(0.25f, p.computedStyle().get(Styles.OPACITY), 1e-4);
        root.animator().tick(1f);
        assertEquals(1f, p.computedStyle().get(Styles.OPACITY), 1e-4);
        assertEquals(true, root.animator().isIdle());
    }
}
