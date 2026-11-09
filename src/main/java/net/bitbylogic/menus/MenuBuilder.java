package net.bitbylogic.menus;

import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.menus.data.MenuData;
import net.bitbylogic.menus.item.MenuItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MenuBuilder {

    private final String id;

    private Component title = Component.text("Inventory");
    private int size = 9;

    private final List<MenuItem> items = new ArrayList<>();
    private MenuData data = new MenuData();

    private Menu menu;

    public MenuBuilder(@NonNull String id) {
        this.id = id;
    }

    public MenuBuilder(@NonNull String id, @NonNull Component title, @NonNull MenuRows menuRows) {
        this(id, title, menuRows.getSize());
    }

    public MenuBuilder(@NonNull String id, @NonNull Component title, int size) {
        this.id = id;
        this.title = title;
        this.size = size;
    }

    public MenuBuilder title(Component title) {
        this.title = title;
        return this;
    }

    public MenuBuilder size(int size) {
        this.size = size;
        return this;
    }

    public MenuBuilder withItem(@NonNull MenuItem item) {
        if(item.getSlots().isEmpty()) {
            data.getItemStorage().add(item);
            return this;
        }

        items.add(item);
        return this;
    }

    public MenuBuilder withItems(List<MenuItem> items) {
        for (MenuItem item : items) {
            if(item.getSlots().isEmpty()) {
                data.getItemStorage().add(item);
                continue;
            }

            this.items.add(item);
        }
        return this;
    }

    public MenuBuilder data(MenuData data) {
        this.data = data;
        return this;
    }

    public Menu build() {
        if (size < 9 || size > 54 || size % 9 != 0) {
            int clampedSize = Math.max(9, Math.min(54, (size / 9) * 9));

            Bukkit.getLogger().warning("Invalid size " + size + " for menu '" + id + "', using " + clampedSize + " instead.");
            size = clampedSize;
        }

        menu = new Menu(id, title, size, data);
        items.forEach(menu::addItem);

        return menu;
    }

}
