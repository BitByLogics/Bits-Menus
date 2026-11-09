package net.bitbylogic.menus.item;

import net.bitbylogic.menus.MenuSession;
import org.bukkit.inventory.ItemStack;

public interface MenuItemUpdateProvider {

    ItemStack requestItem(MenuSession session, MenuItem menuItem);

}
