package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.ModelNotFoundException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.HologramLineModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.FieldQueryGroup;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import org.bukkit.Location;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Handles managing a hologram's list of lines, which is built by a
  doubly linked list and thus provides an easy API which abstracts
  this underlying fact for all callers.
*/
@AutoConstruct
public class HologramHandler implements IHologramHandler {

  // Local cache for hologram lines, mapping the hologram name to a list of lines
  // This is crucial, as holograms will be accessed a lot for drawing, updating, commands, ...
  private final Map<String, List<HologramLineModel>> cache;

  private final IPersistence pers;

  public HologramHandler(
    @AutoInject IPersistence pers
  ) {
    this.pers = pers;
    this.cache = new HashMap<>();
  }

  @Override
  public boolean deleteHologram(String name) throws PersistenceException {
    this.cache.remove(name);

    return pers.delete(
      new QueryBuilder<>(
        HologramLineModel.class,
        "name", EqualityOperation.EQ, name
      )
    ) > 0;
  }

  @Override
  public boolean deleteHologramLine(HologramLineModel line) throws PersistenceException {
    // Find the predecessor which points to the line by "nextLine"
    HologramLineModel predecessor = pers.findFirst(
      new QueryBuilder<>(
        HologramLineModel.class,
        "nextLine", EqualityOperation.EQ, line.getId()
      )
    ).orElse(null);

    // Find the successor which points to the line by "previousLine"
    HologramLineModel successor = pers.findFirst(
      new QueryBuilder<>(
        HologramLineModel.class,
        "previousLine", EqualityOperation.EQ, line.getId()
      )
    ).orElse(null);

    // Node inbetween two other elements
    if (predecessor != null && successor != null) {
      // The predecessor now points to the successor for next
      // and the successor now points to the predecessor for previous,
      // effectively skipping the node to delete
      predecessor.setNextLine(successor.getId());
      successor.setPreviousLine(predecessor.getId());

      pers.store(predecessor);
      pers.store(successor);
    }

    // No successor, is a tail node
    else if (predecessor != null) {
      // Now the predecessor becomes the new tail node
      predecessor.setNextLine(null);
      pers.store(predecessor);
    }

    // No predecessor, is a head node
    else if (successor != null) {
      // Now the successor becomes the new head node
      successor.setPreviousLine(null);
      pers.store(successor);
    }

    // Otherwise: was the only node, nothing to change

    try {
      String name = line.getName();

      // Delete the item which is now not used in
      // any other foreign keys anymore
      pers.delete(line);

      // Load the changes into cache
      getHologramLines(name, true);
      return true;
    } catch (ModelNotFoundException e) {
      return false;
    }
  }

  @Override
  public HologramLineModel createHologramLine(String name, Location loc, String text) throws PersistenceException {
    // Find the tail of the current list of lines
    HologramLineModel tail = pers.findFirst(
      new QueryBuilder<>(
        HologramLineModel.class,
        "name", EqualityOperation.EQ, name
      ).and(
        "nextLine", EqualityOperation.EQ, null
      )
    ).orElse(null);

    // Decide whether previous is null (new is first entry) or the tail's ID
    UUID previous = null;
    if (tail != null)
      previous = tail.getId();

    // Create a new line pointing at previous and having no successor
    HologramLineModel line = new HologramLineModel(name, loc, text, previous, null);
    pers.store(line);

    // Update the "previous tail"'s next to the newly created ID
    if (tail != null) {
      tail.setNextLine(line.getId());
      pers.store(tail);
    }

    // Load the changes into cache
    getHologramLines(name, true);
    return line;
  }

  @Override
  public Optional<List<HologramLineModel>> getHologramLines(String name) throws PersistenceException {
    // Return cached responses
    return getHologramLines(name, false);
  }

  @Override
  public Map<String, List<HologramLineModel>> getNear(Location where, double rangeRadius) throws PersistenceException {
    // This should never be the case...
    if (where.getWorld() == null)
      throw new PersistenceException("Cannot find any near holograms if no world has been provided");

    Map<String, List<HologramLineModel>> ret = new HashMap<>();
    List<HologramLineModel> res = pers.find(
      new QueryBuilder<>(
        // Has to be in the same world
        HologramLineModel.class, "loc__world", EqualityOperation.EQ, where.getWorld().getName()
      )
        // X range constraint
        .and(
          new FieldQueryGroup("loc__x", EqualityOperation.GTE, where.getX() - rangeRadius)
            .and("loc__x", EqualityOperation.LTE, where.getX() + rangeRadius)
        )

        // Y range constraint
        .and(
          new FieldQueryGroup("loc__y", EqualityOperation.GTE, where.getY() - rangeRadius)
            .and("loc__y", EqualityOperation.LTE, where.getY() + rangeRadius)
        )

        // Z range constraint
        .and(
          new FieldQueryGroup("loc__z", EqualityOperation.GTE, where.getZ() - rangeRadius)
            .and("loc__z", EqualityOperation.LTE, where.getZ() + rangeRadius)
        )
    );

    // Group lines by their name for convenience
    for (HologramLineModel line : res) {
      // Create empty lists initially
      if (!ret.containsKey(line.getName()))
        ret.put(line.getName(), new ArrayList<>());

      // Add the line to it's "name group"
      ret.get(line.getName()).add(line);
    }

    return ret;
  }

  /**
   * Get all lines a hologram holds and cache results
   * @param name Name of the hologram
   * @param invalidateCache Whether or not to invalidate the cache and force an update
   * @return Optional list of lines, empty if this hologram didn't yet exist
   */
  private Optional<List<HologramLineModel>> getHologramLines(String name, boolean invalidateCache) throws PersistenceException {
    // Respond from cache
    if (!invalidateCache && cache.containsKey(name.toLowerCase()))
      return Optional.of(cache.get(name.toLowerCase()));

    List<HologramLineModel> unsortedLines = pers.find(
      new QueryBuilder<>(
        HologramLineModel.class,
        "name", EqualityOperation.EQ, name
      )
    );

    // This hologram doesn't yet exist
    if (unsortedLines.size() == 0)
      return Optional.empty();

    List<HologramLineModel> sortedLines = new ArrayList<>();

    // Find the head node (which has no previous line)
    HologramLineModel head = unsortedLines
      .stream()
      .filter(line -> line.getPreviousLine() == null)
      .findFirst()
      .orElseThrow(() -> new PersistenceException("Invalid linked list for hologram '" + name + "'"));

    // Add the head
    sortedLines.add(head);

    // Just navigate the head till' the end
    while (head.getNextLine() != null) {
      UUID next = head.getNextLine();
      head = unsortedLines
        .stream()
        .filter(line -> line.getId().equals(next))
        .findFirst()
        .orElseThrow(() -> new PersistenceException("Invalid linked list for hologram '" + name + "'"));

      // Add the next entry
      sortedLines.add(head);
    }

    // Cache result
    this.cache.put(name.toLowerCase(), sortedLines);
    return Optional.of(sortedLines);
  }
}
