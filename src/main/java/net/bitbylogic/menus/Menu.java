package net.bitbylogic.menus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.menus.data.MenuData;
import net.bitbylogic.menus.item.MenuItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;

@Getter
public class Menu {

    private static final MenuSerializer SERIALIZER = new MenuSerializer();
    private static final String MENU_CONFIG_PATH = "menus/%s.yml";

    private final String id;
    private final Component title;
    private final int size;

    private final MenuData data;

    @Getter(AccessLevel.NONE)
    private final List<MenuItem> items = new CopyOnWriteArrayList<>();

    @Getter(AccessLevel.NONE)
    private final Object itemLock = new Object();

    public Menu(@NonNull String id, @NonNull Component title, @NonNull MenuRows menuRows) {
        this(id, title, menuRows.getSize());
    }

    public Menu(@NonNull String id, @NonNull Component title, int size) {
        this(id, title, size, null);
    }

    public Menu(@NonNull String id, @NonNull Component title, int size, @Nullable MenuData data) {
        this.id = id;
        this.title = title;
        this.size = size;

        if (data == null) {
            this.data = new MenuData();
            return;
        }

        this.data = data;
    }

    public static Optional<Menu> getFromFile(@Nullable File directory, @NonNull String id) {
        File file = new File(directory, String.format(MENU_CONFIG_PATH, id));

        if (!file.exists() || file.isDirectory()) {
            return Optional.empty();
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        return SERIALIZER.deserialize(config);
    }

    public static Optional<Menu> getFromConfig(@Nullable ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }

        return SERIALIZER.deserialize(section);
    }

    public MenuSession createSession() {
        return new MenuSession(this);
    }

    public MenuSession open(@NonNull Player player, @NonNull JavaPlugin plugin) {
        return open(player, plugin, 1);
    }

    public MenuSession open(@NonNull Player player, @NonNull JavaPlugin plugin, int page) {
        MenuSession session = createSession();
        session.open(player, plugin, page);
        return session;
    }

    public Menu addItem(@NonNull MenuItem item) {
        items.add(item);
        return this;
    }

    public Menu setItem(int slot, @NonNull MenuItem item) {
        item.withSlot(slot);
        items.add(item);
        return this;
    }

    public Menu withItem(@NonNull MenuItem menuItem) {
        if (getItem(menuItem.getId()).isPresent()) {
            return this;
        }

        if (!menuItem.getSlots().isEmpty()) {
            return addItem(menuItem);
        }

        if (data.getStoredItem(menuItem.getId()).isPresent()) {
            return this;
        }

        data.getItemStorage().add(menuItem);
        return this;
    }

    public Optional<MenuItem> getItem(@NonNull String id) {
        return items.stream().filter(item -> item.getId().equalsIgnoreCase(id)).findFirst();
    }

    public MenuItem getItemOrCreate(@NonNull String id) {
        synchronized (itemLock) {
            Optional<MenuItem> foundItem = getItem(id);

            if (foundItem.isPresent()) {
                return foundItem.get();
            }

            MenuItem fallbackItem = new MenuItem(id);
            items.add(fallbackItem);
            return fallbackItem;
        }
    }

    public List<MenuItem> getItems() {
        return List.copyOf(items);
    }

    public void saveToFile(@NonNull File directory) {
        File file = new File(directory, String.format(MENU_CONFIG_PATH, id));

        if (file.exists()) {
            return;
        }

        File parent = file.getParentFile();

        if (!parent.exists() && !parent.mkdirs()) {
            Bukkit.getLogger().warning("Failed to create the directory for menu '" + id + "'!");
            return;
        }

        YamlConfiguration config = new YamlConfiguration();
        SERIALIZER.serialize(config, this);

        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().log(Level.WARNING, "Failed to save menu '" + id + "'!", e);
        }
    }

    public boolean saveToConfig(@NonNull ConfigurationSection section) {
        return saveToConfig(section, false);
    }

    public boolean saveToConfig(@NonNull ConfigurationSection section, boolean overwrite) {
        ConfigurationSection itemsSection = section.getConfigurationSection(id + ".Items");

        if (itemsSection != null) {
            items.stream().filter(MenuItem::isSaved).forEach(menuItem -> menuItem.saveToConfig(itemsSection, overwrite));
            data.getItemStorage().stream().filter(MenuItem::isSaved).forEach(menuItem -> menuItem.saveToConfig(itemsSection, overwrite));
        }

        if (section.isSet(id) && !overwrite) {
            return false;
        }

        SERIALIZER.serialize(section.createSection(id), this);
        return true;
    }

}
