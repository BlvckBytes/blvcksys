package me.blvckbytes.blvcksys.packets.communicators.container;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerAnvil;
import org.bukkit.inventory.Inventory;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Represents a standardized container for an anvil.
*/
public class AnvilContainer extends ContainerAnvil implements IContainer {

  @Getter
  private final int containerId;

  @Getter
  private final Inventory inv;

  @Getter
  @Setter
  private int levelCost;

  public AnvilContainer(
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
