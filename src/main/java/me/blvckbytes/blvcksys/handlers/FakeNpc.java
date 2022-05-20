package me.blvckbytes.blvcksys.handlers;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import me.blvckbytes.blvcksys.packets.communicators.npc.INpcCommunicator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/20/2022

  Holds a location and the npc's skin in order to display that
  fake NPC to nearby players. Keeps track of whether the client
  knows this npc and sends destroy signals on destroy to only
  those clients.
 */
@Getter
public class FakeNpc {

  // Specify the max. squared distance between the npc and any
  // given recipient that receives updates here
  private static final double RECIPIENT_MAX_DIST_SQ = Math.pow(30, 2);

  // The delay used between tab manipulation packets (add/remove) and spawning
  // or despawning the fake entity
  private static final long DEL_TAB_MANIP_T = 5;

  private final List<Player> actives;
  private final INpcCommunicator npcComm;
  private final JavaPlugin plugin;
  private final int entityId;

  private Location loc;
  private GameProfile prof;

  public FakeNpc(Location loc, GameProfile prof, int entityId, INpcCommunicator npcComm, JavaPlugin plugin) {
    this.loc = loc;
    this.prof = prof;
    this.entityId = entityId;
    this.npcComm = npcComm;
    this.plugin = plugin;

    this.actives = new ArrayList<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  /**
   * Called whenever the npc is moved
   */
  public void setLoc(Location loc) {
    this.loc = loc;

    // Respawn
    destroy();
  }

  /**
   * Called whenever the npc changes it's game profile
   */
  public void setGameProfile(GameProfile prof) {
    this.prof = prof;

    // Respawn
    destroy();
  }

  /**
   * Called periodically to update the npc
   */
  public void tick() {
    for (Player t : Bukkit.getOnlinePlayers()) {
      // Not a recipient of this NPC, skip
      if (!isRecipient(t)) {

        // Remove and destroy the NPC, if active
        if (actives.contains(t)) {
          npcComm.removeFromTablist(entityId, prof, t);
          npcComm.destroyNpc(entityId, t);
          actives.remove(t);
        }

        continue;
      }

      tickPlayer(t);
    }

    // Don't keep offline players in memory
    cleanupOfflinePlayers();
  }

  /**
   * Called when the end of this npc's lifespan has been reached
   * and all fake instances need to be undone
   */
  public void destroy() {
    // Destroy the npc for all active players
    for (Iterator<Player> activeI = actives.iterator(); activeI.hasNext();) {
      Player p = activeI.next();
      npcComm.removeFromTablist(entityId, prof, p);
      npcComm.destroyNpc(entityId, p);
      activeI.remove();
    }
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Tick this NPC for a specific player
   * @param p Player to tick
   */
  private void tickPlayer(Player p) {
    // Already has an active instance of this NPC
    // NOTE: Maybe let the NPC look directly at the player here? Would be cool!
    if (actives.contains(p))
      return;

    npcComm.addToTablist(entityId, prof, p);

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      npcComm.spawnNpc(entityId, p, loc, prof);
    }, DEL_TAB_MANIP_T);

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      npcComm.removeFromTablist(entityId, prof, p);
    }, DEL_TAB_MANIP_T * 2);

    actives.add(p);
  }

  /**
   * Cleans out offline player entries from the active list
   */
  private void cleanupOfflinePlayers() {
    actives.removeIf(p -> !p.isOnline());
  }

  /**
   * Checks if the given player is a recipient of this npc
   * @param p Player to test for
   * @return True if should receive updates, false otherwise
   */
  private boolean isRecipient(Player p) {
    Location pLoc = p.getLocation();

    // Not in the same world
    if (loc.getWorld() != pLoc.getWorld())
      return false;

    // Check if the player is within reach
    return loc.distanceSquared(pLoc) <= RECIPIENT_MAX_DIST_SQ;
  }
}
