package me.blvckbytes.blvcksys.packets.communicators.container;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Communicates opening a virtual, fully functional container to a player.
*/
public interface IContainerCommunicator {

  /**
   * Open a fully functional virtual anvil for a given player
   * @param p Target player
   * @param title Title of the inventory
   * @return Success state
   */
  boolean openFunctionalAnvil(Player p, String title);

  /**
   * Open a fully functional virtual workbench for a given player
   * @param p Target player
   * @param title Title of the inventory
   * @return Success state
   */
  boolean openFunctionalWorkbench(Player p, String title);

  /**
   * Open a fully functional virtual grindstone for a given player
   * @param p Target player
   * @param title Title of the inventory
   * @return Success state
   */
  boolean openFunctionalGrindstone(Player p, String title);
}
