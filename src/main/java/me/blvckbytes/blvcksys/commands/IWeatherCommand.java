package me.blvckbytes.blvcksys.commands;

import org.bukkit.WeatherType;
import org.bukkit.World;
import org.bukkit.entity.Player;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/03/2022

  Public interfaces which the weather command provides to other consumers.
*/
public interface IWeatherCommand {

  /**
   * Set the weather in a given world to a given type and notify all affected players
   * @param dispatcher Dispatcher of this action
   * @param world World to change the weather of
   * @param type Weather type to apply
   * @param duration Duration of this weather-change
   */
  void setWeather(Player dispatcher, World world, WeatherType type, int duration);

  /**
   * Get the default duration for any given weather when not explicitly specified
   * @return Duration in ticks
   */
  int getDefaultWeatherDurationTticks();
}
