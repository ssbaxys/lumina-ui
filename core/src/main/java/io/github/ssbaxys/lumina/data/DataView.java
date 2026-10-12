package io.github.ssbaxys.lumina.data;

import io.github.ssbaxys.lumina.reactive.ReadableSignal;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.reactive.Subscription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Представление набора данных: поиск по тексту, фильтры, сортировка и разбиение на страницы.
 * Результат — сигналы со списками, которые читают {@code VirtualList}, {@code Table} и {@code Paginator}.
 *
 * <pre>{@code
 * DataView<Item> view = new DataView<>(items)
 *         .searchBy(Item::name)                       // поиск: query().set("алмаз")
 *         .sort(Comparator.comparing(Item::name))
 *         .pageSize(50);                              // страницы: page().set(2)
 * view.setFilter("блоки", Item::isBlock);             // именованный фильтр, null — убрать
 * new VirtualList<Item>(painter).bind(view.visible());
 * new Paginator(view.page(), view.pageCount());
 * }</pre>
 *
 * <p>Пересчёт идёт сразу при изменении условий (без отложенных кадров) и стоит один проход по данным
 * плюс сортировка совпавших. Строки для поиска приводятся к нижнему регистру один раз при смене
 * данных, поэтому набор текста в поле поиска по десяткам тысяч записей не тормозит.</p>
 */
public final class DataView<T> {
    private List<T> source;
    private Function<? super T, String> searchKey;
    private String[] keys;
    private final Map<String, Predicate<? super T>> filters = new LinkedHashMap<>();
    private Comparator<? super T> order;
    private boolean descending;
    private int pageSize;

    private final Signal<String> query = Signal.of("");
    private final Signal<Integer> page = Signal.of(0);
    private final Signal<List<T>> filtered = Signal.of(List.of());
    private final Signal<List<T>> visible = Signal.of(List.of());
    private final Signal<Integer> total = Signal.of(0);
    private final Signal<Integer> pageCount = Signal.of(1);
    private final List<Subscription> subscriptions = new ArrayList<>();
    private Subscription sourceSubscription;
    private boolean batching;
    private boolean dirty;

    public DataView(List<T> source) {
        this.source = source == null ? List.of() : source;
        subscriptions.add(query.subscribe(q -> {
            // Новый запрос — с первой страницы.
            batch(() -> {
                page.set(0);
                refresh();
            });
        }));
        subscriptions.add(page.subscribe(p -> slice()));
        refresh();
    }

    /** Представление над сигналом со списком: пересчитывается при каждом его изменении. */
    public static <T> DataView<T> of(ReadableSignal<List<T>> source) {
        DataView<T> view = new DataView<>(source.get());
        view.sourceSubscription = source.subscribe(view::source);
        return view;
    }

    // ================================================================ условия

    /** Заменить данные (условия поиска, фильтры и сортировка сохраняются). */
    public DataView<T> source(List<T> items) {
        this.source = items == null ? List.of() : items;
        this.keys = null;
        refresh();
        return this;
    }

    public List<T> source() {
        return source;
    }

    /** По какому тексту искать. Без этого поиск сравнивает {@code toString()}. */
    public DataView<T> searchBy(Function<? super T, String> key) {
        this.searchKey = key;
        this.keys = null;
        refresh();
        return this;
    }

    /** Строка поиска: слова через пробел, каждое должно встретиться (без учёта регистра). */
    public Signal<String> query() {
        return query;
    }

    /** Именованный фильтр; {@code null} снимает его. Повторный вызов с тем же именем заменяет. */
    public DataView<T> setFilter(String name, Predicate<? super T> filter) {
        Predicate<? super T> old = filter == null ? filters.remove(name) : filters.put(name, filter);
        if (old != filter) {
            batch(() -> {
                page.set(0);
                refresh();
            });
        }
        return this;
    }

    /** Фильтр, который действует, пока сигнал {@code active} равен {@code true}. */
    public DataView<T> filterWhen(String name, ReadableSignal<Boolean> active, Predicate<? super T> filter) {
        Runnable apply = () -> setFilter(name, Boolean.TRUE.equals(active.get()) ? filter : null);
        apply.run();
        subscriptions.add(active.subscribe(v -> apply.run()));
        return this;
    }

    public boolean hasFilter(String name) {
        return filters.containsKey(name);
    }

    public DataView<T> clearFilters() {
        if (!filters.isEmpty()) {
            filters.clear();
            batch(() -> {
                page.set(0);
                refresh();
            });
        }
        return this;
    }

    /** Порядок; {@code null} — как в исходных данных. */
    public DataView<T> sort(Comparator<? super T> order) {
        return sort(order, false);
    }

    public DataView<T> sort(Comparator<? super T> order, boolean descending) {
        this.order = order;
        this.descending = descending;
        refresh();
        return this;
    }

    public Comparator<? super T> order() {
        return order;
    }

    public boolean isDescending() {
        return descending;
    }

    /** Размер страницы; 0 — без страниц (всё в {@link #visible()}). */
    public DataView<T> pageSize(int size) {
        this.pageSize = Math.max(0, size);
        batch(() -> {
            page.set(0);
            refresh();
        });
        return this;
    }

    public int pageSize() {
        return pageSize;
    }

    // ================================================================ результат

    /** Номер страницы с нуля (можно менять: {@code page().set(3)}; выходящий за границы приводится к ним). */
    public Signal<Integer> page() {
        return page;
    }

    /** Сколько страниц (не меньше одной). */
    public ReadableSignal<Integer> pageCount() {
        return pageCount;
    }

    /** Все записи, прошедшие поиск и фильтры, в нужном порядке. */
    public ReadableSignal<List<T>> filtered() {
        return filtered;
    }

    /** Записи текущей страницы (или все совпавшие, если страниц нет). */
    public ReadableSignal<List<T>> visible() {
        return visible;
    }

    /** Сколько записей совпало. */
    public ReadableSignal<Integer> total() {
        return total;
    }

    public void nextPage() {
        page.set(Math.min(pageCount.peek() - 1, current() + 1));
    }

    public void previousPage() {
        page.set(Math.max(0, current() - 1));
    }

    /** Отписаться от сигналов-источников ({@link #of}, {@link #filterWhen}). */
    public void dispose() {
        subscriptions.forEach(Subscription::close);
        subscriptions.clear();
        if (sourceSubscription != null) {
            sourceSubscription.close();
            sourceSubscription = null;
        }
    }

    // ================================================================ пересчёт

    private int current() {
        Integer p = page.peek();
        return p == null ? 0 : p;
    }

    /** Несколько изменений — один пересчёт в конце. */
    private void batch(Runnable changes) {
        if (batching) {
            changes.run();
            return;
        }
        batching = true;
        dirty = false;
        try {
            changes.run();
        } finally {
            batching = false;
        }
        if (dirty) {
            refresh();
        }
    }

    private String[] keys() {
        if (keys == null || keys.length != source.size()) {
            String[] k = new String[source.size()];
            for (int i = 0; i < k.length; i++) {
                T item = source.get(i);
                String s = item == null ? "" : searchKey != null ? searchKey.apply(item) : item.toString();
                k[i] = s == null ? "" : s.toLowerCase(Locale.ROOT);
            }
            keys = k;
        }
        return keys;
    }

    private void refresh() {
        if (batching) {
            dirty = true;
            return;
        }
        String q = query.peek();
        String[] words = q == null || q.isBlank() ? new String[0] : q.toLowerCase(Locale.ROOT).trim().split("\\s+");
        String[] k = words.length > 0 ? keys() : null;
        List<T> out = new ArrayList<>(Math.min(source.size(), 1024));
        outer:
        for (int i = 0, n = source.size(); i < n; i++) {
            if (k != null) {
                for (String w : words) {
                    if (!k[i].contains(w)) {
                        continue outer;
                    }
                }
            }
            T item = source.get(i);
            for (Predicate<? super T> f : filters.values()) {
                if (!f.test(item)) {
                    continue outer;
                }
            }
            out.add(item);
        }
        if (order != null) {
            Comparator<? super T> c = order;
            out.sort(descending ? Collections.reverseOrder(c) : c);
        }
        List<T> result = Collections.unmodifiableList(out);
        filtered.set(result);
        total.set(result.size());
        pageCount.set(pageSize <= 0 ? 1 : Math.max(1, (result.size() + pageSize - 1) / pageSize));
        slice();
    }

    private void slice() {
        if (batching) {
            dirty = true;
            return;
        }
        List<T> all = filtered.peek();
        int pages = pageCount.peek();
        int p = current();
        if (p < 0 || p >= pages) {
            // Приводим номер к границам: подписка на page вызовет slice заново.
            page.set(Math.max(0, Math.min(pages - 1, p)));
            return;
        }
        if (pageSize <= 0) {
            visible.set(all);
        } else {
            int from = Math.min(all.size(), p * pageSize);
            int to = Math.min(all.size(), from + pageSize);
            visible.set(all.subList(from, to));
        }
    }
}
