package me.blvckbytes.blvcksys.commands;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/22/2022

  Public interfaces which the msg command provides to other consumers.
*/
public interface IMsgCommand {

  /**
   * Get a sender's partner player which they're in a conversation with
   * @param sender Player trying to send to their partner
   * @return Partner player or null if no partner exists at this point in time
   */
  Player getPartner(Player sender);

  /**
   * Send a private message from the sender to the receiver
   * @param sender Player sending the message
   * @param receiver Player receiving the message
   * @param message Message to be sent
   */
  void sendMessage(Player sender, Player receiver, String message);
}
