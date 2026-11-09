package net.bitbylogic.menus;

import lombok.NonNull;
import net.bitbylogic.menus.data.MenuData;
import net.bitbylogic.menus.item.MenuItem;
import net.bitbylogic.utils.config.ConfigSerializer;
import net.bitbylogic.utils.message.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MenuSerializer implements ConfigSerializer<Menu> {

    private static final String NEXT_PAGE_ITEM_ID = "Next-Page-Item";
    private static final String PREVIOUS_PAGE_ITEM_ID = "Previous-Page-Item";
    private static final String NEXT_PAGE_SLOTS_KEY = "Next-Page-Slots";
    private static final String PREVIOUS_PAGE_SLOTS_KEY = "Previous-Page-Slots";

    @Override
    public Optional<Menu> deserialize(@NonNull ConfigurationSection section) {
        MenuBuilder builder = new MenuBuilder(
                section.getString("Id", UUID.randomUUID().toString()),
                MessageUtil.deserialize(section.getString("Title", "Inventory")),
                section.getInt("Size", 9)
        );

        MenuData data = builder.getData();

        data.getValidSlots().addAll(section.getIntegerList("Valid-Slots"));

        ConfigurationSection metaDataSection = section.getConfigurationSection("Metadata");
        List<Integer> nextPageSlots = new ArrayList<>();
        List<Integer> previousPageSlots = new ArrayList<>();

        if (metaDataSection != null) {
            nextPageSlots.addAll(metaDataSection.getIntegerList(NEXT_PAGE_SLOTS_KEY));
            previousPageSlots.addAll(metaDataSection.getIntegerList(PREVIOUS_PAGE_SLOTS_KEY));

            for (String metaKey : metaDataSection.getKeys(false)) {
                if (metaKey.equals(NEXT_PAGE_SLOTS_KEY) || metaKey.equals(PREVIOUS_PAGE_SLOTS_KEY)) {
                    continue;
                }

                data.getMetadata().put(metaKey, metaDataSection.get(metaKey));
            }
        }

        section.getStringList("Flags").forEach(flagName -> {
            Optional<MenuFlag> flag = Arrays.stream(MenuFlag.values())
                    .filter(menuFlag -> menuFlag.name().equalsIgnoreCase(flagName))
                    .findFirst();

            if (flag.isEmpty()) {
                Bukkit.getLogger().warning("Unknown menu flag '" + flagName + "' in menu '" + builder.getId() + "', skipping it.");
                return;
            }

            data.withFlag(flag.get());
        });

        ConfigurationSection itemsSection = section.getConfigurationSection("Items");

        if (itemsSection == null) {
            return Optional.of(builder.build());
        }

        for (String identifier : itemsSection.getKeys(false)) {
            ConfigurationSection itemSection = itemsSection.getConfigurationSection(identifier);

            if (itemSection == null) {
                continue;
            }

            Optional<MenuItem> item = MenuItem.getFromConfig(itemSection);

            if (item.isEmpty()) {
                continue;
            }

            MenuItem menuItem = item.get();

            if (menuItem.getId().equalsIgnoreCase(NEXT_PAGE_ITEM_ID)) {
                builder.withItem(menuItem.nextPageButton(nextPageSlots));
                continue;
            }

            if (menuItem.getId().equalsIgnoreCase(PREVIOUS_PAGE_ITEM_ID)) {
                builder.withItem(menuItem.previousPageButton(previousPageSlots));
                continue;
            }

            builder.withItem(menuItem);
        }

        return Optional.of(builder.build());
    }

    @Override
    public ConfigurationSection serialize(@NonNull ConfigurationSection section, @NonNull Menu menu) {
        section.set("Id", menu.getId());
        section.set("Title", MessageUtil.serialize(menu.getTitle()));
        section.set("Size", menu.getSize());

        MenuData menuData = menu.getData();

        if (!menuData.getValidSlots().isEmpty()) {
            section.set("Valid-Slots", menuData.getValidSlots());
        }

        menuData.getMetadata().forEach((key, value) -> section.set("Metadata." + key, value));

        List<String> flags = new ArrayList<>();
        menuData.getFlags().forEach(menuFlag -> flags.add(menuFlag.name()));

        if (!flags.isEmpty()) {
            section.set("Flags", flags);
        }

        if (menu.getItems().isEmpty() && menuData.getItemStorage().isEmpty()) {
            return section;
        }

        ConfigurationSection itemsSection = section.createSection("Items");

        menu.getItems().forEach(menuItem -> menuItem.saveToConfig(itemsSection));
        menuData.getItemStorage().forEach(menuItem -> menuItem.saveToConfig(itemsSection));

        return section;
    }

}
