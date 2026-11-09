package net.bitbylogic.menus.listener;

import net.bitbylogic.menus.Menu;
import net.bitbylogic.menus.MenuSession;
import net.bitbylogic.menus.data.MenuData;
import net.bitbylogic.menus.task.MenuUpdateTask;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuListenerTest {

    private final JavaPlugin plugin = mock(JavaPlugin.class);
    private final MenuListener listener = new MenuListener(plugin);

    private final MenuSession session = mock(MenuSession.class);
    private final MenuUpdateTask updateTask = mock(MenuUpdateTask.class);
    private final Inventory inventory = mock(Inventory.class);
    private final HumanEntity player = mock(HumanEntity.class);
    private final UUID playerId = UUID.randomUUID();

    MenuListenerTest() {
        Menu menu = mock(Menu.class);

        when(inventory.getHolder()).thenReturn(session);
        when(session.getUpdateTask()).thenReturn(updateTask);
        when(session.getMenu()).thenReturn(menu);
        when(menu.getData()).thenReturn(new MenuData());
        when(player.getUniqueId()).thenReturn(playerId);
    }

    @Test
    void openTracksTheViewerAndStartsTheUpdateTask() {
        InventoryOpenEvent event = mock(InventoryOpenEvent.class);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getPlayer()).thenReturn(player);

        listener.onOpen(event);

        verify(session).addViewer(playerId);
        verify(updateTask).startTask(plugin);
    }

    @Test
    void closeStopsTheUpdateTaskWhenNobodyElseIsViewing() {
        InventoryCloseEvent event = mock(InventoryCloseEvent.class);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getPlayer()).thenReturn(player);
        when(event.getViewers()).thenReturn(List.of(player));

        listener.onClose(event);

        verify(session).removeViewer(playerId);
        verify(updateTask).cancelTask();
    }

    @Test
    void closeKeepsTheUpdateTaskWhileOthersAreViewing() {
        HumanEntity other = mock(HumanEntity.class);
        when(other.getUniqueId()).thenReturn(UUID.randomUUID());

        InventoryCloseEvent event = mock(InventoryCloseEvent.class);
        when(event.getInventory()).thenReturn(inventory);
        when(event.getPlayer()).thenReturn(player);
        when(event.getViewers()).thenReturn(List.of(player, other));

        listener.onClose(event);

        verify(updateTask, never()).cancelTask();
    }

}
