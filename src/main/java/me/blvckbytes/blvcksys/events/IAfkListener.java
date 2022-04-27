package me.blvckbytes.blvcksys.events;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Public interfaces which the afk listener provides to other consumers.
*/
public interface IAfkListener {

  /**
   * Check whether or not a given player is AFK
   * @param p Target player
   * @return AFK state
   */
  boolean isAFK(Player p);

  /**
   * Set a player into AFK state programmatically
   * @param p Target player
   */
  void setAFK(Player p);
}
