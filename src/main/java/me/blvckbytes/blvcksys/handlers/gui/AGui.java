package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
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

  protected final IConfig cfg;
  protected final JavaPlugin plugin;

  // Mapping players to their active instance
  private final Map<Player, GuiInstance> instances;

  // Inventory title supplier
  private final Function<Player, ConfigValue> title;

  private final int rows;
  private int tickerHandle;

  // Mapping slots to a tuple of the item supplier as well as the click event consumer
  private final Map<Integer, GuiItem> items;

  protected AGui(int rows, IConfig cfg, JavaPlugin plugin, Function<Player, ConfigValue> title) {
    this.rows = rows;
    this.cfg = cfg;
    this.plugin = plugin;
    this.title = title;

    this.items = new HashMap<>();
    this.instances = new HashMap<>();
    this.tickerHandle = -1;

    createTicker();
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
    GuiInstance inst = new GuiInstance(viewer, inv, title, items);
    instances.put(viewer, inst);

    opening(viewer, inst);
    redraw(viewer, "*");

    viewer.openInventory(inv);
  }

  //=========================================================================//
  //                                Internals                                //
  //=========================================================================//

  /**
   * Called when a GUI has been closed by a player
   * @param viewer Player that closed a GUI
   */
  abstract protected void closed(Player viewer);

  /**
   * Called before a GUI is being shown to a player
   * @param viewer Player that requested a GUI
   */
  abstract protected void opening(Player viewer, GuiInstance inst);

  /**
   * Add an item to multiple slots using a slot expression
   * @param slotExpr Slot expression, where ranges are expressed as from-to where
   *                 both from and to are inclusive and multiple ranges or single slots
   *                 can be combined by a separating comma. Example: 0-5,8,10,15-20
   *                 To target all available slots, use the asterisk *
   * @param item An item supplier which provides the viewing player and the current slot number
   * @param onClick Action to run when this item has been clicked
   */
  protected void withItem(
    String slotExpr,
    BiFunction<Player, Integer, ItemStackBuilder> item,
    @Nullable BiConsumer<Player, Integer> onClick
  ) {
    for (int slotNumber : slotExprToSlots(slotExpr, rows))
      items.put(slotNumber, new GuiItem((p) -> item.apply(p, slotNumber), onClick, null));
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
    for (int slotNumber : slotExprToSlots(slotExpr, rows)) {
      if (!inst.getItems().containsKey(slotNumber))
        continue;

      inst.getInv().setItem(
        slotNumber,
        inst.getItems().get(slotNumber).item()
          .apply(viewer)
          .build()
      );
    }
  }

  /**
   * Stop the ticker task
   */
  protected void stopTicker() {
    if (tickerHandle > 0)
      Bukkit.getScheduler().cancelTask(tickerHandle);
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

    if (!inst.getInv().equals(e.getClickedInventory()))
      return;

    e.setCancelled(true);

    if (inst.getItems().containsKey(e.getSlot())) {
      BiConsumer<Player, Integer> cb = inst.getItems().get(e.getSlot()).onClick();

      if (cb != null)
        cb.accept(p, e.getSlot());
    }
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e) {
    if (!(e.getPlayer() instanceof Player p))
      return;

    GuiInstance inst = instances.get(p);
    if (inst == null)
      return;

    if (!inst.getInv().equals(e.getInventory()))
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
  public static Set<Integer> slotExprToSlots(String slotExpr, int rows) {
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

  /**
   * Create a new ticker task which ticks all available instances
   */
  private void createTicker() {
    tickerHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
      long time = 0;

      @Override
      public void run() {
        instances.values().forEach(inst -> inst.tick(time));
        time += 10;
      }
    }, 0L, 10L);
  }
}
