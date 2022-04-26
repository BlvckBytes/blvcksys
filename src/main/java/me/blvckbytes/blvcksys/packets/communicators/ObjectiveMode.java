package me.blvckbytes.blvcksys.packets.communicators;

import lombok.Getter;

/**
 * Represents the mode of an objective "registration" packet
 */
public enum ObjectiveMode {
  // Create a new objective
  CREATE(0),

  // Remove an existing objective
  REMOVE(1),

  // Modify an existing objective's display-text
  MODIFY_TEXT(2);

  @Getter
  private final int mode;

  ObjectiveMode(int mode) {
    this.mode = mode;
  }
}
