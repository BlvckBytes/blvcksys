package me.blvckbytes.blvcksys.packets.communicators.fakeitem;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/30/2022

  Communicates setting items in the player's inventory in a fake manner, so
  that the server doesn't handle them and they're only virtual.
*/
public interface IFakeItemCommunicator {

  /**
   * Sets a slot within the player's inventory to an itemstack in a fake manner (only clientside change)
   * @param p Target player
   * @param is ItemStack to set
   * @param slot Slot to change (from 0 to 35)
   * @return Success state
   */
  boolean setFakeSlot(Player p, @Nullable ItemStack is, int slot);
}
