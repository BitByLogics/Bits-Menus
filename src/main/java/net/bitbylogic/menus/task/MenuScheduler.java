package net.bitbylogic.menus.task;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import lombok.NonNull;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MenuScheduler {

    private static final Map<JavaPlugin, FoliaLib> INSTANCES = new ConcurrentHashMap<>();

    private MenuScheduler() {
    }

    public static PlatformScheduler get(@NonNull JavaPlugin plugin) {
        return INSTANCES.computeIfAbsent(plugin, FoliaLib::new).getScheduler();
    }

    public static void release(@NonNull JavaPlugin plugin) {
        FoliaLib foliaLib = INSTANCES.remove(plugin);

        if (foliaLib == null) {
            return;
        }

        foliaLib.getScheduler().cancelAllTasks();
    }

}
