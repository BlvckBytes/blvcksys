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

  Change the weather of your own client back to the server's.
*/
@AutoConstruct
public class PWResetCommand extends APlayerCommand {

  private final IPWeatherCommand pweather;

  public PWResetCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IPWeatherCommand pweather
  ) {
    super(
      plugin, logger, cfg, refl,
      "pwreset",
      "Change the weather of your client back to the server's",
      PlayerPermission.COMMAND_PWEATHER.toString(),
      new CommandArgument("[player]", "Target player", PlayerPermission.COMMAND_PWEATHER_OTHERS.toString())
    );

    this.pweather = pweather;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestOnlinePlayers(p, args, currArg, false);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    Player target = onlinePlayer(args, 0, p);
    pweather.setWeather(p, target, PWeatherType.RESET);

    // Not the dispatcher, inform about change for others
    if (!target.equals(p))
      p.sendMessage(
        cfg.get(ConfigKey.PWEATHER_RESET_OTHERS_SENDER)
          .withPrefix()
          .withVariable("target", target.getName())
          .withVariable("weather", PWeatherType.RESET.name())
          .asScalar()
      );
  }
}
