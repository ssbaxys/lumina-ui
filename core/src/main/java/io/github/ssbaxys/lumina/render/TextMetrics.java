package io.github.ssbaxys.lumina.render;

/** Измерение текста. Нужна и при раскладке (до рендера), поэтому отделена от {@link Canvas}. */
public interface TextMetrics {
    float width(String text);

    float lineHeight();
}
