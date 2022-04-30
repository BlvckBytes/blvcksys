package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.util.di.AutoConstruct;
import me.blvckbytes.blvcksys.util.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 04/30/2022

  Change the weather of the world you're currently in to either storm
  with a specific duration or permanent sun.
*/
@AutoConstruct
public class WeatherCommand extends APlayerCommand {

  // What duration to assume when none is specified
  private static final int DEFAULT_WEATHER_DURATION_TICKS = 20 * 60 * 60; // 1 hour

  /**
   * Represents the types of weather available
   */
  private enum WeatherType {
    STORM,
    SUN;
  }

  public WeatherCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "weather",
      "Change the weather of the world you're in",
      PlayerPermission.COMMAND_WEATHER,
      new CommandArgument("<type>", "The type of weather to set"),
      new CommandArgument("[duration]", "For how long to keep this weather (ticks)")
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    if (currArg == 0)
      return suggestEnum(args, currArg, WeatherType.class);
    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    WeatherType weather = parseEnum(WeatherType.class, args, 0);
    int duration = parseInt(args, 1, DEFAULT_WEATHER_DURATION_TICKS);
    setWeather(p, p.getWorld(), weather, duration);
  }

  //=========================================================================//
  //                                Utilities                                //
  //=========================================================================//

  /**
   * Set the weather in a given world to a given type and notify all affected players
   * @param dispatcher Dispatcher of this action
   * @param world World to change the weather of
   * @param type Weather type to apply
   * @param duration Duration of this weather-change
   */
  private void setWeather(Player dispatcher, World world, WeatherType type, int duration) {
    if (type == WeatherType.STORM) {
      world.setClearWeatherDuration(0);
      world.setWeatherDuration(duration);
      world.setThundering(true);
      world.setStorm(true);
    }

    else if (type == WeatherType.SUN) {
      world.setWeatherDuration(0);
      world.setClearWeatherDuration(duration);
      world.setThundering(false);
      world.setStorm(false);
    }

    // Notify all affected players
    for (Player affected : world.getPlayers())
      affected.sendMessage(
        cfg.get(ConfigKey.WEATHER_SET)
          .withPrefix()
          .withVariable("issuer", dispatcher.getName())
          .withVariable("weather", type.toString())
          .asScalar()
      );
  }
}
