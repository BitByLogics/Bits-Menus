package net.bitbylogic.menus.task;

import com.tcoded.folialib.wrapper.task.WrappedTask;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.menus.MenuFlag;
import net.bitbylogic.menus.MenuSession;
import org.bukkit.plugin.java.JavaPlugin;

@RequiredArgsConstructor
public class MenuUpdateTask {

    private final MenuSession session;

    private WrappedTask task;

    public synchronized void startTask(@NonNull JavaPlugin plugin) {
        if (task != null) {
            return;
        }

        task = MenuScheduler.get(plugin).runTimer(this::pushUpdates, 1, 5);
    }

    public synchronized void cancelTask() {
        if (task == null) {
            return;
        }

        task.cancel();
        task = null;
    }

    public synchronized boolean isActive() {
        return task != null;
    }

    private void pushUpdates() {
        session.appendPageIfFull();

        if (session.getMenu().getData().hasFlag(MenuFlag.DISABLE_UPDATES)) {
            return;
        }

        session.refresh();
    }

}
