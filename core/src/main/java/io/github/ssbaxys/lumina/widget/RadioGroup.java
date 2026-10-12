package io.github.ssbaxys.lumina.widget;

import io.github.ssbaxys.lumina.event.KeyEvent;
import io.github.ssbaxys.lumina.event.UiEvent;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Align;
import io.github.ssbaxys.lumina.layout.Direction;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Группа радио-кнопок: выбрана ровно одна из {@code options}, выбор — в {@link Signal}. Колонкой
 * ({@link #vertical()}, по умолчанию) или строкой ({@link #horizontal()}); стрелки двигают выбор.
 *
 * <pre>{@code
 * RadioGroup.of(difficulty, Difficulty.values()).labels(d -> d.title()).horizontal()
 * }</pre>
 */
public class RadioGroup<T> extends Widget<RadioGroup<T>> {
    private final Signal<T> value;
    private final List<T> options;
    private final List<Checkbox> items = new ArrayList<>();
    private final List<Signal<Boolean>> selected = new ArrayList<>();
    private final FlexLayout flex = FlexLayout.column().gap(4f).align(Align.START);
    private Function<? super T, String> labeler = String::valueOf;
    private Subscription subscription;

    public RadioGroup(Signal<T> value, List<T> options) {
        super("radio-group");
        this.value = value;
        this.options = List.copyOf(options);
        setLayout(flex);
        for (T option : this.options) {
            Signal<Boolean> on = Signal.of(Objects.equals(value.peek(), option));
            Checkbox item = new Checkbox(labeler.apply(option), on)
                    .kind(Checkbox.Kind.RADIO)
                    .action(() -> select(option));
            selected.add(on);
            items.add(item);
            addChild(item);
        }
    }

    @SafeVarargs
    public static <T> RadioGroup<T> of(Signal<T> value, T... options) {
        return new RadioGroup<>(value, Arrays.asList(options));
    }

    public Signal<T> value() {
        return value;
    }

    public List<T> options() {
        return options;
    }

    /** Радио-кнопки группы в порядке вариантов. */
    public List<Checkbox> items() {
        return Collections.unmodifiableList(items);
    }

    /** Подписи вариантов (по умолчанию {@code String.valueOf}). */
    public RadioGroup<T> labels(Function<? super T, String> labeler) {
        this.labeler = labeler;
        for (int i = 0; i < items.size(); i++) {
            items.get(i).label(labeler.apply(options.get(i)));
        }
        return this;
    }

    /** Вид кнопок (по умолчанию {@link Checkbox.Kind#RADIO}). */
    public RadioGroup<T> kind(Checkbox.Kind kind) {
        items.forEach(i -> i.kind(kind));
        return this;
    }

    public RadioGroup<T> vertical() {
        flex.direction(Direction.COLUMN).align(Align.START);
        invalidateLayout();
        return this;
    }

    public RadioGroup<T> horizontal() {
        flex.direction(Direction.ROW).align(Align.CENTER);
        invalidateLayout();
        return this;
    }

    public RadioGroup<T> gap(float gap) {
        flex.gap(gap);
        invalidateLayout();
        return this;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        items.forEach(i -> i.setEnabled(enabled));
    }

    public void select(T option) {
        if (isEnabled() && options.contains(option)) {
            value.set(option);
        }
    }

    private void refresh(T current) {
        for (int i = 0; i < options.size(); i++) {
            selected.get(i).set(Objects.equals(current, options.get(i)));
        }
    }

    @Override
    protected void onAttach() {
        refresh(value.peek());
        subscription = value.subscribe(this::refresh);
    }

    @Override
    protected void onDetach() {
        if (subscription != null) {
            subscription.close();
            subscription = null;
        }
    }

    /** Стрелки: соседний вариант выбирается и получает фокус. */
    @Override
    protected void handleEvent(UiEvent event) {
        if (event.type() != KeyEvent.PRESS || options.isEmpty()) {
            return;
        }
        int key = ((KeyEvent) event).key();
        int step = key == Keys.DOWN || key == Keys.RIGHT ? 1 : key == Keys.UP || key == Keys.LEFT ? -1 : 0;
        if (step == 0) {
            return;
        }
        int at = options.indexOf(value.peek());
        int next = Math.floorMod((at < 0 ? (step > 0 ? -1 : 0) : at) + step, options.size());
        select(options.get(next));
        items.get(next).requestFocus();
        event.consume();
    }
}
