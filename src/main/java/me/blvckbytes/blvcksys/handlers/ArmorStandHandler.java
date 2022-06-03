package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.packets.communicators.armorstand.ArmorStandProperties;
import me.blvckbytes.blvcksys.packets.communicators.armorstand.IArmorStandCommunicator;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.exceptions.DuplicatePropertyException;
import me.blvckbytes.blvcksys.persistence.models.ArmorStandModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import net.minecraft.util.Tuple;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/03/2022

  Manages creating, updating and deleting virtual armor stands as well as
  updating their position and appearance.
*/
@AutoConstruct
public class ArmorStandHandler implements IArmorStandHandler, IAutoConstructed {

  // Period in ticks between fake armor stand tick invocations
  private static final long TICKER_PERIOD_T = 20;

  private final Map<ArmorStandModel, FakeArmorStand> cache;
  private final IPersistence pers;
  private final IArmorStandCommunicator armorComm;
  private final JavaPlugin plugin;
  private BukkitTask tickerHandle;

  public ArmorStandHandler(
    @AutoInject IPersistence pers,
    @AutoInject IArmorStandCommunicator armorComm,
    @AutoInject JavaPlugin plugin
  ) {
    this.cache = new HashMap<>();
    this.pers = pers;
    this.armorComm = armorComm;
    this.plugin = plugin;
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public Optional<ArmorStandModel> create(OfflinePlayer creator, String name, Location loc) {
    try {
      ArmorStandModel as = new ArmorStandModel(creator, name, loc);
      pers.store(as);
      cache.put(as, fakeFromModel(as));
      return Optional.of(as);
    } catch (DuplicatePropertyException e) {
      return Optional.empty();
    }
  }

  @Override
  public boolean delete(String name) {
    for (ArmorStandModel model : cache.keySet()) {
      if (!model.getName().equalsIgnoreCase(name))
        continue;

      FakeArmorStand fas = cache.remove(model);

      if (fas != null)
        fas.destroy();

      break;
    }

    return pers.delete(buildQuery(name)) > 0;
  }

  @Override
  public boolean move(String name, Location loc) {
    ArmorStandModel target = getByName(name).orElse(null);

    if (target == null)
      return false;

    target.setLoc(loc);
    pers.store(target);

    FakeArmorStand fas = cache.get(target);
    fas.setLoc(loc);

    return true;
  }

  @Override
  public Optional<ArmorStandModel> getByName(String name) {
    return cache.keySet().stream()
      .filter(as -> as.getName().equalsIgnoreCase(name))
      .findFirst();
  }

  @Override
  public Optional<ArmorStandModel> getByLocation(Location loc, int radius) {
    double r = Math.pow(radius, 2);
    return cache.keySet().stream()
      .map(as -> new Tuple<>(as, as.getLoc().distanceSquared(loc)))
      .filter(t -> t.b() < r)
      .sorted((a, b) -> (int) (a.b() - b.b()))
      .map(Tuple::a)
      .findFirst();
  }

  @Override
  public void cleanup() {
    if (this.tickerHandle != null)
      this.tickerHandle.cancel();

    cache.values().forEach(FakeArmorStand::destroy);
    cache.clear();
  }

  @Override
  public void initialize() {
    for (ArmorStandModel model : pers.list(ArmorStandModel.class))
      cache.put(model, fakeFromModel(model));

    this.tickerHandle = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
      cache.values().forEach(FakeArmorStand::tick);
    }, 0L, TICKER_PERIOD_T);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Builds a query to select an armor stand by it's name
   * @param name Target name
   */
  private QueryBuilder<ArmorStandModel> buildQuery(String name) {
    return new QueryBuilder<>(
      ArmorStandModel.class,
      "name", EqualityOperation.EQ_IC, name
    );
  }

  /**
   * Generates a new fake armor stand from it's underlying model
   * @param model Model to create from
   * @return Fake armor stand instance
   */
  private FakeArmorStand fakeFromModel(ArmorStandModel model) {
    ArmorStandProperties props = new ArmorStandProperties(
      false, true, false, null
    );
    return new FakeArmorStand(armorComm, props, model.getLoc());
  }
}
