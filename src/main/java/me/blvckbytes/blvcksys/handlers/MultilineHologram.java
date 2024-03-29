package me.blvckbytes.blvcksys.handlers;

import lombok.Getter;
import me.blvckbytes.blvcksys.packets.communicators.armorstand.ArmorStandProperties;
import me.blvckbytes.blvcksys.packets.communicators.armorstand.IArmorStandCommunicator;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Consumer;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;

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

  private final Collection<? extends Player> recipients;
  private final List<Integer> entityIds;
  private final Map<Player, List<Tuple<Entity, ArmorStandProperties>>> entities;
  private final String name;

  private Location loc;
  private Vector vel;
  private boolean collisions, drag, destroyed;
  private double gravity;

  private List<Tuple<Long, List<Object>>> lineTemplates;

  private final IArmorStandCommunicator holoComm;
  private final JavaPlugin plugin;

  public MultilineHologram(
    String name,
    Location loc,
    List<String> lines,
    @Nullable Collection<? extends Player> recipients,
    IArmorStandCommunicator holoComm,
    ILiveVariableSupplier varSupp,
    JavaPlugin plugin
  ) {
    super(varSupp);

    this.name = name;
    this.loc = loc;
    this.holoComm = holoComm;
    this.plugin = plugin;
    this.recipients = recipients;

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
   * Get the location where the next line would end up at
   */
  public Location getNextLocation() {
    Location head = loc.clone();
    head.add(0, lineTemplates.size() * -INTER_LINE_SPACING, 0);
    return head;
  }

  /**
   * Only updates the lines without re-spawning the entities underneith.
   * This only works if the number of new lines is equal to the number of
   * old lines, so no entities need to be removed or added.
   * @param lines Lines to set
   * @return True on success, false if the counts mismatched
   */
  public boolean updateLines(List<String> lines) {
    if (this.lineTemplates.size() != lines.size())
      return false;

    // Build a list of line templates from the string lines
    this.lineTemplates = lines.stream()
      .map(this::buildLineTemplate)
      .toList();

    // Update all entities forcefully
    entities.keySet().forEach(p -> tickPlayer(p, 0, true));
    return true;
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
   * @param gravity Gravity to apply, null is minecraft's default
   * @param drag Whether to apply drag
   * @param collisions Whether to apply collision detection
   * @param onTick Callback providing the current velocity on every tick
   * @param complete Callback that's executed when the hologram stopped moving
   */
  public void setVelocity(
    Vector velocity,
    @Nullable Double gravity,
    boolean drag,
    boolean collisions,
    @Nullable Consumer<Vector> onTick,
    @Nullable Runnable complete
  ) {
    // There's already another velocity active
    if (vel != null)
      return;

    this.vel = velocity;
    this.gravity = gravity == null ? GRAVITY_ACCELERATION : gravity;
    this.drag = drag;
    this.collisions = collisions;

    applyVelocity(
      // The velocity routine needs the position closest to ground, instead of the head
      loc.clone().add(0, INTER_LINE_SPACING * (lineTemplates.size() - 1), 0),
      VELOCITY_MAX_SECS * 20L,
      onTick, complete
    );
  }

  /**
   * Recursive helper function which applies and modifies the current velocity
   * @param location Current location of the hologram (persistent location remains unchanged)
   * @param remainingTicks Ticks remaining until the process is exited no matter of the state
   * @param onTick Callback providing the current velocity on every tick
   * @param complete Completion callback
   */
  private void applyVelocity(
    Location location,
    long remainingTicks,
    @Nullable Consumer<Vector> onTick,
    @Nullable Runnable complete
  ) {
    Location preCollide = null;
    Block collidedWith = null;

    // Loop all players and their lines
    Location newLoc = location.clone().add(vel);

    // Check for collisions against solid blocks
    if (collisions && newLoc.getBlock().getType().isSolid()) {
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
      if (drag && vel.getX() != 0)
        vel.setX(vel.getX() + (vel.getX() < 0 ? DRAG : -DRAG));

      if (drag && vel.getY() != 0)
        vel.setY(vel.getY() + (vel.getY() < 0 ? DRAG : -DRAG));

      if (drag && vel.getZ() != 0)
        vel.setZ(vel.getZ() + (vel.getZ() < 0 ? DRAG : -DRAG));

      vel.add(new Vector(0, -gravity, 0));
    }

    if (onTick != null)
      onTick.accept(vel);

    for (Map.Entry<Player, List<Tuple<Entity, ArmorStandProperties>>> pe : entities.entrySet()) {
      Location tail = newLoc.clone();

      // Start out at the tail (last hologram)
      for (int i = pe.getValue().size() - 1; i >= 0; i--) {
        Tuple<Entity, ArmorStandProperties> e = pe.getValue().get(i);

        // Send the new velocity vector initially or after collisions
        if (remainingTicks == VELOCITY_MAX_SECS * 20 || collidedWith != null)
          holoComm.sendVelocity(pe.getKey(), e.a(), vel);

        holoComm.teleport(pe.getKey(), e.a(), tail, e.b());
        tail.add(0, INTER_LINE_SPACING, 0);
      }
    }

    // If the vector isn't zero and there are still ticks left,
    // invoke another tick of processing. Also, don't tick destroyed holograms.
    if (!destroyed && vel.lengthSquared() > 0 && remainingTicks > 0) {
      Location finalL = newLoc;
      Bukkit.getScheduler().runTaskLater(plugin, () -> applyVelocity(finalL, remainingTicks - 1, onTick, complete), 1);
      return;
    }

    // Done, reset
    if (complete != null)
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
      if (recipients != null && !recipients.contains(t))
        continue;

      if (isRecipient(t))
        tickPlayer(t, time, false);

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
    this.destroyed = true;

    for (Player t : entities.keySet()) {
      for (Tuple<Entity, ArmorStandProperties> ent : entities.get(t)) {
        holoComm.delete(t, ent.a());
        entityIds.remove(Integer.valueOf(ent.a().getEntityId()));
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
   * @param force Whether to force tick, no matter of the current time
   */
  private void tickPlayer(Player p, long time, boolean force) {
    // Make sure that the line entities exist for this player
    if (!this.entities.containsKey(p))
      createLineEntities(p);

    // Update all lines for this player
    List<Tuple<Entity, ArmorStandProperties>> pEnts = entities.get(p);
    for (int i = 0; i < Math.min(pEnts.size(), lineTemplates.size()); i++) {
      Tuple<Long, List<Object>> lineTemplate = lineTemplates.get(i);

      // Is a static line, doesn't need refreshing
      if (!force && lineTemplate.a() < 0)
        continue;

      // Period did not yet elapse
      if (!force && time % lineTemplate.a() != 0)
        continue;

      // Update this line
      Tuple<Entity, ArmorStandProperties> ent = pEnts.get(i);
      ent.b().setName(evaluateLineTemplate(p, lineTemplate.b()));
      holoComm.update(p, ent.a(), ent.b());
    }
  }

  /**
   * Create all existing line entities based on the
   * current state for a player
   * @param p Target player
   */
  private void createLineEntities(Player p) {
    List<Tuple<Entity, ArmorStandProperties>> ents = new ArrayList<>();

    // Make lines grow downwards from the head
    Location head = loc.clone();
    for (Tuple<Long, List<Object>> lineTemplate : lineTemplates) {

      ArmorStandProperties props = new ArmorStandProperties(evaluateLineTemplate(p, lineTemplate.b()));
      Entity ent = holoComm.create(p, head, props);

      ents.add(new Tuple<>(ent, props));
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
    List<Tuple<Entity, ArmorStandProperties>> ents = entities.remove(p);

    // Had no lines
    if (ents == null)
      return;

    // Destroy all lines
    for (Tuple<Entity, ArmorStandProperties> ent : ents) {
      holoComm.delete(p, ent.a());
      entityIds.remove(Integer.valueOf(ent.a().getEntityId()));
    }
  }

  /**
   * Move all existing line entities for a player
   * @param p Target player
   */
  private void moveLineEntities(Player p, Location loc) {
    List<Tuple<Entity, ArmorStandProperties>> ents = entities.get(p);

    // Had no lines
    if (ents == null)
      return;

    // Make lines grow downwards from the head
    Location head = loc.clone();
    for (Tuple<Entity, ArmorStandProperties> ent : ents) {
      holoComm.teleport(p, ent.a(), head, ent.b());
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
