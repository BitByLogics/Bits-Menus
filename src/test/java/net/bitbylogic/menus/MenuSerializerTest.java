package net.bitbylogic.menus;

import net.bitbylogic.utils.message.MessageUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.Inventory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

class MenuSerializerTest {

    private final MenuSerializer serializer = new MenuSerializer();

    private MockedStatic<Bukkit> bukkit;

    @BeforeAll
    static void initMessages() {
        MessageUtil.init();
    }

    @BeforeEach
    void setUp() {
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(() -> Bukkit.createInventory(any(), anyInt(), any(Component.class))).thenAnswer(invocation -> mock(Inventory.class));
        bukkit.when(Bukkit::getLogger).thenReturn(Logger.getAnonymousLogger());
    }

    @AfterEach
    void tearDown() {
        bukkit.close();
    }

    @Test
    void unknownFlagsAreSkippedInsteadOfThrowing() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("Id", "flags");
        config.set("Size", 27);
        config.set("Flags", List.of("allow_input", "NOT_A_FLAG", "debug"));

        Optional<Menu> menu = serializer.deserialize(config);

        assertTrue(menu.isPresent());
        assertEquals(List.of(MenuFlag.ALLOW_INPUT, MenuFlag.DEBUG), menu.get().getData().getFlags());
    }

    @Test
    void titleIsSavedAsAString() {
        Menu menu = new Menu("saved", Component.text("Hello"), 27);
        YamlConfiguration config = new YamlConfiguration();

        serializer.serialize(config, menu);

        assertEquals("Hello", config.getString("Title"));
        assertEquals(27, config.getInt("Size"));
    }

    @Test
    void savedMenusLoadBack() {
        Menu menu = new Menu("saved", Component.text("Hello"), 18);
        menu.getData().withFlag(MenuFlag.ALLOW_REMOVAL);
        YamlConfiguration config = new YamlConfiguration();

        serializer.serialize(config, menu);
        Optional<Menu> loaded = serializer.deserialize(config);

        assertTrue(loaded.isPresent());
        assertEquals("saved", loaded.get().getId());
        assertEquals(18, loaded.get().getSize());
        assertTrue(loaded.get().getData().hasFlag(MenuFlag.ALLOW_REMOVAL));
    }

}
