package net.bitbylogic.menus.task;

import com.tcoded.folialib.FoliaLib;
import com.tcoded.folialib.impl.PlatformScheduler;
import com.tcoded.folialib.wrapper.task.WrappedTask;
import net.bitbylogic.menus.MenuSession;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MenuUpdateTaskTest {

    @Test
    void startingTwiceSchedulesOnlyOnceAndCancelStops() {
        JavaPlugin plugin = mock(JavaPlugin.class);
        PlatformScheduler scheduler = mock(PlatformScheduler.class);
        WrappedTask wrappedTask = mock(WrappedTask.class);

        when(scheduler.runTimer(any(Runnable.class), eq(1L), eq(5L))).thenReturn(wrappedTask);

        try (MockedConstruction<FoliaLib> ignored = mockConstruction(FoliaLib.class,
                (foliaLib, context) -> when(foliaLib.getScheduler()).thenReturn(scheduler))) {
            MenuUpdateTask task = new MenuUpdateTask(mock(MenuSession.class));

            task.startTask(plugin);
            task.startTask(plugin);

            assertTrue(task.isActive());
            verify(scheduler, times(1)).runTimer(any(Runnable.class), eq(1L), eq(5L));

            task.cancelTask();
            task.cancelTask();

            assertFalse(task.isActive());
            verify(wrappedTask, times(1)).cancel();
        }
    }

}
