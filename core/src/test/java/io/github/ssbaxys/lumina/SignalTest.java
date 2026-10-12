package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.reactive.Computed;
import io.github.ssbaxys.lumina.reactive.Signal;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SignalTest {
    @Test
    void computedTracksDependenciesAutomatically() {
        Signal<Integer> a = Signal.of(1);
        Signal<Integer> b = Signal.of(2);
        Computed<Integer> sum = new Computed<>(() -> a.get() + b.get());
        List<Integer> seen = new ArrayList<>();
        sum.subscribe(seen::add);
        a.set(10);
        b.set(5);
        b.set(5);
        assertEquals(15, sum.get());
        assertEquals(List.of(12, 15), seen);
        sum.dispose();
        a.set(0);
        assertEquals(15, sum.get());
    }
}
