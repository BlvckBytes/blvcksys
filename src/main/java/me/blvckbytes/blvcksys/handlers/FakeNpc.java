package me.blvckbytes.blvcksys.handlers;

import com.mojang.authlib.GameProfile;
import lombok.Getter;
import me.blvckbytes.blvcksys.packets.communicators.npc.INpcCommunicator;
import me.blvckbytes.blvcksys.util.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.A;

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

  // Specify the max. squared distance between the npc and a player
  // that it should track with it's rotation
  private static final double LOOK_MAX_DIST_SQ = Math.pow(8, 2);

  // The delay used between tab manipulation packets (add/remove) and spawning
  // or despawning the fake entity
  private static final long DEL_TAB_MANIP_T = 20;

  private final List<Player> actives;
  private final INpcCommunicator npcComm;
  private final JavaPlugin plugin;
  private final int entityId;
  private final String name;

  private Location loc;
  private GameProfile prof;

  public FakeNpc(
    Location loc,
    GameProfile prof,
    int entityId,
    String name,
    INpcCommunicator npcComm,
    JavaPlugin plugin
  ) {
    this.loc = loc;
    this.prof = prof;
    this.entityId = entityId;
    this.npcComm = npcComm;
    this.plugin = plugin;
    this.name = name;

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
    if (actives.contains(p)) {
      // Don't track the player by rotation
      if (loc.distanceSquared(p.getLocation()) > LOOK_MAX_DIST_SQ)
        return;

      lookAtPlayer(p);
      return;
    }

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
   * Makes the NPC look directly at the player's head
   * @param p Player to look at
   */
  private void lookAtPlayer(Player p) {
    // Resting vector (yaw=0, pitch=0) means looking along +z-axis, pitched parallel to it
    Vec3D vResting = new Vec3D(0, 0, 1);

    // Looking vector from the entity's head towards the player's head
    // Add to the Y coordinate of the entity to end up about where the head resides
    Vec3D vLook = Vec3D.fromLocation(p.getEyeLocation()).sub(Vec3D.fromLocation(loc).add(0, 1.6, 0));

    // The angle which vResting and vLook spans is the resulting yaw,
    // where cos is used to decide on the angle and sin is used to
    // get the information about whether it's positive or negative, to end
    // up with an output which ranges from -180 to 180
    double yawAngle = Math.atan2(
      vLook.getX() * vResting.getX() - vLook.getZ() * vResting.getZ(),
      -(vLook.getX() * vResting.getZ() + vLook.getZ() * vResting.getX())
    ) * 180.0 / Math.PI + 90;

    // Calculate the angle between vLook and the x-z plane by getting the
    // angle of the triangle with sides deltaY and the length of vLook
    double pitchAngle = Math.atan(
      (p.getLocation().getY() - loc.getY()) / vLook.abs()
    ) * -180.0 / Math.PI;

    npcComm.setRotation(entityId, p, (float) yawAngle, (float) pitchAngle);
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
