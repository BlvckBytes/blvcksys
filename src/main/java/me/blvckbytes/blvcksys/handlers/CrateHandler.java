package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.AutoInjectLate;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.handlers.gui.ConfirmationGui;
import me.blvckbytes.blvcksys.handlers.gui.CrateContentGui;
import me.blvckbytes.blvcksys.handlers.gui.CrateDrawGui;
import me.blvckbytes.blvcksys.handlers.gui.CrateDrawLayout;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.*;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/27/2022

  Handles managing a crate and it's associated list of items.
*/
@AutoConstruct
public class CrateHandler implements ICrateHandler, Listener, IAutoConstructed {

  // Time between the invocation of particle effect requests
  private static final long EFFECTS_PERIOD_T = 20 * 3;

  // Velocity of the rising helix animation, blocks per winding, the total height as well as the radius
  private static final double EFFECTS_VELOCITY = .055, EFFECTS_BPW = .9, EFFECTS_HEIGHT = 1.2, EFFECTS_RAD = .7;

  // Mapping crate names to cached crate instances
  private final Map<String, CachedCrate> crateCache;

  // Mapping players to a map of crate names and the corresponding keys
  private final Map<OfflinePlayer, Map<String, CrateKeyModel>> keyCache;

  @AutoInjectLate
  private CrateDrawGui drawGui;

  @AutoInjectLate
  private CrateContentGui contentGui;

  private final IPersistence pers;
  private final JavaPlugin plugin;
  private final IAnimationHandler animationHandler;
  private final ConfirmationGui confirmationGui;
  private final IConfig cfg;

  private BukkitTask effectTaskHandle;

  public CrateHandler(
    @AutoInject IPersistence pers,
    @AutoInject JavaPlugin plugin,
    @AutoInject IAnimationHandler animationHandler,
    @AutoInject ConfirmationGui confirmationGui,
    @AutoInject IConfig cfg
  ) {
    this.pers = pers;
    this.plugin = plugin;
    this.animationHandler = animationHandler;
    this.confirmationGui = confirmationGui;
    this.cfg = cfg;

    this.crateCache = new HashMap<>();
    this.keyCache = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public boolean createCrate(Player creator, String name) {
    try {
      // Create a new crate with default null values
      CrateModel crate = new CrateModel(creator, name, null, null, null);
      pers.store(crate);

      // Cache the crate with no items
      crateCache.put(name.toLowerCase(), new CachedCrate(crate, new ArrayList<>()));
      return true;
    } catch (DuplicatePropertyException e) {
      return false;
    }
  }

  @Override
  public boolean deleteCrate(String name) {
    crateCache.remove(name.toLowerCase());
    return pers.delete(buildCrateQuery(name)) > 0;
  }

  @Override
  public boolean moveCrate(String name, @Nullable Location loc) {
    CrateModel crate = getCrate(name).orElse(null);

    if (crate == null)
      return false;

    crate.setLoc(loc);
    pers.store(crate);
    return true;
  }

  @Override
  public boolean setCrateLayout(String name, @Nullable CrateDrawLayout layout) {
    CrateModel crate = getCrate(name).orElse(null);

    if (crate == null)
      return false;

    crate.setLayout(layout);
    pers.store(crate);
    return true;
  }

  @Override
  public boolean setCrateParticleEffectColor(String name, @Nullable ParticleEffectColor color) {
    CrateModel crate = getCrate(name).orElse(null);

    if (crate == null)
      return false;

    crate.setParticleEffectColor(color);
    pers.store(crate);
    return true;
  }

  @Override
  public Optional<CrateModel> getCrate(String name) {
    return crateCache.values().stream()
      .map(CachedCrate::getCrate)
      .filter(c -> c.getName().equals(name))
      .findFirst();
  }

  @Override
  public Optional<CrateModel> getCrate(UUID id) {
    return crateCache.values().stream()
      .map(CachedCrate::getCrate)
      .filter(c -> c.getId().equals(id))
      .findFirst();
  }

  @Override
  public List<Tuple<CrateModel, List<CrateItemModel>>> listCrates() {
    return crateCache.values().stream().map(CachedCrate::asTuple).toList();
  }

  @Override
  public boolean addItem(Player creator, String crateName, ItemStack item, double probability) {
    CachedCrate cc = crateCache.get(crateName.toLowerCase());
    if (cc == null)
      return false;

    // Push the new item into the sorted sequence of it's parent
    CrateItemModel itemModel = new CrateItemModel(cc.getCrate().getId(), creator, item, probability);
    CrateItemModel.pushSequenceMember(itemModel, buildCrateItemsQuery(cc.getCrate().getId()), pers);
    cc.addItem(itemModel);

    return true;
  }

  @Override
  public Optional<List<CrateItemModel>> getItems(String name) {
    if (crateCache.containsKey(name.toLowerCase()))
      return Optional.of(crateCache.get(name.toLowerCase()).getItems());
    return Optional.empty();
  }

  @Override
  public boolean deleteItem(CrateItemModel item) {
    crateCache.values().forEach(cc -> cc.removeItem(item));
    return CrateItemModel.deleteSequenceMember(item, pers);
  }

  @Override
  public boolean updateItem(CrateItemModel item) {
    pers.store(item);
    crateCache.values().stream().filter(cc -> cc.getCrate().getId().equals(item.getCrateId())).forEach(CachedCrate::update);
    return true;
  }

  @Override
  public Tuple<SequenceSortResult, Integer> sortItems(String crateName, int[] itemIdSequence) throws PersistenceException {
    CachedCrate crate = crateCache.get(crateName.toLowerCase());
    if (crate == null)
      return new Tuple<>(SequenceSortResult.MODEL_UNKNOWN, 0);

    Tuple<SequenceSortResult, Integer> res = CrateItemModel.alterSequence(buildCrateItemsQuery(crate.getCrate().getId()), itemIdSequence, pers);

    // Update the items in cache by re-fetching to receive the new order
    if (res.a() == SequenceSortResult.SORTED)
      crate.setItems(CrateItemModel.sequentize(pers.find(buildCrateItemsQuery(crate.getCrate().getId()))));

    return res;
  }

  @Override
  public Optional<CrateItemModel> drawItem(String crateName) {
    CachedCrate cc = crateCache.get(crateName.toLowerCase());

    if (cc == null)
      return Optional.empty();

    return cc.drawItem();
  }

  @Override
  public List<CrateKeyModel> getAllKeys(OfflinePlayer p) {
    if (keyCache.containsKey(p))
      return new ArrayList<>(keyCache.get(p).values());

    List<CrateKeyModel> keys = pers.find(buildKeyQuery(p, null));

    if (!keyCache.containsKey(p))
      keyCache.put(p, new HashMap<>());

    // Check for missing key models and create them
    keys.addAll(
      crateCache.values().stream()
        .map(CachedCrate::getCrate)
        .filter(c -> keys.stream().noneMatch(k -> k.getCrateId().equals(c.getId())))
        .map(c -> {
          CrateKeyModel model = new CrateKeyModel(p, c.getId(), 0);
          pers.store(model);
          return model;
        })
        .toList()
    );

    // Add keys to cache
    for (CrateKeyModel key : keys) {
      getCrate(key.getCrateId())
        .ifPresent(crate -> keyCache.get(p).put(crate.getName().toLowerCase(), key));
    }

    return keys;
  }

  @Override
  public Optional<CrateKeyModel> getKeys(OfflinePlayer p, String crateName) {
    if (keyCache.containsKey(p) && keyCache.get(p).containsKey(crateName.toLowerCase()))
      return Optional.of(keyCache.get(p).get(crateName.toLowerCase()));

    CrateModel crate = getCrate(crateName).orElse(null);
    if (crate == null)
      return Optional.empty();

    return pers.findFirst(buildKeyQuery(p, crate.getId()))
      .map(key -> {
        if (!keyCache.containsKey(p))
          keyCache.put(p, new HashMap<>());

        keyCache.get(p).put(crate.getName().toLowerCase(), key);
        return key;
      });
  }

  @Override
  public boolean updateKeys(OfflinePlayer p, String crateName, int keys) {
    CrateKeyModel model = getKeys(p, crateName).orElse(null);

    if (model == null)
      return false;

    model.setNumberOfKeys(Math.max(0, keys));
    pers.store(model);
    return true;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @EventHandler
  public void onBreak(BlockBreakEvent e) {
    if (
      crateCache.values().stream()
        .map(CachedCrate::getCrate)
        .anyMatch(crate -> crate.getLoc().equals(e.getBlock().getLocation()))
    )
      e.setCancelled(true);
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    if (e.getClickedBlock() == null)
      return;

    Player p = e.getPlayer();

    Location l = e.getClickedBlock().getLocation();
    CrateModel targetCrate = crateCache.values().stream()
      .map(CachedCrate::getCrate)
      .filter(crate -> crate.getLoc().equals(l))
      .findFirst()
      .orElse(null);

    if (targetCrate == null)
      return;

    e.setCancelled(true);

    // Prompt the user for confirmation before opening the crate
    if (e.getAction() == Action.RIGHT_CLICK_BLOCK && drawGui != null) {
      confirmationGui.show(p, (confirmed, inv) -> {
        // Open the crate and start a draw on confirmation
        if (confirmed) {
          drawGui.show(p, targetCrate, me.blvckbytes.blvcksys.handlers.gui.AnimationType.SLIDE_RIGHT, inv);
          return false;
        }

        p.sendMessage(
          cfg.get(ConfigKey.GUI_CRATE_DRAW_KEY_CANCELLED)
            .withPrefix()
            .withVariable("name", targetCrate.getName())
            .asScalar()
        );

        return true;
      }, me.blvckbytes.blvcksys.handlers.gui.AnimationType.SLIDE_UP);
    }

    // Show the crate contents
    else if (e.getAction() == Action.LEFT_CLICK_BLOCK && contentGui != null)
      contentGui.show(p, new Tuple<>(targetCrate, false), me.blvckbytes.blvcksys.handlers.gui.AnimationType.SLIDE_UP);
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    keyCache.remove(e.getPlayer());
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Build a query to select a crate's items by their crate ID
   * @param crateId ID of the target crate
   */
  private QueryBuilder<CrateItemModel> buildCrateItemsQuery(UUID crateId) {
    return new QueryBuilder<>(
      CrateItemModel.class,
      "crateId", EqualityOperation.EQ, crateId
    );
  }

  /**
   * Build a query to select a crate by it's name
   * @param name Name of the target crate
   */
  private QueryBuilder<CrateModel> buildCrateQuery(String name) {
    return new QueryBuilder<>(
      CrateModel.class,
      "name", EqualityOperation.EQ_IC, name
    );
  }

  /**
   * Build a query to select a player's keys
   * @param p Target player
   * @param crateId Optional target crate ID
   */
  private QueryBuilder<CrateKeyModel> buildKeyQuery(OfflinePlayer p, @Nullable UUID crateId) {
    QueryBuilder<CrateKeyModel> query = new QueryBuilder<>(
      CrateKeyModel.class,
      "owner__uuid", EqualityOperation.EQ, p.getUniqueId()
    );

    if (crateId != null)
      query.and("crateId", EqualityOperation.EQ, crateId);

    return query;
  }

  @Override
  public void cleanup() {
    if (effectTaskHandle != null)
      effectTaskHandle.cancel();

    crateCache.clear();
    keyCache.clear();
  }

  @Override
  public void initialize() {
    // Load all creates and all items into the cache
    pers.list(CrateModel.class).forEach(crate -> {
      List<CrateItemModel> items = CrateItemModel.sequentize(pers.find(buildCrateItemsQuery(crate.getId())));
      crateCache.put(crate.getName().toLowerCase(), new CachedCrate(crate, items));
    });

    // Task used to play crate effects
    effectTaskHandle = Bukkit.getScheduler().runTaskTimer(plugin, () -> {

      // Loop all known crates
      for (CachedCrate crate : crateCache.values()) {
        Location loc = crate.getCrate().getLoc();

        // Hasn't got a location yet
        if (loc == null)
          continue;

        // Center in on the location's block
        Location finalLoc = loc.clone().add(0.5, 0, 0.5);

        // Start rising upwards
        animationHandler.startAnimation(
          finalLoc,
          Bukkit.getOnlinePlayers(),
          AnimationType.DOUBLE_HELIX,
          new DoubleHelixParameter(
            new Vector(0, EFFECTS_VELOCITY, 0), EFFECTS_BPW, EFFECTS_RAD,
            crate.getCrate().getParticleEffectColor().getColor()
          )
        );

        // Stop rising when the desired height has been reached
        long animDur = (long) Math.ceil(EFFECTS_HEIGHT / EFFECTS_VELOCITY);
        Bukkit.getScheduler().runTaskLater(plugin, () -> animationHandler.stopAnimation(finalLoc, AnimationType.DOUBLE_HELIX), animDur);
      }

    }, 0L, EFFECTS_PERIOD_T);
  }
}
