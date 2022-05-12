package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.PlayerStatsModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.*;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/12/2022

  Handles initially creating as well as caching player-stats and exposes
  an API to alter the individual statistics available, while also allowing
  other modules to register an interest for occurring updates. Updates
  are mostly bound to events, handled too by this module.
*/
@AutoConstruct
public class PlayerStatsHandler implements IPlayerStatsHandler, IAutoConstructed, Listener {

  // There can be multiple update interests per statistic
  private final Map<PlayerStatistic, List<Consumer<Player>>> updateInterests;

  // Each player has their stats, combined with a "delta-occurred" flag
  private final Map<UUID, PlayerStatsModel> cache;

  private final IPersistence pers;
  private final ILogger logger;

  public PlayerStatsHandler(
    @AutoInject IPersistence pers,
    @AutoInject ILogger logger
  ) {
    this.pers = pers;
    this.logger = logger;

    this.updateInterests = new HashMap<>();
    this.cache = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void registerUpdateInterest(PlayerStatistic statistic, Consumer<Player> origin) {
    if (!this.updateInterests.containsKey(statistic))
      this.updateInterests.put(statistic, new ArrayList<>());
    this.updateInterests.get(statistic).add(origin);
  }

  ///////////////////////////////// KILLS ////////////////////////////////////

  @Override
  public int getKills(Player p) {
    return getStats(p).getKills();
  }

  ///////////////////////////////// DEATHS ////////////////////////////////////

  @Override
  public int getDeaths(Player p) {
    return getStats(p).getDeaths();
  }

  @Override
  public double calculateKD(Player p) {
    double deaths = getDeaths(p);
    double kills = getKills(p);

    if (deaths == 0)
      return kills;

    double kd = kills / deaths;
    return Math.round(kd * 100) / 100D;
  }

  @Override
  public void cleanup() {}

  @Override
  public void initialize() {
    for (Player t : Bukkit.getOnlinePlayers())
      loadPlayer(t);
  }

  //=========================================================================//
  //                                 Listener                                //
  //=========================================================================//

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    loadPlayer(e.getPlayer());
  }

  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    // Save and remove this player from the cache
    this.cache.remove(e.getPlayer().getUniqueId());
  }

  @EventHandler
  public void onKill(EntityDeathEvent e) {
    if (!(e.getEntity() instanceof Player p))
      return;

    Player killer = e.getEntity().getKiller();
    if (killer != null)
      incrementKills(killer);

    incrementDeaths(p);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Get the statistics of a player and create the model on absence
   * @param p Target player
   * @return Stats model
   */
  private PlayerStatsModel getStats(Player p) {
    if (cache.containsKey(p.getUniqueId()))
      return cache.get(p.getUniqueId());
    return loadPlayer(p);
  }

  /**
   * Increment the deaths of a player and store the update
   * @param p Target player
   */
  private void incrementDeaths(Player p) {
    PlayerStatsModel stats = getStats(p);
    stats.setDeaths(stats.getDeaths() + 1);
    pers.store(stats);

    callInterest(PlayerStatistic.KILLS, p);
  }

  /**
   * Increment the kills of a player and store the update
   * @param p Target player
   */
  private void incrementKills(Player p) {
    PlayerStatsModel stats = getStats(p);
    stats.setKills(stats.getKills() + 1);
    pers.store(stats);

    callInterest(PlayerStatistic.DEATHS, p);
  }

  /**
   * Load a player's stats from persistence and cache the result
   * @param p Target player
   * @return Stats model
   */
  private PlayerStatsModel loadPlayer(Player p) {
    PlayerStatsModel model = pers.findFirst(
      new QueryBuilder<>(
        PlayerStatsModel.class,
        "owner__uuid", EqualityOperation.EQ, p.getUniqueId()
      )
    ).orElse(null);

    // This player didn't yet have stats
    if (model == null) {
      model = PlayerStatsModel.createDefault(p);
      pers.store(model);
    }

    this.cache.put(p.getUniqueId(), model);
    return model;
  }

  /**
   * Store the statistics of a player
   * @param model Stats model
   */
  private void save(PlayerStatsModel model) {
    try {
      pers.store(model);
    } catch (Exception e) {
      logger.logError(e);
    }
  }

  /**
   * Call all registered interests for a specific statistic on a given player
   * @param statistic Changed statistic
   * @param origin Target player
   */
  private void callInterest(PlayerStatistic statistic, Player origin) {
    for (Consumer<Player> interest : updateInterests.get(statistic))
      interest.accept(origin);
  }
}
