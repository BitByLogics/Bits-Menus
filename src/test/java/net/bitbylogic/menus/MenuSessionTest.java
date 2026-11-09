package net.bitbylogic.menus;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import net.bitbylogic.menus.data.MenuData;
import net.bitbylogic.menus.inventory.MenuInventory;
import net.bitbylogic.menus.item.MenuItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuSessionTest {

    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.createInventory(any(), anyInt(), any(Component.class))).thenAnswer(invocation -> mock(Inventory.class));
        bukkit.when(Bukkit::getLogger).thenReturn(Logger.getAnonymousLogger());
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    private Menu createMenu(MenuData data) {
        return new Menu("test", Component.text("Test"), 27, data);
    }

    private ItemStack createStack() {
        ItemStack stack = mock(ItemStack.class);
        when(stack.clone()).thenReturn(stack);
        when(stack.getType()).thenReturn(Material.STONE);
        return stack;
    }

    private MenuItem createItem(String id, int slot) {
        return new MenuItem(id).item(createStack()).withSlot(slot);
    }

    @Test
    void sessionsFromTheSameMenuAreIndependent() {
        Menu menu = createMenu(null);
        menu.addItem(createItem("button", 4));

        MenuSession first = menu.createSession();
        MenuSession second = menu.createSession();

        assertNotSame(first.getInventory(), second.getInventory());
        assertEquals(1, first.getItems(first.getInventory(), 4).size());
        assertTrue(first.getItems(second.getInventory(), 4).isEmpty());
    }

    @Test
    void definitionItemsAreRenderedIntoEveryPage() {
        MenuItem button = createItem("button", 4);
        Menu menu = createMenu(new MenuData().withMinInventories(2));
        menu.addItem(button);

        MenuSession session = menu.createSession();

        assertEquals(2, session.getInventories().size());

        for (MenuInventory page : session.getInventories()) {
            verify(page.getInventory()).setItem(eq(4), any(ItemStack.class));
            assertEquals(List.of(button), page.getItems(4));
        }
    }

    @Test
    void setItemPlacesAtAFixedSlotOnlyForThatSession() {
        Menu menu = createMenu(null);
        MenuSession first = menu.createSession();
        MenuSession second = menu.createSession();

        assertTrue(first.setItem(22, createItem("barrier", 22)));

        assertEquals(1, first.getItems(first.getInventory(), 22).size());
        assertTrue(second.getItems(second.getInventory(), 22).isEmpty());
        assertTrue(menu.getItems().isEmpty());
    }

    @Test
    void setItemRejectsInvalidSlotsAndPages() {
        MenuSession session = createMenu(null).createSession();

        assertFalse(session.setItem(99, createItem("button", 99)));
        assertFalse(session.setItem(-1, createItem("button", 0)));
        assertFalse(session.setItem(5, 1, createItem("button", 1)));
    }

    @Test
    void itemsOutsideTheMenuAreSkipped() {
        Menu menu = createMenu(null);
        menu.addItem(createItem("button", 99));

        MenuSession session = menu.createSession();

        assertTrue(session.getItems(session.getInventory(), 99).isEmpty());
    }

    @Test
    void viewRequirementsDecideWhetherAnItemIsShown() {
        boolean[] visible = {false};
        MenuItem button = createItem("button", 4).withViewRequirement((session, page, item) -> visible[0]);
        Menu menu = createMenu(null);
        menu.addItem(button);

        MenuSession session = menu.createSession();
        Inventory inventory = session.getInventory();

        verify(inventory, never()).setItem(eq(4), any(ItemStack.class));

        visible[0] = true;
        session.refresh();

        verify(inventory).setItem(eq(4), any(ItemStack.class));
    }

    @Test
    void updateProvidersReceiveTheSession() {
        MenuSession[] received = new MenuSession[1];
        ItemStack stack = createStack();
        MenuItem button = new MenuItem("button").withSlot(1).updateProvider((session, item) -> {
            received[0] = session;
            return stack;
        });
        Menu menu = createMenu(null);
        menu.addItem(button);

        MenuSession session = menu.createSession();
        session.getInventory();

        assertEquals(session, received[0]);
    }

    @Test
    void addItemUsesTheNextValidSlotAndAppendsPages() {
        Menu menu = createMenu(new MenuData().withValidSlots(List.of(0, 1)).withMaxInventories(2));
        MenuSession session = menu.createSession();

        assertTrue(session.addItem(createItem("a", 0)));
        assertTrue(session.addItem(createItem("b", 0)));
        assertTrue(session.addItem(createItem("c", 0)));
        assertTrue(session.addItem(createItem("d", 0)));
        assertFalse(session.addItem(createItem("e", 0)));
        assertEquals(2, session.getPageCount());
    }

    @Test
    void addItemFailsWithoutValidSlots() {
        MenuSession session = createMenu(null).createSession();

        assertFalse(session.addItem(createItem("a", 0)));
    }

    @Test
    void appendPageStopsAtTheMaximum() {
        MenuSession session = createMenu(new MenuData().withMaxInventories(2)).createSession();

        assertEquals(1, session.getInventories().size());
        assertTrue(session.appendPage().isPresent());
        assertTrue(session.appendPage().isEmpty());
        assertEquals(2, session.getPageCount());
    }

    @Test
    void pageButtonsAreOnlyShownWhenThereIsAPageToGoTo() {
        MenuItem next = new MenuItem("Next-Page-Item").item(createStack()).nextPageButton(List.of(8));
        Menu menu = createMenu(null);
        menu.addItem(next);

        MenuSession session = menu.createSession();
        Inventory first = session.getInventory();

        verify(first, never()).setItem(eq(8), any(ItemStack.class));

        session.appendPage();

        verify(first).setItem(eq(8), any(ItemStack.class));
    }

    @Test
    void getInventoriesIsAReadOnlySnapshot() {
        MenuSession session = createMenu(null).createSession();

        List<MenuInventory> inventories = session.getInventories();

        assertThrows(UnsupportedOperationException.class, inventories::clear);
    }

    @Test
    void viewersAreTrackedAndExposedReadOnly() {
        MenuSession session = createMenu(null).createSession();
        UUID viewer = UUID.randomUUID();

        session.addViewer(viewer);

        assertEquals(List.of(viewer), session.getViewers());
        assertThrows(UnsupportedOperationException.class, () -> session.getViewers().add(UUID.randomUUID()));

        session.removeViewer(viewer);

        assertTrue(session.getViewers().isEmpty());
    }

    @Test
    void placeholdersAreCopiedPerSession() {
        Menu menu = createMenu(null);
        MenuSession first = menu.createSession();
        MenuSession second = menu.createSession();

        first.withPlaceholder(Placeholder.unparsed("name", "value"));

        assertEquals(1, first.getPlaceholders().size());
        assertTrue(second.getPlaceholders().isEmpty());
        assertTrue(menu.getData().getPlaceholders().isEmpty());
    }

    @Test
    void openRejectsInvalidPagesWithoutScheduling() {
        MenuSession session = createMenu(null).createSession();
        JavaPlugin plugin = mock(JavaPlugin.class);
        Player player = mock(Player.class);

        try (MockedConstruction<FoliaLib> foliaLib = mockConstruction(FoliaLib.class)) {
            session.open(player, plugin, 0);
            session.open(player, plugin, 5);

            assertTrue(foliaLib.constructed().isEmpty());
        }
    }

    @Test
    void openSchedulesTheInventoryOnTheEntityScheduler() {
        MenuSession session = createMenu(null).createSession();
        JavaPlugin plugin = mock(JavaPlugin.class);
        Player player = mock(Player.class);
        PlatformScheduler scheduler = mock(PlatformScheduler.class);

        try (MockedConstruction<FoliaLib> ignored = mockConstruction(FoliaLib.class,
                (foliaLib, context) -> when(foliaLib.getScheduler()).thenReturn(scheduler))) {
            session.open(player, plugin);

            verify(scheduler).runAtEntityLater(eq(player), any(Runnable.class), eq(1L));
        }
    }

    @Test
    void openOpensTheRequestedPageDirectly() {
        MenuSession session = createMenu(new MenuData().withMinInventories(2)).createSession();
        Player player = mock(Player.class);

        session.open(player, 2);

        verify(player).openInventory(session.getInventories().get(1).getInventory());
    }

    @Test
    void openRelativePageStaysInsideTheAvailablePages() {
        MenuSession session = createMenu(new MenuData().withMinInventories(2)).createSession();
        Player player = mock(Player.class);
        Inventory first = session.getInventories().get(0).getInventory();
        Inventory second = session.getInventories().get(1).getInventory();

        session.openRelativePage(player, first, 1);
        session.openRelativePage(player, first, -1);
        session.openRelativePage(player, second, 1);

        verify(player).openInventory(second);
        verify(player, never()).openInventory(first);
    }

    @Test
    void menuInventoryHasSpaceOnlyForFreeValidSlots() {
        MenuSession session = createMenu(null).createSession();
        MenuInventory page = session.getInventories().getFirst();

        assertTrue(page.hasSpace(List.of(3)));

        page.place(3, createItem("a", 3));

        assertFalse(page.hasSpace(List.of(3)));
    }

}
