package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Change the weather of the world you're currently in to storm.
*/
@AutoConstruct
public class StormCommand extends APlayerCommand {

  private final IWeatherCommand weather;

  public StormCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl,
    @AutoInject IWeatherCommand weather
  ) {
    super(
      plugin, logger, cfg, refl,
      "storm",
      "Change the weather of the world you're in to storm",
      PlayerPermission.COMMAND_WEATHER,
      new CommandArgument("[duration]", "For how long to keep storming (ticks)")
    );

    this.weather = weather;
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    int duration = parseInt(args, 0, weather.getDefaultWeatherDurationTticks());
    weather.setWeather(p, p.getWorld(), WeatherType.STORM, duration);
  }
}
