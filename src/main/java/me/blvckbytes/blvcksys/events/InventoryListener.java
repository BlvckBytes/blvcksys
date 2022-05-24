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

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/24/2022

  Listens to inventory changes and translates them into custom
  events for a more convenient way of handling them around the system.
*/
@AutoConstruct
public class InventoryListener implements Listener {

  // TODO: Fix and rework hotbar interaction processing

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

    System.out.println("action=" + e.getAction());

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

        from = p.getInventory();
        to = top;

        int invSlot = -1;
        for (int slot : generateInventorySlots(to.getSize(), false)) {
          ItemStack curr = to.getItem(slot);

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
      }

      // Moved down into own inventory
      else {
        ItemStack moved = top.getItem(e.getSlot());

        from = top;
        to = p.getInventory();

        int invSlot = -1;

        for (int slot : generateInventorySlots(to.getSize(), true)) {
          ItemStack curr = to.getItem(slot);

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

        targetSlot = invSlot;
      }

      // No matching stacks found, try to find the first empty to-inv slot
      if (targetSlot < 0) {

        for (int slot : generateInventorySlots(to.getSize(), true)) {
          if (to.getItem(slot) == null) {
            targetSlot = slot;
            break;
          }
        }
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
      // The clicked slot will always be within the set
      Set<Integer> sourceSlots = new HashSet<>();
      sourceSlots.add(e.getSlot());

      // Collected all similar items to the cursor
      if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR && e.getCursor() != null) {
        Inventory inv = e.getClickedInventory();
        ItemStack cursor = e.getCursor();
        int remaining = cursor.getMaxStackSize() - cursor.getAmount();

        // While there's still space on the cursor stack, search for further items and their slots
        for (int slot : generateInventorySlots(inv.getSize(), inv.equals(p.getInventory()))) {
          ItemStack curr = inv.getItem(slot);

          if (remaining <= 0)
            break;

          if (curr == null || !curr.isSimilar(cursor))
            continue;

          sourceSlots.add(slot);
          remaining -= curr.getAmount();
        }
      }

      if (sourceSlots.stream().anyMatch(slot -> checkCancellation(e.getClickedInventory(), p, ManipulationAction.PICKUP, slot)))
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
   *
   * @param inv    Inventory of action
   * @param p      Event causing player
   * @param action Action that has been taken
   * @param slot   Slot of action
   * @return True if the action needs to be cancelled
   */
  private boolean checkCancellation(Inventory inv, Player p, ManipulationAction action, int slot) {
    return checkCancellation(inv, inv, p, action, slot, slot);
  }

  /**
   * Check whether the expressed action has been cancelled by any event receiver
   *
   * @param fromInv  Inventory that has been taken from
   * @param toInv    Inventory that has been added to
   * @param p        Event causing player
   * @param action   Action that has been taken
   * @param fromSlot Slot that has been taken from
   * @param toSlot   Slot that has been added to
   * @return True if the action needs to be cancelled
   */
  private boolean checkCancellation(Inventory fromInv, Inventory toInv, Player p, ManipulationAction action, int fromSlot, int toSlot) {
    InventoryManipulationEvent ime = new InventoryManipulationEvent(
      fromInv, toInv, p, action, fromSlot, toSlot
    );

    Bukkit.getPluginManager().callEvent(ime);
    return ime.isCancelled();
  }

  /**
   * Generate a set of slots to iterate over in order to check for slots in
   * an inventory when figuring out where moved items will end up. For inventories
   * on top, rows are iterated in natural direction, while the player's inventory
   * requires iterating in reversed rows to mimic the real game mechanics.
   *
   * @param size         Size of the inventory
   * @param reversedRows Whether to reverse individual rows
   * @return Set of slots to iterate over when searching
   */
  private Set<Integer> generateInventorySlots(int size, boolean reversedRows) {
    if (!reversedRows)
      return IntStream.range(0, size).boxed().collect(Collectors.toSet());

    Set<Integer> slots = new HashSet<>();

    for (int row = 0; row < size / 9; row++) {
      int first = row * 9, last = first + 8;
      for (int slot = last; slot >= first; slot--)
        slots.add(slot);
    }

    return slots;
  }
}
