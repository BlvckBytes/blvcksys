package me.blvckbytes.blvcksys.persistence.exceptions;

import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/05/2022

  Signals that a collision between two unique properties in a model occurred.
*/
@Getter
public class DuplicatePropertyException extends PersistenceException {

  private final String property, model;
  private final Object value;

  public DuplicatePropertyException(String model, String property, Object value) {
    super("Duplicate unique property '" + property + "' in '" + model + "': " + value);

    this.model = model;
    this.property = property;
    this.value = value;
  }
}
