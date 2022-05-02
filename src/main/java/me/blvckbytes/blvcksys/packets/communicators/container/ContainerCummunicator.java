package me.blvckbytes.blvcksys.packets.communicators.container;

import me.blvckbytes.blvcksys.commands.IGiveCommand;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.core.BlockPosition;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.chat.ChatComponentText;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.network.protocol.game.PacketPlayOutWindowData;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.Containers;
import net.minecraft.world.level.World;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Creates all packets in regard to opening virtual fully functional containers for players.
*/
@AutoConstruct
public class ContainerCummunicator implements IContainerCommunicator, IPacketModifier, Listener, IAutoConstructed {

  private final Map<Player, IContainer> containers;

  private final MCReflect refl;
  private final ILogger logger;
  private final IGiveCommand give;

  public ContainerCummunicator(
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger,
    @AutoInject IGiveCommand give
  ) {
    this.refl = refl;
    this.logger = logger;
    this.give = give;

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
  public void cleanup() {
    // Close all containers and release their items
    for (Player t : Bukkit.getOnlinePlayers())
      clearContainer(t, true);
  }

  @Override
  public void initialize() {}

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @EventHandler
  public void onClose(InventoryCloseEvent e) {
    if (!(e.getPlayer() instanceof Player p))
      return;

    clearContainer(p, false);
  }

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player p))
      return;

    // Get an active container from this player
    IContainer cont = this.containers.get(p);
    if (cont == null)
      return;

    // Is not an anvil
    if (!(cont instanceof AnvilContainer ac))
      return;

    // Not this inventory
    if (!ac.getInv().equals(e.getClickedInventory()))
      return;

    // Is in creative, level-costst are not subtracted
    if (p.getGameMode() == GameMode.CREATIVE)
      return;

    // Remove the level cost from the player's levels
    if (
      e.getRawSlot() == 2 && // Output slot
        e.getCurrentItem() != null &&
        e.getCurrentItem().getType() != Material.AIR
    )
      p.setLevel(p.getLevel() - ac.getLevelCost());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Make a new container for a specific player
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

      // Create a new container access from the world-server and an empty block-position (fake)
      ContainerAccess access = ContainerAccess.a((World) worldHandle, new BlockPosition(0, 0, 0));

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

      throw new IllegalArgumentException("Cannot create container of type " + type);
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }

  /**
   * Open a previously created container for a player
   * @param p Target player
   * @param container Created container
   * @param type Type of the inventory to open
   * @return Success state
   */
  private boolean openContainer(Player p, IContainer container, Containers<?> type) {
    try {
      Object ep = refl.getEntityPlayer(p);

      // Set the active container on the player
      refl.setFieldByType(ep, Container.class, container, 0);

      // Open the window
      Object pow = new PacketPlayOutOpenWindow(
        container.getContainerId(),
        type,
        container.getTitle()
      );

      // Set active slot listener
      refl.invokeMethodByArgsOnly(ep, new Class[] { Container.class }, container);

      // Register locally
      this.containers.put(p, container);

      return refl.sendPacket(p, pow);
    } catch (Exception e) {
      logger.logError(e);
      return false;
    }
  }

  /**
   * Clear a container from the local management, close the window and give
   * the player back their items
   * @param p Target player
   * @param close Whether to close the inventory
   */
  private void clearContainer(Player p, boolean close) {
    // Get an active container from this player
    IContainer cont = this.containers.get(p);
    if (cont == null)
      return;

    // Remove this container again
    this.containers.remove(p);

    // Only "manually" hand back items for anvils
    if (cont instanceof AnvilContainer) {
      // Hand back all items to the player (or drop them)
      for (ItemStack item : cont.getInv().getContents()) {
        if (item != null && item.getType() != Material.AIR)
          give.giveItemsOrDrop(p, item);
      }
    }

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

  //=========================================================================//
  //                                Modifiers                                //
  //=========================================================================//

  @Override
  public Packet<?> modifyIncoming(UUID sender, NetworkManager nm, Packet<?> incoming) {
    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(UUID receiver, NetworkManager nm, Packet<?> outgoing) {
    // Only act on window-data packets
    if (!(outgoing instanceof PacketPlayOutWindowData wd))
      return outgoing;

    // Get the corresponding player
    Player p = Bukkit.getPlayer(receiver);
    if (p == null)
      return outgoing;

    // Get the current container
    IContainer cont = this.containers.get(p);

    // Not an anvil
    if (!(cont instanceof AnvilContainer ac))
      return outgoing;

    // Read the level-cost if the window-id matches
    try {
      int wid = refl.getFieldByType(wd, int.class, 0);
      if (wid == cont.getContainerId())
        ac.setLevelCost(refl.getFieldByType(wd, int.class, 2));
    } catch (Exception e) {
      logger.logError(e);
    }

    return outgoing;
  }
}
