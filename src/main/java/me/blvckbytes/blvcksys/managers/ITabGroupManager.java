package me.blvckbytes.blvcksys.managers;

import me.blvckbytes.blvcksys.packets.modifiers.tablist.TabListGroup;
import org.bukkit.entity.Player;

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
}
