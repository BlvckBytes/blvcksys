package me.blvckbytes.blvcksys.packets.communicators.bookeditor;

import me.blvckbytes.blvcksys.packets.IPacketInterceptor;
import me.blvckbytes.blvcksys.packets.IPacketModifier;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.PacketPlayInBEdit;
import net.minecraft.network.protocol.game.PacketPlayInSetCreativeSlot;
import net.minecraft.network.protocol.game.PacketPlayOutSetSlot;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/28/2022

  Creates all packets in regard to managing a book editor GUI and retrieving it's text.
*/
@AutoConstruct
public class BookEditorCommunicator implements IBookEditorCommunicator, IPacketModifier, Listener, IAutoConstructed {

  /**
   * Represents a book edit request's parameters
   * @param fakeItem Fake book item placed in player's hand
   * @param fakeSlot Slot in the player's hotbar the fake item resides in
   * @param callback Book submit callback (list of pages)
   */
  private record BookEditRequest (
    ItemStack fakeItem,
    int fakeSlot,
    Consumer<@Nullable List<String>> callback
  ) {}

  private final MCReflect refl;
  private final JavaPlugin plugin;
  private final ILogger logger;

  // Map of a player to their bookedit request
  private final Map<Player, BookEditRequest> bookeditRequests;

  public BookEditorCommunicator(
    @AutoInject MCReflect refl,
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IPacketInterceptor interceptor
  ) {
    this.refl = refl;
    this.plugin = plugin;
    this.logger = logger;

    this.bookeditRequests = Collections.synchronizedMap(new HashMap<>());
    interceptor.register(this);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public boolean initBookEditor(Player p, List<String> pages, Consumer<List<String>> submit) {
    // Cancel any previous requests
    undoFakeHand(p, true);

    // Create a new book to set at the player's selected slot
    ItemStack book = new ItemStack(Material.WRITABLE_BOOK, 1);

    // Apply all pages
    BookMeta bookMeta = (BookMeta) book.getItemMeta();
    if (bookMeta != null) {
      for (String page : pages)
        bookMeta.addPage(page);
      bookMeta.setAuthor(p.getName());
      book.setItemMeta(bookMeta);
    }

    // Set the book as a fake slot item
    int slot = p.getInventory().getHeldItemSlot();
    if (!setFakeSlot(p, book, slot))
      return false;

    // Register the request
    this.bookeditRequests.put(p, new BookEditRequest(book, slot, submit));
    return true;
  }

  @Override
  public void cleanup() {
    // Cancel all open book requests on unload
    for (Player p : this.bookeditRequests.keySet())
      undoFakeHand(p, true);
  }

  @Override
  public void initialize() {}


  //=========================================================================//
  //                                 Listener                                //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    Player p = e.getPlayer();

    // No active request
    if (!bookeditRequests.containsKey(p))
      return;

    // Undo this fake hand if the player decides to leave the server
    undoFakeHand(p, true);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onInteract(PlayerInteractEvent e) {
    Player p = e.getPlayer();

    // Not a fake book request
    BookEditRequest req = bookeditRequests.get(p);
    if (req == null)
      return;

    // Cancel the event for the server so nothing actually happens
    e.setCancelled(true);

    // Re-set the slot back to the fake item after the gameloop ticked
    // as the client will now have noticed and changed it back
    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
      setFakeSlot(e.getPlayer(), req.fakeItem, req.fakeSlot);
    }, 1);
  }

  @EventHandler
  public void onInvOpen(InventoryOpenEvent e) {
    if (!(e.getPlayer() instanceof Player p))
      return;

    // No active request
    if (!bookeditRequests.containsKey(p))
      return;

    // Undo this fake hand if the player decides to quit writing and open the inventory
    undoFakeHand(p, true);
  }

  @EventHandler
  public void onInvClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player p))
      return;

    // No active request
    if (!bookeditRequests.containsKey(p))
      return;

    // Undo this fake hand if the player decides to quit writing and clicks in their inventory
    undoFakeHand(p, true);

    // Don't allow to get anything on the cursor
    e.setCancelled(true);
  }

  @EventHandler
  public void onHotbarSelect(PlayerItemHeldEvent e) {
    Player p = e.getPlayer();

    // No active request
    if (!bookeditRequests.containsKey(p))
      return;

    // Undo this fake hand if the player decides to quit writing and selects another slot
    undoFakeHand(p, true);
  }

  @EventHandler
  public void onDrop(PlayerDropItemEvent e) {
    Player p = e.getPlayer();

    // No active request
    if (!bookeditRequests.containsKey(p))
      return;

    // Undo this fake hand if the player decides to quit writing and drop the book
    undoFakeHand(p, true);

    // Don't actually drop anything
    e.setCancelled(true);
  }

  //=========================================================================//
  //                                Modifiers                                //
  //=========================================================================//

  @Override
  @SuppressWarnings("unchecked")
  public Packet<?> modifyIncoming(UUID sender, NetworkManager nm, Packet<?> incoming) {
    // Identify the sending player
    Player p = Bukkit.getPlayer(sender);

    // Not a player
    if (p == null)
      return incoming;

    // Player is in creative mode and tried to override a slot
    // This could possibly turn the fake item into a real item, cancel and exit
    if (incoming instanceof PacketPlayInSetCreativeSlot) {
      // This player has no active request, let the packet through
      if (!bookeditRequests.containsKey(p))
        return incoming;

      // Cancel this packet and exit
      undoFakeHand(p, true);
      return null;
    }

    // Is a book edit packet
    if (!(incoming instanceof PacketPlayInBEdit bookEdit))
      return incoming;

    try {
      List<String> pages = refl.getGenericFieldByType(bookEdit, List.class, String.class, 0);
      // Check if the slot isn't vacant and it's a book.
      // If it's not a book, the player couldn't have possible written it
      // without the fake item placed in their inv by this communicator.
      ItemStack iu = p.getInventory().getItem(EquipmentSlot.HAND);
      if (iu != null && (iu.getType() == Material.WRITABLE_BOOK || iu.getType() == Material.WRITTEN_BOOK)) {

        // Otherwise, check if this book has been overlayed by the fake book
        // after a short delay to allow the packet to take effect
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
          ItemStack hand = p.getInventory().getItem(EquipmentSlot.HAND);

          // Seems to have dropped it
          if (hand == null)
            return;

          BookMeta bm = (BookMeta) hand.getItemMeta();

          // Seems to have swapped it
          if (bm == null)
            return;

          // Get the hand's book's pages
          List<String> handPages = bm.getPages();

          // Not what was received, so not a direct edit to a real item
          if (handPages.size() != pages.size()) {
            bookEditReceived(p, handPages);
            return;
          }

          // Compare the two lists
          boolean equal = true;
          for (int i = 0; i < pages.size(); i++) {
            if (pages.get(i).equals(handPages.get(i)))
              continue;

            equal = false;
            break;
          }

          // This real book has been edited
          if (equal)
            return;

          // Has to be a fake book packet
          bookEditReceived(p, handPages);
        }, 5);

        return incoming;
      }

      // Has to be a fake book packet
      bookEditReceived(p, pages);
    } catch (Exception e) {
      logger.logError(e);
    }
    return incoming;
  }

  @Override
  public Packet<?> modifyOutgoing(UUID receiver, NetworkManager nm, Packet<?> outgoing) {
    return outgoing;
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Called whenever a fake book edit packet has been received
   * @param p Target player
   * @param pages Typed out pages
   */
  private void bookEditReceived(Player p, List<String> pages) {
    BookEditRequest request = bookeditRequests.remove(p);

    // No request!
    if (request == null)
      return;

    // Call the handler in a try-catch block to avoid exceptions from
    // disturbing local control flow
    try {
      // Synchronize this callback
      Bukkit.getScheduler().runTask(plugin, () -> request.callback.accept(pages));
    } catch (Exception e) {
      logger.logError(e);
    }

    // Undo the fake hand in non-cancel mode
    undoFakeHand(p, false);
  }

  /**
   * Undo a player's fake hand item by re-sending the real hand from server state
   * @param p Target player
   * @param isCancel Whether or not this is a cancel call
   */
  private void undoFakeHand(Player p, boolean isCancel) {
    p.getInventory().setItemInMainHand(p.getInventory().getItemInMainHand());

    // Just has been cancelled
    if (isCancel)
      bookEditReceived(p, null);
  }

  /**
   * Sets a slot within the player's hotbar to an itemstack in a fake manner (only clientside change)
   * @param p Target player
   * @param is ItemStack to set
   * @param hotbarSlot Hotbar slot ranging from 0 to 8
   * @return Success state
   */
  private boolean setFakeSlot(Player p, ItemStack is, int hotbarSlot) {
    // Invalid slot
    if (hotbarSlot < 0 || hotbarSlot > 8)
      return false;

    // Create slot setting packet to move this fake book into the inventory
    try {
      Object poss = refl.createPacket(PacketPlayOutSetSlot.class);

      // 36 is the first raw slot in the hot-bar
      int slot = 36 + hotbarSlot;

      refl.setFieldByType(poss, int.class, 0, 0); // Window ID (0=inv)
      refl.setFieldByType(poss, int.class, 0, 1); // State ID (leave at zero for now)
      refl.setFieldByType(poss, int.class, slot, 2); // Slot

      // Convert the bukkit item stack to a craft item stack and set the corresponding field
      Class<?> cisC = refl.getClassBKT("inventory.CraftItemStack");
      Object cis = refl.findMethodByName(cisC, "asNMSCopy", ItemStack.class).invoke(null, is);
      refl.setFieldByType(poss, net.minecraft.world.item.ItemStack.class, cis, 0);

      return refl.sendPacket(p, poss);
    } catch (Exception e) {
      logger.logError(e);
      return false;
    }
  }
}
