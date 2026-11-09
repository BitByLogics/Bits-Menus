package net.bitbylogic.menus.item;

import lombok.NonNull;
import net.bitbylogic.menus.action.InternalClickAction;
import net.bitbylogic.utils.config.ConfigSerializer;
import net.bitbylogic.utils.item.ItemStackUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MenuItemSerializer implements ConfigSerializer<MenuItem> {

    @Override
    public Optional<MenuItem> deserialize(@NonNull ConfigurationSection section) {
        String id = section.getName();
        ItemStack item = ItemStackUtil.getFromConfig(section);
        MenuItem menuItem = new MenuItem(id).item(item).updatable(section.getBoolean("Update", false));

        for (String action : section.getStringList("Actions")) {
            String[] data = action.split(":", 2);
            InternalClickAction type = InternalClickAction.parseType(data[0]);

            if (type == null) {
                Bukkit.getLogger().warning("Unknown action '" + data[0] + "' on menu item '" + id + "', skipping it.");
                continue;
            }

            String actionData = "";

            if (data.length > 1) {
                actionData = data[1];
            }

            menuItem.withInternalAction(type, actionData);
        }

        ConfigurationSection metaDataSection = section.getConfigurationSection("Metadata");

        if (metaDataSection != null) {
            for (String metaKey : metaDataSection.getKeys(false)) {
                menuItem.withMetadata(metaKey, metaDataSection.get(metaKey, ""));
            }
        }

        if (section.getBoolean("Filler", false)) {
            menuItem.filler(true);
        }

        List<Integer> slots = section.getIntegerList("Slots");

        if (!slots.isEmpty()) {
            menuItem.withSlots(slots);
            return Optional.of(menuItem);
        }

        int slot = section.getInt("Slot", -1);

        if (slot != -1) {
            menuItem.withSlot(slot);
        }

        return Optional.of(menuItem);
    }

    @Override
    public ConfigurationSection serialize(@NonNull ConfigurationSection section, @NonNull MenuItem menuItem) {
        if (menuItem.isUpdatable()) {
            section.set("Update", true);
        }

        List<String> actions = new ArrayList<>();
        menuItem.getInternalActions().forEach((internalClickAction, data) -> actions.add(internalClickAction.name() + ":" + data));

        if (!actions.isEmpty()) {
            section.set("Actions", actions);
        }

        menuItem.getMetadata().forEach((key, value) -> section.set("Metadata." + key, value));

        if (menuItem.isFiller()) {
            section.set("Filler", true);
        }

        if (menuItem.getItem() != null) {
            ItemStackUtil.saveToConfig(section, menuItem.getItem());
        }

        if (menuItem.getSlots().isEmpty()) {
            return section;
        }

        if (menuItem.getSlots().size() == 1) {
            section.set("Slot", menuItem.getSlots().getFirst());
            return section;
        }

        section.set("Slots", menuItem.getSlots());
        return section;
    }

}
