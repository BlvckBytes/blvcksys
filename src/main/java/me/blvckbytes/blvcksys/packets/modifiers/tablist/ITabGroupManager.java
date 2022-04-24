package me.blvckbytes.blvcksys.packets.modifiers.tablist;

import org.bukkit.entity.Player;

import java.util.Optional;

public interface ITabGroupManager {

  /**
   * Remove a player from their current group
   * @param p Player to remove
   */
  void resetPlayerGroup(Player p);

  /**
   * Set a player's group membership
   * @param group Target group
   * @param p Player to add to that group
   */
  void setPlayerGroup(TabListGroup group, Player p);

  /**
   * Get a group by it's name (ignores casing)
   * @param name Name of the group
   * @return Target group
   */
  Optional<TabListGroup> getGroup(String name);
}
