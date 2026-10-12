package io.github.ssbaxys.lumina.render;

/** Непрозрачная ссылка на текстуру. Конкретный тип создаёт модуль загрузчика. */
public interface TextureRef {
    String id();
}
