package me.blvckbytes.blvcksys.handlers;

import lombok.Getter;
import me.blvckbytes.blvcksys.packets.communicators.hologram.IHologramCommunicator;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

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

  // Specify the spacing between two lines on the y-axis here
  private static final double INTER_LINE_SPACING = 0.25D;

  // Specify the max. squared distance between the hologram and any
  // given recipient that receives updates here
  private static final double RECIPIENT_MAX_DIST_SQ = Math.pow(30, 2);

  private final Map<Player, List<Entity>> entities;
  private final String name;
  private Location loc;

  private List<Tuple<Long, List<Object>>> lineTemplates;

  private final IHologramCommunicator holoComm;

  public MultilineHologram(
    String name,
    Location loc,
    List<String> lines,
    IHologramCommunicator holoComm,
    ILiveVariableSupplier varSupp
  ) {
    super(varSupp);

    this.name = name;
    this.loc = loc;
    this.holoComm = holoComm;
    this.entities = new HashMap<>();

    this.setLines(lines);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  public void setLines(List<String> lines) {
    // Build a list of line templates from the string lines
    this.lineTemplates = lines.stream()
      .map(this::buildLineTemplate)
      .toList();

    for (Player p : Bukkit.getOnlinePlayers()) {
      destroyLineEntities(p);
      createLineEntities(p);
    }
  }

  public void setLoc(Location loc) {
    this.loc = loc;

    for (Player p : Bukkit.getOnlinePlayers()) {
      destroyLineEntities(p);
      createLineEntities(p);
    }
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
      for (Entity ent : entities.get(t))
        holoComm.deleteLine(t, ent);
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
      ents.add(holoComm.createLine(p, head, evaluateLineTemplate(p, lineTemplate.b())));
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
    for (Entity ent : ents)
      holoComm.deleteLine(p, ent);
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
