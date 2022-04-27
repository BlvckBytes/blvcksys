package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.packets.communicators.team.TeamGroup;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface ITeamHandler {

  /**
   * Get a player's assigned tablist group
   * @param p Target player
   * @return Currently assigned group
   */
  Optional<TeamGroup> getPlayerGroup(Player p);
}
