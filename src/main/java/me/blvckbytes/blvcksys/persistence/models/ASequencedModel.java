package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ForeignKeyAction;
import me.blvckbytes.blvcksys.persistence.ModelProperty;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;

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
}
