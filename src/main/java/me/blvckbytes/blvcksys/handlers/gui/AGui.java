package me.blvckbytes.blvcksys.handlers.gui;

import lombok.Getter;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.events.InventoryManipulationEvent;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.util.SymbolicHead;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import java.util.function.BiConsumer;
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
  protected final IConfig cfg;
  protected final IPlayerTextureHandler textures;

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
    JavaPlugin plugin,
    IConfig cfg,
    IPlayerTextureHandler textures
  ) {
    this.rows = rows;
    this.title = title;
    this.plugin = plugin;
    this.cfg = cfg;
    this.textures = textures;

    this.pageSlots = slotExprToSlots(pageSlotExpr, rows);
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
      activeInstances.put(viewer, new ArrayList<>());

    // Create and register a new GUI instance
    GuiInstance<T> inst = new GuiInstance<>(viewer, this, arg, plugin);
    activeInstances.get(viewer).add(inst);

    // Call the opening event before actually opening
    opening(viewer, inst);

    // Initially draw the whole gui
    inst.redraw("*");
    inst.open(animation, animateFrom);
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
    for (List<GuiInstance<T>> instances : activeInstances.values()) {
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
        for (List<GuiInstance<T>> instances : activeInstances.values()) {
          for (GuiInstance<T> instance : instances) {
            // Don't tick animating GUIs
            if (instance.getAnimating().get())
              continue;

            instance.tick(time);
          }
        }

        time += TICKER_PERIOD_T;
      }
    }, 0L, TICKER_PERIOD_T);

    prepare();
  }

  //=========================================================================//
  //                                Internals                                //
  //=========================================================================//

  /**
   * Called when a GUI has been closed by a player
   * @param inst Instance of the GUI closed
   */
  abstract protected void closed(GuiInstance<T> inst);

  /**
   * Called before a GUI is being shown to a player
   * @param viewer Player that requested a GUI
   * @param inst Instance of the GUI opening
   */
  abstract protected void opening(Player viewer, GuiInstance<T> inst);

  /**
   * Called right after autoconstruction is complete and
   * is ment to prepare all fixed items within the GUI
   */
  abstract protected void prepare();

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
      fixedItems.put(slotNumber, new GuiItem<>((i, s) -> item.apply(i), onClick, null));
  }

  /**
   * Adds a previous, an indicator as well as a next item as fixed and
   * standardized items to the GUI
   * @param prevSlot Slot of the previous button
   * @param indicatorSlot Slot of the page indicator
   * @param nextSlot Slot of the next button
   */
  protected void addPagination(String prevSlot, String indicatorSlot, String nextSlot) {
    fixedItem(prevSlot, g -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.ARROW_LEFT.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_GENERICS_PAGING_PREV_NAME))
        .withLore(cfg.get(ConfigKey.GUI_GENERICS_PAGING_PREV_LORE))
        .build()
    ), e -> {
      e.getGui().previousPage(AnimationType.SLIDE_RIGHT);
      e.getGui().redraw(indicatorSlot);
    });

    fixedItem(indicatorSlot, g -> (
      new ItemStackBuilder(Material.PAPER)
        .withName(
          cfg.get(ConfigKey.GUI_GENERICS_PAGING_INDICATOR_NAME)
            .withVariable("curr_page", g.getCurrentPage())
            .withVariable("num_pages", g.getNumPages())
        )
        .withLore(
          cfg.get(ConfigKey.GUI_GENERICS_PAGING_INDICATOR_LORE)
            .withVariable("num_items", g.getCurrPageNumItems())
            .withVariable("max_items", g.getPageSize())
        )
        .build()
    ), null);

    fixedItem(nextSlot, g -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.ARROW_RIGHT.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_GENERICS_PAGING_NEXT_NAME))
        .withLore(cfg.get(ConfigKey.GUI_GENERICS_PAGING_NEXT_LORE))
        .build()
    ), e -> {
      e.getGui().nextPage(AnimationType.SLIDE_LEFT);
      e.getGui().redraw(indicatorSlot);
    });
  }

  /**
   * Add a standardized state toggle button to the GUI
   * @param slot Where to set the item
   * @param update What slots to update separately when the state changed
   * @param state State supplier
   * @param onClick Click event, providing the current state and the player
   */
  protected void addStateToggle(String slot, @Nullable String update, Function<GuiInstance<T>, Boolean> state, BiConsumer<Boolean, GuiInstance<T>> onClick) {
    fixedItem(slot, i -> {
      boolean s = state.apply(i);

      return new ItemStackBuilder(s ? Material.GREEN_DYE : Material.RED_DYE)
        .withName(cfg.get(s ? ConfigKey.GUI_GENERICS_BUTTONS_DISABLE_NAME : ConfigKey.GUI_GENERICS_BUTTONS_ENABLE_NAME))
        .withLore(cfg.get(s ? ConfigKey.GUI_GENERICS_BUTTONS_DISABLE_LORE : ConfigKey.GUI_GENERICS_BUTTONS_ENABLE_LORE))
        .build();
    }, e -> {
      onClick.accept(state.apply(e.getGui()), e.getGui());
      e.getGui().redraw(slot + "," + (update == null ? "" : update));
    });
  }

  /**
   * Get the standardized state placeholder based on a state
   * @param state State
   * @return State placeholder
   */
  protected String statePlaceholder(boolean state) {
    return cfg.get(state ? ConfigKey.GUI_GENERICS_PLACEHOLDERS_ENABLED : ConfigKey.GUI_GENERICS_PLACEHOLDERS_DISABLED).asScalar();
  }

  /**
   * Adds a fill of fixed items consiting of the provided material to the GUI
   * @param mat Material to use to fill
   */
  protected void addFill(Material mat) {
    StringBuilder slotExpr = new StringBuilder();

    for (int i = 0; i < rows * 9; i++) {
      if (pageSlots.contains(i))
        continue;

      slotExpr.append(i == 0 ? "" : ",").append(i);
    }

    fixedItem(slotExpr.toString(), g -> new ItemStackBuilder(mat).build(), null);
  }

  /**
   * Adds a border of fixed items consiting of the provided material to the GUI
   * @param mat Material to use as a border
   */
  protected void addBorder(Material mat) {
    StringBuilder slotExpr = new StringBuilder();

    for (int i = 0; i < rows; i++) {
      int firstSlot = 9 * i, lastSlot = firstSlot + 8;

      slotExpr.append(i == 0 ? "" : ",").append(firstSlot);

      // First or last, use full range
      if (i == 0 || i == rows - 1)
        slotExpr.append('-');

      // Inbetween, only use first and last
      else
        slotExpr.append(',');

      slotExpr.append(lastSlot);
    }

    fixedItem(slotExpr.toString(), g -> (
      new ItemStackBuilder(mat)
        .build()
    ), null);
  }

  /**
   * Adds a back button as a fixed item to the GUI
   * @param slot Slot of the back button
   * @param gui Gui to open on click
   * @param param Gui parameter
   * @param animation Animation to use when navigating back
   */
  protected<A> void addBack(String slot, AGui<A> gui, A param, @Nullable AnimationType animation) {
    fixedItem(slot, g -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.ARROW_LEFT.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_GENERICS_NAV_BACK_NAME))
        .withLore(cfg.get(ConfigKey.GUI_GENERICS_NAV_BACK_LORE))
        .build()
    ), e -> e.getGui().switchTo(e.getGui(), animation, gui, param));
  }

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
    if (inst.getAnimating().get())
      return;

    // Clicked on a used slot which has a click event bound to it
    GuiItem<T> clicked = inst.getItem(isOrigin ? e.getOriginSlot() : e.getTargetSlot()).orElse(null);
    if (clicked != null && clicked.onClick() != null) {
      GuiClickEvent<T> gce = new GuiClickEvent<>(inst, e);
      clicked.onClick().accept(gce);

      // Undo cancellation if the receiver
      // permitted the use of this slot
      if (gce.isPermitUse())
        e.setCancelled(false);
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
      closed(inst);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Destroy all instances
    if (activeInstances.containsKey(e.getPlayer()))
      activeInstances.remove(e.getPlayer()).forEach(i -> i.getTemplate().closed(i));
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
  public List<Integer> slotExprToSlots(String slotExpr, int rows) {
    if (slotExpr.isBlank())
      return new ArrayList<>();

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
