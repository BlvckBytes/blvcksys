package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

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

  // TODO: Properly handle multiple open instances of the same enderchest (/ec foreign)

  // Caching enderchests per player, as they will be used quite frequently
  // Offline player requests (others) are not cached
  private final Map<OfflinePlayer, EnderchestModel> cache;
  private final IPersistence pers;

  public EnderchestGui(
    @AutoInject IConfig cfg,
    @AutoInject JavaPlugin plugin,
    @AutoInject IPlayerTextureHandler textures,
    @AutoInject IPersistence pers
  ) {
    super(6, "0-44", i -> (
      cfg.get(ConfigKey.GUI_ENDERCHEST_TITLE)
        .withVariable("viewer", i.getArg().getName())
    ), plugin, cfg, textures);

    this.pers = pers;
    this.cache = new HashMap<>();
  }

  @Override
  protected void prepare() {
    fixedItem("45,47,48,50,51,53", i -> new ItemStackBuilder(Material.BLACK_STAINED_GLASS_PANE).build(), null);
    addPagination("46", "49", "52");
  }

  @Override
  protected void closed(GuiInstance<OfflinePlayer> inst) {
    EnderchestModel chest = cache.get(inst.getArg());

    // Whoops, the enderchest got lost somehow, my bad
    if (chest == null)
      return;

    // Ensure there are no animations playing while saving
    inst.fastForwardAnimating();

    // Sync the current page before storing (as is done when paging)
    syncCurrentPage(inst, chest);
    pers.store(chest);

    // Don't keep offline players in cache
    if (!inst.getArg().isOnline())
      cache.remove(inst.getArg());
  }

  @Override
  protected void opening(Player viewer, GuiInstance<OfflinePlayer> inst) {
    EnderchestModel chest = getOrCreate(inst.getArg());

    // Sync up the page that's being paged away from on paging
    inst.setBeforePaging(() -> syncCurrentPage(inst, chest));

    // Get the maximum number of slots this player may use
    int maxSlots;

    // Opened their own enderchest, or the enderchest of another online player
    if (inst.getArg() instanceof Player target) {
      maxSlots = PlayerPermission.COMMAND_ENDERCHEST_MAX_SLOTS.getSuffixNumber(target, true)
        .orElse(EnderchestModel.DEFAULT_MAX_SLOTS);
      chest.setLastMaxSlots(maxSlots);
    }

    // Opened a foreign enderchest of an offline player, use the last number of max slots as the current max slots
    else
      maxSlots = chest.getLastMaxSlots();

    // Add as many items as the enderchest has slots in total
    for (int j = 0; j < EnderchestModel.NUM_PAGES * EnderchestModel.PAGE_ROWS * 9; j++)
      inst.addPagedItem(
        // Dynamic supplier that's reading items directly from the enderchest
        (i, s) -> {
          ItemStack item = getPageItem(chest, i.getCurrentPage(), s);
          int absoluteSlot = (i.getCurrentPage() - 1) * i.getPageSize() + s;

          // Show locks on locked slots but don't shadow any existing items
          if (absoluteSlot >= maxSlots && item == null)
            return buildLock(i.getCurrentPage(), s);

          return item;
        },
        e -> {
          int slot = e.getManipulation().getOriginInventory().equals(e.getGui().getInv()) ? e.getManipulation().getOriginSlot() : e.getManipulation().getTargetSlot();
          int absoluteSlot = (e.getGui().getCurrentPage() - 1) * inst.getPageSize() + slot;
          boolean allowedToUse = absoluteSlot < maxSlots;

          // Tried to remove something from a locked slot that had an item left
          // Permit this action, so that items can be gained back, but slots cannot be
          // used since no items can be put back in
          if (
            !allowedToUse &&
            (
              e.getManipulation().getAction() == ManipulationAction.PICKUP ||
              e.getManipulation().getAction() == ManipulationAction.DROP ||
              // Moved into the player's inventory
              (e.getManipulation().getAction() == ManipulationAction.MOVE && e.getManipulation().getTargetInventory().equals(e.getManipulation().getPlayer().getInventory()))
            ) && !isLock(e.getGui().getInv().getItem(slot))
          ) {
            allowedToUse = true;

            // Check if this slot is empty on the next tick, if so, put a lock there
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
              if (e.getGui().getInv().getItem(slot) == null)
                e.getGui().getInv().setItem(slot, buildLock(e.getGui().getCurrentPage(), slot));
            }, 1);
          }

          if (!allowedToUse) {
            viewer.sendMessage(
              cfg.get(ConfigKey.ENDERCHEST_LOCKED)
                .withPrefix()
                .asScalar()
            );
          }

          e.setPermitUse(allowedToUse);
        },
        null
      );
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
  private EnderchestModel getOrCreate(OfflinePlayer t) {
    // Check the cache first for online players
    if (cache.containsKey(t))
      return cache.get(t);

    Optional<EnderchestModel> res = pers.findFirst(new QueryBuilder<>(
      EnderchestModel.class,
      "owner__uuid", EqualityOperation.EQ, t.getUniqueId()
    ));

    // Found a result in DB
    if (res.isPresent()) {
      cache.put(t, res.get());
      return res.get();
    }

    // Create a new empty enderchest
    EnderchestModel model = EnderchestModel.createEmpty(t);
    pers.store(model);
    return model;
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
   * @param chest Enderchest to sync into
   */
  private void syncCurrentPage(GuiInstance<OfflinePlayer> inst, EnderchestModel chest) {
    syncInventories(inst.getInv(), getEnderchestPage(chest, inst.getCurrentPage()));
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