package me.blvckbytes.blvcksys.commands;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Public interfaces which the tpa command provides to other consumers.
*/
public interface ITpaCommand {

  /**
   * Accept an incoming request from another player
   * @param sender Player that sent this request
   * @param target Player that accepts the request
   * @return True on success, false if the request didn't exist
   */
  boolean acceptRequest(Player sender, Player target);

  /**
   * Deny an incoming request from another player
   * @param sender Player that sent this request
   * @param target Player that denies the request
   * @return True on success, false if the request didn't exist
   */
  boolean denyRequest(Player sender, Player target);

  /**
   * Cancel a request a player has previously sent
   * @param sender Player that sent this request and is now cancelling
   * @param target Player that this request was pointed towards
   * @return True on success, false if the request didn't exist
   */
  boolean cancelRequest(Player sender, Player target);
}
