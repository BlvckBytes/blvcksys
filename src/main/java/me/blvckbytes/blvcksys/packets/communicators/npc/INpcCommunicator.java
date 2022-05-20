package me.blvckbytes.blvcksys.packets.communicators.npc;

import com.mojang.authlib.GameProfile;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/20/2022

  Communicates spawning, destroying and moving fake npcs as well as adding/removing
  them to/from the tablist.
*/
public interface INpcCommunicator {

  /**
   * Spawn a new npc for a given player
   * @param entityId ID of this entity
   * @param receiver Receiver of the packet
   * @param loc Location of the npc
   * @param profile GameProfile of the npc, used for the skin
   */
  void spawnNpc(int entityId, Player receiver, Location loc, GameProfile profile);

  /**
   * Destroy an existing npc for a given player
   * @param entityId ID of this entity
   * @param receiver Receiver of the packet
   */
  void destroyNpc(int entityId, Player receiver);

  /**
   * Add an existing npc to the tablist for a given player
   * @param entityId ID of this entity
   * @param receiver Receiver of the packet
   * @param profile GameProfile of the npc, used for the skin
   */
  void addToTablist(int entityId, GameProfile profile, Player receiver);

  /**
   * Remove an existing npc from the tablist for a given player
   * @param entityId ID of this entity
   * @param receiver Receiver of the packet
   * @param profile GameProfile of the npc, used for the skin
   */
  void removeFromTablist(int entityId, GameProfile profile, Player receiver);

  /**
   * Sets the npc's rotation (head and body) to match the yaw and pitch
   * @param entityId ID of this entity
   * @param receiver Receiver of the packet
   * @param yaw Yaw of the npc
   * @param pitch Pitch of the npc
   */
  void setRotation(int entityId, Player receiver, float yaw, float pitch);
}
