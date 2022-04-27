package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.packets.communicators.team.TeamGroup;
import org.bukkit.entity.Player;

import java.util.Optional;

/*
 Author: BlvckBytes <blvckbytes@gmail.com>
 Created On: 04/27/2022

 Public interfaces which the team handler provides to other consumers.
 */
public interface ITeamHandler {

  /**
   * Get a player's assigned tablist group
   * @param p Target player
   * @return Currently assigned group
   */
  Optional<TeamGroup> getPlayerGroup(Player p);
}
