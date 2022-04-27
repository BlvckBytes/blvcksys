package me.blvckbytes.blvcksys.packets.communicators.team;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/27/2022

  Represents the mode field of a tablist-team packet
*/
public enum TeamAction {

  // Create team
  CREATE(0),

  // Remove team
  REMOVE(1),

  // Update team
  UPDATE(2),

  // Add members to a team
  ADD_MEMBERS(3),

  // Remove members from a team
  REMOVE_MEMBERS(4);

  private final int mode;

  TeamAction(int mode) {
    this.mode = mode;
  }

  /**
   * Mode as it's used in the corresponding packet's field
   */
  public int getMode() {
    return mode;
  }
}
