package me.blvckbytes.blvcksys.commands;

import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/04/2022

  Public interfaces which the pweather command provides to other consumers.
*/
public interface IPWeatherCommand {

  /**
   * Set the weather for a client specifically and notify the user
   * @param dispatcher Dispatching player
   * @param client Client to change the weather for
   * @param type Weather type to apply
   */
  void setWeather(Player dispatcher, Player client, PWeatherType type);
}
