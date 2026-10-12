package io.github.ssbaxys.lumina.anim;

/** Параметры физической пружины. Пружины естественно прерываются и не «дёргаются» при смене цели. */
public record Spring(float stiffness, float damping, float mass) {
    public static final Spring GENTLE = new Spring(120f, 20f, 1f);
    public static final Spring SNAPPY = new Spring(400f, 40f, 1f);
    public static final Spring BOUNCY = new Spring(300f, 14f, 1f);
}
