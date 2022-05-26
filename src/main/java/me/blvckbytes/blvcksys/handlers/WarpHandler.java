package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.WarpModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/26/2022

  Handles creating, moving and deleting warping points.
*/
@AutoConstruct
public class WarpHandler implements IWarpHandler {

  private final Map<String, WarpModel> cache;
  private final IPersistence pers;

  public WarpHandler(
    @AutoInject IPersistence pers
  ) {
    this.cache = new HashMap<>();
    this.pers = pers;
  }

  @Override
  public Optional<WarpModel> getWarp(String name) throws PersistenceException {
    String lcName = name.toLowerCase();

    if (cache.containsKey(lcName))
      return Optional.of(cache.get(lcName));

    return pers.findFirst(buildQuery(name))
      .map(res -> {
        cache.put(lcName, res);
        return res;
      });
  }

  @Override
  public boolean setWarp(String name, OfflinePlayer creator, Location loc) throws PersistenceException {
    try {
      WarpModel warp = new WarpModel(name, loc, creator);
      pers.store(warp);
      cache.put(name.toLowerCase(), warp);
      return true;
    } catch (DuplicatePropertyException e) {
      return false;
    }
  }

  @Override
  public boolean moveWarp(String name, OfflinePlayer creator, Location loc) throws PersistenceException {
    WarpModel existing = getWarp(name).orElse(null);

    if (existing == null)
      return false;

    existing.setLoc(loc);
    pers.store(existing);
    return true;
  }

  @Override
  public boolean deleteWarp(String name) {
    cache.remove(name.toLowerCase());
    return pers.delete(buildQuery(name)) > 0;
  }

  private QueryBuilder<WarpModel> buildQuery(String name) throws PersistenceException {
    return new QueryBuilder<>(
      WarpModel.class,
      "name", EqualityOperation.EQ, name
    );
  }
}
