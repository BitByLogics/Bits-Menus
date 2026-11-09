package net.bitbylogic.menus;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.menus.inventory.MenuInventory;
import net.bitbylogic.menus.item.MenuItem;
import net.bitbylogic.menus.task.MenuScheduler;
import net.bitbylogic.menus.task.MenuUpdateTask;
import net.bitbylogic.menus.view.MenuViewRequirement;
import net.bitbylogic.utils.inventory.InventoryUtil;
import net.bitbylogic.utils.item.ItemStackUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Getter
public class MenuSession implements InventoryHolder {

    private final Menu menu;
    private final MenuUpdateTask updateTask;
    private final List<TagResolver.Single> placeholders = new CopyOnWriteArrayList<>();

    @Getter(AccessLevel.NONE)
    private final List<MenuInventory> pages = new ArrayList<>();

    @Getter(AccessLevel.NONE)
    private final List<UUID> viewers = new CopyOnWriteArrayList<>();

    @Getter(AccessLevel.NONE)
    private final Lock lock = new ReentrantLock();

    public MenuSession(@NonNull Menu menu) {
        this.menu = menu;
        this.updateTask = new MenuUpdateTask(this);
        this.placeholders.addAll(menu.getData().getPlaceholders());
    }

    public MenuSession withPlaceholder(@NonNull TagResolver.Single placeholder) {
        placeholders.add(placeholder);
        return this;
    }

    @Override
    public @NotNull Inventory getInventory() {
        lock.lock();

        try {
            ensurePages();
            return pages.getFirst().getInventory();
        } finally {
            lock.unlock();
        }
    }

    public List<MenuInventory> getInventories() {
        lock.lock();

        try {
            ensurePages();
            return List.copyOf(pages);
        } finally {
            lock.unlock();
        }
    }

    public int getPageCount() {
        lock.lock();

        try {
            return pages.size();
        } finally {
            lock.unlock();
        }
    }

    public int getPageIndex(@NonNull Inventory inventory) {
        lock.lock();

        try {
            for (int index = 0; index < pages.size(); index++) {
                if (pages.get(index).getInventory().equals(inventory)) {
                    return index;
                }
            }

            return -1;
        } finally {
            lock.unlock();
        }
    }

    public Optional<MenuInventory> getPage(@NonNull Inventory inventory) {
        lock.lock();

        try {
            return pages.stream().filter(page -> page.getInventory().equals(inventory)).findFirst();
        } finally {
            lock.unlock();
        }
    }

    public List<MenuItem> getItems(@NonNull Inventory inventory, int slot) {
        lock.lock();

        try {
            Optional<MenuInventory> page = getPage(inventory);

            if (page.isEmpty()) {
                return List.of();
            }

            return page.get().getItems(slot);
        } finally {
            lock.unlock();
        }
    }

    public Optional<MenuItem> getItem(@NonNull Inventory inventory, int slot) {
        List<MenuItem> items = getItems(inventory, slot);

        if (items.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(items.getFirst());
    }

    public List<UUID> getViewers() {
        return List.copyOf(viewers);
    }

    public void addViewer(@NonNull UUID viewer) {
        viewers.add(viewer);
    }

    public void removeViewer(@NonNull UUID viewer) {
        viewers.remove(viewer);
    }

    public boolean isVisible(@NonNull MenuInventory page, @NonNull MenuItem item) {
        for (MenuViewRequirement requirement : item.getViewRequirements()) {
            if (!requirement.canView(this, page, item)) {
                return false;
            }
        }

        return true;
    }

    public Optional<MenuInventory> appendPage() {
        lock.lock();

        try {
            if (!canAppendPage()) {
                return Optional.empty();
            }

            MenuInventory page = createPage();
            refresh();
            return Optional.of(page);
        } finally {
            lock.unlock();
        }
    }

    public void appendPageIfFull() {
        lock.lock();

        try {
            if (menu.getData().getMaxInventories() == -1 || pages.isEmpty()) {
                return;
            }

            if (pages.getLast().hasSpace(menu.getData().getValidSlots())) {
                return;
            }

            appendPage();
        } finally {
            lock.unlock();
        }
    }

    public boolean addItem(@NonNull MenuItem item) {
        lock.lock();

        try {
            List<Integer> validSlots = menu.getData().getValidSlots();

            if (validSlots.isEmpty()) {
                Bukkit.getLogger().warning("Menu '" + menu.getId() + "' has no valid slots, unable to add item '" + item.getId() + "'.");
                return false;
            }

            ensurePages();

            MenuInventory page = findPageWithSpace();

            if (page == null) {
                Optional<MenuInventory> appended = appendPage();

                if (appended.isEmpty()) {
                    if (menu.getData().hasFlag(MenuFlag.DEBUG)) {
                        Bukkit.getLogger().warning("Failed to find an available slot for item '" + item.getId() + "' in menu '" + menu.getId() + "'.");
                    }

                    return false;
                }

                page = appended.get();
            }

            for (int slot : validSlots) {
                if (page.getInventory().getItem(slot) != null || page.hasItem(slot)) {
                    continue;
                }

                placeItem(page, slot, item);
                return true;
            }

            return false;
        } finally {
            lock.unlock();
        }
    }

    public boolean setItem(int slot, @NonNull MenuItem item) {
        return setItem(0, slot, item);
    }

    public boolean setItem(int pageIndex, int slot, @NonNull MenuItem item) {
        lock.lock();

        try {
            ensurePages();

            if (pageIndex < 0 || pageIndex >= pages.size()) {
                Bukkit.getLogger().warning("Page " + pageIndex + " of menu '" + menu.getId() + "' does not exist, unable to set item '" + item.getId() + "'.");
                return false;
            }

            if (slot < 0 || slot >= menu.getSize()) {
                Bukkit.getLogger().warning("Slot " + slot + " of item '" + item.getId() + "' is outside of menu '" + menu.getId() + "'.");
                return false;
            }

            placeItem(pages.get(pageIndex), slot, item);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public void addItemStack(@NonNull ItemStack item) {
        lock.lock();

        try {
            ensurePages();

            List<Integer> validSlots = menu.getData().getValidSlots();
            List<MenuInventory> targets = new ArrayList<>(pages);
            int amountLeft = item.getAmount();
            int index = 0;

            while (amountLeft > 0) {
                MenuInventory page;

                if (index < targets.size()) {
                    page = targets.get(index);
                } else {
                    Optional<MenuInventory> appended = appendPage();

                    if (appended.isEmpty()) {
                        return;
                    }

                    page = appended.get();
                }

                index++;

                Inventory inventory = page.getInventory();
                int availableSpace = InventoryUtil.getAvailableSpace(inventory, item, validSlots);

                if (availableSpace <= 0) {
                    continue;
                }

                int amount = Math.min(availableSpace, amountLeft);
                ItemStack portion = item.clone();
                portion.setAmount(amount);

                InventoryUtil.addItem(inventory, portion, validSlots);
                amountLeft -= amount;
            }
        } finally {
            lock.unlock();
        }
    }

    public void refresh() {
        lock.lock();

        try {
            for (MenuInventory page : pages) {
                for (Map.Entry<Integer, List<MenuItem>> entry : page.getPlacements().entrySet()) {
                    for (MenuItem item : entry.getValue()) {
                        refreshSlot(page, entry.getKey(), item);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void refreshItem(@NonNull MenuItem item) {
        lock.lock();

        try {
            for (MenuInventory page : pages) {
                for (Map.Entry<Integer, List<MenuItem>> entry : page.getPlacements().entrySet()) {
                    if (!entry.getValue().contains(item)) {
                        continue;
                    }

                    showItem(page, entry.getKey(), item);
                }
            }
        } finally {
            lock.unlock();
        }
    }

    public void open(@NonNull HumanEntity player) {
        open(player, 1);
    }

    public void open(@NonNull HumanEntity player, int page) {
        Inventory inventory = getPageInventory(page);

        if (inventory == null) {
            return;
        }

        player.openInventory(inventory);
    }

    public void open(@NonNull Player player, @NonNull JavaPlugin plugin) {
        open(player, plugin, 1);
    }

    public void open(@NonNull Player player, @NonNull JavaPlugin plugin, int page) {
        Inventory inventory = getPageInventory(page);

        if (inventory == null) {
            return;
        }

        MenuScheduler.get(plugin).runAtEntityLater(player, () -> player.openInventory(inventory), 1);
    }

    public void openRelativePage(@NonNull HumanEntity player, @NonNull Inventory current, int offset) {
        int targetPage = getPageIndex(current) + offset + 1;

        if (targetPage < 1 || targetPage > getPageCount()) {
            return;
        }

        open(player, targetPage);
    }

    public long getTotalCapacity() {
        if (menu.getData().getMaxInventories() == -1) {
            return -1;
        }

        return menu.getData().getValidSlots().size() * 64L * menu.getData().getMaxInventories();
    }

    public long getCurrentCapacity() {
        lock.lock();

        try {
            long currentCapacity = 0;

            for (MenuInventory page : pages) {
                for (int validSlot : menu.getData().getValidSlots()) {
                    ItemStack slotItem = page.getInventory().getItem(validSlot);

                    if (slotItem == null || slotItem.getType().isAir()) {
                        continue;
                    }

                    currentCapacity += slotItem.getAmount();
                }
            }

            return currentCapacity;
        } finally {
            lock.unlock();
        }
    }

    public Map<Inventory, Map<Integer, ItemStack>> getVanillaItems() {
        lock.lock();

        try {
            Map<Inventory, Map<Integer, ItemStack>> vanillaItems = new HashMap<>();

            for (MenuInventory page : pages) {
                Inventory inventory = page.getInventory();
                Map<Integer, ItemStack> itemMap = new HashMap<>();

                for (int slot = 0; slot < inventory.getSize(); slot++) {
                    ItemStack item = inventory.getItem(slot);

                    if (item == null || item.getType() == Material.AIR) {
                        continue;
                    }

                    if (page.hasItem(slot)) {
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
            lock.unlock();
        }
    }

    private @Nullable Inventory getPageInventory(int page) {
        lock.lock();

        try {
            ensurePages();

            if (page < 1 || page > pages.size()) {
                Bukkit.getLogger().warning("Unable to open page " + page + " of menu '" + menu.getId() + "', it has " + pages.size() + " page(s).");
                return null;
            }

            return pages.get(page - 1).getInventory();
        } finally {
            lock.unlock();
        }
    }

    private void ensurePages() {
        if (!pages.isEmpty()) {
            return;
        }

        createPage();

        for (int count = 1; count < menu.getData().getMinInventories(); count++) {
            if (!canAppendPage()) {
                return;
            }

            createPage();
        }
    }

    private boolean canAppendPage() {
        int maxInventories = menu.getData().getMaxInventories();
        return maxInventories == -1 || pages.size() < maxInventories;
    }

    private @Nullable MenuInventory findPageWithSpace() {
        for (MenuInventory page : pages) {
            if (page.hasSpace(menu.getData().getValidSlots())) {
                return page;
            }
        }

        return null;
    }

    private MenuInventory createPage() {
        Inventory inventory = Bukkit.createInventory(this, menu.getSize(), menu.getTitle());
        MenuInventory page = new MenuInventory(inventory, menu.getTitle());

        pages.add(page);

        for (MenuItem item : menu.getItems()) {
            for (int slot : item.getSlots()) {
                placeItem(page, slot, item);
            }
        }

        fillPage(page);
        return page;
    }

    private void fillPage(MenuInventory page) {
        Optional<MenuItem> fillerItem = menu.getData().getFillerItem();

        if (fillerItem.isEmpty()) {
            return;
        }

        ItemStack fillerStack = fillerItem.get().getItem();

        if (fillerStack == null || fillerStack.getType().isAir()) {
            return;
        }

        Inventory inventory = page.getInventory();

        for (int slot = 0; slot < menu.getSize(); slot++) {
            if (menu.getData().getValidSlots().contains(slot) || page.hasItem(slot) || inventory.getItem(slot) != null) {
                continue;
            }

            placeItem(page, slot, fillerItem.get());
        }
    }

    private void placeItem(MenuInventory page, int slot, MenuItem item) {
        if (slot < 0 || slot >= menu.getSize()) {
            Bukkit.getLogger().warning("Slot " + slot + " of item '" + item.getId() + "' is outside of menu '" + menu.getId() + "'.");
            return;
        }

        page.place(slot, item);
        showItem(page, slot, item);
    }

    private void showItem(MenuInventory page, int slot, MenuItem item) {
        if (!isVisible(page, item)) {
            clearIfShowing(page.getInventory(), slot, item);
            return;
        }

        ItemStack stack = renderItem(item);

        if (stack == null) {
            return;
        }

        page.getInventory().setItem(slot, stack);
    }

    private void refreshSlot(MenuInventory page, int slot, MenuItem item) {
        Inventory inventory = page.getInventory();

        if (!isVisible(page, item)) {
            clearIfShowing(inventory, slot, item);
            return;
        }

        if (item.getItem() == null && item.getItemUpdateProvider() == null) {
            inventory.setItem(slot, null);
            return;
        }

        if (inventory.getItem(slot) != null && !item.isUpdatable()) {
            return;
        }

        ItemStack stack = renderItem(item);

        if (stack == null) {
            return;
        }

        inventory.setItem(slot, stack);
    }

    private void clearIfShowing(Inventory inventory, int slot, MenuItem item) {
        ItemStack current = inventory.getItem(slot);

        if (current == null) {
            return;
        }

        ItemStack itemStack = item.getItem();

        if (itemStack != null && current.getType() != itemStack.getType()) {
            return;
        }

        inventory.setItem(slot, null);
    }

    private @Nullable ItemStack renderItem(MenuItem menuItem) {
        ItemStack stack = null;

        if (menuItem.getItemUpdateProvider() != null) {
            stack = menuItem.getItemUpdateProvider().requestItem(this, menuItem);
        } else if (menuItem.getItem() != null) {
            stack = menuItem.getItem().clone();
        }

        if (stack == null) {
            return null;
        }

        if (!placeholders.isEmpty()) {
            ItemStackUtil.updateItem(stack, placeholders.toArray(new TagResolver.Single[]{}));
        }

        return stack;
    }

}
