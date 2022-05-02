package me.blvckbytes.blvcksys.packets.communicators.container;

import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.protocol.game.PacketPlayOutCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerPlayer;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.level.World;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Creates all packets in regard to opening virtual fully functional containers for players.
*/
@AutoConstruct
public class ContainerCummunicator implements IContainerCommunicator, Listener, IAutoConstructed {

  private final Map<Player, IContainer> containers;

  private final MCReflect refl;
  private final ILogger logger;

  public ContainerCummunicator(
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger
  ) {
    this.refl = refl;
    this.logger = logger;

    this.containers = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public boolean openFunctionalAnvil(Player p, String title) {
    return createContainer(p, title, Containers.h)
      .map(container -> openContainer(p, container, Containers.h))
      .orElse(false);
  }

  @Override
  public boolean openFunctionalWorkbench(Player p, String title) {
    return createContainer(p, title, Containers.l)
      .map(container -> openContainer(p, container, Containers.l))
      .orElse(false);
  }

  @Override
  public boolean openFunctionalGrindstone(Player p, String title) {
    return createContainer(p, title, Containers.o)
      .map(container -> openContainer(p, container, Containers.o))
      .orElse(false);
  }

  @Override
  public void cleanup() {
    // Close all containers and release their items
    for (Player t : Bukkit.getOnlinePlayers())
      clearContainer(t, true);
  }

  @Override
  public void initialize() {
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @EventHandler
  public void onClose(InventoryCloseEvent e) {
    if (!(e.getPlayer() instanceof Player p))
      return;

    clearContainer(p, false);
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
  private Optional<IContainer> createContainer(Player p, String title, Containers<?> type) {
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

      if (type == Containers.h) {
        return Optional.of(new AnvilContainer(counter, pi, access, new ChatComponentText(title)));
      }

      if (type == Containers.l) {
        return Optional.of(new WorkBenchContainer(counter, pi, access, new ChatComponentText(title)));
      }

      if (type == Containers.o) {
        return Optional.of(new GrindStoneContainer(counter, pi, access, new ChatComponentText(title)));
      }

      throw new IllegalArgumentException("Cannot create container of type " + type);
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
  private boolean openContainer(Player p, IContainer container, Containers<?> type) {
    try {
      Object ep = refl.getEntityPlayer(p);

      // Re-set the active container on the player
      refl.setFieldByType(ep, Container.class, refl.getFieldByType(ep, ContainerPlayer.class, 0), 0);

      // Set the active container on the player
      refl.setFieldByType(ep, Container.class, container, 0);

      // Open the window
      Object pow = new PacketPlayOutOpenWindow(
        container.getContainerId(),
        type,
        container.getTitle()
      );

      // Set active slot listener
      refl.invokeMethodByArgsOnly(ep, new Class[]{Container.class}, container);

      // Register locally
      this.containers.put(p, container);

      return refl.sendPacket(p, pow);
    } catch (Exception e) {
      logger.logError(e);
      return false;
    }
  }

  /**
   * Clear a container from the local management
   *
   * @param p     Target player
   * @param close Whether to close the inventory
   */
  private void clearContainer(Player p, boolean close) {
    // Get an active container from this player
    IContainer cont = this.containers.get(p);
    if (cont == null)
      return;

    // Remove this container again
    this.containers.remove(p);

    if (!close)
      return;

    // Close this inventory
    try {
      Object pcw = refl.createPacket(PacketPlayOutCloseWindow.class);
      refl.setFieldByType(pcw, int.class, cont.getContainerId(), 0);
      refl.sendPacket(p, pcw);
    } catch (Exception e) {
      logger.logError(e);
    }
  }
}
