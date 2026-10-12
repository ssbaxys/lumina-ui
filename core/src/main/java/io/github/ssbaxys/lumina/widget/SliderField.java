package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.reactive.Signal;

/**
 * Строка настройки: подпись, ползунок и числовое поле на одном {@link Signal}
 * ("Толщина рамки [====|---] [1.33]"). Ползунок двигает число, ввод числа двигает ползунок.
 */
public class SliderField extends Widget<SliderField> {
    private final Label label;
    private final Slider slider;
    private final NumberField field;

    public SliderField(String label, Signal<Float> value, float min, float max) {
        super("slider-field");
        setLayout(FlexLayout.row().gap(8).align(Align.CENTER));
        layoutParams().width = Dim.FILL;
        this.label = new Label(label == null ? "" : label).grow(1f);
        this.slider = new Slider(value, min, max);
        this.field = new NumberField(value, min, max).step((max - min) / 100f);
        this.label.setVisible(label != null);
        addChild(this.label);
        addChild(slider);
        addChild(field);
    }

    /** Без подписи: только ползунок и поле. */
    public SliderField(Signal<Float> value, float min, float max) {
        this(null, value, min, max);
        slider.grow(1f);
    }

    /** Шаг ползунка и поля (стрелки, колесо, перетаскивание). */
    public SliderField step(float step) {
        slider.step(step);
        field.step(step);
        return this;
    }

    public SliderField decimals(int decimals) {
        field.decimals(decimals);
        return this;
    }

    public SliderField suffix(String suffix) {
        field.suffix(suffix);
        return this;
    }

    public Label label() {
        return label;
    }

    public Slider slider() {
        return slider;
    }

    public NumberField field() {
        return field;
    }
}
