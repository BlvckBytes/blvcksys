package me.blvckbytes.blvcksys.packets.communicators.container;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerLoom;
import org.bukkit.inventory.Inventory;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Represents a standardized container for a loom.
*/
public class LoomContainer extends ContainerLoom implements IContainer {

  @Getter
  private final int containerId;

  @Getter
  private final Inventory inv;

  @Getter
  @Setter
  private int levelCost;

  public LoomContainer(
    int containerId,
    PlayerInventory inventory,
    ContainerAccess access,
    IChatBaseComponent title
  ) {
    super(containerId, inventory, access);
    this.containerId = containerId;
    this.checkReachable = false;
    this.inv = getBukkitView().getTopInventory();

    setTitle(title);
  }
}
