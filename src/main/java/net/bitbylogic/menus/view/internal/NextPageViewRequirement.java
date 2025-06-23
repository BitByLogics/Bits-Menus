package net.bitbylogic.menus.view.internal;

import net.bitbylogic.menus.Menu;
import net.bitbylogic.menus.MenuFlag;
import net.bitbylogic.menus.item.MenuItem;
import net.bitbylogic.menus.view.MenuViewRequirement;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

public class NextPageViewRequirement implements MenuViewRequirement {

    @Override
    public boolean canView(@NotNull Inventory inventory, @NotNull MenuItem item, @NotNull Menu menu) {
        return menu.getData().hasFlag(MenuFlag.ALWAYS_DISPLAY_NAV) || (menu.getInventories().size() > 1 && menu.getInventoryIndex(inventory) < menu.getInventories().size() - 1);
    }

}
