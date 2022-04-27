package me.blvckbytes.blvcksys.packets.communicators.objective;

import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/27/2022

  Represents the display position of an objective packet.
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
