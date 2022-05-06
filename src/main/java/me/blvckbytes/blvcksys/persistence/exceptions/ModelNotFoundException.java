package me.blvckbytes.blvcksys.persistence.exceptions;

import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/06/2022

  Signals that a model with a specific ID could not be located.
*/
@Getter
public class ModelNotFoundException extends PersistenceException {

  private final String model, id;

  public ModelNotFoundException(String model, String id) {
    super("Could not find model '" + model + "' with id of '" + id + "'");

    this.model = model;
    this.id = id;
  }
}
