package net.bitbylogic.menus.action;

import lombok.NonNull;
import net.bitbylogic.menus.MenuSession;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;

public class PreviousPageAction implements ClickAction {

    @Override
    public void onClick(@NonNull InventoryClickEvent event) {
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory == null) {
            return;
        }

        if (!(clickedInventory.getHolder() instanceof MenuSession session)) {
            return;
        }

        session.openRelativePage(event.getWhoClicked(), clickedInventory, -1);
    }

}
