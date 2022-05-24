package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/24/2022

  Listens to inventory changes and translates them into custom
  events for a more convenient way of handling them around the system.
*/
@AutoConstruct
public class InventoryListener implements Listener {

  // TODO: Fix and rework hotbar interaction processing
  // TODO: Collect to cursor needs to emit a pickup for all affected slots

  @EventHandler
  public void onManip(InventoryManipulationEvent e) {
    System.out.println(e);
  }

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player p))
      return;

    // Clicked into void
    if (e.getClickedInventory() == null)
      return;

    System.out.println(e.getAction());

    // Moved from one inventory into another
    if (
      e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY ||
      e.getAction() == InventoryAction.HOTBAR_MOVE_AND_READD
    ) {
      Inventory top = e.getView().getTopInventory();

      // Fallback: Moved down into own inventory
      Inventory from;
      Inventory to;
      int targetSlot;

      // Move up into foreign inventory
      if (!top.equals(e.getClickedInventory())) {
        ItemStack moved = p.getInventory().getItem(e.getSlot());

        int invSlot = -1;
        for (int slot = 0; slot < top.getSize(); slot++) {
          ItemStack curr = top.getItem(slot);

          if (
            // First empty slot encountered, use that for now
            (invSlot < 0 && curr == null) || (
            // Target inv slot is similar to the moved item
            curr != null && curr.isSimilar(moved) &&
            // And there's enough space left to stack something onto it
            (curr.getMaxStackSize() - curr.getAmount()) > 0)
          )
            invSlot = slot;
        }

        targetSlot = invSlot;
        from = p.getInventory();
        to = top;
      }

      // Moved down into own inventory
      else {
        ItemStack moved = top.getItem(e.getSlot());

        // For some reason, it starts to place items in the rightmost available
        // slot, so search in that same order

        int invSlot = -1;

        for (int row = 0; row < p.getInventory().getSize() / 9; row++) {
          int first = row * 9, last = first + 8;

          for (int slot = last; slot >= first; slot--) {
            ItemStack curr = p.getInventory().getItem(slot);

            if (
              // Target inv slot is similar to the moved item
              curr != null && curr.isSimilar(moved) &&
              // And there's enough space left to stack something onto it
              (curr.getMaxStackSize() - curr.getAmount()) > 0
            ) {
              invSlot = slot;
              break;
            }
          }
        }

        // No matching stacks found, try to find the first empty inv slot
        if (invSlot < 0) {
          for (int row = 0; row < p.getInventory().getSize() / 9; row++) {
            int first = row * 9, last = first + 8;

            for (int slot = last; slot >= first; slot--) {
              if (p.getInventory().getItem(slot) != null)
                continue;

              invSlot = slot;
              break;
            }
          }
        }

        from = top;
        to = p.getInventory();
        targetSlot = invSlot;
      }

      // No more space to move into
      if (targetSlot < 0)
        return;

      if (checkCancellation(from, to, p, ManipulationAction.MOVE, e.getSlot(), targetSlot))
        e.setCancelled(true);
      return;
    }

    // Picked up any number of items
    if (
      e.getAction() == InventoryAction.PICKUP_ALL ||
      e.getAction() == InventoryAction.PICKUP_HALF ||
      e.getAction() == InventoryAction.PICKUP_ONE ||
      e.getAction() == InventoryAction.PICKUP_SOME ||
      e.getAction() == InventoryAction.COLLECT_TO_CURSOR
    ) {
      if (checkCancellation(e.getClickedInventory(), p, ManipulationAction.PICKUP, e.getSlot()))
        e.setCancelled(true);
      return;
    }

    // Placed down any number of items
    if (
      e.getAction() == InventoryAction.PLACE_ALL ||
      e.getAction() == InventoryAction.PLACE_ONE ||
      e.getAction() == InventoryAction.PLACE_SOME
    ) {
      if (checkCancellation(e.getClickedInventory(), p, ManipulationAction.PLACE, e.getSlot()))
        e.setCancelled(true);
      return;
    }

    // Swapped a slot with the cursor contents
    if (e.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
      if (checkCancellation(e.getClickedInventory(), p, ManipulationAction.SWAP, e.getSlot()))
        e.setCancelled(true);
      return;
    }

    // Dropped any number of items from a slot
    if (
      e.getAction() == InventoryAction.DROP_ONE_SLOT ||
      e.getAction() == InventoryAction.DROP_ALL_SLOT
    ) {
      if (checkCancellation(e.getClickedInventory(), p, ManipulationAction.DROP, e.getSlot()))
        e.setCancelled(true);
      return;
    }

    if (checkCancellation(e.getClickedInventory(), p, ManipulationAction.CLICK, e.getSlot()))
      e.setCancelled(true);
  }

  @EventHandler
  public void onDrag(InventoryDragEvent e) {
    if (!(e.getWhoClicked() instanceof Player p))
      return;

    // Check whether this drag event needs to be cancelled by firing an individual
    // place event for each slot (because that's what in effect occurs). If any event
    // receiver cancels any of the slots, the whole drag event needs to be cancelled.
    boolean cancel = e.getInventorySlots().stream()
      .anyMatch(slot -> checkCancellation(e.getInventory(), p, ManipulationAction.PLACE, slot));

    if (cancel)
      e.setCancelled(true);
  }

  /**
   * Check whether the expressed action has been cancelled by any event receiver
   * @param inv Inventory of action
   * @param p Event causing player
   * @param action Action that has been taken
   * @param slot Slot of action
   * @return True if the action needs to be cancelled
   */
  private boolean checkCancellation(Inventory inv, Player p, ManipulationAction action, int slot) {
    return checkCancellation(inv, inv, p, action, slot, slot);
  }

  /**
   * Check whether the expressed action has been cancelled by any event receiver
   * @param fromInv Inventory that has been taken from
   * @param toInv Inventory that has been added to
   * @param p Event causing player
   * @param action Action that has been taken
   * @param fromSlot Slot that has been taken from
   * @param toSlot Slot that has been added to
   * @return True if the action needs to be cancelled
   */
  private boolean checkCancellation(Inventory fromInv, Inventory toInv, Player p, ManipulationAction action, int fromSlot, int toSlot) {
    InventoryManipulationEvent ime = new InventoryManipulationEvent(
      fromInv, toInv, p, action, fromSlot, toSlot
    );

    Bukkit.getPluginManager().callEvent(ime);
    return ime.isCancelled();
  }
}
