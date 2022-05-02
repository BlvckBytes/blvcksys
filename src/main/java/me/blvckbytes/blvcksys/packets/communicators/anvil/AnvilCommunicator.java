package me.blvckbytes.blvcksys.packets.communicators.anvil;

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
import net.minecraft.network.chat.IChatBaseComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayOutCloseWindow;
import net.minecraft.network.protocol.game.PacketPlayOutOpenWindow;
import net.minecraft.network.protocol.game.PacketPlayOutWindowData;
import net.minecraft.world.entity.player.PlayerInventory;
import net.minecraft.world.inventory.Container;
import net.minecraft.world.inventory.ContainerAccess;
import net.minecraft.world.inventory.ContainerAnvil;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.units.qual.A;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/02/2022

  Creates all packets in regard to opening virtual fully functional anvils for players.
*/
@AutoConstruct
public class AnvilCommunicator implements IAnvilCommunicator, IPacketModifier, Listener, IAutoConstructed {

  private final Map<Player, AnvilContainer> anvils;

  private final MCReflect refl;
  private final ILogger logger;
  private final IGiveCommand give;

  public AnvilCommunicator(
    @AutoInject MCReflect refl,
    @AutoInject ILogger logger,
    @AutoInject IGiveCommand give
  ) {
    this.refl = refl;
    this.logger = logger;
    this.give = give;

    this.anvils = new HashMap<>();
  }

  private static class AnvilContainer extends ContainerAnvil {

    private final int containerId;
    private final Inventory inv;
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

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public boolean openFunctionalAnvil(Player p, String title) {
    return createAnvilContainer(p, title)
      .map(container -> openAnvilContainer(p, container))
      .orElse(false);
  }

  @Override
  public void cleanup() {
    // Close all anvils and release their items
    for (Player t : Bukkit.getOnlinePlayers())
      clearAnvil(t, true);
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

    clearAnvil(p, false);
  }

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player p))
      return;

    // Get an active anvil container from this player
    AnvilContainer ac = this.anvils.get(p);
    if (ac == null)
      return;

    // Not this inventory
    if (!ac.inv.equals(e.getClickedInventory()))
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
      p.setLevel(p.getLevel() - ac.levelCost);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Make a new anvil container for a specific player
   * @param p Player to make for
   * @param title Inventory title
   * @return Optional AnvilContainer, empty on errors
   */
  private Optional<AnvilContainer> createAnvilContainer(Player p, String title) {
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

      return Optional.of(new AnvilContainer(counter, pi, access, new ChatComponentText(title)));
    } catch (Exception e) {
      logger.logError(e);
      return Optional.empty();
    }
  }

  /**
   * Open a previously created anvil container for a player
   * @param p Target player
   * @param container Created container
   * @return Success state
   */
  private boolean openAnvilContainer(Player p, AnvilContainer container) {
    try {
      Object ep = refl.getEntityPlayer(p);

      // Set the active container on the player
      refl.setFieldByType(ep, Container.class, container, 0);

      // Open the window
      Object pow = new PacketPlayOutOpenWindow(
        container.containerId,
        Containers.h,
        container.getTitle()
      );

      // Set active slot listener
      refl.invokeMethodByArgsOnly(ep, new Class[] { Container.class }, container);

      // Register locally
      this.anvils.put(p, container);

      return refl.sendPacket(p, pow);
    } catch (Exception e) {
      logger.logError(e);
      return false;
    }
  }

  /**
   * Clear an anvil from the local management, close the window and give
   * the player back their items
   * @param p Target player
   * @param close Whether to close the inventory
   */
  private void clearAnvil(Player p, boolean close) {
    // Get an active anvil container from this player
    AnvilContainer ac = this.anvils.get(p);
    if (ac == null)
      return;

    // Remove this anvil again
    this.anvils.remove(p);

    // Hand back all items to the player (or drop them)
    for (ItemStack item : ac.inv.getContents()) {
      if (item != null && item.getType() != Material.AIR)
        give.giveItemsOrDrop(p, item);
    }

    if (!close)
      return;

    // Close this inventory
    try {
      Object pcw = refl.createPacket(PacketPlayOutCloseWindow.class);
      refl.setFieldByType(pcw, int.class, ac.containerId, 0);
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

    // Get the current anvil
    AnvilContainer ac = this.anvils.get(p);

    // Read the level-cost if the window-id matches
    try {
      int wid = refl.getFieldByType(wd, int.class, 0);
      if (wid == ac.containerId)
        ac.levelCost = refl.getFieldByType(wd, int.class, 2);
    } catch (Exception e) {
      logger.logError(e);
    }

    return outgoing;
  }
}
