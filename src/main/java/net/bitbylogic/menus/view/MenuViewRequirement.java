package net.bitbylogic.menus.view;

import lombok.NonNull;
import net.bitbylogic.menus.Menu;
import net.bitbylogic.menus.item.MenuItem;
import org.bukkit.inventory.Inventory;

public interface MenuViewRequirement {

    boolean canView(@NonNull Inventory inventory, @NonNull MenuItem item, @NonNull Menu menu);

}
