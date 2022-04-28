package me.blvckbytes.blvcksys.packets.communicators.blockspoof;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Communicates fake blocks to a given player.
*/
public interface IBlockSpoofCommunicator {

  /**
   * Spoof a new block at a given location for a player
   * @param p Target player
   * @param loc Location of the spoofed block
   * @param mat Material of the target block
   * @return Success state
   */
  boolean spoofBlock(Player p, Location loc, Material mat);
}
