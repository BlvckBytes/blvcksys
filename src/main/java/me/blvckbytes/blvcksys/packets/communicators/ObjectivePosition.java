package me.blvckbytes.blvcksys.packets.communicators;

import lombok.Getter;

/**
 * Represents the position of a scoreboard objective within the HUD
 */
public enum ObjectivePosition {
  // Within the tab bar (next to the ping indicator)
  TAB(0),

  // Within the sidebar on the right hand side of the screen
  SIDEBAR(1),

  // Below the username above the avatar
  BELOW_NAME(2);

  @Getter
  private final int position;

  ObjectivePosition(int position) {
    this.position = position;
  }
}
