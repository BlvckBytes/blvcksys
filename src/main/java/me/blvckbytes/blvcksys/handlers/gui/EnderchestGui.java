package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.ConfigValue;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.events.ManipulationAction;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.EnderchestModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/24/2022

  This enderchest is ment to provide a rich set of features to the user, such
  as having multiple big pages, which are animated and allow for the locking
  of individual slots which have a sequence number above a certain threshold.
  If a slot has been locked again, but the user still has an itemstack residing
  in that slot, they may take it out at any time and at any rate, but never put
  anything back in. As soon as the slot reached the vacancy state, a lock is
  rendered back in it's place.

  Pages are directly mapped to the model's inventories, but only synced on either
  closing the gui (storing that page) or paging to another page (storing the page
  which has been paged away from). When the inventory is closed, only then that model
  is stored persistently, as serializing all the items is not cheap.
*/
@AutoConstruct
public class EnderchestGui extends AGui<OfflinePlayer> {

  // Caching enderchests per player, as they will be used quite frequently
  private final Map<OfflinePlayer, EnderchestInstance> cache;
  private final IPersistence pers;
  private final ILogger logger;
  private final IStdGuiItemsProvider stdGuiItemsProvider;

  public EnderchestGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IPersistence pers,
    @AutoInject ILogger logger,
    @AutoInject IStdGuiItemsProvider stdGuiItemsProvider
  ) {
    super(6, "0-44", i -> (
      cfg.get(ConfigKey.GUI_ENDERCHEST_TITLE)
        .withVariable("viewer", i.getArg().getName())
    ), plugin, cfg, textures);

    this.pers = pers;
    this.logger = logger;
    this.stdGuiItemsProvider = stdGuiItemsProvider;

    this.cache = new HashMap<>();
  }

  @Override
  protected boolean closed(GuiInstance<OfflinePlayer> inst) {
    EnderchestInstance chestInst = cache.get(inst.getArg());

    // Whoops, the enderchest got lost somehow, my bad
    if (chestInst == null) {
      logger.logError(new IllegalStateException("No enderchest instance found when closing the GUI"));
      return false;
    }

    chestInst.unregisterAfterChanges(inst.getViewer());
    EnderchestModel chest = chestInst.getModel();

    // Ensure there are no animations playing while saving
    inst.fastForwardAnimating();

    // Store the model persistently if there are any changes
    if (chestInst.hasChanges()) {
      pers.store(chest);
      chestInst.stored();
    }

    // Don't keep offline players in cache if they're not used anymore
    if (!chestInst.isInUse() && !inst.getArg().isOnline())
      cache.remove(inst.getArg());

    return false;
  }

  @Override
  protected boolean opening(GuiInstance<OfflinePlayer> inst) {
    Player p = inst.getViewer();

    inst.fixedItem("45,47,48,50,51,53", () -> (
      new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE)
        .withName(ConfigValue.immediate(" "))
        .build()
    ), null, null);
    
    // Paginator
    inst.addPagination("46", "49", "52", stdGuiItemsProvider);

    EnderchestInstance chestInst = getOrCreate(inst.getArg());
    EnderchestModel model = chestInst.getModel();

    // Redraw paging when changes occurred by other viewers
    chestInst.registerAfterChanges(p, c -> inst.redrawPaging());

    // Get the maximum number of slots this player may use
    int maxSlots;

    // Opened their own enderchest, or the enderchest of another online player
    Player online = inst.getArg().getPlayer();
    if (online != null) {
      maxSlots = PlayerPermission.COMMAND_ENDERCHEST_MAX_SLOTS.getSuffixNumber(online, true)
        .orElse(EnderchestModel.DEFAULT_MAX_SLOTS);
      model.setLastMaxSlots(maxSlots);
    }

    // Opened a foreign enderchest of an offline player, use the last number of max slots as the current max slots
    else
      maxSlots = model.getLastMaxSlots();

    inst.setPageContents(() -> {
      List<GuiItem> items = new ArrayList<>();

      // Add as many items as the enderchest has slots in total
      for (int j = 0; j < EnderchestModel.NUM_PAGES * EnderchestModel.PAGE_ROWS * 9; j++) {
        items.add(
          new GuiItem(
            // Dynamic supplier that's reading items directly from the enderchest
            s -> {
              ItemStack item = getPageItem(model, inst.getCurrentPage(), s);
              int absoluteSlot = (inst.getCurrentPage() - 1) * inst.getPageSize() + s;

              // Show locks on locked slots but don't shadow any existing items
              if (absoluteSlot >= maxSlots && item == null)
                return buildLock(inst.getCurrentPage(), s);

              return item;
            },
            e -> {
              // Immediately sync after a change has been made
              Bukkit.getScheduler().runTaskLater(plugin, () -> syncCurrentPage(inst, chestInst), 1);

              int slot = e.getOriginInventory().equals(inst.getInv()) ? e.getOriginSlot() : e.getTargetSlot();
              int absoluteSlot = (inst.getCurrentPage() - 1) * inst.getPageSize() + slot;
              boolean allowedToUse = absoluteSlot < maxSlots;

              // Tried to remove something from a locked slot that had an item left
              // Permit this action, so that items can be gained back, but slots cannot be
              // used since no items can be put back in
              if (
                !allowedToUse &&
                  (
                    e.getAction() == ManipulationAction.PICKUP ||
                      e.getAction() == ManipulationAction.DROP ||
                      // Moved into the player's inventory
                      (e.getAction() == ManipulationAction.MOVE && e.getTargetInventory().equals(e.getPlayer().getInventory()))
                  ) && !isLock(inst.getInv().getItem(slot))
              ) {
                allowedToUse = true;

                // Check if this slot is empty on the next tick, if so, put a lock there
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                  if (inst.getInv().getItem(slot) == null)
                    inst.getInv().setItem(slot, buildLock(inst.getCurrentPage(), slot));
                }, 1);
              }

              if (!allowedToUse) {
                p.sendMessage(
                  cfg.get(ConfigKey.ENDERCHEST_LOCKED)
                    .withPrefix()
                    .asScalar()
                );
              }

              e.setCancelled(!allowedToUse);
            },
            null
          )
        );
      }

      return items;
    });

    return true;
  }

  /**
   * Get an item by a slot relative to a specified page
   * @param chest Enderchest to get from
   * @param page Page requested (one-based)
   * @param slot Slot on that page (zero-based)
   * @return Content of this slot
   */
  private ItemStack getPageItem(EnderchestModel chest, int page, int slot) {
    return getEnderchestPage(chest, page).getItem(slot);
  }

  /**
   * Get or create a new enderchest for a given player
   * @param t Target player
   * @return Enderchest
   */
  private EnderchestInstance getOrCreate(OfflinePlayer t) {
    // Check the cache first
    if (cache.containsKey(t))
      return cache.get(t);

    Optional<EnderchestModel> res = pers.findFirst(new QueryBuilder<>(
      EnderchestModel.class,
      "owner__uuid", EqualityOperation.EQ, t.getUniqueId()
    ));

    // Found a result in DB
    if (res.isPresent()) {
      EnderchestInstance inst = new EnderchestInstance(res.get());
      cache.put(t, inst);
      return inst;
    }

    // Create a new empty enderchest
    EnderchestModel model = EnderchestModel.createEmpty(t);
    pers.store(model);
    return new EnderchestInstance(model);
  }

  /**
   * Get an enderchest page by it's numeric (one-based) index.
   * @param chest Enderchest to get from
   * @param page Page requested
   * @return Enderchest page, throws when indices are out of range
   */
  private Inventory getEnderchestPage(EnderchestModel chest, int page) {
    return switch (page) {
      case 1 -> chest.getPage1();
      case 2 -> chest.getPage2();
      case 3 -> chest.getPage3();
      default -> throw new RuntimeException("Invalid page specified");
    };
  }

  /**
   * Syncs the current GUI page into the corresponding enderchest page
   * @param inst GUI instance to sync from
   * @param chestInst Enderchest to sync into
   */
  private void syncCurrentPage(GuiInstance<OfflinePlayer> inst, EnderchestInstance chestInst) {
    syncInventories(inst.getInv(), getEnderchestPage(chestInst.getModel(), inst.getCurrentPage()));
    chestInst.changed(inst.getViewer());
  }

  /**
   * Sync one inventory's contents into another inventory
   * @param from Source inventory
   * @param to Destination inventory
   */
  private void syncInventories(Inventory from, Inventory to) {
    for (int i = 0; i < Math.min(to.getSize(), from.getSize()); i++)
      to.setItem(i, isLock(from.getItem(i)) ? null : from.getItem(i));
  }

  /**
   * Checks whether a given item is a locking placeholder
   * @param item Item to check
   */
  private boolean isLock(ItemStack item) {
    if (item == null)
      return false;

    if (item.getType() != Material.BARRIER)
      return false;

    ItemMeta meta = item.getItemMeta();

    if (meta == null)
      return false;

    return cfg.get(ConfigKey.GUI_ENDERCHEST_LOCK_NAME).asScalar().equals(meta.getDisplayName());
  }

  /**
   * Build a lock item for a specific slot on a specific page
   * @param page Page to lock at
   * @param slot Slot to lock
   */
  private ItemStack buildLock(int page, int slot) {
    return new ItemStackBuilder(Material.BARRIER)
      .withName(cfg.get(ConfigKey.GUI_ENDERCHEST_LOCK_NAME))
      .withLore(
        cfg.get(ConfigKey.GUI_ENDERCHEST_LOCK_LORE)
          .withVariable("slot", slot + 1)
          .withVariable("page", page)
      )
      .build();
  }
}
