package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.handlers.IPlayerStatsHandler;
import me.blvckbytes.blvcksys.handlers.PlayerStatistic;
import me.blvckbytes.blvcksys.persistence.models.PlayerStatsModel;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.TidyTable;
import me.blvckbytes.blvcksys.util.TimeUtil;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/05/2022

  Displays the top five players in any given measured statistic.
*/
@AutoConstruct
public class Top5Command extends APlayerCommand {

  private final IPlayerStatsHandler stats;
  private final IFontWidthTable fwTable;
  private final TimeUtil timeUtil;

  public Top5Command(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IFontWidthTable fwTable,
    @AutoInject IPlayerStatsHandler stats,
    @AutoInject TimeUtil timeUtil
  ) {
    super(
      plugin, logger, cfg, refl,
      "top5",
      "View the top five players regarding any statistic",
      null,
      new CommandArgument("<statistic>", "Statistic to rank by")
    );

    this.stats = stats;
    this.fwTable = fwTable;
    this.timeUtil = timeUtil;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestEnum(args, currArg, PlayerStatistic.class);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    PlayerStatistic statistic = parseEnum(PlayerStatistic.class, args, 0, null);

    List<PlayerStatsModel> topPlayers = stats.getTop5Ranked(statistic);
    TidyTable table = new TidyTable("|", fwTable);

    table.addLines(
      cfg.get(ConfigKey.TOP5_HEADER)
        .withPrefixes()
        .withVariable("statistic", statistic.name())
        .asScalar()
    );

    // Display existing players
    int i;
    for (i = 0; i < topPlayers.size(); i++) {
      PlayerStatsModel stats = topPlayers.get(i);

      table.addLine(
        cfg.get(ConfigKey.TOP5_ENTRY_PLAYER)
          .withPrefix()
          .withVariable("place", i + 1)
          .withVariable("player", stats.getOwner().getName())
          .withVariable("value", switch (statistic) {
            case KILLS -> stats.getKills();
            case DEATHS -> stats.getDeaths();
            case MONEY -> stats.getMoney();
            case PLAYTIME -> timeUtil.formatDuration(stats.getPlaytimeSeconds());
          })
          .asScalar()
      );
    }

    // Fill up remaining lines
    for (; i < 5; i++) {
      table.addLine(
        cfg.get(ConfigKey.TOP5_ENTRY_EMPTY)
          .withPrefix()
          .withVariable("place", i + 1)
          .asScalar()
      );
    }

    table.displayTo(p);
  }
}
