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
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/17/2022

  Manages creating, update, listing and deleting homes of individual
  players by their home-name.
 */
@AutoConstruct
public class HomeHandler implements IHomeHandler {

  private final IPersistence pers;

  public HomeHandler(
    @AutoInject IPersistence pers
  ) {
    this.pers = pers;
  }

  @Override
  public Optional<HomeModel> createHome(OfflinePlayer creator, String name, Location loc) throws PersistenceException {
    try {
      HomeModel home = new HomeModel(creator, name, loc);
      pers.store(home);
      return Optional.of(home);
    } catch (DuplicatePropertyException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean updateLocation(OfflinePlayer creator, String name, Location loc) throws PersistenceException {
    HomeModel home = pers.findFirst(buildQueryFor(creator, name)).orElse(null);

    if (home == null)
      return false;

    home.setLoc(loc);
    pers.store(home);

    return true;
  }

  @Override
  public boolean deleteHome(OfflinePlayer creator, String name) throws PersistenceException {
    return pers.delete(buildQueryFor(creator, name)) > 0;
  }

  @Override
  public int countHomes(OfflinePlayer creator) throws PersistenceException {
    return pers.count(buildQueryFor(creator, null));
  }

  @Override
  public List<HomeModel> listHomes(OfflinePlayer creator) throws PersistenceException {
    return pers.find(buildQueryFor(creator, null));
  }

  @Override
  public Optional<HomeModel> findHome(OfflinePlayer creator, String name) throws PersistenceException {
    return pers.findFirst(buildQueryFor(creator, name));
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
