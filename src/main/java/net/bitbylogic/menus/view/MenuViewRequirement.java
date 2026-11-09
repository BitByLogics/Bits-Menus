package net.bitbylogic.menus.view;

import lombok.NonNull;
import net.bitbylogic.menus.MenuSession;
import net.bitbylogic.menus.inventory.MenuInventory;
import net.bitbylogic.menus.item.MenuItem;

public interface MenuViewRequirement {

    boolean canView(@NonNull MenuSession session, @NonNull MenuInventory page, @NonNull MenuItem item);

}
