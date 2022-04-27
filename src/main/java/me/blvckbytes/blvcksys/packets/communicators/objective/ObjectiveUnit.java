package me.blvckbytes.blvcksys.packets.communicators.objective;

import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/27/2022

  Represents the unit of metric of an objective packet.
*/
public enum ObjectiveUnit {
  // Count using hearts
  HEARTS("hearts"),

  // Count using a custom metric which uses integers
  INTEGER("integer");

  @Getter
  private final String unit;

  ObjectiveUnit(String unit) {
    this.unit = unit;
  }
}
