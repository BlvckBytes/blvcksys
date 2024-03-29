package me.blvckbytes.blvcksys.handlers.gui;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/29/2022

  List of supported crate draw animation layouts.
*/
@Getter
@AllArgsConstructor
public enum CrateDrawLayout {

  HALF_CIRCLE(
    "37,28,20,12-14,24,34,43",
    "4,22",
    13, 5
  ),
  HORIZONTAL_LINE(
    "9-17",
    "4,22",
    13, 3
  )
  ;

  private final String itemSlots, markerSlots;
  private final int outputSlot, rowsRequired;
}
