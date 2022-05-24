package me.blvckbytes.blvcksys.handlers.gui;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/21/2022

  A personalized, live instance of a GUI template.
*/
public class GuiInstance<T> {

  @Getter
  private final Player viewer;

  @Getter
  private final Inventory inv;

  @Getter
  private final T arg;

  // A list of pages, where each page maps a used page slot to an item
  private final List<Map<Integer, GuiItem<T>>> pages;
  private int currPage;
  private GuiAnimation currAnimation;

  @Setter
  private Runnable beforePaging;

  private final JavaPlugin plugin;

  @Getter
  private final AGui<T> template;

  @Getter
  private final AtomicBoolean animating;

  /**
   * Create a new GUI instance from a template instance
   * @param viewer Viewer of this instance
   * @param template Template instance
   * @param arg Argument of this instance
   * @param plugin JavaPlugin ref
   */
  public GuiInstance(Player viewer, AGui<T> template, T arg, JavaPlugin plugin) {
    this.viewer = viewer;
    this.template = template;
    this.arg = arg;
    this.plugin = plugin;

    this.pages = new ArrayList<>();
    this.animating = new AtomicBoolean(false);

    // In order to evaluate the title supplier, this call needs to follow
    // after the instance's property assignments
    this.inv = Bukkit.createInventory(null, template.getRows() * 9, template.getTitle().apply(this).asScalar());
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  //////////////////////////////// Switching //////////////////////////////////

  /**
   * Switches to another GUI or just closes the screen if the GUI is null
   * @param current Currently open instance
   * @param transition Transition to play while switching GUIs
   * @param gui GUI to switch to
   * @param arg Argument for the gui
   */
  public<A> void switchTo(
    GuiInstance<?> current,
    @Nullable AnimationType transition,
    @Nullable AGui<A> gui,
    A arg
  ) {
    if (gui != null)
      gui.show(viewer, arg, transition, current.getInv());
    else
      viewer.closeInventory();
  }

  //////////////////////////////// Inventory //////////////////////////////////

  /**
   * Opens the inventory for it's viewer
   * @param animation Animation to display when opening the GUI
   * @param animateFrom Inventory to animate based off of (transitions)
   */
  public void open(@Nullable AnimationType animation, @Nullable Inventory animateFrom) {
    // Play the given animation
    if (animation != null) {
      animating.set(true);

      if (currAnimation != null)
        currAnimation.fastForward();

      currAnimation = new GuiAnimation(
        plugin, animation,
        animateFrom == null ? inv : animateFrom,
        animateFrom == null ? null : inv,
        () -> viewer.openInventory(inv),
        () -> {
          currAnimation = null;
          animating.set(false);
        }
      );
    }

    // Show instantly
    else
      viewer.openInventory(inv);
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
    BiFunction<GuiInstance<T>, Integer, ItemStack> item,
    @Nullable Consumer<GuiClickEvent<T>> onClick,
    @Nullable Integer updatePeriod
  ) {
    // Create a new page either initially or if the last page is already fully used
    if (pages.isEmpty() || pages.get(pages.size() - 1).size() >= template.getPageSlots().size())
      pages.add(new HashMap<>());

    // Add the new item to the last page and determine it's slot
    Map<Integer, GuiItem<T>> targetPage = pages.get(pages.size() - 1);
    int slot = template.getPageSlots().get(targetPage.size());
    targetPage.put(slot, new GuiItem<>(item, onClick, updatePeriod));
  }

  /**
   * Redraw a specified set of slots for a given player
   * @param slotExpr Slots to redraw
   */
  public void redraw(String slotExpr) {
    // Iterate all slots which should be redrawn
    for (int slot : AGui.slotExprToSlots(slotExpr, template.getRows())) {

      // Vacant slot, skip
      GuiItem<T> target = getItem(slot).orElse(null);
      if (target == null)
        continue;

      // Update the item by re-calling it's supplier
      inv.setItem(slot, target.item().apply(this, slot));
    }
  }

  /**
   * Get an item by it's current slot
   * @param slot Slot to search in
   * @return Optional item, empty if that slot is vacant
   */
  public Optional<GuiItem<T>> getItem(int slot) {
    // Check for fixed items
    GuiItem<T> fixed = template.getFixedItems().get(slot);
    if (fixed != null)
      return Optional.of(fixed);

    // Got no page items
    if (pages.size() == 0)
      return Optional.empty();

    // Check for page items
    GuiItem<T> pageItem = pages.get(currPage).get(slot);
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
   * Navigate to the next page
   * @return True on success, false if there was no next page
   */
  public boolean nextPage() {
    if (!hasNextPage())
      return false;

    if (beforePaging != null)
      beforePaging.run();

    // Advance to the next page and force an update
    currPage++;
    updatePage(null);
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
  public boolean previousPage() {
    if (!hasPreviousPage())
      return false;

    if (beforePaging != null)
      beforePaging.run();

    // Advance to the previous page and force an update
    currPage--;
    updatePage(null);
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
        inv.setItem(pageSlot, null);
      return;
    }

    // Start out with all available page slots and remove used slots one at a time
    List<Integer> pageSlots = new ArrayList<>(template.getPageSlots());

    // Loop all items of the current page
    for (Map.Entry<Integer, GuiItem<T>> pageItem : pages.get(currPage).entrySet()) {
      GuiItem<T> item = pageItem.getValue();
      pageSlots.remove(pageItem.getKey());

      // Only update on force updates or if the time is a multiple of the item's period
      if (time == null || (item.updatePeriod() != null && item.updatePeriod() > 0 && time % item.updatePeriod() == 0))
        inv.setItem(pageItem.getKey(), item.item().apply(this, pageItem.getKey()));
    }

    // Clear unused page slots if they're not already vacant
    for (Integer vacantPageSlot : pageSlots) {
      if (inv.getItem(vacantPageSlot) != null)
        inv.setItem(vacantPageSlot, null);
    }
  }

  //////////////////////////////// Updating //////////////////////////////////

  /**
   * Called whenever the template ticks all of it's instances
   * @param time Relative time in ticks
   */
  public void tick(long time) {
    // Tick all fixed items
    for (Map.Entry<Integer, GuiItem<T>> itemE : template.getFixedItems().entrySet()) {
      GuiItem<T> item = itemE.getValue();

      // Only tick this item if it has a period which has elapsed
      if (item.updatePeriod() != null && time % item.updatePeriod() == 0)
        inv.setItem(itemE.getKey(), item.item().apply(this, itemE.getKey()));
    }

    // Tick all page items
    updatePage(time);
  }
}
