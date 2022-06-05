package me.blvckbytes.blvcksys.handlers;

import me.blvckbytes.blvcksys.persistence.models.PlayerStatsModel;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.List;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 05/12/2022

  Public interfaces which the player-stats handler provides to other consumers.
*/
public interface IPlayerStatsHandler {

  /**
   * Get the top five ranked players for any given statistic
   * @param statistic Statistic to rank by
   * @return List of top five players, or less, if less players played before
   */
  List<PlayerStatsModel> getTop5Ranked(PlayerStatistic statistic);

  /**
   * Register an interest in statistic updates for a specific category
   * @param statistic Statistic to be notified on
   * @param origin Update event origin
   */
  void registerUpdateInterest(PlayerStatistic statistic, Consumer<OfflinePlayer> origin);

  /**
   * Calculate the current KD (kills/deaths) of a target player
   * @param p Target player
   * @return KD value, rounded
   */
  double calculateKD(OfflinePlayer p);

  /**
   * Set the amount of money a player owns
   * @param p Target player
   * @param amount Amount of money
   */
  void setMoney(OfflinePlayer p, int amount);

  /**
   * Set the last login stamp of a player
   * @param p Target player
   * @param stamp Last login
   */
  void setLastLogin(OfflinePlayer p, Date stamp);

  /**
   * Get all current statistics of a player
   * @param p Target player
   * @return Current statistics
   */
  PlayerStatsModel getStats(OfflinePlayer p);
}
