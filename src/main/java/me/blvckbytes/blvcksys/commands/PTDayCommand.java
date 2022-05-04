package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Change the time of your own client to day.
*/
@AutoConstruct
public class PTDayCommand extends APlayerCommand {

  private final IPTimeCommand ptime;

  public PTDayCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPTimeCommand ptime
  ) {
    super(
      plugin, logger, cfg, refl,
      "ptday",
      "Change the time of your client to day",
      PlayerPermission.COMMAND_PTIME,
      new CommandArgument("[player]", "Target player", PlayerPermission.COMMAND_PTIME_OTHERS)
    );

    this.ptime = ptime;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest all online players
    if (currArg == 0)
      return suggestOnlinePlayers(p, args, currArg, false);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Player target = onlinePlayer(args, 0, p);

    TimeShorthand time = TimeShorthand.DAY;
    ptime.setTime(p, target, time);

    // Not the dispatcher, inform about change for others
    if (!target.equals(p))
      p.sendMessage(
        cfg.get(ConfigKey.PTIME_SET_OTHERS_SENDER)
          .withPrefix()
          .withVariable("target", target.getName())
          .withVariable("time", time.name())
          .asScalar()
      );
  }
}
