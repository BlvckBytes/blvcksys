package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.IntStream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  The base of all GUIs which implements basic functionality.
*/
public abstract class AGui implements Listener {

  private record GuiInstance(
    Player viewer,
    Inventory inv,
    Function<Player, ConfigValue> title,
    Map<Integer, Tuple<Function<Player, ItemStackBuilder>, BiConsumer<Player, Integer>>> items
  ) {}

  protected final IConfig cfg;

  // Mapping players to their active instance
  private final Map<Player, GuiInstance> instances;
  private final Function<Player, ConfigValue> title;
  private final int rows;
  private final Map<Integer, Tuple<Function<Player, ItemStackBuilder>, BiConsumer<Player, Integer>>> items;

  protected AGui(int rows, IConfig cfg, Function<Player, ConfigValue> title) {
    this.rows = rows;
    this.cfg = cfg;
    this.title = title;

    this.items = new HashMap<>();
    this.instances = new HashMap<>();

    setupItems(rows);
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  /**
   * Show a personalized instance of this GUI to a player
   * @param viewer Inventory viewer
   */
  public void show(Player viewer) {
    Inventory inv = Bukkit.createInventory(null, rows * 9, title.apply(viewer).asScalar());
    instances.put(viewer, new GuiInstance(viewer, inv, title, items));

    opening(viewer);
    redraw(viewer, "*");

    viewer.openInventory(inv);
  }

  //=========================================================================//
  //                                Internals                                //
  //=========================================================================//

  /**
   * Called right after constructing in order to set up all Items
   * within the GUI using {@link #withItem(String, BiFunction, BiConsumer)}
   * @param rows Number of rows this GUI has
   */
  abstract protected void setupItems(int rows);

  /**
   * Called when a GUI has been closed by a player
   * @param viewer Player that closed a GUI
   */
  abstract protected void closed(Player viewer);

  /**
   * Called before a GUI is being shown to a player
   * @param viewer Player that requested a GUI
   */
  abstract protected void opening(Player viewer);

  /**
   * Add an item to multiple slots using a slot expression
   * @param slotExpr Slot expression, where ranges are expressed as from-to where
   *                 both from and to are inclusive and multiple ranges or single slots
   *                 can be combined by a separating comma. Example: 0-5,8,10,15-20
   *                 To target all available slots, use the asterisk *
   * @param item An item supplier which provides the viewing player and the current slot number
   * @param onClick Action to run when this item has been clicked
   */
  protected AGui withItem(
    String slotExpr,
    BiFunction<Player, Integer, ItemStackBuilder> item,
    @Nullable BiConsumer<Player, Integer> onClick
  ) {
    for (int slotNumber : slotExprToSlots(slotExpr))
      items.put(slotNumber, new Tuple<>((p) -> item.apply(p, slotNumber), onClick));
    return this;
  }

  /**
   * Redraw a specified set of slots for a given player
   * @param viewer Viewer to redraw for
   * @param slotExpr Same as with {@link #withItem(String, BiFunction, BiConsumer)}
   */
  protected void redraw(Player viewer, String slotExpr) {
    if (!instances.containsKey(viewer))
      return;

    GuiInstance inst = instances.get(viewer);
    for (int slotNumber : slotExprToSlots(slotExpr)) {
      if (!inst.items.containsKey(slotNumber))
        continue;

      inst.inv.setItem(
        slotNumber,
        inst.items.get(slotNumber).a()
          .apply(viewer)
          .build()
      );
    }
  }

  //=========================================================================//
  //                                Listener                                 //
  //=========================================================================//

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player p))
      return;

    GuiInstance inst = instances.get(p);
    if (inst == null)
      return;

    if (!inst.inv.equals(e.getClickedInventory()))
      return;

    e.setCancelled(true);

    if (inst.items.containsKey(e.getSlot()))
      inst.items.get(e.getSlot()).b().accept(p, e.getSlot());;
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e) {
    if (!(e.getPlayer() instanceof Player p))
      return;

    GuiInstance inst = instances.get(p);
    if (inst == null)
      return;

    if (!inst.inv.equals(e.getInventory()))
      return;

    instances.remove(p);
    closed(p);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Convert a slot expression to a set of slot indices
   * @param slotExpr Slot expression
   */
  private Set<Integer> slotExprToSlots(String slotExpr) {
    Set<Integer> slots = new HashSet<>();

    for (String range : slotExpr.split(",")) {
      String[] rangeData = range.split("-");

      if (rangeData[0].equals("*")) {
        IntStream.range(0, rows * 9).forEach(slots::add);
        continue;
      }

      int from = Integer.parseInt(rangeData[0]);

      if (rangeData.length == 1) {
        slots.add(from);
        continue;
      }

      for (int i = from; i <= Integer.parseInt(rangeData[1]); i++) {
        if (i <= 0 || i >= rows * 9)
          continue;

        slots.add(i);
      }
    }

    return slots;
  }
}
