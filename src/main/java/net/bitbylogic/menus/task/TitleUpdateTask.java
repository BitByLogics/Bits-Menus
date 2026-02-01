package net.bitbylogic.menus.task;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.menus.Menu;
import net.bitbylogic.menus.MenuFlag;
import net.bitbylogic.utils.message.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class TitleUpdateTask {

    private final @NonNull Menu menu;
    private int taskId = -1;

    public void start(@NonNull JavaPlugin plugin) {
        if(menu.getData().hasFlag(MenuFlag.DISABLE_TITLE_UPDATE)) {
            return;
        }

        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::run, 5, 1).getTaskId();
    }

    public void cancel() {
        if (taskId == -1) {
            return;
        }

        Bukkit.getScheduler().cancelTask(taskId);
        taskId = -1;
    }

    private void run() {
        List<TagResolver.Single> modifiers = new ArrayList<>();
        modifiers.addAll(menu.getData().getPlaceholders());

        modifiers.add(Placeholder.unparsed("pages", menu.getInventories().size() + ""));

        menu.getInventories().forEach(menuInventory -> {
            Inventory inventory = menuInventory.getInventory();

            List<TagResolver.Single> finalModifiers = new ArrayList<>(modifiers);

            finalModifiers.add(Placeholder.unparsed("page", (menu.getInventories().indexOf(menuInventory) + 1) + ""));

            new ArrayList<>(inventory.getViewers()).forEach(viewer -> {
                String newTitle = MessageUtil.deserializeToSpigot(menuInventory.getTitle(), finalModifiers.toArray(new TagResolver.Single[]{}));

                if (viewer.getOpenInventory().getTopInventory() != inventory || viewer.getOpenInventory().getTitle().equalsIgnoreCase(newTitle)) {
                    return;
                }

                viewer.getOpenInventory().setTitle(newTitle);
            });
        });
    }

    public boolean isActive() {
        return taskId != -1;
    }

}
