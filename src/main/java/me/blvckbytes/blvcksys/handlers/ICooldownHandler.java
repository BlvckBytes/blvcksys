package me.blvckbytes.blvcksys.handlers;

import org.bukkit.entity.Player;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 07/03/2022

  Public interfaces which the cooldown handler provides to other consumers.
 */
public interface ICooldownHandler {

  /**
   * Get the remaining time in seconds of a specific cooldown
   * @param p Target player
   * @param cooldownable Cooldownable to check for
   * @return Remaining time in seconds, empty if there is no active cooldown
   */
  Optional<Long> getCooldownRemaining(Player p, ICooldownable cooldownable);

  /**
   * Create a new cooldown for a specific player on a cooldownable
   * @param p Target player
   * @param cooldownable Target cooldownable
   */
  void createCooldownFor(Player p, ICooldownable cooldownable);

}
