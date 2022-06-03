package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.packets.communicators.armorstand.ArmorStandProperties;
import me.blvckbytes.blvcksys.packets.communicators.armorstand.IArmorStandCommunicator;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/03/2022

  Holds a location and the stand's properties in order to display that
  fake armor stand to nearby players. Keeps track of whether the client
  knows this stand and sends destroy signals on destroy to only
  those clients.
 */
public class FakeArmorStand {

  // Specify the max. squared distance between the armor stand and any
  // given recipient that receives updates here
  private static final double RECIPIENT_MAX_DIST_SQ = Math.pow(30, 2);

  private final IArmorStandCommunicator comm;
  private final Map<Player, Entity> actives;
  private ArmorStandProperties props;
  private Location loc;

  public FakeArmorStand(
    IArmorStandCommunicator comm,
    ArmorStandProperties props,
    Location loc
  ) {
    this.comm = comm;
    this.props = props;
    this.loc = loc;

    this.actives = new HashMap<>();
  }

  /**
   * Moves the armor stand to a new location
   * @param loc New location
   */
  public void setLoc(Location loc) {
    this.loc = loc;
    for (Map.Entry<Player, Entity> active : actives.entrySet())
      comm.moveLine(active.getKey(), active.getValue(), this.loc, false);
  }

  /**
   * Called on every armor stand tick period
   */
  public void tick() {
    for (Player t : Bukkit.getOnlinePlayers()) {
      // Not a recipient of this NPC, skip
      if (!isRecipient(t)) {

        // Remove and destroy the armor stand, if active
        if (actives.containsKey(t)) {
          Entity handle = actives.remove(t);
          comm.delete(t, handle);
        }

        continue;
      }

      tickPlayer(t);
    }

    // Don't keep offline players in memory
    cleanupOfflinePlayers();
  }

  /**
   * Called when the lifetime of this armor stand is over
   */
  public void destroy() {
    // Destroy the armor stand for all active players
    for (Iterator<Player> activeI = actives.keySet().iterator(); activeI.hasNext();) {
      Player p = activeI.next();
      comm.delete(p, actives.get(p));
      activeI.remove();
    }
  }

  /**
   * Tick this armor stand for a specific player
   * @param p Player to tick
   */
  private void tickPlayer(Player p) {
    // Already has an active instance of this armor stand
    if (actives.containsKey(p))
      return;

    Entity handle = comm.create(p, loc, props);
    actives.put(p, handle);
  }

  /**
   * Cleans out offline player entries from the active list
   */
  private void cleanupOfflinePlayers() {
    actives.keySet().removeIf(p -> !p.isOnline());
  }

  /**
   * Checks if the given player is a recipient of this armor stand
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
