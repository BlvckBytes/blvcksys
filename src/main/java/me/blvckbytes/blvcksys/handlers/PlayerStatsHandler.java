package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.di.IAutoConstructed;
import me.blvckbytes.blvcksys.events.IAfkListener;
import me.blvckbytes.blvcksys.events.IChatListener;
import me.blvckbytes.blvcksys.persistence.IPersistence;
import me.blvckbytes.blvcksys.persistence.models.PlayerStatsModel;
import me.blvckbytes.blvcksys.persistence.query.EqualityOperation;
import me.blvckbytes.blvcksys.persistence.query.QueryBuilder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

  // Time in ticks between ticks of the play time task
  private static final long TICKER_PERIOD_S = 30;

  // Amount of kills between killstreak broadcasts
  private static final int KILLSTREAK_BROADCAST = 10;

  // There can be multiple update interests per statistic
  private final Map<PlayerStatistic, List<Consumer<OfflinePlayer>>> updateInterests;

  // Each player is being mapped to their stats model
  private final Map<OfflinePlayer, PlayerStatsModel> cache;

  // Each statistic is being mapped to a list of top players in that statistic
  private final Map<PlayerStatistic, List<PlayerStatsModel>> topCache;

  private final IPersistence pers;
  private final JavaPlugin plugin;
  private final IAfkListener afk;
  private final ICombatLogHandler combatlog;
  private final IChatListener chatListener;
  private final IConfig cfg;

  private BukkitTask tickerHandle;

  public PlayerStatsHandler(
    @AutoInject IPersistence pers,
    @AutoInject JavaPlugin plugin,
    @AutoInject IAfkListener afk,
    @AutoInject ICombatLogHandler combatlog,
    @AutoInject IChatListener chatListener,
    @AutoInject IConfig cfg
  ) {
    this.pers = pers;
    this.plugin = plugin;
    this.afk = afk;
    this.combatlog = combatlog;
    this.chatListener = chatListener;
    this.cfg = cfg;

    this.updateInterests = new HashMap<>();
    this.topCache = new HashMap<>();
    this.cache = new HashMap<>();
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public List<PlayerStatsModel> getTop5Ranked(PlayerStatistic statistic) {
    // This statistic's top players have already been loaded into cache,
    // there is no point fetching from DB again, as there will be no
    // better players available in the data-set
    List<PlayerStatsModel> topStats = topCache.get(statistic);
    if (topStats != null) {

      // Remove all currently cached stats from the top stats list, to refresh them, but
      // keep the non-available, initially fetched, highest stats
      cache.values().forEach(cPs -> {
        topStats.removeIf(tPs -> tPs.getOwner().getUniqueId().equals(cPs.getOwner().getUniqueId()));
      });

      // Decide on new top 5 players from the cache as well as the top cache combined
      List<PlayerStatsModel> newTopStats = Stream.concat(
        cache.values().stream(),
        topStats.stream()
      )
        .sorted((a, b) -> (
          switch (statistic) {
            case KILLS -> b.getKills() - a.getKills();
            case DEATHS -> b.getDeaths() - a.getDeaths();
            case MONEY -> b.getMoney() - a.getMoney();
            case PLAYTIME -> -Long.compare(a.getPlaytimeSeconds(), b.getPlaytimeSeconds());
            case CURRENT_KILLSTREAK -> b.getCurrentKillstreak() - a.getCurrentKillstreak();
            case HIGHEST_KILLSTREAK -> b.getHighestKillstreak() - a.getHighestKillstreak();
          }
        ))
        .limit(5)
        .collect(Collectors.toList());

      // Update and return the new top cache
      topCache.put(statistic, newTopStats);
      return newTopStats;
    }

    QueryBuilder<PlayerStatsModel> query = new QueryBuilder<>(PlayerStatsModel.class).limit(5);

    switch (statistic) {
      case KILLS -> query.orderBy("kills", false);
      case DEATHS -> query.orderBy("deaths", false);
      case MONEY -> query.orderBy("money", false);
      case PLAYTIME -> query.orderBy("playtimeSeconds", false);
      case CURRENT_KILLSTREAK -> query.orderBy("currentKillstreak", false);
      case HIGHEST_KILLSTREAK -> query.orderBy("highestKillstreak", false);
    }

    // Fetch the statistic's top players from DB and cache the result
    List<PlayerStatsModel> top5 = pers.find(query);
    top5.forEach(top -> cache.put(top.getOwner(), top));
    topCache.put(statistic, top5);

    return top5;
  }

  @Override
  public PlayerStatsModel getStats(OfflinePlayer p) {
    if (cache.containsKey(p))
      return cache.get(p);
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
  public void addMoney(OfflinePlayer p, int amount) {
    PlayerStatsModel stats = getStats(p);
    stats.setMoney(stats.getMoney() + amount);
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
  public void cleanup() {
    if (tickerHandle != null)
      tickerHandle.cancel();
  }

  @Override
  public void initialize() {
    for (Player t : Bukkit.getOnlinePlayers())
      loadPlayer(t);

    this.tickerHandle = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::tickPlayTime, 0L, TICKER_PERIOD_S * 20);
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
    this.cache.remove(e.getPlayer());
  }

  @EventHandler
  public void onKill(EntityDeathEvent e) {
    if (!(e.getEntity() instanceof Player p))
      return;

    // Either get the damager from this event or the last known damager
    // from the current combatlog session if it wasn't a player
    Player lastDamager = combatlog.getLastDamager(p).orElse(null);
    Player killer = p.getKiller() == null ? lastDamager : p.getKiller();

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

    // Had an active killstreak
    if (stats.getCurrentKillstreak() > 0) {

      // Was higher than the highest value, update highest
      if (stats.getCurrentKillstreak() > stats.getHighestKillstreak()) {
        stats.setHighestKillstreak(stats.getCurrentKillstreak());
        callInterest(PlayerStatistic.HIGHEST_KILLSTREAK, p);
      }

      // Has at least once been announced
      if (stats.getCurrentKillstreak() >= KILLSTREAK_BROADCAST) {
        chatListener.broadcastMessage(
          Bukkit.getOnlinePlayers(),
          cfg.get(ConfigKey.KILLSTREAK_RESET)
            .withPrefix()
            .withVariable("player", p.getName())
            .withVariable("curr_killstreak", stats.getCurrentKillstreak())
            .asScalar()
        );
      }

      // Reset killstreak back to zero
      stats.setCurrentKillstreak(0);
      callInterest(PlayerStatistic.CURRENT_KILLSTREAK, p);
    }

    pers.store(stats);
    callInterest(PlayerStatistic.DEATHS, p);
  }

  /**
   * Increment the kills of a player and store the update
   * @param p Target player
   */
  private void incrementKills(Player p) {
    PlayerStatsModel stats = getStats(p);
    stats.setKills(stats.getKills() + 1);

    // Advance the current killstreak counter
    stats.setCurrentKillstreak(stats.getCurrentKillstreak() + 1);
    callInterest(PlayerStatistic.CURRENT_KILLSTREAK, p);

    // Has reached a multiple of the killstreak broadcast threshold
    if (stats.getCurrentKillstreak() != 0 && stats.getCurrentKillstreak() % KILLSTREAK_BROADCAST == 0) {
      chatListener.broadcastMessage(
        Bukkit.getOnlinePlayers(),
        cfg.get(ConfigKey.KILLSTREAK_ADVANCED)
          .withPrefix()
          .withVariable("player", p.getName())
          .withVariable("curr_killstreak", stats.getCurrentKillstreak())
          .asScalar()
      );
    }

    pers.store(stats);
    callInterest(PlayerStatistic.KILLS, p);
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

    this.cache.put(p, model);
    return model;
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

  /**
   * Advances the playing time of all currently online players
   */
  private void tickPlayTime() {
    for (Player t : Bukkit.getOnlinePlayers()) {
      // Playing time doesn't advance when you're AFK
      if (afk.isAFK(t))
        continue;

      // Advance the play time by one ticker period
      // This might add a few extra seconds in special cases, but that's
      // neglectable and not worth the extra computing effort
      PlayerStatsModel stats = getStats(t);
      stats.setPlaytimeSeconds(stats.getPlaytimeSeconds() + TICKER_PERIOD_S);
      pers.store(stats);
    }

    // After updating all playtimes in a separate thread, call all interests synchronously
    Bukkit.getScheduler().runTask(plugin, () -> {
      for (Player t : Bukkit.getOnlinePlayers())
        callInterest(PlayerStatistic.PLAYTIME, t);
    });
  }
}
