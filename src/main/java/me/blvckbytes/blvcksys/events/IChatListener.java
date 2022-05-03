package me.blvckbytes.blvcksys.events;

import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.Collection;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/29/2022

  Public interfaces which the chat listener provides to other consumers.
*/
public interface IChatListener {

  /**
   * Send a chat message from a sender to a list of receivers and
   * translate the message's color notations as the sender's permissions allow to
   * @param sender Player who sends the message
   * @param receivers List of message receivers
   * @param message Message to send
   */
  void sendChatMessage(Player sender, Collection<? extends Player> receivers, String message);

  /**
   * Broadcast a message to all online players
   * @param receivers List of message receivers
   * @param message Message to send
   */
  void broadcastMessage(Collection<? extends Player> receivers, String message);

  /**
   * Broadcast a message to all online players
   * @param receivers List of message receivers
   * @param message Message to send
   */
  void broadcastMessage(Collection<? extends Player> receivers, TextComponent message);
}
