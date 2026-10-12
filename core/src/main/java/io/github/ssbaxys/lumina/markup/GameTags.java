package io.github.ssbaxys.lumina.markup;

import io.github.ssbaxys.lumina.inventory.Slot;
import io.github.ssbaxys.lumina.inventory.Stack;
import io.github.ssbaxys.lumina.preview.EntityView;
import io.github.ssbaxys.lumina.preview.ItemView;
import io.github.ssbaxys.lumina.preview.SpinView;

import java.util.Locale;

/**
 * Теги разметки для предметов и сущностей: {@code <slot>}, {@code <entity>}, {@code <player>},
 * {@code <item-view>}. Регистрируются вместе со встроенными тегами {@link Markup}.
 *
 * <pre>{@code
 * <slot item="minecraft:diamond_sword" count="1" glint durability="120/1561" name="Экскалибур" lore="Острый|Древний"/>
 * <entity type="minecraft:zombie" auto-rotate mainhand="minecraft:iron_sword" baby/>
 * <player follow-mouse width="80" height="110"/>
 * <item-view item="minecraft:grass_block" auto-rotate="45"/>
 * }</pre>
 */
final class GameTags {
    private static final String[] EQUIPMENT = {"mainhand", "offhand", "head", "chest", "legs", "feet"};

    private GameTags() {
    }

    static void register() {
        Markup.tag("slot", (n, c) -> {
            Stack stack = stack(n);
            Slot slot = new Slot(stack);
            if ("modern".equals(n.attr("look"))) {
                slot.look(Slot.Look.MODERN);
            }
            if (n.attr("size") != null) {
                float s = n.floatAttr("size", Slot.SIZE);
                slot.size(s, s);
            }
            if (n.attr("placeholder") != null) {
                slot.placeholder(n.attr("placeholder"));
            }
            if ("false".equals(n.attr("takeable"))) {
                slot.takeable(false);
            }
            if ("false".equals(n.attr("droppable"))) {
                slot.dropTarget(false);
            }
            return slot;
        });
        Markup.tag("entity", (n, c) -> entity(new EntityView(n.attr("type")), n));
        Markup.tag("player", (n, c) -> entity(EntityView.player(), n));
        Markup.tag("item-view", (n, c) -> {
            Stack stack = stack(n);
            return spin(new ItemView(stack.isEmpty() ? Stack.of(n.attr("id")) : stack), n);
        });
    }

    /** Стопка из атрибутов {@code item count name lore glint durability}. */
    static Stack stack(Markup.Node n) {
        String id = n.attr("item");
        int count = 1;
        if (n.attr("count") != null) {
            count = Math.max(0, Math.round(CssValues.number(n.attr("count"))));
        }
        Stack s = Stack.parse(id);
        if (s.isEmpty()) {
            return Stack.EMPTY;
        }
        if (n.attr("count") != null) {
            s = s.withCount(count);
        }
        if (n.attr("name") != null) {
            s = s.withName(n.attr("name"));
        }
        if (n.attr("lore") != null) {
            s = s.withLore(n.attr("lore").split("\\|"));
        }
        String glint = n.attr("glint");
        if (glint != null && !"false".equals(glint)) {
            s = s.withGlint(true);
        }
        String dur = n.attr("durability");
        if (dur != null) {
            int slash = dur.indexOf('/');
            if (slash > 0) {
                int left = Math.round(CssValues.number(dur.substring(0, slash).strip()));
                int max = Math.round(CssValues.number(dur.substring(slash + 1).strip()));
                s = s.withDurability(max - left, max);
            }
        }
        if (n.attr("max-stack") != null) {
            s = s.withMaxStackSize(Math.round(CssValues.number(n.attr("max-stack"))));
        }
        return s;
    }

    private static EntityView entity(EntityView view, Markup.Node n) {
        String baby = n.attr("baby");
        if (baby != null && !"false".equals(baby)) {
            view.baby(true);
        }
        for (String slot : EQUIPMENT) {
            if (n.attr(slot) != null) {
                view.equip(slot, n.attr(slot));
            }
        }
        String follow = n.attr("follow-mouse");
        if (follow != null && !"false".equals(follow)) {
            view.followMouse(true);
        }
        return spin(view, n);
    }

    private static <S extends SpinView<S>> S spin(S view, Markup.Node n) {
        if (n.attr("yaw") != null) {
            view.yaw(degrees(n.attr("yaw")));
        }
        if (n.attr("pitch") != null) {
            view.pitch(degrees(n.attr("pitch")));
        }
        String spin = n.attr("auto-rotate");
        if (spin != null && !"false".equals(spin)) {
            view.autoRotate(spin.isEmpty() || "true".equals(spin) ? SpinView.DEFAULT_SPIN : degrees(spin));
        }
        if (n.attr("zoom") != null) {
            view.zoom(CssValues.number(n.attr("zoom")));
        }
        if ("false".equals(n.attr("interactive"))) {
            view.interactive(false);
        }
        if ("false".equals(n.attr("zoomable"))) {
            view.zoomable(false);
        }
        if (n.attr("size") != null) {
            float s = n.floatAttr("size", 64f);
            view.size(s, s);
        }
        return view;
    }

    private static float degrees(String v) {
        String t = v.strip().toLowerCase(Locale.ROOT);
        return Float.parseFloat(t.endsWith("deg") ? t.substring(0, t.length() - 3).strip() : t);
    }
}
