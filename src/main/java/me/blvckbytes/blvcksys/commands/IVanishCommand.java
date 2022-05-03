package me.blvckbytes.blvcksys.commands;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/03/2022

  Public interfaces which the vanish command provides to other consumers.
*/
public interface IVanishCommand {

  /**
   * Checks whether a player is currently vanished
   * @param p Target player
   * @return Vanished state
   */
  boolean isVanished(Player p);
}
