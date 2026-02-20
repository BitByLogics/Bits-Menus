package net.bitbylogic.menus.action;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import net.bitbylogic.utils.RichTextUtil;
import net.bitbylogic.utils.message.MessageUtil;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;

@Getter
@AllArgsConstructor
public enum InternalClickAction {

    RUN_CONSOLE_COMMAND((event, args) -> {
        for (String command : RichTextUtil.getRichText(args, 0)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%player%", event.getWhoClicked().getName()));
        }
    }),
    RUN_PLAYER_COMMAND((event, args) -> {
        for (String command : RichTextUtil.getRichText(args, 0)) {
            ((Player) event.getWhoClicked()).performCommand(command.replace("%player%", event.getWhoClicked().getName()));
        }
    }),
    SEND_MESSAGE((event, args) -> {
        for (String message : RichTextUtil.getRichText(args, 0)) {
            MessageUtil.send(event.getWhoClicked(), MessageUtil.deserialize(message, Placeholder.unparsed("player", event.getWhoClicked().getName())));
        }
    }),
    CLOSE_INVENTORY((event, args) -> event.getWhoClicked().closeInventory());

    private final Action action;

    public static InternalClickAction parseType(String name) {
        for (InternalClickAction type : values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return null;
    }

    public interface Action {

        void onClick(@NonNull InventoryClickEvent event, @NonNull String args);

    }

}
