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
import org.bukkit.OfflinePlayer;
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
  private final Map<PlayerStatistic, List<Consumer<OfflinePlayer>>> updateInterests;

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
  public PlayerStatsModel getStats(OfflinePlayer p) {
    if (cache.containsKey(p.getUniqueId()))
      return cache.get(p.getUniqueId());
    return loadPlayer(p);
  }

  @Override
  public void registerUpdateInterest(PlayerStatistic statistic, Consumer<OfflinePlayer> origin) {
    if (!this.updateInterests.containsKey(statistic))
      this.updateInterests.put(statistic, new ArrayList<>());
    this.updateInterests.get(statistic).add(origin);
  }

  ///////////////////////////////// DEATHS ////////////////////////////////////

  @Override
  public double calculateKD(OfflinePlayer p) {
    PlayerStatsModel stats = getStats(p);
    double deaths = stats.getDeaths();
    double kills = stats.getKills();

    if (deaths == 0)
      return kills;

    double kd = kills / deaths;
    return Math.round(kd * 100) / 100D;
  }

  ///////////////////////////////// MONEY ////////////////////////////////////

  @Override
  public void setMoney(OfflinePlayer p, int amount) {
    PlayerStatsModel stats = getStats(p);
    stats.setMoney(amount);
    pers.store(stats);

    callInterest(PlayerStatistic.MONEY, p);
  }

  @Override
  public void setLastLogin(OfflinePlayer p, Date stamp) {
    PlayerStatsModel stats = getStats(p);
    stats.setLastLogin(stamp);
    pers.store(stats);
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
  private PlayerStatsModel loadPlayer(OfflinePlayer p) {
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
  private void callInterest(PlayerStatistic statistic, OfflinePlayer origin) {
    // No interests registered yet
    if (!updateInterests.containsKey(statistic))
      return;

    for (Consumer<OfflinePlayer> interest : updateInterests.get(statistic))
      interest.accept(origin);
  }
}
