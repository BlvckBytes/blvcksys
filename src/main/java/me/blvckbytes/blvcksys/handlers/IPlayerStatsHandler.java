package me.blvckbytes.blvcksys.handlers;

import org.bukkit.entity.Player;

import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/12/2022

  Public interfaces which the player-stats handler provides to other consumers.
*/
public interface IPlayerStatsHandler {

  /**
   * Register an interest in statistic updates for a specific category
   * @param statistic Statistic to be notified on
   * @param origin Update event origin
   */
  void registerUpdateInterest(PlayerStatistic statistic, Consumer<Player> origin);

  /**
   * Get the number of kills a player currently has
   * @param p Target player
   * @return Number of kills
   */
  int getKills(Player p);

  /**
   * Get the number of deaths a player currently has
   * @param p Target player
   * @return Number of deaths
   */
  int getDeaths(Player p);

  /**
   * Calculate the current KD (kills/deaths) of a target player
   * @param p Target player
   * @return KD value, rounded
   */
  double calculateKD(Player p);
}
