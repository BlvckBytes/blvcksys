package me.blvckbytes.blvcksys.handlers.gui;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.events.InventoryManipulationEvent;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  The base of all GUIs which implements basic functionality.
*/
public abstract class AGui<T> implements IAutoConstructed, Listener {

  protected final JavaPlugin plugin;
  protected final IConfig cfg;

  // Mapping players to their active instances
  @Getter
  private final Map<Player, Set<GuiInstance<T>>> activeInstances;

  private int tickerHandle;

  // Inventory title supplier
  @Getter
  @Setter(AccessLevel.PROTECTED)
  private Function<GuiInstance<T>, ConfigValue> title;

  @Getter
  private final int rows;

  @Getter
  private final InventoryType type;

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
    JavaPlugin plugin,
    IConfig cfg
  ) {
    this(rows, pageSlotExpr, title, plugin, cfg, InventoryType.CHEST);
  }

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
    JavaPlugin plugin,
    IConfig cfg,
    InventoryType type
  ) {
    this.rows = rows;
    this.title = title;
    this.plugin = plugin;
    this.cfg = cfg;
    this.type = type;

    this.pageSlots = slotExprToSlots(pageSlotExpr, rows);
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
   * @param animation Animation to display when opening the GUI
   * @param animateFrom Inventory to animate based off of (transitions)
   */
  public void show(
    Player viewer,
    T arg,
    @Nullable AnimationType animation,
    @Nullable Inventory animateFrom
  ) {
    if (!activeInstances.containsKey(viewer))
      activeInstances.put(viewer, new HashSet<>());

    // Create and register a new GUI instance
    GuiInstance<T> inst = new GuiInstance<>(viewer, this, arg, cfg, plugin);

    // Call the opening event before actually opening
    if (!opening(inst))
      return;

    activeInstances.get(viewer).add(inst);

    // Initially draw the whole gui and fetch pages
    inst.refreshPageContents();
    inst.redraw("*");
    inst.open(animation, animateFrom);

    opened(inst);
  }

  /**
   * Show a personalized instance of this GUI to a player
   * @param viewer Inventory viewer
   * @param arg Argument to be passed to the instance
   * @param animation Animation to display when opening the GUI
   */
  public void show(Player viewer, T arg, @Nullable AnimationType animation) {
    show(viewer, arg, animation, null);
  }

  @Override
  public void cleanup() {
    if (tickerHandle > 0)
      Bukkit.getScheduler().cancelTask(tickerHandle);

    // Destroy all instances of all players
    for (Set<GuiInstance<T>> instances : activeInstances.values()) {
      for (Iterator<GuiInstance<T>> instI = instances.iterator(); instI.hasNext();) {
        GuiInstance<T> inst = instI.next();
        inst.getViewer().closeInventory();
        closed(inst);
        instI.remove();
      }
    }
  }

  @Override
  public void initialize() {
    tickerHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {

      long time = 0;

      @Override
      public void run() {

        // Tick all instances of all players
        for (Set<GuiInstance<T>> instances : activeInstances.values()) {
          for (GuiInstance<T> instance : instances) {
            // Don't tick animating GUIs
            if (!instance.getAnimating().get())
              instance.tick(time);
          }
        }

        time++;
      }
    }, 0L, 1L);
  }

  //=========================================================================//
  //                                Internals                                //
  //=========================================================================//

  /**
   * Called when a GUI has been closed by a player
   * @param inst Instance of the GUI closed
   * @return Whether to prevent closing the GUI
   */
  abstract protected boolean closed(GuiInstance<T> inst);

  /**
   * Called before a GUI is being shown to a player
   * @param inst Instance of the GUI opening
   * @return Whether to open the GUI, false cancels
   */
  abstract protected boolean opening(GuiInstance<T> inst);

  /**
   * Called after a GUI is being shown to a player
   * @param inst Instance of the GUI opened
   */
  protected void opened(GuiInstance<T> inst) {};

  //=========================================================================//
  //                                Listener                                 //
  //=========================================================================//

  @EventHandler
  public void onManip(InventoryManipulationEvent e) {
    // Check if the origin-inventory was involved in a GUI
    GuiInstance<T> inst = findByInventory(e.getPlayer(), e.getOriginInventory()).orElse(null);
    boolean isOrigin = true;

    // Origin is not involved but origin doesn't equal target, lookup target too
    if (inst == null && !e.getOriginInventory().equals(e.getTargetInventory())) {
      inst = findByInventory(e.getPlayer(), e.getTargetInventory()).orElse(null);
      isOrigin = false;
    }

    // Not a GUI-bound event
    if (inst == null)
      return;

    // Always cancel as soon as possible as a fallback,
    // as permitting is usually the exception
    e.setCancelled(true);

    // Ignore interactions while animating
    if (inst.getAnimating().get()) {
      inst.fastForwardAnimating();
      return;
    }

    // Clicked on a used slot which has a click event bound to it
    GuiItem clicked = inst.getItem(isOrigin ? e.getOriginSlot() : e.getTargetSlot()).orElse(null);
    if (clicked != null && clicked.onClick() != null)
      clicked.onClick().accept(e);
  }

  @EventHandler
  public void onClose(InventoryCloseEvent e) {
    if (!(e.getPlayer() instanceof Player p))
      return;

    // The player has no instance
    GuiInstance<T> inst = findByInventory(p, e.getInventory()).orElse(null);
    if (inst == null)
      return;

    // Prevent closing the inventory
    if (closed(inst)) {
      Bukkit.getScheduler().runTaskLater(plugin, () -> p.openInventory(e.getInventory()), 1);
      return;
    }

    // Destroy the instance
    activeInstances.get(p).remove(inst);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    Player p = e.getPlayer();

    // Destroy all instances
    if (activeInstances.containsKey(p))
      activeInstances.remove(p).forEach(i -> i.getTemplate().closed(i));
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
   * @param rows Number of rows of the GUI
   */
  public List<Integer> slotExprToSlots(String slotExpr, int rows) {
    if (slotExpr.isBlank())
      return new ArrayList<>();

    List<Integer> slots = new ArrayList<>();

    for (String range : slotExpr.split(",")) {
      String[] rangeData = range.split("-");

      if (rangeData[0].equals("*")) {
        IntStream.range(0, rows * 9).forEach(slots::add);
        break;
      }

      int from = Integer.parseInt(rangeData[0]);

      if (rangeData.length == 1) {
        slots.add(from);
        continue;
      }

      int to = Integer.parseInt(rangeData[1]);

      for (int i = from; from > to ? (i >= to) : (i <= to); i += (from > to ? -1 : 1)) {
        if (i >= 0 && i < rows * 9)
          slots.add(i);
      }
    }

    return slots;
  }

  /**
   * Formats a constant to a human readable string
   * @param constant Constant to format
   * @return Formatted string
   */
  protected String formatConstant(String constant) {
    return WordUtils.capitalizeFully(constant.replace("_", " ").replace(".", " "));
  }

  /**
   * Get the standardized state placeholder based on a state (enabled / disabled)
   * @param state State
   * @return State placeholder
   */
  protected String statePlaceholderED(boolean state) {
    return cfg.get(state ? ConfigKey.GUI_GENERICS_PLACEHOLDERS_ENABLED : ConfigKey.GUI_GENERICS_PLACEHOLDERS_DISABLED).asScalar();
  }

  /**
   * Get the standardized state placeholder based on a state (yes / no)
   * @param state State
   * @return State placeholder
   */
  protected String statePlaceholderYN(boolean state) {
    return cfg.get(state ? ConfigKey.GUI_GENERICS_PLACEHOLDERS_YES : ConfigKey.GUI_GENERICS_PLACEHOLDERS_NO).asScalar();
  }
}
