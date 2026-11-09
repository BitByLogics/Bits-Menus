package net.bitbylogic.menus.inventory;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import net.bitbylogic.menus.item.MenuItem;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.Inventory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class MenuInventory {

    private final Inventory inventory;

    @Setter
    private Component title;

    @Getter(AccessLevel.NONE)
    private final Map<Integer, List<MenuItem>> placements = new HashMap<>();

    public MenuInventory(@NonNull Inventory inventory, @NonNull Component title) {
        this.inventory = inventory;
        this.title = title;
    }

    public void place(int slot, @NonNull MenuItem item) {
        placements.computeIfAbsent(slot, ignored -> new ArrayList<>()).add(item);
    }

    public boolean hasItem(int slot) {
        return placements.containsKey(slot);
    }

    public List<MenuItem> getItems(int slot) {
        List<MenuItem> items = placements.get(slot);

        if (items == null) {
            return List.of();
        }

        return List.copyOf(items);
    }

    public Map<Integer, List<MenuItem>> getPlacements() {
        Map<Integer, List<MenuItem>> copy = new HashMap<>();
        placements.forEach((slot, items) -> copy.put(slot, List.copyOf(items)));
        return copy;
    }

    public boolean hasSpace(@NonNull List<Integer> validSlots) {
        for (int slot : validSlots) {
            if (inventory.getItem(slot) == null && !placements.containsKey(slot)) {
                return true;
            }
        }

        return false;
    }

}
