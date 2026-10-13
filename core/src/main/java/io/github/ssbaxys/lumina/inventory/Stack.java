package io.github.ssbaxys.lumina.inventory;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Стопка предметов для интерфейса — независимое от версии игры значение: id предмета, количество и то,
 * что видно в слоте и подсказке (своё название, строки описания, перелив зачарования, прочность).
 * Неизменяема: {@code with}-методы возвращают копию. Пустая стопка — {@link #EMPTY}.
 *
 * <pre>{@code
 * Stack sword = Stack.of("minecraft:diamond_sword")
 *         .withName("Экскалибур").withLore("Острый", "Очень острый")
 *         .withGlint(true).withDurability(120, 1561);
 * Stack coal = Stack.of("minecraft:coal", 48);
 * }</pre>
 *
 * Стопки складываются ({@link #canStackWith}), если совпадает всё, кроме количества. Предел стопки —
 * {@link #maxStackSize()}: 64 по умолчанию и 1 у предметов с прочностью.
 */
public final class Stack {
    public static final Stack EMPTY = new Stack("", 0, null, List.of(), false, 0, 0, 0, null);

    private final String id;
    private final int count;
    private final String name;
    private final List<String> lore;
    private final boolean glint;
    private final int damage;
    private final int maxDamage;
    private final int maxStack;
    private final Object platformStack;

    private Stack(String id, int count, String name, List<String> lore, boolean glint,
                  int damage, int maxDamage, int maxStack, Object platformStack) {
        this.id = id;
        this.count = count;
        this.name = name;
        this.lore = lore;
        this.glint = glint;
        this.damage = damage;
        this.maxDamage = maxDamage;
        this.maxStack = maxStack;
        this.platformStack = platformStack;
    }

    /** Один предмет. */
    public static Stack of(String itemId) {
        return of(itemId, 1);
    }

    /** {@code count} предметов; пустой id или {@code count <= 0} — {@link #EMPTY}. */
    public static Stack of(String itemId, int count) {
        if (itemId == null || itemId.isBlank() || count <= 0) {
            return EMPTY;
        }
        String id = itemId.strip();
        if (id.indexOf(':') < 0) {
            id = "minecraft:" + id;
        }
        return new Stack(id, count, null, List.of(), false, 0, 0, 0, null);
    }

    /**
     * Из строки {@code "minecraft:diamond"}, {@code "diamond*12"} или {@code "minecraft:stone x64"}
     * (разметка и короткие списки). Пусто или {@code null} — {@link #EMPTY}.
     */
    public static Stack parse(String text) {
        if (text == null || text.isBlank()) {
            return EMPTY;
        }
        String s = text.strip();
        int count = 1;
        int sep = Math.max(s.lastIndexOf('*'), s.lastIndexOf(" x"));
        if (sep > 0) {
            String tail = s.substring(s.charAt(sep) == '*' ? sep + 1 : sep + 2).strip();
            try {
                count = Integer.parseInt(tail);
                s = s.substring(0, sep).strip();
            } catch (NumberFormatException ignored) {
                // не число — значит, это часть id
            }
        }
        return of(s, count);
    }

    // ================================================================ свойства

    public boolean isEmpty() {
        return count <= 0;
    }

    /** Id предмета ({@code "minecraft:diamond"}); у пустой стопки — пустая строка. */
    public String itemId() {
        return id;
    }

    public int count() {
        return count;
    }

    /** Своё название или {@code null}. */
    public String customName() {
        return name;
    }

    /** Название для подсказки: своё или выведенное из id ("minecraft:diamond_sword" → "Diamond Sword"). */
    public String displayName() {
        return name != null ? name : prettyName(id);
    }

    /** Строки описания под названием. */
    public List<String> lore() {
        return lore;
    }

    /** Перелив зачарования. */
    public boolean hasGlint() {
        return glint;
    }

    public int damage() {
        return damage;
    }

    public int maxDamage() {
        return maxDamage;
    }

    /** «Родная» стопка платформы ({@link #withPlatformStack}) или {@code null}. */
    public Object platformStack() {
        return platformStack;
    }

    /** Есть ли у предмета прочность. */
    public boolean isDamageable() {
        return maxDamage > 0;
    }

    /** Повреждён ли предмет (тогда в слоте видна полоска прочности). */
    public boolean isDamaged() {
        return maxDamage > 0 && damage > 0;
    }

    /** Остаток прочности 0..1 (1 — целый или без прочности). */
    public float durability() {
        return maxDamage <= 0 ? 1f : Math.max(0f, Math.min(1f, 1f - (float) damage / maxDamage));
    }

    /** Предел стопки: заданный {@link #withMaxStackSize}, иначе 1 у предметов с прочностью и 64 у остальных. */
    public int maxStackSize() {
        if (maxStack > 0) {
            return maxStack;
        }
        return maxDamage > 0 ? 1 : 64;
    }

    // ================================================================ изменения

    /** То же с другим количеством; {@code count <= 0} — {@link #EMPTY}. */
    public Stack withCount(int count) {
        if (count <= 0) {
            return EMPTY;
        }
        if (count == this.count || isEmpty()) {
            return this;
        }
        return new Stack(id, count, name, lore, glint, damage, maxDamage, maxStack, platformStack);
    }

    /** Больше на {@code delta} (меньше при отрицательном). */
    public Stack grow(int delta) {
        return withCount(count + delta);
    }

    public Stack withName(String name) {
        if (isEmpty()) {
            return this;
        }
        String n = name == null || name.isEmpty() ? null : name;
        return new Stack(id, count, n, lore, glint, damage, maxDamage, maxStack, null);
    }

    public Stack withLore(String... lines) {
        return withLore(lines == null ? List.of() : Arrays.asList(lines));
    }

    public Stack withLore(List<String> lines) {
        if (isEmpty()) {
            return this;
        }
        return new Stack(id, count, name, lines == null ? List.of() : List.copyOf(lines), glint, damage, maxDamage, maxStack,
                null);
    }

    public Stack withGlint(boolean glint) {
        if (isEmpty() || glint == this.glint) {
            return this;
        }
        return new Stack(id, count, name, lore, glint, damage, maxDamage, maxStack, null);
    }

    /** Прочность: {@code damage} из {@code maxDamage} израсходовано. */
    public Stack withDurability(int damage, int maxDamage) {
        if (isEmpty()) {
            return this;
        }
        int max = Math.max(0, maxDamage);
        return new Stack(id, count, name, lore, glint, Math.max(0, Math.min(damage, max)), max, maxStack, null);
    }

    /** Свой предел стопки (1..99; 16 у жемчуга эндера и т. п.). */
    public Stack withMaxStackSize(int maxStackSize) {
        if (isEmpty()) {
            return this;
        }
        return new Stack(id, count, name, lore, glint, damage, maxDamage, Math.max(1, Math.min(99, maxStackSize)), null);
    }

    /**
     * Та же стопка с «родной» стопкой платформы ({@code ItemStack} игры), из которой она получена. По ней бэкенд
     * рисует предмет точно (цвет зелья, голова игрока, крашеная броня), а не только по id. В сравнении стопок не
     * участвует; переживает {@link #withCount}, остальные {@code with}-методы её сбрасывают (вид мог измениться).
     */
    public Stack withPlatformStack(Object platformStack) {
        if (isEmpty() || platformStack == this.platformStack) {
            return this;
        }
        return new Stack(id, count, name, lore, glint, damage, maxDamage, maxStack, platformStack);
    }

    /** Складываются ли стопки: совпадает всё, кроме количества. Пустые ни с чем не складываются. */
    public boolean canStackWith(Stack other) {
        return other != null && !isEmpty() && !other.isEmpty()
                && id.equals(other.id)
                && Objects.equals(name, other.name)
                && lore.equals(other.lore)
                && glint == other.glint
                && damage == other.damage
                && maxDamage == other.maxDamage
                && maxStack == other.maxStack;
    }

    /** "minecraft:diamond_sword" → "Diamond Sword". */
    static String prettyName(String id) {
        if (id == null || id.isEmpty()) {
            return "";
        }
        String path = id.substring(id.indexOf(':') + 1);
        StringBuilder sb = new StringBuilder(path.length());
        boolean upper = true;
        for (int i = 0; i < path.length(); i++) {
            char c = path.charAt(i);
            if (c == '_' || c == '/' || c == '.') {
                sb.append(' ');
                upper = true;
            } else {
                sb.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Stack s && (s == this || (s.count == count && (count == 0 || canStackWith(s))));
    }

    @Override
    public int hashCode() {
        return isEmpty() ? 0 : Objects.hash(id, count, name, lore, glint, damage, maxDamage, maxStack);
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "Stack[empty]";
        }
        return "Stack[" + id + " x" + count + (name != null ? " \"" + name + "\"" : "")
                + (glint ? " glint" : "") + (maxDamage > 0 ? " " + damage + "/" + maxDamage : "") + "]";
    }

    /** Подпись количества в слоте: пусто для одного предмета, иначе число ("1k" для тысяч). */
    static String countLabel(int count) {
        if (count <= 1) {
            return "";
        }
        if (count < 1000) {
            return Integer.toString(count);
        }
        return String.format(Locale.ROOT, count < 10_000 ? "%.1fk" : "%.0fk", count / 1000f);
    }
}
