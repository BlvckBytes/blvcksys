package me.blvckbytes.blvcksys.persistence.exceptions;

import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/05/2022

  Signals that a model is missing it's identifier (for updates or deletions).
*/
@Getter
public class MissingIdentifierException extends PersistenceException {

  private final String model;

  public MissingIdentifierException(String model) {
    super("The model '" + model + "' is missing it's identifier value!");

    this.model = model;
  }
}
