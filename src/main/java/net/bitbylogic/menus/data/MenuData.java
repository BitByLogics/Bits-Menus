package net.bitbylogic.menus.data;

import lombok.*;
import net.bitbylogic.menus.MenuCloseAction;
import net.bitbylogic.menus.MenuFlag;
import net.bitbylogic.menus.action.ClickAction;
import net.bitbylogic.menus.item.MenuItem;
import net.bitbylogic.utils.GenericHashMap;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Getter
@Setter
@AllArgsConstructor
@RequiredArgsConstructor
public class MenuData implements Cloneable {

    private @Nullable MenuCloseAction closeAction;
    private @Nullable ClickAction externalClickAction;
    private @Nullable ClickAction clickAction;

    private int minInventories = 1;
    private int maxInventories = -1;

    private final @NonNull List<MenuItem> itemStorage;
    private final @NonNull List<MenuFlag> flags;
    private final @NonNull List<Integer> validSlots;

    private final @NonNull List<TagResolver.Single> placeholders;

    private final @NonNull GenericHashMap<String, Object> metadata;

    public MenuData() {
        this.itemStorage = new ArrayList<>();
        this.flags = new ArrayList<>();
        this.validSlots = new ArrayList<>();

        this.placeholders = new ArrayList<>();

        this.metadata = new GenericHashMap<>();
    }

    public MenuData withCloseAction(@NonNull MenuCloseAction closeAction) {
        this.closeAction = closeAction;
        return this;
    }

    public MenuData withExternalClickAction(@NonNull ClickAction externalClickAction) {
        this.externalClickAction = externalClickAction;
        return this;
    }

    public MenuData withClickAction(@NonNull ClickAction clickAction) {
        this.clickAction = clickAction;
        return this;
    }

    public MenuData withMinInventories(int minInventories) {
        this.minInventories = minInventories;
        return this;
    }

    public MenuData withMaxInventories(int maxInventories) {
        this.maxInventories = maxInventories;
        return this;
    }

    public MenuData withStoredItem(@NonNull MenuItem menuItem) {
        this.itemStorage.add(menuItem);
        return this;
    }

    public MenuData withStoredItems(@NonNull List<MenuItem> storedItems) {
        this.itemStorage.addAll(storedItems);
        return this;
    }

    public MenuData withFlag(@NonNull MenuFlag flag) {
        flags.add(flag);
        return this;
    }

    public MenuData withFlags(@NonNull List<MenuFlag> flags) {
        this.flags.addAll(flags);
        return this;
    }

    public MenuData withValidSlots(@NonNull List<Integer> validSlots) {
        this.validSlots.addAll(validSlots);
        return this;
    }

    public MenuData withModifier(@NonNull TagResolver.Single modifier) {
        this.placeholders.add(modifier);
        return this;
    }

    public MenuData withPlaceholders(@NonNull List<TagResolver.Single> modifiers) {
        this.placeholders.addAll(modifiers);
        return this;
    }

    public MenuData withMetadata(@NonNull String key, @NonNull Object value) {
        metadata.put(key, value);
        return this;
    }

    public MenuData withMetadata(@NonNull GenericHashMap<String, Object> metadata) {
        this.metadata.putAll(metadata);
        return this;
    }

    public boolean hasFlag(@NonNull MenuFlag flag) {
        return flags.contains(flag);
    }

    public void addPlaceholder(@NonNull TagResolver.Single placeholder) {
        placeholders.add(placeholder);
    }

    public Optional<MenuItem> getStoredItem(String id) {
        return itemStorage.stream().filter(item -> item.getId().equalsIgnoreCase(id)).findFirst();
    }

    public MenuItem getStoredItemOrCreate(String id) {
        Optional<MenuItem> optionalItem = itemStorage.stream().filter(item -> item.getId().equalsIgnoreCase(id)).findFirst();

        if(optionalItem.isPresent()) {
            return optionalItem.get();
        }

        MenuItem fallbackItem = new MenuItem(id);

        itemStorage.add(fallbackItem);
        return fallbackItem;
    }

    public Optional<MenuItem> getFillerItem() {
        return itemStorage.stream().filter(MenuItem::isFiller).findFirst();
    }

    @Override
    public MenuData clone() {
        List<MenuItem> itemStorage = new ArrayList<>();
        this.itemStorage.forEach(item -> itemStorage.add(item.clone()));

        GenericHashMap<String, Object> metadata = new GenericHashMap<>();
        metadata.putAll(this.metadata);

        return new MenuData(closeAction, externalClickAction, clickAction, minInventories,
                maxInventories, itemStorage, new ArrayList<>(flags), new ArrayList<>(validSlots),
                new ArrayList<>(placeholders), metadata);
    }
}
