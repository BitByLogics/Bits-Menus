package net.bitbylogic.menus.inventory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.bitbylogic.menus.Menu;
import org.bukkit.inventory.Inventory;

@Getter @Setter
@AllArgsConstructor
public class MenuInventory {

    private final Inventory inventory;
    private String title;

    public Menu getMenu() {
        return (Menu) inventory.getHolder();
    }

    public boolean hasSpace() {
        return getMenu().getData().getValidSlots().stream().anyMatch(slot -> inventory.getItem(slot) == null);
    }

}
