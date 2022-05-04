package me.blvckbytes.blvcksys.handlers;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/01/2022

  Public interfaces which the objective handler provides to other consumers.
*/
public interface IObjectiveHandler {

  /**
   * Set a below name flag for a specific player's below name text
   * @param target Target player
   * @param flag Below name flag
   * @param active Whether to add or remove this flag
   */
  void setBelowNameFlag(Player target, BelowNameFlag flag, boolean active);

  /**
   * Update a player's below name score on demand
   * @param target Target player
   */
  void updateBelowName(Player target);

  /**
   * Toggle the sidebar objective's visibility for a specific player
   * @param target Target player
   * @param status Visibility status
   */
  void setSidebarVisibility(Player target, boolean status);

  /**
   * Get a player's current sidebar objective's visibility
   * @param target Target player
   * @return Visibility status
   */
  boolean getSidebarVisibility(Player target);
}
