package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.handlers.gui.VirtualFurnace;
import org.bukkit.entity.Player;

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
  VirtualFurnace accessFurnace(Player p, int index);
}
