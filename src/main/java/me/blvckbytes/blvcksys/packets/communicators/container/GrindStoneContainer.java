package me.blvckbytes.blvcksys.packets.communicators.container;

import lombok.Getter;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerGrindstone;
import org.bukkit.inventory.Inventory;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Represents a standardized container for a grindstone.
*/
public class GrindStoneContainer extends ContainerGrindstone implements IContainer {

  @Getter
  private final int containerId;

  @Getter
  private final Inventory inv;

  public GrindStoneContainer(
    int containerId,
    PlayerInventory playerinventory,
    ContainerAccess containeraccess,
    IChatBaseComponent title
  ) {
    super(containerId, playerinventory, containeraccess);
    this.containerId = containerId;
    this.checkReachable = false;
    this.inv = getBukkitView().getTopInventory();

    setTitle(title);
  }
}
