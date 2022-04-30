package me.blvckbytes.blvcksys.commands;

import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Public interfaces which the time command provides to other consumers.
*/
public interface ITimeCommand {

  /**
   * Set the time to a pre-set shorthand
   * @param dispatcher Who dispatched this command, leave null for programmatical invocations
   * @param world World to affect
   * @param shorthand Shorthand to set
   */
  void setTime(@Nullable Player dispatcher, World world, TimeShorthand shorthand);
}
