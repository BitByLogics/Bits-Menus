package net.bitbylogic.menus.listener;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.menus.Menu;
import net.bitbylogic.menus.MenuFlag;
import net.bitbylogic.menus.item.MenuItem;
import net.bitbylogic.utils.cooldown.CooldownUtil;
import net.bitbylogic.utils.inventory.InventoryUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class MenuListener implements Listener {

    private final JavaPlugin plugin;
    private final TimeUnit clickCooldownUnit = TimeUnit.MILLISECONDS;

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        Inventory topInventory = InventoryUtil.getViewInventory(event, "getTopInventory");
        Inventory bottomInventory = InventoryUtil.getViewInventory(event, "getBottomInventory");

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        if (!(topInventory.getHolder() instanceof Menu menu)) {
            return;
        }

        if (event.getClick() == ClickType.NUMBER_KEY && event.getClickedInventory() == topInventory) {
            event.setCancelled(true);
            return;
        }

        if (event.isShiftClick() && event.getClickedInventory() != topInventory) {
            event.setCancelled(!menu.getData().hasFlag(MenuFlag.ALLOW_INPUT));
            return;
        }

        if (event.getClickedInventory() == bottomInventory) {
            if (menu.getData().getExternalClickAction() != null) {
                if(CooldownUtil.hasCooldown(menu.getId() + "exc-" + event.getSlot(), event.getWhoClicked().getUniqueId())) {
                    event.setCancelled(!menu.getData().hasFlag(MenuFlag.LOWER_INTERACTION));
                    return;
                }

                int clickCooldownTime = 200;
                CooldownUtil.startCooldown(plugin, menu.getId() + "exc-" + event.getSlot(), event.getWhoClicked().getUniqueId(), clickCooldownTime, clickCooldownUnit);

                menu.getData().getExternalClickAction().onClick(event);
            }

            event.setCancelled(!menu.getData().hasFlag(MenuFlag.LOWER_INTERACTION));
            return;
        }

        if (event.getClickedInventory() != topInventory) {
            return;
        }

        if(menu.getData().getClickAction() != null) {
            menu.getData().getClickAction().onClick(event);
        }

        if (menu.getItem(topInventory, event.getSlot()).isEmpty() && (event.getCursor() == null || event.getCursor().getType() == Material.AIR) && menu.getData().hasFlag(MenuFlag.ALLOW_REMOVAL)) {
            return;
        }

        event.setCancelled(menu.getItem(topInventory, event.getSlot()).isPresent() || !menu.getData().hasFlag(MenuFlag.ALLOW_INPUT));

        List<MenuItem> clickedItemsCopy = new ArrayList<>(menu.getItems(topInventory, event.getSlot()));

        clickedItemsCopy.forEach(menuItem -> {
            if (menuItem.getViewRequirements().stream().anyMatch(requirement -> !requirement.canView(topInventory, menuItem, menu))) {
                return;
            }

            if (menuItem.getClickRequirements().stream().anyMatch(requirement -> !requirement.canClick((Player) event.getWhoClicked()))) {
                return;
            }

            menuItem.onClick(event, plugin);

            List<Inventory> sourceInventoriesCopy = new ArrayList<>(menuItem.getSourceInventories());

            sourceInventoriesCopy.forEach(inventory -> {
                List<Integer> slotsCopy = new ArrayList<>(menuItem.getSlots());
                slotsCopy.forEach(slot ->
                        inventory.setItem(slot, menuItem.getItemUpdateProvider() == null ? menuItem.getItem().clone() : menuItem.getItemUpdateProvider().requestItem(menuItem))
                );
            });
        });
    }

    @EventHandler
    public void onTransfer(InventoryMoveItemEvent event) {
        Inventory inventory = event.getDestination();

        if (!(inventory.getHolder() instanceof Menu menu)) {
            return;
        }

        event.setCancelled(!menu.getData().hasFlag(MenuFlag.ALLOW_INPUT));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        Inventory bottomInventory = InventoryUtil.getViewInventory(event, "getBottomInventory");

        if (!(inventory.getHolder() instanceof Menu menu)) {
            return;
        }

        if (inventory == bottomInventory && menu.getData().hasFlag(MenuFlag.LOWER_INTERACTION)) {
            return;
        }

        event.setCancelled(!menu.getData().hasFlag(MenuFlag.ALLOW_INPUT));
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        Inventory inventory = event.getInventory();

        if (!(inventory.getHolder() instanceof Menu menu)) {
            return;
        }

        menu.getViewers().add(event.getPlayer().getUniqueId());

        if (menu.getTitleUpdateTask() != null && !menu.getTitleUpdateTask().isActive()) {
            menu.getTitleUpdateTask().start(plugin);
        }

        if (menu.getUpdateTask() == null || menu.getUpdateTask().isActive()) {
            return;
        }

        menu.getUpdateTask().startTask(plugin);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        Inventory inventory = event.getInventory();

        if (!(inventory.getHolder() instanceof Menu menu)) {
            return;
        }

        menu.getViewers().remove(event.getPlayer().getUniqueId());

        if (event.getViewers().stream().filter(p -> !p.getUniqueId().equals(event.getPlayer().getUniqueId())).count() == 0) {
            menu.getUpdateTask().cancelTask();

            if (menu.getTitleUpdateTask() != null) {
                menu.getTitleUpdateTask().cancel();
            }
        }

        if (menu.getData().getCloseAction() == null) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> ((Player) event.getPlayer()).updateInventory(), 1);
        Bukkit.getScheduler().runTaskLater(plugin, () -> menu.getData().getCloseAction().onClose(event), 2);
    }

}
