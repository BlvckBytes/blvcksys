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

  /**
   * Get the chat hidden preference
   * @param p Target player
   */
  boolean isChatHidden(Player p);

  /**
   * Set the chat hidden preference
   * @param p Target player
   * @param hidden Whether the scoreboard should be hidden
   */
  void setChatHidden(Player p, boolean hidden);

  /**
   * Get the msg disabled preference
   * @param p Target player
   */
  boolean isMsgDisabled(Player p);

  /**
   * Set the msg disabled preference
   * @param p Target player
   * @param disabled Whether private messages should be disabled
   */
  void setMsgDisabled(Player p, boolean disabled);
}
