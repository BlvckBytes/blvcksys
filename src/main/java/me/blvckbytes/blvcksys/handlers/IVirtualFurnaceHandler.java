package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.handlers.gui.VirtualFurnace;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.List;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/01/2022

  Public interfaces which the virtual furnace handler provides to other consumers.
 */
public interface IVirtualFurnaceHandler {

  /**
   * Access a player's virtual furnace by it's sequence number (index) and
   * create a new furnace if this index is still vacant.
   * @param p Target player
   * @param index Target index
   * @return Virtual furnace instance
   */
  VirtualFurnace accessFurnace(OfflinePlayer p, int index);

  /**
   * Get a list of all owned furnaces of a player
   * @param p Target player
   */
  List<VirtualFurnace> listFurnaces(OfflinePlayer p);

  /**
   * Get the number of owned furnaces of a player
   * @param p Target player
   * @return Number of owned furnaces
   */
  int getUsedNumberOfFurnaces(OfflinePlayer p);

  /**
   * Get the maximum number of furnaces a player may have
   * @param p Target player
   * @return Number of furnaces
   */
  int getAvailableNumberOfFurnaces(Player p);
}
