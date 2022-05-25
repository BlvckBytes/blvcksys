package me.blvckbytes.blvcksys.handlers;

import lombok.Getter;
import me.blvckbytes.blvcksys.packets.communicators.hologram.IHologramCommunicator;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/09/2022

  Holds multiple hologram lines and manages the layouting, deciding
  of recipients (based on visibility) as well as keeping the hologram's
  variables in sync.
 */
@Getter
public class MultilineHologram extends ATemplateHandler {

  // Constants used to simulate physics on armor stand entities
  // A minecraft vector's axis-unit is block/ticks
  // All of these constants are to be read as per tick
  private static final double
    // Added to the velocity's y-axis on every tick
    GRAVITY_ACCELERATION = 0.08,

    // Added to all axies evenly on every tick
    DRAG = 0.02,

    // Minimum squared length of the velocity before it's called "dead" (zero)
    MIN_SQ_VELOCITY_LEN = Math.pow(0.30, 2),

    // By how much to reduce the velocity on impacts
    IMPACT_REDUCTION = 0.5;

  // Maximum number of seconds that a velocity will be processed for
  private static final long VELOCITY_MAX_SECS = 10;

  // Specify the spacing between two lines on the y-axis here
  private static final double INTER_LINE_SPACING = 0.25D;

  // Specify the max. squared distance between the hologram and any
  // given recipient that receives updates here
  private static final double RECIPIENT_MAX_DIST_SQ = Math.pow(30, 2);

  private final List<Integer> entityIds;
  private final Map<Player, List<Entity>> entities;
  private final String name;

  private Location loc;
  private Vector vel;

  private List<Tuple<Long, List<Object>>> lineTemplates;

  private final IHologramCommunicator holoComm;
  private final JavaPlugin plugin;

  public MultilineHologram(
    String name,
    Location loc,
    List<String> lines,
    IHologramCommunicator holoComm,
    ILiveVariableSupplier varSupp,
    JavaPlugin plugin
  ) {
    super(varSupp);

    this.name = name;
    this.loc = loc;
    this.holoComm = holoComm;
    this.plugin = plugin;

    this.entities = new HashMap<>();
    this.entityIds = new ArrayList<>();

    this.setLines(lines);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  /**
   * Checks whether this set of lines contains a line with the given entity id
   * @param entityId Entity id to search for
   * @return True if it contains it, false otherwise
   */
  public boolean containsEntityId(int entityId) {
    return entityIds.contains(entityId);
  }

  /**
   * Sets new content lines for all players by building new
   * templates and then destroying the old entities to generate new
   * ones per line template.
   * @param lines Lines to display
   */
  public void setLines(List<String> lines) {
    // Build a list of line templates from the string lines
    this.lineTemplates = lines.stream()
      .map(this::buildLineTemplate)
      .toList();

    for (Player p : Bukkit.getOnlinePlayers()) {
      if (!entities.containsKey(p))
        continue;

      destroyLineEntities(p);
      createLineEntities(p);
    }
  }

  /**
   * Sets the location of this hologram by teleporting all lines
   * of all players to the given location
   * @param loc New hologram location
   */
  public void setLoc(Location loc) {
    this.loc = loc;

    for (Player p : entities.keySet())
      moveLineEntities(p, loc);
  }

  /**
   * Set the hologram's velocity and internally send all movements
   * which that velocity describes
   * @param velocity Velocity to set
   * @param complete Callback that's executed the hologram stopped moving
   */
  public void setVelocity(Vector velocity, Runnable complete) {
    // There's already another velocity active
    if (vel != null)
      return;

    this.vel = velocity;

    for (Map.Entry<Player, List<Entity>> x : entities.entrySet()) {
      for (Entity ent : x.getValue())
        holoComm.sendVelocity(x.getKey(), ent, velocity);
    }

    applyVelocity(
      // The velocity routine needs the position closest to ground, instead of the head
      loc.clone().add(0, INTER_LINE_SPACING * (lineTemplates.size() - 1), 0),
      VELOCITY_MAX_SECS * 20L,
      complete
    );
  }

  /**
   * Recursive helper function which applies and modifies the current velocity
   * @param location Current location of the hologram (persistent location remains unchanged)
   * @param remainingTicks Ticks remaining until the process is exited no matter of the state
   * @param complete Completion callback
   */
  private void applyVelocity(Location location, long remainingTicks, Runnable complete) {
    Location preCollide = null;
    Block collidedWith = null;

    // Loop all players and their lines
    Location newLoc = location.clone().add(vel);

    // Check for collisions against solid blocks
    if (newLoc.getBlock().getType().isSolid()) {
      Vector dir = vel.clone().normalize();

      // Walk the velocity vector step by step until a solid block is encountered
      double step = .10;
      for (double l = 0; l < vel.length(); l += step) {
        Location currLoc = location.clone().add(dir.clone().multiply(l));
        Block currBlock = currLoc.getBlock();

        if (!currBlock.getType().isSolid())
          continue;

        // Set the pre-collide location to either the previous step or if it's
        // the first step, to one virtual step back (negative direction)
        collidedWith = currBlock;
        preCollide = currLoc.clone().add(dir.multiply(-step));
        break;
      }
    }

    // If it collided, bounce back if there's still enough strength
    // left, otherwise set the velocity to zero
    if (collidedWith != null) {
      newLoc = preCollide;
      vel.multiply(vel.lengthSquared() >= MIN_SQ_VELOCITY_LEN ? -IMPACT_REDUCTION : 0);
    }

    // Apply drag and gravity
    else {
      vel.setX(vel.getX() + (vel.getX() < 0 ? DRAG : -DRAG));
      vel.setY(vel.getY() + (vel.getY() < 0 ? DRAG : -DRAG));
      vel.setZ(vel.getZ() + (vel.getZ() < 0 ? DRAG : -DRAG));
      vel.add(new Vector(0, -GRAVITY_ACCELERATION, 0));
    }

    for (Map.Entry<Player, List<Entity>> pe : entities.entrySet()) {
      Location tail = newLoc.clone();

      // Start out at the tail (last hologram)
      for (int i = pe.getValue().size() - 1; i >= 0; i--) {
        Entity e = pe.getValue().get(i);

        // Send the new velocity vector after collisions
        if (collidedWith != null)
          holoComm.sendVelocity(pe.getKey(), e, vel);

        holoComm.moveLine(pe.getKey(), e, tail);
        tail.add(0, INTER_LINE_SPACING, 0);
      }
    }

    // If the vector isn't zero and there are still ticks left,
    // invoke another tick of processing
    if (vel.lengthSquared() > 0 && remainingTicks > 0) {
      Location finalL = newLoc;
      Bukkit.getScheduler().runTaskLater(plugin, () -> applyVelocity(finalL, remainingTicks - 1, complete), 1);
      return;
    }

    // Done, reset
    complete.run();
    vel = null;
  }

  /**
   * Called whenever there's a chance to update this hologram, which
   * doesn't mean that on every tick changes have to occur.
   * @param time Relative time in ticks since start
   */
  public void tick(long time) {
    for (Player t : Bukkit.getOnlinePlayers()) {
      if (isRecipient(t))
        tickPlayer(t, time);

      // Don't keep players which are out of reach in memory
      else
        destroyLineEntities(t);
    }

    // Don't keep offline players in memory
    cleanupOfflinePlayers();
  }

  /**
   * Called whenever this hologram should be destroyed for
   * all online players.
   */
  public void destroy() {
    for (Player t : entities.keySet()) {
      for (Entity ent : entities.get(t)) {
        holoComm.deleteLine(t, ent);
        entityIds.remove(Integer.valueOf(ent.getEntityId()));
      }
    }
    entities.clear();
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Cleans out offline player entries from the entity map
   */
  private void cleanupOfflinePlayers() {
    entities.keySet().removeIf(p -> !p.isOnline());
  }

  /**
   * Called whenever the hologram should update for a specific player
   * @param p Target player
   * @param time Relative time in ticks since start
   */
  private void tickPlayer(Player p, long time) {
    // Make sure that the line entities exist for this player
    if (!this.entities.containsKey(p))
      createLineEntities(p);

    // Update all lines for this player
    List<Entity> pEnts = entities.get(p);
    for (int i = 0; i < Math.min(pEnts.size(), lineTemplates.size()); i++) {
      Tuple<Long, List<Object>> lineTemplate = lineTemplates.get(i);

      // Is a static line, doesn't need refreshing
      if (lineTemplate.a() < 0)
        continue;

      // Period did not yet elapse
      if (time % lineTemplate.a() != 0)
        continue;

      // Update this line
      Entity ent = pEnts.get(i);
      holoComm.updateLine(p, ent, evaluateLineTemplate(p, lineTemplate.b()));
    }
  }

  /**
   * Create all existing line entities based on the
   * current state for a player
   * @param p Target player
   */
  private void createLineEntities(Player p) {
    List<Entity> ents = new ArrayList<>();

    // Make lines grow downwards from the head
    Location head = loc.clone();
    for (Tuple<Long, List<Object>> lineTemplate : lineTemplates) {
      Entity ent = holoComm.createLine(p, head, evaluateLineTemplate(p, lineTemplate.b()));

      ents.add(ent);
      entityIds.add(ent.getEntityId());

      head.add(0, -INTER_LINE_SPACING, 0);
    }

    entities.put(p, ents);
  }

  /**
   * Destroy all existing line entities for a player
   * @param p Target player
   */
  private void destroyLineEntities(Player p) {
    List<Entity> ents = entities.remove(p);

    // Had no lines
    if (ents == null)
      return;

    // Destroy all lines
    for (Entity ent : ents) {
      holoComm.deleteLine(p, ent);
      entityIds.remove(Integer.valueOf(ent.getEntityId()));
    }
  }

  /**
   * Move all existing line entities for a player
   * @param p Target player
   */
  private void moveLineEntities(Player p, Location loc) {
    List<Entity> ents = entities.get(p);

    // Had no lines
    if (ents == null)
      return;

    // Make lines grow downwards from the head
    Location head = loc.clone();
    for (Entity ent : ents) {
      holoComm.teleportLine(p, ent, head);
      head.add(0, -INTER_LINE_SPACING, 0);
    }
  }

  /**
   * Checks if the given player is a recipient of this hologram
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
