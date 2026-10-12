package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.anim.AnimatedFloat;
import io.github.ssbaxys.lumina.anim.Animator;
import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.anim.Spring;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnimationTest {
    @Test
    void tweenReachesTargetAndLeavesAnimator() {
        Animator animator = new Animator();
        AnimatedFloat f = new AnimatedFloat(animator, 0f).tween(0.5f, Easing.LINEAR);
        f.animateTo(10f);
        animator.tick(0.25f);
        assertEquals(5f, f.get(), 1e-4);
        animator.tick(0.3f);
        assertEquals(10f, f.get());
        assertTrue(animator.isIdle());
    }

    @Test
    void retargetDoesNotDuplicateInAnimator() {
        Animator animator = new Animator();
        AnimatedFloat f = new AnimatedFloat(animator, 0f).tween(1f, Easing.LINEAR);
        f.animateTo(1f);
        f.snapTo(0f);
        f.animateTo(2f);
        animator.tick(0.01f);
        assertEquals(1, animator.activeCount());
    }

    @Test
    void springSettles() {
        Animator animator = new Animator();
        AnimatedFloat f = new AnimatedFloat(animator, 0f).spring(Spring.SNAPPY);
        f.animateTo(100f);
        for (int i = 0; i < 600 && !animator.isIdle(); i++) {
            animator.tick(1f / 60f);
        }
        assertTrue(animator.isIdle());
        assertEquals(100f, f.get());
    }

    @Test
    void cubicBezierMatchesEndpointsAndIsMonotonic() {
        Easing e = Easing.cubicBezier(0.25f, 0.1f, 0.25f, 1f);
        assertEquals(0f, e.apply(0f));
        assertEquals(1f, e.apply(1f));
        float prev = 0f;
        for (int i = 1; i <= 100; i++) {
            float v = e.apply(i / 100f);
            assertTrue(v >= prev - 1e-4f);
            prev = v;
        }
    }
}
