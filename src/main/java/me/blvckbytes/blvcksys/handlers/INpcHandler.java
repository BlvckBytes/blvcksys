package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.models.NpcModel;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.List;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/20/2022

  Public interfaces which the npc handler provides to other consumers.
 */
public interface INpcHandler {

  /**
   * Create a new npc at a given location
   * @param creator Executing player
   * @param name Name of the npc
   * @param loc Location of the npc
   * @return NpcModel on success, empty if there's already an npc with this name
   */
  Optional<NpcModel> createNpc(OfflinePlayer creator, String name, Location loc);

  /**
   * Delete an existing npc
   * @param name Name of the npc
   * @return True on success, false if there's no npc with this name
   */
  boolean deleteNpc(String name);

  /**
   * Move an npc to a given location
   * @param name Name of the npc
   * @param loc Location to move to
   * @return True on success, false if there's no npc with this name
   */
  boolean moveNpc(String name, Location loc);

  /**
   * Change the skin of a npc
   * @param name Name of the npc
   * @param skin New skin value
   * @return SUCC on success, EMPTY if there is no npc with this name, ERR if the skin could not be loaded
   * and ERR if the skin textures could not be loaded
   */
  TriResult changeSkin(String name, String skin);

  /**
   * Get the nearest npc handle of a given location
   * @param where Where to search at
   * @return Nearest npc, empty if there was no npc within reach
   */
  Optional<FakeNpc> getNearestNpc(Location where);

  /**
   * Get a list of npcs which are near a specified location
   * @param where Where to search at
   * @param rangeRadius Max. distance between where and the hologram
   * @return List of npcs that are within the radius
   */
  List<NpcModel> getNear(Location where, double rangeRadius);
}
