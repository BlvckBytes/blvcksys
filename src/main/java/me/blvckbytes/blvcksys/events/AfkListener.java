package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.handlers.ITeamHandler;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.di.IAutoConstructed;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/27/2022

  Listens to events like moving, commands and chat to try and determine whether
  a player is afk or not, then marks them grayed in the tablist accordingly.
 */
@AutoConstruct
public class AfkListener implements Listener, IAutoConstructed, IAfkListener {

  // Timeout in milliseconds for a player to be marked as AFK
  private static final int TIMEOUT_MS = 1000 * 60 * 10; // 10 minutes

  // Each player and a timestamp of their last action on the server
  private final Map<Player, Long> lastAction;

  // Handle of the repeating task that polls the last action map
  private int taskHandle;

  private final IConfig cfg;
  private final JavaPlugin plugin;
  private final ITeamHandler teams;
  private final IMoveListener move;

  public AfkListener(
    @AutoInject ITeamHandler teams,
    @AutoInject JavaPlugin plugin,
    @AutoInject IConfig cfg,
    @AutoInject IMoveListener move
  ) {
    this.teams = teams;
    this.plugin = plugin;
    this.cfg = cfg;

    this.move = move;
    this.lastAction = new HashMap<>();
  }

  //=========================================================================//
  //                                    API                                  //
  //=========================================================================//

  @Override
  public void cleanup() {
    Bukkit.getScheduler().cancelTask(taskHandle);
  }

  @Override
  public void initialize() {
    // Periodically poll the list of last actions for inactive players
    taskHandle = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::checkActions, 0L, 20L);

    // Reactivate all timeouts initially
    for (Player t : Bukkit.getOnlinePlayers()) {
      reactivateTimeout(t);

      // Reactivate timeouts on movement
      this.move.subscribe(t, () -> reactivateTimeout(t));
    }
  }

  @Override
  public boolean isAFK(Player p) {
    long last = lastAction.getOrDefault(p, System.currentTimeMillis());
    return System.currentTimeMillis() > last + TIMEOUT_MS;
  }

  @Override
  public void setAFK(Player p) {
    // Is already AFK
    if (isAFK(p))
      return;

    // Mark as AFK
    markAFK(p);
  }

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    reactivateTimeout(e.getPlayer());

    // Reactivate timeouts on movement
    this.move.subscribe(e.getPlayer(), () -> reactivateTimeout(e.getPlayer()));
  }


  @EventHandler
  public void onQuit(PlayerQuitEvent e) {
    lastAction.remove(e.getPlayer());
  }

  @EventHandler(ignoreCancelled = true)
  public void onChat(AsyncPlayerChatEvent e) {
    reactivateTimeout(e.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onCommand(PlayerCommandPreprocessEvent e) {
    // Ignore the AFK command to avoid flip-floping around
    if (e.getMessage().toLowerCase().startsWith("/afk"))
      return;

    reactivateTimeout(e.getPlayer());
  }

  @EventHandler
  public void onInteract(PlayerInteractEvent e) {
    reactivateTimeout(e.getPlayer());
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Refresh the AFK timeout for a given player
   * @param p Target player
   */
  private void reactivateTimeout(Player p) {
    lastAction.put(p, System.currentTimeMillis());

    // Fast resume "interrupt"
    if (teams.getGrayed(p))
      checkActions(p);
  }

  /**
   * Check all players' actions for afk timeouts
   */
  private void checkActions() {
    for (Player p : lastAction.keySet())
      checkActions(p);
  }

  /**
   * Check a specific player's action for an afk timeout
   * @param p Target player
   */
  private void checkActions(Player p) {
    // Still active
    if (!isAFK(p)) {

      // Not marked as AFK
      if (!teams.getGrayed(p))
        return;

      // Unmark grayed
      teams.setGrayed(p, false);

      // Broadcast
      for (Player t : Bukkit.getOnlinePlayers())
        t.sendMessage(
          cfg.get(ConfigKey.AFK_RESUMED)
            .withPrefix()
            .withVariable("name", p.getName())
            .asScalar()
        );

      return;
    }

    markAFK(p);
  }

  /**
   * Mark a player as AFK
   * @param p Target player
   */
  private void markAFK(Player p) {
    // Already marked as afk
    if (teams.getGrayed(p))
      return;

    // Just to make sure: set an action far enough in the past, since
    // this procedure can also be called programmatically at any time
    lastAction.put(p, System.currentTimeMillis() - TIMEOUT_MS);

    // Mark grayed
    teams.setGrayed(p, true);

    // Broadcast
    for (Player t : Bukkit.getOnlinePlayers())
      t.sendMessage(
        cfg.get(ConfigKey.AFK_WENT)
          .withPrefix()
          .withVariable("name", p.getName())
          .asScalar()
      );
  }
}
