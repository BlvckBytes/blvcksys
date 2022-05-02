package me.blvckbytes.blvcksys.packets.communicators.container;

import net.minecraft.network.chat.IChatBaseComponent;
import org.bukkit.inventory.Inventory;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  All public interfaces any given type of container needs to implement.
*/
public interface IContainer {
  Inventory getInv();
  int getContainerId();
  IChatBaseComponent getTitle();
}
