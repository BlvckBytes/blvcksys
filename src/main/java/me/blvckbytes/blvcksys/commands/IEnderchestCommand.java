package me.blvckbytes.blvcksys.commands;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/24/2022

  Public interfaces which the enderchest command provides to other consumers.
*/
public interface IEnderchestCommand {

  /**
   * Open a player's enderchest programmatically
   * @param executor The executing player
   */
  void openEnderchest(Player executor);
}
