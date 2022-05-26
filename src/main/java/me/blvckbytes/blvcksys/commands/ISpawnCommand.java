package me.blvckbytes.blvcksys.commands;

import org.bukkit.Location;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/26/2022

  Public interfaces which the spawn command provides to other consumers.
*/
public interface ISpawnCommand {

  /**
   * Get the spawn point location
   */
  Optional<Location> getSpawn();
}
