package me.blvckbytes.blvcksys.packets.communicators.team;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/27/2022

  Communicates creating/updating/removing scoreboard teams to a client.
*/
public interface ITeamCommunicator {

  /**
   * Send a scoreboard creation/update/removal request
   * @param p Target player
   * @param group Corresponding tablist group
   * @param action Action mode
   * @param members List of members within this request
   * @return Success state
   */
  boolean sendScoreboardTeam(
    Player p,
    TeamGroup group,
    TeamAction action,
    @Nullable Collection<? extends Player> members
  );
}
