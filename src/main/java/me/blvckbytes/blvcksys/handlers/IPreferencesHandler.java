package me.blvckbytes.blvcksys.handlers;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/08/2022

  Public interfaces which the preferences handler provides to other consumers.
*/
public interface IPreferencesHandler {

  /**
   * Get the scoreboard hidden preference
   * @param p Target player
   */
  boolean isScoreboardHidden(Player p);

  /**
   * Set the scoreboard hidden preference
   * @param p Target player
   * @param hidden Whether the scoreboard should be hidden
   */
  void setScoreboardHidden(Player p, boolean hidden);
}