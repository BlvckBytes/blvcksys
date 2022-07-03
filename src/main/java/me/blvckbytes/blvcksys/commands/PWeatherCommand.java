package me.blvckbytes.blvcksys.commands;

import me.blvckbytes.blvcksys.commands.exceptions.CommandException;
import me.blvckbytes.blvcksys.config.ConfigKey;
import me.blvckbytes.blvcksys.config.IConfig;
import me.blvckbytes.blvcksys.config.PlayerPermission;
import me.blvckbytes.blvcksys.util.MCReflect;
import me.blvckbytes.blvcksys.di.AutoConstruct;
import me.blvckbytes.blvcksys.di.AutoInject;
import me.blvckbytes.blvcksys.util.logging.ILogger;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.stream.Stream;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Change the weather of your own client to either storm, sun or the server's.
*/
@AutoConstruct
public class PWeatherCommand extends APlayerCommand implements IPWeatherCommand {

  public PWeatherCommand(
    @AutoInject JavaPlugin plugin,
    @AutoInject ILogger logger,
    @AutoInject IConfig cfg,
    @AutoInject MCReflect refl
  ) {
    super(
      plugin, logger, cfg, refl,
      "pweather",
      "Change the weather of your client",
      PlayerPermission.COMMAND_PWEATHER.toString(),
      new CommandArgument("<type>", "The type of weather to set"),
      new CommandArgument("[player]", "Target player", PlayerPermission.COMMAND_PWEATHER_OTHERS.toString())
    );
  }

  //=========================================================================//
  //                                 Handler                                 //
  //=========================================================================//

  @Override
  protected Stream<String> onTabCompletion(Player p, String[] args, int currArg) {
    // Suggest all types of weather
    if (currArg == 0)
      return suggestEnum(args, currArg, PWeatherType.class);

    // Suggest all online players
    if (currArg == 1)
      return suggestOnlinePlayers(p, args, currArg, false);

    return super.onTabCompletion(p, args, currArg);
  }

  @Override
  protected void invoke(Player p, String label, String[] args) throws CommandException {
    PWeatherType type = parseEnum(PWeatherType.class, args, 0, null);
    Player target = onlinePlayer(args, 1, p);
    setWeather(p, target, type);

    // Not the dispatcher, inform about change for others
    if (!target.equals(p))
      p.sendMessage(
        cfg.get(type == PWeatherType.RESET ? ConfigKey.PWEATHER_RESET_OTHERS_SENDER : ConfigKey.PWEATHER_SET_OTHERS_SENDER)
          .withPrefix()
          .withVariable("target", target.getName())
          .withVariable("weather", type.name())
          .asScalar()
      );
  }

  //=========================================================================//
  //                                   API                                   //
  //=========================================================================//

  @Override
  public void setWeather(Player dispatcher, Player client, PWeatherType type) {
    // Map the custom weather enum back to bukkit's
    if (type == PWeatherType.CLEAR)
      client.setPlayerWeather(WeatherType.CLEAR);
    else if (type == PWeatherType.DOWNFALL)
      client.setPlayerWeather(WeatherType.DOWNFALL);
    else if (type == PWeatherType.RESET)
      client.resetPlayerWeather();

    // Inform the client about the change
    client.sendMessage(
      cfg.get(
        // Weather has been changed for the dispatcher themselves
        dispatcher.equals(client) ? (type == PWeatherType.RESET ? ConfigKey.PWEATHER_RESET : ConfigKey.PWEATHER_SET) :
        // Weather has been changed for someone else
        (type == PWeatherType.RESET ? ConfigKey.PWEATHER_RESET_OTHERS_RECEIVER : ConfigKey.PWEATHER_SET_OTHERS_RECEIVER)
      )
        .withPrefix()
        .withVariable("target", client.getName())
        .withVariable("issuer", dispatcher.getName())
        .withVariable("weather", type.name())
        .asScalar()
    );
  }
}
