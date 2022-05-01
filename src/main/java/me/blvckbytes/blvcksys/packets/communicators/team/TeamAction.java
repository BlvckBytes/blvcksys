package me.blvckbytes.blvcksys.packets.communicators.team;

import lombok.AllArgsConstructor;
import lombok.Getter;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/27/2022

  Represents the mode field of a tablist-team packet
*/
@AllArgsConstructor
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

  @Getter
  private final int mode;
}
