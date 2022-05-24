package me.blvckbytes.blvcksys.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/24/2022

  Represents a pre-analyzed change in an inventory.
*/
@Getter
public class InventoryManipulationEvent extends Event implements Cancellable {

  private static final HandlerList HANDLERS = new HandlerList();

  // Origin has always been taken from and target inventory is where the items are taken to
  // If an action only affects one slot, origin will be equal to target
  private final Inventory originInventory, targetInventory;

  private final Player player;
  private final ManipulationAction action;
  private final int originSlot, targetSlot;
  private final ClickType click;

  @Setter
  private boolean cancelled;

  public InventoryManipulationEvent(
    Inventory originInventory,
    Inventory targetInventory,
    Player player,
    ManipulationAction action,
    int originSlot,
    int targetSlot,
    ClickType click
  ) {
    this.originInventory = originInventory;
    this.targetInventory = targetInventory;
    this.player = player;
    this.action = action;
    this.originSlot = originSlot;
    this.targetSlot = targetSlot;
    this.click = click;
  }

  @Override
  public HandlerList getHandlers() {
    return HANDLERS;
  }

  public static HandlerList getHandlerList() {
    return HANDLERS;
  }

  @Override
  public String toString() {
    return (
      "InventoryManipulationEvent (\n" +
      "  player=" + player.getName() + "\n" +
      "  originSlot=" + originSlot + "\n" +
      "  targetSlot=" + targetSlot + "\n" +
      "  action=" + action + "\n" +
      "  originInventoryHolder=" + originInventory.getHolder() + "\n" +
      "  targetInventoryHolder=" + targetInventory.getHolder() + "\n" +
      ")"
    );
  }
}
