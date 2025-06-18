package net.bitbylogic.menus.action;

import lombok.NonNull;
import org.bukkit.event.inventory.InventoryClickEvent;

public interface ClickAction {

    void onClick(@NonNull InventoryClickEvent event);

}
