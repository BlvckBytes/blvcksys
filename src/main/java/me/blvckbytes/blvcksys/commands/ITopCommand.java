package me.blvckbytes.blvcksys.commands;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/03/2022

  Public interfaces which the top command provides to other consumers.
*/
public interface ITopCommand {

  /**
   * Search through a player's current Y-slice of the world to find
   * a block which matches the criteria provided by the parameters.
   * @param p Target player
   * @param air Whether to search for air or non-air blocks
   * @param down Whether to go down or up
   * @param exhaust Whether to exhaust (search until the is-air-state changes again)
   * @param boundaries Whether to search till' the world's boundaries
   * @return Optional location, empty if no remaining block meets the criteria
   */
  Optional<Location> searchYSlice(Player p, boolean air, boolean down, boolean exhaust, boolean boundaries);
}
