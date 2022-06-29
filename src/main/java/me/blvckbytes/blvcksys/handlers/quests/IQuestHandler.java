package me.blvckbytes.blvcksys.handlers.quests;

import me.blvckbytes.blvcksys.config.sections.QuestSection;
import me.blvckbytes.blvcksys.config.sections.QuestStageSection;
import me.blvckbytes.blvcksys.config.sections.QuestTaskSection;
import me.blvckbytes.blvcksys.handlers.TriResult;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/*
  Author: BlvckBytes <blvckbytes@gmail.com>
  Created On: 06/25/2022

  Public interfaces which the quest handler provides to other consumers.
*/
public interface IQuestHandler {

  /**
   * Get all existing tasks loaded from configs
   */
  Map<String, QuestTaskSection> getTasks();

  /**
   * Get all existing quests loaded from configs
   */
  List<QuestSection> getQuests();

  /**
   * Get the parent stage of a task
   * @param task Target task
   * @return Parent stage, if exists
   */
  Optional<QuestStageSection> getParentStage(QuestTaskSection task);

  /**
   * Get the parent quest of a stage
   * @param stage Target stage
   * @return Parent quest, if exists
   */
  Optional<QuestSection> getParentQuest(QuestStageSection stage);

  /**
   * Get the token level separator sequence
   */
  String getTokenSeparator();

  /**
   * Fire an existing task due to a given player's action
   * @param p Target player
   * @param token Task token to fire
   * @return SUCC on successful firing, ERR if the player couldn't reach this task
   * and EMPTY if there was no task with that token or the player was unloaded
   */
  TriResult fireTask(Player p, String token);

  /**
   * Register an interest for the progress being made on quests
   * @param target Player that made progress
   */
  void registerProgressInterest(Consumer<Player> target);

  /**
   * Get the zero-based index of the currently active stage
   * within a quest's list of stages
   * @param p Target player
   * @param quest Target quest
   * @return Index, empty if the player has no progress on this quest yet
   */
  Optional<Integer> getActiveQuestStage(Player p, QuestSection quest);

  /**
   * Get the total quest progress in percent, based on how far
   * each of the tasks has been completed
   * @param p Target player
   * @param quest Target quest
   * @return Percentage value rounded to two decimals between 0 and 100
   */
  double getQuestProgress(Player p, QuestSection quest);

  /**
   * Get the number of completed counts on a task
   * @param p Target player
   * @param task Target task
   * @return Number of completed counts
   */
  int getTaskCompletedCount(Player p, QuestTaskSection task);

  /**
   * Checks whether the player has completed a full stage of tasks
   * @param p Target player
   * @param stage Target stage
   * @return True on completion, false if there's still progress to be made
   */
  boolean isStageComplete(Player p, QuestStageSection stage);
}
