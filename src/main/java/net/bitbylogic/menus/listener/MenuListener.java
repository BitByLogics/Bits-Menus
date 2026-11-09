package net.bitbylogic.menus.listener;

import lombok.RequiredArgsConstructor;
import net.bitbylogic.menus.MenuFlag;
import net.bitbylogic.menus.MenuSession;
import net.bitbylogic.menus.data.MenuData;
import net.bitbylogic.menus.inventory.MenuInventory;
import net.bitbylogic.menus.item.MenuItem;
import net.bitbylogic.menus.task.MenuScheduler;
import net.bitbylogic.utils.cooldown.CooldownUtil;
import net.bitbylogic.utils.inventory.InventoryUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class MenuListener implements Listener {

    private static final int EXTERNAL_CLICK_COOLDOWN = 200;

    private final JavaPlugin plugin;

    @EventHandler
    public void onMenuClick(InventoryClickEvent event) {
        Inventory topInventory = InventoryUtil.getViewInventory(event, "getTopInventory");
        Inventory bottomInventory = InventoryUtil.getViewInventory(event, "getBottomInventory");

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        if (!(topInventory.getHolder() instanceof MenuSession session)) {
            return;
        }

        MenuData data = session.getMenu().getData();

        if (event.getClick() == ClickType.NUMBER_KEY && event.getClickedInventory() == topInventory) {
            event.setCancelled(true);
            return;
        }

        if (event.isShiftClick() && event.getClickedInventory() != topInventory) {
            event.setCancelled(!data.hasFlag(MenuFlag.ALLOW_INPUT));
            return;
        }

        if (event.getClickedInventory() == bottomInventory) {
            handleExternalClick(event, session);
            return;
        }

        if (event.getClickedInventory() != topInventory) {
            return;
        }

        if (data.getClickAction() != null) {
            data.getClickAction().onClick(event);
        }

        List<MenuItem> clickedItems = session.getItems(topInventory, event.getSlot());
        boolean cursorEmpty = event.getCursor().getType() == Material.AIR;

        if (clickedItems.isEmpty() && cursorEmpty && data.hasFlag(MenuFlag.ALLOW_REMOVAL)) {
            return;
        }

        event.setCancelled(!clickedItems.isEmpty() || !data.hasFlag(MenuFlag.ALLOW_INPUT));

        MenuInventory page = session.getPage(topInventory).orElse(null);

        if (page == null) {
            return;
        }

        for (MenuItem menuItem : clickedItems) {
            if (!session.isVisible(page, menuItem)) {
                continue;
            }

            if (menuItem.getClickRequirements().stream().anyMatch(requirement -> !requirement.canClick(player))) {
                continue;
            }

            menuItem.onClick(event, plugin);
            session.refreshItem(menuItem);
        }
    }

    @EventHandler
    public void onTransfer(InventoryMoveItemEvent event) {
        if (!(event.getDestination().getHolder() instanceof MenuSession session)) {
            return;
        }

        event.setCancelled(!session.getMenu().getData().hasFlag(MenuFlag.ALLOW_INPUT));
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        Inventory inventory = event.getInventory();
        Inventory bottomInventory = InventoryUtil.getViewInventory(event, "getBottomInventory");

        if (!(inventory.getHolder() instanceof MenuSession session)) {
            return;
        }

        MenuData data = session.getMenu().getData();

        if (inventory == bottomInventory && data.hasFlag(MenuFlag.LOWER_INTERACTION)) {
            return;
        }

        event.setCancelled(!data.hasFlag(MenuFlag.ALLOW_INPUT));
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuSession session)) {
            return;
        }

        session.addViewer(event.getPlayer().getUniqueId());
        session.getUpdateTask().startTask(plugin);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (!(event.getInventory().getHolder() instanceof MenuSession session)) {
            return;
        }

        session.removeViewer(event.getPlayer().getUniqueId());

        boolean otherViewers = event.getViewers().stream().anyMatch(viewer -> !viewer.getUniqueId().equals(event.getPlayer().getUniqueId()));

        if (!otherViewers) {
            session.getUpdateTask().cancelTask();
        }

        if (session.getMenu().getData().getCloseAction() == null) {
            return;
        }

        Player player = (Player) event.getPlayer();

        MenuScheduler.get(plugin).runAtEntityLater(player, () -> {
            if (player.isOnline()) {
                player.updateInventory();
            }
        }, 1);
        MenuScheduler.get(plugin).runAtEntityLater(player, () -> session.getMenu().getData().getCloseAction().onClose(event), 2);
    }

    private void handleExternalClick(InventoryClickEvent event, MenuSession session) {
        MenuData data = session.getMenu().getData();
        boolean lowerInteraction = data.hasFlag(MenuFlag.LOWER_INTERACTION);

        if (data.getExternalClickAction() == null) {
            event.setCancelled(!lowerInteraction);
            return;
        }

        String cooldownKey = session.getMenu().getId() + "exc-" + event.getSlot();

        if (CooldownUtil.hasCooldown(cooldownKey, event.getWhoClicked().getUniqueId())) {
            event.setCancelled(!lowerInteraction);
            return;
        }

        CooldownUtil.startCooldown(plugin, cooldownKey, event.getWhoClicked().getUniqueId(), EXTERNAL_CLICK_COOLDOWN, TimeUnit.MILLISECONDS);

        data.getExternalClickAction().onClick(event);
        event.setCancelled(!lowerInteraction);
    }

}
