package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.models.AHStateModel;
import org.bukkit.OfflinePlayer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/10/2022

  Public interfaces which the auction house handler provides to other consumers.
 */
public interface IAHHandler {

  /**
   * Get the current state of a given player
   * @param p Target player
   * @return Current state model
   */
  AHStateModel getState(OfflinePlayer p);

  /**
   * Store the current state for a given player
   * @param state Current state model
   */
  void storeState(AHStateModel state);

}
