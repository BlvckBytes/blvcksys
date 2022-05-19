package me.blvckbytes.blvcksys.events;

import me.blvckbytes.blvcksys.commands.IVanishCommand;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerStatsHandler;
import me.blvckbytes.blvcksys.persistence.models.PlayerStatsModel;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Date;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/23/2022

  Overrides the vanilla join- and quit messages by config values.
*/
@AutoConstruct
public class JoinQuitListener implements Listener {

  private final IConfig cfg;
  private final IVanishCommand vanish;
  private final IPlayerStatsHandler stats;
  private final JavaPlugin plugin;

  public JoinQuitListener(
    @AutoInject IConfig cfg,
    @AutoInject IVanishCommand vanish,
    @AutoInject IPlayerStatsHandler stats,
    @AutoInject JavaPlugin plugin
  ) {
    this.cfg = cfg;
    this.vanish = vanish;
    this.stats = stats;
    this.plugin = plugin;
  }

  //=========================================================================//
  //                                 Listeners                               //
  //=========================================================================//

  @EventHandler
  public void onJoin(PlayerJoinEvent e) {
    e.setJoinMessage(
      cfg.get(ConfigKey.GENERIC_JOINED)
        .withPrefix()
        .withVariable("name", e.getPlayer().getDisplayName())
        .asScalar()
    );

    displayMOTD(e.getPlayer());
    stats.setLastLogin(e.getPlayer(), new Date());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onQuit(PlayerQuitEvent e) {
    // Don't print quit messages for vanished players
    if (vanish.isVanished(e.getPlayer())) {
      e.setQuitMessage(null);
      return;
    }

    e.setQuitMessage(
      cfg.get(ConfigKey.GENERIC_QUIT)
        .withPrefix()
        .withVariable("name", e.getPlayer().getDisplayName())
        .asScalar()
    );
  }

  //=========================================================================//
  //                                 Utilities                               //
  //=========================================================================//

  /**
   * Display the MOTD screen
   * @param p Target player
   */
  private void displayMOTD(Player p) {
    PlayerStatsModel s = stats.getStats(p);

    p.sendMessage(
      cfg.get(s.getLastLogin() != null ? ConfigKey.MOTD_SCREEN_RELOGIN : ConfigKey.MOTD_SCREEN_FIRST_JOIN)
        .withVariable("last_login", s.getLastLoginStr())
        .withVariable("name", p.getName())
        .withVariable("num_online", Bukkit.getOnlinePlayers().size())
        .withVariable("num_slots", plugin.getServer().getMaxPlayers())
        .asScalar()
    );
  }
}
