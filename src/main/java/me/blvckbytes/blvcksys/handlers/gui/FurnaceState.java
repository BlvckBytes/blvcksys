package me.blvckbytes.blvcksys.handlers.gui;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/01/2022

  Represents all states a furnace can be in.
*/
public enum FurnaceState {
  // Currently smelts items
  SMELTING,

  // Is full and thus cannot smelt anymore
  FULL,

  // Is completely empty
  EMPTY,

  // Is out of fuel but could smelt further with fuel
  OUT_OF_FUEL,

  // Has remains that should be taken out
  HAS_REMAINS
}
