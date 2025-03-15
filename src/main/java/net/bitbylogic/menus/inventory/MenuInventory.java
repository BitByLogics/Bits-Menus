package net.bitbylogic.menus.inventory;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import net.bitbylogic.menus.Menu;
import net.bitbylogic.utils.inventory.InventoryUtil;
import net.bitbylogic.utils.item.ItemStackUtil;
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
        return InventoryUtil.hasSpace(inventory, null, getMenu().getData().getValidSlots());
    }

}
