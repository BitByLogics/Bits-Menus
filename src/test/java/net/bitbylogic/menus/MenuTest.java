package net.bitbylogic.menus;

import net.bitbylogic.menus.item.MenuItem;
import net.kyori.adventure.text.Component;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuTest {

    private final Menu menu = new Menu("test", Component.text("Test"), 27);

    @Test
    void getItemsReturnsAnUnmodifiableSnapshot() {
        menu.addItem(new MenuItem("one").withSlot(1));

        List<MenuItem> snapshot = menu.getItems();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(new MenuItem("two")));

        menu.addItem(new MenuItem("two").withSlot(2));

        assertEquals(1, snapshot.size());
        assertEquals(2, menu.getItems().size());
    }

    @Test
    void withItemStoresItemsWithoutSlotsAndSkipsDuplicates() {
        MenuItem stored = new MenuItem("stored");
        MenuItem slotted = new MenuItem("slotted").withSlot(3);

        menu.withItem(stored).withItem(slotted).withItem(new MenuItem("slotted").withSlot(4));

        assertTrue(menu.getData().getStoredItem("stored").isPresent());
        assertEquals(List.of(slotted), menu.getItems());
    }

    @Test
    void getItemOrCreateReturnsTheSameItem() {
        MenuItem created = menu.getItemOrCreate("button");

        assertSame(created, menu.getItemOrCreate("BUTTON"));
        assertEquals(1, menu.getItems().size());
    }

}
