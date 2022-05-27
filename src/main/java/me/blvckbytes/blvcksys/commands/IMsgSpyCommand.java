package me.blvckbytes.blvcksys.commands;

import org.bukkit.entity.Player;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Public interfaces which the msgspy command provides to other consumers.
*/
public interface IMsgSpyCommand {

  /**
   * Get all spies of a target player
   * @param target Target player
   */
  List<Player> getSpies(Player target);
}
