package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IMuteHandler;
import me.blvckbytes.blvcksys.handlers.IPlayerStatsHandler;
import me.blvckbytes.blvcksys.handlers.IWarnHandler;
import me.blvckbytes.blvcksys.persistence.models.MuteModel;
import me.blvckbytes.blvcksys.persistence.models.PlayerStatsModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.TimeUtil;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/18/2022

  Display the publicly visible statistics of any given player.
*/
@AutoConstruct
public class StatsCommand extends APlayerCommand {

  private final IPlayerStatsHandler stats;
  private final IWarnHandler warns;
  private final IMuteHandler mutes;
  private final TimeUtil time;

  public StatsCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPlayerStatsHandler stats,
    @AutoInject IWarnHandler warns,
    @AutoInject IMuteHandler mutes,
    @AutoInject TimeUtil time
  ) {
    super(
      plugin, logger, cfg, refl,
      "stats",
      "Display the statistics of yourself or others",
      null,
      new CommandArgument("[player]", "Player to display the stats of")
    );

    this.stats = stats;
    this.warns = warns;
    this.mutes = mutes;
    this.time = time;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOfflinePlayers(args, currArg);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    OfflinePlayer target = offlinePlayer(args, 0, p);

    MuteModel currentMute = mutes.isCurrentlyMuted(target).orElse(null);
    PlayerStatsModel s = stats.getStats(target);

    p.sendMessage(
      cfg.get(ConfigKey.STATS_SCREEN)
        .withPrefixes()
        .withVariable("target", target.getName())
        .withVariable("kills", s.getKills())
        .withVariable("deaths", s.getDeaths())
        .withVariable("kd", stats.calculateKD(target))
        .withVariable("coins", s.getMoney())
        .withVariable("warns_total", warns.countAllWarns(target))
        .withVariable("warns_active", warns.countActiveWarns(target))
        .withVariable("playtime", time.formatDuration(s.getPlaytimeSeconds()))
        .withVariable(
          "mute_duration",
          currentMute == null ?
            cfg.get(ConfigKey.STATS_NONE).asScalar() :
            time.formatDuration(currentMute.getDurationSeconds())
        )
        .withVariable(
          "mute_remaining",
          currentMute == null ?
            cfg.get(ConfigKey.STATS_NONE).asScalar() :
            time.formatDuration(currentMute.getRemainingSeconds())
        )
        .asScalar()
    );
  }
}
