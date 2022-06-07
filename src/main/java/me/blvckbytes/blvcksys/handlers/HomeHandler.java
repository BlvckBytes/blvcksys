package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.HomeModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Manages creating, update, listing and deleting homes of individual
  players by their home-name.
 */
@AutoConstruct
public class HomeHandler implements IHomeHandler, Listener {

  private final Map<OfflinePlayer, List<HomeModel>> cache;
  private final IPersistence pers;

  public HomeHandler(
    @AutoInject IPersistence pers
  ) {
    this.pers = pers;
    this.cache = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public Optional<HomeModel> createHome(OfflinePlayer creator, String name, Location loc) throws PersistenceException {
    try {
      HomeModel home = HomeModel.createDefault(creator, name, loc);
      pers.store(home);
      cacheHome(creator, home);
      return Optional.of(home);
    } catch (DuplicatePropertyException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean updateLocation(OfflinePlayer creator, String name, Location loc) throws PersistenceException {
    HomeModel home = findHome(creator, name).orElse(null);

    if (home == null)
      return false;

    home.setLoc(loc);
    pers.store(home);

    return true;
  }

  @Override
  public boolean updateIcon(OfflinePlayer creator, String name, Material icon) throws PersistenceException {
    HomeModel home = findHome(creator, name).orElse(null);

    if (home == null)
      return false;

    home.setIcon(icon);
    pers.store(home);

    return true;
  }

  @Override
  public boolean deleteHome(OfflinePlayer creator, String name) throws PersistenceException {
    if (pers.delete(buildQueryFor(creator, name)) > 0) {
      if (cache.containsKey(creator))
        cache.get(creator).removeIf(h -> h.getName().equalsIgnoreCase(name));
      return true;
    }
    return false;
  }

  @Override
  public int countHomes(OfflinePlayer creator) throws PersistenceException {
    return pers.count(buildQueryFor(creator, null));
  }

  @Override
  public List<HomeModel> listHomes(OfflinePlayer creator) throws PersistenceException {
    // No homes cached at all or some homes are still missing, fetch from DB
    if (!cache.containsKey(creator) || cache.get(creator).size() != countHomes(creator)) {
      List<HomeModel> homes = pers.find(buildQueryFor(creator, null));

      cache.remove(creator);
      homes.forEach(h -> cacheHome(creator, h));

      return homes;
    }

    // Return from cache
    return cache.get(creator);
  }

  @Override
  public Optional<HomeModel> findHome(OfflinePlayer creator, String name) throws PersistenceException {
    return cache.getOrDefault(creator, new ArrayList<>())
      .stream().filter(h -> h.getName().equalsIgnoreCase(name))
      .findFirst()
      .or(() -> pers.findFirst(buildQueryFor(creator, name)))
      .map(h -> {
        cacheHome(creator, h);
        return h;
      });
  }

  //=========================================================================//
  //                                Listeners                                //
  //=========================================================================//

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    cache.remove(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Saves a home into cache for a specific player
   * @param creator Target player
   * @param home Home to cache
   */
  private void cacheHome(OfflinePlayer creator, HomeModel home) {
    // Don't cache for offline players
    if (!creator.isOnline())
      return;

    if (!cache.containsKey(creator))
      cache.put(creator, new ArrayList<>());
    cache.get(creator).add(home);
  }

  /**
   * Build a personalized query to receive only results in regards to this creator
   * @param creator creator to receive results for
   * @param name Name of a specific home, null to receive all existing homes
   */
  private QueryBuilder<HomeModel> buildQueryFor(OfflinePlayer creator, @Nullable String name) {
    QueryBuilder<HomeModel> query = new QueryBuilder<>(
      HomeModel.class,
      "creator__uuid", EqualityOperation.EQ, creator.getUniqueId()
    );

    if (name != null)
      query.and("name", EqualityOperation.EQ_IC, name);

    return query;
  }
}
