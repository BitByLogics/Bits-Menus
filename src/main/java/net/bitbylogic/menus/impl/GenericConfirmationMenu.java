package net.bitbylogic.menus.impl;

import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.menus.Menu;
import net.bitbylogic.menus.action.ClickAction;
import net.bitbylogic.menus.data.MenuData;
import net.bitbylogic.utils.Placeholder;
import net.bitbylogic.utils.item.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Getter
public class GenericConfirmationMenu {

    public GenericConfirmationMenu(@NonNull Player player, @NonNull ConfigurationSection menuSection, @NonNull String question, @NonNull Consumer<Void> confirmConsumer,
                                   @NonNull Consumer<Void> cancelConsumer, @NonNull Consumer<Void> closeConsumer) {
        Placeholder questionPlaceholder = new Placeholder("%question%", question);

        Menu menu = Menu.getFromConfig(menuSection)
                .orElse(new Menu(
                        "Confirmation-Menu",
                        "%question%",
                        27,
                        new MenuData()
                                .withModifier(questionPlaceholder)
                ));

        ClickAction confirmAction = event -> {
            if (menu.getData().getMetadata().containsKey("completed")) {
                return;
            }

            confirmConsumer.accept(null);
            menu.getData().getMetadata().put("completed", "true");
            event.getWhoClicked().closeInventory();
        };

        ClickAction cancelAction = event -> {
            if (menu.getData().getMetadata().containsKey("completed")) {
                return;
            }

            cancelConsumer.accept(null);
            menu.getData().getMetadata().put("completed", "true");
            event.getWhoClicked().closeInventory();
        };

        menu.getData().setCloseAction(event -> {
            if (menu.getData().getMetadata().containsKey("completed")) {
                return;
            }

            closeConsumer.accept(null);
            menu.getData().getMetadata().put("completed", "true");
        });

        menu.getItemOrCreate("Confirm")
                .withSlot(10)
                .item(ItemBuilder.of(Material.LIME_STAINED_GLASS_PANE).name("&a&lConfirm").build())
                .withAction(confirmAction);

        menu.getItemOrCreate("GreenPane")
                .withSlots(new ArrayList<>(List.of(0, 1, 2, 9, 11, 18, 19, 20)))
                .item(ItemBuilder.of(Material.LIME_STAINED_GLASS_PANE).name("&aClick To Confirm").build())
                .withAction(confirmAction);

        menu.getItemOrCreate("Cancel")
                .withSlot(16)
                .item(ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).name("&c&lCancel").build())
                .withAction(cancelAction);

        menu.getItemOrCreate("RedPane")
                .withSlots(new ArrayList<>(List.of(6, 7, 8, 15, 17, 23, 25, 26)))
                .item(ItemBuilder.of(Material.RED_STAINED_GLASS_PANE).name("&c&lClick To Cancel").build())
                .withAction(cancelAction);

        menu.getItemOrCreate("Info-Item")
                .withSlot(13)
                .item(ItemBuilder.of(Material.PLAYER_HEAD).name("&a%question%").skullName("MHF_Question").build());
        
        menu.saveToConfig(menuSection);

        player.openInventory(menu.getInventory());
    }

}
