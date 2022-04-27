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

  /**
   * Set a player as grayed within the tablist
   * @param p Target player
   * @param state Grayed state
   */
  void setGrayed(Player p, boolean state);

  /**
   * Get a player's grayed state within the tablist
   * @param p Target player
   * @return Grayed state
   */
  boolean getGrayed(Player p);
}
