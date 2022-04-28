package me.blvckbytes.blvcksys.adapters;

import org.bukkit.Location;
import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Offers a public interface to interact with protective regions
*/
public interface IRegionAdapter {

  /**
   * Check whether a player can build at a given location
   * @param p Target player
   * @param loc Location to check at
   */
  boolean canBuild(Player p, Location loc);
}
