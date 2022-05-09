package me.blvckbytes.blvcksys.handlers;

import lombok.Getter;
import me.blvckbytes.blvcksys.packets.communicators.hologram.IHologramCommunicator;
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
public class MultilineHologram {

  // Specify the spacing between two lines on the y-axis here
  private static final double INTER_LINE_SPACING = 0.25D;

  // Specify the max. squared distance between the hologram and any
  // given recipient that receives updates here
  private static final double RECIPIENT_MAX_DIST_SQ = Math.pow(30, 2);

  private final Map<Player, List<Entity>> entities;
  private final String name;
  private Location loc;

  private List<List<Object>> lineTemplates;

  private final IHologramCommunicator holoComm;
  private final IHologramVariableSupplier varSupp;

  public MultilineHologram(
    String name,
    Location loc,
    List<String> lines,
    IHologramCommunicator holoComm,
    IHologramVariableSupplier varSupp
  ) {
    this.name = name;
    this.loc = loc;
    this.holoComm = holoComm;
    this.varSupp = varSupp;
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
   */
  public void tick() {
    for (Player t : Bukkit.getOnlinePlayers()) {
      if (isRecipient(t))
        tickPlayer(t);

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
   */
  private void tickPlayer(Player p) {
    // Make sure that the line entities exist for this player
    if (!this.entities.containsKey(p))
      createLineEntities(p);

    // Update all lines for this player
    List<Entity> pEnts = entities.get(p);
    for (int i = 0; i < Math.min(pEnts.size(), lineTemplates.size()); i++) {
      List<Object> lineTemplate = lineTemplates.get(i);
      Entity ent = pEnts.get(i);
      holoComm.updateLine(p, ent, evaluateLineTemplate(p, lineTemplate));
    }
  }

  /**
   * Evaluate a line template consisting of strings and variable
   * type placeholders for a given player
   * @param p Target player
   * @param template Template to evaluate
   * @return Resulting string to display
   */
  private String evaluateLineTemplate(Player p, List<Object> template) {
    StringBuilder sb = new StringBuilder();

    // Iterate all parts of this template
    for (Object part : template) {
      // Append the string as is
      if (part instanceof String s) {
        sb.append(s);
        continue;
      }

      // Resolve this variable and append the result
      if (part instanceof HologramVariable hv)
        sb.append(varSupp.resolveVariable(p, hv));
    }

    return sb.toString();
  }

  /**
   * Append an object (String or variable type) to a list of objects (the template)
   * @param template Template to append to
   * @param append Object to append
   */
  private void appendToTemplate(List<Object> template, Object append) {
    // Append the initial element or variables as they are
    if (template.size() == 0 || !(append instanceof String sc)) {
      template.add(append);
      return;
    }

    // Concat the last entry with the new, current entry
    int lastIndex = template.size() - 1;
    if (template.get(lastIndex) instanceof String sl) {
      String newLast = sl + sc;
      template.remove(lastIndex);
      template.add(newLast);
    }

    // Last entry was no string, just append
    else
      template.add(sc);
  }

  /**
   * Build a line template for a given player
   * @param line Line template
   * @return Templated line
   */
  private List<Object> buildLineTemplate(String line) {
    List<Object> res = new ArrayList<>();

    // Char iteration state machine
    int lastOpenCurly = -1;

    char[] lineChars = line.toCharArray();
    for (int i = 0; i < lineChars.length; i++) {
      char c = lineChars[i];

      // Found a variable notation begin
      if (c == '{') {

        // Already found a previous begin, push that range
        if (lastOpenCurly >= 0)
          appendToTemplate(res, line.substring(lastOpenCurly, i));

        lastOpenCurly = i;

        // Only continue if '{' isn't the whole string
        if (lineChars.length != 1)
          continue;
      }

      // Currently, there's an open curly bracket waiting for being closed
      if (lastOpenCurly >= 0) {

        // Variable notation found in range [lastOpenCurly,i]
        if (c == '}') {
          String varNotation = line.substring(lastOpenCurly, i + 1);
          HologramVariable var = HologramVariable.fromPlaceholder(varNotation);

          // Variable unknown, append unaltered notation
          if (var == null)
            appendToTemplate(res, varNotation);

          // Add variable type as placeholder
          else
            appendToTemplate(res, var);

          lastOpenCurly = -1;
        }

        // The last open curly never closed again, just add that range as is
        else if (i == lineChars.length - 1)
          appendToTemplate(res, line.substring(lastOpenCurly));
      }

      // Append all chars outside of variables
      else
        appendToTemplate(res, String.valueOf(c));
    }

    return res;
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
    for (List<Object> lineTemplate : lineTemplates) {
      ents.add(holoComm.createLine(p, head, evaluateLineTemplate(p, lineTemplate)));
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
