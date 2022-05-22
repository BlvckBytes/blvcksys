package me.blvckbytes.blvcksys.handlers.gui;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  The base of all GUIs which implements basic functionality.
*/
public abstract class AGui<T> implements IAutoConstructed, Listener {

  // The time between GUI ticks
  private static final long TICKER_PERIOD_T = 10L;

  protected final JavaPlugin plugin;

  // Mapping players to their active instances
  private final Map<Player, List<GuiInstance<T>>> activeInstances;
  private int tickerHandle;

  // Inventory title supplier
  @Getter
  private final Function<GuiInstance<T>, ConfigValue> title;

  @Getter
  private final int rows;

  // Mapping slots to their corresponding fixed item
  @Getter
  private final Map<Integer, GuiItem<T>> fixedItems;

  // Set of slots which are reserved for pages (items may differ for every session)
  @Getter
  private final List<Integer> pageSlots;

  /**
   * Create a new GUI template base
   * @param rows Number of rows
   * @param pageSlotExpr Which slots to use for dynamic paging
   * @param title Title supplier for the inventory
   * @param plugin Plugin ref
   */
  protected AGui(
    int rows,
    String pageSlotExpr,
    Function<GuiInstance<T>, ConfigValue> title,
    JavaPlugin plugin
  ) {
    this.rows = rows;
    this.title = title;
    this.plugin = plugin;

    this.pageSlots = AGui.slotExprToSlots(pageSlotExpr, rows);
    this.fixedItems = new HashMap<>();
    this.activeInstances = new HashMap<>();
    this.tickerHandle = -1;
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  /**
   * Show a personalized instance of this GUI to a player
   * @param viewer Inventory viewer
   * @param arg Argument to be passed to the instance
   */
  public void show(Player viewer, T arg) {
    if (!activeInstances.containsKey(viewer))
      activeInstances.put(viewer, new ArrayList<>());

    // Create and register a new GUI instance
    GuiInstance<T> inst = new GuiInstance<>(viewer, this, arg);
    activeInstances.get(viewer).add(inst);

    // Call the opening event before actually opening
    opening(viewer, inst);

    // Initially apply the first page
    inst.updatePage(null);

    // Initially draw the whole gui
    inst.redraw("*");
    inst.open();
  }

  @Override
  public void cleanup() {
    if (tickerHandle > 0)
      Bukkit.getScheduler().cancelTask(tickerHandle);
  }

  @Override
  public void initialize() {
    tickerHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {

      long time = 0;

      @Override
      public void run() {

        // Tick all instances of all players
        for (List<GuiInstance<T>> instances : activeInstances.values()) {
          for (GuiInstance<T> instance : instances) {
            instance.tick(time);
          }
        }

        time += TICKER_PERIOD_T;
      }
    }, 0L, TICKER_PERIOD_T);
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
  abstract protected void opening(Player viewer, GuiInstance<T> inst);

  /**
   * Add a fixed item, which is an item that will always have the same position,
   * no matter of the viewer's state
   * @param slotExpr Slot(s) to set this item to
   * @param item An item supplier which provides the viewer's instance
   * @param onClick Action to run when this item has been clicked
   */
  protected void fixedItem(
    String slotExpr,
    Function<GuiInstance<T>, ItemStack> item,
    @Nullable Consumer<GuiClickEvent<T>> onClick
  ) {
    for (int slotNumber : slotExprToSlots(slotExpr, rows))
      fixedItems.put(slotNumber, new GuiItem<>(item, onClick, null));
  }

  //=========================================================================//
  //                                Listener                                 //
  //=========================================================================//

  @EventHandler
  public void onClick(InventoryClickEvent e) {
    if (!(e.getWhoClicked() instanceof Player p))
      return;

    // The player has no instance
    GuiInstance<T> inst = findByInventory(p, e.getInventory()).orElse(null);
    if (inst == null)
      return;

    e.setCancelled(true);

    // Clicked on a used slot which has a click event bound to it
    GuiItem<T> clicked = inst.getItem(e.getSlot()).orElse(null);
    if (clicked != null && clicked.onClick() != null) {
      clicked.onClick().accept(
        new GuiClickEvent<>(inst, e.getSlot(), e.getClick())
      );
    }
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e) {
    if (!(e.getPlayer() instanceof Player p))
      return;

    // The player has no instance
    GuiInstance<T> inst = findByInventory(p, e.getInventory()).orElse(null);
    if (inst == null)
      return;

    // Destroy the instance
    if (activeInstances.get(p).remove(inst))
      closed(p);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Destroy all instances
    if (activeInstances.containsKey(e.getPlayer()))
      activeInstances.remove(e.getPlayer()).forEach(i -> i.getTemplate().closed(e.getPlayer()));
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Find a gui instance by it's inventory ref
   * @param viewer Viewing player
   * @param inv Inventory ref
   * @return Optional instance, empty if there is no such instance
   */
  private Optional<GuiInstance<T>> findByInventory(Player viewer, Inventory inv) {
    if (!activeInstances.containsKey(viewer))
      return Optional.empty();

    return activeInstances.get(viewer).stream()
      .filter(inst -> inst.getInv().equals(inv))
      .findFirst();
  }

  /**
   * Convert a slot expression to a set of slot indices
   * @param slotExpr Slot expression
   */
  public static List<Integer> slotExprToSlots(String slotExpr, int rows) {
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
        if (i < 0 || i >= rows * 9)
          continue;

        slots.add(i);
      }
    }

    // Sort slots in ascending order to start appending top left
    return slots.stream()
      .sorted()
      .toList();
  }
}
