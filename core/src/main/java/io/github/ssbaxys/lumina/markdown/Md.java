package io.github.ssbaxys.lumina.markdown;

import java.util.List;

/** Дерево разобранного Markdown: блоки и строчные элементы. Неизменяемые, кроме отметок задач. */
public final class Md {
    /** Флаги строчного форматирования. */
    public static final int BOLD = 1;
    public static final int ITALIC = 1 << 1;
    public static final int STRIKE = 1 << 2;
    public static final int UNDERLINE = 1 << 3;
    public static final int HIGHLIGHT = 1 << 4;

    private Md() {
    }

    public sealed interface Block permits Heading, Paragraph, CodeBlock, Quote, ListBlock, Table, Rule {
    }

    public record Heading(int level, List<Inline> content) implements Block {
    }

    public record Paragraph(List<Inline> content) implements Block {
    }

    public record CodeBlock(String language, String code) implements Block {
    }

    public record Quote(List<Block> blocks) implements Block {
    }

    /** Список; {@code start} — номер первого пункта нумерованного списка. */
    public record ListBlock(boolean ordered, int start, List<ListItem> items) implements Block {
    }

    public static final class ListItem {
        private final List<Block> blocks;
        private Boolean checked;

        public ListItem(List<Block> blocks, Boolean checked) {
            this.blocks = blocks;
            this.checked = checked;
        }

        public List<Block> blocks() {
            return blocks;
        }

        /** {@code null} — обычный пункт; иначе пункт-задача «[ ]» / «[x]». */
        public Boolean checked() {
            return checked;
        }

        public void setChecked(boolean checked) {
            if (this.checked != null) {
                this.checked = checked;
            }
        }
    }

    /** Выравнивание столбцов таблицы. */
    public enum Align {
        LEFT, CENTER, RIGHT
    }

    public record Table(List<List<Inline>> header, List<Align> aligns, List<List<List<Inline>>> rows) implements Block {
    }

    public record Rule() implements Block {
    }

    public sealed interface Inline permits Text, Code, Link, Image, LineBreak {
    }

    public record Text(String text, int flags) implements Inline {
    }

    public record Code(String code) implements Inline {
    }

    public record Link(String url, List<Inline> content) implements Inline {
    }

    /**
     * Картинка. Кроме обычных URL понимаются {@code item:minecraft:diamond} (иконка предмета) и
     * {@code texture:minecraft:textures/block/stone.png} (текстура).
     */
    public record Image(String alt, String url) implements Inline {
    }

    public record LineBreak() implements Inline {
    }
}
