package me.blvckbytes.blvcksys.packets.communicators.armorstand;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Communicates spawning virtual armor stands for per-player customized armor stands.
*/
public interface IArmorStandCommunicator {

  /**
   * Create a new armor stand for the first time for a player
   * @param p Target player
   * @param loc Location to spawn at
   * @param properties Properties of the armor stand
   * @return Handle to the entity for further actions
   */
  Entity create(Player p, Location loc, ArmorStandProperties properties);

  /**
   * Update an existing, previously created armor stand for a player
   * @param p Target player
   * @param handle Entity handle from creation
   * @param properties New properties of the armor stand
   */
  void update(Player p, Entity handle, ArmorStandProperties properties);

  /**
   * Delete an existing, previously created armor stand for a player
   * @param p Target player
   * @param handle Entity handle from creation
   */
  void delete(Player p, Entity handle);

  /**
   * Teleports an existing armor stand to another location
   * @param p Target player
   * @param handle Entity handle from creation
   * @param loc Location to teleport to
   * @param isShifted Whether the armor stand location is shifted
   */
  void teleport(Player p, Entity handle, Location loc, boolean isShifted);

  /**
   * Move a armor stand relative to it's current position
   * @param p Target player
   * @param handle Entity handle from creation
   * @param loc Location to move to
   * @param isShifted Whether the armor stand location is shifted
   */
  void move(Player p, Entity handle, Location loc, boolean isShifted);

  /**
   * Sends information about the armor stand's current velocity to the client. This is kind
   * of optional, but considered good practise, as the client can then predict missing
   * positions to make the movement look as continous as possible.
   * @param p Target player
   * @param handle Entity handle from creation
   * @param velocity Velocity to send
   */
  void sendVelocity(Player p, Entity handle, Vector velocity);
}
