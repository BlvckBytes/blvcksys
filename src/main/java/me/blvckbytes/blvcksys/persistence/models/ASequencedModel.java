package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ForeignKeyAction;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import net.minecraft.util.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/10/2022

  Represents a model that uses a doubly linked list by self-referencing
  through foreign keys in order to keep a fixed sequence.
*/
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class ASequencedModel extends APersistentModel {

  @ModelProperty(
    foreignKey = ASequencedModel.class,
    isNullable = true,
    foreignChanges = ForeignKeyAction.SET_NULL
  )
  private UUID previous;

  @ModelProperty(
    foreignKey = ASequencedModel.class,
    isNullable = true,
    foreignChanges = ForeignKeyAction.SET_NULL
  )
  private UUID next;

  /**
   * Sort a list of unsorted items into the sequence which is determined
   * by their next- and previous linked list pointers.
   * @param unsorted List of unsorted items
   * @return Optional List of items in sequence
   */
  public static<T extends ASequencedModel> List<T> sequentize(List<T> unsorted) throws PersistenceException {
    List<T> ret = new ArrayList<>();

    // Empty list provided
    if (unsorted.size() == 0)
      return ret;

    // Find the head node (which has no previous line)
    T head = unsorted
      .stream()
      .filter(line -> line.getPrevious() == null)
      .findFirst()
      .orElseThrow(() -> new PersistenceException("Invalid linked list encountered"));

    // Add the head
    ret.add(head);

    // Just navigate the head till' the end
    while (head.getNext() != null) {
      UUID next = head.getNext();
      head = unsorted
        .stream()
        .filter(line -> line.getId().equals(next))
        .findFirst()
        .orElseThrow(() -> new PersistenceException("Invalid linked list encountered"));

      // Add the next entry
      ret.add(head);
    }

    return ret;
  }

  /**
   * Delete a member of a sequence and make sure it's direct
   * neighbors's fields are updated too
   * @param member Member to delete
   * @param pers Persistence ref
   */
  public static<T extends ASequencedModel> void deleteSequenceMember(T member, IPersistence pers) throws PersistenceException {
    // Find the predecessor which points to the line by "next"
    ASequencedModel predecessor = pers.findFirst(
      new QueryBuilder<>(
        member.getClass(),
        "next", EqualityOperation.EQ, member.getId()
      )
    ).orElse(null);

    // Find the successor which points to the line by "previous"
    ASequencedModel successor = pers.findFirst(
      new QueryBuilder<>(
        member.getClass(),
        "previous", EqualityOperation.EQ, member.getId()
      )
    ).orElse(null);

    // Node inbetween two other elements
    if (predecessor != null && successor != null) {
      // The predecessor now points to the successor for next
      // and the successor now points to the predecessor for previous,
      // effectively skipping the node to delete
      predecessor.setNext(successor.getId());
      successor.setPrevious(predecessor.getId());

      pers.store(predecessor);
      pers.store(successor);
    }

    // No successor, is a tail node
    else if (predecessor != null) {
      // Now the predecessor becomes the new tail node
      predecessor.setNext(null);
      pers.store(predecessor);
    }

    // No predecessor, is a head node
    else if (successor != null) {
      // Now the successor becomes the new head node
      successor.setPrevious(null);
      pers.store(successor);
    }

    // Otherwise: was the only node, nothing to change

    // Delete the member which is not used in any other foreign keys anymore
    pers.delete(member);
  }

  /**
   * Push a member onto the end of an existing sequence by setting it's
   * and the last item's fields accordingly
   * @param member Member to push
   * @param membersQuery Query that will yield all members of the target sequence
   * @param pers Persistence ref
   */
  public static<T extends ASequencedModel> void pushSequenceMember(
    T member,
    QueryBuilder<T> membersQuery,
    IPersistence pers
  ) throws PersistenceException {
    // Find the tail of the current list of lines
    ASequencedModel tail = pers.findFirst(
      membersQuery.and(
        "next", EqualityOperation.EQ, null
      )
    ).orElse(null);

    // Decide whether previous is null (new is first entry) or the tail's ID
    UUID previous = null;
    if (tail != null)
      previous = tail.getId();

    // Set the previous and next pointers of the new member
    member.setPrevious(previous);
    member.setNext(null);
    pers.store(member);

    // Update the "previous tail"'s next to the newly created ID
    if (tail != null) {
      tail.setNext(member.getId());
      pers.store(tail);
    }
  }

  /**
   * Alter the sequence of a sequentized list to a given order
   * @param membersQuery Query that will yield all members of the target sequence
   * @param sequence Relative sequence (relative to the current state), ranging from 1..n, where
   *                 1 is the head and n is the tail. All numbers need to be present.
   * @param pers Persistence ref
   */
  public static<T extends ASequencedModel> Tuple<SequenceSortResult, Integer> alterSequence(
    QueryBuilder<T> membersQuery,
    int[] sequence,
    IPersistence pers
  ) throws PersistenceException {
    List<T> members = sequentize(pers.find(membersQuery));

    // There are some IDs missing
    if (members.size() > sequence.length)
      return new Tuple<>(SequenceSortResult.IDS_MISSING, members.size() - sequence.length);

    // Sort the members as specified by the ID-list
    List<T> sorted = new ArrayList<>();
    for (int sequenceId : sequence) {
      if (sequenceId <= 0 || sequenceId > members.size())
        return new Tuple<>(SequenceSortResult.ID_INVALID, sequenceId);
      sorted.add(members.get(sequenceId - 1));
    }

    // Change linked list pointers accordingly
    for (int i = 0; i < sorted.size(); i++) {
      T curr = sorted.get(i);
      curr.setPrevious(i == 0 ? null : sorted.get(i - 1).getId());
      curr.setNext(i == sorted.size() - 1 ? null : sorted.get(i + 1).getId());
      pers.store(curr);
    }

    // Sorted successfully
    return new Tuple<>(SequenceSortResult.SORTED, 0);
  }
}
