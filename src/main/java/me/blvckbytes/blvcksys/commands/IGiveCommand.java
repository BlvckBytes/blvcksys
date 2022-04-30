package me.blvckbytes.blvcksys.commands;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Public interfaces which the give command provides to other consumers.
*/
public interface IGiveCommand {

  /**
   * Give an item stack to a specific player and drop all items at the
   * target's position that won't fit into the inventory
   * @param target Target player
   * @param stack Stack to give
   * @return Number of dropped items
   */
  int giveItemsOrDrop(Player target, ItemStack stack);
}
