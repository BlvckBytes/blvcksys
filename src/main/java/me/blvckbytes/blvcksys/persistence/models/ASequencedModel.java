package me.blvckbytes.blvcksys.persistence.models;

import lombok.*;
import me.blvckbytes.blvcksys.persistence.ForeignKeyAction;
import me.blvckbytes.blvcksys.persistence.ModelProperty;

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
public abstract class ASequencedModel<T extends APersistentModel> extends APersistentModel {

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
}
