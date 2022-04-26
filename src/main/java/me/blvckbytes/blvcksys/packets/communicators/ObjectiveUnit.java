package me.blvckbytes.blvcksys.packets.communicators;

import lombok.Getter;

/**
 * Represents the unit an objective "counts" scores in
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
