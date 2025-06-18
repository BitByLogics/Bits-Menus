package net.bitbylogic.menus.requirement;

import lombok.NonNull;
import org.bukkit.entity.Player;

public interface ClickRequirement {

    boolean canClick(@NonNull Player player);

}
