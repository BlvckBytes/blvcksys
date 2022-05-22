package me.blvckbytes.blvcksys.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/22/2022

  Represents a head which uses a skin that makes it's head display
  something symbolic, which can be used for decorative purposes.
 */
@Getter
@AllArgsConstructor
public enum SymbolicHead {
  ARROW_LEFT("MHF_ArrowLeft"),
  ARROW_RIGHT("MHF_ArrowRight");

  private final String owner;

  @Override
  public String toString() {
    return owner;
  }
}
