package me.blvckbytes.blvcksys.handlers;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  Public interfaces which the combatlog handler provides to other consumers.
 */
public interface ICombatLogHandler {

  /**
   * Checks whether the given player is currently in combat
   * @param p Target player
   */
  boolean isInCombat(Player p);
}
