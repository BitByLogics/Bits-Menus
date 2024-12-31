package net.bitbylogic.menus;

import lombok.NonNull;
import org.bukkit.event.inventory.InventoryClickEvent;

public interface MenuAction {

    void onClick(@NonNull InventoryClickEvent event);

}
