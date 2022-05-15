package me.blvckbytes.blvcksys.persistence.query;

import lombok.AllArgsConstructor;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/15/2022

  Represents all existing field operations that can be performed between two columns.
*/
@AllArgsConstructor
public enum FieldOperation {
  PLUS('+'),
  MINUS('-'),
  MULTIPLY('*'),
  DIVIDE('/');

  private final char symbol;

  @Override
  public String toString() {
    return String.valueOf(symbol);
  }
}
