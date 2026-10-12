package io.github.ssbaxys.lumina;

import io.github.ssbaxys.lumina.data.DataView;
import io.github.ssbaxys.lumina.input.Keys;
import io.github.ssbaxys.lumina.layout.Dim;
import io.github.ssbaxys.lumina.layout.FlexLayout;
import io.github.ssbaxys.lumina.node.UiElement;
import io.github.ssbaxys.lumina.node.UiRoot;
import io.github.ssbaxys.lumina.reactive.Signal;
import io.github.ssbaxys.lumina.style.ModernTheme;
import io.github.ssbaxys.lumina.widget.Label;
import io.github.ssbaxys.lumina.widget.Paginator;
import io.github.ssbaxys.lumina.widget.Panel;
import io.github.ssbaxys.lumina.widget.ScrollPanel;
import io.github.ssbaxys.lumina.widget.Table;
import io.github.ssbaxys.lumina.widget.VirtualList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Представление данных, виртуальный список, переключатель страниц и отсечение строк таблицы. */
class DataViewListTest {
    private record Item(int id, String name, boolean block) {
    }

    @BeforeAll
    static void init() {
        TestSupport.init();
    }

    private static List<Item> items(int n) {
        List<Item> out = new ArrayList<>(n);
        String[] names = {"Алмаз", "Камень", "Золото", "Железо", "Изумруд"};
        for (int i = 0; i < n; i++) {
            out.add(new Item(i, names[i % names.length] + " " + i, i % 2 == 0));
        }
        return out;
    }

    private static UiRoot mount(UiElement... e) {
        UiRoot root = new UiRoot(Panel.box(FlexLayout.column()).fill().add(e), ModernTheme.dark());
        root.resize(400, 300);
        root.tick(0f);
        return root;
    }

    private static List<String> paint(UiRoot root) {
        TestSupport.RecordingCanvas canvas = new TestSupport.RecordingCanvas();
        canvas.begin(400, 300);
        root.render(canvas, 0f);
        canvas.end();
        return canvas.ops;
    }

    private static void click(UiRoot root, float x, float y) {
        root.pointerMove(x, y);
        root.pointerDown(x, y, Keys.MOUSE_LEFT);
        root.pointerUp(x, y, Keys.MOUSE_LEFT);
    }

    // ================================================================ DataView

    @Test
    void searchFiltersSortAndPages() {
        DataView<Item> view = new DataView<>(items(1000)).searchBy(Item::name).pageSize(50);
        assertEquals(1000, view.total().get());
        assertEquals(20, view.pageCount().get());
        assertEquals(50, view.visible().get().size());

        view.page().set(3);
        assertEquals(150, view.visible().get().get(0).id());

        // Поиск: все слова, без учёта регистра; новый запрос — с первой страницы.
        view.query().set("алмаз 99");
        assertEquals(0, view.page().get());
        assertTrue(view.filtered().get().stream().allMatch(i -> i.name().startsWith("Алмаз") && i.name().contains("99")));
        assertEquals(List.of(990, 995), view.filtered().get().stream().map(Item::id).filter(id -> id >= 990).toList());

        view.query().set("");
        view.setFilter("блоки", Item::block);
        assertEquals(500, view.total().get());
        view.sort(Comparator.comparingInt(Item::id), true);
        assertEquals(998, view.visible().get().get(0).id());

        view.setFilter("блоки", null);
        view.sort(null);
        assertEquals(1000, view.total().get());
        assertEquals(0, view.visible().get().get(0).id());
    }

    @Test
    void pageIsClampedAndFiltersFollowSignals() {
        DataView<Item> view = new DataView<>(items(95)).pageSize(10);
        view.page().set(42);
        assertEquals(9, view.page().get(), "page clamped to the last one");
        assertEquals(5, view.visible().get().size());

        Signal<Boolean> onlyBlocks = Signal.of(false);
        view.filterWhen("блоки", onlyBlocks, Item::block);
        assertEquals(95, view.total().get());
        onlyBlocks.set(true);
        assertEquals(48, view.total().get());
        assertEquals(5, view.pageCount().get());
        onlyBlocks.set(false);
        assertEquals(95, view.total().get());

        view.nextPage();
        view.nextPage();
        assertEquals(2, view.page().get());
        view.previousPage();
        assertEquals(1, view.page().get());

        Signal<List<Item>> source = Signal.of(items(3));
        DataView<Item> live = DataView.of(source);
        assertEquals(3, live.total().get());
        source.set(items(7));
        assertEquals(7, live.total().get());
        live.dispose();
    }

    // ================================================================ VirtualList

    @Test
    void virtualListPaintsOnlyVisibleRows() {
        List<Item> data = items(20_000);
        int[] painted = {0};
        VirtualList<Item> list = new VirtualList<Item>(16, (c, item, i, x, y, w, h, row) -> {
            painted[0]++;
            c.drawText(item.name(), x + 4, y + 3, row.textColor(), false);
        }).items(data).height(Dim.px(160)).width(Dim.px(200));
        UiRoot root = mount(list);
        paint(root);
        // 160 - отступы ≈ 9-10 строк, а не 20 000.
        assertTrue(painted[0] > 0 && painted[0] <= 12, "painted rows: " + painted[0]);
        assertEquals(0, list.firstVisible());

        // Колесо: плавная прокрутка вниз.
        float x = list.screenX() + 50;
        float y = list.screenY() + 50;
        root.scroll(x, y, 0, -10);
        root.tick(1f);
        assertTrue(list.scrollOffset() > 0f);
        assertTrue(list.firstVisible() > 0);

        // Клавиши: End — последняя строка, она становится видимой.
        root.setFocus(list);
        root.keyDown(Keys.END, 0, 0);
        root.tick(1f);
        assertEquals(19_999, list.selectedIndex());
        assertEquals(data.get(19_999), list.selection().get());
        assertEquals(19_999, list.lastVisible());
        root.keyDown(Keys.UP, 0, 0);
        assertEquals(19_998, list.selectedIndex());
        root.keyDown(Keys.HOME, 0, 0);
        root.tick(1f);
        assertEquals(0, list.selectedIndex());
        assertEquals(0f, list.scrollOffset(), 0.05f);
        root.keyDown(Keys.PAGE_DOWN, 0, 0);
        assertTrue(list.selectedIndex() >= 5, "page down moves by a screen: " + list.selectedIndex());
    }

    @Test
    void virtualListClickSelectsAndDoubleClickActivates() {
        List<Item> activated = new ArrayList<>();
        VirtualList<Item> list = VirtualList.<Item>ofText(16, Item::name).items(items(100))
                .onActivate(activated::add).height(Dim.px(120)).width(Dim.px(200));
        UiRoot root = mount(list);
        float x = list.screenX() + 40;
        float rowY = list.screenY() + list.padding().top() + 16 * 2 + 8;
        click(root, x, rowY);
        assertEquals(2, list.selectedIndex());
        click(root, x, rowY);
        assertEquals(List.of(list.items().get(2)), activated);

        // Выбор извне находит строку; смена данных без выбранной записи снимает выбор.
        list.selection().set(list.items().get(40));
        assertEquals(40, list.selectedIndex());
        list.items(items(10));
        assertNull(list.selection().get());
        assertEquals(-1, list.selectedIndex());
    }

    @Test
    void virtualListRecyclesWidgetRows() {
        int[] created = {0};
        VirtualList<Item> list = new VirtualList<Item>(16, null)
                .<Label>rows(() -> {
                    created[0]++;
                    return new Label("");
                }, (label, item, i) -> label.text(item.name()))
                .items(items(5_000)).height(Dim.px(100)).width(Dim.px(200));
        UiRoot root = mount(list);
        paint(root);
        int pool = created[0];
        assertTrue(pool > 0 && pool <= 8, "row widgets: " + pool);
        assertEquals("Алмаз 0", ((Label) list.children().get(0)).text());

        list.scrollTo(16 * 1000, false);
        root.tick(0f);
        paint(root);
        assertEquals(pool, created[0], "rows are reused, not created");
        boolean shows1000 = list.children().stream().anyMatch(e -> e.isVisible() && ((Label) e).text().equals("Камень 1001"));
        assertTrue(shows1000, "rebound to the scrolled rows");
    }

    @Test
    void virtualListFollowsDataView() {
        DataView<Item> view = new DataView<>(items(10_000)).searchBy(Item::name);
        VirtualList<Item> list = VirtualList.<Item>ofText(16, Item::name).bind(view.visible()).height(Dim.px(100));
        mount(list);
        assertEquals(10_000, list.items().size());
        view.query().set("изумруд");
        assertEquals(2_000, list.items().size());
        list.selection().set(list.items().get(3));
        view.query().set("изумруд 1");
        // Выбранной записи нет среди найденных — выбор снят.
        assertTrue(list.selection().get() == null || list.items().contains(list.selection().get()));
    }

    // ================================================================ Paginator

    @Test
    void paginatorShowsEdgesNeighboursAndEllipsis() {
        Signal<Integer> page = Signal.of(0);
        Signal<Integer> count = Signal.of(20);
        Paginator p = new Paginator(page, count);
        assertArrayEquals(new int[]{0, 1, 2, 3, 4, -1, 19}, p.visiblePages());
        page.set(10);
        assertArrayEquals(new int[]{0, -1, 9, 10, 11, -1, 19}, p.visiblePages());
        page.set(19);
        assertArrayEquals(new int[]{0, -1, 15, 16, 17, 18, 19}, p.visiblePages());
        count.set(4);
        page.set(1);
        assertArrayEquals(new int[]{0, 1, 2, 3}, p.visiblePages());
    }

    @Test
    void paginatorClicksAndKeys() {
        DataView<Item> view = new DataView<>(items(200)).pageSize(10);
        Paginator p = new Paginator(view);
        UiRoot root = mount(p);
        paint(root);
        // Ячейки: «, ‹, 1, 2, 3, 4, 5, …, 20, ›, » — по 16px с зазором 2px; «›» — десятая.
        float y = p.screenY() + p.padding().top() + 8;
        float x0 = p.screenX() + p.padding().left();
        click(root, x0 + 18 * 3 + 8, y);   // «2»
        assertEquals(1, view.page().get());
        root.setFocus(p);
        root.keyDown(Keys.RIGHT, 0, 0);
        assertEquals(2, view.page().get());
        root.keyDown(Keys.END, 0, 0);
        assertEquals(19, view.page().get());
        root.keyDown(Keys.RIGHT, 0, 0);
        assertEquals(19, view.page().get(), "stays on the last page");
        root.keyDown(Keys.HOME, 0, 0);
        assertEquals(0, view.page().get());
        assertTrue(paint(root).stream().anyMatch(op -> op.startsWith("text 20 ")));
    }

    // ================================================================ Table

    @Test
    void tableInScrollPanelPaintsOnlyVisibleRows() {
        Table<Item> table = new Table<Item>().column("Имя", 2, Item::name)
                .column("Id", 1, i -> String.valueOf(i.id()), Comparator.comparingInt(Item::id))
                .rows(items(2_000)).width(Dim.FILL);
        ScrollPanel scroll = new ScrollPanel(table).height(Dim.px(150)).width(Dim.px(300));
        UiRoot root = mount(scroll);
        List<String> ops = paint(root);
        int[] range = table.paintedRows();
        assertEquals(0, range[0]);
        assertTrue(range[1] - range[0] < 12, "painted rows: " + Arrays.toString(range));
        long texts = ops.stream().filter(op -> op.startsWith("text ")).count();
        assertTrue(texts < 40, "text ops: " + texts);

        scroll.scrollTo(16 * 500, false);
        root.tick(0f);
        paint(root);
        range = table.paintedRows();
        assertTrue(range[0] >= 490 && range[0] <= 500 && range[1] < 515, Arrays.toString(range));
    }

    @Test
    void tableBoundToDataViewSortsTheWholeView() {
        DataView<Item> view = new DataView<>(items(100)).pageSize(10);
        Table<Item> table = new Table<Item>().column("Имя", 2, Item::name)
                .column("Id", 1, i -> String.valueOf(i.id()), Comparator.comparingInt(Item::id))
                .bind(view).width(Dim.px(300));
        mount(table);
        assertEquals(10, table.rows().size());
        table.sortBy(1);
        table.sortBy(1);
        // По убыванию во всём представлении: первая страница начинается с 99.
        assertEquals(99, table.rows().get(0).id());
        view.page().set(9);
        assertEquals(9, table.rows().get(0).id());
    }
}
