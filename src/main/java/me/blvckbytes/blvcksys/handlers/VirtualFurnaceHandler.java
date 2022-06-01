package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.handlers.gui.VirtualFurnace;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.VirtualFurnaceModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.MCReflect;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/01/2022

  Handles virtual furnaces by keeping a list of instances per player which
  are ticked inside the main loop and persisted periodically as well as when
  the handler is unloaded.
*/
@AutoConstruct
public class VirtualFurnaceHandler implements IVirtualFurnaceHandler, IAutoConstructed, Listener {

  // Interval for storing all loaded furnaces in seconds
  private static final long STORE_INTERVAL_S = 60;

  // Maximum time of inactivity in a furnace until it's persisted and unloaded in seconds
  private static final long INACTIVITY_MAX_S = 60;

  // Minimum number of furnaces a player is allowed to have
  private static final int MIN_FURNACES = 1;

  // Mapping players to their virtual furnaces, which each have a unique
  // sequence ID, as players may own multiple concurrent furnaces
  private final Map<OfflinePlayer, Map<Integer, Tuple<VirtualFurnace, @Nullable VirtualFurnaceModel>>> furnaces;

  private final JavaPlugin plugin;
  private final IPersistence pers;
  private final MCReflect refl;
  private BukkitTask tickerHandle, storeHandle;

  public VirtualFurnaceHandler(
    @AutoInject JavaPlugin plugin,
    @AutoInject MCReflect refl,
    @AutoInject IPersistence pers
  ) {
    this.furnaces = new HashMap<>();
    this.plugin = plugin;
    this.refl = refl;
    this.pers = pers;
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public VirtualFurnace accessFurnace(OfflinePlayer p, int index) {
    return loadOrCreateFurnace(p, index);
  }

  @Override
  public List<VirtualFurnace> listFurnaces(OfflinePlayer p) {
    loadAllFurnaces(p);
    return furnaces
      .getOrDefault(p, new HashMap<>())
      .values()
      .stream()
      .map(Tuple::a)
      .sorted(Comparator.comparingInt(VirtualFurnace::getIndex))
      .toList();
  }

  @Override
  public int getUsedNumberOfFurnaces(OfflinePlayer p) {
    if (furnaces.containsKey(p))
      return furnaces.get(p).size();
    return pers.count(buildQuery(p));
  }

  @Override
  public int getAvailableNumberOfFurnaces(Player p) {
    return PlayerPermission.COMMAND_FURNACE_INSTANCES.getSuffixNumber(p, true).orElse(MIN_FURNACES);
  }

  @Override
  public void cleanup() {
    if (this.tickerHandle != null)
      tickerHandle.cancel();

    if (this.storeHandle != null)
      storeHandle.cancel();

    for (OfflinePlayer owner : furnaces.keySet())
      persistFurnaces(owner);
  }

  @Override
  public void initialize() {
    this.tickerHandle = Bukkit.getScheduler().runTaskTimer(plugin, this::tickFurnaces, 0L, 1L);
    this.storeHandle = Bukkit.getScheduler().runTaskTimer(plugin, this::saveFurnaces, 0L, STORE_INTERVAL_S * 20);

    for (Player p : Bukkit.getOnlinePlayers())
      loadAllFurnaces(p);
  }

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Store and unload all furnaces of this player
    persistFurnaces(e.getPlayer());
    furnaces.remove(e.getPlayer());
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    // Load all the player's furnaces on join
    loadAllFurnaces(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Tick all currently registered furnaces
   */
  private void tickFurnaces() {
    for (Map<Integer, Tuple<VirtualFurnace, VirtualFurnaceModel>> pFurnaces : furnaces.values()) {
      for (Iterator<Tuple<VirtualFurnace, VirtualFurnaceModel>> furnaceI = pFurnaces.values().iterator(); furnaceI.hasNext();) {
        Tuple<VirtualFurnace, VirtualFurnaceModel> furnace = furnaceI.next();
        VirtualFurnace vf = furnace.a();

        vf.tick(refl);

        // Furnace is inactive, unload and persist
        if (System.currentTimeMillis() > vf.getLastActivity() + INACTIVITY_MAX_S * 1000) {
          furnaceI.remove();
          persistFurnace(furnace, false);
        }
      }
    }
  }

  /**
   * Save all currently registered furnaces persistently
   */
  private void saveFurnaces() {
    for (OfflinePlayer owner : furnaces.keySet())
      persistFurnaces(owner);
  }

  /**
   * Persist a single furnace and either create or re-use it's corresponding model
   * @param vf Furnace to persist
   * @param cache Whether to store the updated model in cache
   */
  private void persistFurnace(Tuple<VirtualFurnace, VirtualFurnaceModel> vf, boolean cache) {
    VirtualFurnace furnace = vf.a();
    VirtualFurnaceModel presentModel = vf.b();

    // Furnace already has a persistent model, update that
    if (presentModel != null) {
      furnace.takeSnapshot(presentModel);
      pers.store(presentModel);
      return;
    }

    // Furnace doesn't yet have a model, create a new one with default values, then take a snapshot into that
    VirtualFurnaceModel model = new VirtualFurnaceModel(furnace.getHolder(), vf.a().getIndex(), null, null, null, 0, 0);
    furnace.takeSnapshot(model);
    pers.store(model);

    // Save model in cache
    if (cache)
      furnaces.get(furnace.getHolder()).put(furnace.getIndex(), new Tuple<>(furnace, model));
  }

  /**
   * Persist all furnaces a player currently has loaded
   * @param p Target player
   */
  private void persistFurnaces(OfflinePlayer p) {
    Map<Integer, Tuple<VirtualFurnace, VirtualFurnaceModel>> pFurnaces = furnaces.get(p);

    // Player has no furnaces yet
    if (pFurnaces == null)
      return;

    for (Tuple<VirtualFurnace, VirtualFurnaceModel> furnace : pFurnaces.values())
      persistFurnace(furnace, true);
  }

  /**
   * Loads a furnace either from the local cache or from persistence and
   * creates a brand new instance if the index isn't yet registered
   * @param p Target player
   * @param index Target index
   * @return Virtual furnace instance
   */
  private VirtualFurnace loadOrCreateFurnace(OfflinePlayer p, int index) {
    if (!furnaces.containsKey(p))
      furnaces.put(p, new HashMap<>());

    // Try to get a furnace from the local cache
    Map<Integer, Tuple<VirtualFurnace, VirtualFurnaceModel>> pFurnaces = furnaces.get(p);
    Tuple<VirtualFurnace, VirtualFurnaceModel> furnace = pFurnaces.get(index);

    // Furnace available
    if (furnace != null)
      return furnace.a();

    // Try to load a furnace from persistence
    VirtualFurnaceModel model = pers.findFirst(buildQuery(p, index)).orElse(null);

    // Furnace available
    if (model != null) {
      VirtualFurnace vf = VirtualFurnace.loadFromSnapshot(model);
      pFurnaces.put(index, new Tuple<>(vf, model));
      return vf;
    }

    // Create a brand new furnace without a corresponding
    // model (until first store) and cache it
    VirtualFurnace vf = new VirtualFurnace(p, index);
    pFurnaces.put(index, new Tuple<>(vf, null));
    return vf;
  }

  /**
   * Load all furnaces of a given player so they can start to be processed
   * @param p Target player
   */
  private void loadAllFurnaces(OfflinePlayer p) {
    if (!furnaces.containsKey(p))
      furnaces.put(p, new HashMap<>());

    Map<Integer, Tuple<VirtualFurnace, VirtualFurnaceModel>> pFurnaces = furnaces.get(p);

    // All furnaces are loaded already
    if (pFurnaces.size() == pers.count(buildQuery(p)))
      return;

    List<VirtualFurnaceModel> models = pers.find(buildQuery(p));
    for (VirtualFurnaceModel model : models) {
      // This furnace is already loaded, don't overwrite it
      if (pFurnaces.containsKey(model.getIndex()))
        continue;

      VirtualFurnace vf = VirtualFurnace.loadFromSnapshot(model);
      pFurnaces.put(model.getIndex(), new Tuple<>(vf, model));
    }
  }

  /**
   * Builds the selecting query for a player's indexed virtual furnace
   * @param p Target player
   * @param index Target index
   */
  private QueryBuilder<VirtualFurnaceModel> buildQuery(OfflinePlayer p, int index) {
    return new QueryBuilder<>(
      VirtualFurnaceModel.class,
      "owner__uuid", EqualityOperation.EQ, p.getUniqueId()
    )
      .and("index", EqualityOperation.EQ, index);
  }

  /**
   * Builds the selecting query for all virtual furnaces of a player
   * @param p Target player
   */
  private QueryBuilder<VirtualFurnaceModel> buildQuery(OfflinePlayer p) {
    return new QueryBuilder<>(
      VirtualFurnaceModel.class,
      "owner__uuid", EqualityOperation.EQ, p.getUniqueId()
    );
  }
}
