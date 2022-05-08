package me.blvckbytes.blvcksys.packets.communicators.hologram;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Communicates spawning virtual invisible armor-stands for
  per-player customized holograms.
*/
public interface IHologramCommunicator {

  /**
   * Create a new hologram line for the first time for a player
   * @param p Target player
   * @param loc Location of the hologram
   * @param line Line to display
   * @return Handle to the entity for further actions
   */
  Entity createLine(Player p, Location loc, String line);

  /**
   * Update an existing, previously created hologram line for a player
   * @param p Target player
   * @param handle Entity handle from creation
   * @param newLine New line content
   */
  void updateLine(Player p, Entity handle, String newLine);

  /**
   * Delete an existing, previously created hologram line for a player
   * @param p Target player
   * @param handle Entity handle from creation
   */
  void deleteLine(Player p, Entity handle);
}
