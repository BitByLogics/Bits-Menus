package net.bitbylogic.menus;

import net.bitbylogic.menus.item.MenuItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class MenuBuilderTest {

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

    @Test
    void invalidSizesAreClampedInsteadOfThrowing() {
        assertEquals(9, new MenuBuilder("a", Component.text("A"), 0).build().getSize());
        assertEquals(9, new MenuBuilder("a", Component.text("A"), 10).build().getSize());
        assertEquals(54, new MenuBuilder("a", Component.text("A"), 100).build().getSize());
        assertEquals(27, new MenuBuilder("a", Component.text("A"), 27).build().getSize());
    }

    @Test
    void itemsWithoutSlotsGoToStorage() {
        MenuItem stored = new MenuItem("stored");
        MenuItem slotted = new MenuItem("slotted");
        slotted.withSlot(2);

        MenuBuilder builder = new MenuBuilder("a", Component.text("A"), 9);
        builder.withItem(stored).withItems(List.of(slotted));

        Menu menu = builder.build();

        assertTrue(menu.getData().getStoredItem("stored").isPresent());
        assertEquals(1, menu.getItems().size());
        assertEquals("slotted", menu.getItems().getFirst().getId());
    }

}
