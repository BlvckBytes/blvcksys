package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.exceptions.PersistenceException;
import me.blvckbytes.blvcksys.persistence.models.NpcModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.FieldQueryGroup;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/26/2022

  Handles spawning npcs for players within a given radius and catches
  interactions to fire custom npc events.
*/
@AutoConstruct
public class NpcHandler implements INpcHandler, IAutoConstructed {

  // Specifies the time between hologram update triggers in ticks
  private static final long UPDATE_INTERVAL_TICKS = 20;

  private final Map<String, FakeNpc> npcs;
  private final JavaPlugin plugin;
  private final IPersistence pers;
  private int intervalHandle;

  public NpcHandler(
    @AutoInject JavaPlugin plugin,
    @AutoInject IPersistence pers
  ) {
    this.plugin = plugin;
    this.pers = pers;

    this.intervalHandle = -1;
    this.npcs = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public Optional<NpcModel> createNpc(OfflinePlayer creator, String name, Location loc) {
    try {
      NpcModel npc = new NpcModel(creator, name, loc, null);
      pers.store(npc);
      npcs.put(name.toLowerCase(), fakeNpcFromModel(npc));
      return Optional.of(npc);
    } catch (DuplicatePropertyException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean deleteNpc(String name) {
    boolean res = pers.delete(buildQuery(name)) > 0;

    if (res)
      npcs.remove(name.toLowerCase()).destroy();

    return res;
  }

  @Override
  public boolean moveNpc(String name, Location loc) {
    NpcModel npc = pers.findFirst(buildQuery(name)).orElse(null);

    if (npc == null)
      return false;

    npc.setLoc(loc);
    pers.store(npc);

    findFakeNpcByModel(npc).setLoc(loc);
    return true;
  }

  @Override
  public boolean changeSkin(String name, String skin) {
    NpcModel npc = pers.findFirst(buildQuery(name)).orElse(null);

    if (npc == null)
      return false;

    npc.setSkin(skin);
    pers.store(npc);

    findFakeNpcByModel(npc).setSkin(skin);
    return true;
  }

  @Override
  public List<NpcModel> getNear(Location where, double rangeRadius) {
    // This should never be the case...
    if (where.getWorld() == null)
      throw new PersistenceException("Cannot find any near npcs if no world has been provided");

    return pers.find(
      new QueryBuilder<>(
        // Has to be in the same world
        NpcModel.class, "loc__world", EqualityOperation.EQ, where.getWorld().getName()
      )
        // X range constraint
        .and(
          new FieldQueryGroup("loc__x", EqualityOperation.GTE, where.getX() - rangeRadius)
            .and("loc__x", EqualityOperation.LTE, where.getX() + rangeRadius)
        )

        // Y range constraint
        .and(
          new FieldQueryGroup("loc__y", EqualityOperation.GTE, where.getY() - rangeRadius)
            .and("loc__y", EqualityOperation.LTE, where.getY() + rangeRadius)
        )

        // Z range constraint
        .and(
          new FieldQueryGroup("loc__z", EqualityOperation.GTE, where.getZ() - rangeRadius)
            .and("loc__z", EqualityOperation.LTE, where.getZ() + rangeRadius)
        )
    );
  }

  @Override
  public void cleanup() {
    if (intervalHandle > 0)
      Bukkit.getScheduler().cancelTask(intervalHandle);

    for (FakeNpc npc : npcs.values())
      npc.destroy();

    npcs.clear();
  }

  @Override
  public void initialize() {
    for (NpcModel npc : pers.list(NpcModel.class))
      npcs.put(npc.getName().toLowerCase(), fakeNpcFromModel(npc));

    intervalHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
      for (FakeNpc npc : npcs.values())
        npc.tick();
    }, 0L, UPDATE_INTERVAL_TICKS);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Find a fake npc by it's model from cache, creates cache entry if
   * the npc could not be located
   * @param model Corresponding model
   * @return Fake npc found or created
   */
  private FakeNpc findFakeNpcByModel(NpcModel model) {
    String name = model.getName().toLowerCase();

    if (this.npcs.containsKey(name))
      return this.npcs.get(name);

    FakeNpc fn = fakeNpcFromModel(model);
    this.npcs.put(name, fn);

    return fn;
  }

  /**
   * Create a new managed fake npc from it's underlying model
   * @param model Corresponding model
   * @return New fake npc
   */
  private FakeNpc fakeNpcFromModel(NpcModel model) {
    return new FakeNpc(
      model.getLoc(), model.getSkin()
    );
  }

  /**
   * Build the selection query for a specific npc by name
   * @param name Target name
   */
  private QueryBuilder<NpcModel> buildQuery(String name) {
    return new QueryBuilder<>(
      NpcModel.class,
      "name", EqualityOperation.EQ, name
    );
  }
}
