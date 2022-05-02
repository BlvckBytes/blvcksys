package me.blvckbytes.blvcksys.packets.communicators.container;

import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerPlayer;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.level.World;
import org.bukkit.entity.Player;

import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Creates all packets in regard to opening virtual fully functional containers for players.
*/
@AutoConstruct
public class ContainerCummunicator implements IContainerCommunicator {

  private final MCReflect refl;
  private final ILogger logger;

  public ContainerCummunicator(
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger
  ) {
    this.refl = refl;
    this.logger = logger;
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public boolean openContainer(Player p, ContainerType type, String title) {
    return createContainer(p, title, type)
      .map(container -> openContainer(p, container, type.getType()))
      .orElse(false);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Make a new container for a specific player
   *
   * @param p Player to make for
   * @param title Inventory title
   * @param type Type of the container
   * @return Optional IContainer, empty on errors
   */
  private Optional<Container> createContainer(Player p, String title, ContainerType type) {
    try {
      Object cp = refl.getCraftPlayer(p);

      // Get the craft-world CraftPlayer#getWorld
      Object cw = refl.findMethodByName(cp.getClass(), "getWorld").invoke(cp);

      // Get the world-server CraftWorld#getHandle
      Object worldHandle = refl.findMethodByName(cw.getClass(), "getHandle").invoke(cw);

      // Create a new container access from the world-server and a block-position at the player
      ContainerAccess access = ContainerAccess.a((World) worldHandle, new BlockPosition(
        p.getLocation().getX(), p.getLocation().getY(), p.getLocation().getZ()
      ));

      Object ep = refl.getEntityPlayer(p);

      // Get the PlayerInventory within EntityPlayer
      PlayerInventory pi = refl.getFieldByType(ep, PlayerInventory.class, 0);

      // Invoke EntityPlayer#nextContainerCounter
      int counter = (int) refl.findMethodByName(ep.getClass(), "nextContainerCounter").invoke(ep);

      // Create container
      return Optional.of(type.instantiate(counter, pi, access, new ChatComponentText(title)));
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }

  /**
   * Open a previously created container for a player
   *
   * @param p         Target player
   * @param container Created container
   * @param type      Type of the inventory to open
   * @return Success state
   */
  private boolean openContainer(Player p, Container container, Containers<?> type) {
    try {
      Object ep = refl.getEntityPlayer(p);

      // Re-set the active container on the player
      refl.setFieldByType(ep, Container.class, refl.getFieldByType(ep, ContainerPlayer.class, 0), 0);

      // Set the active container on the player
      refl.setFieldByType(ep, Container.class, container, 0);

      // Open the window
      Object pow = new PacketPlayOutOpenWindow(
        container.j,
        type,
        container.getTitle()
      );

      // Set active slot listener
      refl.invokeMethodByArgsOnly(ep, new Class[]{Container.class}, container);

      return refl.sendPacket(p, pow);
    } catch (Exception e) {
      logger.logError(e);
      return false;
    }
  }
}
