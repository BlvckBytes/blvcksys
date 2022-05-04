package me.blvckbytes.blvcksys.commands;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Public interfaces which the ptime command provides to other consumers.
*/
public interface IPTimeCommand {

  /**
   * Set the time of a client to a pre-set shorthand and notify the client
   * @param dispatcher Who dispatched this command
   * @param client Target client
   * @param shorthand Shorthand to set
   */
  void setTime(Player dispatcher, Player client, TimeShorthand shorthand);
}
