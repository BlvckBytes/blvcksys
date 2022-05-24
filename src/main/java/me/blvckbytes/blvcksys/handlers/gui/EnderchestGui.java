package me.blvckbytes.blvcksys.handlers.gui;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerTextureHandler;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.EnderchestModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
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

  View and manage your enderchest.
*/
@AutoConstruct
public class EnderchestGui extends AGui<OfflinePlayer> {

  // FIXME: Enderchests are pretty important! Make all operations failsafe and prevent an item loss as much as possible

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

    // TODO: Decide on how to handle maxSlots when viewing foreign enderchests
    // Get the maximum number of slots this player may use
    int maxSlots = PlayerPermission.COMMAND_ENDERCHEST_MAX_SLOTS.getSuffixNumber(viewer, true).orElse(1);

    // Add as many items as the enderchest has slots in total
    for (int j = 0; j < EnderchestModel.NUM_PAGES * EnderchestModel.PAGE_ROWS * 9; j++)
      inst.addPagedItem(
        // Dynamic supplier that's reading items directly from the enderchest
        (i, s) -> {
          ItemStack item = getPageItem(chest, i.getCurrentPage(), s);
          int absoluteSlot = (i.getCurrentPage() - 1) * 9 + s;

          // Show locks on locked slots but don't shadow any existing items
          if (absoluteSlot >= maxSlots && item == null) {
            return new ItemStackBuilder(Material.BARRIER)
              .withName(cfg.get(ConfigKey.GUI_ENDERCHEST_LOCK_NAME))
              .withLore(
                cfg.get(ConfigKey.GUI_ENDERCHEST_LOCK_LORE)
                  .withVariable("slot", s + 1)
                  .withVariable("page", i.getCurrentPage())
              )
              .build();
          }

          return item;
        },
        e -> {
          int absoluteSlot = (e.getGui().getCurrentPage() - 1) * 9 + e.getSlot();
          boolean allowedToUse = absoluteSlot < maxSlots;

          if (!allowedToUse) {
            viewer.sendMessage(
              cfg.get(ConfigKey.ENDERCHEST_LOCKED)
                .withPrefix()
                .asScalar()
            );
          }

          // TODO: Would be great if there was more granular click information available, to allow
          // TODO: the player to get items out of locked slots but don't swap them out or put any in
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
}
