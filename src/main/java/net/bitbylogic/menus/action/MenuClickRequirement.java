package net.bitbylogic.menus.action;

import lombok.NonNull;
import org.bukkit.entity.Player;

public interface MenuClickRequirement {

    boolean canClick(@NonNull Player player);

}
