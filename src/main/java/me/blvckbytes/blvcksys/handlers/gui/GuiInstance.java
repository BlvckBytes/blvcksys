package me.blvckbytes.blvcksys.handlers.gui;

import lombok.Getter;
import lombok.Setter;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.events.InventoryManipulationEvent;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.util.SymbolicHead;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  A personalized, live instance of a GUI template.
*/
public class GuiInstance<T> {

  @Getter
  private final Player viewer;

  @Getter
  private Inventory inv;

  @Getter
  private final T arg;

  // Items which are on fixed slots
  private final Map<Integer, GuiItem> fixedItems;

  // A list of pages, where each page maps a used page slot to an item
  private final List<Map<Integer, GuiItem>> pages;

  // Mapping slots to their redrawing listeners
  private final Map<Integer, List<Runnable>> redrawListeners;

  private int currPage;
  private GuiAnimation currAnimation;
  private ItemStack fill, border;

  @Setter
  private final List<Runnable> beforePaging;

  @Setter
  private Consumer<Long> tickReceiver;

  private final JavaPlugin plugin;

  @Getter
  private final AGui<T> template;
  private final IPlayerTextureHandler textures;
  private final IConfig cfg;

  @Getter
  private final AtomicBoolean animating;

  /**
   * Create a new GUI instance from a template instance
   * @param viewer Viewer of this instance
   * @param template Template instance
   * @param arg Argument of this instance
   * @param textures Texture handler ref
   * @param cfg Config ref
   * @param plugin JavaPlugin ref
   */
  public GuiInstance(Player viewer, AGui<T> template, T arg, IPlayerTextureHandler textures, IConfig cfg, JavaPlugin plugin) {
    this.viewer = viewer;
    this.template = template;
    this.arg = arg;
    this.plugin = plugin;
    this.cfg = cfg;
    this.textures = textures;

    this.fixedItems = new HashMap<>();
    this.redrawListeners = new HashMap<>();
    this.pages = new ArrayList<>();
    this.beforePaging = new ArrayList<>();
    this.animating = new AtomicBoolean(false);

    // In order to evaluate the title supplier, this call needs to follow
    // after the instance's property assignments
    if (template.getType() == InventoryType.CHEST)
      this.inv = Bukkit.createInventory(null, template.getRows() * 9, template.getTitle().apply(this).asScalar());
    else
      this.inv = Bukkit.createInventory(null, template.getType(), template.getTitle().apply(this).asScalar());
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  //////////////////////////////// Switching //////////////////////////////////

  /**
   * Switches to another GUI
   * @param transition Transition to play while switching GUIs
   * @param gui GUI to switch to
   * @param arg Argument for the gui
   */
  public<A> void switchTo(
    @Nullable AnimationType transition,
    @Nullable AGui<A> gui,
    A arg
  ) {
    if (gui != null)
      gui.show(viewer, arg, transition, inv);
    else
      viewer.closeInventory();
  }

  /**
   * Reopens another instance from the template with the
   * exact same viewer and argument and animates using a previous inventory
   * @param animation Animation to play when opening
   * @param previous Previous inventory
   */
  public<A> void reopen(AnimationType animation, @Nullable GuiInstance<A> previous) {
    if (!template.getActiveInstances().containsKey(viewer))
      template.getActiveInstances().put(viewer, new ArrayList<>());

    // Re-register this instance which was unregistered when closed
    template.getActiveInstances().get(viewer).add(this);

    Bukkit.getScheduler().runTask(plugin, () -> {
      redraw("*");
      open(animation, previous == null ? null : previous.getInv());
    });
  }

  /**
   * Reopens another instance from the template with the
   * exact same viewer and argument
   * @param animation Animation to play when opening
   */
  public void reopen(AnimationType animation) {
    reopen(animation, null);
  }

  //////////////////////////////// Inventory //////////////////////////////////

  /**
   * Resizes the inventory to a given number of rows and copies over as much of
   * the previous content as will fit
   * @param rows Number of new rows
   * @param update Whether to directly open the new inventory to the viewer
   */
  public void resize(int rows, boolean update) {
    Inventory newInv = Bukkit.createInventory(null, rows * 9, template.getTitle().apply(this).asScalar());

    // Copy over contents
    for (int i = 0; i < Math.min(this.inv.getSize(), newInv.getSize()); i++)
      newInv.setItem(i, this.inv.getItem(i));

    this.inv = newInv;

    if (update)
      this.viewer.openInventory(this.inv);
  }

  /**
   * Opens the inventory for it's viewer
   * @param animation Animation to display when opening the GUI
   * @param animateFrom Inventory to animate based off of (transitions)
   */
  public void open(@Nullable AnimationType animation, @Nullable Inventory animateFrom) {
    // Play the given animation
    if (
      // Not a chest, not supported
      (animateFrom != null && animateFrom.getType() != InventoryType.CHEST) ||
      !playAnimation(animation, animateFrom == null ? null : animateFrom.getContents(), null, () -> viewer.openInventory(inv))
    )
      viewer.openInventory(inv);
  }

  /**
   * Closes this inventory on the next tick
   */
  public void close() {
    if (viewer.getOpenInventory().getTopInventory().equals(inv))
      Bukkit.getScheduler().runTask(plugin, viewer::closeInventory);
  }

  /**
   * Fast forwards the currently active animation, if there is any
   */
  public void fastForwardAnimating() {
    if (currAnimation != null)
      currAnimation.fastForward();
  }

  /**
   * Add another item to the list of paged items
   * @param item Item to add
   * @param onClick Click event for the item
   * @param updatePeriod Update period in ticks, null means never
   */
  public void addPagedItem(
    Function<Integer, ItemStack> item,
    @Nullable Consumer<InventoryManipulationEvent> onClick,
    @Nullable Integer updatePeriod
  ) {
    // Create a new page either initially or if the last page is already fully used
    if (pages.isEmpty() || pages.get(pages.size() - 1).size() >= template.getPageSlots().size())
      pages.add(new HashMap<>());

    // Add the new item to the last page and determine it's slot
    Map<Integer, GuiItem> targetPage = pages.get(pages.size() - 1);
    int slot = template.getPageSlots().get(targetPage.size());
    targetPage.put(slot, new GuiItem(item, onClick, updatePeriod));
  }

  /**
   * Add a fixed item, which is an item that will always have the same position,
   * no matter of the viewer's state
   * @param slot Slot to set this item to
   * @param item An item supplier
   * @param onClick Action to run when this item has been clicked
   */
  protected void fixedItem(
    int slot,
    Supplier<ItemStack> item,
    @Nullable Consumer<InventoryManipulationEvent> onClick
  ) {
    fixedItem(slot, item, onClick, null);
  }

  /**
   * Add a fixed item, which is an item that will always have the same position,
   * no matter of the viewer's state
   * @param slot Slot to set this item to
   * @param item An item supplier
   * @param onClick Action to run when this item has been clicked
   * @param updatePeriod Item update period in ticks, null means never
   */
  protected void fixedItem(
    int slot,
    Supplier<ItemStack> item,
    @Nullable Consumer<InventoryManipulationEvent> onClick,
    Integer updatePeriod
  ) {
    fixedItem(String.valueOf(slot), item, onClick, updatePeriod);
  }

  /**
   * Add a fixed item, which is an item that will always have the same position,
   * no matter of the viewer's state
   * @param slotExpr Slot(s) to set this item to
   * @param item An item supplier
   * @param onClick Action to run when this item has been clicked
   * @param updatePeriod Item update period in ticks, null means never
   */
  protected void fixedItem(
    String slotExpr,
    Supplier<ItemStack> item,
    @Nullable Consumer<InventoryManipulationEvent> onClick,
    Integer updatePeriod
  ) {
    for (int slotNumber : template.slotExprToSlots(slotExpr))
      fixedItems.put(slotNumber, new GuiItem(s -> item.get(), onClick, updatePeriod));
  }

  /**
   * Add a fixed item, which is an item that will always have the same position,
   * no matter of the viewer's state
   * @param slotExpr Slot(s) to set this item to
   * @param item An item supplier
   * @param onClick Action to run when this item has been clicked
   */
  protected void fixedItem(
    String slotExpr,
    Supplier<ItemStack> item,
    @Nullable Consumer<InventoryManipulationEvent> onClick
  ) {
    fixedItem(slotExpr, item, onClick, null);
  }

  /**
   * Adds a previous, an indicator as well as a next item as fixed and
   * standardized items to the GUI
   * @param prevSlot Slot of the previous button
   * @param indicatorSlot Slot of the page indicator
   * @param nextSlot Slot of the next button
   */
  protected void addPagination(int prevSlot, int indicatorSlot, int nextSlot) {
    beforePaging.add(() -> Bukkit.getScheduler().runTaskLater(plugin, () -> redraw(String.valueOf(indicatorSlot)), 10));

    fixedItem(prevSlot, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.ARROW_LEFT.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_GENERICS_PAGING_PREV_NAME))
        .withLore(cfg.get(ConfigKey.GUI_GENERICS_PAGING_PREV_LORE))
        .build()
    ), e -> {
      previousPage(AnimationType.SLIDE_RIGHT);
      redraw(String.valueOf(indicatorSlot));
    }, null);

    fixedItem(indicatorSlot, () -> (
      new ItemStackBuilder(Material.PAPER)
        .withName(
          cfg.get(ConfigKey.GUI_GENERICS_PAGING_INDICATOR_NAME)
            .withVariable("curr_page", getCurrentPage())
            .withVariable("num_pages", getNumPages())
        )
        .withLore(
          cfg.get(ConfigKey.GUI_GENERICS_PAGING_INDICATOR_LORE)
            .withVariable("page_num_items", getCurrPageNumItems())
            .withVariable("total_num_items", getTotalNumItems())
            .withVariable("page_size", getPageSize())
        )
        .build()
    ), null, null);

    fixedItem(nextSlot, () -> (
      new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.ARROW_RIGHT.getOwner()))
        .withName(cfg.get(ConfigKey.GUI_GENERICS_PAGING_NEXT_NAME))
        .withLore(cfg.get(ConfigKey.GUI_GENERICS_PAGING_NEXT_LORE))
        .build()
    ), e -> {
      nextPage(AnimationType.SLIDE_LEFT);
      redraw(String.valueOf(indicatorSlot));
    }, null);
  }

  /**
   * Add a standardized state toggle button to the GUI
   * @param slot Where to set the item
   * @param update What slots to update separately when the state changed
   * @param state State supplier
   * @param onClick Click event, providing the current state and the player
   */
  protected void addStateToggle(int slot, @Nullable Integer update, Supplier<Boolean> state, Consumer<Boolean> onClick) {
    fixedItem(slot, () -> {
      boolean s = state.get();

      return new ItemStackBuilder(s ? Material.GREEN_DYE : Material.RED_DYE)
        .withName(cfg.get(s ? ConfigKey.GUI_GENERICS_BUTTONS_DISABLE_NAME : ConfigKey.GUI_GENERICS_BUTTONS_ENABLE_NAME))
        .withLore(cfg.get(s ? ConfigKey.GUI_GENERICS_BUTTONS_DISABLE_LORE : ConfigKey.GUI_GENERICS_BUTTONS_ENABLE_LORE))
        .build();
    }, e -> {
      onClick.accept(state.get());
      redraw(slot + "," + (update == null ? "" : update));
    }, null);
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

    for (int i = 0; i < template.getRows() * 9; i++) {
      if (template.getPageSlots().contains(i))
        continue;

      slotExpr.append(i == 0 ? "" : ",").append(i);
    }

    fill = new ItemStackBuilder(mat)
      .build();

    fixedItem(slotExpr.toString(), () -> fill, null, null);
  }

  /**
   * Adds a border of fixed items consiting of the provided material to the GUI
   * @param mat Material to use as a border
   */
  protected void addBorder(Material mat) {
    StringBuilder slotExpr = new StringBuilder();

    for (int i = 0; i < template.getRows(); i++) {
      int firstSlot = 9 * i, lastSlot = firstSlot + 8;

      slotExpr.append(i == 0 ? "" : ",").append(firstSlot);

      // First or last, use full range
      if (i == 0 || i == template.getRows() - 1)
        slotExpr.append('-');

        // Inbetween, only use first and last
      else
        slotExpr.append(',');

      slotExpr.append(lastSlot);
    }

    border = new ItemStackBuilder(mat)
      .build();

    fixedItem(slotExpr.toString(), () -> border, null, null);
  }

  /**
   * Builds the standardized back button
   */
  private ItemStack buildBackButton() {
    return new ItemStackBuilder(textures.getProfileOrDefault(SymbolicHead.ARROW_LEFT.getOwner()))
      .withName(cfg.get(ConfigKey.GUI_GENERICS_NAV_BACK_NAME))
      .withLore(cfg.get(ConfigKey.GUI_GENERICS_NAV_BACK_LORE))
      .build();
  }

  /**
   * Adds a back button as a fixed item to the GUI
   * @param slot Slot of the back button
   * @param clicked Event callback
   */
  protected<A> void addBack(int slot, Consumer<InventoryManipulationEvent> clicked) {
    fixedItem(slot, this::buildBackButton, clicked);
  }

  /**
   * Adds a back button as a fixed item to the GUI
   * @param slot Slot of the back button
   * @param gui Gui to open on click
   * @param param Gui parameter
   * @param animation Animation to use when navigating back
   */
  protected<A> void addBack(int slot, AGui<A> gui, Supplier<A> param, @Nullable AnimationType animation) {
    fixedItem(slot, this::buildBackButton, e -> switchTo(animation, gui, param == null ? null : param.get()), null);
  }

  /**
   * Redraw only the dynamic page items
   */
  public void redrawPaging() {
    updatePage(null);
  }

  /**
   * Redraw a specified set of slots for a given player
   * @param slotExpr Slots to redraw
   */
  public void redraw(String slotExpr) {
    // Iterate all slots which should be redrawn
    for (int slot : template.slotExprToSlots(slotExpr)) {

      // Vacant slot, skip
      GuiItem target = getItem(slot).orElse(null);
      if (target == null)
        continue;

      // Update the item by re-calling it's supplier
      setItem(slot, target.item().apply(slot));
    }
  }

  /**
   * Register a new listener for a specific slot's redraw events
   * @param slot Target slot
   * @param callback Event listener
   */
  public void onRedrawing(int slot, Runnable callback) {
    if (!this.redrawListeners.containsKey(slot))
      this.redrawListeners.put(slot, new ArrayList<>());
    this.redrawListeners.get(slot).add(callback);
  }

  /**
   * Get an item by it's current slot
   * @param slot Slot to search in
   * @return Optional item, empty if that slot is vacant
   */
  public Optional<GuiItem> getItem(int slot) {
    // Check for fixed items
    GuiItem fixed = fixedItems.get(slot);
    if (fixed != null)
      return Optional.of(fixed);

    // Got no page items
    if (pages.size() == 0)
      return Optional.empty();

    // Check for page items
    GuiItem pageItem = pages.get(currPage).get(slot);
    if (pageItem != null)
      return Optional.of(pageItem);

    // This slot has to be vacant
    return Optional.empty();
  }

  //////////////////////////////// Paging //////////////////////////////////

  /**
   * Checks whether there is a next page to navigate to
   */
  public boolean hasNextPage() {
    return pages.size() > currPage + 1;
  }

  /**
   * Navigate to the last page
   */
  public void lastPage(@Nullable AnimationType animation) {
    if (beforePaging != null)
      beforePaging.forEach(Runnable::run);

    // Already at last page
    if (!hasNextPage())
      return;

    ItemStack[] before = inv.getContents().clone();

    // Advance to the last page and force an update
    currPage = pages.size() - 1;
    updatePage(null);
    playAnimation(animation, before, template.getPageSlots(), null);
  }

  /**
   * Navigate to the next page
   * @return True on success, false if there was no next page
   */
  public boolean nextPage(@Nullable AnimationType animation) {
    if (beforePaging != null)
      beforePaging.forEach(Runnable::run);

    if (!hasNextPage())
      return false;

    ItemStack[] before = inv.getContents().clone();

    // Advance to the next page and force an update
    currPage++;
    updatePage(null);
    playAnimation(animation, before, template.getPageSlots(), null);
    return true;
  }

  /**
   * Checks whether there is a previous page to navigate to
   */
  public boolean hasPreviousPage() {
    return currPage > 0;
  }

  /**
   * Navigate to the previous page
   * @return True on success, false if there was no previous page
   */
  public boolean previousPage(@Nullable AnimationType animation) {
    if (beforePaging != null)
      beforePaging.forEach(Runnable::run);

    if (!hasPreviousPage())
      return false;

    ItemStack[] before = inv.getContents().clone();

    // Advance to the previous page and force an update
    currPage--;
    updatePage(null);
    playAnimation(animation, before, template.getPageSlots(), null);
    return true;
  }

  /**
   * Get the number of available pages
   */
  public int getNumPages() {
    return Math.max(1, pages.size());
  }

  /**
   * Get the current page
   */
  public int getCurrentPage() {
    return currPage + 1;
  }

  /**
   * Get the size of a page
   */
  public int getPageSize() {
    return template.getPageSlots().size();
  }

  /**
   * Get the added number of items of all pages
   */
  public int getTotalNumItems() {
    return pages.stream().reduce(0, (acc, curr) -> acc + curr.size(), Integer::sum);
  }

  /**
   * Get the number of items on the current page
   */
  public int getCurrPageNumItems() {
    if (pages.size() == 0)
      return 0;
    return pages.get(currPage).size();
  }

  /**
   * Update the current page's items within the GUI inventory
   * @param time Current relative time, null to force an update upon all items
   */
  public void updatePage(@Nullable Long time) {
    // There are no pages yet, clear all page slots
    if (pages.size() == 0) {
      for (int pageSlot : template.getPageSlots())
        setItem(pageSlot, null);
      return;
    }

    // Start out with all available page slots and remove used slots one at a time
    List<Integer> pageSlots = new ArrayList<>(template.getPageSlots());

    // Loop all items of the current page
    for (Map.Entry<Integer, GuiItem> pageItem : pages.get(currPage).entrySet()) {
      GuiItem item = pageItem.getValue();
      pageSlots.remove(pageItem.getKey());

      // Only update on force updates or if the time is a multiple of the item's period
      if (time == null || (item.updatePeriod() != null && item.updatePeriod() > 0 && time % item.updatePeriod() == 0))
        setItem(pageItem.getKey(), item.item().apply(pageItem.getKey()));
    }

    // Clear unused page slots if they're not already vacant
    for (Integer vacantPageSlot : pageSlots) {
      if (inv.getItem(vacantPageSlot) != null)
        setItem(vacantPageSlot, null);
    }
  }

  //////////////////////////////// Updating //////////////////////////////////

  /**
   * Called whenever the template ticks all of it's instances
   * @param time Relative time in ticks
   */
  public void tick(long time) {
    if (tickReceiver != null)
      tickReceiver.accept(time);

    // Tick all fixed items
    for (Map.Entry<Integer, GuiItem> itemE : fixedItems.entrySet()) {
      GuiItem item = itemE.getValue();

      // Only tick this item if it has a period which has elapsed
      if (item.updatePeriod() != null && time % item.updatePeriod() == 0)
        setItem(itemE.getKey(), item.item().apply(itemE.getKey()));
    }

    // Tick all page items
    updatePage(time);
  }

  /**
   * Play a given animation on the GUI and manage entering
   * and leaving the animation lock state
   * @param animation Animation to play
   * @param from Items to animate from
   * @param mask List of slots to animate, leave at null to animate all slots
   * @param ready Ready callback, signals that the GUI may be presented by now
   * @return Whether the animation has been initialized
   */
  private boolean playAnimation(
    @Nullable AnimationType animation,
    @Nullable ItemStack[] from,
    @Nullable List<Integer> mask,
    @Nullable Runnable ready
  ) {
    if (animation == null)
      return false;

    // Fastforward the currently playing animation, if any
    if (currAnimation != null)
      currAnimation.fastForward();

    // Enter animating lock and start playing
    animating.set(true);
    currAnimation = new GuiAnimation(
      plugin, animation,
      from, inv.getContents(),
      inv, mask,
      // Use either the border (higher precedence) or the fill
      // item stack to fill animation gaps
      border == null ? fill : border,
      ready == null ? () -> {} : ready,
      () -> {
        // Leave animating lock
        currAnimation = null;
        animating.set(false);
      }
    );

    return true;
  }

  /**
   * Sets an item to a specified slot in the current inventory and
   * protects against index out of range calls
   * @param slot Slot to set at
   * @param item Item to set
   */
  private void setItem(int slot, ItemStack item) {
    if (slot < inv.getSize()) {
      inv.setItem(slot, item);
      redrawListeners.getOrDefault(slot, new ArrayList<>()).forEach(Runnable::run);
    }
  }
}
