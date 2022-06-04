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
  ARROW_RIGHT("MHF_ArrowRight"),
  ARROW_UP("MHF_ArrowUp"),
  ARROW_DOWN("MHF_ArrowDown"),
  LETTER_H("OakWoodH"),
  LETTER_C("OakWoodC"),
  LETTER_L("OakWoodL"),
  LETTER_B("OakWoodB"),
  LETTER_R("OakWoodR"),
  LETTER_S("OakWoodS")
  ;

  private final String owner;

  @Override
  public String toString() {
    return owner;
  }
}
