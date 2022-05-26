package me.blvckbytes.blvcksys.handlers;

import org.bukkit.entity.Player;

import java.util.Optional;

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

  /**
   * Get the last damager within a combatlog session of a given player
   * @param p Target player
   * @return Optional last damager, empty if there was none
   */
  Optional<Player> getLastDamager(Player p);
}
