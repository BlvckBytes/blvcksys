package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.ModelNotFoundException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.HologramLineModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Handles managing a hologram's list of lines, which is built by a
  doubly linked list and thus provides an easy API which abstracts
  this underlying fact for all callers.
*/
@AutoConstruct
public class HologramHandler implements IHologramHandler {

  // TODO: Implement a cache that caches holo-lines for fast access (commands, drawing, ...)

  private final IPersistence pers;

  public HologramHandler(
    @AutoInject IPersistence pers
  ) {
    this.pers = pers;
  }

  @Override
  public boolean deleteHologram(String name) throws PersistenceException {
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
      // Delete the item which is now not used in
      // any other foreign keys anymore
      pers.delete(line);
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

    return line;
  }

  @Override
  public Optional<List<HologramLineModel>> getHologramLines(String name) throws PersistenceException {
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

    return Optional.of(sortedLines);
  }
}
