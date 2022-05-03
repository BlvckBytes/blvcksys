package me.blvckbytes.blvcksys.events;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/03/2022

  Public interfaces which the teleport listener provides to other consumers.
*/
public interface ITeleportListener {

  /**
   * Get the next location in a player's location-history
   * @param p Target player
   * @return Location, empty if there is no next
   */
  Optional<Location> getHistoryNext(Player p);

  /**
   * Get the previous location in a player's location-history
   * @param p Target player
   * @return Location, empty if there is no previous
   */
  Optional<Location> getHistoryPrevious(Player p);
}
