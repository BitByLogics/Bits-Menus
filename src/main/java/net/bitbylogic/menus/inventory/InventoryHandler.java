package net.bitbylogic.menus.inventory;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.bitbylogic.menus.Menu;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class InventoryHandler {

    private final @NonNull Menu menu;

    @Getter(AccessLevel.NONE)
    private final List<MenuInventory> inventories = new ArrayList<>();

}
