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
}
