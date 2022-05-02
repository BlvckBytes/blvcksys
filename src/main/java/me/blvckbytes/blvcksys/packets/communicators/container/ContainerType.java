package me.blvckbytes.blvcksys.packets.communicators.container;

import lombok.Getter;
import me.blvckbytes.blvcksys.util.MCReflect;
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Represents types of containers that can be spawned for a player and maps
  them to their Containers-type and their minecraft class.
*/
public enum ContainerType {
  ANVIL(Containers.h, ContainerAnvil.class),
  GRINDSTONE(Containers.o, ContainerGrindstone.class),
  LOOM(Containers.r, ContainerLoom.class),
  WORKBENCH(Containers.l, ContainerWorkbench.class),
  SMITHING(Containers.u, ContainerSmithing.class),
  STONECUTTER(Containers.x, ContainerStonecutter.class),
  ENCHANTING(Containers.m, CustomContainerEnchantTable.class)
  ;

  @Getter
  private final Containers<?> type;
  private final Class<? extends Container> clazz;

  ContainerType(Containers<?> type, Class<? extends Container> clazz) {
    this.type = type;
    this.clazz = clazz;
  }

  /**
   * Instantiate the selected container by it's parameters
   * @param containerId ID of this container window
   * @param pi PlayerInventory ref
   * @param access ContainerAccess ref
   * @param title Title of the inventory
   * @return Instantiated container
   */
  public Container instantiate(
    int containerId,
    PlayerInventory pi,
    ContainerAccess access,
    IChatBaseComponent title,
    MCReflect refl
  ) throws Exception {

    // Try to instantiate with custom dependencies (for custom impls)
    Container inst;
    try {
      inst = clazz
        .getDeclaredConstructor(int.class, PlayerInventory.class, ContainerAccess.class, MCReflect.class)
        .newInstance(containerId, pi, access, refl);
    }

    // Or just invoke the default constructor
    catch (NoSuchMethodException e) {
       inst = clazz
        .getDeclaredConstructor(int.class, PlayerInventory.class, ContainerAccess.class)
        .newInstance(containerId, pi, access);
    }

    inst.setTitle(title);
    inst.checkReachable = false;

    return inst;
  }
}
