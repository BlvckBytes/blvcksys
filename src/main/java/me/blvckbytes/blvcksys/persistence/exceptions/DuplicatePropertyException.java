package me.blvckbytes.blvcksys.persistence.exceptions;

import lombok.Getter;
import net.minecraft.util.Tuple;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/05/2022

  Signals that a collision between unique properties in a model occurred.
*/
@Getter
public class DuplicatePropertyException extends PersistenceException {

  private final String model;
  private final List<Tuple<String, Object>> propVals;

  public DuplicatePropertyException(String model, List<Tuple<String, Object>> propVals) {
    super("Duplicate unique properties in '" + model + "'");

    this.model = model;
    this.propVals = propVals;
  }
}
