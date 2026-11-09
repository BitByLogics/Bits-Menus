package net.bitbylogic.menus.item;

import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.menus.action.ClickAction;
import net.bitbylogic.menus.action.InternalClickAction;
import net.bitbylogic.menus.action.NextPageAction;
import net.bitbylogic.menus.action.PreviousPageAction;
import net.bitbylogic.menus.requirement.ClickRequirement;
import net.bitbylogic.menus.view.MenuViewRequirement;
import net.bitbylogic.menus.view.internal.NextPageViewRequirement;
import net.bitbylogic.menus.view.internal.PreviousPageViewRequirement;
import net.bitbylogic.utils.GenericHashMap;
import net.bitbylogic.utils.cooldown.CooldownUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Getter
public class MenuItem implements Cloneable {

    private static final MenuItemSerializer SERIALIZER = new MenuItemSerializer();

    private final @NotNull String id;

    private final @NotNull List<Integer> slots = new CopyOnWriteArrayList<>();

    private final @NotNull List<ClickAction> actions = new CopyOnWriteArrayList<>();
    private final @NotNull Map<InternalClickAction, String> internalActions = new ConcurrentHashMap<>();

    private final @NotNull List<ClickRequirement> clickRequirements = new CopyOnWriteArrayList<>();
    private final @NotNull List<MenuViewRequirement> viewRequirements = new CopyOnWriteArrayList<>();

    private final @NotNull GenericHashMap<String, Object> metadata = new GenericHashMap<>();

    private @Nullable ItemStack item;
    private @Nullable MenuItemUpdateProvider itemUpdateProvider;

    private boolean filler;
    private boolean updatable;
    private boolean saved = true;

    private int clickCooldownTime = 200;
    private TimeUnit clickCooldownUnit = TimeUnit.MILLISECONDS;

    public MenuItem(@NonNull String id) {
        this.id = id;
    }

    public MenuItem withSlot(int slot) {
        slots.add(slot);
        return this;
    }

    public MenuItem withSlots(@NonNull List<Integer> slots) {
        this.slots.addAll(slots);
        return this;
    }

    public MenuItem withAction(@NonNull ClickAction action) {
        actions.add(action);
        return this;
    }

    public MenuItem withActions(@NonNull List<ClickAction> actions) {
        this.actions.addAll(actions);
        return this;
    }

    public MenuItem withInternalAction(@NonNull InternalClickAction actionType, @NonNull String data) {
        internalActions.put(actionType, data);
        return this;
    }

    public MenuItem withInternalActions(@NonNull Map<InternalClickAction, String> internalActions) {
        this.internalActions.putAll(internalActions);
        return this;
    }

    public MenuItem withClickRequirement(@NonNull ClickRequirement requirement) {
        clickRequirements.add(requirement);
        return this;
    }

    public MenuItem withClickRequirement(@NonNull List<ClickRequirement> clickRequirements) {
        this.clickRequirements.addAll(clickRequirements);
        return this;
    }

    public MenuItem withViewRequirement(@NonNull MenuViewRequirement requirement) {
        viewRequirements.add(requirement);
        return this;
    }

    public MenuItem withViewRequirements(@NonNull List<MenuViewRequirement> viewRequirements) {
        this.viewRequirements.addAll(viewRequirements);
        return this;
    }

    public MenuItem withMetadata(@NonNull String key, @NonNull Object value) {
        metadata.put(key, value);
        return this;
    }

    public MenuItem withMetadata(@NonNull GenericHashMap<String, Object> metadata) {
        this.metadata.putAll(metadata);
        return this;
    }

    public MenuItem item(@Nullable ItemStack item) {
        this.item = item;
        return this;
    }

    public MenuItem filler(boolean filler) {
        this.filler = filler;
        return this;
    }

    public MenuItem updatable(boolean updatable) {
        this.updatable = updatable;
        return this;
    }

    public MenuItem saved(boolean saved) {
        this.saved = saved;
        return this;
    }

    public MenuItem updateProvider(@NonNull MenuItemUpdateProvider updateProvider) {
        this.itemUpdateProvider = updateProvider;
        return this;
    }

    public MenuItem withClickCooldownTime(int cooldownTime) {
        this.clickCooldownTime = cooldownTime;
        return this;
    }

    public MenuItem withClickCooldownUnit(@NonNull TimeUnit unit) {
        this.clickCooldownUnit = unit;
        return this;
    }

    public MenuItem nextPageButton(@NonNull List<Integer> slots) {
        return withSlots(slots)
                .withViewRequirement(new NextPageViewRequirement())
                .withAction(new NextPageAction());
    }

    public MenuItem previousPageButton(@NonNull List<Integer> slots) {
        return withSlots(slots)
                .withViewRequirement(new PreviousPageViewRequirement())
                .withAction(new PreviousPageAction());
    }

    public void onClick(@NonNull InventoryClickEvent event, @NonNull JavaPlugin plugin) {
        if (clickRequirements.stream().anyMatch(requirement -> !requirement.canClick((Player) event.getWhoClicked()))) {
            return;
        }

        if (CooldownUtil.hasCooldown(id + "-" + event.getSlot(), event.getWhoClicked().getUniqueId())) {
            return;
        }

        CooldownUtil.startCooldown(plugin, id + "-" + event.getSlot(), event.getWhoClicked().getUniqueId(), clickCooldownTime, clickCooldownUnit);

        internalActions.forEach((action, data) -> action.getAction().onClick(event, data));
        actions.forEach(action -> action.onClick(event));
    }

    public boolean saveToConfig(@NonNull ConfigurationSection section) {
        return saveToConfig(section, false);
    }

    public boolean saveToConfig(@NonNull ConfigurationSection section, boolean overwrite) {
        if (!overwrite && section.isSet(id) || !saved) {
            return false;
        }

        ConfigurationSection itemSection = section.getConfigurationSection(id);

        if (itemSection == null) {
            itemSection = section.createSection(id);
        }

        SERIALIZER.serialize(itemSection, this);
        return true;
    }

    public static Optional<MenuItem> getFromConfig(@Nullable ConfigurationSection section) {
        if (section == null) {
            return Optional.empty();
        }

        return SERIALIZER.deserialize(section);
    }

    public MenuItem clone() {
        return clone(true);
    }

    public MenuItem clone(boolean cloneActions) {
        MenuItem copy = new MenuItem(id);

        copy.slots.addAll(slots);
        copy.internalActions.putAll(internalActions);
        copy.clickRequirements.addAll(clickRequirements);
        copy.viewRequirements.addAll(viewRequirements);
        copy.metadata.putAll(metadata);

        if (cloneActions) {
            copy.actions.addAll(actions);
        }

        copy.item = item;
        copy.itemUpdateProvider = itemUpdateProvider;
        copy.filler = filler;
        copy.updatable = updatable;
        copy.saved = false;
        copy.clickCooldownTime = clickCooldownTime;
        copy.clickCooldownUnit = clickCooldownUnit;
        return copy;
    }

}
