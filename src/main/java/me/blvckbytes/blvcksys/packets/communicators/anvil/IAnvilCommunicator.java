package me.blvckbytes.blvcksys.packets.communicators.anvil;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Communicates opening a virtual, fully functional anvil to a player.
*/
public interface IAnvilCommunicator {

  /**
   * Open a fully functional virtual anvil for a given player
   * @param p Target player
   * @param title Title of the inventory
   * @return Success state
   */
  boolean openFunctionalAnvil(Player p, String title);
}
