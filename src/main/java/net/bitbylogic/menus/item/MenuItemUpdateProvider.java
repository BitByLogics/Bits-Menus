package net.bitbylogic.menus.item;

import org.bukkit.inventory.ItemStack;

public interface MenuItemUpdateProvider {

    ItemStack requestItem(MenuItem menuItem);

}
