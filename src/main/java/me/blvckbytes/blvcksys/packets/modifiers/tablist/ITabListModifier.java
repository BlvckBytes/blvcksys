package me.blvckbytes.blvcksys.packets.modifiers.tablist;

import org.bukkit.entity.Player;

import java.util.Optional;

public interface ITabListModifier {

  /**
   * Get a player's assigned tablist group
   * @param p Target player
   * @return Currently assigned group
   */
  Optional<TabListGroup> getPlayerGroup(Player p);
}
