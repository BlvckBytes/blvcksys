package me.blvckbytes.blvcksys.events;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Public interfaces which the move listener provides to other consumers.
*/
public interface IMoveListener {

  /**
   * Subscribe to movements of a player
   * @param target Player to track
   * @param callback Callback for movements
   * @return Registered callback ref
   */
  Runnable subscribe(Player target, Runnable callback);

  /**
   * Unsubscribe from movements of a player
   * @param target Player that's being tracked
   * @param callback Callback registered previously
   */
  void unsubscribe(Player target, Runnable callback);
}
