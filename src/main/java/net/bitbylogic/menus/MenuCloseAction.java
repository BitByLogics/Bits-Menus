package net.bitbylogic.menus;

import lombok.NonNull;
import org.bukkit.event.inventory.InventoryCloseEvent;

public interface MenuCloseAction {

    void onClose(@NonNull InventoryCloseEvent event);

}
