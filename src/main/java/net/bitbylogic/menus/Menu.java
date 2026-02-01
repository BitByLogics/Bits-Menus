package net.bitbylogic.menus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.menus.data.MenuData;
import net.bitbylogic.menus.inventory.MenuInventory;
import net.bitbylogic.menus.item.MenuItem;
import net.bitbylogic.menus.task.MenuUpdateTask;
import net.bitbylogic.menus.task.TitleUpdateTask;
import net.bitbylogic.menus.view.internal.NextPageViewRequirement;
import net.bitbylogic.menus.view.internal.PreviousPageViewRequirement;
import net.bitbylogic.utils.Pair;
import net.bitbylogic.utils.inventory.InventoryUtil;
import net.bitbylogic.utils.item.ItemStackUtil;
import net.bitbylogic.utils.message.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.stream.Collectors;

@Getter
@Setter
public class Menu implements InventoryHolder, Cloneable {

    private static final MenuSerializer SERIALIZER = new MenuSerializer();
    private static final String MENU_CONFIG_PATH = "menus/%s.yml";

    private final String id;
    private final String title;
    private final int size;

    private final MenuData data;

    private final List<MenuItem> items;
    private final List<UUID> viewers;

    @Getter(AccessLevel.NONE)
    private final List<MenuInventory> inventories;

    private final MenuUpdateTask updateTask;
    private final TitleUpdateTask titleUpdateTask;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    public Menu(@NonNull String id, @NonNull String title, @NonNull MenuRows menuRows) {
        this(id, title, menuRows.getSize());
    }

    public Menu(@NonNull String id, @NonNull String title, int size) {
        this(id, title, size, null);
    }

    public Menu(@NonNull String id, @NonNull String title, int size, @Nullable MenuData data) {
        this.id = id;
        this.title = title;
        this.size = size;

        this.items = new ArrayList<>();
        this.data = data == null ? new MenuData() : data;
        this.inventories = new ArrayList<>();
        this.viewers = new ArrayList<>();

        titleUpdateTask = new TitleUpdateTask(this);
        updateTask = new MenuUpdateTask(this);
    }

    public Menu(@NonNull String id, @NonNull String title, int size, @Nullable MenuData data,
                @Nullable List<MenuItem> items, @Nullable List<MenuInventory> inventories) {
        this.id = id;
        this.title = title;
        this.size = size;
        this.data = data == null ? new MenuData() : data;
        this.items = items == null ? new ArrayList<>() : items;
        this.inventories = inventories == null ? new ArrayList<>() : inventories;
        this.viewers = new ArrayList<>();

        titleUpdateTask = new TitleUpdateTask(this);
        updateTask = new MenuUpdateTask(this);
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

    /**
     * Add an item to the Menu.
     *
     * @param item The item to add.
     * @return The Menu instance.
     */
    public Menu addItem(MenuItem item) {
        writeLock.lock();

        try {
            item.setMenu(this);

            if (item.getSourceInventories().isEmpty()) {
                item.getSourceInventories().add(inventories.isEmpty() ? getInventory() : inventories.getFirst().getInventory());
            }

            data.getFillerItem().ifPresent(fillerItem -> fillerItem.getSlots().removeAll(item.getSlots()));

            items.add(item);
            return this;
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Add an item to the Menu and set its
     * slot to the next available slot
     *
     * @param item The item to add.
     */
    public void addAndSetItem(@NonNull MenuItem item) {
        writeLock.lock();
        try {
            if (inventories.isEmpty()) {
                generateInventories();
            }

            Pair<Inventory, Integer> availableSlot = getNextAvailableSlot();

            if(availableSlot == null) {
                if(data.hasFlag(MenuFlag.DEBUG)) {
                    Bukkit.getLogger().log(Level.SEVERE, "Failed to find available slot for: " + item.getId());
                }

                return;
            }

            item.setMenu(this);
            items.add(item);

            boolean locked = item.isLocked();

            item.getSlots().clear();

            if (locked) {
                item.setLocked(false);
            }

            item.withSlot(availableSlot.getValue());
            item.withSourceInventory(availableSlot.getKey());
            item.setLocked(locked);

            availableSlot.getKey().setItem(availableSlot.getValue(), item.getItemUpdateProvider() == null ? item.getItem() : item.getItemUpdateProvider().requestItem(item));
        } finally {
            writeLock.unlock();
        }
    }

    public void addItemStack(ItemStack item) {
        writeLock.lock();
        try {
            if (inventories.isEmpty()) {
                generateInventories();
            }

            HashMap<MenuInventory, Integer> itemDistribution = new HashMap<>();

            ItemStack clonedItem = item.clone();
            int amountLeft = item.getAmount();

            for (MenuInventory menuInventory : inventories) {
                Inventory inventory = menuInventory.getInventory();
                int availableSpace = InventoryUtil.getAvailableSpace(inventory, item, data.getValidSlots());

                if (availableSpace >= amountLeft) {
                    amountLeft = 0;
                    InventoryUtil.addItem(inventory, clonedItem, data.getValidSlots());
                    break;
                }

                itemDistribution.put(menuInventory, availableSpace);
                amountLeft -= availableSpace;
                clonedItem.setAmount(amountLeft);
            }

            while (amountLeft > 0) {
                Optional<MenuInventory> generatedOptional = generateNewInventory();

                if (generatedOptional.isEmpty() || (data.getMaxInventories() != -1 && inventories.size() >= data.getMaxInventories())) {
                    break;
                }

                MenuInventory menuInventory = generatedOptional.get();
                inventories.add(menuInventory);

                Inventory inventory = menuInventory.getInventory();
                int availableSpace = InventoryUtil.getAvailableSpace(inventory, item, data.getValidSlots());

                if (availableSpace >= amountLeft) {
                    amountLeft = 0;
                    InventoryUtil.addItem(inventory, clonedItem, data.getValidSlots());
                    break;
                }

                itemDistribution.put(menuInventory, availableSpace);
                amountLeft -= availableSpace;
                clonedItem.setAmount(amountLeft);
            }

            itemDistribution.forEach((menuInventory, amount) -> {
                Inventory inventory = menuInventory.getInventory();

                ItemStack distributionItem = item.clone();
                distributionItem.setAmount(amount);
                InventoryUtil.addItem(inventory, distributionItem, data.getValidSlots());
            });
        } finally {
            writeLock.unlock();
        }
    }

    public Menu setItem(int slot, MenuItem item) {
        writeLock.lock();
        try {
            item.setMenu(this);
            item.withSlot(slot);
            items.add(item);
            return this;
        } finally {
            writeLock.unlock();
        }
    }

    public Optional<MenuItem> getItem(String id) {
        readLock.lock();
        try {
            return items.stream().filter(item -> item.getId().equalsIgnoreCase(id)).findFirst();
        } finally {
            readLock.unlock();
        }
    }

    public MenuItem getItemOrCreate(@NonNull String id) {
        writeLock.lock();
        try {
            Optional<MenuItem> foundItem = getItem(id);

            if (foundItem.isPresent()) {
                return foundItem.get();
            }

            MenuItem fallbackItem = new MenuItem(id);
            fallbackItem.setMenu(this);

            items.add(fallbackItem);
            return fallbackItem;
        } finally {
            writeLock.unlock();
        }
    }

    public Menu withItem(@NonNull MenuItem menuItem) {
        writeLock.lock();
        try {
            Optional<MenuItem> foundItem = getItem(menuItem.getId());

            if (foundItem.isPresent()) {
                return this;
            }

            if (menuItem.getSlots().isEmpty()) {
                if (data.getStoredItem(menuItem.getId()).isPresent()) {
                    return this;
                }

                data.getItemStorage().add(menuItem);
                return this;
            }

            addItem(menuItem);
            return this;
        } finally {
            writeLock.unlock();
        }
    }

    public List<MenuItem> getItems(Inventory inventory, int slot) {
        readLock.lock();
        try {
            return items.stream().filter(item -> item.getSlots().contains(slot) && item.getSourceInventories().contains(inventory)).collect(Collectors.toList());
        } finally {
            readLock.unlock();
        }
    }

    public Optional<MenuItem> getItem(Inventory inventory, int slot) {
        readLock.lock();
        try {
            List<MenuItem> items = getItems(inventory, slot);
            return items.isEmpty() ? Optional.empty() : Optional.of(items.getFirst());
        } finally {
            readLock.unlock();
        }
    }

    public Optional<MenuInventory> generateNewInventory() {
        writeLock.lock();
        try {
            if (data.getMaxInventories() != -1 && inventories.size() >= data.getMaxInventories()) {
                return Optional.empty();
            }

            List<Integer> validSlots = new ArrayList<>(data.getValidSlots());

            if (validSlots.isEmpty()) {
                for (int i = 0; i < (items.size() > size - 1 ? size - 9 : size); i++) {
                    validSlots.add(i);
                }
            }

            List<TagResolver.Single> placeholders = new ArrayList<>();
            placeholders.addAll(data.getPlaceholders());

            placeholders.add(Placeholder.unparsed("pages", inventories.size() + 1 + ""));
            placeholders.add(Placeholder.unparsed("page", inventories.size() + 1 + ""));

            AtomicReference<List<Integer>> availableSlots = new AtomicReference<>(new ArrayList<>(validSlots));
            Inventory inventory = Bukkit.createInventory(this, size, MessageUtil.deserializeToSpigot(title, placeholders.toArray(new TagResolver.Single[]{})));

            List<MenuItem> itemCache = new ArrayList<>();

            getItem("Next-Page-Item").ifPresentOrElse(nextPageItem -> {
                nextPageItem.setLocked(false);

                nextPageItem.saveSlots(false);
                nextPageItem.setMenu(this);
                nextPageItem.withSourceInventory(inventory);
                nextPageItem.withSlots(getData().getMetadata().getValueAsOrDefault("Next-Page-Slots", new ArrayList<>()));
                nextPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));

                if(data.hasFlag(MenuFlag.DISABLE_UPDATES)) {
                    nextPageItem.getSlots().forEach(slot -> inventory.setItem(slot, nextPageItem.getItem().clone()));
                }

                nextPageItem.setLocked(true);
            }, () -> {
                getData().getStoredItem("Next-Page-Item").ifPresent(nextPageItem -> {
                    nextPageItem.setLocked(false);
                    nextPageItem.saveSlots(false);
                    nextPageItem.setMenu(this);
                    nextPageItem.withSourceInventory(inventory);

                    if (!data.hasFlag(MenuFlag.ALWAYS_DISPLAY_NAV)) {
                        nextPageItem.withViewRequirement(new NextPageViewRequirement());
                    }

                    nextPageItem.withAction(event -> {
                        Inventory currentInventory = event.getClickedInventory();
                        int nextIndex = getInventoryIndex(currentInventory) + 1;

                        if (nextIndex > getInventories().size() - 1) {
                            return;
                        }

                        event.getWhoClicked().openInventory(getInventories().get(nextIndex).getInventory());
                    });

                    nextPageItem.withSlots(getData().getMetadata().getValueAsOrDefault("Next-Page-Slots", new ArrayList<>()));
                    nextPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));
                    nextPageItem.setLocked(true);
                    nextPageItem.setGlobal(false);

                    if(data.hasFlag(MenuFlag.DISABLE_UPDATES)) {
                        nextPageItem.getSlots().forEach(slot -> inventory.setItem(slot, nextPageItem.getItem().clone()));
                    }

                    itemCache.add(nextPageItem);
                });
            });

            getItem("Previous-Page-Item").ifPresentOrElse(previousPageItem -> {
                previousPageItem.setLocked(false);

                previousPageItem.saveSlots(false);
                previousPageItem.setMenu(this);
                previousPageItem.withSourceInventory(inventory);
                previousPageItem.withSlots(getData().getMetadata().getValueAsOrDefault("Previous-Page-Slots", new ArrayList<>()));
                previousPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));

                if(data.hasFlag(MenuFlag.DISABLE_UPDATES)) {
                    previousPageItem.getSlots().forEach(slot -> inventory.setItem(slot, previousPageItem.getItem().clone()));
                }

                previousPageItem.setLocked(true);
            }, () -> {
                getData().getStoredItem("Previous-Page-Item").ifPresent(previousPageItem -> {
                    previousPageItem.setLocked(false);
                    previousPageItem.saveSlots(false);
                    previousPageItem.setMenu(this);
                    previousPageItem.withSourceInventory(inventory);

                    if (!data.hasFlag(MenuFlag.ALWAYS_DISPLAY_NAV)) {
                        previousPageItem.withViewRequirement(new PreviousPageViewRequirement());
                    }

                    previousPageItem.withAction(event -> {
                        Inventory currentInventory = event.getClickedInventory();
                        int previousIndex = getInventoryIndex(currentInventory) - 1;

                        if (previousIndex <= -1) {
                            return;
                        }

                        event.getWhoClicked().openInventory(getInventories().get(previousIndex).getInventory());
                    });

                    previousPageItem.withSlots(getData().getMetadata().getValueAsOrDefault("Previous-Page-Slots", new ArrayList<>()));
                    previousPageItem.getSlots().forEach(slot -> availableSlots.get().remove(slot));
                    previousPageItem.setLocked(true);
                    previousPageItem.setGlobal(false);

                    if(data.hasFlag(MenuFlag.DISABLE_UPDATES)) {
                        previousPageItem.getSlots().forEach(slot -> inventory.setItem(slot, previousPageItem.getItem().clone()));
                    }

                    itemCache.add(previousPageItem);
                });
            });

            items.forEach(menuItem -> {
                if (menuItem.getItem() == null && menuItem.getItemUpdateProvider() == null) {
                    return;
                }

                ItemStack item = menuItem.getItemUpdateProvider() == null ? menuItem.getItem().clone() : menuItem.getItemUpdateProvider().requestItem(menuItem);

                if (!data.getPlaceholders().isEmpty()) {
                    List<TagResolver.Single> allPlaceholders = new ArrayList<>(data.getPlaceholders());
                    ItemStackUtil.updateItem(item, allPlaceholders.toArray(new TagResolver.Single[]{}));
                }

                if(!menuItem.getSourceInventories().isEmpty() && !menuItem.isGlobal()) {
                    return;
                }

                if (!menuItem.getSlots().isEmpty()) {
                    menuItem.withSourceInventory(inventory);
                    menuItem.getSlots().forEach(slot -> {
                        availableSlots.get().removeAll(Collections.singletonList(slot));

                        if (menuItem.getViewRequirements().stream().anyMatch(requirement -> !requirement.canView(inventory, menuItem, this))) {
                            return;
                        }

                        inventory.setItem(slot, item);
                    });

                    return;
                }

                if(availableSlots.get().isEmpty()) {
                    return;
                }

                int slot = availableSlots.get().getFirst();
                availableSlots.get().removeAll(Collections.singletonList(slot));

                menuItem.withSourceInventory(inventory);
                menuItem.getSlots().add(slot);

                if (menuItem.getViewRequirements().stream().anyMatch(requirement -> !requirement.canView(inventory, menuItem, this))) {
                    return;
                }

                inventory.setItem(slot, item);
            });

            items.addAll(itemCache);

            data.getFillerItem().ifPresent(fillerItem -> {
                if (fillerItem.getItem() == null || fillerItem.getItem().getType().isAir()) {
                    return;
                }

                fillerItem.setLocked(false);
                fillerItem.saveSlots(false);

                for (int i = 0; i < inventory.getSize(); i++) {
                    if (fillerItem.getSlots().contains(i) || inventory.getItem(i) != null || getData().getValidSlots().contains(i)) {
                        continue;
                    }

                    if(!fillerItem.getSourceInventories().contains(inventory)) {
                        fillerItem.withSourceInventory(inventory);
                    }

                    fillerItem.withSlot(i);
                    inventory.setItem(i, fillerItem.getItem());
                }

                fillerItem.setLocked(true);

                if(fillerItem.getSourceInventories().isEmpty()) {
                    return;
                }

                addItem(fillerItem);
            });

            return Optional.of(new MenuInventory(inventory, title));
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        writeLock.lock();
        try {
            if (inventories.isEmpty()) {
                generateInventories();
            }

            return inventories.getFirst().getInventory();
        } finally {
            writeLock.unlock();
        }
    }

    public void open(@NonNull Player player, @NonNull JavaPlugin plugin, int page) {
        writeLock.lock();
        try {
            if (inventories.isEmpty()) {
                generateInventories();
            }

            if (page > inventories.size()) {
                return;
            }

            MenuInventory inventory = inventories.get(page - 1);
            Bukkit.getScheduler().runTaskLater(plugin, () -> player.openInventory(inventory.getInventory()), 1);
        } finally {
            writeLock.unlock();
        }
    }

    public void open(@NonNull Player player, @NonNull JavaPlugin plugin) {
        open(player, plugin, 1);
    }

    public List<MenuInventory> getInventories() {
        writeLock.lock();
        try {
            if (inventories.isEmpty()) {
                generateInventories();
            }

            return inventories;
        } finally {
            writeLock.unlock();
        }
    }

    private void generateInventories() {
        writeLock.lock();
        try {
            for (int i = 0; i < data.getMinInventories(); i++) {
                generateNewInventory().ifPresent(inventories::add);
            }
        } finally {
            writeLock.unlock();
        }
    }

    public Inventory getGlobalMenu() {
        writeLock.lock();
        try {
            if (inventories.isEmpty()) {
                generateInventories();
            }

            return inventories.getFirst().getInventory();
        } finally {
            writeLock.unlock();
        }
    }

    public MenuInventory getMenuInventory(Inventory inventory) {
        readLock.lock();
        try {
            return inventories.stream().filter(mInventory -> mInventory.getInventory().equals(inventory)).findFirst().orElse(null);
        } finally {
            readLock.unlock();
        }
    }

    public int getInventoryIndex(Inventory inventory) {
        readLock.lock();
        try {
            MenuInventory menuInventory = getMenuInventory(inventory);

            if (menuInventory == null) {
                return -1;
            }

            return inventories.indexOf(menuInventory);
        } finally {
            readLock.unlock();
        }
    }

    public long getTotalCapacity() {
        if (data.getMaxInventories() == -1) {
            return -1;
        }

        return data.getValidSlots().size() * 64L * data.getMaxInventories();
    }

    public long getCurrentCapacity() {
        readLock.lock();
        try {
            long currentCapacity = 0;

            for (MenuInventory menuInventory : inventories) {
                Inventory inventory = menuInventory.getInventory();

                for (Integer validSlot : data.getValidSlots()) {
                    ItemStack slotItem = inventory.getItem(validSlot);

                    if (slotItem == null || slotItem.getType().isAir()) {
                        continue;
                    }

                    currentCapacity += slotItem.getAmount();
                }
            }

            return currentCapacity;
        } finally {
            readLock.unlock();
        }
    }

    public Pair<Inventory, Integer> getNextAvailableSlot() {
        writeLock.lock();
        try {
            if (inventories.isEmpty()) {
                generateInventories();
            }

            if(inventories.stream().noneMatch(MenuInventory::hasSpace)) {
                Optional<MenuInventory> optionalMenuInventory = generateNewInventory();
                optionalMenuInventory.ifPresent(inventories::add);

                return optionalMenuInventory.map(menuInventory -> new Pair<>(menuInventory.getInventory(), data.getValidSlots().getFirst())).orElse(null);
            }

            for (MenuInventory inventory : inventories) {
                if (data.getValidSlots().stream().noneMatch(slot -> inventory.getInventory().getItem(slot) == null)) {
                    continue;
                }

                for (int validSlot : data.getValidSlots()) {
                    if (inventory.getInventory().getItem(validSlot) != null) {
                        continue;
                    }

                    if (getItem(inventory.getInventory(), validSlot).isPresent()) {
                        continue;
                    }

                    return new Pair<>(inventory.getInventory(), validSlot);
                }
            }

            return null;
        } finally {
            writeLock.unlock();
        }
    }

    public HashMap<Inventory, HashMap<Integer, ItemStack>> getVanillaItems() {
        readLock.lock();
        try {
            if (inventories == null || inventories.isEmpty()) {
                return new HashMap<>();
            }

            HashMap<Inventory, HashMap<Integer, ItemStack>> vanillaItems = new HashMap<>();

            for (MenuInventory menuInventory : inventories) {
                Inventory inventory = menuInventory.getInventory();
                HashMap<Integer, ItemStack> itemMap = vanillaItems.getOrDefault(inventory, new HashMap<>());


                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    ItemStack item = inventory.getItem(slot);

                    if (item == null || item.getType() == Material.AIR) {
                        continue;
                    }

                    if (getItem(inventory, slot).isPresent()) {
                        continue;
                    }

                    itemMap.put(slot, item);
                }

                if (itemMap.isEmpty()) {
                    continue;
                }

                vanillaItems.put(inventory, itemMap);
            }

            return vanillaItems;
        } finally {
            readLock.unlock();
        }
    }

    public void saveToFile(@NonNull File directory) {
        File file = new File(directory, String.format(MENU_CONFIG_PATH, getId()));

        if(file.exists()) {
            return;
        }

        File parent = file.getParentFile();

        if (!parent.exists()) {
            parent.mkdirs();
        }

        YamlConfiguration config = new YamlConfiguration();
        SERIALIZER.serialize(config, this);

        try {
            config.save(file);
        } catch (IOException e) {
            Bukkit.getLogger().warning("Failed to save menu '" + id + "'!");
            e.printStackTrace();
        }
    }

    public boolean saveToConfig(@NonNull ConfigurationSection section) {
        return saveToConfig(section, false);
    }

    public boolean saveToConfig(@NonNull ConfigurationSection section, boolean overwrite) {
        readLock.lock();
        try {
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
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public Menu clone() {
        readLock.lock();
        try {
            List<MenuItem> items = new ArrayList<>();
            this.items.forEach(item -> items.add(item.clone()));
            return new Menu(id, title, size, data.clone(), items, new ArrayList<>());
        } finally {
            readLock.unlock();
        }
    }

}