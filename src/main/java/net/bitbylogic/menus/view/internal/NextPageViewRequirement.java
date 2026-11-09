package net.bitbylogic.menus.view.internal;

import lombok.NonNull;
import net.bitbylogic.menus.MenuFlag;
import net.bitbylogic.menus.MenuSession;
import net.bitbylogic.menus.inventory.MenuInventory;
import net.bitbylogic.menus.item.MenuItem;
import net.bitbylogic.menus.view.MenuViewRequirement;

public class NextPageViewRequirement implements MenuViewRequirement {

    @Override
    public boolean canView(@NonNull MenuSession session, @NonNull MenuInventory page, @NonNull MenuItem item) {
        if (session.getMenu().getData().hasFlag(MenuFlag.ALWAYS_DISPLAY_NAV)) {
            return true;
        }

        int pageCount = session.getPageCount();

        if (pageCount <= 1) {
            return false;
        }

        return session.getPageIndex(page.getInventory()) < pageCount - 1;
    }

}
